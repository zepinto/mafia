package com.zepinto.mafia

import android.os.SystemClock
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** A full-screen column with the night or the day behind it. */
@Composable
private fun Page(day: Boolean = false, center: Boolean = false, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .then(if (day) Modifier.dayBackground() else Modifier.nightBackground())
            .statusBarsPadding()
            .navigationBarsPadding()
            .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = if (center) Arrangement.Center else Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/** A player in a list: avatar, name and an optional tag; tappable when [onClick] is given. */
@Composable
private fun PlayerRow(
    game: Game,
    index: Int,
    selected: Boolean = false,
    enabled: Boolean = true,
    tag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val dead = !game.alive[index]
    Surface(
        shape = Soft,
        color = when {
            selected -> Palette.Moon.copy(alpha = 0.22f)
            else -> Color.White.copy(alpha = if (enabled) 0.08f else 0.03f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (selected) Palette.Moon else Color.Transparent, Soft)
            .clip(Soft)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(game.players[index], index, size = 38.dp, dead = dead)
            Spacer(Modifier.width(12.dp))
            Text(
                game.players[index],
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = if (enabled && !dead) 1f else 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            tag?.let { Text(it, fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f)) }
            if (selected) Text("  ✅", fontSize = 20.sp)
        }
    }
}

// ---------------------------------------------------------------- the night

@Composable
internal fun NightIntroScreen(state: UiState, onBegin: () -> Unit, onRules: () -> Unit) {
    val t = LocalStrings.current
    val game = state.game ?: return
    Page(center = true) {
        Text("🌙", fontSize = 84.sp)
        Text(t.nightTitle(game.round), fontSize = 40.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        t.nightIntro.forEach {
            Text(it, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
        }
        Spacer(Modifier.height(28.dp))
        PillButton(t.passRound, onBegin, Modifier.fillMaxWidth(), color = Palette.Moon)
    }
}

/** Nothing secret on screen: just whose turn it is to take the phone. */
@Composable
internal fun NightPassScreen(state: UiState, onReady: () -> Unit) {
    val t = LocalStrings.current
    val game = state.game ?: return
    val player = state.nightPlayer ?: return
    val name = game.players[player]
    // a fast double tap on the previous screen's button must not skip straight past this hand-over
    val shownAt = remember(state.stage, state.nightPos) { SystemClock.uptimeMillis() }
    Page(center = true) {
        Chip("🌙 ${t.nightTitle(game.round)}", Palette.Moon)
        Spacer(Modifier.height(28.dp))
        Avatar(name, player, size = 110.dp)
        Spacer(Modifier.height(18.dp))
        Text(t.passPhoneTo(name), fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(t.progressOf(state.nightPos + 1, state.nightOrder.size), color = Palette.Lilac, fontSize = 16.sp)
        Spacer(Modifier.height(30.dp))
        PillButton(t.imName(name), { if (SystemClock.uptimeMillis() - shownAt > 500) onReady() }, Modifier.fillMaxWidth())
    }
}

/** The secret night choice. Every role sees the same kind of screen: a question, the living players and a confirm button. */
@Composable
internal fun NightActionScreen(state: UiState, onChoose: (Int) -> Unit) {
    val t = LocalStrings.current
    val game = state.game ?: return
    val player = state.nightPlayer ?: return
    val role = game.roles[player]
    val choices = Rules.choices(game, player)
    var picked by rememberSaveable(state.nightPos, game.round) { mutableIntStateOf(-1) }
    Page {
        Chip("🌙 ${t.nightTitle(game.round)}", Palette.Moon)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(game.players[player], player, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Text(game.players[player], fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Chip("${roleEmoji(role)} ${t.roleName(role)}", Palette.role(role))
        }
        Text(t.actionQuestion(role), fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(t.actionHint(role), color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
        if (role == Role.MAFIA && game.mafia.count { game.alive[it] } > 1) {
            Surface(shape = Soft, color = Palette.Blood.copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.partnerPicks.isEmpty()) Text(t.noPicksYet, fontSize = 14.sp)
                    state.partnerPicks.forEach { (who, target) ->
                        Text("🎩 ${t.partnerPicked(game.players[who], game.players[target])}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
        game.living.forEach { p ->
            val tag = when {
                role == Role.MAFIA && game.roles[p] == Role.MAFIA -> "🎩"
                role == Role.DOCTOR && p == game.lastProtected -> t.protectedLastNight
                else -> null
            }
            PlayerRow(game, p, selected = picked == p, enabled = p in choices, tag = tag) { picked = p }
        }
        Spacer(Modifier.height(4.dp))
        PillButton(t.confirm, { onChoose(picked) }, Modifier.fillMaxWidth(), enabled = picked in choices)
    }
}

/** The choice is in. The Detective reads their answer here; everybody else just hides the screen. */
@Composable
internal fun NightDoneScreen(state: UiState, onPass: () -> Unit) {
    val t = LocalStrings.current
    val game = state.game ?: return
    val nextPlayer = state.nightOrder.getOrNull(state.nightPos + 1)
    val check = state.check
    Page(center = true) {
        Chip("🌙 ${t.nightTitle(game.round)}", Palette.Moon)
        Spacer(Modifier.height(24.dp))
        if (check != null) {
            Avatar(game.players[check.player], check.player, size = 96.dp)
            Spacer(Modifier.height(16.dp))
            Surface(shape = Soft, color = if (check.mafia) Palette.Blood else Palette.Mint, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (check.mafia) "🎩 ${t.checkIsMafia(game.players[check.player])}" else "🏡 ${t.checkNotMafia(game.players[check.player])}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (check.mafia) Color.White else Palette.Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp),
                )
            }
        } else {
            Hat(Modifier.size(120.dp), bob = false, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(16.dp))
            Text(t.doneText, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(30.dp))
        PillButton(nextPlayer?.let { t.hideAndPass(game.players[it]) } ?: t.hideAndWake, onPass, Modifier.fillMaxWidth())
    }
}

// ---------------------------------------------------------------- the morning

@Composable
internal fun MorningScreen(state: UiState, onReveal: () -> Unit, onContinue: () -> Unit) {
    val t = LocalStrings.current
    val game = state.game ?: return
    val night = game.lastNight ?: return
    var revealed by rememberSaveable(night.night) { mutableStateOf(false) }
    Page(day = true, center = true) {
        Text("🌅", fontSize = 84.sp)
        Text(t.morningTitle(night.night), fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        if (!revealed) {
            Text(t.morningIntro, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center, fontSize = 18.sp)
            Spacer(Modifier.height(28.dp))
            PillButton(t.reveal, { revealed = true; onReveal() }, Modifier.fillMaxWidth(), color = Palette.Dawn)
        } else {
            val victim = night.victim
            if (victim != null) {
                Avatar(game.players[victim], victim, size = 96.dp, dead = true)
                Spacer(Modifier.height(12.dp))
                Surface(shape = Soft, color = Palette.Blood, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔪 ${t.killedLine(game.players[victim])}", fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        if (game.revealRoles) {
                            Spacer(Modifier.height(6.dp))
                            Text(t.wasRole("${roleEmoji(game.roles[victim])} ${t.roleName(game.roles[victim])}"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(shape = Soft, color = Palette.Mint, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💊 ${t.nobodyDied}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Palette.Ink, textAlign = TextAlign.Center)
                        if (game.hasRole(Role.DOCTOR) && night.target != null) Text(t.savedLine, color = Palette.Ink, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            PillButton(if (game.over) t.continueLabel else "☀️ ${t.startDay}", onContinue, Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------- the day

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DayScreen(state: UiState, onVote: () -> Unit, onMinute: () -> Unit, onTimeUp: () -> Unit, onRules: () -> Unit) {
    val t = LocalStrings.current
    val game = state.game ?: return
    val end = state.timerEnd
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var rang by remember(end) { mutableStateOf(end != null && end <= SystemClock.elapsedRealtime()) }
    LaunchedEffect(end) {
        while (end != null) {
            now = SystemClock.elapsedRealtime()
            if (!rang && now >= end) {
                rang = true
                onTimeUp()
            }
            delay(250)
        }
    }
    Page(day = true) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Chip("☀️ ${t.dayTitle(game.round)}", Palette.Dawn)
            Spacer(Modifier.weight(1f))
            HelpButton(onRules)
        }
        game.lastNight?.let { night ->
            Text(
                night.victim?.let { "🔪 ${t.killedLine(game.players[it])}" } ?: "💊 ${t.nobodyDied}",
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
        Text(t.dayHint, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (end != null) {
            val left = ((end - now + 999) / 1000).coerceAtLeast(0)
            Surface(shape = Soft, color = if (left == 0L) Palette.Blood.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.25f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (left == 0L) "⏰ ${t.timeUp}" else "⏱️ %d:%02d".format(left / 60, left % 60),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onMinute) { Text(t.plusMinute, color = Palette.Moon, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
            }
        }
        Text(t.aliveLabel(game.living.size), fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Start))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            game.living.forEach { PlayerTag(game, it) }
        }
        val dead = game.players.indices.filter { !game.alive[it] }
        if (dead.isNotEmpty()) {
            Text(t.outLabel, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Start))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                dead.forEach { PlayerTag(game, it) }
            }
        }
        Spacer(Modifier.height(4.dp))
        PillButton("👉 ${t.toVote}", onVote, Modifier.fillMaxWidth())
    }
}

@Composable
private fun PlayerTag(game: Game, index: Int) {
    val t = LocalStrings.current
    val dead = !game.alive[index]
    Surface(shape = Pill, color = Color.White.copy(alpha = if (dead) 0.04f else 0.1f)) {
        Row(Modifier.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(game.players[index], index, size = 28.dp, dead = dead)
            Spacer(Modifier.width(8.dp))
            Text(game.players[index], fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, color = Color.White.copy(alpha = if (dead) 0.5f else 1f))
            if (dead && game.revealRoles) Text("  ${roleEmoji(game.roles[index])} ${t.roleName(game.roles[index])}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

/** The town has pointed at once. Somebody taps who got the most fingers, or nobody after a tie. */
@Composable
internal fun VoteScreen(state: UiState, onBack: () -> Unit, onEliminate: (Int?) -> Unit) {
    val t = LocalStrings.current
    val game = state.game ?: return
    // -1: nothing picked yet, -2: nobody (a tie)
    var picked by rememberSaveable(game.round) { mutableIntStateOf(-1) }
    Page(day = true) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text("← ${t.back}", color = Palette.Moon, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Text("👉 ${t.voteTitle}", fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text(t.voteHow, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        Text(t.whoIsOut, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Start))
        game.living.forEach { p -> PlayerRow(game, p, selected = picked == p) { picked = p } }
        Surface(
            shape = Soft,
            color = if (picked == -2) Palette.Moon.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
            modifier = Modifier.fillMaxWidth().border(2.dp, if (picked == -2) Palette.Moon else Color.Transparent, Soft).clip(Soft).clickable { picked = -2 },
        ) {
            Text("🤝 ${t.nobodyTie}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp))
        }
        Spacer(Modifier.height(4.dp))
        PillButton(
            when {
                picked >= 0 -> t.confirmOut(game.players[picked])
                picked == -2 -> t.confirmNobody
                else -> t.confirm
            },
            { onEliminate(picked.takeIf { it >= 0 }) },
            Modifier.fillMaxWidth(),
            enabled = picked != -1,
            color = if (picked >= 0) Palette.Blood else Palette.Gold,
            content = if (picked >= 0) Color.White else Palette.Ink,
        )
    }
}

@Composable
internal fun VoteResultScreen(state: UiState, onContinue: () -> Unit) {
    val t = LocalStrings.current
    val game = state.game ?: return
    val out = state.voted
    Page(day = true, center = true) {
        if (out != null) {
            Avatar(game.players[out], out, size = 110.dp, dead = true)
            Spacer(Modifier.height(16.dp))
            Text(t.outLine(game.players[out]), fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            if (game.revealRoles) {
                Spacer(Modifier.height(12.dp))
                val role = game.roles[out]
                Surface(shape = Soft, color = Palette.role(role).copy(alpha = 0.3f), modifier = Modifier.border(2.dp, Palette.role(role), Soft)) {
                    Text(t.wasRole("${roleEmoji(role)} ${t.roleName(role)}"), fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
                }
            }
        } else {
            Text("🤝", fontSize = 84.sp)
            Text(t.noOutLine, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(30.dp))
        PillButton(if (game.over) t.continueLabel else "🌙 ${t.nextNight}", onContinue, Modifier.fillMaxWidth())
    }
}

// ---------------------------------------------------------------- the end

@Composable
internal fun GameOverScreen(state: UiState, onNewGame: () -> Unit, onSetup: () -> Unit) {
    val t = LocalStrings.current
    val game = state.game ?: return
    val town = game.winner == Side.TOWN
    Page {
        Text(if (town) "🏡" else "🎩", fontSize = 64.sp)
        Surface(shape = Soft, color = if (town) Palette.Mint else Palette.Blood, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (town) t.townWins else t.mafiaWins, fontSize = 30.sp, fontWeight = FontWeight.Black, color = if (town) Palette.Ink else Color.White)
                Text(if (town) t.townWinsText else t.mafiaWinsText, color = if (town) Palette.Ink else Color.White, textAlign = TextAlign.Center)
            }
        }
        Text(t.rolesTitle, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.align(Alignment.Start))
        game.players.indices.forEach { i ->
            val role = game.roles[i]
            Surface(shape = Soft, color = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(game.players[i], i, size = 36.dp, dead = !game.alive[i])
                    Spacer(Modifier.width(12.dp))
                    Text(game.players[i], fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                    Chip("${roleEmoji(role)} ${t.roleName(role)}", Palette.role(role))
                }
            }
        }
        Text("📜 ${t.story}", fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.align(Alignment.Start))
        Story(game)
        PillButton(t.newGame, onNewGame, Modifier.fillMaxWidth())
        TextButton(onClick = onSetup) { Text(t.editPlayers, color = Palette.Moon, fontWeight = FontWeight.Bold) }
    }
}

/** Every night and day: who the Mafia went after, who was protected and checked, who died, who was voted out. */
@Composable
private fun Story(game: Game) {
    val t = LocalStrings.current
    val name = { i: Int -> game.players[i] }
    val rounds = maxOf(game.nights.size, game.days.size)
    Surface(shape = Soft, color = Color.White.copy(alpha = 0.06f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 1..rounds) {
                game.nights.firstOrNull { it.night == r }?.let { n ->
                    Text(t.storyNight(r), fontWeight = FontWeight.Black, color = Palette.Moon)
                    n.target?.let { Text("🎩 ${t.storyMafia(name(it))}", fontSize = 14.sp) }
                    n.protected?.let { Text("💊 ${t.storyDoctor(name(it))}", fontSize = 14.sp) }
                    n.checked?.let { Text("🔍 ${t.storyDetective(name(it), game.roles[it] == Role.MAFIA)}", fontSize = 14.sp) }
                    if (n.suspects.isNotEmpty()) {
                        val tally = n.suspects.groupingBy { it.second }.eachCount().entries.sortedByDescending { it.value }
                        Text("🏡 ${t.storySuspects(tally.joinToString(", ") { "${name(it.key)} ×${it.value}" })}", fontSize = 14.sp)
                    }
                    Text(n.victim?.let { "🔪 ${t.storyKilled(name(it))}" } ?: "✨ ${t.storySaved}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                game.days.firstOrNull { it.day == r }?.let { d ->
                    Text(t.storyDay(r), fontWeight = FontWeight.Black, color = Palette.Dawn)
                    Text(d.eliminated?.let { "👉 ${t.storyOut(name(it))}" } ?: "🤝 ${t.storyNobodyOut}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
