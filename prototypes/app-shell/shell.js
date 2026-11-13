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
      body: 'Mantra library with category subpages.'
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
    if (group === 'grantha') {
      const copy = granthaCopy[key] || granthaCopy.stotra;
      byId('grantha-detail-title').textContent = copy.title;
      byId('grantha-detail').querySelector('p').textContent = copy.body;
    }
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
      if (prevBtn) prevBtn.disabled = index <= 0;
      if (nextBtn) nextBtn.disabled = done || index >= PATHYAS.length - 1;
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

    prevBtn?.addEventListener('click', () => {
      if (index > 0) {
        index -= 1;
        done = false;
        panel.querySelectorAll('[data-day-action]').forEach((b) => b.setAttribute('aria-pressed', 'false'));
        render();
      }
    });

    nextBtn?.addEventListener('click', () => {
      if (index < PATHYAS.length - 1) {
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
          index = PATHYAS.length - 1;
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
    app?.classList.toggle('has-child-sections', !!meta.childSections);

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
      byId('theme-dark')?.setAttribute('aria-pressed', String(dark));
      byId('theme-light')?.setAttribute('aria-pressed', String(!dark));
      if (byId('theme-select')) byId('theme-select').value = theme;
    };
    byId('theme-dark')?.addEventListener('click', () => {
      window.madhvaTheme?.set('g100');
      syncPressed();
    });
    byId('theme-light')?.addEventListener('click', () => {
      window.madhvaTheme?.set('white');
      syncPressed();
    });
    window.addEventListener('madhva-theme', syncPressed);
    syncPressed();
  }

  function bindJapaSample() {
    let count = 108;
    const label = byId('dash-japa-count');
    byId('dash-japa-inc')?.addEventListener('click', () => {
      count += 1;
      if (label) label.textContent = String(count);
    });
    byId('dash-japa-undo')?.addEventListener('click', () => {
      count = Math.max(0, count - 1);
      if (label) label.textContent = String(count);
    });
  }

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
    bindUserVault();
    bindStudioMessages();
    showChild('nityakarma', 'sandhya');
    showChild('grantha', 'stotra');
    navigate('dashboard');
  });

  window.madhvaShell = { navigate, applyScale, showNityaTab };
})();
