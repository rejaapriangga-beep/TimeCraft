extends Node
## PropertyManager: siapa pemilik petak mana, membeli, dan menghitung/membayar sewa.
## Berlaku untuk semua petak yang bisa dimiliki: property, stasiun, utilitas.

signal property_bought(player: PlayerState, tile_index: int)
signal rent_paid(payer: PlayerState, receiver: PlayerState, amount: int, tile_index: int)

const OWNABLE_TYPES := ["property", "station", "utility"]

var owners: Dictionary = {}   # tile_index -> player_id (tidak ada = milik bank)
var player_manager: Node


func setup(players: Node) -> void:
	player_manager = players
	owners.clear()


# ---------- Pertanyaan ----------

func is_ownable(tile_index: int) -> bool:
	return GameData.get_tile(tile_index).type in OWNABLE_TYPES


## -1 = belum ada pemilik (milik bank).
func get_owner_id(tile_index: int) -> int:
	return owners.get(tile_index, -1)


func get_price(tile_index: int) -> int:
	return int(GameData.get_tile(tile_index).get("price", 0))


func can_afford(player: PlayerState, tile_index: int) -> bool:
	return player.money >= get_price(tile_index)


## Semua petak milik seorang pemain, urut sesuai board.
func owned_tiles(player_id: int) -> Array[int]:
	var result: Array[int] = []
	for tile_index in owners:
		if owners[tile_index] == player_id:
			result.append(tile_index)
	result.sort()
	return result


## Jumlah petak jenis tertentu (dan grup tertentu) yang dimiliki pemain.
func count_owned(player_id: int, type: String, group: String = "") -> int:
	var count := 0
	for tile_index in owned_tiles(player_id):
		var tile := GameData.get_tile(tile_index)
		if tile.type == type and (group == "" or tile.get("group", "") == group):
			count += 1
	return count


## Berapa property dalam satu grup warna (mis. coklat = 2).
func group_size(group: String) -> int:
	var count := 0
	for tile in GameData.tiles:
		if tile.type == "property" and tile.group == group:
			count += 1
	return count


func owns_full_group(player_id: int, group: String) -> bool:
	return count_owned(player_id, "property", group) == group_size(group)


## Sewa petak ini sekarang. dice_total dipakai untuk utilitas.
func calculate_rent(tile_index: int, dice_total: int) -> int:
	var owner_id := get_owner_id(tile_index)
	if owner_id < 0:
		return 0
	var tile := GameData.get_tile(tile_index)
	match String(tile.type):
		"property":
			var rent := int(tile.rent)
			if owns_full_group(owner_id, tile.group):
				rent *= int(GameData.config.get("monopoly_rent_multiplier", 2))
			return rent
		"station":
			var table: Array = GameData.config.get("station_rent", [25, 50, 100, 200])
			var count := count_owned(owner_id, "station")
			return int(table[clampi(count - 1, 0, table.size() - 1)])
		"utility":
			var multipliers: Array = GameData.config.get("utility_multiplier", [4, 10])
			var count := count_owned(owner_id, "utility")
			return dice_total * int(multipliers[clampi(count - 1, 0, multipliers.size() - 1)])
	return 0


# ---------- Aksi ----------

func buy(player: PlayerState, tile_index: int) -> void:
	if get_owner_id(tile_index) >= 0 or not can_afford(player, tile_index):
		return
	owners[tile_index] = player.id
	player.stats.properties_bought += 1
	player_manager.add_money(player, -get_price(tile_index))
	property_bought.emit(player, tile_index)


## Bayar sewa ke pemilik. Mengembalikan jumlah yang dibayar.
## Uang boleh sampai minus; aturan bangkrut dibuat di langkah 1.9.
func pay_rent(payer: PlayerState, tile_index: int, dice_total: int) -> int:
	var owner_id := get_owner_id(tile_index)
	if owner_id < 0 or owner_id == payer.id:
		return 0
	var receiver: PlayerState = player_manager.players[owner_id]
	var amount := calculate_rent(tile_index, dice_total)
	player_manager.transfer(payer, receiver, amount)
	payer.stats.rent_paid += amount
	receiver.stats.rent_earned += amount
	receiver.stats.biggest_rent = maxi(receiver.stats.biggest_rent, amount)
	rent_paid.emit(payer, receiver, amount, tile_index)
	return amount
