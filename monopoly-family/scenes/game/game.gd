extends Control
## Layar permainan.
## Langkah 1.1–1.3: menampilkan board, pemain aktif, dan daftar pemain.
## Manager (TurnManager, DiceManager, dst.) akan ditambahkan ke node "Managers" mulai langkah 1.4.

## Dipakai jika scene ini dijalankan langsung (F6) tanpa lewat layar Setup.
const TEST_PLAYERS := [
	{"name": "Andi", "token": "cat"},
	{"name": "Budi", "token": "dog"},
]

var players: Array = []          # [{name, token, money}]
var current_player_index: int = 0

@onready var board_view = %BoardView
@onready var turn_token_slot: CenterContainer = %TurnTokenSlot
@onready var turn_name: Label = %TurnName
@onready var turn_money: Label = %TurnMoney
@onready var players_strip: GridContainer = %PlayersStrip
@onready var menu_button: Button = %MenuButton


func _ready() -> void:
	var starting_money := int(GameData.config.get("starting_money", 1500))
	for p in SceneRouter.params.get("players", TEST_PLAYERS):
		players.append({"name": p.name, "token": p.token, "money": starting_money})

	board_view.tile_pressed.connect(_on_tile_pressed)
	menu_button.pressed.connect(SceneRouter.go_back)   # Fase 1.10: diganti PauseMenu
	_refresh_players()


# ---------- Tampilan pemain ----------

func _refresh_players() -> void:
	var current: Dictionary = players[current_player_index]
	var token := GameData.get_token(current.token)

	for child in turn_token_slot.get_children():
		child.queue_free()
	turn_token_slot.add_child(UIStyle.make_token_badge(token, 80))
	turn_name.text = current.name
	turn_money.text = GameData.format_money(current.money)

	for child in players_strip.get_children():
		child.queue_free()
	for i in players.size():
		players_strip.add_child(_make_player_chip(players[i], i == current_player_index))


## Kartu kecil: token + nama + uang. Pemain aktif diberi garis oranye.
func _make_player_chip(player: Dictionary, active: bool) -> Control:
	var chip := PanelContainer.new()
	chip.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var border := UIStyle.ORANGE if active else UIStyle.LINE
	var box := UIStyle.rounded_box(UIStyle.WHITE, 20, border, 4 if active else 2)
	box.set_content_margin_all(12)
	chip.add_theme_stylebox_override("panel", box)

	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", 10)
	chip.add_child(row)
	row.add_child(UIStyle.make_token_badge(GameData.get_token(player.token), 52))

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
