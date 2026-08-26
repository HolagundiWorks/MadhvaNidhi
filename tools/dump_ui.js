const fs = require("fs");
const xml = fs.readFileSync("C:/Users/holag/Projects/paperdesk/ui.xml", "utf8");
const re = /text="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g;
const rows = [];
let m;
while ((m = re.exec(xml))) {
  if (m[1]) rows.push(m[1].slice(0, 70) + "  " + m[2] + "," + m[3]);
}
console.log(rows.slice(0, 16).join("\n"));
