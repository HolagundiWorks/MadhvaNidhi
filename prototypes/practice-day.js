/* Shared mock PracticeDayStore — Sandhya / Japa / Puja day flags (localStorage). */
(() => {
  'use strict';

  const PRACTICE_KEY = 'madhvanidhi-practice-day';

  function dateKey(d = new Date()) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  function load() {
    try {
      return JSON.parse(localStorage.getItem(PRACTICE_KEY) || '{}') || {};
    } catch {
      return {};
    }
  }

  function save(map) {
    try {
      localStorage.setItem(PRACTICE_KEY, JSON.stringify(map));
    } catch {}
  }

  function row(date = dateKey()) {
    const map = load();
    return map[date] || {};
  }

  function write(mutator, date = dateKey()) {
    const map = load();
    const next = { ...(map[date] || {}) };
    mutator(next);
    map[date] = next;
    save(map);
    return next;
  }

  function normalizeSandhya(session) {
    if (session === 'madhya' || session === 'madhyahna_sandhya') return 'madhyahna';
    if (session === 'pratah_sandhya') return 'pratah';
    if (session === 'sayam_sandhya') return 'sayam';
    return session;
  }

  function setSandhya(session, done, date = dateKey()) {
    write((r) => {
      const s = normalizeSandhya(session);
      r[s] = !!done;
      if (done) r[`sandhya_skip_${s}`] = false;
    }, date);
  }

  function setSandhyaDay(session, complete, skipped, date = dateKey()) {
    write((r) => {
      const s = normalizeSandhya(session);
      r[s] = !!complete && !skipped;
      r[`sandhya_skip_${s}`] = !!skipped && !complete;
    }, date);
  }

  function isSandhyaSkipped(session, date = dateKey()) {
    const s = normalizeSandhya(session);
    const r = row(date);
    return !!r[`sandhya_skip_${s}`] && !r[s];
  }

  function isSandhyaDone(session, date = dateKey()) {
    const s = normalizeSandhya(session);
    return !!row(date)[s] && !isSandhyaSkipped(session, date);
  }

  function sandhyaFlags(date = dateKey()) {
    const r = row(date);
    return [!!r.pratah, !!(r.madhyahna || r.madhya), !!r.sayam];
  }

  function setJapaDay(complete, skipped, date = dateKey()) {
    write((r) => {
      r.japa_complete = !!complete;
      r.japa_skipped = !!skipped && !complete;
    }, date);
  }

  function japaComplete(date = dateKey()) {
    return !!row(date).japa_complete;
  }

  function japaSkipped(date = dateKey()) {
    return !!row(date).japa_skipped && !japaComplete(date);
  }

  function setPujaDay(complete, skipped, date = dateKey()) {
    write((r) => {
      r.puja_complete = !!complete;
      r.puja_skipped = !!skipped && !complete;
    }, date);
  }

  function pujaComplete(date = dateKey()) {
    return !!row(date).puja_complete;
  }

  function pujaSkipped(date = dateKey()) {
    return !!row(date).puja_skipped && !pujaComplete(date);
  }

  function setParayanaDay(complete, skipped, date = dateKey()) {
    write((r) => {
      r.parayana_complete = !!complete;
      r.parayana_skipped = !!skipped && !complete;
    }, date);
  }

  function parayanaComplete(date = dateKey()) {
    return !!row(date).parayana_complete;
  }

  function parayanaSkipped(date = dateKey()) {
    return !!row(date).parayana_skipped && !parayanaComplete(date);
  }

  /** Apply aria-pressed on Skip/Complete within a day-actions group. */
  function paintDayActions(group, skipped, complete) {
    if (!group) return;
    group.querySelectorAll('[data-day-action]').forEach((btn) => {
      const kind = btn.dataset.dayAction;
      const on = kind === 'skip' ? !!skipped : kind === 'complete' ? !!complete : false;
      btn.setAttribute('aria-pressed', on ? 'true' : 'false');
    });
  }

  window.madhvaPracticeDay = {
    dateKey,
    load,
    save,
    setSandhya,
    setSandhyaDay,
    isSandhyaSkipped,
    isSandhyaDone,
    sandhyaFlags,
    setJapaDay,
    japaComplete,
    japaSkipped,
    setPujaDay,
    pujaComplete,
    pujaSkipped,
    setParayanaDay,
    parayanaComplete,
    parayanaSkipped,
    paintDayActions
  };
})();
