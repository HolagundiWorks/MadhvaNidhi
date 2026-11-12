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
    settings: 'ಸೆಟ್ಟಿಂಗ್‌ಗಳು'
  };

  const railFor = {
    nityakarma: 'nityakarma', sandhya: 'nityakarma', japa: 'nityakarma', puja: 'nityakarma',
    grantha: 'grantha', nitya: 'grantha', stotra: 'grantha', suktas: 'grantha',
    mantras: 'grantha', namavali: 'grantha',
    sangeeta: 'sangeeta', hadugalu: 'sangeeta', playlists: 'sangeeta',
    samskruti: 'samskruti', aayadi: 'samskruti', rangavalli: 'samskruti', recipes: 'samskruti',
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

  function navigate(id) {
    if (!titles[id]) return;
    page = id;
    byId('page-title').textContent = titles[id];
    const rail = railFor[id];
    document.querySelectorAll('.nav-button[data-page]').forEach((button) => {
      const current = button.dataset.page === rail;
      if (current) button.setAttribute('aria-current', 'page');
      else button.removeAttribute('aria-current');
    });
    document.querySelectorAll('.page').forEach((view) => {
      view.classList.toggle('is-active', view.dataset.page === id);
    });
    closeNav();
    window.parent.postMessage({ type: 'menu-page', page: id }, '*');
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
    byId('smaller').addEventListener('click', () => applyScale(scale - 0.25));
    byId('larger').addEventListener('click', () => applyScale(scale + 0.25));
    byId('settings-scale').addEventListener('input', (event) => {
      applyScale(Number(event.target.value));
    });
    byId('theme-select').addEventListener('change', (event) => {
      if (window.madhvaTheme) window.madhvaTheme.set(event.target.value);
    });
    window.addEventListener('madhva-theme', () => {
      if (window.madhvaTheme) byId('theme-select').value = window.madhvaTheme.get();
    });
  }

  function bindJapa() {
    let count = 108;
    const label = byId('japa-count');
    byId('japa-inc').addEventListener('click', () => {
      count += 1;
      label.textContent = String(count);
    });
    byId('japa-108').addEventListener('click', () => {
      count += 108;
      label.textContent = String(count);
    });
    byId('japa-undo').addEventListener('click', () => {
      count = Math.max(0, count - 1);
      label.textContent = String(count);
    });
    byId('japa-reset').addEventListener('click', () => {
      count = 0;
      label.textContent = '0';
    });
  }

  function bindKala() {
    document.querySelectorAll('[data-kala]').forEach((button) => {
      button.addEventListener('click', () => {
        button.parentElement.querySelectorAll('button').forEach((peer) => {
          peer.setAttribute('aria-pressed', peer === button ? 'true' : 'false');
        });
      });
    });
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

  document.addEventListener('DOMContentLoaded', () => {
    try {
      const saved = Number(localStorage.getItem('madhvanidhi-menu-scale'));
      if (saved >= 1 && saved <= 1.5) scale = saved;
    } catch {}
    applyScale(scale);
    if (window.madhvaTheme) byId('theme-select').value = window.madhvaTheme.get();
    bindNav();
    bindTextControls();
    bindJapa();
    bindKala();
    bindAayadi();
    bindStudio();
    const start = new URLSearchParams(location.search).get('page') || 'nityakarma';
    navigate(start);
  });

  window.madhvaMenuPages = { navigate, applyScale };
})();
