#!/usr/bin/env python3
"""Release-Liste in README.md aus den GitHub-Releases (zwischen den Markierungen releases:start und releases:end).

Läuft im Release-Workflow nach jedem neuen Release; lokal: python .github/scripts/release_list.py
Je Release: Version (Link zur Release-Seite), Datum, Beta/Stabil und direkte Download-Links der beiden Jars.
Token optional (GH_TOKEN oder GITHUB_TOKEN), ohne Token gilt das öffentliche API-Limit.
"""
import json
import os
import re
import sys
import urllib.request
from pathlib import Path

REPO = os.environ.get("GITHUB_REPOSITORY", "nillim007/Minecraft-Infinite-Horizon")
README = Path(__file__).resolve().parents[2] / "README.md"
START = "<!-- releases:start (erzeugt von .github/scripts/release_list.py aus den GitHub-Releases) -->"
END = "<!-- releases:end -->"
LOADERS = (("neoforge", "NeoForge"), ("fabric", "Fabric"))


def fetch():
    req = urllib.request.Request(f"https://api.github.com/repos/{REPO}/releases?per_page=100",
                                 headers={"Accept": "application/vnd.github+json", "User-Agent": "release-list"})
    token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(req) as r:
        return [x for x in json.load(r) if not x.get("draft")]


def size(n):
    return f"{n / 1_000_000:.1f} MB".replace(".", ",")


def jar(rel, key, name):
    for a in rel.get("assets", []):
        if a["name"].endswith(".jar") and f"-{key}-" in a["name"]:
            return f"[{name}-Jar]({a['browser_download_url']}) ({size(a['size'])})"
    return "–"


def table(rels):
    out = ["| Version | Datum | Art | NeoForge | Fabric |", "| --- | --- | --- | --- | --- |"]
    for r in rels:  # die API liefert die neueste zuerst
        y, m, d = r["published_at"][:10].split("-")
        kind = "Beta" if r.get("prerelease") else "Stabil"
        cells = [f"[{r['tag_name']}]({r['html_url']})", f"{d}.{m}.{y}", kind] + [jar(r, k, n) for k, n in LOADERS]
        out.append("| " + " | ".join(cells) + " |")
    if not rels:
        out.append("| – | – | – | – | – |")
    return "\n".join(out)


def main():
    text = README.read_text(encoding="utf-8")
    pattern = re.compile(re.escape(START) + r".*?" + re.escape(END), re.S)
    if not pattern.search(text):
        sys.exit("README.md: Markierungen releases:start/releases:end fehlen")
    new = pattern.sub(lambda _: START + "\n\n" + table(fetch()) + "\n\n" + END, text, count=1)
    if new == text:
        print("Release-Liste unverändert")
        return
    README.write_text(new, encoding="utf-8", newline="\n")
    print("Release-Liste aktualisiert")


if __name__ == "__main__":
    main()
