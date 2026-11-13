/**
 * Wire haridasa/*.txt bodies into catalogue/index.json + append manifest rows.
 */
import { readFileSync, writeFileSync, appendFileSync, existsSync, readdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const indexPath = join(root, "app/src/main/assets/catalogue/index.json");
const textsRoot = join(root, "app/src/main/assets/madhwa/texts/haridasa");
const manPath = join(root, "docs/haridasa-import-manifest.csv");
const importedPath = join(root, "tmp/wikisource/imported.json");

const rootJson = JSON.parse(readFileSync(indexPath, "utf8"));
const g = rootJson.groups.find((x) => x.id === "dasara_padagalu");
const byId = Object.fromEntries(g.items.map((i) => [i.id, i]));
const byFile = Object.fromEntries(g.items.filter((i) => i.file).map((i) => [i.file, i]));

const idByHint = {
  "ಆಚಾರವಿಲ್ಲದ ನಾಲಿಗೆ": "dasa_012",
  "ಆದದ್ದೆಲ್ಲ ಒಳಿತೆ ಆಯಿತು": "dasa_014",
  "ಅಂಬೆಗಾಲಿಕ್ಕುತಲಿ": "dasa_026",
  "ಅಂಬಿಗ ನಾ ನಿನ್ನ ನಂಬಿದೆ": "dasa_027",
  "ಭಾಗ್ಯದ ಲಕ್ಷ್ಮೀ ಬಾರಮ್ಮ": "dasa_bhagyada",
  "ಆಡಿಸಿದಳೆಶೋದಾ ಜಗದೋದ್ಧಾರನ": "dasa_jagadodharana",
  "ಕೃಷ್ಣ ನೀ ಬೇಗನೆ ಬಾರೊ": "dasa_krishna_baaro",
};

function deityGuess(title) {
  if (/ಲಕ್ಷ್ಮೀ|ಲಕುಮಿ/u.test(title)) return "Mahalakshmi";
  if (/ರಾಮ/u.test(title)) return "Raama Devaru";
  if (/ಗಣೇಶ|ವಿನಾಯಕ|ಗಜವದನ|ಗಜಾನನ/u.test(title)) return "Ganesha";
  if (/ಕೃಷ್ಣ|ಯಾದವ|ರಂಗ/u.test(title)) return "Krishna";
  if (/ಹನುಮ|ಮುಖ್ಯಪ್ರಾಣ/u.test(title)) return "Hanuma";
  return "Hari / Narayana";
}

function slugId(file) {
  const base = file.split("/").pop().replace(/\.txt$/i, "");
  return `dasa_ws_${base.replace(/[^a-z0-9_]/gi, "_").slice(0, 40)}`;
}

function wikiUrl(title) {
  return `https://kn.wikisource.org/wiki/${encodeURIComponent(title.replace(/ /g, "_"))}`;
}

const imported = existsSync(importedPath)
  ? JSON.parse(readFileSync(importedPath, "utf8")).filter((x) => x.source === "wikisource")
  : [];

// Also discover any on-disk files not in imported.json
function walk(dir, prefix = "") {
  const out = [];
  for (const name of readdirSync(dir, { withFileTypes: true })) {
    if (name.isDirectory()) out.push(...walk(join(dir, name.name), `${prefix}${name.name}/`));
    else if (name.name.endsWith(".txt")) out.push(`${prefix}${name.name}`);
  }
  return out;
}
const onDisk = walk(textsRoot).map((rel) => ({
  title: rel.split("/").pop().replace(/\.txt$/i, "").replace(/_/g, " "),
  file: `haridasa/${rel}`,
  author: rel.startsWith("vyasaraya/") ? "vyasaraya" : "purandara",
  url: "",
  source: "disk",
}));

const byRel = new Map();
for (const row of [...imported.map((r) => ({
  ...r,
  file: r.file.startsWith("haridasa/") ? r.file : `haridasa/${r.file}`,
})), ...onDisk]) {
  byRel.set(row.file, row);
}

let linked = 0;
let added = 0;
const manifestRows = [];
const date = new Date().toISOString().slice(0, 10);

for (const row of byRel.values()) {
  const base = row.file.split("/").pop();
  const title = row.title || base;
  const existingId =
    idByHint[title] ||
    (byFile[row.file] && byFile[row.file].id) ||
    Object.values(byId).find((i) => i.file === row.file)?.id;
  const author = row.author || "purandara";
  const license = "cc-by-sa-wikisource";
  const url = row.url || (row.source === "wikisource" ? wikiUrl(title) : "");

  if (existingId && byId[existingId]) {
    Object.assign(byId[existingId], {
      file: row.file,
      author,
      license,
      script: "kn",
      type: "dasara_pada",
      ...(url ? { url } : {}),
    });
    linked++;
    manifestRows.push({ id: existingId, title: byId[existingId].kn || title, file: row.file, author });
    continue;
  }

  const id = slugId(row.file);
  if (byId[id]) {
    Object.assign(byId[id], { file: row.file, author, license, script: "kn", ...(url ? { url } : {}) });
    linked++;
    manifestRows.push({ id, title: byId[id].kn || title, file: row.file, author });
  } else {
    const item = {
      id,
      kn: title,
      sa: "",
      en: title,
      type: "dasara_pada",
      deity: deityGuess(title),
      author,
      script: "kn",
      file: row.file,
      license,
      url,
    };
    g.items.push(item);
    byId[id] = item;
    byFile[row.file] = item;
    added++;
    manifestRows.push({ id, title, file: row.file, author });
  }
}

writeFileSync(indexPath, `${JSON.stringify(rootJson, null, 2)}\n`, "utf8");

const existingMan = existsSync(manPath) ? readFileSync(manPath, "utf8") : "id,title_kn,author,form,file,license_tier,print_source,permission_ref,verified_date,notes\n";
const existingIds = new Set(
  existingMan
    .split(/\r?\n/)
    .slice(1)
    .map((l) => l.split(",")[0])
    .filter(Boolean)
);
if (!existsSync(manPath)) writeFileSync(manPath, existingMan, "utf8");
let appended = 0;
for (const m of manifestRows) {
  if (existingIds.has(m.id)) continue;
  const line = [
    m.id,
    String(m.title).replace(/,/g, " "),
    m.author,
    "pada",
    m.file,
    "cc-by-sa-wikisource",
    "kn.wikisource.org",
    "CC-BY-SA-4.0",
    date,
    "Phase 1 continued",
  ].join(",");
  appendFileSync(manPath, `${line}\n`);
  existingIds.add(m.id);
  appended++;
}

const withFile = g.items.filter((i) => i.file && String(i.file).trim()).length;
console.log(JSON.stringify({ total: g.items.length, withFile, linked, added, manifestAppended: appended }, null, 2));
