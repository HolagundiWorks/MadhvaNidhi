# Converts forked stotrasamhita TeX into compact library assets for Acharya OS.
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VENDOR = ROOT.parent / "local-artifacts" / "acharya" / "upstream-sources"
OUT = ROOT / "app" / "src" / "main" / "assets" / "library"

SKIP_NAME = re.compile(
    r"(kindle|learn|preamble|frontmatter|shloka\.sty|nAmA\.tex|nAmAvalis|compact)",
    re.I,
)

CHAPT = re.compile(r"\\(?:chapt|chapter|sect|section)\*?\{([^{}]+)\}")
TWOLINE = re.compile(r"\\twolineshloka\*?\{([^{}]*)\}\s*\{([^{}]*)\}", re.S)
THREELINE = re.compile(
    r"\\threelineshloka\*?\{([^{}]*)\}\s*\{([^{}]*)\}\s*\{([^{}]*)\}", re.S
)
FOURLINE = re.compile(
    r"\\fourlineshloka\*?\{([^{}]*)\}\s*\{([^{}]*)\}\s*\{([^{}]*)\}\s*\{([^{}]*)\}",
    re.S,
)
CMD_ONE = re.compile(r"\\[A-Za-z]+\*?\{([^{}]*)\}")
CMD_BARE = re.compile(r"\\[A-Za-z]+\*?")
BRACE = re.compile(r"[{}]")
COMMENT = re.compile(r"(?<!\\)%[^\n]*")
META_TITLE = re.compile(r"wiki_title:\s*(.+)")


def title_from_name(stem: str) -> str:
    s = re.sub(r"[_-]+", " ", stem)
    s = re.sub(r"([a-z])([A-Z])", r"\1 \2", s)
    return re.sub(r"\s+", " ", s).strip()


def strip_tex(raw: str) -> tuple[str, str]:
    title = ""
    m = META_TITLE.search(raw)
    if m:
        title = m.group(1).strip()
    cm = CHAPT.search(raw)
    if cm:
        title = cm.group(1).strip() or title
    text = COMMENT.sub("", raw)
    text = re.sub(r"%\s*!TeX[^\n]*", "", text)
    text = re.sub(r"%\s*--meta--.*?%?\s*--end-meta--", "", text, flags=re.S)
    text = TWOLINE.sub(lambda m: m.group(1).strip() + "\n" + m.group(2).strip(), text)
    text = THREELINE.sub(
        lambda m: "\n".join(g.strip() for g in m.groups()), text
    )
    text = FOURLINE.sub(
        lambda m: "\n".join(g.strip() for g in m.groups()), text
    )
    text = text.replace("~", " ").replace("\\\\", "\n").replace("\\,", " ")
    text = text.replace("\\-", "").replace("\\&", "&")
    for _ in range(4):
        text = CMD_ONE.sub(r"\1", text)
    text = CMD_BARE.sub("", text)
    text = BRACE.sub("", text)
    text = text.replace("\u00a0", " ")
    lines = []
    blank = False
    for line in text.splitlines():
        line = re.sub(r"[ \t]+", " ", line).strip()
        if not line:
            if not blank:
                lines.append("")
            blank = True
            continue
        blank = False
        lines.append(line)
    body = "\n".join(lines).strip()
    return title, body


def collect(src: Path, out_name: str, section_from_parent: bool) -> int:
    dest = OUT / out_name
    dest.mkdir(parents=True, exist_ok=True)
    items = []
    used = set()
    for path in sorted(src.rglob("*.tex")):
        if SKIP_NAME.search(path.name) or SKIP_NAME.search(str(path.relative_to(src))):
            continue
        raw = path.read_text(encoding="utf-8", errors="ignore")
        title, body = strip_tex(raw)
        if len(body) < 40:
            continue
        stem = path.stem
        ident = re.sub(r"[^A-Za-z0-9._-]+", "_", stem).strip("_") or "item"
        if ident in used:
            ident = ident + "_" + path.parent.name
        used.add(ident)
        if not title:
            title = title_from_name(stem)
        section = path.parent.name if section_from_parent else out_name
        (dest / f"{ident}.txt").write_text(body, encoding="utf-8")
        preview = " ".join(body.split()[:12])
        items.append({"id": ident, "title": title, "section": section, "preview": preview})
    (dest / "index.json").write_text(
        json.dumps({"source": str(src.name), "items": items}, ensure_ascii=False, indent=0),
        encoding="utf-8",
    )
    print(f"{out_name}: {len(items)} texts")
    return len(items)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    n = 0
    n += collect(VENDOR / "vedamantra-book" / "mantras", "mantras", False)
    n += collect(VENDOR / "puja-vidhanam", "puja", True)
    n += collect(VENDOR / "stotra-sangrahah" / "stotras", "stotra", True)
    n += collect(VENDOR / "namavali-manjari", "namavali", True)
    (OUT / "SOURCE.txt").write_text(
        "Texts forked from Stotra Samhita (https://github.com/stotrasamhita)\n"
        "vedamantra-book / puja-vidhanam / stotra-sangrahah / namavali-manjari\n"
        "Converted from XeLaTeX for on-device reading. Accents kept where present.\n",
        encoding="utf-8",
    )
    print("total", n)


if __name__ == "__main__":
    main()
