/**
 * Import Kannada / English / Sanskrit Madhwa texts only.
 * Skip Tamil PDFs and pages. Store cleaned .txt under app assets.
 */
import { mkdirSync, writeFileSync, readFileSync, existsSync } from "fs";
import { dirname, join } from "path";
import { fileURLToPath } from "url";
import { createRequire } from "module";

const require = createRequire(import.meta.url);
const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = join(__dirname, "..");
const OUT = join(ROOT, "app", "src", "main", "assets", "madhwa", "texts");
const RAW = join(ROOT, "tools", ".madhwa-raw");

mkdirSync(OUT, { recursive: true });
mkdirSync(RAW, { recursive: true });

const TAMIL = /[\u0B80-\u0BFF]/g;
const KANNADA = /[\u0C80-\u0CFF]/g;
const DEVANAGARI = /[\u0900-\u097F]/g;

function tamilRatio(text) {
  const t = (text.match(TAMIL) || []).length;
  const k = (text.match(KANNADA) || []).length;
  const d = (text.match(DEVANAGARI) || []).length;
  const letters = t + k + d;
  if (letters < 40) return 0;
  return t / letters;
}

function isTamilName(name) {
  return /tamil|\btam\b|_tam_|-tam-|தமிழ்/i.test(name);
}

function tidy(text) {
  return text
    .replace(/\r\n/g, "\n")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .replace(/Share this:[\s\S]*$/i, "")
    .replace(/Published by[\s\S]*$/i, "")
    .replace(/Join \d+ other subscribers[\s\S]*$/i, "")
    .replace(/Sign me up[\s\S]*$/i, "")
    .replace(/Already have a WordPress[\s\S]*$/i, "")
    .replace(/Search for:[\s\S]*$/i, "")
    .replace(/Follow Blog via Email[\s\S]*$/i, "")
    .replace(/Privacy & Cookies:[\s\S]*$/i, "")
    .replace(/Like Loading\.\.\./g, "")
    .trim();
}

function htmlToText(html) {
  let h = html;
  const article =
    h.match(/<article[\s\S]*?<\/article>/i)?.[0] ||
    h.match(/<div[^>]+class="[^"]*entry-content[^"]*"[\s\S]*?<\/div>/i)?.[0] ||
    h;
  h = article
    .replace(/<script[\s\S]*?<\/script>/gi, " ")
    .replace(/<style[\s\S]*?<\/style>/gi, " ")
    .replace(/<nav[\s\S]*?<\/nav>/gi, " ")
    .replace(/<footer[\s\S]*?<\/footer>/gi, " ")
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/p>/gi, "\n\n")
    .replace(/<\/h[1-6]>/gi, "\n\n")
    .replace(/<\/(div|li|tr)>/gi, "\n")
    .replace(/<[^>]+>/g, " ")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(Number(n)))
    .replace(/&[a-z]+;/gi, " ");
  return tidy(h.replace(/[ \t]{2,}/g, " "));
}

async function get(url) {
  const res = await fetch(url, {
    headers: {
      "User-Agent": "PaperDeskMadhwaImport/1.0 (local catalog; KN/EN/SA only)",
      Accept: "text/html,application/pdf,*/*",
    },
    redirect: "follow",
  });
  if (!res.ok) throw new Error(`${res.status} ${url}`);
  return res;
}

async function savePdfText(url, id, pdfParse) {
  const name = url.split("/").pop() || id;
  if (isTamilName(name) || isTamilName(id)) {
    console.log("skip tamil name", name);
    return null;
  }
  const dest = join(RAW, name);
  let buf;
  if (existsSync(dest) && require("fs").statSync(dest).size > 1000) {
    buf = readFileSync(dest);
  } else {
    const res = await get(url);
    buf = Buffer.from(await res.arrayBuffer());
    writeFileSync(dest, buf);
  }
  const parsed = await pdfParse(buf);
  const text = tidy(parsed.text || "");
  if (tamilRatio(text) > 0.12) {
    console.log("skip tamil body", name, tamilRatio(text).toFixed(2));
    return null;
  }
  const kn = (text.match(KANNADA) || []).length;
  const sa = (text.match(DEVANAGARI) || []).length;
  const latin = (text.match(/[A-Za-z]/g) || []).length;
  if (kn + sa + latin < 80) {
    console.log("skip empty/scan", name);
    return null;
  }
  const out = join(OUT, `${id}.txt`);
  writeFileSync(out, text, "utf8");
  console.log("pdf", id, text.length, "kn", kn, "sa", sa);
  return { id, file: `${id}.txt`, chars: text.length };
}

async function saveHtml(url, id) {
  const res = await get(url);
  const html = await res.text();
  if (isTamilName(url) || /tamil pdf/i.test(html.slice(0, 4000))) {
    const title = html.match(/<title>([^<]+)/i)?.[1] || "";
    if (/tamil/i.test(title)) {
      console.log("skip tamil html", url);
      return null;
    }
  }
  const text = htmlToText(html);
  if (tamilRatio(text) > 0.12) {
    console.log("skip tamil html body", id);
    return null;
  }
  writeFileSync(join(OUT, `${id}.txt`), text, "utf8");
  console.log("html", id, text.length);
  return { id, file: `${id}.txt`, chars: text.length };
}

const PDFS = [
  ["hks", "https://madhwafestivals.com/wp-content/uploads/2017/03/hks.pdf"],
  ["anu_hks", "https://madhwafestivals.com/wp-content/uploads/2016/09/anu-hari-kathamruta-saara-docx.pdf"],
  ["dasara_madhwa", "https://madhwafestivals.com/wp-content/uploads/2016/12/madhwa.pdf"],
  ["dasara_hanuma", "https://madhwafestivals.com/wp-content/uploads/2016/12/hanuma.pdf"],
  ["suladhi_pancharatna", "https://madhwafestivals.com/wp-content/uploads/2016/10/pancharatna-suladhigalu.pdf"],
  ["suladhi_vivaha", "https://madhwafestivals.com/wp-content/uploads/2016/11/vivaha-suladhi-kannada1.pdf"],
  ["suladhi_hasthodhaka", "https://madhwafestivals.com/wp-content/uploads/2017/11/hasthodhaka-suladhi.pdf"],
  ["stotra_raghavendra", "https://madhwafestivals.com/wp-content/uploads/2016/08/raghavendra-stotram.pdf"],
  ["stotra_raghavendra_mangala", "https://madhwafestivals.com/wp-content/uploads/2016/12/raghavendra-mangalashtakam.pdf"],
  ["stotra_dashavatara", "https://madhwafestivals.com/wp-content/uploads/2017/01/dashavathara-stothra.pdf"],
  ["puja_udupi_krishna", "https://madhwafestivals.com/wp-content/uploads/2017/10/udupi-krishna-pooja-haadu-english.pdf"],
];

const PAGES = [
  ["hks_index", "https://madhwafestivals.com/2017/03/16/harikathamruta-saara/"],
  ["hks_vigneshwara", "https://madhwafestivals.com/2016/09/03/vigneshwara-sthothra-sandhi-hari-kathamruta-saara/"],
  ["stotra_yantrodharaka_page", "https://madhwafestivals.com/2016/08/03/yantrodharaka-hanuman-stothram/"],
  ["dasara_jagadodharana", "https://madhwafestivals.com/2016/08/16/1190/"],
  ["dasara_bhagyada_lakshmi", "https://madhwafestivals.com/2016/08/13/bhagyada-lakshmi-baramma/"],
  ["kesava_nama_page", "https://madhwafestivals.com/2016/08/18/kesava-nama/"],
  ["ugabhoga_favorite", "https://madhwafestivals.com/2017/06/11/my-favorite-ugabhoga-collection/"],
  ["ugabhoga_sripadaraja", "https://madhwafestivals.com/2016/11/30/ugabhoga-by-sripadarajaru/"],
  ["ugabhoga_index", "https://madhwafestivals.com/2018/05/16/index-ughabhoga-by-various-hari-dasaru/"],
  ["suladhi_index", "https://madhwafestivals.com/2017/03/21/index-sulaadhi/"],
  ["suladhi_madhwa_matha", "https://madhwafestivals.com/2016/09/23/sri-man-madhwa-matha-mahima-sulaadhi/"],
  ["suladhi_nrisimha", "https://madhwafestivals.com/2016/09/29/sri-narasimha-devara-suladi/"],
  ["suladhi_durga", "https://madhwafestivals.com/2016/09/19/durga-suladhi/"],
  ["suladhi_raghavendra", "https://madhwafestivals.com/2016/09/30/raghavendra-sulaadhi/"],
  ["suladhi_vayu_traya", "https://madhwafestivals.com/2016/10/13/vayudevara-avathara-traya-suladhi-by-sri-raghavendra-theertharu/"],
  ["suladhi_madhwacharya", "https://madhwafestivals.com/2017/03/26/madhwacharyaru-suladhiby-purandara-dasaru/"],
  ["suladhi_hanumanta", "https://madhwafestivals.com/2017/09/07/hanumanta-suladhipurandara-dasaru/"],
  ["puja_mahalakshmi_padagalu", "https://madhwafestivals.com/2019/04/29/mahalakshmi-pooja-through-dasara-padagalu/"],
  ["dasara_index", "https://madhwafestivals.com/2017/04/15/index-dasara-padagalu-lyrics/"],
];

const TAMIL_SKIP = [
  "https://madhwafestivals.com/wp-content/uploads/2025/05/sri-vijayadasara-25-suladigalu-book.pdf",
  "https://madhwafestivals.com/wp-content/uploads/2026/05/kesava-nama-print-format-book.pdf",
  "https://madhwafestivals.com/wp-content/uploads/2025/06/pratha-sankalpa-gadhya-tamil-book.pdf",
  "https://madhwafestivals.com/wp-content/uploads/2025/05/haridasa-hrudaya-book-part-1-1.pdf",
  "https://madhwafestivals.com/wp-content/uploads/2025/05/dasar-songs-compile-2.0.pdf",
];

const result = { pdfs: [], pages: [], skippedTamil: TAMIL_SKIP };

try {
  const pdfParse = (await import("pdf-parse")).default;
  for (const [id, url] of PDFS) {
    try {
      const row = await savePdfText(url, id, pdfParse);
      if (row) result.pdfs.push(row);
    } catch (e) {
      console.log("pdf fail", id, e.message);
    }
  }
} catch (e) {
  console.log("pdf-parse missing", e.message);
}

for (const [id, url] of PAGES) {
  try {
    const row = await saveHtml(url, id);
    if (row) result.pages.push(row);
  } catch (e) {
    console.log("html fail", id, e.message);
  }
}

writeFileSync(join(RAW, "import-log.json"), JSON.stringify(result, null, 2));
console.log("done", JSON.stringify(result, null, 2));
