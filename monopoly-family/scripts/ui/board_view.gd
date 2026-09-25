extends Control
## Menggambar seluruh board dari data JSON (GameData.tiles).
##
## Board berbentuk persegi. Petak 0 (START) di pojok kanan bawah, lalu berjalan
## searah jarum jam: sisi bawah (kanan -> kiri), kiri (bawah -> atas),
## atas (kiri -> kanan), kanan (atas -> bawah).
## Jumlah petak bebas asalkan kelipatan 4 (32, 40, ...), layout menyesuaikan otomatis.

signal tile_pressed(index: int)

const TileView := preload("res://scripts/ui/tile_view.gd")

## Petak sudut lebih besar dari petak biasa (1,4x), seperti board klasik.
const CORNER_RATIO := 1.4

var tile_views: Array = []
var selected_index: int = -1

var _center_panel: PanelContainer
var _center_title: Label
var _center_body: Label


func _ready() -> void:
	_build_center()
	_build_tiles()
	_layout()


func _notification(what: int) -> void:
	if what == NOTIFICATION_RESIZED and tile_views.size() > 0:
		_layout()


# ---------- Membangun node ----------

func _build_tiles() -> void:
	var per_side := GameData.get_board_size() / 4
	for i in GameData.get_board_size():
		var tile := TileView.new()
		tile.name = "Tile%02d" % i
		tile.setup(i, GameData.get_tile(i), i / per_side, i % per_side == 0)
		tile.pressed.connect(_on_tile_pressed)
		add_child(tile)
		tile_views.append(tile)


func _build_center() -> void:
	_center_panel = PanelContainer.new()
	_center_panel.theme_type_variation = &"CardPanel"
	_center_panel.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(_center_panel)
	# Saat teks berubah, panel bisa ikut membesar. Kembalikan ke ukuran semestinya.
	_center_panel.minimum_size_changed.connect(_layout_center, CONNECT_DEFERRED)

	var box := VBoxContainer.new()
	box.alignment = BoxContainer.ALIGNMENT_CENTER
	box.add_theme_constant_override("separation", 12)
	_center_panel.add_child(box)

	_center_title = Label.new()
	_center_title.theme_type_variation = &"TitleLabel"
	_center_title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_center_title.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	box.add_child(_center_title)

	_center_body = Label.new()
	_center_body.theme_type_variation = &"MutedLabel"
	_center_body.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_center_body.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	box.add_child(_center_body)

	show_center_message("MONOPOLY\nFAMILY", "Tap petak untuk melihat detail")


## Tulis pesan di tengah board (judul besar + keterangan).
func show_center_message(title: String, body: String) -> void:
	_center_title.text = title
	_center_body.text = body


# ---------- Posisi & ukuran ----------

func _board_length() -> float:
	return minf(size.x, size.y)


func _corner_size() -> float:
	var per_side := GameData.get_board_size() / 4
	return _board_length() * CORNER_RATIO / (per_side - 1 + 2 * CORNER_RATIO)


func _tile_width() -> float:
	var per_side := GameData.get_board_size() / 4
	return _board_length() / (per_side - 1 + 2 * CORNER_RATIO)


## Kotak (posisi + ukuran) petak ke-index, relatif terhadap BoardView.
func get_tile_rect(index: int) -> Rect2:
	var per_side := GameData.get_board_size() / 4
	var side := index / per_side
	var k := index % per_side   # 0 = petak sudut
	var length := _board_length()
	var c := _corner_size()
	var w := _tile_width()
	var far := length - c       # koordinat awal baris/kolom terjauh

	match side:
		0:  # bawah, dari kanan ke kiri
			return Rect2(far, far, c, c) if k == 0 else Rect2(far - k * w, far, w, c)
		1:  # kiri, dari bawah ke atas
			return Rect2(0, far, c, c) if k == 0 else Rect2(0, far - k * w, c, w)
		2:  # atas, dari kiri ke kanan
			return Rect2(0, 0, c, c) if k == 0 else Rect2(c + (k - 1) * w, 0, w, c)
		_:  # kanan, dari atas ke bawah
			return Rect2(far, 0, c, c) if k == 0 else Rect2(far, c + (k - 1) * w, c, w)


## Titik tengah petak. Dipakai di langkah 1.5 untuk menaruh token pemain.
func get_tile_center(index: int) -> Vector2:
	return get_tile_rect(index).get_center()


func _layout() -> void:
	for tile in tile_views:
		var rect := get_tile_rect(tile.index)
		tile.position = rect.position
		tile.size = rect.size
	_layout_center()


## Panel tengah mengisi ruang di dalam cincin petak.
func _layout_center() -> void:
	var c := _corner_size()
	var center_rect := Rect2(c, c, _board_length() - 2 * c, _board_length() - 2 * c).grow(-10)
	if center_rect.size.x <= 0:
		return
	_center_panel.position = center_rect.position
	_center_panel.size = center_rect.size
	# Label yang autowrap butuh lebar tetap, kalau tidak tingginya membengkak keluar board.
	var text_width := center_rect.size.x - 48
	_center_title.custom_minimum_size.x = text_width
	_center_body.custom_minimum_size.x = text_width
	_center_title.add_theme_font_size_override("font_size", int(center_rect.size.x * 0.09))
	_center_body.add_theme_font_size_override("font_size", int(center_rect.size.x * 0.055))


# ---------- Interaksi ----------

func _on_tile_pressed(index: int) -> void:
	select_tile(index)
	tile_pressed.emit(index)


## Sorot satu petak (garis oranye). -1 = hapus sorotan.
func select_tile(index: int) -> void:
	if selected_index >= 0:
		tile_views[selected_index].selected = false
	selected_index = index
	if index >= 0:
		tile_views[index].selected = true
