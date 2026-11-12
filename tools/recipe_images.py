"""Rezeptbilder für das Wiki (SVG im Stil eines Minecraft-Fensters).

Eigene Items und Blöcke zeigen ihre Texturen aus common/src/generated/resources (eingebettet),
Vanilla-Items ein gezeichnetes Ersatzsymbol aus docs/handbook/handbook.json (`icons`), weil
Vanilla-Texturen nicht mitgeliefert werden dürfen. Aufruf über tools/handbook.py.
"""
from __future__ import annotations

import base64
import html
from pathlib import Path

import specdata as sd

TEX = sd.ROOT / "common" / "src" / "generated" / "resources" / "assets" / "infinite_horizon" / "textures"
FONT = "font-family=\"'Segoe UI','Helvetica Neue',Arial,sans-serif\""
SLOT = 36
PAD = 14
HEAD = 34

_cache: dict = {}


def shade(col: str, f: float) -> str:
    c = col.lstrip("#")
    r, g, b = (int(c[i:i + 2], 16) for i in (0, 2, 4))
    return "#%02X%02X%02X" % tuple(max(0, min(255, int(v * f))) for v in (r, g, b))


def texture(i: str):
    if i in _cache:
        return _cache[i]
    name = i.split(":", 1)[1]
    found = None
    for p in (TEX / "item" / f"{name}.png", TEX / "block" / f"{name}_front_running.png",
              TEX / "block" / f"{name}.png", TEX / "block" / f"{name}_still.png"):
        if p.exists():
            found = "data:image/png;base64," + base64.b64encode(p.read_bytes()).decode()
            break
    _cache[i] = found
    return found


def fluid_color(ctx, i: str) -> str:
    e = ctx.spec.items.get(i)
    if e and e.get("color"):
        return e["color"]
    ic = ctx.config.get("icons", {}).get(i)
    return ic[1] if ic else "#3F76E4"


def drawn_icon(shape: str, col: str, x: float, y: float) -> str:
    """Ersatzsymbol 32 × 32 px, pixelig gezeichnet."""
    d, l = shade(col, 0.65), shade(col, 1.25)

    def r(px, py, w, h, c):
        return f'<rect x="{x + px * 2}" y="{y + py * 2}" width="{w * 2}" height="{h * 2}" fill="{c}"/>'
    if shape == "ingot":
        return (r(2, 9, 12, 2, l) + r(1, 11, 14, 3, col) + r(1, 14, 14, 1, d) + r(3, 7, 10, 2, l))
    if shape == "raw":
        return (r(4, 4, 7, 3, l) + r(3, 7, 10, 4, col) + r(4, 11, 8, 2, d) + r(6, 8, 2, 2, d) + r(9, 5, 2, 2, col))
    if shape == "dust":
        return (r(6, 6, 4, 2, l) + r(4, 8, 8, 2, col) + r(2, 10, 12, 3, col) + r(2, 13, 12, 1, d) + r(5, 10, 1, 1, l)
                + r(9, 11, 1, 1, d))
    if shape == "gem":
        return (r(6, 3, 4, 2, l) + r(4, 5, 8, 3, col) + r(3, 8, 10, 2, col) + r(5, 10, 6, 2, d) + r(7, 12, 2, 1, d))
    if shape == "bucket":
        return (r(3, 5, 10, 2, d) + r(4, 5, 8, 2, col if col != "#C8C8C8" else shade(col, 0.8))
                + r(3, 7, 10, 6, "#C8C8C8") + r(4, 13, 8, 1, "#8A8A8A") + r(3, 7, 1, 6, "#E8E8E8"))
    if shape == "fluid":
        return r(2, 3, 12, 11, col) + r(2, 3, 12, 2, l) + r(4, 7, 3, 1, l) + r(9, 10, 3, 1, l)
    if shape == "block":
        top = f"{x + 16},{y + 3} {x + 29},{y + 9.5} {x + 16},{y + 16} {x + 3},{y + 9.5}"
        left = f"{x + 3},{y + 9.5} {x + 16},{y + 16} {x + 16},{y + 30} {x + 3},{y + 23.5}"
        right = f"{x + 16},{y + 16} {x + 29},{y + 9.5} {x + 29},{y + 23.5} {x + 16},{y + 30}"
        return (f'<polygon points="{top}" fill="{l}"/><polygon points="{left}" fill="{col}"/>'
                f'<polygon points="{right}" fill="{d}"/>')
    return r(3, 3, 10, 10, col) + r(3, 3, 10, 2, l) + r(3, 12, 10, 1, d)


def resolve(ctx, i: str) -> str:
    """Tag oder Alternativen auf ein darstellbares Item abbilden."""
    if i.startswith("#c:"):
        import re
        m = re.match(r"#c:(\w+)/(\w+)$", i)
        if m:
            form = {"plates": "plate", "ingots": "ingot", "rods": "rod", "wires": "wire", "dusts": "dust"}.get(m.group(1))
            cand = f"infinite_horizon:{m.group(2)}_{form}"
            if cand in ctx.spec.items:
                return cand
    return i


def icon(ctx, i: str, x: float, y: float) -> str:
    i = resolve(ctx, i)
    uri = texture(i) if i.startswith("infinite_horizon:") else None
    if uri:
        return (f'<image x="{x}" y="{y}" width="32" height="32" href="{uri}" '
                'style="image-rendering:pixelated;image-rendering:crisp-edges" preserveAspectRatio="none"/>')
    ic = ctx.config.get("icons", {}).get(i)
    if ic:
        return drawn_icon(ic[0], ic[1], x, y)
    return drawn_icon("item", "#9A9A9A", x, y)


def slot(x, y, size=SLOT):
    return (f'<rect x="{x}" y="{y}" width="{size}" height="{size}" fill="#8B8B8B"/>'
            f'<path d="M{x} {y + size} V{y} H{x + size}" stroke="#373737" stroke-width="2" fill="none"/>'
            f'<path d="M{x + size} {y} V{y + size} H{x}" stroke="#FFFFFF" stroke-width="2" fill="none"/>')


def count_text(x, y, n, size=SLOT):
    if n == 1:
        return ""
    t = str(n)
    return (f'<text x="{x + size - 2.5}" y="{y + size - 2.5}" text-anchor="end" {FONT} font-size="14" font-weight="700" '
            f'fill="#3F3F3F">{t}</text><text x="{x + size - 4}" y="{y + size - 4}" text-anchor="end" {FONT} '
            f'font-size="14" font-weight="700" fill="#FFFFFF">{t}</text>')


def item_slot(ctx, i, n, x, y, size=SLOT):
    off = (size - 32) / 2
    return slot(x, y, size) + icon(ctx, i, x + off, y + off) + count_text(x, y, n, size)


def tank(ctx, fluid, amount, x, y, h=SLOT * 2):
    col = fluid_color(ctx, fluid)
    fill = 0.8  # Anzeige: gut sichtbar, die Menge steht darunter
    fh = (h - 4) * fill
    from handbook import num  # noqa: PLC0415 (Zyklus vermeiden)
    return (f'<rect x="{x}" y="{y}" width="22" height="{h}" fill="#373737"/>'
            f'<rect x="{x + 2}" y="{y + 2}" width="18" height="{h - 4}" fill="#1E1E1E"/>'
            f'<rect x="{x + 2}" y="{y + 2 + (h - 4) - fh:.1f}" width="18" height="{fh:.1f}" fill="{col}"/>'
            f'<rect x="{x + 2}" y="{y + 2 + (h - 4) - fh:.1f}" width="18" height="3" fill="#FFFFFF" opacity="0.3"/>'
            f'<text x="{x + 11}" y="{y + h + 14}" text-anchor="middle" {FONT} font-size="11" fill="#3F3F3F">'
            f'{num(amount)} mB</text>')


def arrow(x, y, w=44):
    return (f'<path d="M{x} {y - 5} H{x + w - 14} V{y - 11} L{x + w} {y} L{x + w - 14} {y + 11} V{y + 5} H{x} Z" '
            'fill="#FFFFFF" stroke="#373737" stroke-width="2"/>')


def panel(w, h, title, sub):
    return [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}">',
            f'<rect x="1" y="1" width="{w - 2}" height="{h - 2}" rx="6" fill="#C6C6C6" stroke="#1E1E1E" stroke-width="2"/>',
            f'<path d="M4 {h - 4} V5 H{w - 5}" stroke="#FFFFFF" stroke-width="2" fill="none"/>',
            f'<path d="M{w - 4} 5 V{h - 4} H5" stroke="#555555" stroke-width="2" fill="none"/>',
            f'<text x="{PAD}" y="23" {FONT} font-size="15" font-weight="700" fill="#3F3F3F">{html.escape(title)}</text>',
            f'<text x="{w - PAD}" y="23" text-anchor="end" {FONT} font-size="12" fill="#555555">{html.escape(sub)}</text>']


def recipe_svg(ctx, r) -> str:
    from handbook import recipe_station, recipe_time, num  # noqa: PLC0415
    st = recipe_station(ctx, r)
    station = ctx.name(st["station"]) if st["station"].startswith("infinite_horizon:") else st["de"]
    ticks, ept = recipe_time(ctx, r)
    sub = station
    if r["type"] not in ("crafting_shaped", "crafting_shapeless", "smelting") and ticks:
        sub += f" · {num(round(ticks / 20, 1))} s" + (f" · {num(ept)} J/t" if ept else "")
    res = r["results"][0] if r["results"] else None
    title = ctx.name(res["id"]) if res else ctx.name(r["fluid_results"][0]["fluid"])

    body = []
    if r.get("pattern"):
        pat = [row.ljust(3) for row in r["pattern"]] + ["   "] * (3 - len(r["pattern"]))
        x0, y0 = PAD + 4, HEAD + 10
        for j, row in enumerate(pat[:3]):
            for k, c in enumerate(row[:3]):
                x, y = x0 + k * SLOT, y0 + j * SLOT
                body.append(item_slot(ctx, sd.ingredient_ids(r["key"][c])[0], 1, x, y) if c.strip() else slot(x, y))
        ax = x0 + 3 * SLOT + 14
        body.append(arrow(ax, y0 + 1.5 * SLOT))
        rx = ax + 58
        big = 48
        body.append(item_slot(ctx, res["id"], res["count"], rx, y0 + 1.5 * SLOT - big / 2, big))
        w, h = rx + big + PAD + 4, y0 + 3 * SLOT + PAD
        return "\n".join(panel(w, h, title, sub) + body + ["</svg>"])

    ings = [(sd.ingredient_ids(g)[0], g.get("count", 1)) for g in r.get("ingredients") or []]
    if r["type"] == "crafting_shapeless":
        pass
    cols = 3 if len(ings) > 3 else max(1, len(ings))
    rows = (len(ings) + cols - 1) // cols if ings else 0
    x, y0 = PAD + 4, HEAD + 10
    grid_h = max(rows * SLOT, SLOT)
    has_tank = bool(r.get("fluid")) or bool(r.get("fluid_results"))
    inner_h = max(grid_h, SLOT * 2 if has_tank else SLOT)
    cy = y0 + inner_h / 2
    if ings:
        gy = cy - rows * SLOT / 2
        for n, (i, c) in enumerate(ings):
            body.append(item_slot(ctx, i, c, x + (n % cols) * SLOT, gy + (n // cols) * SLOT))
        x += cols * SLOT + 8
    if r.get("mold"):
        body.append(item_slot(ctx, r["mold"], 1, x, cy - SLOT / 2))
        body.append(f'<text x="{x + SLOT / 2}" y="{cy + SLOT / 2 + 13}" text-anchor="middle" {FONT} font-size="11" '
                    'fill="#3F3F3F">Form</text>')
        x += SLOT + 8
    if r.get("fluid"):
        body.append(tank(ctx, r["fluid"]["fluid"], r["fluid"]["amount"], x, cy - SLOT))
        x += 30
    x += 6
    body.append(arrow(x, cy))
    x += 58
    outs = r["results"]
    for n, o in enumerate(outs):
        oy = cy - len(outs) * SLOT / 2 + n * SLOT
        body.append(item_slot(ctx, o["id"], o["count"], x, oy))
        if o.get("chance", 1) != 1:
            body.append(f'<text x="{x + SLOT + 4}" y="{oy + 22}" {FONT} font-size="11" fill="#3F3F3F">'
                        f'{num(round(o["chance"] * 100))} %</text>')
    if outs:
        x += SLOT + 10
    for f in r.get("fluid_results") or []:
        body.append(tank(ctx, f["fluid"], f["amount"], x, cy - SLOT))
        x += 46
    w = max(x + PAD, 300)
    h = y0 + inner_h + (22 if has_tank else 0) + PAD
    if r.get("mold"):
        h = max(h, cy + SLOT / 2 + 13 + PAD)
    return "\n".join(panel(int(w), int(h), title, sub) + body + ["</svg>"])


def image_name(rid: str) -> str:
    return rid.split(":", 1)[1].replace("/", "_") + ".svg"
