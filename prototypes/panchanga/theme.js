(() => {
 'use strict';
 const key='madhvanidhi-preview-theme';
 const valid=value=>value==='light'||value==='dark';
 function syncControl(){
  const button=document.getElementById('theme-toggle');if(!button)return;
  const dark=document.documentElement.dataset.theme!=='light';
  const action=dark?'Switch to light theme':'Switch to dark theme';
  button.setAttribute('aria-label',action);button.title=action;
 }
 function apply(theme,persist=false){
  document.documentElement.dataset.theme=valid(theme)?theme:'dark';
  syncControl();
  if(persist){try{localStorage.setItem(key,document.documentElement.dataset.theme);}catch{}}
 }
 let saved='dark';try{const value=localStorage.getItem(key);if(valid(value))saved=value;}catch{}
 apply(saved);
 document.addEventListener('DOMContentLoaded',()=>{
  syncControl();
  const button=document.getElementById('theme-toggle');
  if(button)button.addEventListener('click',()=>apply(document.documentElement.dataset.theme==='dark'?'light':'dark',true));
 });
 window.addEventListener('storage',event=>{if(event.key===key)apply(valid(event.newValue)?event.newValue:'dark');});
 window.madhvaTheme={set:theme=>apply(theme,true),get:()=>document.documentElement.dataset.theme};
})();
