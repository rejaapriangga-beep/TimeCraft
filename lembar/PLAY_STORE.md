# Rilis Lembar ke Google Play

Panduan langkah demi langkah, dari akun developer sampai aplikasi tayang. Semua teks
listing dan jawaban formulir sudah disiapkan di bawah, tinggal disalin.

## 0. Yang sudah siap di repo

| Bagian | Lokasi |
|---|---|
| Build rilis (AAB) bertanda tangan, R8 aktif | `.github/workflows/lembar-build.yml`, artifact `lembar-release-aab` |
| Ikon toko 512 × 512 | `lembar/play-store/icon-512.png` |
| Feature graphic 1024 × 500 | `lembar/play-store/feature-graphic-1024x500.png` |
| Kebijakan privasi | `backend/web/lembar-privacy.html`, disajikan di `https://timecraft.my.id/lembar/privacy` |
| applicationId | `id.my.timecraft.lembar` (permanen setelah upload pertama) |

## 1. Pasang upload key di GitHub (sekali saja)

Play memakai *Play App Signing*: Google menyimpan kunci penandatangan aplikasi yang asli,
dan kamu hanya memegang **upload key** untuk mengunggah. Kalau upload key hilang, bisa
di-reset lewat Play Console, jadi tidak fatal.

1. Simpan `lembar-upload.jks` dan `RAHASIA-upload-key-lembar.txt` di tempat aman
   (bukan di repo).
2. Buka repo di GitHub → **Settings → Secrets and variables → Actions → New repository secret**.
   Buat 4 secret sesuai isi `RAHASIA-upload-key-lembar.txt`:
   - `LEMBAR_UPLOAD_KEYSTORE_BASE64`
   - `LEMBAR_UPLOAD_KEYSTORE_PASSWORD`
   - `LEMBAR_UPLOAD_KEY_ALIAS`
   - `LEMBAR_UPLOAD_KEY_PASSWORD`
3. Buka tab **Actions → Lembar Build → Run workflow** (atau push perubahan apa pun ke
   `lembar/`). Setelah selesai, unduh artifact **`lembar-release-aab`**, ekstrak, dan
   dapatkan `Lembar-release.aab`.

## 2. Pasang halaman kebijakan privasi

Deploy ulang backend TimeCraft di VPS seperti biasa (lihat `backend/DEPLOY-UBUNTU.md`).
Setelah itu pastikan `https://timecraft.my.id/lembar/privacy` bisa dibuka di browser.

## 3. Buat aplikasi di Play Console

1. Buka [play.google.com/console](https://play.google.com/console) → **Create app**.
   - App name: `Lembar: Catatan & Tulis Tangan`
   - Default language: Indonesian – id
   - App or game: **App**
   - Free or paid: **Free**
2. **Penting untuk akun pribadi** yang dibuat setelah 13 November 2023: sebelum boleh rilis
   ke Production, Google mewajibkan **closed testing dengan minimal 12 penguji yang ikut
   selama 14 hari berturut-turut**. Jadi urutannya: Closed testing dulu → 14 hari →
   ajukan akses Production. Akun organisasi tidak terkena aturan ini.

## 4. App content (menu Policy → App content)

| Formulir | Jawaban |
|---|---|
| Privacy policy | `https://timecraft.my.id/lembar/privacy` |
| Ads | **No, my app does not contain ads** |
| App access | **All functionality is available without special access** (tidak ada login) |
| Content rating | Kategori **Utility, Productivity, Communication, or Other**. Jawab **No** untuk semua pertanyaan (kekerasan, seksual, judi, dsb.). Untuk "berbagi konten antarpengguna / interaksi pengguna": **No**. Hasilnya biasanya rating 3+ / Semua Umur. |
| Target audience | **18 tahun ke atas** (paling sederhana; memilih usia di bawah 13 memicu kewajiban kebijakan Keluarga). |
| News app | **No** |
| Data safety | Lihat bagian di bawah |
| Government app | **No** |
| Financial features | **My app doesn't provide any financial features** |
| Health | **No** |

### Data safety

- *Does your app collect or share any of the required user data types?* → **No**
- Tidak ada akun, jadi tidak perlu URL hapus akun.

Ini akurat karena Lembar tidak punya izin internet sama sekali; catatan hanya ada di
perangkat. (Cadangan otomatis Android tidak dihitung sebagai pengumpulan data oleh
developer.)

## 5. Store listing (Grow → Store presence → Main store listing)

**App name** (maks 30): `Lembar: Catatan & Tulis Tangan`

**Short description** (maks 80):
```
Ketik atau tulis tangan pakai pensil tablet. Sederhana, tanpa iklan, offline.
```

**Full description**:
```
Lembar adalah aplikasi catatan yang sengaja dibuat sederhana. Buka, ketuk sekali, langsung menulis.

DUA CARA MENCATAT
• Mode Ketik: teks bebas dan checklist dengan bilah progres di kartu.
• Mode Tulis: tulis tangan atau gambar sketsa pakai pensil tablet (S Pen, pensil USI) atau jari.
Satu catatan boleh berisi keduanya.

NYAMAN UNTUK PENSIL
• Tebal goresan mengikuti tekanan pensil.
• Palm rejection: telapak tangan boleh bertumpu di layar, hanya pensil yang menulis.
• Ujung penghapus pada pensil langsung menghapus.
• Pena, stabilo, penghapus per goresan, 4 warna tinta, dan urungkan.
• Kertas bergaris, bertitik, atau polos.
• Catatan berlembar-lembar: tambah, pindah, dan hapus lembar.
• Alat Pilih: blok area lalu pindahkan, salin, potong, hapus, tempel, atau ganti warna coretan.

RAPI TANPA RIBET
• Kelompokkan catatan ke ruang Kerja, Belajar, atau Pribadi.
• Sematkan catatan penting agar selalu di atas.
• 7 warna kartu pastel.
• Pencarian langsung di judul, isi, dan checklist.
• Tampilan dua panel di tablet, mode gelap mengikuti sistem.

PRIVAT SEPENUHNYA
Tanpa akun, tanpa iklan, tanpa internet. Semua catatan hanya tersimpan di perangkatmu.
```

**Kategori**: Productivity. **Tags**: Notes, Productivity.

**Grafis**:
- App icon: `lembar/play-store/icon-512.png`
- Feature graphic: `lembar/play-store/feature-graphic-1024x500.png`
- Phone screenshots: **minimal 2**, potret 9:16. Ambil dari HP sungguhan (tombol
  power + volume bawah). Saran urutan:
  1. Daftar catatan (dengan beberapa kartu berwarna)
  2. Mode Tulis dengan tulisan tangan
  3. Alat Pilih dengan coretan terpilih
  4. Mode Ketik dengan checklist
- Tablet screenshots (7" dan 10"): opsional, tapi dibutuhkan agar Lembar ditampilkan
  sebagai aplikasi tablet. Ambil di tablet dengan tampilan dua panel.

## 6. Closed testing (wajib untuk akun pribadi baru)

1. **Test and release → Testing → Closed testing → Create track** (atau pakai track
   "Alpha" bawaan).
2. **Testers**: buat daftar email, minimal 12 orang (keluarga, teman). Masing-masing harus
   membuka link opt-in dari Play Console, menekan "Become a tester", lalu memasang
   aplikasinya dari Play Store, dan tetap terdaftar selama 14 hari.
3. **Create new release** → unggah `Lembar-release.aab`.
   - Saat pertama kali, Play menawarkan *Play App Signing* → pilih **Use Google-generated key**
     (default). Upload key dari langkah 1 otomatis terdaftar dari AAB pertama.
   - Release name: `1.0.0 (1)`; Release notes: `Rilis pertama Lembar.`
4. **Review release → Start rollout to Closed testing**. Review pertama biasanya beberapa
   hari.
5. Setelah 14 hari, buka **Dashboard → Apply for production access** dan jawab pertanyaan
   tentang pengujiannya.

## 7. Update berikutnya

Setiap build baru untuk Play wajib punya `versionCode` lebih besar:
ubah `versionCode` (dan `versionName`) di `lembar/app/build.gradle.kts`, push, lalu unggah
`lembar-release-aab` yang baru.
