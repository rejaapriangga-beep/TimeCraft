# Langkah 1.1–1.3 — Proyek Godot, Layar Home/Setup, dan Board

Hasil langkah ini: aplikasi bisa dibuka, pindah **Home → Setup → Game**, dan board 32 petak
tampil rapi dari file JSON. Tap petak mana saja untuk melihat detailnya di tengah board.
Dadu dan gerakan token **belum** ada (langkah 1.4–1.5).

Sudah diuji di Godot **4.4.1** pada ukuran 720×1280 (HP), 800×1280 (tablet), dan 540×1200 (HP 20:9).

---

## Cara memasang & menjalankan

1. Unduh **Godot 4.4 (Standard, bukan .NET)** dari https://godotengine.org/download
   (versi 4.3 ke atas seharusnya juga bisa).
2. Ambil folder `monopoly-family/` dari repo ini (git clone, atau Code → Download ZIP di GitHub).
3. Buka Godot → **Import** → pilih file `monopoly-family/project.godot` → **Import & Edit**.
   Pertama kali dibuka, Godot butuh beberapa detik untuk meng-import font.
   > Pada pembukaan **pertama** mungkin muncul beberapa error merah di panel Output tentang
   > font / `main_theme.tres`. Ini normal: theme dimuat sebelum font selesai di-import.
   > Pilih **Project → Reload Current Project** sekali, error tidak akan muncul lagi.
4. Tekan **F5** (atau tombol ▶ di kanan atas) untuk menjalankan game.
   Jendela muncul dalam bentuk HP tegak (405×720).
5. Ingin langsung mencoba board tanpa lewat menu? Buka `scenes/game/Game.tscn`, lalu tekan
   **F6**. Game otomatis memakai 2 pemain contoh (Andi & Budi).

Tidak ada plugin atau aset tambahan yang perlu dipasang. Font Nunito sudah ikut di
`assets/fonts/` (lisensi bebas SIL OFL, lihat `assets/fonts/OFL.txt`).

---

## Daftar file (apa isinya dan kenapa)

| File | Isi |
|---|---|
| `project.godot` | Pengaturan proyek: layar tegak, resolusi dasar 720×1280, renderer *Compatibility* (paling luas didukung HP Android), autoload, theme, warna latar |
| `assets/themes/main_theme.tres` | **Theme**: font, warna, dan bentuk tombol/label/kartu untuk semua layar. Ubah warna di sini, semua layar ikut berubah |
| `assets/fonts/` | Nunito SemiBold (teks biasa) dan ExtraBold (judul & tombol) |
| `data/board_classic.json` | 32 petak board: nama, harga, sewa, grup warna |
| `data/config.json` | Angka aturan: uang awal, bonus START, simbol uang, dll. |
| `data/tokens.json` | Daftar token pemain + warnanya |
| `scripts/autoload/game_data.gd` | **GameData**: membaca semua JSON sekali saat start. Dipakai di mana saja: `GameData.get_tile(5)` |
| `scripts/autoload/scene_router.gd` | **SceneRouter**: pindah layar + tombol Back Android. `SceneRouter.goto("setup", {...})` |
| `scripts/ui/ui_style.gd` | Warna (NAVY, ORANGE, ...) + helper membuat kotak rounded & lingkaran token |
| `scripts/ui/board_view.gd` | Menyusun 32 petak menjadi board persegi + panel tengah |
| `scripts/ui/tile_view.gd` | Menggambar 1 petak: latar, pita warna, nama, harga. Ukuran font menyesuaikan petak |
| `scenes/home/Home.tscn` + `home.gd` | Layar utama: MAIN, MODE KELUARGA, VS BOT (nonaktif sampai Fase 3) |
| `scenes/setup/Setup.tscn` + `setup.gd` | Pilih jumlah pemain 2–4, nama, token |
| `scenes/setup/PlayerSlot.tscn` + `player_slot.gd` | Satu baris pemain (dipakai ulang 2–4 kali) |
| `scenes/game/Game.tscn` + `game.gd` | Layar permainan: bar giliran, board, daftar pemain, tombol LEMPAR DADU |

---

## Cara kerjanya (versi singkat)

**Alur layar**
```
Home ──MAIN / MODE KELUARGA──► Setup ──MULAI──► Game
  ▲                              │  ▲              │
  └──────── KEMBALI / Back ──────┘  └── MENU / Back┘  (pemain yang sudah diisi tetap diingat)
```
Setup mengirim daftar pemain ke Game lewat `SceneRouter.goto("game", {"players": [...]})`,
lalu Game membacanya dari `SceneRouter.params`.

**Board dibangun dari data, bukan digambar manual**

`board_view.gd` membaca jumlah petak dari JSON, lalu menghitung posisi setiap petak.
Petak 0 (START) di pojok kanan bawah, lalu berjalan searah jarum jam. Petak sudut dibuat
1,4× lebih besar dari petak biasa. Karena semuanya dihitung, board otomatis menyesuaikan
ukuran layar, dan board 40 petak pun bisa dipakai hanya dengan mengganti JSON.

Fungsi `get_tile_center(index)` di `board_view.gd` sudah disiapkan untuk langkah 1.5
(menaruh dan menggerakkan token).

---

## Mengubah sendiri (latihan kecil)

| Ingin... | Ubah di |
|---|---|
| Ganti nama/harga petak | `data/board_classic.json` → `name`, `short_name`, `price` |
| Ganti warna grup | `data/board_classic.json` → bagian `groups` |
| Ganti simbol uang jadi "Rp" | `data/config.json` → `"currency_symbol": "Rp"` |
| Ganti warna tombol utama | `assets/themes/main_theme.tres` (klik 2× di FileSystem → panel Theme) |
| Ganti teks judul | `scenes/home/Home.tscn` → node `Title` |

Setelah mengubah JSON, cukup jalankan ulang game (F5).

---

## Checklist uji

- [ ] F5 → layar Home tampil dengan 6 kotak warna dan 3 tombol
- [ ] MODE KELUARGA → layar Setup; tombol 2/3/4 menambah/mengurangi baris pemain
- [ ] Tap lingkaran token → token berganti, tidak pernah sama dengan pemain lain
- [ ] MULAI → board tampil penuh, nama petak terbaca, semua pita warna menghadap ke tengah
- [ ] Tap petak → petak diberi garis oranye dan detailnya muncul di tengah board
- [ ] MENU → kembali ke Setup, nama pemain tetap terisi
- [ ] (Opsional) Project Settings → Display → Window → ubah *Window Width/Height Override*
      ke 450×800 atau 640×1024 untuk mencoba ukuran layar lain
