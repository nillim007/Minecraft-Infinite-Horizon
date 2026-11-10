"""Prüft spec/data auf Vollständigkeit und Widerspruchsfreiheit.

Aufruf:  python tools/validate_spec.py [--quiet]
Ende mit Code 1, wenn Fehler gefunden wurden. Warnungen brechen nicht ab.
"""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import specdata as sd  # noqa: E402

errors: list[str] = []
warnings: list[str] = []


def err(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


def is_mod(i):
    return isinstance(i, str) and i.startswith(sd.MOD)


def known(spec, i):
    if not isinstance(i, str):
        return False
    if i.startswith("#") or i.startswith("minecraft:"):
        return True
    return i in spec.items


# ---------------------------------------------------------------- items

def check_items(spec):
    for iid, e in spec.items.items():
        if not is_mod(iid):
            err(f"items.json: ID ohne Namespace sternenwerft: {iid}")
        for f in ("kind", "name_de", "name_en", "obtained_by", "spec_ref"):
            if e.get(f) in (None, "", []):
                err(f"items.json: {iid} hat kein Feld '{f}'")
        if e.get("kind") == "fluid":
            for f in ("fluid_type", "color"):
                if e.get(f) is None:
                    err(f"items.json: Fluid {iid} hat kein Feld '{f}'")


# ---------------------------------------------------------------- recipes

def check_recipes(spec):
    seen = set()
    valid_types = {"crafting_shaped", "crafting_shapeless", "smelting", "electric_smelting", "crushing", "pressing",
                   "high_temp_smelting", "refining", "electrolysis", "cryo", "regolith_heating", "zero_g", "assembly"}
    for r in spec.recipes:
        rid = r.get("id")
        if rid in seen:
            err(f"recipes.json: doppelte Rezept-ID {rid}")
        seen.add(rid)
        if r.get("type") not in valid_types:
            err(f"recipes.json: {rid} hat unbekannten Typ {r.get('type')}")
        if not r.get("spec_ref"):
            warn(f"recipes.json: {rid} ohne spec_ref")
        if not sd.recipe_results(r):
            err(f"recipes.json: {rid} hat kein Ergebnis")
        for i in sd.recipe_inputs(r) + sd.recipe_results(r):
            if not known(spec, i):
                err(f"recipes.json: {rid} nutzt unbekannte ID {i}")
        if r.get("type") == "assembly":
            slots = sd.assembly_slots(r)
            if slots > 9:
                err(f"recipes.json: {rid} braucht {slots} Slots (max. 9)")
            fl = r.get("fluid")
            if fl and fl.get("amount", 0) > 16_000:
                err(f"recipes.json: {rid} braucht {fl['amount']} mB Fluid (max. 16.000)")


def producers(spec):
    out = defaultdict(list)
    for r in spec.recipes:
        for res in sd.recipe_results(r):
            out[res].append(r)
    return out


def all_ids(obj) -> set[str]:
    """Alle sternenwerft-IDs, die irgendwo in einem verschachtelten Objekt als Text vorkommen."""
    out, stack = set(), [obj]
    while stack:
        cur = stack.pop()
        if isinstance(cur, dict):
            stack.extend(cur.keys())
            stack.extend(cur.values())
        elif isinstance(cur, list):
            stack.extend(cur)
        elif isinstance(cur, str) and cur.startswith(sd.MOD):
            out.add(cur)
    return out


def balance_ids(spec) -> set[str]:
    return set().union(*(all_ids(v) for v in spec.balance.values())) if spec.balance else set()


def world_sources(spec):
    """IDs, die aus Worldgen, Block-Drops, Mob-Drops oder Weltobjekten (Quellen, Geysire) kommen."""
    src = set()
    for p in spec.planets.values():
        src |= all_ids(p)
    for name in ("items", "creatures", "events"):
        src |= all_ids(spec.balance.get(name, {}))
    for p in spec.planets.values():
        for b in p.get("blocks", []) or []:
            src.add(b.get("id"))
            drops = b.get("drops")
            if isinstance(drops, list):
                src.update(d.get("id") for d in drops)
        for o in p.get("ores", []) or []:
            src.add(o.get("block"))
            src.update(d.get("id") for d in sd.drop_list(o.get("drops")))
        src.add(p.get("default_block"))
    for c in spec.balance.get("creatures", {}).values():
        for d in c.get("drops", []) or []:
            src.add(d.get("id") or d.get("item"))
    return {s for s in src if s}


def check_obtainable(spec):
    prod = producers(spec)
    world = world_sources(spec)
    # Maschinenfunktionen ohne Rezept (Gas-Sammler, Pumpstation, Nahrungspacker …) stehen in den Balance-Dateien.
    machine_functions = balance_ids(spec)
    used = defaultdict(list)
    for r in spec.recipes:
        for i in sd.recipe_inputs(r):
            used[i].append(r.get("id"))
    for iid, e in spec.items.items():
        kind = e.get("kind")
        if kind not in ("item", "block", "fluid"):
            continue
        how = set(e.get("obtained_by") or [])
        if "recipe" in how and iid not in prod:
            err(f"{iid}: laut items.json per Rezept erhältlich, aber kein Rezept erzeugt es")
        if "machine_output" in how and iid not in prod and iid not in machine_functions:
            warn(f"{iid}: laut items.json Maschinenausgabe, aber weder ein Rezept noch eine Balance-Datei nennt es")
        if how & {"worldgen", "block_drop", "mob_drop"} and iid not in world and iid not in prod:
            warn(f"{iid}: laut items.json aus Welt oder Drop, aber in planets.json/creatures.json nicht gefunden")
    for i, rids in used.items():
        if is_mod(i) and i in spec.items:
            how = set(spec.items[i].get("obtained_by") or [])
            if not how or how <= {"none", "not_obtainable"}:
                err(f"{i}: wird in {rids[0]} gebraucht, ist aber nicht erhältlich")
    for iid, e in spec.items.items():
        if e.get("category") in ("material", "component") and iid not in used and iid not in machine_functions:
            warn(f"{iid}: Zwischenprodukt ohne Verwendung in Rezepten, Werft oder Balance-Dateien")


# ---------------------------------------------------------------- research

def check_research(spec):
    ids = {n.get("id"): n for n in spec.nodes}
    if len(ids) != len(spec.nodes):
        err("research.json: doppelte Knoten-IDs")
    tiers = {t.get("tier") for t in spec.tiers}
    if tiers != set(range(1, 7)):
        err(f"research.json: Stufen sind {sorted(tiers)}, erwartet 1–6")
    for n in spec.nodes:
        for req in n.get("requires", []):
            if req not in ids:
                err(f"research.json: {n['id']} verlangt unbekannten Knoten {req}")
            elif ids[req].get("tier", 0) > n.get("tier", 0):
                err(f"research.json: {n['id']} (Stufe {n['tier']}) verlangt {req} aus höherer Stufe {ids[req]['tier']}")
        for u in n.get("unlocks", []):
            if u.get("type") == "item" and not known(spec, u.get("id")):
                err(f"research.json: {n['id']} schaltet unbekannte ID {u.get('id')} frei")
            if u.get("type") == "probe_target" and u.get("id") not in spec.planets:
                err(f"research.json: {n['id']} hat unbekanntes Sondenziel {u.get('id')}")
    # Zyklen
    state = {}

    def visit(nid, path):
        if state.get(nid) == 1:
            err(f"research.json: Zyklus {' → '.join(path + [nid])}")
            return
        if state.get(nid) == 2:
            return
        state[nid] = 1
        for req in ids.get(nid, {}).get("requires", []):
            if req in ids:
                visit(req, path + [nid])
        state[nid] = 2

    for nid in ids:
        visit(nid, [])
    # Stufen der Zutaten
    unlock_tier = {}
    for n in spec.nodes:
        for u in n.get("unlocks", []):
            if u.get("type") == "item":
                prev = unlock_tier.get(u["id"])
                if prev is not None and prev != n["tier"]:
                    warn(f"research.json: {u['id']} wird in Stufe {prev} und {n['tier']} freigeschaltet")
                unlock_tier[u["id"]] = min(prev or 99, n["tier"])
    prod = producers(spec)
    for item, tier in unlock_tier.items():
        for r in prod.get(item, []):
            for ing in sd.recipe_inputs(r):
                t = unlock_tier.get(ing, 0)
                if t > tier:
                    err(f"Stufenfehler: {item} (Stufe {tier}) braucht {ing} aus Stufe {t} ({r.get('id')})")
    # Sondenziele: jedes Ziel außer Erde/Erdorbit muss freischaltbar sein
    targets = {u.get("id") for n in spec.nodes for u in n.get("unlocks", []) if u.get("type") == "probe_target"}
    for pid in spec.planets:
        if pid not in ("minecraft:overworld", sd.MOD + "earth_orbit") and pid not in targets:
            err(f"research.json: Ziel {pid} wird von keinem Knoten als Sondenziel freigeschaltet")


# ---------------------------------------------------------------- planets & fuel

def check_planets(spec):
    if len(spec.planets) != 21:
        err(f"planets.json: {len(spec.planets)} Ziele, erwartet 21")
    for pid, p in spec.planets.items():
        for f in ("type", "ring", "distance_points", "gravity", "spec_ref"):
            if p.get(f) is None:
                err(f"planets.json: {pid} ohne '{f}'")
        for b in p.get("blocks", []) or []:
            if not known(spec, b.get("id")):
                err(f"planets.json: {pid} nennt unbekannten Block {b.get('id')}")
        for o in p.get("ores", []) or []:
            if not known(spec, o.get("block")):
                err(f"planets.json: {pid} nennt unbekanntes Erz {o.get('block')}")
            for d in sd.drop_list(o.get("drops")):
                if not known(spec, d.get("id")):
                    err(f"planets.json: {pid} Erz-Drop unbekannt {d.get('id')}")
        for s in p.get("structures", []) or []:
            if not known(spec, s.get("id")):
                warn(f"planets.json: {pid} Struktur {s.get('id')} fehlt in items.json")
        for bi in p.get("biomes", []) or []:
            if not known(spec, bi.get("id")):
                warn(f"planets.json: {pid} Biom {bi.get('id')} fehlt in items.json")
    if "minecraft:overworld" not in spec.planets:
        err("planets.json: Eintrag minecraft:overworld fehlt")
        return
    for row in sd.fuel_table(spec):
        if row["min_config"] == "nicht erreichbar":
            err(f"Treibstoff: {row['id']} ist hin und zurück ({row['from_earth']:.0f} mB) mit keiner Ausstattung erreichbar")
    # Beispiele aus Kapitel 12.2
    c = sd.fuel_constants(spec)
    examples = [("minecraft:overworld", sd.MOD + "moon", 35_360), (sd.MOD + "moon", "minecraft:overworld", 7_440)]
    for a, b, expect in examples:
        if a in spec.planets and b in spec.planets:
            got = sd.trip_cost(spec.planets[a], spec.planets[b], c)
            if abs(got - expect) > 1:
                err(f"Treibstoff: {a} → {b} = {got:.0f} mB, laut 12.2 erwartet {expect}")


# ---------------------------------------------------------------- rocket sums

def check_rocket_sums(spec):
    by_result = {}
    for r in spec.recipes:
        for res in sd.recipe_results(r):
            by_result.setdefault(res, r)

    def total(parts):
        out = defaultdict(int)
        for pid, n in parts:
            r = by_result.get(sd.MOD + pid)
            if not r:
                err(f"Raketensumme: kein Rezept für {pid}")
                continue
            for ing in r.get("ingredients", []):
                for i in sd.ingredient_ids(ing)[:1]:
                    out[i] += ing.get("count", 1) * n
        return out

    t1 = total([("rocket_capsule_t1", 1), ("rocket_tank_t1", 4), ("rocket_engine_t1", 1), ("rocket_fin_t1", 4)])
    expect1 = {"aluminum_plate": 200, "titanium_plate": 64, "circuit": 32}
    t2 = total([("rocket_capsule_t2", 1), ("rocket_tank_t2", 6), ("rocket_engine_t2", 1), ("rocket_fin_t2", 4)])
    expect2 = {"lightweight_plate": 128, "heat_shield": 64, "advanced_circuit": 32, "cooling_system": 4}
    for label, got, expect in (("T1", t1, expect1), ("T2", t2, expect2)):
        for k, v in expect.items():
            g = sum(val for key, val in got.items() if key.endswith(k))
            if g != v:
                err(f"Raketensumme {label}: {k} = {g}, erwartet {v} (22.3)")


# ---------------------------------------------------------------- balance

def check_balance(spec):
    expected = {"machines", "generators", "transport", "storage", "environment", "suits", "domes", "spaceflight",
                "vehicles", "creatures", "events", "logistics", "shipyard", "research", "agriculture", "stations",
                "items", "misc"}
    missing = expected - set(spec.balance)
    for m in sorted(missing):
        err(f"balance/{m}.json fehlt")
    for fname, entries in spec.balance.items():
        if not isinstance(entries, dict):
            err(f"balance/{fname}.json: 'entries' ist kein Objekt")
            continue
        for key, e in entries.items():
            if key.startswith(sd.MOD) and not known(spec, key):
                err(f"balance/{fname}.json: Schlüssel {key} fehlt in items.json")
            if isinstance(e, dict) and not e.get("spec_ref"):
                warn(f"balance/{fname}.json: {key} ohne spec_ref")
    creatures = {k: v for k, v in spec.balance.get("creatures", {}).items() if k.startswith(sd.MOD)}
    mobs = [e["id"] for e in spec.all_entries if e.get("category") == "creature"]
    if creatures and len(creatures) != len(mobs):
        warn(f"Kreaturen: {len(creatures)} in creatures.json, {len(mobs)} in items.json")
    for key in mobs:
        if creatures and key not in creatures:
            err(f"creatures.json: Kreatur {key} fehlt")
    keyed = set().union(*(set(v) for v in spec.balance.values() if isinstance(v, dict))) if spec.balance else set()
    for iid, e in spec.items.items():
        if e.get("category") in ("machine", "generator", "storage") and spec.balance and iid not in keyed:
            warn(f"Balance: {e.get('category')} {iid} hat in keiner Balance-Datei einen Eintrag")


def main():
    quiet = "--quiet" in sys.argv
    spec = sd.load()
    errors.extend(spec.load_errors)
    if spec.items:
        check_items(spec)
    if spec.recipes:
        check_recipes(spec)
    if spec.items and spec.recipes:
        check_obtainable(spec)
        check_rocket_sums(spec)
    if spec.nodes:
        check_research(spec)
    if spec.planets:
        check_planets(spec)
    if spec.balance:
        check_balance(spec)

    print(f"Geprüft: {', '.join(spec.files)}")
    print(f"{len(spec.items)} IDs, {len(spec.recipes)} Rezepte, {len(spec.nodes)} Forschungsknoten, "
          f"{len(spec.planets)} Ziele, {len(spec.balance)} Balance-Dateien")
    print(f"\nFehler: {len(errors)}")
    for e in errors:
        print(f"  ✗ {e}")
    print(f"\nWarnungen: {len(warnings)}")
    if not quiet:
        for w in warnings:
            print(f"  ! {w}")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
