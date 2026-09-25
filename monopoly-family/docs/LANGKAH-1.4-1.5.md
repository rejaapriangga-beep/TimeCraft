# Langkah 1.4–1.5 — Dadu dan Token Bergerak

Hasil langkah ini: tekan **LEMPAR DADU** → dua dadu berputar ±0,7 detik di tengah board →
token pemain melompat **petak demi petak** → melewati/berhenti di START dapat **+$200** →
giliran pindah ke pemain berikutnya.

Diuji di Godot 4.4.1: hasil dadu yang dipaksakan, bonus START, tombol ditekan 2× saat animasi,
dan 60 lemparan acak (posisi token selalu sama dengan data pemain, uang selalu benar).

---

## Cara mencoba

1. Buka proyek di Godot (lihat `LANGKAH-1.1-1.3.md` jika belum pernah).
2. Tekan **F5** → MODE KELUARGA → isi pemain → MULAI.
3. Tekan **LEMPAR DADU** berulang kali. Perhatikan:
   - token pemain yang sedang giliran punya **cincin oranye**,
   - dua token di petak yang sama berdiri berdampingan,
   - uang di kartu pemain bertambah $200 setiap melewati START.

---

## File baru & yang berubah

| File | Status | Isi |
|---|---|---|
| `scripts/models/player_state.gd` | baru | **PlayerState**: data satu pemain (nama, token, uang, posisi, statistik) |
| `scripts/managers/dice_manager.gd` | baru | **DiceManager**: lempar 2 dadu acak. Ada `force_next(3, 4)` untuk uji coba |
| `scripts/managers/board_manager.gd` | baru | **BoardManager**: hitung jalur langkah, cek lewat START |
| `scripts/managers/player_manager.gd` | baru | **PlayerManager**: daftar pemain, ubah uang/posisi, pemain berikutnya |
| `scripts/ui/dice_view.gd` | baru | Gambar 2 dadu + animasi lempar |
| `scripts/ui/token_view.gd` | baru | Gambar 1 token (lingkaran + huruf, cincin oranye saat giliran) |
| `scripts/ui/board_view.gd` | ubah | + dadu di tengah board, + lapisan token, + `move_token()` animasi per petak |
| `scenes/game/Game.tscn` | ubah | + node DiceManager, PlayerManager, BoardManager di bawah `Managers`; tombol LEMPAR DADU aktif |
| `scenes/game/game.gd` | ubah | Alur satu giliran di `_play_turn()`; `can_leave()` menahan MENU/Back selama animasi |
| `scripts/autoload/scene_router.gd` | ubah | Bertanya `can_leave()` ke layar aktif sebelum kembali |

Script yang **tidak** berubah: GameData, Home, Setup, TileView, theme.

---

## Cara kerjanya

```
Tombol LEMPAR DADU
  → game.gd _play_turn()
      1. DiceManager.roll()                 → {die_1, die_2, total}
      2. await DiceView.play_roll(...)      → animasi dadu (menunggu selesai)
      3. BoardManager.build_move_path()     → mis. dari 30 maju 4 = [31, 0, 1, 2]
      4. await BoardView.move_token(path)   → token melompat per petak
      5. PlayerManager.set_position()
      6. BoardManager.passes_start(path)?   → PlayerManager.add_money(+200)
      7. PlayerManager.next_player()        → kartu pemain & cincin token pindah
```

**Kenapa `await`?** `await` artinya "tunggu sampai ini selesai, baru lanjut ke baris
berikutnya". Jadi token baru berjalan setelah dadu berhenti, dan giliran baru berpindah
setelah token sampai. Selama itu `is_busy = true` dan tombol dimatikan, jadi menekan
tombol berkali-kali tidak membuat token jalan dua kali.

**Logika vs tampilan dipisah.** Manager (folder `scripts/managers/`) hanya mengurus angka:
dadu, posisi, uang. Tampilan (folder `scripts/ui/`) hanya menggambar dan beranimasi.
Contoh: `PlayerManager.add_money()` tidak menyentuh label uang; ia memancarkan signal
`money_changed`, lalu `game.gd` menggambar ulang kartu pemain. Pola ini nanti memudahkan bot
(Fase 3): bot cukup memanggil manager yang sama, tanpa perlu menekan tombol.

---

## Mengubah sendiri

| Ingin... | Ubah di |
|---|---|
| Token berjalan lebih cepat/lambat | `scripts/ui/board_view.gd` → `STEP_SECONDS` (default 0.16) |
| Dadu berputar lebih lama | `scripts/ui/dice_view.gd` → `ROLL_SECONDS` (default 0.7) |
| Bonus START | `data/config.json` → `pass_start_bonus` |
| Warna titik dadu | `scripts/ui/dice_view.gd` → `UIStyle.NAVY` di `_draw_die()` |

**Uji dengan hasil dadu tertentu:** lihat `LANGKAH-1.6.md` (sejak langkah 1.6 alur giliran
pindah ke TurnManager).

---

## Yang sengaja belum ada (menyusul)

- **Dadu dobel main lagi** dan **3× dobel masuk penjara** → langkah 1.6 (TurnManager).
- Setelah token berhenti, giliran langsung pindah. Di langkah 1.6 ditambah fase
  "SELESAI GILIRAN" dan layar **OPER KE [nama]** (langkah 1.10).
- Petak belum punya efek (beli, sewa, pajak, penjara) → langkah 1.7–1.8.

---

## Checklist uji

- [ ] LEMPAR DADU → dadu berputar lalu berhenti; total di tengah board = jumlah titik kedua dadu
- [ ] Token berjalan per petak searah jarum jam (START → Melati → Kesempatan → Mawar → ...)
- [ ] Nama di bar atas, kartu pemain oranye, dan cincin token pindah ke pemain berikutnya
- [ ] Melewati START → uang +$200 dan muncul tulisan "Lewat START: +$200"
- [ ] Menekan tombol berkali-kali saat animasi tidak membuat token jalan 2×
- [ ] Selama animasi, tombol MENU (dan Back Android) tidak bereaksi; setelah token berhenti, berfungsi lagi
- [ ] Dua pemain berhenti di petak yang sama → token berdampingan, tidak saling menutupi
