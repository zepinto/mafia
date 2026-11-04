package com.zepinto.mafia

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

private fun names(n: Int) = (1..n).map { "P$it" }

/** A 7-player game with known roles: P1 and P2 Mafia, P3 Detective, P4 Doctor, the rest Villagers, at night 1. */
private fun game(): Game {
    val g = Rules.dealt(Rules.newGame(names(7), Setup(mafia = 2), Random(1)))
    return g.copy(roles = listOf(Role.MAFIA, Role.MAFIA, Role.DETECTIVE, Role.DOCTOR, Role.VILLAGER, Role.VILLAGER, Role.VILLAGER))
}

class SetupTest {
    @Test
    fun aboutOnePlayerInFourIsMafia() {
        assertEquals(listOf(1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5), (5..16).map { Rules.recommendedMafia(it) })
    }

    @Test
    fun theMafiaAlwaysStartsAsAMinority() {
        for (n in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS) assertTrue(Rules.maxMafia(n) * 2 < n)
    }

    @Test
    fun aNewGameDealsTheChosenRoles() {
        for (n in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS) repeat(10) { seed ->
            val m = Rules.recommendedMafia(n)
            val g = Rules.newGame(names(n), Setup(mafia = m), Random(seed))
            assertEquals(m, g.roles.count { it == Role.MAFIA })
            assertEquals(1, g.roles.count { it == Role.DETECTIVE })
            assertEquals(1, g.roles.count { it == Role.DOCTOR })
            assertEquals(n - m - 2, g.roles.count { it == Role.VILLAGER })
            assertEquals(Phase.DEAL, g.phase)
            assertTrue(g.alive.all { it })
        }
        val plain = Rules.newGame(names(5), Setup(mafia = 1, detective = false, doctor = false))
        assertEquals(4, plain.roles.count { it == Role.VILLAGER })
    }

    @Test(expected = IllegalArgumentException::class)
    fun tooManyMafiaIsRefused() {
        Rules.newGame(names(6), Setup(mafia = 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun tooFewPlayersIsRefused() {
        Rules.newGame(names(4), Setup(mafia = 1))
    }
}

class NightTest {
    @Test
    fun everyLivingPlayerTakesThePhoneStartingOneSeatLaterEachNight() {
        val g = game()
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), Rules.nightOrder(g))
        val later = g.copy(round = 3, alive = g.alive.toMutableList().also { it[3] = false })
        assertEquals(listOf(2, 4, 5, 6, 0, 1), Rules.nightOrder(later))
    }

    @Test
    fun eachRoleChoosesAmongTheRightPlayers() {
        val g = game()
        assertEquals(listOf(2, 3, 4, 5, 6), Rules.choices(g, 0))
        assertEquals(listOf(0, 1, 3, 4, 5, 6), Rules.choices(g, 2))
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), Rules.choices(g, 3))
        assertEquals(listOf(0, 1, 2, 3, 5, 6), Rules.choices(g, 4))
    }

    @Test
    fun theDoctorCannotProtectTheSamePlayerTwoNightsRunning() {
        val g = Rules.eliminate(Rules.resolveNight(game(), listOf(0 to 5, 1 to 5), protected = 3, checked = 0), null)
        assertEquals(3, g.lastProtected)
        assertFalse(3 in Rules.choices(g, 3))
        assertTrue(4 in Rules.choices(g, 3))
    }

    @Test
    fun theMostPickedPlayerIsTheVictimAndATieGoesToTheFirstPick() {
        assertEquals(4, Rules.mafiaTarget(listOf(0 to 4, 1 to 4)))
        assertEquals(5, Rules.mafiaTarget(listOf(0 to 5, 1 to 4)))
        assertEquals(6, Rules.mafiaTarget(listOf(0 to 4, 1 to 6, 2 to 6)))
        assertNull(Rules.mafiaTarget(emptyList()))
    }

    @Test
    fun theTargetDiesUnlessTheDoctorProtectedThem() {
        val killed = Rules.resolveNight(game(), listOf(0 to 5, 1 to 5), protected = 3, checked = 0)
        assertEquals(5, killed.lastNight?.victim)
        assertFalse(killed.alive[5])
        assertEquals(Phase.DAY, killed.phase)

        val saved = Rules.resolveNight(game(), listOf(0 to 5, 1 to 5), protected = 5, checked = 0)
        assertNull(saved.lastNight?.victim)
        assertEquals(5, saved.lastNight?.target)
        assertTrue(saved.alive.all { it })
    }

    @Test
    fun theNightIsRecordedForTheStory() {
        val g = Rules.resolveNight(game(), listOf(0 to 5, 1 to 4), protected = 2, checked = 1, suspects = listOf(4 to 1, 5 to 0, 6 to 1))
        val n = g.lastNight!!
        assertEquals(1, n.night)
        assertEquals(5, n.target)
        assertEquals(2, n.protected)
        assertEquals(1, n.checked)
        assertEquals(3, n.suspects.size)
    }
}

class DayTest {
    @Test
    fun votingSomebodyOutStartsTheNextNight() {
        val day = Rules.resolveNight(game(), listOf(0 to 5, 1 to 5), protected = 3, checked = 0)
        val g = Rules.eliminate(day, 6)
        assertFalse(g.alive[6])
        assertEquals(Phase.NIGHT, g.phase)
        assertEquals(2, g.round)
        assertEquals(DayLog(1, 6), g.days.last())
    }

    @Test
    fun aTieMeansNobodyIsOut() {
        val day = Rules.resolveNight(game(), listOf(0 to 5, 1 to 5), protected = 5, checked = 0)
        val g = Rules.eliminate(day, null)
        assertTrue(g.alive.all { it })
        assertNull(g.days.last().eliminated)
    }

    @Test
    fun theDeadCannotBeVotedOutAgain() {
        val day = Rules.resolveNight(game(), listOf(0 to 5, 1 to 5), protected = 3, checked = 0)
        assertSame(day, Rules.eliminate(day, 5))
    }
}

class WinTest {
    @Test
    fun theTownWinsWhenTheLastMafiaIsOut() {
        var g = Rules.resolveNight(game(), listOf(0 to 6, 1 to 6), protected = 6, checked = 0)
        g = Rules.eliminate(g, 0)
        assertNull(g.winner)
        g = Rules.resolveNight(g, listOf(1 to 6), protected = 5, checked = 1)
        g = Rules.eliminate(g, 1)
        assertEquals(Side.TOWN, g.winner)
        assertTrue(g.over)
    }

    @Test
    fun theMafiaWinsWhenTheyMatchTheTown() {
        // 2 Mafia against 5: kill one each night, the town votes out a Villager each day
        var g = game()
        g = Rules.eliminate(Rules.resolveNight(g, listOf(0 to 6, 1 to 6), null, null), 5) // 2 vs 3
        assertNull(g.winner)
        g = Rules.resolveNight(g, listOf(0 to 4, 1 to 4), null, null) // 2 vs 2
        assertEquals(Side.MAFIA, g.winner)
        assertEquals(Phase.DAY, g.phase)
    }

    @Test
    fun nothingMovesOnceTheGameIsOver() {
        var g = game()
        g = Rules.eliminate(Rules.resolveNight(g, listOf(0 to 6, 1 to 6), null, null), 5)
        g = Rules.resolveNight(g, listOf(0 to 4, 1 to 4), null, null)
        assertSame(g, Rules.eliminate(g, 2))
    }
}

class CodecTest {
    @Test
    fun aSavedGameLoadsBackTheSame() {
        var g = game().copy(revealRoles = false, talkMinutes = 5)
        g = Rules.resolveNight(g, listOf(0 to 5, 1 to 4), protected = 4, checked = 1, suspects = listOf(6 to 0))
        g = Rules.eliminate(g, null)
        val back = GameCodec.load(GameCodec.save(g))
        assertNotNull(back)
        assertEquals(g, back)
    }

    @Test
    fun brokenTextLoadsAsNothing() {
        assertNull(GameCodec.load("not json"))
        assertNull(GameCodec.load("{\"players\":[\"A\"],\"roles\":[0],\"alive\":[true]}"))
    }
}
