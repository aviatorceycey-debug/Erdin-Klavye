package com.erdin.klavye

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class ErdinKeyboardView(context: Context, private val onKey: (String) -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rows = listOf(
        listOf("1","2","3","4","5","6","7","8","9","0"),
        listOf("q","w","e","r","t","y","u","ı","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
        listOf("?123",",","🌐","SPACE",".","↵")
    )
    private val alternatives = mapOf("g" to "ğ", "u" to "ü", "c" to "ç", "s" to "ş", "o" to "ö", "ı" to "i")
    private var shift = false
    private var downKey: String? = null
    private var downIndex = -1
    private var longPressed = false
    private val handler = Handler(Looper.getMainLooper())
    private val longPress = Runnable {
        val key = downKey ?: return@Runnable
        val alt = alternatives[key]
        if (alt != null) {
            longPressed = true
            onKey(alt)
        }
    }

    init { isFocusable = true }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, (resources.displayMetrics.density * 252).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(0xFF101522.toInt())
        val density = resources.displayMetrics.density
        val gap = 4f * density
        val top = 8f * density
        val rowHeight = (height - top - gap * 6) / rows.size
        rows.forEachIndexed { rowIndex, row ->
            val keyWidth = (width - gap * (row.size + 1)) / row.size
            row.forEachIndexed { col, key ->
                val left = gap + col * (keyWidth + gap)
                val rect = RectF(left, top + rowIndex * (rowHeight + gap), left + keyWidth, top + rowIndex * (rowHeight + gap) + rowHeight)
                paint.color = if (key == "SPACE") 0xFF28334A.toInt() else 0xFF202B40.toInt()
                canvas.drawRoundRect(rect, 9f * density, 9f * density, paint)
                paint.color = 0xFFF3F6FF.toInt()
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = if (key == "SPACE") 12f * density else 17f * density
                val label = if (key == "SPACE") "Türkçe • English" else if (shift && key.length == 1) key.uppercase() else key
                val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2
                canvas.drawText(label, rect.centerX(), baseline, paint)
            }
        }
    }

    private fun hitKey(x: Float, y: Float): String? {
        val gap = 4f * resources.displayMetrics.density
        val top = 8f * resources.displayMetrics.density
        val rowHeight = (height - top - gap * 6) / rows.size
        val row = ((y - top) / (rowHeight + gap)).toInt()
        if (row !in rows.indices) return null
        val keyWidth = (width - gap * (rows[row].size + 1)) / rows[row].size
        val col = ((x - gap) / (keyWidth + gap)).toInt()
        return rows[row].getOrNull(col)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downKey = hitKey(event.x, event.y)
                downIndex = rows.indexOfFirst { downKey in it }
                longPressed = false
                if (downKey != null) handler.postDelayed(longPress, 420)
                return true
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                val key = downKey
                if (key != null && !longPressed) {
                    when (key) {
                        "⇧" -> { shift = !shift; invalidate() }
                        "SPACE" -> onKey(" ")
                        "?123" -> onKey("?123")
                        else -> onKey(if (shift && key.length == 1) key.uppercase() else key)
                    }
                }
                downKey = null
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPress); downKey = null; return true
            }
        }
        return true
    }
}
