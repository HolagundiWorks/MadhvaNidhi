import fs from "node:fs";
import path from "node:path";

const input = process.argv[2] || "tmp/rigveda-sandhya.html";
const output = process.argv[3] || "app/src/main/assets/madhwa/texts/rigveda_sandhyavandana.txt";
const html = fs.readFileSync(input, "utf8");
const start = html.indexOf("<p>ಶ್ರೀ ಗುರುಭ್ಯೋ ನಮಃ");
const endNeedle = "<p>ಸರ್ವಂ ಶ್ರೀಮನ್ನಾರಾಯಣಾರ್ಪಣಮಸ್ತು ||</p>";
const end = html.indexOf(endNeedle, start);
if (start < 0 || end < 0) throw new Error("Rigveda Sandhyavandana article boundaries were not found");

let body = html.slice(start, end + endNeedle.length);
body = body.replace(/<div class=['"]code-block[\s\S]*?<\/div>/g, "");
body = body.replace(/<br\s*\/?>/gi, "\n").replace(/<\/p>/gi, "\n\n");
body = body.replace(/<[^>]+>/g, "");
body = body.replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(Number(n)));
body = body.replace(/&#x([0-9a-f]+);/gi, (_, n) => String.fromCodePoint(parseInt(n, 16)));
body = body.replace(/&nbsp;/g, " ").replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">");
body = body.replace(/[ \t]+\n/g, "\n").replace(/\n{3,}/g, "\n\n").trim();
const header = [
  "ಋಗ್ವೇದೀಯ ತ್ರಿಕಾಲ ಸಂಧ್ಯಾವಂದನೆ",
  "ಆಶ್ವಲಾಯನ ಸೂತ್ರ · ಕನ್ನಡ · ಸಂಪೂರ್ಣ ಪ್ರಾತಃ/ಮಾಧ್ಯಾಹ್ನಿಕ/ಸಾಯಂ ಕ್ರಮ",
  "",
  "ಆಧಾರ: Mantra Samhita — Rigveda Sandhyavandanam in Kannada",
  "https://mantrasamhita.com/kn/rigveda-sandhyavandanam-in-kannada/",
  "",
  "ಸೂಚನೆ: ಕಾಲಕ್ಕೆ ಅನುಗುಣವಾದ (ಪ್ರಾತಃ), (ಮಧ್ಯಾಹ್ನ), (ಸಾಯಂ) ಭಾಗವನ್ನು ಅನುಸರಿಸಿ. ಕುಟುಂಬದ ಗುರು/ಆಚಾರ್ಯರ ಉಪದೇಶಕ್ಕೆ ಆದ್ಯತೆ ನೀಡಿ.",
  ""
].join("\n");
fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, header + body + "\n", "utf8");
console.log(`Wrote ${output} (${body.length} characters)`);
