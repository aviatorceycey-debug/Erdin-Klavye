package com.erdin.klavye

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import androidx.core.view.ViewCompat
import androidx.customview.widget.ExploreByTouchHelper
import android.view.HapticFeedbackConstants
import kotlin.math.floor
import java.util.Locale

private data class KeyboardKey(
    val label: String,
    val value: String = label,
    val weight: Float = 1f,
    val kind: Kind = Kind.NORMAL
) {
    enum class Kind { NORMAL, ACTION, ACCENT, SPACE }
}


private data class KeyboardPalette(
    val background: IntArray,
    val normal: Int,
    val action: Int,
    val accent: Int,
    val space: Int,
    val accentOutline: Int,
    val spaceOutline: Int,
    val popup: Int,
    val selected: Int,
    val suggestion: Int
)

class ErdinKeyboardView(
    context: Context,
    private val onKey: (String) -> Unit,
    private val onSuggestion: (String) -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val suggestionMeasurePaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var keyTone: ToneGenerator? = null
    private fun playKeySound() {
        if (keyTone == null) keyTone = ToneGenerator(AudioManager.STREAM_SYSTEM, 35)
        keyTone?.startTone(ToneGenerator.TONE_PROP_BEEP, 28)
    }
    private fun upper(value: String) = value.uppercase(if (activeLanguageLabel == "English") Locale.US else Locale("tr", "TR"))
    // Resolve the theme once per frame; every key and popup shares this palette.
    private var activeDrawPalette: KeyboardPalette? = null
    private var cachedBackgroundShader: LinearGradient? = null
    private var cachedBackgroundWidth = -1
    private var cachedBackgroundHeight = -1
    private var cachedBackgroundColorsHash = 0

    private fun loadPalette(): KeyboardPalette {
        return when (context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
            .getString("keyboard_theme", "midnight")) {
            "ocean" -> KeyboardPalette(
                intArrayOf(0xFF061B25.toInt(), 0xFF0B3444.toInt(), 0xFF092632.toInt()),
                0xFF164657.toInt(), 0xFF1D5364.toInt(), 0xFF087F8C.toInt(), 0xFF1B5969.toInt(),
                0x8876E4E8.toInt(), 0x5576E4E8.toInt(), 0xFF155A68.toInt(), 0xFF00A6B2.toInt(),
                0x3345D5D8
            )
            "violet" -> KeyboardPalette(
                intArrayOf(0xFF171020.toInt(), 0xFF302044.toInt(), 0xFF1E172D.toInt()),
                0xFF39294F.toInt(), 0xFF49345F.toInt(), 0xFF7045A3.toInt(), 0xFF49345F.toInt(),
                0x99C69BFF.toInt(), 0x55C69BFF.toInt(), 0xFF563878.toInt(), 0xFF8754C7.toInt(),
                0x334F2F75
            )
            else -> KeyboardPalette(
                intArrayOf(0xFF0C1220.toInt(), 0xFF17243B.toInt(), 0xFF101827.toInt()),
                0xFF202D44.toInt(), 0xFF293750.toInt(), 0xFF24558A.toInt(), 0xFF263B5D.toInt(),
                0x885ABEFF.toInt(), 0x553F9DFF.toInt(), 0xFF263C5D.toInt(), 0xFF287DCE.toInt(),
                0x332D83D5
            )
        }
    }
    private fun palette(): KeyboardPalette = activeDrawPalette ?: loadPalette()

    private val density = resources.displayMetrics.density
    private val gap = 5f * density
    private val topInset = 42f * density
    private var cachedLayoutRows: List<List<KeyboardKey>>? = null
    private var cachedLayoutMode: String? = null
    private var cachedLayoutWidth = -1
    private var cachedLayoutHeight = -1
    private var cachedKeyRectangles: List<Pair<KeyboardKey, RectF>> = emptyList()
    private var suggestions: List<String> = emptyList()
    private val suggestionRects = mutableListOf<RectF>()
    private val handler = Handler(Looper.getMainLooper())
    private var shift = false
    private var capsLock = false
    private var symbols = false
    private var symbolsPageTwo = false
    private var emojiMode = false
    private var emojiCategory = 0
    private var clipboardMode = false
    private var clipboardAvailable = true
    // 0 = regular, 1 = numeric, 2 = phone, 3 = date, 4 = time.
    private var numericInputMode = 0
    private var numericAllowsSign = false
    private var numericAllowsDecimal = false
    private var numericDecimalSeparator = java.text.DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    private var clipboardItems: List<String> = emptyList()
    private var activeLanguageLabel = "Türkçe"
    private var editorActionDescription: String? = null
    private val multiTouchGuard = MultiTouchGestureGuard()
    private var downKey: KeyboardKey? = null
    private var downRect: RectF? = null
    private var longPressed = false
    private var alternateOptions: List<String> = emptyList()
    private var popupRect: RectF? = null
    private var selectedAlternate = -1
    private var lastCursorX = 0f
    private var cursorMoved = false
    private var swipeTyping = false
    private val swipePath = mutableListOf<Pair<Float, Float>>()

    private fun appendSwipeSegment(x: Float, y: Float) {
        SwipePathSampler.appendSegment(swipePath, x, y, density)
    }
    private val deleteRepeatRunnable = object : Runnable {
        override fun run() {
            if (downKey?.label == "⌫" && longPressed) {
                onKey("⌫")
                handler.postDelayed(this, 65)
            }
        }
    }

    private val alternatives = mapOf(
        "g" to listOf("ğ", "Ğ"), "u" to listOf("ü", "Ü"),
        "c" to listOf("ç", "Ç"), "s" to listOf("ş", "Ş"),
        "o" to listOf("ö", "Ö"), "i" to listOf("ı", "İ")
    )

    private val punctuationAlternatives = mapOf(
        "." to listOf("…", "?", "!"),
        "," to listOf(";", ":"),
        "-" to listOf("—", "–", "_")
    )

    private fun letterRows() = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0").map { KeyboardKey(it) },
        "qwertyuiop".map { KeyboardKey(it.toString()) },
        "asdfghjkl".map { KeyboardKey(it.toString()) },
        listOf(KeyboardKey("⇧", kind = KeyboardKey.Kind.ACTION, weight = 1.35f)) +
            "zxcvbnm".map { KeyboardKey(it.toString()) } +
            listOf(KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.35f)),
        listOf(
            KeyboardKey("?123", kind = KeyboardKey.Kind.ACCENT, weight = 1.55f),
            KeyboardKey(",", weight = .8f),
            KeyboardKey("😊", kind = KeyboardKey.Kind.ACTION, weight = .9f),
            KeyboardKey(activeLanguageLabel, " ", 4.2f, KeyboardKey.Kind.SPACE),
            KeyboardKey(".", weight = .8f),
            KeyboardKey("↵", kind = KeyboardKey.Kind.ACCENT, weight = 1.25f)
        )
    )

    private fun emojiRows(): List<List<KeyboardKey>> {
        val prefs = context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
        val recent = prefs.getString("recent_emojis", "").orEmpty()
            .split("|").filter { it.isNotBlank() }.distinct().take(8)
        val groups = when (emojiCategory) {
            1 -> listOf(
                listOf("😀","😃","😄","😁","😆","😅","😂","🤣","🙂","🙃"),
                listOf("😊","😍","🥰","😘","😎","🤩","🥳","😭","😌","😏"),
                listOf("😢","😡","🤔","🤗","😴","🙄","😇","🤯","😱","😤"),
                listOf("🥺","😋","😜","🤪","😬","🫡","🤫","🤭","🫣","🤓")
            )
            2 -> listOf(
                listOf("👍","👎","👏","🙏","🫱‍🫲","👌","✌️","🤞","🫶","🙌"),
                listOf("👋","🖐️","✋","🤟","🤘","💪","🤦","🖕","👊","✊"),
                listOf("☝️","👇","👆","👉","👈","🤙","✍️","🫰","🫵","👐"),
                listOf("🙌","👐","🤲","🫳","🫴","🤷","🤌","🙋","🙅","🖖"),
                listOf("👶","👦","👧","🧑","👨","👩","🧓","👴","👵","🧔"),
                listOf("👮","👷","💂","🕵️","👳","👲","👼","🎅","🤶","🧙"),
                listOf("🧚","🧛","🧜","🧝","🧞","🧟","🤰","🤱","🧑‍🍼","🫄")
            )
            3 -> listOf(
                listOf("🐱","🐶","🐼","🦊","🐻","🐸","🐵","🦁"),
                listOf("🌹","🌷","🌻","🌼","🍀","🌿","🌵","🌳"),
                listOf("☀️","🌙","⭐","🌈","☁️","⚡","🔥","❄️"),
                listOf("🦋","🐝","🐞","🕊️","🐬","🐢","🦄","🌊")
            )
            4 -> listOf(
                listOf("🍎","🍊","🍋","🍉","🍇","🍓","🍒","🍍","🥝","🥭"),
                listOf("🍕","🍔","🍟","🌭","🍿","🍩","🍪","🎂","🍰","🥐"),
                listOf("☕","🧋","🥤","🍵","🥛","🍺","🍷","🍹","🧃","🍶"),
                listOf("🎉","🎁","🎈","❤️","💯","✨","💡","🚀","💎","🪄")
            )
            5 -> listOf(
                listOf("🚗","🚕","🚌","🚆","✈️","🚀","🚲","🛵","🚢","🚁"),
                listOf("🏖️","🏕️","⛺","🗺️","🧭","🏔️","🌋","🏝️","🏜️","🏙️"),
                listOf("⚽","🏀","🎾","🏐","🏓","🎮","🎸","🎨","🎤","🎧"),
                listOf("🏆","🥇","🎯","🎳","🎿","🏄","🚴","🧳","🧗","🏋️")
            )
            6 -> listOf(
                listOf("📱","💻","⌨️","🖥️","🖨️","🖱️","📷","🎥","📺","🎙️"),
                listOf("⌚","⏰","🔋","💡","🔦","📡","🔌","🧲","🪫","🧯"),
                listOf("📚","📖","✏️","🖊️","📝","📎","📌","✂️","🗂️","📏"),
                listOf("🔑","🔒","🔔","🎁","🛍️","💳","🧰","⚙️","🪙","🧸")
            )
            else -> if (emojiCategory == 0 && recent.isNotEmpty()) {
                listOf(recent)
            } else {
                listOf(
                    listOf("😀","😃","😄","😁","😆","😅","😂","🤣"),
                    listOf("😊","😍","🥰","😘","😎","🤩","🥳","😭"),
                    listOf("👍","👎","👏","🙏","❤️","🔥","✨","💯"),
                    listOf("🎉","🌹","☀️","🌙","🐱","🐶","🍀","☕")
                )
            }
        }
        val rows = groups.map { row -> row.map { KeyboardKey(it) } }.toMutableList()
        rows += listOf(
            KeyboardKey("ABC", kind = KeyboardKey.Kind.ACCENT, weight = 1.2f),
            KeyboardKey("🌐", kind = KeyboardKey.Kind.ACTION, weight = .9f),
            KeyboardKey(activeLanguageLabel, " ", 4.8f, KeyboardKey.Kind.SPACE),
            KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.1f)
        )
        return rows
    }

    private val skinToneBases = setOf(
        "👍", "👎", "👏", "🙏", "🫱‍🫲", "👌", "✌️", "🤞", "🫶", "🙌", "👋", "🖐️", "✋",
        "🤟", "🤘", "💪", "👊", "✊", "☝️", "👇", "👆", "👉", "👈", "🤙", "🫰",
        "🫵", "👐", "🤲", "🫳", "🫴", "💅", "🤌", "🖖", "🤦", "🤷", "🙋", "🙅",
        "👶", "👦", "👧", "🧑", "👨", "👩", "🧓", "👴", "👵", "🧔", "👮", "👷",
        "💂", "🕵️", "👳", "👲", "👼", "🎅", "🤶", "🧙", "🧚", "🧛", "🧜", "🧝",
        "🧞", "🧟", "🤰", "🤱", "🧑‍🍼", "🫄"
    )
    private val skinToneModifiers = listOf("", "🏻", "🏼", "🏽", "🏾", "🏿")

    private fun preferredEmoji(value: String): String {
        if (!emojiMode || value !in skinToneBases) return value
        val tone = context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
            .getString("preferred_emoji_skin_tone", "").orEmpty()
        return EmojiSkinTone.apply(value, tone)
    }

    private fun rememberEmoji(value: String) {
        val prefs = context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
        val recent = prefs.getString("recent_emojis", "").orEmpty()
            .split("|").filter { it.isNotBlank() && it != value }
        prefs.edit().putString("recent_emojis", (listOf(value) + recent).take(8).joinToString("|")).apply()
    }

    private fun symbolRows(): List<List<KeyboardKey>> = if (!symbolsPageTwo) listOf(
        listOf("1","2","3","4","5","6","7","8","9","0").map { KeyboardKey(it) },
        listOf("@","#","₺","%","&","-","+","(",")","/").map { KeyboardKey(it) },
        listOf("*","\"","'",":",";","!","?","€","£","$").map { KeyboardKey(it) },
        listOf(KeyboardKey("=\\<", kind = KeyboardKey.Kind.ACCENT, weight = 1.35f)) +
            listOf("_","\\","|","~","<",">","[").map { KeyboardKey(it) } +
            listOf(KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.35f)),
        symbolBottomRow("ABC")
    ) else listOf(
        listOf("1","2","3","4","5","6","7","8","9","0").map { KeyboardKey(it) },
        listOf("~","`","|","•","√","π","÷","×","¶","∆").map { KeyboardKey(it) },
        listOf("£","¢","€","¥","^","°","=","{","}","\\").map { KeyboardKey(it) },
        listOf(KeyboardKey("123", kind = KeyboardKey.Kind.ACCENT, weight = 1.35f)) +
            listOf("%","©","®","™","✓","[","]").map { KeyboardKey(it) } +
            listOf(KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.35f)),
        symbolBottomRow("ABC")
    )

    private fun symbolBottomRow(label: String) = listOf(
        KeyboardKey(label, kind = KeyboardKey.Kind.ACCENT, weight = 1.55f),
        KeyboardKey(",", weight = .8f),
        KeyboardKey("😊", kind = KeyboardKey.Kind.ACTION, weight = .9f),
        KeyboardKey(activeLanguageLabel, " ", 4.2f, KeyboardKey.Kind.SPACE),
        KeyboardKey(".", weight = .8f),
        KeyboardKey("↵", kind = KeyboardKey.Kind.ACCENT, weight = 1.25f)
    )

    private fun numericRows(): List<List<KeyboardKey>> {
        if (numericInputMode == 3 || numericInputMode == 4) {
            val isDate = numericInputMode == 3
            val separators = if (isDate) listOf("/", "0", "-") else listOf(":", "0", ".")
            return listOf(
                listOf(KeyboardKey("1"), KeyboardKey("2"), KeyboardKey("3")),
                listOf(KeyboardKey("4"), KeyboardKey("5"), KeyboardKey("6")),
                listOf(KeyboardKey("7"), KeyboardKey("8"), KeyboardKey("9")),
                separators.map { KeyboardKey(it) },
                listOf(
                    KeyboardKey("🌐", kind = KeyboardKey.Kind.ACTION, weight = .9f),
                    KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.1f),
                    KeyboardKey(activeLanguageLabel, " ", 2.2f, KeyboardKey.Kind.SPACE),
                    KeyboardKey("↵", kind = KeyboardKey.Kind.ACCENT, weight = 1.1f)
                )
            )
        }
        if (numericInputMode == 2) {
            return listOf(
                listOf(KeyboardKey("1"), KeyboardKey("2 ABC", "2"), KeyboardKey("3 DEF", "3")),
                listOf(KeyboardKey("4 GHI", "4"), KeyboardKey("5 JKL", "5"), KeyboardKey("6 MNO", "6")),
                listOf(KeyboardKey("7 PQRS", "7"), KeyboardKey("8 TUV", "8"), KeyboardKey("9 WXYZ", "9")),
                listOf(KeyboardKey("*"), KeyboardKey("0", weight = 1.4f), KeyboardKey("#")),
                listOf(
                    KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.1f),
                    KeyboardKey("🌐", kind = KeyboardKey.Kind.ACTION, weight = .9f),
                    KeyboardKey(activeLanguageLabel, " ", 3.2f, KeyboardKey.Kind.SPACE),
                    KeyboardKey("↵", kind = KeyboardKey.Kind.ACCENT, weight = 1.1f)
                )
            )
        }
        return listOf(
            listOf(KeyboardKey("1"), KeyboardKey("2"), KeyboardKey("3")),
            listOf(KeyboardKey("4"), KeyboardKey("5"), KeyboardKey("6")),
            listOf(KeyboardKey("7"), KeyboardKey("8"), KeyboardKey("9")),
            buildList {
                if (numericAllowsSign) add(KeyboardKey("−", "-", kind = KeyboardKey.Kind.ACTION))
                add(KeyboardKey("0", weight = 1.4f))
                if (numericAllowsDecimal) add(KeyboardKey(numericDecimalSeparator, kind = KeyboardKey.Kind.ACTION))
                add(KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION))
            },
            listOf(
                KeyboardKey("🌐", kind = KeyboardKey.Kind.ACTION, weight = .9f),
                KeyboardKey(activeLanguageLabel, " ", 3.2f, KeyboardKey.Kind.SPACE),
                KeyboardKey("↵", kind = KeyboardKey.Kind.ACCENT, weight = 1.1f)
            )
        )
    }

    private fun rows() = when {
        clipboardMode -> {
            val items = clipboardItems.take(6).mapIndexed { index, item ->
                KeyboardKey(
                    android.text.TextUtils.ellipsize(item.replace("\n", " ").replace("\r", " "), android.text.TextPaint().apply { textSize = 12f * density }, 190f * density, android.text.TextUtils.TruncateAt.END).toString(),
                    "__CLIP_ITEM__$index",
                    1f,
                    KeyboardKey.Kind.NORMAL
                )
            }
            val padded = items + List(6 - items.size) { KeyboardKey("", "", 1f, KeyboardKey.Kind.ACTION) }
            listOf(padded.take(2), padded.slice(2..3), padded.drop(4)) +
                listOf(listOf(KeyboardKey("ABC", kind = KeyboardKey.Kind.ACCENT, weight = 1.2f),
                    KeyboardKey("🌐", kind = KeyboardKey.Kind.ACTION, weight = .9f),
                    KeyboardKey(activeLanguageLabel, " ", 4.8f, KeyboardKey.Kind.SPACE),
                    KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.1f)))
        }
        numericInputMode != 0 -> numericRows()
        emojiMode -> emojiRows()
        symbols -> symbolRows()
        else -> letterRows()
    }

    private val longPressRunnable = Runnable {
        val key = downKey ?: return@Runnable
        if (key.label == "⌫") {
            longPressed = true
            onKey("⌫")
            handler.postDelayed(deleteRepeatRunnable, 180)
        } else if (key.value.isNotBlank()) {
            // Keep touch and screen-reader long-press rules identical.
            // This also prevents clipboard items from opening character popups.
            val anchor = downRect ?: return@Runnable
            showAlternateOptions(key, anchor)
        }
    }

    private fun alternatePopupBounds(anchor: RectF, optionCount: Int): RectF {
        val bounds = AlternatePopupLayout.calculate(
            viewWidth = width.toFloat(),
            gap = gap,
            density = density,
            anchorCenterX = anchor.centerX(),
            anchorTop = anchor.top,
            optionCount = optionCount
        )
        return RectF(bounds.left, bounds.top, bounds.right, bounds.bottom)
    }

    private fun showAlternateOptions(key: KeyboardKey, anchor: RectF) {
        val options = alternateOptionsFor(key)
        if (options.isEmpty()) return
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(deleteRepeatRunnable)
        downKey = key
        downRect = anchor
        longPressed = true
        cursorMoved = false
        swipeTyping = false
        swipePath.clear()
        alternateOptions = options
        selectedAlternate = -1
        popupRect = alternatePopupBounds(anchor, options.size)
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun setClipboardAvailable(enabled: Boolean) {
        clipboardAvailable = enabled
        if (!enabled) hideClipboard()
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun setClipboardItems(items: List<String>) {
        clipboardItems = items.take(6)
        clipboardMode = true
        emojiMode = false
        symbols = false
        symbolsPageTwo = false
        // A panel switch must not leave stale key gestures or popups behind.
        cancelTransientKeyInteraction()
        cachedLayoutRows = null
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun hideClipboard() {
        clipboardMode = false
        cachedLayoutRows = null
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun resetPanelsForInput() {
        cancelTransientKeyInteraction()
        clipboardMode = false
        emojiMode = false
        symbols = false
        symbolsPageTwo = false
        cachedLayoutRows = null
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun setAutoShift(enabled: Boolean) {
        if (!capsLock && shift != enabled) {
            shift = enabled
            accessibilityHelper.invalidateRoot()
            invalidate()
        }
    }

    fun setNumericInputMode(inputType: Int) {
        // Editor type can change while the IME view remains attached (for
        // example, when switching fields or input subtypes). Never let a
        // gesture started under the previous layout complete in the new one.
        cancelTransientKeyInteraction()
        cachedLayoutRows = null
        val inputClass = inputType and android.text.InputType.TYPE_MASK_CLASS
        numericInputMode = when (inputClass) {
            android.text.InputType.TYPE_CLASS_PHONE -> 2
            android.text.InputType.TYPE_CLASS_NUMBER -> 1
            android.text.InputType.TYPE_CLASS_DATETIME -> {
                when (inputType and android.text.InputType.TYPE_MASK_VARIATION) {
                    android.text.InputType.TYPE_DATETIME_VARIATION_TIME -> 4
                    else -> 3
                }
            }
            else -> 0
        }
        numericAllowsSign = inputType and android.text.InputType.TYPE_NUMBER_FLAG_SIGNED != 0
        numericAllowsDecimal = inputType and android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL != 0
        if (numericInputMode != 0) {
            symbols = false
            symbolsPageTwo = false
            emojiMode = false
            clipboardMode = false
        }
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun setEditorActionDescription(description: String?) {
        editorActionDescription = description?.takeIf { it.isNotBlank() }
        accessibilityHelper.invalidateRoot()
    }

    fun setInputLanguage(localeTag: String) {
        val nextLanguage = if (localeTag.startsWith("en", ignoreCase = true)) "English" else "Türkçe"
        if (activeLanguageLabel == nextLanguage) return
        // The language label is embedded in the cached spacebar key. Rebuild
        // the layout and discard any gesture that began under the old label.
        cancelTransientKeyInteraction()
        activeLanguageLabel = nextLanguage
        cachedLayoutRows = null
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun setSuggestions(items: List<String>) {
        suggestions = items.distinct().take(3)
        suggestionRects.clear()
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    private fun accessibleKeyRects() = keyRects().filter { it.first.label.isNotBlank() }

    private val accessibilityHelper by lazy {
        object : ExploreByTouchHelper(this) {
            override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
                val accessibleKeys = accessibleKeyRects()
                val activeAlternateKeyIndex = downKey?.let { activeKey ->
                    accessibleKeys.indexOfFirst { it.first == activeKey }
                } ?: -1
                virtualViewIds.addAll(
                    AlternateAccessibilityNodeOrder.insertAfterKey(
                        keyCount = accessibleKeys.size,
                        activeKeyIndex = activeAlternateKeyIndex,
                        optionCount = alternateOptions.size
                    )
                )
                if (!clipboardMode && !emojiMode && !symbols) {
                    suggestions.forEachIndexed { index, _ -> virtualViewIds.add(1000 + index) }
                }
                if (clipboardAvailable) virtualViewIds.add(2000)
                if (emojiMode) (0..6).forEach { virtualViewIds.add(2100 + it) }
                if (clipboardMode) {
                    virtualViewIds.add(2002)
                    if (clipboardItems.isEmpty()) virtualViewIds.add(2003)
                } else virtualViewIds.add(2001)
            }

            override fun getVirtualViewAt(x: Float, y: Float): Int {
                // The floating alternatives popup overlays the original key.
                // Hit-test it first so TalkBack can reach each visible option.
                val popup = popupRect
                if (popup != null && popup.contains(x, y) && alternateOptions.isNotEmpty()) {
                    val optionIndex = floor((x - popup.left) / (popup.width() / alternateOptions.size))
                        .toInt().coerceIn(0, alternateOptions.lastIndex)
                    return 3000 + optionIndex
                }
                val keyIndex = accessibleKeyRects().indexOfFirst { it.second.contains(x, y) }
                if (keyIndex >= 0) return keyIndex
                if (clipboardMode && clipboardItems.isEmpty() && y >= topInset) return 2003
                if (!clipboardMode && !emojiMode && !symbols) {
                    val suggestionIndex = suggestionRects.indexOfFirst { it.contains(x, y) }
                    if (suggestionIndex >= 0 && suggestionIndex < suggestions.size) return 1000 + suggestionIndex
                }
                if (clipboardAvailable && y < topInset && x < 58f * density) return 2000
                if (emojiMode && y < topInset && x >= 58f * density && x <= width - 58f * density) {
                    val categoryWidth = (width - 116f * density) / 7f
                    return 2100 + (((x - 58f * density) / categoryWidth).toInt().coerceIn(0, 6))
                }
                if (y < topInset && x > width - 58f * density) return if (clipboardMode) 2002 else 2001
                return INVALID_ID
            }

            override fun onPopulateNodeForVirtualView(
                virtualViewId: Int,
                node: androidx.core.view.accessibility.AccessibilityNodeInfoCompat
            ) {
                if (virtualViewId >= 3000 && virtualViewId < 4000) {
                    val option = alternateOptions.getOrNull(virtualViewId - 3000)
                    if (option == null) {
                        node.isEnabled = false
                        return
                    }
                    node.text = option
                    val optionIndex = virtualViewId - 3000
                    val isEmojiTone = emojiMode &&
                        optionIndex < alternateOptions.size && option.length > alternateOptions.first().length
                    val isDefaultEmojiTone = emojiMode && optionIndex == 0 &&
                        alternateOptions.size == 6
                    node.contentDescription = if (isEmojiTone || isDefaultEmojiTone) {
                        EmojiToneAccessibilityLabel.describe(
                            optionIndex,
                            option,
                            activeLanguageLabel == "English"
                        )
                    } else if (activeLanguageLabel == "English") {
                        "Emoji or alternative character: $option"
                    } else {
                        "Emoji veya alternatif karakter: $option"
                    }
                    node.className = android.widget.Button::class.java.name
                    node.isFocusable = true
                    node.isClickable = true
                    node.addAction(androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK)
                    val popup = popupRect ?: RectF()
                    val cellWidth = popup.width() / alternateOptions.size
                    val left = popup.left + (virtualViewId - 3000) * cellWidth
                    node.setBoundsInParent(Rect(left.toInt(), popup.top.toInt(), (left + cellWidth).toInt(), popup.bottom.toInt()))
                    return
                }
                if (virtualViewId == 2003) {
                    val english = activeLanguageLabel == "English"
                    node.text = if (english) "Clipboard is empty. Copied text will appear here." else "Pano henüz boş. Kopyaladığın metinler burada görünecek."
                    node.contentDescription = node.text
                    node.className = android.widget.TextView::class.java.name
                    node.isFocusable = true
                    val bottomRowTop = keyRects().lastOrNull()?.second?.top ?: height * 0.78f
                    node.setBoundsInParent(Rect(0, topInset.toInt(), width, bottomRowTop.toInt()))
                    return
                }
                if (virtualViewId in 2100..2106) {
                    val labels = listOf("Son", "Yüzler", "İşaretler", "Doğa", "Yiyecek", "Seyahat", "Nesneler")
                    val englishLabels = listOf("Recent", "Faces", "Gestures", "Nature", "Food", "Travel", "Objects")
                    val index = virtualViewId - 2100
                    node.text = if (activeLanguageLabel == "English") englishLabels[index] else labels[index]
                    node.contentDescription = node.text
                    node.className = android.widget.Button::class.java.name
                    node.isFocusable = true
                    node.isClickable = true
                    node.isSelected = emojiCategory == index
                    node.addAction(androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK)
                    val cellWidth = (width - 116f * density) / 7f
                    val left = 58f * density + index * cellWidth
                    node.setBoundsInParent(Rect(left.toInt(), 0, (left + cellWidth).toInt(), topInset.toInt()))
                    return
                }
                if (virtualViewId >= 2000) {
                    val label = when (virtualViewId) {
                        2000 -> if (activeLanguageLabel == "English") "Open clipboard" else "Panoyu aç"
                        2001 -> if (activeLanguageLabel == "English") "Change keyboard theme" else "Klavye temasını değiştir"
                        2002 -> if (activeLanguageLabel == "English") "Clear clipboard history" else "Pano geçmişini temizle"
                        else -> if (activeLanguageLabel == "English") "Keyboard tool" else "Klavye aracı"
                    }
                    node.text = label
                    node.contentDescription = label
                    node.className = android.widget.Button::class.java.name
                    node.isFocusable = true
                    node.isClickable = true
                    node.addAction(androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK)
                    val left = if (virtualViewId == 2000) 0f else width - 58f * density
                    node.setBoundsInParent(Rect(left.toInt(), 0, (left + 58f * density).toInt(), topInset.toInt()))
                    return
                }
                if (virtualViewId >= 1000) {
                    val index = virtualViewId - 1000
                    val suggestion = suggestions.getOrNull(index)
                    if (suggestion != null) {
                        node.text = suggestion
                        node.contentDescription = if (activeLanguageLabel == "English") "Suggestion: $suggestion" else "Öneri: $suggestion"
                        node.className = android.widget.Button::class.java.name
                        node.isFocusable = true
                        node.isClickable = true
                        node.addAction(androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK)
                        val rect = suggestionRects.getOrNull(index)
                            ?: suggestionBounds(index, suggestions.size)
                        node.setBoundsInParent(Rect(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt()))
                        return
                    }
                }
                val pair = accessibleKeyRects().getOrNull(virtualViewId)
                if (pair == null || pair.first.label.isBlank()) {
                    node.contentDescription = if (activeLanguageLabel == "English") "Empty key" else "Boş tuş"
                    node.isEnabled = false
                    return
                }
                val key = pair.first
                val rect = pair.second
                val english = activeLanguageLabel == "English"
                val clipboardIndex = key.value.removePrefix("__CLIP_ITEM__").toIntOrNull()
                    .takeIf { key.value.startsWith("__CLIP_ITEM__") }
                val clipboardText = clipboardIndex?.let { clipboardItems.getOrNull(it) }
                val description = clipboardText?.let {
                    if (english) "Clipboard item: $it" else "Pano öğesi: $it"
                } ?: when (key.kind) {
                    KeyboardKey.Kind.SPACE -> if (english) "Space, $activeLanguageLabel" else "Boşluk, $activeLanguageLabel"
                    KeyboardKey.Kind.ACCENT -> when (key.label) {
                        "↵" -> editorActionDescription ?: if (english) "Enter" else "Gönder veya yeni satır"
                        "?123" -> if (english) "Numbers and symbols" else "Sayılar ve semboller"
                        "ABC" -> if (english) "Letters" else "Harfler"
                        else -> key.label
                    }
                    KeyboardKey.Kind.ACTION -> when (key.label) {
                        "⌫" -> if (english) "Backspace" else "Geri sil"
                        "⇧" -> if (english) "Shift" else "Büyük harf"
                        "🌐" -> if (english) "Switch language" else "Dil değiştir"
                        "😊" -> "Emoji"
                        else -> key.label
                    }
                    else -> if (emojiMode) {
                        EmojiAccessibilityLabeler.label(preferredEmoji(key.value), english)
                    } else {
                        key.label
                    }
                }
                node.text = description
                node.contentDescription = description
                if (key.kind == KeyboardKey.Kind.ACTION && key.label == "⇧") {
                    node.stateDescription = when {
                        capsLock -> if (english) "Caps Lock on" else "Büyük harf kilidi açık"
                        shift -> if (english) "Shift on" else "Büyük harf açık"
                        else -> if (english) "Off" else "Kapalı"
                    }
                }
                node.className = android.widget.Button::class.java.name
                node.isFocusable = true
                node.isClickable = true
                node.addAction(androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK)
                if (alternateOptionsFor(key).isNotEmpty()) {
                    node.isLongClickable = true
                    node.addAction(androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_LONG_CLICK)
                }
                node.setBoundsInParent(Rect(
                    rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt()
                ))
            }

            override fun onPerformActionForVirtualView(
                virtualViewId: Int,
                action: Int,
                arguments: android.os.Bundle?
            ): Boolean {
                if (virtualViewId >= 3000 && virtualViewId < 4000 &&
                    action == AccessibilityNodeInfo.ACTION_CLICK
                ) {
                    val option = alternateOptions.getOrNull(virtualViewId - 3000) ?: return false
                    commitAlternateOption(option)
                    // Announce the click while the virtual option still exists;
                    // then remove the popup and refresh the accessibility tree.
                    sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
                    cancelTransientKeyInteraction()
                    invalidateRoot()
                    invalidate()
                    return true
                }
                if (action == androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_LONG_CLICK &&
                    virtualViewId < 1000
                ) {
                    val pair = accessibleKeyRects().getOrNull(virtualViewId) ?: return false
                    if (alternateOptionsFor(pair.first).isEmpty()) return false
                    showAlternateOptions(pair.first, pair.second)
                    sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)
                    return true
                }
                if (action != AccessibilityNodeInfo.ACTION_CLICK) return false
                val selectedEmojiCategory = EmojiCategoryAccessibility.indexFor(virtualViewId)
                if (selectedEmojiCategory != null) {
                    setEmojiCategory(selectedEmojiCategory)
                    sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
                    return true
                }
                if (virtualViewId >= 2000) {
                    when (virtualViewId) {
                        2000 -> { if (!clipboardAvailable) return false else onKey("__CLIPBOARD__") }
                        2001 -> {
                            val prefs = context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
                            val next = when (prefs.getString("keyboard_theme", "midnight")) {
                                "midnight" -> "ocean"
                                "ocean" -> "violet"
                                else -> "midnight"
                            }
                            prefs.edit().putString("keyboard_theme", next).apply()
                            invalidate()
                        }
                        2002 -> onKey("__CLIP_CLEAR__")
                    }
                    sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
                    return true
                }
                if (virtualViewId >= 1000) {
                    val suggestion = suggestions.getOrNull(virtualViewId - 1000) ?: return false
                    onSuggestion(suggestion)
                    sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
                    return true
                }
                val key = accessibleKeyRects().getOrNull(virtualViewId)?.first ?: return false
                performAccessibleKey(key)
                sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
                return true
            }
        }
    }

    private fun alternateOptionsFor(key: KeyboardKey): List<String> {
        if (clipboardMode) return emptyList()
        // Emoji skin-tone variants are offered only on long press.
        if (emojiMode && key.value in skinToneBases) {
            return skinToneModifiers.map { tone -> EmojiSkinTone.apply(key.value, tone) }
        }
        if (emojiMode) return emptyList()
        val punctuation = punctuationAlternatives[key.value]
        if (punctuation != null) return punctuation
        return if (!symbols && activeLanguageLabel == "Türkçe") {
            alternatives[key.value.lowercase()] ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun commitAlternateOption(value: String) {
        if (emojiMode) {
            val tone = EmojiSkinTone.modifierFrom(value)
            context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
                .edit().putString("preferred_emoji_skin_tone", tone).apply()
            rememberEmoji(value)
        }
        onKey(value)
    }

    private fun cancelTransientKeyInteraction() {
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(deleteRepeatRunnable)
        downKey = null
        downRect = null
        longPressed = false
        cursorMoved = false
        swipeTyping = false
        swipePath.clear()
        alternateOptions = emptyList()
        popupRect = null
        selectedAlternate = -1
    }

    private fun refreshPanelLayout() {
        cachedLayoutRows = null
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    private fun setEmojiCategory(category: Int) {
        // Category changes replace the visible emoji keys. Cancel any gesture
        // or alternate popup tied to the previous category before relayout.
        cancelTransientKeyInteraction()
        emojiCategory = category.coerceIn(0, 6)
        refreshPanelLayout()
    }

    private fun showEmojiPanel() {
        cancelTransientKeyInteraction()
        emojiMode = true
        symbols = false
        symbolsPageTwo = false
        clipboardMode = false
        refreshPanelLayout()
    }

    private fun showSymbolsPanel() {
        cancelTransientKeyInteraction()
        symbols = true
        symbolsPageTwo = false
        emojiMode = false
        clipboardMode = false
        refreshPanelLayout()
    }

    private fun showLetterPanel() {
        cancelTransientKeyInteraction()
        symbols = false
        symbolsPageTwo = false
        emojiMode = false
        clipboardMode = false
        refreshPanelLayout()
    }

    private fun setSymbolsPageTwo(enabled: Boolean) {
        cancelTransientKeyInteraction()
        symbols = true
        symbolsPageTwo = enabled
        emojiMode = false
        clipboardMode = false
        refreshPanelLayout()
    }

    private fun performAccessibleKey(key: KeyboardKey) {
        when (key.label) {
            "⇧" -> {
                if (shift && !capsLock) capsLock = true
                else if (capsLock) { capsLock = false; shift = false }
                else shift = true
                invalidate()
            }
            "⌫" -> onKey("⌫")
            "?123" -> showSymbolsPanel()
            "ABC" -> showLetterPanel()
            "=\\<" -> setSymbolsPageTwo(true)
            "123" -> setSymbolsPageTwo(false)
            "😊" -> showEmojiPanel()
            "🌐" -> onKey("🌐")
            activeLanguageLabel -> onKey(" ")
            else -> if (key.value.isNotEmpty()) {
                val output = if (emojiMode) preferredEmoji(key.value) else key.value
                if (emojiMode) rememberEmoji(output)
                onKey(if ((shift || capsLock) && !symbols && !emojiMode && key.value.length == 1) upper(key.value) else output)
                if (shift && !capsLock && !symbols && !emojiMode && key.value.length == 1) shift = false
            }
        }
        accessibilityHelper.invalidateRoot()
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)
    }

    init { isFocusable = true; ViewCompat.setAccessibilityDelegate(this, accessibilityHelper) }

    override fun onDetachedFromWindow() {
        // A detached keyboard must not retain a half-finished key or swipe gesture.
        handler.removeCallbacksAndMessages(null)
        cancelTransientKeyInteraction()
        multiTouchGuard.beginGesture()
        keyTone?.release()
        keyTone = null
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            // A popup or swipe trace is tied to the old key geometry. Do not
            // let it complete after rotation, resizing, or height changes.
            cancelTransientKeyInteraction()
            cachedLayoutRows = null
            accessibilityHelper.invalidateRoot()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val preferredHeight = context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
            .getInt("keyboard_height_dp", 310)
        val desiredHeight = (density * preferredHeight.coerceIn(260, 380)).toInt()
        val measuredWidth = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun keyRects(): List<Pair<KeyboardKey, RectF>> {
        val current = rows()
        val mode = context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
            .getString("one_handed_mode", "normal") ?: "normal"
        if (current == cachedLayoutRows &&
            mode == cachedLayoutMode &&
            width == cachedLayoutWidth &&
            height == cachedLayoutHeight
        ) {
            return cachedKeyRectangles
        }

        val availableHeight = height - topInset - gap * (current.size + 1)
        val isLetterLayout = !symbols && !emojiMode && !clipboardMode && numericInputMode == 0
        // Keep the number row visually quieter and give the main letter rows
        // more vertical room, matching the compact reference keyboard.
        val rowWeights = current.indices.map { index ->
            if (isLetterLayout && index == 0) 0.72f else 1f
        }
        val totalRowWeight = rowWeights.sum()
        val rowHeights = rowWeights.map { availableHeight * it / totalRowWeight }
        var rowTop = topInset + gap
        val result = mutableListOf<Pair<KeyboardKey, RectF>>()
        val contentWidth = if (mode == "normal") width.toFloat() else width * 0.82f
        val offset = when (mode) {
            "left" -> 0f
            "right" -> width - contentWidth
            else -> 0f
        }
        current.forEachIndexed { rowIndex, row ->
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            // A restrained QWERTY stagger gives the letter rows their own
            // ergonomic rhythm instead of making every row look like a grid.
            val rowInset = if (!symbols && !emojiMode && !clipboardMode && numericInputMode == 0) {
                // Give the home row a clear ergonomic offset and keep the
                // bottom row centered without squeezing the modifier keys.
                when (rowIndex) {
                    2 -> contentWidth * 0.048f
                    3 -> contentWidth * 0.012f
                    else -> 0f
                }
            } else 0f
            val rowWidth = contentWidth - rowInset * 2f
            val available = rowWidth - gap * (row.size + 1)
            var x = offset + rowInset + gap
            row.forEach { key ->
                val keyWidth = available * key.weight / totalWeight
                val currentRowHeight = rowHeights[rowIndex]
                result += key to RectF(x, rowTop, x + keyWidth, rowTop + currentRowHeight)
                x += keyWidth + gap
            }
            rowTop += rowHeights[rowIndex] + gap
        }
        cachedLayoutRows = current
        cachedLayoutMode = mode
        cachedLayoutWidth = width
        cachedLayoutHeight = height
        cachedKeyRectangles = result
        return cachedKeyRectangles
    }

    private fun blendColor(from: Int, to: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val a = (from ushr shift) and 0xFF
            val b = (to ushr shift) and 0xFF
            return (a + (b - a) * t).toInt().coerceIn(0, 255)
        }
        return (channel(24) shl 24) or (channel(16) shl 16) or
            (channel(8) shl 8) or channel(0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        activeDrawPalette = loadPalette()
        val colors = palette()
        val backgroundColorsHash = colors.background.contentHashCode()
        if (cachedBackgroundShader == null ||
            cachedBackgroundWidth != width ||
            cachedBackgroundHeight != height ||
            cachedBackgroundColorsHash != backgroundColorsHash
        ) {
            cachedBackgroundShader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                colors.background, null, Shader.TileMode.CLAMP
            )
            cachedBackgroundWidth = width
            cachedBackgroundHeight = height
            cachedBackgroundColorsHash = backgroundColorsHash
        }
        paint.shader = cachedBackgroundShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        drawSuggestionStrip(canvas)
        keyRects().forEach { (key, rect) ->
            if (key.label.isBlank()) return@forEach
            val baseKeyColor = when (key.kind) {
                KeyboardKey.Kind.SPACE -> colors.space
                KeyboardKey.Kind.ACCENT -> colors.accent
                KeyboardKey.Kind.ACTION -> colors.action
                KeyboardKey.Kind.NORMAL -> colors.normal
            }
            // Shift feedback is visible at a glance; caps-lock is slightly
            // brighter than one-shot shift while keeping the original layout.
            val shiftActive = key.label == "⇧" && (shift || capsLock)
            val keyPressed = downKey == key
            val keyColor = when {
                key.label == "⇧" && capsLock -> blendColor(baseKeyColor, colors.accent, 0.78f)
                shiftActive -> blendColor(baseKeyColor, colors.accent, 0.48f)
                keyPressed -> blendColor(baseKeyColor, colors.accent, 0.22f)
                else -> baseKeyColor
            }
            // Pressed caps travel down a fraction and lose their floating shadow,
            // giving immediate tactile feedback without changing hit rectangles.
            val pressInset = if (keyPressed) 0.8f * density else 0f
            val faceRect = RectF(
                rect.left + pressInset,
                rect.top + pressInset,
                rect.right - pressInset,
                rect.bottom + pressInset
            )
            val keyBottom = when (key.kind) {
                KeyboardKey.Kind.ACCENT -> blendColor(keyColor, 0xFF101A32.toInt(), 0.20f)
                KeyboardKey.Kind.SPACE -> blendColor(keyColor, 0xFF101A32.toInt(), 0.16f)
                else -> blendColor(keyColor, 0xFF080E1A.toInt(), 0.22f)
            }
            paint.setShadowLayer(
                (if (keyPressed) 1.5f else 5f) * density,
                0f, (if (keyPressed) 0.6f else 2.5f) * density, 0x99000000.toInt()
            )
            paint.shader = LinearGradient(
                faceRect.left, faceRect.top, faceRect.left, faceRect.bottom,
                keyColor, keyBottom, Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(faceRect, 8.5f * density, 8.5f * density, paint)
            paint.shader = null
            paint.clearShadowLayer()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.7f * density
            paint.color = when (key.kind) {
                KeyboardKey.Kind.ACCENT -> colors.accentOutline
                KeyboardKey.Kind.SPACE -> colors.spaceOutline
                else -> if (shiftActive) blendColor(colors.accentOutline, 0xFFFFFFFF.toInt(), 0.22f)
                    else blendColor(keyColor, 0xFFFFFFFF.toInt(), 0.13f)
            }
            canvas.drawRoundRect(faceRect, 8.5f * density, 8.5f * density, paint)
            paint.style = Paint.Style.FILL
            // A small upper highlight makes the keycaps feel dimensional
            // without introducing a glossy, oversized SwiftKey-like style.
            paint.color = if (keyPressed) 0x10FFFFFF else 0x22FFFFFF
            canvas.drawRoundRect(
                RectF(faceRect.left + density, faceRect.top + density,
                    faceRect.right - density, faceRect.top + 3f * density),
                2f * density, 2f * density, paint
            )
            if (key.kind == KeyboardKey.Kind.SPACE) {
                // Give the primary spacebar a quiet, ownable signature:
                // inset hairline + short accent rail, without changing hit areas.
                val inset = RectF(
                    rect.left + 3f * density,
                    rect.top + 3f * density,
                    rect.right - 3f * density,
                    rect.bottom - 3f * density
                )
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.55f * density
                paint.color = blendColor(colors.spaceOutline, 0xFFFFFFFF.toInt(), 0.16f)
                canvas.drawRoundRect(inset, 6f * density, 6f * density, paint)
                paint.style = Paint.Style.FILL
                val railWidth = (rect.width() * 0.24f).coerceAtLeast(18f * density)
                val railLeft = rect.centerX() - railWidth / 2f
                paint.shader = LinearGradient(
                    railLeft, 0f, railLeft + railWidth, 0f,
                    intArrayOf(0x003F9DFF, colors.accent, 0x003F9DFF),
                    null, Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(
                    RectF(railLeft, rect.bottom - 3.2f * density,
                        railLeft + railWidth, rect.bottom - 2.1f * density),
                    density, density, paint
                )
                paint.shader = null
            }
            if (!symbols && !emojiMode && numericInputMode == 0 &&
                activeLanguageLabel == "Türkçe" &&
                (key.value == "f" || key.value == "j")
            ) {
                // Physical-keyboard home-row markers help fingers re-center
                // without adding labels or changing the compact key geometry.
                val markerWidth = 8f * density
                val markerHeight = 1.15f * density
                val markerLeft = rect.centerX() - markerWidth / 2f
                val markerTop = rect.bottom - 4f * density
                paint.color = blendColor(colors.accentOutline, 0xFFFFFFFF.toInt(), 0.08f)
                canvas.drawRoundRect(
                    RectF(markerLeft, markerTop, markerLeft + markerWidth, markerTop + markerHeight),
                    markerHeight, markerHeight, paint
                )
            }
            if (key.label == "⇧") {
                // A compact status lamp makes one-shot shift and caps-lock
                // recognizable even when the arrow glyph itself is unchanged.
                paint.style = Paint.Style.FILL
                paint.color = when {
                    capsLock -> 0xFFFFFFFF.toInt()
                    shift -> colors.accent
                    else -> 0xFF64748B.toInt()
                }
                canvas.drawCircle(
                    rect.right - 5.5f * density,
                    rect.top + 5.5f * density,
                    (if (capsLock) 2.3f else 1.8f) * density,
                    paint
                )
            }
            val phoneParts = if (numericInputMode == 2) key.label.split(' ', limit = 2) else emptyList()
            if (phoneParts.size == 2 && phoneParts[0].length == 1) {
                // Telephone keypad legends use a clear two-level hierarchy:
                // the dialed digit stays prominent, its letter group is secondary.
                paint.textAlign = Paint.Align.CENTER
                paint.color = 0xFFF5F8FF.toInt()
                paint.textSize = 21f * density
                val digitBaseline = rect.centerY() - 2f * density -
                    (paint.ascent() + paint.descent()) / 2f
                canvas.drawText(phoneParts[0], rect.centerX(), digitBaseline, paint)
                paint.color = 0xFFB9C8DE.toInt()
                paint.textSize = 8f * density
                val lettersBaseline = rect.centerY() + 11f * density -
                    (paint.ascent() + paint.descent()) / 2f
                canvas.drawText(phoneParts[1], rect.centerX(), lettersBaseline, paint)
            } else {
                val label = when {
                    key.kind == KeyboardKey.Kind.SPACE -> key.label
                    (shift || capsLock) && !symbols && key.label.length == 1 -> upper(key.label)
                    else -> key.label
                }
                paint.color = if (key.kind == KeyboardKey.Kind.SPACE) 0xFFC5D5EE.toInt() else 0xFFF5F8FF.toInt()
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = when {
                    key.kind == KeyboardKey.Kind.SPACE -> 10f * density
                    key.label.length > 3 -> 11f * density
                    key.label.length > 1 -> 14f * density
                    else -> 18f * density
                }
                val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2f
                canvas.drawText(label, rect.centerX(), baseline, paint)

                // Corner legends quietly reveal useful long-press options:
                // Turkish letters stay compact, while punctuation hints work in either language.
                if (!emojiMode && numericInputMode == 0 && key.value.length == 1) {
                    val isTurkishLetter = !symbols && activeLanguageLabel == "Türkçe" &&
                        alternatives.containsKey(key.value.lowercase(Locale.ROOT))
                    val isPunctuation = punctuationAlternatives.containsKey(key.value)
                    val alternateHint = when {
                        isTurkishLetter -> alternatives[key.value.lowercase(Locale.ROOT)]
                            ?.getOrNull(if (shift || capsLock) 1 else 0)
                        isPunctuation -> punctuationAlternatives[key.value]?.firstOrNull()
                        else -> null
                    }
                    if (alternateHint != null) {
                        paint.textAlign = Paint.Align.RIGHT
                        paint.textSize = 7.5f * density
                        paint.color = blendColor(colors.accentOutline, 0xFFFFFFFF.toInt(), 0.12f)
                        canvas.drawText(
                            alternateHint,
                            rect.right - 4f * density,
                            rect.top + 10f * density,
                            paint
                        )
                    }
                }
            }
        }
        if (clipboardMode && clipboardItems.isEmpty()) drawClipboardEmptyState(canvas)
        drawKeyPreview(canvas)
        drawAlternatePopup(canvas)
    }

    private fun drawClipboardEmptyState(canvas: Canvas) {
        val bottomRowTop = keyRects().lastOrNull()?.second?.top ?: height * 0.78f
        val centerY = (topInset + bottomRowTop) / 2f
        paint.textAlign = Paint.Align.CENTER
        paint.color = 0xFFEAF1FC.toInt()
        paint.textSize = 17f * density
        canvas.drawText(
            if (activeLanguageLabel == "English") "Clipboard is empty" else "Pano henüz boş",
            width / 2f, centerY - 5f * density, paint
        )
        paint.color = 0xFF9EABC2.toInt()
        paint.textSize = 12f * density
        canvas.drawText(
            if (activeLanguageLabel == "English") "Copied text will appear here" else "Kopyaladığın metinler burada görünecek",
            width / 2f, centerY + 20f * density, paint
        )
    }

    private fun drawKeyPreview(canvas: Canvas) {
        if (longPressed || symbols || emojiMode || clipboardMode) return
        if (!context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
                .getBoolean("key_preview_enabled", true)) return
        val key = downKey ?: return
        val anchor = downRect ?: return
        if (key.value.length != 1 || !key.value.first().isLetter()) return
        val previewWidth = 54f * density
        val previewHeight = 62f * density
        val left = (anchor.centerX() - previewWidth / 2f).coerceIn(gap, width - previewWidth - gap)
        val top = (anchor.top - previewHeight - 8f * density).coerceAtLeast(topInset + 2f * density)
        val rect = RectF(left, top, left + previewWidth, top + previewHeight)
        paint.setShadowLayer(8f * density, 0f, 3f * density, 0x99000000.toInt())
        paint.color = palette().accent
        canvas.drawRoundRect(rect, 13f * density, 13f * density, paint)
        paint.clearShadowLayer()
        paint.color = 0xFFFFFFFF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 30f * density
        val label = if (shift || capsLock) upper(key.value) else key.value
        val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(label, rect.centerX(), baseline, paint)
    }

    private fun suggestionBounds(index: Int, count: Int, target: RectF = RectF()): RectF {
        val sideControlWidth = 58f * density
        val cellWidth = (width - sideControlWidth * 2f).coerceAtLeast(0f) / count.coerceAtLeast(1)
        return target.apply {
            set(
                sideControlWidth + index * cellWidth + 4f * density,
                5f * density,
                sideControlWidth + (index + 1) * cellWidth - 4f * density,
                37f * density
            )
        }
    }

    private fun drawSuggestionStrip(canvas: Canvas) {
        paint.color = palette().accentOutline
        paint.textSize = 17f * density
        paint.textAlign = Paint.Align.CENTER
        if (clipboardAvailable) canvas.drawText("▣", 28f * density, 26f * density, paint)
        if (!clipboardMode) {
            paint.color = palette().accentOutline
            paint.textSize = 17f * density
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("◐", width - 28f * density, 26f * density, paint)
        }
        if (emojiMode) {
            suggestionRects.clear()
            val labels = listOf("🕘", "😀", "👍", "🌿", "🍕", "🚗", "💻")
            val categoryWidth = (width - 116f * density) / 7f
            labels.forEachIndexed { index, label ->
                val left = 58f * density + index * categoryWidth
                if (emojiCategory == index) {
                    paint.color = palette().suggestion
                    canvas.drawRoundRect(
                        RectF(left + 2f * density, 3f * density, left + categoryWidth - 2f * density, 38f * density),
                        8f * density, 8f * density, paint
                    )
                }
                paint.color = 0xFFEAF1FC.toInt()
                paint.textSize = 19f * density
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(label, left + categoryWidth / 2f, 26f * density, paint)
            }
            return
        }
        if (clipboardMode) {
            suggestionRects.clear()
            paint.color = 0xFFB8C9E4.toInt()
            paint.textSize = 11f * density
            paint.textAlign = Paint.Align.CENTER
            val english = activeLanguageLabel == "English"
            canvas.drawText(
                when {
                    clipboardItems.isEmpty() && english -> "CLIPBOARD EMPTY"
                    clipboardItems.isEmpty() -> "PANO BOŞ"
                    english -> "CLIPBOARD  •  RECENT COPIES"
                    else -> "PANO  •  SON KOPYALANANLAR"
                },
                width / 2f, 25f * density, paint
            )
            paint.textSize = 10f * density
            canvas.drawText(if (english) "CLEAR" else "TEMİZLE", width - 32f * density, 25f * density, paint)
            return
        }
        if (emojiMode || symbols) {
            suggestionRects.clear()
            return
        }
        val count = suggestions.size
        if (count == 0) {
            suggestionRects.clear()
            val centerY = 21f * density
            val brandX = width / 2f
            // A restrained identity strip replaces the generic placeholder:
            // monogram, wordmark and a subtle electric-blue baseline.
            paint.color = palette().accent
            canvas.drawCircle(brandX - 61f * density, centerY - 1f * density, 12f * density, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 14f * density
            paint.isFakeBoldText = true
            canvas.drawText("E", brandX - 61f * density, centerY + 4f * density, paint)
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.LEFT
            paint.color = 0xFFF2F6FF.toInt()
            paint.textSize = 12f * density
            paint.isFakeBoldText = true
            canvas.drawText("ERDİN", brandX - 42f * density, centerY - 1f * density, paint)
            paint.isFakeBoldText = false
            paint.color = 0xFF9EABC2.toInt()
            paint.textSize = 8f * density
            canvas.drawText(
                if (activeLanguageLabel == "English") "SMART KEYBOARD" else "AKILLI KLAVYE",
                brandX - 42f * density, centerY + 10f * density, paint
            )
            // Keep the brand rail clear of the right-side theme control,
            // including on compact screens and one-handed layouts.
            val railStart = brandX + 65f * density
            val railEnd = width - 64f * density
            if (railEnd > railStart + 8f * density) {
                paint.shader = LinearGradient(
                    railStart, 0f, railEnd, 0f,
                    intArrayOf(palette().accent, 0x003F9DFF), null, Shader.TileMode.CLAMP
                )
                paint.strokeWidth = 1.2f * density
                canvas.drawLine(railStart, centerY + 12f * density,
                    railEnd, centerY + 12f * density, paint)
                paint.shader = null
            }
            return
        }
        suggestions.forEachIndexed { index, suggestion ->
            val rect = suggestionBounds(
                index,
                count,
                suggestionRects.getOrNull(index) ?: RectF().also { suggestionRects.add(it) }
            )
            if (index == 1) {
                paint.color = palette().suggestion
                canvas.drawRoundRect(rect, 9f * density, 9f * density, paint)
            }
            paint.color = 0xFFEAF1FC.toInt()
            paint.textSize = 14f * density
            paint.textAlign = Paint.Align.CENTER
            val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2f
            val display = if (paint.measureText(suggestion) > rect.width() - 8f * density) {
                android.text.TextUtils.ellipsize(
                    suggestion,
                    suggestionMeasurePaint.apply { set(paint) },
                    rect.width() - 8f * density,
                    android.text.TextUtils.TruncateAt.END
                ).toString()
            } else suggestion
            canvas.drawText(display, rect.centerX(), baseline, paint)
        }
        if (suggestionRects.size > count) {
            suggestionRects.subList(count, suggestionRects.size).clear()
        }
    }

    private fun drawAlternatePopup(canvas: Canvas) {
        val anchor = downRect ?: return
        if (alternateOptions.isEmpty()) return
        val rect = popupRect ?: alternatePopupBounds(anchor, alternateOptions.size)
        val cellWidth = rect.width() / alternateOptions.size
        val left = rect.left
        val top = rect.top
        val popupHeight = rect.height()
        paint.setShadowLayer(9f * density, 0f, 3f * density, 0xAA000000.toInt())
        paint.color = palette().popup
        canvas.drawRoundRect(rect, 13f * density, 13f * density, paint)
        paint.clearShadowLayer()
        alternateOptions.forEachIndexed { index, value ->
            val cell = RectF(left + index * cellWidth, top, left + (index + 1) * cellWidth, top + popupHeight)
            if (index == selectedAlternate) {
                paint.color = palette().selected
                canvas.drawRoundRect(cell, 9f * density, 9f * density, paint)
            }
            paint.color = 0xFFF7FAFF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 21f * density
            val baseline = cell.centerY() - (paint.ascent() + paint.descent()) / 2f
            canvas.drawText(value, cell.centerX(), baseline, paint)
        }
    }

    private fun updateAlternateSelection(x: Float) {
        val rect = popupRect ?: return
        val cellWidth = rect.width() / alternateOptions.size
        selectedAlternate = floor((x - rect.left) / cellWidth).toInt().coerceIn(0, alternateOptions.lastIndex)
        invalidate()
    }

    private fun findKey(x: Float, y: Float): Pair<KeyboardKey, RectF>? =
        keyRects().firstOrNull { it.second.contains(x, y) }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Mask pointer-index bits so ACTION_POINTER_DOWN is recognized
        // regardless of which pointer Android assigns as the new index.
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            multiTouchGuard.beginGesture()
        } else if (multiTouchGuard.shouldIgnore(action)) {
            return true
        }
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (clipboardAvailable && event.y < topInset && event.x < 58f * density) {
                    onKey("__CLIPBOARD__")
                    return true
                }
                if (event.y < topInset && event.x > width - 58f * density && clipboardMode) {
                    onKey("__CLIP_CLEAR__")
                    return true
                }
                if (event.y < topInset && event.x > width - 58f * density && !clipboardMode) {
                    val prefs = context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
                    val current = prefs.getString("keyboard_theme", "midnight")
                    val next = when (current) {
                        "midnight" -> "ocean"
                        "ocean" -> "violet"
                        else -> "midnight"
                    }
                    prefs.edit().putString("keyboard_theme", next).apply()
                    invalidate()
                    return true
                }
                if (emojiMode && event.y < topInset && event.x >= 58f * density && event.x <= width - 58f * density) {
                    val categoryWidth = (width - 116f * density) / 7f
                    val category = ((event.x - 58f * density) / categoryWidth).toInt().coerceIn(0, 6)
                    setEmojiCategory(category)
                    return true
                }
                if (!emojiMode && !symbols && !clipboardMode) {
                    val suggestionIndex = suggestionRects.indexOfFirst { it.contains(event.x, event.y) }
                    if (suggestionIndex >= 0 && suggestionIndex < suggestions.size) {
                        onSuggestion(suggestions[suggestionIndex])
                        return true
                    }
                }
                val hit = findKey(event.x, event.y)
                downKey = hit?.first
                downRect = hit?.second
                if (hit != null) {
                    val prefs = context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
                    if (prefs.getBoolean("haptic_enabled", true)) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    if (prefs.getBoolean("key_sound_enabled", false)) {
                        playKeySound()
                    }
                }
                longPressed = false
                cursorMoved = false
                swipeTyping = false
                swipePath.clear()
                lastCursorX = event.x
                alternateOptions = emptyList()
                popupRect = null
                selectedAlternate = -1
                if (downKey != null) handler.postDelayed(longPressRunnable, 380)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (longPressed && alternateOptions.isNotEmpty()) {
                    updateAlternateSelection(event.x)
                } else if (downKey?.value?.length == 1 && downKey?.value?.firstOrNull()?.isLetter() == true &&
                    context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
                        .getBoolean("swipe_typing_enabled", false)) {
                    val dx = event.x - (downRect?.centerX() ?: event.x)
                    val dy = event.y - (downRect?.centerY() ?: event.y)
                    if (!swipeTyping && kotlin.math.hypot(dx.toDouble(), dy.toDouble()) > 18f * density) {
                        swipeTyping = true
                        handler.removeCallbacks(longPressRunnable)
                        swipePath.clear()
                        downRect?.let { swipePath += it.centerX() to it.centerY() }
                    }
                    if (swipeTyping) {
                        // MotionEvent may contain intermediate points between delivered frames.
                        // Keep those bends instead of tracing a straight shortcut.
                        for (historyIndex in 0 until event.historySize) {
                            appendSwipeSegment(
                                event.getHistoricalX(historyIndex),
                                event.getHistoricalY(historyIndex)
                            )
                        }
                        appendSwipeSegment(event.x, event.y)
                    }
                } else if (downKey?.kind == KeyboardKey.Kind.SPACE) {
                    val threshold = 22f * density
                    val delta = event.x - lastCursorX
                    if (kotlin.math.abs(delta) >= threshold) {
                        val steps = (kotlin.math.abs(delta) / threshold).toInt().coerceAtMost(5)
                        repeat(steps) { onKey(if (delta < 0) "__CURSOR_LEFT__" else "__CURSOR_RIGHT__") }
                        lastCursorX += (if (delta < 0) -1 else 1) * steps * threshold
                        cursorMoved = true
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                handler.removeCallbacks(deleteRepeatRunnable)
                val key = downKey
                if (swipeTyping) {
                    for (historyIndex in 0 until event.historySize) {
                        appendSwipeSegment(
                            event.getHistoricalX(historyIndex),
                            event.getHistoricalY(historyIndex)
                        )
                    }
                    appendSwipeSegment(event.x, event.y)
                    val letterKeys = keyRects().filter { it.first.value.length == 1 && it.first.value.first().isLetter() }
                    val swipeKeys = letterKeys.map { (key, rect) ->
                        SwipeKeyBounds(key.value, rect.left, rect.top, rect.right, rect.bottom)
                    }
                    val word = SwipeTraceDecoder.decode(
                        swipePath,
                        swipeKeys,
                        density,
                        activeLanguageLabel == "English"
                    )
                    if (word.length >= 3) {
                        onKey("__SWIPE_WORD__" + if (shift || capsLock) upper(word) else word)
                        if (shift && !capsLock) shift = false
                    } else if (key != null) {
                        onKey(if (shift || capsLock) upper(key.value) else key.value)
                    }
                } else if (longPressed && alternateOptions.isNotEmpty()) {
                    if (selectedAlternate >= 0) {
                        val selected = alternateOptions[selectedAlternate]
                        commitAlternateOption(selected)
                    } else if (key != null) {
                        onKey(if (shift || capsLock) upper(key.value) else key.value)
                    }
                } else if (key != null && !longPressed && !cursorMoved) {
                    when (key.label) {
                        "⇧" -> {
                            if (shift && !capsLock) {
                                capsLock = true
                            } else if (capsLock) {
                                capsLock = false
                                shift = false
                            } else {
                                shift = true
                            }
                            invalidate()
                        }
                        "⌫" -> onKey("⌫")
                        "=\\<" -> setSymbolsPageTwo(true)
                        "123" -> setSymbolsPageTwo(false)
                        "?123" -> showSymbolsPanel()
                        "ABC" -> showLetterPanel()
                        "😊" -> showEmojiPanel()
                        "🌐" -> onKey("🌐")
                        activeLanguageLabel -> onKey(" ")
                        else -> {
                            val output = if (emojiMode) preferredEmoji(key.value) else key.value
                            if (emojiMode && output.isNotEmpty()) rememberEmoji(output)
                            if (key.value.isNotEmpty()) onKey(if ((shift || capsLock) && !symbols && !emojiMode && key.value.length == 1) upper(key.value) else output)
                            if (shift && !capsLock && !symbols && !emojiMode && key.value.length == 1) shift = false
                        }
                    }
                }
                // End the completed gesture through the same reset path used
                // for cancellation and panel changes; no long-press or cursor
                // state should leak into accessibility actions after ACTION_UP.
                cancelTransientKeyInteraction()
                accessibilityHelper.invalidateRoot()
                invalidate()
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Multi-finger gestures are not supported. Cancel the original
                // key gesture so lifting either finger cannot insert a character.
                multiTouchGuard.onAdditionalPointerDown()
                cancelTransientKeyInteraction()
                // Removing a long-press popup also removes its virtual
                // accessibility nodes; refresh the tree immediately.
                accessibilityHelper.invalidateRoot()
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                // Android can cancel a gesture without a normal ACTION_UP
                // (for example, when the IME is interrupted). Clear every
                // transient flag, including long-press and cursor movement.
                cancelTransientKeyInteraction()
                accessibilityHelper.invalidateRoot()
                invalidate()
                return true
            }
        }
        return true
    }
}
