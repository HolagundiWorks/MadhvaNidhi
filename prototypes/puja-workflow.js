(async () => {
  const root = document.querySelector('[data-puja-workflow]');
  if (!root) return;
  const picker = root.querySelector('[data-puja-picker]');
  const reader = root.querySelector('.puja-section-reader');
  const sectionPicker = root.querySelector('[data-puja-breadcrumb-section]');
  const key = 'madhvanidhi-puja-reading-position';
  let state = { rite: '', positions: {} };
  try {
    const saved = JSON.parse(localStorage.getItem(key));
    if (saved && saved.positions && typeof saved.positions === 'object') state = saved;
  } catch {}
  try {
    const response = await fetch('../puja-catalogue.json');
    if (!response.ok) throw new Error('Unavailable');
    const rites = await response.json();
    rites.forEach(rite => picker.add(new Option(rite.title, rite.id)));
    picker.value = rites.some(rite => rite.id === state.rite) ? state.rite : rites[0]?.id || '';
    function render(focusReader = false) {
      const rite = rites.find(rite => rite.id === picker.value);
      if (!rite) return;
      state.rite = rite.id;
      const index = Math.min(rite.sections.length - 1, Math.max(0, Number(state.positions[rite.id]) || 0));
      state.positions[rite.id] = index;
      root.querySelector('[data-puja-breadcrumb-name]').textContent = rite.title;
      sectionPicker.replaceChildren();
      rite.sections.forEach((section, position) => sectionPicker.add(new Option(section.title, String(position))));
      sectionPicker.value = String(index);
      reader.querySelector('h2').textContent = rite.sections[index].title;
      reader.querySelector('.puja-section-body').textContent = rite.sections[index].body;
      reader.scrollTop = 0;
      root.querySelector('progress').setAttribute('aria-valuetext', 'Section ' + (index + 1) + ' of ' + rite.sections.length);
      root.querySelector('progress').value = 100 * (index + 1) / rite.sections.length;
      if (focusReader) reader.focus();
      try { localStorage.setItem(key, JSON.stringify(state)); } catch {}
    }
    picker.addEventListener('change', () => render());
    sectionPicker.addEventListener('change', () => {
      state.positions[picker.value] = Number(sectionPicker.value);
      render(true);
    });
    render();
  } catch {
    reader.textContent = 'Unable to load Puja procedures. Reload to try again.';
  }
})();
