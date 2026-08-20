package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.session.model.AssetPort
import com.couchraoke.tv.domain.session.model.ConnectionId
import com.couchraoke.tv.domain.session.model.DeviceId
import com.couchraoke.tv.domain.session.model.RosterEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T030: covers [SessionRoster]'s admit path only (contracts/domain-api.md). Capacity (T051)
 * and reclaim (T057) belong to US2/US3 and are not exercised here.
 *
 * T042 later extends this file to cover [SessionRoster.detach]'s FR-023 retention path,
 * which the disconnect flow needs alongside the admit path.
 */
class SessionRosterTest {

    @Test(timeout = 30_000)
    fun admittingANewDeviceReturnsAdmittedWithAMatchingEntry() {
        val roster = SessionRoster()

        val result = roster.admit(
            deviceId = DEVICE_A,
            displayName = "Alice's Phone",
            appVersion = "1.0.0",
            assetPort = AssetPort(8080),
            connectionId = ConnectionId(1),
        )

        val admitted = result as? RosterAdmission.Admitted
            ?: error("expected RosterAdmission.Admitted, was $result")
        assertEquals(
            RosterEntry(
                deviceId = DEVICE_A,
                displayName = "Alice's Phone",
                appVersion = "1.0.0",
                assetPort = AssetPort(8080),
                connection = ConnectionId(1),
            ),
            admitted.entry,
        )
    }

    @Test(timeout = 30_000)
    fun admittingANewDeviceGrowsSizeByOne() {
        val roster = SessionRoster()
        assertEquals(0, roster.size)

        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))

        assertEquals(1, roster.size)

        roster.admit(DEVICE_B, "Bob's Phone", "1.0.0", AssetPort(8081), ConnectionId(2))

        assertEquals(
            "a second, distinct device must grow size to 2, not merge with the first",
            2,
            roster.size,
        )
    }

    @Test(timeout = 30_000)
    fun connectedReflectsEveryAdmittedDeviceWithALiveConnection() {
        val roster = SessionRoster()
        assertTrue("connected must start empty", roster.connected.isEmpty())

        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))

        assertEquals(1, roster.connected.size)
        assertEquals(DEVICE_A, roster.connected.single().deviceId)
        assertEquals(
            "connected entries must carry the just-allocated ConnectionId",
            ConnectionId(1),
            roster.connected.single().connection,
        )
    }

    @Test(timeout = 30_000)
    fun entriesReflectsEveryAdmittedDeviceRegardlessOfConnectionState() {
        val roster = SessionRoster()

        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))
        roster.admit(DEVICE_B, "Bob's Phone", "1.0.0", AssetPort(8081), ConnectionId(2))

        assertEquals(setOf(DEVICE_A, DEVICE_B), roster.entries.map { it.deviceId }.toSet())
    }

    @Test(timeout = 30_000)
    fun detachingAConnectedDeviceClearsItsConnectionButRetainsTheEntry() {
        val roster = SessionRoster()
        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))

        val detached = roster.detach(DEVICE_A, ConnectionId(1))

        assertTrue("detach must report success for a known, connected device", detached)
        assertEquals(
            "the entry must survive detach -- only its live connection is cleared (FR-023)",
            1,
            roster.size,
        )
        assertTrue("the device must no longer be projected as connected", roster.connected.isEmpty())
        assertEquals(null, roster.entries.single { it.deviceId == DEVICE_A }.connection)
    }

    @Test(timeout = 30_000)
    fun detachingAnUnknownDeviceReturnsFalseAndChangesNothing() {
        val roster = SessionRoster()
        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))

        val detached = roster.detach(DEVICE_B, ConnectionId(2))

        assertFalse("detach must report false for a device never admitted", detached)
        assertEquals(1, roster.size)
        assertEquals(1, roster.connected.size)
    }

    @Test(timeout = 30_000)
    fun constructingWithANonPositiveCapacityThrows() {
        assertThrows(IllegalArgumentException::class.java) { SessionRoster(capacity = 0) }
        assertThrows(IllegalArgumentException::class.java) { SessionRoster(capacity = -1) }
    }

    /**
     * Re-admitting a `deviceId` already on the roster is T057's reclaim job (see this
     * class's KDoc); this phase has no reclaim path to fall into, so it must fail loudly
     * rather than silently double-admitting or overwriting the existing entry. This test
     * documents that current, deliberate boundary -- T057 replaces the [TODO] this reaches
     * with real reclaim behaviour, at which point this test is expected to be rewritten.
     */
    @Test(timeout = 30_000)
    fun admittingAnAlreadyPresentDeviceIsNotYetImplemented() {
        val roster = SessionRoster()
        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))

        assertThrows(NotImplementedError::class.java) {
            roster.admit(DEVICE_A, "Alice's Phone (second hello)", "1.0.0", AssetPort(8082), ConnectionId(2))
        }
    }

    private companion object {
        val DEVICE_A = DeviceId("device-aaaa")
        val DEVICE_B = DeviceId("device-bbbb")
    }
}
