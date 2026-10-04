package com.keluargakendali.ui

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner iklan AdMob - HANYA dipasang di dashboard orang tua (lihat MainActivity.kt), TIDAK
 * PERNAH di akun anak. Keputusan desain ini disengaja: Google Play Families Policy mewajibkan
 * jaringan iklan bersertifikasi keluarga kalau iklan tampil di akun yang dipakai anak - AdMob
 * standar (yang dipakai di sini) HANYA aman untuk audiens dewasa, jadi membatasinya ke akun
 * orang tua saja menghindari kerumitan kepatuhan itu sama sekali.
 *
 * Wadahnya dialokasikan persis 10% tinggi layar oleh pemanggil lewat Modifier.weight (lihat
 * MainActivity.kt) - banner ini diisi dengan ukuran "adaptive banner" RESMI dari Google yang
 * menyesuaikan lebar layar (tingginya otomatis dipilih Google, biasanya 50-90dp tergantung
 * lebar layar, dalam praktiknya sering mendekati 10% tinggi layar ponsel pada umumnya, TAPI
 * TIDAK dijamin pas persis). Kreatif iklan SENGAJA tidak dipaksa meregang mengisi penuh tinggi
 * wadah - meregangkan iklan di luar ukuran yang didukung Google melanggar kebijakan AdMob.
 *
 * App ID (AndroidManifest.xml) & ID unit iklan di bawah ini ID TES RESMI Google (sama untuk
 * semua developer, didokumentasikan resmi di https://developers.google.com/admob/android/test-ads) -
 * WAJIB diganti ke App ID & ID unit iklan banner ASLI dari akun AdMob sebelum rilis produksi.
 * Menampilkan iklan tes ke pengguna sungguhan (bukan tes) melanggar kebijakan AdMob.
 */
@Composable
fun ParentAdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, AdSize.FULL_WIDTH))
            adUnitId = "ca-app-pub-3940256099942544/9214589741"
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    // adView.resume()/pause() mengikuti siklus hidup Activity TIDAK dipasang di sini (hanya
    // load sekali + destroy saat composable ini benar-benar hilang dari komposisi) - sedikit
    // kurang optimal untuk baterai/refresh iklan dibanding mengikat ke lifecycle Activity penuh,
    // tapi cukup untuk kebutuhan sekarang & menghindari dependency tambahan hanya untuk ini.
    DisposableEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
        onDispose { adView.destroy() }
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(factory = { adView })
    }
}
