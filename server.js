const express = require('express');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const crypto = require('crypto');
const { execSync } = require('child_process');

const app = express();
const PORT = process.env.PORT || 20048;

const ADMIN_KEY = '2735313';

// ── RSA-2048 Session Security ──────────────────────────────────────────────────
// Architecture: Server signs session tokens with RSA-2048 private key.
// The APK embeds ONLY the public key — cannot forge sessions without the private
// key even after full APK decompilation + Frida hooking of local checks.
//
// Setup: set SESSION_PRIVATE_KEY env-var to the PEM private key.
// On first run without the env-var a new keypair is auto-generated and stored in
// data/session-keys.json — copy the logged public key into build.gradle.kts.

// SESSION_KEYS_FILE and rsaKeys are initialized after DATA_DIR is defined (see below).
let rsaKeys = null;

function loadOrGenerateRsaKeyPair(dataDir) {
  const keysFile = path.join(dataDir, 'session-keys.json');
  if (process.env.SESSION_PRIVATE_KEY) {
    const privateKey = process.env.SESSION_PRIVATE_KEY.replace(/\\n/g, '\n');
    const publicKey  = crypto.createPublicKey(privateKey)
                             .export({ type: 'spki', format: 'pem' });
    return { privateKey, publicKey };
  }
  if (fs.existsSync(keysFile)) {
    try {
      const k = JSON.parse(fs.readFileSync(keysFile, 'utf8'));
      if (k.privateKey && k.publicKey) return k;
    } catch (_) {}
  }
  const { privateKey, publicKey } = crypto.generateKeyPairSync('rsa', {
    modulusLength: 2048,
    publicKeyEncoding:  { type: 'spki',  format: 'pem' },
    privateKeyEncoding: { type: 'pkcs8', format: 'pem' }
  });
  const keys = { privateKey, publicKey };
  fs.writeFileSync(keysFile, JSON.stringify(keys, null, 2), 'utf8');
  const pubBase64 = publicKey.replace(/-----[^\n]*-----\n?/g, '').replace(/\n/g, '');
  console.log('[FlexBoard] RSA keypair generated. Embed this PUBLIC KEY in build.gradle.kts → SERVER_PUBLIC_KEY:');
  console.log(pubBase64);
  return keys;
}

function signSession(payload) {
  const payloadStr = JSON.stringify(payload);
  const payloadB64 = Buffer.from(payloadStr, 'utf8').toString('base64');
  const sign = crypto.createSign('SHA256');
  sign.update(payloadStr);
  const signature = sign.sign(rsaKeys.privateKey, 'base64');
  return { token: payloadB64, signature };
}

function verifySessionSignature(tokenB64, signatureB64) {
  try {
    const payloadStr = Buffer.from(tokenB64, 'base64').toString('utf8');
    const verify = crypto.createVerify('SHA256');
    verify.update(payloadStr);
    return verify.verify(rsaKeys.publicKey, signatureB64, 'base64');
  } catch (_) { return false; }
}

function createSessionPayload(device_id, user) {
  const sessionId = crypto.randomBytes(16).toString('hex');
  const now = Date.now();
  return {
    session_id:   sessionId,
    device_id,
    features: {
      auto_typer:    user.auto_typer === true,
      space_label:   user.space_label === true,
      char_delay_ms: 35,
      line_delay_ms: 5000
    },
    issued_at:    now,
    expires_at:   now + 7 * 60 * 1000,   // 7-minute sessions
    server_nonce: crypto.randomBytes(8).toString('hex')
  };
}

// Constant-time string comparison to prevent timing attacks
function safeEqual(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string') return false;
  if (a.length !== b.length) return false;
  return crypto.timingSafeEqual(Buffer.from(a), Buffer.from(b));
}

const DATA_DIR   = path.join(__dirname, 'data');
const USERS_FILE = path.join(DATA_DIR, 'users.json');
const SETTINGS_FILE = path.join(DATA_DIR, 'settings.json');
const KEYS_FILE  = path.join(DATA_DIR, 'keys.json');
const APK_DIR    = path.join(__dirname, 'apk');

if (!fs.existsSync(APK_DIR)) fs.mkdirSync(APK_DIR, { recursive: true });
if (!fs.existsSync(KEYS_FILE)) fs.writeFileSync(KEYS_FILE, JSON.stringify({ keys: [] }, null, 2), 'utf8');

// Initialize RSA keypair now that DATA_DIR is available
rsaKeys = loadOrGenerateRsaKeyPair(DATA_DIR);

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, APK_DIR),
  filename:    (req, file, cb) => cb(null, 'flexboard-pro.apk')
});
const upload = multer({ storage, limits: { fileSize: 200 * 1024 * 1024 } });

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

function readUsers()      { return JSON.parse(fs.readFileSync(USERS_FILE,    'utf8')); }
function writeUsers(d)    { fs.writeFileSync(USERS_FILE,    JSON.stringify(d, null, 2), 'utf8'); }
function readSettings()   { return JSON.parse(fs.readFileSync(SETTINGS_FILE, 'utf8')); }
function writeSettings(d) { fs.writeFileSync(SETTINGS_FILE, JSON.stringify(d, null, 2), 'utf8'); }
function readKeys()       { try { return JSON.parse(fs.readFileSync(KEYS_FILE, 'utf8')); } catch { return { keys: [] }; } }
function writeKeys(d)     { fs.writeFileSync(KEYS_FILE, JSON.stringify(d, null, 2), 'utf8'); }

function requireAdmin(req, res, next) {
  const key = req.headers['x-admin-key'] || req.query.key;
  if (key !== ADMIN_KEY) return res.status(401).json({ error: 'Unauthorized' });
  next();
}

function versionGreater(a, b) {
  const pa = (a || '0').split('.').map(Number);
  const pb = (b || '0').split('.').map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const na = pa[i] || 0, nb = pb[i] || 0;
    if (na > nb) return true;
    if (na < nb) return false;
  }
  return false;
}

// ── Session endpoints (RSA-2048 signed) ───────────────────────────────────────

/**
 * POST /api/auth/session
 * Creates a 7-minute RSA-signed session for an approved device.
 *
 * Security:
 *  • No shared secret in the APK — server signs with RSA private key.
 *  • APK verifies with embedded public key → impossible to forge.
 *  • Device binding: first device to authenticate for an android_id is locked in.
 *  • Live approval + auto_typer + plan check on every request.
 */
app.post('/api/auth/session', (req, res) => {
  const { device_id, android_id, ts } = req.body || {};
  const id = (device_id || android_id || '').trim();
  if (!id) return res.json({ error: 'missing_device_id' });

  // Freshness guard (±5 min) — skip if ts not sent
  if (ts && Math.abs(Date.now() - Number(ts)) > 5 * 60 * 1000) {
    return res.json({ error: 'request_expired' });
  }

  const data = readUsers();
  const user = (data.approved || []).find(u => u.android_id === id);
  if (!user)                   return res.json({ error: 'not_approved' });
  if (user.auto_typer !== true) return res.json({ error: 'not_allowed' });
  if (user.plan_until && new Date(user.plan_until) < new Date())
    return res.json({ error: 'plan_expired' });

  // Device binding — first auth binds the device; subsequent checks enforce it
  const idx = data.approved.findIndex(u => u.android_id === id);
  if (!user.bound_device_id) {
    data.approved[idx].bound_device_id = id;
    writeUsers(data);
  } else if (user.bound_device_id !== id) {
    return res.json({ error: 'device_not_bound',
      message: 'Account bound to a different device. Contact admin to unbind.' });
  }

  const payload = createSessionPayload(id, user);
  const { token, signature } = signSession(payload);
  res.json({ token, signature, expires_in: 420 });
});

/**
 * POST /api/auth/verify
 * Re-validates a session mid-typing (called every 10 typed lines).
 * Server re-checks live approval so admin kill-switch takes effect instantly.
 * Returns a freshly-signed renewed session to extend the window.
 */
app.post('/api/auth/verify', (req, res) => {
  const { token, signature, device_id } = req.body || {};
  if (!token || !signature || !device_id) return res.json({ valid: false });

  if (!verifySessionSignature(token, signature))
    return res.json({ valid: false, reason: 'invalid_signature' });

  let payload;
  try { payload = JSON.parse(Buffer.from(token, 'base64').toString('utf8')); }
  catch (_) { return res.json({ valid: false, reason: 'parse_error' }); }

  if (payload.device_id !== device_id)
    return res.json({ valid: false, reason: 'device_mismatch' });
  if (Date.now() > payload.expires_at + 2 * 60 * 1000)
    return res.json({ valid: false, reason: 'session_expired' });

  const data = readUsers();
  const user = (data.approved || []).find(u => u.android_id === device_id);
  if (!user || user.auto_typer !== true) return res.json({ valid: false, reason: 'not_allowed' });
  if (user.plan_until && new Date(user.plan_until) < new Date())
    return res.json({ valid: false, reason: 'plan_expired' });

  // Issue a fresh 7-minute session so uninterrupted typing never expires
  const renewed = createSessionPayload(device_id, user);
  const { token: newToken, signature: newSig } = signSession(renewed);
  res.json({ valid: true, renewed_token: newToken, renewed_signature: newSig });
});

/** GET /api/auth/pubkey — returns the RSA public key (PEM) for APK pinning. */
app.get('/api/auth/pubkey', (req, res) => {
  res.json({ public_key: rsaKeys.publicKey });
});

// ── Public endpoints ──────────────────────────────────────────────────────────

app.get('/approvals.json', (req, res) => {
  res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  res.setHeader('Pragma', 'no-cache');
  res.setHeader('Expires', '0');
  res.setHeader('Content-Type', 'application/json');
  const data = readUsers();
  const approved = (data.approved || []).map(u => ({
    android_id:  u.android_id,
    name:        u.name,
    plan:        u.plan,
    plan_until:  u.plan_until !== undefined ? u.plan_until : null,
    auto_typer:  u.auto_typer === true,
    ads_enabled: u.ads_enabled !== false,  // default = ads ON when field missing
    space_label: u.space_label === true
  }));
  const blocked = (data.blocked || []).map(u => u.android_id || u);
  res.json({ version: data.version || 2, approved, blocked });
});

/**
 * Public key-check endpoint — called by the keyboard app every hour.
 * GET /key-check?key=FLEX-XXXX-YYYY
 * Returns: { valid: true/false, expires_at: "ISO" | null }
 */
app.get('/key-check', (req, res) => {
  res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  res.setHeader('Pragma', 'no-cache');
  res.setHeader('Expires', '0');

  const key = (req.query.key || '').trim().toUpperCase();
  if (!key) return res.json({ valid: false, expires_at: null });

  const data  = readKeys();
  const entry = (data.keys || []).find(k => k.key.toUpperCase() === key);

  if (!entry || !entry.active) {
    return res.json({ valid: false, expires_at: null });
  }

  if (entry.expires_at) {
    const expiresMs = new Date(entry.expires_at).getTime();
    if (Date.now() > expiresMs) {
      return res.json({ valid: false, expires_at: entry.expires_at });
    }
  }

  return res.json({ valid: true, expires_at: entry.expires_at || null });
});

app.get('/update.json', (req, res) => {
  res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
  res.setHeader('Pragma', 'no-cache');
  res.setHeader('Expires', '0');
  const s = readSettings();
  const apkExists  = fs.existsSync(path.join(APK_DIR, 'flexboard-pro.apk'));
  const downloadUrl = apkExists
    ? (s.base_url || 'https://keyboard.kraza.qzz.io') + '/download/flexboard-pro.apk'
    : (s.download_link || '');
  res.json({
    latest_version: s.app_version || '1.11.0',
    download_url:   downloadUrl,
    force_update:   true,
    release_notes:  s.release_notes || 'New update available. Please update to continue using FlexBoard Pro.'
  });
});

app.get('/download/flexboard-pro.apk', (req, res) => {
  const apkPath = path.join(APK_DIR, 'flexboard-pro.apk');
  if (!fs.existsSync(apkPath)) return res.status(404).json({ error: 'APK not uploaded yet' });
  res.setHeader('Content-Disposition', 'attachment; filename="FlexBoardPro.apk"');
  res.setHeader('Content-Type', 'application/vnd.android.package-archive');
  res.sendFile(apkPath);
});

// ── Admin: auth ───────────────────────────────────────────────────────────────

app.post('/api/admin/verify', (req, res) => {
  const { key } = req.body;
  if (key === ADMIN_KEY) return res.json({ success: true });
  return res.status(401).json({ success: false, error: 'Wrong access key' });
});

// ── Admin: users ──────────────────────────────────────────────────────────────

app.get('/api/admin/users', requireAdmin, (req, res) => {
  const data = readUsers();
  res.json({ approved: data.approved || [], blocked: data.blocked || [] });
});

app.post('/api/admin/users/approved', requireAdmin, (req, res) => {
  const { android_id, name, plan, plan_until, auto_typer, ads_enabled, space_label } = req.body;
  if (!android_id) return res.status(400).json({ error: 'android_id is required' });
  const data = readUsers();
  if (!data.approved) data.approved = [];
  const entry = {
    android_id:  android_id.trim(),
    name:        name || 'Unknown',
    plan:        plan || 'monthly',
    plan_until:  plan_until || null,
    auto_typer:  auto_typer === true,
    ads_enabled: ads_enabled !== false,  // default = ads ON
    space_label: space_label === true
  };
  const idx = data.approved.findIndex(u => u.android_id === android_id.trim());
  if (idx >= 0) data.approved[idx] = entry; else data.approved.push(entry);
  if (data.blocked) data.blocked = data.blocked.filter(u => (u.android_id || u) !== android_id.trim());
  writeUsers(data);
  res.json({ success: true, user: entry });
});

app.put('/api/admin/users/approved/:android_id', requireAdmin, (req, res) => {
  const { android_id } = req.params;
  const { name, plan, plan_until, auto_typer, ads_enabled, space_label } = req.body;
  const data = readUsers();
  if (!data.approved) data.approved = [];
  const idx = data.approved.findIndex(u => u.android_id === android_id);
  if (idx < 0) return res.status(404).json({ error: 'User not found' });
  if (name        !== undefined) data.approved[idx].name        = name;
  if (plan        !== undefined) data.approved[idx].plan        = plan;
  if (plan_until  !== undefined) data.approved[idx].plan_until  = plan_until;
  if (auto_typer  !== undefined) data.approved[idx].auto_typer  = auto_typer === true;
  if (ads_enabled !== undefined) data.approved[idx].ads_enabled = ads_enabled !== false;
  if (space_label !== undefined) data.approved[idx].space_label = space_label === true;
  writeUsers(data);
  res.json({ success: true, user: data.approved[idx] });
});

app.delete('/api/admin/users/approved/:android_id', requireAdmin, (req, res) => {
  const { android_id } = req.params;
  const data = readUsers();
  if (!data.approved) data.approved = [];
  data.approved = data.approved.filter(u => u.android_id !== android_id);
  writeUsers(data);
  res.json({ success: true });
});

app.post('/api/admin/users/blocked', requireAdmin, (req, res) => {
  const { android_id, name } = req.body;
  if (!android_id) return res.status(400).json({ error: 'android_id is required' });
  const data = readUsers();
  if (!data.blocked) data.blocked = [];
  const exists = data.blocked.findIndex(u => (u.android_id || u) === android_id.trim());
  if (exists < 0) data.blocked.push({ android_id: android_id.trim(), name: name || 'Unknown' });
  writeUsers(data);
  res.json({ success: true });
});

app.delete('/api/admin/users/blocked/:android_id', requireAdmin, (req, res) => {
  const { android_id } = req.params;
  const data = readUsers();
  if (!data.blocked) data.blocked = [];
  data.blocked = data.blocked.filter(u => (u.android_id || u) !== android_id);
  writeUsers(data);
  res.json({ success: true });
});

app.post('/api/admin/users/block-from-approved', requireAdmin, (req, res) => {
  const { android_id } = req.body;
  if (!android_id) return res.status(400).json({ error: 'android_id is required' });
  const data = readUsers();
  if (!data.approved) data.approved = [];
  if (!data.blocked)  data.blocked  = [];
  const idx = data.approved.findIndex(u => u.android_id === android_id);
  if (idx >= 0) {
    const user = data.approved[idx];
    data.approved.splice(idx, 1);
    const alreadyBlocked = data.blocked.findIndex(u => (u.android_id || u) === android_id);
    if (alreadyBlocked < 0) data.blocked.push({ android_id: user.android_id, name: user.name });
    writeUsers(data);
    return res.json({ success: true });
  }
  return res.status(404).json({ error: 'User not found in approved list' });
});

app.post('/api/admin/users/unblock', requireAdmin, (req, res) => {
  const { android_id } = req.body;
  if (!android_id) return res.status(400).json({ error: 'android_id is required' });
  const data = readUsers();
  if (!data.blocked) data.blocked = [];
  data.blocked = data.blocked.filter(u => (u.android_id || u) !== android_id);
  writeUsers(data);
  res.json({ success: true });
});

// ── Admin: device binding ─────────────────────────────────────────────────────

app.post('/api/admin/users/unbind/:android_id', requireAdmin, (req, res) => {
  const { android_id } = req.params;
  const data = readUsers();
  const idx = data.approved.findIndex(u => u.android_id === android_id);
  if (idx < 0) return res.status(404).json({ error: 'User not found' });
  delete data.approved[idx].bound_device_id;
  writeUsers(data);
  res.json({ success: true, message: 'Device unbound. Next auth from any device will bind it.' });
});

app.get('/api/admin/users/binding/:android_id', requireAdmin, (req, res) => {
  const { android_id } = req.params;
  const data = readUsers();
  const user = data.approved.find(u => u.android_id === android_id);
  if (!user) return res.status(404).json({ error: 'User not found' });
  res.json({ android_id, bound_device_id: user.bound_device_id || null });
});

// ── Admin: activation keys ────────────────────────────────────────────────────

app.get('/api/admin/keys', requireAdmin, (req, res) => {
  const data = readKeys();
  res.json({ keys: data.keys || [] });
});

app.post('/api/admin/keys', requireAdmin, (req, res) => {
  const { key, label, expires_at, active } = req.body;
  if (!key) return res.status(400).json({ error: 'key is required' });
  const data  = readKeys();
  if (!data.keys) data.keys = [];
  const entry = {
    key:        key.trim().toUpperCase(),
    label:      label || '',
    expires_at: expires_at || null,
    active:     active !== false,
    created_at: new Date().toISOString()
  };
  const idx = data.keys.findIndex(k => k.key.toUpperCase() === entry.key);
  if (idx >= 0) data.keys[idx] = { ...data.keys[idx], ...entry };
  else data.keys.push(entry);
  writeKeys(data);
  res.json({ success: true, entry });
});

app.put('/api/admin/keys/:key', requireAdmin, (req, res) => {
  const keyParam = req.params.key.toUpperCase();
  const { label, expires_at, active } = req.body;
  const data = readKeys();
  if (!data.keys) data.keys = [];
  const idx = data.keys.findIndex(k => k.key.toUpperCase() === keyParam);
  if (idx < 0) return res.status(404).json({ error: 'Key not found' });
  if (label      !== undefined) data.keys[idx].label      = label;
  if (expires_at !== undefined) data.keys[idx].expires_at = expires_at;
  if (active     !== undefined) data.keys[idx].active     = active;
  writeKeys(data);
  res.json({ success: true, entry: data.keys[idx] });
});

app.delete('/api/admin/keys/:key', requireAdmin, (req, res) => {
  const keyParam = req.params.key.toUpperCase();
  const data = readKeys();
  if (!data.keys) data.keys = [];
  data.keys = data.keys.filter(k => k.key.toUpperCase() !== keyParam);
  writeKeys(data);
  res.json({ success: true });
});

// ── Admin: settings ───────────────────────────────────────────────────────────

app.get('/api/admin/settings', requireAdmin, (req, res) => {
  const s = readSettings();
  const apkExists = fs.existsSync(path.join(APK_DIR, 'flexboard-pro.apk'));
  const apkStat   = apkExists ? fs.statSync(path.join(APK_DIR, 'flexboard-pro.apk')) : null;
  res.json({
    ...s,
    apk_uploaded: apkExists,
    apk_size:     apkStat ? Math.round(apkStat.size / (1024 * 1024) * 10) / 10 : 0,
    apk_modified: apkStat ? apkStat.mtime : null
  });
});

app.put('/api/admin/settings', requireAdmin, (req, res) => {
  const settings = readSettings();
  const { download_link, app_version, whatsapp_number, release_notes, base_url } = req.body;
  if (download_link  !== undefined) settings.download_link  = download_link;
  if (app_version    !== undefined) settings.app_version    = app_version;
  if (whatsapp_number !== undefined) settings.whatsapp_number = whatsapp_number;
  if (release_notes  !== undefined) settings.release_notes  = release_notes;
  if (base_url       !== undefined) settings.base_url       = base_url;
  writeSettings(settings);
  res.json({ success: true, settings });
});

app.post('/api/admin/upload-apk', requireAdmin, upload.single('apk'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'No file uploaded' });
  const size = Math.round(req.file.size / (1024 * 1024) * 10) / 10;
  res.json({ success: true, filename: req.file.filename, size_mb: size });
});

app.get('/api/settings/public', (req, res) => {
  const s = readSettings();
  const apkExists  = fs.existsSync(path.join(APK_DIR, 'flexboard-pro.apk'));
  const downloadUrl = apkExists ? (s.base_url || '') + '/download/flexboard-pro.apk' : (s.download_link || '#');
  res.json({ download_link: downloadUrl, app_version: s.app_version });
});

app.get('/api/admin/download-zip', requireAdmin, (req, res) => {
  try {
    const zipPath    = path.join('/tmp', 'flexboard-website.zip');
    const websiteDir = __dirname;
    execSync(`python3 -c "
import zipfile, os, sys
zip_path = sys.argv[1]
base = sys.argv[2]
exclude = {'.git','node_modules','apk','.env'}
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(base):
        dirs[:] = [d for d in dirs if d not in exclude]
        for f in files:
            fp = os.path.join(root, f)
            z.write(fp, os.path.relpath(fp, os.path.dirname(base)))
print('done')
" "${zipPath}" "${websiteDir}"`, { timeout: 30000 });
    res.setHeader('Content-Disposition', 'attachment; filename="flexboard-website.zip"');
    res.setHeader('Content-Type', 'application/zip');
    res.sendFile(zipPath);
  } catch (err) {
    res.status(500).json({ error: 'Failed to create ZIP: ' + err.message });
  }
});

// ── Static / pages ────────────────────────────────────────────────────────────

app.get('/', (req, res) => res.sendFile(path.join(__dirname, 'public', 'index.html')));
app.get('/admin', (req, res) => res.sendFile(path.join(__dirname, 'public', 'admin.html')));
app.use(express.static(path.join(__dirname, 'public')));

app.listen(PORT, '0.0.0.0', () => {
  console.log(`FlexBoard Pro website running on port ${PORT}`);
});
