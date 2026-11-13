/**
 * Split kn.wikisource.org ಕನಕದಾಸರ ಸಾಹಿತ್ಯ === sections into lyric bodies.
 * License: CC BY-SA 4.0 (same as other Wikisource imports).
 */
import { mkdirSync, readFileSync, writeFileSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const src = join(root, "tmp/wikisource/ಕನಕದಾಸರ ಸಾಹಿತ್ಯ.wiki.txt");
const outDir = join(root, "app/src/main/assets/madhwa/texts/haridasa/kanaka");
const importedPath = join(root, "tmp/wikisource/imported.json");

if (!existsSync(src)) {
  console.error("missing", src);
  process.exit(1);
}

mkdirSync(outDir, { recursive: true });

function slug(title) {
  const map = {
    ಅಂಧಂತಮಸು: "andhantamasu",
    ಅಂಬುಜದಳಾಕ್ಷಗೆ: "ambujadalakshage",
    ಅಜ್ಞಾನಿಗಳ: "ajnani_kanaka",
    ಆನೆ: "ane",
    ಏನು: "yenu",
    ಬಾಗಿಲನು: "bagilanu",
    ತಲ್ಲಣ: "tallana",
    ನಮ್ಮಮ್ಮ: "nammamma",
  };
  for (const [k, v] of Object.entries(map)) {
    if (title.includes(k)) return v + "_" + title.length;
  }
  // transliterate-ish: keep only ASCII-ish via codepoints hash + first words length
  const ascii = title
    .normalize("NFC")
    .replace(/[^\u0C80-\u0CFF\w]+/gu, "_")
    .replace(/^_|_$/g, "")
    .slice(0, 48);
  // Prefer readable latin slug from known first lines
  const known = {
    "ಅಂಧಂತಮಸು ಇನ್ನಾರಿಗೆ": "andhantamasu_innarige",
    "ಅಂಬುಜದಳಾಕ್ಷಗೆ": "ambujadalakshage_mangala",
    "ಅಜ್ಞಾನಿಗಳ ಕೂಡ ಅಧಿಕ": "ajnani_kuda_adhika_kanaka",
    "ಆನೆ ಬಂದಿದೆ ಇದಿಗೊ": "ane_bandide_idigo",
    "ಏನು ಕಾರಣ ಬಾಯಿ ತೆರೆದಿ": "yenu_kaarana_baayi",
  };
  if (known[title]) return known[title];
  // fallback: sequential will be set by caller
  return `kanaka_${Buffer.from(title, "utf8").toString("hex").slice(0, 16)}`;
}

function cleanBody(raw) {
  let t = raw.normalize("NFC").replace(/\r\n?/g, "\n");
  t = t.replace(/\{\{[^}]*\}\}/gs, "");
  t = t.replace(/\[\[[^\]]*\|([^\]]+)\]\]/g, "$1");
  t = t.replace(/\[\[([^\]]+)\]\]/g, "$1");
  t = t.replace(/\[https?:\/\/[^\]]+\]/gi, "");
  t = t.replace(/<ref[^>]*>.*?<\/ref>/gis, "");
  t = t.replace(/<ref[^>]*\/>/gi, "");
  t = t.replace(/'{2,}/g, "");
  t = t.replace(/<\/?br\s*\/?>/gi, "\n");
  t = t.replace(/<\/?[^>]+>/g, "");
  t = t.replace(/\\\\ಪ\/\//g, "||ಪ||");
  t = t.replace(/\\\\ಅ\/\//g, "||ಅ||");
  // Insert line breaks before trailing verse numbers stuck to previous text
  t = t.replace(/([^\s\d])(\d{1,2})(?=\s|$)/gu, "$1 ||$2||");
  const lines = [];
  for (const original of t.split("\n")) {
    let line = original.replace(/[ \t]+/g, " ").trim();
    if (!line) {
      if (lines.length && lines[lines.length - 1] !== "") lines.push("");
      continue;
    }
    if (/^(ರಚನೆ|ಸಾಹಿತ್ಯ|ರಾಗ|ತಾಳ|ವರ್ಗ)\s*:/u.test(line)) continue;
    lines.push(line);
  }
  return lines.join("\n").replace(/\n{3,}/g, "\n\n").trim() + "\n";
}

const text = readFileSync(src, "utf8");
const padStart = text.search(/^===\s*ಅಂಧಂತಮಸು/mu);
const body = padStart >= 0 ? text.slice(padStart) : text;
const parts = body.split(/^===\s*(.+?)\s*===\s*$/mu);
// parts: [preamble, title1, body1, title2, body2, ...]
const imported = existsSync(importedPath) ? JSON.parse(readFileSync(importedPath, "utf8")) : [];
const seen = new Set(imported.filter((x) => x.file?.includes("/kanaka/")).map((x) => x.file));
let written = 0;
const rows = [];

for (let i = 1; i + 1 < parts.length; i += 2) {
  const title = parts[i].replace(/\[\[|\]\]/g, "").trim();
  if (!title || title.length > 80) continue;
  if (/^(ವಿಕಿಪೀಡಿಯ|ಪದ್ಯಗಳು|ಕನಕದಾಸ)/u.test(title)) continue;
  const lyric = cleanBody(parts[i + 1] || "");
  // stop at next major section markers leftover
  const cut = lyric.search(/\n==[^=]+\n/);
  const bodyText = (cut > 0 ? lyric.slice(0, cut) : lyric).trim() + "\n";
  if (bodyText.replace(/\s/g, "").length < 60) continue;
  const fileSlug = slug(title);
  const rel = `kanaka/${fileSlug}.txt`;
  const abs = join(outDir, `${fileSlug}.txt`);
  writeFileSync(abs, bodyText, "utf8");
  const file = `haridasa/${rel}`;
  // Prefer Wikisource title over generic disk slug titles when patching
  const existing = imported.find((x) => x.file === rel || x.file === file.replace(/^haridasa\//, ""));
  if (existing) {
    existing.title = title;
    existing.author = "kanaka";
    existing.source = "wikisource";
  } else if (!seen.has(file)) {
    imported.push({
      title,
      file: rel,
      url: "https://kn.wikisource.org/wiki/%E0%B2%95%E0%B2%A8%E0%B2%95%E0%B2%A6%E0%B2%BE%E0%B2%B8%E0%B2%B0_%E0%B2%B8%E0%B2%BE%E0%B2%B9%E0%B2%BF%E0%B2%A4%E0%B3%8D%E0%B2%AF",
      author: "kanaka",
      source: "wikisource",
    });
    seen.add(file);
  }
  rows.push({ title, file: rel, chars: bodyText.length });
  written++;
}

writeFileSync(importedPath, JSON.stringify(imported, null, 2), "utf8");
console.log(JSON.stringify({ written, sample: rows.slice(0, 8), total: rows.length }, null, 2));
