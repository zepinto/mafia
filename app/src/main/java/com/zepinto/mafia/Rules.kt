package com.zepinto.mafia

import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

enum class Role { VILLAGER, MAFIA, DETECTIVE, DOCTOR }
enum class Side { TOWN, MAFIA }

/** Where the game stands: dealing the cards, a night going round the table, or a day of talking and voting. */
enum class Phase { DEAL, NIGHT, DAY }

/** Choices made on the setup screen. */
data class Setup(
    val mafia: Int,
    val detective: Boolean = true,
    val doctor: Boolean = true,
    /** Show the role of a player when they die or are voted out. */
    val revealRoles: Boolean = true,
    /** Length of the day's discussion timer, 0 for no timer. */
    val talkMinutes: Int = 3,
)

/** One night: who the Mafia chose, who the Doctor protected, who the Detective checked, and who died. */
data class NightLog(
    val night: Int,
    /** (Mafia member, their pick), in the order they chose. */
    val picks: List<Pair<Int, Int>>,
    val target: Int?,
    val protected: Int?,
    val checked: Int?,
    /** The target, unless the Doctor protected them. */
    val victim: Int?,
    /** (villager, the player they suspect): the villagers' night task, shown in the story at the end. */
    val suspects: List<Pair<Int, Int>> = emptyList(),
)

/** One day: who the town voted out, or null when nobody was (a tie, or no agreement). */
data class DayLog(val day: Int, val eliminated: Int?)

data class Game(
    val players: List<String>,
    val roles: List<Role>,
    val alive: List<Boolean>,
    /** Number of the current night and day, from 1. Night 1 comes before day 1. */
    val round: Int,
    val phase: Phase,
    val revealRoles: Boolean = true,
    val talkMinutes: Int = 3,
    val nights: List<NightLog> = emptyList(),
    val days: List<DayLog> = emptyList(),
    val winner: Side? = null,
) {
    val n get() = players.size
    val over get() = winner != null
    val mafia get() = roles.indices.filter { roles[it] == Role.MAFIA }
    val living get() = players.indices.filter { alive[it] }
    val lastNight get() = nights.lastOrNull()
    /** The Doctor may not protect the same player two nights running. */
    val lastProtected get() = nights.lastOrNull()?.protected
    fun hasRole(role: Role) = roles.indices.any { roles[it] == role }
    fun isAlive(role: Role) = roles.indices.any { roles[it] == role && alive[it] }
}

object Rules {
    const val MIN_PLAYERS = 5
    const val MAX_PLAYERS = 16
    val TALK_OPTIONS = listOf(2, 3, 5, 0)

    fun side(role: Role) = if (role == Role.MAFIA) Side.MAFIA else Side.TOWN

    /** About one player in four is Mafia: 5–6 players 1, 7–9 players 2, 10–12 players 3, 13–15 players 4, 16 players 5. */
    fun recommendedMafia(players: Int) = ((players - 1) / 3).coerceAtLeast(1)

    /** The Mafia must start as a minority, or they would have won already. */
    fun maxMafia(players: Int) = ((players - 1) / 2).coerceAtLeast(1)

    fun newGame(players: List<String>, setup: Setup, rng: Random = Random.Default): Game {
        require(players.size in MIN_PLAYERS..MAX_PLAYERS) { "the game is for $MIN_PLAYERS to $MAX_PLAYERS players" }
        require(setup.mafia in 1..maxMafia(players.size)) { "the Mafia must be a minority" }
        val specials = listOfNotNull(Role.DETECTIVE.takeIf { setup.detective }, Role.DOCTOR.takeIf { setup.doctor })
        val roles = (List(setup.mafia) { Role.MAFIA } + specials).let { it + List(players.size - it.size) { Role.VILLAGER } }.shuffled(rng)
        return Game(
            players = players,
            roles = roles,
            alive = List(players.size) { true },
            round = 1,
            phase = Phase.DEAL,
            revealRoles = setup.revealRoles,
            talkMinutes = setup.talkMinutes,
        )
    }

    /** Everybody has seen their card: the first night begins. */
    fun dealt(g: Game) = if (g.phase == Phase.DEAL) g.copy(phase = Phase.NIGHT) else g

    /**
     * Who takes the phone at night, and in what order: every living player, so that holding the phone says
     * nothing about your role. The order goes round the table and starts one seat further on every night.
     */
    fun nightOrder(g: Game): List<Int> = List(g.n) { (g.round - 1 + it) % g.n }.filter { g.alive[it] }

    /** The players [player] may choose tonight. */
    fun choices(g: Game, player: Int): List<Int> = g.living.filter { p ->
        when (g.roles[player]) {
            Role.MAFIA -> g.roles[p] != Role.MAFIA
            Role.DOCTOR -> p != g.lastProtected
            Role.DETECTIVE, Role.VILLAGER -> p != player
        }
    }

    /** The Mafia's victim: the player picked most often; a tie goes to the one picked first. */
    fun mafiaTarget(picks: List<Pair<Int, Int>>): Int? {
        if (picks.isEmpty()) return null
        val counts = picks.groupingBy { it.second }.eachCount()
        val top = counts.values.max()
        return picks.first { counts[it.second] == top }.second
    }

    /**
     * The night is over. The Mafia's target dies unless the Doctor protected them. The day begins,
     * or the game ends if the Mafia now match the town.
     */
    fun resolveNight(g: Game, picks: List<Pair<Int, Int>>, protected: Int?, checked: Int?, suspects: List<Pair<Int, Int>> = emptyList()): Game {
        if (g.over || g.phase != Phase.NIGHT) return g
        val target = mafiaTarget(picks)
        val victim = target?.takeIf { it != protected }
        val alive = g.alive.toMutableList().also { a -> victim?.let { a[it] = false } }
        val next = g.copy(
            alive = alive,
            phase = Phase.DAY,
            nights = g.nights + NightLog(g.round, picks, target, protected, checked, victim, suspects),
        )
        return next.copy(winner = winner(next))
    }

    /** The town has voted [who] out (null: nobody). Night falls again, or the game ends. */
    fun eliminate(g: Game, who: Int?): Game {
        if (g.over || g.phase != Phase.DAY) return g
        if (who != null && (who !in 0 until g.n || !g.alive[who])) return g
        val alive = g.alive.toMutableList().also { a -> who?.let { a[it] = false } }
        val next = g.copy(alive = alive, days = g.days + DayLog(g.round, who))
        val win = winner(next)
        return if (win != null) next.copy(winner = win) else next.copy(phase = Phase.NIGHT, round = g.round + 1)
    }

    /** The town wins when no Mafia are left; the Mafia win as soon as they are as many as everybody else. */
    fun winner(g: Game): Side? {
        val mafia = g.living.count { g.roles[it] == Role.MAFIA }
        val town = g.living.size - mafia
        return when {
            mafia == 0 -> Side.TOWN
            mafia >= town -> Side.MAFIA
            else -> null
        }
    }
}

/** Saving a game in progress, so that closing the app does not lose it. */
object GameCodec {
    private fun pairs(l: List<Pair<Int, Int>>) = JSONArray(l.map { JSONArray(listOf(it.first, it.second)) })
    private fun parsePairs(a: JSONArray) = List(a.length()) { a.getJSONArray(it).let { p -> p.getInt(0) to p.getInt(1) } }
    private fun opt(v: Int?) = v ?: -1
    private fun JSONObject.optIndex(key: String) = getInt(key).takeIf { it >= 0 }

    fun save(g: Game): String = JSONObject()
        .put("players", JSONArray(g.players))
        .put("roles", JSONArray(g.roles.map { it.ordinal }))
        .put("alive", JSONArray(g.alive))
        .put("round", g.round).put("phase", g.phase.ordinal)
        .put("reveal", g.revealRoles).put("talk", g.talkMinutes)
        .put("winner", g.winner?.ordinal ?: -1)
        .put(
            "nights",
            JSONArray(
                g.nights.map {
                    JSONObject().put("n", it.night).put("picks", pairs(it.picks)).put("target", opt(it.target)).put("saved", opt(it.protected))
                        .put("checked", opt(it.checked)).put("victim", opt(it.victim)).put("suspects", pairs(it.suspects))
                }
            )
        )
        .put("days", JSONArray(g.days.map { JSONObject().put("n", it.day).put("out", opt(it.eliminated)) }))
        .toString()

    fun load(text: String): Game? = try {
        val o = JSONObject(text)
        val players = o.getJSONArray("players").let { a -> List(a.length()) { a.getString(it) } }
        val roles = o.getJSONArray("roles").let { a -> List(a.length()) { Role.entries[a.getInt(it)] } }
        val alive = o.getJSONArray("alive").let { a -> List(a.length()) { a.getBoolean(it) } }
        require(players.size in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS && roles.size == players.size && alive.size == players.size)
        val seats = players.indices
        val nights = o.getJSONArray("nights").let { a ->
            List(a.length()) {
                val r = a.getJSONObject(it)
                NightLog(
                    night = r.getInt("n"), picks = parsePairs(r.getJSONArray("picks")), target = r.optIndex("target"),
                    protected = r.optIndex("saved"), checked = r.optIndex("checked"), victim = r.optIndex("victim"),
                    suspects = if (r.has("suspects")) parsePairs(r.getJSONArray("suspects")) else emptyList(),
                )
            }
        }
        val days = o.getJSONArray("days").let { a -> List(a.length()) { a.getJSONObject(it).let { d -> DayLog(d.getInt("n"), d.optIndex("out")) } } }
        nights.forEach { n ->
            require((n.picks + n.suspects).all { it.first in seats && it.second in seats })
            require(listOfNotNull(n.target, n.protected, n.checked, n.victim).all { it in seats })
        }
        require(days.all { d -> d.eliminated == null || d.eliminated in seats })
        Game(
            players = players,
            roles = roles,
            alive = alive,
            round = o.getInt("round").also { require(it >= 1) },
            phase = Phase.entries[o.getInt("phase")],
            revealRoles = o.optBoolean("reveal", true),
            talkMinutes = o.optInt("talk", 3),
            nights = nights,
            days = days,
            winner = o.getInt("winner").takeIf { it >= 0 }?.let { Side.entries[it] },
        )
    } catch (_: Exception) {
        null
    }
}
