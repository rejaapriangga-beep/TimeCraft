extends Node
## BoardManager: aturan pergerakan di board (bukan tampilan — tampilan ada di BoardView).


## Daftar petak yang dilewati satu per satu, mis. dari 30 maju 4 -> [31, 0, 1, 2].
## Petak terakhir = petak tujuan.
func build_move_path(from_index: int, steps: int) -> Array[int]:
	var path: Array[int] = []
	var board_size := GameData.get_board_size()
	for step in range(1, steps + 1):
		path.append((from_index + step) % board_size)
	return path


## Index petak pertama dengan type tertentu, mis. find_tile_index("jail") -> 8. -1 jika tidak ada.
func find_tile_index(type: String) -> int:
	for i in GameData.get_board_size():
		if GameData.get_tile(i).type == type:
			return i
	return -1


## True jika jalur melewati atau berhenti di START (petak 0).
func passes_start(path: Array[int]) -> bool:
	return path.has(0)
