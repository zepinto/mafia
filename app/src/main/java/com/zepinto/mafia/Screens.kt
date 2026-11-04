package com.zepinto.mafia

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MafiaApp(vm: GameViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val t = state.lang.strings
    val owner = LocalLifecycleOwner.current
    var showRules by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }

    // Whatever is secret goes off screen the moment the app is left.
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) vm.onPause()
            if (event == Lifecycle.Event.ON_RESUME) vm.onResume()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = state.stage != Stage.SETUP) {
        when {
            state.stage == Stage.REVEAL && state.cardUp -> vm.hide()
            state.stage == Stage.VOTE -> vm.cancelVote()
            state.stage == Stage.GAME_OVER -> vm.backToSetup()
            else -> confirmLeave = true
        }
    }
    // declared last so it wins: Back closes the rules page first
    BackHandler(enabled = showRules) { showRules = false }

    CompositionLocalProvider(LocalStrings provides t) {
        if (showRules) {
            RulesScreen(onBack = { showRules = false })
        } else {
            when (state.stage) {
                Stage.SETUP -> SetupScreen(state, vm, onRules = { showRules = true })
                Stage.REVEAL -> RevealScreen(state, onFlip = vm::flip, onHide = vm::hide, onNext = vm::next)
                Stage.NIGHT_INTRO -> NightIntroScreen(state, onBegin = vm::beginNight, onRules = { showRules = true })
                Stage.NIGHT_PASS -> NightPassScreen(state, onReady = vm::ready)
                Stage.NIGHT_ACTION -> NightActionScreen(state, onChoose = vm::choose)
                Stage.NIGHT_DONE -> NightDoneScreen(state, onPass = vm::passOn)
                Stage.MORNING -> MorningScreen(state, onReveal = vm::revealMorning, onContinue = vm::afterMorning)
                Stage.DAY -> DayScreen(state, onVote = vm::startVote, onMinute = vm::addMinute, onTimeUp = vm::timeUp, onRules = { showRules = true })
                Stage.VOTE -> VoteScreen(state, onBack = vm::cancelVote, onEliminate = vm::eliminate)
                Stage.VOTE_RESULT -> VoteResultScreen(state, onContinue = vm::afterVote)
                Stage.GAME_OVER -> GameOverScreen(state, onNewGame = vm::newGame, onSetup = vm::backToSetup)
            }
        }

        if (confirmLeave) {
            AlertDialog(
                onDismissRequest = { confirmLeave = false },
                title = { Text(t.leaveTitle) },
                text = { Text(t.leaveText) },
                confirmButton = { TextButton(onClick = { confirmLeave = false; vm.backToSetup() }) { Text(t.leave) } },
                dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(t.stay) } },
            )
        }
    }
}

// ---------------------------------------------------------------- setup

@Composable
internal fun HelpButton(onClick: () -> Unit) {
    val t = LocalStrings.current
    Surface(
        shape = CircleShape,
        color = Palette.Moon.copy(alpha = 0.18f),
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .semantics { contentDescription = t.rulesButton }
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("?", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Palette.Moon)
        }
    }
}

/** Music on or off. The sound effects stay on: they are part of the game (a peek at a card buzzes). */
@Composable
internal fun MusicButton(on: Boolean, onClick: () -> Unit) {
    val t = LocalStrings.current
    Surface(
        shape = CircleShape,
        color = Palette.Moon.copy(alpha = if (on) 0.18f else 0.06f),
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .semantics { contentDescription = t.musicButton }
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(if (on) "🎵" else "🔇", fontSize = 20.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupScreen(state: UiState, vm: GameViewModel, onRules: () -> Unit) {
    val t = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val n = state.n
    val canStart = n in Rules.MIN_PLAYERS..Rules.MAX_PLAYERS
    val submit = {
        if (vm.addPlayer(name)) {
            name = ""
            nameError = null
        } else if (name.isNotBlank()) {
            nameError = if (n >= Rules.MAX_PLAYERS) t.maxPlayers(Rules.MAX_PLAYERS) else t.nameTaken
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .nightBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // with the keyboard open on a small phone the player list needs the room more than the header does
        val keyboard = WindowInsets.isImeVisible
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!keyboard) {
                Hat(Modifier.size(72.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(t.appTitle, fontSize = if (keyboard) 24.sp else 34.sp, fontWeight = FontWeight.Black)
                if (!keyboard) Text(t.tagline, color = Palette.Lilac)
            }
            MusicButton(state.musicOn) { vm.setMusic(!state.musicOn) }
            Spacer(Modifier.width(8.dp))
            HelpButton(onRules)
        }
        if (!keyboard) {
            Spacer(Modifier.height(12.dp))
            LanguagePicker(state.lang, vm::setLanguage)
        }
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                isError = nameError != null,
                placeholder = { Text(t.whoIsPlaying) },
                singleLine = true,
                shape = Pill,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PillButton(t.add, submit, enabled = name.isNotBlank(), color = Palette.Moon, content = Palette.Ink)
        }
        nameError?.let { Text(it, color = Palette.Blood, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp)) }
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.players, key = { _, p -> p }) { i, player ->
                Surface(shape = Soft, color = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth().animateItem()) {
                    Row(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(player, i)
                        Spacer(Modifier.width(12.dp))
                        Text(player, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.removePlayer(i) }) {
                            Text("✕", color = Palette.Blood, fontSize = 18.sp, modifier = Modifier.semantics { contentDescription = t.removePlayer })
                        }
                    }
                }
            }
            item(key = "info") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canStart) SettingsCard(state, vm) else Text(t.addAtLeast(Rules.MIN_PLAYERS), color = Palette.Lilac)
                    Text(t.seatingHint, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    Text("🔊 ${t.volumeHint}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        if (state.hasSaved) {
            PillButton(t.resumeGame, vm::resume, Modifier.fillMaxWidth(), color = Palette.Moon)
            Spacer(Modifier.height(8.dp))
        }
        PillButton(t.dealRoles, vm::startGame, Modifier.fillMaxWidth(), enabled = canStart)
    }
}

/** How many Mafia, which special roles, and the day's timer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsCard(state: UiState, vm: GameViewModel) {
    val t = LocalStrings.current
    val n = state.n
    val m = state.mafiaCount
    val villagers = n - m - (if (state.detective) 1 else 0) - (if (state.doctor) 1 else 0)
    Surface(shape = Soft, color = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t.rolesSummary(m, state.detective, state.doctor, villagers), fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${roleEmoji(Role.MAFIA)} ${t.mafiaLabel}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StepButton("−", enabled = m > 1) { vm.changeMafia(-1) }
                Text("$m", fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 14.dp))
                StepButton("+", enabled = m < Rules.maxMafia(n)) { vm.changeMafia(+1) }
            }
            if (m == Rules.recommendedMafia(n)) Text(t.recommended, fontSize = 12.sp, color = Palette.Mint)
            ToggleRow("${roleEmoji(Role.DETECTIVE)} ${t.roleName(Role.DETECTIVE)}", state.detective, vm::setDetective)
            ToggleRow("${roleEmoji(Role.DOCTOR)} ${t.roleName(Role.DOCTOR)}", state.doctor, vm::setDoctor)
            ToggleRow("🃏 ${t.revealLabel}", state.revealRoles, vm::setRevealRoles)
            Text("⏱️ ${t.talkLabel}", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Rules.TALK_OPTIONS.forEach { min ->
                    val on = state.talkMinutes == min
                    Surface(
                        shape = Pill,
                        color = if (on) Palette.Moon.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.weight(1f).border(2.dp, if (on) Palette.Moon else Color.Transparent, Pill).clip(Pill).clickable { vm.setTalk(min) },
                    ) {
                        Text(
                            if (min == 0) t.noTimer else t.minutes(min),
                            fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 1,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 9.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (enabled) Palette.Moon.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 22.sp, fontWeight = FontWeight.Black, color = if (enabled) Palette.Moon else Color.White.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(Soft).clickable { onChange(!on) }) {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(
            checked = on,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.Moon, checkedThumbColor = Palette.Ink),
        )
    }
}

@Composable
internal fun LanguagePicker(selected: Lang, onSelect: (Lang) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Lang.entries.forEach { lang ->
            val on = lang == selected
            Surface(
                shape = Pill,
                color = if (on) Palette.Moon.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, if (on) Palette.Moon else Color.Transparent, Pill)
                    .clip(Pill)
                    .clickable { onSelect(lang) },
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lang.flag, fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(lang.code.uppercase(), fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp, maxLines = 1)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- role cards

/** One role card at a time, face down with its owner's name under it, dealt round the table. */
@Composable
private fun RevealScreen(state: UiState, onFlip: () -> Unit, onHide: () -> Unit, onNext: () -> Unit) {
    val t = LocalStrings.current
    val game = state.game ?: return
    val index = state.position
    val seenThis = index in state.seen
    val name = game.players[index]
    val nextName = if (state.onLastCard) null else game.players[index + 1]

    Column(
        Modifier
            .fillMaxSize()
            .nightBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Chip("🃏 ${t.cardOf(index + 1, game.n)}", Palette.Moon)
        Spacer(Modifier.height(16.dp))

        // a new composition per card, so the flip state never carries over to the next player
        androidx.compose.runtime.key(index) {
            FlipCard(
                up = state.cardUp,
                onTap = onFlip,
                modifier = Modifier.weight(1f).fillMaxWidth(0.88f),
                back = { CardBackFace(index) },
                front = { RoleFace(game, index) },
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name, index, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Text(name, fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                state.cardUp -> t.memorize
                seenThis -> t.hiddenPass
                else -> t.tapToFlip(name)
            },
            color = Palette.Lilac,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        when {
            state.cardUp -> PillButton(t.hideCard, onHide, Modifier.fillMaxWidth(), color = Palette.Moon)
            seenThis -> PillButton(if (nextName != null) t.passTo(nextName) else "🌙 ${t.nightFalls}", onNext, Modifier.fillMaxWidth())
            else -> Spacer(Modifier.height(54.dp))
        }
    }
}

/** A card that flips around its vertical axis. The front is only composed once it is told to be up and has turned past the edge. */
@Composable
private fun FlipCard(
    up: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    back: @Composable () -> Unit,
    front: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (up) 180f else 0f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
        label = "flip",
    )
    Box(
        modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density
            }
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onTap),
    ) {
        // Gate on `up` too: hiding must be instant, not after the spring has swung past the edge.
        if (up && rotation > 90f) {
            Box(Modifier.graphicsLayer { rotationY = 180f }) { front() }
        } else {
            back()
        }
    }
}

@Composable
private fun CardBackFace(index: Int) {
    val shape = RoundedCornerShape(32.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Palette.Night, Palette.Ink)))
            .border(3.dp, Palette.avatar(index), shape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Hat(Modifier.size(150.dp), color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(8.dp))
            Text("?", fontSize = 60.sp, fontWeight = FontWeight.Black, color = Palette.avatar(index))
        }
    }
}

/** The same layout for every role, so a glance from the side reveals little. */
@Composable
private fun RoleFace(game: Game, index: Int) {
    val t = LocalStrings.current
    val role = game.roles[index]
    val partners = if (role == Role.MAFIA) game.mafia.filter { it != index }.map { game.players[it] } else emptyList()
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF2A2140), Color(0xFF5A2E4F))))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(roleEmoji(role), fontSize = 64.sp)
        Spacer(Modifier.height(4.dp))
        Text(t.youAre, color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
        Text(t.roleName(role), fontSize = 40.sp, fontWeight = FontWeight.Black, color = Palette.role(role), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(t.roleHint(role), color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        if (partners.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(shape = Soft, color = Color.Black.copy(alpha = 0.25f)) {
                Text(
                    t.fellowMafia(partners.joinToString(", ")),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- rules

@Composable
private fun RulesScreen(onBack: () -> Unit) {
    val t = LocalStrings.current
    Column(
        Modifier
            .fillMaxSize()
            .nightBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        TextButton(onClick = onBack) { Text("← ${t.back}", color = Palette.Moon, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Text("📖 ${t.rulesTitle}", fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            t.rulesSections.forEach { section ->
                Surface(shape = Soft, color = Color.White.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${section.emoji}  ${section.title}", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Gold)
                        section.lines.forEach { line ->
                            Row {
                                Text("•", color = Palette.Lilac, modifier = Modifier.width(18.dp))
                                Text(line, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
