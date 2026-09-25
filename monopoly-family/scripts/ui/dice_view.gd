extends Control
## Menggambar 2 dadu dan animasi lemparannya.
##
## Pakai:  await dice_view.play_roll(3, 5)   # tunggu sampai animasi selesai

const ROLL_SECONDS := 0.7      # lama dadu "berputar"
const FACE_CHANGE_SECONDS := 0.07

## Posisi titik (pip) di dadu, dalam satuan 0..1 dari sisi dadu.
const PIPS := {
	1: [Vector2(0.5, 0.5)],
	2: [Vector2(0.27, 0.27), Vector2(0.73, 0.73)],
	3: [Vector2(0.27, 0.27), Vector2(0.5, 0.5), Vector2(0.73, 0.73)],
	4: [Vector2(0.27, 0.27), Vector2(0.73, 0.27), Vector2(0.27, 0.73), Vector2(0.73, 0.73)],
	5: [Vector2(0.27, 0.27), Vector2(0.73, 0.27), Vector2(0.5, 0.5), Vector2(0.27, 0.73), Vector2(0.73, 0.73)],
	6: [Vector2(0.27, 0.25), Vector2(0.73, 0.25), Vector2(0.27, 0.5), Vector2(0.73, 0.5), Vector2(0.27, 0.75), Vector2(0.73, 0.75)],
}

var die_size: float = 96.0:
	set(value):
		die_size = value
		custom_minimum_size = Vector2(die_size * 2 + _gap(), die_size * 1.15)
		queue_redraw()

var faces: Array[int] = [1, 1]
var _angles: Array[float] = [0.0, 0.0]
var _pop: float = 1.0:     # efek "membal" saat dadu berhenti
	set(value):
		_pop = value
		queue_redraw()
var _rng := RandomNumberGenerator.new()


func _ready() -> void:
	_rng.randomize()
	faces = [_rng.randi_range(1, 6), _rng.randi_range(1, 6)]
	die_size = die_size   # hitung ukuran minimum


func _gap() -> float:
	return die_size * 0.3


## Animasi: wajah dadu berganti-ganti acak sambil sedikit berputar, lalu berhenti di hasil akhir.
func play_roll(final_1: int, final_2: int) -> void:
	var elapsed := 0.0
	while elapsed < ROLL_SECONDS:
		faces = [_rng.randi_range(1, 6), _rng.randi_range(1, 6)]
		_angles = [_rng.randf_range(-0.35, 0.35), _rng.randf_range(-0.35, 0.35)]
		queue_redraw()
		await create_tween().tween_interval(FACE_CHANGE_SECONDS).finished
		elapsed += FACE_CHANGE_SECONDS

	faces = [final_1, final_2]
	_angles = [0.0, 0.0]
	var tween := create_tween()
	tween.tween_property(self, "_pop", 1.18, 0.08).set_trans(Tween.TRANS_SINE)
	tween.tween_property(self, "_pop", 1.0, 0.14).set_trans(Tween.TRANS_BACK).set_ease(Tween.EASE_OUT)
	await tween.finished


func _draw() -> void:
	var total_width := die_size * 2 + _gap()
	var start_x := (size.x - total_width) / 2.0
	for i in 2:
		var center := Vector2(start_x + die_size / 2.0 + i * (die_size + _gap()), size.y / 2.0)
		_draw_die(center, faces[i], _angles[i])


func _draw_die(center: Vector2, value: int, angle: float) -> void:
	var s := die_size * _pop
	draw_set_transform(center, angle, Vector2.ONE)
	var rect := Rect2(-s / 2.0, -s / 2.0, s, s)
	var box := UIStyle.rounded_box(UIStyle.WHITE, int(s * 0.2), UIStyle.NAVY, maxi(2, int(s * 0.04)))
	box.shadow_color = Color(0.1176, 0.1373, 0.1882, 0.18)
	box.shadow_size = int(s * 0.08)
	box.shadow_offset = Vector2(0, s * 0.05)
	draw_style_box(box, rect)
	for pip in PIPS[value]:
		draw_circle(rect.position + pip * s, s * 0.085, UIStyle.NAVY)
	draw_set_transform(Vector2.ZERO, 0.0, Vector2.ONE)
