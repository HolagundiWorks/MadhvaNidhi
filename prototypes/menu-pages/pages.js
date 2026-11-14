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
    mantras: 'grantha', namavali: 'grantha', hadugalu: 'sangeeta',
    sangeeta: 'sangeeta', playlists: 'sangeeta',
    samskruti: 'samskruti', aayadi: 'samskruti', rangavalli: 'samskruti', recipes: 'samskruti',
    user: 'user',
    settings: 'settings'
  };

  const byId = (id) => document.getElementById(id);
  const root = document.documentElement;
  let scale = 1;
  let page = 'nityakarma';

  function applyScale(value) {
    scale = Math.min(2, Math.max(1, value));
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

  function syncDrawer() {
    const drawer = byId('drawer-links');
    const source = byId('nav-links');
    if (!drawer || !source) return;
    drawer.replaceChildren();
    source.querySelectorAll('.nav-button').forEach((button) => {
      const clone = button.cloneNode(true);
      clone.addEventListener('click', () => {
        const pageId = button.dataset.page;
        closeNav();
        if (pageId) navigate(pageId);
      });
      drawer.append(clone);
    });
  }

  function closeNav() {
    const drawer = byId('nav-drawer');
    const toggle = byId('nav-toggle');
    const backdrop = byId('nav-backdrop');
    if (drawer) drawer.hidden = true;
    byId('side-nav')?.classList.remove('is-expanded');
    if (toggle) toggle.setAttribute('aria-expanded', 'false');
    if (backdrop) backdrop.hidden = true;
  }

  function openNav() {
    const drawer = byId('nav-drawer');
    const toggle = byId('nav-toggle');
    const backdrop = byId('nav-backdrop');
    syncDrawer();
    if (drawer) drawer.hidden = false;
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

  function parentPage(id) {
    if (id === 'dashboard') return null;
    if (nityaTabs.has(id)) return 'dashboard';
    if (id === 'user') return 'settings';
    if (titles[id] || railFor[id]) return 'dashboard';
    return 'dashboard';
  }

  function goBack() {
    const drawer = byId('nav-drawer');
    if (drawer && !drawer.hidden) {
      closeNav();
      return;
    }
    const viewId = nityaTabs.has(page) ? 'nityakarma' : page;
    if (viewId === 'settings' && window.consumeSettingsBack?.()) return;
    const parent = parentPage(viewId === 'nityakarma' ? 'nityakarma' : page);
    if (parent) navigate(parent);
  }

  function navigate(id) {
    if (samskrutiIds.includes(id)) {
      selectSamskrutiTab(id);
      id = 'samskruti';
    }
    if (granthaIds.includes(id)) {
      selectGranthaTab(id);
      id = 'grantha';
    }
    if (sangeetaIds.includes(id)) { selectSangeetaTab(id); id = 'sangeeta'; }
    if (id === 'user') {
      window.selectSettingsTab('users');
      id = 'settings';
    }
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
    byId('app')?.classList.toggle('has-child-sections', viewId === 'puja' || !!document.querySelector('.page.is-active [role="tablist"]'));
    closeNav();
    window.parent.postMessage({ type: 'menu-page', page: page }, '*');
  }

  /** Japa Skip/Complete — day flags only; never auto-fill bead count. */
  function bindJapaDayActions(scope) {
    const panel = (scope || document).querySelector?.('[data-nitya-panel="japa"]') || scope;
    if (!panel || !panel.querySelectorAll) return;
    const group = panel.querySelector('.nitya-day-actions');
    if (!group) return;
    const practice = window.madhvaPracticeDay;
    const statusEl = panel.querySelector('[data-japa-status]');

    function paint() {
      const skipped = !!practice?.japaSkipped?.();
      const complete = !!practice?.japaComplete?.();
      practice?.paintDayActions?.(group, skipped, complete);
      if (statusEl) {
        if (complete) statusEl.textContent = 'ಇಂದು ಪೂರ್ಣ · Completed';
        else if (skipped) statusEl.textContent = 'ಇಂದು ಬಿಟ್ಟಿದೆ · Skipped';
      }
    }

    group.querySelectorAll('[data-day-action]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const on = btn.getAttribute('aria-pressed') !== 'true';
        if (btn.dataset.dayAction === 'complete') {
          practice?.setJapaDay?.(!!on, false);
        } else if (btn.dataset.dayAction === 'skip') {
          practice?.setJapaDay?.(false, !!on);
        }
        paint();
      });
    });
    paint();
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
    bindJapaDayActions(root.querySelector('[data-nitya-panel="japa"]'));
  }

  function bindNav() {
    byId('nav-toggle').addEventListener('click', () => {
      const drawer = byId('nav-drawer');
      if (drawer && !drawer.hidden) closeNav();
      else openNav();
    });
    byId('nav-drawer-close')?.addEventListener('click', closeNav);
    byId('nav-backdrop').addEventListener('click', closeNav);
    document.querySelectorAll('#nav-links button[data-page]').forEach((el) => {
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
      byId('theme-mode').checked = dark;
      if (byId('theme-select')) byId('theme-select').value = theme;
    };
    byId('theme-select')?.addEventListener('change', (event) => {
      window.madhvaTheme?.set(event.target.value);
      syncTheme();
    });
    byId('theme-mode').addEventListener('change', event => {
      window.madhvaTheme.set(event.target.checked ? 'g100' : 'white');
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
    const practice = window.madhvaPracticeDay;
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
    let done = !!practice?.isSandhyaDone?.(kala);
    let skipped = !!practice?.isSandhyaSkipped?.(kala);
    if (done) index = PATHYAS.length - 1;

    const pathyEl = panel.querySelector('[data-sandhya-pathy]');
    const crumbEl = panel.querySelector('[data-sandhya-crumb]');
    const bodyEl = panel.querySelector('[data-sandhya-body]');
    const labelEl = panel.querySelector('[data-sandhya-progress-label]');
    const pctEl = panel.querySelector('[data-sandhya-progress-pct]');
    const barEl = panel.querySelector('[data-sandhya-progress]');
    const barWrap = panel.querySelector('[data-sandhya-progressbar]');
    const dayGroup = panel.querySelector('.nitya-day-actions');

    function render() {
      const item = PATHYAS[index] || PATHYAS[0];
      const step = Math.min(index + 1, PATHYAS.length);
      const pct = done ? 100 : skipped ? 0 : Math.min(99, Math.round(((index + 1) / PATHYAS.length) * 100));
      if (pathyEl) pathyEl.textContent = item.title;
      if (bodyEl) bodyEl.textContent = item.body;
      if (crumbEl) crumbEl.textContent = `ನಿತ್ಯಕರ್ಮ · ಸಂಧ್ಯಾವಂದನೆ · ${kalaLabels[kala]} · ${item.title}`;
      if (labelEl) labelEl.textContent = `${step} / ${PATHYAS.length} ಪಠ್ಯ · ${kalaLabels[kala]}`;
      if (pctEl) pctEl.textContent = `${pct}%`;
      if (barEl) barEl.style.width = `${pct}%`;
      if (barWrap) barWrap.setAttribute('aria-valuenow', String(pct));
      practice?.paintDayActions?.(dayGroup, skipped, done);
    }

    function goPrev() {
      if (index > 0) {
        index -= 1;
        done = false;
        skipped = false;
        practice?.setSandhyaDay?.(kala, false, false);
        render();
      }
    }

    function goNext() {
      if (index < PATHYAS.length - 1) {
        index += 1;
        if (done || skipped) {
          done = false;
          skipped = false;
          practice?.setSandhyaDay?.(kala, false, false);
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
        skipped = !!practice?.isSandhyaSkipped?.(kala);
        done = !!practice?.isSandhyaDone?.(kala);
        if (done) index = PATHYAS.length - 1;
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
        if (btn.dataset.dayAction === 'complete') {
          done = !!on;
          skipped = false;
          if (on) index = PATHYAS.length - 1;
          practice?.setSandhyaDay?.(kala, !!on, false);
        } else if (btn.dataset.dayAction === 'skip') {
          skipped = !!on;
          done = false;
          if (on) index = 0;
          practice?.setSandhyaDay?.(kala, false, !!on);
        }
        render();
      });
    });

    render();
  }

  function bindParayana() {
    const panel = document.querySelector('[data-parayana-root]');
    if (!panel) return;
    const practice = window.madhvaPracticeDay;
    const group = panel.querySelector('.nitya-day-actions');
    if (!group) return;
    function paint() {
      practice?.paintDayActions?.(group, !!practice.parayanaSkipped?.(), !!practice.parayanaComplete?.());
    }
    group.querySelectorAll('[data-day-action]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const on = btn.getAttribute('aria-pressed') !== 'true';
        if (btn.dataset.dayAction === 'complete') practice?.setParayanaDay?.(!!on, false);
        else if (btn.dataset.dayAction === 'skip') practice?.setParayanaDay?.(false, !!on);
        paint();
      });
    });
    paint();
  }

  function bindPuja() {
    const panel = document.querySelector('[data-puja-root]');
    if (!panel) return;
    const practice = window.madhvaPracticeDay;
    const BODIES = {
      nitya: 'ಸಂಕಲ್ಪ\nಶ್ರೀಮದ್ ಭಾರತವರ್ಷೇ ಭರತಖಂಡೇ …\nಶ್ರೀ ಶಾಲಿವಾಹನ ಶಕೇ ೧೯೪೮ ಪರಾಭವ ನಾಮ ಸಂವತ್ಸರೇ ಕಾರ್ತಿಕ ಮಾಸೇ ಶುಕ್ಲಪಕ್ಷೇ ತೃತೀಯಾಯಾಂ …\n\nಆವಾಹನ\nInvite the chosen murti for ನಿತ್ಯ ಪೂಜೆ.\n\nಉಪಚಾರ\nಷೋಡಶೋಪಚಾರ sample block. Offer the sequential upacharas.\n\nಸಮರ್ಪಣ\nಸಮರ್ಪಣ / ಉದ್ವಾಸನ. Close with dedication.',
      vishnu: 'ಸಂಕಲ್ಪ\nವಿಷ್ಣು ಪೂಜೆ sample — continuous scroll.\n\nಆವಾಹನ · ಉಪಚಾರ · ಸಮರ್ಪಣ\nFull rite text scrolls in one pane.',
      ganapati: 'ಸಂಕಲ್ಪ\nಗಣಪತಿ ಪೂಜೆ sample — continuous scroll.\n\nಆವಾಹನ · ಉಪಚಾರ · ಸಮರ್ಪಣ\nFull rite text scrolls in one pane.'
    };
    const riteLabels = { nitya: 'ನಿತ್ಯ ಪೂಜೆ', vishnu: 'ವಿಷ್ಣು ಪೂಜೆ', ganapati: 'ಗಣಪತಿ ಪೂಜೆ' };
    let rite = 'nitya';

    const stepEl = panel.querySelector('[data-puja-step]');
    const crumbEl = panel.querySelector('[data-puja-crumb]');
    const bodyEl = panel.querySelector('[data-puja-body]');
    const barEl = panel.querySelector('[data-puja-progress]');
    const barWrap = panel.querySelector('[data-puja-progressbar]');
    const riteSelect = panel.querySelector('[data-puja-rite]');
    const dayGroup = panel.querySelector('.nitya-day-actions');

    function render() {
      const riteLabel = riteLabels[rite] || riteLabels.nitya;
      const done = !!practice?.pujaComplete?.();
      const skipped = !!practice?.pujaSkipped?.();
      const pct = done ? 100 : 0;
      if (stepEl) stepEl.textContent = riteLabel;
      if (bodyEl) bodyEl.textContent = BODIES[rite] || BODIES.nitya;
      if (crumbEl) crumbEl.textContent = `ಪೂಜೆ · ${riteLabel}`;
      if (barEl) barEl.style.width = `${pct}%`;
      if (barWrap) barWrap.setAttribute('aria-valuenow', String(pct));
      if (riteSelect && riteSelect.value !== rite) riteSelect.value = rite;
      practice?.paintDayActions?.(dayGroup, skipped, done);
    }

    riteSelect?.addEventListener('change', () => {
      rite = riteSelect.value || 'nitya';
      render();
    });

    panel.querySelectorAll('[data-day-action]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const on = btn.getAttribute('aria-pressed') !== 'true';
        if (btn.dataset.dayAction === 'complete') {
          practice?.setPujaDay?.(!!on, false);
        } else if (btn.dataset.dayAction === 'skip') {
          practice?.setPujaDay?.(false, !!on);
        }
        render();
      });
    });

    render();
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
      passphraseConfirm: byId('user-backup-passphrase-confirm'),
      backupBtn: byId('user-backup-btn'),
      restoreBtn: byId('user-restore-btn'),
      fileInput: byId('user-restore-file')
    });
    byId('user-form')?.addEventListener('change', syncJapaMantras);
    binding?.ready?.then(syncJapaMantras);
    window.madhvaUserVault?.bindLanguageSelect(byId('settings-lang'));
  }

  document.addEventListener('DOMContentLoaded', () => {

    applyScale(scale);
    if (window.madhvaTheme) byId('theme-select').value = window.madhvaTheme.get();
    bindNav();
    bindNityaTabs();
    bindSandhya();
    bindParayana();
    bindPuja();
    bindTextControls();
    bindJapa();
    bindDayActions(document.querySelector('[data-nitya-panel="japa"]'));

    bindUserVault();
    bindStudio();
    const start = new URLSearchParams(location.search).get('page') || 'nityakarma';
    navigate(start);
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      goBack();
    }
  });

  window.madhvaMenuPages = { navigate, goBack, applyScale, showNityaTab };
})();
