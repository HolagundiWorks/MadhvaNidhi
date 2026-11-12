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
 function render() {
  const unavailable=state.unsupportedDate;
  app.classList.toggle('no-date-data',!!unavailable);byId('calendar-empty').hidden=!unavailable;byId('missing-context').hidden=!unavailable;byId('era-text').hidden=!!unavailable;
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
     marker.dataset.icon=ekadashi?'AppleDash':phase==='shukla'?'full-moon':'new-moon';
     marker.setAttribute('viewBox','0 0 32 32');
     marker.setAttribute('aria-hidden','true');
     marker.setAttribute('focusable','false');
     // Carbon AppleDash, IBM, Apache-2.0; see CARBON-ICONS-LICENSE.txt.
     marker.innerHTML=ekadashi?`<path d="M7.0825,11.6714l-1.6748-1.0928c-.9062,1.3887-1.3928,3.2305-1.4072,5.3267l2,.0137c.0117-1.7119,.3857-3.1807,1.082-4.2476Z"/><path d="M6.0852,17.8164l-1.9907,.1934c.1887,1.938,.6592,3.6738,1.3982,5.1592l1.7905-.8911c-.6301-1.2661-1.0332-2.7676-1.198-4.4614Z"/><path d="M8.2378,23.7964l-1.5688,1.2402c1.333,1.6865,3.0972,2.7573,4.8396,2.9375l.2061-1.9893c-1.1912-.1235-2.491-.9414-3.4768-2.1885Z"/><path d="M16,25c-.7334,0-1.4783,.271-2.1987,.5332-.1809,.0654-.3635,.1318-.5469,.1943l.6426,1.894c.197-.0669,.3936-.1382,.5879-.209,.5576-.2026,1.134-.4126,1.5151-.4126,.3809,0,.9575,.21,1.5149,.4121,.1809,.0659,.3635,.1323,.5466,.1953l.6479-1.8926c-.1711-.0586-.3418-.1206-.511-.1821-.7205-.2617-1.4653-.5327-2.1985-.5327Z"/><path d="M20.2546,25.9878l.1855,1.9917c1.7456-.1631,3.5178-1.2227,4.8621-2.9072l-1.5632-1.2476c-.9924,1.2437-2.2949,2.0522-3.4844,2.1631Z"/><path d="M24.7004,22.3115l1.7871,.8975c.7449-1.4834,1.2205-3.2178,1.4138-5.1558l-1.9902-.1987c-.1689,1.6938-.5762,3.1934-1.2107,4.457Z"/><path d="M28,15.9492c-.0076-2.0938-.4854-3.9365-1.3818-5.3311l-1.6821,1.082c.6897,1.0728,1.0576,2.5444,1.064,4.2559l2-.0068Z"/><path d="M20,4h-1c-1.1046,0-2,.8954-2,2v1h1c1.1046,0,2-.8954,2-2v-1Z"/><path d="M23.8125,10.5308l1.1035-1.668c-.8655-.5728-1.8467-.8628-2.916-.8628-1.4155,0-2.4175,.2881-3.3013,.542-.5764,.1655-1.1362,.3203-1.7979,.4004-.502-2.8047-2.9541-4.9424-5.9009-4.9424v2c1.8223,0,3.3464,1.2319,3.8274,2.9014-.5442-.0864-1.031-.2173-1.5261-.3594-.8838-.2539-1.8857-.542-3.3013-.542-1.0693,0-2.0505,.29-2.916,.8628l1.1035,1.668c.5325-.3521,1.1423-.5308,1.8125-.5308,1.134,0,1.9185,.2256,2.7488,.4639,.917,.2637,1.8652,.5361,3.2512,.5361s2.3342-.2725,3.2512-.5361c.8303-.2383,1.6147-.4639,2.7488-.4639,.6702,0,1.28,.1787,1.8125,.5308Z"/>`:`<circle cx="16" cy="16" r="11" fill="${phase==='shukla'?'currentColor':'none'}" stroke="currentColor" stroke-width="2"/>`;
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
  byId('today-date').disabled=selectedDates.length===1&&selectedDates[0]===localToday();
  byId('missing-date-text').textContent=unavailable?`${dateLabel(selectedDates)} ${year} · ಪಂಚಾಂಗ ದತ್ತಾಂಶ ಜೋಡಣೆ ಬಾಕಿ`:'';
  byId('selected-tithi').textContent=unavailable?'ತಿಥಿ ಜೋಡಣೆ ಬಾಕಿ':selected.tithis.map(t=>name(t,state.selected.phase)).join(' + ');
  byId('range').textContent=state.paged?titles[phases[state.page]]:'1–15 / 15';
  byId('range').setAttribute('aria-label',state.paged?`${titles[phases[state.page]]}, all 15 tithis, page ${state.page+1} of 2`:'Both pakshas, all 15 tithis');
  byId('previous').setAttribute('aria-label','Shukla paksha');byId('previous').setAttribute('aria-pressed',String(state.page===0&&!unavailable));
  byId('next').setAttribute('aria-label','Krishna paksha');byId('next').setAttribute('aria-pressed',String(state.page===1&&!unavailable));
  byId('previous').disabled=false;
  byId('next').disabled=false;
  byId('smaller').disabled=state.scale<=1;byId('larger').disabled=state.scale>=1.5;
  byId('size-value').textContent=`${Math.round(state.scale*100)}%`;
  byId('text-size').value=String(state.scale);
 }
 function layout(keepSelection=false) {
  app.classList.toggle('compact',innerHeight<400 || (innerHeight<=500 && state.scale>1.25));
  const phaseBox=document.querySelector('.phases');
  const width=phaseBox.clientWidth;
  const gap=parseFloat(getComputedStyle(byId(phases[state.page]||'shukla')).columnGap)||4;
  const heading=Math.max(...[...document.querySelectorAll('.phase-head')].map(el=>el.getBoundingClientRect().height||Math.max(parseFloat(getComputedStyle(el).minHeight),parseFloat(getComputedStyle(el.querySelector('h3')).lineHeight))));
  const innerGap=parseFloat(getComputedStyle(document.querySelector('.phase')).rowGap)||0;
  state.paged=true;state.columns=5;state.gap=gap;
  app.classList.add('paksha-pages');
  if(keepSelection)state.page=phases.indexOf(state.selected.phase);
  const rows=Math.max(...phases.map(p=>placements(p,5).at(-1).row+1));
  const widthSide=(width-gap*4)/5;
  const heightSide=(phaseBox.clientHeight-heading-innerGap-(rows-1)*gap)/rows;
  state.side=Math.max(1,Math.min(widthSide,heightSide));
  for(const phase of phases)byId(phase).style.width=(state.side*5+gap*4)+'px';
  render();
 }
 function scale(value) {state.scale=Math.max(1,Math.min(1.5,value));app.style.setProperty('--scale',state.scale);layout(true);}
 byId('smaller').onclick=()=>scale(state.scale-.25);byId('larger').onclick=()=>scale(state.scale+.25);
 byId('text-size').onchange=e=>scale(Number(e.target.value));
 byId('previous').onclick=()=>changePhase(0);byId('next').onclick=()=>changePhase(1);
 byId('today-date').onclick=()=>selectDate(localToday());
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
 const resize=new ResizeObserver(()=>layout());resize.observe(document.querySelector('.phases'));
 layout(true);
 // Inspection hooks for the prototype's independent browser checks.
 window.panchangaPreview={groups,state,placements,layout,scale,changePhase};
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
})();
