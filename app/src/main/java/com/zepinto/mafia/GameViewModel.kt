package com.zepinto.mafia

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class Stage {
    SETUP,
    /** Role cards, one face-down card at a time. */
    REVEAL,
    /** Night falls: everybody gets ready to pass the phone round. */
    NIGHT_INTRO,
    /** The phone is handed to the next player; nothing secret is on screen. */
    NIGHT_PASS,
    /** The player holding the phone makes their secret night choice. */
    NIGHT_ACTION,
    /** The choice is in (the Detective sees their answer here): hide it and pass on. */
    NIGHT_DONE,
    /** The town wakes up and learns who died. */
    MORNING,
    /** Talking, with the discussion timer. */
    DAY,
    /** The town has voted by pointing: who is out? */
    VOTE,
    VOTE_RESULT,
    GAME_OVER,
}

/** What the Detective learned tonight: [player] is or is not Mafia. */
data class Check(val player: Int, val mafia: Boolean)

data class UiState(
    val lang: Lang,
    val players: List<String>,
    val hasSaved: Boolean,
    /** The number of Mafia chosen on the setup screen, or null for the recommended number. */
    val mafiaChoice: Int? = null,
    val detective: Boolean = true,
    val doctor: Boolean = true,
    val revealRoles: Boolean = true,
    val talkMinutes: Int = 3,
    val musicOn: Boolean = true,
    val stage: Stage = Stage.SETUP,
    val game: Game? = null,

    // role cards
    val position: Int = 0,
    val cardUp: Boolean = false,
    val seen: Set<Int> = emptySet(),

    // the night, going round the table
    val nightOrder: List<Int> = emptyList(),
    val nightPos: Int = 0,
    /** For a Mafia player holding the phone: what their partners have picked so far tonight. */
    val partnerPicks: List<Pair<Int, Int>> = emptyList(),
    val check: Check? = null,

    // the day
    /** When the discussion time runs out (elapsedRealtime, ms), or null without a timer. */
    val timerEnd: Long? = null,
    val voted: Int? = null,
) {
    val n get() = players.size
    val mafiaCount get() = (mafiaChoice ?: Rules.recommendedMafia(n)).coerceIn(1, Rules.maxMafia(n))
    val onLastCard get() = position == n - 1
    val nightPlayer get() = nightOrder.getOrNull(nightPos)
}

/**
 * The whole game on one phone. The night's secret choices are kept here and never put in [UiState]
 * (except what the player holding the phone may see), so nothing secret is on screen while the phone is passed round.
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("mafia", Context.MODE_PRIVATE)
    private val sfx = Sfx(app)
    private val music = Music(app)
    private val picks = ArrayList<Pair<Int, Int>>()
    private val suspects = ArrayList<Pair<Int, Int>>()
    private var protected: Int? = null
    private var checked: Int? = null

    private val _state = MutableStateFlow(
        UiState(
            lang = Lang.fromCode(prefs.getString(KEY_LANG, null)) ?: Lang.deviceDefault(),
            players = prefs.getString(KEY_PLAYERS, "").orEmpty().split('\n').filter { it.isNotBlank() }.distinctBy { it.lowercase() },
            hasSaved = loadSaved() != null,
            mafiaChoice = prefs.getInt(KEY_MAFIA, 0).takeIf { it > 0 },
            detective = prefs.getBoolean(KEY_DETECTIVE, true),
            doctor = prefs.getBoolean(KEY_DOCTOR, true),
            revealRoles = prefs.getBoolean(KEY_REVEAL, true),
            talkMinutes = prefs.getInt(KEY_TALK, 3).takeIf { it in Rules.TALK_OPTIONS } ?: 3,
            musicOn = prefs.getBoolean(KEY_MUSIC, true),
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        music.setEnabled(_state.value.musicOn)
        // the music follows the game: the lobby tune, the night, the day, and silence for the final sting
        viewModelScope.launch {
            _state.map { trackFor(it.stage) }.distinctUntilChanged().collect { music.play(it) }
        }
    }

    private fun trackFor(stage: Stage): Music.Track? = when (stage) {
        Stage.SETUP, Stage.REVEAL -> Music.Track.LOBBY
        Stage.NIGHT_INTRO, Stage.NIGHT_PASS, Stage.NIGHT_ACTION, Stage.NIGHT_DONE -> Music.Track.NIGHT
        Stage.MORNING, Stage.DAY, Stage.VOTE, Stage.VOTE_RESULT -> Music.Track.DAY
        Stage.GAME_OVER -> null
    }

    fun setMusic(on: Boolean) {
        _state.update { it.copy(musicOn = on) }
        music.setEnabled(on)
        prefs.edit().putBoolean(KEY_MUSIC, on).apply()
    }

    // ---- setup ----

    fun setLanguage(lang: Lang) {
        _state.update { it.copy(lang = lang) }
        prefs.edit().putString(KEY_LANG, lang.code).apply()
    }

    fun addPlayer(raw: String): Boolean {
        val name = raw.trim().take(MAX_NAME_LENGTH)
        val s = _state.value
        if (name.isEmpty() || s.players.size >= Rules.MAX_PLAYERS || s.players.any { it.equals(name, ignoreCase = true) }) return false
        _state.update { it.copy(players = it.players + name) }
        savePlayers()
        return true
    }

    fun removePlayer(index: Int) {
        _state.update { it.copy(players = it.players.filterIndexed { i, _ -> i != index }) }
        savePlayers()
    }

    /** One Mafia more or fewer than now. Going back to the recommended number forgets the choice. */
    fun changeMafia(delta: Int) {
        val s = _state.value
        val m = (s.mafiaCount + delta).coerceIn(1, Rules.maxMafia(s.n))
        val choice = m.takeIf { it != Rules.recommendedMafia(s.n) }
        _state.update { it.copy(mafiaChoice = choice) }
        prefs.edit().putInt(KEY_MAFIA, choice ?: 0).apply()
    }

    fun setDetective(on: Boolean) {
        _state.update { it.copy(detective = on) }
        prefs.edit().putBoolean(KEY_DETECTIVE, on).apply()
    }

    fun setDoctor(on: Boolean) {
        _state.update { it.copy(doctor = on) }
        prefs.edit().putBoolean(KEY_DOCTOR, on).apply()
    }

    fun setRevealRoles(on: Boolean) {
        _state.update { it.copy(revealRoles = on) }
        prefs.edit().putBoolean(KEY_REVEAL, on).apply()
    }

    fun setTalk(minutes: Int) {
        if (minutes !in Rules.TALK_OPTIONS) return
        _state.update { it.copy(talkMinutes = minutes) }
        prefs.edit().putInt(KEY_TALK, minutes).apply()
    }

    fun startGame() {
        val s = _state.value
        if (s.n !in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS) return
        val game = Rules.newGame(s.players, Setup(s.mafiaCount, s.detective, s.doctor, s.revealRoles, s.talkMinutes))
        save(game)
        _state.update { it.copy(stage = Stage.REVEAL, game = game, hasSaved = true, position = 0, cardUp = false, seen = emptySet()) }
    }

    /** Back into a saved game: the deal starts again from the first card, a night from its first player. */
    fun resume() {
        val game = loadSaved() ?: return
        _state.update { it.copy(game = game, players = game.players, hasSaved = true, position = 0, cardUp = false, seen = emptySet()) }
        when (game.phase) {
            Phase.DEAL -> _state.update { it.copy(stage = Stage.REVEAL) }
            Phase.NIGHT -> enterNightIntro()
            Phase.DAY -> enterDay()
        }
    }

    // ---- role cards, dealt round the table ----

    /** Tap on the card: flip it up, or back down. The first flip chimes, flipping a seen card again buzzes. */
    fun flip() {
        val s = _state.value
        if (s.stage != Stage.REVEAL) return
        if (s.cardUp) {
            _state.update { it.copy(cardUp = false) }
        } else {
            sfx.play(if (s.position in s.seen) Cue.WARN else Cue.FLIP)
            _state.update { it.copy(cardUp = true, seen = it.seen + it.position) }
        }
    }

    fun hide() {
        if (_state.value.cardUp) _state.update { it.copy(cardUp = false) }
    }

    /** Pass the phone on, once this card has been seen and turned back over. After the last card, night falls. */
    fun next() {
        val s = _state.value
        val game = s.game ?: return
        if (s.stage != Stage.REVEAL || s.cardUp || s.position !in s.seen) return
        if (s.onLastCard) {
            val g = Rules.dealt(game)
            save(g)
            _state.update { it.copy(game = g) }
            enterNightIntro()
        } else {
            _state.update { it.copy(position = it.position + 1) }
        }
    }

    // ---- the night ----

    fun beginNight() {
        val game = _state.value.game ?: return
        if (_state.value.stage != Stage.NIGHT_INTRO) return
        picks.clear(); suspects.clear(); protected = null; checked = null
        _state.update { it.copy(stage = Stage.NIGHT_PASS, nightOrder = Rules.nightOrder(game), nightPos = 0, partnerPicks = emptyList(), check = null) }
    }

    /** The person holding the phone is the right one: show them their night screen. */
    fun ready() {
        val s = _state.value
        val game = s.game ?: return
        val player = s.nightPlayer ?: return
        if (s.stage != Stage.NIGHT_PASS) return
        val partners = if (game.roles[player] == Role.MAFIA) picks.toList() else emptyList()
        _state.update { it.copy(stage = Stage.NIGHT_ACTION, partnerPicks = partners) }
    }

    /** The night choice of the player holding the phone. Every role makes one, so nobody can tell roles apart by watching. */
    fun choose(target: Int) {
        val s = _state.value
        val game = s.game ?: return
        val player = s.nightPlayer ?: return
        if (s.stage != Stage.NIGHT_ACTION || target !in Rules.choices(game, player)) return
        var check: Check? = null
        when (game.roles[player]) {
            Role.MAFIA -> picks += player to target
            Role.DOCTOR -> protected = target
            Role.DETECTIVE -> {
                checked = target
                check = Check(target, game.roles[target] == Role.MAFIA)
            }
            Role.VILLAGER -> suspects += player to target
        }
        sfx.play(Cue.BLIP)
        _state.update { it.copy(stage = Stage.NIGHT_DONE, partnerPicks = emptyList(), check = check) }
    }

    /** Hide the screen and hand the phone to the next player. After the last one, morning comes. */
    fun passOn() {
        val s = _state.value
        val game = s.game ?: return
        if (s.stage != Stage.NIGHT_DONE) return
        val pos = s.nightPos + 1
        if (pos < s.nightOrder.size) {
            _state.update { it.copy(stage = Stage.NIGHT_PASS, nightPos = pos, check = null) }
            return
        }
        val g = Rules.resolveNight(game, picks.toList(), protected, checked, suspects.toList())
        picks.clear(); suspects.clear(); protected = null; checked = null
        if (g.over) clearSaved() else save(g)
        sfx.play(Cue.DAWN)
        _state.update { it.copy(game = g, stage = Stage.MORNING, check = null, nightPos = pos, hasSaved = !g.over) }
    }

    /** The town is told what happened in the night. */
    fun revealMorning() {
        val night = _state.value.game?.lastNight ?: return
        sfx.play(if (night.victim != null) Cue.DEATH else Cue.SAVED)
    }

    fun afterMorning() {
        val game = _state.value.game ?: return
        if (_state.value.stage != Stage.MORNING) return
        if (game.over) enterGameOver() else enterDay()
    }

    // ---- the day ----

    fun addMinute() {
        val end = _state.value.timerEnd ?: return
        _state.update { it.copy(timerEnd = maxOf(end, SystemClock.elapsedRealtime()) + 60_000) }
    }

    fun timeUp() = sfx.play(Cue.BELL)

    fun startVote() {
        if (_state.value.stage == Stage.DAY) _state.update { it.copy(stage = Stage.VOTE) }
    }

    fun cancelVote() {
        if (_state.value.stage == Stage.VOTE) _state.update { it.copy(stage = Stage.DAY) }
    }

    /** The town has pointed: [who] is out, or nobody (null) after a tie. */
    fun eliminate(who: Int?) {
        val s = _state.value
        val game = s.game ?: return
        if (s.stage != Stage.VOTE) return
        val g = Rules.eliminate(game, who)
        if (g === game) return
        if (g.over) clearSaved() else save(g)
        sfx.play(if (who != null) Cue.OUT else Cue.BLIP)
        _state.update { it.copy(game = g, stage = Stage.VOTE_RESULT, voted = who, hasSaved = !g.over) }
    }

    fun afterVote() {
        val game = _state.value.game ?: return
        if (_state.value.stage != Stage.VOTE_RESULT) return
        if (game.over) enterGameOver() else enterNightIntro()
    }

    // ---- the end ----

    fun newGame() = startGame()

    fun backToSetup() {
        clearSaved()
        _state.update { it.copy(stage = Stage.SETUP, game = null, hasSaved = false, cardUp = false, check = null, partnerPicks = emptyList()) }
    }

    /** The app left the foreground: nothing secret may stay on screen. */
    fun onPause() {
        music.pause()
        when (_state.value.stage) {
            Stage.REVEAL -> hide()
            Stage.NIGHT_ACTION -> _state.update { it.copy(stage = Stage.NIGHT_PASS, partnerPicks = emptyList()) }
            Stage.NIGHT_DONE -> passOn()
            else -> Unit
        }
    }

    fun onResume() = music.resume()

    // ---- helpers ----

    private fun enterNightIntro() {
        sfx.play(Cue.NIGHT)
        _state.update { it.copy(stage = Stage.NIGHT_INTRO, check = null, partnerPicks = emptyList(), voted = null) }
    }

    private fun enterDay() {
        val game = _state.value.game ?: return
        val end = if (game.talkMinutes > 0) SystemClock.elapsedRealtime() + game.talkMinutes * 60_000L else null
        _state.update { it.copy(stage = Stage.DAY, timerEnd = end) }
    }

    private fun enterGameOver() {
        val game = _state.value.game ?: return
        clearSaved()
        sfx.play(if (game.winner == Side.TOWN) Cue.TOWN_WINS else Cue.MAFIA_WINS)
        _state.update { it.copy(stage = Stage.GAME_OVER, hasSaved = false) }
    }

    private fun savePlayers() {
        prefs.edit().putString(KEY_PLAYERS, _state.value.players.joinToString("\n")).apply()
    }

    private fun save(g: Game) {
        prefs.edit().putString(KEY_GAME, GameCodec.save(g)).apply()
    }

    private fun clearSaved() {
        prefs.edit().remove(KEY_GAME).apply()
    }

    private fun loadSaved(): Game? = prefs.getString(KEY_GAME, null)?.let { GameCodec.load(it) }?.takeIf { !it.over }

    override fun onCleared() {
        sfx.release()
        music.release()
    }

    companion object {
        const val MAX_NAME_LENGTH = 20
        private const val KEY_LANG = "lang"
        private const val KEY_PLAYERS = "players"
        private const val KEY_GAME = "game"
        private const val KEY_MAFIA = "mafia"
        private const val KEY_DETECTIVE = "detective"
        private const val KEY_DOCTOR = "doctor"
        private const val KEY_REVEAL = "reveal"
        private const val KEY_TALK = "talk"
        private const val KEY_MUSIC = "music"
    }
}
