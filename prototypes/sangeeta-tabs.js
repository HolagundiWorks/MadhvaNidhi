const sangeetaIds = ['hadugalu', 'suladhigalu', 'ugabhogagalu', 'favorites'];
function selectSangeetaTab(id) {
  const current = ({ sukta: 'suktas', mantra: 'mantras' })[id] || id;
  for (const key of sangeetaIds) {
    const selected = key === current;
    const tab = document.getElementById('sangeeta-tab-' + key);
    if (!tab) continue;
    tab.tabIndex = selected ? 0 : -1;
    tab.setAttribute('aria-selected', String(selected));
    const panel = document.getElementById(tab.getAttribute('aria-controls'));
    if (panel) panel.hidden = !selected;
  }
}
sangeetaIds.forEach((id, index) => {
  const tab = document.getElementById('sangeeta-tab-' + id);
  if (!tab) return;
  tab.addEventListener('click', () => selectSangeetaTab(id));
  tab.addEventListener('keydown', event => {
    const n = sangeetaIds.length;
    const positions = {
      Home: 0,
      End: n - 1,
      ArrowLeft: (index + n - 1) % n,
      ArrowRight: (index + 1) % n
    };
    if (!(event.key in positions)) return;
    event.preventDefault();
    const next = sangeetaIds[positions[event.key]];
    selectSangeetaTab(next);
    document.getElementById('sangeeta-tab-' + next)?.focus();
  });
});
