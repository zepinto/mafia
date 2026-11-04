package com.zepinto.mafia

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/** Languages the game is available in. */
enum class Lang(val code: String, val flag: String, val label: String) {
    EN("en", "🇬🇧", "English"),
    PT("pt", "🇵🇹", "Português"),
    ES("es", "🇪🇸", "Español");

    val strings: Strings
        get() = when (this) {
            EN -> English
            PT -> Portuguese
            ES -> Spanish
        }

    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code }

        /** The phone's language if the game has it, English otherwise. */
        fun deviceDefault() = fromCode(Locale.getDefault().language) ?: EN
    }
}

val LocalStrings = staticCompositionLocalOf { English }

/** One block of the rules page: a heading and its lines. */
data class RulesSection(val emoji: String, val title: String, val lines: List<String>)

/** Every user-visible text. Adding a field forces all three languages to provide it. */
data class Strings(
    // setup
    val appTitle: String,
    val tagline: String,
    val whoIsPlaying: String,
    val add: String,
    val nameTaken: String,
    val maxPlayers: (Int) -> String,
    val addAtLeast: (Int) -> String,
    val dealRoles: String,
    val resumeGame: String,
    val volumeHint: String,
    val seatingHint: String,
    val removePlayer: String,
    val mafiaLabel: String,
    val recommended: String,
    val revealLabel: String,
    val talkLabel: String,
    val minutes: (Int) -> String,
    val noTimer: String,
    val rolesSummary: (Int, Boolean, Boolean, Int) -> String,

    // roles
    val roleName: (Role) -> String,
    val roleHint: (Role) -> String,

    // role cards
    val cardOf: (Int, Int) -> String,
    val tapToFlip: (String) -> String,
    val memorize: String,
    val hiddenPass: String,
    val hideCard: String,
    val passTo: (String) -> String,
    val nightFalls: String,
    val youAre: String,
    val fellowMafia: (String) -> String,

    // the night
    val nightTitle: (Int) -> String,
    val nightIntro: List<String>,
    val passRound: String,
    val passPhoneTo: (String) -> String,
    val progressOf: (Int, Int) -> String,
    val imName: (String) -> String,
    val actionQuestion: (Role) -> String,
    val actionHint: (Role) -> String,
    val partnerPicked: (String, String) -> String,
    val noPicksYet: String,
    val protectedLastNight: String,
    val confirm: String,
    val doneText: String,
    val checkIsMafia: (String) -> String,
    val checkNotMafia: (String) -> String,
    val hideAndPass: (String) -> String,
    val hideAndWake: String,

    // the morning
    val morningTitle: (Int) -> String,
    val morningIntro: String,
    val reveal: String,
    val killedLine: (String) -> String,
    val nobodyDied: String,
    val savedLine: String,
    val wasRole: (String) -> String,
    val startDay: String,

    // the day and the vote
    val dayTitle: (Int) -> String,
    val dayHint: String,
    val timeUp: String,
    val plusMinute: String,
    val toVote: String,
    val aliveLabel: (Int) -> String,
    val outLabel: String,
    val voteTitle: String,
    val voteHow: String,
    val whoIsOut: String,
    val nobodyTie: String,
    val confirmOut: (String) -> String,
    val confirmNobody: String,
    val outLine: (String) -> String,
    val noOutLine: String,
    val nextNight: String,

    // end of the game
    val townWins: String,
    val mafiaWins: String,
    val townWinsText: String,
    val mafiaWinsText: String,
    val rolesTitle: String,
    val story: String,
    val storyNight: (Int) -> String,
    val storyDay: (Int) -> String,
    val storyMafia: (String) -> String,
    val storyDoctor: (String) -> String,
    val storyDetective: (String, Boolean) -> String,
    val storyKilled: (String) -> String,
    val storySaved: String,
    val storySuspects: (String) -> String,
    val storyOut: (String) -> String,
    val storyNobodyOut: String,
    val newGame: String,
    val editPlayers: String,

    // shared
    val leaveTitle: String,
    val leaveText: String,
    val leave: String,
    val stay: String,
    val back: String,
    val close: String,
    val continueLabel: String,
    val rulesButton: String,
    val musicButton: String,
    val rulesTitle: String,
    val rulesSections: List<RulesSection>,
)

val English = Strings(
    appTitle = "Mafia",
    tagline = "The town sleeps. The Mafia doesn't.",
    whoIsPlaying = "Who's playing?",
    add = "Add",
    nameTaken = "That name is already playing",
    maxPlayers = { "The game is for up to $it players" },
    addAtLeast = { "Add at least $it players" },
    dealRoles = "Deal the roles",
    resumeGame = "Resume game",
    volumeHint = "Turn the sound up: the phone chimes when a card is flipped, and buzzes if someone peeks again. 🎵 turns the music off.",
    seatingHint = "Add the players in the order they sit: the phone goes round the table in that order.",
    removePlayer = "Remove player",
    mafiaLabel = "Mafia",
    recommended = "recommended",
    revealLabel = "Show the role of the dead",
    talkLabel = "Discussion each day",
    minutes = { "$it min" },
    noTimer = "No timer",
    rolesSummary = { m, det, doc, v ->
        listOfNotNull("$m Mafia", "Detective".takeIf { det }, "Doctor".takeIf { doc }, if (v == 1) "1 Villager" else "$v Villagers").joinToString(" · ")
    },
    roleName = {
        when (it) {
            Role.MAFIA -> "Mafia"
            Role.DETECTIVE -> "Detective"
            Role.DOCTOR -> "Doctor"
            Role.VILLAGER -> "Villager"
        }
    },
    roleHint = {
        when (it) {
            Role.MAFIA -> "Each night, choose a victim with your partners. By day, pretend to be a Villager."
            Role.DETECTIVE -> "Each night, find out whether one player is in the Mafia. Careful who you tell."
            Role.DOCTOR -> "Each night, protect one player from the Mafia. It can be you."
            Role.VILLAGER -> "No powers, just your eyes and your voice. Find the Mafia and vote them out."
        }
    },
    cardOf = { i, n -> "Card $i of $n" },
    tapToFlip = { "$it, tap the card to see your role" },
    memorize = "Remember it, then hide it",
    hiddenPass = "Card hidden. Pass the phone on.",
    hideCard = "Hide card",
    passTo = { "Pass to $it" },
    nightFalls = "Night falls",
    youAre = "You are",
    fellowMafia = { "Your partners: $it" },
    nightTitle = { "Night $it" },
    nightIntro = listOf(
        "Everybody takes the phone in turn, even the Villagers, so nobody can tell who did what.",
        "Keep a straight face, and take your time.",
        "The dead stay silent.",
    ),
    passRound = "Pass the phone round",
    passPhoneTo = { "Pass the phone to $it" },
    progressOf = { i, n -> "$i of $n" },
    imName = { "I'm $it" },
    actionQuestion = {
        when (it) {
            Role.MAFIA -> "Who does the Mafia eliminate tonight?"
            Role.DETECTIVE -> "Who do you investigate?"
            Role.DOCTOR -> "Who do you protect tonight?"
            Role.VILLAGER -> "Who do you suspect most?"
        }
    },
    actionHint = {
        when (it) {
            Role.MAFIA -> "The player picked most often is the victim. A tie goes to the first pick."
            Role.DETECTIVE -> "You'll find out whether they are in the Mafia."
            Role.DOCTOR -> "Anyone, you too, but not the same player two nights running."
            Role.VILLAGER -> "Nobody sees this until the end of the game. It just means everybody spends the same time with the phone."
        }
    },
    partnerPicked = { who, target -> "$who picked $target" },
    noPicksYet = "Your partners haven't chosen yet",
    protectedLastNight = "protected last night",
    confirm = "Confirm",
    doneText = "Done. Your choice is in.",
    checkIsMafia = { "$it IS in the Mafia!" },
    checkNotMafia = { "$it is not in the Mafia." },
    hideAndPass = { "Hide and pass to $it" },
    hideAndWake = "Hide and put the phone in the middle",
    morningTitle = { "Morning of day $it" },
    morningIntro = "Everybody, wake up! What happened in the night?",
    reveal = "Find out",
    killedLine = { "$it was killed in the night" },
    nobodyDied = "Nobody died last night",
    savedLine = "The Doctor got there in time.",
    wasRole = { "Role: $it" },
    startDay = "Start the day",
    dayTitle = { "Day $it" },
    dayHint = "Talk it over. Accuse, defend, bluff. Then everybody votes at once.",
    timeUp = "Time's up!",
    plusMinute = "+1 min",
    toVote = "Go to the vote",
    aliveLabel = { "Alive ($it)" },
    outLabel = "Out",
    voteTitle = "The vote",
    voteHow = "Count to three and everybody points at one player at the same time. The player with the most fingers is out. On a tie, nobody is.",
    whoIsOut = "Who got the most votes?",
    nobodyTie = "Nobody: it's a tie",
    confirmOut = { "$it is out" },
    confirmNobody = "Nobody is out",
    outLine = { "$it is out of the game" },
    noOutLine = "Nobody is out today",
    nextNight = "Night falls again",
    townWins = "The town wins!",
    mafiaWins = "The Mafia wins!",
    townWinsText = "Every member of the Mafia has been found.",
    mafiaWinsText = "The Mafia are now as many as everybody else.",
    rolesTitle = "Roles",
    story = "The story of the game",
    storyNight = { "🌙 Night $it" },
    storyDay = { "☀️ Day $it" },
    storyMafia = { "The Mafia went after $it" },
    storyDoctor = { "The Doctor protected $it" },
    storyDetective = { who, mafia -> "The Detective checked $who: " + if (mafia) "Mafia!" else "not Mafia" },
    storyKilled = { "$it was killed" },
    storySaved = "Nobody died",
    storySuspects = { "The Villagers suspected: $it" },
    storyOut = { "The town voted out $it" },
    storyNobodyOut = "Nobody was voted out",
    newGame = "New game",
    editPlayers = "Change the players",
    leaveTitle = "Leave the game?",
    leaveText = "The game in progress will be lost.",
    leave = "Leave",
    stay = "Keep playing",
    back = "Back",
    close = "Close",
    continueLabel = "Continue",
    rulesButton = "Rules",
    musicButton = "Music on or off",
    rulesTitle = "How to play",
    rulesSections = listOf(
        RulesSection(
            "🎯", "The goal",
            listOf(
                "A few players are secretly the Mafia. Everybody else is the town.",
                "The town wins by voting out every member of the Mafia.",
                "The Mafia wins by eliminating townspeople until they are as many as everybody else.",
            ),
        ),
        RulesSection(
            "🃏", "The roles",
            listOf(
                "🎩 Mafia: they know each other. Each night they choose a victim.",
                "🔍 Detective: each night, finds out whether one player is in the Mafia.",
                "💊 Doctor: each night, protects one player. It can be themselves, but not the same player two nights running.",
                "🏡 Villagers: no powers. They have to find the Mafia by talking.",
                "About one player in four is Mafia (5–6 players: 1, 7–9: 2, 10–12: 3). You can change it, and turn the Detective and the Doctor off.",
                "Each player flips their card on the phone, remembers it, hides it and passes the phone on.",
            ),
        ),
        RulesSection(
            "🌙", "The night",
            listOf(
                "The phone goes round the table and every living player takes it, whatever their role. Hold it for about the same time whoever you are.",
                "The Mafia choose a victim. Each one sees what their partners picked before them. The player picked most often is the victim; a tie goes to the first pick.",
                "The Doctor chooses who to protect and the Detective who to investigate.",
                "Villagers choose who they suspect most. Nobody sees it until the story at the end of the game.",
            ),
        ),
        RulesSection(
            "☀️", "The morning",
            listOf(
                "The phone tells everybody who was killed in the night, or that nobody died because the Doctor protected the victim.",
                "If the option is on, the dead player's role is shown.",
                "Dead players are out: they stay silent until the end and don't take the phone any more.",
            ),
        ),
        RulesSection(
            "🗣️", "The day",
            listOf(
                "Everybody talks: accuse, defend yourself, bluff. The timer keeps the day moving.",
                "Then count to three and everybody points at one player at the same time.",
                "The player with the most fingers pointing at them is out. On a tie, nobody is.",
                "Tap that player on the phone, and night falls again.",
            ),
        ),
        RulesSection(
            "🏆", "Winning",
            listOf(
                "The town wins as soon as no Mafia are left.",
                "The Mafia wins as soon as they are as many as the rest of the living players.",
                "At the end the phone shows every role and the story of each night.",
            ),
        ),
        RulesSection(
            "💡", "Tips",
            listOf(
                "Mafia: behave exactly like a Villager. Accuse, but not too hard, and don't defend your partners too openly.",
                "Detective: the Mafia will come for you as soon as you speak up. Choose the moment.",
                "Watch the votes: who saved whom, who changed their mind at the last moment.",
            ),
        ),
    ),
)

val Portuguese = Strings(
    appTitle = "Máfia",
    tagline = "A vila dorme. A Máfia não.",
    whoIsPlaying = "Quem joga?",
    add = "Adicionar",
    nameTaken = "Esse nome já está no jogo",
    maxPlayers = { "O jogo é para até $it jogadores" },
    addAtLeast = { "Adiciona pelo menos $it jogadores" },
    dealRoles = "Distribuir as funções",
    resumeGame = "Retomar jogo",
    volumeHint = "Liga o som: o telemóvel toca quando se vira uma carta, e zumbe se alguém espreitar outra vez. 🎵 desliga a música.",
    seatingHint = "Adiciona os jogadores pela ordem em que estão sentados: o telemóvel dá a volta à mesa nessa ordem.",
    removePlayer = "Remover jogador",
    mafiaLabel = "Máfia",
    recommended = "recomendado",
    revealLabel = "Mostrar a função dos mortos",
    talkLabel = "Discussão em cada dia",
    minutes = { "$it min" },
    noTimer = "Sem tempo",
    rolesSummary = { m, det, doc, v ->
        listOfNotNull("$m Máfia", "Detetive".takeIf { det }, "Médico".takeIf { doc }, if (v == 1) "1 Aldeão" else "$v Aldeões").joinToString(" · ")
    },
    roleName = {
        when (it) {
            Role.MAFIA -> "Máfia"
            Role.DETECTIVE -> "Detetive"
            Role.DOCTOR -> "Médico"
            Role.VILLAGER -> "Aldeão"
        }
    },
    roleHint = {
        when (it) {
            Role.MAFIA -> "Todas as noites, escolhe uma vítima com os teus parceiros. De dia, finge que és um Aldeão."
            Role.DETECTIVE -> "Todas as noites, descobre se um jogador é da Máfia. Cuidado a quem contas."
            Role.DOCTOR -> "Todas as noites, protege um jogador da Máfia. Podes ser tu."
            Role.VILLAGER -> "Sem poderes, só os teus olhos e a tua voz. Encontra a Máfia e expulsa-a."
        }
    },
    cardOf = { i, n -> "Carta $i de $n" },
    tapToFlip = { "$it, toca na carta para veres a tua função" },
    memorize = "Memoriza e esconde",
    hiddenPass = "Carta escondida. Passa o telemóvel.",
    hideCard = "Esconder carta",
    passTo = { "Passar a $it" },
    nightFalls = "Cai a noite",
    youAre = "És",
    fellowMafia = { "Os teus parceiros: $it" },
    nightTitle = { "Noite $it" },
    nightIntro = listOf(
        "Todos pegam no telemóvel à vez, até os Aldeões, para ninguém saber quem fez o quê.",
        "Mantém a cara séria e não tenhas pressa.",
        "Os mortos ficam calados.",
    ),
    passRound = "Passar o telemóvel à volta",
    passPhoneTo = { "Passa o telemóvel a $it" },
    progressOf = { i, n -> "$i de $n" },
    imName = { "Sou $it" },
    actionQuestion = {
        when (it) {
            Role.MAFIA -> "Quem é que a Máfia elimina esta noite?"
            Role.DETECTIVE -> "Quem investigas?"
            Role.DOCTOR -> "Quem proteges esta noite?"
            Role.VILLAGER -> "De quem desconfias mais?"
        }
    },
    actionHint = {
        when (it) {
            Role.MAFIA -> "A vítima é o jogador mais escolhido. Num empate, fica a primeira escolha."
            Role.DETECTIVE -> "Vais saber se é da Máfia."
            Role.DOCTOR -> "Qualquer um, tu também, mas não o mesmo jogador duas noites seguidas."
            Role.VILLAGER -> "Ninguém vê isto até ao fim do jogo. Serve para todos ficarem o mesmo tempo com o telemóvel."
        }
    },
    partnerPicked = { who, target -> "$who escolheu $target" },
    noPicksYet = "Os teus parceiros ainda não escolheram",
    protectedLastNight = "protegido na noite passada",
    confirm = "Confirmar",
    doneText = "Pronto. A tua escolha está feita.",
    checkIsMafia = { "$it É da Máfia!" },
    checkNotMafia = { "$it não é da Máfia." },
    hideAndPass = { "Esconder e passar a $it" },
    hideAndWake = "Esconder e pôr o telemóvel no meio",
    morningTitle = { "Manhã do dia $it" },
    morningIntro = "Acordem todos! O que aconteceu durante a noite?",
    reveal = "Descobrir",
    killedLine = { "$it foi morto durante a noite" },
    nobodyDied = "Ninguém morreu esta noite",
    savedLine = "O Médico chegou a tempo.",
    wasRole = { "Função: $it" },
    startDay = "Começar o dia",
    dayTitle = { "Dia $it" },
    dayHint = "Conversem. Acusem, defendam-se, façam bluff. Depois votam todos ao mesmo tempo.",
    timeUp = "Acabou o tempo!",
    plusMinute = "+1 min",
    toVote = "Ir à votação",
    aliveLabel = { "Vivos ($it)" },
    outLabel = "Fora",
    voteTitle = "A votação",
    voteHow = "Contem até três e todos apontam para um jogador ao mesmo tempo. Sai quem tiver mais dedos apontados. Num empate, não sai ninguém.",
    whoIsOut = "Quem teve mais votos?",
    nobodyTie = "Ninguém: houve empate",
    confirmOut = { "$it sai" },
    confirmNobody = "Não sai ninguém",
    outLine = { "$it está fora do jogo" },
    noOutLine = "Hoje não sai ninguém",
    nextNight = "Cai a noite outra vez",
    townWins = "A vila ganha!",
    mafiaWins = "A Máfia ganha!",
    townWinsText = "Todos os membros da Máfia foram descobertos.",
    mafiaWinsText = "A Máfia já é tanta como todos os outros.",
    rolesTitle = "Funções",
    story = "A história do jogo",
    storyNight = { "🌙 Noite $it" },
    storyDay = { "☀️ Dia $it" },
    storyMafia = { "A Máfia foi atrás de $it" },
    storyDoctor = { "O Médico protegeu $it" },
    storyDetective = { who, mafia -> "O Detetive investigou $who: " + if (mafia) "Máfia!" else "não é da Máfia" },
    storyKilled = { "$it foi morto" },
    storySaved = "Ninguém morreu",
    storySuspects = { "Os Aldeões desconfiaram de: $it" },
    storyOut = { "A vila expulsou $it" },
    storyNobodyOut = "Ninguém foi expulso",
    newGame = "Novo jogo",
    editPlayers = "Mudar os jogadores",
    leaveTitle = "Sair do jogo?",
    leaveText = "O jogo em curso vai perder-se.",
    leave = "Sair",
    stay = "Continuar a jogar",
    back = "Voltar",
    close = "Fechar",
    continueLabel = "Continuar",
    rulesButton = "Regras",
    musicButton = "Ligar ou desligar a música",
    rulesTitle = "Como se joga",
    rulesSections = listOf(
        RulesSection(
            "🎯", "O objetivo",
            listOf(
                "Alguns jogadores são, em segredo, a Máfia. Todos os outros são a vila.",
                "A vila ganha se expulsar todos os membros da Máfia.",
                "A Máfia ganha se eliminar gente da vila até ser tanta como todos os outros.",
            ),
        ),
        RulesSection(
            "🃏", "As funções",
            listOf(
                "🎩 Máfia: conhecem-se uns aos outros. Todas as noites escolhem uma vítima.",
                "🔍 Detetive: todas as noites descobre se um jogador é da Máfia.",
                "💊 Médico: todas as noites protege um jogador. Pode ser ele próprio, mas não o mesmo jogador duas noites seguidas.",
                "🏡 Aldeões: sem poderes. Têm de descobrir a Máfia a conversar.",
                "Cerca de um jogador em cada quatro é da Máfia (5–6 jogadores: 1, 7–9: 2, 10–12: 3). Podes mudar isto, e desligar o Detetive e o Médico.",
                "Cada jogador vira a sua carta no telemóvel, memoriza, esconde e passa o telemóvel.",
            ),
        ),
        RulesSection(
            "🌙", "A noite",
            listOf(
                "O telemóvel dá a volta à mesa e todos os jogadores vivos pegam nele, seja qual for a função. Fica com ele mais ou menos o mesmo tempo, sejas quem fores.",
                "A Máfia escolhe uma vítima. Cada um vê o que os parceiros escolheram antes dele. A vítima é o jogador mais escolhido; num empate, fica a primeira escolha.",
                "O Médico escolhe quem protege e o Detetive quem investiga.",
                "Os Aldeões escolhem de quem desconfiam mais. Ninguém vê isso até à história do fim do jogo.",
            ),
        ),
        RulesSection(
            "☀️", "A manhã",
            listOf(
                "O telemóvel diz a todos quem foi morto durante a noite, ou que ninguém morreu porque o Médico protegeu a vítima.",
                "Se a opção estiver ligada, mostra a função de quem morreu.",
                "Os mortos estão fora: ficam calados até ao fim e não voltam a pegar no telemóvel.",
            ),
        ),
        RulesSection(
            "🗣️", "O dia",
            listOf(
                "Todos conversam: acusam, defendem-se, fazem bluff. O cronómetro não deixa o dia arrastar-se.",
                "Depois contam até três e todos apontam para um jogador ao mesmo tempo.",
                "Sai quem tiver mais dedos apontados. Num empate, não sai ninguém.",
                "Toca nesse jogador no telemóvel, e cai a noite outra vez.",
            ),
        ),
        RulesSection(
            "🏆", "Quem ganha",
            listOf(
                "A vila ganha logo que não reste ninguém da Máfia.",
                "A Máfia ganha logo que seja tanta como o resto dos jogadores vivos.",
                "No fim, o telemóvel mostra todas as funções e a história de cada noite.",
            ),
        ),
        RulesSection(
            "💡", "Dicas",
            listOf(
                "Máfia: porta-te exatamente como um Aldeão. Acusa, mas sem exagero, e não defendas os teus parceiros às claras.",
                "Detetive: a Máfia vem atrás de ti assim que falares. Escolhe bem o momento.",
                "Olha para os votos: quem salvou quem, quem mudou de ideias à última hora.",
            ),
        ),
    ),
)

val Spanish = Strings(
    appTitle = "Mafia",
    tagline = "El pueblo duerme. La Mafia no.",
    whoIsPlaying = "¿Quién juega?",
    add = "Añadir",
    nameTaken = "Ese nombre ya está en el juego",
    maxPlayers = { "El juego es para hasta $it jugadores" },
    addAtLeast = { "Añade al menos $it jugadores" },
    dealRoles = "Repartir los papeles",
    resumeGame = "Seguir partida",
    volumeHint = "Sube el volumen: el móvil suena al girar una carta, y zumba si alguien vuelve a mirar. 🎵 quita la música.",
    seatingHint = "Añade a los jugadores en el orden en que están sentados: el móvil da la vuelta a la mesa en ese orden.",
    removePlayer = "Quitar jugador",
    mafiaLabel = "Mafia",
    recommended = "recomendado",
    revealLabel = "Mostrar el papel de los muertos",
    talkLabel = "Debate de cada día",
    minutes = { "$it min" },
    noTimer = "Sin tiempo",
    rolesSummary = { m, det, doc, v ->
        listOfNotNull("$m Mafia", "Detective".takeIf { det }, "Médico".takeIf { doc }, if (v == 1) "1 Aldeano" else "$v Aldeanos").joinToString(" · ")
    },
    roleName = {
        when (it) {
            Role.MAFIA -> "Mafia"
            Role.DETECTIVE -> "Detective"
            Role.DOCTOR -> "Médico"
            Role.VILLAGER -> "Aldeano"
        }
    },
    roleHint = {
        when (it) {
            Role.MAFIA -> "Cada noche, elige una víctima con tus compañeros. De día, finge que eres un Aldeano."
            Role.DETECTIVE -> "Cada noche, descubre si un jugador es de la Mafia. Cuidado a quién se lo cuentas."
            Role.DOCTOR -> "Cada noche, protege a un jugador de la Mafia. Puedes ser tú."
            Role.VILLAGER -> "Sin poderes, solo tus ojos y tu voz. Encuentra a la Mafia y échala."
        }
    },
    cardOf = { i, n -> "Carta $i de $n" },
    tapToFlip = { "$it, toca la carta para ver tu papel" },
    memorize = "Memorízalo y escóndelo",
    hiddenPass = "Carta escondida. Pasa el móvil.",
    hideCard = "Esconder carta",
    passTo = { "Pasar a $it" },
    nightFalls = "Cae la noche",
    youAre = "Eres",
    fellowMafia = { "Tus compañeros: $it" },
    nightTitle = { "Noche $it" },
    nightIntro = listOf(
        "Todos cogen el móvil por turnos, también los Aldeanos, para que nadie sepa quién hizo qué.",
        "Pon cara de póquer y tómate tu tiempo.",
        "Los muertos se callan.",
    ),
    passRound = "Pasar el móvil",
    passPhoneTo = { "Pasa el móvil a $it" },
    progressOf = { i, n -> "$i de $n" },
    imName = { "Soy $it" },
    actionQuestion = {
        when (it) {
            Role.MAFIA -> "¿A quién elimina la Mafia esta noche?"
            Role.DETECTIVE -> "¿A quién investigas?"
            Role.DOCTOR -> "¿A quién proteges esta noche?"
            Role.VILLAGER -> "¿De quién sospechas más?"
        }
    },
    actionHint = {
        when (it) {
            Role.MAFIA -> "La víctima es el jugador más elegido. En un empate, cuenta la primera elección."
            Role.DETECTIVE -> "Sabrás si es de la Mafia."
            Role.DOCTOR -> "A cualquiera, también a ti, pero no al mismo jugador dos noches seguidas."
            Role.VILLAGER -> "Nadie lo verá hasta el final de la partida. Sirve para que todos estén el mismo tiempo con el móvil."
        }
    },
    partnerPicked = { who, target -> "$who eligió a $target" },
    noPicksYet = "Tus compañeros aún no han elegido",
    protectedLastNight = "protegido anoche",
    confirm = "Confirmar",
    doneText = "Listo. Tu elección está hecha.",
    checkIsMafia = { "¡$it ES de la Mafia!" },
    checkNotMafia = { "$it no es de la Mafia." },
    hideAndPass = { "Esconder y pasar a $it" },
    hideAndWake = "Esconder y dejar el móvil en el centro",
    morningTitle = { "Mañana del día $it" },
    morningIntro = "¡Despertad todos! ¿Qué ha pasado esta noche?",
    reveal = "Descubrir",
    killedLine = { "$it murió durante la noche" },
    nobodyDied = "Nadie murió anoche",
    savedLine = "El Médico llegó a tiempo.",
    wasRole = { "Papel: $it" },
    startDay = "Empezar el día",
    dayTitle = { "Día $it" },
    dayHint = "Hablad. Acusad, defendeos, tirad faroles. Después votáis todos a la vez.",
    timeUp = "¡Se acabó el tiempo!",
    plusMinute = "+1 min",
    toVote = "Ir a la votación",
    aliveLabel = { "Vivos ($it)" },
    outLabel = "Fuera",
    voteTitle = "La votación",
    voteHow = "Contad hasta tres y todos señalan a un jugador a la vez. Sale quien tenga más dedos señalándole. En un empate, no sale nadie.",
    whoIsOut = "¿Quién tuvo más votos?",
    nobodyTie = "Nadie: hay empate",
    confirmOut = { "$it sale" },
    confirmNobody = "No sale nadie",
    outLine = { "$it queda fuera del juego" },
    noOutLine = "Hoy no sale nadie",
    nextNight = "Vuelve a caer la noche",
    townWins = "¡Gana el pueblo!",
    mafiaWins = "¡Gana la Mafia!",
    townWinsText = "Todos los miembros de la Mafia han sido descubiertos.",
    mafiaWinsText = "La Mafia ya es tanta como todos los demás.",
    rolesTitle = "Papeles",
    story = "La historia de la partida",
    storyNight = { "🌙 Noche $it" },
    storyDay = { "☀️ Día $it" },
    storyMafia = { "La Mafia fue a por $it" },
    storyDoctor = { "El Médico protegió a $it" },
    storyDetective = { who, mafia -> "El Detective investigó a $who: " + if (mafia) "¡Mafia!" else "no es de la Mafia" },
    storyKilled = { "$it murió" },
    storySaved = "Nadie murió",
    storySuspects = { "Los Aldeanos sospecharon de: $it" },
    storyOut = { "El pueblo echó a $it" },
    storyNobodyOut = "No echaron a nadie",
    newGame = "Nueva partida",
    editPlayers = "Cambiar los jugadores",
    leaveTitle = "¿Salir de la partida?",
    leaveText = "Se perderá la partida en curso.",
    leave = "Salir",
    stay = "Seguir jugando",
    back = "Volver",
    close = "Cerrar",
    continueLabel = "Continuar",
    rulesButton = "Reglas",
    musicButton = "Activar o desactivar la música",
    rulesTitle = "Cómo se juega",
    rulesSections = listOf(
        RulesSection(
            "🎯", "El objetivo",
            listOf(
                "Algunos jugadores son, en secreto, la Mafia. Todos los demás son el pueblo.",
                "El pueblo gana si echa a todos los miembros de la Mafia.",
                "La Mafia gana si elimina gente del pueblo hasta ser tantos como todos los demás.",
            ),
        ),
        RulesSection(
            "🃏", "Los papeles",
            listOf(
                "🎩 Mafia: se conocen entre sí. Cada noche eligen una víctima.",
                "🔍 Detective: cada noche descubre si un jugador es de la Mafia.",
                "💊 Médico: cada noche protege a un jugador. Puede ser él mismo, pero no al mismo jugador dos noches seguidas.",
                "🏡 Aldeanos: sin poderes. Tienen que descubrir a la Mafia hablando.",
                "Más o menos uno de cada cuatro jugadores es de la Mafia (5–6 jugadores: 1, 7–9: 2, 10–12: 3). Puedes cambiarlo, y quitar al Detective y al Médico.",
                "Cada jugador gira su carta en el móvil, la memoriza, la esconde y pasa el móvil.",
            ),
        ),
        RulesSection(
            "🌙", "La noche",
            listOf(
                "El móvil da la vuelta a la mesa y todos los jugadores vivos lo cogen, sea cual sea su papel. Quédate con él más o menos el mismo tiempo, seas quien seas.",
                "La Mafia elige una víctima. Cada uno ve lo que eligieron sus compañeros antes. La víctima es el jugador más elegido; en un empate, cuenta la primera elección.",
                "El Médico elige a quién protege y el Detective a quién investiga.",
                "Los Aldeanos eligen de quién sospechan más. Nadie lo ve hasta la historia del final de la partida.",
            ),
        ),
        RulesSection(
            "☀️", "La mañana",
            listOf(
                "El móvil dice a todos quién murió durante la noche, o que nadie murió porque el Médico protegió a la víctima.",
                "Si la opción está activada, muestra el papel de quien murió.",
                "Los muertos están fuera: se callan hasta el final y ya no cogen el móvil.",
            ),
        ),
        RulesSection(
            "🗣️", "El día",
            listOf(
                "Todos hablan: acusan, se defienden, tiran faroles. El cronómetro hace que el día no se alargue.",
                "Después contad hasta tres y todos señalan a un jugador a la vez.",
                "Sale quien tenga más dedos señalándole. En un empate, no sale nadie.",
                "Toca a ese jugador en el móvil, y vuelve a caer la noche.",
            ),
        ),
        RulesSection(
            "🏆", "Quién gana",
            listOf(
                "El pueblo gana en cuanto no quede nadie de la Mafia.",
                "La Mafia gana en cuanto sea tanta como el resto de jugadores vivos.",
                "Al final, el móvil muestra todos los papeles y la historia de cada noche.",
            ),
        ),
        RulesSection(
            "💡", "Consejos",
            listOf(
                "Mafia: compórtate exactamente como un Aldeano. Acusa, pero sin pasarte, y no defiendas a tus compañeros a las claras.",
                "Detective: la Mafia irá a por ti en cuanto hables. Elige bien el momento.",
                "Fíjate en los votos: quién salvó a quién, quién cambió de idea en el último momento.",
            ),
        ),
    ),
)
