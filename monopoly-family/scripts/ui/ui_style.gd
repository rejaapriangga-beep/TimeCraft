class_name UIStyle
extends RefCounted
## Warna dan helper tampilan yang dipakai di banyak layar.
## Warna yang sama juga ada di assets/themes/main_theme.tres (untuk tombol, label, dll.).

const NAVY := Color(0.1216, 0.2275, 0.4078)       # #1F3A68
const ORANGE := Color(1.0, 0.6235, 0.2627)        # #FF9F43
const YELLOW := Color(1.0, 0.8196, 0.4)           # #FFD166
const BACKGROUND := Color(0.9647, 0.9686, 0.9843) # #F6F7FB
const TEXT := Color(0.1176, 0.1373, 0.1882)       # #1E2330
const MUTED := Color(0.4196, 0.4471, 0.502)       # #6B7280
const LINE := Color(0.8431, 0.8588, 0.8941)       # #D7DBE4
const WHITE := Color(1, 1, 1)

const BOLD_FONT := preload("res://assets/fonts/Nunito-ExtraBold.ttf")


static func rounded_box(color: Color, radius: int = 16, border_color: Color = Color.TRANSPARENT, border_width: int = 0) -> StyleBoxFlat:
	var box := StyleBoxFlat.new()
	box.bg_color = color
	box.set_corner_radius_all(radius)
	box.anti_aliasing = true
	if border_width > 0:
		box.border_color = border_color
		box.set_border_width_all(border_width)
	return box


## Lingkaran berwarna berisi huruf pertama nama token. Nanti diganti gambar token.
static func make_token_badge(token: Dictionary, diameter: int) -> Label:
	var badge := Label.new()
	badge.text = String(token.name).left(1)
	badge.custom_minimum_size = Vector2(diameter, diameter)
	badge.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	badge.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	badge.size_flags_vertical = Control.SIZE_SHRINK_CENTER
	badge.add_theme_font_override("font", BOLD_FONT)
	badge.add_theme_font_size_override("font_size", int(diameter * 0.5))
	badge.add_theme_color_override("font_color", WHITE)
	badge.add_theme_stylebox_override("normal", rounded_box(Color.html(token.color), diameter / 2, WHITE, 3))
	return badge
