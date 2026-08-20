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
 * T030: covers [SessionRoster]'s admit path only (contracts/domain-api.md). T045 extends it
 * with the capacity cases (FR-015); reclaim (T057) belongs to US3 and is not exercised here.
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

    @Test(timeout = 30_000)
    fun theEleventhPreviouslyUnseenDeviceReturnsAtCapacity() {
        val roster = SessionRoster()
        DEFAULT_CAPACITY_DEVICE_IDS.forEachIndexed { index, deviceId ->
            roster.admit(deviceId, "Phone $index", "1.0.0", AssetPort(8080), ConnectionId(index + 1))
        }
        assertEquals("the roster must be full after admitting capacity distinct devices", 10, roster.size)

        val result = roster.admit(
            ELEVENTH_DEVICE,
            "Phone 11",
            "1.0.0",
            AssetPort(8080),
            ConnectionId(DEFAULT_CAPACITY_DEVICE_IDS.size + 1),
        )

        assertEquals(RosterAdmission.AtCapacity, result)
        assertEquals(
            "a device refused for capacity must not consume a roster slot",
            10,
            roster.size,
        )
    }

    @Test(timeout = 30_000)
    fun capacityIsEvaluatedOnlyForPreviouslyUnseenDevices() {
        val roster = SessionRoster(capacity = 1)
        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))

        val unseenResult = roster.admit(DEVICE_B, "Bob's Phone", "1.0.0", AssetPort(8081), ConnectionId(2))
        assertEquals(
            "a previously-unseen device must be refused once the roster is full",
            RosterAdmission.AtCapacity,
            unseenResult,
        )

        // A known deviceId must never be refused for capacity (FR-015, FR-020, FR-021): it must
        // still reach the reclaim branch (T057's TODO), not be short-circuited into AtCapacity.
        assertThrows(NotImplementedError::class.java) {
            roster.admit(DEVICE_A, "Alice's Phone (again)", "1.0.0", AssetPort(8082), ConnectionId(3))
        }
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

        // Ten distinct device ids filling the default capacity, plus one more previously-unseen.
        val DEFAULT_CAPACITY_DEVICE_IDS = (1..10).map { DeviceId("device-000$it") }
        val ELEVENTH_DEVICE = DeviceId("device-00011")
    }
}
