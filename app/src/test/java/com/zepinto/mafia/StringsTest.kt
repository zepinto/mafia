package com.zepinto.mafia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StringsTest {
    @Test
    fun everyLanguageIsComplete() {
        for (lang in Lang.entries) {
            val t = lang.strings
            assertEquals(7, t.rulesSections.size)
            t.rulesSections.forEach { assertTrue(it.title.isNotBlank() && it.lines.isNotEmpty()) }
            for (f in listOf(t.passTo, t.passPhoneTo, t.tapToFlip, t.imName, t.hideAndPass, t.killedLine, t.confirmOut, t.outLine, t.checkIsMafia, t.checkNotMafia)) {
                assertTrue(f("Ana").contains("Ana"))
            }
            assertTrue(t.partnerPicked("Ana", "Rui").let { it.contains("Ana") && it.contains("Rui") })
            assertTrue(t.fellowMafia("Ana, Rui").contains("Ana, Rui"))
            assertTrue(t.nightTitle(3).contains("3") && t.dayTitle(3).contains("3"))
            assertEquals(Role.entries.size, Role.entries.map { t.roleName(it) }.toSet().size)
            Role.entries.forEach { assertTrue(t.actionQuestion(it).isNotBlank() && t.roleHint(it).isNotBlank()) }
            assertNotEquals(t.townWins, t.mafiaWins)
            assertNotEquals(t.checkIsMafia("A"), t.checkNotMafia("A"))
        }
        assertEquals(Lang.PT, Lang.fromCode("pt"))
        assertNull(Lang.fromCode("fr"))
    }
}
