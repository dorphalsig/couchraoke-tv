package com.couchraoke.tv.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionRegistryTest {

    @Test
    fun `given first registered client, when register, then connectionId == 1u`() {
        val registry = ConnectionRegistry()
        val id = registry.register("client1", "Phone 1", 8080, "192.168.1.1")
        assertEquals(1u.toUShort(), id)
    }

    @Test
    fun `given second registered client, when register, then connectionId == 2u`() {
        val registry = ConnectionRegistry()
        registry.register("client1", "Phone 1", 8080, "192.168.1.1")
        val id = registry.register("client2", "Phone 2", 8080, "192.168.1.2")
        assertEquals(2u.toUShort(), id)
    }

    @Test
    fun `given re-registering same clientId (reconnect), when register, then new incremented id assigned`() {
        val registry = ConnectionRegistry()
        val id1 = registry.register("client1", "Phone 1", 8080, "192.168.1.1")
        val id2 = registry.register("client1", "Phone 1 (reconnect)", 8080, "192.168.1.1")
        assertEquals(1u.toUShort(), id1)
        assertEquals(2u.toUShort(), id2)
    }

    @Test
    fun `given client registered, when deregister, then getByClientId returns null`() {
        val registry = ConnectionRegistry()
        registry.register("client1", "Phone 1", 8080, "192.168.1.1")
        registry.deregister("client1")
        assertNull(registry.getByClientId("client1"))
    }

    @Test
    fun `given client registered, when getByConnectionId, then finds the correct connection by id`() {
        val registry = ConnectionRegistry()
        val id = registry.register("client1", "Phone 1", 8080, "192.168.1.1")
        val connection = registry.getByConnectionId(id)
        assertEquals("client1", connection?.clientId)
        assertEquals(id, connection?.connectionId)
    }
}
