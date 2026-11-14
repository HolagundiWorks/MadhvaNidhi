const settingsTabs = [...document.querySelectorAll('#settings-tabs [role="tab"]')];
function selectSettingsTab(name) {
  if (name === 'dashboard') window.refreshSettingsJapa?.();
  if (name === 'users') {
    const form = document.querySelector('#user-form');
    const fields = document.querySelector('#user-profile-fields');
    if (fields?.disabled && window.madhvaUserVault) {
      window.madhvaUserVault.loadLocal().then(profile => {
        window.madhvaUserVault.renderJapaTargetsView(form, profile || window.madhvaUserVault.DEFAULTS);
      }).catch(() => {});
    }
  }
  for (const tab of settingsTabs) {
    const selected = tab.id === 'settings-tab-' + name;
    tab.tabIndex = selected ? 0 : -1;
    tab.setAttribute('aria-selected', String(selected));
    const panel = document.getElementById(tab.getAttribute('aria-controls'));
    if (panel) panel.hidden = !selected;
  }
}
/** Back from Users/Backup/Legal returns to UI Settings before leaving the hub. */
function consumeSettingsBack() {
  const selected = settingsTabs.find((tab) => tab.getAttribute('aria-selected') === 'true');
  if (!selected || selected.id === 'settings-tab-ui') return false;
  selectSettingsTab('ui');
  return true;
}
settingsTabs.forEach((tab, index) => {
  tab.onclick = () => selectSettingsTab(tab.id.slice('settings-tab-'.length));
  tab.onkeydown = event => {
    const n = settingsTabs.length;
    const targets = {
      ArrowRight: (index + 1) % n,
      ArrowLeft: (index + n - 1) % n,
      Home: 0,
      End: n - 1
    };
    if (!(event.key in targets)) return;
    event.preventDefault();
    const next = settingsTabs[targets[event.key]];
    next.focus();
    next.click();
  };
});
window.selectSettingsTab = selectSettingsTab;
window.consumeSettingsBack = consumeSettingsBack;

(() => {
  const awake = document.getElementById('settings-awake');
  const status = document.getElementById('settings-awake-status');
  const volume = document.getElementById('settings-volume');
  const label = document.getElementById('settings-volume-label');
  if (!awake || !volume) return;
  let wakeLock;
  awake.addEventListener('change', async () => {
    try {
      if (awake.checked) {
        if (!navigator.wakeLock) throw new Error('Unavailable');
        wakeLock = await navigator.wakeLock.request('screen');
        status.textContent = 'Screen awake while this preview is visible';
        wakeLock.addEventListener('release', () => {
          awake.checked = false;
          status.textContent = '';
        });
      } else {
        await wakeLock?.release();
        status.textContent = '';
      }
    } catch {
      awake.checked = false;
      status.textContent = 'Screen wake lock is unavailable in this browser';
    }
  });
  try { volume.value = localStorage.getItem('madhvanidhi-preview-volume') || '100'; } catch {}
  function applyVolume() {
    label.textContent = volume.value + '%';
    document.querySelectorAll('audio, video').forEach(media => media.volume = Number(volume.value) / 100);
  }
  volume.addEventListener('input', () => {
    applyVolume();
    try { localStorage.setItem('madhvanidhi-preview-volume', volume.value); } catch {}
  });
  applyVolume();
})();

(() => {
  const target = document.querySelector('[data-settings-japa-targets]');
  const slots = document.querySelector('[data-settings-japa-slots]');
  if (!target || !slots) return;
  const key = 'madhvanidhi-dashboard-japas';
  async function refresh() {
    const vault = window.madhvaUserVault;
    if (!vault || !window.madhvaJapaSession) return;
    const profile = await vault.loadLocal().catch(() => null) || vault.DEFAULTS;
    target.innerHTML = '<div data-view-japa-targets></div>';
    vault.renderJapaTargetsView(target, profile);
    slots.replaceChildren();
    let choices;
    try { choices = JSON.parse(localStorage.getItem(key)); } catch {}
    if (!Array.isArray(choices)) choices = [];
    for (let i = 0; i < 5; i++) {
      const label = document.createElement('label');
      label.className = 'cds--label';
      label.textContent = 'Column ' + (i + 1);
      const control = document.createElement(i ? 'select' : 'span');
      if (i) {
        control.className = 'cds--select-input';
        control.add(new Option('Choose Japa', ''));
        vault.DIKSHA_MANTRAS.forEach(item => control.add(new Option(item.label, item.id)));
        control.value = choices[i - 1] || '';
        control.addEventListener('change', () => {
          choices[i - 1] = control.value;
          localStorage.setItem(key, JSON.stringify(choices));
          window.dispatchEvent(new Event('dashboard-japa-slots-changed'));
        });
      } else {
        control.textContent = 'ಗಾಯತ್ರೀ';
      }
      label.append(control);
      slots.append(label);
    }
  }
  window.refreshSettingsJapa = refresh;
  window.addEventListener('japa-record-changed', () => {
    if (!document.querySelector('#settings-panel-dashboard')?.hidden) refresh();
  });
  window.addEventListener('japa-profile-saved', refresh);
  window.addEventListener('load', refresh);
})();

(() => {
  const host = document.querySelector('[data-settings-shortcuts]');
  if (!host) return;
  const options = [
    ['panchanga', 'ಪಂಚಾಂಗ'], ['sandhya', 'ಸಂಧ್ಯಾವಂದನೆ'], ['japa', 'ಜಪ'],
    ['nitya', 'ನಿತ್ಯ ಪಾರಾಯಣ'], ['puja', 'ಪೂಜೆ'], ['grantha', 'ಗ್ರಂಥ'],
    ['sangeeta', 'ಭಕ್ತಿ ಸಂಗೀತ'], ['samskruti', 'ಗೃಹ ಸಂಸ್ಕೃತಿ'], ['settings', 'Settings']
  ];
  const key = 'madhvanidhi-dashboard-shortcuts';
  let choices = ['panchanga', 'japa', 'grantha'];
  try {
    const saved = JSON.parse(localStorage.getItem(key));
    if (Array.isArray(saved) && saved.length === 3) choices = saved;
  } catch {}
  choices.forEach((choice, index) => {
    const label = document.createElement('label');
    label.className = 'cds--label';
    label.textContent = 'Shortcut ' + (index + 1);
    const select = document.createElement('select');
    select.className = 'cds--select-input';
    options.forEach(([id, name]) => select.add(new Option(name, id)));
    select.value = choice;
    select.addEventListener('change', () => {
      choices[index] = select.value;
      localStorage.setItem(key, JSON.stringify(choices));
      window.dispatchEvent(new Event('dashboard-shortcuts-changed'));
    });
    label.append(select);
    host.append(label);
  });
})();
