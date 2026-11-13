const samskrutiIds = ['aayadi', 'rangavalli', 'recipes'];
function selectSamskrutiTab(id) {
  for (const key of samskrutiIds) {
    const selected = key === id;
    const tab = document.getElementById('samskruti-tab-' + key);
    tab.tabIndex = selected ? 0 : -1;
    tab.setAttribute('aria-selected', String(selected));
    document.getElementById(tab.getAttribute('aria-controls')).hidden = !selected;
  }
}
samskrutiIds.forEach((id, index) => {
  const tab = document.getElementById('samskruti-tab-' + id);
  tab.addEventListener('click', () => selectSamskrutiTab(id));
  tab.addEventListener('keydown', event => {
    const positions = { Home: 0, End: 2, ArrowLeft: (index + 2) % 3, ArrowRight: (index + 1) % 3 };
    if (!(event.key in positions)) return;
    event.preventDefault();
    const next = samskrutiIds[positions[event.key]];
    selectSamskrutiTab(next);
    document.getElementById('samskruti-tab-' + next).focus();
  });
});

  (function bindAayadi() {
    const byId = id => document.getElementById(id);
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
  }());
