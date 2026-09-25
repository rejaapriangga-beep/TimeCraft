extends Node
## Autoload "SceneRouter": satu-satunya tempat yang memindahkan layar.
##
## Pakai:  SceneRouter.goto("setup", {"mode": "family"})
## Layar tujuan membaca parameter lewat SceneRouter.params.

const SCENES := {
	"home": "res://scenes/home/Home.tscn",
	"setup": "res://scenes/setup/Setup.tscn",
	"game": "res://scenes/game/Game.tscn",
}

## Layar "sebelumnya" untuk tombol Back Android.
const BACK_TARGET := {
	"setup": "home",
	"game": "setup",
}

var params: Dictionary = {}
var current: String = "home"


func goto(scene_name: String, new_params: Dictionary = {}) -> void:
	if not SCENES.has(scene_name):
		push_error("Layar tidak dikenal: %s" % scene_name)
		return
	params = new_params
	current = scene_name
	get_tree().change_scene_to_file(SCENES[scene_name])


func go_back() -> void:
	if current == "home":
		get_tree().quit()
	else:
		# Parameter ikut dibawa agar layar Setup tetap ingat pemain yang sudah diisi.
		goto(BACK_TARGET.get(current, "home"), params)


func _notification(what: int) -> void:
	# Tombol Back di Android.
	if what == NOTIFICATION_WM_GO_BACK_REQUEST:
		go_back()
