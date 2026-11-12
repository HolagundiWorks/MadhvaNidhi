(() => {
 'use strict';
 const key='madhvanidhi-preview-theme';
 const valid=value=>value==='light'||value==='dark';
 function syncControl(){
  const button=document.getElementById('theme-toggle');if(!button)return;
  const dark=(document.documentElement.dataset.appearance||'dark')!=='light';
  const action=dark?'Switch to light theme':'Switch to dark theme';
  button.setAttribute('aria-label',action);button.title=action;
 }
 function syncSystemChrome(){
  const bg=getComputedStyle(document.documentElement).getPropertyValue('--cds-background').trim()||'#161616';
  let meta=document.querySelector('meta[name="theme-color"]');
  if(!meta){meta=document.createElement('meta');meta.name='theme-color';document.head.appendChild(meta);}
  meta.content=bg;
  document.documentElement.style.backgroundColor=bg;
  if(document.body)document.body.style.backgroundColor=bg;
 }
 function apply(theme,persist=false){
  const next=valid(theme)?theme:'dark';
  /* Map legacy light/dark onto Carbon theme tokens used by carbon-theme.css */
  document.documentElement.dataset.theme=next==='light'?'white':next==='dark'?'g100':next;
  if(next==='light'||next==='dark')document.documentElement.dataset.appearance=next;
  syncControl();
  syncSystemChrome();
  if(persist){try{localStorage.setItem(key,next);}catch{}}
 }
 let saved='dark';try{const value=localStorage.getItem(key);if(valid(value))saved=value;}catch{}
 apply(saved);
 document.addEventListener('DOMContentLoaded',()=>{
  syncControl();
  const button=document.getElementById('theme-toggle');
  if(button)button.addEventListener('click',()=>apply((document.documentElement.dataset.appearance||'dark')==='dark'?'light':'dark',true));
 });
 window.addEventListener('storage',event=>{if(event.key===key)apply(valid(event.newValue)?event.newValue:'dark');});
 window.madhvaTheme={set:theme=>apply(theme,true),get:()=>document.documentElement.dataset.appearance||(document.documentElement.dataset.theme==='white'?'light':'dark')};
})();
