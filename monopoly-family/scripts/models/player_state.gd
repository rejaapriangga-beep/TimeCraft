class_name PlayerState
extends RefCounted
## Data satu pemain selama satu pertandingan. Hanya data, tanpa tampilan.
## Yang mengubah isinya hanya PlayerManager.

var id: int = 0
var name: String = ""
var token_id: String = ""
var money: int = 0
var position: int = 0          # index petak, 0 = START
var in_jail: bool = false      # dipakai langkah 1.8
var jail_turns: int = 0
var is_bankrupt: bool = false  # dipakai langkah 1.9

## Statistik untuk layar hasil (langkah 1.9).
var stats := {
	"rolls": 0,
	"best_roll": 0,
	"passed_start": 0,
	"rent_paid": 0,
	"rent_earned": 0,
	"biggest_rent": 0,
	"properties_bought": 0,
}


func _init(player_id: int, player_name: String, player_token: String, starting_money: int) -> void:
	id = player_id
	name = player_name
	token_id = player_token
	money = starting_money
