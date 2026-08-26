import fs from "fs";
import path from "path";
import vm from "vm";
import { fileURLToPath } from "url";

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "..");
const knDir = path.join(root, "tools", "kn");
const texts = path.join(root, "app", "src", "main", "assets", "madhwa", "texts");

const ctx = {};
vm.createContext(ctx);
vm.runInContext(
  fs.readFileSync(path.join(knDir, "map_nudi_baraha.js"), "utf8") + "\n" +
  fs.readFileSync(path.join(knDir, "helper.js"), "utf8") + "\n" +
  fs.readFileSync(path.join(knDir, "a2u.js"), "utf8") + "\n" +
  `var _dv = dependent_vowels.slice();
   dependent_vowels = {};
   for (var i = 0; i < _dv.length; i++) dependent_vowels[_dv[i]] = _dv[i];`,
  ctx
);

function ascii2unicode(raw) {
  return raw.split(/\r?\n/).map((line) => ctx.kn_ascii2unicode(line)).join("\n");
}

function knToSa(text) {
  let out = "";
  for (const ch of text) {
    const cp = ch.codePointAt(0);
    if (cp >= 0x0c80 && cp <= 0x0cff) out += String.fromCodePoint(cp - 0x0380);
    else out += ch;
  }
  return out;
}

function dropJunk(line) {
  const t = line.trim();
  if (!t) return true;
  if (/^--\s*\d+\s+of\s+\d+\s*--$/.test(t)) return true;
  if (/^Dasara Padagalu/i.test(t)) return true;
  if (/^Sri Raghavendra/i.test(t)) return true;
  if (/^MADHWA /i.test(t)) return true;
  if (/^\d+$/.test(t)) return true;
  const kn = (t.match(/[\u0C80-\u0CFF]/g) || []).length;
  const sa = (t.match(/[\u0900-\u097F]/g) || []).length;
  const lat = (t.match(/[A-Za-z]/g) || []).length;
  if (kn + sa < 4 && lat >= 8) return true;
  return false;
}

function tidy(text) {
  const keep = [];
  for (const line of text.split("\n")) {
    if (dropJunk(line)) {
      if (keep.length && keep[keep.length - 1] !== "") keep.push("");
      continue;
    }
    keep.push(line.replace(/[ \t]+$/g, "").replace(/[ \t]{2,}/g, " "));
  }
  return keep.join("\n").replace(/\n{3,}/g, "\n\n").trim() + "\n";
}

const nudi = [
  "stotra_dashavatara.txt",
  "stotra_raghavendra.txt",
  "stotra_raghavendra_mangala.txt",
  "dasara_hanuma.txt",
  "dasara_madhwa.txt"
];

const toSa = new Set([
  "stotra_dashavatara.txt",
  "stotra_raghavendra.txt",
  "stotra_raghavendra_mangala.txt"
]);

for (const name of nudi) {
  const fp = path.join(texts, name);
  const raw = fs.readFileSync(fp, "latin1");
  let uni = ascii2unicode(raw);
  if (toSa.has(name)) uni = knToSa(uni);
  const out = tidy(uni);
  fs.writeFileSync(fp, out, "utf8");
  const kn = (out.match(/[\u0C80-\u0CFF]/g) || []).length;
  const sa = (out.match(/[\u0900-\u097F]/g) || []).length;
  const nudiLeft = (out.match(/[À-ÿ]/g) || []).length;
  console.log(name, "kn=" + kn, "sa=" + sa, "nudiLeft=" + nudiLeft, "len=" + out.length);
}
