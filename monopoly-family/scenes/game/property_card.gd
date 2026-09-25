extends Control
## Kartu tawaran membeli petak (property, stasiun, utilitas) dengan tombol BELI / LEWATI.
##
## Pakai:  var beli: bool = await property_card.ask(player, tile_index, rows)
## rows = [["Harga", "$60"], ["Sewa", "$4"], ...] disiapkan oleh layar Game.

signal decided(buy: bool)

@onready var header: PanelContainer = %Header
@onready var type_label: Label = %TypeLabel
@onready var name_label: Label = %NameLabel
@onready var rows_grid: GridContainer = %Rows
@onready var money_label: Label = %MoneyLabel
@onready var skip_button: Button = %SkipButton
@onready var buy_button: Button = %BuyButton


func _ready() -> void:
	skip_button.pressed.connect(func(): decided.emit(false))
	buy_button.pressed.connect(func(): decided.emit(true))


## Tampilkan kartu, tunggu pemain memilih, sembunyikan lagi. true = BELI.
func ask(player: PlayerState, tile_index: int, rows: Array) -> bool:
	_fill(player, tile_index, rows)
	show()
	var wants_to_buy: bool = await decided
	hide()
	return wants_to_buy


func _fill(player: PlayerState, tile_index: int, rows: Array) -> void:
	var tile := GameData.get_tile(tile_index)
	var price := int(tile.price)

	# Header berwarna sesuai grup. Teks gelap di warna terang, teks putih di warna gelap.
	var color := _header_color(tile)
	var header_box := UIStyle.rounded_box(color, 18)
	header_box.set_content_margin_all(20)
	header.add_theme_stylebox_override("panel", header_box)
	var text_color := UIStyle.TEXT if color.get_luminance() > 0.55 else UIStyle.WHITE
	type_label.add_theme_color_override("font_color", text_color)
	name_label.add_theme_color_override("font_color", text_color)
	type_label.text = _type_text(tile)
	name_label.text = tile.name

	for child in rows_grid.get_children():
		child.queue_free()
	for row in rows:
		rows_grid.add_child(_make_label(row[0], UIStyle.MUTED, false))
		rows_grid.add_child(_make_label(row[1], UIStyle.TEXT, true))

	money_label.text = "Uang %s: %s  →  %s" % [player.name, GameData.format_money(player.money), GameData.format_money(player.money - price)]
	buy_button.text = "BELI %s" % GameData.format_money(price)


func _header_color(tile: Dictionary) -> Color:
	match String(tile.type):
		"property":
			return GameData.get_group_color(tile.group)
		"station":
			return UIStyle.NAVY
	return UIStyle.YELLOW


func _type_text(tile: Dictionary) -> String:
	match String(tile.type):
		"property":
			return "PROPERTY  ·  GRUP %s" % GameData.get_group_name(tile.group).to_upper()
		"station":
			return "STASIUN"
	return "UTILITAS"


## Kolom kiri: judul baris (tidak dipotong). Kolom kanan: nilai, boleh 2 baris.
func _make_label(text: String, color: Color, is_value: bool) -> Label:
	var label := Label.new()
	label.text = text
	label.add_theme_color_override("font_color", color)
	if is_value:
		label.add_theme_font_override("font", UIStyle.BOLD_FONT)
		label.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		# Label autowrap butuh lebar tetap, kalau tidak teksnya turun per huruf.
		label.custom_minimum_size.x = 340
		label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	return label
