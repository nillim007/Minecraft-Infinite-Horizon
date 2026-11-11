"""Erzeugt das Spieler-Wiki in docs/wiki/ aus docs/handbook/ und spec/data.

Aufruf:  python tools/gen_wiki.py            (schreibt docs/wiki/)
         python tools/gen_wiki.py --check    (nur prüfen, Exit 1 bei veralteten Dateien)

Dieselbe Quelle erzeugt über tools/gen_resources.py das Ingenieurs-Handbuch im Spiel.
Die Dateien in docs/wiki/ nie von Hand ändern. Der Ordner ist das Wiki (Einstieg docs/wiki/README.md).
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import handbook  # noqa: E402
import specdata as sd  # noqa: E402

OUT = sd.ROOT / "docs" / "wiki"


def main():
    check = "--check" in sys.argv
    hb = handbook.build()
    for w in hb.warnings:
        print(f"Warnung: {w}")
    stale = []
    existing = {p.relative_to(OUT).as_posix() for p in OUT.rglob("*") if p.is_file()} if OUT.exists() else set()
    for name, text in sorted(hb.wiki.items()):
        path = OUT / name
        if not path.exists() or path.read_text(encoding="utf-8") != text:
            stale.append(name)
            if not check:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(text, encoding="utf-8", newline="\n")
    for name in sorted(existing - set(hb.wiki)):
        stale.append(name)
        if not check:
            (OUT / name).unlink()
    if check:
        if stale:
            print("Veraltet: " + ", ".join(stale))
            sys.exit("docs/wiki ist veraltet – python tools/gen_wiki.py ausführen")
        print(f"docs/wiki aktuell ({len(hb.wiki)} Seiten)")
        return
    print(f"{len(hb.pages)} Handbuchseiten in {len(hb.chapters)} Kapiteln, {len(hb.wiki)} Wiki-Dateien → docs/wiki "
          f"({len(stale)} geändert)")


if __name__ == "__main__":
    main()
