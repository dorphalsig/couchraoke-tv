package com.couchraoke.tv.presentation.join

import com.couchraoke.tv.di.SessionComponent
import com.couchraoke.tv.domain.control.AdmissionDecision
import com.couchraoke.tv.domain.control.ControlConnectionHandler
import com.couchraoke.tv.domain.control.ControlTransport
import com.couchraoke.tv.domain.control.StartedTransport
import com.couchraoke.tv.domain.control.model.Hello
import com.couchraoke.tv.domain.platform.AnnouncementHandle
import com.couchraoke.tv.domain.platform.LocalAddressProvider
import com.couchraoke.tv.domain.platform.MulticastLease
import com.couchraoke.tv.domain.platform.SessionAnnouncer
import com.couchraoke.tv.domain.session.JoinCodeGenerator
import com.couchraoke.tv.domain.session.SessionCoordinator
import com.couchraoke.tv.domain.session.model.DeviceId
import com.couchraoke.tv.presentation.qr.QrPayloadEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress

/**
 * T032: [JoinViewModel.uiState]'s `connectedCount` must track
 * [SessionCoordinator.connectedDevices] -- the live connections -- and follow a join or a drop
 * with no user action (SC-007), never [com.couchraoke.tv.domain.session.model.SessionSnapshot.roster]`.size`,
 * which keeps a retained-but-disconnected entry (FR-025). [JoinViewModel] performs no network
 * I/O (FR-035): every test below drives the *real* [SessionCoordinator] directly and never
 * touches a socket, so the only way `uiState` could change is by observing the coordinator.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JoinViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test(timeout = 30_000)
    fun connectedCountStartsAtZeroWithNoDeviceAdmitted() {
        val viewModel = newViewModel(newCoordinator())

        assertEquals(
            "acceptance scenario 1: the connected count reads zero before any phone joins",
            0,
            viewModel.uiState.value.connectedCount,
        )
    }

    @Test(timeout = 30_000)
    fun connectedCountTracksAnAdmittedDeviceWithNoUserAction() {
        val coordinator = newCoordinator()
        val viewModel = newViewModel(coordinator)

        coordinator.admit(helloFrom(DEVICE_A))

        assertEquals(
            "connectedCount must follow a join with no user action, refresh, or re-opening (SC-007)",
            1,
            viewModel.uiState.value.connectedCount,
        )
    }

    @Test(timeout = 30_000)
    fun connectedCountTracksASecondAdmittedDeviceWithNoUserAction() {
        // Guards against a hardcoded 1: a second real connection must also be reflected, not
        // just a flip from zero to one.
        val coordinator = newCoordinator()
        val viewModel = newViewModel(coordinator)

        coordinator.admit(helloFrom(DEVICE_A))
        coordinator.admit(helloFrom(DEVICE_B))

        assertEquals(2, viewModel.uiState.value.connectedCount)
    }

    @Test(timeout = 30_000)
    fun connectedCountDropsWhenADeviceDisconnectsWithNoUserAction() {
        val coordinator = newCoordinator()
        val viewModel = newViewModel(coordinator)
        val admitted = coordinator.admit(helloFrom(DEVICE_A)) as AdmissionDecision.Admitted

        coordinator.onDisconnected(DeviceId(DEVICE_A), admitted.connectionId)

        assertEquals(
            "connectedCount must follow a drop with no user action (SC-007)",
            0,
            viewModel.uiState.value.connectedCount,
        )
    }

    @Test(timeout = 30_000)
    fun connectedCountNeverCountsARetainedButDisconnectedRosterEntry() {
        // Pins FR-025 directly: a disconnected device keeps its roster slot (so roster.size
        // stays 1 below) but connectedCount must already read 0, proving the derivation is
        // coordinator.connectedDevices.size and not SessionSnapshot.roster.size.
        val coordinator = newCoordinator()
        val viewModel = newViewModel(coordinator)
        val admitted = coordinator.admit(helloFrom(DEVICE_A)) as AdmissionDecision.Admitted

        coordinator.onDisconnected(DeviceId(DEVICE_A), admitted.connectionId)

        assertEquals(
            "guard: the roster slot must still be retained after a disconnect (FR-024)",
            1,
            coordinator.snapshot.value.roster.size,
        )
        assertEquals(
            "FR-025: connectedCount must not count a retained-but-disconnected roster entry",
            0,
            viewModel.uiState.value.connectedCount,
        )
    }

    @Test(timeout = 30_000)
    fun uiStateExposesTheJoinCodeDisplayAndQrPayloadDerivedFromTheCoordinatorAndEndpoint() {
        val coordinator = newCoordinator()
        val viewModel = newViewModel(coordinator)

        val expectedPayload = QrPayloadEncoder.encode(
            ENDPOINT.address,
            ENDPOINT.port,
            coordinator.snapshot.value.joinCode,
        )

        assertEquals(coordinator.snapshot.value.joinCode.display, viewModel.uiState.value.joinCodeDisplay)
        assertEquals(expectedPayload, viewModel.uiState.value.qrPayload)
    }

    @Test(timeout = 30_000)
    fun onOverlayDismissedLeavesTheSessionAndConnectedDevicesUntouched() {
        // FR-033 at the view-model layer: dismissing the overlay must not end the session or
        // disconnect anyone. JoinViewModel does no I/O (FR-035), so the only way it *could*
        // violate that is by calling something on the coordinator -- this proves it doesn't,
        // by asserting the coordinator's own observable state is bit-for-bit unchanged.
        val coordinator = newCoordinator()
        val viewModel = newViewModel(coordinator)
        coordinator.admit(helloFrom(DEVICE_A))
        val snapshotBefore = coordinator.snapshot.value
        val connectedBefore = coordinator.connectedDevices.value

        viewModel.onOverlayDismissed()

        assertEquals(snapshotBefore, coordinator.snapshot.value)
        assertEquals(connectedBefore, coordinator.connectedDevices.value)
        assertEquals(1, viewModel.uiState.value.connectedCount)
    }

    private fun newViewModel(coordinator: SessionCoordinator): JoinViewModel =
        JoinViewModel(coordinator = coordinator, qrEncoder = QrPayloadEncoder, endpoint = ENDPOINT)

    private fun newCoordinator(): SessionCoordinator = SessionComponent(
        transport = FakeControlTransport,
        announcer = FakeSessionAnnouncer,
        addressProvider = LocalAddressProvider { null },
        multicastLease = FakeMulticastLease,
        joinCodeGenerator = JoinCodeGenerator(),
        clock = { 0L },
    ).createCoordinator()

    private fun helloFrom(clientId: String): Hello = Hello(
        type = "hello",
        protocolVersion = 1,
        clientId = clientId,
        deviceName = "$clientId's Phone",
        appVersion = "1.0.0",
        httpPort = 8080,
    )

    /** A do-nothing [ControlTransport]; the loopback gate proves the real one, not this test. */
    private object FakeControlTransport : ControlTransport {
        override suspend fun start(port: Int, handler: ControlConnectionHandler): StartedTransport =
            object : StartedTransport {
                override val boundPort: Int = port
            }

        override suspend fun stop() = Unit
    }

    private object FakeSessionAnnouncer : SessionAnnouncer {
        override suspend fun publish(
            address: Inet4Address,
            instanceName: String,
            port: Int,
            joinCode: String,
            protocolVersion: Int,
        ): AnnouncementHandle = object : AnnouncementHandle {
            override val registeredInstanceName: String = instanceName
        }

        override suspend fun withdraw(handle: AnnouncementHandle) = Unit
    }

    private object FakeMulticastLease : MulticastLease {
        override fun acquire() = Unit
        override fun release() = Unit
    }

    private companion object {
        const val DEVICE_A = "device-aaaa"
        const val DEVICE_B = "device-bbbb"
        val ENDPOINT = ControlEndpoint(
            address = InetAddress.getByName("192.168.1.42") as Inet4Address,
            port = 51900,
        )
    }
}
