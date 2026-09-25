extends Node
## TurnManager: mengatur urutan giliran dan fase di dalam satu giliran (state machine).
##
##   WAIT_ROLL ──LEMPAR──► ROLLING ──► MOVING ──► RESOLVING ──► WAIT_END
##       ▲                                                        │
##       │            dadu dobel: LEMPAR LAGI (pemain sama) ◄──────┤
##       └───────────── SELESAI GILIRAN: pemain berikutnya ◄───────┘
##
## Di dalam RESOLVING, giliran bisa berhenti di WAIT_DECISION (menunggu BELI / LEWATI).
##
## TurnManager tidak menyentuh tombol/label. Untuk animasi dan pesan, ia meminta
## "view" (layar Game) lewat 4 fungsi: animate_roll(), animate_move(), show_message(),
## dan ask_buy() (menampilkan kartu BELI / LEWATI lalu menunggu jawaban).
## Layar Game yang memutuskan bagaimana semua itu digambar.

signal state_changed(state: State)
signal turn_started(player: PlayerState)

enum State { WAIT_ROLL, ROLLING, MOVING, RESOLVING, WAIT_DECISION, WAIT_END, GAME_OVER }

var state: State = State.WAIT_ROLL
var doubles_in_row: int = 0       # berapa kali dobel berturut-turut di giliran ini
var can_roll_again: bool = false  # true setelah dadu dobel
var round_number: int = 1         # ronde ke-berapa (dipakai batas ronde di langkah 1.9)

var dice_manager: Node
var player_manager: Node
var board_manager: Node
var property_manager: Node
var view: Node


func setup(dice: Node, players: Node, board: Node, properties: Node, game_view: Node) -> void:
	dice_manager = dice
	player_manager = players
	board_manager = board
	property_manager = properties
	view = game_view


func start_game() -> void:
	round_number = 1
	_start_turn()


# ---------- Permintaan dari tombol / bot ----------

## LEMPAR DADU (awal giliran) atau LEMPAR LAGI (setelah dobel).
func request_roll() -> void:
	var allowed := state == State.WAIT_ROLL or (state == State.WAIT_END and can_roll_again)
	if not allowed:
		return
	can_roll_again = false
	await _roll_and_move()


## SELESAI GILIRAN. Tidak bisa selama masih punya lemparan dobel.
func request_end_turn() -> void:
	if state != State.WAIT_END or can_roll_again:
		return
	_next_turn()


## True jika sedang tidak ada animasi (boleh buka menu / keluar).
func is_idle() -> bool:
	return state in [State.WAIT_ROLL, State.WAIT_END, State.GAME_OVER]


# ---------- Alur giliran ----------

func _start_turn() -> void:
	doubles_in_row = 0
	can_roll_again = false
	var player: PlayerState = player_manager.get_current()
	var hint := "Tekan LEMPAR DADU"
	if player.in_jail:
		# SEMENTARA sampai langkah 1.8 (bayar denda / coba dobel): langsung bebas.
		player.in_jail = false
		hint = "Keluar dari Penjara. Tekan LEMPAR DADU"
	_set_state(State.WAIT_ROLL)
	turn_started.emit(player)
	view.show_message("Giliran %s" % player.name, hint)


func _next_turn() -> void:
	var previous_index: int = player_manager.current_index
	player_manager.next_player()
	if player_manager.current_index <= previous_index:
		round_number += 1   # urutan kembali ke pemain pertama = ronde baru
	_start_turn()


func _roll_and_move() -> void:
	var player: PlayerState = player_manager.get_current()

	# 1. Lempar dadu
	_set_state(State.ROLLING)
	var roll: Dictionary = dice_manager.roll()
	player.stats.rolls += 1
	player.stats.best_roll = maxi(player.stats.best_roll, roll.total)
	view.show_message(player.name, "Melempar dadu...")
	await view.animate_roll(roll)

	# 2. Dobel 3x berturut-turut -> langsung ke Penjara, tanpa berjalan
	if roll.is_double:
		doubles_in_row += 1
	var max_doubles := int(GameData.config.get("max_doubles_before_jail", 3))
	if roll.is_double and doubles_in_row >= max_doubles:
		view.show_message("Dobel %d kali!" % doubles_in_row, "%s langsung masuk Penjara" % player.name)
		_set_state(State.MOVING)
		await _send_to_jail(player)
		_set_state(State.WAIT_END)
		return

	# 3. Berjalan petak demi petak
	var roll_text := "%d + %d = %d langkah" % [roll.die_1, roll.die_2, roll.total]
	if roll.is_double:
		roll_text += "  ·  DOBEL!"
	view.show_message(player.name, roll_text)
	_set_state(State.MOVING)
	var path: Array[int] = board_manager.build_move_path(player.position, roll.total)
	await view.animate_move(player, path)
	player_manager.set_position(player, path[-1])

	# 4. Efek petak
	_set_state(State.RESOLVING)
	var notes: Array[String] = ["%s berhenti di sini" % player.name]
	if board_manager.passes_start(path):
		var bonus := int(GameData.config.get("pass_start_bonus", 200))
		player_manager.add_money(player, bonus)
		player.stats.passed_start += 1
		notes.append("Lewat START: +%s" % GameData.format_money(bonus))
	await _resolve_tile(player, notes, roll.total)

	# 5. Dobel = boleh lempar lagi (kecuali baru saja masuk penjara)
	can_roll_again = roll.is_double and not player.in_jail
	if can_roll_again:
		notes.append("Dobel! Lempar sekali lagi")
	view.show_message(GameData.get_tile(player.position).name, "\n".join(notes))
	_set_state(State.WAIT_END)


## Efek petak tempat pemain berhenti. Hasilnya ditambahkan ke notes (pesan di tengah board).
## Langkah 1.7: beli & sewa (property, stasiun, utilitas).
## Langkah 1.8: pajak, petak Masuk Penjara.
func _resolve_tile(player: PlayerState, notes: Array[String], dice_total: int) -> void:
	var tile_index := player.position
	if property_manager.is_ownable(tile_index):
		await _resolve_ownable(player, tile_index, notes, dice_total)


func _resolve_ownable(player: PlayerState, tile_index: int, notes: Array[String], dice_total: int) -> void:
	var owner_id: int = property_manager.get_owner_id(tile_index)
	var price: int = property_manager.get_price(tile_index)

	if owner_id < 0:
		# Belum ada pemilik: tawarkan untuk dibeli
		if not property_manager.can_afford(player, tile_index):
			notes.append("Uang tidak cukup untuk membeli (%s)" % GameData.format_money(price))
			return
		_set_state(State.WAIT_DECISION)
		var wants_to_buy: bool = await view.ask_buy(player, tile_index)
		_set_state(State.RESOLVING)
		if wants_to_buy:
			property_manager.buy(player, tile_index)
			notes.append("Dibeli seharga %s" % GameData.format_money(price))
		else:
			notes.append("Tidak dibeli")
	elif owner_id == player.id:
		notes.append("Ini milikmu sendiri")
	else:
		var amount: int = property_manager.pay_rent(player, tile_index, dice_total)
		var receiver: PlayerState = player_manager.players[owner_id]
		notes.append("Bayar sewa %s ke %s" % [GameData.format_money(amount), receiver.name])


func _send_to_jail(player: PlayerState) -> void:
	var jail_index: int = board_manager.find_tile_index("jail")
	await view.animate_move(player, [jail_index] as Array[int])
	player_manager.send_to_jail(player, jail_index)


func _set_state(new_state: State) -> void:
	state = new_state
	state_changed.emit(state)
