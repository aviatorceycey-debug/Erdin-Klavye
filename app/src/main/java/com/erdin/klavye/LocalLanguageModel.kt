package com.erdin.klavye

import android.content.Context
import org.json.JSONObject
import java.util.Locale

/**
 * Small, private, on-device language model.
 * It learns words and adjacent-word pairs from text the user types.
 * No text leaves the device.
 */
class LocalLanguageModel(context: Context) {
    private val prefs = context.getSharedPreferences("erdin_language_model", Context.MODE_PRIVATE)
    private val locale = Locale("tr", "TR")

    private val starterWords = listOf(
        "acaba","açık","açıklama","adam","adres","aile","akşam","alabilir","alışveriş",
        "ama","ancak"," Ankara","anlat","ara","arkadaş","artık","aslında","at"," bugün",
        "bana","başarı","başka","bazen","bekle","belki","ben","bence","beni","beraber",
        "bilgi","bir","biraz","biz","böyle","bugün","büyük","çalışma","çalışmak","çok",
        "da","daha","dakika","de","değil","demek","deneme","diğer","doğru","dün",
        "edebilir","ederim","ediyorum","elbette","en","ev","evet","fakat","farklı",
        "fiyat","gerek","gibi","gidecek","gidelim","güzel","hafta","hangi","hakkında",
        "hayat","hayır","her","herhangi","hemen","hep","hepsi","hiç","için","iyi",
        "iş","işlem","kadar","kalmış","kampanya","karşı","kendi","kesin","ki","kitap",
        "kolay","konu","konuşma","lazım","lütfen","mağaza","merhaba","mesaj","mı","mi",
        "mükemmel","nasıl","neden","nerede","olabilir","olacak","olarak","oldu","oldukça",
        "olmak","onay","önce","orada","oyun","para","pazar","peki","program","şimdi",
        "sadece","sana","sonra","sorun","söz","tamam","tabii","tarafından","tekrar",
        "telefon","teşekkür","tüm","uygulama","üzerinde","var","ve","veya","yani","yap",
        "yapabilir","yapılacak","yapmak","yardım","yeni","yer","yok","zaman"
    ).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    private fun readCounts(key: String): MutableMap<String, Int> {
        val json = runCatching { JSONObject(prefs.getString(key, "{}") ?: "{}") }.getOrNull()
            ?: JSONObject()
        val result = mutableMapOf<String, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val word = keys.next()
            result[word] = json.optInt(word, 0)
        }
        return result
    }

    private fun saveCounts(key: String, values: Map<String, Int>) {
        val json = JSONObject()
        values.forEach { (word, count) -> json.put(word, count) }
        prefs.edit().putString(key, json.toString()).apply()
    }

    @Synchronized
    fun learn(word: String, previousWord: String?) {
        val clean = normalize(word)
        if (clean.length < 2 || clean.any { !it.isLetter() }) return

        val words = readCounts("word_counts")
        words[clean] = (words[clean] ?: 0) + 1
        if (words.size > 2500) {
            val leastUsed = words.entries.sortedBy { it.value }.take(words.size - 2000)
            leastUsed.forEach { words.remove(it.key) }
        }
        saveCounts("word_counts", words)

        val previous = previousWord?.let(::normalize).orEmpty()
        if (previous.isNotBlank() && previous != clean) {
            val pairs = readCounts("pair_counts")
            val key = "$previous>$clean"
            pairs[key] = (pairs[key] ?: 0) + 1
            if (pairs.size > 5000) {
                val leastUsed = pairs.entries.sortedBy { it.value }.take(pairs.size - 4000)
                leastUsed.forEach { pairs.remove(it.key) }
            }
            saveCounts("pair_counts", pairs)
        }
    }

    @Synchronized
    fun completions(prefix: String): List<String> {
        val key = normalize(prefix)
        if (key.length < 2) return emptyList()
        val learned = readCounts("word_counts")
        return (starterWords.asSequence().filter { it.startsWith(key) } +
            learned.keys.asSequence().filter { it.startsWith(key) })
            .distinct()
            .filterNot { it.equals(key, ignoreCase = true) }
            .sortedWith(compareByDescending<String> { learned[it] ?: 0 }.thenBy { it.length })
            .take(2)
            .toList()
    }

    @Synchronized
    fun nextWords(previousWord: String): List<String> {
        val previous = normalize(previousWord)
        if (previous.isBlank()) return emptyList()
        val pairs = readCounts("pair_counts")
        return pairs.entries
            .asSequence()
            .filter { it.key.startsWith("$previous>") }
            .sortedByDescending { it.value }
            .map { it.key.substringAfter('>') }
            .distinct()
            .take(3)
            .toList()
    }

    private fun normalize(value: String): String = value.trim().lowercase(locale)
}
