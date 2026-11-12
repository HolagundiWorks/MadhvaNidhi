(() => {
  'use strict';
  const key = 'madhvanidhi-shell-theme';
  const themes = ['g100', 'g90', 'g10', 'white'];
  const labels = {
    g100: 'Gray 100',
    g90: 'Gray 90',
    g10: 'Gray 10',
    white: 'White'
  };
  const valid = (value) => themes.includes(value);

  function syncControl() {
    const select = document.getElementById('theme-select');
    const button = document.getElementById('theme-cycle');
    const theme = document.documentElement.dataset.theme || 'g100';
    if (select) select.value = theme;
    if (button) {
      const next = themes[(themes.indexOf(theme) + 1) % themes.length];
      const action = `Switch to ${labels[next]}`;
      button.setAttribute('aria-label', action);
      button.title = action;
    }
  }

  function syncSystemChrome() {
    const bg = getComputedStyle(document.documentElement).getPropertyValue('--cds-background').trim() || '#161616';
    let meta = document.querySelector('meta[name="theme-color"]');
    if (!meta) {
      meta = document.createElement('meta');
      meta.name = 'theme-color';
      document.head.appendChild(meta);
    }
    meta.content = bg;
    document.documentElement.style.backgroundColor = bg;
    if (document.body) document.body.style.backgroundColor = bg;
  }

  function apply(theme, persist = false) {
    document.documentElement.dataset.theme = valid(theme) ? theme : 'g100';
    syncControl();
    syncSystemChrome();
    if (persist) {
      try {
        localStorage.setItem(key, document.documentElement.dataset.theme);
      } catch {}
    }
    window.dispatchEvent(new CustomEvent('madhva-theme', {
      detail: { theme: document.documentElement.dataset.theme }
    }));
  }

  let saved = 'g100';
  try {
    const value = localStorage.getItem(key);
    if (valid(value)) saved = value;
  } catch {}
  apply(saved);

  document.addEventListener('DOMContentLoaded', () => {
    syncControl();
    const select = document.getElementById('theme-select');
    if (select) {
      select.addEventListener('change', () => apply(select.value, true));
    }
    const button = document.getElementById('theme-cycle');
    if (button) {
      button.addEventListener('click', () => {
        const current = document.documentElement.dataset.theme || 'g100';
        const next = themes[(themes.indexOf(current) + 1) % themes.length];
        apply(next, true);
      });
    }
  });

  window.addEventListener('storage', (event) => {
    if (event.key === key) apply(valid(event.newValue) ? event.newValue : 'g100');
  });

  window.madhvaTheme = {
    set: (theme) => apply(theme, true),
    get: () => document.documentElement.dataset.theme,
    themes,
    labels
  };
})();
