/**
 * Convert Kannada Wikisource pad wikitext → clean lyric bodies.
 * Source license: CC BY-SA 4.0 (kn.wikisource.org). Attribution in
 * app/src/main/assets/licenses/haridasa-wikisource.txt
 */
import { mkdirSync, readdirSync, readFileSync, writeFileSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const wikiDir = join(root, "tmp", "wikisource");
const outBase = join(root, "app", "src", "main", "assets", "madhwa", "texts", "haridasa");

function stripWiki(raw) {
  let t = raw.normalize("NFC").replace(/\r\n?/g, "\n");
  t = t.replace(/\{\{[^}]*\}\}/gs, "");
  t = t.replace(/\[\[[^\]]*\|([^\]]+)\]\]/g, "$1");
  t = t.replace(/\[\[([^\]]+)\]\]/g, "$1");
  t = t.replace(/\[https?:\/\/[^\s\]]+\s+[^\]]*\]/gi, "");
  t = t.replace(/\[https?:\/\/[^\]]+\]/gi, "");
  t = t.replace(/'{2,}/g, "");
  t = t.replace(/<\/?poem>/gi, "\n");
  t = t.replace(/<br\s*\/?>/gi, "\n");
  t = t.replace(/<hr\s*\/?>/gi, "\n");
  t = t.replace(/<\/?[^>]+>/g, "");
  t = t.replace(/^[=]{1,6}\s*.*$/gm, "");
  t = t.replace(/^----+$/gm, "");
  t = t.replace(/^#+\s*.*$/gm, "");
  // Drop meta lines (author/raga labels kept out of chant body)
  const lines = [];
  for (const original of t.split("\n")) {
    let line = original.replace(/[ \t]+/g, " ").trim();
    if (!line) {
      if (lines.length && lines[lines.length - 1] !== "") lines.push("");
      continue;
    }
    if (/^(ರಚನೆ|ಸಾಹಿತ್ಯ|ರಾಗ|ತಾಳ)\s*:/u.test(line)) continue;
    if (/^ವರ್ಗ:/u.test(line)) continue;
    if (/ಕೇಳಿರೀ|ಕೇಳಿರಿ|ಯೂಟ್ಯೂಬ್|youtube|ಹಾಡಿದವರು/iu.test(line)) continue;
    if (/^ಪರಿವಿಡಿ$/u.test(line)) continue;
    lines.push(line);
  }
  return lines.join("\n").replace(/\n{3,}/g, "\n\n").trim() + "\n";
}

/** Manual pads extracted from https://kn.wikisource.org/wiki/ದಾಸ_ಸಾಹಿತ್ಯ/ಪುರಂದರದಾಸರು (CC BY-SA). */
const collectionPads = {
  "purandara/ajnani_gala_kuda.txt": `ಅಜ್ಞಾನಿಗಳ ಕೂಡ ಅಧಿಕ ಸ್ನೇಹಕ್ಕಿಂತ
ಸುಜ್ಞಾನಿಗಳ ಕೂಡ ಜಗಳವೇ ಲೇಸು ||ಪ||

ಡಂಭಕರ ಮನೆಯ ಅಪಮಾನ ಊಟಕ್ಕಿಂತ
ತುಂಬಿದ ಪಟ್ಟಣದಿ ತಿರಿದುಂಬುವುದೆ ಲೇಸು
ಹಂಬಲಿಸಿ ಹಾಳು ಹರಟೆ ಹೊಡೆವುದಕಿಂತ
ನಂಬಿ ಹರಿದಾಸರೊಳಾಡುವುದೆ ಲೇಸು ||೧||

ಒಡಲ ಹಂಗಿಸುವರ ಮನೆಯ ಓಗರಕಿಂತ
ಕುಡಿನೀರ ಕುಡಿದುಕೊಂಡಿಹುದೆ ಲೇಸು
ಬಿಡದೆ ಕಡಿದಾಡುವರ ನೆರೆಯಲಿಹುದಕಿಂತ
ಅಡವಿಯೊಳಜ್ಞಾತವಾಸವೇ ಲೇಸು ||೨||

ಮಸೆದು ಮತ್ಸರಿಸುವನ ಬಳಿಯಲಿಹುದಕಿಂತ
ಹಸನಾದ ಹಾಳುಗುಡಿಗಳೆ ಲೇಸು
ಬಿಸಜಾಕ್ಷ ಪುರಂದರವಿಠಲನ ನೆನೆನೆನೆದು
ವಸುಧೆಯೊಳು ಚಿರಕಾಲವಿರುವುದೆ ಲೇಸು ||೩||
`,
  "purandara/antakana_dutarige.txt": `ಅಂತಕನ ದೂತರಿಗೆ ಕೃಪೆಯಿಲ್ಲವದರಿಂದ
ಚಿಂತೆಯನು ಬಿಟ್ಟು ಶ್ರೀ ಹರಿಯೆ ನೆನೆ ಮನವೆ ||ಪ||

ದಿವರಾತ್ರಿಯೆನ್ನದೇ ತಿರುಗಿ ಲಂಪಟನಾಗಿ
ಸವಿಗಂಡ ಊಟಗಳ ಉಣಲರಿಯದೆ
ಅವನ ಕೊಂದು ಇವನ ಕೊಂದು ಅರ್ಥವನು ಗಳಿಸಿಕೊಂಡು
ಜವನ ದೂತರು ಬಂದು ಎಳೆವ ಹೊತ್ತರಿಯೆ ||೧||

ಮೊನ್ನೆ ಮದುವೆಯಾದೆ ಕರೆವುವು ಒಂದೆರಡಮ್ಮೆ
ನಿನ್ನೆ ಕೊಂಡೆನು ಕ್ಷೇತ್ರ ಫಲಬಾಹೊದು
ಹೊನ್ನು ಹಣವುಂಟೆನಗೆ ಸಾಯಲಾರೆನು ಎನಲಾ
ತನ್ನ ದೂತರು ಬಂದು ಎಳೆವ ಹೊತ್ತರಿಯೆ ||೨||

ಹೊಸ ಮನೆಯ ಕಟ್ಟಿದೆನು ಗೃಹಶಾಂತಿ ಮನೆಯೊಳಗೆ
ಬಸಿರೆ ಹೆಂಡತಿ ಮಗನ ಮದುವೆಯು ನಾಳೆ
ಹಸನವಾಗಿದೆ ಬದುಕು ಸಾಯಲಾರೆನು ಎನಲು
ವಿಷಮ ದೂತರು ಬಂದು ಎಳೆವ ಹೊತ್ತರಿಯೆ ||೩||

ಪುತ್ರ ಹುಟ್ಟಿದ ದಿವಸ ಹಾಲು ಊಟದ ಹಬ್ಬ
ಮತ್ತೊಬ್ಬ ಮಗನ ಉಪನಯನ ನಾಳೆ
ಅರ್ಥಿಯಾಗದೆ ಬದುಕು ಸಾಯಲಾರೆನು ಎನಲು
ಮೃತ್ಯು ಹೆಡತಲೆಯಲಿ ನಗುತಿಹುದರಿಯೆ ||೪||

ಅಟ್ಟಡಿಗೆ ಉಣಲಿಲ್ಲ ಇಟ್ಟ ನೀರ್ಮೀಯಲಿಲ್ಲ
ಕೊಟ್ಟ ಸಾಲವ ಕೇಳ್ವ ಹೊತ್ತ ಕಾಣೆ
ಕಟ್ಟಕಡೆಯ ದಿವಸ ತಂಬಿದ ಬಳಿಕಿರಲಿಲ್ಲ
ಇಷ್ಟರೊಳು ಪುರಂದರವಿಠಲನ ನೆನೆ ಮನವೆ ||೫||
`,
  "purandara/antarangadalli_hariya.txt": `ಅಂತರಂಗದಲಿ ಹರಿಯ ಕಾಣದವ ತಾ ಹುಟ್ಟುಗುರುಡನೊ
ಸಂತತ ಶ್ರೀ ಕೃಷ್ಣಚರಿತೆ ಕೇಳದವ ಜಡಮತಿಯೆ ಕಿವುಡನೊ ||ಪ||

ಹರುಷದಿಂದಲಿ ಮುರಹರನ ಪೂಜೆಯ ಮಾಡದವನೆ ಕೈ ಮುರಿದವನೊ
ಕುರುವೀರ ಸೂತನ ಮುಂದೆ ಕೃಷ್ಣ ಎಂದು ಕುಣಿಯದವನೆ ಕುಂಟನೊ
ನರಹರಿ ಪಾದೋದಕ ಧರಿಸದ ಶಿರ ನಾಯುಂಡ ಹಂಚು ಕಾಣೊ
ಸುರವರ ಕೃಷ್ಣ ಪ್ರಸಾದವಿಲ್ಲದ ಊಟ ಸೂಕರಭೋಜನವೋ ||೧||

ಅಮರೇಶ ಕೃಷ್ಣಗರ್ಪಿತವಲ್ಲದ ಕರ್ಮ ಅಸತಿಯ ವ್ರತನೇಮವೊ
ರಮೆಯರಸಗೆ ಪ್ರೀತಿಯಿಲ್ಲದ ವಿತ್ತವು ರಂಡೆ ಕೊರಳ ಸೂತ್ರ
ಕಮಲನಾಭನ ಪೊಗಳದ ಸಾರ ಸಂಗೀತ ಗಾರ್ದಭರೋದನವೊ
ಮಮತೆಯಿಂದಲಿ ಕೇಶವಗೆ ನಮಸ್ಕಾರ ಮಾಡದವನೆ ಮೃಗವೊ ||೨||

ಜರೆ ಹುಟ್ಟು ಮರಣವ ತಡೆವ ಸುದ್ದಿಯ ಬಿಟ್ಟು ಸುರೆಯ ಸುರಿಯ ಬೇಡವೊ
ಸುರಧೇನು ಇರಲಾಗಿ ಶ್ವಾನನ ಮೊಲೆಹಾಲ ಕರೆದು ಕುಡಿಯಬೇಡವೊ
ಕರಿ ರಥ ತುರಗವೇರಲು ಇದ್ದು ಕೆಡಹುವ ಕತ್ತೆ ಏರಲಿಬೇಡವೊ
ಪರಮ ಪುರುಷ ಪುರಂದರವಿಠಲನಿರಲು ನರರ ಭಜಿಸಬೇಡವೊ ||೩||
`,
  "purandara/anjabeda_bedavele.txt": `ಅಂಜಬೇಡ ಬೇಡವೆಲೆ ಜೀವ
ಭವ ಭಂಜನ ಹರಿಶರಣರ ತಾವ ||ಪ||

ಬಂದಷ್ಟರಿಂದಲಿ ಬಾಳಿಕೊ ಬಲು
ಸಂದೇಹ ಬಂದಲ್ಲಿ ಕೇಳಿಕೊ
ನಿಂದಾ ಸ್ತುತಿಗಳನು ತಾಳಿಕೊ
ಗೋವಿಂದ ನಿನ್ನವನೆಂದು ಹೇಳಿಕೊ ||೧||

ಮಾಧವನಿಗೆ ತನುಮನ ಮೆಚ್ಚು
ಕಾಮ ಕ್ರೋಧಾದಿಗಳ ಕಲಿಮಲ ಕೊಚ್ಚು
ಮೋದ ತೀರ್ಥ ವಚನವೆ ಹೆಚ್ಚು
ಮಾಯಾವಾದಿ ಮತಕೆ ಬೆಂಕಿಯ ಹಚ್ಚು ||೨||

ಪರವನಿತೆಯ ರಾಶಿಯ ಬಿಡು
ನೀ ಹರಿ ಸರ್ವೋತ್ತಮನೆಂದು ಕೊಂಡಾಡು
ಪರಮಾತ್ಮನ ಧ್ಯಾನವ ಮಾಡು
ನಮ್ಮ ಸಿರಿ ಪುರಂದರವಿಠಲನ ನೀನೋಡು ||೩||
`,
};

/** Stable ASCII slugs for known Wikisource titles (author folder + file). */
const slugByTitle = {
  "ಅಂಬಿಗ ನಾ ನಿನ್ನ ನಂಬಿದೆ": "purandara/ambiga_na_ninna.txt",
  "ಅಂಬೆಗಾಲಿಕ್ಕುತಲಿ": "purandara/ambegalikkutali.txt",
  "ಆಚಾರವಿಲ್ಲದ ನಾಲಿಗೆ": "purandara/acharavillada_nalige.txt",
  "ಆಡಿಸಿದಳೆಶೋದಾ ಜಗದೋದ್ಧಾರನ": "purandara/jagadodharana.txt",
  "ಆದದ್ದೆಲ್ಲ ಒಳಿತೆ ಆಯಿತು": "purandara/adadella_olite.txt",
  "ಭಾಗ್ಯದ ಲಕ್ಷ್ಮೀ ಬಾರಮ್ಮ": "purandara/bhagyada_lakshmi.txt",
  "ಕರುಣಿಸೋ ರಂಗ": "purandara/karuniso_ranga.txt",
  "ದಾಸನ ಮಾಡಿಕೊ ಎನ್ನ": "purandara/dasana_madiko.txt",
  "ನೀನ್ಯಾಕೊ ನಿನ್ನ ಹಂಗ್ಯಾಕೊ": "purandara/neenyako_ninna.txt",
  "ಅನುಗಾಲವು ಚಿಂತೆ": "purandara/anugalavu_chinte.txt",
  "ಕೃಷ್ಣ ನೀ ಬೇಗನೆ ಬಾರೊ": "vyasaraya/krishna_nee_begane.txt",
  "ಪೋಗದಿರೆಲೊ ರಂಗ": "purandara/pogadirelo_ranga.txt",
  "ಬಂದದೆಲ್ಲ ಬರಲಿ": "purandara/bandadella_barali.txt",
  "ಯಾದವ ನೀ ಬಾ": "purandara/yadava_nee_baa.txt",
  "ರಂಗನಾಯಕ ರಾಜೀವಲೋಚನ": "purandara/ranganayaka_rajeevalochana.txt",
  "ಹರಿ ಸ್ಮರಣೆ ಮಾಡೊ": "purandara/hari_smarane_mado.txt",
  "ಕಲ್ಲು ಸಕ್ಕರೆ ಕೊಳ್ಳಿರೊ": "purandara/kallu_sakkare.txt",
  "ಗಜಾನನ ಸ್ತುತಿ - ಗಜವದನ ಬೇಡುವೆ": "purandara/gajavadana_beduve.txt",
  "ಗುರುವಿನ ಗುಲಾಮ": "purandara/guruvina_gulama.txt",
  "ಡೊಂಕು ಬಾಲದ ನಾಯಕರೆ": "purandara/donku_balada.txt",
  "ದೇವ ಬಂದ": "purandara/deva_banda.txt",
  "ನಾ ನಿನ್ನ ಧ್ಯಾನದೊಳಿರಲು": "purandara/na_ninna_dhyana.txt",
  "ಪಾಲಿಸೆನ್ನನು ಶ್ರೀಮಹಾಲಕ್ಷ್ಮೀ": "purandara/palisennanu_lakshmi.txt",
  "ರಂಗ ಬಾರೋ ರಂಗ ಬಾರೋ": "purandara/ranga_baro.txt",
  "ರಾಮ ನಾಮ ಪಾಯಸಕ್ಕೆ": "purandara/rama_nama_payasa.txt",
  "ಶರಣು ಸಿದ್ಧಿವಿನಾಯಕ": "purandara/sharanu_siddhivinayaka.txt",
  "ಹರಿ ಚಿತ್ತ ಸತ್ಯ": "purandara/hari_chitta_satya.txt",
  "ಯಾರಿಗೆ ಯಾರುಂಟು ಎರವಿನ ಸಂಸಾರ": "purandara/yarige_yaruntu.txt",
  "ಮೋಸ ಹೋದೆನಲ್ಲಾ": "purandara/mosa_hodenalla.txt",
  "ಯಾಕೆ ಚಿಂತಿಸುತಿದ್ದಿ ಕೋತಿಮನವೆ": "purandara/yake_chintisutiddi.txt",
  "ರಕ್ಷಿಸು ಲೋಕನಾಯಕನೆ ರಕ್ಷಿಸು": "purandara/rakshisu_lokanayaka.txt",
  "ಶ್ರೀನಿವಾಸ ಎನ್ನ ಬಿಟ್ಟು": "purandara/srinivasa_enna_bittu.txt",
  "ಗಜೇಂದ್ರಮೋಕ್ಷ": "purandara/gajendra_moksha.txt",
  "ದುಗ್ಗಾಣಿ ಎಂಬೊದು": "purandara/duggani_embodu.txt",
  "ನಾ ಮಾಡಿದ ಕರ್ಮ": "purandara/na_madida_karma.txt",
  "ಯಮನೆಲ್ಲೋ ಕಾಣೆನೆಂದ್ಹೇಳಬೇಡ": "purandara/yamanello_kaane.txt",
  "ಯಾಕೆ ನಿರ್ದಯನಾದೆ ಎಲೊ ದೇವನೆ": "purandara/yake_nirdayanaade.txt",
  "ಯಾಕೆ ಮೂರ್ಖನಾದ್ಯೋ ಮನುಜ": "purandara/yake_moorkhanaadyo.txt",
  "ಯಾಕೆನ್ನೋಳಿನಿತು ಕೃಪೆಯಿಲ್ಲ ಹರಿಯೆ": "purandara/yakennoLinaitu.txt",
  "ಯಾರು ಒಲಿದರೇನು": "purandara/yaru_olidarenu.txt",
  "ಯಾರೆ ಬಂದವರು ಮನೆಗೆ": "purandara/yare_bandavaru.txt",
  "ಯಾರೆ ರಂಗನ ಯಾರೆ ಕೃಷ್ಣನ": "purandara/yare_rangana.txt",
  "ರಾಗಿ ತಂದೀರಾ ಭಿಕ್ಷಕೆ ರಾಗಿ ತಂದೀರಾ": "purandara/ragi_tandira.txt",
  "ಯಾದವ ನೀ ಬಾ ಯದುಕುಲ ನಂದನ": "purandara/yadava_nee_baa_yadukula.txt",
  "ಕೃಷ್ಣಾ ಬಾರೋ": "purandara/krishnaa_baro.txt",
  "ಗಿಳಿಯು ಪಂಜರದೊಳಿಲ್ಲ": "purandara/giliyu_panjaradollilla.txt",
  "ತೇಲಿಸೊ ಇಲ್ಲ ಮುಳುಗಿಸೊ": "purandara/teliso_illa.txt",
  "ಯಾರು ಬಿಟ್ಟರೂ ಕೈಯ ನೀ ಬಿಡದಿರು": "purandara/yaru_bittaru_kaiya.txt",
  "ಯಾರೂ ಸಂಗಡ ಬಾಹೋರಿಲ್ಲ": "purandara/yaroo_sangada.txt",
  "ತಲ್ಲಣಸಿದಿರು ಕಂಡ್ಯ": "kanaka/tallanasidiru_kandya.txt",
};

function titleFromWikiName(name) {
  return name.replace(/\.wiki\.txt$/u, "").replace(/\.txt$/u, "");
}

function wikiUrl(title) {
  return `https://kn.wikisource.org/wiki/${encodeURIComponent(title.replace(/ /g, "_"))}`;
}

mkdirSync(join(outBase, "purandara"), { recursive: true });
mkdirSync(join(outBase, "vyasaraya"), { recursive: true });

let written = 0;
const imported = [];
for (const [rel, body] of Object.entries(collectionPads)) {
  const path = join(outBase, rel);
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, body.normalize("NFC"), "utf8");
  written++;
  imported.push({ title: rel, file: rel, source: "collection" });
  console.log("manual", rel);
}

const wikiFiles = readdirSync(wikiDir).filter((n) => n.endsWith(".txt"));
for (const file of wikiFiles) {
  if (file === "category_members.txt" || file === "ಪುರಂದರದಾಸರು.txt") continue;
  const title = titleFromWikiName(file);
  const out = slugByTitle[title];
  if (!out) {
    console.warn("no slug", title);
    continue;
  }
  const src = join(wikiDir, file);
  const body = stripWiki(readFileSync(src, "utf8"));
  if (body.replace(/\s/g, "").length < 40) {
    console.warn("too short", file);
    continue;
  }
  const dest = join(outBase, out);
  mkdirSync(dirname(dest), { recursive: true });
  writeFileSync(dest, body, "utf8");
  written++;
  imported.push({
    title,
    file: out,
    url: wikiUrl(title),
    author: out.startsWith("vyasaraya/") ? "vyasaraya" : "purandara",
    source: "wikisource",
  });
  console.log("wiki", out, body.length);
}

writeFileSync(join(wikiDir, "imported.json"), JSON.stringify(imported, null, 2), "utf8");
console.log(JSON.stringify({ written, imported: imported.length }, null, 2));
