package com.flexboard.utils

import android.content.Context
import android.content.SharedPreferences

object SettingsStore {
    private const val NAME = "flexboard_settings"

    fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // Theme
    const val KEY_THEME = "theme_id"
    const val KEY_KEY_BG = "color_key_bg"
    const val KEY_KEY_TEXT = "color_key_text"
    const val KEY_KB_BG = "color_kb_bg"
    const val KEY_SUGG_BG = "color_sugg_bg"
    const val KEY_PRESSED = "color_pressed"
    const val KEY_BORDER_STYLE = "border_style"
    const val KEY_KEY_OPACITY = "key_opacity"
    const val KEY_BG_IMAGE_URI = "bg_image_uri"
    const val KEY_BG_IMAGE_OPACITY = "bg_image_opacity"
    const val KEY_BG_SHOW_BORDERS = "bg_show_borders"   // true = show key boxes over bg image, false = transparent
    // When true, the same custom bg image is also applied as the background
    // of every individual key (each key shows the corresponding portion of
    // the keyboard image). User-requested on/off toggle in Theme settings.
    const val KEY_BG_IMAGE_ON_KEYS = "bg_image_on_keys"

    // Keyboard
    const val KEY_HAPTIC = "haptic_enabled"
    const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
    const val KEY_SOUND = "sound_enabled"
    const val KEY_SOUND_VOLUME = "sound_volume"
    const val KEY_KB_HEIGHT_PCT = "kb_height_pct"
    const val KEY_KEY_HEIGHT_DP = "key_height_dp"
    // v1.7 — per-key spacing slider. Default 3 dp gives a tight Gboard-like
    // packed look; user can dial up to 8 dp for more separation. Replaces
    // the hard-coded 6 dp from v1.6 which was making keys look small.
    const val KEY_KEY_MARGIN_DP = "key_margin_dp"
    // v1.7 — toggle baseline 3D shadow under each key (Gboard-style).
    // ON = soft elevation drop-shadow at rest, key "presses in" on tap.
    // OFF = flat keys (slightly faster, matches the v1.5 look).
    const val KEY_KEY_3D_SHADOW = "key_3d_shadow"
    const val KEY_KEY_TEXT_SIZE = "key_text_size"
    const val KEY_FONT_PATH = "custom_font_path"
    const val KEY_FONT_BOLD = "font_bold"
    const val KEY_FONT_ITALIC = "font_italic"
    const val KEY_FONTS_LIST = "fonts_list_json"
    // Text shown on the wide space key. Blank means use the built-in
    // branding label from the obfuscated build constants.
    const val KEY_SPACE_LABEL = "space_key_label"
    // Unicode style applied to every typed character so the OUTPUT in chat
    // apps actually changes ("Aa" picker). TTF fonts only restyle the keys
    // on FlexBoard's own keyboard — they cannot affect text inside other
    // apps. This is the real fix for "fancy text in any chat app".
    const val KEY_FONT_STYLE = "font_unicode_style"   // id from UnicodeFonts.STYLES, e.g. "normal", "bold_script"
    // Layout order — when true, suggestion row sits ABOVE the toolbar
    // (Gboard-style). Default true; user can flip in Theme settings.
    const val KEY_SUGGESTIONS_ON_TOP = "suggestions_on_top"
    const val KEY_LANGUAGE = "current_language"
    // When true, the Enter key ALWAYS inserts \n (legacy behavior).
    // When false (NEW default), Enter is "smart":
    //   - Multi-line / IME_ACTION_NONE / IME_ACTION_UNSPECIFIED  → newline
    //     (chat apps like WhatsApp / Messenger / Telegram fall here)
    //   - Otherwise (browser address bar, search box, single-line "Done"
    //     fields, etc.) → fire the field's IME action (real Enter press).
    //   - LONG-PRESS on Enter → always inserts a literal \n no matter what.
    const val KEY_ENTER_NEWLINE = "enter_always_newline"

    // Auto-Type
    const val KEY_AT_DELAY = "autotype_delay"
    const val KEY_AT_LOOP = "autotype_loop"
    const val KEY_AT_SEND_MODE = "autotype_send_mode"  // direct | paste
    const val KEY_AT_LAST_FILE = "autotype_last_file"
    const val KEY_AT_LAST_FILE_NAME = "autotype_last_file_name"
    const val KEY_AT_START_LINE = "autotype_start_line"
    const val KEY_AT_AUTO_SEND = "autotype_auto_send"
    const val KEY_AT_CUSTOM_TEXT = "autotype_custom_text"
    const val KEY_AT_SEND_DELAY_MS = "autotype_send_delay_ms"   // delay between commit and send (default 300)
    const val KEY_AT_SEND_METHOD = "autotype_send_method"       // auto | ime | accessibility | pointer
    const val KEY_AT_CHAR_DELAY_MS = "autotype_char_delay_ms"   // ms between each typed character in DIRECT mode (default 35)
    // v1.10 — optional "Target name" prefix written before every auto-typed
    // line. When non-blank, AutoTypeEngine.start() prepends "$targetName "
    // (with a single trailing space) to each line. Blank / unset = no
    // prefix, behaviour matches earlier builds. Use case: addressing each
    // chat reply to a specific person, e.g. setting target=Raza turns the
    // line "kaisa hai" into "Raza kaisa hai".
    const val KEY_AT_TARGET_NAME = "autotype_target_name"

    // Pointer overlay
    const val KEY_POINTER_ENABLED = "pointer_enabled"
    const val KEY_POINTER_X = "pointer_x"
    const val KEY_POINTER_Y = "pointer_y"
    const val KEY_POINTER_LOCKED = "pointer_locked"     // when true, dot ignores touches (just visual)

    // Suggestions / Auto-save sentence
    const val KEY_SUGGESTIONS = "suggestions_enabled"
    const val KEY_AUTOCORRECT = "autocorrect_enabled"
    const val KEY_AUTO_SAVE_SENTENCE = "auto_save_sentence"
    const val KEY_AUTO_SAVE_MIN_LEN = "auto_save_min_len"
    // Auto-capitalize the first letter of each sentence: ON when the cursor
    // is at the start of an empty field, right after a sentence terminator
    // (`.` `?` `!` `۔`) followed by space, or right after a newline. Default
    // ON — matches Gboard / SwiftKey / iOS behaviour the user expects.
    const val KEY_AUTO_CAPS = "auto_capitalize"

    // Clipboard
    const val KEY_CLIP_AUTO_DELETE_DAYS = "clip_auto_delete_days"

    // Emoji
    const val KEY_EMOJI_RECENT = "emoji_recent"

    // ===== v1.11 — Trial + Plan gating =====
    // Local 3-day trial persistence keys (TrialManager 3-layer anti-reset).
    // KEY_TRIAL_START_MS — epoch-ms when the trial started on this device.
    // KEY_TRIAL_SIG      — HMAC/SHA-256 signature binding the timestamp to
    //                      this device (signing cert SHA + android_id). If
    //                      the sig is missing or mismatched the trial is
    //                      pinned to "already expired" to deter spoofing.
    const val KEY_TRIAL_START_MS = "trial_start_ms_v11"
    const val KEY_TRIAL_SIG      = "trial_sig_v11"

    // v1.12 — Trial is now MANUALLY granted via the "trial" array in
    // Users.json (android_id + trial_until). Auto local trial removed.
    // KEY_TRIAL_UNTIL_MS caches the epoch-ms of trial_until from the last
    // successful network fetch. 0 = not in trial list, -1 = blocked/expired.
    const val KEY_TRIAL_UNTIL_MS = "trial_until_ms_v12"

    // Cached plan_until (epoch-ms, 0 if no plan set on GitHub side, -1 if
    // user is not in approved list). Filled in by ApprovalGate on every
    // successful evaluate(). Used by AccessGate so the IME can decide
    // synchronously whether to show the keyboard or the lock view.
    const val KEY_PLAN_UNTIL_MS = "plan_until_ms_v11"

    // WhatsApp community popup — true when the user ticked "Don't show
    // again". Default false → popup fires on every MainActivity launch
    // until the user dismisses it permanently.
    const val KEY_WA_POPUP_HIDDEN = "wa_popup_hidden_v11"

    // When true, AutoTypeScreen should immediately launch the file picker on
    // opening — set by the toolbar "File" tile in the IME so the user lands
    // directly in the file browser without a manual tap.
    const val KEY_AUTO_PICK_FILE = "auto_pick_file_v12"

    // Math / Leet style — separate from the Unicode font styles above.
    // KEY_MATH_STYLE_ON  : true when the user has a math/leet style active.
    // KEY_MATH_STYLE_ID  : which leet style is selected (e.g. "leet_pro").
    // KEY_PREV_FONT_STYLE: the KEY_FONT_STYLE value saved before math mode
    //                      was turned on so it can be restored on turn-off.
    const val KEY_MATH_STYLE_ON   = "math_style_on"
    const val KEY_MATH_STYLE_ID   = "math_style_id"
    const val KEY_PREV_FONT_STYLE = "prev_font_style"

    // Key press animation style. Default "press_in" (Gboard-style scale-down).
    // See KeyboardView.applyKeyPressAnim for the full list of style IDs.
    const val KEY_KEY_ANIMATION = "key_animation"

    // Last name the user typed in the Plans purchase flow. Pre-fills the
    // text field on subsequent visits so they don't have to retype on
    // every plan switch / re-purchase.
    const val KEY_LAST_BUYER_NAME = "last_buyer_name_v11"

    // v1.10 — first-run defaults flag. Bumping the suffix forces a re-apply
    // of the curated default theme + sizing settings (pastel-blue background,
    // 71% key opacity, 56dp keys, 1dp spacing, 3D shadow, etc.) for users
    // who installed older builds. Once applied we set this to true so we
    // never overwrite the user's customisations later.
    // Bumped suffix → "_b" so users who already ran the first v1.10 build
    // (which defaulted to the dark theme) get the new Rose Gold default
    // re-applied on next launch. Re-applying the picture/keys values that
    // were already correct is a harmless no-op. Bump again whenever the
    // curated default set in FlexboardApp.applyDefaultsOnFirstRun changes.
    const val KEY_DEFAULTS_APPLIED = "defaults_v10_applied_b"
}
