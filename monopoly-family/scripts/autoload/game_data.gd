extends Node
## Autoload "GameData": membaca semua data game dari folder data/ satu kali saat game dibuka.
##
## Script lain cukup memanggil, misalnya:
##   GameData.get_tile(5)            -> Dictionary data petak ke-5
##   GameData.config.starting_money  -> uang awal
##
## Catatan: JSON membaca semua angka sebagai float. Gunakan int(...) saat butuh bilangan bulat.

const CONFIG_PATH := "res://data/config.json"
const BOARD_PATH := "res://data/board_classic.json"
const TOKENS_PATH := "res://data/tokens.json"

var config: Dictionary = {}
var board: Dictionary = {}
var tiles: Array = []
var tokens: Array = []


func _ready() -> void:
	config = _load_json(CONFIG_PATH)
	board = _load_json(BOARD_PATH)
	tiles = board.get("tiles", [])
	tokens = _load_json(TOKENS_PATH).get("tokens", [])

	if tiles.size() == 0 or tiles.size() % 4 != 0:
		push_error("Board harus punya jumlah petak kelipatan 4, sekarang: %d" % tiles.size())


# ---------- Board ----------

func get_board_size() -> int:
	return tiles.size()


func get_tile(index: int) -> Dictionary:
	return tiles[index % tiles.size()]


func get_group_color(group: String) -> Color:
	var groups: Dictionary = board.get("groups", {})
	return Color.html(groups.get(group, "#CCCCCC"))


## "brown" -> "Coklat". Nama grup ada di board JSON bagian group_names.
func get_group_name(group: String) -> String:
	return board.get("group_names", {}).get(group, group)


# ---------- Token ----------

func get_token(token_id: String) -> Dictionary:
	for token in tokens:
		if token.id == token_id:
			return token
	return tokens[0]


## Token yang boleh dipakai sekarang. Fase 2: ditambah token yang sudah di-unlock.
func get_available_tokens() -> Array:
	return tokens.filter(func(token): return token.starter)


# ---------- Uang ----------

## 1500 -> "$1.500"
func format_money(amount: int) -> String:
	var digits := str(absi(amount))
	var result := ""
	while digits.length() > 3:
		result = "." + digits.right(3) + result
		digits = digits.left(digits.length() - 3)
	var minus := "-" if amount < 0 else ""
	return minus + config.get("currency_symbol", "$") + digits + result


# ---------- Helper ----------

func _load_json(path: String) -> Dictionary:
	var file := FileAccess.open(path, FileAccess.READ)
	if file == null:
		push_error("Gagal membuka %s" % path)
		return {}
	var data = JSON.parse_string(file.get_as_text())
	if typeof(data) != TYPE_DICTIONARY:
		push_error("Format JSON salah di %s" % path)
		return {}
	return data
