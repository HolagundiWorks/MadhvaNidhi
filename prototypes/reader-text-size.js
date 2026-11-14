(() => {
  const key = 'madhvanidhi-content-text-scale';
  let scale = 1;
  try { scale = Math.max(1, Math.min(2, Number(localStorage.getItem(key)) || 1)); } catch {}
  const selector = '.songs-lyrics, .puja-section-body, .reader-body, .reader-body-sample, [data-japa-step-body], [data-japa-anga-panel] .cds--body-01';
  const hosts = new Map();
  const bases = new WeakMap();
  function apply() {
    document.querySelectorAll(selector).forEach(text => {
      if (!bases.has(text)) bases.set(text, parseFloat(getComputedStyle(text).fontSize));
      text.style.setProperty('font-size', bases.get(text) * scale + 'px', 'important');
      text.style.lineHeight = '1.7';
    });
    hosts.forEach(({increase, decrease}) => {
      increase.disabled = scale >= 2;
      decrease.disabled = scale <= 1;
      increase.title = 'Increase text size · ' + Math.round(scale * 100) + '%';
      decrease.title = 'Decrease text size · ' + Math.round(scale * 100) + '%';
    });
  }
  function change(delta) {
    scale = Math.max(1, Math.min(2, Math.round((scale + delta) * 100) / 100));
    try { localStorage.setItem(key, String(scale)); } catch {}
    apply();
  }
  function mount() {
    for (const [host] of hosts) if (!host.isConnected) hosts.delete(host);
    document.querySelectorAll(selector).forEach(text => {
      const host = text.closest('.songs-reader, .puja-section-reader, .sandhya-reader__main, [data-japa-anga-panel], .cds--tile') || text.parentElement;
      if (hosts.has(host)) return;
      host.classList.add('content-text-host');
      const anchor = document.createElement('div');
      anchor.className = 'content-text-anchor';
      const controls = document.createElement('div');
      controls.className = 'content-text-controls';
      controls.setAttribute('role', 'group');
      controls.setAttribute('aria-label', 'Reading text size');
      function button(label, text, delta) {
        const node = document.createElement('button');
        node.type = 'button';
        node.className = 'cds--btn cds--btn--tertiary';
        node.textContent = text;
        node.setAttribute('aria-label', label);
        node.addEventListener('click', () => change(delta));
        controls.append(node);
        return node;
      }
      const increase = button('Increase reading text size', 'A+', .1);
      const decrease = button('Decrease reading text size', 'A−', -.1);
      anchor.append(controls);
      host.prepend(anchor);
      hosts.set(host, {increase, decrease});
    });
    apply();
  }
  mount();
  new MutationObserver(mutations => {
    if (mutations.some(mutation => [...mutation.addedNodes].some(node => node.nodeType === 1))) mount();
  }).observe(document.body, {childList: true, subtree: true});
})();
