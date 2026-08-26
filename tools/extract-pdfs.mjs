import { mkdirSync, writeFileSync, readFileSync, existsSync } from "fs";
import { dirname, join } from "path";
import { fileURLToPath } from "url";
import { PDFParse } from "pdf-parse";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
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
  return /tamil|\btam\b|_tam_|-tam-/i.test(name);
}

function tidy(text) {
  return String(text || "")
    .replace(/\r\n/g, "\n")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

async function getBuf(url, name) {
  const dest = join(RAW, name);
  if (existsSync(dest) && dest.length) {
    const st = readFileSync(dest);
    if (st.length > 1000) return st;
  }
  const res = await fetch(url, {
    headers: { "User-Agent": "PaperDeskMadhwaImport/1.0" },
    redirect: "follow",
  });
  if (!res.ok) throw new Error(`${res.status} ${url}`);
  const buf = Buffer.from(await res.arrayBuffer());
  writeFileSync(dest, buf);
  return buf;
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

const MORE_HTML = [
  ["ugabhoga_vyasaraja", "https://madhwafestivals.com/2016/12/01/ugabhoga-by-vyasarajaru/"],
  ["ugabhoga_purandara_1", "https://madhwafestivals.com/2016/12/01/ugabhoga-by-purandara-dasarupart-1/"],
  ["ugabhoga_purandara_2", "https://madhwafestivals.com/2016/12/02/ugabhoga-by-purandara-dasarupart-2/"],
  ["ugabhoga_vijaya", "https://madhwafestivals.com/2016/12/02/ugabhogagalu-by-vijaya-dasaru/"],
  ["ugabhoga_gopala", "https://madhwafestivals.com/2016/12/03/ugabhoga-by-sri-gopala-dasaru/"],
  ["ugabhoga_jagannatha", "https://madhwafestivals.com/2016/12/03/ugabhogagalu-by-sri-jagannatha-dasaru/"],
  ["dasara_alli_nodalu", "https://madhwafestivals.com/2016/08/16/alli-nodalu-raama/"],
  ["dasara_anjikinyataka", "https://madhwafestivals.com/2016/08/16/anjikinyatakayya-sajjanarige/"],
  ["dasara_kande_govinda", "https://madhwafestivals.com/2016/08/18/kande-na-govindana/"],
  ["hks_mangala", "https://madhwafestivals.com/2016/09/01/mangala-charana-sandhi/"],
  ["hks_karuna", "https://madhwafestivals.com/2016/09/02/karuna-sandhi/"],
];

function htmlToText(html) {
  const article =
    html.match(/<article[\s\S]*?<\/article>/i)?.[0] ||
    html.match(/<div[^>]+class="[^"]*entry-content[^"]*"[\s\S]*?<\/div>/i)?.[0] ||
    html;
  return tidy(
    article
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
      .replace(/&[a-z]+;/gi, " ")
      .replace(/[ \t]{2,}/g, " ")
  )
    .replace(/Share this:[\s\S]*$/i, "")
    .replace(/Published by[\s\S]*$/i, "")
    .trim();
}

for (const [id, url] of PDFS) {
  const name = url.split("/").pop();
  if (isTamilName(name) || isTamilName(id)) {
    console.log("skip tamil", name);
    continue;
  }
  try {
    const buf = await getBuf(url, name);
    const parser = new PDFParse({ data: buf });
    const result = await parser.getText();
    await parser.destroy();
    const text = tidy(result?.text || "");
    if (tamilRatio(text) > 0.12) {
      console.log("skip tamil body", id, tamilRatio(text).toFixed(2));
      continue;
    }
    const kn = (text.match(KANNADA) || []).length;
    const sa = (text.match(DEVANAGARI) || []).length;
    const latin = (text.match(/[A-Za-z]/g) || []).length;
    console.log("pdf", id, "len", text.length, "kn", kn, "sa", sa, "latin", latin);
    if (kn + sa + latin < 80) {
      console.log("skip weak", id);
      continue;
    }
    writeFileSync(join(OUT, `${id}.txt`), text, "utf8");
  } catch (e) {
    console.log("pdf fail", id, e.message);
  }
}

for (const [id, url] of MORE_HTML) {
  try {
    const res = await fetch(url, { headers: { "User-Agent": "PaperDeskMadhwaImport/1.0" }, redirect: "follow" });
    if (!res.ok) {
      console.log("html miss", id, res.status);
      continue;
    }
    const html = await res.text();
    if (/<title>[^<]*tamil/i.test(html)) {
      console.log("skip tamil html", id);
      continue;
    }
    const text = htmlToText(html);
    if (tamilRatio(text) > 0.12) {
      console.log("skip tamil html body", id);
      continue;
    }
    if (text.length < 200) {
      console.log("html short", id, text.length);
      continue;
    }
    writeFileSync(join(OUT, `${id}.txt`), text, "utf8");
    console.log("html", id, text.length);
  } catch (e) {
    console.log("html fail", id, e.message);
  }
}
