# Mafia

The classic party game of the town and the Mafia, for 5 to 16 players sharing **one phone**. The phone is the
narrator: nobody has to sit out to run the game.

> Mafia is a traditional party game (also known as Werewolf). This app is an independent implementation;
> its texts and artwork are original.

## How it plays

- Add the players in the order they sit, choose the number of Mafia (about one in four is recommended) and
  whether to play with a **Detective** and a **Doctor**. Then deal: each player secretly flips a face-down card,
  hides it and passes the phone on. The Mafia also see who their partners are.
- **Night.** The phone goes round the table and *every* living player takes it, whatever their role, so holding
  the phone gives nothing away. The Mafia pick a victim (each one sees what their partners picked before them;
  the most-picked player is the victim, a tie goes to the first pick). The Doctor protects one player (themselves
  too, but never the same player two nights running). The Detective learns whether one player is Mafia.
  Villagers say who they suspect most; nobody sees that until the end.
- **Morning.** The phone reveals who was killed in the night, or that nobody died because the Doctor got there
  first. Optionally it shows the dead player's role.
- **Day.** Everybody talks, with a timer (2, 3 or 5 minutes, or none). Then on three everybody points at once; the
  player with the most fingers is out (a tie: nobody), and someone taps them on the phone. Night falls again.
- **Winning.** The town wins when no Mafia are left. The Mafia win as soon as they are as many as everybody else.
  The end screen shows every role and the story of each night.

| Players | 5–6 | 7–9 | 10–12 | 13–15 | 16 |
|---|---|---|---|---|---|
| Recommended Mafia | 1 | 2 | 3 | 4 | 5 |

## Features

- English, Portuguese and Spanish, with an in-app rules page.
- No internet, no permissions, no data collected.
- Window secure flag on by default, so roles and night choices do not show up in screenshots or the recent-apps thumbnail.
- The game is saved at the start of each night and day; an interrupted game can be resumed from the setup screen.
- Sound effects and music made with ElevenLabs; the same neutral sound for every night choice, so nothing leaks through audio.

## Sound and music

The sound effects and the music were generated with [ElevenLabs](https://elevenlabs.io) from the prompts in
`tools/audio.json`. `tools/audio.py` regenerates them into `app/src/main/res/raw/` (it needs an ElevenLabs API key in
`$ELEVENLABS_API_KEY` or `~/.config/elevenlabs/api_key`; paid generations are cached in `tools/audio-cache/`, which is
not in git, so only changed prompts are generated again). Effects are trimmed and levelled; music is made into a
seamless loop.

Music follows the game: a noir jazz tune in the menu and the deal, a quiet tense one at night, a swing one by day, and
silence for the final sting.

The 🎵 button on the first screen turns the music off. The sound effects stay on: they are part of the game.

## Build

Requires JDK 17 and the Android SDK (platform 35).

```
./gradlew testDebugUnitTest        # rules tests
./gradlew assembleRelease          # app/build/outputs/apk/release/app-release.apk
./gradlew assembleDebug -PallowScreenshots   # variant without the secure window flag, for screenshots
```

The release build is signed with the debug key, which is fine for sideloading.

## Layout

| Path | What |
|---|---|
| `app/src/main/java/com/zepinto/mafia/Rules.kt` | Roles, night and day rules, win check, saved-game codec (plain Kotlin, unit tested) |
| `.../GameViewModel.kt` | Game state machine; the night's secret choices never reach the UI state |
| `.../Screens.kt`, `PlayScreens.kt`, `Theme.kt` | Compose UI and look |
| `.../Strings.kt` | All texts in the three languages, including the rules page |
| `.../Sfx.kt` | Sound effects and music player |
