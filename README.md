# 🎵 Precision Metronome (Android / Jetpack Compose)

Modern, ultra-düşük gecikmeli (low-latency), sıfır zamanlama kaymalı (zero-drift) ve analog estetiğe sahip profesyonel Android Metronom uygulaması.

---

## 📱 Ekran Görüntüsü

<p align="center">
  <img src="screenshots/metronome_preview.png" alt="Metronome App Preview" width="360"/>
</p>

---

## ✨ Özellikler

* **Örnekleme Hassasiyetinde Zamanlama (Sample-Accurate AudioTrack)**:
  * Donanım ses saati (DAC clock) referanslı 16-bit PCM streaming altyapısı.
  * `Thread.sleep()` veya işletim sistemi zamanlayıcı dalgalanmalarından bağımsız mutlak zamanlama matematiği ile **0.00 ms birikimli sapma (zero cumulative drift)**.
* **Sentetik ve Doğal Ahşap (Woodblock) Tınısı**:
  * Disk I/O yapmadan belleğe yüklenen 16-bit PCM woodblock ses sentezi.
  * 1. vuruş için yüksek rezonanslı **Accent (1450 Hz)**, diğer vuruşlar için tok **Normal (920 Hz)** vuruş tınısı.
* **Dinamik Zaman Ölçüsü & Görsel Gösterge (Visualizer)**:
  * İstenen her ölçü için dinamik vuruş daireleri (örn. 5/4 için 5 daire, 7/8 için 7 daire vb.).
  * Sıralı vuruş animasyonu ve 1. vuruşta parlak Turuncu (`#EF6905`) vurgu.
  * **İkili Görünüm Modu**: Hem yatay sıra (Row) hem de dairesel kadran (Ring) formatı.
* **Hızlı ve Akıcı Kontroller**:
  * 10 ile 260 BPM arası geniş tempo aralığı ve kademesiz hassas kaydırıcı (Slider).
  * Basılı tutulduğunda ivmelenerek hızlanan `− / +` butonları (Hold-to-repeat).
  * Zaman ölçüsü payı (A: 1-20) ve paydası (B: 1-20) seçimi.
* **Analog & Modern M3 Tasarım**:
  * Bordo (`#8B2626`), Turuncu (`#EF6905`), Krem (`#F1E5A1`) ve Yeşil (`#486C2F`) renk paleti.
  * Çalma sırasında ekranı açık tutma (`FLAG_KEEP_SCREEN_ON`) ve son ayarları otomatik hatırlama (SharedPreferences).

---

## 🛠️ Mimari ve Teknolojiler

* **Dil:** Kotlin
* **UI Framework:** Jetpack Compose (Material Design 3)
* **Ses Motoru:** AudioTrack (16-bit PCM Streaming, URGENT_AUDIO öncelikli thread)
* **Durum Yönetimi:** Kotlin Coroutines & StateFlow (MVVM)
* **Test & Ekran Doğrulama:** Robolectric, Roborazzi

---

## 🚀 Projeyi Çalıştırma

1. Projeyi Android Studio ile açın.
2. Gradle senkronizasyonunu tamamlayın (`compileSdk 36`, `minSdk 26`).
3. Emülatörde veya fiziksel Android cihazınızda `Run` butonuna basın.

### Testleri Çalıştırma:
```bash
./gradlew :app:testDebugUnitTest
```

---

## 📤 GitHub'a Aktarma (AI Studio)

AI Studio üzerinden bu projeyi kendi GitHub hesabınıza aktarmak için:
1. Sağ üst köşedeki **Settings / Ayarlar** veya **Export / Share** menüsünü açın.
2. **"Push to GitHub"** veya **"Export to GitHub"** seçeneğini seçin.
3. GitHub hesabınızı yetkilendirip dilediğiniz repository adıyla projeyi doğrudan GitHub'a gönderebilirsiniz.
