package com.erdin.klavye

object EmojiAccessibilityLabeler {
    private val toneModifiers = listOf("🏻", "🏼", "🏽", "🏾", "🏿")

    private val names = mapOf(
        "😀" to ("grinning face" to "sırıtan yüz"),
        "😂" to ("face with tears of joy" to "kahkahadan ağlayan yüz"),
        "😊" to ("smiling face with smiling eyes" to "gülümseyen gözlü yüz"),
        "😍" to ("smiling face with heart eyes" to "kalp gözlü yüz"),
        "🥰" to ("smiling face with hearts" to "kalplerle gülümseyen yüz"),
        "😎" to ("smiling face with sunglasses" to "güneş gözlüklü yüz"),
        "🥳" to ("partying face" to "parti yapan yüz"),
        "😭" to ("loudly crying face" to "yüksek sesle ağlayan yüz"),
        "🤔" to ("thinking face" to "düşünen yüz"),
        "😴" to ("sleeping face" to "uyuyan yüz"),
        "👍" to ("thumbs up" to "başparmak yukarı"),
        "👎" to ("thumbs down" to "başparmak aşağı"),
        "👏" to ("clapping hands" to "alkış"),
        "🙏" to ("folded hands" to "birleşmiş eller"),
        "🤝" to ("handshake" to "tokalaşma"),
        "👌" to ("OK hand" to "tamam işareti"),
        "✌️" to ("victory hand" to "zafer işareti"),
        "🤞" to ("crossed fingers" to "çapraz parmaklar"),
        "🫶" to ("heart hands" to "kalp yapan eller"),
        "🙌" to ("raising hands" to "havaya kaldırılmış eller"),
        "👋" to ("waving hand" to "el sallama"),
        "💪" to ("flexed biceps" to "pazı"),
        "👊" to ("oncoming fist" to "yumruk"),
        "✊" to ("raised fist" to "kalkık yumruk"),
        "👶" to ("baby" to "bebek"),
        "👦" to ("boy" to "erkek çocuk"),
        "👧" to ("girl" to "kız çocuk"),
        "🧑" to ("person" to "kişi"),
        "👨" to ("man" to "erkek"),
        "👩" to ("woman" to "kadın"),
        "🧓" to ("older person" to "yaşlı kişi"),
        "👴" to ("old man" to "yaşlı erkek"),
        "👵" to ("old woman" to "yaşlı kadın"),
        "🧔" to ("person with beard" to "sakallı kişi"),
        "👮" to ("police officer" to "polis"),
        "👷" to ("construction worker" to "inşaat işçisi"),
        "🕵️" to ("detective" to "dedektif"),
        "🎅" to ("Santa Claus" to "Noel Baba"),
        "🤶" to ("Mrs. Claus" to "Noel Anne"),
        "🧙" to ("mage" to "büyücü"),
        "🧚" to ("fairy" to "peri"),
        "🧛" to ("vampire" to "vampir"),
        "🧜" to ("merperson" to "deniz insanı"),
        "🧝" to ("elf" to "elf"),
        "🧞" to ("genie" to "cin"),
        "🧟" to ("zombie" to "zombi"),
        "🤰" to ("pregnant person" to "hamile kişi"),
        "🤱" to ("breast-feeding" to "emziren kişi"),
        "❤️" to ("red heart" to "kırmızı kalp"),
        "🔥" to ("fire" to "ateş"),
        "✨" to ("sparkles" to "parıltılar"),
        "💯" to ("hundred points" to "yüz puan"),
        "🎉" to ("party popper" to "konfeti"),
        "🌹" to ("rose" to "gül"),
        "☀️" to ("sun" to "güneş"),
        "🌙" to ("crescent moon" to "hilal"),
        "🐱" to ("cat face" to "kedi yüzü"),
        "🐶" to ("dog face" to "köpek yüzü"),
        "🍎" to ("red apple" to "kırmızı elma"),
        "🍕" to ("pizza" to "pizza"),
        "☕" to ("hot beverage" to "sıcak içecek"),
        "🚗" to ("car" to "araba"),
        "✈️" to ("airplane" to "uçak"),
        "📱" to ("mobile phone" to "cep telefonu"),
        "💻" to ("laptop" to "dizüstü bilgisayar")
    )

    fun label(emoji: String, english: Boolean): String {
        val tone = toneModifiers.firstOrNull { emoji.endsWith(it) }
        val base = if (tone == null) emoji else emoji.removeSuffix(tone)
        val name = names[base]?.let { if (english) it.first else it.second }
            ?: if (english) "emoji" else "emoji"
        if (tone == null) return name
        val toneName = when (tone) {
            "🏻" -> if (english) "light skin tone" else "açık ten rengi"
            "🏼" -> if (english) "medium-light skin tone" else "açık-orta ten rengi"
            "🏽" -> if (english) "medium skin tone" else "orta ten rengi"
            "🏾" -> if (english) "medium-dark skin tone" else "orta-koyu ten rengi"
            else -> if (english) "dark skin tone" else "koyu ten rengi"
        }
        return "$name, $toneName"
    }
}
