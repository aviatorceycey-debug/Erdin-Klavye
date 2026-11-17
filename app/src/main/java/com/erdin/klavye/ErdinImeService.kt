package com.erdin.klavye

import android.inputmethodservice.InputMethodService
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import android.text.InputType
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

class ErdinImeService : InputMethodService(), SpellCheckerSession.SpellCheckerSessionListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var spellCheckerSession: SpellCheckerSession? = null
    private var spellCheckerLocale: Locale? = null
    private var keyboardView: ErdinKeyboardView? = null
    private var activeSubtypeLocale = "tr_TR"
    private var lastQueriedWord = ""
    private var suggestionRequestSequence = 0
    private var lastContextWord = ""
    private var lastContextPreviousWord = ""
    private lateinit var languageModel: LocalLanguageModel
    private var learningEnabled = true
    private var suggestionsEnabled = true
    private var doubleSpacePeriodEnabled = true
    private var clipboardEnabled = true
    private val clipboardHistory = mutableListOf<String>()
    private var pendingSuggestionRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        languageModel = LocalLanguageModel(this)
    }

    override fun onDestroy() {
        cancelPendingSuggestion()
        suggestionRequestSequence++
        spellCheckerSession?.close()
        spellCheckerSession = null
        spellCheckerLocale = null
        keyboardView = null
        clipboardHistory.clear()
        super.onDestroy()
    }

    override fun onCurrentInputMethodSubtypeChanged(subtype: InputMethodSubtype) {
        super.onCurrentInputMethodSubtypeChanged(subtype)
        activeSubtypeLocale = subtype.locale.ifBlank { "tr_TR" }
        keyboardView?.setInputLanguage(activeSubtypeLocale)
        keyboardView?.setEditorActionDescription(editorActionDescription(currentInputEditorInfo))
        updateAutoShift()
        invalidateSpellCheckRequests()
        keyboardView?.setSuggestions(emptyList())
        currentInputEditorInfo?.let { attribute ->
            // A language switch can happen in the middle of an editor session.
            // Rebuild phrase context from the actual cursor position so the
            // newly selected language never inherits stale context from before.
            val textBeforeCursor = currentInputConnection
                ?.getTextBeforeCursor(120, 0)?.toString().orEmpty()
            val contextWords = textBeforeCursor
                .split(Regex("[^\\p{L}'’]+"))
                .filter { it.isNotBlank() }
            lastContextWord = contextWords.lastOrNull().orEmpty()
            lastContextPreviousWord = contextWords.getOrNull(contextWords.lastIndex - 1).orEmpty()

            val inputType = attribute.inputType
            keyboardView?.setNumericInputMode(inputType)
            val inputClass = inputType and InputType.TYPE_MASK_CLASS
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            val plainTextField = inputClass == InputType.TYPE_CLASS_TEXT &&
                variation == InputType.TYPE_TEXT_VARIATION_NORMAL
            val passwordField = (inputClass == InputType.TYPE_CLASS_TEXT &&
                variation in setOf(
                    InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                )) || (inputClass == InputType.TYPE_CLASS_NUMBER &&
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
            val explicitlyNoSuggestions = inputClass == InputType.TYPE_CLASS_TEXT &&
                (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
            val suggestionsPreference = getSharedPreferences(
                "erdin_keyboard_settings", MODE_PRIVATE
            ).getBoolean("suggestions_enabled", true)
            suggestionsEnabled = plainTextField && !passwordField &&
                !explicitlyNoSuggestions && suggestionsPreference
            clipboardEnabled = inputClass == InputType.TYPE_CLASS_TEXT && !passwordField &&
                ((attribute?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) == 0
            keyboardView?.setClipboardAvailable(clipboardEnabled)
            if (!clipboardEnabled) {
                // A subtype change can happen without a new editor session. If the
                // destination is sensitive, discard any previously visible snippets.
                clipboardHistory.clear()
                keyboardView?.setClipboardItems(emptyList())
                keyboardView?.hideClipboard()
            }
            doubleSpacePeriodEnabled = plainTextField && !passwordField && !explicitlyNoSuggestions
            val personalLearningEnabled = getSharedPreferences(
                "erdin_keyboard_settings", MODE_PRIVATE
            ).getBoolean("personal_learning_enabled", true)
            val editorDisablesPersonalLearning =
                attribute.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0
            learningEnabled = plainTextField && !passwordField && !explicitlyNoSuggestions &&
                personalLearningEnabled && !editorDisablesPersonalLearning && usesTurkishModel()
            if (suggestionsEnabled) {
                refreshSpellCheckerSession()
                requestSuggestionsSoon()
            } else {
                invalidateSpellCheckRequests()
                spellCheckerSession?.close()
                spellCheckerSession = null
                spellCheckerLocale = null
                keyboardView?.setSuggestions(emptyList())
            }
        }
    }

    override fun onCreateInputView(): View {
        return ErdinKeyboardView(
            this,
            onKey = { value -> handleKey(value) },
            onSuggestion = { value -> applySuggestion(value) }
        ).also {
            keyboardView = it
            it.setClipboardAvailable(clipboardEnabled)
            it.setInputLanguage(activeSubtypeLocale)
            it.setEditorActionDescription(editorActionDescription(currentInputEditorInfo))
            it.setNumericInputMode(currentInputEditorInfo?.inputType ?: InputType.TYPE_NULL)
            updateAutoShift()
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        cancelPendingSuggestion()
        lastQueriedWord = ""
        suggestionRequestSequence++
        lastContextWord = ""
        lastContextPreviousWord = ""
        val inputType = attribute?.inputType ?: InputType.TYPE_NULL
        keyboardView?.resetPanelsForInput()
        keyboardView?.setNumericInputMode(inputType)
        keyboardView?.setEditorActionDescription(editorActionDescription(attribute))
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val plainTextField = inputClass == InputType.TYPE_CLASS_TEXT &&
            variation == InputType.TYPE_TEXT_VARIATION_NORMAL
        val passwordField = (inputClass == InputType.TYPE_CLASS_TEXT &&
            variation in setOf(
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )) || (inputClass == InputType.TYPE_CLASS_NUMBER &&
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        val explicitlyNoSuggestions = inputClass == InputType.TYPE_CLASS_TEXT &&
            (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        val personalLearningEnabled = getSharedPreferences("erdin_keyboard_settings", MODE_PRIVATE)
            .getBoolean("personal_learning_enabled", true)
        val editorDisablesPersonalLearning =
            (attribute?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0
        learningEnabled = plainTextField && !passwordField && !explicitlyNoSuggestions &&
            personalLearningEnabled && !editorDisablesPersonalLearning && usesTurkishModel()
        val suggestionsPreference = getSharedPreferences("erdin_keyboard_settings", MODE_PRIVATE)
            .getBoolean("suggestions_enabled", true)
        suggestionsEnabled = plainTextField && !passwordField && !explicitlyNoSuggestions &&
            suggestionsPreference
        doubleSpacePeriodEnabled = plainTextField && !passwordField && !explicitlyNoSuggestions
        clipboardEnabled = inputClass == InputType.TYPE_CLASS_TEXT && !passwordField &&
                ((attribute?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) == 0
        keyboardView?.setClipboardAvailable(clipboardEnabled)
        // Each editor session starts with an empty clipboard panel. This
        // prevents snippets copied in one app or field from appearing in another.
        clipboardHistory.clear()
        keyboardView?.setClipboardItems(emptyList())
        keyboardView?.hideClipboard()
        keyboardView?.setSuggestions(emptyList())
        updateAutoShift()
        if (suggestionsEnabled) refreshSpellCheckerSession()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd
        )
        updateAutoShift()

        // Keep suggestions aligned with the word at the cursor, including when
        // the user moves the caret with a hardware keyboard or editor controls.
        if (newSelStart != newSelEnd) {
            invalidateSpellCheckRequests()
            keyboardView?.setSuggestions(emptyList())
            // A selection changes the insertion point. Keep next-word context
            // anchored to the text before the selection, not the word that was
            // previously active at the caret.
            val beforeSelection = currentInputConnection
                ?.getTextBeforeCursor(120, 0)?.toString().orEmpty()
            val contextWords = beforeSelection
                .split(Regex("[^\\p{L}'’]+"))
                .filter { it.isNotBlank() }
            lastContextWord = contextWords.lastOrNull().orEmpty()
            lastContextPreviousWord = contextWords.getOrNull(contextWords.lastIndex - 1).orEmpty()
        } else if (newSelStart != oldSelStart || newSelEnd != oldSelEnd) {
            // Rebuild the local phrase context from the text immediately before
            // the caret. This keeps next-word learning accurate after cursor moves.
            val before = currentInputConnection?.getTextBeforeCursor(120, 0)?.toString().orEmpty()
            val words = before.split(Regex("[^\\p{L}'’]+")).filter { it.isNotBlank() }
            lastContextWord = words.lastOrNull().orEmpty()
            lastContextPreviousWord = words.getOrNull(words.lastIndex - 1).orEmpty()
            requestSuggestionsSoon()
        }
    }

    override fun onFinishInput() {
        cancelPendingSuggestion()
        suggestionRequestSequence++
        lastQueriedWord = ""
        // Drop cached phrase context at the same boundary as clipboard data.
        // It is derived from typed text and must not carry into another editor.
        lastContextWord = ""
        lastContextPreviousWord = ""
        // Clipboard snippets are intentionally session-only; discard them when
        // the input session ends so they cannot linger across apps.
        clipboardHistory.clear()
        // A finished editor session must not leave the clipboard entry point
        // active on a keyboard view that may briefly remain attached.
        clipboardEnabled = false
        keyboardView?.setClipboardAvailable(false)
        keyboardView?.setClipboardItems(emptyList())
        keyboardView?.hideClipboard()
        spellCheckerSession?.close()
        spellCheckerSession = null
        spellCheckerLocale = null
        keyboardView?.setSuggestions(emptyList())
        super.onFinishInput()
    }

    private fun editorActionDescription(info: EditorInfo?): String? {
        if (info == null) return null
        info.actionLabel?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        val english = activeSubtypeLocale.startsWith("en", ignoreCase = true)
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> if (english) "Go" else "Git"
            EditorInfo.IME_ACTION_SEARCH -> if (english) "Search" else "Ara"
            EditorInfo.IME_ACTION_SEND -> if (english) "Send" else "Gönder"
            EditorInfo.IME_ACTION_NEXT -> if (english) "Next field" else "Sonraki alan"
            EditorInfo.IME_ACTION_DONE -> if (english) "Done" else "Bitti"
            EditorInfo.IME_ACTION_PREVIOUS -> if (english) "Previous field" else "Önceki alan"
            else -> if (english) "Enter" else "Yeni satır"
        }
    }

    private fun updateAutoShift() {
        val enabled = getSharedPreferences("erdin_keyboard_settings", MODE_PRIVATE)
            .getBoolean("auto_capitalization", true)
        val capsMode = currentInputConnection?.getCursorCapsMode(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) ?: 0
        keyboardView?.setAutoShift(enabled && capsMode != 0)
    }

    // The language explicitly selected on the keyboard takes priority over an
    // app's locale hint, so switching to English works inside the same editor.
    private fun activeEditorLocale(): Locale {
        val subtype = activeSubtypeLocale
            .takeIf { it.isNotBlank() }
            ?.replace('_', '-')
            ?.let(Locale::forLanguageTag)
        if (subtype != null && subtype.language.isNotBlank()) return subtype
        currentInputEditorInfo?.hintLocales?.get(0)?.let { return it }
        return Locale("tr", "TR")
    }

    private fun refreshSpellCheckerSession() {
        val requestedLocale = activeEditorLocale()
        if (spellCheckerLocale != requestedLocale) {
            spellCheckerSession?.close()
            spellCheckerSession = null
            spellCheckerLocale = requestedLocale
        }
        if (spellCheckerSession == null) {
            val manager = getSystemService(TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager
            spellCheckerSession = manager?.newSpellCheckerSession(
                null, requestedLocale, this, true
            )
        }
    }

    private fun usesTurkishModel(): Boolean =
        activeEditorLocale().language.equals("tr", ignoreCase = true)

    private fun nextWordsForActiveLanguage(current: String, previous: String?): List<String> {
        val suggestions = if (usesTurkishModel()) {
            languageModel.nextWords(current, previous)
        } else {
            languageModel.englishNextWords(current, previous)
        }
        val autoCapitalization = getSharedPreferences(
            "erdin_keyboard_settings", MODE_PRIVATE
        ).getBoolean("auto_capitalization", true)
        val capsAtCursor = currentInputConnection?.getCursorCapsMode(
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        ) ?: 0
        if (!autoCapitalization || capsAtCursor == 0) return suggestions
        val locale = activeEditorLocale()
        return suggestions.map { word ->
            word.replaceFirstChar { it.uppercase(locale) }
        }
    }

    private fun handleKey(value: String) {
        val connection = currentInputConnection ?: return
        when {
            value.startsWith("__SWIPE_WORD__") -> {
                val traced = value.removePrefix("__SWIPE_WORD__")
                // The local swipe vocabulary is Turkish-only. Keep the raw trace in English mode
                // rather than silently replacing it with an unrelated Turkish dictionary word.
                val decoded = if (usesTurkishModel()) languageModel.decodeSwipePath(traced) else traced
                if (decoded.isNotBlank()) {
                    connection.commitText(decoded, 1)
                    updateAutoShift()
                    requestSuggestionsSoon()
                }
            }
            value == "__CLIPBOARD__" -> {
                if (!clipboardEnabled) return
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = clipboard?.primaryClip
                val markedSensitive = clipboard?.primaryClipDescription?.extras
                    ?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true
                if (markedSensitive) {
                    // Do not leave older snippets visible when the system clipboard
                    // explicitly marks its current contents as sensitive.
                    clipboardHistory.clear()
                    keyboardView?.setClipboardItems(emptyList())
                    keyboardView?.hideClipboard()
                    return
                }
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).coerceToText(this)?.toString()?.trim().orEmpty()
                    if (text.isNotBlank() && text.length <= 2000) {
                        clipboardHistory.remove(text)
                        clipboardHistory.add(0, text)
                        while (clipboardHistory.size > 10) clipboardHistory.removeAt(clipboardHistory.lastIndex)
                    }
                }
                keyboardView?.setClipboardItems(clipboardHistory.toList())
            }
            value == "__CLIP_CLEAR__" -> {
                if (!clipboardEnabled) return
                clipboardHistory.clear()
                keyboardView?.setClipboardItems(emptyList())
            }
            value.startsWith("__CLIP_ITEM__") -> {
                if (!clipboardEnabled) return
                val index = value.removePrefix("__CLIP_ITEM__").toIntOrNull()
                val item = index?.let { clipboardHistory.getOrNull(it) }
                if (item != null) {
                    currentInputConnection?.commitText(item, 1)
                    keyboardView?.hideClipboard()
                    requestSuggestionsSoon()
                }
            }
            value == "__CURSOR_LEFT__" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
            }
            value == "__CURSOR_RIGHT__" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
            }
            value == "⌫" -> {
                if (!connection.deleteSurroundingTextInCodePoints(1, 0)) {
                    connection.deleteSurroundingText(1, 0)
                }
                updateAutoShift()
                requestSuggestionsSoon()
            }
            value == "↵" -> {
                val action = currentInputEditorInfo?.imeOptions
                    ?.and(EditorInfo.IME_MASK_ACTION)
                    ?: EditorInfo.IME_ACTION_NONE
                if (action != EditorInfo.IME_ACTION_NONE &&
                    action != EditorInfo.IME_ACTION_UNSPECIFIED
                ) {
                    connection.performEditorAction(action)
                } else {
                    connection.commitText("\n", 1)
                }
                keyboardView?.setSuggestions(emptyList())
                updateAutoShift()
            }
            value == "🌐" -> switchToNextInputMethod(true)
            else -> {
                if (value == " ") {
                    invalidateSpellCheckRequests()
                    val before = connection.getTextBeforeCursor(120, 0)?.toString().orEmpty()
                    val doubleSpacePeriod = getSharedPreferences(
                        "erdin_keyboard_settings", MODE_PRIVATE
                    ).getBoolean("double_space_period", true)
                    if (doubleSpacePeriodEnabled && doubleSpacePeriod && before.endsWith(" ") &&
                        before.dropLast(1).lastOrNull()?.isLetter() == true
                    ) {
                        connection.deleteSurroundingText(1, 0)
                        connection.commitText(". ", 1)
                        keyboardView?.setSuggestions(emptyList())
                        updateAutoShift()
                        return
                    }
                    val words = before.split(Regex("[^\\p{L}'’]+")).filter { it.isNotBlank() }
                    val current = words.lastOrNull().orEmpty()
                    val previous = words.getOrNull(words.lastIndex - 1)
                    val previousPrevious = words.getOrNull(words.lastIndex - 2)
                    lastContextWord = current
                    lastContextPreviousWord = previous.orEmpty()
                    if (learningEnabled && usesTurkishModel() && current.isNotBlank()) {
                        languageModel.learn(current, previous, previousPrevious)
                    }
                    connection.commitText(value, 1)
                    updateAutoShift()
                    keyboardView?.setSuggestions(
                        if (suggestionsEnabled) nextWordsForActiveLanguage(current, previous) else emptyList()
                    )
                } else {
                    if (!value.all { it.isLetter() }) invalidateSpellCheckRequests()
                    connection.commitText(value, 1)
                    updateAutoShift()
                    if (value.all { it.isLetter() || it == '\'' || it == '’' }) requestSuggestionsSoon()
                    else keyboardView?.setSuggestions(emptyList())
                }
            }
        }
    }

    private fun invalidateSpellCheckRequests() {
        cancelPendingSuggestion()
        suggestionRequestSequence++
        lastQueriedWord = ""
    }

    private fun cancelPendingSuggestion() {
        pendingSuggestionRunnable?.let(mainHandler::removeCallbacks)
        pendingSuggestionRunnable = null
    }

    private fun requestSuggestionsSoon() {
        cancelPendingSuggestion()
        val requestSequence = ++suggestionRequestSequence
        lastQueriedWord = ""
        if (!suggestionsEnabled) {
            keyboardView?.setSuggestions(emptyList())
            return
        }
        val request = Runnable {
            if (requestSequence != suggestionRequestSequence) return@Runnable
            pendingSuggestionRunnable = null
            val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
            val word = before.takeLastWhile { it.isLetter() || it == '\'' || it == '’' }
            if (word.length < 2 || word.firstOrNull()?.isLetter() != true || word.lastOrNull()?.isLetter() != true) {
                lastQueriedWord = ""
                keyboardView?.setSuggestions(emptyList())
                return@Runnable
            }
            lastQueriedWord = word
            val local = if (usesTurkishModel()) languageModel.completions(word) else languageModel.englishCompletions(word)
            keyboardView?.setSuggestions((listOf(word) + local).take(3))
            spellCheckerSession?.getSuggestions(
                arrayOf(TextInfo(word, 0, requestSequence)), 3, false
            )
        }
        pendingSuggestionRunnable = request
        mainHandler.postDelayed(request, 180)
    }

    private fun applySuggestion(replacement: String) {
        val connection = currentInputConnection ?: return
        val before = connection.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val currentWord = before.takeLastWhile { it.isLetter() || it == '\'' || it == '’' }
        if (currentWord.isEmpty()) {
            invalidateSpellCheckRequests()
            val insertedWords = replacement.split(Regex("[^\\p{L}'’]+")).filter { it.isNotBlank() }
            var previousContext = lastContextWord
            var previousPreviousContext = lastContextPreviousWord
            if (learningEnabled && usesTurkishModel()) {
                insertedWords.forEach { word ->
                    languageModel.learn(word, previousContext, previousPreviousContext)
                    previousPreviousContext = previousContext
                    previousContext = word
                }
            } else if (insertedWords.isNotEmpty()) {
                previousPreviousContext = previousContext
                previousContext = insertedWords.last()
            }
            connection.commitText("$replacement ", 1)
            lastContextWord = previousContext
            lastContextPreviousWord = previousPreviousContext
            keyboardView?.setSuggestions(
                if (suggestionsEnabled) nextWordsForActiveLanguage(lastContextWord, lastContextPreviousWord) else emptyList()
            )
            return
        }
        val contextWords = before.split(Regex("[^\\p{L}'’]+")).filter { it.isNotBlank() }
        // The final token is the word being replaced, even when it repeats
        // an earlier word (for example: "çok çok").
        var previousContext = contextWords.dropLast(1).lastOrNull()
            ?: lastContextWord
        var previousPreviousContext = contextWords.dropLast(2).lastOrNull()
            ?: lastContextPreviousWord
        val insertedWords = replacement.split(Regex("[^\\p{L}'’]+")).filter { it.isNotBlank() }
        if (insertedWords.isNotEmpty()) {
            insertedWords.forEach { word ->
                if (learningEnabled && usesTurkishModel() &&
                    !(insertedWords.size == 1 && word.equals(currentWord, ignoreCase = true))
                ) {
                    languageModel.learn(word, previousContext, previousPreviousContext)
                }
                previousPreviousContext = previousContext
                previousContext = word
            }
            lastContextWord = previousContext
            lastContextPreviousWord = previousPreviousContext
        }
        val count = currentWord.codePointCount(0, currentWord.length)
        if (!connection.deleteSurroundingTextInCodePoints(count, 0)) {
            connection.deleteSurroundingText(currentWord.length, 0)
        }
        connection.commitText(replacement, 1)
        keyboardView?.setSuggestions(emptyList())
        requestSuggestionsSoon()
    }

    private fun matchSuggestionCase(candidate: String, typed: String): String {
        val letters = typed.filter { it.isLetter() }
        if (letters.isEmpty()) return candidate
        val locale = if (usesTurkishModel()) Locale("tr", "TR") else Locale.ENGLISH
        return when {
            letters.all { it.isUpperCase() } -> candidate.uppercase(locale)
            letters.first().isUpperCase() -> candidate.replaceFirstChar { it.uppercase(locale) }
            else -> candidate.lowercase(locale)
        }
    }

    override fun onGetSuggestions(results: Array<SuggestionsInfo>) {
        val queriedWord = lastQueriedWord
        if (queriedWord.isBlank() || !suggestionsEnabled) return
        val activeSequence = suggestionRequestSequence
        val matchingResults = results.filter { it.cookie == 0 && it.sequence == activeSequence }
        if (matchingResults.isEmpty()) return
        val corrections = matchingResults
            .flatMap { info ->
                (0 until info.suggestionsCount).mapNotNull { index ->
                    info.getSuggestionAt(index)
                }
            }
            .filter { it.isNotBlank() && !it.equals(queriedWord, ignoreCase = true) }
            .map { matchSuggestionCase(it, queriedWord) }
            .distinctBy { it.lowercase(activeEditorLocale()) }
            .take(2)
        val localCompletions = if (usesTurkishModel()) languageModel.completions(queriedWord) else languageModel.englishCompletions(queriedWord)
        val candidates = (listOf(queriedWord) + corrections + localCompletions)
            .distinctBy { it.lowercase(activeEditorLocale()) }
            .take(3)
        mainHandler.post {
            // A newer request can start after this callback arrives but before
            // this UI update runs. Check both the word and request token again.
            if (suggestionRequestSequence == activeSequence &&
                lastQueriedWord == queriedWord
            ) {
                keyboardView?.setSuggestions(candidates)
            }
        }
    }

    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>) = Unit
}
