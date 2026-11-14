window.consumeSongsBack = () => {
  const panel = [...document.querySelectorAll('[data-song-collection]')].find(panel =>
    !panel.hidden && panel.closest('.view, .page')?.classList.contains('is-active') &&
    panel.querySelector('.songs-reader') && !panel.querySelector('.songs-reader').hidden);
  if (!panel) return false;
  panel.querySelector('.songs-back').click();
  return true;
};
(async () => {
  const panels = [...document.querySelectorAll('[data-song-collection]')];
  panels.forEach(panel => panel.textContent = 'Loading songs…');
  const favoriteKey = 'madhvanidhi-favorite-songs';
  let favorites;
  try { favorites = new Set(JSON.parse(localStorage.getItem(favoriteKey)) || []); } catch { favorites = new Set(); }
  const views = [];
  function saveFavorites() {
    localStorage.setItem(favoriteKey, JSON.stringify([...favorites]));
    views.forEach(view => view.refresh());
  }
  function marker(item) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'cds--btn cds--btn--ghost song-favorite';
    function sync() {
      const on = favorites.has(item.id);
      button.innerHTML = on ? "<svg viewBox=\"0 0 32 32\" width=\"20\" height=\"20\" aria-hidden=\"true\" fill=\"currentColor\"><path d=\"M16,2l-4.55,9.22L1.28,12.69l7.36,7.18L6.9,30,16,25.22,25.1,30,23.36,19.87l7.36-7.17L20.55,11.22Z\"/></svg>" : "<svg viewBox=\"0 0 32 32\" width=\"20\" height=\"20\" aria-hidden=\"true\" fill=\"currentColor\"><path d=\"M16,6.52l2.76,5.58.46,1,1,.15,6.16.89L22,18.44l-.75.73.18,1,1.05,6.13-5.51-2.89L16,23l-.93.49L9.56,26.34l1-6.13.18-1L10,18.44,5.58,14.09l6.16-.89,1-.15.46-1L16,6.52M16,2l-4.55,9.22L1.28,12.69l7.36,7.18L6.9,30,16,25.22,25.1,30,23.36,19.87l7.36-7.17L20.55,11.22Z\"/></svg>";
      button.setAttribute('aria-pressed', String(on));
      button.setAttribute('aria-label', (on ? 'Remove from favorites: ' : 'Add to favorites: ') + (item.kn || item.en));
    }
    button.addEventListener('click', () => {
      if (favorites.has(item.id)) favorites.delete(item.id); else favorites.add(item.id);
      saveFavorites();
    });
    sync();
    return {button, sync};
  }
  try {
    const response = await fetch('../songs-catalogue.json');
    if (!response.ok) throw new Error('Catalogue unavailable');
    const catalogue = await response.json();
    document.getElementById('sangeeta-tab-favorites')?.addEventListener('click', () => {
      const panel = document.getElementById('sangeeta-panel-favorites');
      panel?.querySelector('.songs-back')?.click();
      panel?.querySelector('input')?.dispatchEvent(new Event('input'));
    });
    panels.forEach(panel => {
      const collection = catalogue.find(group => group.id === panel.dataset.songCollection);
      panel.innerHTML = '<div class="songs-browse"><div class="songs-toolbar"><label class="cds--label">Search songs<input class="cds--search-input" type="search"></label><label class="cds--label">Author<select class="cds--select-input"></select></label></div><p class="cds--label songs-results" role="status"></p><div class="songs-list"></div></div><article class="songs-reader" hidden><button type="button" class="cds--btn cds--btn--ghost songs-back">← Back to songs</button><h2></h2><p class="cds--label songs-author"></p><div class="songs-lyrics"></div><div class="songs-reader-actions"><button type="button" class="cds--btn cds--btn--tertiary songs-prev">Previous song</button><button type="button" class="cds--btn cds--btn--tertiary songs-next">Next song</button></div></article>';
      const browse = panel.querySelector('.songs-browse');
      const reader = panel.querySelector('.songs-reader');
      const search = panel.querySelector('input');
      const author = panel.querySelector('select');
      const list = panel.querySelector('.songs-list');
      const results = panel.querySelector('.songs-results');
      let filtered = [];
      let selected = 0;
      let origin;
      const allSongs = catalogue.flatMap(group => group.items);
      const items = panel.dataset.songCollection === 'favorites' ? allSongs : collection?.items || [];
      let readerItem;
      let readerMarker;
      function syncReader() {
        if (!readerItem) return;
        readerMarker?.button.remove();
        readerMarker = marker(readerItem);
        reader.querySelector('h2').after(readerMarker.button);
      }
      author.add(new Option('All authors', ''));
      [...new Set(items.map(item => item.author).filter(Boolean))].sort().forEach(name => author.add(new Option(name, name)));
      function open(index) {
        selected = index;
        const item = filtered[index];
        readerItem = item;
        syncReader();
        reader.querySelector('h2').textContent = item.kn || item.en;
        reader.querySelector('.songs-author').textContent = item.author;
        reader.querySelector('.songs-lyrics').textContent = item.body;
        reader.querySelector('.songs-prev').disabled = index === 0;
        reader.querySelector('.songs-next').disabled = index === filtered.length - 1;
        browse.hidden = true;
        reader.hidden = false;
        reader.scrollTop = 0;
        reader.querySelector('.songs-back').focus();
      }
      function paint() {
        const query = search.value.trim().toLocaleLowerCase();
        filtered = items.filter(item => (panel.dataset.songCollection !== 'favorites' || favorites.has(item.id)) && (!author.value || item.author === author.value) && [item.kn, item.en, item.author].join(' ').toLocaleLowerCase().includes(query));
        results.textContent = filtered.length + ' songs';
        list.replaceChildren();
        filtered.forEach((item, index) => {
          const row = document.createElement('div');
          row.className = 'songs-row';
          const openButton = document.createElement('button');
          openButton.type = 'button';
          openButton.dataset.songId = item.id;
          openButton.className = 'song-open';
          const title = document.createElement('strong');
          title.textContent = item.kn || item.en;
          const subtitle = document.createElement('span');
          subtitle.textContent = item.author;
          openButton.append(title, subtitle);
          openButton.addEventListener('click', () => { origin = openButton; open(index); });
          row.append(openButton, marker(item).button);
          list.append(row);
        });
        if (!filtered.length) list.textContent = panel.dataset.songCollection === 'favorites' && !favorites.size
          ? 'No favorites yet. Use the star beside a song to add it here.'
          : 'No matching songs. Try another title or author.';
      }
      reader.querySelector('.songs-back').addEventListener('click', () => {
        const position = list.scrollTop;
        const id = readerItem?.id;
        reader.hidden = true;
        browse.hidden = false;
        paint();
        list.scrollTop = position;
        const target = [...list.querySelectorAll('.song-open')].find(button => button.dataset.songId === id);
        (target || search).focus({preventScroll: true});
      });
      reader.querySelector('.songs-prev').addEventListener('click', () => open(selected - 1));
      reader.querySelector('.songs-next').addEventListener('click', () => open(selected + 1));
      search.addEventListener('input', paint);
      author.addEventListener('change', paint);
      views.push({refresh: () => {
        if (reader.hidden) paint(); else syncReader();
      }});
      paint();
    });
  } catch {
    panels.forEach(panel => panel.textContent = 'Unable to load songs. Reload the preview to try again.');
  }
})();
