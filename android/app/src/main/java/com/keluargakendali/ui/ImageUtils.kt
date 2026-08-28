package com.keluargakendali.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Target sisi terpanjang untuk thumbnail kecil (grid/bubble) - cukup untuk tampilan kecil, hemat memori. */
const val THUMBNAIL_DECODE_TARGET_PX = 240

/** Target sisi terpanjang untuk pratinjau layar penuh/zoom - cukup detail untuk diperbesar tanpa membebani memori seperti resolusi kamera asli (bisa puluhan megapiksel). */
const val PREVIEW_DECODE_TARGET_PX = 2048

/**
 * Decode byte gambar dengan downsampling (BitmapFactory.inSampleSize) ke sekitar [targetLongSide]
 * pada sisi terpanjang - dipakai supaya foto beresolusi tinggi (kamera asli, sampai puluhan
 * megapiksel sejak batas ukuran dinaikkan) tidak menghabiskan memori saat cuma perlu ditampilkan
 * sebagai thumbnail atau pratinjau. Byte aslinya (dikirim ke server / disimpan) TIDAK diubah oleh
 * fungsi ini - cuma bitmap hasil decode-nya yang lebih kecil.
 */
fun decodeSampledBitmap(bytes: ByteArray, targetLongSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= targetLongSide || bounds.outHeight / (sampleSize * 2) >= targetLongSide) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}
