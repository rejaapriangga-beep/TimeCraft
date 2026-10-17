# Lembar

Aplikasi catatan Android yang sederhana: ketik atau tulis tangan pakai pensil tablet.
Konsep dan prototipenya: https://claude.ai/artifact/C5AQTCFrNzkFgKfGVXNMuW

## Fitur

- **Mode Ketik**: teks bebas + checklist dengan bilah progres di kartu.
- **Mode Tulis**: kanvas tulis tangan (rasio 3:4) dengan pena peka tekanan, stabilo,
  penghapus per goresan, 4 warna tinta, urungkan, dan kertas garis/titik/polos.
  - **Berlembar-lembar**: tambah lembar baru, pindah lembar, hapus atau bersihkan lembar
    (semuanya bisa diurungkan).
  - **Alat Pilih**: seret kotak untuk memblok coretan, lalu pindahkan (seret kotaknya),
    salin, potong, hapus, tempel, atau ganti warnanya. Papan klip berlaku lintas lembar
    dan lintas catatan.
  - Ujung penghapus pada pensil (S Pen, pensil USI) langsung menghapus.
  - Penolakan telapak (palm rejection):
    - Pensil selalu menang. Kalau telapak menempel duluan lalu pensil menyentuh layar,
      coretan telapak dibuang dan pensil langsung menulis.
    - Telapak boleh terus bertumpu sementara pensil diangkat dan diturunkan berkali-kali.
    - Sentuhan yang dibatalkan Android sendiri sebagai telapak juga dibuang.
    - Begitu pensil pertama kali terdeteksi, "Hanya pensil" menyala otomatis dan diingat
      untuk semua catatan, juga setelah aplikasi dibuka ulang. Bisa dimatikan manual.
- Satu catatan boleh berisi teks dan tulisan tangan sekaligus.
- Ruang Kerja / Belajar / Pribadi, sematkan, 7 warna kartu (lewat lingkaran warna di bilah atas,
  tersedia di mode Ketik maupun Tulis), pencarian judul/isi/checklist.
- **Tablet** (lebar ≥ 720 dp): daftar di kiri, editor di kanan.
- Simpan otomatis saat aplikasi ditutup atau berpindah catatan. Mode gelap mengikuti sistem.
- Tanpa izin, tanpa internet, tanpa iklan. Semua data ada di `files/notes.json` di perangkat.

## Struktur

```
lembar/app/src/main/java/id/my/timecraft/lembar/
├── MainActivity.kt        # navigasi daftar ↔ editor, tata letak ponsel/tablet, simpan otomatis
├── NotesViewModel.kt      # state daftar catatan
├── data/                  # model Note/InkStroke, penyimpanan JSON, catatan contoh
└── ui/
    ├── NoteListScreen.kt  # daftar, pencarian, filter ruang, tombol catatan baru
    ├── EditorScreen.kt    # editor: mode Ketik & Tulis
    ├── ink/               # kanvas tulis tangan, penggambar goresan, pratinjau di kartu
    └── theme/             # warna, huruf (Fraunces, Plus Jakarta Sans, Caveat)
```

Goresan disimpan sebagai titik vektor (x, y, tekanan) dalam ruang logis 1000 × 1333,
sehingga tetap tajam di layar ukuran apa pun.

## Build

APK debug dibangun otomatis oleh GitHub Actions (`.github/workflows/lembar-build.yml`)
setiap ada push yang mengubah folder `lembar/`. Unduh dari tab **Actions** → run terbaru →
**Artifacts** → `lembar-debug-apk`.

Build lokal (butuh Android SDK 36 dan JDK 21):

```bash
cd lembar
./gradlew assembleDebug
# hasil: app/build/outputs/apk/debug/Lembar-debug.apk
```
