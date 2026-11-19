#!/usr/bin/env python3
"""Balancing-Übersicht (Kapitel 27, Abnahme M9): Rohstoffsumme je Rakete und je Werftabschnitt.

Liest alle Rezept-JSONs der Mod (``common/src/generated/resources/data/*/recipe``, erzeugt von gen_resources.py) und
rechnet jedes Bauteil auf Rohstoffe herunter. Regeln aus Kapitel 27:

- Rohstoff = Item oder Fluid ohne eigenes Rezept.
- Bei mehreren Rezepten zählt das erste; Rezepte sind alphabetisch nach ID sortiert.
- Nebenprodukte werden ignoriert: Ein Rezept liefert das gesuchte Ergebnis, alle anderen Ausgaben zählen nicht.
- Mengen werden durch die Ausgabemenge geteilt (2 Staub je Roherz → 0,5 Roherz je Staub).
- Fluide werden in mB summiert, Items in Stück.
- Pressformen (``mold``) werden ignoriert.
- Bei Tags zählt das erste Element des Tags (ein Tag ohne Datei hier bleibt als Rohstoff ``#tag`` stehen).

SPEC-GAP #123: Ein Kreis aus Rezepten (A aus B, B aus A) endet beim zweiten Auftreten; das Item gilt dann als Rohstoff.
Raketen sind die kleinste gültige Bauform je Stufe (rocket_parts.json: Kapsel, Triebwerk, Flossen, Mindestzahl Tanks)
und die Frachtraketen; Werftabschnitte stehen in shipyard.json.

Aufruf: ``python tools/balance_sheet.py`` (Tabelle), ``--json`` (maschinenlesbar).
"""
from __future__ import annotations

import argparse
import json
import sys
from fractions import Fraction
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "common" / "src" / "generated" / "resources" / "data"
SPEC = ROOT / "spec" / "data"
NS = "infinite_horizon"

FLUID = "fluid"
ITEM = "item"


# ---------------------------------------------------------------- Rezepte lesen

def _first(value):
    """Erstes Element einer Zutat: Text, Liste oder Objekt ({"item": …}, {"tag": …})."""
    if isinstance(value, list):
        return _first(value[0]) if value else None
    if isinstance(value, dict):
        if "item" in value:
            return value["item"]
        if "tag" in value:
            return "#" + value["tag"]
        if "id" in value:
            return value["id"]
        if "ingredient" in value:
            return _first(value["ingredient"])
        return None
    return value


def parse_recipe(doc: dict) -> tuple[list, list]:
    """(Eingaben, Ausgaben) eines Rezepts; jede Angabe ist (art, id, menge). Pressformen bleiben weg."""
    ins, outs = [], []
    t = doc.get("type", "")
    if t == "minecraft:crafting_shaped":
        key = doc.get("key", {})
        for row in doc.get("pattern", []):
            for ch in row:
                if ch != " " and ch in key:
                    ins.append((ITEM, _first(key[ch]), 1))
    elif t == "minecraft:crafting_shapeless":
        for ing in doc.get("ingredients", []):
            ins.append((ITEM, _first(ing), 1))
    elif "ingredient" in doc and not doc.get("ingredients"):
        # Ofen-Rezepte (smelting, blasting …)
        ins.append((ITEM, _first(doc["ingredient"]), doc.get("count", 1)))
    for ing in doc.get("ingredients", []) if t not in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless") else []:
        if isinstance(ing, dict) and "count" in ing:
            ins.append((ITEM, _first(ing.get("ingredient", ing)), ing["count"]))
        else:
            ins.append((ITEM, _first(ing), 1))
    fluid = doc.get("fluid")
    if isinstance(fluid, dict) and fluid.get("fluid"):
        ins.append((FLUID, fluid["fluid"], fluid.get("amount", 0)))
    result = doc.get("result")
    if isinstance(result, dict):
        outs.append((ITEM, result.get("id") or result.get("item"), result.get("count", 1)))
    elif isinstance(result, str):
        outs.append((ITEM, result, 1))
    for r in doc.get("results", []) or []:
        outs.append((ITEM, r.get("id") or r.get("item"), r.get("count", 1)))
    for r in doc.get("fluid_results", []) or []:
        outs.append((FLUID, r["fluid"], r.get("amount", 0)))
    ins = [(k, i, n) for k, i, n in ins if i]
    return ins, outs


def load_recipes(data: Path = DATA) -> list:
    """Alle Rezepte als (id, eingaben, ausgaben), alphabetisch nach ID."""
    out = []
    for f in sorted(data.glob("*/recipe/**/*.json")):
        ns = f.parent
        while ns.name != "recipe":
            ns = ns.parent
        rid = f"{ns.parent.name}:{f.relative_to(ns).with_suffix('').as_posix()}"
        doc = json.loads(f.read_text(encoding="utf-8"))
        ins, outs = parse_recipe(doc)
        out.append((rid, ins, outs))
    out.sort(key=lambda r: r[0])
    return out


def load_tags(data: Path = DATA) -> dict:
    """Item-Tags dieser Mod: "#ns:pfad" → Werte."""
    tags = {}
    for f in data.glob("*/tags/item/**/*.json"):
        base = f.parents[0]
        while base.name != "item":
            base = base.parent
        name = f"#{base.parent.parent.name}:{f.relative_to(base).with_suffix('').as_posix()}"
        tags[name] = json.loads(f.read_text(encoding="utf-8")).get("values", [])
    return tags


# ---------------------------------------------------------------- Rechnen

class Sheet:
    """Löst Bauteile in Rohstoffe auf (Regeln oben)."""

    def __init__(self, recipes: list, tags: dict):
        self.tags = tags
        self.producer = {}
        for rid, ins, outs in recipes:  # schon alphabetisch: das erste Rezept gewinnt
            for kind, oid, amount in outs:
                if amount and (kind, oid) not in self.producer:
                    self.producer[(kind, oid)] = (rid, ins, amount)

    def resolve_tag(self, name: str, seen=None) -> str:
        seen = seen or set()
        while name.startswith("#") and name in self.tags and name not in seen:
            seen.add(name)
            values = self.tags[name]
            if not values:
                break
            v = values[0]
            name = v.get("id") if isinstance(v, dict) else v
        return name

    def raw(self, kind: str, oid: str, amount, out: dict, stack=()):
        """Fügt ``amount`` von ``oid`` als Rohstoffe zu ``out`` hinzu."""
        if kind == ITEM and oid.startswith("#"):
            oid = self.resolve_tag(oid)
        key = (kind, oid)
        rec = self.producer.get(key)
        if rec is None or key in stack:
            out[key] = out.get(key, Fraction(0)) + Fraction(amount)
            return
        _, ins, per = rec
        factor = Fraction(amount) / Fraction(per)
        for k, iid, n in ins:
            self.raw(k, iid, factor * n, out, stack + (key,))

    def bill(self, parts: list) -> dict:
        """Rohstoffe für eine Liste von (art, id, menge)."""
        out = {}
        for kind, oid, amount in parts:
            self.raw(kind, oid, amount, out)
        return out


def rockets(spec: Path = SPEC) -> dict:
    """Kleinste gültige Rakete je Stufe und die Frachtraketen."""
    rp = json.loads((spec / "rocket_parts.json").read_text(encoding="utf-8"))
    parts = {p["block"] for p in rp.get("parts", [])}
    out = {}
    for rule in rp.get("rules", []):
        t = rule["tier"]
        need = [(f"{NS}:rocket_capsule_t{t}", 1), (f"{NS}:rocket_engine_t{t}", 1),
                (f"{NS}:rocket_fin_t{t}", rule.get("fins", 0)), (f"{NS}:rocket_tank_t{t}", rule.get("tanks", [1])[0])]
        if all(p in parts for p, _ in need):
            out[f"Rakete T{t}"] = [(ITEM, p, n) for p, n in need]
    for name, rid in (("Frachtrakete", f"{NS}:cargo_rocket"), ("Schwerlast-Frachter", f"{NS}:heavy_freighter")):
        out[name] = [(ITEM, rid, 1)]
    return out


def sections(spec: Path = SPEC) -> dict:
    doc = json.loads((spec / "balance" / "shipyard.json").read_text(encoding="utf-8"))["entries"]
    out = {}
    for k in sorted((k for k in doc if k.isdigit()), key=int):
        mats = []
        for m in doc[k].get("materials", []):
            if m.get("fluid"):
                mats.append((FLUID, m["fluid"], m.get("amount", 0)))
            else:
                mats.append((ITEM, m["id"], m.get("count", 1)))
        out[f"Werftabschnitt {k} ({doc[k].get('name_de', '')})"] = mats
    return out


def fmt(x: Fraction) -> str:
    if x.denominator == 1:
        return f"{x.numerator:,}".replace(",", ".")
    return f"{float(x):,.2f}".replace(",", "X").replace(".", ",").replace("X", ".")


def report(sheet: Sheet, groups: dict) -> dict:
    out = {}
    for name, parts in groups.items():
        bill = sheet.bill(parts)
        out[name] = {f"{oid} ({'mB' if kind == FLUID else 'Stück'})": v
                     for (kind, oid), v in sorted(bill.items(), key=lambda kv: (kv[0][0] != ITEM, kv[0][1]))}
    return out


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--json", action="store_true", help="Ausgabe als JSON")
    args = ap.parse_args(argv)
    if not any(DATA.glob("*/recipe")):
        print("Keine Rezepte gefunden – erst python tools/gen_resources.py ausführen", file=sys.stderr)
        return 1
    sheet = Sheet(load_recipes(), load_tags())
    groups = {**rockets(), **sections()}
    rep = report(sheet, groups)
    total = {}
    for name, bill in rep.items():
        if name.startswith("Werftabschnitt"):
            for k, v in bill.items():
                total[k] = total.get(k, Fraction(0)) + v
    rep["Sternenschiff (alle Werftabschnitte)"] = dict(sorted(total.items()))
    if args.json:
        print(json.dumps({g: {k: float(v) for k, v in b.items()} for g, b in rep.items()}, ensure_ascii=False, indent=2))
        return 0
    for name, bill in rep.items():
        print(f"## {name}")
        width = max((len(k) for k in bill), default=0)
        for k, v in bill.items():
            print(f"  {k.ljust(width)}  {fmt(v):>14}")
        print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
