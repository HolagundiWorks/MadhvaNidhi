/**
 * Baraha-style roman → Kannada. Reads UTF-8. Does not invent lyrics.
 * Longest-match consonants, then optional dependent vowel, then M/H.
 */
import { readFileSync, writeFileSync } from "fs";

const CONS = [
  ["kSh", "ಕ್ಷ"],
  ["j~n", "ಜ್ಞ"],
  ["~n", "ಞ"],
  ["~g", "ಙ"],
  ["kh", "ಖ"],
  ["gh", "ಘ"],
  ["ch", "ಛ"],
  ["jh", "ಝ"],
  ["Th", "ಠ"],
  ["Dh", "ಢ"],
  ["th", "ಥ"],
  ["dh", "ಧ"],
  ["ph", "ಫ"],
  ["bh", "ಭ"],
  ["Sh", "ಷ"],
  ["sh", "ಶ"],
  ["K", "ಖ"],
  ["G", "ಘ"],
  ["C", "ಛ"],
  ["J", "ಝ"],
  ["P", "ಫ"],
  ["B", "ಭ"],
  ["S", "ಶ"],
  ["T", "ಟ"],
  ["D", "ಡ"],
  ["N", "ಣ"],
  ["L", "ಳ"],
  ["k", "ಕ"],
  ["g", "ಗ"],
  ["c", "ಚ"],
  ["j", "ಜ"],
  ["t", "ತ"],
  ["d", "ದ"],
  ["n", "ನ"],
  ["p", "ಪ"],
  ["b", "ಬ"],
  ["m", "ಮ"],
  ["y", "ಯ"],
  ["r", "ರ"],
  ["l", "ಲ"],
  ["v", "ವ"],
  ["s", "ಸ"],
  ["h", "ಹ"],
  ["x", "ಕ್ಷ"],
];

const IND = [
  ["ai", "ಐ"],
  ["au", "ಔ"],
  ["A", "ಆ"],
  ["I", "ಈ"],
  ["U", "ಊ"],
  ["E", "ಏ"],
  ["O", "ಓ"],
  ["Ru", "ಋ"],
  ["a", "ಅ"],
  ["i", "ಇ"],
  ["u", "ಉ"],
  ["e", "ಎ"],
  ["o", "ಒ"],
];

const DEP = [
  ["ai", "ೈ"],
  ["au", "ೌ"],
  ["Ru", "ೃ"],
  ["A", "ಾ"],
  ["I", "ೀ"],
  ["U", "ೂ"],
  ["E", "ೇ"],
  ["O", "ೋ"],
  ["a", ""],
  ["i", "ಿ"],
  ["u", "ು"],
  ["e", "ೆ"],
  ["o", "ೊ"],
];

function matchAt(s, i, pairs) {
  for (const [k, v] of pairs) {
    if (s.startsWith(k, i)) return [k, v];
  }
  return null;
}

function convert(src) {
  let s = src
    .replace(/À/g, "")
    .replace(/\^\^/g, "")
    .replace(/d~h/g, "dh")
    .replace(/t~h/g, "th")
    .replace(/~h/g, "H");
  let out = "";
  let i = 0;
  while (i < s.length) {
    const ch = s[i];
    if (ch === "\n" || ch === "\r" || ch === " " || ch === "|" || ch === "-" ||
        ch === "." || ch === "," || ch === ":" || ch === ";" || ch === "(" ||
        ch === ")" || ch === "[" || ch === "]" || ch === '"' || ch === "'" ||
        ch === "!" || ch === "?" || ch === "/" || ch === "*" || ch === "=") {
      out += ch;
      i++;
      continue;
    }
    if (ch === "~") {
      if (out.endsWith("್")) { i++; continue; }
      out += "್";
      i++;
      continue;
    }
    if (ch === "M") { out += "ಂ"; i++; continue; }
    if (ch === "H") { out += "ಃ"; i++; continue; }
    const cons = matchAt(s, i, CONS);
    if (cons) {
      i += cons[0].length;
      let base = cons[1];
      if (s[i] === "~" && !matchAt(s, i, CONS)) {
        out += base + "್";
        i++;
        continue;
      }
      const dep = matchAt(s, i, DEP);
      if (dep) {
        out += base + dep[1];
        i += dep[0].length;
      } else {
        out += base + "್";
      }
      continue;
    }
    const ind = matchAt(s, i, IND);
    if (ind) {
      out += ind[1];
      i += ind[0].length;
      continue;
    }
    out += ch;
    i++;
  }
  return out;
}

const inFile = process.argv[2];
const outFile = process.argv[3];
if (!inFile || !outFile) {
  console.error("usage: node baraha2kn.mjs <in> <out>");
  process.exit(1);
}
writeFileSync(outFile, convert(readFileSync(inFile, "utf8")), "utf8");
console.log("wrote", outFile);
