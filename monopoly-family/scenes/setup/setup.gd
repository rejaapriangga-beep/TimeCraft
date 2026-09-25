extends Control
## Layar Setup: pilih jumlah pemain (2–4), nama, dan token. Lalu kirim daftar pemain ke layar Game.

const PLAYER_SLOT := preload("res://scenes/setup/PlayerSlot.tscn")
const MIN_PLAYERS := 2
const MAX_PLAYERS := 4

var player_count: int = 2
var count_buttons: Dictionary = {}   # jumlah -> Button

@onready var title: Label = %Title
@onready var count_row: HBoxContainer = %CountRow
@onready var slots: VBoxContainer = %Slots
@onready var back_button: Button = %BackButton
@onready var start_button: Button = %StartButton


func _ready() -> void:
	var mode: String = SceneRouter.params.get("mode", "family")
	title.text = "Mode Keluarga" if mode == "family" else "Main Cepat"
	back_button.pressed.connect(SceneRouter.go_back)
	start_button.pressed.connect(_on_start_pressed)

	for count in range(MIN_PLAYERS, MAX_PLAYERS + 1):
		var button := Button.new()
		button.text = str(count)
		button.custom_minimum_size = Vector2(0, 96)
		button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		button.pressed.connect(_set_player_count.bind(count))
		count_row.add_child(button)
		count_buttons[count] = button

	# Jika kembali dari layar Game, isi ulang dengan pemain sebelumnya.
	var previous: Array = SceneRouter.params.get("players", [])
	_set_player_count(clampi(previous.size(), MIN_PLAYERS, MAX_PLAYERS) if previous.size() > 0 else 2)
	for i in previous.size():
		if i < slots.get_child_count():
			slots.get_child(i).setup(i, previous[i].name, previous[i].token)


func _set_player_count(count: int) -> void:
	player_count = count
	for c in count_buttons:
		# Tombol terpilih = biru penuh, yang lain = tombol putih bergaris.
		count_buttons[c].theme_type_variation = &"" if c == count else &"SecondaryButton"

	# Tambah slot yang kurang, hapus slot yang berlebih. Nama yang sudah diketik tetap aman.
	while slots.get_child_count() < count:
		var slot := PLAYER_SLOT.instantiate()
		slots.add_child(slot)
		var index := slots.get_child_count() - 1
		slot.setup(index, "", _first_free_token())
		slot.token_pressed.connect(_on_token_pressed)
	while slots.get_child_count() > count:
		var last := slots.get_child(slots.get_child_count() - 1)
		slots.remove_child(last)
		last.queue_free()


## Ganti token slot ke token berikutnya yang belum dipakai pemain lain.
func _on_token_pressed(slot) -> void:
	var available := GameData.get_available_tokens()
	var ids: Array = available.map(func(token): return token.id)
	var start := ids.find(slot.token_id)
	for step in range(1, ids.size() + 1):
		var candidate: String = ids[(start + step) % ids.size()]
		if candidate == slot.token_id or not _used_tokens().has(candidate):
			slot.set_token(candidate)
			return


func _used_tokens() -> Array:
	return slots.get_children().map(func(slot): return slot.token_id)


func _first_free_token() -> String:
	for token in GameData.get_available_tokens():
		if not _used_tokens().has(token.id):
			return token.id
	return GameData.get_available_tokens()[0].id


func _on_start_pressed() -> void:
	var players: Array = []
	for slot in slots.get_children():
		players.append({"name": slot.get_player_name(), "token": slot.token_id})
	SceneRouter.goto("game", {"mode": SceneRouter.params.get("mode", "family"), "players": players})
