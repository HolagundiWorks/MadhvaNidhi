const settingsTabs = [...document.querySelectorAll('#settings-tabs [role="tab"]')];
function selectSettingsTab(name) {
  for (const tab of settingsTabs) {
    const selected = tab.id === 'settings-tab-' + name;
    tab.tabIndex = selected ? 0 : -1;
    tab.setAttribute('aria-selected', String(selected));
    const panel = document.getElementById(tab.getAttribute('aria-controls'));
    if (panel) panel.hidden = !selected;
  }
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
