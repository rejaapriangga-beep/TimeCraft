# Tahap 1 — Desain Game "Monopoly Family" (nama kerja)

> Dokumen ini adalah fondasi sebelum coding. Semua keputusan di sini sengaja dibuat
> **sederhana** agar mudah diimplementasikan oleh pemula di Godot 4.x + GDScript.

> ⚠️ **Catatan nama:** "Monopoly" adalah merek dagang Hasbro. Untuk rilis di Play Store,
> gunakan nama sendiri, misalnya **"Kota Keluarga"**, **"Family Tycoon"**, atau
> **"Juragan Kota"**. Jangan pakai logo, nama tile, atau desain kartu asli Monopoly.
> Di dokumen ini "Monopoly Family" hanya nama kerja.

Isi:
1. Game Design Document (GDD) sederhana
2. Screen flow
3. UI hierarchy
4. Game architecture
5. Scene structure
6. Folder structure
7. Data structure
8. MVP development roadmap

---

## 1. Game Design Document (GDD)

### 1.1 Ringkasan

| Item | Keputusan |
|---|---|
| Genre | Board game ekonomi (gaya Monopoly), casual, family-friendly |
| Platform | Android (HP & tablet), **portrait** |
| Engine | Godot 4.x, GDScript |
| Mode | Local 2–4 pemain (Pass & Play), Vs Bot |
| Durasi 1 game | Target 15–25 menit (lebih pendek dari Monopoly asli) |
| Formula | 70% board game klasik + 20% koleksi & progression + 10% fitur keluarga |
| Core loop | PLAY → EARN XP/COINS → LEVEL UP → UNLOCK → PLAY AGAIN |
| Monetisasi (nanti) | Hanya kosmetik / rewarded ads opsional. Tanpa pay-to-win, tanpa energy system |

### 1.2 Keputusan desain penting: board 32 tile (bukan 40)

Board Monopoly klasik punya 40 tile (grid 11×11). Di HP portrait lebar ±360dp, setiap tile
hanya ±30dp — **nama property tidak terbaca**. Karena itu MVP memakai:

- **32 tile** (grid 9×9: 4 sudut + 7 tile per sisi)
- Lebar tile ±40dp di HP kecil, ±80dp di tablet
- Layout tetap klasik (persegi, sudut Start/Jail/Parkir/Masuk Penjara)
- Board disimpan di file JSON, jadi kalau nanti mau 40 tile cukup ganti data

Efek samping positif: game lebih cepat selesai, cocok untuk keluarga.

### 1.3 Isi board (32 tile)

| Jenis | Jumlah | Keterangan |
|---|---|---|
| Sudut | 4 | START, PENJARA (Just Visiting), PARKIR GRATIS, MASUK PENJARA |
| Property | 16 | 6 grup warna: 2-3-3-3-3-2 |
| Stasiun | 4 | Satu di tengah setiap sisi |
| Utilitas | 2 | Listrik & Air |
| Kesempatan (Event) | 4 | Ambil kartu event acak |
| Pajak | 2 | Bayar jumlah tetap |

Urutan lengkap ada di [`../data/board_classic.json`](../data/board_classic.json).

### 1.4 Aturan inti (MVP)

**Awal game**
- Setiap pemain mulai dengan **1.500** uang di petak START.
- Urutan giliran = urutan pemain di layar setup (tidak perlu lempar dadu untuk urutan).

**Satu giliran**
1. Tampilkan pemain aktif (nama, token, uang).
2. Pemain tekan **ROLL** → 2 dadu (1–6) beranimasi ±0,8 detik.
3. Token bergerak **per petak** (±0,15 detik/petak).
4. Melewati/mendarat di START → **+200**.
5. Game membaca tile tujuan → munculkan aksi yang relevan (lihat tabel di bawah).
6. Pemain memutuskan → giliran selesai.
7. **Dadu kembar (double)** → main lagi. Double 3× berturut-turut → masuk penjara.
8. Pindah ke pemain berikutnya (mode Pass & Play: layar "OPER KE BUDI").

**Aksi per tile**

| Tile | Belum dimiliki | Milik sendiri | Milik orang lain |
|---|---|---|---|
| Property | BELI / LEWATI | (MVP: tidak ada aksi) · Fase 3: UPGRADE | Bayar sewa otomatis |
| Stasiun | BELI / LEWATI | — | Sewa 25 / 50 / 100 / 200 (tergantung jumlah stasiun pemilik) |
| Utilitas | BELI / LEWATI | — | Sewa = total dadu × 4 (1 utilitas) atau × 10 (2 utilitas) |
| Kesempatan | Ambil 1 kartu event | | |
| Pajak | Bayar jumlah di tile | | |
| Masuk Penjara | Pindah ke Penjara, tidak lewat START | | |
| Parkir Gratis / Penjara (mampir) | Tidak terjadi apa-apa | | |

**Sewa property**
- Sewa dasar tertulis di data.
- Jika pemilik punya **semua** property satu warna → sewa **×2** (belum di-upgrade).

**Penjara (disederhanakan)**
- Di penjara maksimal 3 giliran.
- Tiap giliran pilih: **BAYAR 50** lalu roll normal, atau **COBA DOUBLE**.
- Jika dapat double → keluar & bergerak. Setelah gagal ke-3 → wajib bayar 50 lalu bergerak.

**Bangkrut**
- Jika uang < 0 setelah membayar:
  - MVP: pemain **langsung bangkrut**; semua property-nya kembali ke bank (tanpa pemilik).
  - Fase 3: diberi kesempatan jual/gadai dulu.
- Pemain bangkrut keluar dari permainan (tokennya jadi abu-abu).

**Menang**
- Pemenang = pemain terakhir yang tidak bangkrut, **atau**
- Batas waktu opsional (mis. 20 ronde): pemenang = kekayaan tertinggi
  (uang + harga beli semua property). Ini menjaga durasi game tetap pendek untuk keluarga.

### 1.5 Pemain & token

Setiap pemain punya: nama, token, uang, daftar property, posisi, status penjara, statistik game.

Token awal (gratis): Kucing, Anjing, Mobil. Token yang bisa di-unlock: Kereta, Roket,
Panda, Dinosaurus. **Semua token hanya kosmetik** — tidak ada kelebihan gameplay.

### 1.6 Progression (Fase 2)

- **XP** dan **Coins** adalah milik *profil perangkat* (bukan uang di dalam game).
- Sumber XP: menyelesaikan game (+100), menang (+100), beli property (+5/property),
  menyelesaikan misi harian, bonus main pertama hari ini (+50).
- Kalah tetap dapat XP → pemain tetap merasa maju.
- Kebutuhan XP naik level: `100 + (level × 50)` → Level 1→2 = 150 XP, Level 10→11 = 600 XP.
- Setiap level membuka 1 hadiah (token, skin dadu, tema board, atau coins).

### 1.7 Koleksi (Fase 2–3)

| Koleksi | Contoh | Cara dapat |
|---|---|---|
| Token | Kereta, Roket, Panda, Dinosaurus | Level up / beli dengan coins |
| Skin dadu | Klasik, Kayu, Neon, Emas, Permen | Level up / misi |
| Tema board | Classic City, Tropical, Night City, Railway, Fantasy | Level tinggi / fragmen koleksi |

### 1.8 Misi harian (Fase 2)

- 3 misi per hari, reset jam 00:00 waktu HP.
- Contoh: Lempar dadu 20×, Beli 3 property, Selesaikan 1 game, Kumpulkan 500 sewa.
- Semua bisa selesai secara alami dalam 1–2 game. **Tidak ada** timer tunggu atau energy.

### 1.9 Event (Fase 3, kartu Kesempatan)

Hanya muncul saat mendarat di tile Kesempatan (4 dari 32 tile), jadi tidak terlalu sering.

| Event | Efek |
|---|---|
| Hari Keberuntungan | +200 |
| Perbaikan Jalan | −50 |
| Festival Kota | Semua sewa +20% selama 1 ronde |
| Kereta Ekspres | Pindah ke stasiun terdekat di depan |
| Ulang Tahun | Terima 20 dari setiap pemain lain |
| Maju ke START | Pindah ke START, +200 |

### 1.10 Bot (Fase 3)

Bot memakai aturan yang sama dengan manusia (tidak curang, tidak tahu dadu berikutnya).

| Level | Logika beli property |
|---|---|
| Easy | Beli jika uang cukup, 80% kemungkinan; sisanya acak |
| Normal | Beli jika sisa uang setelah beli ≥ 200 |
| Hard | Prioritaskan melengkapi grup warna & stasiun; jaga cadangan sesuai sewa tertinggi di board |

### 1.11 Prinsip UX — aturan 3 detik

Dalam 3 detik pemain harus tahu: **giliran siapa → apa yang harus dilakukan → berapa uangnya**.
- Nama pemain aktif + warna token selalu tampil paling atas.
- Hanya **satu tombol utama** menyala pada satu waktu (ROLL, atau BELI/LEWATI, atau SELESAI).
- Tombol minimal tinggi 56dp, teks utama minimal 18sp.

### 1.12 Gaya visual

| Elemen | Nilai |
|---|---|
| Background | Off-white `#F6F7FB` |
| Primary | Navy `#1F3A68` |
| Accent | Orange `#FF9F43`, Yellow `#FFD166` |
| Teks | `#1E2330` (utama), `#6B7280` (sekunder) |
| Grup warna property | Coklat `#A47551`, Biru muda `#8ECAE6`, Pink `#F4A6C1`, Oranye `#F7B267`, Merah `#EF6F6C`, Hijau `#7BC47F` |
| Sudut | Radius 12–16dp |
| Bayangan | Lembut, 1 level saja |
| Font | Rounded modern, mis. **Nunito** atau **Baloo 2** (gratis, Google Fonts) |

Hindari: warna emas berlebihan, lampu kedip, suara koin ala kasino, efek "jackpot".

---

## 2. Screen Flow

```
[Splash / Loading]
        │
        ▼
[HOME] ◄──────────────────────────────────────────────┐
  │  Bottom nav: HOME · KOLEKSI · HADIAH · PROFIL      │
  │                                                    │
  ├── MAIN ─────────┐                                  │
  ├── MODE KELUARGA ┼──► [SETUP GAME]                  │
  └── VS BOT ───────┘     jumlah pemain, nama,         │
                          token, manusia/bot           │
                                │ MULAI                │
                                ▼                      │
                          [GAME / BOARD] ◄──┐          │
                           │    │           │          │
                           │    ├─ popup: Property / Event / Penjara
                           │    ├─ overlay: "OPER KE BUDI" (Pass & Play)
                           │    └─ [PAUSE] → Lanjut / Menyerah / Ke Home
                           │ game selesai   │          │
                           ▼                │          │
                          [HASIL GAME] ─ MAIN LAGI ────┘ (ke Setup dengan pemain sama)
                           │
                           └─ HOME ──────────────────────┘
```

Catatan:
- "MAIN" = shortcut Quick Play (1 manusia vs 1–3 bot dengan setting terakhir).
- "MODE KELUARGA" = semua pemain manusia (Pass & Play).
- "VS BOT" = pilih jumlah & level bot.
- Fase 1 cukup: Home (tanpa bottom nav) → Setup → Game → Hasil.

---

## 3. UI Hierarchy

### 3.1 Home

```
Home (Control, full screen)
├── Header
│   ├── Avatar + Nama profil
│   └── Level badge + XP bar          (Fase 2)
├── Logo game
├── Tombol MAIN            (besar, primary)
├── Tombol MODE KELUARGA   (besar, secondary)
├── Tombol VS BOT          (besar, secondary)
└── BottomNav              (Fase 2)
    ├── HOME  ├── KOLEKSI  ├── HADIAH  └── PROFIL
```

### 3.2 Game screen (portrait 9:16) — layar paling penting

```
Game (Control)
├── TopBar                     ~10% tinggi
│   ├── CurrentPlayerCard      token + NAMA BESAR + uang
│   └── PauseButton
├── BoardArea                  ~62% tinggi (persegi, lebar = lebar layar − 16dp)
│   ├── Board (32 TileView)
│   ├── Tokens (maks 4 TokenView)
│   └── BoardCenter
│       ├── Logo kecil
│       ├── DiceView (2 dadu)
│       └── Pesan status ("Andi membayar sewa 30 ke Budi")
├── PlayersStrip               ~10% tinggi
│   └── PlayerChip × 2–4       token, nama, uang, jumlah property; chip aktif disorot
└── ActionPanel                ~18% tinggi
    └── (berganti sesuai state) ROLL | BELI + LEWATI | BAYAR 50 + COBA DOUBLE | SELESAI

Overlay / Popup (di atas semuanya)
├── PropertyCard       nama, warna grup, harga, sewa, pemilik  [BELI] [LEWATI]
├── EventCard          judul, deskripsi, [OK]
├── PassDeviceOverlay  "OPER KE BUDI" + [SAYA BUDI, MULAI]
├── TileInfoPopup      muncul saat tile di-tap (lihat detail kapan saja)
└── PauseMenu          [LANJUT] [MENYERAH] [KE HOME]
```

Prinsip: tile kecil di board cukup menampilkan **pita warna + nama singkat + harga**.
Detail lengkap selalu tersedia lewat tap tile → TileInfoPopup.

### 3.3 Setup Game

```
Setup
├── Judul mode
├── Pilih jumlah pemain [2] [3] [4]
├── PlayerSlot × n
│   ├── Token picker
│   ├── Input nama
│   └── Toggle Manusia / Bot (+ level bot)   (Fase 3)
├── Opsi: Batas ronde [Tanpa batas | 20 | 30]
└── Tombol MULAI
```

### 3.4 Hasil Game

```
Result
├── "GAME SELESAI"
├── Pemenang (token besar + nama + 🏆)
├── Tabel peringkat (uang, property, sewa diterima)
├── Highlight: Kekayaan Tertinggi · Property Terbanyak · Sewa Terbesar · Lemparan Terbaik
├── Hadiah: +Coins, +XP, progress bar level     (Fase 2)
└── [MAIN LAGI]  [HOME]
```

---

## 4. Game Architecture

### 4.1 Prinsip

1. **Data terpisah dari tampilan.** Aturan game mengubah *state* (data). UI hanya membaca
   state dan menggambar ulang. Ini membuat bot, save, dan debugging jauh lebih mudah.
2. **Satu manager = satu tanggung jawab.** Tidak ada script raksasa.
3. **Komunikasi lewat signal.** Manager memancarkan signal, UI mendengarkan.
   Manager tidak memanggil node UI secara langsung.
4. **Data game dari JSON.** Harga, sewa, nama tile, event, reward → file JSON, bukan
   ditulis di dalam kode.

### 4.2 Dua jenis manager

**A. Autoload (singleton, hidup sepanjang aplikasi)** — didaftarkan di
*Project Settings → Globals → Autoload*:

| Autoload | Tugas | Fase |
|---|---|---|
| `GameData` | Membaca JSON (board, event, item koleksi) sekali saat start | 1 |
| `SceneRouter` | Pindah layar (Home → Setup → Game → Result), membawa parameter | 1 |
| `SaveManager` | Simpan/muat profil (`user://save.json`) | 2 |
| `ProgressionManager` | XP, level, coins, unlock, misi harian | 2 |

**B. Manager di dalam scene Game (hidup hanya selama 1 pertandingan)** — node anak dari
`Game.tscn`:

| Manager | Tugas |
|---|---|
| `GameManager` | "Sutradara": memulai game, menyambungkan semua manager, cek kondisi menang |
| `TurnManager` | State giliran: siapa aktif, fase giliran, double, pindah pemain |
| `PlayerManager` | Data pemain: uang, posisi, penjara, bangkrut, statistik |
| `BoardManager` | Data tile, hitung posisi baru, lewat START, posisi pixel tiap tile |
| `DiceManager` | Lempar 2 dadu (RNG), deteksi double, animasi dadu |
| `PropertyManager` | Kepemilikan, beli, hitung sewa, (Fase 3: upgrade) |
| `EventManager` | Ambil kartu event & terapkan efek (Fase 3) |
| `BotController` | Memutuskan aksi untuk pemain bot (Fase 3) |
| `UIManager` | Menghubungkan signal ke node UI: tampilkan popup, update HUD |

### 4.3 Alur satu giliran (siapa memanggil siapa)

```
UI: tombol ROLL ditekan
  → TurnManager.request_roll()
      → DiceManager.roll()                    ──signal── dice_rolled(d1, d2)
      → BoardManager.move(player, d1+d2)      ──signal── token_moved / passed_start
      → PlayerManager.add_money(+200) jika lewat START
      → TurnManager.resolve_tile(tile)
          ├─ property kosong  → signal buy_offer(tile)      → UI: PropertyCard
          ├─ property orang   → PropertyManager.pay_rent()  → signal rent_paid
          ├─ pajak            → PlayerManager.pay()
          ├─ kesempatan       → EventManager.draw()          → UI: EventCard
          └─ masuk penjara    → PlayerManager.send_to_jail()
      → PlayerManager.check_bankrupt()
      → GameManager.check_winner()
      → TurnManager.end_turn()  ──signal── turn_started(next_player)
                                          → UI: PassDeviceOverlay / HUD
```

### 4.4 State machine giliran (TurnManager)

```
WAIT_ROLL ──roll──► ROLLING ──► MOVING ──► RESOLVE_TILE ──► WAIT_DECISION
                                                                  │
      ▲                                                           ▼
      └──── (double) ◄───────────────────────────────────── END_TURN ──► GAME_OVER
                      (bukan double) → pemain berikutnya ──► WAIT_ROLL
```

Tombol UI hanya aktif di state yang sesuai (ROLL hanya di `WAIT_ROLL`), sehingga pemain
tidak bisa menekan dua kali saat animasi.

---

## 5. Scene Structure (Godot)

```
scenes/
├── main/Main.tscn                 # Scene awal; hanya memuat Home via SceneRouter
├── home/Home.tscn
├── setup/Setup.tscn
│   └── PlayerSlot.tscn            # dipakai ulang 2–4 kali
├── game/Game.tscn
│   ├── Managers (Node)
│   │   ├── GameManager
│   │   ├── TurnManager
│   │   ├── PlayerManager
│   │   ├── BoardManager
│   │   ├── DiceManager
│   │   ├── PropertyManager
│   │   ├── EventManager           (Fase 3)
│   │   └── BotController          (Fase 3)
│   └── UI (CanvasLayer) — UIManager.gd
│       ├── TopBar
│       ├── BoardView.tscn
│       │   ├── TileView.tscn × 32 (dibuat otomatis dari JSON)
│       │   └── TokenView.tscn × 2–4
│       ├── DiceView.tscn
│       ├── PlayersStrip → PlayerChip.tscn × 2–4
│       ├── ActionPanel
│       └── Popups: PropertyCard.tscn, EventCard.tscn,
│                   PassDeviceOverlay.tscn, PauseMenu.tscn, TileInfoPopup.tscn
├── result/Result.tscn
└── collection/Collection.tscn     (Fase 2)
```

Board dibangun **otomatis** oleh `BoardView.gd` dari `board_classic.json`, jadi Anda tidak
perlu menaruh 32 tile secara manual di editor.

---

## 6. Folder Structure (proyek Godot)

```
monopoly-family/                  # root proyek Godot (berisi project.godot)
├── project.godot
├── assets/
│   ├── fonts/                    Nunito-Regular.ttf, Nunito-Bold.ttf
│   ├── images/
│   │   ├── tokens/               cat.png, dog.png, car.png, ...
│   │   ├── dice/                 classic/1.png ... 6.png
│   │   ├── tiles/                icon stasiun, listrik, air, pajak, kesempatan
│   │   └── ui/                   ikon tombol, logo
│   ├── audio/
│   │   ├── sfx/                  dice.ogg, step.ogg, buy.ogg, pay.ogg
│   │   └── music/
│   └── themes/
│       └── main_theme.tres       Theme Godot: warna, font, style tombol
├── data/
│   ├── board_classic.json        32 tile
│   ├── events.json               kartu Kesempatan        (Fase 3)
│   ├── tokens.json               daftar token + syarat unlock  (Fase 2)
│   ├── dice_skins.json                                   (Fase 2)
│   ├── missions.json                                     (Fase 2)
│   └── config.json               uang awal, bonus START, dll.
├── scripts/
│   ├── autoload/                 game_data.gd, scene_router.gd, save_manager.gd, progression_manager.gd
│   ├── managers/                 game_manager.gd, turn_manager.gd, player_manager.gd,
│   │                             board_manager.gd, dice_manager.gd, property_manager.gd,
│   │                             event_manager.gd, bot_controller.gd
│   ├── models/                   player_state.gd, tile_state.gd  (class data sederhana)
│   └── ui/                       ui_manager.gd, board_view.gd, tile_view.gd, token_view.gd,
│                                 dice_view.gd, player_chip.gd, property_card.gd, ...
├── scenes/                       (lihat bagian 5)
└── docs/
    └── TAHAP-1-DESAIN.md         dokumen ini
```

Konvensi penamaan (standar GDScript): file & folder `snake_case`, scene `PascalCase.tscn`,
class `PascalCase`, fungsi & variabel `snake_case`, signal bentuk lampau (`dice_rolled`).

---

## 7. Data Structure

### 7.1 `data/config.json`

```json
{
  "starting_money": 1500,
  "pass_start_bonus": 200,
  "jail_fine": 50,
  "max_jail_turns": 3,
  "max_doubles_before_jail": 3,
  "monopoly_rent_multiplier": 2,
  "station_rent": [25, 50, 100, 200],
  "utility_multiplier": [4, 10]
}
```

### 7.2 `data/board_classic.json` (satu tile)

```json
{
  "index": 1,
  "type": "property",
  "name": "Jl. Melati",
  "short_name": "Melati",
  "group": "brown",
  "price": 60,
  "rent": 4,
  "upgrade_cost": 50,
  "rent_levels": [4, 20, 60, 180]
}
```

Nilai `type`: `start`, `property`, `station`, `utility`, `event`, `tax`, `jail`,
`free_parking`, `go_to_jail`. Field yang tidak relevan untuk suatu tipe cukup dihilangkan.
`rent_levels` baru dipakai di Fase 3 (upgrade). File lengkap: `data/board_classic.json`.

### 7.3 State pemain saat game (di memori, `PlayerState`)

```gdscript
class_name PlayerState
var id: int
var name: String
var token_id: String          # "cat", "dog", ...
var is_bot: bool = false
var bot_level: String = ""    # "easy" | "normal" | "hard"
var money: int = 1500
var position: int = 0
var in_jail: bool = false
var jail_turns: int = 0
var is_bankrupt: bool = false
var stats := {                # untuk layar hasil
    "rolls": 0, "best_roll": 0, "rent_paid": 0,
    "rent_earned": 0, "biggest_rent": 0, "properties_bought": 0
}
```

### 7.4 State tile saat game (`TileState`)

```gdscript
class_name TileState
var index: int
var owner_id: int = -1        # -1 = milik bank
var level: int = 0            # 0 = belum di-upgrade (Fase 3)
```

Data statis (nama, harga) tetap di `GameData`; `TileState` hanya menyimpan yang **berubah**.

### 7.5 Profil tersimpan (`user://save.json`, Fase 2)

```json
{
  "version": 1,
  "profile_name": "Andi",
  "level": 3,
  "xp": 120,
  "coins": 450,
  "unlocked": {
    "tokens": ["cat", "dog", "car", "panda"],
    "dice": ["classic"],
    "boards": ["classic_city"]
  },
  "selected": { "token": "panda", "dice": "classic", "board": "classic_city" },
  "daily": {
    "date": "2026-09-25",
    "missions": [
      { "id": "roll_20", "progress": 12, "target": 20, "claimed": false }
    ]
  },
  "records": { "games_played": 14, "wins": 5, "highest_wealth": 4200, "best_roll": 12 }
}
```

`version` penting agar save lama tetap bisa dibaca ketika struktur berubah.

---

## 8. MVP Development Roadmap

Setiap langkah menghasilkan sesuatu yang **bisa dijalankan dan dicoba**. Jangan lanjut
sebelum langkah sebelumnya berjalan.

### Fase 1 — Prototype yang bisa dimainkan (target 2–4 minggu)

| # | Langkah | Hasil yang bisa dicek |
|---|---|---|
| 1.1 | Setup proyek Godot, orientasi portrait, Theme (warna + font) | Layar kosong dengan warna & font benar |
| 1.2 | Autoload `GameData` + `SceneRouter`, layar Home & Setup sederhana | Bisa pindah Home → Setup → Game kosong |
| 1.3 | `BoardView` menggambar 32 tile dari JSON | Board tampil rapi di HP & tablet |
| 1.4 | `DiceManager` + `DiceView` | Tombol ROLL → dadu beranimasi, angka acak |
| 1.5 | `PlayerManager` + `TokenView`, gerak per petak | Token jalan sesuai dadu, +200 lewat START |
| 1.6 | `TurnManager` (state machine) + TopBar + PlayersStrip | Giliran berganti 2–4 pemain |
| 1.7 | `PropertyManager`: beli & sewa + PropertyCard | Bisa beli, bayar sewa, sewa ×2 grup lengkap |
| 1.8 | Pajak, stasiun, utilitas, penjara, masuk penjara | Semua tile punya efek |
| 1.9 | Bangkrut + pemenang + layar Hasil + Main Lagi | Satu game penuh bisa selesai |
| 1.10 | PassDeviceOverlay, PauseMenu, SFX dasar | Siap dicoba bersama keluarga |
| 1.11 | Export APK & uji di HP asli | APK terpasang di Android |

### Fase 2 — Alasan untuk kembali bermain

SaveManager → Coins & XP → Level + progress bar → Hadiah level → Koleksi token →
Koleksi dadu → Misi harian → Bottom navigation (Koleksi / Hadiah / Profil).

### Fase 3 — Kedalaman gameplay

Bot (Easy → Normal → Hard) → Upgrade property → Kartu event → Jual/gadai sebelum bangkrut →
Trade antar pemain → Achievement → Tema board tambahan.

### Fase 4 — Setelah game stabil

Online multiplayer, cloud save, konten musiman, kota & kosmetik tambahan.

### Strategi pengujian sederhana

- Tambahkan tombol **DEBUG** (hanya di build debug): atur hasil dadu, tambah uang, lompat ke tile.
- Setelah tiap langkah, mainkan 1 game penuh 2 pemain.
- Uji di minimal 2 ukuran layar: HP kecil (360×800) dan tablet (800×1280).
