---
name: gradle-validation
description: Runs the four validation gates (unit, screenshot, loopback, device) that prove a slice works. Use before returning from implementation and before marking a slice done.
---
# Gradle Validation

A slice is proved by the gates it declares in `plan.md`, not by `testBranch` alone. `testBranch`
runs entirely in the JVM. It cannot observe a native decoder, a real socket, or a real clock, so it
cannot see whether video plays, whether lyrics advance, or whether the layout fits a TV.

**Every slice MUST pass at least one L or D gate.** A slice proved only by U and S is not proved.

## Context

Read in order:
- `.specify/memory/constitution.md` — Principle IV defines the gates
- `plan.md` §2 — the gate vocabulary and which gates each slice declares
- `CLAUDE.md`
- Load `navigation` skill

## The four gates

| Tag | What it proves | Command |
|-----|----------------|---------|
| **U** | Pure logic. Parsers, scoring maths, state machines. | `.\gradlew.bat :app:testBranch --src <FQCNs> --test <FQCNs>` |
| **S** | Layout at real TV size, against a committed baseline. | `.\gradlew.bat :app:verifyRoborazziDebug` |
| **L** | The real peer protocol over loopback. | `mockphone` + the slice's loopback test, per `plan.md` |
| **D** | The real device: native decoder, real GPU, real clock. | `.\gradlew.bat :app:connectedDebugAndroidTest` |

This is a Windows/PowerShell repository: use `.\gradlew.bat`, and do **not** wrap commands in
`timeout`, which does not exist in PowerShell. See `CLAUDE.md` §3a.

## U — unit

`.\gradlew.bat :app:testBranch --src <changed prod FQCNs> --test <changed test FQCNs>`

Include changed prod FQCNs in `--src` and changed test FQCNs in `--test`. When either side of a
source/test pair changes, include both.

`testBranch` also runs detekt and JaCoCo. They are hygiene rather than proof of behaviour, but they
are still part of the gate.

**Run `testBranch`, not `testDebugUnitTest --tests "…"`.** They are not interchangeable. A scoped
`--tests` run executes the tests and nothing else: it skips detekt and it skips JaCoCo entirely.
Substituting it produces a green result that has checked strictly less than the gate claims, and a
lint violation introduced by that task then surfaces later as someone else's build failure, long
after the task was marked done. If you have a reason to run `--tests` for a fast inner loop, run
`.\gradlew.bat :app:detekt` before you report a result.

When a class is deliberately excluded from `--src` — thin adapters, per `plan.md` — say so
explicitly in the report. An omitted FQCN and a deliberately excluded one look identical afterwards.

### What the coverage gate measures

`LINE ≥ 80%` and `BRANCH ≥ 70%`, both enforced, aggregated over the `--src` selection.

Do not read a green coverage result as "80% of this class is tested". Three properties change what
the number means:

- **BRANCH is skipped, not failed, on a class with no `if`/`when`.** JaCoCo reports 0/0, the ratio is
  `NaN`, and `Limit.check` returns no violation for `NaN`. Most of `domain/` has no decision points,
  so for those classes only the LINE limit is doing any work.
- **The rule evaluates the bundle, not each class.** A 100% class and a 60% class in one `--src`
  selection average to a pass. Narrow selections measure what you think they measure; wide ones
  hide things.
- **A line counts as covered once any one of its instructions runs.** LINE therefore over-reports
  partially executed lines. BRANCH exists to close that gap, and reads 10–20 points lower on the same
  code — a lower threshold on a stricter metric is not a weaker standard.

Thresholds are fixed and there is no per-class override. If a class genuinely cannot meet them yet,
the mechanism is a deferral from `--src` recorded in `plan.md` with its reason, where a reviewer will
see it — not a number quietly lowered in the build.

If you ever change a threshold or a counter, **prove the new limit can fail** before trusting it:
raise it past the measured value, watch the build break and name the counter, then restore it. A
coverage rule that silently passes is worse than none, because it is believed.

## S — screenshot

`.\gradlew.bat :app:verifyRoborazziDebug`

Verify against a committed baseline. Do **not** use `testBranchRoborazzi` as evidence: it sets
`roborazzi.test.record=true`, which rewrites the baseline and therefore can never fail. That is why
the oversized layouts shipped three times.

The Robolectric qualifier MUST be `w960dp-h540dp-land-television-xhdpi-notouch`. This is a 1080p TV
at density 2.0, matching `TvPreviewWidth` and `TvPreviewHeight` in `tv_app.md` §2.6. A qualifier of
`w1920dp-h1080dp` renders at four times the target area, and every screenshot will look correct
while being wrong on the TV.

When a baseline changes, inspect the diff and state in the checkpoint why the new rendering is
right. A changed baseline nobody looked at is not evidence.

Required when the slice touches any Screen, Modal, Preview, or Roborazzi state. Steps:

1. Run `git branch --show-current`.
2. Read `specs/<branch-name>/spec.md` and the wireframe it references.
3. Compare each rendered screenshot to the spec: labels, layout, sizes, colours, fonts.
4. Record a UI checkpoint: screenshot filename, spec section compared against, result.

## L — loopback

Start the real `mockphone` process and drive the app against it over `127.0.0.1`. `mockphone` is a
separate Python repo. It is not a fake and cannot be stubbed, which is the point.

```
python mock_phone_reconnect.py --tv-host 127.0.0.1 --song-dir <fixture songs> --mode perfect
```

Modes: `perfect`, `silence`, `partial`, `octave_off`, `fixed_gaps`, `replay`. Use `--hit-rate` with
`partial`, `--gap-frames` with `fixed_gaps`, `--replay-file` with `replay`. Add `--disconnect-after`
and `--reconnect-delay` to exercise session lifecycle.

L is the default gate for anything crossing the peer boundary: handshake, clock sync, pitch stream,
manifest fetch, file transfer, reconnect.

`mockphone` runs under `uv`, which needs `UV_SYSTEM_CERTS=1` in its environment on this network. A
test harness that launches the peer as a subprocess must set it in the **subprocess** environment;
setting it only in your own shell is not inherited and the peer fails to launch. A peer that never
started is a `blocked` gate, not a passed one.

Assert on the peer's documented exit status and its `JOIN_RESULT` output, and surface a non-zero
exit as an explicit assertion failure. A harness that ignores the peer's exit code can report green
while the peer refused the connection.

## D — device

`.\gradlew.bat :app:connectedDebugAndroidTest`

Required for anything touching LibVLC, the GPU, or wall-clock timing. Assert on screencaps pulled
from the device, not on log lines.

If no device is reachable, the slice is **blocked**, not passed. Say so and stop.

## Validation fails if

- any declared gate fails
- the slice declares no L or D gate
- a test inside a gate was skipped; `assumeTrue` and equivalent runtime skips are failures, not
  passes
- a screenshot baseline changed and nobody inspected the diff
- a required Screen, Modal, or `@Preview` is missing or wrong
- a rendered screenshot differs from the spec's behaviour, copy, state, or layout

## Completion

Report green only when every gate the slice declares passed fresh, at least one of them was L or D,
and all failures are resolved. Report `blocked` when a gate cannot run.

Never report a slice complete on U and S alone. An implementation green result is not completion:
completion requires `orchestration` to run the gates independently and audit the evidence.
