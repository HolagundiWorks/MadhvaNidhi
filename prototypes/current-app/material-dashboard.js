import '@material/web/all.js';

const dialog = document.querySelector('#detail-dialog');
const title = document.querySelector('#detail-title');
const content = document.querySelector('#detail-body');
let week = Array.from({length:7},(_,i)=>i===6?[true,true,false]:[true,true,true]);
try { const saved=JSON.parse(localStorage.getItem('material-dashboard-practice')); if(saved?.length===7)week=saved; } catch {}
function show(name) { dialog.classList.remove('landscape-reader'); title.textContent=name; content.replaceChildren(); dialog.show(); }
function readerLayout(enabled) {
  dialog.classList.toggle('landscape-reader',enabled);
  if(window.parent!==window) window.parent.postMessage({type:'material-reader-layout',landscape:enabled},location.origin);
}
dialog.addEventListener('closed',()=>readerLayout(false));
document.querySelector('#close-dialog').addEventListener('click',()=>dialog.close());
function paint() {
  const count=week[6].filter(Boolean).length;
  document.querySelector('#today-count').textContent=count+' / 3';
  document.querySelector('#today-progress').value=count/3;
  document.querySelector('#week-score').textContent=week.filter(day=>day.every(Boolean)).length+' of 7 days';
  document.querySelectorAll('.session-icons span').forEach((icon,i)=>icon.classList.toggle('pending',!week[6][i]));
  const host=document.querySelector('#practice-week');host.replaceChildren();
  const names=['ಭಾನು','ಸೋಮ','ಮಂಗಳ','ಬುಧ','ಗುರು','ಶುಕ್ರ','ಶನಿ'];
  week.forEach((marks,index)=>{
    const date=new Date();date.setDate(date.getDate()-6+index);
    const row=document.createElement('div');row.className='week-row';
    const label=document.createElement('div');label.textContent=names[date.getDay()];
    const small=document.createElement('small');small.textContent=date.toLocaleDateString('en-IN',{day:'numeric',month:'short'});label.append(small);row.append(label);
    marks.forEach((done,i)=>{const mark=document.createElement('span');mark.className='material-symbols-outlined'+(done?'':' pending');mark.textContent=['wb_twilight','light_mode','dark_mode'][i];mark.setAttribute('aria-label',['Morning','Noon','Evening'][i]+(done?' complete':' not complete'));row.append(mark)});
    host.append(row);
  });
}
document.querySelector('#open-week').addEventListener('click',()=>document.querySelector('#practice-week').scrollIntoView({behavior:'smooth',block:'center'}));
document.querySelector('#profile-button').addEventListener('click',()=>{
  show('User settings');
  const summary=document.createElement('p');summary.textContent='Demo Sadhaka · Kannada · Madhva practice';content.append(summary);
  const heading=document.createElement('h3');heading.textContent='Practice preferences · ಆಚರಣೆಯ ಆಯ್ಕೆಗಳು';content.append(heading);
  let saved={};try{saved=JSON.parse(localStorage.getItem('material-user-practice')||'{}')}catch{}
  const veda=[['rigveda_ashvalayana','ಋಗ್ವೇದ — ಆಶ್ವಲಾಯನ'],['krishna_yajurveda_taittiriya','ಕೃಷ್ಣ ಯಜುರ್ವೇದ — ತೈತ್ತಿರೀಯ']];
  const traditions=[["srs_mantralaya", "ಶ್ರೀ ರಾಘವೇಂದ್ರ ಸ್ವಾಮಿ ಮಠ — ಮಂತ್ರಾಲಯ"], ["uttaradi", "ಶ್ರೀ ಉತ್ತರಾದಿ ಮಠ"], ["udupi_palimaru", "ಉಡುಪಿ ಶ್ರೀ ಪಲಿಮಾರು ಮಠ"], ["udupi_adamaru", "ಉಡುಪಿ ಶ್ರೀ ಅದಮಾರು ಮಠ"], ["udupi_krishnapura", "ಉಡುಪಿ ಶ್ರೀ ಕೃಷ್ಣಾಪುರ ಮಠ"], ["udupi_puttige", "ಉಡುಪಿ ಶ್ರೀ ಪುತ್ತಿಗೆ ಮಠ"], ["udupi_shirur", "ಉಡುಪಿ ಶ್ರೀ ಶಿರೂರು ಮಠ"], ["udupi_sodhe", "ಉಡುಪಿ ಶ್ರೀ ಸೋದೆ ಮಠ"], ["udupi_kaniyooru", "ಉಡುಪಿ ಶ್ರೀ ಕಾಣಿಯೂರು ಮಠ"], ["udupi_pejavara", "ಉಡುಪಿ ಶ್ರೀ ಪೇಜಾವರ ಮಠ"]];
  function preference(key,label,choices){
    const select=document.createElement('md-outlined-select');select.id='user-'+key;select.label=label;
    choices.forEach(([id,name])=>{const option=document.createElement('md-select-option');option.value=id;const text=document.createElement('div');text.slot='headline';text.textContent=name;option.append(text);select.append(option)});
    select.value=saved[key]||choices[0][0];select.addEventListener('change',()=>{saved[key]=select.value;localStorage.setItem('material-user-practice',JSON.stringify(saved))});content.append(select);
  }
  preference('veda','Sandhyavandana Veda Shakha',veda);
  preference('panchanga','Panchanga Parampara',traditions);
  const hint=document.createElement('p');hint.textContent='These preferences save automatically. Demo data stays in this browser.';content.append(hint);
});
document.querySelector('#theme-button').addEventListener('click',()=>{
  const dark=document.documentElement.dataset.theme!=='dark';
  document.documentElement.dataset.theme=dark?'dark':'light';
  document.querySelector('#theme-button span').textContent=dark?'light_mode':'dark_mode';
  localStorage.setItem('material-dashboard-theme',dark?'dark':'light');
});
try { if(localStorage.getItem('material-dashboard-theme')==='dark'){document.documentElement.dataset.theme='dark';document.querySelector('#theme-button span').textContent='light_mode';} } catch {}
const japas=[['ಗಾಯತ್ರೀ ಮಂತ್ರ — ಉಪದೇಶ ಪಡೆದವರಿಗೆ ಮಾತ್ರ',64,101,12540],['ನಾರಾಯಣ ಅಷ್ಟಾಕ್ಷರ',101,101,5001],['ಶ್ರೀರಾಮ ನಾಮ',36,101,1108],['ಶ್ರೀಹನುಮಂತ ನಾಮ',11,101,540],['ಶ್ರೀರಾಘವೇಂದ್ರ ಗುರು ನಾಮ',21,101,2208]];
// Demo portal completion state; never inferred from count / target.
const japaState=new URLSearchParams(location.search).get('japa')||'complete';
const number=n=>new Intl.NumberFormat('en-IN').format(n);
function openJapa(item) {
  show(item[0]+' · Japa');
  const count=document.createElement('h2');count.textContent=number(item[1])+' / '+number(item[2]);
  const note=document.createElement('p');note.textContent='Read-only overview. Count Japa and mark completion in the Japa screen.';
  content.append(count,note);
}
function paintJapas(){
  const awards=document.querySelector('#award-badges');awards.replaceChildren();
  const total=japas.reduce((sum,item)=>sum+item[3],0);
  [['silver',number(total),'Silver award for the cumulative Japa milestone',false],['gold','7','Next gold award: all 7 days complete',true]].forEach(([finish,value,label,next])=>{
    const badge=document.createElement('div');badge.className='award-badge'+(next?' next':'');badge.setAttribute('aria-label',label);badge.title=label;
    const icon=document.createElement('span');icon.className='material-symbols-outlined award-symbol award-'+finish;icon.textContent='award_star';icon.setAttribute('aria-hidden','true');
    const text=document.createElement('small');text.textContent=value;badge.append(icon,text);awards.append(badge);
  });
  const complete=japaState==='complete';
  const status=complete?'ಪೂರ್ಣ · Completed':japaState==='skipped'?'ಬಿಟ್ಟಿದೆ · Skipped':'ಬಾಕಿ · Not marked complete';
  const overall=document.querySelector('.overall');overall.classList.toggle('is-complete',complete);
  document.querySelector('#overall-status').textContent=status;
  document.querySelector('#resume-japa').textContent=complete?'Open Japa':'Continue Japa';
  const host=document.querySelector('#japa-cards');host.replaceChildren();
  const summary=japas[0];
  document.querySelector('.overall p strong').textContent=number(summary[1])+' / '+number(summary[2]);
  document.querySelector('.overall md-circular-progress').value=Math.min(1,summary[1]/summary[2]);

  japas.forEach(item=>{
    const card=document.createElement('article');card.className='surface japa-card'+(complete?' is-complete':'');card.setAttribute('aria-label',item[0]+'. '+status);
    const name=document.createElement('strong');name.textContent=item[0];
    const count=document.createElement('span');count.textContent=number(item[1])+' / '+number(item[2]);
    const progress=document.createElement('md-linear-progress');progress.value=Math.min(1,item[1]/item[2]);
    const total=document.createElement('small');total.textContent='Cumulative · '+number(item[3]);
    const state=document.createElement('p');state.className='completion-state';state.textContent=status;
    const open=document.createElement('md-text-button');open.textContent='Open';open.addEventListener('click',()=>openJapa(item));card.append(name,count,progress,total,state,open);host.append(card);
  });
}
document.querySelector('#resume-japa').addEventListener('click',()=>openJapa(japas[0]));
async function collection(url,label) {
  show(label);
  content.textContent='Loading…';
  try {
    const response=await fetch(url);if(!response.ok)throw Error('Unavailable');
    const records=await response.json();content.replaceChildren();
    const items=label==='Puja Krama'?records:records.flatMap(group=>group.items);
    const select=document.createElement('md-outlined-select');select.label=label;
    items.forEach((item,index)=>{const option=document.createElement('md-select-option');option.value=String(index);const text=document.createElement('div');text.slot='headline';text.textContent=item.title||item.kn||item.en;option.append(text);select.append(option)});
    const hint=document.createElement('p');hint.textContent='Select a title to read in landscape.';
    function read() {
      const item=items[Number(select.value)];if(!item)return;
      const body=document.createElement('pre');body.textContent=item.body||item.sections?.map(section=>section.body).join('\n\n')||'';
      const controls=document.createElement('div');controls.className='reader-controls';
      let size=20;
      ['−','+'].forEach((label,index)=>{const button=document.createElement('md-outlined-button');button.textContent=label;button.setAttribute('aria-label',index?'Increase text size':'Decrease text size');button.addEventListener('click',()=>{size=Math.max(16,Math.min(40,size+(index?2:-2)));body.style.fontSize=size+'px'});controls.append(button)});
      const back=document.createElement('md-text-button');back.textContent='Back to titles';back.addEventListener('click',()=>{readerLayout(false);title.textContent=label;content.replaceChildren(select,hint)});controls.append(back);
      title.textContent=item.title||item.kn||item.en;
      content.replaceChildren(controls,body);body.style.fontSize=size+'px';readerLayout(true);
    }
    select.addEventListener('change',read);content.append(select,hint);

  }catch{content.textContent='Content could not load. Close and try again.';}
}
document.querySelector('#quick-puja').addEventListener('click',()=>collection('../puja-catalogue.json','Puja Krama'));
document.querySelector('#quick-songs').addEventListener('click',()=>collection('../songs-catalogue.json','Songs'));
document.querySelector('#quick-grantha').addEventListener('click',()=>{show('Grantha');['Stotra','Suktas','Mantras','Namavali'].forEach(name=>{const text=document.createElement('p');text.textContent=name;content.append(text)});});
document.querySelectorAll('[data-scroll]').forEach(button=>button.addEventListener('click',()=>{
  document.querySelectorAll('[data-scroll]').forEach(item=>{item.classList.remove('active');item.removeAttribute('aria-current')});
  button.classList.add('active');button.setAttribute('aria-current','location');
  document.getElementById(button.dataset.scroll).scrollIntoView({behavior:'smooth',block:'start'});
}));
paint();paintJapas();
