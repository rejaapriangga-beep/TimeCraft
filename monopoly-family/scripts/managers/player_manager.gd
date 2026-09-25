extends Node
## PlayerManager: menyimpan semua pemain dan satu-satunya yang boleh mengubah uang/posisi mereka.
## UI tidak diubah langsung dari sini; UI mendengarkan signal.

signal money_changed(player: PlayerState, delta: int)
signal position_changed(player: PlayerState)
signal current_player_changed(player: PlayerState)

var players: Array[PlayerState] = []
var current_index: int = 0


## player_defs: [{"name": "Andi", "token": "cat"}, ...] dari layar Setup.
func setup(player_defs: Array, starting_money: int) -> void:
	players.clear()
	for i in player_defs.size():
		players.append(PlayerState.new(i, player_defs[i].name, player_defs[i].token, starting_money))
	current_index = 0


func get_current() -> PlayerState:
	return players[current_index]


func add_money(player: PlayerState, amount: int) -> void:
	player.money += amount
	money_changed.emit(player, amount)


## Pindahkan uang dari satu pemain ke pemain lain (mis. bayar sewa).
func transfer(from_player: PlayerState, to_player: PlayerState, amount: int) -> void:
	add_money(from_player, -amount)
	add_money(to_player, amount)


func set_position(player: PlayerState, tile_index: int) -> void:
	player.position = tile_index
	position_changed.emit(player)


## Pindahkan pemain ke Penjara (tanpa bonus START).
## Aturan keluar penjara (bayar denda / coba dobel) dibuat di langkah 1.8.
func send_to_jail(player: PlayerState, jail_index: int) -> void:
	player.in_jail = true
	player.jail_turns = 0
	set_position(player, jail_index)


## Pindah ke pemain berikutnya yang belum bangkrut.
func next_player() -> PlayerState:
	for i in players.size():
		current_index = (current_index + 1) % players.size()
		if not players[current_index].is_bankrupt:
			break
	current_player_changed.emit(get_current())
	return get_current()
