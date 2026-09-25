extends Control
## Layar utama. Fase 1: MAIN dan MODE KELUARGA sama-sama menuju Setup (semua pemain manusia).
## VS BOT diaktifkan di Fase 3.

@onready var group_colors: HBoxContainer = %GroupColors
@onready var play_button: Button = %PlayButton
@onready var family_button: Button = %FamilyButton


func _ready() -> void:
	play_button.pressed.connect(func(): SceneRouter.goto("setup", {"mode": "quick"}))
	family_button.pressed.connect(func(): SceneRouter.goto("setup", {"mode": "family"}))
	_build_group_colors()


## Deretan kotak warna grup property sebagai hiasan logo.
func _build_group_colors() -> void:
	for group in GameData.board.get("groups", {}):
		var swatch := Panel.new()
		swatch.custom_minimum_size = Vector2(44, 44)
		swatch.add_theme_stylebox_override("panel", UIStyle.rounded_box(GameData.get_group_color(group), 10))
		group_colors.add_child(swatch)
