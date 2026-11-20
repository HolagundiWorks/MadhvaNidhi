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
    // Mirrors domain/measure, domain/aayadi, domain/search and data/rules/KarnatakaShodashavargaPack.kt.
    // The pack is DRAFT, so no candidate is judged PASS or FAIL. Only the remainders and the working are shown.
    const INCHES_PER_HASTA = 18;
    const RADIUS = 12;
    const NAMES = { 1: 'ಧ್ವಜ', 2: 'ಧೂಮ', 3: 'ಸಿಂಹ', 4: 'ಶ್ವಾನ', 5: 'ವೃಷಭ', 6: 'ಖರ', 7: 'ಗಜ', 8: 'ಕಾಕ' };
    const PACK = {
      reviewStatus: 'DRAFT',
      translatorOrEditor: '',
      params: [
        { key: 'aaya', name: 'ಆಯ', m: 9, mod: 8, operand: 'padam', criterion: { type: 'in', set: [1, 3, 5, 7] } },
        { key: 'dhana', name: 'ಧನ', m: 8, mod: 12, operand: 'padam', criterion: { type: 'unsourced', reason: 'ಧನ ಮತ್ತು ಋಣ ಹೋಲಿಕೆಯ ನಿಯಮ ಮೂಲದಲ್ಲಿ ದೃಢಪಟ್ಟಿಲ್ಲ' } },
        { key: 'runa', name: 'ಋಣ', m: 3, mod: 8, operand: 'padam', criterion: { type: 'unsourced', reason: 'ಧನ ಮತ್ತು ಋಣ ಹೋಲಿಕೆಯ ನಿಯಮ ಮೂಲದಲ್ಲಿ ದೃಢಪಟ್ಟಿಲ್ಲ' } },
        { key: 'ayush', name: 'ಆಯುಷ್ಯ', m: 9, mod: 120, operand: 'padam', criterion: { type: 'above', t: 60 } },
        { key: 'vyaya', name: 'ವ್ಯಯ', m: 9, mod: 10, operand: 'length', criterion: { type: 'unsourced', reason: 'ವ್ಯಯ < ಆಯ ನಿಯಮ ಮೂಲ ಗ್ರಂಥದಲ್ಲಿ ಇಲ್ಲ' } },
      ],
      notApplicable: ['yoni', 'nakshatra', 'vara', 'tithi'],
    };
    const TABLE_COLUMNS = ['ಉದ್ದ', 'ಅಗಲ', 'ವಿಸ್ತೀರ್ಣ (ಚ.ಅ.)', 'ಆಯ', 'ವ್ಯಯ', 'ಧನ', 'ಋಣ', 'ಯೋನಿ', 'ನಕ್ಷತ್ರ', 'ವಾರ', 'ತಿಥಿ'];

    const gcd = (a, b) => (b ? gcd(b, a % b) : Math.abs(a));
    const frac = (n, d) => { const g = gcd(n, d) || 1; const s = d < 0 ? -1 : 1; return { n: (n / g) * s, d: Math.abs(d / g) }; };
    const isWhole = f => f.d === 1;
    const fracStr = f => (isWhole(f) ? String(f.n) : `${f.n}/${f.d}`);
    const hasta = inches => frac(inches, INCHES_PER_HASTA);
    const fmt = n => String(Number(n.toFixed(2)));
    const feetInches = total => `${Math.floor(total / 12)}′${total % 12}″`;
    const range = (a, b) => (a === b ? feetInches(a) : `${feetInches(a)}–${feetInches(b)}`);

    // Problems that stop the pack giving judgments (same list as RulePack.validate()).
    function packProblems() {
      const out = [];
      if (!PACK.translatorOrEditor) out.push('ಸಂಪಾದಕ / ಅನುವಾದಕ ದಾಖಲಾಗಿಲ್ಲ');
      if (PACK.reviewStatus !== 'REVIEWED') out.push('ನಿಯಮ ಪಟ್ಟಿ ಇನ್ನೂ ಪರಿಶೀಲನೆಯಲ್ಲಿದೆ (DRAFT)');
      for (const p of PACK.params) if (p.criterion.type === 'unsourced') out.push(`${p.name}: ${p.criterion.reason}`);
      return out;
    }

    // One candidate, in exact arithmetic. Lengths are total inches.
    function evaluate(L, B) {
      const lh = hasta(L), bh = hasta(B);
      const padam = frac(lh.n * bh.n, lh.d * bh.d);
      const fractional = !isWhole(lh) || !isWhole(bh);
      const problems = packProblems();
      const draft = PACK.reviewStatus !== 'REVIEWED' || problems.length > 0;
      const parameters = {};
      for (const p of PACK.params) {
        const op = p.operand === 'padam' ? padam : lh;
        const exact = `${fracStr(op)} × ${p.m} mod ${p.mod}`;
        const row = { key: p.key, name: p.name, exact, remainder: null, classification: null, verdict: 'UNDETERMINED', mandatory: true, explanation: '' };
        if (!isWhole(op)) {
          row.explanation = 'ಭಿನ್ನ ಮೌಲ್ಯಕ್ಕೆ ಶೇಷದ ನಿಯಮ ಮೂಲದಲ್ಲಿ ಇಲ್ಲ';
        } else if (fractional) {
          row.explanation = 'ಭಿನ್ನ ಹಸ್ತ: ಪೂರ್ಣ ಹಸ್ತ ಮಾತ್ರ';
        } else {
          const raw = ((op.n * p.m) % p.mod + p.mod) % p.mod;
          row.remainder = raw === 0 ? p.mod : raw;
          row.explanation = draft ? 'ನಿಯಮ ಪಟ್ಟಿ DRAFT: ತೀರ್ಮಾನ ನೀಡುವುದಿಲ್ಲ' : '';
          if (!draft && p.criterion.type === 'in') row.verdict = p.criterion.set.includes(row.remainder) ? 'PASS' : 'FAIL';
          if (!draft && p.criterion.type === 'above') row.verdict = row.remainder > p.criterion.t ? 'PASS' : 'FAIL';
          if (!draft && p.criterion.type === 'unsourced') row.explanation = p.criterion.reason;
        }
        if (p.key === 'aaya' && row.remainder !== null && !draft) row.classification = NAMES[row.remainder] || null;
        parameters[p.key] = row;
      }
      for (const key of PACK.notApplicable) {
        parameters[key] = { key, name: key, exact: '', remainder: null, classification: 'Not applicable', verdict: 'UNDETERMINED', mandatory: false, explanation: 'ಈ ನಿಯಮ ಪಟ್ಟಿಯಲ್ಲಿ ಈ ಅಂಶಕ್ಕೆ ಮೂಲ ಸೂತ್ರವಿಲ್ಲ' };
      }
      const mandatory = Object.values(parameters).filter(p => p.mandatory);
      let reason = null;
      if (draft) reason = `ನಿಯಮ ಪಟ್ಟಿ DRAFT: ${problems[0] || 'ಪರಿಶೀಲನೆ ಬಾಕಿ'}`;
      else if (fractional) reason = 'ಭಿನ್ನ ಹಸ್ತ: ಈ ನಿಯಮ ಪಟ್ಟಿ ಪೂರ್ಣ ಹಸ್ತವನ್ನು ಮಾತ್ರ ಸ್ವೀಕರಿಸುತ್ತದೆ';
      let verdict = 'UNDETERMINED';
      if (!reason) {
        if (mandatory.some(p => p.verdict === 'FAIL')) verdict = 'FAIL';
        else if (mandatory.every(p => p.verdict === 'PASS')) verdict = 'PASS';
      }
      return { L, B, lh, bh, area: frac(L * B, 144), parameters, verdict, reason };
    }

    function candidates(L, B) {
      const out = [];
      for (let dl = -RADIUS; dl <= RADIUS; dl++) {
        for (let db = -RADIUS; db <= RADIUS; db++) {
          const l = L + dl, b = B + db;
          if (l <= 0 || b <= 0) continue;
          out.push([l, b]);
        }
      }
      return out;
    }

    function sortResults(list, L, B, largestArea) {
      const dev = c => Math.abs(c.L - L) + Math.abs(c.B - B);
      const maxDev = c => Math.max(Math.abs(c.L - L), Math.abs(c.B - B));
      const area = c => c.area.n / c.area.d;
      return [...list].sort((x, y) => {
        if (largestArea) return area(y) - area(x) || x.L - y.L || x.B - y.B;
        return dev(x) - dev(y) || maxDev(x) - maxDev(y) || area(y) - area(x) || x.L - y.L || x.B - y.B;
      });
    }

    function measure() {
      const lf = Number(byId('len-ft').value), li = Number(byId('len-in').value);
      const bf = Number(byId('br-ft').value), bi = Number(byId('br-in').value);
      const ok = [lf, bf].every(v => Number.isFinite(v) && v >= 0)
        && Number.isInteger(li) && li >= 0 && li <= 11 && Number.isInteger(bi) && bi >= 0 && bi <= 11;
      if (!ok) return { error: 'ಇಂಚು 0 ರಿಂದ 11 ರೊಳಗಿರಬೇಕು' };
      const L = lf * 12 + li, B = bf * 12 + bi;
      if (L <= 0 || B <= 0) return { error: 'ಉದ್ದ ಮತ್ತು ಅಗಲ ಸೊನ್ನೆಗಿಂತ ಹೆಚ್ಚಿರಬೇಕು' };
      return { L, B };
    }

    function tileText(p) {
      if (!p) return '—';
      const word = { PASS: 'ಶುಭ', FAIL: 'ಅಶುಭ', UNDETERMINED: 'ಬಾಕಿ' }[p.verdict];
      return `${p.remainder ?? '—'}${p.classification ? '  ·  ' + p.classification : ''}  (${word})`;
    }

    function fillTable(viable, configIncomplete) {
      const wrap = byId('aayadi-table-wrap');
      const empty = byId('aayadi-table-empty');
      const table = byId('aayadi-table');
      table.innerHTML = '';
      if (configIncomplete) {
        empty.textContent = 'ನಿಯಮ ಪಟ್ಟಿ ಪರಿಶೀಲನೆಗೆ ಬಾಕಿ: ಶುಭ ಅಳತೆಗಳನ್ನು ತೋರಿಸಲಾಗುವುದಿಲ್ಲ';
        wrap.hidden = true;
        return;
      }
      if (viable.length === 0) {
        empty.textContent = 'ಈ ವ್ಯಾಪ್ತಿಯಲ್ಲಿ ಎಲ್ಲಾ ಮುಖ್ಯ ನಿಯಮಗಳನ್ನು ಪೂರೈಸುವ ಅಳತೆ ಇಲ್ಲ';
        wrap.hidden = true;
        return;
      }
      empty.textContent = '';
      wrap.hidden = false;
      const head = table.createTHead().insertRow();
      TABLE_COLUMNS.forEach(t => { const th = document.createElement('th'); th.textContent = t; head.appendChild(th); });
      const body = table.createTBody();
      for (const c of viable) {
        const cell = k => (c.parameters[k].classification === 'Not applicable' ? 'Not applicable' : (c.parameters[k].remainder ?? '—'));
        const cells = [feetInches(c.L), feetInches(c.B), fmt(c.area.n / c.area.d), cell('aaya'), cell('vyaya'), cell('dhana'), cell('runa'), cell('yoni'), cell('nakshatra'), cell('vara'), cell('tithi')];
        const tr = body.insertRow();
        tr.tabIndex = 0;
        cells.forEach(t => { tr.insertCell().textContent = t; });
      }
    }

    function render() {
      const m = measure();
      byId('aayadi-error').hidden = !m.error;
      byId('aayadi-error').textContent = m.error || '';
      const shown = !m.error;
      ['aayadi-summary-card', 'aayadi-results', 'aayadi-table-card'].forEach(id => { byId(id).hidden = !shown; });
      if (!shown) return;
      const { L, B } = m;
      const original = evaluate(L, B);
      const all = candidates(L, B).map(([l, b]) => evaluate(l, b));
      const viable = sortResults(all.filter(c => c.verdict === 'PASS'), L, B, byId('aayadi-sort').checked);
      const problems = packProblems();
      const undetermined = all.filter(c => c.verdict === 'UNDETERMINED').length;
      byId('aayadi-summary-text').textContent =
        `ನೀಡಿದ ಅಳತೆ: ${feetInches(L)} × ${feetInches(B)}\n` +
        `ಹಸ್ತ: ${fracStr(original.lh)} × ${fracStr(original.bh)} (1 ಹಸ್ತ = ${INCHES_PER_HASTA} ಇಂಚು)\n` +
        `ಹುಡುಕಾಟ ವ್ಯಾಪ್ತಿ: ±1 ಅಡಿ, ${all.length} ಅಳತೆ\n` +
        `ಮೌಲ್ಯಮಾಪನ: ${all.length} · ಶುಭ: ${viable.length} · ತೀರ್ಮಾನ ಬಾಕಿ: ${undetermined}`;
      const cfg = byId('aayadi-config-error');
      cfg.hidden = problems.length === 0;
      cfg.textContent = problems.length ? `ನಿಯಮ ಪಟ್ಟಿ ಅಪೂರ್ಣ: ${problems.join('; ')}` : '';

      const P = original.parameters;
      byId('aayadi-summary').textContent = original.verdict === 'UNDETERMINED'
        ? `ತೀರ್ಮಾನ ಬಾಕಿ: ${original.reason || ''}`
        : (original.verdict === 'PASS' ? 'ಎಲ್ಲಾ ಅಂಶಗಳೂ ಅನುಕೂಲ' : 'ಪರಿಶೀಲಿಸಿ');
      byId('aayadi-padam').textContent = `${fracStr(frac(original.lh.n * original.bh.n, original.lh.d * original.bh.d))} ಚದರ ಹಸ್ತ`;
      byId('aayadi-aaya').textContent = tileText(P.aaya);
      byId('aayadi-vyaya').textContent = tileText(P.vyaya);
      byId('aayadi-dhana').textContent = `${P.dhana.remainder ?? '—'} (${P.dhana.verdict === 'PASS' ? 'ಶುಭ' : 'ಬಾಕಿ'})`;
      byId('aayadi-rina').textContent = `${P.runa.remainder ?? '—'} (${P.runa.verdict === 'PASS' ? 'ಶುಭ' : 'ಬಾಕಿ'})`;
      byId('aayadi-ayush').textContent = tileText(P.ayush);
      fillTable(viable, problems.length > 0);
    }

    ['len-ft', 'len-in', 'br-ft', 'br-in', 'aayadi-sort'].forEach(id => byId(id).addEventListener('change', render));
    ['len-ft', 'br-ft'].forEach(id => byId(id).addEventListener('input', render));
    byId('aayadi-calc').addEventListener('click', render);
    render();
  }());
