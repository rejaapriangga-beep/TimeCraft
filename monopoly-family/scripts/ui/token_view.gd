extends Control
## Bidak (token) satu pemain di atas board: lingkaran berwarna + huruf pertama nama token.
## Nanti bisa diganti gambar (assets/images/tokens/) tanpa mengubah BoardView.

var player_id: int = 0
var token: Dictionary = {}
## Pemain yang sedang giliran diberi cincin oranye.
var active: bool = false:
	set(value):
		active = value
		queue_redraw()


func setup(id: int, token_data: Dictionary) -> void:
	player_id = id
	token = token_data
	mouse_filter = Control.MOUSE_FILTER_IGNORE


func set_diameter(diameter: float) -> void:
	size = Vector2(diameter, diameter)
	pivot_offset = size / 2.0   # agar efek membesar/mengecil berpusat di tengah
	queue_redraw()


func _draw() -> void:
	var radius := size.x / 2.0
	var center := size / 2.0
	# bayangan lembut
	draw_circle(center + Vector2(0, radius * 0.12), radius * 0.96, Color(0.1176, 0.1373, 0.1882, 0.22))
	if active:
		draw_circle(center, radius, UIStyle.ORANGE)
	draw_circle(center, radius * (0.84 if active else 0.96), UIStyle.WHITE)
	draw_circle(center, radius * (0.72 if active else 0.82), Color.html(token.color))

	var letter := String(token.name).left(1)
	var font_size := int(radius * 0.95)
	var text_size := UIStyle.BOLD_FONT.get_string_size(letter, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size)
	var baseline := center + Vector2(-text_size.x / 2.0, UIStyle.BOLD_FONT.get_ascent(font_size) - text_size.y / 2.0)
	draw_string(UIStyle.BOLD_FONT, baseline, letter, HORIZONTAL_ALIGNMENT_LEFT, -1, font_size, UIStyle.WHITE)
