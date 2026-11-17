const samskrutiIds = ['aayadi', 'rangavalli', 'recipes'];
function selectSamskrutiTab(id) {
  for (const key of samskrutiIds) {
    const selected = key === id;
    const tab = document.getElementById('samskruti-tab-' + key);
    tab.tabIndex = selected ? 0 : -1;
    tab.setAttribute('aria-selected', String(selected));
    document.getElementById(tab.getAttribute('aria-controls')).hidden = !selected;
  }
}
samskrutiIds.forEach((id, index) => {
  const tab = document.getElementById('samskruti-tab-' + id);
  tab.addEventListener('click', () => selectSamskrutiTab(id));
  tab.addEventListener('keydown', event => {
    const positions = { Home: 0, End: 2, ArrowLeft: (index + 2) % 3, ArrowRight: (index + 1) % 3 };
    if (!(event.key in positions)) return;
    event.preventDefault();
    const next = samskrutiIds[positions[event.key]];
    selectSamskrutiTab(next);
    document.getElementById('samskruti-tab-' + next).focus();
  });
});

  (function bindAayadi() {
    const byId = id => document.getElementById(id);
    // Same rules as app/src/main/java/com/paperdesk/launcher/data/Aayadi.kt.
    const NAMES = ['', 'ಧ್ವಜ', 'ಧೂಮ', 'ಸಿಂಹ', 'ಶ್ವಾನ', 'ವೃಷಭ', 'ಖರ', 'ಗಜ', 'ಕಾಕ'];
    const GOOD = [1, 3, 5, 7];
    // A zero remainder is reported as the divisor, not as 0.
    const rem = (value, divisor) => {
      const raw = value % divisor;
      return Math.abs(raw) < 0.000001 ? divisor : raw;
    };
    const fmt = n => String(Number(n.toFixed(2)));

    // Shodashavarga table (Kannada book, page 97): area is length x breadth in square feet. Inches only 0, 3, 6, 9,
    // and the area must be whole. As in Aayadi.kt.
    const INCH_STEPS = [0, 3, 6, 9];
    function calculate(lengthFt, lengthIn, breadthFt, breadthIn) {
      if (![lengthFt, lengthIn, breadthFt, breadthIn].every(v => Number.isFinite(v) && v >= 0)) return { problem: 'empty' };
      if (!INCH_STEPS.includes(lengthIn) || !INCH_STEPS.includes(breadthIn)) return { problem: 'inches' };
      const length = lengthFt + lengthIn / 12;
      const breadth = breadthFt + breadthIn / 12;
      if (!(length > 0 && breadth > 0)) return { problem: 'empty' };
      const padam = length * breadth;
      if (padam % 1 !== 0) return { problem: 'area' };
      const aaya = rem(padam * 9, 8);
      const dhana = rem(padam * 8, 12);
      const runa = rem(padam * 3, 8);
      const ayush = rem(padam * 9, 120);
      // Vyaya is defined on whole numbers only: null when the length is not whole feet.
      const vyaya = length % 1 === 0 ? rem(length * 9, 10) : null;
      const whole = Math.trunc(aaya);
      const ordinal = Math.abs(aaya - whole) < 0.001 ? whole : null;
      const aayaName = ordinal >= 1 && ordinal <= 8 ? NAMES[ordinal] : 'ಭಿನ್ನಾಂಶ ಫಲ';
      const tests = [
        GOOD.includes(ordinal) ? 'ಆಯ ಅನುಕೂಲ' : 'ಆಯ ಪರಿಶೀಲಿಸಿ',
        dhana > runa ? 'ಧನ > ಋಣ' : 'ಧನ ≤ ಋಣ',
        ayush > 60 ? 'ಆಯುಷ್ಯ > 60' : 'ಆಯುಷ್ಯ ≤ 60',
      ];
      return {
        padam, aaya, vyaya, dhana, runa, ayush, aayaName, summary: tests.join('  ·  '),
        aayaOk: GOOD.includes(ordinal),
        vyayaOk: vyaya === null ? null : vyaya < aaya,
        ayushOk: ayush > 60,
      };
    }

    // Red (Carbon support-error) when a value falls outside its parameter.
    function alertColour(id, ok) {
      byId(id).style.color = ok ? '' : '#da1e28';
    }

    function render() {
      const r = calculate(
        Number(byId('len-ft').value) || 0, Number(byId('len-in').value),
        Number(byId('br-ft').value) || 0, Number(byId('br-in').value)
      );
      const failed = !!r.problem;
      byId('aayadi-error').hidden = !failed;
      byId('aayadi-error').textContent = {
        empty: 'ಉದ್ದ ಮತ್ತು ಅಗಲ ಸೊನ್ನೆಗಿಂತ ಹೆಚ್ಚಿರಬೇಕು',
        inches: 'ಇಂಚು 0, 3, 6 ಅಥವಾ 9 ಆಗಿರಬೇಕು',
        area: 'ಕ್ಷೇತ್ರಫಲ ಪೂರ್ಣ ಚದರ ಅಡಿ ಆಗಬೇಕು. ಇಂಚನ್ನು ಬದಲಿಸಿ (ಉದಾ: 30 ಅಡಿ 3 ಇಂಚು × 40 ಅಡಿ)',
      }[r.problem] || '';
      byId('aayadi-results').hidden = failed;
      if (failed) return;
      byId('aayadi-padam').textContent = fmt(r.padam);
      byId('aayadi-aaya').textContent = `${fmt(r.aaya)}  ·  ${r.aayaName}`;
      byId('aayadi-vyaya').textContent = r.vyaya === null ? '—' : fmt(r.vyaya);
      byId('aayadi-dhana').textContent = fmt(r.dhana);
      byId('aayadi-rina').textContent = fmt(r.runa);
      byId('aayadi-ayush').textContent = fmt(r.ayush);
      byId('aayadi-summary').textContent = r.summary;
      alertColour('aayadi-aaya', r.aayaOk);
      alertColour('aayadi-vyaya', r.vyayaOk !== false);
      alertColour('aayadi-ayush', r.ayushOk);
    }

    byId('aayadi-calc').addEventListener('click', render);
    render();
  }());
