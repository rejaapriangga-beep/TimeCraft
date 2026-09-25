extends Control
## Menggambar seluruh board dari data JSON (GameData.tiles).
##
## Board berbentuk persegi. Petak 0 (START) di pojok kanan bawah, lalu berjalan
## searah jarum jam: sisi bawah (kanan -> kiri), kiri (bawah -> atas),
## atas (kiri -> kanan), kanan (atas -> bawah).
## Jumlah petak bebas asalkan kelipatan 4 (32, 40, ...), layout menyesuaikan otomatis.

signal tile_pressed(index: int)

const TileView := preload("res://scripts/ui/tile_view.gd")
const TokenView := preload("res://scripts/ui/token_view.gd")
const DiceView := preload("res://scripts/ui/dice_view.gd")

## Petak sudut lebih besar dari petak biasa (1,4x), seperti board klasik.
const CORNER_RATIO := 1.4

## Lama token berpindah satu petak (detik).
const STEP_SECONDS := 0.16

var tile_views: Array = []
var selected_index: int = -1

var _center_panel: PanelContainer
var _center_title: Label
var _center_body: Label

## Dadu di tengah board. Dipakai oleh layar Game: await board_view.dice_view.play_roll(...)
var dice_view: DiceView

## Token pemain: player_id -> TokenView, dan player_id -> index petak tempat token berdiri.
var _tokens: Dictionary = {}
var _token_tiles: Dictionary = {}
var _tokens_layer: Control


func _ready() -> void:
	_build_center()
	_build_tiles()
	# Lapisan token dibuat terakhir agar selalu tergambar di atas petak dan panel tengah.
	_tokens_layer = Control.new()
	_tokens_layer.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(_tokens_layer)
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

	dice_view = DiceView.new()
	dice_view.mouse_filter = Control.MOUSE_FILTER_IGNORE
	box.add_child(dice_view)

	_center_body = Label.new()
	_center_body.theme_type_variation = &"MutedLabel"
	_center_body.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	_center_body.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	box.add_child(_center_body)

	show_center_message("MONOPOLY\nFAMILY", "Tekan LEMPAR DADU untuk mulai")


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
	_tokens_layer.size = size
	for player_id in _tokens:
		_tokens[player_id].set_diameter(_token_diameter())
	for tile_index in _token_tiles.values():
		_arrange_tile(tile_index, false)


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
	dice_view.die_size = center_rect.size.x * 0.19


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


# ---------- Token ----------

func _token_diameter() -> float:
	return _tile_width() * 0.56


## Taruh token pemain di sebuah petak (dipanggil sekali saat game mulai).
func add_token(player_id: int, token: Dictionary, tile_index: int) -> void:
	var token_view := TokenView.new()
	token_view.name = "Token%d" % player_id
	token_view.setup(player_id, token)
	token_view.set_diameter(_token_diameter())
	_tokens_layer.add_child(token_view)
	_tokens[player_id] = token_view
	_token_tiles[player_id] = tile_index
	_arrange_tile(tile_index, false)


## Beri cincin oranye pada token pemain yang sedang giliran.
func set_active_token(player_id: int) -> void:
	for id in _tokens:
		_tokens[id].active = id == player_id
	if _tokens.has(player_id):
		_tokens[player_id].move_to_front()


## Gerakkan token petak demi petak mengikuti path. Pakai dengan await.
func move_token(player_id: int, path: Array[int]) -> void:
	var token_view: Control = _tokens[player_id]
	var from_tile: int = _token_tiles[player_id]
	_token_tiles.erase(player_id)
	_arrange_tile(from_tile, true)   # token lain di petak asal merapat ke tengah
	token_view.move_to_front()

	for tile_index in path:
		var target := get_token_anchor(tile_index) - token_view.size / 2.0
		var tween := create_tween().set_parallel()
		tween.tween_property(token_view, "position", target, STEP_SECONDS).set_trans(Tween.TRANS_SINE)
		# efek melompat: membesar lalu kembali
		tween.tween_property(token_view, "scale", Vector2.ONE * 1.3, STEP_SECONDS / 2.0).set_trans(Tween.TRANS_SINE)
		tween.chain().tween_property(token_view, "scale", Vector2.ONE, STEP_SECONDS / 2.0).set_trans(Tween.TRANS_SINE)
		await tween.finished

	_token_tiles[player_id] = path[-1]
	_arrange_tile(path[-1], true)
	await create_tween().tween_interval(0.15).finished


## Titik tempat token berdiri: sedikit ke sisi luar petak, supaya nama petak tidak tertutup.
func get_token_anchor(tile_index: int) -> Vector2:
	var per_side := GameData.get_board_size() / 4
	var outward := _outward(tile_index)
	if tile_index % per_side == 0:
		# petak sudut: geser ke arah pojok luar board
		var next_outward: Vector2 = [Vector2.DOWN, Vector2.LEFT, Vector2.UP, Vector2.RIGHT][(tile_index / per_side + 3) % 4]
		outward = (outward + next_outward) * 0.75
	return get_tile_center(tile_index) + outward * _corner_size() * 0.24


## Arah "keluar board" dari sebuah petak: bawah, kiri, atas, atau kanan.
func _outward(tile_index: int) -> Vector2:
	var per_side := GameData.get_board_size() / 4
	return [Vector2.DOWN, Vector2.LEFT, Vector2.UP, Vector2.RIGHT][tile_index / per_side]


## Susun semua token di satu petak agar tidak saling menutupi.
func _arrange_tile(tile_index: int, animate: bool) -> void:
	var ids: Array = []
	for id in _token_tiles:
		if _token_tiles[id] == tile_index:
			ids.append(id)
	var offsets := _slot_offsets(ids.size(), _outward(tile_index))
	for i in ids.size():
		var token_view: Control = _tokens[ids[i]]
		var target := get_token_anchor(tile_index) + offsets[i] - token_view.size / 2.0
		if animate:
			create_tween().tween_property(token_view, "position", target, 0.12)
		else:
			token_view.position = target


## Posisi relatif tiap token terhadap titik berdirinya, tergantung jumlah token di petak itu.
## Token berjajar mengikuti arah sisi board; untuk 3–4 token dibuat 2 baris.
func _slot_offsets(count: int, outward: Vector2) -> Array[Vector2]:
	var d := _token_diameter() * 0.45
	var along := Vector2(outward.y, outward.x).abs() * d   # tegak lurus arah keluar
	match count:
		0, 1:
			return [Vector2.ZERO]
		2:
			return [-along, along]
		_:
			var inner := -outward * d * 1.6
			return [-along + inner, along + inner, -along, along]
