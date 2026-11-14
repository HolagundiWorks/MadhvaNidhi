(() => {
  const key = 'madhvanidhi-current-app-demo-week';
  let week = Array.from({length: 7}, (_, i) => i === 6 ? [true, true, false] : [true, true, true]);
  try { const saved = JSON.parse(localStorage.getItem(key)); if (saved?.length === 7) week = saved; } catch {}
  const days = ['ಭಾನು','ಸೋಮ','ಮಂಗಳ','ಬುಧ','ಗುರು','ಶುಕ್ರ','ಶನಿ'];
  const dialog = document.createElement('dialog');
  dialog.className = 'current-reader';
  dialog.id = 'current-practice-dialog';
  dialog.innerHTML = '<form method="dialog"><button>Close</button></form><h2></h2><div></div>';
  document.body.append(dialog);
  function openDay(index) {
    const date = new Date();
    date.setDate(date.getDate() - 6 + index);
    dialog.querySelector('h2').textContent = date.toLocaleDateString('en-IN', {day:'numeric', month:'long', year:'numeric'});
    const host = dialog.querySelector('div');
    host.replaceChildren();
    ['ಪ್ರಾತಃ · Morning','ಮಧ್ಯಾಹ್ನ · Noon','ಸಾಯಂ · Evening'].forEach((name, session) => {
      const label = document.createElement('label');
      const input = document.createElement('input');
      input.type = 'checkbox';
      input.checked = week[index][session];
      input.addEventListener('change', () => {
        week[index][session] = input.checked;
        localStorage.setItem(key, JSON.stringify(week));
        paintWeek();
      });
      label.append(input, document.createTextNode(name));
      host.append(label);
    });
    dialog.showModal();
  }
  function paintWeek() {
    const host = document.querySelector('[data-current-week]');
    host.replaceChildren();
    week.forEach((marks, index) => {
      const date = new Date();
      date.setDate(date.getDate() - 6 + index);
      const row = document.createElement('button');
      row.type = 'button';
      row.className = 'current-week-row' + (index === 6 ? ' today' : '');
      const label = document.createElement('span');
      label.className = 'day-name';
      const name = document.createElement('strong');
      name.textContent = days[date.getDay()];
      const detail = document.createElement('small');
      detail.textContent = date.toLocaleDateString('en-IN',{day:'numeric',month:'short'});
      label.append(name,detail);
      row.append(label);
      marks.forEach((done, session) => {
        const mark = document.createElement('span');
        mark.className = 'session-mark' + (done ? ' done' : '');
        mark.textContent = done ? '✓' : '—';
        mark.setAttribute('aria-label', ['Morning','Noon','Evening'][session]+(done?' complete':' pending'));
        row.append(mark);
      });
      row.addEventListener('click',()=>openDay(index));
      host.append(row);
    });
    const completed = week.filter(day => day.every(Boolean)).length;
    document.querySelector('[data-current-week-score]').textContent = completed + '/7';
    document.querySelector('[data-current-week-progress]').value = completed;
    document.querySelector('[data-current-summary]').textContent = week[6].filter(Boolean).length + '/3 · 5/7';
  }
  paintWeek();
  fetch('./native-icons.json').then(response => response.json()).then(icons => {
    const today = document.querySelector('.today-summary');
    today.firstChild.remove();
    today.insertAdjacentHTML('afterbegin', icons.ic_today);
    const weekHead = document.querySelector('.current-week-head');
    weekHead.insertAdjacentHTML('afterbegin', icons.ic_badge_week);
    weekHead.lastElementChild.innerHTML = icons.ic_morning + icons.ic_noon + icons.ic_evening;
    document.querySelector('.current-streak strong').innerHTML = icons.ic_badge_streak + ' 20';
    document.querySelector('.current-badges').innerHTML = '<span>' + icons.ic_badge_streak + '14</span><span>' + icons.ic_badge_japa + '30</span>';
    document.querySelectorAll('.current-shortcuts button').forEach((button, index) => {
      button.firstChild.remove();
      button.insertAdjacentHTML('afterbegin', [icons.ic_calendar, icons.ic_badge_japa, icons.ic_book][index]);
    });
  }).catch(() => {});
  const today = document.querySelector('.today-summary');
  today.removeAttribute('data-dashboard-dialog');
  // Use the current dashboard's day dialog instead of the design-reference dialog.
  today.addEventListener('click', event => { event.stopImmediatePropagation(); openDay(6); }, true);
  const japas = [
    ['gayatri','ಗಾಯತ್ರಿ',64,101,12540],
    ['narayana','ನಾರಾಯಣ',101,101,5001],
    ['rama','ಶ್ರೀರಾಮ',36,101,1108],
    ['hanuman','ಹನುಮಂತ',11,101,540],
    ['raghavendra','ರಾಘವೇಂದ್ರ',21,101,2208]
  ];
  const format = n => new Intl.NumberFormat('en-IN').format(n);
  japas.forEach(([id,name,count,target,total])=>{
    const button = document.createElement('button');
    button.className = 'current-japa';
    button.innerHTML = '<strong></strong><span class="japa-value"></span><progress max="100"></progress><small></small>';
    button.querySelector('strong').textContent = name + (count >= target ? ' ✓' : '');
    button.querySelector('.japa-value').textContent = format(count)+'/'+format(target);
    button.querySelector('progress').value = Math.min(100,count*100/target);
    button.querySelector('small').textContent = 'Σ '+format(total);
    button.addEventListener('click',()=>{
      window.madhvaShell.navigate('nityakarma','japa');
      window.dispatchEvent(new CustomEvent('japa-select',{detail:id}));
    });
    document.querySelector('[data-current-japas]').append(button);
  });
  async function puja() {
    const response = await fetch('../puja-catalogue.json');
    if (!response.ok) throw Error('Puja unavailable');
    const rites = await response.json();
    const select = document.querySelector('[data-current-puja-picker]');
    const skip = document.querySelector('[data-current-puja-skip]');
    const complete = document.querySelector('[data-current-puja-complete]');
    let state = {complete:false,skip:false};
    rites.forEach(rite=>select.add(new Option(rite.title,rite.id)));
    function paint() {
      const rite=rites.find(rite=>rite.id===select.value);
      document.querySelector('[data-current-puja-title]').textContent=rite.title;
      document.querySelector('[data-current-puja-crumb]').textContent='ಪೂಜೆ · '+rite.title;
      document.querySelector('[data-current-puja-body]').textContent=rite.sections.map(section=>section.title+'\n\n'+section.body).join('\n\n');
      skip.setAttribute('aria-pressed',String(state.skip));
      complete.setAttribute('aria-pressed',String(state.complete));
      document.querySelector('[data-current-puja-progress]').value=state.complete?100:0;
    }
    select.addEventListener('change',()=>{state={complete:false,skip:false};paint()});
    skip.addEventListener('click',()=>{state={skip:!state.skip,complete:false};paint()});
    complete.addEventListener('click',()=>{state={skip:false,complete:!state.complete};paint()});
    paint();
  }
  async function songs() {
    const response=await fetch('../songs-catalogue.json');
    if(!response.ok)throw Error('Songs unavailable');
    const groups=await response.json();
    const select=document.querySelector('[data-current-song-group]');
    groups.forEach(group=>select.add(new Option(group.title,group.id)));
    const reader=document.createElement('dialog');
    reader.className='current-reader';
    reader.innerHTML='<form method="dialog"><button>Back</button></form><h2></h2><pre></pre>';
    document.body.append(reader);
    function paint(){
      const group=groups.find(group=>group.id===select.value);
      const host=document.querySelector('[data-current-song-list]');
      host.replaceChildren();
      group.items.forEach(item=>{
        const button=document.createElement('button');
        button.className='current-song-row';
        button.textContent=item.kn||item.en;
        button.addEventListener('click',()=>{reader.querySelector('h2').textContent=item.kn||item.en;reader.querySelector('pre').textContent=item.body;reader.showModal()});
        host.append(button);
      });
    }
    select.addEventListener('change',paint);
    const playlists=document.querySelector('[data-current-playlists]');
    groups.forEach(group=>{
      const button=document.createElement('button');
      button.className='current-song-row';
      button.textContent=group.title+' · '+group.items.length;
      button.addEventListener('click',()=>{select.value=group.id;tab('songs');paint()});
      playlists.append(button);
    });
    function tab(id) {
      document.querySelector('[data-current-songs]').hidden=id!=='songs';
      playlists.hidden=id!=='playlists';
      document.querySelectorAll('[data-current-song-tab]').forEach(button=>button.setAttribute('aria-selected',String(button.dataset.currentSongTab===id)));
    }
    document.querySelectorAll('[data-current-song-tab]').forEach(button=>button.addEventListener('click',()=>tab(button.dataset.currentSongTab)));
    paint();
  }
  puja().catch(()=>document.querySelector('[data-current-puja-body]').textContent='Unable to load Puja. Reload to try again.');
  songs().catch(()=>document.querySelector('[data-current-song-list]').textContent='Unable to load songs. Reload to try again.');
})();
