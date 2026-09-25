extends PanelContainer
## Satu baris pengaturan pemain: token (tap untuk ganti) + nama.

signal token_pressed(slot)

var slot_index: int = 0
var token_id: String = ""

@onready var token_button: Button = %TokenButton
@onready var slot_label: Label = %SlotLabel
@onready var name_edit: LineEdit = %NameEdit


func _ready() -> void:
	token_button.pressed.connect(func(): token_pressed.emit(self))


func setup(index: int, player_name: String, new_token_id: String) -> void:
	slot_index = index
	name_edit.text = player_name
	set_token(new_token_id)


func set_token(new_token_id: String) -> void:
	token_id = new_token_id
	var token := GameData.get_token(token_id)
	var color := Color.html(token.color)
	token_button.text = String(token.name).left(1)
	token_button.tooltip_text = token.name
	for state in ["normal", "hover", "pressed", "hover_pressed"]:
		token_button.add_theme_stylebox_override(state, UIStyle.rounded_box(color, 52, UIStyle.WHITE, 4))
	slot_label.text = "Pemain %d  ·  %s" % [slot_index + 1, token.name]


## Nama kosong otomatis jadi "Pemain N".
func get_player_name() -> String:
	var typed := name_edit.text.strip_edges()
	return typed if typed != "" else "Pemain %d" % (slot_index + 1)
