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

    // Three-word phrase hints help before personal data has been learned.
    private val starterTrigrams = mapOf(
        "iyi>bir" to listOf("gün", "fikir", "insan", "şekilde"),
        "ben>de" to listOf("varım", "istiyorum", "düşünüyorum", "geliyorum"),
        "ne>kadar" to listOf("güzel", "sürer", "oldu", "önemli"),
        "çok>iyi" to listOf("oldu", "görünüyor", "geldi", "hissettiriyor"),
        "bir>daha" to listOf("bakalım", "deneyelim", "sonra", "fazla"),
        "bugün>çok" to listOf("güzel", "yoğun", "mutluyum", "işim"),
        "yardım>eder" to listOf("misin", "mısın", "mi", "lütfen"),
        "teşekkür>ederim" to listOf("sana", "çok", "yardımın", "ilgin"),
        "iyi>akşamlar" to listOf("dilerim", "herkese", "nasılsınız"),
        "iyi>günler" to listOf("dilerim", "herkese", "nasılsınız"),
        "görüşmek>üzere" to listOf("kendine", "hoşça", "yakında"),
        "hoş>geldin" to listOf("nasılsın", "tekrar", "arkadaşım"),
        "hoş>bulduk" to listOf("teşekkürler", "nasılsınız"),
        "kolay>gelsin" to listOf("teşekkürler", "sana", "size"),
        "eline>sağlık" to listOf("çok", "teşekkür", "harika"),
        "iyi>ki" to listOf("varsın", "geldin", "yaptım"),
        "bir>şey" to listOf("daha", "söyleyeceğim", "soracağım"),
        "şu>anda" to listOf("müsaitim", "uygun", "çalışıyorum"),
        "haber>ver" to listOf("lütfen", "bana", "inceleyelim"),
        "ne>zaman" to listOf("geleceksin", "başlayacak", "uygun"),
        "seni>çok" to listOf("seviyorum", "özledim", "bekliyorum"),
        "her>şey" to listOf("çok", "güzel", "yolunda"),
        "iyi>çalışmalar" to listOf("dilerim", "kolay", "sana"),
        "iyi>geceler" to listOf("dilerim", "herkese", "sana"),
        "afiyet>olsun" to listOf("teşekkürler", "hepimize", "size"),
        "geçmiş>olsun" to listOf("çok", "teşekkürler", "dilerim"),
        "tebrik>ederim" to listOf("seni", "başarını", "çok"),
        "mutlu>yıllar" to listOf("dilerim", "sana", "hepimize"),
        "doğum>günün" to listOf("kutlu", "kutlu olsun", "ne zaman"),
        "nasıl>gidiyor" to listOf("iyi", "her şey", "işler"),
        "ne>yapıyorsun" to listOf("şimdi", "bugün", "orada"),
        "çok>teşekkür" to listOf("ederim", "ederiz", "sana"),
        "yardımcı>olur" to listOf("musun", "muyum", "muyuz"),
        "müsait>misin" to listOf("şimdi", "bugün", "yarın"),
        "başarılar>dilerim" to listOf("sana", "hepimize", "yolunda"),
        "iyi>ki" to listOf("varsın", "geldin", "yaptın"),
        "sana>da" to listOf("iyi", "haber", "kolay gelsin"),
        "umarım>her" to listOf("şey", "zaman", "şey yolundadır"),
        "benim>için" to listOf("çok", "önemli", "uygun"),
        "şu>an" to listOf("için", "müsaitim", "uygun"),
        "haber>bekliyorum" to listOf("senden", "sizden", "lütfen"),
        "tekrar>görüşmek" to listOf("üzere", "dileğiyle"),
        "çok>memnun" to listOf("oldum", "olduk", "olacağım"),
        "emeğine>sağlık" to listOf("çok", "teşekkürler", "harika"),
        "iyi>ki" to listOf("varsın", "doğdun", "geldin")
    )

    // General-purpose context hints. Personal usage counts gradually take priority.
    private val starterNextWords = mapOf(
        "iyi" to listOf("bir", "günler", "akşamlar", "olur"),
        "bir" to listOf("şey", "daha", "sonraki", "süre", "kez"),
        "çok" to listOf("iyi", "güzel", "teşekkür", "önemli"),
        "ne" to listOf("zaman", "kadar", "oldu", "yapıyorsun"),
        "merhaba" to listOf("nasılsın", "arkadaşlar", "herkese"),
        "günaydın" to listOf("herkese", "nasılsın"),
        "teşekkür" to listOf("ederim", "ederiz", "çok"),
        "rica" to listOf("ederim", "ederiz"),
        "nasıl" to listOf("oldu", "yapılır", "gidiyor", "istersen"),
        "bugün" to listOf("çok", "hava", "nasıl", "için"),
        "şimdi" to listOf("ne", "bir", "hemen", "başlayalım"),
        "daha" to listOf("iyi", "güzel", "fazla", "sonra"),
        "bence" to listOf("de", "çok", "bu", "şöyle"),
        "tamam" to listOf("olur", "devam", "teşekkür", "anladım"),
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
        "güzel" to listOf("oldu", "bir", "gün", "görünüyor"),
        "afiyet" to listOf("olsun"),
        "geçmiş" to listOf("olsun"),
        "tebrik" to listOf("ederim", "ederiz"),
        "mutlu" to listOf("yıllar", "olsun"),
        "doğum" to listOf("günün", "günü"),
        "müsait" to listOf("misin", "misiniz", "olunca"),
        "başarılar" to listOf("dilerim", "sana", "hepimize")
    )

    // A separate, fixed English vocabulary supports offline completions without
    // mixing English text into the user's private Turkish learning model.
    private val englishStarterWords = """
        about above after again all also always am an and another any are around as ask
        at away back be because before begin best better between big both bring but by
        call can change come could day different do does doing down each early easy end
        even every example feel few find first for from get give go good great had has
        have he help her here high him his home how I if in into is it its just keep kind
        know large last later learn leave left let life like little live long look love
        made make many may me mean might more most much must my name near need never new
        next no not now number of off often old on once one only open or other our out
        over own part people place point put read real right same say see seem she should
        show small so some something start still such take than that the their them then
        there these they thing think this those through time to together too try turn two
        under up us use very want was way we well were what when where which while who
        why will with without word work world would write year yes you your thanks hello
        today tomorrow please okay sure maybe great morning evening friend message
        able accept account across act action actually add address almost alone already
        amazing answer anything app apple area ask asked available away awesome baby
        bad beautiful believe body book busy buy camera care case catch cause celebrate
        center certain check city class clean close coffee cold color company complete
        computer continue cool create customer data date decision detail dinner drive
        during earth education email enjoy enough especially event everything excited
        face family far fast favorite feel feeling file finally fine finish follow food
        free fresh full fun future game general girl glad glass goal going gone happy
        hard hear heart history hope hour house idea important information inside interest
        job join key keyboard language late learn letter level library listen local lost
        main market matter meeting mind minute mobile money month move music must name
        natural necessary night note nothing office often okay old online option order
        page paper parent password phone photo picture plan play power practice pretty
        problem product project question quick quiet reason remember result review room
        run safe save school screen search second security send service share short simple
        slow smart software solution sound speak special spend spring standard store story
        strong study success summer support system table talk team text theme together
        touch travel true understand update useful user value video visit voice wait walk
        watch water website week welcome whole win winter wish woman wonderful words
        write wrong yesterday young
    """.trimIndent().split(Regex("\\s+")).map { it.lowercase(Locale.US) }.distinct()

    // Two-word English context improves phrase completion beyond single-word hints.
    private val englishContextHints = mapOf(
        "thank>you" to listOf("for", "so much"),
        "looking>forward" to listOf("to"),
        "see>you" to listOf("soon", "tomorrow", "there"),
        "how>are" to listOf("you"),
        "nice>to" to listOf("meet", "see", "hear"),
        "i>am" to listOf("sorry", "happy", "ready", "sure"),
        "i>think" to listOf("so", "that", "we"),
        "have>a" to listOf("good", "great", "nice"),
        "good>morning" to listOf("everyone", "to you"),
        "in>the" to listOf("morning", "future", "end", "same"),
        "hope>you" to listOf("are", "have", "had"),
        "let>me" to listOf("know", "help", "check"),
        "could>you" to listOf("please", "help", "send"),
        "would>you" to listOf("like", "please", "mind"),
        "looking>for" to listOf("a", "something", "the"),
        "happy>to" to listOf("help", "hear", "see"),
        "thanks>for" to listOf("your", "the", "helping"),
        "take>care" to listOf("of", "and"),
        "on>the" to listOf("way", "phone", "other"),
        "all>the" to listOf("best", "time", "same"),
        "talk>to" to listOf("you", "them", "her"),
        "as>soon" to listOf("as"),
        "out>of" to listOf("the", "time", "town"),
        "one>of" to listOf("the", "my", "those"),
        "good>to" to listOf("see", "know", "hear"),
        "i>hope" to listOf("you", "that", "everything"),
        "i>will" to listOf("be", "send", "let"),
        "please>let" to listOf("me", "us"),
        "let>us" to listOf("know", "go", "start"),
        "looking>good" to listOf("today", "so far"),
        "at>the" to listOf("moment", "same time", "end"),
        "for>your" to listOf("help", "time", "support"),
        "have>been" to listOf("working", "trying", "thinking"),
        "as>well" to listOf("as", "today"),
        "much>appreciated" to listOf("thank you", "indeed")
    )

    private val englishNextHints = mapOf(
        "good" to listOf("morning", "to", "idea", "work"),
        "thank" to listOf("you", "you for"),
        "how" to listOf("are", "is", "can", "about"),
        "i" to listOf("am", "think", "want", "can", "will"),
        "see" to listOf("you", "you soon", "that", "what"),
        "have" to listOf("a", "been", "to", "time"),
        "let" to listOf("me", "us", "it"),
        "looking" to listOf("forward", "for"),
        "nice" to listOf("to", "work"),
        "goodbye" to listOf("and", "for now"),
        "hello" to listOf("there", "everyone", "again"),
        "please" to listOf("let", "help", "send"),
        "can" to listOf("you", "i", "we", "do"),
        "will" to listOf("be", "do", "have", "see"),
        "are" to listOf("you", "we", "they", "there"),
        "this" to listOf("is", "will", "looks", "one"),
        "that" to listOf("is", "sounds", "was", "works"),
        "we" to listOf("can", "are", "will", "should"),
        "much" to listOf("better", "more", "appreciated"),
        "not" to listOf("sure", "now", "yet", "working"),
        "what" to listOf("do", "is", "about", "happened"),
        "when" to listOf("you", "we", "will", "is"),
        "very" to listOf("good", "well", "happy", "much")
    )

    private val englishFrequencyRank = listOf(
        "the", "you", "that", "this", "there", "they", "their", "then", "them",
        "what", "when", "where", "which", "who", "will", "with", "would", "your",
        "have", "has", "had", "how", "here", "help", "hello", "good", "great",
        "get", "got", "going", "go", "can", "could", "come", " see", "some",
        "for", "from", "first", "find", "feel", "just", "know", "like", "look",
        "make", "more", "most", "much", "need", "new", "now", "not", "one",
        "our", "out", "please", "people", "really", "right", "said", "same",
        "see", "send", "should", "some", "still", "take", "than", "thank",
        "think", "time", "today", "tomorrow", "too", "use", "very", "want",
        "way", "well", "were", "work", "world", "yes"
    ).mapIndexed { index, word -> word.trim() to index }.toMap()

    @Synchronized
    fun englishCompletions(prefix: String): List<String> {
        val typed = prefix.trim()
        val key = typed.lowercase(Locale.US)
        if (key.length < 2) return emptyList()
        val candidates = englishStarterWords.asSequence()
            .filter { it.startsWith(key) && it != key }
            .distinct()
            .sortedWith(compareBy<String> { englishFrequencyRank[it] ?: Int.MAX_VALUE }.thenBy { it })
            .take(2)
            .toList()
        return candidates.map { candidate ->
            when {
                typed.all { !it.isLetter() || it.isUpperCase() } -> candidate.uppercase(Locale.US)
                typed.firstOrNull()?.isUpperCase() == true ->
                    candidate.replaceFirstChar { it.uppercase(Locale.US) }
                else -> candidate
            }
        }
    }

    @Synchronized
    fun englishNextWords(previousWord: String, wordBeforePrevious: String? = null): List<String> {
        val previous = previousWord.trim().lowercase(Locale.US)
        if (previous.isBlank()) return emptyList()
        val context = wordBeforePrevious?.trim()?.lowercase(Locale.US).orEmpty()
        val contextual = if (context.isBlank()) emptyList()
            else englishContextHints["$context>$previous"].orEmpty()
        return (contextual + englishNextHints[previous].orEmpty())
            .distinct()
            .take(3)
    }

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

    // Keep the small personal model in memory while the keyboard is open.
    // This avoids parsing JSON on every key press.
    private val wordCounts by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        readCounts("word_counts")
    }
    private val pairCounts by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        readCounts("pair_counts")
    }
    private val trigramCounts by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        readCounts("trigram_counts")
    }

    private fun saveCounts(key: String, values: Map<String, Int>) {
        val json = JSONObject()
        values.forEach { (word, count) -> json.put(word, count) }
        prefs.edit().putString(key, json.toString()).apply()
    }

    @Synchronized
    fun clearPersonalData() {
        wordCounts.clear()
        pairCounts.clear()
        trigramCounts.clear()
        prefs.edit()
            .remove("word_counts")
            .remove("pair_counts")
            .remove("trigram_counts")
            .apply()
    }

    @Synchronized
    fun learnedWords(): List<Pair<String, Int>> =
        wordCounts.entries
            .filter { it.value > 0 }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }

    @Synchronized
    fun deletePersonalWord(word: String) {
        val clean = normalize(word)
        if (clean.isBlank()) return
        wordCounts.remove(clean)
        // Remove the word wherever it appears in a learned context, not only
        // when it is the predicted final token.
        pairCounts.keys.removeAll { key -> key.split('>').any { it == clean } }
        trigramCounts.keys.removeAll { key -> key.split('>').any { it == clean } }
        saveCounts("word_counts", wordCounts)
        saveCounts("pair_counts", pairCounts)
        saveCounts("trigram_counts", trigramCounts)
    }

    @Synchronized
    fun learn(word: String, previousWord: String?, previousPreviousWord: String? = null) {
        val clean = normalize(word)
        if (clean.length < 2 || clean.any { !it.isLetter() }) return

        val words = wordCounts
        words[clean] = (words[clean] ?: 0) + 1
        if (words.size > 2500) {
            words.entries.sortedBy { it.value }.take(words.size - 2000)
                .forEach { words.remove(it.key) }
        }
        saveCounts("word_counts", words)

        val previous = previousWord?.let(::normalize).orEmpty()
        if (previous.isNotBlank()) {
            val pairs = pairCounts
            val key = "$previous>$clean"
            pairs[key] = (pairs[key] ?: 0) + 1
            if (pairs.size > 5000) {
                pairs.entries.sortedBy { it.value }.take(pairs.size - 4000)
                    .forEach { pairs.remove(it.key) }
            }
            saveCounts("pair_counts", pairs)
        }

        val previousPrevious = previousPreviousWord?.let(::normalize).orEmpty()
        if (previousPrevious.isNotBlank() && previous.isNotBlank()) {
            val trigrams = trigramCounts
            val key = "$previousPrevious>$previous>$clean"
            trigrams[key] = (trigrams[key] ?: 0) + 1
            if (trigrams.size > 6000) {
                trigrams.entries.sortedBy { it.value }.take(trigrams.size - 4500)
                    .forEach { trigrams.remove(it.key) }
            }
            saveCounts("trigram_counts", trigrams)
        }
    }

    @Synchronized
    fun completions(prefix: String): List<String> {
        val key = normalize(prefix)
        if (key.length < 2) return emptyList()
        val learned = wordCounts
        val candidates = (starterWords.asSequence().filter { it.startsWith(key) } +
            learned.keys.asSequence().filter { it.startsWith(key) })
            .distinct()
            .filterNot { it.equals(key, ignoreCase = true) }
            .sortedWith(compareByDescending<String> { learned[it] ?: 0 }.thenBy { it.length })
            .take(2)
            .toList()

        // Match the casing the user has already started, including Turkish İ/ı.
        val typed = prefix.trim()
        return candidates.map { candidate ->
            when {
                typed.any { it.isLetter() } && typed.filter { it.isLetter() }.all { it.isUpperCase() } ->
                    candidate.uppercase(locale)
                typed.firstOrNull()?.isUpperCase() == true ->
                    candidate.replaceFirstChar { it.uppercase(locale) }
                else -> candidate
            }
        }
    }

    /**
     * Conservative local swipe decoder. Only replaces the traced sequence when
     * a known word is a very close spelling match; otherwise keeps the raw path.
     */
    @Synchronized
    fun decodeSwipePath(path: String): String {
        val clean = normalize(path).filter { it.isLetter() }
        if (clean.length < 3) return clean
        val candidates = (starterWords.asSequence() + wordCounts.keys.asSequence())
            .filter { kotlin.math.abs(it.length - clean.length) <= 1 }
            .distinct()
            .toList()
        if (candidates.isEmpty()) return clean

        fun distance(a: String, b: String): Int {
            // Only distance 0 or 1 can be accepted, so stop as soon as
            // a candidate is provably outside that range.
            if (kotlin.math.abs(a.length - b.length) > 1) return 2
            var twoRowsBack = IntArray(b.length + 1) { Int.MAX_VALUE / 4 }
            var previous = IntArray(b.length + 1) { it }
            for (i in 1..a.length) {
                val current = IntArray(b.length + 1) { 2 }
                current[0] = i
                var rowMinimum = current[0]
                val from = maxOf(1, i - 1)
                val to = minOf(b.length, i + 1)
                for (j in from..to) {
                    val substitution = if (a[i - 1] == b[j - 1]) 0 else 1
                    var score = minOf(
                        previous[j] + 1,
                        current[j - 1] + 1,
                        previous[j - 1] + substitution
                    )
                    if (i > 1 && j > 1 &&
                        a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]
                    ) {
                        score = minOf(score, twoRowsBack[j - 2] + 1)
                    }
                    current[j] = score
                    rowMinimum = minOf(rowMinimum, score)
                }
                if (rowMinimum > 1) return 2
                twoRowsBack = previous
                previous = current
            }
            return previous[b.length].coerceAtMost(2)
        }

        val ranked = candidates.map { it to distance(clean, it) }
            .sortedWith(compareBy<Pair<String, Int>> { it.second }
                .thenByDescending { wordCounts[it.first] ?: 0 })
        val best = ranked.firstOrNull() ?: return clean
        val maxDistance = 1
        val runnerUp = ranked.getOrNull(1)
        val bestPersonalCount = wordCounts[best.first] ?: 0
        val runnerUpPersonalCount = runnerUp?.let { wordCounts[it.first] ?: 0 } ?: 0
        val clearlyLearned = bestPersonalCount >= 3 &&
            bestPersonalCount >= runnerUpPersonalCount * 2
        val unambiguous = runnerUp == null || runnerUp.second > best.second
        return if (best.second <= maxDistance && (unambiguous || clearlyLearned)) {
            best.first
        } else {
            clean
        }
    }

    @Synchronized
    fun nextWords(previousWord: String, previousPreviousWord: String? = null): List<String> {
        val previous = normalize(previousWord)
        if (previous.isBlank()) return emptyList()

        val learned = pairCounts.asSequence()
            .filter { (key, _) -> key.startsWith("$previous>") }
            .associate { (key, count) -> key.substringAfter('>') to count }
        val previousPrevious = previousPreviousWord?.let(::normalize).orEmpty()
        val trigramPrefix = if (previousPrevious.isBlank()) "" else "$previousPrevious>$previous>"
        val contextual = if (trigramPrefix.isBlank()) emptyMap() else {
            trigramCounts.asSequence()
                .filter { (key, _) -> key.startsWith(trigramPrefix) }
                .associate { (key, count) -> key.substringAfterLast('>') to count }
        }
        val seeded = starterNextWords[previous].orEmpty()
        val seededContext = starterTrigrams["$previousPrevious>$previous"].orEmpty()
        val candidates = (seededContext.asSequence() + seeded.asSequence() +
            learned.keys.asSequence() + contextual.keys.asSequence()).distinct().toList()

        // Personal usage should influence ranking quickly, while built-in hints
        // remain useful until the user's own patterns become established.
        fun score(word: String): Int {
            val trigramScore = (contextual[word] ?: 0).coerceAtMost(100) * 12
            val contextIndex = seededContext.indexOf(word)
            val seededTrigramScore = if (contextIndex < 0) 0 else (seededContext.size - contextIndex) * 3
            val bigramScore = (learned[word] ?: 0).coerceAtMost(100) * 5
            val personalWordScore = (wordCounts[word] ?: 0).coerceAtMost(100)
            val seedIndex = seeded.indexOf(word)
            val seedScore = if (seedIndex < 0) 0 else (seeded.size - seedIndex) * 2
            return trigramScore + seededTrigramScore + bigramScore + personalWordScore + seedScore
        }

        return candidates.sortedWith(
            compareByDescending<String> { score(it) }
                .thenByDescending { contextual[it] ?: 0 }
                .thenByDescending { learned[it] ?: 0 }
                .thenBy { it.length }
        ).take(3)
    }

    private fun normalize(value: String): String = value.trim().lowercase(locale)
}
