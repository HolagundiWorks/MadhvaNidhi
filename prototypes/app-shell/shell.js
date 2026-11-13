(() => {
  'use strict';

  // 30:70 context/calendar lives only inside the Panchanga embed.
  // All other shell destinations use full-width primary content.
  const destinations = {
    dashboard: { title: 'ಮುಖಪುಟ', en: 'Dashboard', context: false },
    panchanga: { title: 'ಪಂಚಾಂಗ', en: 'Panchanga', context: false, embed: true },
    nityakarma: { title: 'ನಿತ್ಯಕರ್ಮ', en: 'Nitya Karma', context: false, childSections: true },
    puja: { title: 'ಪೂಜೆ', en: 'Pooja', context: false, childSections: true },
    grantha: { title: 'ಗ್ರಂಥ', en: 'Grantha', context: false },
    sangeeta: { title: 'ಭಕ್ತಿ ಸಂಗೀತ', en: 'Bhakti Sangeeta', context: false },
    samskruti: { title: 'ಗೃಹ ಸಂಸ್ಕೃತಿ', en: 'Gruha Samskruti', context: false },
    user: { title: 'ಬಳಕೆದಾರ', en: 'User', context: false },
    settings: { title: 'ಸೆಟ್ಟಿಂಗ್‌ಗಳು', en: 'Settings', context: false }
  };

  const nityaCopy = {
    sandhya: {
      title: 'ಸಂಧ್ಯಾವಂದನೆ · Sandhyavandana',
      body: 'Pratah, Madhyahna, and Sayam sessions. Completion is explicit; opening the reader does not complete a session.'
    },
    japa: {
      title: 'ಜಪ · Japa',
      body: 'Repetition counter with mala conversion. Persistence stores repetitions, not only displayed malas.'
    },
    nitya: {
      title: 'ನಿತ್ಯ ಪಾರಾಯಣ · Nitya Parayana',
      body: 'Daily parayana schedule and reading surface. Resume position stays local.'
    }
  };

  const granthaCopy = {
    stotra: {
      title: 'ಸ್ತೋತ್ರಗಳು · Stotras',
      body: 'Stotra collection with bookmarks and adjustable text size.'
    },
    sukta: {
      title: 'ಸೂಕ್ತಗಳು · Suktas',
      body: 'Sukta readings from the catalogue.'
    },
    mantra: {
      title: 'ಮಂತ್ರಗಳು · Mantras',
      body: 'Mantra library.'
    }
  };

  const byId = (id) => document.getElementById(id);
  const root = document.documentElement;
  let scale = 1;
  let dest = 'dashboard';

  function applyScale(value) {
    scale = Math.min(1.5, Math.max(1, value));
    root.style.setProperty('--scale', String(scale));
    const label = Math.round(scale * 100) + '%';
    const sizeValue = byId('size-value');
    const settingsLabel = byId('settings-scale-label');
    const settingsRange = byId('settings-scale');
    if (sizeValue) sizeValue.textContent = label;
    if (settingsLabel) settingsLabel.textContent = label;
    if (settingsRange) settingsRange.value = String(scale);
    try {
      localStorage.setItem('madhvanidhi-shell-scale', String(scale));
    } catch {}
    window.parent.postMessage({ type: 'shell-scale', scale }, '*');
  }

  function closeNav() {
    const nav = byId('side-nav');
    const toggle = byId('nav-toggle');
    const backdrop = byId('nav-backdrop');
    if (!nav) return;
    nav.classList.remove('is-expanded');
    if (toggle) toggle.setAttribute('aria-expanded', 'false');
    if (backdrop) backdrop.hidden = true;
  }

  function openNav() {
    const nav = byId('side-nav');
    const toggle = byId('nav-toggle');
    const backdrop = byId('nav-backdrop');
    if (!nav) return;
    nav.classList.add('is-expanded');
    if (toggle) toggle.setAttribute('aria-expanded', 'true');
    if (backdrop) backdrop.hidden = false;
  }

  function showNityaTab(key) {
    const tabKey = nityaCopy[key] ? key : 'sandhya';
    const root = document.querySelector('[data-nitya-tabs]');
    if (!root) return;
    root.querySelectorAll('[data-nitya-tab]').forEach((tab) => {
      const selected = tab.dataset.nityaTab === tabKey;
      tab.setAttribute('aria-selected', selected ? 'true' : 'false');
      tab.tabIndex = selected ? 0 : -1;
    });
    root.querySelectorAll('[data-nitya-panel]').forEach((panel) => {
      const active = panel.dataset.nityaPanel === tabKey;
      panel.hidden = !active;
    });
  }

  function showChild(group, key) {
    if (group === 'nityakarma') showNityaTab(key);
    if (group === 'grantha') selectGranthaTab(key);
  }

  function bindDayActions(scope) {
    (scope || document).querySelectorAll('.nitya-day-actions').forEach((group) => {
      group.querySelectorAll('[data-day-action]').forEach((btn) => {
        btn.addEventListener('click', () => {
          const on = btn.getAttribute('aria-pressed') !== 'true';
          group.querySelectorAll('[data-day-action]').forEach((peer) => {
            peer.setAttribute('aria-pressed', peer === btn && on ? 'true' : 'false');
          });
        });
      });
    });
  }

  function bindSandhya(root) {
    const panel = (root || document).querySelector('[data-sandhya-root]');
    if (!panel) return;
    const PATHYAS = [
      {
        title: 'ಆಚಮನ',
        body: 'ಓಂ ಕೇಶವಾಯ ಸ್ವಾಹಾ ।\nಓಂ ನಾರಾಯಣಾಯ ಸ್ವಾಹಾ ।\nಓಂ ಮಾಧವಾಯ ಸ್ವಾಹಾ ।\n\nSample reading surface. Catalogue text binds later.'
      },
      {
        title: 'ಪ್ರಾಣಾಯಾಮ',
        body: 'ಓಂ ಭೂಃ । ಓಂ ಭುವಃ । ಓಂ ಸ್ವಃ ।\n\nPranayama sample block for the selected kala.'
      },
      {
        title: 'ಗಾಯತ್ರೀ ಜಪ',
        body: 'ಓಂ ಭೂರ್ಭುವಃ ಸ್ವಃ ।\nತತ್ ಸವಿತುರ್ವರೇಣ್ಯಂ ।\nಭರ್ಗೋ ದೇವಸ್ಯ ಧೀಮಹಿ ।\nಧಿಯೋ ಯೋ ನಃ ಪ್ರಚೋದಯಾತ್ ॥'
      },
      {
        title: 'ಅರ್ಘ್ಯಪ್ರದಾನ',
        body: 'ಅರ್ಘ್ಯಪ್ರದಾನ sample.\nOffer arghya facing the sun for the active sandhya kala.'
      }
    ];
    const kalaLabels = { pratah: 'ಪ್ರಾತಃ', madhyahna: 'ಮಧ್ಯಾಹ್ನ', sayam: 'ಸಾಯಂ' };
    let kala = 'pratah';
    let index = 0;
    let done = false;

    const pathyEl = panel.querySelector('[data-sandhya-pathy]');
    const crumbEl = panel.querySelector('[data-sandhya-crumb]');
    const bodyEl = panel.querySelector('[data-sandhya-body]');
    const labelEl = panel.querySelector('[data-sandhya-progress-label]');
    const pctEl = panel.querySelector('[data-sandhya-progress-pct]');
    const barEl = panel.querySelector('[data-sandhya-progress]');
    const barWrap = panel.querySelector('[data-sandhya-progressbar]');
    const prevBtn = panel.querySelector('[data-sandhya-prev]');
    const nextBtn = panel.querySelector('[data-sandhya-next]');

    function render() {
      const item = PATHYAS[index] || PATHYAS[0];
      const step = Math.min(index + 1, PATHYAS.length);
      const pct = done ? 100 : Math.round(((index + 1) / PATHYAS.length) * 100);
      if (pathyEl) pathyEl.textContent = item.title;
      if (bodyEl) bodyEl.textContent = item.body;
      if (crumbEl) crumbEl.textContent = `ನಿತ್ಯಕರ್ಮ · ಸಂಧ್ಯಾವಂದನೆ · ${kalaLabels[kala]} · ${item.title}`;
      if (labelEl) labelEl.textContent = `${step} / ${PATHYAS.length} ಪಠ್ಯ · ${kalaLabels[kala]}`;
      if (pctEl) pctEl.textContent = `${pct}%`;
      if (barEl) barEl.style.width = `${pct}%`;
      if (barWrap) barWrap.setAttribute('aria-valuenow', String(pct));
    }

    function goPrev() {
      if (index > 0) {
        index -= 1;
        done = false;
        panel.querySelectorAll('[data-day-action]').forEach((b) => b.setAttribute('aria-pressed', 'false'));
        render();
      }
    }

    function goNext() {
      if (index < PATHYAS.length - 1) {
        index += 1;
        if (index >= PATHYAS.length - 1) {
          done = true;
          window.madhvaPractice?.setSandhya(kala === 'madhyahna' ? 'madhya' : kala, true);
        }
        render();
      }
    }

    panel.querySelectorAll('[data-kala]').forEach((button) => {
      button.addEventListener('click', () => {
        const group = button.closest('.nitya-kala-actions') || button.parentElement;
        group.querySelectorAll('[data-kala]').forEach((peer) => {
          peer.setAttribute('aria-pressed', peer === button ? 'true' : 'false');
        });
        kala = button.dataset.kala;
        index = 0;
        done = false;
        panel.querySelectorAll('[data-day-action]').forEach((b) => b.setAttribute('aria-pressed', 'false'));
        render();
      });
    });

    let longPress = false;
    let pressTimer = 0;
    barWrap?.addEventListener('pointerdown', () => {
      longPress = false;
      pressTimer = window.setTimeout(() => {
        pressTimer = 0;
        longPress = true;
        goPrev();
      }, 450);
    });
    const clearPress = () => {
      if (pressTimer) {
        window.clearTimeout(pressTimer);
        pressTimer = 0;
      }
    };
    barWrap?.addEventListener('pointerup', clearPress);
    barWrap?.addEventListener('pointerleave', clearPress);
    barWrap?.addEventListener('pointercancel', clearPress);
    barWrap?.addEventListener('click', (ev) => {
      if (longPress) {
        ev.preventDefault();
        longPress = false;
        return;
      }
      goNext();
    });
    barWrap?.addEventListener('contextmenu', (ev) => ev.preventDefault());

    panel.querySelectorAll('[data-day-action]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const on = btn.getAttribute('aria-pressed') !== 'true';
        panel.querySelectorAll('[data-day-action]').forEach((peer) => {
          peer.setAttribute('aria-pressed', peer === btn && on ? 'true' : 'false');
        });
        if (btn.dataset.dayAction === 'complete' && on) {
          done = true;
          index = PATHYAS.length - 1;
          window.madhvaPractice?.setSandhya(kala, true);
        } else if (btn.dataset.dayAction === 'skip' && on) {
          done = false;
          index = 0;
          window.madhvaPractice?.setSandhya(kala, false);
        } else {
          done = false;
        }
        window.madhvaPractice?.paintDashboard?.();
        render();
      });
    });

    render();
  }

  function bindPuja(root) {
    const panel = (root || document).querySelector('[data-puja-root]');
    if (!panel) return;
    const STEPS = [
      {
        title: 'ಸಂಕಲ್ಪ',
        body: 'ಶ್ರೀಮದ್ ಭಾರತವರ್ಷೇ ಭರತಖಂಡೇ …\nಶ್ರೀ ಶಾಲಿವಾಹನ ಶಕೇ ೧೯೪೮ ಪರಾಭವ ನಾಮ ಸಂವತ್ಸರೇ ಕಾರ್ತಿಕ ಮಾಸೇ ಶುಕ್ಲಪಕ್ಷೇ ತೃತೀಯಾಯಾಂ …\n\nSample sankalpa. Masa / paksha / tithi bind from Panchanga later.'
      },
      {
        title: 'ಆವಾಹನ',
        body: 'ಆವಾಹನ sample.\nInvite the chosen murti for the selected ಪೂಜಾ ಕ್ರಮ.'
      },
      {
        title: 'ಉಪಚಾರ',
        body: 'ಷೋಡಶೋಪಚಾರ sample block.\nOffer the sequential upacharas for the active rite.'
      },
      {
        title: 'ಸಮರ್ಪಣ',
        body: 'ಸಮರ್ಪಣ / ಉದ್ವಾಸನ sample.\nClose the puja with dedication and farewell.'
      }
    ];
    const riteLabels = { nitya: 'ನಿತ್ಯ ಪೂಜೆ', vishnu: 'ವಿಷ್ಣು ಪೂಜೆ', ganapati: 'ಗಣಪತಿ ಪೂಜೆ' };
    let rite = 'nitya';
    let index = 0;
    let done = false;

    const stepEl = panel.querySelector('[data-puja-step]');
    const crumbEl = panel.querySelector('[data-puja-crumb]');
    const bodyEl = panel.querySelector('[data-puja-body]');
    const barEl = panel.querySelector('[data-puja-progress]');
    const barWrap = panel.querySelector('[data-puja-progressbar]');
    const prevBtn = panel.querySelector('[data-puja-prev]');
    const nextBtn = panel.querySelector('[data-puja-next]');
    const riteSelect = panel.querySelector('[data-puja-rite]');

    function render() {
      const item = STEPS[index] || STEPS[0];
      const pct = done ? 100 : Math.round(((index + 1) / STEPS.length) * 100);
      const riteLabel = riteLabels[rite] || riteLabels.nitya;
      if (stepEl) stepEl.textContent = item.title;
      if (bodyEl) bodyEl.textContent = item.body;
      if (crumbEl) crumbEl.textContent = `ಪೂಜೆ · ${riteLabel} · ${item.title}`;
      if (barEl) barEl.style.width = `${pct}%`;
      if (barWrap) barWrap.setAttribute('aria-valuenow', String(pct));
      if (prevBtn) prevBtn.disabled = index <= 0;
      if (nextBtn) nextBtn.disabled = done || index >= STEPS.length - 1;
      if (riteSelect && riteSelect.value !== rite) riteSelect.value = rite;
    }

    riteSelect?.addEventListener('change', () => {
      rite = riteSelect.value || 'nitya';
      index = 0;
      done = false;
      panel.querySelectorAll('[data-day-action]').forEach((b) => b.setAttribute('aria-pressed', 'false'));
      render();
    });

    prevBtn?.addEventListener('click', () => {
      if (index > 0) {
        index -= 1;
        done = false;
        panel.querySelectorAll('[data-day-action]').forEach((b) => b.setAttribute('aria-pressed', 'false'));
        render();
      }
    });

    nextBtn?.addEventListener('click', () => {
      if (index < STEPS.length - 1) {
        index += 1;
        render();
      }
    });

    panel.querySelectorAll('[data-day-action]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const on = btn.getAttribute('aria-pressed') !== 'true';
        panel.querySelectorAll('[data-day-action]').forEach((peer) => {
          peer.setAttribute('aria-pressed', peer === btn && on ? 'true' : 'false');
        });
        if (btn.dataset.dayAction === 'complete' && on) {
          done = true;
          index = STEPS.length - 1;
        } else if (btn.dataset.dayAction === 'skip' && on) {
          done = false;
          index = 0;
        } else {
          done = false;
        }
        render();
      });
    });

    render();
  }

  function bindNityaTabs() {
    const root = document.querySelector('[data-nitya-tabs]');
    if (!root) return;
    const tabs = [...root.querySelectorAll('[data-nitya-tab]')];
    tabs.forEach((tab) => {
      tab.addEventListener('click', () => showNityaTab(tab.dataset.nityaTab));
      tab.addEventListener('keydown', (event) => {
        const i = tabs.indexOf(tab);
        if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
          event.preventDefault();
          const next = event.key === 'ArrowRight'
            ? tabs[(i + 1) % tabs.length]
            : tabs[(i - 1 + tabs.length) % tabs.length];
          showNityaTab(next.dataset.nityaTab);
          next.focus();
        }
      });
    });
    bindSandhya(root);
    bindDayActions(root.querySelector('[data-nitya-panel="japa"]'));
    window.madhvaJapaSession?.bind(root.querySelector('[data-japa-root]'));
    bindPuja(document);
  }

  function syncHeaderDate() {
    const dateEl = byId('header-date-value');
    const tithiEl = byId('header-tithi');
    const contextDate = byId('context-date');
    const contextTithi = byId('context-tithi');
    if (dateEl && contextDate) dateEl.textContent = contextDate.textContent.trim();
    if (tithiEl && contextTithi) tithiEl.textContent = contextTithi.textContent.trim();
  }

  function navigate(id, child) {
    if (samskrutiIds.includes(id) || samskrutiIds.includes(child)) {
      selectSamskrutiTab(samskrutiIds.includes(id) ? id : child);
      id = 'samskruti';
    }
    if (granthaIds.includes(id)) { child = id; id = "grantha"; }
    if (granthaIds.includes(child)) id = "grantha";
    if (id === 'user') {
      window.selectSettingsTab('users');
      id = 'settings';
    }
    if (id === 'nityakarma' && child === 'puja') {
      navigate('puja');
      return;
    }
    if (id === 'puja' || child === 'puja') {
      id = 'puja';
      child = undefined;
    }
    if (!destinations[id]) return;
    dest = id;
    const meta = destinations[id];
    byId('page-title').textContent = meta.title;
    syncHeaderDate();

    document.querySelectorAll('.nav-button[data-dest]').forEach((button) => {
      const current = button.dataset.dest === id;
      if (current) button.setAttribute('aria-current', 'page');
      else button.removeAttribute('aria-current');
    });

    document.querySelectorAll('.view').forEach((view) => {
      view.classList.toggle('is-active', view.dataset.view === id);
    });

    const workspace = byId('workspace');
    const primary = byId('primary-content');
    const app = byId('app');
    workspace.classList.toggle('full-content', !meta.context);
    primary.classList.toggle('no-pad', !!meta.embed);
    app?.classList.toggle('has-child-sections', id === 'dashboard' || !!meta.childSections || !!document.querySelector('.view.is-active [role="tablist"]'));

    if (id === 'panchanga') {
      const frame = byId('panchanga-frame');
      if (frame && !frame.dataset.loaded) {
        frame.dataset.loaded = '1';
        // Reload once so embed query is honored if the first paint raced theme sync.
        frame.src = '../panchanga/panchanga.html?embed=1';
      }
      syncEmbedTheme();
    }

    if (child) {
      if (id === 'nityakarma') showChild('nityakarma', child);
      if (id === 'grantha') showChild('grantha', child);
    }

    closeNav();
    window.parent.postMessage({ type: 'shell-dest', dest: id }, '*');
  }

  function syncEmbedTheme() {
    const frame = byId('panchanga-frame');
    if (!frame || !frame.contentWindow) return;
    const theme = window.madhvaTheme ? window.madhvaTheme.get() : root.dataset.theme;
    const mapped = theme === 'white' || theme === 'g10' ? 'light' : 'dark';
    try {
      frame.contentWindow.postMessage({ type: 'madhva-embed-theme', theme: mapped }, '*');
    } catch {}
  }

  function bindNav() {
    byId('nav-toggle').addEventListener('click', () => {
      const nav = byId('side-nav');
      if (nav.classList.contains('is-expanded')) closeNav();
      else openNav();
    });
    byId('nav-backdrop').addEventListener('click', closeNav);

    document.querySelectorAll('[data-dest]').forEach((el) => {
      el.addEventListener('click', () => navigate(el.dataset.dest, el.dataset.child));
    });
    byId('open-panchanga')?.addEventListener('click', () => navigate('panchanga'));
    byId('quick-panchanga')?.addEventListener('click', () => navigate('panchanga'));

    document.querySelectorAll('[data-child-show]').forEach((el) => {
      el.addEventListener('click', () => {
        const key = el.dataset.childShow;
        if (key === 'puja') {
          navigate('puja');
          return;
        }
        if (key === 'parayana') {
          navigate('nityakarma', 'nitya');
          return;
        }
        if (nityaCopy[key]) {
          navigate('nityakarma', key);
          return;
        }
        if (granthaCopy[key]) showChild('grantha', key);
      });
    });
  }

  function bindTextControls() {
    byId('settings-scale')?.addEventListener('input', (event) => {
      applyScale(Number(event.target.value));
    });
  }

  function bindThemeControls() {
    const syncPressed = () => {
      const theme = document.documentElement.dataset.theme || 'g100';
      const dark = theme === 'g100' || theme === 'g90';
      byId('theme-mode').checked = dark;
      if (byId('theme-select')) byId('theme-select').value = theme;
    };
    byId('theme-mode').addEventListener('change', event => {
      window.madhvaTheme.set(event.target.checked ? 'g100' : 'white');
    });
    window.addEventListener('madhva-theme', syncPressed);
    syncPressed();
  }

  function bindJapaSample() {
    const label = byId('dash-japa-count');
    const meta = byId('dash-japa-meta');
    const readCount = () => {
      try {
        const raw = JSON.parse(localStorage.getItem('madhvanidhi-japa-session') || '{}');
        return Math.max(0, Number(raw.count) || 0);
      } catch {
        return 0;
      }
    };
    const writeCount = (count) => {
      try {
        const raw = JSON.parse(localStorage.getItem('madhvanidhi-japa-session') || '{}');
        raw.count = count;
        localStorage.setItem('madhvanidhi-japa-session', JSON.stringify(raw));
      } catch {}
      if (label) label.textContent = String(count);
      if (meta) meta.textContent = `today · ${count}`;
      window.madhvaPractice?.paintDashboard?.();
    };
    writeCount(readCount());
    byId('dash-japa-inc')?.addEventListener('click', () => writeCount(readCount() + 1));
    byId('dash-japa-undo')?.addEventListener('click', () => writeCount(Math.max(0, readCount() - 1)));
  }

  function bindPracticeDialog() {
    const dialog = byId('dashboard-practice-dialog');
    if (!dialog) return;
    const sync = () => {
      const map = loadPractice();
      const today = dateKey();
      const row = map[today] || {};
      const pratah = byId('sandhya-pratah');
      const madhya = byId('sandhya-madhyahna');
      const sayam = byId('sandhya-sayam');
      if (pratah) pratah.checked = !!row.pratah;
      if (madhya) madhya.checked = !!(row.madhyahna || row.madhya);
      if (sayam) sayam.checked = !!row.sayam;
      const s1 = byId('stotra-1');
      const s2 = byId('stotra-2');
      if (s1) s1.checked = !!row.stotra_hari_vayu;
      if (s2) s2.checked = !!row.stotra_rama_dwadasha;
    };
    const wire = (id, key) => {
      byId(id)?.addEventListener('change', (ev) => {
        const map = loadPractice();
        const today = dateKey();
        const row = map[today] || {};
        row[key] = !!ev.target.checked;
        map[today] = row;
        savePractice(map);
        paintDashboardPractice();
      });
    };
    wire('sandhya-pratah', 'pratah');
    wire('sandhya-madhyahna', 'madhyahna');
    wire('sandhya-sayam', 'sayam');
    wire('stotra-1', 'stotra_hari_vayu');
    wire('stotra-2', 'stotra_rama_dwadasha');
    dialog.addEventListener('toggle', () => {
      if (dialog.open) sync();
    });
    sync();
  }

  const PRACTICE_KEY = 'madhvanidhi-practice-day';
  const KN_DAYS = ['ಭಾ', 'ಸೋ', 'ಮಂ', 'ಬು', 'ಗು', 'ಶು', 'ಶ'];
  function dateKey(d = new Date()) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
  function loadPractice() {
    try { return JSON.parse(localStorage.getItem(PRACTICE_KEY) || '{}') || {}; }
    catch { return {}; }
  }
  function savePractice(map) {
    try { localStorage.setItem(PRACTICE_KEY, JSON.stringify(map)); } catch {}
  }
  function sandhyaFlags(map, key) {
    const row = map[key] || {};
    return [!!row.pratah, !!(row.madhyahna || row.madhya), !!row.sayam];
  }
  function paintDashboardPractice() {
    const map = loadPractice();
    const week = byId('dash-sandhya-week');
    const today = dateKey();
    if (week) {
      week.innerHTML = '';
      for (let i = 6; i >= 0; i -= 1) {
        const d = new Date();
        d.setDate(d.getDate() - i);
        const key = dateKey(d);
        const flags = sandhyaFlags(map, key);
        const done = flags.filter(Boolean).length;
        const future = key > today;
        const pending = key === today && done < 3;
        const status = done === 3 ? 'complete' : (future || (done === 0 && !pending) ? 'pending' : (pending ? 'pending' : 'skipped'));
        const marks = future
          ? ['–', '–', '–']
          : flags.map((f) => (f ? '✓' : (pending ? '–' : '×')));
        const li = document.createElement('li');
        li.className = 'sandhya-day';
        li.dataset.status = status;
        li.dataset.date = key;
        li.innerHTML = `<span aria-hidden="true">${KN_DAYS[d.getDay()]}</span><span class="sandhya-marks" aria-hidden="true">${marks.map((m) => `<span>${m}</span>`).join('')}</span>`;
        if (!future) {
          li.style.cursor = 'pointer';
          li.addEventListener('click', () => {
            const next = loadPractice();
            const row = next[key] || {};
            const order = ['pratah', 'madhyahna', 'sayam'];
            const idx = order.findIndex((s) => !row[s]);
            if (idx >= 0) row[order[idx]] = true;
            else order.forEach((s) => { row[s] = false; });
            next[key] = row;
            savePractice(next);
            paintDashboardPractice();
          });
        }
        week.appendChild(li);
      }
    }
    let current = 0;
    const cursor = new Date();
    if (!sandhyaFlags(map, dateKey(cursor)).every(Boolean)) cursor.setDate(cursor.getDate() - 1);
    while (sandhyaFlags(map, dateKey(cursor)).every(Boolean) && current < 400) {
      current += 1;
      cursor.setDate(cursor.getDate() - 1);
    }
    let best = Number(map.bestStreak) || 0;
    let run = 0;
    const scan = new Date();
    for (let i = 0; i < 400; i += 1) {
      if (sandhyaFlags(map, dateKey(scan)).every(Boolean)) {
        run += 1;
        best = Math.max(best, run);
      } else run = 0;
      scan.setDate(scan.getDate() - 1);
    }
    best = Math.max(best, current);
    map.bestStreak = best;
    savePractice(map);
    const curEl = byId('dash-streak-current');
    const bestEl = byId('dash-streak-best');
    if (curEl) curEl.textContent = `${current} days`;
    if (bestEl) bestEl.textContent = `${best} days`;

    const slots = ['gayatri', 'narayana', 'rama', 'hanuman', 'raghavendra'];
    let cum = {};
    try { cum = JSON.parse(localStorage.getItem('madhvanidhi-japa-cumulative') || '{}') || {}; } catch {}
    let session = {};
    try { session = JSON.parse(localStorage.getItem('madhvanidhi-japa-session') || '{}') || {}; } catch {}
    document.querySelectorAll('.dash-japa-slot').forEach((btn, i) => {
      const id = btn.dataset.mantra || slots[i];
      const target = Number(session.dailyTarget) || 108;
      const count = id === (session.mantraId || session.mantra || 'gayatri')
        ? (Number(session.count) || 0)
        : 0;
      const countEl = btn.querySelector('.dash-japa-count');
      const targetEl = btn.querySelector('.dash-japa-target');
      if (countEl) countEl.textContent = String(count);
      if (targetEl) targetEl.textContent = `/ ${target}`;
      void cum;
    });
    const progress = document.querySelector('.dash-progress');
    if (progress) {
      const flags = sandhyaFlags(map, today);
      const done = flags.filter(Boolean).length;
      const japa = Number(session.count) || 0;
      const goal = Number(session.dailyTarget) || 108;
      progress.textContent = `ದಿನದ ಅಭ್ಯಾಸ · ${done}/3 ಸಂಧ್ಯಾ · ಜಪ ${japa}/${goal}`;
    }
  }
  window.madhvaPractice = {
    setSandhya(session, done, date = dateKey()) {
      const map = loadPractice();
      const row = map[date] || {};
      const key = session === 'madhya' ? 'madhyahna' : session;
      row[key] = !!done;
      map[date] = row;
      savePractice(map);
    },
    paintDashboard: paintDashboardPractice
  };

  function bindStudioMessages() {
    window.addEventListener('message', (event) => {
      const data = event.data || {};
      if (data.type === 'shell-design' && data.design) {
        if (typeof data.design.scale === 'number') applyScale(data.design.scale);
        if (data.design.theme && window.madhvaTheme) window.madhvaTheme.set(data.design.theme);
      }
      if (data.type === 'shell-navigate' && data.dest) navigate(data.dest);
    });
    window.addEventListener('madhva-theme', syncEmbedTheme);
  }

  function bindUserVault() {
    window.madhvaUserVault?.bindProfile({
      form: byId('user-form'),
      fields: byId('user-profile-fields'),
      editBtn: byId('user-edit-btn'),
      saveBtn: byId('user-save-btn'),
      cancelBtn: byId('user-cancel-btn'),
      status: byId('user-vault-status'),
      passphrase: byId('user-backup-passphrase'),
      backupBtn: byId('user-backup-btn'),
      restoreBtn: byId('user-restore-btn'),
      fileInput: byId('user-restore-file')
    });
    window.madhvaUserVault?.bindLanguageSelect(byId('settings-lang'));
  }

  document.addEventListener('DOMContentLoaded', () => {
    try {
      const saved = Number(localStorage.getItem('madhvanidhi-shell-scale'));
      if (saved >= 1 && saved <= 1.5) scale = saved;
    } catch {}
    applyScale(scale);
    bindNav();
    bindNityaTabs();
    bindTextControls();
    bindThemeControls();
    bindJapaSample();
    bindPracticeDialog();
    bindUserVault();
    bindStudioMessages();
    showChild('nityakarma', 'sandhya');
    showChild('grantha', 'stotra');
    document.querySelectorAll('[data-dashboard-dialog]').forEach(button => {
      button.addEventListener('click', () => byId(button.dataset.dashboardDialog).showModal());
    });
    document.querySelectorAll('#view-dashboard dialog [data-dest]').forEach(button => {
      button.addEventListener('click', () => button.closest('dialog').close());
    });
    bindDashboardShortcuts();
    paintDashboardPractice();
    navigate('dashboard');
  });

  const SHORTCUT_CHOICES = [
    { id: 'panchanga', label: 'ಪಂಚಾಂಗ', dest: 'panchanga' },
    { id: 'sandhya', label: 'ಸಂಧ್ಯಾವಂದನೆ', dest: 'nityakarma', child: 'sandhya' },
    { id: 'japa', label: 'ಜಪ', dest: 'nityakarma', child: 'japa' },
    { id: 'nitya', label: 'ನಿತ್ಯ ಪಾರಾಯಣ', dest: 'nityakarma', child: 'nitya' },
    { id: 'puja', label: 'ಪೂಜೆ', dest: 'puja' },
    { id: 'grantha', label: 'ಗ್ರಂಥ', dest: 'grantha' },
    { id: 'sangeeta', label: 'ಭಕ್ತಿ ಸಂಗೀತ', dest: 'sangeeta' },
    { id: 'samskruti', label: 'ಗೃಹ ಸಂಸ್ಕೃತಿ', dest: 'samskruti' },
    { id: 'settings', label: 'ಸೆಟ್ಟಿಂಗ್ಸ್', dest: 'settings' }
  ];

  function bindDashboardShortcuts() {
    const key = 'madhvanidhi-dashboard-shortcuts';
    const defaults = ['panchanga', 'japa', 'grantha'];
    let slots = defaults.slice();
    try {
      const saved = JSON.parse(localStorage.getItem(key) || 'null');
      if (Array.isArray(saved) && saved.length >= 3) slots = saved.slice(0, 3);
    } catch {}
    const paint = () => {
      document.querySelectorAll('[data-shortcut-slot]').forEach(btn => {
        const i = Number(btn.dataset.shortcutSlot);
        const choice = SHORTCUT_CHOICES.find(c => c.id === slots[i]) || SHORTCUT_CHOICES[0];
        const label = btn.querySelector('.dash-shortcut-label');
        if (label) label.textContent = choice.label;
        else btn.textContent = choice.label;
        btn.dataset.dest = choice.dest;
        if (choice.child) btn.dataset.child = choice.child;
        else delete btn.dataset.child;
      });
    };
    paint();
    document.querySelectorAll('[data-shortcut-slot]').forEach(btn => {
      btn.addEventListener('contextmenu', e => {
        e.preventDefault();
        const i = Number(btn.dataset.shortcutSlot);
        const labels = SHORTCUT_CHOICES.map(c => c.label).join('\n');
        const pick = window.prompt(`Shortcut ${i + 1} — enter number:\n` + SHORTCUT_CHOICES.map((c, n) => `${n + 1}. ${c.label}`).join('\n'), '1');
        const n = Number(pick) - 1;
        if (n >= 0 && n < SHORTCUT_CHOICES.length) {
          slots[i] = SHORTCUT_CHOICES[n].id;
          try { localStorage.setItem(key, JSON.stringify(slots)); } catch {}
          paint();
        }
        void labels;
      });
    });
  }

  window.madhvaShell = { navigate, applyScale, showNityaTab };
})();
