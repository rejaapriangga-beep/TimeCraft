extends Node
## DiceManager: melempar 2 dadu secara acak. Hanya logika, animasinya ada di DiceView.

signal dice_rolled(die_1: int, die_2: int)

var _rng := RandomNumberGenerator.new()
var _forced: Array = []   # untuk uji coba: hasil lemparan berikutnya yang dipaksakan


func _ready() -> void:
	_rng.randomize()


## Lempar 2 dadu. Hasil: {"die_1", "die_2", "total", "is_double"}
func roll() -> Dictionary:
	var die_1 := _rng.randi_range(1, 6)
	var die_2 := _rng.randi_range(1, 6)
	if _forced.size() == 2:
		die_1 = _forced[0]
		die_2 = _forced[1]
		_forced.clear()
	dice_rolled.emit(die_1, die_2)
	return {
		"die_1": die_1,
		"die_2": die_2,
		"total": die_1 + die_2,
		"is_double": die_1 == die_2,
	}


## Uji coba: paksa hasil lemparan berikutnya, mis. force_next(3, 4).
func force_next(die_1: int, die_2: int) -> void:
	_forced = [clampi(die_1, 1, 6), clampi(die_2, 1, 6)]
