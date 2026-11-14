(() => {
  const vault = window.madhvaUserVault;
  const records = window.madhvaJapaSession;
  const host = document.querySelector('.dashboard-japa');
  const key = 'madhvanidhi-dashboard-japas';
  let choices = [];
  try { choices = JSON.parse(localStorage.getItem(key)) || []; } catch {}
  if (!Array.isArray(choices)) choices = [];
  let profile = vault.DEFAULTS;
  const tiles = [];
  function openJapa(id) {
    if (!id) return;
    window.madhvaShell.navigate('nityakarma', 'japa');
    window.dispatchEvent(new CustomEvent('japa-select', {detail: id}));
  }
  function paint() {
    const days = records.dayRecords();
    const session = records.loadSession();
    tiles.forEach(({select, fields}, index) => {
      const id = index ? select.value : 'gayatri';
      const target = profile.japaTargets?.[id];
      const total = records.getCumulative(id);
      const today = days[id] || (session.mantra === id ? session : {});
      const values = id ? [
        vault.formatIndianCount(today.count || 0) + '/' + vault.formatIndianCount(today.dailyTarget || target?.daily || vault.DEFAULT_DAILY),
        vault.formatIndianCount(vault.sankalpa(target?.major, total)),
        vault.formatIndianCount(total)
      ] : [];
      fields.hidden = !id;
      const title = fields.parentElement.querySelector('button');
      title.textContent = vault.DIKSHA_MANTRAS.find(item => item.id === id)?.label.split(' · ')[0] || '—';
      title.disabled = !id;
      fields.querySelectorAll('dd').forEach((el, i) => el.textContent = values[i] || '');
    });
    const label = document.getElementById('dash-japa-count');
    if (label) label.textContent = vault.formatIndianCount(session.count);
  }
  for (let index = 0; index < 5; index++) {
    const tile = document.createElement('article');
    tile.className = 'panel dash-japa-slot';
    const select = { value: index ? choices[index - 1] || '' : 'gayatri' };
    const title = document.createElement('button');
    title.type = 'button';
    title.className = 'cds--btn cds--btn--ghost';
    title.addEventListener('click', () => openJapa(index ? select.value : 'gayatri'));
    tile.append(title);
    const fields = document.createElement('dl');
    fields.innerHTML = '<dt>Daily</dt><dd></dd><dt>Sankalpa</dt><dd></dd><dt>Cumulative</dt><dd></dd>';
    tile.append(fields);
    host.append(tile);
    tiles.push({select, fields});
  }
  async function loadProfile() {
    profile = await vault.loadLocal().catch(() => null) || vault.DEFAULTS;
    paint();
  }
  window.addEventListener('dashboard-japa-slots-changed', () => {
    try { choices = JSON.parse(localStorage.getItem(key)) || []; } catch { return; }
    tiles.forEach(({select}, index) => { if (index) select.value = choices[index - 1] || ''; });
    paint();
  });
  window.paintDashboardJapas = paint;
  window.addEventListener('japa-record-changed', paint);
  window.addEventListener('japa-profile-saved', loadProfile);
  paint();
  loadProfile();
})();
