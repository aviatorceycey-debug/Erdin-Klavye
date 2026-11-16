package com.erdin.klavye

import android.content.Context
import org.json.JSONObject
import java.util.Locale

/**
 * Small, private, on-device Turkish language model.
 * Learns words and adjacent-word pairs locally. Typed text never leaves the device.
 */
class LocalLanguageModel(context: Context) {
    private val prefs = context.getSharedPreferences("erdin_language_model", Context.MODE_PRIVATE)
    private val locale = Locale("tr", "TR")

    // Compact seed vocabulary: useful everyday Turkish words, kept on-device.
    private val starterWords = """
        acaba açık açıklama adam adres aile akşam alabilir alışveriş ama ancak anlat ara
        arkadaş artık aslında asla at bugün bana başarı başka bazen bekle belki ben bence
        beni beraber bilgi bilmek bir biraz biz böyle büyük çalışma çalışmak çok da daha
        dakika de değil demek deneme diğer doğru dün edebilir ederim ediyorum elbette en
        erken ev evet fakat farklı fikir fiyat gelecek gerek gibi gidecek gidelim güzel
        gün günaydın hafta hangi hakkında hayat hayır hemen hep hepsi hiç için iyi ilk
        insan iş işlem kadar kaldı kalmak kampanya karşı kendi kesin ki kitap kolay konu
        konuşmak lazım lütfen mağaza merhaba mesaj mı mi mümkün mükemmel nasıl neden
        nerede şimdi oldu olabilir olacak olarak olmak onlar ondan önce orada oyun para
        pazar peki program rahat gerçekten renk saat sadece sana sonra sağ selam sevgili
        sizin siz sorun söz tabii tamam tekrar teşekkür telefon tüm türkçe uygulama
        uygun üzgün var ve veya yapmak yapabilir yardım yeni yer yok zaman zaten güzel
        çünkü şimdi bugün yarın burada buraya oraya benim sen senin onun bizim sizin
        ister misin olur mu olur tamamdır harika süper devam birlikte hemen sonra önce
        önemli güzel doğru yanlış hızlı yavaş kolay zor ücretsiz ücretli mağaza ziyaret
        müşteri ürün liste kontrol rapor plan çalışma toplantı dosya ekran klavye yazı
        kelime öneri tahmin düzeltme otomatik akıllı kişisel güvenli çevrimdışı internet
        kahve yemek su ev araba telefon bilgisayar uygulama oyun müzik film haber
        Türkiye Bursa İstanbul Ankara İzmir Eskişehir
    """.trimIndent().split(Regex("\\s+")).map { normalize(it) }.distinct()

    // General-purpose context hints. Personal usage counts gradually take priority.
    private val starterNextWords = mapOf(
        "iyi" to listOf("bir", "günler", "akşamlar", "olur"),
        "bir" to listOf("şey", "daha", "sonraki", "süre", "kez"),
        "çok" to listOf("iyi", "güzel", "teşekkür", "önemli"),
        "ne" to listOf("zaman", "kadar", "oldu", "yapıyorsun"),
        "merhaba" to listOf("nasılsın", "arkadaşlar", "herkese"),
        "günaydın" to listOf("herkese", "nasılsın"),
        "teşekkür" to listOf("ederim", "ler", "ederiz"),
        "rica" to listOf("ederim", "ederiz"),
        "nasıl" to listOf("oldu", "yapılır", "gidiyor", "istersen"),
        "bugün" to listOf("çok", "hava", "nasıl", "için"),
        "şimdi" to listOf("ne", "bir", "hemen", "başlayalım"),
        "daha" to listOf("iyi", "güzel", "fazla", "sonra"),
        "bence" to listOf("de", "çok", "bu", "şöyle"),
        "tamam" to listOf("dır", "olur", "devam", "teşekkür"),
        "evet" to listOf("ama", "tabii", "kesinlikle"),
        "hayır" to listOf("teşekkürler", "öyle", "henüz"),
        "ben" to listOf("de", "değil", "sana", "bunu", "istiyorum"),
        "sana" to listOf("bir", "yardım", "haber", "göndereceğim"),
        "bunu" to listOf("da", "nasıl", "hemen", "yapabiliriz"),
        "bu" to listOf("çok", "bir", "konuda", "şekilde"),
        "sonra" to listOf("görüşürüz", "konuşuruz", "bakarız"),
        "lütfen" to listOf("yardım", "bana", "bunu", "devam"),
        "yardım" to listOf("eder", "edin", "istiyorum"),
        "devam" to listOf("edelim", "et", "edebiliriz"),
        "güzel" to listOf("oldu", "bir", "gün", "görünüyor")
    )

    private fun readCounts(key: String): MutableMap<String, Int> {
        val json = runCatching { JSONObject(prefs.getString(key, "{}") ?: "{}") }.getOrNull()
            ?: JSONObject()
        val result = mutableMapOf<String, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val word = keys.next()
            result[word] = json.optInt(word, 0).coerceAtLeast(0)
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
            words.entries.sortedBy { it.value }.take(words.size - 2000)
                .forEach { words.remove(it.key) }
        }
        saveCounts("word_counts", words)

        val previous = previousWord?.let(::normalize).orEmpty()
        if (previous.isNotBlank() && previous != clean) {
            val pairs = readCounts("pair_counts")
            val key = "$previous>$clean"
            pairs[key] = (pairs[key] ?: 0) + 1
            if (pairs.size > 5000) {
                pairs.entries.sortedBy { it.value }.take(pairs.size - 4000)
                    .forEach { pairs.remove(it.key) }
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

        val learned = readCounts("pair_counts")
            .filterKeys { it.startsWith("$previous>") }
            .mapKeys { it.key.substringAfter('>') }
        val seeded = starterNextWords[previous].orEmpty()
        return (seeded.asSequence() + learned.keys.asSequence())
            .distinct()
            .sortedWith(
                compareByDescending<String> { learned[it] ?: 0 }
                    .thenBy { seeded.indexOf(it).let { index -> if (index < 0) Int.MAX_VALUE else index } }
                    .thenBy { it.length }
            )
            .take(3)
            .toList()
    }

    private fun normalize(value: String): String = value.trim().lowercase(locale)
}
