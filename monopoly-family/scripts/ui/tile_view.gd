extends Control
## Tampilan SATU petak board. Dibuat otomatis oleh BoardView, bukan ditaruh manual di editor.
##
## Isi petak: latar, pita warna grup (property/stasiun/utilitas), nama singkat, harga/keterangan.
## Semua digambar langsung di _draw() agar teks selalu pas di dalam petak sekecil apa pun.
## Pita warna selalu menghadap ke tengah board, seperti board klasik.

signal pressed(index: int)

enum Side { BOTTOM, LEFT, TOP, RIGHT }

const STATION_COLOR := Color(0.1216, 0.2275, 0.4078)   # navy
const UTILITY_COLOR := Color(1.0, 0.8196, 0.4)         # kuning
const CORNER_COLORS := {
	"start": Color(1.0, 0.95, 0.82),
	"jail": Color(0.91, 0.92, 0.95),
	"free_parking": Color(0.88, 0.96, 0.89),
	"go_to_jail": Color(0.99, 0.89, 0.88),
}
const EVENT_BG := Color(1.0, 0.95, 0.89)
const TAX_BG := Color(0.95, 0.95, 0.97)

var index: int = 0
var data: Dictionary = {}
var side: int = Side.BOTTOM
var is_corner: bool = false
var selected: bool = false:
	set(value):
		selected = value
		queue_redraw()

var _name_text: String = ""
var _info_text: String = ""
var _name_color: Color = UIStyle.TEXT


func setup(tile_index: int, tile_data: Dictionary, tile_side: int, corner: bool) -> void:
	index = tile_index
	data = tile_data
	side = tile_side
	is_corner = corner
	mouse_filter = Control.MOUSE_FILTER_STOP
	_fill_text()


func _fill_text() -> void:
	var type: String = data.type
	match type:
		"property", "station", "utility":
			_name_text = data.short_name
			_info_text = GameData.format_money(int(data.price))
		"event":
			_name_text = "?"
			_name_color = UIStyle.ORANGE
			_info_text = "Kesempatan"
		"tax":
			_name_text = data.short_name
			_info_text = GameData.format_money(int(data.amount))
		"start":
			_name_text = "START"
			_info_text = "+" + GameData.format_money(int(GameData.config.get("pass_start_bonus", 200)))
		"jail":
			_name_text = "PENJARA"
			_info_text = "Mampir"
		"free_parking":
			_name_text = "PARKIR"
			_info_text = "Gratis"
		"go_to_jail":
			_name_text = "MASUK PENJARA"
			_info_text = ""
		_:
			_name_text = data.get("short_name", "")


## Warna pita di sisi dalam petak. Transparan = tanpa pita.
func get_band_color() -> Color:
	match String(data.type):
		"property":
			return GameData.get_group_color(data.group)
		"station":
			return STATION_COLOR
		"utility":
			return UTILITY_COLOR
	return Color.TRANSPARENT


func _band_rect(tile_rect: Rect2, thickness: float) -> Rect2:
	match side:
		Side.BOTTOM:  # pita di atas
			return Rect2(tile_rect.position, Vector2(tile_rect.size.x, thickness))
		Side.TOP:     # pita di bawah
			return Rect2(tile_rect.position.x, tile_rect.end.y - thickness, tile_rect.size.x, thickness)
		Side.LEFT:    # pita di kanan
			return Rect2(tile_rect.end.x - thickness, tile_rect.position.y, thickness, tile_rect.size.y)
		_:            # RIGHT: pita di kiri
			return Rect2(tile_rect.position, Vector2(thickness, tile_rect.size.y))


func _band_thickness() -> float:
	return minf(size.x, size.y) * 0.24


func _background_color() -> Color:
	var type: String = data.type
	if CORNER_COLORS.has(type):
		return CORNER_COLORS[type]
	if type == "event":
		return EVENT_BG
	if type == "tax":
		return TAX_BG
	return UIStyle.WHITE


func _draw() -> void:
	var gap := 2.0
	var tile_rect := Rect2(Vector2.ZERO, size).grow(-gap)
	var radius := int(minf(size.x, size.y) * 0.12)
	var border := UIStyle.ORANGE if selected else UIStyle.LINE
	draw_style_box(UIStyle.rounded_box(_background_color(), radius, border, 4 if selected else 1), tile_rect)

	var band_color := get_band_color()
	if band_color.a > 0.0:
		var band := _band_rect(tile_rect.grow(-4), _band_thickness())
		draw_style_box(UIStyle.rounded_box(band_color, maxi(radius - 2, 2)), band)

	_draw_texts()


## Area petak yang tidak tertutup pita, tempat teks digambar.
func _content_rect() -> Rect2:
	var content := Rect2(Vector2.ZERO, size).grow(-6)
	if get_band_color().a > 0.0:
		var band := _band_thickness() + 2
		match side:
			Side.BOTTOM: content = content.grow_side(SIDE_TOP, -band)
			Side.TOP: content = content.grow_side(SIDE_BOTTOM, -band)
			Side.LEFT: content = content.grow_side(SIDE_RIGHT, -band)
			Side.RIGHT: content = content.grow_side(SIDE_LEFT, -band)
	return content


## Gambar nama + keterangan di tengah area konten.
## Ukuran font dikecilkan otomatis sampai kata terpanjang muat di lebar petak.
func _draw_texts() -> void:
	var content := _content_rect()
	var regular: Font = get_theme_default_font()
	var short_edge := minf(size.x, size.y)
	var name_max := int(short_edge * (0.5 if data.type == "event" else 0.24))
	var info_max := int(short_edge * 0.19)

	var name_size := _fit_font_size(UIStyle.BOLD_FONT, _name_text, content.size.x, name_max)
	var info_size := mini(_fit_font_size(regular, _info_text, content.size.x, info_max), name_size)

	var name_para := _make_paragraph(UIStyle.BOLD_FONT, _name_text, name_size, content.size.x)
	var info_para := _make_paragraph(regular, _info_text, info_size, content.size.x)
	var total_height := name_para.get_size().y
	if _info_text != "":
		total_height += info_para.get_size().y

	var y := content.position.y + (content.size.y - total_height) / 2.0
	name_para.draw(get_canvas_item(), Vector2(content.position.x, y), _name_color)
	if _info_text != "":
		info_para.draw(get_canvas_item(), Vector2(content.position.x, y + name_para.get_size().y), UIStyle.MUTED)


func _make_paragraph(font: Font, text: String, font_size: int, width: float) -> TextParagraph:
	var para := TextParagraph.new()
	para.add_string(text, font, font_size)
	para.width = width
	para.alignment = HORIZONTAL_ALIGNMENT_CENTER
	para.break_flags = TextServer.BREAK_MANDATORY | TextServer.BREAK_WORD_BOUND
	para.max_lines_visible = 2
	return para


## Ukuran font terbesar (<= max_size) agar kata terpanjang tidak melebihi lebar petak.
func _fit_font_size(font: Font, text: String, width: float, max_size: int) -> int:
	var font_size := max_size
	while font_size > 9:
		var widest := 0.0
		for word in text.split(" "):
			widest = maxf(widest, font.get_string_size(word, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size).x)
		if widest <= width:
			break
		font_size -= 1
	return font_size


func _notification(what: int) -> void:
	if what == NOTIFICATION_RESIZED:
		queue_redraw()


func _gui_input(event: InputEvent) -> void:
	# Tap dihitung saat jari/mouse diangkat.
	if event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT and not event.pressed:
		pressed.emit(index)
		accept_event()
