import { existsSync, readFileSync, readdirSync, unlinkSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = join(dirname(fileURLToPath(import.meta.url)), "..");
const assets = join(projectRoot, "app", "src", "main", "assets");
const refreshOnline = process.argv.includes("--refresh-online");

const exclusions = {
  namavali: new Set(["nAmA-print"]),
  puja: new Set([
    "kartika-mahatmyam",
    "pujavidhanam",
    "KarttikaMasaMahatmyam-transformed",
    "bhagavatam-07-08-orig",
    "bhagavatam-07-09-orig",
    "tulasi-puja-old",
  ]),
  stotra: new Set(["AhalyakrtaRamaStotram_rama", "Ramopadesha"]),
};

const titleOverrides = {
  mantras: {
    MrtyunjayaJapaMantrah: "मृत्युञ्जयजप-मन्त्राः",
  },
  namavali: {
    Anjaneya_Ramarahasya_108: "रामरहस्योक्त-हनुमदष्टोत्तरशतनामावलिः",
    SundaraHanuman_27: "सुन्दरहनुमत्सप्तविंशतिनामावलिः",
    Ganesha_108: "गणेशाष्टोत्तरशतनामावलिः — पाठान्तरम्",
    Ganga_108_2: "श्रीगङ्गाष्टोत्तरशतनामावलिः — पाठान्तरम्",
    Nrisimha_108: "नृसिंहाष्टोत्तरशतनामावलिः",
    Rama_Ramarahasya_108: "रामरहस्योक्त-रामाष्टोत्तरशतनामावलिः",
    Sita_Ramarahasya_108: "रामरहस्योक्त-सीताष्टोत्तरशतनामावलिः",
    Vishnu_1000_annotated: "विष्णुसहस्रनामावलिः — सटिप्पणी पाठः",
  },
  puja: {
    "37-unmilani": "उन्मीलनीव्रतम् — सप्तत्रिंशोऽध्यायः",
    "ekadashi-mahatmyam-padma-puranam": "एकादशीमाहात्म्यम् — उन्मीलनीव्रतम्",
    "somavara-vrata-katha": "सोमवारव्रतकथा",
    "sriramanavami-vrata-katha": "श्रीरामनवमीव्रतकथा",
  },
  stotra: {
    "BhajaGovindam-laghu": "भज गोविन्दम् — लघुपाठः",
    VatapuranathaAshtakam: "श्रीवातपुरनाथाष्टकम्",
    SkandaShashthiKavacham: "स्कन्दषष्ठिकवचम्",
  },
};

const metadataOnly = /^(?:preamble|frontmatter|mainmatter|backmatter|document|individual|individualfalse|individualtrue|katha|kathafalse|kathatrue|kindle|kindlefalse|kindletrue|print|printfalse|printtrue|veda|vedafalse|vedatrue|pujas|nAmAvalis|wallpaper|shloka|enumerate|center|flushleft|flushright|autocols?\[?\]?|multicols\d*|totalshlokas|\[?\d+\]?(?:book|geometry)|\d+(?:\.\d+)?(?:pt|ex|em|cm|mm)|-?\d+(?:\.\d+)?(?:pt|ex|em|cm|mm))$/i;
const attribution = /^(?:encoded|proofread|transliterated|translation|latest update|send corrections|site access|file name|text title|itxtitle|engtitle|category|location|sublocation|subdeity|language|subject|description-comments|indexextra)\b/i;

function decodeHtml(value) {
  return value
    .replace(/<h2[^>]*>.*?<\/h2>/gis, "")
    .replace(/<[^>]+>/g, "")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">");
}

function cleanBody(raw) {
  let text = raw.normalize("NFC").replace(/\r\n?/g, "\n").replace(/\u00a0/g, " ");
  text = text
    .replace(/[\u0B80-\u0BFF]+/gu, "")
    .replace(/\[Script=[^\]]*\]\S*/gi, "")
    .replace(/\bsec:(?:start_)?[\w.-]+\b/gi, "")
    .replace(/\b(?:shlokacount[\w-]*|multicols\d*|flushleft|flushright|centerline|devanumber\d*|addtocounter|AutoCols?\[?\]?|Arial Unicode MS)\b/gi, "")
    .replace(/\btotalshlokas[\w-]*\b/gi, "")
    .replace(/\]?hyperref\b/gi, "")
    .replace(/\[anuvakam\]/gi, "")
    .replace(/(?:^|(?<=\D))-?\d+(?:\.\d+)?pt\b/gi, "")
    .replace(/\[\d+\]/g, "")
    .replace(/#\d+\b/g, "");

  const output = [];
  const indicSource = /[\u0900-\u097F\u0C80-\u0CFF]/u.test(text);
  for (const original of text.split("\n")) {
    let line = original.replace(/[ \t]+/g, " ").trim();
    if (!line) {
      if (output.length && output.at(-1) !== "") output.push("");
      continue;
    }
    if (metadataOnly.test(line) || attribution.test(line)) continue;
    if (/^(?:https?:\/\/|www\.)\S+$/i.test(line)) continue;
    if (/^[\w./\\-]+\.tex$/i.test(line)) continue;
    if (/^\[[^\]]*\](?:book|geometry)$/i.test(line)) continue;
    if (/^[-=]?\d+(?:\.\d+)?(?:pt|ex|em|cm|mm)$/i.test(line)) continue;
    if (indicSource && /[A-Za-z\u00C0-\u024F]/u.test(line)) {
      const lineHasIndic = /[\u0900-\u097F\u0C80-\u0CFF]/u.test(line);
      if (!lineHasIndic) continue;
      line = line.replace(/[A-Za-z\u00C0-\u024F][A-Za-z0-9\u00C0-\u024F_'’.-]*/gu, " ");
    }
    line = line.replace(/[ \t]+/g, " ").trim();
    if (!line || /^[\p{P}\p{S}\s=]+$/u.test(line) || line === output.at(-1)) continue;
    output.push(line);
  }
  return output.join("\n").replace(/\n{3,}/g, "\n\n").trim() + "\n";
}

async function refreshSkanda() {
  if (!refreshOnline) return;
  const url = "https://sanskritdocuments.org/doc_subrahmanya/skanda-6-kavacha.html";
  const response = await fetch(url);
  if (!response.ok) throw new Error(`Unable to fetch ${url}: HTTP ${response.status}`);
  const html = await response.text();
  const match = html.match(/<PRE[^>]*id="content"[^>]*>([\s\S]*?)<\/PRE>/i);
  if (!match) throw new Error("Skanda source did not contain the expected text block");
  let body = decodeHtml(match[1]);
  const start = body.indexOf("स्कन्द षष्ठि कवचं");
  if (start < 0) throw new Error("Skanda source title was not found");
  body = body.slice(start).replace(/\nTransliterated by[\s\S]*$/i, "");
  writeFileSync(join(assets, "library", "stotra", "SkandaShashthiKavacham.txt"), cleanBody(body), "utf8");
}

function cleanLibrary(collection) {
  const dir = join(assets, "library", collection);
  const indexPath = join(dir, "index.json");
  const index = JSON.parse(readFileSync(indexPath, "utf8"));
  const removed = [];
  const kept = [];
  for (const item of index.items || []) {
    const path = join(dir, `${item.id}.txt`);
    if (exclusions[collection]?.has(item.id)) {
      if (existsSync(path)) unlinkSync(path);
      removed.push(item.id);
      continue;
    }
    if (!existsSync(path)) {
      removed.push(`${item.id} (missing)`);
      continue;
    }
    const body = cleanBody(readFileSync(path, "utf8"));
    writeFileSync(path, body, "utf8");
    item.title = titleOverrides[collection]?.[item.id] || item.title;
    item.preview = body.split(/\s+/).slice(0, 18).join(" ");
    if (collection === "stotra" && item.id === "SkandaShashthiKavacham") {
      item.source = "https://sanskritdocuments.org/doc_subrahmanya/skanda-6-kavacha.html";
      item.language = "Tamil transliterated into Devanagari; rendered in Kannada by AcharyaOS";
    }
    kept.push(item);
  }
  index.items = kept;
  index.audit = "Sanitized by tools/sanitize-catalogue.mjs";
  writeFileSync(indexPath, `${JSON.stringify(index, null, 2)}\n`, "utf8");
  return { collection, kept: kept.length, removed };
}

function cleanMadhwa() {
  const path = join(assets, "madhwa", "catalog.json");
  const index = JSON.parse(readFileSync(path, "utf8"));
  const excluded = new Set([
    "dasara_index",
    "suladhi_index",
    "haridasa_padagalu",
    "haridasa_ugabhoga",
    "haridasa_suladhi",
    "puja_mahalakshmi",
  ]);
  const removedFiles = new Set();
  const kept = (index.items || []).filter((item) => {
    if (String(item.file || "").startsWith("lib:")) return false;
    if (!excluded.has(item.id)) return true;
    if (item.file && !item.file.startsWith("lib:")) removedFiles.add(item.file);
    return false;
  });
  const cleanedFiles = new Set();
  for (const item of kept) {
    if (String(item.file || "").startsWith("lib:") || !item.file || cleanedFiles.has(item.file)) continue;
    const bodyPath = join(assets, "madhwa", "texts", item.file);
    if (existsSync(bodyPath)) writeFileSync(bodyPath, cleanBody(readFileSync(bodyPath, "utf8")), "utf8");
    cleanedFiles.add(item.file);
  }
  for (const item of kept) {
    if (item.author === "Jagannatha Dasa") item.author = "जगन्नाथदासः";
    if (item.note && /[A-Za-z]/.test(item.note) && !/[\u0900-\u097F\u0C80-\u0CFF]/u.test(item.note)) item.note = "";
  }
  const navigation = JSON.parse(readFileSync(join(assets, "catalogue", "index.json"), "utf8"));
  const directReferences = new Set(kept.map((item) => item.file).filter((file) => file && !file.startsWith("lib:")));
  for (const group of navigation.groups || []) {
    for (const item of group.items || []) {
      if (item.file && !item.file.startsWith("lib:")) directReferences.add(item.file.split("/").at(-1));
    }
  }
  const textRoot = join(assets, "madhwa", "texts");
  for (const file of readdirSync(textRoot).filter((name) => name.endsWith(".txt"))) {
    const bodyPath = join(textRoot, file);
    if (!directReferences.has(file)) {
      unlinkSync(bodyPath);
      continue;
    }
    const body = cleanBody(readFileSync(bodyPath, "utf8"));
    writeFileSync(bodyPath, body, "utf8");
  }
  const retainedFiles = new Set(kept.map((item) => item.file));
  for (const file of removedFiles) {
    if (retainedFiles.has(file)) continue;
    const filePath = join(assets, "madhwa", "texts", file);
    if (existsSync(filePath)) unlinkSync(filePath);
  }
  index.items = kept;
  index.audit = "Duplicate aliases and non-Kannada blog indexes removed";
  writeFileSync(path, `${JSON.stringify(index, null, 2)}\n`, "utf8");
  return { collection: "madhwa", kept: kept.length, removed: [...excluded] };
}

await refreshSkanda();
const results = ["mantras", "namavali", "puja", "stotra"].map(cleanLibrary);
results.push(cleanMadhwa());
for (const result of results) {
  console.log(`${result.collection}: ${result.kept} kept; removed ${result.removed.join(", ") || "none"}`);
}
