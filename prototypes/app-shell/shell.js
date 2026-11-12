(() => {
  'use strict';

  const destinations = {
    dashboard: { title: 'ಮುಖಪುಟ', en: 'Dashboard', context: true },
    panchanga: { title: 'ಪಂಚಾಂಗ', en: 'Panchanga', context: false, embed: true },
    nityakarma: { title: 'ನಿತ್ಯಕರ್ಮ', en: 'Nitya Karma', context: true },
    grantha: { title: 'ಗ್ರಂಥ', en: 'Grantha', context: true },
    sangeeta: { title: 'ಭಕ್ತಿ ಸಂಗೀತ', en: 'Bhakti Sangeeta', context: true },
    samskruti: { title: 'ಗೃಹ ಸಂಸ್ಕೃತಿ', en: 'Gruha Samskruti', context: true },
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
    puja: {
      title: 'ಪೂಜೆ · Pooja',
      body: 'Pooja procedures from the bundled Madhwa catalogue. Sankalpa details use the selected Panchanga date.'
    }
  };

  const granthaCopy = {
    parayana: {
      title: 'ನಿತ್ಯ ಪಾರಾಯಣ · Nitya Parayana',
      body: 'Daily parayana schedule and reading surface. Resume position stays local.'
    },
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

  function showChild(group, key) {
    if (group === 'nityakarma') {
      const copy = nityaCopy[key] || nityaCopy.sandhya;
      byId('nitya-detail-title').textContent = copy.title;
      byId('nitya-detail-body').textContent = copy.body;
    }
    if (group === 'grantha') {
      const copy = granthaCopy[key] || granthaCopy.parayana;
      byId('grantha-detail-title').textContent = copy.title;
      byId('grantha-detail').querySelector('p').textContent = copy.body;
    }
  }

  function navigate(id, child) {
    if (!destinations[id]) return;
    dest = id;
    const meta = destinations[id];
    byId('page-title').textContent = meta.title;

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
    workspace.classList.toggle('full-content', !meta.context);
    primary.classList.toggle('no-pad', !!meta.embed);

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
    byId('open-panchanga').addEventListener('click', () => navigate('panchanga'));
    byId('quick-panchanga').addEventListener('click', () => navigate('panchanga'));

    document.querySelectorAll('[data-child-show]').forEach((el) => {
      el.addEventListener('click', () => {
        const key = el.dataset.childShow;
        if (nityaCopy[key]) showChild('nityakarma', key);
        if (granthaCopy[key]) showChild('grantha', key);
      });
    });
  }

  function bindTextControls() {
    byId('smaller').addEventListener('click', () => applyScale(scale - 0.25));
    byId('larger').addEventListener('click', () => applyScale(scale + 0.25));
    byId('settings-scale').addEventListener('input', (event) => {
      applyScale(Number(event.target.value));
    });
  }

  function bindJapaSample() {
    let count = 108;
    const label = byId('japa-count');
    byId('japa-inc').addEventListener('click', () => {
      count += 1;
      label.textContent = String(count);
    });
    byId('japa-undo').addEventListener('click', () => {
      count = Math.max(0, count - 1);
      label.textContent = String(count);
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

  document.addEventListener('DOMContentLoaded', () => {
    try {
      const saved = Number(localStorage.getItem('madhvanidhi-shell-scale'));
      if (saved >= 1 && saved <= 1.5) scale = saved;
    } catch {}
    applyScale(scale);
    bindNav();
    bindTextControls();
    bindJapaSample();
    bindStudioMessages();
    showChild('nityakarma', 'sandhya');
    showChild('grantha', 'parayana');
    navigate('dashboard');
  });

  window.madhvaShell = { navigate, applyScale };
})();
