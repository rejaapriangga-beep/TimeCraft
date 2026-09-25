extends Control
## Layar permainan.
## Langkah 1.4–1.5: lempar dadu, token berjalan per petak, +bonus saat melewati START.
##
## Alur satu giliran untuk sementara diatur di _play_turn() di bawah.
## Di langkah 1.6 alur ini dipindah ke TurnManager (dengan aturan dadu dobel dan state giliran).

## Dipakai jika scene ini dijalankan langsung (F6) tanpa lewat layar Setup.
const TEST_PLAYERS := [
	{"name": "Andi", "token": "cat"},
	{"name": "Budi", "token": "dog"},
]

var is_busy: bool = false   # true selama animasi dadu/token berjalan

@onready var dice_manager = %DiceManager
@onready var player_manager = %PlayerManager
@onready var board_manager = %BoardManager
@onready var board_view = %BoardView
@onready var turn_token_slot: CenterContainer = %TurnTokenSlot
@onready var turn_name: Label = %TurnName
@onready var turn_money: Label = %TurnMoney
@onready var players_strip: GridContainer = %PlayersStrip
@onready var menu_button: Button = %MenuButton
@onready var roll_button: Button = %RollButton


func _ready() -> void:
	var starting_money := int(GameData.config.get("starting_money", 1500))
	player_manager.setup(SceneRouter.params.get("players", TEST_PLAYERS), starting_money)
	for player in player_manager.players:
		board_view.add_token(player.id, GameData.get_token(player.token_id), player.position)

	board_view.tile_pressed.connect(_on_tile_pressed)
	player_manager.money_changed.connect(func(_player, _delta): _refresh_players())
	player_manager.current_player_changed.connect(func(_player): _refresh_players())
	roll_button.pressed.connect(_play_turn)
	menu_button.pressed.connect(SceneRouter.go_back)   # langkah 1.10: diganti PauseMenu
	_refresh_players()


# ---------- Alur giliran (sementara, sebelum TurnManager) ----------

func _play_turn() -> void:
	if is_busy:
		return
	_set_busy(true)
	var player: PlayerState = player_manager.get_current()

	# 1. Lempar dadu + animasi
	var roll: Dictionary = dice_manager.roll()
	board_view.show_center_message(player.name, "Melempar dadu...")
	await board_view.dice_view.play_roll(roll.die_1, roll.die_2)
	player.stats.rolls += 1
	player.stats.best_roll = maxi(player.stats.best_roll, roll.total)
	board_view.show_center_message(player.name, "%d + %d = %d langkah" % [roll.die_1, roll.die_2, roll.total])

	# 2. Token berjalan petak demi petak
	var path: Array[int] = board_manager.build_move_path(player.position, roll.total)
	await board_view.move_token(player.id, path)
	player_manager.set_position(player, path[-1])

	# 3. Bonus melewati / berhenti di START
	var notes: Array[String] = []
	if board_manager.passes_start(path):
		var bonus := int(GameData.config.get("pass_start_bonus", 200))
		player_manager.add_money(player, bonus)
		player.stats.passed_start += 1
		notes.append("Lewat START: +%s" % GameData.format_money(bonus))

	# 4. Tampilkan petak tujuan. (Beli/sewa/pajak menyusul di langkah 1.7–1.8.)
	var tile := GameData.get_tile(player.position)
	notes.push_front("%s berhenti di sini" % player.name)
	board_view.show_center_message(tile.name, "\n".join(notes))
	board_view.select_tile(player.position)

	# 5. Giliran pemain berikutnya
	player_manager.next_player()
	_set_busy(false)


## Selama animasi: tombol dimatikan dan layar tidak boleh ditinggalkan.
func _set_busy(busy: bool) -> void:
	is_busy = busy
	roll_button.disabled = busy
	menu_button.disabled = busy


## Dipanggil SceneRouter sebelum pindah layar (tombol MENU / Back Android).
func can_leave() -> bool:
	return not is_busy


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
