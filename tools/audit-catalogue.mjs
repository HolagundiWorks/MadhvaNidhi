import { createHash } from "node:crypto";
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { basename, dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..", "app", "src", "main", "assets");
const libraryRoot = join(root, "library");
const noisePattern = /\b(?:shlokacount\d*|flushleft|flushright|autocols?|arial unicode ms|fontsize|baselineskip|renewcommand|setlength|begin\{|end\{|documentclass|usepackage|hfill|medskip|bigskip|smallskip|newpage|pagebreak|hyperref|textbf|textit|10pt|12pt)\b/i;

function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

function normalized(value) {
  return value.normalize("NFC").toLocaleLowerCase("en").replace(/[\p{P}\p{S}\s]+/gu, "");
}

function sha(value) {
  return createHash("sha256").update(value.normalize("NFC").replace(/\s+/gu, " ").trim()).digest("hex");
}

function scripts(value) {
  return {
    kn: (value.match(/[\u0C80-\u0CFF]/gu) || []).length,
    sa: (value.match(/[\u0900-\u097F]/gu) || []).length,
    ta: (value.match(/[\u0B80-\u0BFF]/gu) || []).length,
    latin: (value.match(/[A-Za-z]/g) || []).length,
  };
}

function groupedDuplicates(rows, key) {
  const groups = new Map();
  for (const row of rows) {
    const value = key(row);
    if (!value) continue;
    const list = groups.get(value) || [];
    list.push(row);
    groups.set(value, list);
  }
  return [...groups.values()].filter((group) => group.length > 1);
}

const rows = [];
const missingFiles = [];
const orphanFiles = [];
for (const collectionEntry of readdirSync(libraryRoot, { withFileTypes: true })) {
  if (!collectionEntry.isDirectory()) continue;
  const collection = collectionEntry.name;
  const dir = join(libraryRoot, collection);
  const index = readJson(join(dir, "index.json"));
  const items = index.items || [];
  const known = new Set(items.map((item) => `${item.id}.txt`.toLocaleLowerCase("en")));
  for (const item of items) {
    const path = join(dir, `${item.id}.txt`);
    try {
      if (!statSync(path).isFile()) throw new Error("not a file");
    } catch {
      missingFiles.push({ collection, id: item.id, title: item.title });
      continue;
    }
    const body = readFileSync(path, "utf8");
    rows.push({
      collection,
      id: item.id,
      title: item.title,
      section: item.section || "",
      path: relative(root, path).replaceAll("\\", "/"),
      body,
      bodyHash: sha(body),
      titleKey: normalized(item.title || ""),
      script: scripts(body),
      noise: [...new Set(body.split(/\r?\n/).filter((line) => noisePattern.test(line)).map((line) => line.trim()))],
    });
  }
  for (const name of readdirSync(dir)) {
    if (name.endsWith(".txt") && !known.has(name.toLocaleLowerCase("en")) && name !== "SOURCE.txt") {
      orphanFiles.push(relative(root, join(dir, name)).replaceAll("\\", "/"));
    }
  }
}

const madhwaIndex = readJson(join(root, "madhwa", "catalog.json"));
const recordedPaths = new Set(rows.map((row) => row.path));
for (const item of madhwaIndex.items || []) {
  const libMatch = String(item.file || "").match(/^lib:([^/]+)\/(.+)$/);
  const path = libMatch
    ? join(root, "library", libMatch[1], `${libMatch[2]}.txt`)
    : join(root, "madhwa", "texts", item.file || "");
  try {
    if (!item.file || !statSync(path).isFile()) throw new Error("not a file");
  } catch {
    missingFiles.push({ collection: "madhwa", id: item.id, title: item.title, file: item.file || "" });
    continue;
  }
  const body = readFileSync(path, "utf8");
  rows.push({
    collection: "madhwa",
    id: item.id,
    title: item.title,
    section: item.category || "",
    path: relative(root, path).replaceAll("\\", "/"),
    body,
    bodyHash: sha(body),
    titleKey: normalized(item.title || ""),
    script: scripts(body),
    noise: [...new Set(body.split(/\r?\n/).filter((line) => noisePattern.test(line)).map((line) => line.trim()))],
  });
  recordedPaths.add(relative(root, path).replaceAll("\\", "/"));
}

const navigation = readJson(join(root, "catalogue", "index.json"));
for (const group of navigation.groups || []) {
  for (const item of group.items || []) {
    if (!item.file || item.file.startsWith("lib:")) continue;
    const path = item.file.startsWith("madhwa/")
      ? join(root, item.file)
      : join(root, "madhwa", "texts", item.file);
    const relativePath = relative(root, path).replaceAll("\\", "/");
    if (recordedPaths.has(relativePath)) continue;
    if (!existsSync(path)) {
      missingFiles.push({ collection: `catalogue:${group.id}`, id: item.id, title: item.kn || item.sa || item.en, file: item.file });
      continue;
    }
    const body = readFileSync(path, "utf8");
    rows.push({
      collection: `catalogue:${group.id}`,
      id: item.id,
      title: item.kn || item.sa || item.en,
      section: group.id,
      path: relativePath,
      body,
      bodyHash: sha(body),
      titleKey: normalized(item.kn || item.sa || item.en || ""),
      script: scripts(body),
      noise: [...new Set(body.split(/\r?\n/).filter((line) => noisePattern.test(line)).map((line) => line.trim()))],
    });
    recordedPaths.add(relativePath);
  }
}

const duplicateBodies = groupedDuplicates(rows, (row) => row.bodyHash).map((group) => group.map(({ collection, id, title, path }) => ({ collection, id, title, path })));
const duplicateTitles = groupedDuplicates(rows, (row) => `${row.collection}:${row.titleKey}`).map((group) => group.map(({ collection, id, title, path }) => ({ collection, id, title, path })));
const noIndic = rows.filter((row) => row.script.kn === 0 && row.script.sa === 0).map(({ body, bodyHash, titleKey, ...row }) => row);
const tamilOnly = rows.filter((row) => row.script.ta > 0 && row.script.kn === 0 && row.script.sa === 0).map(({ body, bodyHash, titleKey, ...row }) => row);
const noisy = rows.filter((row) => row.noise.length).map(({ body, bodyHash, titleKey, ...row }) => row);

const result = {
  generatedAt: new Date().toISOString(),
  totals: {
    indexedBodies: rows.length,
    missingFiles: missingFiles.length,
    orphanFiles: orphanFiles.length,
    duplicateBodyGroups: duplicateBodies.length,
    duplicateTitleGroups: duplicateTitles.length,
    noIndicBodies: noIndic.length,
    tamilOnlyBodies: tamilOnly.length,
    noisyBodies: noisy.length,
  },
  missingFiles,
  orphanFiles,
  duplicateBodies,
  duplicateTitles,
  noIndic,
  tamilOnly,
  noisy,
};

process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
