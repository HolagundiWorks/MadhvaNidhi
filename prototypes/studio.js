/**
 * Portrait mockup studio: viewport preset + live iframe preview.
 */
(() => {
  const PRESETS = [
    { id: "m115f-logical", label: "Galaxy M11 app · 387 × 854 · 90% text", w: 678 / 1.75, h: 1495 / 1.75, fontScale: 0.9 },
    { id: "phone-sm", label: "Phone Portrait · 360 × 640", w: 360, h: 640 },
    { id: "phone", label: "Phone Portrait · 390 × 844", w: 390, h: 844 },
    { id: "phone-lg", label: "Phone Portrait · 412 × 915", w: 412, h: 915 },
    { id: "m115f", label: "SM-M115F Portrait · 720 × 1560", w: 720, h: 1560 },
    { id: "m115f-app", label: "SM-M115F App Area · 694 × 1495", w: 694, h: 1495 },
    { id: "compact", label: "Compact Portrait · 360 × 720", w: 360, h: 720 },
    { id: "7in", label: "7-inch Tablet · 600 × 1024", w: 600, h: 1024 },
    { id: "8in", label: "8-inch Tablet · 800 × 1280", w: 800, h: 1280 },
    { id: "10in", label: "10-inch Tablet · 800 × 1280", w: 800, h: 1280 },
    { id: "custom", label: "Custom Portrait Resolution", w: null, h: null }
  ];

  const root = document.currentScript;
  const storageKey = root?.dataset.storage || "madhva-mockup-viewport-v2-portrait";
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

  let state = { preset: "m115f-logical", customW: 678 / 1.75, customH: 1495 / 1.75 };
  try {
    const saved = JSON.parse(localStorage.getItem(storageKey));
    if (saved) state = { ...state, ...saved };
    // Migrate landscape presets / dimensions into portrait defaults
    if (["m115f", "m115f-app"].includes(state.preset) && state.customW > state.customH) {
      state.preset = "m115f-logical";
      state.customW = 678 / 1.75;
      state.customH = 1495 / 1.75;
    }
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
      || PRESETS.find((p) => p.id === "m115f-logical")
      || PRESETS[0];
    return { w: preset.w, h: preset.h };
  }

  function validate(w, h) {
    if (!Number.isFinite(w) || !Number.isFinite(h) || w < 240 || h < 320) {
      return "Enter valid portrait dimensions.";
    }
    if (h <= w) {
      return "Portrait required. Height must exceed width.";
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

    const deviceFontScale = PRESETS.find(p => p.id === state.preset)?.fontScale || 1;
    try {
      frame.contentDocument.documentElement.style.setProperty('--device-font-scale', String(deviceFontScale));
    } catch {}
    frame.style.width = `${w}px`;
    frame.style.height = `${h}px`;
    frame.style.transform = `scale(${zoom})`;
    const wrap = frame.parentElement;
    if (wrap) {
      wrap.style.width = `${w * zoom}px`;
      wrap.style.height = `${h * zoom}px`;
    }
    if (dims) {
      dims.textContent = `${Math.round(w)} × ${Math.round(h)} CSS px · ${Math.round(zoom * 100)}% fit`;
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

  frame.addEventListener("load", () => {
    fit();
    const screen = new URLSearchParams(location.search).get("screen");
    if (screen) frame.contentWindow.madhvaShell?.navigate(screen);
    const tab = new URLSearchParams(location.search).get("tab");
    if (screen === "settings" && tab) frame.contentWindow.selectSettingsTab?.(tab);
  });
  new ResizeObserver(fit).observe(stage);
  fit();
})();
