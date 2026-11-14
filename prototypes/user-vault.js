/* User profile vault · docs/frozen-user-profile.md
 * 2026-11-12 freeze + 2026-11-12 design pass: japa daily/Sankalpas table. */
(() => {
  'use strict';

  const STORAGE_KEY = 'madhvanidhi-user-vault';
  const DEVICE_KEY = 'madhvanidhi-device-key';
  const FORMAT = 'madhvanidhi-user-backup';
  const VERSION = 1;
  const ITERATIONS = 100000;
  const enc = new TextEncoder();
  const dec = new TextDecoder();

  const DIKSHA_MANTRAS = [
    { id: 'gayatri', label: 'ಗಾಯತ್ರೀ · Gayatri' },
    { id: 'narasimha', label: 'ನರಸಿಂಹ · Narasimha' },
    { id: 'rama', label: 'ರಾಮ · Rama' },
    { id: 'krishna', label: 'ಕೃಷ್ಣ · Krishna' },
    { id: 'hanuman', label: 'ಹನುಮಾನ್ · Hanuman' },
    { id: 'mrityunjaya', label: 'ಮೃತ್ಯುಂಜಯ · Mrityunjaya' },
    { id: 'venkatesha', label: 'ವೆಂಕಟೇಶ · Venkatesha' },
    { id: 'raghavendra', label: 'ರಾಘವೇಂದ್ರ · Raghavendra' },
    { id: 'sudarshana', label: 'ಸುದರ್ಶನ · Sudarshana' },
    { id: 'hayagriva', label: 'ಹಯಗ್ರೀವ · Hayagriva' }
  ];

  /* Daily · 11 → 5,001 · Major · 5,001 → 10,00,001 (Indian grouping, ascending) */
  const DAILY_TARGETS = [11, 101, 1001, 5001];
  const MAJOR_TARGETS = [5001, 10001, 50001, 100001, 1000001];
  const DEFAULT_DAILY = 101;
  const DEFAULT_MAJOR = 0;
  function sankalpa(explicit, total) {
    if (Number(explicit) > 0) return Number(explicit);
    return MAJOR_TARGETS.find(level => level > total)
      || MAJOR_TARGETS[MAJOR_TARGETS.length - 1];
  }

  /** Indian numbering: 50,001 · 1,00,001 · 10,00,001 (hundreds / thousand / lakh / crore) */
  function formatIndianCount(value) {
    const n = Math.trunc(Math.abs(Number(value) || 0));
    const s = String(n);
    if (s.length <= 3) return s;
    const last3 = s.slice(-3);
    let rest = s.slice(0, -3);
    const groups = [];
    while (rest.length > 2) {
      groups.unshift(rest.slice(-2));
      rest = rest.slice(0, -2);
    }
    if (rest) groups.unshift(rest);
    return `${groups.join(',')},${last3}`;
  }

  const MARITAL_OPTIONS = ['unmarried', 'married'];

  const LANG_KEY = 'madhvanidhi-language';
  const LANG_OPTIONS = ['ಕನ್ನಡ', 'संस्कृतम्', 'English'];

  const RASHI_OPTIONS = [
    'ಮೇಷ', 'ವೃಷಭ', 'ಮಿಥುನ', 'ಕರ್ಕಾಟಕ', 'ಸಿಂಹ', 'ಕನ್ಯಾ',
    'ತುಲಾ', 'ವೃಶ್ಚಿಕ', 'ಧನು', 'ಮಕರ', 'ಕುಂಭ', 'ಮೀನ'
  ];

  const NAKSHATRA_OPTIONS = [
    'ಅಶ್ವಿನಿ', 'ಭರಣಿ', 'ಕೃತ್ತಿಕಾ', 'ರೋಹಿಣಿ', 'ಮೃಗಶಿರ', 'ಆರ್ದ್ರಾ',
    'ಪುನರ್ವಸು', 'ಪುಷ್ಯ', 'ಆಶ್ಲೇಷಾ', 'ಮಘಾ', 'ಪೂರ್ವ ಫಲ್ಗುಣಿ', 'ಉತ್ತರ ಫಲ್ಗುಣಿ',
    'ಹಸ್ತ', 'ಚಿತ್ರಾ', 'ಸ್ವಾತಿ', 'ವಿಶಾಖಾ', 'ಅನುರಾಧಾ', 'ಜ್ಯೇಷ್ಠ',
    'ಮೂಲ', 'ಪೂರ್ವಾಷಾಢಾ', 'ಉತ್ತರಾಷಾಢಾ', 'ಶ್ರವಣ', 'ಧನಿಷ್ಠಾ', 'ಶತಭಿಷಾ',
    'ಪೂರ್ವ ಭಾದ್ರಪದಾ', 'ಉತ್ತರ ಭಾದ್ರಪದಾ', 'ರೇವತಿ'
  ];

  const DEFAULTS = {
    name: 'ರಾಮಚಂದ್ರ',
    gotra: 'ಭಾರದ್ವಾಜ',
    marital: 'married',
    rashi: 'ಮೇಷ',
    nakshatra: 'ರೋಹಿಣಿ',
    veda: 'Rigveda — Ashvalayana',
    matha: 'Sri Raghavendra Swamy Matha',
    dikshaMantras: ['gayatri'],
    japaTargets: { gayatri: { daily: DEFAULT_DAILY, major: DEFAULT_MAJOR } }
  };

  function pickTarget(value, allowed, fallback) {
    const n = Number(value);
    return allowed.includes(n) ? n : fallback;
  }

  function normalizeJapaTargets(raw, mantraIds) {
    const ids = normalizeDiksha(mantraIds);
    const src = raw && typeof raw === 'object' ? raw : {};
    const out = {};
    ids.forEach((id) => {
      const row = src[id] || {};
      out[id] = {
        daily: pickTarget(row.daily, DAILY_TARGETS, DEFAULT_DAILY),
        major: pickTarget(row.major, MAJOR_TARGETS, DEFAULT_MAJOR)
      };
    });
    return out;
  }

  function b64encode(buffer) {
    const bytes = buffer instanceof ArrayBuffer ? new Uint8Array(buffer) : buffer;
    let binary = '';
    bytes.forEach((b) => { binary += String.fromCharCode(b); });
    return btoa(binary);
  }

  function b64decode(value) {
    const binary = atob(value);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
    return bytes;
  }

  async function importAesKey(raw) {
    return crypto.subtle.importKey('raw', raw, 'AES-GCM', false, ['encrypt', 'decrypt']);
  }

  async function getDeviceKey() {
    try {
      const existing = localStorage.getItem(DEVICE_KEY);
      if (existing) return importAesKey(b64decode(existing));
      const raw = crypto.getRandomValues(new Uint8Array(32));
      localStorage.setItem(DEVICE_KEY, b64encode(raw));
      return importAesKey(raw);
    } catch {
      throw new Error('Local encrypted storage unavailable');
    }
  }

  async function deriveBackupKey(passphrase, salt) {
    const base = await crypto.subtle.importKey('raw', enc.encode(passphrase), 'PBKDF2', false, ['deriveKey']);
    return crypto.subtle.deriveKey(
      { name: 'PBKDF2', salt, iterations: ITERATIONS, hash: 'SHA-256' },
      base,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt']
    );
  }

  async function encryptJson(key, data) {
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const ciphertext = await crypto.subtle.encrypt(
      { name: 'AES-GCM', iv },
      key,
      enc.encode(JSON.stringify(data))
    );
    return { iv: b64encode(iv), ciphertext: b64encode(ciphertext) };
  }

  async function decryptJson(key, ivB64, cipherB64) {
    const plain = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: b64decode(ivB64) },
      key,
      b64decode(cipherB64)
    );
    return JSON.parse(dec.decode(plain));
  }

  function normalizeDiksha(list) {
    const allowed = new Set(DIKSHA_MANTRAS.map((item) => item.id));
    const values = Array.isArray(list) ? list : String(list || '').split(',').map((v) => v.trim()).filter(Boolean);
    const unique = [...new Set(values.filter((id) => allowed.has(id)))];
    return unique.length ? unique : [...DEFAULTS.dikshaMantras];
  }

  function normalizeMarital(value) {
    const key = String(value || '').trim().toLowerCase();
    return MARITAL_OPTIONS.includes(key) ? key : DEFAULTS.marital;
  }

  function normalizeChoice(value, options, fallback) {
    const key = String(value || '').trim();
    return options.includes(key) ? key : fallback;
  }

  function normalizeLanguage(value) {
    const lang = String(value || '').trim();
    return LANG_OPTIONS.includes(lang) ? lang : LANG_OPTIONS[0];
  }

  function loadLanguage() {
    try {
      return normalizeLanguage(localStorage.getItem(LANG_KEY));
    } catch {
      return LANG_OPTIONS[0];
    }
  }

  function saveLanguage(value) {
    const lang = normalizeLanguage(value);
    try {
      localStorage.setItem(LANG_KEY, lang);
    } catch {}
    return lang;
  }

  function bindLanguageSelect(select) {
    if (!select) return;
    select.value = loadLanguage();
    select.addEventListener('change', () => {
      saveLanguage(select.value);
    });
  }

  function normalizeProfile(input) {
    const profile = { ...DEFAULTS, ...(input || {}) };
    // Legacy backups may still carry `lang` — apply into Settings storage on restore.
    if (profile.lang) saveLanguage(profile.lang);
    const dikshaMantras = normalizeDiksha(profile.dikshaMantras);
    return {
      name: String(profile.name || '').trim() || DEFAULTS.name,
      gotra: String(profile.gotra || DEFAULTS.gotra),
      marital: normalizeMarital(profile.marital),
      rashi: normalizeChoice(profile.rashi, RASHI_OPTIONS, DEFAULTS.rashi),
      nakshatra: normalizeChoice(profile.nakshatra, NAKSHATRA_OPTIONS, DEFAULTS.nakshatra),
      veda: String(profile.veda || DEFAULTS.veda),
      matha: String(profile.matha || DEFAULTS.matha),
      dikshaMantras,
      japaTargets: normalizeJapaTargets(profile.japaTargets, dikshaMantras)
    };
  }

  function dikshaLabels(ids) {
    const map = Object.fromEntries(DIKSHA_MANTRAS.map((item) => [item.id, item.label]));
    return normalizeDiksha(ids).map((id) => map[id] || id);
  }

  function readDikshaFromForm(form) {
    const checks = form?.querySelectorAll('input[name="dikshaMantras"]');
    if (checks && checks.length) {
      return Array.from(checks).filter((el) => el.checked).map((el) => el.value);
    }
    const select = form?.elements?.dikshaMantras;
    if (select?.selectedOptions) {
      return Array.from(select.selectedOptions).map((option) => option.value);
    }
    return [];
  }

  function syncFluidMultiSelect(root, ids) {
    if (!root) return;
    const selected = new Set(normalizeDiksha(ids));
    const labels = [];
    root.querySelectorAll('.cds--list-box__menu-item[data-value]').forEach((item) => {
      const value = item.dataset.value;
      const on = selected.has(value);
      item.setAttribute('aria-selected', String(on));
      item.classList.toggle('cds--list-box__menu-item--active', on);
      const checkbox = item.querySelector('input[type="checkbox"]');
      if (checkbox) checkbox.checked = on;
      if (on) {
        const text = item.querySelector('.cds--checkbox__label-text')?.textContent || value;
        labels.push(String(text).split(' · ')[0].trim());
      }
    });
    const count = root.querySelector('[data-fluid-count]');
    const countValue = root.querySelector('[data-fluid-count-value]');
    const placeholder = root.querySelector('[data-fluid-placeholder]');
    if (countValue) countValue.textContent = String(labels.length);
    if (count) count.hidden = labels.length === 0;
    if (placeholder) {
      placeholder.textContent = labels.length
        ? labels.join(', ')
        : 'Choose mantras for japa';
    }
  }

  function setFluidMenuOpen(root, open) {
    if (!root) return;
    const box = root.querySelector('[data-fluid-box]');
    const trigger = root.querySelector('[data-fluid-trigger]');
    const menu = root.querySelector('[data-fluid-menu]');
    if (box) box.classList.toggle('cds--list-box--expanded', open);
    if (trigger) trigger.setAttribute('aria-expanded', String(open));
    if (menu) menu.hidden = !open;
  }

  function bindFluidMultiSelect(form) {
    const root = form?.querySelector('[data-fluid-multiselect="diksha"]');
    if (!root || root.dataset.bound === '1') return root;
    root.dataset.bound = '1';
    const trigger = root.querySelector('[data-fluid-trigger]');
    const menu = root.querySelector('[data-fluid-menu]');

    const refresh = () => syncFluidMultiSelect(root, readDikshaFromForm(form));

    trigger?.addEventListener('click', (event) => {
      event.preventDefault();
      if (form.querySelector('#user-profile-fields')?.disabled) return;
      const open = trigger.getAttribute('aria-expanded') === 'true';
      setFluidMenuOpen(root, !open);
    });
    trigger?.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        trigger.click();
      } else if (event.key === 'Escape') {
        setFluidMenuOpen(root, false);
      }
    });
    menu?.addEventListener('change', (event) => {
      if (event.target?.name === 'dikshaMantras') {
        refresh();
        syncJapaTargetsTable(form);
      }
    });
    menu?.addEventListener('click', (event) => {
      const item = event.target.closest('.cds--list-box__menu-item');
      if (!item || event.target.closest('input, label')) return;
      const checkbox = item.querySelector('input[type="checkbox"]');
      if (!checkbox || checkbox.disabled) return;
      checkbox.checked = !checkbox.checked;
      checkbox.dispatchEvent(new Event('change', { bubbles: true }));
    });
    document.addEventListener('click', (event) => {
      if (!root.contains(event.target)) setFluidMenuOpen(root, false);
    });
    refresh();
    return root;
  }

  function writeDikshaToForm(form, ids) {
    const selected = normalizeDiksha(ids);
    const checks = form?.querySelectorAll('input[name="dikshaMantras"]');
    if (checks && checks.length) {
      const set = new Set(selected);
      checks.forEach((el) => {
        el.checked = set.has(el.value);
      });
      syncFluidMultiSelect(form.querySelector('[data-fluid-multiselect="diksha"]'), selected);
      return;
    }
    const select = form?.elements?.dikshaMantras;
    if (select?.options) {
      const set = new Set(selected);
      Array.from(select.options).forEach((option) => {
        option.selected = set.has(option.value);
      });
    }
  }

  function readJapaTargetsFromForm(form, mantraIds) {
    const ids = normalizeDiksha(mantraIds ?? readDikshaFromForm(form));
    const prev = {};
    form?.querySelectorAll('[data-japa-daily][data-japa-id]').forEach((el) => {
      const id = el.dataset.japaId;
      if (!prev[id]) prev[id] = {};
      prev[id].daily = el.value;
    });
    form?.querySelectorAll('[data-japa-major][data-japa-id]').forEach((el) => {
      const id = el.dataset.japaId;
      if (!prev[id]) prev[id] = {};
      prev[id].major = el.value;
    });
    return normalizeJapaTargets(prev, ids);
  }

  function optionList(values, selected) {
    return values.map((n) => {
      const opt = document.createElement('option');
      opt.value = String(n);
      opt.textContent = formatIndianCount(n);
      if (n === selected) opt.selected = true;
      return opt;
    });
  }

  function syncJapaTargetsTable(form, targets) {
    const body = form?.querySelector('[data-japa-targets-body]');
    const block = form?.querySelector('[data-japa-targets-block]');
    if (!body) return;
    const ids = normalizeDiksha(readDikshaFromForm(form));
    const current = normalizeJapaTargets(
      targets || readJapaTargetsFromForm(form, ids),
      ids
    );
    const labelMap = Object.fromEntries(DIKSHA_MANTRAS.map((item) => [item.id, item.label]));
    body.replaceChildren();
    ids.forEach((id) => {
      const row = document.createElement('tr');
      const nameTd = document.createElement('td');
      nameTd.textContent = (labelMap[id] || id).split(' · ')[0];

      const dailyTd = document.createElement('td');
      const dailyWrap = document.createElement('div');
      dailyWrap.className = 'cds--select cds--select--inline';
      const dailySelect = document.createElement('select');
      dailySelect.className = 'cds--select-input';
      dailySelect.dataset.japaDaily = '';
      dailySelect.dataset.japaId = id;
      dailySelect.setAttribute('aria-label', `${nameTd.textContent} daily target`);
      dailySelect.append(...optionList(DAILY_TARGETS, current[id].daily));
      dailyWrap.append(dailySelect);
      dailyTd.append(dailyWrap);

      const majorTd = document.createElement('td');
      const majorWrap = document.createElement('div');
      majorWrap.className = 'cds--select cds--select--inline';
      const majorSelect = document.createElement('select');
      majorSelect.className = 'cds--select-input';
      majorSelect.dataset.japaMajor = '';
      majorSelect.dataset.japaId = id;
      majorSelect.setAttribute('aria-label', `${nameTd.textContent} Sankalpa`);
      const total = window.madhvaJapaSession.getCumulative(id);
      majorSelect.append(new Option('Auto · ' + formatIndianCount(sankalpa(0, total)), '0', false, !current[id].major), ...optionList(MAJOR_TARGETS, current[id].major));
      majorWrap.append(majorSelect);
      majorTd.append(majorWrap);

      const totalCell = document.createElement('td');
      totalCell.textContent = formatIndianCount(total);
      row.append(nameTd, dailyTd, majorTd, totalCell);
      body.append(row);
    });
    if (block) block.hidden = ids.length === 0;
    renderJapaTargetsView(form, { dikshaMantras: ids, japaTargets: current });
  }

  function renderJapaTargetsView(root, profile) {
    const host = root?.querySelector?.('[data-view-japa-targets]');
    if (!host) return;
    const data = normalizeProfile(profile);
    host.replaceChildren();
    if (!data.dikshaMantras.length) {
      host.textContent = '—';
      return;
    }
    const table = document.createElement('table');
    table.className = 'cds--data-table cds--data-table--sm cds--data-table--zebra';
    table.innerHTML = '<thead><tr><th>Japa</th><th>Daily</th><th>Sankalpa</th><th>Cumulative</th></tr></thead>';
    const tbody = document.createElement('tbody');
    const labelMap = Object.fromEntries(DIKSHA_MANTRAS.map((item) => [item.id, item.label]));
    data.dikshaMantras.forEach((id) => {
      const tr = document.createElement('tr');
      const t = data.japaTargets[id] || { daily: DEFAULT_DAILY, major: DEFAULT_MAJOR };
      tr.innerHTML = `<td>${(labelMap[id] || id).split(' · ')[0]}</td><td>${formatIndianCount(t.daily)}</td><td>${t.major ? "" : "Auto · "}${formatIndianCount(sankalpa(t.major, window.madhvaJapaSession.getCumulative(id)))}</td><td>${formatIndianCount(window.madhvaJapaSession.getCumulative(id))}</td>`;
      tbody.append(tr);
    });
    table.append(tbody);
    host.append(table);
  }

  function readForm(form) {
    if (!form) return normalizeProfile();
    const dikshaMantras = readDikshaFromForm(form);
    return normalizeProfile({
      name: form.elements.name?.value,
      gotra: form.elements.gotra?.value,
      marital: form.elements.marital?.value,
      rashi: form.elements.rashi?.value,
      nakshatra: form.elements.nakshatra?.value,
      veda: form.elements.veda?.value,
      matha: form.elements.matha?.value,
      dikshaMantras,
      japaTargets: readJapaTargetsFromForm(form, dikshaMantras)
    });
  }

  function maritalLabel(value) {
    return normalizeMarital(value) === 'married' ? 'Married · ವಿವಾಹಿತ' : 'Unmarried · ಅವಿವಾಹಿತ';
  }

  function renderProfileView(root, profile) {
    const view = root?.querySelector?.('[data-profile-view]') || root;
    if (!view?.querySelector) return;
    const data = normalizeProfile(profile);
    const values = {
      name: data.name,
      gotra: data.gotra,
      marital: maritalLabel(data.marital),
      rashi: data.rashi,
      nakshatra: data.nakshatra,
      veda: data.veda,
      matha: data.matha,
      dikshaMantras: dikshaLabels(data.dikshaMantras).map((label) => label.split(' · ')[0]).join(', ')
    };
    Object.entries(values).forEach(([key, value]) => {
      const el = view.querySelector(`[data-view-field="${key}"]`);
      if (el) el.textContent = value || '—';
    });
    renderJapaTargetsView(view, data);
  }

  function writeForm(form, profile) {
    if (!form) return;
    const data = normalizeProfile(profile);
    if (form.elements.name) form.elements.name.value = data.name;
    if (form.elements.gotra) form.elements.gotra.value = data.gotra;
    if (form.elements.marital) form.elements.marital.value = data.marital;
    if (form.elements.rashi) form.elements.rashi.value = data.rashi;
    if (form.elements.nakshatra) form.elements.nakshatra.value = data.nakshatra;
    if (form.elements.veda) form.elements.veda.value = data.veda;
    if (form.elements.matha) form.elements.matha.value = data.matha;
    writeDikshaToForm(form, data.dikshaMantras);
    syncJapaTargetsTable(form, data.japaTargets);
    renderProfileView(form, data);
  }

  function fillJapaMantraSelect(select, profile) {
    if (!select) return;
    const data = normalizeProfile(profile);
    const previous = select.value;
    select.replaceChildren();
    dikshaLabels(data.dikshaMantras).forEach((label, index) => {
      const option = document.createElement('option');
      option.value = data.dikshaMantras[index];
      option.textContent = label.split(' · ')[0];
      select.append(option);
    });
    if (data.dikshaMantras.includes(previous)) select.value = previous;
  }

  async function saveLocal(profile) {
    const key = await getDeviceKey();
    const sealed = await encryptJson(key, normalizeProfile(profile));
    const payload = {
      format: FORMAT,
      version: VERSION,
      scope: 'device',
      alg: 'AES-GCM',
      ...sealed,
      updatedAt: new Date().toISOString()
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
    return payload.updatedAt;
  }

  async function loadLocal() {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const payload = JSON.parse(raw);
    if (!payload?.ciphertext || !payload?.iv) return null;
    const key = await getDeviceKey();
    return normalizeProfile(await decryptJson(key, payload.iv, payload.ciphertext));
  }

  async function createBackupBlob(profile, passphrase) {
    if (!passphrase || passphrase.length < 4) {
      throw new Error('Enter a backup passphrase (min 4 characters)');
    }
    const salt = crypto.getRandomValues(new Uint8Array(16));
    const key = await deriveBackupKey(passphrase, salt);
    const sealed = await encryptJson(key, normalizeProfile(profile));
    const document = {
      format: FORMAT,
      version: VERSION,
      scope: 'portable',
      alg: 'AES-GCM',
      kdf: 'PBKDF2',
      iterations: ITERATIONS,
      salt: b64encode(salt),
      ...sealed,
      createdAt: new Date().toISOString()
    };
    return new Blob([JSON.stringify(document, null, 2)], { type: 'application/json' });
  }

  function downloadBackup(blob) {
    const stamp = new Date().toISOString().slice(0, 10);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `madhvanidhi-user-${stamp}.mnbackup`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }

  async function restoreFromFile(file, passphrase) {
    if (!file) throw new Error('Choose a backup file');
    if (!passphrase || passphrase.length < 4) {
      throw new Error('Enter the backup passphrase');
    }
    const text = await file.text();
    const document = JSON.parse(text);
    if (document.format !== FORMAT || !document.salt || !document.iv || !document.ciphertext) {
      throw new Error('Not a MadhvaNidhi user backup file');
    }
    const key = await deriveBackupKey(passphrase, b64decode(document.salt));
    const profile = normalizeProfile(await decryptJson(key, document.iv, document.ciphertext));
    await saveLocal(profile);
    return profile;
  }

  function setStatus(el, message, tone) {
    if (!el) return;
    el.textContent = message || '';
    el.dataset.tone = tone || 'info';
  }

  function bindProfile(options) {
    const form = options.form;
    const status = options.status;
    const passphrase = options.passphrase;
    const passphraseConfirm = options.passphraseConfirm;
    const backupBtn = options.backupBtn;
    const restoreBtn = options.restoreBtn;
    const fileInput = options.fileInput;
    const editBtn = options.editBtn || form?.querySelector('#user-edit-btn');
    const saveBtn = options.saveBtn || form?.querySelector('#user-save-btn');
    const cancelBtn = options.cancelBtn || form?.querySelector('#user-cancel-btn');
    const fields = options.fields || form?.querySelector('#user-profile-fields');
    const view = options.view || form?.querySelector('[data-profile-view]');
    if (!form) return { ready: Promise.resolve() };

    let editing = false;
    let snapshot = normalizeProfile(DEFAULTS);

    function setEditing(next) {
      editing = !!next;
      if (fields) {
        fields.hidden = !editing;
        fields.disabled = !editing;
      }
      if (view) view.hidden = editing;
      if (editBtn) editBtn.hidden = editing;
      if (saveBtn) saveBtn.hidden = !editing;
      if (cancelBtn) cancelBtn.hidden = !editing;
      form.dataset.editing = String(editing);
      if (!editing) renderProfileView(form, readForm(form));
    }

    function captureSnapshot() {
      snapshot = readForm(form);
      return snapshot;
    }

    bindFluidMultiSelect(form);

    const accordion = document.querySelector('[data-accordion].user-vault') || form?.closest('.view, .page')?.querySelector('[data-accordion].user-vault');
    const accordionItem = accordion?.querySelector('[data-accordion-item]');
    const accordionHeading = accordion?.querySelector('[data-accordion-heading]');
    const accordionPanel = accordion?.querySelector('[data-accordion-panel]');
    accordionHeading?.addEventListener('click', () => {
      const open = accordionHeading.getAttribute('aria-expanded') === 'true';
      const next = !open;
      accordionHeading.setAttribute('aria-expanded', String(next));
      accordionItem?.classList.toggle('cds--accordion__item--active', next);
      if (accordionPanel) accordionPanel.hidden = !next;
    });

    editBtn?.addEventListener('click', () => {
      captureSnapshot();
      setEditing(true);
      setStatus(status, 'Editing profile · Save to encrypt, or Cancel', 'info');
      form.querySelector('input:not([type="checkbox"]), select, [data-fluid-trigger]')?.focus();
    });

    saveBtn?.addEventListener('click', async () => {
      try {
        const when = await saveLocal(readForm(form));
        captureSnapshot();
        setFluidMenuOpen(form.querySelector('[data-fluid-multiselect="diksha"]'), false);
        setEditing(false);
        form.dispatchEvent(new Event('change', { bubbles: true }));
        window.dispatchEvent(new Event('japa-profile-saved'));
        setStatus(status, `Saved · encrypted locally · ${new Date(when).toLocaleString()}`, 'ok');
      } catch (err) {
        setStatus(status, err.message || 'Could not save locally', 'error');
      }
    });

    cancelBtn?.addEventListener('click', () => {
      writeForm(form, snapshot);
      setFluidMenuOpen(form.querySelector('[data-fluid-multiselect="diksha"]'), false);
      setEditing(false);
      setStatus(status, 'Edit cancelled · showing saved profile', 'info');
    });

    backupBtn?.addEventListener('click', async () => {
      try {
        if (editing) throw new Error('Save or Cancel profile edits before creating a backup');
        const pass = passphrase?.value || '';
        const confirm = passphraseConfirm?.value || '';
        if (passphraseConfirm && pass !== confirm) {
          throw new Error('Passphrases do not match');
        }
        const blob = await createBackupBlob(readForm(form), pass);
        downloadBackup(blob);
        setStatus(status, 'Backup file downloaded · move to new phone and Restore', 'ok');
      } catch (err) {
        setStatus(status, err.message || 'Backup failed', 'error');
      }
    });

    restoreBtn?.addEventListener('click', () => {
      if (editing) {
        setStatus(status, 'Save or Cancel profile edits before restore', 'error');
        return;
      }
      fileInput?.click();
    });
    fileInput?.addEventListener('change', async () => {
      const file = fileInput.files && fileInput.files[0];
      fileInput.value = '';
      try {
        const profile = await restoreFromFile(file, passphrase?.value || '');
        writeForm(form, profile);
        captureSnapshot();
        setEditing(false);
        form.dispatchEvent(new Event('change', { bubbles: true }));
        window.dispatchEvent(new Event('japa-profile-saved'));
        setStatus(status, 'Restored from backup · encrypted copy saved on this device', 'ok');
      } catch (err) {
        setStatus(status, err.message || 'Restore failed', 'error');
      }
    });

    const ready = (async () => {
      try {
        const existing = await loadLocal();
        if (existing) {
          writeForm(form, existing);
          captureSnapshot();
          setStatus(status, 'Loaded encrypted local profile', 'ok');
        } else {
          writeForm(form, DEFAULTS);
          await saveLocal(DEFAULTS);
          captureSnapshot();
          setStatus(status, 'Encrypted local profile created', 'info');
        }
      } catch (err) {
        writeForm(form, DEFAULTS);
        captureSnapshot();
        setStatus(status, err.message || 'Vault unavailable · using session only', 'error');
      }
      setEditing(false);
    })();

    return {
      ready,
      saveLocal: () => saveLocal(readForm(form)),
      readForm: () => readForm(form),
      setEditing
    };
  }

  window.madhvaUserVault = {
    DEFAULTS,
    DIKSHA_MANTRAS,
    DAILY_TARGETS,
    MAJOR_TARGETS,
    sankalpa,
    DEFAULT_DAILY,
    DEFAULT_MAJOR,
    formatIndianCount,
    RASHI_OPTIONS,
    NAKSHATRA_OPTIONS,
    LANG_OPTIONS,
    readForm,
    writeForm,
    fillJapaMantraSelect,
    dikshaLabels,
    normalizeJapaTargets,
    syncJapaTargetsTable,
    renderJapaTargetsView,
    loadLanguage,
    saveLanguage,
    bindLanguageSelect,
    saveLocal,
    loadLocal,
    createBackupBlob,
    downloadBackup,
    restoreFromFile,
    bindProfile
  };
})();
