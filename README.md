# Boka — Mobile (Compose Multiplatform)

Native **Android + iOS** app for the Boka chess platform, built with **Kotlin Multiplatform + Compose Multiplatform** (one shared UI codebase). It talks to the *existing* backend — no new server work needed.

- **Auth / payments:** `https://brivont.co.ke/chessapp/processor/`
- **Game & analysis API:** `https://brivont.co.ke/chess-api`
- **Live game WebSocket:** `wss://brivont.co.ke/chess-socket/`

All three are centralised in `composeApp/src/commonMain/kotlin/ke/co/brivont/boka/core/Core.kt` (`Config`).

## Project layout
```
composeApp/
  src/commonMain/kotlin/ke/co/brivont/boka/
    App.kt                     # root + navigation
    core/Core.kt               # config, Ktor client, token session, key-value store (expect)
    core/JwtUser.kt            # recover user id/name from the stored JWT on cold start
    data/Models.kt             # @Serializable DTOs
    data/Api.kt                # AuthApi + GameApi (REST)
    data/GameSocket.kt         # live game / coach WebSocket
    ui/theme/Theme.kt          # Boka dark+gold palette & typography
    ui/board/ChessBoard.kt     # FEN renderer + tap-to-move
    ui/*.kt                    # Auth, Dashboard, Leaderboard, MyGames, Game, Coach, Analysis
  src/androidMain/             # MainActivity, manifest, SharedPreferences store
  src/iosMain/                 # MainViewController, NSUserDefaults store
iosApp/                        # SwiftUI wrapper (needs Xcode project — see below)
```

## Feature status
| Feature | Status |
|---|---|
| Sign in / register (real backend) | ✅ working |
| Session persistence (token) | ✅ working |
| Dashboard / navigation | ✅ working |
| Leaderboard (global top + your rank) | ✅ working |
| My Games (history + open a game) | ✅ working |
| Game review / analysis (queue + poll + step-through) | ✅ working |
| Play Online (live multiplayer via WebSocket, tap-to-move + legal-move dots) | ✅ working (server-validated) |
| **Spectate** live games (lobby list + read-only live board) | ✅ `ui/SpectateScreen.kt` |
| **Auth token refresh** (WS 4001 + REST 401 → refresh & retry) | ✅ `AuthApi.refresh` |
| Boka theme (dark + gold) | ✅ |
| **On-device chess engine** (legal moves, make-move, check/mate, SAN) | ✅ `chess/Chess.kt` |
| **Professional board** (cburnett vector pieces, coords, gold highlights, check) | ✅ `ui/board/{ChessBoard,Pieces}.kt` |
| **Coach vs Stockfish** (full offline play, coaching, eval, move scrubbing) | ✅ working |
| Move scrubbing in **live** games (⏮ ‹ › Live) | ✅ `ui/GameScreen.kt` |
| **Openings study** (curated lines, SAN replay, ideas) | ✅ `ui/OpeningsScreen.kt` + `data/Openings.kt` |
| **Smooth board animation** (moved piece slides, no teleport) | ✅ `ui/board/ChessBoard.kt` |
| **Sounds** (move/capture/check via SoundPool) | ✅ Android `Sfx.android.kt` · iOS no-op (TODO) |
| **Notifications** ("your move" via our WebSocket, no Firebase) | ✅ Android `Notify.android.kt` (app backgrounded) · iOS no-op (TODO) |
| In-app subscription | 🔜 web checkout today; native billing needs Play Console (see below) |

## Run it
### Android
1. Open the `nativeAndroid/Boka` folder in **Android Studio** (Ladybug or newer, with the Kotlin Multiplatform plugin).
2. Let it sync. The Gradle wrapper is now committed (`./gradlew`, Gradle 8.9).
3. Pick the `composeApp` run config and run on a device/emulator. CLI: `./gradlew :composeApp:installDebug`.

### iOS (needs macOS + Xcode)
The Xcode project is now provided: **`iosApp/iosApp.xcodeproj`** (with a shared `iosApp` scheme). It has a *Compile Kotlin Framework* build phase that runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, so the `ComposeApp` framework is built automatically.
1. On a Mac with Xcode 15+, open `iosApp/iosApp.xcodeproj`.
2. Select the `iosApp` scheme + a simulator, set your signing team, and Run.
3. If Xcode ever refuses to open the hand-authored `project.pbxproj`, regenerate it via the Android Studio **KMP plugin** (New → project from this module) — the Swift entry, `Info.plist`, `Assets.xcassets`, and `MainViewController.kt` are all already in place.

> **iOS sounds** are currently a no-op (`Sfx.ios.kt`). To enable: add the WAVs to the iOS target's bundle and implement `playSfx` with `AVAudioPlayer` (or `AudioServicesCreateSystemSoundID`).

### Notifications — WebSocket-driven (no Firebase)
Implemented **without** Firebase: the game already holds a live WebSocket (`GameSocket`), so when an event arrives (opponent moved → your turn, or a match is found) and the app is **backgrounded**, we post a local system notification (`Notify.kt` / `Notify.android.kt`). `MainActivity` tracks foreground state (`AppForeground`) and requests `POST_NOTIFICATIONS` (Android 13+).

- **Covers:** you tab away / lock the screen mid-game while the process is still alive — the common case.
- **Does not cover:** a fully **killed** app (the socket is gone). For that you'd need either **FCM** *or* an Android **foreground service** holding the socket open (battery cost). Add one of those later if killed-app delivery is required.
- **iOS:** `Notify.ios.kt` is a no-op — implement with `UNUserNotificationCenter`.

### In-app subscription (planned — web checkout works today)
Premium upsell currently routes to the existing **web** payment flow (`App.kt` `onUpsell`), which is the simplest compliant path. For native billing:
- **Android:** add Google Play Billing, define the subscription product in Play Console, verify purchases against the .NET backend, and unlock Premium on the account. Requires a signed release uploaded to a Play testing track — not testable on a debug emulator.
- **iOS:** StoreKit 2 + App Store Connect products. Note Apple/Google take 15–30%; keep the web flow as the default where policy allows.

## Versions (adjust in Android Studio if it flags a mismatch)
Kotlin 2.1.0 · Compose Multiplatform 1.7.3 · AGP 8.7.3 · Ktor 3.0.3 · min SDK 26.

## Roadmap
Done: on-device engine · professional cburnett board · smooth slide animation · Coach vs Stockfish · move navigation (live + coach) · openings study · sounds (Android) · WebSocket "your move" notifications (Android) · iOS Xcode project.

Remaining:
1. **Native in-app subscription** (web checkout works today — see above).
2. **iOS parity**: sounds (`AVAudioPlayer`), local notifications (`UNUserNotificationCenter`), app icon / launch art.
3. Optional **killed-app push** via FCM or an Android foreground service (only if needed beyond the backgrounded-app case).
4. Reconnect/resume an in-progress game (`resume_game`) and spectating (`spectate`).

The architecture (config → api → socket → screens) is set up so each of these is an additive screen/module, not a rewrite.
