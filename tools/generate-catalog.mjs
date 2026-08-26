import { readFileSync, writeFileSync, mkdirSync } from "fs";
import { dirname, join } from "path";
import { fileURLToPath } from "url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const TEXTS = join(ROOT, "app", "src", "main", "assets", "madhwa", "texts");
const CATALOG = join(ROOT, "app", "src", "main", "assets", "madhwa", "catalog.json");

const SANDHI = [
  [1, "मङ्गलाचरण सन्धिः", "जगन्नाथदासः"],
  [2, "करुणा सन्धिः", "जगन्नाथदासः"],
  [3, "व्याप्ति सन्धिः", "Jagannatha Dasa"],
  [4, "भोजन सन्धिः", "Jagannatha Dasa"],
  [5, "विभूति सन्धिः", "Jagannatha Dasa"],
  [6, "पञ्चमहायज्ञ सन्धिः", "Jagannatha Dasa"],
  [7, "पञ्चतन्मात्र सन्धिः", "Jagannatha Dasa"],
  [8, "मातृका सन्धिः", "Jagannatha Dasa"],
  [9, "वर्णप्रक्रिया सन्धिः", "Jagannatha Dasa"],
  [10, "सर्वप्रतीक सन्धिः", "Jagannatha Dasa"],
  [11, "स्थावरजङ्गम सन्धिः", "Jagannatha Dasa"],
  [12, "नाडीप्रकरण सन्धिः", "Jagannatha Dasa"],
  [13, "नामस्मरण सन्धिः", "Jagannatha Dasa"],
  [14, "पितृगण सन्धिः", "Jagannatha Dasa"],
  [15, "श्वास सन्धिः", "Jagannatha Dasa"],
  [16, "दत्तस्वातन्त्र्य सन्धिः", "Jagannatha Dasa"],
  [17, "स्वगतस्वातन्त्र्य सन्धिः", "Jagannatha Dasa"],
  [18, "क्रीडाविलास सन्धिः", "Jagannatha Dasa"],
  [19, "बिम्बापरोक्ष सन्धिः", "Jagannatha Dasa"],
  [20, "गुणतारतम्य सन्धिः", "Jagannatha Dasa"],
  [21, "कर्मविमोचन सन्धिः", "Jagannatha Dasa"],
  [22, "भक्तापराधसहिष्णु सन्धिः", "Jagannatha Dasa"],
  [23, "बृहत्तारतम्य सन्धिः", "Jagannatha Dasa"],
  [24, "कल्पसाधन सन्धिः", "Jagannatha Dasa"],
  [25, "आरोहण तारतम्य सन्धिः", "Jagannatha Dasa"],
  [26, "अवरोहण तारतम्य सन्धिः", "Jagannatha Dasa"],
  [27, "अनुक्रमणिका तारतम्य सन्धिः", "Jagannatha Dasa"],
  [28, "विघ्नेश्वर स्तोत्र सन्धिः", "Jagannatha Dasa"],
  [29, "अणुतारतम्य सन्धिः", "Jagannatha Dasa"],
  [30, "दैत्यतारतम्य सन्धिः", "Jagannatha Dasa"],
  [31, "नैवेद्यप्रकरण सन्धिः", "Jagannatha Dasa"],
  [32, "कक्षातारतम्य सन्धिः", "Jagannatha Dasa"],
  [33, "फलश्रुति सन्धिः", "Jagannatha Dasa"],
];

function cleanPdf(text) {
  return text
    .replace(/\r\n/g, "\n")
    .replace(/^HariKathaamrutaSaara\s*$/gm, "")
    .replace(/^https:\/\/madhwafestivals[^\n]*$/gm, "")
    .replace(/^-- \d+ of \d+ --\s*$/gm, "")
    .replace(/^Page \d+\s*$/gm, "")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

const raw = readFileSync(join(TEXTS, "hks.txt"), "utf8");
const cleaned = cleanPdf(raw);
writeFileSync(join(TEXTS, "hks.txt"), cleaned, "utf8");

const lines = cleaned.split("\n");
const starts = [];
for (let i = 0; i < lines.length; i++) {
  const m = lines[i].match(/^(\d{1,2})\.\s+/);
  if (m) {
    const n = Number(m[1]);
    if (n >= 1 && n <= 33) starts.push({ n, i });
  }
}
const byN = new Map();
for (const row of starts) {
  if (!byN.has(row.n)) byN.set(row.n, row.i);
}
for (const [n, title] of SANDHI) {
  const i = byN.get(n);
  if (i == null) continue;
  const next = SANDHI.find(([m]) => m > n && byN.has(m));
  const end = next ? byN.get(next[0]) : lines.length;
  const body = lines.slice(i, end).join("\n").trim();
  const id = `hks_${String(n).padStart(2, "0")}`;
  writeFileSync(join(TEXTS, `${id}.txt`), `${title}\n\n${body}\n`, "utf8");
}

function item(id, category, title, author, note, file) {
  return { id, category, title, author, note, file };
}

const items = [
  item("sampradaya", "sampradaya", "माध्व सम्प्रदायः", "मध्वाचार्यः", "Hari Sarvottama. Vayu Jivottama. Tattvavada.", "sampradaya.txt"),
  item("hks_full", "sampradaya", "श्री हरिकथामृतसारः", "जगन्नाथदासः", "Kannada moola from HKS PDF. 33 sandhis.", "hks.txt"),
  item("hks_vigneshwara", "sampradaya", "विघ्नेश्वर स्तोत्र सन्धिः", "जगन्नाथदासः", "Hari Kathamruta Saara. Vigneshwara sandhi. Kannada and English.", "hks_vigneshwara.txt"),
  item("anu_hks", "sampradaya", "अणु हरिकथामृतसारः", "जगन्नाथदासः", "Anu Hari Kathamruta Saara. Kannada.", "anu_hks.txt"),
  item("hks_index", "sampradaya", "हरिकथामृतसार सन्धि सूची", "जगन्नाथदासः", "Sandhi list. Kannada and English.", "hks_index.txt"),

  item("hari_vayu_stuti", "stotra", "श्री हरिवायुस्तुतिः", "त्रिविक्रम पण्डिताचार्यः", "Hari Vayu Stuti with Nakha Stuti.", "hari_vayu_stuti.txt"),
  item("nakha_stuti", "stotra", "नखस्तुतिः", "मध्वाचार्यः", "Narasimha Nakha Stuti.", "nakha_stuti.txt"),
  item("dvadasha_stotra", "stotra", "द्वादशस्तोत्रम्", "मध्वाचार्यः", "Dvadasha Stotra. First stotra.", "dvadasha_stotra.txt"),
  item("yantrodharaka", "stotra", "यन्त्रोद्धारक हनूमत्स्तोत्रम्", "व्यासराजः", "Yantrodharaka Hanuman Stotram. Local patha.", "yantrodharaka.txt"),
  item("yantrodharaka_page", "stotra", "यन्त्रोद्धारक स्तोत्रम् (कन्नड)", "व्यासराजः", "Kannada and English from the Madhwa archive.", "stotra_yantrodharaka_page.txt"),
  item("vishnu_sahasra", "stotra", "विष्णुसहस्रनामस्तोत्रम्", "", "Vishnu Sahasranama. Local library.", "lib:stotra/VishnuSahasranamaStotram"),
  item("nrisimha_ashtottara", "stotra", "नृसिंहाष्टोत्तरशतनामस्तोत्रम्", "", "Nrisimha ashtottara. Local library.", "lib:stotra/NrisimhaAshtottaraShatanamaStotram"),
  item("lakshmi_nrisimha", "stotra", "लक्ष्मीनृसिंहकरुणारसस्तोत्रम्", "", "Lakshmi Nrisimha Karunarasa. Local library.", "lib:stotra/LakshmiNrisimhaKarunarasaStotram"),
  item("venkatesha_stotra", "stotra", "वेङ्कटेशस्तोत्रम्", "", "Venkatesha Stotram. Local library.", "lib:stotra/VenkateshaStotram"),
  item("venkatesha_suprabhata", "stotra", "वेङ्कटेशसुप्रभातम्", "", "Venkatesha Suprabhatam. Local library.", "lib:stotra/VenkateshaSuprabhatam"),
  item("rama_raksha", "stotra", "रामरक्षास्तोत्रम्", "", "Rama Raksha Stotram. Local library.", "lib:stotra/RamarakshaStotram"),
  item("hanuman_chalisa", "stotra", "हनुमानचालीसा", "", "Hanuman Chalisa. Local library.", "lib:stotra/HanumanChalisa"),
  item("tulasi_ashtottara", "stotra", "तुलस्यष्टोत्तरशतनामस्तोत्रम्", "", "Tulasi ashtottara. Local library.", "lib:stotra/TulasiAshtottaraShatanamaStotram"),
  item("vyasashtakam", "stotra", "व्यासाष्टकम्", "", "Vyasashtakam. Local library.", "lib:stotra/Vyasashtakam"),

  item("nrisimha_mantra", "mantra", "श्री नृसिंह महामन्त्रः", "", "Nrisimha maha-mantra and dwadasa nama.", "nrisimha_mantra.txt"),
  item("pratah_sankalpa", "mantra", "प्रातः सङ्कल्प गद्यम्", "", "Morning sankalpa. Madhwa anushtana.", "pratah_sankalpa.txt"),
  item("sandhyavandanam", "mantra", "सन्ध्यावन्दनम्", "", "Sandhyavandanam. Local library.", "lib:mantras/Sandhyavandanam"),
  item("purusha_suktam", "mantra", "पुरुषसूक्तम्", "", "Purusha Suktam. Local library.", "lib:mantras/PurushaSuktam"),
  item("narayana_suktam", "mantra", "नारायणसूक्तम्", "", "Narayana Suktam. Local library.", "lib:mantras/NarayanaSuktam"),
  item("vishnu_suktam", "mantra", "विष्णुसूक्तम्", "", "Vishnu Suktam. Local library.", "lib:mantras/VishnuSuktam"),
  item("shri_suktam", "mantra", "श्रीसूक्तम्", "", "Shri Suktam. Local library.", "lib:mantras/ShriSuktam"),
  item("aghamarshana", "mantra", "अघमर्षणसूक्तम्", "", "Aghamarshana Suktam. Local library.", "lib:mantras/AghamarshanaSuktam"),
  item("mantra_pushpam", "mantra", "मन्त्रपुष्पम्", "", "Mantra Pushpam. Local library.", "lib:mantras/MantraPushpam"),
  item("brahmayajna", "mantra", "ब्रह्मयज्ञः", "", "Brahmayajna. Local library.", "lib:mantras/Brahmayajna"),

  item("dasara_jagadodharana", "dasara", "जगदोद्धारण", "पुरन्दरदासः", "Jagadodharana. Kannada and English.", "dasara_jagadodharana.txt"),
  item("dasara_bhagyada", "dasara", "भाग्यद लक्ष्मी बारम्मा", "पुरन्दरदासः", "Bhagyada Lakshmi Baramma. Kannada and English.", "dasara_bhagyada_lakshmi.txt"),
  item("dasara_mahalakshmi", "dasara", "दासरा पदगळु — महालक्ष्मी पूजा", "हरिदासाः", "Mahalakshmi puja through Dasara padagalu. Kannada and English.", "puja_mahalakshmi_padagalu.txt"),

  item("ugabhoga_favorite", "ugabhoga", "इष्ट उगभोग सङ्ग्रहः", "हरिदासाः", "Favorite ugabhoga. Kannada and English.", "ugabhoga_favorite.txt"),
  item("ugabhoga_sripadaraja", "ugabhoga", "उगभोग — श्रीपादराजः", "श्रीपादराजः", "Ugabhoga by Sripadarajaru. Kannada and English.", "ugabhoga_sripadaraja.txt"),
  item("ugabhoga_note", "ugabhoga", "उगभोग", "हरिदासाः", "Ugabhoga nitya parayana note.", "ugabhoga.txt"),

  item("suladhi_madhwa_matha", "suladhi", "श्रीमन्माध्वमत महिमा सुळादि", "विजयदासः", "Madhwa matha mahima suladhi. Kannada and English.", "suladhi_madhwa_matha.txt"),
  item("suladhi_nrisimha", "suladhi", "नरसिंह देवर सुळादि", "विजयदासः", "Narasimha devara suladi. Kannada and English.", "suladhi_nrisimha.txt"),
  item("suladhi_durga", "suladhi", "दुर्गा सुळादि", "", "Durga suladhi. Kannada and English.", "suladhi_durga.txt"),
  item("suladhi_raghavendra", "suladhi", "राघवेन्द्र सुळादि", "गोपालदासः", "Raghavendra suladhi. Kannada and English.", "suladhi_raghavendra.txt"),
  item("suladhi_vayu_traya", "suladhi", "वायुदेव अवतारत्रय सुळादि", "राघवेन्द्रतीर्थः", "Vayu avatara traya suladhi. Kannada and English.", "suladhi_vayu_traya.txt"),
  item("suladhi_madhwacharya", "suladhi", "मध्वाचार्य सुळादि", "पुरन्दरदासः", "Madhwacharyaru suladhi. Kannada and English.", "suladhi_madhwacharya.txt"),
  item("suladhi_hanumanta", "suladhi", "हनुमन्त सुळादि", "पुरन्दरदासः", "Hanumanta suladhi. Kannada and English.", "suladhi_hanumanta.txt"),
  item("suladhi_hasthodhaka", "suladhi", "हस्तोदक सुळादि", "", "Hasthodhaka suladhi. Kannada PDF.", "suladhi_hasthodhaka.txt"),
  item("suladhi_vivaha", "suladhi", "विवाह सुळादि", "", "Vivaha suladhi. Kannada PDF.", "suladhi_vivaha.txt"),
  item("suladhi_pancharatna", "suladhi", "पञ्चरत्न सुळादिगळु", "विजयदासः", "Pancharatna suladhigalu. Kannada PDF.", "suladhi_pancharatna.txt"),

  item("kesava_nama", "kesava", "केशवनाम", "", "Chaturvimshati nama. Nitya parayana.", "kesava_nama.txt"),
  item("kesava_nama_page", "kesava", "केशव नाम — दासर पद", "कनकदासः", "Kesava nama. Kannada and English.", "kesava_nama_page.txt"),


  item("yathigalu", "yathi", "माध्व यतिगळु गुरुगळु", "", "Madhwa, Sripadaraja, Vyasaraja, Vadiraja, Raghavendra.", "yathigalu.txt"),

  item("festivals", "festival", "माध्व उत्सवाः", "", "Ugadi, Akshaya Tritiya, Narasimha Jayanti, Madhwa Navami.", "festivals.txt"),

  item("puja_anushtana", "puja", "पूजा अनुष्ठानम्", "", "Nitya karma. Saligrama. Tulasi. Ekadasi.", "puja_anushtana.txt"),
  item("puja_udupi", "puja", "उडुपी कृष्ण पूजा हाडु", "", "Udupi Krishna puja haadu. English baraha.", "puja_udupi_krishna.txt"),
  item("puja_sandhya", "puja", "सन्ध्यावन्दनम्", "", "Sandhyavandanam. Local library.", "lib:mantras/Sandhyavandanam"),
  item("puja_aupasana", "puja", "औपासनम्", "", "Aupasanam. Local library.", "lib:mantras/Aupasanam"),
  item("puja_sandhya_prayoga", "puja", "सन्ध्यावन्दन प्रयोगः", "", "Sandhyavandana prayoga. Local library.", "lib:mantras/SandhyavandanaPrayoga"),

  item("books_note", "books", "माध्व ग्रन्थाः", "", "Sumadhva Vijaya, HKS, Dvadasha Stotra, MBTN.", "books.txt"),
];

for (const [n, title] of SANDHI) {
  const file = `hks_${String(n).padStart(2, "0")}.txt`;
  items.push(item(`hks_${String(n).padStart(2, "0")}`, "books", `हरिकथामृतसारः ${n} — ${title}`, "जगन्नाथदासः", "", file));
}

mkdirSync(dirname(CATALOG), { recursive: true });
const canonicalItems = items.filter(({ file }) => !file.startsWith("lib:"));
writeFileSync(CATALOG, JSON.stringify({ source: "local", langs: ["kn", "en", "sa"], items: canonicalItems }, null, 2));
console.log("items", canonicalItems.length);
