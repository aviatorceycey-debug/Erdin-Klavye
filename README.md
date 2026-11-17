# Erdin Klavye

Türkçe QWERTY odaklı, çevrimdışı kullanım ve gizlilik öncelikleriyle geliştirilen Android klavye uygulaması.

## Platform
- Paket: `com.erdin.klavye`
- Minimum Android: 10 (API 29)
- Compile / Target SDK: 35
- Java ve Kotlin: 17

## Klavye özellikleri
- Kompakt QWERTY düzeni ve sürekli sayı satırı.
- Türkçe karakter alternatifleri uzun basışla açılır: ç, ğ, ı, ö, ş, ü.
- Shift, Caps Lock, otomatik büyük harf ve çift boşlukla nokta.
- Türkçe ve İngilizce düzen/dil geçişi.
- Yerel kelime tamamlama, sonraki kelime tahminleri ve sistem yazım denetimi entegrasyonu.
- Cihazda tutulan kişisel kelime öğrenimi; kişisel sözlüğü görüntüleme ve kelime/veri silme.
- Emoji paneli, son kullanılan emojiler ve oturumla sınırlı pano geçmişi.
- Sembol sayfaları ve noktalama tuşlarında alternatif karakterler.
- Sayısal, telefon, tarih ve saat alanlarına uygun tuş takımları.
- Boşluk çubuğunda kaydırarak imleç hareketi; isteğe bağlı deneysel kaydırarak yazma.
- Ayarlanabilir yükseklik, tek elle kullanım, üç tema, titreşim, tuş sesi ve harf önizlemesi.
- TalkBack için tuş, öneri, araç çubuğu ve uzun basış alternatiflerine erişilebilir düğümler.

## Gizlilik
- Yazılan metin ve kişisel öğrenme kayıtları uygulama tarafından sunucuya gönderilmez.
- Kişisel öğrenme ayarı kapatılabilir; öğrenilen kelimeler tek tek veya topluca silinebilir.
- Pano geçmişi giriş oturumu boyunca geçicidir ve oturum bitince temizlenir.
- Parola alanlarında öneriler ve kişisel öğrenme devre dışı bırakılır.

## Derleme
GitHub Actions, Android projesini Java 17 ve Gradle 8.9 ile derler. Her başarılı iş akışı APK çıktısını artefakt olarak yayınlar.

## Test durumu
CI derlemesi otomatik olarak doğrulanır. Gerçek cihazlarda klavye davranışı, farklı uygulama alanları ve TalkBack erişilebilirliği ayrıca test edilmelidir; CI başarısı bu cihaz testlerinin yapıldığı anlamına gelmez.
