/* Illustrative fixture only. These are not astronomical/calendar calculations. */
(() => {
 'use strict';
 if(new URLSearchParams(location.search).get('embed')==='1')document.body.classList.add('embed');
 window.addEventListener('message',e=>{
  if(e.data?.type!=='madhva-embed-theme')return;
  const theme=e.data.theme==='light'?'light':'dark';
  if(window.madhvaTheme)window.madhvaTheme.set(theme);
  else document.documentElement.dataset.theme=theme;
 });
 const app = document.getElementById('app');
 const names = ['ಪಾಡ್ಯಮಿ','ಬಿದಿಗೆ','ತದಿಗೆ','ಚೌತಿ','ಪಂಚಮಿ','ಷಷ್ಠಿ','ಸಪ್ತಮಿ','ಅಷ್ಟಮಿ','ನವಮಿ','ದಶಮಿ','ಏಕಾದಶಿ','ದ್ವಾದಶಿ','ತ್ರಯೋದಶಿ','ಚತುರ್ದಶಿ'];
 const phases = ['shukla','krishna'];
 const titles = {shukla:'ಶುಕ್ಲಪಕ್ಷ',krishna:'ಕೃಷ್ಣಪಕ್ಷ'};
 const byId = id => document.getElementById(id);
 function fixture(phase) {
  const start = phase === 'shukla' ? Date.UTC(2026,3,18) : Date.UTC(2026,4,3);
  const pair = phase === 'shukla' ? 6 : 11;
  let day = 0;
  return Array.from({length:15}, (_,i) => {
   const tithi = i + 1;
   if(tithi === pair + 1) day--;
   const dates = [new Date(start + day * 86400000).toISOString().slice(0,10)];
   if(phase === 'shukla' && tithi === 3) { day++; dates.push(new Date(start + day * 86400000).toISOString().slice(0,10)); }
   day++;
   return {tithi,dates};
  });
 }
 function merge(entries) {
  const result=[];
  for(const entry of entries) {
   const previous=result.at(-1);
   if(previous && previous.dates.some(date=>entry.dates.includes(date))) {
    previous.tithis.push(entry.tithi);
    previous.dates=[...new Set([...previous.dates,...entry.dates])];
   } else result.push({tithis:[entry.tithi],dates:[...entry.dates]});
  }
  return result;
 }
 const groups=Object.fromEntries(phases.map(phase=>[phase,merge(fixture(phase))]));
 const state={scale:1,page:0,selected:{phase:'shukla',tithi:11},ranges:[],unsupportedDate:null};
 function placements(phase,columns) {
  let row=0,column=0;
  return groups[phase].map(group=>{
   const span=group.tithis.length;
   const limit=columns;
   if(column+span>limit){row++;column=0;}
   const position={group,row,column};column+=span;
   return position;
  });
 }
 function changePhase(index){
  if(index===state.page&&!state.unsupportedDate)return;
  state.unsupportedDate=null;
  state.page=Math.max(0,Math.min(1,index));
  state.selected={phase:phases[state.page],tithi:state.selected.tithi};
  render();layout(true);
 }
 function localToday(){
  const now=new Date();return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`;
 }
 function dateLabel(dates) {
  const month=new Intl.DateTimeFormat('kn-IN',{month:'long',timeZone:'UTC'}).format(new Date(dates[0]+'T00:00:00Z'));
  return `${dates.map(d=>Number(d.slice(8))).join('–')} ${month}`;
 }
 function selectDate(date){
  const phase=phases.find(p=>groups[p].some(g=>g.dates.includes(date)));
  if(phase){const group=groups[phase].find(g=>g.dates.includes(date));state.unsupportedDate=null;state.selected={phase,tithi:group.tithis[0],date};state.page=phases.indexOf(phase);}
  else{state.unsupportedDate=date;state.selected={phase:state.selected.phase,tithi:11,date};}
  render();layout(true);
 }
 function weekdayLabel(dates,full=false) {
  const weekdays=full?['ಭಾನುವಾರ','ಸೋಮವಾರ','ಮಂಗಳವಾರ','ಬುಧವಾರ','ಗುರುವಾರ','ಶುಕ್ರವಾರ','ಶನಿವಾರ']:
   dates.length>1?['ಭಾ','ಸೋ','ಮಂ','ಬು','ಗು','ಶು','ಶ']:['ಭಾನು','ಸೋಮ','ಮಂಗಳ','ಬುಧ','ಗುರು','ಶುಕ್ರ','ಶನಿ'];
  return dates.map(date=>weekdays[new Date(date+'T00:00:00Z').getUTCDay()]).join('–');
 }
 function name(tithi,phase) {return tithi===15?(phase==='shukla'?'ಪೌರ್ಣಿಮಾ':'ಅಮಾವಾಸ್ಯೆ'):names[tithi-1];}
 function syncPakshaCycle() {
  const cycle=byId('paksha-cycle');
  if(!cycle)return;
  const shukla=state.page===0;
  const start=cycle.querySelector('[data-cycle="start"]');
  const mid=cycle.querySelector('[data-cycle="mid"]');
  const end=cycle.querySelector('[data-cycle="end"]');
  if(start)start.className='moon'+(shukla?'':' full');
  if(mid)mid.className='moon '+(shukla?'half':'wane');
  if(end)end.className='moon'+(shukla?' full':'');
  cycle.setAttribute('aria-label',shukla?'ಶುಕ್ಲಪಕ್ಷ · ಅಮಾವಾಸ್ಯೆ to ಹುಣ್ಣಿಮೆ':'ಕೃಷ್ಣಪಕ್ಷ · ಹುಣ್ಣಿಮೆ to ಅಮಾವಾಸ್ಯೆ');
 }
 function measureLineWidth(el) {
  const range=document.createRange();
  range.selectNodeContents(el);
  return range.getBoundingClientRect().width;
 }
 function fitEraLines() {
  const targets=[byId('era-shaka-line'),byId('era-samvat-line'),byId('era-masa-line')].filter(Boolean);
  const measures=[byId('era-shaka-line'),byId('era-samvat-line'),byId('era-masa-row')||byId('era-masa-line')].filter(Boolean);
  const copy=document.querySelector('.era-copy');
  if(!targets.length||!copy)return;
  const width=copy.clientWidth;
  if(width<=0)return;
  /* Carbon productive sizes only: heading-compact-01 (14), -02 (16), heading-03 (20). */
  const steps=[
   {px:20,type:'heading-03'},
   {px:16,type:'heading-compact-02'},
   {px:14,type:'heading-compact-01'}
  ];
  let chosen=steps[steps.length-1];
  for(const step of steps){
   for(const el of targets){el.style.fontSize=step.px+'px';el.dataset.type=step.type;}
   const needed=Math.max(...measures.map(measureLineWidth));
   if(needed>0&&needed<=width*0.98){chosen=step;break;}
  }
  for(const el of targets){el.style.fontSize=chosen.px+'px';el.dataset.type=chosen.type;}
 }
 function render() {
  const unavailable=state.unsupportedDate;
  app.classList.toggle('no-date-data',!!unavailable);byId('calendar-empty').hidden=!unavailable;byId('missing-context').hidden=!unavailable;
  const eraLines=[byId('era-shaka-line'),byId('era-samvat-line'),byId('era-masa'),byId('era-masa-line')];
  for(const el of eraLines){if(el)el.hidden=!!unavailable;}
  const eraText=byId('era-text');if(eraText)eraText.hidden=true;
  requestAnimationFrame(fitEraLines);
  app.style.setProperty('--tile-side',state.side+'px');
  app.classList.toggle('tight-tiles',state.side<112);
  app.classList.toggle('micro-tiles',state.side<80);
  for(const phase of phases) {
   const grid=byId(phase); grid.replaceChildren();
   grid.closest('.phase').hidden=!!unavailable||(state.paged&&phase!==phases[state.page]);
   const positions=placements(phase,state.columns);
   const rows=positions.at(-1).row+1;
   grid.style.height=(rows*state.side+(rows-1)*state.gap)+'px';
   grid.style.gridTemplateColumns=`repeat(${state.columns},minmax(0,1fr))`;
   for(const {group:g,row,column} of positions) {
    const b=document.createElement('button'); b.type='button';
    const selected=state.selected.phase===phase && g.tithis.includes(state.selected.tithi);
    b.className='day'+(g.tithis.includes(11)?' ekadashi':'')+(g.tithis.includes(15)?' end':'');
    b.style.gridColumn=`${column+1} / span ${g.tithis.length}`;
    b.style.gridRow=String(row+1);
    b.dataset.tithis=g.tithis.join(','); b.dataset.phase=phase;
    b.setAttribute('aria-pressed',String(selected));
    b.setAttribute('aria-label',`${titles[phase]} ${g.tithis.map(t=>name(t,phase)).join(' + ')}, ${dateLabel(g.dates)} 2026, ${weekdayLabel(g.dates,true)}${g.tithis.length>1?', shared date: merged tithis':''}`);
    const num=document.createElement('span'); num.className='num'; num.textContent=g.tithis.map(t=>name(t,phase)).join(' + ');
    const date=document.createElement('span'); date.className='date'; date.textContent=g.dates.map(d=>Number(d.slice(8))).join('–');
    const weekday=document.createElement('span'); weekday.className='weekday'; weekday.textContent=weekdayLabel(g.dates,state.side>=120);
    b.append(date,weekday,num);
    if(g.tithis.includes(11)||g.tithis.includes(15)) {
     const marker=document.createElementNS('http://www.w3.org/2000/svg','svg');
     const ekadashi=g.tithis.includes(11);
     marker.classList.add('tile-marker');
     marker.dataset.icon=ekadashi?'Transparency_01':phase==='shukla'?'full-moon':'new-moon';
     marker.setAttribute('viewBox','0 0 32 32');
     marker.setAttribute('aria-hidden','true');
     marker.setAttribute('focusable','false');
     // Carbon Transparency_01, @carbon/pictograms 12.88.0, IBM, Apache-2.0; see CARBON-ICONS-LICENSE.txt.
     marker.innerHTML=ekadashi
      ? '<path d="M30,30.36H14v-0.72h15.64V2.36H21V1.64h9c0.199,0,0.36,0.161,0.36,0.36v28 C30.36,30.199,30.199,30.36,30,30.36z M11,30.36H7v-0.72h4V30.36z M4,30.36H2c-0.199,0-0.36-0.161-0.36-0.36v-2h0.72v1.64H4V30.36z M2.36,25H1.64v-4h0.72C2.36,21,2.36,25,2.36,25z M17,24.36h-2c-4.922,0-8.36-3.85-8.36-9.36c0-0.199,0.161-0.36,0.36-0.36h1.64V11 c0-1.179,0.869-2.159,2-2.333V7h0.72v1.667c1.131,0.174,2,1.154,2,2.333c0,0.047,0.004,0.426,0.091,0.923 c1.193-0.455,2.591-0.35,3.707,0.287c0.596-0.942,1.647-1.57,2.843-1.57c1.581,0,1.639-1.474,1.64-1.642L22.36,9 c0,0.552-0.226,1.436-0.948,1.951c1.149,0.534,1.948,1.7,1.948,3.049c0,0.216-0.021,0.43-0.062,0.64H25 c0.199,0,0.36,0.161,0.36,0.36C25.36,20.511,21.922,24.36,17,24.36z M7.365,15.36c0.145,4.894,3.243,8.28,7.635,8.28h2 c4.392,0,7.49-3.386,7.635-8.28H7.365z M17.438,14.64h5.124c0.053-0.209,0.078-0.423,0.078-0.64c0-1.456-1.184-2.64-2.64-2.64 s-2.64,1.185-2.64,2.64C17.36,14.217,17.386,14.432,17.438,14.64z M13.62,12.63c0.303,0.969,0.959,2.01,2.38,2.01h0.701 C16.66,14.43,16.64,14.216,16.64,14c0-0.401,0.07-0.785,0.2-1.142C15.875,12.291,14.648,12.207,13.62,12.63z M9.36,14.64h4.669 c-1.06-0.946-1.389-2.608-1.389-3.64c0-0.904-0.736-1.64-1.64-1.64S9.36,10.096,9.36,11V14.64z M2.36,18H1.64v-4h0.72 C2.36,14,2.36,18,2.36,18z M2.36,11H1.64V7h0.72C2.36,7,2.36,11,2.36,11z M2.36,4H1.64V2c0-0.199,0.161-0.36,0.36-0.36h2v0.72H2.36 V4z M18,2.36h-4V1.64h4V2.36z M11,2.36H7V1.64h4V2.36z"/>'
      : `<circle cx="16" cy="16" r="11" fill="${phase==='shukla'?'currentColor':'none'}" stroke="currentColor" stroke-width="2"/>`;
     b.classList.add('has-marker');
     b.append(marker);
    }
    b.onclick=()=>{state.unsupportedDate=null;state.selected={phase,tithi:g.tithis[0]};render();byId(phase).querySelector('[aria-pressed=true]').focus({preventScroll:true});byId('date-dialog').showModal();};
    grid.append(b);
   }
  }
  const selected=unavailable?{dates:[unavailable],tithis:[]}:groups[state.selected.phase].find(g=>g.tithis.includes(state.selected.tithi));
  const selectedDates=state.selected.date&&selected.dates.includes(state.selected.date)?[state.selected.date]:selected.dates;
  const year=selectedDates[0].slice(0,4);
  const event=unavailable?{title:'ವಿವರಗಳು ಲಭ್ಯವಿಲ್ಲ',description:'ಈ ದಿನಾಂಕದ ಪಂಚಾಂಗ ದತ್ತಾಂಶ ಜೋಡಣೆ ಬಾಕಿಯಿದೆ.'}:selected.tithis.includes(11)
   ? {title:'ಏಕಾದಶಿ',description:'ವ್ರತ ಮತ್ತು ಹರಿಸ್ಮರಣೆಯ ಮಹತ್ವ. ಮಾದರಿ ಮಾಹಿತಿ.'}
   : selected.tithis.includes(15)
    ? {title:state.selected.phase==='shukla'?'ಹುಣ್ಣಿಮೆ':'ಅಮಾವಾಸ್ಯೆ',description:'ಪಕ್ಷದ ಕೊನೆಯ ತಿಥಿ. ಮಾದರಿ ಮಾಹಿತಿ.'}
    : {title:'ವಿಶೇಷ ಕಾರ್ಯಕ್ರಮ ದಾಖಲಾಗಿಲ್ಲ',description:'ಈ ದಿನಕ್ಕೆ ಮಾದರಿ ಕಾರ್ಯಕ್ರಮ ಸೇರಿಸಿಲ್ಲ.'};
  byId('event-title').textContent=innerHeight<=420&&!unavailable&&!selected.tithis.includes(11)&&!selected.tithis.includes(15)?'ವಿಶೇಷವಿಲ್ಲ':event.title;
  byId('event-summary').textContent=event.description;
  byId('event-dialog-title').textContent=event.title;
  byId('event-dialog-description').textContent=event.description;
  byId('date-dialog-title').textContent=`${dateLabel(selectedDates)} ${year}`;
  byId('date-dialog-weekday').textContent=weekdayLabel(selectedDates,true);
  byId('date-dialog-phase').textContent=unavailable?'ಜೋಡಣೆ ಬಾಕಿ':titles[state.selected.phase];
  byId('date-dialog-month').textContent=unavailable?'ಜೋಡಣೆ ಬಾಕಿ':'ವೈಶಾಖಮಾಸ';
  for(const field of ['phase','tithi','month'])byId('date-dialog-'+field).classList.toggle('data-pending',!!unavailable);
  byId('date-dialog-tithi').textContent=unavailable?'ಜೋಡಣೆ ಬಾಕಿ':selected.tithis.map(t=>name(t,state.selected.phase)).join(' + ');
  byId('date-dialog-event').textContent=event.title;
  byId('date-dialog-summary').textContent=event.description;
  for(const field of ['rashi','nakshatra','bhava','karana']){byId('date-dialog-'+field).textContent='ಜೋಡಣೆ ಬಾಕಿ';byId('context-'+field).textContent='ಜೋಡಣೆ ಬಾಕಿ';}
  const isEkadashi=selected.tithis.includes(11);
  app.classList.toggle('ekadashi-selected',isEkadashi);
  byId('date-dialog').classList.toggle('is-ekadashi',isEkadashi);
  byId('era-phase').textContent=titles[state.selected.phase];
  byId('selected').replaceChildren();
  const strong=document.createElement('strong'); strong.textContent=`${dateLabel(selectedDates)} ${year}`;
  byId('selected').append(strong);
  const selectedLabel=byId('selected');selectedLabel.style.fontSize='';
  const labelSize=parseFloat(getComputedStyle(selectedLabel).fontSize);
  if(selectedLabel.scrollWidth>selectedLabel.clientWidth&&selectedLabel.clientWidth>0){
   selectedLabel.style.fontSize=Math.max(16,labelSize*selectedLabel.clientWidth/selectedLabel.scrollWidth-.25)+'px';
  }
  byId('selected-weekday').textContent=weekdayLabel(selectedDates,innerHeight>420);
  byId('selected-date-label').textContent=selectedDates.length===1&&selectedDates[0]===localToday()?'ಇಂದಿನ ದಿನಾಂಕ':'ಆಯ್ದ ದಿನಾಂಕ';
  byId('missing-date-text').textContent=unavailable?`${dateLabel(selectedDates)} ${year} · ಪಂಚಾಂಗ ದತ್ತಾಂಶ ಜೋಡಣೆ ಬಾಕಿ`:'';
  const headerDate = byId('header-date-value');
  const headerTithi = byId('header-tithi');
  if (headerDate) headerDate.textContent = `${dateLabel(selectedDates)} ${year}`;
  if (headerTithi) headerTithi.textContent = unavailable ? 'ತಿಥಿ ಜೋಡಣೆ ಬಾಕಿ' : selected.tithis.map(t => name(t, state.selected.phase)).join(' + ');
  byId('selected-tithi').textContent = unavailable ? 'ತಿಥಿ ಜೋಡಣೆ ಬಾಕಿ' : selected.tithis.map(t => name(t, state.selected.phase)).join(' + ');
  byId('range').textContent=state.paged?titles[phases[state.page]]:'1–15 / 15';
  byId('range').setAttribute('aria-label',state.paged?`${titles[phases[state.page]]}, all 15 tithis, page ${state.page+1} of 2`:'Both pakshas, all 15 tithis');
  byId('previous').setAttribute('aria-label','Shukla paksha');byId('previous').setAttribute('aria-pressed',String(state.page===0&&!unavailable));
  byId('next').setAttribute('aria-label','Krishna paksha');byId('next').setAttribute('aria-pressed',String(state.page===1&&!unavailable));
  byId('previous').disabled=false;
  byId('next').disabled=false;
  syncPakshaCycle();
  const textSize=byId('text-size');
  if(textSize) textSize.value=String(state.scale);
 }
 function layout(keepSelection=false) {
  /* Same proportions as 960×540: scale chrome + tiles together (640…1560+) */
  const density=Math.min(1.75,Math.max(0.55,Math.min(innerWidth/960,innerHeight/540)));
  app.style.setProperty('--density',String(density));
  app.classList.toggle('compact',innerHeight<400 || innerWidth<=700 || (innerHeight<=500 && state.scale>1.25));
  const phaseBox=document.querySelector('.phases');
  const width=phaseBox.clientWidth;
  const gapRaw=parseFloat(getComputedStyle(byId(phases[state.page]||'shukla')).columnGap);
  const gap=Number.isFinite(gapRaw)?gapRaw:Math.max(2,Math.round(8*density));
  const heading=Math.max(0,...[...document.querySelectorAll('.phase-head')].map(el=>{
   if(getComputedStyle(el).display==='none')return 0;
   return el.getBoundingClientRect().height||Math.max(parseFloat(getComputedStyle(el).minHeight)||0,parseFloat(getComputedStyle(el.querySelector('h3'))?.lineHeight)||0);
  }));
  const phaseEl=document.querySelector('.phase');
  const innerGap=phaseEl?parseFloat(getComputedStyle(phaseEl).rowGap)||0:0;
  state.paged=true;state.columns=5;state.gap=gap;
  app.classList.add('paksha-pages');
  if(keepSelection)state.page=phases.indexOf(state.selected.phase);
  const rows=Math.max(...phases.map(p=>placements(p,5).at(-1).row+1));
  const widthSide=(width-gap*4)/5;
  const heightSide=(phaseBox.clientHeight-heading-innerGap-(rows-1)*gap)/rows;
  const fillSide=Math.max(1,Math.min(widthSide,heightSide));
  /* Cap tile growth to ~110px × density so 1560×720 keeps 960×540 proportions */
  const proportionSide=110*density;
  state.side=Math.max(1,Math.min(fillSide,proportionSide));
  const tilePad=Math.max(4,Math.round(Math.min(state.side*0.145,16*density)));
  app.style.setProperty('--tile-side',state.side+'px');
  app.style.setProperty('--tile-pad',tilePad+'px');
  app.style.setProperty('--tile-gap',gap+'px');
  for(const phase of phases)byId(phase).style.width=(state.side*5+gap*4)+'px';
  render();
 }
 function scale(value) {state.scale=Math.max(1,Math.min(1.5,value));app.style.setProperty('--scale',state.scale);layout(true);}
 
 try {
 byId('text-size')&&(byId('text-size').onchange=e=>scale(Number(e.target.value)));
 byId('previous').onclick=()=>changePhase(0);byId('next').onclick=()=>changePhase(1);
 byId('show-sample').onclick=()=>{state.unsupportedDate=null;state.selected={phase:'shukla',tithi:11};state.page=0;render();layout(true);};
 byId('settings').onclick=()=>{toggleNavigation(false);byId('preferences').showModal();};byId('close-settings').onclick=()=>byId('preferences').close();
 function toggleNavigation(open){
  byId('side-nav').classList.toggle('is-expanded',open);
  byId('nav-toggle').setAttribute('aria-expanded',String(open));
  byId('nav-toggle').setAttribute('aria-label',open?'Collapse navigation':'Expand navigation');
  byId('nav-backdrop').hidden=!open;
 }
 byId('nav-toggle').onclick=()=>toggleNavigation(byId('nav-toggle').getAttribute('aria-expanded')!=='true');
 byId('nav-backdrop').onclick=()=>toggleNavigation(false);
 document.addEventListener('keydown',e=>{if(e.key==='Escape')toggleNavigation(false);});
 document.querySelectorAll('[data-screen]').forEach(button=>button.onclick=()=>{
  toggleNavigation(false);
  if(button.dataset.screen==='panchanga')return;
  byId('section-title').textContent=button.querySelector('.nav-label').textContent;
  byId('section-preview').showModal();
 });
 byId('close-section').onclick=()=>byId('section-preview').close();
 const pickerMinYear=1900,pickerMaxYear=Math.max(2100,new Date().getFullYear()+100);
 let pickerYear=2026,pickerMonth=3,pickerFocus='2026-04-28';
 const isoDate=date=>date.toISOString().slice(0,10);
 for(let month=0;month<12;month++){
  const option=document.createElement('option');option.value=String(month);option.textContent=new Intl.DateTimeFormat('kn-IN',{month:'long',timeZone:'UTC'}).format(new Date(Date.UTC(2026,month,1)));byId('picker-month').append(option);
 }
 for(let year=pickerMinYear;year<=pickerMaxYear;year++){const option=document.createElement('option');option.value=String(year);option.textContent=String(year);byId('picker-year').append(option);}
 function renderPicker(){
  byId('picker-month').value=String(pickerMonth);byId('picker-year').value=String(pickerYear);
  byId('picker-prev').disabled=pickerYear===pickerMinYear&&pickerMonth===0;
  byId('picker-next').disabled=pickerYear===pickerMaxYear&&pickerMonth===11;
  const grid=byId('picker-days');grid.replaceChildren();
  const first=new Date(Date.UTC(pickerYear,pickerMonth,1));
  const start=new Date(Date.UTC(pickerYear,pickerMonth,1-first.getUTCDay()));
  grid.setAttribute('aria-label',byId('picker-month').selectedOptions[0].textContent+' '+pickerYear);
  for(let offset=0;offset<42;offset++){
   const date=new Date(start.getTime()+offset*86400000),iso=isoDate(date);
   const button=document.createElement('button');button.type='button';button.className='picker-day'+(date.getUTCMonth()!==pickerMonth?' outside-month':'')+(iso===localToday()?' is-today':'');
   button.dataset.date=iso;button.textContent=String(date.getUTCDate());button.disabled=date.getUTCFullYear()<pickerMinYear||date.getUTCFullYear()>pickerMaxYear;
   button.setAttribute('aria-pressed',String(iso===byId('chosen-date').value));
   button.setAttribute('aria-label',new Intl.DateTimeFormat('kn-IN',{dateStyle:'full',timeZone:'UTC'}).format(date));
   if(iso===localToday())button.setAttribute('aria-current','date');
   button.tabIndex=iso===pickerFocus?0:-1;
   button.onclick=()=>{byId('chosen-date').value=iso;pickerFocus=iso;pickerYear=date.getUTCFullYear();pickerMonth=date.getUTCMonth();renderPicker();grid.querySelector('[data-date="'+iso+'"]').focus({preventScroll:true});};
   button.onkeydown=e=>{
    const delta={ArrowLeft:-1,ArrowRight:1,ArrowUp:-7,ArrowDown:7,Home:-date.getUTCDay(),End:6-date.getUTCDay()}[e.key];
    if(delta===undefined)return;e.preventDefault();const next=new Date(date.getTime()+delta*86400000);
    if(next.getUTCFullYear()<pickerMinYear||next.getUTCFullYear()>pickerMaxYear)return;
    pickerFocus=isoDate(next);pickerYear=next.getUTCFullYear();pickerMonth=next.getUTCMonth();renderPicker();grid.querySelector('[data-date="'+pickerFocus+'"]').focus({preventScroll:true});
   };
   grid.append(button);
  }
 }
 function pickerView(year,month){
  const date=new Date(Date.UTC(year,month,1));pickerYear=date.getUTCFullYear();pickerMonth=date.getUTCMonth();
  const day=Math.min(Number(pickerFocus.slice(8)),new Date(Date.UTC(pickerYear,pickerMonth+1,0)).getUTCDate());pickerFocus=isoDate(new Date(Date.UTC(pickerYear,pickerMonth,day)));renderPicker();
 }
 byId('picker-prev').onclick=()=>pickerView(pickerYear,pickerMonth-1);byId('picker-next').onclick=()=>pickerView(pickerYear,pickerMonth+1);
 byId('picker-month').onchange=e=>pickerView(pickerYear,Number(e.target.value));byId('picker-year').onchange=e=>pickerView(Number(e.target.value),pickerMonth);
 byId('picker-today').onclick=()=>{const today=localToday();byId('chosen-date').value=today;pickerFocus=today;const date=new Date(today+'T00:00:00Z');pickerYear=date.getUTCFullYear();pickerMonth=date.getUTCMonth();renderPicker();};
 byId('choose-date').onclick=()=>{
  const selected=groups[state.selected.phase].find(g=>g.tithis.includes(state.selected.tithi));
  const value=state.selected.date||selected.dates[0];byId('chosen-date').value=value;pickerFocus=value;
  const date=new Date(value+'T00:00:00Z');pickerYear=date.getUTCFullYear();pickerMonth=date.getUTCMonth();
  renderPicker();byId('date-picker-error').hidden=true;byId('date-picker').showModal();
  byId('picker-days').querySelector('[data-date="'+pickerFocus+'"]').focus({preventScroll:true});
 };
 byId('cancel-date-picker').onclick=()=>byId('date-picker').close();
 byId('date-picker-form').onsubmit=e=>{
  e.preventDefault();const date=byId('chosen-date').value;
  if(!date||!/^\d{4}-\d{2}-\d{2}$/.test(date)||!Number.isFinite(Date.parse(date+'T00:00:00Z'))){byId('date-picker-error').textContent='ಸರಿಯಾದ ದಿನಾಂಕ ಆಯ್ಕೆಮಾಡಿ.';byId('date-picker-error').hidden=false;return;}
  selectDate(date);byId('date-picker').close();
 };
 byId('selected-date-details').onclick=()=>byId('date-dialog').showModal();byId('close-date').onclick=()=>byId('date-dialog').close();
 byId('event-details').onclick=()=>byId('event-dialog').showModal();byId('close-event').onclick=()=>byId('event-dialog').close();
 const phasesEl=document.querySelector('.phases');
 if(phasesEl){const resize=new ResizeObserver(()=>layout());resize.observe(phasesEl);}
 const eraCopy=document.querySelector('.era-copy');
 if(eraCopy){const eraResize=new ResizeObserver(()=>fitEraLines());eraResize.observe(eraCopy);}
 try{layout(true);}catch(err){console.error('panchanga layout failed',err);window.__panchangaBootError=String(err&&err.stack||err);}
 if(document.fonts&&document.fonts.ready)document.fonts.ready.then(()=>fitEraLines());
 // Inspection hooks for the prototype's independent browser checks.
 window.panchangaPreview={groups,state,placements,layout,scale,changePhase,fitEraLines};
 function applyDesign(d){
  window.previewDesign=d;
  const bounded=(n,min,max)=>Math.min(max,Math.max(min,Number(n)||min));
  app.style.setProperty('--page-inset',bounded(d.inset,8,32)+'px');
  app.style.setProperty('--tile-gap',bounded(d.tileGap,2,12)+'px');
  scale(bounded(d.scale,1,1.5));
 }
 window.addEventListener('message',e=>{
  if(e.source!==parent || e.data?.type!=='panchanga-design')return;
  applyDesign(e.data.design);
 });
 if(window===parent){try{const saved=JSON.parse(localStorage.getItem('panchanga-design-v1'));if(saved?.design)applyDesign(saved.design);}catch{}}
 } catch (err) {
  console.error('panchanga boot failed', err);
  window.__panchangaBootError = String(err && err.stack || err);
 }
})();
