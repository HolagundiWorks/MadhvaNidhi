const settingsTabs = [...document.querySelectorAll('#settings-tabs [role="tab"]')];
function selectSettingsTab(name) {
  for (const tab of settingsTabs) {
    const selected = tab.id === 'settings-tab-' + name;
    tab.tabIndex = selected ? 0 : -1;
    tab.setAttribute('aria-selected', String(selected));
    document.getElementById(tab.getAttribute('aria-controls')).hidden = !selected;
  }
}
settingsTabs.forEach((tab, index) => {
  tab.onclick = () => selectSettingsTab(tab.id.slice('settings-tab-'.length));
  tab.onkeydown = event => {
    const targets = { ArrowRight: (index + 1) % 3, ArrowLeft: (index + 2) % 3, Home: 0, End: 2 };
    if (!(event.key in targets)) return;
    event.preventDefault();
    const next = settingsTabs[targets[event.key]];
    next.focus();
    next.click();
  };
});
