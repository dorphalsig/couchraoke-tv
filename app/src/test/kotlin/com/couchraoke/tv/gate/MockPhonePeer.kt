package com.couchraoke.tv.gate

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Drives the real `mock-phone` peer (a separate Python repository, located by
 * [peerDirectory]) as a subprocess over loopback — T027, per plan.md's "Loopback gate
 * design" and quickstart.md's exit-status mapping. FR-039 bars an in-process fake from
 * proving the control transport works, so every claim this gate makes has to come from
 * a real external process; this class is the only thing that launches it and parses
 * what it reports.
 *
 * Two verified traps (spec.md Out-of-Scope Observation 17):
 * - the peer writes **nothing** to stdout — `logging.basicConfig` is configured with no
 *   `stream=`, which defaults to `sys.stderr` — so the single `JOIN_RESULT {…}` line is
 *   only ever on stderr. [ProcessBuilder.redirectErrorStream] merges it into the stream
 *   this class reads, so it is captured regardless of which stream the peer chooses.
 * - the line is timestamp-prefixed — `13:40:28.197 [INFO    ] mock_phone_reconnect:
 *   JOIN_RESULT {…}` — so it is located with `indexOf(JOIN_RESULT_MARKER)`, never
 *   `startsWith`.
 *
 * `uv` needs `UV_SYSTEM_CERTS=1` on this network or it fails before ever reaching the
 * peer's own code. That must be set in the **subprocess** environment — it is not
 * inherited from the Gradle worker's — which is why it is written directly into
 * [ProcessBuilder.environment] below rather than assumed.
 *
 * Exit statuses 4 and 6 are assertions against the TV under test, not ordinary
 * outcomes (quickstart.md; spec.md Obs 17): 4 means a refusal closed the socket without
 * delivering its reason first (FR-016/FR-017), and 6 means the handshake deadline was
 * never enforced. [run] raises both as an explicit [AssertionError] instead of folding
 * them into [JoinProbeResult], so a TV protocol regression cannot be missed by a test
 * that only checks the exit code its own scenario expects.
 */
object MockPhonePeer {

    private const val PEER_DIRECTORY_PROPERTY = "mockphone.dir"
    private const val PEER_DIRECTORY_ENV = "MOCKPHONE_DIR"
    private const val PEER_MARKER_FILE = "mock_phone_reconnect.py"
    private const val PEER_DIRECTORY_NAME = "mockphone"
    private const val JOIN_RESULT_MARKER = "JOIN_RESULT "
    private const val DEFAULT_TIMEOUT_SECONDS = 30L
    private const val READER_DRAIN_TIMEOUT_SECONDS = 5L

    private const val EXIT_CLOSED_WITHOUT_REASON = 4
    private const val EXIT_HANDSHAKE_DEADLINE_NOT_ENFORCED = 6

    /**
     * Where the peer repository is checked out.
     *
     * Deliberately not a hardcoded absolute path and not a fixed relative one. `mockphone`
     * is a sibling of the *main* checkout, but this suite also runs from a git worktree
     * nested two levels deeper, so no single relative path reaches it from both — the same
     * relative-depth trap that broke the vendored-fixtures symlink (spec.md Observation 2).
     * Resolution order: the `mockphone.dir` system property, then the `MOCKPHONE_DIR`
     * environment variable, then the nearest ancestor of the working directory containing a
     * `mockphone/` directory that actually holds [PEER_MARKER_FILE]. The marker check stops
     * an unrelated same-named directory from being picked up and failing later as a
     * confusing `uv` error.
     */
    private val peerDirectory: File by lazy { resolvePeerDirectory() }

    /**
     * Runs `uv run mock-phone --tv-host [tvHost] --tv-port [tvPort]`, plus `--token
     * [token]` when [token] is non-null, plus [extraArgs] (e.g. `--join-only`,
     * `--protocol-version 2`, `--malformed-hello clientId`, `--silent-handshake`,
     * `--client-id`, `--hold`), and waits up to [timeoutSeconds] for it to exit.
     *
     * [timeoutSeconds] is this harness's own bound, not the peer's `--join-timeout` —
     * it must stay comfortably above whatever `--join-timeout` [extraArgs] carries (the
     * peer defaults that to 15s) so a hung peer fails the test instead of hanging the
     * Gradle worker forever; a timeout here forcibly kills the process rather than
     * waiting indefinitely.
     */
    fun run(
        tvPort: Int,
        tvHost: String = "127.0.0.1",
        token: String? = null,
        extraArgs: List<String> = emptyList(),
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    ): JoinProbeResult {
        val args = buildArgs(tvHost, tvPort, token, extraArgs)
        val process = startPeerProcess(args)

        val output = StringBuilder()
        val outputReader = Thread({
            process.inputStream.bufferedReader().forEachLine { output.appendLine(it) }
        }, "mock-phone-output-reader").apply {
            isDaemon = true
            start()
        }

        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            outputReader.join(TimeUnit.SECONDS.toMillis(READER_DRAIN_TIMEOUT_SECONDS))
            fail(
                "mock-phone did not exit within ${timeoutSeconds}s and was killed; a hung peer " +
                    "must fail the test, not hang the Gradle worker. args=$args output so far:\n$output",
            )
        }
        outputReader.join(TimeUnit.SECONDS.toMillis(READER_DRAIN_TIMEOUT_SECONDS))

        val exitStatus = process.exitValue()
        val result = parseResult(exitStatus, output.toString(), args)
        failOnTvProtocolViolation(exitStatus, result)
        return result
    }

    private fun buildArgs(tvHost: String, tvPort: Int, token: String?, extraArgs: List<String>): List<String> =
        buildList {
            add("--tv-host")
            add(tvHost)
            add("--tv-port")
            add(tvPort.toString())
            if (token != null) {
                add("--token")
                add(token)
            }
            addAll(extraArgs)
        }

    private fun startPeerProcess(args: List<String>): Process {
        val builder = ProcessBuilder(listOf("uv", "run", "mock-phone") + args)
            .directory(peerDirectory)
            .redirectErrorStream(true)
        // Must be set on the subprocess's own environment map — the Gradle worker's
        // environment is not inherited by uv's resolved child process.
        builder.environment()["UV_SYSTEM_CERTS"] = "1"
        return builder.start()
    }

    private fun resolvePeerDirectory(): File {
        val configured = System.getProperty(PEER_DIRECTORY_PROPERTY)
            ?: System.getenv(PEER_DIRECTORY_ENV)
        if (configured != null) {
            val directory = File(configured)
            if (!File(directory, PEER_MARKER_FILE).isFile) {
                fail(
                    "The mock-phone peer directory was configured as '$configured', but that " +
                        "directory does not contain $PEER_MARKER_FILE. Point " +
                        "-D$PEER_DIRECTORY_PROPERTY or $PEER_DIRECTORY_ENV at the mockphone checkout.",
                )
            }
            return directory
        }

        val workingDirectory = System.getProperty("user.dir") ?: "."
        val discovered = generateSequence(File(workingDirectory).absoluteFile) { it.parentFile }
            .map { File(it, PEER_DIRECTORY_NAME) }
            .firstOrNull { File(it, PEER_MARKER_FILE).isFile }

        return discovered ?: fail(
            "Could not find the mock-phone peer. FR-039 bars an in-process fake from proving " +
                "the transport, so this gate cannot run without the real peer. Looked for a " +
                "'$PEER_DIRECTORY_NAME' directory containing $PEER_MARKER_FILE in every ancestor of " +
                "'$workingDirectory'. Clone the peer repository beside this one, or " +
                "set -D$PEER_DIRECTORY_PROPERTY / $PEER_DIRECTORY_ENV to its location.",
        )
    }

    /** Locates the single `JOIN_RESULT {…}` line and decodes its JSON tail into [JoinProbeResult]. */
    private fun parseResult(exitStatus: Int, fullOutput: String, args: List<String>): JoinProbeResult {
        val resultLine = fullOutput.lineSequence().singleOrNull { it.contains(JOIN_RESULT_MARKER) }
            ?: fail(
                "mock-phone produced no single JOIN_RESULT line (exit $exitStatus). " +
                    "args=$args full output:\n$fullOutput",
            )

        val jsonStart = resultLine.indexOf(JOIN_RESULT_MARKER) + JOIN_RESULT_MARKER.length
        val json = Json.parseToJsonElement(resultLine.substring(jsonStart)).jsonObject

        return JoinProbeResult(
            exitStatus = exitStatus,
            outcome = json.getValue("outcome").jsonPrimitive.content,
            clientId = json.getValue("clientId").jsonPrimitive.content,
            deviceName = json.getValue("deviceName").jsonPrimitive.content,
            errorCode = json.getValue("errorCode").jsonPrimitive.contentOrNull,
            errorMessage = json.getValue("errorMessage").jsonPrimitive.contentOrNull,
            closeCode = json.getValue("closeCode").jsonPrimitive.intOrNull,
            closeReason = json.getValue("closeReason").jsonPrimitive.contentOrNull,
            sessionId = json.getValue("sessionId").jsonPrimitive.contentOrNull,
            connectionId = json.getValue("connectionId").jsonPrimitive.intOrNull,
            validationError = json.getValue("validationError").jsonPrimitive.contentOrNull,
            heldSeconds = json.getValue("heldSeconds").jsonPrimitive.doubleOrNull,
        )
    }

    private fun failOnTvProtocolViolation(exitStatus: Int, result: JoinProbeResult) {
        when (exitStatus) {
            EXIT_CLOSED_WITHOUT_REASON -> fail(
                "mock-phone exit 4: the TV closed the connection WITHOUT delivering a refusal " +
                    "reason first, violating FR-016/FR-017. result=$result",
            )
            EXIT_HANDSHAKE_DEADLINE_NOT_ENFORCED -> fail(
                "mock-phone exit 6: the TV neither answered nor closed within --join-timeout — " +
                    "the handshake deadline was never enforced (FR-017). result=$result",
            )
        }
    }

    private fun fail(message: String): Nothing = throw AssertionError(message)
}

/**
 * The parsed shape of one `JOIN_RESULT` line (`mock_phone_reconnect.py`'s `JoinOutcome`).
 * Every field is always present in the peer's JSON — the nullable ones are `null` when
 * not applicable to [outcome] rather than absent, so no field here is ever "missing".
 *
 * [outcome] is one of `accepted`, `rejected`, `closed`, `timeout`, `unexpected`,
 * `connect_failed`. [exitStatus] is read from the process's real exit code, which
 * mirrors the peer's own self-reported `exitStatus` JSON field by construction
 * (`sys.exit(outcome.exit_status())`).
 */
data class JoinProbeResult(
    val exitStatus: Int,
    val outcome: String,
    val clientId: String,
    val deviceName: String,
    val errorCode: String?,
    val errorMessage: String?,
    val closeCode: Int?,
    val closeReason: String?,
    val sessionId: String?,
    val connectionId: Int?,
    val validationError: String?,
    val heldSeconds: Double?,
)
