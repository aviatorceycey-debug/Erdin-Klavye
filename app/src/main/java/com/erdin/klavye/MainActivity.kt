package com.erdin.klavye

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.view.View
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.SeekBar
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.Switch

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(0xFF101522.toInt())
        }
        val title = TextView(this).apply {
            text = "ERDİN KLAVYE"
            textSize = 26f
            setTextColor(0xFFF4F7FF.toInt())
        }
        val info = TextView(this).apply {
            text = "Kurulum için önce klavyeyi etkinleştir, ardından varsayılan klavye olarak seç."
            textSize = 15f
            setTextColor(0xFFB8C3D9.toInt())
            setPadding(0, 18, 0, 24)
        }
        val enable = Button(this).apply {
            text = "Klavyeyi etkinleştir"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        }
        val choose = Button(this).apply {
            text = "Varsayılan klavyeyi seç"
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        }
        val heightLabel = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            setPadding(0, 28, 0, 8)
        }
        val prefs = getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
        val heightSeek = SeekBar(this).apply {
            max = 120
            progress = (prefs.getInt("keyboard_height_dp", 310) - 260).coerceIn(0, 120)
        }
        fun updateHeightLabel(value: Int) {
            heightLabel.text = "Klavye yüksekliği  •  $value dp"
        }
        updateHeightLabel(heightSeek.progress + 260)
        heightSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 260
                updateHeightLabel(value)
                prefs.edit().putInt("keyboard_height_dp", value).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        root.addView(title); root.addView(info); root.addView(enable); root.addView(choose)

        val hapticSwitch = Switch(this).apply {
            text = "Tuş titreşimi"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            isChecked = prefs.getBoolean("haptic_enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("haptic_enabled", checked).apply()
            }
        }
        hapticSwitch.setPadding(0, 22, 0, 8)
        root.addView(hapticSwitch)

        val autoCapsSwitch = Switch(this).apply {
            text = "Otomatik büyük harf"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            isChecked = prefs.getBoolean("auto_capitalization", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("auto_capitalization", checked).apply()
            }
        }
        autoCapsSwitch.setPadding(0, 10, 0, 8)
        root.addView(autoCapsSwitch)
        val handLabel = TextView(this).apply {
            text = "Tek elle kullanım"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            setPadding(0, 24, 0, 8)
        }
        val handGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        val handOptions = listOf("Normal" to "normal", "Sol el" to "left", "Sağ el" to "right")
        handOptions.forEach { (label, value) ->
            handGroup.addView(RadioButton(this).apply {
                text = label
                tag = value
                setTextColor(0xFFEAF1FC.toInt())
                id = View.generateViewId()
            })
        }
        val selectedMode = prefs.getString("one_handed_mode", "normal") ?: "normal"
        for (i in 0 until handGroup.childCount) {
            val child = handGroup.getChildAt(i) as RadioButton
            if (child.tag == selectedMode) handGroup.check(child.id)
        }
        handGroup.setOnCheckedChangeListener { group, checkedId ->
            val selected = group.findViewById<RadioButton>(checkedId)?.tag as? String ?: "normal"
            prefs.edit().putString("one_handed_mode", selected).apply()
        }
        root.addView(heightLabel); root.addView(heightSeek)
        root.addView(handLabel); root.addView(handGroup)

        val themeLabel = TextView(this).apply {
            text = "Klavye teması"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            setPadding(0, 24, 0, 8)
        }
        val themeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        val themeOptions = listOf("Gece" to "midnight", "Okyanus" to "ocean", "Menekşe" to "violet")
        themeOptions.forEach { (label, value) ->
            themeGroup.addView(RadioButton(this).apply {
                text = label
                tag = value
                setTextColor(0xFFEAF1FC.toInt())
                id = View.generateViewId()
            })
        }
        val selectedTheme = prefs.getString("keyboard_theme", "midnight") ?: "midnight"
        for (i in 0 until themeGroup.childCount) {
            val child = themeGroup.getChildAt(i) as RadioButton
            if (child.tag == selectedTheme) themeGroup.check(child.id)
        }
        themeGroup.setOnCheckedChangeListener { group, checkedId ->
            val selected = group.findViewById<RadioButton>(checkedId)?.tag as? String ?: "midnight"
            prefs.edit().putString("keyboard_theme", selected).apply()
        }
        root.addView(themeLabel); root.addView(themeGroup)
        setContentView(root)
    }
}
