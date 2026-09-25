# Langkah 1.6 — Sistem Giliran (TurnManager)

Hasil langkah ini: setiap giliran punya fase yang jelas.

1. Layar menampilkan **"Giliran Budi"** → tombol **LEMPAR DADU**.
2. Dadu berputar, token berjalan, bonus START (seperti langkah 1.4–1.5).
3. Tombol berubah menjadi **SELESAI GILIRAN**. Pemain bisa melihat hasil dulu, tap petak
   untuk detail, lalu menyerahkan HP ke pemain berikutnya.
4. Dadu **dobel** → tombol menjadi **DOBEL! LEMPAR LAGI** (pemain yang sama main lagi).
5. **Dobel 3× berturut-turut** → token langsung terbang ke **Penjara**, tidak berjalan,
   dan giliran harus diakhiri.

Diuji di Godot 4.4.1 dengan dadu yang dipaksakan untuk setiap aturan di atas, ditambah
150 tekan tombol acak (posisi token dan uang selalu benar, ronde dihitung benar).

---

## File baru & yang berubah

| File | Status | Isi |
|---|---|---|
| `scripts/managers/turn_manager.gd` | baru | **TurnManager**: state machine giliran, aturan dobel, hitung ronde |
| `scripts/managers/board_manager.gd` | ubah | + `find_tile_index("jail")` |
| `scripts/managers/player_manager.gd` | ubah | + `send_to_jail()` |
| `scenes/game/Game.tscn` | ubah | + node `TurnManager`; tombol `RollButton` diganti nama jadi `ActionButton` |
| `scenes/game/game.gd` | ubah | Alur giliran dipindah ke TurnManager. Layar ini sekarang hanya tampilan |

---

## Cara kerjanya

**State machine** artinya giliran selalu berada di tepat satu "fase", dan hanya aksi
tertentu yang boleh dilakukan di fase itu:

```
WAIT_ROLL ──LEMPAR──► ROLLING ──► MOVING ──► RESOLVING ──► WAIT_END
    ▲                                                          │
    │              dobel: LEMPAR LAGI (pemain sama) ◄──────────┤
    └──────────── SELESAI GILIRAN: pemain berikutnya ◄─────────┘
```

| Fase | Artinya | Tombol |
|---|---|---|
| `WAIT_ROLL` | Menunggu pemain melempar | LEMPAR DADU |
| `ROLLING` | Animasi dadu | mati |
| `MOVING` | Token berjalan | mati |
| `RESOLVING` | Efek petak (beli/sewa di langkah 1.7) | mati |
| `WAIT_END` | Giliran selesai, menunggu konfirmasi | SELESAI GILIRAN / DOBEL! LEMPAR LAGI |

`request_roll()` dan `request_end_turn()` di TurnManager selalu memeriksa fase dulu. Jadi
walaupun tombol ditekan di waktu yang salah (atau nanti bot memanggilnya), tidak ada yang
rusak: permintaan itu diabaikan saja.

**Pembagian tugas**

```
game.gd (tampilan)                     TurnManager (aturan)
───────────────────                    ────────────────────
tombol ditekan  ─────────────────────► request_roll() / request_end_turn()
                                         │
animate_roll()  ◄──── await ─────────────┤  "putar dadu ini"
animate_move()  ◄──── await ─────────────┤  "jalankan token lewat petak ini"
show_message()  ◄────────────────────────┤  "tulis ini di tengah board"
                                         │
_on_turn_state_changed() ◄── signal ─────┘  state_changed → ubah label/aktifkan tombol
```

TurnManager tidak tahu ada tombol atau label. Keuntungannya: di Fase 3, **bot** cukup
memanggil `request_roll()` / `request_end_turn()` yang sama.

---

## Aturan sementara (akan diganti)

- **Keluar penjara:** pemain yang masuk penjara karena 3× dobel langsung bebas di awal
  giliran berikutnya (muncul pesan "Keluar dari Penjara"). Aturan lengkap (bayar $50 atau
  coba dobel, maksimal 3 giliran) dibuat di langkah **1.8**, di `TurnManager._start_turn()`.
- **Efek petak:** `TurnManager._resolve_tile()` masih kosong. Diisi di langkah **1.7**
  (beli & sewa) dan **1.8** (pajak, stasiun, utilitas, petak Masuk Penjara).

---

## Mengubah sendiri

| Ingin... | Ubah di |
|---|---|
| Masuk penjara setelah 2× dobel (bukan 3×) | `data/config.json` → `max_doubles_before_jail` |
| Teks tombol | `scenes/game/game.gd` → `_on_turn_state_changed()` |
| Pesan awal giliran | `scripts/managers/turn_manager.gd` → `_start_turn()` |

**Uji dobel:** tambahkan sementara `dice_manager.force_next(3, 3)` di `game.gd` sebelum
`turn_manager.request_roll()` di `_on_action_pressed()`. Hapus lagi setelah selesai.

---

## Checklist uji

- [ ] Awal game: tengah board "Giliran Andi", tombol LEMPAR DADU
- [ ] Setelah token berhenti: tombol SELESAI GILIRAN, pemain di bar atas **belum** berganti
- [ ] SELESAI GILIRAN → bar atas, kartu oranye, dan cincin token pindah ke pemain berikutnya
- [ ] Dadu dobel → tombol "DOBEL! LEMPAR LAGI", pemain yang sama melempar lagi
- [ ] 3× dobel berturut-turut → token terbang ke Penjara, tombol SELESAI GILIRAN, uang tidak bertambah
- [ ] Giliran berikutnya pemain itu: pesan "Keluar dari Penjara"
- [ ] Selama animasi, tombol aksi dan MENU mati
