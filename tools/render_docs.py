"""Erzeugt lesbare Tabellen aus spec/data nach docs/spec/generated/.

Aufruf:  python tools/render_docs.py
Die erzeugten Dateien nie von Hand ändern; Quelle ist spec/data.
"""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import specdata as sd  # noqa: E402

OUT = sd.ROOT / "docs" / "spec" / "generated"
HEADER = "<!-- Erzeugt von tools/render_docs.py aus spec/data. Nicht von Hand ändern. -->\n\n"


def short(i):
    if not isinstance(i, str):
        return str(i)
    return i.replace(sd.MOD, "")


def code(i):
    return f"`{short(i)}`"


def num(v):
    if isinstance(v, float) and v.is_integer():
        v = int(v)
    if isinstance(v, int):
        return f"{v:,}".replace(",", ".")
    if isinstance(v, float):
        return f"{v:g}".replace(".", ",")
    return "–" if v is None else str(v)


def table(headers, rows):
    out = ["| " + " | ".join(headers) + " |", "| " + " | ".join("---" for _ in headers) + " |"]
    for r in rows:
        out.append("| " + " | ".join(str(c).replace("|", "\\|").replace("\n", " ") for c in r) + " |")
    return "\n".join(out) + "\n"


def write(name, title, body):
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / name).write_text(HEADER + f"# {title}\n\n" + body, encoding="utf-8")
    print(f"docs/spec/generated/{name}")


def ingredient_text(ing):
    ids = sd.ingredient_ids(ing)
    name = " oder ".join(code(i) for i in ids)
    comp = f" {ing['components']}" if ing.get("components") else ""
    return f"{ing.get('count', 1)} {name}{comp}"


def render_items(spec):
    groups = defaultdict(list)
    for e in spec.all_entries:
        groups[(e.get("kind"), e.get("category"))].append(e)
    body = ""
    for (kind, cat), entries in sorted(groups.items(), key=lambda kv: (str(kv[0][0]), str(kv[0][1]))):
        body += f"## {kind} · {cat}\n\n"
        rows = [(code(e["id"]), e.get("name_de", ""), e.get("name_en", ""), num(e.get("phase")),
                 ", ".join(e.get("obtained_by") or []), e.get("spec_ref", "")) for e in sorted(entries, key=lambda x: x["id"])]
        body += table(["ID", "Deutsch", "Englisch", "Phase", "Herkunft", "Kapitel"], rows) + "\n"
    write("items.md", f"IDs und Namen ({len(spec.all_entries)})", body)


def render_recipes(spec):
    groups = defaultdict(list)
    for r in spec.recipes:
        groups[r.get("type")].append(r)
    body = ""
    for typ, rs in sorted(groups.items()):
        body += f"## {typ} ({len(rs)})\n\n"
        rows = []
        for r in sorted(rs, key=lambda x: x.get("id", "")):
            res = ", ".join(f"{x.get('count', 1)} {code(x['id'])}" + (f" ({int(x['chance'] * 100)} %)" if x.get("chance", 1) < 1 else "")
                            for x in r.get("results") or [])
            fres = ", ".join(f"{num(x.get('amount'))} mB {code(x['fluid'])}" for x in r.get("fluid_results") or [])
            if r.get("pattern"):
                key = ", ".join(f"{k} = {code(sd.ingredient_ids(v)[0])}" for k, v in (r.get("key") or {}).items())
                ins = " / ".join(f"`{line}`" for line in r["pattern"]) + f" ({key})"
            else:
                ins = ", ".join(ingredient_text(i) for i in r.get("ingredients") or [])
            if r.get("fluid"):
                ins += f" + {num(r['fluid'].get('amount'))} mB {code(r['fluid'].get('fluid'))}"
            if r.get("mold"):
                ins += f" (Form {code(r['mold'])})"
            out = ", ".join(x for x in (res, fres) if x)
            time = f"{num(r.get('ticks'))} t · {num(r.get('energy_per_tick'))} J/t" if r.get("ticks") else "–"
            rows.append((out, ins, time, r.get("spec_ref", "")))
        body += table(["Ergebnis", "Zutaten", "Zeit · Energie", "Kapitel"], rows) + "\n"
    write("recipes.md", f"Rezepte ({len(spec.recipes)})", body)


def render_research(spec):
    body = "## Stufen\n\n"
    rows = [(t.get("tier"), t.get("name_de", ""), num(t.get("cost_default")), f"`{t.get('gate')}`") for t in sorted(spec.tiers, key=lambda x: x.get("tier", 0))]
    body += table(["Stufe", "Name", "Kosten je Knoten", "Tor"], rows) + "\n"
    for tier in range(1, 7):
        nodes = [n for n in spec.nodes if n.get("tier") == tier]
        body += f"## Stufe {tier} ({len(nodes)} Knoten)\n\n"
        rows = []
        for n in sorted(nodes, key=lambda x: (x.get("branch", ""), x.get("id"))):
            unl = ", ".join((code(u["id"]) if u.get("type") == "item" else f"{u.get('type')}: {code(u.get('id'))}") for u in n.get("unlocks", []))
            rows.append((code(n["id"]), n.get("name_de", ""), n.get("branch", ""), num(n.get("cost")),
                         ", ".join(code(x) for x in n.get("requires", [])) or "–", unl))
        body += table(["Knoten", "Name", "Ast", "Kosten", "Voraussetzung", "Schaltet frei"], rows) + "\n"
    write("research.md", "Forschung", body)


def render_planets(spec):
    rows = []
    for pid, p in sorted(spec.planets.items(), key=lambda kv: (kv[1].get("distance_points", 0), kv[0])):
        t = p.get("temperature") or {}
        rows.append((code(pid), p.get("name_de", ""), p.get("type", ""), p.get("ring"), num(p.get("distance_points")),
                     num(p.get("gravity")), p.get("atmosphere", ""), p.get("pressure", ""),
                     f"{num(t.get('night'))} / {num(t.get('day'))}", p.get("radiation"), num(p.get("solar_factor")),
                     p.get("required_rocket_tier"), "ja" if p.get("requires_long_range") else "nein"))
    body = table(["Ziel", "Name", "Typ", "Ring", "DP", "g", "Atmosphäre", "Druck", "Temp. Nacht/Tag", "Strahlung",
                  "Solar", "Rakete", "Fernflug"], rows) + "\n"
    for pid, p in sorted(spec.planets.items(), key=lambda kv: (kv[1].get("distance_points", 0), kv[0])):
        if not (p.get("biomes") or p.get("ores") or p.get("structures") or p.get("zones")):
            continue
        body += f"## {p.get('name_de', pid)} ({code(pid)})\n\n"
        if p.get("zones"):
            body += "**Zonen:** " + "; ".join(
                ", ".join(f"{k} {v}" for k, v in z.items() if v is not None and k != "spec_ref") for z in p["zones"]) + "\n\n"
        if p.get("biomes"):
            body += table(["Biom", "Name", "Oberfläche", "Form", "Häufigkeit"],
                          [(code(b.get("id")), b.get("name_de", ""), code(b.get("surface")), b.get("shape", ""), b.get("rarity", ""))
                           for b in p["biomes"]]) + "\n"
        if p.get("ores"):
            body += table(["Erz", "y", "Ader", "je Chunk", "Drop"],
                          [(code(o.get("block")), f"{num(o.get('y_min'))}–{num(o.get('y_max'))}", num(o.get("vein_size")),
                            num(o.get("per_chunk")), ", ".join(code(d.get("id")) for d in sd.drop_list(o.get("drops"))))
                           for o in p["ores"]]) + "\n"
        if p.get("structures"):
            body += table(["Struktur", "Erkundungsziel", "Hinweis"],
                          [(code(s.get("id")), "ja" if s.get("exploration_target") else "nein", s.get("notes") or "")
                           for s in p["structures"]]) + "\n"
    write("planets.md", "Ziele und Planeten", body)


def render_fuel(spec):
    rows = [(r["name"], code(r["id"]), num(round(r["from_earth"])),
             num(round(r["from_station"])) if r["from_station"] is not None else "–", r["min_config"])
            for r in sd.fuel_table(spec)]
    c = sd.fuel_constants(spec)
    body = (f"Formel (Kapitel 12.2): Start = {num(c.launch_per_g)} × g, Transfer = {num(c.transfer_per_dp)} × |ΔDP| "
            f"+ {num(c.transfer_body_change)} bei Körperwechsel, Landung = {num(c.landing_factor)} × {num(c.launch_per_g)} × g. "
            "Werte je Treibstoffkomponente in mB, Hin- und Rückflug.\n\n")
    body += table(["Ziel", "ID", "ab Erdoberfläche", "ab Station im Erdorbit", "Mindestausstattung ab Erde"], rows)
    write("fuel_table.md", "Treibstoff für Hin- und Rückflug", body)


def render_creatures(spec):
    rows = []
    for cid, c in sorted(spec.balance.get("creatures", {}).items()):
        if not isinstance(c, dict) or not cid.startswith(sd.MOD):
            continue
        spawn = c.get("spawn") or {}
        if isinstance(spawn, list):
            spawn = spawn[0] if spawn else {}
        drops = ", ".join(f"{num(d.get('count_min', d.get('min', d.get('count'))))}–{num(d.get('count_max', d.get('max', d.get('count'))))} {code(d.get('id') or d.get('item'))}"
                          for d in c.get("drops") or [])
        rows.append((code(cid), num(c.get("hp")), num(c.get("damage")), num(c.get("speed")),
                     f"{num(c.get('width'))} × {num(c.get('height'))}", code(spawn.get("dimension")), drops or "–"))
    write("creatures.md", "Kreaturen", table(["Kreatur", "HP", "Schaden", "Tempo", "Größe", "Dimension", "Drops"], rows))


def render_shipyard(spec):
    rows = []
    for key, s in sorted(spec.balance.get("shipyard", {}).items(), key=lambda kv: str(kv[0])):
        if not isinstance(s, dict) or "materials" not in s:
            continue
        mats = ", ".join((f"{num(m.get('count'))} {code(m.get('id'))}" if m.get("id") else f"{num(m.get('amount'))} mB {code(m.get('fluid'))}")
                         + (f" {m['components']}" if m.get("components") else "") for m in s["materials"])
        rows.append((key, s.get("name_de", ""), mats, num(s.get("build_ticks"))))
    write("shipyard.md", "Werft-Bauabschnitte", table(["#", "Abschnitt", "Material", "Bauzeit (t)"], rows))


def main():
    spec = sd.load()
    for e in spec.load_errors:
        print(f"Hinweis: {e}")
    if spec.items:
        render_items(spec)
    if spec.recipes:
        render_recipes(spec)
    if spec.nodes:
        render_research(spec)
    if spec.planets:
        render_planets(spec)
        render_fuel(spec)
    if spec.balance.get("creatures"):
        render_creatures(spec)
    if spec.balance.get("shipyard"):
        render_shipyard(spec)


if __name__ == "__main__":
    main()
