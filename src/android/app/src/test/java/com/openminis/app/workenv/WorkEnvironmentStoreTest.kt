package com.openminis.app.workenv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkEnvironmentStoreTest {
    @Test
    fun parseHostPortWithExplicitPort() {
        val parsed = WorkEnvironmentStore.parseHostPort("192.168.1.8:18765")
        assertEquals("192.168.1.8", parsed?.first)
        assertEquals(18765, parsed?.second)
    }

    @Test
    fun parseHostPortDefaultPort() {
        val parsed = WorkEnvironmentStore.parseHostPort("office.local")
        assertEquals("office.local", parsed?.first)
        assertEquals(WorkEnvironmentDefaults.DefaultPort, parsed?.second)
    }

    @Test
    fun parseHostPortRejectsEmpty() {
        assertNull(WorkEnvironmentStore.parseHostPort("   "))
        assertNull(WorkEnvironmentStore.parseHostPort("nohost"))
    }

    @Test
    fun parseHostPortRejectsBadPort() {
        assertNull(WorkEnvironmentStore.parseHostPort("host:99999"))
        assertNull(WorkEnvironmentStore.parseHostPort("host:abc"))
    }
}
