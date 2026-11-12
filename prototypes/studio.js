/**
 * Landscape-only mockup studio: viewport preset + live iframe preview.
 * MOCKUP_ONLY — do not couple to Android.
 */
(() => {
  const PRESETS = [
    { id: "phone-sm", label: "Phone Landscape · 640 × 360", w: 640, h: 360 },
    { id: "phone", label: "Phone Landscape · 800 × 480", w: 800, h: 480 },
    { id: "phone-lg", label: "Phone Landscape · 960 × 540", w: 960, h: 540 },
    { id: "m115f", label: "SM-M115F Landscape · 1560 × 720", w: 1560, h: 720 },
    { id: "m115f-app", label: "SM-M115F App Area · 1495 × 694", w: 1495, h: 694 },
    { id: "compact", label: "Compact Landscape · 960 × 600", w: 960, h: 600 },
    { id: "7in", label: "7-inch Tablet · 1024 × 600", w: 1024, h: 600 },
    { id: "8in", label: "8-inch Tablet · 1280 × 800", w: 1280, h: 800 },
    { id: "10in", label: "10-inch Tablet · 1920 × 1200", w: 1920, h: 1200 },
    { id: "11in", label: "11-inch Tablet · 2000 × 1200", w: 2000, h: 1200 },
    { id: "12in", label: "12-inch Tablet · 2560 × 1600", w: 2560, h: 1600 },
    { id: "custom", label: "Custom Landscape Resolution", w: null, h: null }
  ];

  const root = document.currentScript;
  const storageKey = root?.dataset.storage || "madhva-mockup-viewport-v1";
  const frame = document.getElementById("preview");
  const select = document.getElementById("viewport");
  const stage = document.querySelector(".stage");
  const dims = document.getElementById("dimensions");
  const err = document.getElementById("viewport-error");
  const customFields = document.getElementById("custom-fields");
  const customW = document.getElementById("custom-w");
  const customH = document.getElementById("custom-h");

  if (!frame || !select || !stage) return;

  PRESETS.forEach((p) => {
    const opt = document.createElement("option");
    opt.value = p.id;
    opt.textContent = p.label;
    select.appendChild(opt);
  });

  let state = { preset: "m115f", customW: 1560, customH: 720 };
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey));
    if (saved) state = { ...state, ...saved };
  } catch {}

  select.value = state.preset;
  if (customW) customW.value = String(state.customW);
  if (customH) customH.value = String(state.customH);

  function save() {
    try {
      localStorage.setItem(storageKey, JSON.stringify(state));
    } catch {}
  }

  function resolveSize() {
    if (state.preset === "custom") {
      const w = Number(customW?.value || state.customW);
      const h = Number(customH?.value || state.customH);
      return { w, h };
    }
    const preset = PRESETS.find((p) => p.id === state.preset)
      || PRESETS.find((p) => p.id === "m115f")
      || PRESETS[0];
    return { w: preset.w, h: preset.h };
  }

  function validate(w, h) {
    if (!Number.isFinite(w) || !Number.isFinite(h) || w < 320 || h < 240) {
      return "Enter valid landscape dimensions.";
    }
    if (w <= h) {
      return "Portrait is not allowed. Width must exceed height.";
    }
    return "";
  }

  function fit() {
    const { w, h } = resolveSize();
    const message = validate(w, h);
    if (err) {
      err.textContent = message;
      err.hidden = !message;
    }
    if (message) return;

    state.customW = w;
    state.customH = h;
    customFields?.classList.toggle("is-open", state.preset === "custom");

    const pad = 32;
    const availableW = Math.max(160, stage.clientWidth - pad);
    const availableH = Math.max(160, stage.clientHeight - pad);
    const zoom = Math.min(1, availableW / w, availableH / h);

    frame.style.width = `${w}px`;
    frame.style.height = `${h}px`;
    frame.style.transform = `scale(${zoom})`;
    const wrap = frame.parentElement;
    if (wrap) {
      wrap.style.width = `${w * zoom}px`;
      wrap.style.height = `${h * zoom}px`;
    }
    if (dims) {
      dims.textContent = `${w} × ${h} · ${Math.round(zoom * 100)}% fit`;
    }
    save();
  }

  select.addEventListener("change", () => {
    state.preset = select.value;
    fit();
  });
  customW?.addEventListener("change", fit);
  customH?.addEventListener("change", fit);
  customW?.addEventListener("input", () => {
    state.customW = Number(customW.value);
  });
  customH?.addEventListener("input", () => {
    state.customH = Number(customH.value);
  });

  frame.addEventListener("load", fit);
  new ResizeObserver(fit).observe(stage);
  fit();
})();
