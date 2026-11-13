const granthaIds = ['stotra', 'suktas', 'mantras', 'hadugalu'];
function selectGranthaTab(id) {
  const current = ({ sukta: 'suktas', mantra: 'mantras' })[id] || id;
  for (const key of granthaIds) {
    const selected = key === current;
    const tab = document.getElementById('grantha-tab-' + key);
    tab.tabIndex = selected ? 0 : -1;
    tab.setAttribute('aria-selected', String(selected));
    document.getElementById(tab.getAttribute('aria-controls')).hidden = !selected;
  }
}
granthaIds.forEach((id, index) => {
  const tab = document.getElementById('grantha-tab-' + id);
  tab.addEventListener('click', () => selectGranthaTab(id));
  tab.addEventListener('keydown', event => {
    const positions = { Home: 0, End: 3, ArrowLeft: (index + 3) % 4, ArrowRight: (index + 1) % 4 };
    if (!(event.key in positions)) return;
    event.preventDefault();
    const next = granthaIds[positions[event.key]];
    selectGranthaTab(next);
    document.getElementById('grantha-tab-' + next).focus();
  });
});
