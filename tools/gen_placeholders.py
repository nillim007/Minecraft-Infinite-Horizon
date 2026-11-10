"""Erzeugt 16×16-Platzhalter-Texturen für alle Items und Blöcke aus spec/data/items.json (Kapitel 24.5).

Aufruf:  python tools/gen_placeholders.py [--only id1,id2,...]
Ausgabe: common/src/generated/resources/assets/sternenwerft/textures/{block,item}/<id>.png
Nur Python-Standardbibliothek. Bestehende Dateien werden überschrieben (die Ausgabe ist vollständig generiert).
"""
from __future__ import annotations

import argparse
import hashlib
import struct
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import specdata as sd  # noqa: E402

OUT = sd.ROOT / "common" / "src" / "generated" / "resources" / "assets" / "sternenwerft" / "textures"

PHASE = {  # Grundfarbe, Akzent (24.5)
    1: ("#6E6E6E", "#E8C020"),
    2: ("#E8E8E8", "#E07B20"),
    3: ("#E8E8E8", "#E07B20"),
    4: ("#3A3F47", "#4FB3E8"),
    5: ("#3A3F47", "#4FB3E8"),
    6: ("#1A1420", "#9B4FE8"),
}
PLANET = {  # Gesteins- und Bodenfarben (24.5)
    "moon": "#8C8C8C", "moon_basalt": "#3A3A3A", "mars": "#B5502A", "gypsum": "#E6DFD0", "dry_ice": "#F0F4F8",
    "venus": "#3B2F2A", "mercury": "#5A5550", "asteroid": "#6B6B6B", "ceres": "#7A776F", "salt": "#E8E8E0",
    "io": "#E8C440", "europa": "#E8EEF2", "titan": "#A0622A", "enceladus": "#F4F8FF", "triton": "#C9A4A4",
    "pluto_tholin": "#5A3A2A", "pluto": "#D8C8A8", "quartz_sand": "#E8DCB0", "ancient_alloy": "#2A2A38",
}
METAL = {
    "aluminum": "#C8CCD0", "bauxite": "#B07850", "titanium": "#8A94A6", "magnesium": "#D8D8C0", "nickel": "#A8A890",
    "tungsten": "#4A4E58", "lithium": "#F0E8F0", "platinum": "#E0E0E8", "iridium": "#B0C0D8", "uranium": "#7AC040",
    "sulfur": "#E8D020", "iron": "#D8D0C8", "copper": "#D88050", "gold": "#F0D040",
}


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def png(pixels):
    raw = b"".join(b"\x00" + bytes(v for px in row for v in px) for row in pixels)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def rng(seed):
    h = hashlib.sha256(seed.encode()).digest()
    i = 0
    while True:
        yield h[i % len(h)]
        i += 1
        if i % len(h) == 0:
            h = hashlib.sha256(h).digest()


def metal_of(name):
    for m in sorted(METAL, key=len, reverse=True):
        if m in name:
            return METAL[m]
    return None


def base_colors(e):
    name = e["id"].split(":")[1]
    cat = e.get("category")
    if cat in ("planet_block", "ore", "world"):
        for k in sorted(PLANET, key=len, reverse=True):
            if name.startswith(k) or k in name:
                return rgb(PLANET[k]), rgb(metal_of(name) or PLANET[k])
    if cat == "material":
        m = metal_of(name)
        if m:
            return rgb(m), shade(rgb(m), 1.25)
    base, accent = PHASE.get(e.get("phase") or 1, PHASE[1])
    return rgb(base), rgb(accent)


def texture(e):
    base, accent = base_colors(e)
    r = rng(e["id"])
    name = e["id"].split(":")[1]
    item_like = e.get("kind") == "item"
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            if item_like and (x < 2 or x > 13 or y < 2 or y > 13):
                continue  # Items: transparenter Rand
            c = base
            noise = (next(r) % 21 - 10) / 100
            c = shade(c, 1 + noise)
            if edge or (item_like and (x in (2, 13) or y in (2, 13))):
                c = shade(base, 0.6)
            px[y][x] = (*c, 255)
    if e.get("category") == "ore" or "ore" in name:
        for _ in range(9):
            x, y = 2 + next(r) % 12, 2 + next(r) % 12
            px[y][x] = (*accent, 255)
            px[y][min(15, x + 1)] = (*shade(accent, 0.85), 255)
    elif e.get("kind") == "block":
        for x in range(6, 10):  # Akzentstreifen (Statuslampe/Warnstreifen)
            px[2][x] = (*accent, 255)
        px[7][7] = px[8][8] = (*shade(accent, 0.8), 255)
    else:
        px[7][7] = px[8][8] = px[7][8] = px[8][7] = (*accent, 255)
    return png(px)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", help="kommagetrennte IDs ohne Namespace")
    args = ap.parse_args()
    only = set(args.only.split(",")) if args.only else None
    spec = sd.load()
    n = 0
    for e in spec.all_entries:
        if e.get("kind") not in ("item", "block"):
            continue
        name = e["id"].split(":")[1]
        if only and name not in only:
            continue
        folder = "block" if e["kind"] == "block" else "item"
        p = OUT / folder / f"{name}.png"
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(texture(e))
        n += 1
    print(f"{n} Platzhalter-Texturen erzeugt in {OUT.relative_to(sd.ROOT)}")


if __name__ == "__main__":
    main()
