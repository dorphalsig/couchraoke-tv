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
 * T030: covers [SessionRoster]'s admit path for previously-unseen devices
 * (contracts/domain-api.md). T045 extends it with the capacity cases (FR-015). T042 extends
 * it with [SessionRoster.detach]'s FR-023 retention path. T054 extends
 * [SessionRosterReclaimAndReleaseTest], a sibling class in this file, with reclaim
 * (FR-020, FR-021), `detach`'s FR-022 identity guard, and `release`/`releaseDisconnected`
 * (FR-024).
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
        // still reclaim, not be short-circuited into AtCapacity.
        val knownResult = roster.admit(DEVICE_A, "Alice's Phone (again)", "1.0.0", AssetPort(8082), ConnectionId(3))
        assertTrue(
            "a known deviceId must reclaim rather than be refused for capacity",
            knownResult is RosterAdmission.Reclaimed,
        )
        assertEquals("reclaiming a known device must not change roster size", 1, roster.size)
    }

    private companion object {
        val DEVICE_A = DeviceId("device-aaaa")
        val DEVICE_B = DeviceId("device-bbbb")

        // Ten distinct device ids filling the default capacity, plus one more previously-unseen.
        val DEFAULT_CAPACITY_DEVICE_IDS = (1..10).map { DeviceId("device-000$it") }
        val ELEVENTH_DEVICE = DeviceId("device-00011")
    }
}

/**
 * T057 extends [SessionRosterTest] with reclaim, `detach`'s FR-022 identity guard, and
 * `release`/`releaseDisconnected` (FR-024). Kept as a sibling class in the same file to stay
 * under detekt's `TooManyFunctions` limit on [SessionRosterTest].
 */
class SessionRosterReclaimAndReleaseTest {

    @Test(timeout = 30_000)
    fun reclaimingAKnownDeviceRefreshesFieldsKeepsSizeUnchangedAndPreservesOrder() {
        val roster = SessionRoster()
        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))
        roster.admit(DEVICE_B, "Bob's Phone", "1.0.0", AssetPort(8081), ConnectionId(2))

        val result = roster.admit(
            deviceId = DEVICE_A,
            displayName = "Alice's Phone (v2)",
            appVersion = "2.0.0",
            assetPort = AssetPort(9090),
            connectionId = ConnectionId(3),
        )

        val reclaimed = result as? RosterAdmission.Reclaimed
            ?: error("expected RosterAdmission.Reclaimed, was $result")
        assertEquals(
            "reclaim must refresh displayName, appVersion and assetPort from the new hello",
            RosterEntry(
                deviceId = DEVICE_A,
                displayName = "Alice's Phone (v2)",
                appVersion = "2.0.0",
                assetPort = AssetPort(9090),
                connection = ConnectionId(3),
            ),
            reclaimed.entry,
        )
        assertEquals("reclaim must report the entry's prior live connection", ConnectionId(1), reclaimed.previous)
        assertEquals("reclaiming a known device must not grow the roster", 2, roster.size)
        assertEquals(
            "reclaim must not move the device to the end of admission order",
            listOf(DEVICE_A, DEVICE_B),
            roster.entries.map { it.deviceId },
        )
    }

    @Test(timeout = 30_000)
    fun reclaimSucceedsAtCapacity() {
        val roster = SessionRoster(capacity = 1)
        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))

        val result = roster.admit(DEVICE_A, "Alice's Phone (again)", "1.0.0", AssetPort(8080), ConnectionId(2))

        assertTrue(
            "a known deviceId must reclaim even when the roster is at capacity (FR-020, FR-021)",
            result is RosterAdmission.Reclaimed,
        )
        assertEquals(1, roster.size)
    }

    @Test(timeout = 30_000)
    fun reclaimSucceedsWhenDeviceHasNoLiveConnection() {
        val roster = SessionRoster()
        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))
        roster.detach(DEVICE_A, ConnectionId(1))
        assertEquals(null, roster.entries.single().connection)

        val result = roster.admit(DEVICE_A, "Alice's Phone (reconnect)", "1.0.0", AssetPort(8080), ConnectionId(2))

        val reclaimed = result as? RosterAdmission.Reclaimed
            ?: error("expected RosterAdmission.Reclaimed, was $result")
        assertEquals(
            "reclaim without a live connection must report a null previous ConnectionId (FR-021)",
            null,
            reclaimed.previous,
        )
        assertEquals(1, roster.size)
        assertEquals(ConnectionId(2), roster.entries.single().connection)
    }

    @Test(timeout = 30_000)
    fun detachWithAStaleConnectionIdReturnsFalseAndMutatesNothing() {
        val roster = SessionRoster()
        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))
        roster.admit(DEVICE_A, "Alice's Phone (reconnect)", "1.0.0", AssetPort(8080), ConnectionId(2))

        val detached = roster.detach(DEVICE_A, ConnectionId(1))

        assertFalse("a superseded ConnectionId must not detach the replacement (FR-022)", detached)
        assertEquals(1, roster.size)
        assertEquals(
            "the active connection must be unchanged by the stale detach attempt",
            ConnectionId(2),
            roster.entries.single().connection,
        )
    }

    @Test(timeout = 30_000)
    fun releaseRemovesTheDeviceAndFreesItsCapacitySlot() {
        val roster = SessionRoster(capacity = 1)
        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))

        roster.release(DEVICE_A)

        assertEquals("release must drop the entry entirely, freeing its capacity slot", 0, roster.size)
        val result = roster.admit(DEVICE_B, "Bob's Phone", "1.0.0", AssetPort(8081), ConnectionId(2))
        assertTrue(result is RosterAdmission.Admitted)
    }

    @Test(timeout = 30_000)
    fun releaseDisconnectedDropsOnlyEntriesWithNoLiveConnection() {
        val roster = SessionRoster()
        roster.admit(DEVICE_A, "Alice's Phone", "1.0.0", AssetPort(8080), ConnectionId(1))
        roster.admit(DEVICE_B, "Bob's Phone", "1.0.0", AssetPort(8081), ConnectionId(2))
        roster.detach(DEVICE_A, ConnectionId(1))

        roster.releaseDisconnected()

        assertEquals(
            "releaseDisconnected must drop only entries with no live connection",
            listOf(DEVICE_B),
            roster.entries.map { it.deviceId },
        )
    }

    private companion object {
        val DEVICE_A = DeviceId("device-aaaa")
        val DEVICE_B = DeviceId("device-bbbb")
    }
}
