/* Japa session helper · mock local persistence · diksha + profile targets sync */
(() => {
  'use strict';

  const STORAGE_KEY = 'madhvanidhi-japa-session';
  const CUMULATIVE_KEY = 'madhvanidhi-japa-cumulative';
  const MALA_SIZE = 108;
  const DEFAULT_DAILY = 101;
  const DEFAULT_MAJOR = 0;

  const STEPS = {
    poorvanga: {
      id: 'poorvanga',
      label: 'ಪೂರ್ವಾಂಗ',
      title: 'ಪೂರ್ವಾಂಗ',
      body: 'Sankalpa and preparatory angas before the main mantra count. Sample text — catalogue binding later.'
    },
    uttaranga: {
      id: 'uttaranga',
      label: 'ಉತ್ತರಾಂಗ',
      title: 'ಉತ್ತರಾಂಗ',
      body: 'Closing angas after the main count. Mark complete when the day’s japa is finished.'
    }
  };

  function todayKey() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  function defaultSession() {
    return {
      date: todayKey(),
      count: 0,
      mantra: 'gayatri',
      step: 'poorvanga',
      dailyTarget: DEFAULT_DAILY,
      majorTarget: DEFAULT_MAJOR,
      lastDelta: 0
    };
  }

  function loadCumulative() {
    try {
      return JSON.parse(localStorage.getItem(CUMULATIVE_KEY) || '{}') || {};
    } catch {
      return {};
    }
  }

  function saveCumulative(map) {
    try {
      localStorage.setItem(CUMULATIVE_KEY, JSON.stringify(map));
    } catch {}
  }

  function getCumulative(mantra) {
    const map = loadCumulative();
    return Math.max(0, Number(map[mantra]) || 0);
  }

  function addCumulative(mantra, delta) {
    if (!delta) return getCumulative(mantra);
    const map = loadCumulative();
    map[mantra] = Math.max(0, (Number(map[mantra]) || 0) + delta);
    saveCumulative(map);
    return map[mantra];
  }

  function loadSession() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return defaultSession();
      const data = JSON.parse(raw);
      const step = STEPS[data?.step] ? data.step : 'poorvanga';
      if (!data || data.date !== todayKey()) {
        return {
          ...defaultSession(),
          mantra: data?.mantra || 'gayatri',
          dailyTarget: Number(data?.dailyTarget) || DEFAULT_DAILY,
          majorTarget: Number(data?.majorTarget) || DEFAULT_MAJOR,
          step
        };
      }
      return {
        ...defaultSession(),
        ...data,
        count: Math.max(0, Number(data.count) || 0),
        dailyTarget: Math.max(1, Number(data.dailyTarget) || DEFAULT_DAILY),
        majorTarget: Number(data.majorTarget) || DEFAULT_MAJOR,
        step
      };
    } catch {
      return defaultSession();
    }
  }

  function dayRecords() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY + '-days') || '{}');
      return saved.date === todayKey() ? saved.records || {} : {};
    } catch { return {}; }
  }

  function saveSession(session) {
    try {
      const records = dayRecords();
      records[session.mantra] = { count: session.count, dailyTarget: session.dailyTarget };
      localStorage.setItem(STORAGE_KEY + '-days', JSON.stringify({date: session.date, records}));
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {}
  }

  function progressStats(count, dailyTarget, cumulative, majorTarget) {
    const daily = Math.max(1, dailyTarget || DEFAULT_DAILY);
    const major = window.madhvaUserVault.sankalpa(majorTarget, cumulative);
    const malas = Math.floor(count / MALA_SIZE);
    const dailyPct = Math.min(100, Math.round((count / daily) * 100));
    const majorPct = Math.min(100, Math.round((cumulative / major) * 100));
    return { malas, daily, major, dailyPct, majorPct, cumulative };
  }

  function formatCount(value) {
    return window.madhvaUserVault?.formatIndianCount?.(value) || String(value);
  }

  /** e.g. 01/101 · pad current to daily digit width (min 2) */
  function formatProgressPair(count, dailyTarget) {
    const daily = Math.max(1, Number(dailyTarget) || DEFAULT_DAILY);
    const width = Math.max(2, String(daily).length);
    const current = String(Math.max(0, Number(count) || 0)).padStart(width, '0');
    return {
      current,
      daily: String(daily),
      dailyLabel: formatCount(daily),
      text: `${current}/${daily}`
    };
  }

  function renderProgressPair(el, count, dailyTarget) {
    if (!el) return;
    const pair = formatProgressPair(count, dailyTarget);
    el.replaceChildren();
    const done = document.createElement('span');
    done.className = 'japa-count-tile__done';
    done.textContent = pair.current;
    const sep = document.createElement('span');
    sep.className = 'japa-count-tile__sep';
    sep.textContent = '/';
    const target = document.createElement('span');
    target.className = 'japa-count-tile__target';
    target.textContent = pair.dailyLabel;
    el.append(done, sep, target);
    el.setAttribute('aria-label', `${pair.current} of ${pair.dailyLabel}`);
  }

  function bind(root) {
    if (!root) return null;
    const q = (sel) => root.querySelector(sel);
    const countEl = q('[data-japa-count]');
    const stepBody = q('[data-japa-step-body]');
    const mantraSelect = q('[data-japa-mantra]');
    const statusEl = q('[data-japa-status]');
    const angaRoot = q('[data-japa-anga-tabs]');

    let session = loadSession();
    if (!STEPS[session.step]) session.step = 'poorvanga';

    function setStatus(message, tone) {
      if (!statusEl) return;
      statusEl.textContent = message || '';
      if (tone) statusEl.dataset.tone = tone;
      else delete statusEl.dataset.tone;
    }

    function renderAngaTabs() {
      const step = STEPS[session.step] || STEPS.poorvanga;
      const panel = angaRoot?.querySelector(`[data-japa-anga-panel="${session.step}"]`);
      const bodyInPanel = panel?.querySelector('.cds--body-01');
      if (bodyInPanel) bodyInPanel.textContent = step.body;
      else if (stepBody) stepBody.textContent = step.body;

      root.querySelectorAll('[data-japa-step]').forEach((btn) => {
        const on = btn.dataset.japaStep === session.step;
        btn.setAttribute('aria-selected', String(on));
        btn.setAttribute('tabindex', on ? '0' : '-1');
      });
      root.querySelectorAll('[data-japa-anga-panel]').forEach((el) => {
        const on = el.dataset.japaAngaPanel === session.step;
        el.hidden = !on;
      });
    }

    function selectMantra(id) {
      session.mantra = id;
      session.count = dayRecords()[id]?.count || 0;
    }

    function render() {
      const cumulative = getCumulative(session.mantra);
      const stats = progressStats(session.count, session.dailyTarget, cumulative, session.majorTarget);
      renderProgressPair(countEl, session.count, stats.daily);
      const record = q('[data-japa-record]');
      if (record) record.textContent = 'Cumulative ' + formatCount(cumulative) + ' · Sankalpa ' + formatCount(stats.major);
      renderAngaTabs();

      if (mantraSelect && session.mantra) {
        const has = [...mantraSelect.options].some((o) => o.value === session.mantra);
        if (has) mantraSelect.value = session.mantra;
      }
      saveSession(session);
      window.dispatchEvent(new Event('japa-record-changed'));
    }

    function applyTargetsFromProfile(profile, mantra) {
      const vault = window.madhvaUserVault;
      const id = mantra || session.mantra;
      const targets = vault?.normalizeJapaTargets?.(profile?.japaTargets, profile?.dikshaMantras)
        || profile?.japaTargets
        || {};
      const row = targets[id] || {};
      session.dailyTarget = Number(row.daily) || DEFAULT_DAILY;
      session.majorTarget = Number(row.major) || DEFAULT_MAJOR;
    }

    function bump(delta) {
      if (session.date !== todayKey()) {
        session.date = todayKey();
        session.count = 0;
      }
      const next = Math.max(0, session.count + delta);
      const applied = next - session.count;
      session.lastDelta = delta > 0 ? delta : session.lastDelta;
      session.count = next;
      if (applied !== 0) addCumulative(session.mantra, applied);
      render();
      setStatus(applied > 0 ? `+${applied}` : '', applied > 0 ? 'ok' : 'info');
    }

    q('[data-japa-inc]')?.addEventListener('click', () => bump(1));
    root.querySelectorAll('[data-japa-goal-add]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const amount = Math.max(0, Number(btn.dataset.japaGoalAdd) || 0);
        if (!amount) return;
        session.dailyTarget = Math.max(1, (Number(session.dailyTarget) || DEFAULT_DAILY) + amount);
        render();
        setStatus(`Daily goal +${formatCount(amount)} → ${formatCount(session.dailyTarget)}`, 'info');
      });
    });
    q('[data-japa-reset]')?.addEventListener('click', async () => {
      let daily = DEFAULT_DAILY;
      try {
        const profile = (await window.madhvaUserVault?.loadLocal()) || window.madhvaUserVault?.DEFAULTS;
        const targets = window.madhvaUserVault?.normalizeJapaTargets?.(profile?.japaTargets, profile?.dikshaMantras)
          || profile?.japaTargets
          || {};
        daily = Number(targets[session.mantra]?.daily) || DEFAULT_DAILY;
      } catch {
        daily = DEFAULT_DAILY;
      }
      session.dailyTarget = daily;
      render();
      setStatus(`Daily goal reset · ${formatCount(daily)}`, 'info');
    });

    root.querySelectorAll('[data-japa-step]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const next = btn.dataset.japaStep;
        if (!STEPS[next]) return;
        session.step = next;
        render();
        setStatus('', null);
        btn.focus();
      });
      btn.addEventListener('keydown', (event) => {
        if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
        const tabs = [...root.querySelectorAll('[data-japa-step]')];
        const i = tabs.indexOf(btn);
        if (i < 0) return;
        event.preventDefault();
        const delta = event.key === 'ArrowRight' ? 1 : -1;
        const next = tabs[(i + delta + tabs.length) % tabs.length];
        next?.click();
      });
    });

    mantraSelect?.addEventListener('change', async () => {
      selectMantra(mantraSelect.value);
      try {
        const profile = (await window.madhvaUserVault?.loadLocal()) || window.madhvaUserVault?.DEFAULTS;
        applyTargetsFromProfile(profile, session.mantra);
      } catch {}
      render();
    });

    async function syncMantras() {
      const vault = window.madhvaUserVault;
      if (!vault || !mantraSelect) return;
      try {
        const profile = (await vault.loadLocal()) || vault.DEFAULTS;
        vault.fillJapaMantraSelect(mantraSelect, profile);
        if (![...mantraSelect.options].some((o) => o.value === session.mantra)) {
          selectMantra(mantraSelect.options[0]?.value || 'gayatri');
        }
        mantraSelect.value = session.mantra;
        applyTargetsFromProfile(profile, session.mantra);
        saveSession(session);
      } catch {
        vault.fillJapaMantraSelect(mantraSelect, vault.DEFAULTS);
        applyTargetsFromProfile(vault.DEFAULTS, session.mantra);
      }
      render();
    }

    render();
    syncMantras();
    window.addEventListener('japa-profile-saved', syncMantras);
    window.addEventListener('japa-select', async event => {
      const id = event.detail;
      const entry = window.madhvaUserVault.DIKSHA_MANTRAS.find(item => item.id === id);
      if (!entry) return;
      selectMantra(id);
      if (![...mantraSelect.options].some(option => option.value === id)) {
        mantraSelect.add(new Option(entry.label, id));
      }
      const profile = await window.madhvaUserVault.loadLocal().catch(() => null);
      applyTargetsFromProfile(profile || window.madhvaUserVault.DEFAULTS, id);
      render();
    });
    window.addEventListener('dashboard-japa-bump', event => bump(event.detail));

    return {
      render,
      syncMantras,
      getSession: () => ({ ...session })
    };
  }

  window.madhvaJapaSession = {
    getCumulative,
    dayRecords,
    MALA_SIZE,
    STEPS,
    loadSession,
    saveSession,
    progressStats,
    formatProgressPair,
    bind
  };
})();
