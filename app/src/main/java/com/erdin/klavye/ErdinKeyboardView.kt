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

private data class KeyboardKey(
    val label: String,
    val value: String = label,
    val weight: Float = 1f,
    val kind: Kind = Kind.NORMAL
) {
    enum class Kind { NORMAL, ACTION, ACCENT, SPACE }
}

class ErdinKeyboardView(context: Context, private val onKey: (String) -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private val gap = 5f * density
    private val topInset = 10f * density
    private var shift = false
    private var symbols = false
    private var downKey: KeyboardKey? = null
    private var downRect: RectF? = null
    private var longPressed = false
    private val handler = Handler(Looper.getMainLooper())

    private val letterRows = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0").map { KeyboardKey(it) },
        "qwertyuıop".map { KeyboardKey(it.toString()) },
        "asdfghjkl".map { KeyboardKey(it.toString()) },
        listOf(KeyboardKey("⇧", kind = KeyboardKey.Kind.ACTION, weight = 1.35f)) +
            "zxcvbnm".map { KeyboardKey(it.toString()) } +
            listOf(KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.35f)),
        listOf(
            KeyboardKey("?123", kind = KeyboardKey.Kind.ACCENT, weight = 1.55f),
            KeyboardKey(",", weight = .8f),
            KeyboardKey("🌐", kind = KeyboardKey.Kind.ACTION, weight = .9f),
            KeyboardKey("Türkçe  •  English", " ", 4.2f, KeyboardKey.Kind.SPACE),
            KeyboardKey(".", weight = .8f),
            KeyboardKey("↵", kind = KeyboardKey.Kind.ACCENT, weight = 1.25f)
        )
    )

    private val symbolRows: List<List<KeyboardKey>> = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0").map { KeyboardKey(it) },
        listOf("@","#","₺","%","&","-","+","(",")","/").map { KeyboardKey(it) },
        listOf("*","\"","'",":",";","!","?","€","£","₺").map { KeyboardKey(it) },
        listOf(KeyboardKey("=\\<", kind = KeyboardKey.Kind.ACTION, weight = 1.35f)) +
            listOf("_","\\","|","~","<",">","[").map { KeyboardKey(it) } +
            listOf(KeyboardKey("⌫", kind = KeyboardKey.Kind.ACTION, weight = 1.35f)),
        listOf(
            KeyboardKey("ABC", kind = KeyboardKey.Kind.ACCENT, weight = 1.55f),
            KeyboardKey(",", weight = .8f),
            KeyboardKey("🌐", kind = KeyboardKey.Kind.ACTION, weight = .9f),
            KeyboardKey("Türkçe  •  English", " ", 4.2f, KeyboardKey.Kind.SPACE),
            KeyboardKey(".", weight = .8f),
            KeyboardKey("↵", kind = KeyboardKey.Kind.ACCENT, weight = 1.25f)
        )
    )

    private fun rows() = if (symbols) symbolRows else letterRows

    init { isFocusable = true }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, (density * 278).toInt())
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
        val bg = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(0xFF101522.toInt(), 0xFF151D31.toInt(), 0xFF101522.toInt()),
            null, Shader.TileMode.CLAMP)
        paint.shader = bg
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        keyRects().forEach { (key, rect) ->
            paint.setShadowLayer(3f * density, 0f, 2f * density, 0x66000000)
            paint.color = when (key.kind) {
                KeyboardKey.Kind.SPACE -> 0xFF263653.toInt()
                KeyboardKey.Kind.ACCENT -> 0xFF244B78.toInt()
                KeyboardKey.Kind.ACTION -> 0xFF29344B.toInt()
                KeyboardKey.Kind.NORMAL -> 0xFF202B40.toInt()
            }
            canvas.drawRoundRect(rect, 10f * density, 10f * density, paint)
            paint.clearShadowLayer()

            if (key.kind == KeyboardKey.Kind.ACCENT) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f * density
                paint.color = 0x6657B8FF
                canvas.drawRoundRect(rect, 10f * density, 10f * density, paint)
                paint.style = Paint.Style.FILL
            }

            val label = when {
                key.kind == KeyboardKey.Kind.SPACE -> key.label
                shift && !symbols && key.label.length == 1 -> key.label.uppercase()
                else -> key.label
            }
            paint.color = if (key.kind == KeyboardKey.Kind.SPACE) 0xFFB9C9E2.toInt() else 0xFFF4F7FF.toInt()
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
    }

    private fun findKey(x: Float, y: Float): Pair<KeyboardKey, RectF>? =
        keyRects().firstOrNull { it.second.contains(x, y) }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val hit = findKey(event.x, event.y)
                downKey = hit?.first
                downRect = hit?.second
                longPressed = false
                if (downKey != null) {
                    handler.postDelayed({
                        val key = downKey ?: return@postDelayed
                        if (key.label == "⌫") {
                            longPressed = true
                            onKey("⌫⌫")
                        }
                    }, 420)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacksAndMessages(null)
                val key = downKey
                if (key != null && !longPressed) {
                    when (key.label) {
                        "⇧" -> { shift = !shift; invalidate() }
                        "⌫" -> onKey("⌫")
                        "?123" -> { symbols = true; invalidate() }
                        "ABC" -> { symbols = false; invalidate() }
                        "Türkçe  •  English" -> onKey(" ")
                        else -> onKey(if (shift && !symbols && key.value.length == 1) key.value.uppercase() else key.value)
                    }
                }
                downKey = null
                downRect = null
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacksAndMessages(null)
                downKey = null
                downRect = null
                return true
            }
        }
        return true
    }
}