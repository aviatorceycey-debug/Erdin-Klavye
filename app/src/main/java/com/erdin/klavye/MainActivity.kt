package com.erdin.klavye

import android.app.Activity
import android.app.AlertDialog
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
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {
    private var setupStatus: TextView? = null
    private var enableKeyboardButton: Button? = null
    private var chooseKeyboardButton: Button? = null

    override fun onResume() {
        super.onResume()
        refreshSetupStatus()
    }

    private fun refreshSetupStatus() {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = manager.enabledInputMethodList.any { it.packageName == packageName }
        val defaultIme = Settings.Secure.getString(
            contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
        ).orEmpty()
        val selected = defaultIme.startsWith("$packageName/")
        enableKeyboardButton?.text = if (enabled) "✓ Klavye etkin (ayarları aç)" else "1. Klavyeyi etkinleştir"
        chooseKeyboardButton?.text = if (selected) "✓ Varsayılan klavye seçili" else "2. Erdin Klavye'yi varsayılan seç"
        setupStatus?.apply {
            text = buildString {
                append(if (enabled) "✓ Klavye etkin" else "1. Klavyeyi etkinleştir")
                append("\n")
                append(if (selected) "✓ Varsayılan klavye seçili" else "2. Erdin Klavye'yi varsayılan seç")
            }
            contentDescription = text
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            setTextColor(if (enabled && selected) 0xFF72E0B0.toInt() else 0xFFFFD18A.toInt())
        }
    }
    private fun showPersonalDictionary() {
        val model = LocalLanguageModel(this)
        val search = android.widget.EditText(this).apply {
            hint = "Kelime ara"
            contentDescription = "Kişisel sözlükte kelime ara"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setTextColor(0xFFF4F7FF.toInt())
            setHintTextColor(0xFF8492AA.toInt())
            setPadding(24, 18, 24, 18)
        }
        val emptyLabel = TextView(this).apply {
            text = "Henüz öğrenilmiş kelime yok. Kişisel öğrenme açıkken klavye kullandığın kelimeleri cihazında öğrenir."
            textSize = 14f
            setTextColor(0xFFB8C3D9.toInt())
            setPadding(16, 28, 16, 28)
            visibility = View.GONE
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val list = android.widget.ListView(this).apply {
            contentDescription = "Öğrenilmiş kişisel kelimeler"
            divider = android.graphics.drawable.ColorDrawable(0x263D5275)
            dividerHeight = 1
            isFastScrollEnabled = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
            addView(search, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(emptyLabel, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(list, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (280 * resources.displayMetrics.density).toInt()
            ))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Kişisel sözlük")
            .setView(content)
            .setPositiveButton("Kapat", null)
            .create()

        var allWords = model.learnedWords()
        var visibleWords = allWords
        val adapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_list_item_1,
            visibleWords.map { (word, count) -> "$word     ·     $count kullanım" }
        )
        list.adapter = adapter

        fun refresh(query: String = search.text.toString()) {
            allWords = model.learnedWords()
            val normalizedQuery = query.trim().lowercase(Locale("tr", "TR"))
            visibleWords = allWords.filter {
                it.first.lowercase(Locale("tr", "TR")).contains(normalizedQuery)
            }
            adapter.clear()
            adapter.addAll(visibleWords.map { (word, count) -> "$word     ·     $count kullanım" })
            adapter.notifyDataSetChanged()
            val hasWords = allWords.isNotEmpty()
            search.visibility = if (hasWords) View.VISIBLE else View.GONE
            list.visibility = if (visibleWords.isNotEmpty()) View.VISIBLE else View.GONE
            emptyLabel.visibility = if (visibleWords.isEmpty()) View.VISIBLE else View.GONE
            emptyLabel.text = when {
                !hasWords -> "Henüz öğrenilmiş kelime yok. Kişisel öğrenme açıkken klavye kullandığın kelimeleri cihazında öğrenir."
                query.isNotBlank() -> "“${query.trim()}” için eşleşen kelime bulunamadı."
                else -> "Kişisel sözlük boş."
            }
            dialog.setTitle("Kişisel sözlük  ·  ${visibleWords.size}/${allWords.size}")
        }

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refresh(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })

        list.setOnItemClickListener { _, _, position, _ ->
            val selected = visibleWords.getOrNull(position)?.first ?: return@setOnItemClickListener
            AlertDialog.Builder(this)
                .setTitle("\"$selected\" silinsin mi?")
                .setMessage("Kelimenin öğrenilmiş kullanım sayısı ve ona bağlı tahmin kayıtları silinecek.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sil") { _, _ ->
                    model.deletePersonalWord(selected)
                    Toast.makeText(this, "\"$selected\" silindi", Toast.LENGTH_SHORT).show()
                    refresh()
                }
                .show()
        }

        dialog.setOnShowListener {
            refresh("")
        }
        dialog.show()
    }

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
        }.also { enableKeyboardButton = it }
        val choose = Button(this).apply {
            text = "Varsayılan klavyeyi seç"
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        }.also { chooseKeyboardButton = it }
        val heightLabel = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            setPadding(0, 28, 0, 8)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val prefs = getSharedPreferences("erdin_keyboard_settings", Context.MODE_PRIVATE)
        val heightSeek = SeekBar(this).apply {
            max = 120
            progress = (prefs.getInt("keyboard_height_dp", 310) - 260).coerceIn(0, 120)
            contentDescription = "Klavye yüksekliği ayarı, 260 ile 380 dp arasında"
        }
        fun updateHeightLabel(value: Int) {
            heightLabel.text = "Klavye yüksekliği  •  $value dp"
            heightLabel.contentDescription = "Seçilen klavye yüksekliği: $value dp"
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
        setupStatus = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFFFD18A.toInt())
            setPadding(18, 16, 18, 16)
            setBackgroundColor(0xFF1B2638.toInt())
        }
        root.addView(title); root.addView(info); root.addView(setupStatus)
        root.addView(enable); root.addView(choose)

        val keyboardTestLabel = TextView(this).apply {
            text = "Klavye deneme alanı"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            setPadding(0, 24, 0, 8)
        }
        val keyboardTestField = android.widget.EditText(this).apply {
            hint = "Buraya yazarak Erdin Klavye'yi dene…"
            textSize = 16f
            minLines = 2
            maxLines = 3
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setTextColor(0xFFF4F7FF.toInt())
            setHintTextColor(0xFF8492AA.toInt())
            setPadding(18, 16, 18, 16)
            contentDescription = "Erdin Klavye deneme yazı alanı"
        }
        root.addView(keyboardTestLabel)
        root.addView(keyboardTestField, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

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

        val keySoundSwitch = Switch(this).apply {
            text = "Tuş sesi"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            isChecked = prefs.getBoolean("key_sound_enabled", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("key_sound_enabled", checked).apply()
            }
        }
        keySoundSwitch.setPadding(0, 10, 0, 8)
        root.addView(keySoundSwitch)

        val keyPreviewSwitch = Switch(this).apply {
            text = "Tuşa basınca harf önizlemesi"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            isChecked = prefs.getBoolean("key_preview_enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("key_preview_enabled", checked).apply()
            }
        }
        keyPreviewSwitch.setPadding(0, 10, 0, 8)
        root.addView(keyPreviewSwitch)

        val swipeTypingSwitch = Switch(this).apply {
            text = "Kaydırarak yazma (deneysel)"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            isChecked = prefs.getBoolean("swipe_typing_enabled", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("swipe_typing_enabled", checked).apply()
            }
        }
        swipeTypingSwitch.setPadding(0, 10, 0, 8)
        root.addView(swipeTypingSwitch)

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

        val doubleSpaceSwitch = Switch(this).apply {
            text = "Çift boşlukla nokta koy"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            isChecked = prefs.getBoolean("double_space_period", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("double_space_period", checked).apply()
            }
        }
        doubleSpaceSwitch.setPadding(0, 10, 0, 8)
        root.addView(doubleSpaceSwitch)

        val suggestionsSwitch = Switch(this).apply {
            text = "Kelime önerileri ve düzeltme"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            isChecked = prefs.getBoolean("suggestions_enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("suggestions_enabled", checked).apply()
            }
        }
        suggestionsSwitch.setPadding(0, 10, 0, 8)
        root.addView(suggestionsSwitch)

        val learningSwitch = Switch(this).apply {
            text = "Kişisel kelime öğrenme"
            textSize = 15f
            setTextColor(0xFFEAF1FC.toInt())
            isChecked = prefs.getBoolean("personal_learning_enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("personal_learning_enabled", checked).apply()
            }
        }
        learningSwitch.setPadding(0, 10, 0, 8)
        root.addView(learningSwitch)

        val privacyNote = TextView(this).apply {
            text = "GİZLİLİK  •  Yazdıkların sunucuya gönderilmez. Kişisel kelime öğrenme verileri yalnızca bu cihazda saklanır. Şifreli ve önerilerin kapalı olduğu alanlarda öğrenme devre dışıdır. İstediğinde aşağıdan silebilirsin."
            textSize = 12f
            setTextColor(0xFF9FB4D2.toInt())
            setPadding(16, 12, 16, 12)
            setBackgroundColor(0xFF182438.toInt())
            contentDescription = text
        }
        root.addView(privacyNote)

        val clearLearningButton = Button(this).apply {
            text = "Öğrenilen kelimeleri temizle"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Kişisel kelime verileri silinsin mi?")
                    .setMessage("Öğrenilen kelimeler ve kişisel kelime sıklıkları bu cihazdan silinecek. Hazır Türkçe öneriler etkilenmez.")
                    .setNegativeButton("Vazgeç", null)
                    .setPositiveButton("Temizle") { _, _ ->
                        LocalLanguageModel(this@MainActivity).clearPersonalData()
                        Toast.makeText(this@MainActivity, "Kişisel kelime verileri temizlendi", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        }
        root.addView(clearLearningButton)

        val dictionaryButton = Button(this).apply {
            text = "Kişisel sözlüğü görüntüle"
            setOnClickListener { showPersonalDictionary() }
        }
        root.addView(dictionaryButton)
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
        val settingsScroll = android.widget.ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root)
        }
        setContentView(settingsScroll)
    }
}
