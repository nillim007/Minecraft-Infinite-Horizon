"""Erzeugt 16×16-Platzhalter-Texturen für alle Items und Blöcke aus spec/data/items.json (Kapitel 24.5).

Aufruf:  python tools/gen_placeholders.py [--only id1,id2,...]
Ausgabe: common/src/generated/resources/assets/infinite_horizon/textures/{block,item}/<id>.png
Nur Python-Standardbibliothek. Bestehende Dateien werden überschrieben (die Ausgabe ist vollständig generiert).
"""
from __future__ import annotations

import argparse
import re
import hashlib
import struct
import sys
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import specdata as sd  # noqa: E402

OUT = sd.ROOT / "common" / "src" / "generated" / "resources" / "assets" / "infinite_horizon" / "textures"

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
            # nur ganze Namensteile: "titan" darf nicht in "titanium_ore" treffen
            if re.search(rf"(^|_){re.escape(k)}(_|$)", name):
                return rgb(PLANET[k]), rgb(metal_of(name) or PLANET[k])
    if cat == "ore" or name.endswith("_ore"):
        # Erz der Erde: Gestein (Stein bzw. Tiefenschiefer) mit Sprenkeln in der Metallfarbe (24.5)
        host = "#4D4D55" if name.startswith("deepslate_") else "#7F7F7F"
        m = metal_of(name)
        if m:
            return rgb(host), rgb(m)
    if cat in ("material", "pipe"):  # Rohre: Metallfarbe (copper_pipe -> Kupfer)
        m = metal_of(name)
        if m:
            return rgb(m), shade(rgb(m), 1.25)
    base, accent = PHASE.get(e.get("phase") or 1, PHASE[1])
    return rgb(base), rgb(accent)


def texture_px(e):
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
    return px


def texture(e):
    return png(texture_px(e))


LAMPS = {"idle": "#F0C020", "running": "#40D040", "error": "#E04030"}


def machine_top(e):
    px = [row[:] for row in texture_px(e)]
    for y in range(16):
        for x in range(16):
            if x not in (0, 15) and y not in (0, 15):
                px[y][x] = (*shade(px[y][x][:3], 1.15), 255)
    return png(px)


def machine_front(e, status):
    px = [row[:] for row in texture_px(e)]
    for y in range(3, 13):
        for x in range(3, 13):
            c = shade(px[y][x][:3], 0.55)
            if x in (3, 12) or y in (3, 12):
                c = shade(c, 0.6)
            px[y][x] = (*c, 255)
    for y in range(4, 8):
        for x in range(8, 12):
            px[y][x] = (*rgb(LAMPS[status]), 255)
    return png(px)


# Ports (4.3): Eingang blau, Ausgang orange (Rahmen um ein Feld in der Mitte)
PORT_MODES = {"input": "#3F76E4", "output": "#F08020"}


def port_texture(e, mode):
    px = [row[:] for row in texture_px(e)]
    c = rgb(PORT_MODES[mode])
    for y in range(4, 12):
        for x in range(4, 12):
            frame = x in (4, 11) or y in (4, 11)
            px[y][x] = (*(c if frame else shade(c, 0.6)), 255)
    return png(px)


def fluid_texture(e, flow):
    """Fluidtextur: einfarbig in der Fluidfarbe mit leichtem Rauschen (still bzw. fliessend, 16x16)."""
    base = rgb(e["color"])
    r = rng(e["id"] + (":flow" if flow else ":still"))
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            noise = (next(r) % 13 - 6) / 100
            if flow and (x + y) % 8 == 0:
                noise += 0.06  # schwache Streifen fuer die Fliessrichtung
            row.append((*shade(base, 1 + noise), 255))
        px.append(row)
    return png(px)


def bucket_texture(e):
    """Eimer: grauer Eimer, Inhalt in der Fluidfarbe."""
    base = rgb(e["color"])
    r = rng(e["id"] + ":bucket")
    iron = rgb("#9A9A9A")
    px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(3, 14):
        inset = (y - 3) // 4  # Eimer verjuengt sich nach unten
        for x in range(3 + inset, 13 - inset):
            edge = x in (3 + inset, 12 - inset) or y in (3, 13)
            c = shade(iron, 0.7 if edge else 1 + (next(r) % 11 - 5) / 100)
            if y in (4, 5, 6) and not edge:
                c = shade(base, 1 + (next(r) % 9 - 4) / 100)  # Fuellung
            px[y][x] = (*c, 255)
    return png(px)


def block_ids(shape):
    import json
    impl = json.loads((sd.ROOT / "common" / "implemented_ids.json").read_text(encoding="utf-8"))
    return {i.split(":")[1] for i, k in impl["blocks"].items() if k == shape}


def machine_ids():
    return block_ids("machine")


FORCE = False


def png_sized(pixels):
    """PNG beliebiger Größe (Zeilen aus RGBA-Tupeln)."""
    h, w = len(pixels), len(pixels[0])
    raw = b"".join(b"\x00" + bytes(v for px in row for v in px) for row in pixels)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


# 24.5 "Weitere Platzhalter ab M3": Erde am Himmel 32 x 32 als Scheibe #3F76E4 mit Rauschen
def celestial_disc(color, size, seed):
    r = rng(seed)
    c = rgb(color)
    out = []
    for y in range(size):
        row = []
        for x in range(size):
            dx, dy = x - (size - 1) / 2, y - (size - 1) / 2
            if dx * dx + dy * dy > (size / 2) ** 2:
                row.append((0, 0, 0, 0))
            else:
                f = 0.8 + (next(r) % 40) / 100
                row.append(shade(c, f) + (255,))
        out.append(row)
    return png_sized(out)


# Atemmaske 64 x 32 (Zombie: 64 x 64 wie seine Textur), Kopfbereich #6E6E6E: untere Hälfte der vier Kopfseiten
MASK_MODELS = {"minecraft:zombie": ((64, 64), (0, 0)), "minecraft:skeleton": ((64, 32), (0, 0)),
               "minecraft:creeper": ((64, 32), (0, 0)), "minecraft:spider": ((64, 32), (32, 4))}


def breathing_mask(entity):
    (w, h), (u, v) = MASK_MODELS.get(entity, ((64, 32), (0, 0)))
    gray = rgb("#6E6E6E") + (255,)
    filt = shade(rgb("#6E6E6E"), 0.6) + (255,)
    out = [[(0, 0, 0, 0)] * w for _ in range(h)]
    for y in range(v + 12, v + 16):          # untere Hälfte des 8 x 8 x 8-Kopfs (Seitenflächen ab v + 8)
        for x in range(u, u + 32):
            out[y][x] = gray
    for y in range(v + 13, v + 15):          # Filter vorn
        for x in range(u + 10, u + 14):
            out[y][x] = filt
    return png_sized(out)


# Anzug-Schichten (Equipment-Asset, 64 x 32) in #E8E8E8 mit Akzent #E07B20 (24.5)
def suit_layer(seed):
    r = rng(seed)
    base, accent = rgb("#E8E8E8"), rgb("#E07B20")
    out = []
    for y in range(32):
        row = []
        for x in range(64):
            c = accent if (y % 8 == 7 or x % 16 == 0) else base
            row.append(shade(c, 0.9 + (next(r) % 20) / 100) + (255,))
        out.append(row)
    return png_sized(out)


def put(path, data):
    """Schreibt eine Platzhalter-Textur; vorhandene Dateien (z. B. fertige Grafiken) bleiben ohne --force erhalten."""
    if path.exists() and not FORCE:
        return 0
    path.write_bytes(data)
    return 1


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", help="kommagetrennte IDs ohne Namespace")
    ap.add_argument("--force", action="store_true", help="vorhandene Texturen überschreiben (sonst bleiben sie)")
    args = ap.parse_args()
    global FORCE
    FORCE = args.force
    only = set(args.only.split(",")) if args.only else None
    spec = sd.load()
    n = 0
    machines = machine_ids()
    ports = block_ids("port")
    for e in spec.all_entries:
        name = e["id"].split(":")[1]
        if e.get("kind") == "fluid" and e.get("color"):
            if only and name not in only:
                continue
            (OUT / "block").mkdir(parents=True, exist_ok=True)
            put(OUT / "block" / f"{name}_still.png", fluid_texture(e, False))
            put(OUT / "block" / f"{name}_flow.png", fluid_texture(e, True))
            n += 2
            continue
        if e.get("kind") not in ("item", "block"):
            continue
        if only and name not in only:
            continue
        if name.endswith("_bucket") and f"infinite_horizon:{name[:-7]}" in spec.items:
            fl = spec.items[f"infinite_horizon:{name[:-7]}"]
            if fl.get("kind") == "fluid" and fl.get("color"):
                (OUT / "item").mkdir(parents=True, exist_ok=True)
                put(OUT / "item" / f"{name}.png", bucket_texture(fl))
                n += 1
                continue
        folder = "block" if e["kind"] == "block" else "item"
        p = OUT / folder / f"{name}.png"
        p.parent.mkdir(parents=True, exist_ok=True)
        put(p, texture(e))
        n += 1
        if folder == "block" and name in machines:
            put(p.parent / f"{name}_top.png", machine_top(e))
            for st in LAMPS:
                put(p.parent / f"{name}_front_{st}.png", machine_front(e, st))
            n += 4
        if folder == "block" and name in ports:
            for mode in PORT_MODES:
                put(p.parent / f"{name}_{mode}.png", port_texture(e, mode))
            n += 2
    # Himmelskörper und Atemmasken der Planeten in implemented_ids.json (24.5, ab M3)
    import json as _json
    impl = _json.loads((sd.ROOT / "common" / "implemented_ids.json").read_text(encoding="utf-8"))
    for dim in impl.get("dimensions", []):
        p = spec.planets[dim]
        for b in (p.get("sky") or {}).get("celestial_bodies", []):
            name = b["texture"].rsplit("/", 1)[-1].removesuffix(".png")
            if only and name not in only:
                continue
            (OUT / "environment" / "celestial").mkdir(parents=True, exist_ok=True)
            n += put(OUT / "environment" / "celestial" / f"{name}.png", celestial_disc("#3F76E4", 32, name))
        for mob in p.get("vanilla_mobs", []):
            name = mob["entity"].split(":", 1)[1]
            if only and f"breathing_mask_{name}" not in only:
                continue
            (OUT / "entity" / "breathing_mask").mkdir(parents=True, exist_ok=True)
            n += put(OUT / "entity" / "breathing_mask" / f"{name}.png", breathing_mask(mob["entity"]))
    registered = set(impl.get("items", []))
    for sid, e in spec.balance.get("suits", {}).items():
        if not isinstance(e, dict) or "pieces" not in e or not set(e["pieces"]) <= registered:
            continue
        if only and f"equipment_{sid}" not in only:
            continue
        for layer in ("humanoid", "humanoid_leggings"):
            (OUT / "entity" / "equipment" / layer).mkdir(parents=True, exist_ok=True)
            n += put(OUT / "entity" / "equipment" / layer / f"{sid}.png", suit_layer(sid + layer))
    print(f"{n} Platzhalter-Texturen erzeugt in {OUT.relative_to(sd.ROOT)}")


if __name__ == "__main__":
    main()
