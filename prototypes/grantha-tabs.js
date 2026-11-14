const granthaIds = ['stotra', 'suktas', 'mantras', 'namavali'];
function selectGranthaTab(id) {
  const current = ({ sukta: 'suktas', mantra: 'mantras' })[id] || id;
  for (const key of granthaIds) {
    const selected = key === current;
    const tab = document.getElementById('grantha-tab-' + key);
    if (!tab) continue;
    tab.tabIndex = selected ? 0 : -1;
    tab.setAttribute('aria-selected', String(selected));
    const panel = document.getElementById(tab.getAttribute('aria-controls'));
    if (panel) panel.hidden = !selected;
  }
}
granthaIds.forEach((id, index) => {
  const tab = document.getElementById('grantha-tab-' + id);
  if (!tab) return;
  tab.addEventListener('click', () => selectGranthaTab(id));
  tab.addEventListener('keydown', event => {
    const n = granthaIds.length;
    const positions = {
      Home: 0,
      End: n - 1,
      ArrowLeft: (index + n - 1) % n,
      ArrowRight: (index + 1) % n
    };
    if (!(event.key in positions)) return;
    event.preventDefault();
    const next = granthaIds[positions[event.key]];
    selectGranthaTab(next);
    document.getElementById('grantha-tab-' + next)?.focus();
  });
});
