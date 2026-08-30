# FlexBoard Pro

Native Android keyboard app (IME) built with Kotlin + Jetpack Compose for the
settings UI and a custom `View`-based key grid for the keyboard surface.

## Module layout
- `app/src/main/java/com/flexboard/ime/` — IME service, key view, auto-typer,
  accessibility helper.
- `app/src/main/java/com/flexboard/ui/` — Compose Material3 settings app,
  per-screen files in `ui/screens/`.
- `app/src/main/java/com/flexboard/utils/` — themes, fonts, clipboard watcher,
  prefs store.
- `app/src/main/java/com/flexboard/data/db/` — Room DB (clipboard items,
  saved sentences).
- `app/src/main/assets/fonts/` — bundled `.ttf` files for the built-in font
  picker (loaded by `BuiltInFonts.kt`).

## How the built-in fonts work
`BuiltInFonts.ALL` is the source of truth. Each entry has:
- `assetPath` — `fonts/<file>.ttf` for asset-backed faces, `system:<family>`
  for system family aliases (e.g. `system:serif`), or `""` for default Roboto.
- `style` — `Typeface.NORMAL` / `BOLD` / `ITALIC` / `BOLD_ITALIC`, applied on
  top of the loaded face.
- `path` (computed) — `"asset:fonts/X.ttf|<style>"` or `"system:family|<style>"`.
  Style is **baked into the path** so two entries that share a `.ttf` file
  (Bold + Italic of the same family) get unique identifiers and the
  Theme picker radio button highlights only the actual selection.

Add a font: drop the `.ttf` into `assets/fonts/`, append a `BuiltInFont(...)`
entry to `BuiltInFonts.ALL`. Backwards compat: legacy paths without the
`|<style>` suffix resolve to the first matching asset.

## Auto-typer safety
`AutoTypeEngine.trySend` refuses to fire keystrokes / pointer clicks when:
1. No IME injector is bound (FlexBoard is not the active keyboard in a
   real input field), OR
2. The current foreground package is the system UI, a known launcher, or
   FlexBoard itself (`shouldBlockSendForPackage`).

`MainActivity.onStop` / `onResume` / `onDestroy` HARD-STOP the typer
(cancels its coroutine + tears down the foreground service) whenever the
user leaves the chat app outside the brief Start-window — this prevents
the floating-pointer click from landing on the home screen.

## Clipboard de-duplication
`ClipboardWatcher` ignores a clipboard change when:
- the same text was already saved in the last 1.5 s (handles OEM-double
  listener fires), OR
- the most recent DB row already has identical text (post-process-restart
  guard), OR
- a UI re-copy path called `ClipboardWatcher.suppressNext(text)` right
  before its own `cm.setPrimaryClip(...)`.

Any new "copy this string back to the system clipboard" UI MUST call
`suppressNext(text)` before `setPrimaryClip` to avoid creating a
duplicate history row.

## Backspace behaviour
Touch handler in `KeyboardView` arms a `Handler`-driven repeat loop on
`ACTION_DOWN` and cancels it on `ACTION_UP` / `ACTION_CANCEL`. Initial
350 ms grace, then deletes accelerate from 80 ms → 25 ms per repeat.

## Recent changes (Apr 2026)
- Added Jameel Noori Nastaleeq Urdu font + Shade Blue decorative font.
- Rewrote BuiltInFonts so style-variant entries have unique paths.
- Replaced 3-dot menu icon with hamburger (≡) in MainActivity top bar.
- Backspace continuous delete with acceleration (replaces long-press menu).
- AutoTyper hard-stops on minimize + foreground package guard.
- Clipboard watcher dedupes within 1.5 s + supports manual `suppressNext`.

## v1.9 — security hardening + device-ID approval system
Implements `SECURITY_AND_APPROVAL_PROMPT.txt` end-to-end. Two pillars:
**(A) tamper resistance** so the APK can't be casually rebranded and
re-distributed, and **(B) device-ID gate** so only Android IDs on the
developer's GitHub-hosted `Users.json` can use the keyboard.

### New module: `com.flexboard.security`
- **`Obf.kt`** — runtime XOR/Base64 decryption of the obfuscated
  string blobs. The XOR key is `SHA-256("flexboard_obf_v1::" + pkg +
  "::" + signingCertSha)` so a repackaged APK (different cert) derives
  a different key → every `Obf.decode(...)` returns garbage. Skips
  decryption in dev builds (`IS_OBFUSCATED == false`).
- **`SecurityGuard.kt`** — `verifyOrDie(ctx)` checks: (1) signature
  pinning against `ObfConstants.EXPECTED_SIGNING_SHA256`, (2)
  `FLAG_DEBUGGABLE` not set in production, (3) no live debugger. In
  debug / unsigned builds it always returns true so dev isn't blocked.
- **`DeviceId.kt`** — stable fingerprint = `SHA-256(ANDROID_ID + "|"
  + MANUFACTURER + "|" + MODEL + "|" + DEVICE + "|" + BRAND)`,
  truncated to 16 hex chars. Short enough to dictate over WhatsApp,
  64-bit collision-resistant. Resets on factory reset (intentional —
  prevents one approval from covering multiple devices).
- **`ApprovalGate.kt`** — fetches the JSON via OkHttp, parses
  `approved` / `blocked` arrays (`{"android_id": "...", "name": "..."}`),
  caches the verdict in regular `SharedPreferences("flexboard_gate")`.
  Network rules: at most one fetch per 6 h; on Approved cache the
  network is skipped entirely; on failure falls back to cache up to 7
  days old; `blocked` always wins over `approved`. Exposes
  `cachedState()` + `isApprovedCached()` for the IME-side fast path
  and `evaluate()` (suspend) for the full check.

### New UI
- **`ui/screens/LockScreen.kt`** — full-screen Compose UI with
  FlexBoard Pro orange title, state-coloured message banner, the
  device ID in a big monospace tap-to-copy box, **[Copy ID]** button,
  green **[Send to <Owner> via WhatsApp]** button (opens `wa.me/...`
  with `Approval request%0AID: <id>` pre-filled), and orange
  **[Re-check approval]** button that forces a fresh fetch and unlocks
  immediately if the developer just added the ID upstream.
- **`ui/GatedApp.kt`** — wraps the existing `AppRoot` so every screen
  is only rendered after both `SecurityGuard.verifyOrDie` AND
  `ApprovalGate.evaluate` return Approved. Brief loading splash
  (orange ring) is shown while the first check runs.

### Wiring
- **`MainActivity.onCreate`** — `setContent { FlexboardTheme {
  GatedApp { AppRoot(...) } } }`. Tamper / approval failure routes
  the user to LockScreen.
- **`FlexboardIMEService.onCreateInputView`** — fast cached check via
  `ApprovalGate.isApprovedCached(this)` + `SecurityGuard.verifyOrDie`.
  Failure → returns a small "🔒 FlexBoard Pro is locked" TextView
  instead of the keyboard, tappable to launch MainActivity. The user
  can still long-press space to switch to a different IME.
- **`AboutScreen.kt`** — every brand-name string ("Kashif Raza", "ATF
  Team", Instagram URL, WhatsApp channel + community URLs, copyright
  line) now reads via `Obf.decode(ctx, ObfConstants.<...>)` with a
  hard-coded `ifBlank { ... }` fallback. The "100 % offline" line is
  reworded to "Internet used only for licence check".
- **`AndroidManifest.xml`** — new `INTERNET` and
  `ACCESS_NETWORK_STATE` permissions (required by ApprovalGate).

### Build infrastructure
- **`app/build.gradle.kts`** — added `generateObfConstants` task that
  reads the configured release keystore via `java.security.KeyStore`,
  computes its cert SHA-256, derives the XOR key, encrypts every
  branded string, and writes
  `build/generated/source/obf/com/flexboard/security/ObfConstants.kt`
  before each Kotlin compile. The generated file's source dir is
  added to the main source set. Also enabled `isMinifyEnabled = true`
  and `isShrinkResources = true` on the release variant. Added
  `com.squareup.okhttp3:okhttp:4.12.0` dep. Bumped `versionCode` to 3
  / `versionName` to "1.9.0".
- **`proguard-rules.pro`** — full rewrite with named-symbol keeps for
  every manifest-loaded entry point, the security module's public
  surface, Room entities/DAOs, Compose runtime, and OkHttp/TLS dontwarn
  rules so R8 doesn't break Compose or coroutines.

### Operational notes
- The owner uploads `Users.json` (sample at repo root) to
  `https://github.com/nobita1366/keyboard-approve-users` on the `main`
  branch. Format:
  ```json
  { "version": 1, "approved": [ { "android_id": "...", "name": "..." } ], "blocked": [] }
  ```
  Adding a new ID + commit is enough — every device re-checks within
  6 h (or sooner if the user taps Re-check). Removing an ID locks the
  user out within the same window.
- The approval URL is **not** hard-coded as a plaintext string in the
  release APK — it lives only as the encrypted XOR blob inside
  `ObfConstants.APPROVAL_URL` and is reconstructed at runtime using
  the signing cert SHA. `strings *.apk | grep raw.githubusercontent`
  on a release build returns nothing useful.
- The release keystore SHA-256 is computed automatically by Gradle at
  build time — there is no manual paste step. The Actions workflow
  already decodes `KEYSTORE_BASE64` before running `assembleRelease`,
  so as long as that secret is set, the obfuscation is fully bound to
  the real release cert.

## v1.8.1 — hotfix for keyboard-open crash
- **Crash on every `onCreateInputView()`**: v1.8 added two new
  `mutableListOf<...>` properties (`pendingKeyImageBindings` and
  `keyImageBindings`) but DECLARED them at the bottom of the class —
  below the `init { ... rebuild() }` block. Kotlin runs property
  initializers in declaration order interleaved with init blocks, so
  at the moment `rebuild()` was first called from the init block both
  lists were still `null`, and the new `pendingKeyImageBindings.clear()
  / keyImageBindings.clear()` calls at the top of `rebuild()` blew
  up with `NullPointerException: Attempt to invoke interface method
  'void java.util.List.clear()' on a null object reference` — the IME
  refused to show. (Same gotcha that bit `charKeyViews` in v1.7 — the
  comment above `charKeyViews` literally warns about this.)
- **Fix**: moved both list declarations next to `charKeyViews`,
  ABOVE the `init` block, with a fat warning comment so we don't
  step on this rake a third time.

## v1.8 — single continuous wallpaper across all keys
- **`BgImageKeyDrawable` no longer center-crops per key**: the v1.6
  implementation painted ONE center-cropped copy of the user's wallpaper
  inside every key — so the same image showed up 30+ times stamped
  across the keyboard (the user's exact complaint: "har key me alag pic
  lagti ha"). Now the drawable holds the full keyboard size + this
  key's offset within it (`setKeyboardWindow(kbW, kbH, keyX, keyY)`)
  and uses a single shared bitmap-shader matrix:
  `scale = max(kbW/bmpW, kbH/bmpH); translate = (centeringOffset - keyXY)`.
  The result: ONE continuous wallpaper across the whole keyboard, each
  key acts as a transparent rounded "window" cut out of it. The label
  text still draws on top.
- **Position feeding** (new `applyContinuousKeyWallpaper()` in
  `KeyboardView` + `pendingKeyImageBindings` / `keyImageBindings`
  lists): we collect every per-key bitmap drawable as the rows are
  built, then `post()` after `rebuild()` to walk them once layout
  settles each key's `left/top`. Uses
  `offsetDescendantRectToMyCoords(view, rect)` to translate each key's
  local origin into the KeyboardView's coordinate space. Both the
  normal-state and pressed-state drawables get the same window so the
  wallpaper stays aligned across taps. Both lists are cleared at the
  start of each rebuild so stale bindings can't leak.
- **Fallback**: until the post-layout call lands the drawable falls
  back to the v1.7 per-key center-crop so there's no blank flash on
  first frame.

## v1.7 — true Gboard 3D look + key spacing slider + custom font icon
- **Gboard-style 3D keys** (`KeyboardView.rebuild` + `makeKeyBackground` +
  `BgImageKeyDrawable.getOutline`): each key is now `View.elevation = 3 dp`
  with `outlineProvider = ViewOutlineProvider.BACKGROUND`, so Android
  renders a real soft drop-shadow under every key. The pill background is
  no longer a flat colour — `makeKeyBackground` now returns a vertical
  `GradientDrawable` (top ~10 % lighter, bottom ~12 % darker) which
  produces the soft glossy "soap bar" look from the Gboard reference
  screenshot. Corner radius bumped 8→12 dp for a softer pill profile.
  `BgImageKeyDrawable.getOutline` was added so the same elevation shadow
  hugs the rounded shape even in per-key bg image mode.
- **Press-IN animation (corrected direction)**: previous v1.6 lifted
  keys UP on press (translationZ +4 dp) which felt backwards. Now keys
  PRESS INTO the surface — `scale 1.0 → 0.96` and
  `translationZ → -restElevation` on ACTION_DOWN (60 ms decelerate),
  spring back on UP/CANCEL with `OvershootInterpolator(1.6, 120 ms)`.
- **Key spacing slider** (new `SettingsStore.KEY_KEY_MARGIN_DP` + Theme
  screen slider, range 1..8 dp, default 3 dp): replaces the hard-coded
  6 dp gap from v1.6 that made keys *appear* smaller because each one
  lost ~12 dp of width to inter-key gaps. Default 3 dp now matches the
  tight Gboard packing in the user's reference screenshot.
- **3D shadow toggle** (new `SettingsStore.KEY_KEY_3D_SHADOW`, default ON):
  lets users disable the elevation shadow on devices where it conflicts
  with their custom theme (or for the rare battery-conscious user — flat
  keys skip the GPU shadow rasterisation pass).
- **New generated font-picker icon** (`drawable-nodpi/ic_font_picker_v2.png` —
  glossy 3D gradient "A" with sparkle accent, generated via
  `generateImage` + transparent background): replaces the simple "Aa"
  vector. Toolbar now uses `tintIcon = false` for this icon so its
  gradient survives instead of being flattened to the theme text colour.
- All v1.7 prefs added to `FlexboardIMEService.prefsListener` so changes
  trigger a live keyboard rebuild without re-opening the IME.

## v1.6 — keys polish + missing emojis + sound + theme tile fix
- **Themes tile opens Theme tab directly** (`KeyboardView.allToolActions`,
  new `onOpenSection` callback wired in `FlexboardIMEService.onCreateInputView`):
  the tile previously called `onOpenSettings()` which always landed on Home.
  Now routes through `openSettingsTo("theme")`.
- **Apply background image to keys** (new `SettingsStore.KEY_BG_IMAGE_ON_KEYS` +
  `KeyboardView.BgImageKeyDrawable` + `bgBitmap` cache + Theme screen toggle):
  when on, the user's wallpaper paints on every key clipped to its rounded
  shape (center-crop via `BitmapShader` + `Matrix`). Pressed state stacks a
  translucent dark overlay on top. Defaults OFF — additive feature.
- **Bigger keys + 3D Gboard-style press animation** (`KeyboardView.rebuild`):
  default key height 50→58 dp (range 44..90 dp), per-key margin 4→6 dp,
  text size 16→17 sp. Touch listener animates scale 1.0→0.92 + lift
  `translationZ +4 dp` on DOWN (DecelerateInterpolator, 70 ms) and springs
  back with OvershootInterpolator on UP/CANCEL (120 ms). Cancels in-flight
  animators so burst-typing doesn't pile interpolators.
- **Custom font-picker icon** (new `res/drawable/ic_font_picker.xml` +
  `ToolAction.iconRes` + ImageView path in `addToolbarButton`/`buildToolTile`):
  replaces the 🔤 emoji (rendered as tofu on several Android skins).
  Vector "Aa" + underline graphic recolored at runtime via
  `ImageView.setColorFilter(theme.keyText)` so it matches the active theme.
- **Missing emojis added** (`EmojiData.kt` rewritten): now includes the four
  glyphs the user explicitly asked for — 🫪 🙂‍↔️ 🥹 🙂‍↕️ — plus a
  comprehensive sweep of Unicode 13/14/15/16 additions across every
  category.
- **Click sound — reliable playback + volume slider** (`KeyboardView.doFeedback`,
  Theme screen): switched to `AudioManager.playSoundEffect(FX_KEYPRESS_STANDARD, vol)`
  with view-level click as fallback. Theme screen exposes a 5–100 % volume
  slider (visible when Click sound is on). Honours `KEY_SOUND` and
  `KEY_SOUND_VOLUME` prefs.
- **Notification permission prompt** (`MainActivity.onCreate`): on Android 13+
  (Tiramisu) the app requests `POST_NOTIFICATIONS` so the auto-typer
  foreground service can keep its persistent notification visible. Wrapped
  in try/catch — best-effort, never blocks app launch.

## Build
Standard Android Gradle build. No build workflow in this Replit env (no
Android SDK present); validate via static review, build/install on device
with Android Studio or `./gradlew installDebug`.
