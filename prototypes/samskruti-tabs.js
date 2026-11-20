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
    // Mirrors domain/measure, domain/aayadi, domain/search and assets/rules/karnataka-shodashavarga.json.
    // Exact fractions with BigInt: no rounding. Status APPLICATION_STANDARD: the documented criteria are used,
    // and the source has not been checked against the printed book yet.
    const INCHES_PER_HASTA = 18n;
    const RADIUS = 12;
    const NAMES = { 1: 'ಧ್ವಜ', 2: 'ಧೂಮ', 3: 'ಸಿಂಹ', 4: 'ಶ್ವಾನ', 5: 'ವೃಷಭ', 6: 'ಖರ', 7: 'ಗಜ', 8: 'ಕಾಕ' };
    const PACK = {
      reviewStatus: 'APPLICATION_STANDARD',
      fractionalPolicy: 'EXACT_FRACTION',
      params: [
        { key: 'aaya', name: 'ಆಯ', m: 9n, mod: 8n, operand: 'padam', labels: NAMES, criterion: { type: 'in', set: [1, 3, 5, 7] }, mandatory: true },
        { key: 'dhana', name: 'ಧನ', m: 8n, mod: 12n, operand: 'padam', criterion: { type: 'compare', other: 'runa', relation: 'GREATER' }, mandatory: true },
        { key: 'runa', name: 'ಋಣ', m: 3n, mod: 8n, operand: 'padam', criterion: { type: 'none' }, mandatory: false },
        { key: 'ayush', name: 'ಆಯುಷ್ಯ', m: 9n, mod: 120n, operand: 'padam', criterion: { type: 'above', t: 60n }, mandatory: true },
        { key: 'vyaya', name: 'ವ್ಯಯ', m: 9n, mod: 10n, operand: 'length', criterion: { type: 'compare', other: 'aaya', relation: 'LESS' }, mandatory: true },
      ],
    };
    const TABLE_COLUMNS = ['Length', 'Breadth', 'Aaya', 'Dhana', 'Runa / Vyaya', 'Aayushu', 'Result'];

    const gcd = (a, b) => (b ? gcd(b, a % b) : (a < 0n ? -a : a));
    // A fraction {n, d} with d > 0, reduced.
    const frac = (n, d) => {
      const g = gcd(n, d) || 1n;
      let nn = n / g, dd = d / g;
      if (dd < 0n) { nn = -nn; dd = -dd; }
      return { n: nn, d: dd };
    };
    const add = (a, b) => frac(a.n * b.d - b.n * a.d, a.d * b.d);
    const mul = (a, b) => frac(a.n * b.n, a.d * b.d);
    const cmp = (a, b) => { const x = a.n * b.d, y = b.n * a.d; return x < y ? -1 : (x > y ? 1 : 0); };
    const isWhole = f => f.d === 1n;
    const isZero = f => f.n === 0n;
    const fracStr = f => (isWhole(f) ? String(f.n) : `${f.n}/${f.d}`);
    const fracNum = f => Number(f.n) / Number(f.d);
    const hasta = inches => frac(BigInt(inches), INCHES_PER_HASTA);
    // Exact remainder in [0, m): x - m * floor(x / m). Zero is reported as the modulus.
    function modulo(x, m) {
      const mm = BigInt(m);
      const q = x.n / (x.d * mm); // floor(x / m): x >= 0 for every use here, so truncation is floor
      const r = frac(x.n - mm * q * x.d, x.d);
      return isZero(r) ? { n: mm, d: 1n } : r;
    }
    const fmt = n => String(Number(n.toFixed(2)));
    const feetInches = total => `${Math.floor(total / 12)}′${total % 12}″`;

    // Problems that stop the pack giving judgments (same list as RulePack.validate()).
    function packProblems() {
      const out = [];
      if (PACK.reviewStatus === 'DRAFT') out.push('ನಿಯಮ ಪಟ್ಟಿ ಇನ್ನೂ ಪರಿಶೀಲನೆಯಲ್ಲಿದೆ (DRAFT)');
      return out;
    }

    // One candidate. Lengths are total inches.
    function evaluate(L, B) {
      const lh = hasta(L), bh = hasta(B);
      const padam = mul(lh, bh);
      const problems = packProblems();
      const draft = PACK.reviewStatus === 'DRAFT' || problems.length > 0;
      const fractional = !isWhole(lh) || !isWhole(bh);
      const working = {};
      for (const p of PACK.params) {
        const op = p.operand === 'padam' ? padam : (p.operand === 'length' ? lh : bh);
        const exact = `${fracStr(op)} × ${p.m} mod ${p.mod}`;
        const rem = (fractional && PACK.fractionalPolicy === 'WHOLE_HASTA_ONLY') ? null : modulo(mul(op, { n: p.m, d: 1n }), p.mod);
        working[p.key] = { p, exact, rem };
      }
      const parameters = {};
      for (const p of PACK.params) {
        const w = working[p.key];
        const row = { key: p.key, name: p.name, exact: w.exact, remainder: w.rem, classification: null, verdict: 'UNDETERMINED', mandatory: p.mandatory, explanation: '' };
        if (!w.rem) { row.explanation = 'ಭಿನ್ನ ಹಸ್ತ: ಪೂರ್ಣ ಹಸ್ತ ಮಾತ್ರ'; }
        else if (draft) { row.explanation = 'ನಿಯಮ ಪಟ್ಟಿ DRAFT: ತೀರ್ಮಾನ ನೀಡುವುದಿಲ್ಲ'; }
        else {
          const c = p.criterion;
          const whole = isWhole(w.rem) ? w.rem.n : null;
          if (whole !== null && NAMES[Number(whole)] && p.labels) row.classification = NAMES[Number(whole)];
          if (c.type === 'in') {
            const ok = whole !== null && c.set.includes(Number(whole));
            row.verdict = ok ? 'PASS' : 'FAIL';
            row.explanation = ok ? 'ಶುಭ ವರ್ಗ' : 'ಅಶುಭ ವರ್ಗ';
          } else if (c.type === 'above') {
            const ok = cmp(w.rem, { n: c.t, d: 1n }) > 0;
            row.verdict = ok ? 'PASS' : 'FAIL';
            row.explanation = `ಶೇಷ ${c.t} ಕ್ಕಿಂತ ಹೆಚ್ಚು ಇರಬೇಕು`;
          } else if (c.type === 'compare') {
            const other = working[c.other].rem;
            const ok = c.relation === 'GREATER' ? cmp(w.rem, other) > 0 : cmp(w.rem, other) < 0;
            row.verdict = ok ? 'PASS' : 'FAIL';
            row.explanation = `${p.name} (${fracStr(w.rem)}) ${c.relation === 'GREATER' ? '>' : '<'} ${c.other} (${fracStr(other)})`;
          } else {
            row.explanation = 'ಮೌಲ್ಯ ಮಾತ್ರ; ನಿರ್ಧಾರ ಮಾನದಂಡ ಇಲ್ಲ';
          }
        }
        parameters[p.key] = row;
      }
      const mandatory = Object.values(parameters).filter(p => p.mandatory);
      let reason = null;
      if (draft) reason = `ನಿಯಮ ಪಟ್ಟಿ DRAFT: ${problems[0] || 'ಪರಿಶೀಲನೆ ಬಾಕಿ'}`;
      else if (fractional && PACK.fractionalPolicy === 'WHOLE_HASTA_ONLY') reason = 'ಭಿನ್ನ ಹಸ್ತ: ಈ ನಿಯಮ ಪಟ್ಟಿ ಪೂರ್ಣ ಹಸ್ತವನ್ನು ಮಾತ್ರ ಸ್ವೀಕರಿಸುತ್ತದೆ';
      let verdict = 'UNDETERMINED';
      if (!reason) {
        if (mandatory.some(p => p.verdict === 'FAIL')) verdict = 'FAIL';
        else if (mandatory.every(p => p.verdict === 'PASS')) verdict = 'PASS';
      }
      return { L, B, lh, bh, padam, area: frac(BigInt(L) * BigInt(B), 144n), parameters, verdict, reason };
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
      const area = c => fracNum(c.area);
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

    const verdictWord = p => ({ PASS: 'Pass', FAIL: 'Fail', UNDETERMINED: 'Undetermined' }[p ? p.verdict : 'UNDETERMINED']);
    function tileText(p) {
      if (!p) return '—';
      return `${p.remainder ? fracStr(p.remainder) : '—'}${p.classification ? '  ·  ' + p.classification : ''}  (${verdictWord(p)})`;
    }
    const cell = (c, k) => {
      const p = c.parameters[k];
      if (!p || !p.remainder) return '—';
      const rem = fracStr(p.remainder);
      return k === 'aaya' && p.classification ? `${rem} ${p.classification}` : rem;
    };

    function fillTable(viable, L, B, configIncomplete) {
      const wrap = byId('aayadi-table-wrap');
      const empty = byId('aayadi-table-empty');
      const table = byId('aayadi-table');
      table.innerHTML = '';
      if (configIncomplete) {
        empty.textContent = 'No valid combinations can be shown: the rule pack is incomplete';
        wrap.hidden = true;
        return;
      }
      if (viable.length === 0) {
        empty.textContent = 'No valid combinations found within ±1 foot.';
        wrap.hidden = true;
        return;
      }
      empty.textContent = '';
      wrap.hidden = false;
      const head = table.createTHead().insertRow();
      TABLE_COLUMNS.forEach(t => { const th = document.createElement('th'); th.textContent = t; head.appendChild(th); });
      const body = table.createTBody();
      for (const c of viable) {
        const isOriginal = c.L === L && c.B === B;
        const cells = [
          feetInches(c.L), feetInches(c.B), cell(c, 'aaya'), cell(c, 'dhana'),
          `${cell(c, 'runa')} / ${cell(c, 'vyaya')}`, cell(c, 'ayush'),
          isOriginal ? 'Pass (original)' : 'Pass',
        ];
        const tr = body.insertRow();
        tr.tabIndex = 0;
        if (isOriginal) tr.style.fontWeight = '700';
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
      byId('aayadi-summary-text').textContent =
        `ನೀಡಿದ ಅಳತೆ: ${feetInches(L)} × ${feetInches(B)}\n` +
        `ಹಸ್ತ: ${fracStr(original.lh)} × ${fracStr(original.bh)} (1 ಹಸ್ತ = ${INCHES_PER_HASTA} ಇಂಚು)\n` +
        `ಹುಡುಕಾಟ ವ್ಯಾಪ್ತಿ: ±1 ಅಡಿ, ${all.length} ಅಳತೆ\n` +
        `ಮೌಲ್ಯಮಾಪನ: ${all.length} · Pass: ${viable.length} · ಬಾಕಿ: ${all.filter(c => c.verdict === 'UNDETERMINED').length}`;
      const cfg = byId('aayadi-config-error');
      cfg.hidden = problems.length === 0;
      cfg.textContent = problems.length ? `ನಿಯಮ ಪಟ್ಟಿ ಅಪೂರ್ಣ: ${problems.join('; ')}` : '';

      const P = original.parameters;
      byId('aayadi-summary').textContent = original.verdict === 'PASS' ? 'Pass: all mandatory conditions are met'
        : (original.verdict === 'FAIL'
          ? 'Fail: ' + Object.values(P).filter(p => p.mandatory && p.verdict === 'FAIL').map(p => p.name).join(', ')
          : `Undetermined: ${original.reason || ''}`);
      byId('aayadi-padam').textContent = `${fracStr(original.padam)} ಚದರ ಹಸ್ತ`;
      byId('aayadi-aaya').textContent = tileText(P.aaya);
      byId('aayadi-vyaya').textContent = tileText(P.vyaya);
      byId('aayadi-dhana').textContent = `Dhana: ${P.dhana.remainder ? fracStr(P.dhana.remainder) : '—'} (${verdictWord(P.dhana)})`;
      byId('aayadi-rina').textContent = `Runa: ${P.runa.remainder ? fracStr(P.runa.remainder) : '—'} (${verdictWord(P.runa)})`;
      byId('aayadi-ayush').textContent = tileText(P.ayush);
      fillTable(viable, L, B, problems.length > 0);
    }

    ['len-ft', 'len-in', 'br-ft', 'br-in', 'aayadi-sort'].forEach(id => byId(id).addEventListener('change', render));
    ['len-ft', 'br-ft'].forEach(id => byId(id).addEventListener('input', render));
    byId('aayadi-calc').addEventListener('click', render);
    render();
  }());
