extends Control
## Layar permainan.
##
## Tugas layar ini hanya TAMPILAN: tombol, label, board, animasi.
## Aturan dan urutan giliran ada di TurnManager (scripts/managers/turn_manager.gd).
## TurnManager meminta animasi lewat 3 fungsi "view" di bawah:
## animate_roll(), animate_move(), show_message().

const TurnManager := preload("res://scripts/managers/turn_manager.gd")

## Dipakai jika scene ini dijalankan langsung (F6) tanpa lewat layar Setup.
const TEST_PLAYERS := [
	{"name": "Andi", "token": "cat"},
	{"name": "Budi", "token": "dog"},
]

@onready var dice_manager = %DiceManager
@onready var player_manager = %PlayerManager
@onready var board_manager = %BoardManager
@onready var turn_manager: TurnManager = %TurnManager
@onready var board_view = %BoardView
@onready var turn_token_slot: CenterContainer = %TurnTokenSlot
@onready var turn_name: Label = %TurnName
@onready var turn_money: Label = %TurnMoney
@onready var players_strip: GridContainer = %PlayersStrip
@onready var menu_button: Button = %MenuButton
@onready var action_button: Button = %ActionButton


func _ready() -> void:
	var starting_money := int(GameData.config.get("starting_money", 1500))
	player_manager.setup(SceneRouter.params.get("players", TEST_PLAYERS), starting_money)
	for player in player_manager.players:
		board_view.add_token(player.id, GameData.get_token(player.token_id), player.position)

	board_view.tile_pressed.connect(_on_tile_pressed)
	player_manager.money_changed.connect(func(_player, _delta): _refresh_players())
	turn_manager.turn_started.connect(func(_player): _refresh_players())
	turn_manager.state_changed.connect(_on_turn_state_changed)
	action_button.pressed.connect(_on_action_pressed)
	menu_button.pressed.connect(SceneRouter.go_back)   # langkah 1.10: diganti PauseMenu

	turn_manager.setup(dice_manager, player_manager, board_manager, self)
	turn_manager.start_game()


# ---------- Tombol aksi ----------

## Satu tombol besar yang fungsinya berganti sesuai fase giliran.
func _on_action_pressed() -> void:
	match turn_manager.state:
		TurnManager.State.WAIT_ROLL:
			turn_manager.request_roll()
		TurnManager.State.WAIT_END:
			if turn_manager.can_roll_again:
				turn_manager.request_roll()
			else:
				turn_manager.request_end_turn()


func _on_turn_state_changed(state: TurnManager.State) -> void:
	var idle: bool = turn_manager.is_idle()
	action_button.disabled = not idle
	menu_button.disabled = not idle
	match state:
		TurnManager.State.WAIT_ROLL:
			action_button.text = "LEMPAR DADU"
		TurnManager.State.WAIT_END:
			action_button.text = "DOBEL! LEMPAR LAGI" if turn_manager.can_roll_again else "SELESAI GILIRAN"


## Dipanggil SceneRouter sebelum pindah layar (tombol MENU / Back Android).
func can_leave() -> bool:
	return turn_manager.is_idle()


# ---------- View: dipanggil oleh TurnManager ----------

func animate_roll(roll: Dictionary) -> void:
	await board_view.dice_view.play_roll(roll.die_1, roll.die_2)


func animate_move(player: PlayerState, path: Array[int]) -> void:
	await board_view.move_token(player.id, path)
	board_view.select_tile(path[-1])


func show_message(title: String, body: String) -> void:
	board_view.show_center_message(title, body)


# ---------- Tampilan pemain ----------

func _refresh_players() -> void:
	var current: PlayerState = player_manager.get_current()

	for child in turn_token_slot.get_children():
		child.queue_free()
	turn_token_slot.add_child(UIStyle.make_token_badge(GameData.get_token(current.token_id), 80))
	turn_name.text = current.name
	turn_money.text = GameData.format_money(current.money)
	board_view.set_active_token(current.id)

	for child in players_strip.get_children():
		child.queue_free()
	for player in player_manager.players:
		players_strip.add_child(_make_player_chip(player, player == current))


## Kartu kecil: token + nama + uang. Pemain aktif diberi garis oranye.
func _make_player_chip(player: PlayerState, active: bool) -> Control:
	var chip := PanelContainer.new()
	chip.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var border := UIStyle.ORANGE if active else UIStyle.LINE
	var box := UIStyle.rounded_box(UIStyle.WHITE, 20, border, 4 if active else 2)
	box.set_content_margin_all(12)
	chip.add_theme_stylebox_override("panel", box)

	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 10)
	chip.add_child(row)
	row.add_child(UIStyle.make_token_badge(GameData.get_token(player.token_id), 52))

	var info := VBoxContainer.new()
	info.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	info.add_theme_constant_override("separation", -6)
	row.add_child(info)

	var name_label := Label.new()
	name_label.text = player.name
	name_label.text_overrun_behavior = TextServer.OVERRUN_TRIM_ELLIPSIS
	name_label.clip_text = true
	name_label.add_theme_font_override("font", UIStyle.BOLD_FONT)
	name_label.add_theme_font_size_override("font_size", 26)
	info.add_child(name_label)

	var money_label := Label.new()
	money_label.text = GameData.format_money(player.money)
	money_label.theme_type_variation = &"MutedLabel"
	money_label.add_theme_font_size_override("font_size", 22)
	info.add_child(money_label)
	return chip


# ---------- Detail petak ----------

func _on_tile_pressed(index: int) -> void:
	var tile := GameData.get_tile(index)
	board_view.show_center_message(tile.name, _describe_tile(tile))


func _describe_tile(tile: Dictionary) -> String:
	var money := GameData.format_money
	match String(tile.type):
		"property":
			return "Harga %s\nSewa %s\n(×2 jika punya satu grup warna)" % [money.call(int(tile.price)), money.call(int(tile.rent))]
		"station":
			var rents: Array = GameData.config.get("station_rent", [25, 50, 100, 200])
			return "Harga %s\nSewa %s–%s\n(tergantung jumlah stasiun pemilik)" % [money.call(int(tile.price)), money.call(int(rents[0])), money.call(int(rents[-1]))]
		"utility":
			var mult: Array = GameData.config.get("utility_multiplier", [4, 10])
			return "Harga %s\nSewa = total dadu ×%d\n(×%d jika punya keduanya)" % [money.call(int(tile.price)), int(mult[0]), int(mult[1])]
		"event":
			return "Ambil kartu Kesempatan"
		"tax":
			return "Bayar %s" % money.call(int(tile.amount))
		"start":
			return "Lewat atau berhenti di sini: dapat %s" % money.call(int(GameData.config.get("pass_start_bonus", 200)))
		"jail":
			return "Hanya mampir, tidak terjadi apa-apa"
		"free_parking":
			return "Istirahat sejenak, tidak terjadi apa-apa"
		"go_to_jail":
			return "Langsung pindah ke Penjara"
	return ""
