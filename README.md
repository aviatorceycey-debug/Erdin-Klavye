# Erdin Klavye

Android için çevrimdışı çalışmayı önceliklendiren, Türkçe QWERTY odaklı bir klavye projesi.

## Proje durumu

Bu depo, Erdin Klavye'nin Android kaynak projesi için başlangıç noktasıdır. İlk aşamada temel Android Input Method Service (IME), kompakt QWERTY düzeni ve Türkçe karakter uzun-basışları kurulacaktır. Sonraki aşamalarda öneri motoru, emoji/clipboard, tema ve kişiselleştirme özellikleri eklenecektir.

## Tasarım kararları

- Uygulama adı: Erdin Klavye
- Paket: `com.erdin.klavye`
- Minimum Android: 10 (API 29)
- Varsayılan düzen: kompakt QWERTY ve 1–0 sayı satırı
- Türkçe karakterler uzun basışla: G→Ğ, U→Ü, C→Ç, S→Ş, O→Ö, I→İ/ı
- Temel yazma işlemi cihaz üzerinde; yazılan metin otomatik olarak sunucuya gönderilmez.

## Lisans ve temel

Bu depo başlangıçta özgün Erdin Klavye uygulama iskeletini barındırır. FlorisBoard, ayrı bir Apache-2.0 lisanslı açık kaynak proje olarak değerlendirilmiştir; kaynak kodu bu depoya henüz aktarılmamıştır.
