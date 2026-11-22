# Tarihin İncisi — Android Studio ile AAB Alma Kılavuzu

Bu klasördeki `TarihinIncisi/` bir Android Studio projesidir. Uygulamanın tamamı
`app/src/main/assets/index.html` dosyasının içindedir; uygulama **internet olmadan** çalışır,
alan adına ya da sunucuya ihtiyaç duymaz.

| Bilgi | Değer |
|---|---|
| Paket adı (applicationId) | `com.tarihinincisi.app` |
| Sürüm | versionCode **1**, versionName **1.0.0** |
| En düşük Android | 7.0 (API 24) |
| Hedef Android | 16 (API 36) |
| İmza anahtarı | `tarihin-incisi.keystore` (ayrı zip'te), takma ad `tarihinincisi` |

---

## 1. Projeyi aç

1. Zip'i bilgisayarında bir klasöre çıkar (ör. Belgeler).
2. Android Studio'yu aç → **Open** → çıkardığın klasördeki **TarihinIncisi** klasörünü seç → **Open**.
3. "Trust project?" sorulursa **Trust Project** de.
4. Android Studio projeyi hazırlar (**Gradle Sync**). İlk seferde internetten birkaç yüz MB indirir; 2–10 dakika sürebilir.
   Alt çubukta işlem bitene kadar bekle.

### Sync sırasında çıkabilecek uyarılar

- **"Android SDK Platform 36 is not installed" / "Failed to find target android-36":**
  Hata mesajındaki **Install missing platform** bağlantısına tıkla. Ya da **Tools → SDK Manager → SDK Platforms** sekmesinde
  **Android 16 (API 36)** kutusunu işaretleyip **Apply** de.
- **"Android Gradle Plugin can be upgraded" / Upgrade Assistant:** Yükseltmeyi kabul edebilirsin; zorunlu değil.
- **"Gradle JDK" hatası:** **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** alanında
  Android Studio'nun kendi JDK'sını (**jbr-17** ya da **jbr-21**) seç.

## 2. (İsteğe bağlı) Telefonda ya da sanal cihazda dene

- **Sanal cihaz:** **Tools → Device Manager → +** ile bir Pixel cihazı oluştur, sonra üstteki yeşil **▶ Run** düğmesine bas.
- **Kendi Android telefonun:** Telefonda *Ayarlar → Telefon hakkında → Yapım numarası*na 7 kez dokun,
  *Geliştirici seçenekleri → USB hata ayıklama*yı aç, telefonu kabloyla bağla ve **▶ Run**'a bas.

Denerken bakılacaklar: sorular açılıyor mu, geri tuşu ekranlar arasında dönüyor mu, PDF indirince
"İndirilenler › Tarihin İncisi klasörüne kaydedildi" yazıyor mu, tema değişince üst çubuğun rengi değişiyor mu.

## 3. AAB dosyasını al

1. Menüden **Build → Generate Signed App Bundle or APK…**
2. **Android App Bundle** seç → **Next**.
3. **Key store path:** **Choose existing…** → `tarihin-incisi.keystore` dosyasını seç.
4. **Key store password**, **Key alias** (`tarihinincisi`) ve **Key password** alanlarını
   `anahtar-bilgileri.txt` dosyasından kopyala. (**Remember passwords** kutusunu işaretleme.)
5. **Next** → **release** seç → **Create**.
6. Bitince sağ altta **locate** bağlantısı çıkar. Dosya şurada olur:
   `TarihinIncisi/app/release/app-release.aab`

## 4. Play Console'a yükle

1. play.google.com/console → uygulamanı oluştur (ad: Tarihin İncisi, dil: Türkçe, Uygulama, Ücretsiz).
2. **Test → Kapalı test** (yeni hesaplarda önce zorunlu) ya da **Üretim → Yeni sürüm oluştur**.
3. **Play Uygulama İmzalama** sorulursa Google'ın önerisini kabul et. Bu durumda senin keystore'un
   "yükleme anahtarı" olur; Google mağazadaki imzayı kendi anahtarıyla atar.
4. `app-release.aab` dosyasını yükle.
5. Mağaza girişi için:
   - **Uygulama ikonu (512×512):** `play-ikon-512.png`
   - **Gizlilik politikası adresi:** zorunlu. `gizlilik.html` dosyasındaki `ILETISIM_EPOSTASI` yerlerine
     e-postanı yaz ve herkese açık bir adrese koy (kendi alan adın, Google Sites vb.).
   - **Veri güvenliği:** "Kullanıcı verisi toplanmıyor veya paylaşılmıyor" (her şey yalnızca telefonda saklanır; uygulamanın internet izni bile yok).
   - **Reklam:** Hayır · **Hedef kitle:** 18 yaş ve üzeri · **Kategori:** Eğitim

## 5. Güncelleme yaparken

1. Yeni `index.html` dosyasını `app/src/main/assets/index.html` ile değiştir.
2. `app/build.gradle` içinde **versionCode**'u bir artır (1 → 2) ve **versionName**'i değiştir (1.0.0 → 1.0.1).
3. 3. adımı tekrarla, yeni AAB'yi Play Console'da **yeni sürüm** olarak yükle.

Kullanıcıların kayıtları (çözümler, istatistikler, favoriler) güncellemede korunur.
Uygulama kaldırılırsa kayıtlar silinir.

## Bilmen gerekenler

- Bu proje Android SDK'sı olmayan bir ortamda hazırlandı; Android Studio'da derlenip bir cihazda denenmedi.
  Sync ya da derleme sırasında bir hata çıkarsa hata mesajını olduğu gibi kopyalayıp gönder.
- Keystore dosyasını ve şifresini proje klasörünün içine koyma; ayrı ve güvenli bir yerde sakla.
