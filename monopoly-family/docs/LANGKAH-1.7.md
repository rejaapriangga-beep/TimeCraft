# Langkah 1.7 — Beli & Sewa

Hasil langkah ini: ekonomi game mulai berjalan.

- Berhenti di petak **property, stasiun, atau utilitas yang belum ada pemiliknya** → muncul
  kartu dengan info harga, sewa, dan progres grup → pilih **BELI** atau **LEWATI**.
- Petak yang sudah dibeli ditandai: latar diwarnai tipis dan ada **garis warna token
  pemilik** di sisi luar petak.
- Berhenti di petak **milik pemain lain** → sewa dibayar otomatis ke pemiliknya.
- Kartu pemain menampilkan jumlah aset, mis. `$1.420 · 3 aset`.
- Tap petak → tengah board menampilkan pemilik dan sewa saat ini.

| Jenis petak | Sewa |
|---|---|
| Property | Sewa dasar; **×2** jika pemilik punya semua property satu grup warna |
| Stasiun | $25 / $50 / $100 / $200 tergantung jumlah stasiun pemilik |
| Utilitas | Total dadu **×4** (punya 1) atau **×10** (punya 2) |

Uang yang tidak cukup: kartu tidak muncul, tertulis "Uang tidak cukup untuk membeli".

> **Perubahan rencana:** sewa stasiun & utilitas semula dijadwalkan di langkah 1.8. Karena
> dibeli lewat kartu yang sama dan hitungannya hanya satu fungsi, sekarang dikerjakan di
> sini. Langkah 1.8 menjadi: pajak, petak Masuk Penjara, dan aturan keluar penjara.

Diuji di Godot 4.4.1: setiap aturan sewa di atas dengan dadu dipaksakan, uang tidak cukup,
kartu mengunci tombol MENU, dan 200 tekan acak (beli/lewati acak). Total uang semua pemain
selalu cocok: uang awal + bonus START − harga semua petak yang dibeli (sewa hanya berpindah
tangan, tidak ada uang yang hilang/muncul).

---

## File baru & yang berubah

| File | Status | Isi |
|---|---|---|
| `scripts/managers/property_manager.gd` | baru | **PropertyManager**: pemilik petak, beli, hitung sewa, bayar sewa |
| `scenes/game/PropertyCard.tscn` + `property_card.gd` | baru | Kartu BELI / LEWATI (latar gelap transparan + kartu di tengah) |
| `scripts/managers/turn_manager.gd` | ubah | + fase `WAIT_DECISION`; `_resolve_tile()` sekarang berisi beli/sewa |
| `scripts/managers/player_manager.gd` | ubah | + `transfer()` (pindah uang antar pemain) |
| `scripts/ui/tile_view.gd` | ubah | + `owner_color`: latar diwarnai + garis pemilik |
| `scripts/ui/board_view.gd` | ubah | + `set_tile_owner()` |
| `scenes/game/Game.tscn` + `game.gd` | ubah | + node PropertyManager & PropertyCard; fungsi view `ask_buy()`; detail pemilik saat tap petak; jumlah aset di kartu pemain |
| `scripts/autoload/game_data.gd` | ubah | + `get_group_name()` |
| `data/board_classic.json` | ubah | + `group_names` (Coklat, Biru Muda, ...) |
| `data/tokens.json` | ubah | **Warna token diganti** (lihat di bawah) |

**Kenapa warna token diganti?** Sebelumnya Kucing oranye, Anjing coklat, Mobil merah, yaitu
warna yang sama dengan pita grup property. Akibatnya "Jl. Mawar milik Andi" terlihat
seperti pita grup oranye. Sekarang warna token pekat (ungu, teal, magenta, biru, ...) dan
berbeda dari warna pastel grup, jadi tanda kepemilikan tidak tertukar.

---

## Cara kerjanya

```
TurnManager._resolve_tile()
  └─ petak bisa dimiliki?
       ├─ belum ada pemilik
       │    ├─ uang cukup → state WAIT_DECISION
       │    │               → await view.ask_buy()      (layar Game menampilkan PropertyCard)
       │    │               → BELI: PropertyManager.buy()   ──signal── property_bought
       │    │                                                  → BoardView.set_tile_owner()
       │    └─ uang kurang → pesan "Uang tidak cukup"
       ├─ milik sendiri → pesan "Ini milikmu sendiri"
       └─ milik orang lain → PropertyManager.pay_rent() → PlayerManager.transfer()
```

**Kartu yang "menunggu".** `PropertyCard.ask()` menampilkan kartu lalu `await decided`,
yaitu berhenti sampai salah satu tombol ditekan. Hasilnya (`true` = BELI) dikembalikan ke
TurnManager. Selama menunggu, state = `WAIT_DECISION`, jadi tombol MENU dan Back Android
ditahan.

**Uang minus.** Kalau sewa lebih besar dari uang pemain, uangnya boleh minus dulu
(mis. `-$40`). Aturan bangkrut dibuat di langkah 1.9.

---

## Mengubah sendiri

| Ingin... | Ubah di |
|---|---|
| Harga / sewa property | `data/board_classic.json` → `price`, `rent` |
| Sewa grup lengkap ×3 (bukan ×2) | `data/config.json` → `monopoly_rent_multiplier` |
| Tabel sewa stasiun | `data/config.json` → `station_rent` |
| Pengali utilitas | `data/config.json` → `utility_multiplier` |
| Warna token | `data/tokens.json` → `color` (hindari warna yang mirip warna grup) |
| Teks di kartu | `scenes/game/game.gd` → `_ownable_rows()` |

---

## Checklist uji

- [ ] Berhenti di property kosong → kartu muncul dengan warna grup, harga, sewa, "kamu punya X dari Y"
- [ ] BELI → uang berkurang, petak diberi garis warna token, kartu pemain "1 aset"
- [ ] LEWATI → tidak ada perubahan uang, petak tetap tanpa pemilik
- [ ] Pemain lain berhenti di situ → "Bayar sewa $X ke [nama]", uang kedua pemain berubah
- [ ] Punya 2 property coklat → sewa Jl. Mawar menjadi $12 (tap petak untuk mengecek)
- [ ] Stasiun: sewa naik sesuai jumlah stasiun pemilik
- [ ] Utilitas: sewa = total dadu × 4
- [ ] Uang kurang dari harga → tidak ada kartu, pesan "Uang tidak cukup"
- [ ] Selama kartu tampil, tombol MENU mati
