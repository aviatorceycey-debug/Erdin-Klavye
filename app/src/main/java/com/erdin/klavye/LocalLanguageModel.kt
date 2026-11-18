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
        "merak>etme" to listOf("hallederiz", "sorun değil", "lütfen"),
        "senin>için" to listOf("hazırladım", "buradayım", "teşekkürler"),
        "şimdiden>teşekkürler" to listOf("yardımın", "desteğin", "ilgin"),
        "bir>sonraki" to listOf("adım", "gün", "sefer"),
        "uygun>olduğunda" to listOf("haber", "yaz", "ara"),
        "sorun>değil" to listOf("hiç", "merak", "tabii"),
        "ne>güzel" to listOf("haber", "tesadüf", "oldu"),
        "iyi>oldu" to listOf("böyle", "ki", "gerçekten"),
        "ben>buradayım" to listOf("senin", "yardımcı", "her zaman"),
        "ne>haber" to listOf("nasılsın", "var", "senden"),
        "ben>iyiyim" to listOf("teşekkürler", "sen", "çok şükür"),
        "sizi>arayacağım" to listOf("bugün", "yarın", "daha sonra"),
        "gün>içinde" to listOf("haber", "tekrar", "müsait"),
        "bilgi>verir" to listOf("misin", "misiniz", "lütfen"),
        "tekrar>teşekkürler" to listOf("yardımın", "desteğin", "ilgin"),
        "umarım>görüşürüz" to listOf("yakında", "tekrar", "en kısa zamanda")
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
        "başarılar" to listOf("dilerim", "sana", "hepimize"),
        "merak" to listOf("etme", "ettim", "ediyorum"),
        "senin" to listOf("için", "de", "le"),
        "şimdiden" to listOf("teşekkürler", "sağ ol", "haber vereyim"),
        "uygun" to listOf("olduğunda", "musun", "değil"),
        "sorun" to listOf("değil", "olursa", "var mı"),
        "harika" to listOf("oldu", "görünüyor", "bir fikir")
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
        "thank>you" to listOf("for", "again", "so much", "very much"),
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
        "much>appreciated" to listOf("thank you", "indeed"),
        "if>you" to listOf("need", "want", "have"),
        "in>case" to listOf("you", "of"),
        "would>like" to listOf("to", "some", "me"),
        "as>far" to listOf("as"),
        "please>feel" to listOf("free", "welcome"),
        "feel>free" to listOf("to", "to ask"),

        "see>what" to listOf("happens", "you think", "we can do"),
        "looking>great" to listOf("today", "as always"),
        "hope>everything" to listOf("is", "goes well"),
        "get>back" to listOf("to", "to you", "soon"),
        "i>have" to listOf("a", "been", "to"),
        "i>need" to listOf("to", "help", "more"),
        "i>want" to listOf("to", "you", "a"),
        "i>can" to listOf("help", "do", "send"),
        "i>was" to listOf("wondering", "looking", "thinking"),
        "we>are" to listOf("ready", "working", "going"),
        "you>can" to listOf("use", "find", "try"),
        "there>is" to listOf("a", "no", "one"),
        "it>is" to listOf("a", "not", "very"),
        "this>is" to listOf("a", "the", "my"),
        "that>is" to listOf("great", "right", "fine"),
        "do>you" to listOf("have", "want", "know"),
        "are>you" to listOf("available", "ready", "okay"),
        "can>i" to listOf("help", "ask", "call"),
        "please>send" to listOf("me", "it", "the"),
        "send>me" to listOf("the", "a", "your"),
        "good>luck" to listOf("with", "today", "everyone")
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
        "get", "got", "going", "go", "can", "could", "come", "some",
        "for", "from", "first", "find", "feel", "just", "know", "like", "look",
        "make", "more", "most", "much", "need", "new", "now", "not", "one",
        "our", "out", "please", "people", "really", "right", "said", "same",
        "see", "send", "should", "still", "take", "than", "thank",
        "think", "time", "today", "tomorrow", "too", "use", "very", "want",
        "way", "well", "were", "work", "world", "yes"
    ).mapIndexed { index, word -> word.trim() to index }.toMap()

    @Synchronized
    fun englishCompletions(prefix: String): List<String> {
        val typed = prefix.trim()
        val key = UnicodeWordNormalizer.normalize(typed, Locale.US)
        if (key.length < 2) return emptyList()
        refreshPersonalDataIfChanged()
        val candidates = SuggestionCandidateSelector.top(
            (englishStarterWords.asSequence() + englishWordCounts.keys.asSequence())
                .filter { it.startsWith(key) && it != key }
                .distinct(),
            limit = 2,
            comparator = compareByDescending<String> { englishWordCounts[it] ?: 0 }
                .thenBy { englishFrequencyRank[it] ?: Int.MAX_VALUE }
                .thenBy { it }
        )
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
        val previous = UnicodeWordNormalizer.normalize(previousWord, Locale.US)
        if (previous.isBlank()) return emptyList()
        val context = wordBeforePrevious?.let { UnicodeWordNormalizer.normalize(it, Locale.US) }.orEmpty()
        val contextual = if (context.isBlank()) emptyList()
            else englishContextHints["$context>$previous"].orEmpty()
        val learnedPair = englishPairNextCounts[previous].orEmpty()
        val prefix = if (context.isBlank()) "" else "$context>$previous"
        val learnedTrigram = if (prefix.isBlank()) emptyMap() else englishTrigramNextCounts[prefix].orEmpty()
        return EnglishContextSuggestionSelector.select(
            contextualHints = contextual,
            seededHints = englishNextHints[previous].orEmpty(),
            learnedPairCounts = learnedPair,
            learnedTrigramCounts = learnedTrigram,
            personalWordCounts = englishWordCounts,
            limit = 3
        )
    }

    private fun readCounts(key: String, dataLocale: Locale): MutableMap<String, Int> {
        val json = runCatching { JSONObject(prefs.getString(key, "{}") ?: "{}") }.getOrNull()
            ?: JSONObject()
        val stored = mutableMapOf<String, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val word = keys.next()
            stored[word] = json.optInt(word, 0).coerceAtLeast(0)
        }
        return PersonalDataNormalizer.normalizeCounts(stored, dataLocale)
    }

    // Keep the small personal model in memory while the keyboard is open.
    // A revision marker lets separate Settings/IME instances see each other's edits
    // without reparsing the JSON on every key press.
    private var loadedRevision = prefs.getLong("personal_data_revision", 0L)
    private var wordCounts = readCounts("word_counts", locale)
    private var englishWordCounts = readCounts("english_word_counts", Locale.US)
    private var pairCounts = readCounts("pair_counts", locale)
    private var trigramCounts = readCounts("trigram_counts", locale)
    private var englishPairCounts = readCounts("english_pair_counts", Locale.US)
    private var englishTrigramCounts = readCounts("english_trigram_counts", Locale.US)
    // Prefix indexes avoid scanning every learned pair and trigram for each suggestion.
    private var pairNextCounts = indexContextCounts(pairCounts, 1)
    private var trigramNextCounts = indexContextCounts(trigramCounts, 2)
    private var englishPairNextCounts = indexContextCounts(englishPairCounts, 1)
    private var englishTrigramNextCounts = indexContextCounts(englishTrigramCounts, 2)

    // Persist canonicalized legacy keys once, so migration survives restarts.
    init {
        fun encode(values: Map<String, Int>): String {
            val json = JSONObject()
            values.toSortedMap().forEach { (word, count) -> json.put(word, count) }
            return json.toString()
        }
        val tables = listOf(
            "word_counts" to wordCounts,
            "english_word_counts" to englishWordCounts,
            "pair_counts" to pairCounts,
            "trigram_counts" to trigramCounts,
            "english_pair_counts" to englishPairCounts,
            "english_trigram_counts" to englishTrigramCounts
        )
        if (tables.any { (key, values) -> prefs.getString(key, "{}") != encode(values) }) {
            val revision = loadedRevision + 1L
            val editor = prefs.edit()
            tables.forEach { (key, values) -> editor.putString(key, encode(values)) }
            editor.putLong("personal_data_revision", revision).apply()
            loadedRevision = revision
        }
    }

    private fun indexContextCounts(
        source: Map<String, Int>,
        prefixWords: Int
    ): MutableMap<String, MutableMap<String, Int>> {
        val index = mutableMapOf<String, MutableMap<String, Int>>()
        source.forEach { (key, count) ->
            val parts = key.split('>')
            if (parts.size == prefixWords + 1 && parts.all { it.isNotBlank() }) {
                val prefix = parts.take(prefixWords).joinToString(">")
                index.getOrPut(prefix) { mutableMapOf() }[parts.last()] = count
            }
        }
        return index
    }

    private fun refreshPersonalDataIfChanged() {
        val revision = prefs.getLong("personal_data_revision", 0L)
        if (revision == loadedRevision) return
        wordCounts = readCounts("word_counts", locale)
        englishWordCounts = readCounts("english_word_counts", Locale.US)
        pairCounts = readCounts("pair_counts", locale)
        trigramCounts = readCounts("trigram_counts", locale)
        englishPairCounts = readCounts("english_pair_counts", Locale.US)
        englishTrigramCounts = readCounts("english_trigram_counts", Locale.US)
        pairNextCounts = indexContextCounts(pairCounts, 1)
        trigramNextCounts = indexContextCounts(trigramCounts, 2)
        englishPairNextCounts = indexContextCounts(englishPairCounts, 1)
        englishTrigramNextCounts = indexContextCounts(englishTrigramCounts, 2)
        loadedRevision = revision
    }

    // Persist all model tables and their revision in one preferences transaction.
    // This avoids several asynchronous disk writes for every learned word.
    private fun persistPersonalData() {
        val revision = maxOf(loadedRevision, prefs.getLong("personal_data_revision", 0L)) + 1L
        fun encode(values: Map<String, Int>): String {
            val json = JSONObject()
            values.toSortedMap().forEach { (word, count) -> json.put(word, count) }
            return json.toString()
        }
        prefs.edit()
            .putString("word_counts", encode(wordCounts))
            .putString("english_word_counts", encode(englishWordCounts))
            .putString("pair_counts", encode(pairCounts))
            .putString("trigram_counts", encode(trigramCounts))
            .putString("english_pair_counts", encode(englishPairCounts))
            .putString("english_trigram_counts", encode(englishTrigramCounts))
            .putLong("personal_data_revision", revision)
            .apply()
        loadedRevision = revision
    }

    @Synchronized
    fun clearPersonalData() {
        refreshPersonalDataIfChanged()
        wordCounts.clear()
        englishWordCounts.clear()
        pairCounts.clear()
        trigramCounts.clear()
        englishPairCounts.clear()
        englishTrigramCounts.clear()
        pairNextCounts.clear()
        trigramNextCounts.clear()
        englishPairNextCounts.clear()
        englishTrigramNextCounts.clear()
        val revision = maxOf(loadedRevision, prefs.getLong("personal_data_revision", 0L)) + 1L
        prefs.edit()
            .remove("word_counts")
            .remove("english_word_counts")
            .remove("pair_counts")
            .remove("trigram_counts")
            .remove("english_pair_counts")
            .remove("english_trigram_counts")
            .putLong("personal_data_revision", revision)
            .apply()
        loadedRevision = revision
    }

    @Synchronized
    fun learnedWords(): List<Pair<String, Int>> {
        refreshPersonalDataIfChanged()
        return (wordCounts.entries + englishWordCounts.entries)
            .filter { it.value > 0 }
            .groupBy { it.key }
            .map { (word, entries) ->
                word to entries.fold(0) { total, entry -> PersonalCount.combine(total, entry.value) }
            }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
    }

    @Synchronized
    fun deletePersonalWord(word: String) {
        refreshPersonalDataIfChanged()
        val clean = normalize(word)
        val englishClean = UnicodeWordNormalizer.normalize(word, Locale.US)
        if (clean.isBlank() && englishClean.isBlank()) return
        wordCounts.remove(clean)
        englishWordCounts.remove(englishClean)
        // Remove the word wherever it appears in a learned context, not only
        // when it is the predicted final token.
        PersonalContextPruner.removeWord(pairCounts, trigramCounts, clean)
        PersonalContextPruner.removeWord(englishPairCounts, englishTrigramCounts, englishClean)
        pairNextCounts = indexContextCounts(pairCounts, 1)
        trigramNextCounts = indexContextCounts(trigramCounts, 2)
        englishPairNextCounts = indexContextCounts(englishPairCounts, 1)
        englishTrigramNextCounts = indexContextCounts(englishTrigramCounts, 2)
        persistPersonalData()
    }

    @Synchronized
    fun learn(word: String, previousWord: String?, previousPreviousWord: String? = null) {
        refreshPersonalDataIfChanged()
        val clean = normalize(word)
        if (!PersonalWordValidator.isValid(clean)) return

        val words = wordCounts
        words[clean] = PersonalCount.increment(words[clean] ?: 0)
        if (words.size > 2500) {
            words.entries.sortedBy { it.value }.take(words.size - 2000)
                .forEach { words.remove(it.key) }
        }
        val previous = previousWord?.let(::normalize).orEmpty()
        if (previous.isNotBlank()) {
            val pairs = pairCounts
            val key = "$previous>$clean"
            pairs[key] = PersonalCount.increment(pairs[key] ?: 0)
            pairNextCounts.getOrPut(previous) { mutableMapOf() }[clean] = pairs.getValue(key)
            if (pairs.size > 5000) {
                pairs.entries.sortedBy { it.value }.take(pairs.size - 4000)
                    .forEach { pairs.remove(it.key) }
                pairNextCounts = indexContextCounts(pairs, 1)
            }
        }

        val previousPrevious = previousPreviousWord?.let(::normalize).orEmpty()
        if (previousPrevious.isNotBlank() && previous.isNotBlank()) {
            val trigrams = trigramCounts
            val prefix = "$previousPrevious>$previous"
            val key = "$prefix>$clean"
            trigrams[key] = PersonalCount.increment(trigrams[key] ?: 0)
            trigramNextCounts.getOrPut(prefix) { mutableMapOf() }[clean] = trigrams.getValue(key)
            if (trigrams.size > 6000) {
                trigrams.entries.sortedBy { it.value }.take(trigrams.size - 4500)
                    .forEach { trigrams.remove(it.key) }
                trigramNextCounts = indexContextCounts(trigrams, 2)
            }
        }
        persistPersonalData()
    }

    /** Learn an English word locally without mixing it into the Turkish model. */
    @Synchronized
    fun learnEnglish(
        word: String,
        previousWord: String? = null,
        previousPreviousWord: String? = null
    ) {
        refreshPersonalDataIfChanged()
        val clean = UnicodeWordNormalizer.normalize(word, Locale.US)
        if (!PersonalWordValidator.isValid(clean)) return
        englishWordCounts[clean] = PersonalCount.increment(englishWordCounts[clean] ?: 0)
        val previous = previousWord?.let { UnicodeWordNormalizer.normalize(it, Locale.US) }.orEmpty()
        if (PersonalWordValidator.isValid(previous)) {
            val key = "$previous>$clean"
            englishPairCounts[key] = PersonalCount.increment(englishPairCounts[key] ?: 0)
            englishPairNextCounts.getOrPut(previous) { mutableMapOf() }[clean] = englishPairCounts.getValue(key)
        }
        val previousPrevious = previousPreviousWord?.let { UnicodeWordNormalizer.normalize(it, Locale.US) }.orEmpty()
        if (PersonalWordValidator.isValid(previousPrevious) && PersonalWordValidator.isValid(previous)) {
            val prefix = "$previousPrevious>$previous"
            val key = "$prefix>$clean"
            englishTrigramCounts[key] = PersonalCount.increment(englishTrigramCounts[key] ?: 0)
            englishTrigramNextCounts.getOrPut(prefix) { mutableMapOf() }[clean] = englishTrigramCounts.getValue(key)
        }
        if (englishWordCounts.size > 2500) {
            englishWordCounts.entries.sortedBy { it.value }
                .take(englishWordCounts.size - 2000)
                .forEach { englishWordCounts.remove(it.key) }
        }
        if (englishPairCounts.size > 5000) {
            englishPairCounts.entries.sortedBy { it.value }
                .take(englishPairCounts.size - 4000).forEach { englishPairCounts.remove(it.key) }
            englishPairNextCounts = indexContextCounts(englishPairCounts, 1)
        }
        if (englishTrigramCounts.size > 6000) {
            englishTrigramCounts.entries.sortedBy { it.value }
                .take(englishTrigramCounts.size - 4500).forEach { englishTrigramCounts.remove(it.key) }
            englishTrigramNextCounts = indexContextCounts(englishTrigramCounts, 2)
        }
        persistPersonalData()
    }

    @Synchronized
    fun completions(prefix: String): List<String> {
        refreshPersonalDataIfChanged()
        val key = normalize(prefix)
        if (key.length < 2) return emptyList()
        val learned = wordCounts
        val candidates = SuggestionCandidateSelector.top(
            (starterWords.asSequence().filter { it.startsWith(key) } +
                learned.keys.asSequence().filter { it.startsWith(key) })
                .distinct()
                .filterNot { it.equals(key, ignoreCase = true) },
            limit = 2,
            comparator = compareByDescending<String> { learned[it] ?: 0 }.thenBy { it.length }
        )

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
        refreshPersonalDataIfChanged()
        val clean = normalize(path).filter { it.isLetter() }
        if (clean.length < 3) return clean
        val candidates = (starterWords.asSequence() + wordCounts.keys.asSequence())
            .filter { kotlin.math.abs(it.length - clean.length) <= 1 }
            .distinct()
        val rankedCandidates = candidates.mapNotNull { word ->
            val distance = SwipeWordDistance.calculate(clean, word)
            if (distance > 1) null else SwipeWordCandidate(
                word = word,
                distance = distance,
                personalCount = wordCounts[word] ?: 0
            )
        }
        return SwipeCandidateSelector.selectTurkish(rankedCandidates, clean)
    }

    /**
     * Conservative English swipe decoder using fixed and privately learned vocabulary.
     */
    @Synchronized
    fun decodeEnglishSwipePath(path: String): String {
        refreshPersonalDataIfChanged()
        val clean = path.lowercase(Locale.US).filter { it in 'a'..'z' }
        if (clean.length < 3) return clean
        val rankedCandidates = (englishStarterWords.asSequence() + englishWordCounts.keys.asSequence())
            .distinct()
            .filter { kotlin.math.abs(it.length - clean.length) <= 1 }
            .mapNotNull { word ->
                val distance = SwipeWordDistance.calculate(clean, word)
                if (distance > 1) null else SwipeWordCandidate(
                    word = word,
                    distance = distance,
                    personalCount = englishWordCounts[word] ?: 0,
                    frequencyRank = englishFrequencyRank[word] ?: Int.MAX_VALUE
                )
            }
        return SwipeCandidateSelector.selectEnglish(rankedCandidates, clean)
    }

    @Synchronized
    fun nextWords(previousWord: String, previousPreviousWord: String? = null): List<String> {
        refreshPersonalDataIfChanged()
        val previous = normalize(previousWord)
        if (previous.isBlank()) return emptyList()

        val learned = pairNextCounts[previous].orEmpty()
        val previousPrevious = previousPreviousWord?.let(::normalize).orEmpty()
        val trigramPrefix = if (previousPrevious.isBlank()) "" else "$previousPrevious>$previous"
        val contextual = if (trigramPrefix.isBlank()) emptyMap()
            else trigramNextCounts[trigramPrefix].orEmpty()
        val seeded = starterNextWords[previous].orEmpty()
        val seededContext = starterTrigrams["$previousPrevious>$previous"].orEmpty()
        return TurkishContextSuggestionSelector.select(
            contextualHints = seededContext,
            seededHints = seeded,
            learnedPairCounts = learned,
            learnedTrigramCounts = contextual,
            personalWordCounts = wordCounts,
            limit = 3
        )
    }

    private fun normalize(value: String): String = UnicodeWordNormalizer.normalize(value, locale)
}
