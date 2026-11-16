package com.erdin.klavye

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
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

class ErdinKeyboardView(
    context: Context,
    private val onKey: (String) -> Unit,
    private val onSuggestion: (String) -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private fun upper(value: String) = value.uppercase(Locale("tr", "TR"))
    private val density = resources.displayMetrics.density
    private val gap = 5f * density
    private val topInset = 42f * density
    private var suggestions: List<String> = emptyList()
    private val suggestionRects = mutableListOf<RectF>()
    private val handler = Handler(Looper.getMainLooper())
    private var shift = false
    private var capsLock = false
    private var symbols = false
    private var emojiMode = false
    private var downKey: KeyboardKey? = null
    private var downRect: RectF? = null
    private var longPressed = false
    private var alternateOptions: List<String> = emptyList()
    private var popupRect: RectF? = null
    private var selectedAlternate = -1
    private var lastCursorX = 0f
    private var cursorMoved = false
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

    private val letterRows = listOf(
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
            KeyboardKey("Türkçe  •  English", " ", 4.2f, KeyboardKey.Kind.SPACE),
            KeyboardKey(".", weight = .8f),
            KeyboardKey("↵", kind = KeyboardKey.Kind.ACCENT, weight = 1.25f)
        )
    )

    private val emojiRows = listOf(
        listOf("😀","😃","😄","😁","😆","😅","😂","🤣").map { KeyboardKey(it) },
        listOf("😊","😍","🥰","😘","😎","🤩","🥳","😭").map { KeyboardKey(it) },
        listOf("👍","👎","👏","🙏","❤️","🔥","✨","💯").map { KeyboardKey(it) },
        listOf("🎉","🌹","☀️","🌙","🐱","🐶","🍀","☕").map { KeyboardKey(it) },
        listOf(
            KeyboardKey("ABC", kind = KeyboardKey.Kind.ACCENT, weight = 1.2f),
            KeyboardKey("🌐", kind = KeyboardKey.Kind.ACTION, weight = .9f),
            KeyboardKey(" ", " ", 4.8f, KeyboardKey.Kind.SPACE),
            KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.1f)
        )
    )

    private val symbolRows = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0").map { KeyboardKey(it) },
        listOf("@","#","₺","%","&","-","+","(",")","/").map { KeyboardKey(it) },
        listOf("*","\"","'",":",";","!","?","€","£","₺").map { KeyboardKey(it) },
        listOf(KeyboardKey("=\\<", kind = KeyboardKey.Kind.ACTION, weight = 1.35f)) +
            listOf("_","\\","|","~","<",">","[").map { KeyboardKey(it) } +
            listOf(KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.35f)),
        listOf(
            KeyboardKey("ABC", kind = KeyboardKey.Kind.ACCENT, weight = 1.55f),
            KeyboardKey(",", weight = .8f),
            KeyboardKey("😊", kind = KeyboardKey.Kind.ACTION, weight = .9f),
            KeyboardKey("Türkçe  •  English", " ", 4.2f, KeyboardKey.Kind.SPACE),
            KeyboardKey(".", weight = .8f),
            KeyboardKey("↵", kind = KeyboardKey.Kind.ACCENT, weight = 1.25f)
        )
    )

    private fun rows() = when {
        emojiMode -> emojiRows
        symbols -> symbolRows
        else -> letterRows
    }

    private val longPressRunnable = Runnable {
        val key = downKey ?: return@Runnable
        if (key.label == "⌫") {
            longPressed = true
            onKey("⌫")
            handler.postDelayed(deleteRepeatRunnable, 180)
        } else if (!symbols && !emojiMode && key.value.length == 1) {
            val options = alternatives[key.value.lowercase()]
            if (!options.isNullOrEmpty()) {
                longPressed = true
                alternateOptions = options
                selectedAlternate = -1
                invalidate()
            }
        }
    }

    fun setSuggestions(items: List<String>) {
        suggestions = items.distinct().take(3)
        invalidate()
    }

    init { isFocusable = true }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val preferredHeight = context.getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
            .getInt("keyboard_height_dp", 310)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (density * preferredHeight.coerceIn(260, 380)).toInt())
    }

    private fun keyRects(): List<Pair<KeyboardKey, RectF>> {
        val current = rows()
        val rowHeight = (height - topInset - gap * (current.size + 1)) / current.size
        val result = mutableListOf<Pair<KeyboardKey, RectF>>()
        current.forEachIndexed { rowIndex, row ->
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat()
            val available = width - gap * (row.size + 1)
            var x = gap
            row.forEach { key ->
                val keyWidth = available * key.weight / totalWeight
                val y = topInset + gap + rowIndex * (rowHeight + gap)
                result += key to RectF(x, y, x + keyWidth, y + rowHeight)
                x += keyWidth + gap
            }
        }
        return result
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(0xFF0C1220.toInt(), 0xFF17243B.toInt(), 0xFF101827.toInt()),
            null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        drawSuggestionStrip(canvas)
        keyRects().forEach { (key, rect) ->
            paint.setShadowLayer(4f * density, 0f, 2f * density, 0x77000000)
            paint.color = when (key.kind) {
                KeyboardKey.Kind.SPACE -> 0xFF263B5D.toInt()
                KeyboardKey.Kind.ACCENT -> 0xFF24558A.toInt()
                KeyboardKey.Kind.ACTION -> 0xFF293750.toInt()
                KeyboardKey.Kind.NORMAL -> 0xFF202D44.toInt()
            }
            canvas.drawRoundRect(rect, 10f * density, 10f * density, paint)
            paint.clearShadowLayer()
            if (key.kind == KeyboardKey.Kind.ACCENT || key.kind == KeyboardKey.Kind.SPACE) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = density
                paint.color = if (key.kind == KeyboardKey.Kind.SPACE) 0x553F9DFF.toInt() else 0x885ABEFF.toInt()
                canvas.drawRoundRect(rect, 10f * density, 10f * density, paint)
                paint.style = Paint.Style.FILL
            }
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
        }
        drawAlternatePopup(canvas)
    }

    private fun drawSuggestionStrip(canvas: Canvas) {
        suggestionRects.clear()
        if (emojiMode || symbols) return
        val count = suggestions.size
        if (count == 0) {
            paint.color = 0xFF9EABC2.toInt()
            paint.textSize = 11f * density
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("ERDİN  •  Türkçe klavye", width / 2f, 25f * density, paint)
            return
        }
        val cellWidth = width.toFloat() / count
        suggestions.forEachIndexed { index, suggestion ->
            val rect = RectF(index * cellWidth + 4f * density, 5f * density,
                (index + 1) * cellWidth - 4f * density, 37f * density)
            suggestionRects += rect
            if (index == 1) {
                paint.color = 0x332D83D5
                canvas.drawRoundRect(rect, 9f * density, 9f * density, paint)
            }
            paint.color = 0xFFEAF1FC.toInt()
            paint.textSize = 14f * density
            paint.textAlign = Paint.Align.CENTER
            val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2f
            val display = if (paint.measureText(suggestion) > rect.width() - 8f * density) {
                android.text.TextUtils.ellipsize(suggestion, android.text.TextPaint(paint),
                    rect.width() - 8f * density, android.text.TextUtils.TruncateAt.END).toString()
            } else suggestion
            canvas.drawText(display, rect.centerX(), baseline, paint)
        }
    }

    private fun drawAlternatePopup(canvas: Canvas) {
        val anchor = downRect ?: return
        if (alternateOptions.isEmpty()) return
        val cellWidth = 46f * density
        val popupHeight = 52f * density
        val popupWidth = cellWidth * alternateOptions.size
        val left = (anchor.centerX() - popupWidth / 2f).coerceIn(gap, width - popupWidth - gap)
        val top = (anchor.top - popupHeight - 7f * density).coerceAtLeast(3f * density)
        val rect = RectF(left, top, left + popupWidth, top + popupHeight)
        popupRect = rect
        paint.setShadowLayer(9f * density, 0f, 3f * density, 0xAA000000.toInt())
        paint.color = 0xFF263C5D.toInt()
        canvas.drawRoundRect(rect, 13f * density, 13f * density, paint)
        paint.clearShadowLayer()
        alternateOptions.forEachIndexed { index, value ->
            val cell = RectF(left + index * cellWidth, top, left + (index + 1) * cellWidth, top + popupHeight)
            if (index == selectedAlternate) {
                paint.color = 0xFF287DCE.toInt()
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
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!emojiMode && !symbols) {
                    val suggestionIndex = suggestionRects.indexOfFirst { it.contains(event.x, event.y) }
                    if (suggestionIndex >= 0 && suggestionIndex < suggestions.size) {
                        onSuggestion(suggestions[suggestionIndex])
                        return true
                    }
                }
                val hit = findKey(event.x, event.y)
                downKey = hit?.first
                downRect = hit?.second
                if (hit != null) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                longPressed = false
                cursorMoved = false
                lastCursorX = event.x
                alternateOptions = emptyList()
                popupRect = null
                selectedAlternate = -1
                if (downKey != null) handler.postDelayed(longPressRunnable, 380)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (longPressed && alternateOptions.isNotEmpty()) {
                    updateAlternateSelection(event.x)
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
                if (longPressed && alternateOptions.isNotEmpty()) {
                    if (selectedAlternate >= 0) {
                        onKey(alternateOptions[selectedAlternate])
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
                        "?123" -> { symbols = true; emojiMode = false; invalidate() }
                        "ABC" -> { symbols = false; emojiMode = false; invalidate() }
                        "😊" -> { emojiMode = true; symbols = false; invalidate() }
                        "🌐" -> onKey("🌐")
                        "Türkçe  •  English" -> onKey(" ")
                        else -> {
                            onKey(if ((shift || capsLock) && !symbols && !emojiMode && key.value.length == 1) upper(key.value) else key.value)
                            if (shift && !capsLock && !symbols && !emojiMode && key.value.length == 1) shift = false
                        }
                    }
                }
                downKey = null
                downRect = null
                alternateOptions = emptyList()
                popupRect = null
                selectedAlternate = -1
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                handler.removeCallbacks(deleteRepeatRunnable)
                downKey = null
                downRect = null
                alternateOptions = emptyList()
                popupRect = null
                selectedAlternate = -1
                invalidate()
                return true
            }
        }
        return true
    }
}
