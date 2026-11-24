package com.tarihinincisi.app;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Tarihin İncisi — uygulamanın tamamı assets/index.html içindedir ve internetsiz çalışır.
 * Sayfa, güvenli bir yerel adresten (https://appassets.androidplatform.net) açılır;
 * böylece kayıtlar (localStorage / IndexedDB) telefonda kalıcı olarak saklanır.
 */
public class MainActivity extends Activity {

    private static final String ADRES = "https://appassets.androidplatform.net/assets/index.html";
    private static final String KOK = "appassets.androidplatform.net";
    private static final int DOSYA_SECIMI = 4242;

    private WebView web;
    private FrameLayout kok;
    private ValueCallback<Uri[]> dosyaGeriDonus;
    private int zeminRengi = Color.parseColor("#F1E7D2");

    /** Test/deneme bitiminde gösterilen tam ekran reklam birimi. */
    private static final String REKLAM_BIRIMI = "ca-app-pub-6655722499363603/8671213302";
    /** İki reklam arasında en az bu kadar süre geçer. */
    private static final long REKLAM_ARASI_MS = 90_000L;
    private InterstitialAd reklam;
    private boolean reklamYukleniyor;
    private boolean reklamHazirlandi;
    private long sonReklamZamani;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        kok = new FrameLayout(this);
        kok.setBackgroundColor(zeminRengi);
        web = new WebView(this);
        web.setBackgroundColor(zeminRengi);
        kok.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(kok);
        kenarlariAyarla();

        WebSettings ayar = web.getSettings();
        ayar.setJavaScriptEnabled(true);
        ayar.setDomStorageEnabled(true);
        ayar.setDatabaseEnabled(true);
        ayar.setMediaPlaybackRequiresUserGesture(false);
        ayar.setAllowFileAccess(false);
        ayar.setAllowContentAccess(false);
        ayar.setTextZoom(100);
        ayar.setSupportZoom(false);

        final WebViewAssetLoader yukleyici = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return yukleyici.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (KOK.equals(u.getHost())) return false;
                // Uygulama dışı bağlantılar (e-posta, web sitesi) telefonun kendi uygulamasında açılır.
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "Bağlantı açılamadı", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> geriDonus, FileChooserParams parametre) {
                if (dosyaGeriDonus != null) dosyaGeriDonus.onReceiveValue(null);
                dosyaGeriDonus = geriDonus;
                try {
                    startActivityForResult(parametre.createIntent(), DOSYA_SECIMI);
                } catch (ActivityNotFoundException e) {
                    dosyaGeriDonus = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Kopru(), "TIAndroid");

        geriTusunuAyarla();
        reklamlariBaslat();

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(ADRES);
    }

    /** Android 15+ ekranı kenardan kenara çizer; sayfanın durum ve gezinme çubuklarının altında kalmaması için boşluk bırakılır. */
    private void kenarlariAyarla() {
        if (Build.VERSION.SDK_INT >= 30) kenarlar30();
        cubukRenkleriniUygula();
    }

    @TargetApi(30)
    private void kenarlar30() {
        getWindow().setDecorFitsSystemWindows(false);
        kok.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets b = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return WindowInsets.CONSUMED;
        });
    }

    @SuppressWarnings("deprecation")
    private void cubukRenkleriniUygula() {
        boolean acik = parlaklik(zeminRengi) > 0.5;
        kok.setBackgroundColor(zeminRengi);
        web.setBackgroundColor(zeminRengi);
        Window w = getWindow();
        if (Build.VERSION.SDK_INT < 35) {
            w.setStatusBarColor(zeminRengi);
            w.setNavigationBarColor(zeminRengi);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            simgeRengi30(acik);
        } else {
            View d = w.getDecorView();
            int f = d.getSystemUiVisibility();
            f = acik ? (f | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) : (f & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            d.setSystemUiVisibility(f);
        }
    }

    @TargetApi(30)
    private void simgeRengi30(boolean acik) {
        WindowInsetsController c = getWindow().getInsetsController();
        if (c != null) {
            int bayrak = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            c.setSystemBarsAppearance(acik ? bayrak : 0, bayrak);
        }
    }

    private static double parlaklik(int renk) {
        return (0.299 * Color.red(renk) + 0.587 * Color.green(renk) + 0.114 * Color.blue(renk)) / 255.0;
    }

    /** Geri tuşu: uygulama içinde bir önceki ekrana döner; ana ekrandaysa uygulamayı kapatır. */
    private void geriTusunuAyarla() {
        if (Build.VERSION.SDK_INT >= 33) geri33();
    }

    @TargetApi(33)
    private void geri33() {
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::geriGit);
    }

    private void geriGit() {
        if (web != null && web.canGoBack()) web.goBack();
        else finish();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        geriGit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DOSYA_SECIMI && dosyaGeriDonus != null) {
            dosyaGeriDonus.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            dosyaGeriDonus = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) web.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /** Reklam altyapısını arka planda başlatır; hata olursa uygulama reklamsız çalışmaya devam eder. */
    private void reklamlariBaslat() {
        new Thread(() -> {
            try {
                MobileAds.initialize(MainActivity.this, durum -> runOnUiThread(() -> {
                    reklamHazirlandi = true;
                    reklamYukle();
                }));
            } catch (Throwable ignored) {
                // Reklam başlatılamazsa uygulama etkilenmez.
            }
        }).start();
    }

    /** Bir sonraki reklamı önceden yükler. İnternet yoksa yükleme sessizce başarısız olur. */
    private void reklamYukle() {
        if (!reklamHazirlandi || reklam != null || reklamYukleniyor || isFinishing()) return;
        reklamYukleniyor = true;
        try {
            InterstitialAd.load(this, REKLAM_BIRIMI, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd yuklenen) {
                    reklam = yuklenen;
                    reklamYukleniyor = false;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError hata) {
                    reklam = null;
                    reklamYukleniyor = false;
                }
            });
        } catch (Throwable t) {
            reklamYukleniyor = false;
        }
    }

    /** Hazır bir reklam varsa gösterir; yoksa (ör. internet kapalıyken) hiçbir şey yapmaz. */
    private void reklamGoster() {
        long simdi = System.currentTimeMillis();
        if (reklam == null) {
            reklamYukle();
            return;
        }
        if (simdi - sonReklamZamani < REKLAM_ARASI_MS || isFinishing()) return;
        final InterstitialAd gosterilecek = reklam;
        reklam = null;
        gosterilecek.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                reklamYukle();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError hata) {
                reklamYukle();
            }
        });
        try {
            sonReklamZamani = simdi;
            gosterilecek.show(this);
        } catch (Throwable t) {
            reklamYukle();
        }
    }

    /** Sayfanın çağırdığı köprü: PDF kaydetme, kısa mesaj, tema rengi ve reklam. */
    private class Kopru {

        @JavascriptInterface
        public void reklamGoster() {
            runOnUiThread(MainActivity.this::reklamGoster);
        }

        @JavascriptInterface
        public void dosyaKaydet(String ad, String tur, String base64) {
            try {
                byte[] veri = Base64.decode(base64, Base64.DEFAULT);
                String guvenliAd = ad.replaceAll("[\\\\/:*?\"<>|]", "_");
                if (Build.VERSION.SDK_INT >= 29) {
                    Uri uri = indirilenlereYaz(guvenliAd, tur, veri);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "İndirilenler › Tarihin İncisi klasörüne kaydedildi", Toast.LENGTH_LONG).show();
                        ac(uri, tur);
                    });
                } else {
                    File klasor = new File(getCacheDir(), "paylasim");
                    if (!klasor.exists() && !klasor.mkdirs()) throw new IllegalStateException("klasör yok");
                    File f = new File(klasor, guvenliAd);
                    try (FileOutputStream os = new FileOutputStream(f)) {
                        os.write(veri);
                    }
                    Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".dosyalar", f);
                    runOnUiThread(() -> paylas(uri, tur));
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Dosya kaydedilemedi", Toast.LENGTH_SHORT).show());
            }
        }

        @JavascriptInterface
        public void mesaj(String metin) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, metin, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void temaRengi(String renk) {
            try {
                final int r = Color.parseColor(renk.trim());
                runOnUiThread(() -> {
                    zeminRengi = r;
                    cubukRenkleriniUygula();
                });
            } catch (Exception ignored) {
                // Renk okunamazsa varsayılan renk kalır.
            }
        }
    }

    @TargetApi(29)
    private Uri indirilenlereYaz(String ad, String tur, byte[] veri) throws Exception {
        ContentResolver cr = getContentResolver();
        ContentValues d = new ContentValues();
        d.put(MediaStore.MediaColumns.DISPLAY_NAME, ad);
        d.put(MediaStore.MediaColumns.MIME_TYPE, tur);
        d.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Tarihin İncisi");
        Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, d);
        if (uri == null) throw new IllegalStateException("kayıt yeri yok");
        try (OutputStream os = cr.openOutputStream(uri)) {
            if (os == null) throw new IllegalStateException("yazılamadı");
            os.write(veri);
        }
        return uri;
    }

    private void ac(Uri uri, String tur) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, tur);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException ignored) {
            // PDF açacak uygulama yoksa dosya yine İndirilenler'de durur.
        }
    }

    private void paylas(Uri uri, String tur) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(tur);
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(i, "Dosyayı kaydet veya paylaş"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Dosya kaydedilemedi", Toast.LENGTH_SHORT).show();
        }
    }
}
