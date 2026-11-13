(() => {
  'use strict';

  const titles = {
    nityakarma: 'ನಿತ್ಯಕರ್ಮ',
    sandhya: 'ಸಂಧ್ಯಾವಂದನೆ',
    japa: 'ಜಪ',
    puja: 'ಪೂಜೆ',
    grantha: 'ಗ್ರಂಥ',
    nitya: 'ನಿತ್ಯ ಪಾರಾಯಣ',
    stotra: 'ಸ್ತೋತ್ರಗಳು',
    suktas: 'ಸೂಕ್ತಗಳು',
    mantras: 'ಮಂತ್ರಗಳು',
    namavali: 'ನಾಮಾವಳಿ',
    sangeeta: 'ಭಕ್ತಿ ಸಂಗೀತ',
    hadugalu: 'ಹಾಡುಗಳು',
    playlists: 'ಪ್ಲೇಲಿಸ್ಟ್‌ಗಳು',
    samskruti: 'ಗೃಹ ಸಂಸ್ಕೃತಿ',
    aayadi: 'ಆಯಾದಿ',
    rangavalli: 'ರಂಗವಲ್ಲಿ',
    recipes: 'ಪಾಕ / ನೈವೇದ್ಯ',
    user: 'ಬಳಕೆದಾರ',
    settings: 'ಸೆಟ್ಟಿಂಗ್‌ಗಳು'
  };

  const railFor = {
    nityakarma: 'nityakarma', sandhya: 'nityakarma', japa: 'nityakarma', nitya: 'nityakarma',
    puja: 'puja',
    grantha: 'grantha', stotra: 'grantha', suktas: 'grantha',
    mantras: 'grantha', namavali: 'grantha',
    sangeeta: 'sangeeta', hadugalu: 'sangeeta', playlists: 'sangeeta',
    samskruti: 'samskruti', aayadi: 'samskruti', rangavalli: 'samskruti', recipes: 'samskruti',
    user: 'user',
    settings: 'settings'
  };

  const byId = (id) => document.getElementById(id);
  const root = document.documentElement;
  let scale = 1;
  let page = 'nityakarma';

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
    try { localStorage.setItem('madhvanidhi-menu-scale', String(scale)); } catch {}
    window.parent.postMessage({ type: 'menu-scale', scale }, '*');
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

  const nityaTabs = new Set(['sandhya', 'japa', 'nitya']);

  function showNityaTab(key) {
    const tabKey = nityaTabs.has(key) ? key : 'sandhya';
    const root = document.querySelector('[data-nitya-tabs]');
    if (!root) return;
    root.querySelectorAll('[data-nitya-tab]').forEach((tab) => {
      const selected = tab.dataset.nityaTab === tabKey;
      tab.setAttribute('aria-selected', selected ? 'true' : 'false');
      tab.tabIndex = selected ? 0 : -1;
    });
    root.querySelectorAll('[data-nitya-panel]').forEach((panel) => {
      panel.hidden = panel.dataset.nityaPanel !== tabKey;
    });
  }

  function navigate(id) {
    const nityaChild = nityaTabs.has(id) ? id : null;
    const viewId = nityaChild ? 'nityakarma' : id;
    if (!titles[viewId] && !titles[id]) return;
    page = nityaChild || viewId;
    byId('page-title').textContent = titles[nityaChild || viewId] || titles.nityakarma;
    const rail = railFor[nityaChild || viewId];
    document.querySelectorAll('.nav-button[data-page]').forEach((button) => {
      const current = button.dataset.page === rail;
      if (current) button.setAttribute('aria-current', 'page');
      else button.removeAttribute('aria-current');
    });
    document.querySelectorAll('.page').forEach((view) => {
      view.classList.toggle('is-active', view.dataset.page === viewId);
    });
    if (viewId === 'nityakarma') showNityaTab(nityaChild || 'sandhya');
    byId('app')?.classList.toggle('has-child-sections', viewId === 'nityakarma' || viewId === 'puja');
    closeNav();
    window.parent.postMessage({ type: 'menu-page', page: page }, '*');
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

  function bindNityaTabs() {
    const root = document.querySelector('[data-nitya-tabs]');
    if (!root) return;
    const tabs = [...root.querySelectorAll('[data-nitya-tab]')];
    tabs.forEach((tab) => {
      tab.addEventListener('click', () => {
        showNityaTab(tab.dataset.nityaTab);
        page = tab.dataset.nityaTab;
        byId('page-title').textContent = titles[page] || titles.nityakarma;
        window.parent.postMessage({ type: 'menu-page', page }, '*');
      });
      tab.addEventListener('keydown', (event) => {
        const i = tabs.indexOf(tab);
        if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
          event.preventDefault();
          const next = event.key === 'ArrowRight'
            ? tabs[(i + 1) % tabs.length]
            : tabs[(i - 1 + tabs.length) % tabs.length];
          next.click();
          next.focus();
        }
      });
    });
    bindDayActions(root);
  }

  function bindNav() {
    byId('nav-toggle').addEventListener('click', () => {
      const nav = byId('side-nav');
      if (nav.classList.contains('is-expanded')) closeNav();
      else openNav();
    });
    byId('nav-backdrop').addEventListener('click', closeNav);
    document.querySelectorAll('[data-page]').forEach((el) => {
      el.addEventListener('click', () => navigate(el.dataset.page));
    });
  }

  function bindTextControls() {
    byId('settings-scale')?.addEventListener('input', (event) => {
      applyScale(Number(event.target.value));
    });
    const syncTheme = () => {
      const theme = window.madhvaTheme?.get() || document.documentElement.dataset.theme || 'g100';
      const dark = theme === 'g100' || theme === 'g90';
      byId('theme-dark')?.setAttribute('aria-pressed', String(dark));
      byId('theme-light')?.setAttribute('aria-pressed', String(!dark));
      if (byId('theme-select')) byId('theme-select').value = theme;
    };
    byId('theme-select')?.addEventListener('change', (event) => {
      window.madhvaTheme?.set(event.target.value);
      syncTheme();
    });
    byId('theme-dark')?.addEventListener('click', () => {
      window.madhvaTheme?.set('g100');
      syncTheme();
    });
    byId('theme-light')?.addEventListener('click', () => {
      window.madhvaTheme?.set('white');
      syncTheme();
    });
    window.addEventListener('madhva-theme', syncTheme);
    syncTheme();
  }

  function bindJapa() {
    window.madhvaJapaSession?.bind(document.querySelector('[data-japa-root]'));
  }

  function bindSandhya() {
    const panel = document.querySelector('[data-sandhya-root]');
    if (!panel) return;
    const PATHYAS = [
      {
        title: 'ಆಚಮನ',
        body: 'ಓಂ ಕೇಶವಾಯ ಸ್ವಾಹಾ ।\nಓಂ ನಾರಾಯಣಾಯ ಸ್ವಾಹಾ ।\nಓಂ ಮಾಧವಾಯ ಸ್ವಾಹಾ ।\n\nSample reading surface. Android loads the SandhyaStore / catalogue text for the selected kala and item.'
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

  function bindPuja() {
    const panel = document.querySelector('[data-puja-root]');
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

  function bindAayadi() {
    byId('aayadi-calc').addEventListener('click', () => {
      const length = Number(byId('len-ft').value) + Number(byId('len-in').value) / 12;
      const breadth = Number(byId('br-ft').value) + Number(byId('br-in').value) / 12;
      const area = length * breadth;
      const padam = ((area * 9) % 8) || 8;
      const aaya = ((area * 8) % 12) || 12;
      const dhana = ((area * 3) % 7) || 7;
      const rina = ((area * 4) % 7) || 7;
      byId('aayadi-padam').textContent = String(Math.round(padam));
      byId('aayadi-aaya').textContent = String(Math.round(aaya));
      byId('aayadi-dhana').textContent = String(Math.round(dhana));
      byId('aayadi-rina').textContent = String(Math.round(rina));
      byId('aayadi-results').hidden = false;
    });
  }

  function bindStudio() {
    window.addEventListener('message', (event) => {
      const data = event.data || {};
      if (data.type === 'menu-design' && data.design) {
        if (typeof data.design.scale === 'number') applyScale(data.design.scale);
        if (data.design.theme && window.madhvaTheme) window.madhvaTheme.set(data.design.theme);
      }
      if (data.type === 'menu-navigate' && data.page) navigate(data.page);
    });
  }

  function syncJapaMantras() {
    const vault = window.madhvaUserVault;
    const select = document.querySelector('[data-japa-mantra]');
    if (!vault || !select) return;
    vault.loadLocal().then((profile) => {
      vault.fillJapaMantraSelect(select, profile || vault.DEFAULTS);
    }).catch(() => vault.fillJapaMantraSelect(select, vault.DEFAULTS));
  }

  function bindUserVault() {
    const binding = window.madhvaUserVault?.bindProfile({
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
    byId('user-form')?.addEventListener('change', syncJapaMantras);
    binding?.ready?.then(syncJapaMantras);
    window.madhvaUserVault?.bindLanguageSelect(byId('settings-lang'));
  }

  document.addEventListener('DOMContentLoaded', () => {
    try {
      const saved = Number(localStorage.getItem('madhvanidhi-menu-scale'));
      if (saved >= 1 && saved <= 1.5) scale = saved;
    } catch {}
    applyScale(scale);
    if (window.madhvaTheme) byId('theme-select').value = window.madhvaTheme.get();
    bindNav();
    bindNityaTabs();
    bindSandhya();
    bindPuja();
    bindTextControls();
    bindJapa();
    bindDayActions(document.querySelector('[data-nitya-panel="japa"]'));
    bindAayadi();
    bindUserVault();
    bindStudio();
    const start = new URLSearchParams(location.search).get('page') || 'nityakarma';
    navigate(start);
  });

  window.madhvaMenuPages = { navigate, applyScale, showNityaTab };
})();
