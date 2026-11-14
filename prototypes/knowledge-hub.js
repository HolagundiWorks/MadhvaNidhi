/* MadhvaNidhi knowledge hub mockup: Wiki, Notifications, Achievements, sharing posters.
 * Mockup only. Data lives in prototypes/knowledge/*.json. Viewer-local state uses localStorage in try/catch.
 * Content marked "pending" is structure, not verified fact. */
(function () {
  const KEY_BOOKMARKS = 'mn.wiki.bookmarks';
  const KEY_NOTIFY = 'mn.notify';
  const ACCENT = '#FF4F18';
  const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const store = {
    get(key, fallback) { try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : fallback; } catch (e) { return fallback; } },
    set(key, value) { try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* storage unavailable */ } }
  };
  const fmt = n => Number(n).toLocaleString('en-IN');

  /* ---------- Wiki ---------- */
  const wikiRoot = document.querySelector('[data-wiki-root]');
  let wiki = null;
  let wikiState = { category: 'all', query: '', article: null };

  async function loadWiki() {
    if (wiki) return wiki;
    const res = await fetch('../knowledge/wiki.json');
    wiki = await res.json();
    return wiki;
  }

  function articleById(id) { return wiki.articles.find(a => a.id === id); }

  function renderWiki() {
    if (!wikiRoot || !wiki) return;
    const bookmarks = store.get(KEY_BOOKMARKS, []);
    if (wikiState.article) { wikiRoot.innerHTML = renderArticle(articleById(wikiState.article), bookmarks); return; }
    const q = wikiState.query.trim().toLowerCase();
    const items = wiki.articles.filter(a =>
      (wikiState.category === 'all' || a.category === wikiState.category) &&
      (!q || [a.title_en, a.title_kn, a.summary].join(' ').toLowerCase().includes(q)));
    const chips = [{ id: 'all', title_en: 'All', title_kn: 'ಎಲ್ಲ' }, ...wiki.categories].map(c =>
      `<button type="button" class="cds--btn cds--btn--ghost wiki-chip" aria-pressed="${wikiState.category === c.id}" data-wiki-cat="${esc(c.id)}">${esc(c.title_en)}</button>`).join('');
    wikiRoot.innerHTML = `
      <div class="cds--form-item">
        <label class="cds--label" for="wiki-search">Search · ಹುಡುಕಿ</label>
        <input class="cds--text-input" id="wiki-search" type="search" value="${esc(wikiState.query)}" placeholder="Matha, Peethadhipati, concept">
      </div>
      <div class="wiki-chips" role="group" aria-label="Categories">${chips}</div>
      <p class="cds--label-01">${items.length} articles · offline bundled</p>
      <ul class="wiki-list">${items.map(a => `
        <li><button type="button" class="cds--tile wiki-item" data-wiki-open="${esc(a.id)}">
          <span class="cds--body-compact-01">${esc(a.title_en)} · ${esc(a.title_kn)}</span>
          <span class="cds--label-01">${esc(a.summary)}</span>
          <span class="cds--tag cds--tag--${a.status === 'sourced' ? 'green' : 'gray'}">${a.status === 'pending' ? 'Pending source' : esc(a.status)}</span>
          ${bookmarks.includes(a.id) ? '<span class="cds--label-01">Bookmarked</span>' : ''}
        </button></li>`).join('') || '<li class="cds--body-01">No matching article.</li>'}</ul>`;
  }

  function renderArticle(a, bookmarks) {
    if (!a) return '<p class="cds--body-01">Article not found.</p>';
    const saved = bookmarks.includes(a.id);
    const sections = a.sections.map(s => `
      <section class="cds--tile wiki-section">
        <h3 class="cds--heading-03">${esc(s.heading)}</h3>
        <p class="cds--body-01">${s.body ? esc(s.body) : '<span class="cds--tag cds--tag--gray">Pending source</span> Content not yet verified.'}</p>
      </section>`).join('');
    const related = (a.related || []).map(id => articleById(id)).filter(Boolean).map(r =>
      `<button type="button" class="cds--btn cds--btn--tertiary" data-wiki-open="${esc(r.id)}">${esc(r.title_en)}</button>`).join('');
    const sources = a.sources.length ? a.sources.map(s => `<li>${esc(s.title)} — ${esc(s.note || '')}</li>`).join('')
      : '<li class="cds--label-01">No citation yet. Add one before this article is marked sourced.</li>';
    const loc = a.location ? `<p class="cds--label-01">Location: ${esc(a.location.place)} · map coordinates ${a.location.status === 'pending' ? 'pending' : esc(a.location.lat + ', ' + a.location.lng)}</p>` : '';
    return `
      <div class="wiki-toolbar">
        <button type="button" class="cds--btn cds--btn--ghost" data-wiki-back>Back to list</button>
        <button type="button" class="cds--btn cds--btn--ghost" aria-pressed="${saved}" data-wiki-bookmark="${esc(a.id)}">${saved ? 'Bookmarked' : 'Bookmark'}</button>
      </div>
      <h2 class="cds--heading-04">${esc(a.title_en)} · ${esc(a.title_kn)}</h2>
      <p class="cds--body-01">${esc(a.summary)}</p>
      ${loc}
      ${sections}
      <h3 class="cds--heading-03">Sources</h3>
      <ul class="wiki-sources">${sources}</ul>
      <h3 class="cds--heading-03">Related</h3>
      <div class="wiki-chips">${related || '<span class="cds--label-01">None</span>'}</div>`;
  }

  /* ---------- Notifications ---------- */
  const notifyRoot = document.querySelector('[data-notify-root]');
  const NOTIFY_DEFAULTS = {
    master: true, quietStart: '22:00', quietEnd: '05:30', triviaFrequency: 'weekly', achievements: true, sharingPrompts: true,
    categories: { pratah: true, madhyahnika: true, sayam: true, gayatri: true, japa: false, stotra: false, observances: true, trivia: false }
  };
  const NOTIFY_ROWS = [
    ['pratah', 'Pratah Sandhyavandana', '06:00'], ['madhyahnika', 'Madhyahnika Sandhyavandana', '12:00'],
    ['sayam', 'Sayam Sandhyavandana', '18:00'], ['gayatri', 'Daily Gayatri Japa', '06:30'],
    ['japa', 'Other Japa activities', '20:00'], ['stotra', 'Daily stotra or devotional reading', '07:00'],
    ['observances', 'Important rituals and observances', '—'], ['trivia', 'Knowledge and trivia (linked to wiki)', '—']
  ];
  const NOTIFY_HISTORY_DEMO = [
    'Demo · Pratah Sandhyavandana reminder · 06:00',
    'Demo · Achievement unlocked: 25-day Sandhyavandana · yesterday',
    'Demo · Trivia: Guru Parampara fact (linked to wiki) · last week'
  ];

  function loadNotify() { return { ...NOTIFY_DEFAULTS, ...store.get(KEY_NOTIFY, {}), categories: { ...NOTIFY_DEFAULTS.categories, ...(store.get(KEY_NOTIFY, {}).categories || {}) } }; }

  function renderNotify() {
    if (!notifyRoot) return;
    const s = loadNotify();
    const toggle = (id, label, on, disabled) => `
      <label class="cds--toggle"><input type="checkbox" class="cds--toggle__input" data-notify="${id}" ${on ? 'checked' : ''} ${disabled ? 'disabled' : ''}>
      <span class="cds--toggle__label">${esc(label)}</span></label>`;
    notifyRoot.innerHTML = `
      <div class="cds--tile">
        ${toggle('master', 'Notifications on', s.master)}
        <p class="cds--label-01">Android notification permission is requested only when you turn this on. Reminders use local scheduling.</p>
      </div>
      <div class="cds--tile">
        <h3 class="cds--heading-03">Daily ritual reminders</h3>
        ${NOTIFY_ROWS.slice(0, 8).map(([id, label, time]) => `
          <div class="notify-row">${toggle(id, label, s.categories[id], !s.master)}
            <span class="cds--label-01">${esc(time)}</span></div>`).join('')}
      </div>
      <div class="cds--tile">
        <h3 class="cds--heading-03">Preferences</h3>
        <div class="cds--form-item"><label class="cds--label" for="notify-trivia">Trivia frequency</label>
          <select class="cds--select-input" id="notify-trivia" data-notify-select="triviaFrequency" ${!s.master ? 'disabled' : ''}>
            ${['off', 'daily', 'weekly'].map(v => `<option value="${v}" ${s.triviaFrequency === v ? 'selected' : ''}>${v}</option>`).join('')}
          </select></div>
        <div class="cds--form-item"><label class="cds--label" for="notify-qs">Quiet hours start</label>
          <input class="cds--text-input" type="time" id="notify-qs" data-notify-select="quietStart" value="${esc(s.quietStart)}" ${!s.master ? 'disabled' : ''}></div>
        <div class="cds--form-item"><label class="cds--label" for="notify-qe">Quiet hours end</label>
          <input class="cds--text-input" type="time" id="notify-qe" data-notify-select="quietEnd" value="${esc(s.quietEnd)}" ${!s.master ? 'disabled' : ''}></div>
        ${toggle('achievements', 'Achievement notifications', s.achievements, !s.master)}
        ${toggle('sharingPrompts', 'Sharing prompts', s.sharingPrompts, !s.master)}
      </div>
      <div class="cds--tile">
        <h3 class="cds--heading-03">Notification history</h3>
        <ul class="wiki-sources">${NOTIFY_HISTORY_DEMO.map(h => `<li>${esc(h)}</li>`).join('')}</ul>
      </div>`;
  }

  function saveNotifyFromDom() {
    const next = loadNotify();
    notifyRoot.querySelectorAll('[data-notify]').forEach(el => {
      const id = el.dataset.notify;
      if (id === 'master' || id === 'achievements' || id === 'sharingPrompts') next[id] = el.checked;
      else next.categories[id] = el.checked;
    });
    notifyRoot.querySelectorAll('[data-notify-select]').forEach(el => { next[el.dataset.notifySelect] = el.value; });
    store.set(KEY_NOTIFY, next);
    renderNotify();
  }

  /* ---------- Achievements and posters ---------- */
  const achieveRoot = document.querySelector('[data-achieve-root]');
  let achieve = null;
  let selectedAchievement = null;

  async function loadAchieve() {
    if (achieve) return achieve;
    achieve = await (await fetch('../knowledge/achievements.json')).json();
    return achieve;
  }

  function earnedList() {
    const d = achieve.demo;
    const out = [];
    for (const series of achieve.series) {
      if (series.kind === 'daily') {
        out.push({ key: series.id, label: series.title_en, value: 1, number: '1', message: series.message_en, category: series.category, earned: true });
      } else if (series.kind === 'streak') {
        for (const m of series.milestones) out.push({ key: `${series.id}-${m}`, label: `${m}-day streak`, value: d.sandhya_streak_days, target: m, number: String(m), message: series.message_template.replace('{n}', m), category: series.category, earned: d.sandhya_streak_days >= m });
      } else if (series.kind === 'cumulative') {
        const total = series.id === 'gayatri-japa' ? d.gayatri_cumulative : d.japa_cumulative;
        for (const m of series.milestones) out.push({ key: `${series.id}-${m}`, label: `${fmt(m)} Japa`, value: total, target: m, number: fmt(m), message: series.message_template.replace('{n}', fmt(m)), category: series.category, earned: total >= m, configure: series.status === 'configure' });
      }
    }
    return out;
  }

  function posterSVG(model, format) {
    const W = 1080, H = format === 'portrait' ? 1350 : 1080;
    const cx = W / 2;
    const fy = H * 0.27;
    const petals = Array.from({ length: 8 }, (_, i) => `<ellipse cx="${cx}" cy="${fy - 70}" rx="30" ry="70" fill="none" stroke="${ACCENT}" stroke-width="4" transform="rotate(${i * 45} ${cx} ${fy})"/>`).join('');
    const lines = (text, size, max) => {
      const words = String(text).split(' ');
      const rows = []; let cur = '';
      for (const w of words) { const next = cur ? cur + ' ' + w : w; if (next.length > max) { rows.push(cur); cur = w; } else cur = next; }
      if (cur) rows.push(cur);
      return rows.map((r, i) => `<text x="${cx}" y="${0}" dy="${i * size * 1.35}" font-size="${size}" text-anchor="middle" fill="#F4F4F4">${esc(r)}</text>`);
    };
    const place = (rows, y) => rows.map(r => r.replace('y="0"', `y="${y}"`)).join('');
    const category = esc(model.label || '');
    const msgRows = lines(model.message, 48, 34);
    const topY = H * 0.62;
    return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" font-family="IBM Plex Sans, system-ui, sans-serif">
      <rect width="${W}" height="${H}" fill="#161616"/>
      <rect x="60" y="60" width="${W - 120}" height="${H - 120}" fill="none" stroke="#393939" stroke-width="2"/>
      <g>${petals}<circle cx="${cx}" cy="${fy}" r="18" fill="${ACCENT}"/></g>
      <text x="${cx}" y="${H * 0.1}" font-size="34" text-anchor="middle" fill="#C6C6C6" letter-spacing="6">MADHVANIDHI</text>
      <text x="${cx}" y="${H * 0.47}" font-size="34" text-anchor="middle" fill="#C6C6C6">${category}</text>
      <text x="${cx}" y="${H * 0.55 + 40}" font-size="${model.number.length > 6 ? 118 : 170}" font-weight="600" text-anchor="middle" fill="${ACCENT}">${esc(model.number)}</text>
      ${place(msgRows, topY + 80)}
      ${model.date ? `<text x="${cx}" y="${H - 150}" font-size="30" text-anchor="middle" fill="#C6C6C6">${esc(model.date)}</text>` : ''}
      <text x="${cx}" y="${H - 100}" font-size="26" text-anchor="middle" fill="#8D8D8D">Made with MadhvaNidhi</text>
    </svg>`;
  }

  function promoSVG(format) {
    return posterSVG({ label: 'MadhvaNidhi', number: 'ನಿತ್ಯ', message: 'Sandhyavandana, Japa, stotra and Madhva Sampradaya knowledge in one app. Offline and private.', date: '' }, format);
  }

  function svgToPng(svg, size) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = size.w; canvas.height = size.h;
        canvas.getContext('2d').drawImage(img, 0, 0, size.w, size.h);
        canvas.toBlob(b => (b ? resolve(b) : reject(new Error('png failed'))), 'image/png');
      };
      img.onerror = reject;
      img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
    });
  }

  function renderAchieve() {
    if (!achieveRoot || !achieve) return;
    const list = earnedList();
    const demo = achieve.demo;
    const tiles = list.map(m => {
      const pct = m.target ? Math.min(100, Math.round(m.value / m.target * 100)) : 100;
      return `<li class="achieve-row">
        <button type="button" class="cds--tile achieve-tile" data-achieve="${esc(m.key)}" ${m.earned ? '' : 'disabled'}>
          <span class="cds--body-compact-01">${esc(m.label)}${m.configure ? ' · configure' : ''}</span>
          <span class="cds--label-01">${m.earned ? 'Earned' : 'Progress ' + pct + '%'}</span>
          <div class="achieve-bar" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${pct}" aria-label="${esc(m.label)} progress"><span style="width:${pct}%"></span></div>
        </button></li>`;
    }).join('');
    const preview = selectedAchievement ? list.find(m => m.key === selectedAchievement) : null;
    achieveRoot.innerHTML = `
      <p class="cds--label-01">Demo progress: ${demo.sandhya_streak_days}-day streak · ${fmt(demo.gayatri_cumulative)} Gayatri · ${fmt(demo.japa_cumulative)} Japa. Each milestone is awarded once.</p>
      <ul class="achieve-list">${tiles}</ul>
      <section class="cds--tile" aria-live="polite">
        <h3 class="cds--heading-03">Poster preview</h3>
        ${preview ? `
          <img class="achieve-preview" alt="Poster for ${esc(preview.label)}" src="data:image/svg+xml;charset=utf-8,${encodeURIComponent(posterSVG({ label: preview.category === 'sandhya' ? 'Sandhyavandana' : preview.category === 'gayatri' ? 'Gayatri Japa' : 'Japa', number: preview.number, message: preview.message, date: 'Demo date' }, 'square'))}">
          <div class="wiki-toolbar">
            <button type="button" class="cds--btn cds--btn--primary" data-achieve-share="${esc(preview.key)}">Share</button>
            <button type="button" class="cds--btn cds--btn--secondary" data-achieve-save="${esc(preview.key)}">Save image</button>
            <button type="button" class="cds--btn cds--btn--ghost" data-achieve-dismiss>Dismiss</button>
          </div>` : '<p class="cds--body-01">Select an earned milestone to preview its poster. Nothing is shared until you press Share.</p>'}
      </section>
      <section class="cds--tile">
        <h3 class="cds--heading-03">Share MadhvaNidhi</h3>
        <p class="cds--body-01">Promotional poster. Opens your device share sheet only when you choose.</p>
        <div class="wiki-toolbar">
          <button type="button" class="cds--btn cds--btn--primary" data-promo-share>Share MadhvaNidhi</button>
          <button type="button" class="cds--btn cds--btn--secondary" data-promo-save>Save promo image</button>
        </div>
      </section>`;
  }

  async function shareOrSave(svg, name, share) {
    const blob = await svgToPng(svg, { w: 1080, h: svg.includes('height="1350"') ? 1350 : 1080 });
    if (share) {
      const file = new File([blob], name, { type: 'image/png' });
      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        await navigator.share({ files: [file], title: 'MadhvaNidhi' });
        return 'shared';
      }
      return 'unsupported';
    }
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = name; a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    return 'saved';
  }

  function note(msg) {
    const el = document.getElementById('knowledge-status');
    if (el) el.textContent = msg;
  }

  /* ---------- Event wiring ---------- */
  wikiRoot?.addEventListener('click', e => {
    const t = e.target.closest('button');
    if (!t) return;
    if (t.dataset.wikiCat) { wikiState.category = t.dataset.wikiCat; wikiState.article = null; renderWiki(); }
    else if (t.dataset.wikiOpen) { wikiState.article = t.dataset.wikiOpen; renderWiki(); }
    else if (t.hasAttribute('data-wiki-back')) { wikiState.article = null; renderWiki(); }
    else if (t.dataset.wikiBookmark) {
      const list = store.get(KEY_BOOKMARKS, []);
      const id = t.dataset.wikiBookmark;
      store.set(KEY_BOOKMARKS, list.includes(id) ? list.filter(x => x !== id) : [...list, id]);
      renderWiki();
    }
  });
  wikiRoot?.addEventListener('input', e => {
    if (e.target.id === 'wiki-search') { wikiState.query = e.target.value; const pos = e.target.selectionStart; renderWiki(); const el = document.getElementById('wiki-search'); el?.focus(); el?.setSelectionRange(pos, pos); }
  });
  notifyRoot?.addEventListener('change', saveNotifyFromDom);
  achieveRoot?.addEventListener('click', async e => {
    const t = e.target.closest('button');
    if (!t) return;
    if (t.dataset.achieve) { selectedAchievement = t.dataset.achieve; renderAchieve(); return; }
    if (t.hasAttribute('data-achieve-dismiss')) { selectedAchievement = null; renderAchieve(); return; }
    if (t.dataset.achieveShare || t.dataset.achieveSave) {
      const key = t.dataset.achieveShare || t.dataset.achieveSave;
      const m = earnedList().find(x => x.key === key);
      if (!m) return;
      const svg = posterSVG({ label: m.category === 'sandhya' ? 'Sandhyavandana' : m.category === 'gayatri' ? 'Gayatri Japa' : 'Japa', number: m.number, message: m.message, date: 'Demo date' }, 'square');
      try {
        const result = await shareOrSave(svg, `madhvanidhi-${key}.png`, Boolean(t.dataset.achieveShare));
        note(result === 'shared' ? 'Shared.' : result === 'unsupported' ? 'Sharing is not available in this browser; use Save image.' : 'Image saved.');
      } catch (err) { note('Could not create the image.'); }
      return;
    }
    if (t.hasAttribute('data-promo-share') || t.hasAttribute('data-promo-save')) {
      try {
        const result = await shareOrSave(promoSVG('square'), 'madhvanidhi-promo.png', t.hasAttribute('data-promo-share'));
        note(result === 'shared' ? 'Shared.' : result === 'unsupported' ? 'Sharing is not available in this browser; use Save promo image.' : 'Promo image saved.');
      } catch (err) { note('Could not create the image.'); }
    }
  });

  window.madhvaKnowledge = {
    refreshWiki: () => loadWiki().then(renderWiki),
    refreshNotify: renderNotify,
    refreshAchieve: () => loadAchieve().then(renderAchieve),
    posterSVG, promoSVG
  };

  // Initial render: wiki loads on first use (tab click or initial render); others on demand.
  document.getElementById('grantha-tab-wiki')?.addEventListener('click', () => window.madhvaKnowledge.refreshWiki());
  if (wikiRoot) window.madhvaKnowledge.refreshWiki().catch(() => { wikiRoot.textContent = 'Wiki data could not be loaded.'; });
  document.getElementById('settings-tab-notifications')?.addEventListener('click', renderNotify);
  document.getElementById('settings-tab-achievements')?.addEventListener('click', () => window.madhvaKnowledge.refreshAchieve());
  renderNotify();
})();
