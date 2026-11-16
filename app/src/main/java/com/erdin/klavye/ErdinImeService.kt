package com.erdin.klavye

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import java.util.Locale

class ErdinImeService : InputMethodService(), SpellCheckerSession.SpellCheckerSessionListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var spellCheckerSession: SpellCheckerSession? = null
    private var keyboardView: ErdinKeyboardView? = null
    private var lastQueriedWord = ""
    private lateinit var languageModel: LocalLanguageModel

    override fun onCreate() {
        super.onCreate()
        languageModel = LocalLanguageModel(this)
    }

    override fun onCreateInputView(): View {
        return ErdinKeyboardView(
            this,
            onKey = { value -> handleKey(value) },
            onSuggestion = { value -> applySuggestion(value) }
        ).also { keyboardView = it }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        lastQueriedWord = ""
        keyboardView?.setSuggestions(emptyList())
        if (spellCheckerSession == null) {
            val manager = getSystemService(TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager
            spellCheckerSession = manager?.newSpellCheckerSession(
                null, Locale("tr", "TR"), this, true
            )
        }
    }

    override fun onFinishInput() {
        mainHandler.removeCallbacksAndMessages(null)
        spellCheckerSession?.close()
        spellCheckerSession = null
        keyboardView?.setSuggestions(emptyList())
        super.onFinishInput()
    }

    private fun handleKey(value: String) {
        val connection = currentInputConnection ?: return
        when (value) {
            "⌫" -> {
                if (!connection.deleteSurroundingTextInCodePoints(1, 0)) {
                    connection.deleteSurroundingText(1, 0)
                }
                requestSuggestionsSoon()
            }
            "↵" -> {
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
            }
            "🌐" -> switchToNextInputMethod(false)
            else -> {
                if (value == " ") {
                    val before = connection.getTextBeforeCursor(120, 0)?.toString().orEmpty()
                    val words = before.split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }
                    val current = words.lastOrNull().orEmpty()
                    val previous = words.getOrNull(words.lastIndex - 1)
                    if (current.isNotBlank()) languageModel.learn(current, previous)
                    connection.commitText(value, 1)
                    keyboardView?.setSuggestions(languageModel.nextWords(current))
                } else {
                    connection.commitText(value, 1)
                    if (value.all { it.isLetter() }) requestSuggestionsSoon()
                    else keyboardView?.setSuggestions(emptyList())
                }
            }
        }
    }

    private fun requestSuggestionsSoon() {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
            val word = before.takeLastWhile { it.isLetter() }
            if (word.length < 2) {
                lastQueriedWord = ""
                keyboardView?.setSuggestions(emptyList())
                return@postDelayed
            }
            lastQueriedWord = word
            val local = languageModel.completions(word)
            keyboardView?.setSuggestions((listOf(word) + local).take(3))
            spellCheckerSession?.getSuggestions(arrayOf(TextInfo(word)), 3, false)
        }, 180)
    }

    private fun applySuggestion(replacement: String) {
        val connection = currentInputConnection ?: return
        val before = connection.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val currentWord = before.takeLastWhile { it.isLetter() }
        if (currentWord.isEmpty()) {
            connection.commitText("$replacement ", 1)
            keyboardView?.setSuggestions(languageModel.nextWords(replacement))
            return
        }
        val count = currentWord.codePointCount(0, currentWord.length)
        if (!connection.deleteSurroundingTextInCodePoints(count, 0)) {
            connection.deleteSurroundingText(currentWord.length, 0)
        }
        connection.commitText(replacement, 1)
        keyboardView?.setSuggestions(emptyList())
        requestSuggestionsSoon()
    }

    override fun onGetSuggestions(results: Array<SuggestionsInfo>) {
        val queriedWord = lastQueriedWord
        if (queriedWord.isBlank()) return
        val corrections = results
            .flatMap { info ->
                (0 until info.suggestionsCount).mapNotNull { index ->
                    info.getSuggestionAt(index)
                }
            }
            .filter { it.isNotBlank() && !it.equals(queriedWord, ignoreCase = true) }
            .distinct()
            .take(2)
        val candidates = (listOf(queriedWord) + corrections + languageModel.completions(queriedWord))
            .distinct()
            .take(3)
        mainHandler.post {
            if (lastQueriedWord == queriedWord) keyboardView?.setSuggestions(candidates)
        }
    }

    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>) = Unit
}
