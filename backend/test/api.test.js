const test = require("node:test");
const assert = require("node:assert/strict");
const { server } = require("../server");

let baseUrl;
test.before(async () => {
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});
test.after(() => server.close());

async function request(path, options = {}) {
  const response = await fetch(`${baseUrl}${path}`, { headers: { "Content-Type": "application/json", ...options.headers }, ...options });
  return { status: response.status, body: await response.json() };
}

test("orang tua dapat memberi hadiah akses setelah tugas disetujui", async () => {
  const suffix = Date.now();
  const registered = await request("/auth/register-parent", { method: "POST", body: JSON.stringify({ familyName: "Keluarga Uji", name: "Ibu", email: `ibu${suffix}@contoh.id`, password: "rahasia-aman", acceptedTerms: true }) });
  assert.equal(registered.status, 201);
  const parentAuth = { Authorization: `Bearer ${registered.body.token}` };
  const child = await request("/family/children", { method: "POST", headers: parentAuth, body: JSON.stringify({ name: "Budi", pin: "1234" }) });
  const task = await request("/tasks", { method: "POST", headers: parentAuth, body: JSON.stringify({ childId: child.body.child.id, title: "Belajar", rewardMinutes: 20 }) });
  const childLogin = await request("/auth/login-child", { method: "POST", body: JSON.stringify({ familyCode: child.body.familyCode, pin: "1234" }) });
  const childAuth = { Authorization: `Bearer ${childLogin.body.token}` };
  await request(`/tasks/${task.body.task.id}/submit`, { method: "POST", headers: childAuth, body: JSON.stringify({ evidence: "Sudah selesai" }) });
  const decision = await request(`/tasks/${task.body.task.id}/decision`, { method: "POST", headers: parentAuth, body: JSON.stringify({ approved: true }) });
  assert.equal(decision.body.task.status, "approved");
  const balance = await request("/access-balance", { headers: childAuth });
  assert.equal(balance.body.minutes, 20);
});

test("chat grup keluarga (thread \"family\") tidak bocor ke keluarga lain", async () => {
  const suffix = Date.now();
  const familyA = await request("/auth/register-parent", { method: "POST", body: JSON.stringify({ familyName: "Keluarga A", name: "Ayah A", email: `ayahA${suffix}@contoh.id`, password: "rahasia-aman", acceptedTerms: true }) });
  const familyB = await request("/auth/register-parent", { method: "POST", body: JSON.stringify({ familyName: "Keluarga B", name: "Ayah B", email: `ayahB${suffix}@contoh.id`, password: "rahasia-aman", acceptedTerms: true }) });
  const authA = { Authorization: `Bearer ${familyA.body.token}` };
  const authB = { Authorization: `Bearer ${familyB.body.token}` };

  const sent = await request("/chat/family/messages", { method: "POST", headers: authA, body: JSON.stringify({ type: "text", text: "Pesan rahasia keluarga A" }) });
  assert.equal(sent.status, 201);

  // Keluarga B TIDAK boleh melihat pesan grup keluarga A di thread "family" miliknya sendiri.
  const seenByB = await request("/chat/family/messages", { headers: authB });
  assert.equal(seenByB.status, 200);
  assert.deepEqual(seenByB.body.messages, []);

  // Keluarga A tetap melihat pesannya sendiri seperti biasa.
  const seenByA = await request("/chat/family/messages", { headers: authA });
  assert.equal(seenByA.body.messages.length, 1);
  assert.equal(seenByA.body.messages[0].text, "Pesan rahasia keluarga A");
  // Kontrak API klien tidak berubah - childId yang dikembalikan tetap sentinel "family".
  assert.equal(seenByA.body.messages[0].childId, "family");
});

test("hapus akun menolak kata sandi salah, lalu menghapus seluruh data keluarga kalau benar", async () => {
  const suffix = Date.now();
  const registered = await request("/auth/register-parent", { method: "POST", body: JSON.stringify({ familyName: "Keluarga Hapus", name: "Ibu", email: `hapus${suffix}@contoh.id`, password: "rahasia-aman", acceptedTerms: true }) });
  const parentAuth = { Authorization: `Bearer ${registered.body.token}` };
  const child = await request("/family/children", { method: "POST", headers: parentAuth, body: JSON.stringify({ name: "Budi", pin: "1234" }) });
  await request("/tasks", { method: "POST", headers: parentAuth, body: JSON.stringify({ childId: child.body.child.id, title: "Belajar", rewardMinutes: 20 }) });
  await request("/chat/family/messages", { method: "POST", headers: parentAuth, body: JSON.stringify({ type: "text", text: "Halo keluarga" }) });

  const wrongPassword = await request("/account", { method: "DELETE", headers: parentAuth, body: JSON.stringify({ password: "salah-total" }) });
  assert.equal(wrongPassword.status, 403);

  const deleted = await request("/account", { method: "DELETE", headers: parentAuth, body: JSON.stringify({ password: "rahasia-aman" }) });
  assert.equal(deleted.status, 200);

  // Token orang tua yang barusan dipakai untuk hapus akun sendiri langsung tidak berlaku lagi.
  const afterDelete = await request("/family", { headers: parentAuth });
  assert.equal(afterDelete.status, 401);

  // Login anak dengan family code lama juga sudah tidak berlaku (keluarganya sudah tidak ada).
  const childLoginAfter = await request("/auth/login-child", { method: "POST", body: JSON.stringify({ familyCode: child.body.familyCode, pin: "1234" }) });
  assert.equal(childLoginAfter.status, 401);
});

test("ubah kata sandi: tolak kata sandi lama salah, cabut sesi device lain, sesi sendiri tetap hidup", async () => {
  const suffix = Date.now();
  const registered = await request("/auth/register-parent", { method: "POST", body: JSON.stringify({ familyName: "Keluarga Sandi", name: "Ayah", email: `sandi${suffix}@contoh.id`, password: "sandi-lama-aman", acceptedTerms: true }) });
  const authA = { Authorization: `Bearer ${registered.body.token}` };

  // Simulasikan "device lain" - login ulang pakai kredensial yang sama menghasilkan token kedua.
  const loginB = await request("/auth/login-parent", { method: "POST", body: JSON.stringify({ email: `sandi${suffix}@contoh.id`, password: "sandi-lama-aman" }) });
  const authB = { Authorization: `Bearer ${loginB.body.token}` };

  const wrongCurrent = await request("/account/change-password", { method: "POST", headers: authA, body: JSON.stringify({ currentPassword: "salah", newPassword: "sandi-baru-aman" }) });
  assert.equal(wrongCurrent.status, 403);

  const changed = await request("/account/change-password", { method: "POST", headers: authA, body: JSON.stringify({ currentPassword: "sandi-lama-aman", newPassword: "sandi-baru-aman" }) });
  assert.equal(changed.status, 200);

  // Sesi yang dipakai untuk ganti kata sandi ini SENDIRI tetap hidup.
  const stillWorksA = await request("/family", { headers: authA });
  assert.equal(stillWorksA.status, 200);

  // Sesi device LAIN (token lama, dari sebelum kata sandi diganti) langsung tercabut.
  const revokedB = await request("/family", { headers: authB });
  assert.equal(revokedB.status, 401);

  // Login dengan kata sandi lama sudah tidak bisa, harus pakai yang baru.
  const loginOldPassword = await request("/auth/login-parent", { method: "POST", body: JSON.stringify({ email: `sandi${suffix}@contoh.id`, password: "sandi-lama-aman" }) });
  assert.equal(loginOldPassword.status, 401);
  const loginNewPassword = await request("/auth/login-parent", { method: "POST", body: JSON.stringify({ email: `sandi${suffix}@contoh.id`, password: "sandi-baru-aman" }) });
  assert.equal(loginNewPassword.status, 200);
});

test("pendaftaran orang tua ditolak kalau Kebijakan Penggunaan belum disetujui", async () => {
  const suffix = Date.now();
  const withoutTerms = await request("/auth/register-parent", { method: "POST", body: JSON.stringify({ familyName: "Keluarga Tanpa Setuju", name: "Ibu", email: `tanpasetuju${suffix}@contoh.id`, password: "rahasia-aman" }) });
  assert.equal(withoutTerms.status, 400);

  const withFalseTerms = await request("/auth/register-parent", { method: "POST", body: JSON.stringify({ familyName: "Keluarga Tanpa Setuju", name: "Ibu", email: `tanpasetuju2${suffix}@contoh.id`, password: "rahasia-aman", acceptedTerms: false }) });
  assert.equal(withFalseTerms.status, 400);

  const withTerms = await request("/auth/register-parent", { method: "POST", body: JSON.stringify({ familyName: "Keluarga Setuju", name: "Ibu", email: `setuju${suffix}@contoh.id`, password: "rahasia-aman", acceptedTerms: true }) });
  assert.equal(withTerms.status, 201);
  assert.ok(withTerms.body.token);
});

test("blokir domain: hanya orang tua yang bisa mengatur, anak cuma bisa baca, domain dinormalisasi & dedup", async () => {
  const suffix = Date.now();
  const registered = await request("/auth/register-parent", { method: "POST", body: JSON.stringify({ familyName: "Keluarga Blokir", name: "Ibu", email: `blokir${suffix}@contoh.id`, password: "rahasia-aman", acceptedTerms: true }) });
  const parentAuth = { Authorization: `Bearer ${registered.body.token}` };
  const child = await request("/family/children", { method: "POST", headers: parentAuth, body: JSON.stringify({ name: "Budi", pin: "1234" }) });
  const childLogin = await request("/auth/login-child", { method: "POST", body: JSON.stringify({ familyCode: child.body.familyCode, pin: "1234" }) });
  const childAuth = { Authorization: `Bearer ${childLogin.body.token}` };

  // Anak tidak boleh mengubah daftar blokir.
  const childTriesToSet = await request("/family/blocked-domains", { method: "POST", headers: childAuth, body: JSON.stringify({ customDomains: ["contoh.com"] }) });
  assert.equal(childTriesToSet.status, 401);

  // Domain dinormalisasi (protokol/www/path dibuang, huruf kecil) & duplikat dihapus.
  const setResult = await request("/family/blocked-domains", {
    method: "POST", headers: parentAuth,
    body: JSON.stringify({ customDomains: ["https://Contoh.com/halaman", "www.contoh.com", "lain.id", "bukan domain valid"] })
  });
  assert.equal(setResult.status, 200);
  assert.deepEqual(setResult.body.customDomains.sort(), ["contoh.com", "lain.id"]);

  // Anak bisa baca daftar efektifnya (untuk sinkron VPN filter).
  const childReads = await request("/family/blocked-domains", { headers: childAuth });
  assert.equal(childReads.status, 200);
  assert.deepEqual(childReads.body.effectiveDomains.sort(), ["contoh.com", "lain.id"]);
});

test("status izin perangkat anak: dilaporkan sendiri, terlihat orang tua, tercatat di log saat berubah", async () => {
  const suffix = Date.now();
  const registered = await request("/auth/register-parent", { method: "POST", body: JSON.stringify({ familyName: "Keluarga Izin", name: "Ibu", email: `izin${suffix}@contoh.id`, password: "rahasia-aman", acceptedTerms: true }) });
  const parentAuth = { Authorization: `Bearer ${registered.body.token}` };
  const child = await request("/family/children", { method: "POST", headers: parentAuth, body: JSON.stringify({ name: "Budi", pin: "1234" }) });
  const childLogin = await request("/auth/login-child", { method: "POST", body: JSON.stringify({ familyCode: child.body.familyCode, pin: "1234" }) });
  const childAuth = { Authorization: `Bearer ${childLogin.body.token}` };

  // Sebelum pernah lapor, status undefined (bukan false) - orang tua bisa bedakan dari "sudah dicabut".
  const beforeReport = await request("/family", { headers: parentAuth });
  const childBefore = beforeReport.body.children.find((item) => item.id === child.body.child.id);
  assert.equal(childBefore.overlayPermissionGranted, undefined);
  assert.equal(childBefore.vpnPermissionGranted, undefined);
  assert.equal(childBefore.guardPermissionGranted, undefined);

  // Laporan pertama: true/true, TANPA guardGranted (mensimulasikan APK anak versi lama sebelum
  // fitur Accessibility Service ada) - harus tetap diterima 200, guardPermissionGranted tetap
  // undefined (bukan diisi false), dan TIDAK dicatat sebagai "dicabut" di log (belum ada status
  // sebelumnya untuk dibandingkan).
  const firstReport = await request("/children/permission-status", { method: "POST", headers: childAuth, body: JSON.stringify({ overlayGranted: true, vpnGranted: true }) });
  assert.equal(firstReport.status, 200);
  const logAfterFirst = await request("/activity-log", { headers: parentAuth });
  assert.ok(!logAfterFirst.body.entries.some((entry) => entry.action.includes("permission")));
  const afterFirstFamily = await request("/family", { headers: parentAuth });
  assert.equal(afterFirstFamily.body.children.find((item) => item.id === child.body.child.id).guardPermissionGranted, undefined);

  // Anak mencabut izin VPN - status terbaru terlihat orang tua & tercatat di log aktivitas.
  const secondReport = await request("/children/permission-status", { method: "POST", headers: childAuth, body: JSON.stringify({ overlayGranted: true, vpnGranted: false }) });
  assert.equal(secondReport.status, 200);
  const afterRevoke = await request("/family", { headers: parentAuth });
  const childAfter = afterRevoke.body.children.find((item) => item.id === child.body.child.id);
  assert.equal(childAfter.overlayPermissionGranted, true);
  assert.equal(childAfter.vpnPermissionGranted, false);
  const logAfterRevoke = await request("/activity-log", { headers: parentAuth });
  assert.ok(logAfterRevoke.body.entries.some((entry) => entry.action === "vpn_permission_revoked"));

  // APK anak sudah update, mulai kirim guardGranted juga - laporan pertama untuk field ini juga
  // tidak dicatat sebagai "dicabut" (baseline baru), lalu pencabutannya tercatat sama seperti overlay/vpn.
  const thirdReport = await request("/children/permission-status", { method: "POST", headers: childAuth, body: JSON.stringify({ overlayGranted: true, vpnGranted: false, guardGranted: true }) });
  assert.equal(thirdReport.status, 200);
  const afterThirdFamily = await request("/family", { headers: parentAuth });
  assert.equal(afterThirdFamily.body.children.find((item) => item.id === child.body.child.id).guardPermissionGranted, true);
  const logAfterThird = await request("/activity-log", { headers: parentAuth });
  assert.ok(!logAfterThird.body.entries.some((entry) => entry.action.includes("guard")));

  const fourthReport = await request("/children/permission-status", { method: "POST", headers: childAuth, body: JSON.stringify({ overlayGranted: true, vpnGranted: false, guardGranted: false }) });
  assert.equal(fourthReport.status, 200);
  const afterFourthFamily = await request("/family", { headers: parentAuth });
  assert.equal(afterFourthFamily.body.children.find((item) => item.id === child.body.child.id).guardPermissionGranted, false);
  const logAfterFourth = await request("/activity-log", { headers: parentAuth });
  assert.ok(logAfterFourth.body.entries.some((entry) => entry.action === "guard_permission_revoked"));
});
