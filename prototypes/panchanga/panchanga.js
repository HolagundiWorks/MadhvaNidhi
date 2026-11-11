/* Illustrative fixture only. These are not astronomical/calendar calculations. */
(() => {
 'use strict';
 const app = document.getElementById('app');
 const names = ['ಪ್ರತಿಪದೆ','ದ್ವಿತೀಯಾ','ತೃತೀಯಾ','ಚತುರ್ಥೀ','ಪಂಚಮೀ','ಷಷ್ಠೀ','ಸಪ್ತಮೀ','ಅಷ್ಟಮೀ','ನವಮೀ','ದಶಮೀ','ಏಕಾದಶೀ','ದ್ವಾದಶೀ','ತ್ರಯೋದಶೀ','ಚತುರ್ದಶೀ'];
 const phases = ['shukla','krishna'];
 const titles = {shukla:'ಶುಕ್ಲಪಕ್ಷ',krishna:'ಕೃಷ್ಣಪಕ್ಷ'};
 const byId = id => document.getElementById(id);
 function fixture(phase) {
  const start = phase === 'shukla' ? Date.UTC(2026,3,18) : Date.UTC(2026,4,3);
  const pair = phase === 'shukla' ? 7 : 10;
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
 const state={scale:1,page:0,selected:{phase:'shukla',tithi:11},ranges:[]};
 function rangesFor(capacity) {
  const ranges=[];
  for(let start=1;start<=15;) {
   let end=Math.min(15,start+capacity-1);
   while(phases.some(phase=>groups[phase].some(g=>g.tithis[0]<=end && g.tithis.at(-1)>end))) end--;
   if(end<start) throw Error('Capacity must fit merged tithis');
   ranges.push({start,end}); start=end+1;
  }
  return ranges;
 }
 function dateLabel(dates) {
  const month=dates[0].slice(5,7)==='04'?'ಏಪ್ರಿಲ್':'ಮೇ';
  return `${dates.map(d=>Number(d.slice(8))).join('–')} ${month}`;
 }
 function name(tithi,phase) {return tithi===15?(phase==='shukla'?'ಹುಣ್ಣಿಮೆ':'ಅಮಾವಾಸ್ಯೆ'):names[tithi-1];}
 function render() {
  const {start,end}=state.ranges[state.page];
  for(const phase of phases) {
   const grid=byId(phase); grid.replaceChildren();
   grid.style.gridTemplateColumns=`repeat(${end-start+1},minmax(0,1fr))`;
   for(const g of groups[phase].filter(g=>g.tithis[0]>=start && g.tithis.at(-1)<=end)) {
    const b=document.createElement('button'); b.type='button';
    const selected=state.selected.phase===phase && g.tithis.includes(state.selected.tithi);
    b.className='day'+(g.tithis.includes(11)?' ekadashi':'')+(g.tithis.includes(15)?' end':'');
    b.style.gridColumn=`${g.tithis[0]-start+1} / span ${g.tithis.length}`;
    b.dataset.tithis=g.tithis.join(','); b.dataset.phase=phase;
    b.setAttribute('aria-pressed',String(selected));
    b.setAttribute('aria-label',`${titles[phase]} ${g.tithis.map(t=>name(t,phase)).join(' + ')}, ${dateLabel(g.dates)} 2026${g.tithis.length>1?', shared date: merged tithis':''}`);
    const num=document.createElement('span'); num.className='num'; num.textContent=g.tithis.join(' + ');
    const label=document.createElement('span'); label.className='name'; label.textContent=g.tithis.map(t=>name(t,phase)).join(' + ');
    const date=document.createElement('span'); date.className='date'; date.textContent=dateLabel(g.dates);
    b.append(num,label,date);
    b.onclick=()=>{state.selected={phase,tithi:g.tithis[0]};render();};
    grid.append(b);
   }
  }
  const selected=groups[state.selected.phase].find(g=>g.tithis.includes(state.selected.tithi));
  byId('era-phase').textContent=titles[state.selected.phase];
  byId('selected').replaceChildren();
  const strong=document.createElement('strong'); strong.textContent=`${dateLabel(selected.dates)} 2026`;
  byId('selected').append(strong,` · ${titles[state.selected.phase]} · ${selected.tithis.map(t=>name(t,state.selected.phase)).join(' + ')}`);
  byId('range').textContent=`${start}–${end} / 15`;
  byId('previous').disabled=state.page===0;
  byId('next').disabled=state.page===state.ranges.length-1;
  byId('smaller').disabled=state.scale<=1;byId('larger').disabled=state.scale>=1.5;
  byId('size-value').textContent=`${Math.round(state.scale*100)}%`;
  byId('text-size').value=String(state.scale);
 }
 function layout(keepSelection=false) {
  app.classList.toggle('compact',innerHeight<400 || (innerHeight<=500 && state.scale>1.25));
  const oldStart=state.ranges[state.page]?.start||1;
  const width=byId('shukla').clientWidth;
  const gap=parseFloat(getComputedStyle(byId('shukla')).columnGap)||4;
  const capacity=Math.max(2,Math.min(15,Math.floor((width+gap)/((window.previewDesign?.tileWidth||88)*state.scale+gap))));
  state.ranges=rangesFor(capacity);
  const target=keepSelection?state.selected.tithi:oldStart;
  state.page=Math.max(0,state.ranges.findIndex(r=>r.start<=target && r.end>=target));
  render();
 }
 function scale(value) {state.scale=Math.max(1,Math.min(1.5,value));app.style.setProperty('--scale',state.scale);layout(true);}
 byId('smaller').onclick=()=>scale(state.scale-.25);byId('larger').onclick=()=>scale(state.scale+.25);
 byId('text-size').onchange=e=>scale(Number(e.target.value));
 byId('previous').onclick=()=>{state.page--;render();};byId('next').onclick=()=>{state.page++;render();};
 byId('settings').onclick=()=>byId('preferences').showModal();byId('close-settings').onclick=()=>byId('preferences').close();
 byId('screen').onchange=e=>{if(e.target.value==='panchanga')return;byId('section-title').textContent=e.target.selectedOptions[0].textContent;byId('section-preview').showModal();e.target.value='panchanga';};
 byId('close-section').onclick=()=>byId('section-preview').close();
 byId('context').onclick=()=>{byId('full-era').textContent=document.querySelector('.era').textContent;byId('context-selected').textContent=byId('selected').textContent;byId('calendar-context').showModal();};
 byId('close-context').onclick=()=>byId('calendar-context').close();
 new ResizeObserver(()=>layout()).observe(byId('shukla'));
 layout(true);
 // Inspection hooks for the prototype's independent browser checks.
 window.panchangaPreview={groups,state,rangesFor,layout,scale};
 function applyDesign(d){
  window.previewDesign=d;
  const bounded=(n,min,max)=>Math.min(max,Math.max(min,Number(n)||min));
  app.style.setProperty('--page-inset',bounded(d.inset,8,32)+'px');
  app.style.setProperty('--phase-gap',bounded(d.phaseGap,4,32)+'px');
  app.style.setProperty('--tile-gap',bounded(d.tileGap,2,12)+'px');
  scale(bounded(d.scale,1,1.5));
 }
 window.addEventListener('message',e=>{
  if(e.source!==parent || e.data?.type!=='panchanga-design')return;
  applyDesign(e.data.design);
 });
 if(window===parent){try{const saved=JSON.parse(localStorage.getItem('panchanga-design-v1'));if(saved?.design)applyDesign(saved.design);}catch{}}
})();
