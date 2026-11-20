"""Gemeinsamer Lader für spec/data und reine Rechenfunktionen (Treibstoff, Slots, Raketen).

Wird von validate_spec.py und render_docs.py benutzt. Keine Abhängigkeiten außer der Standardbibliothek.
"""
from __future__ import annotations

import json
import math
import sys
from dataclasses import dataclass, field
from pathlib import Path

# Windows-Konsolen nutzen sonst cp1252 und scheitern an Umlauten und Symbolen.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "spec" / "data"
MOD = "infinite_horizon:"

TANK = 16_000
LONG_RANGE_DEFAULT = 128_000



# SPEC-GAP #101: Erzwerte fehlen in planets.json; wie die Mondvorlage (moon_titanium_ore, moon_ice)
ORE_DEFAULTS = {
    "infinite_horizon:mercury_titanium_ore": {"y_min": 0, "y_max": 80, "vein_size": 6, "per_chunk": 4},
    "infinite_horizon:mercury_iron_ore": {"y_min": 0, "y_max": 80, "vein_size": 8, "per_chunk": 6},
    "minecraft:ice": {"y_min": 40, "y_max": 100, "vein_size": 16, "per_chunk": 4},
    # Io (SPEC-GAP #104): wie das Schwefelerz der Venus
    "infinite_horizon:io_sulfur_ore": {"y_min": 40, "y_max": 100, "vein_size": 8, "per_chunk": 4},
}


def ore_with_defaults(ore: dict) -> dict:
    """Erz-Eintrag mit ORE_DEFAULTS für fehlende Werte (SPEC-GAP #101)."""
    out = dict(ore)
    for k, v in ORE_DEFAULTS.get(ore["block"], {}).items():
        if out.get(k) is None:
            out[k] = v
    return out

def load_json(path: Path):
    with path.open(encoding="utf-8") as f:
        return json.load(f)


@dataclass
class Spec:
    items: dict = field(default_factory=dict)          # id -> bevorzugter Eintrag
    all_entries: list = field(default_factory=list)    # alle Einträge, auch gleiche ID in anderer Registry
    recipes: list = field(default_factory=list)
    tiers: list = field(default_factory=list)
    nodes: list = field(default_factory=list)
    planets: dict = field(default_factory=dict)        # id -> entry
    rocket: dict = field(default_factory=dict)
    balance: dict = field(default_factory=dict)        # dateiname ohne .json -> entries
    load_errors: list = field(default_factory=list)
    files: list = field(default_factory=list)


def _entries(doc, key):
    value = doc.get(key, [])
    return list(value.values()) if isinstance(value, dict) else list(value)


def load(data_dir: Path = DATA) -> Spec:
    spec = Spec()

    def read(rel):
        p = data_dir / rel
        if not p.exists():
            spec.load_errors.append(f"Datei fehlt: spec/data/{rel}")
            return None
        try:
            doc = load_json(p)
        except json.JSONDecodeError as e:
            spec.load_errors.append(f"Ungültiges JSON in spec/data/{rel}: {e}")
            return None
        spec.files.append(rel)
        if doc.get("schema_version") != 1:
            spec.load_errors.append(f"spec/data/{rel}: schema_version fehlt oder ist nicht 1")
        return doc

    doc = read("items.json")
    if doc:
        # Dieselbe ID darf in verschiedenen Registries vorkommen (z. B. lander als Item und Entity).
        # spec.items hält je ID den „greifbaren“ Eintrag (Item/Block/Fluid vor allem anderen).
        priority = {"item": 0, "block": 0, "fluid": 0, "entity": 1}
        seen = set()
        for e in _entries(doc, "entries"):
            key = (e.get("id"), e.get("kind"))
            if key in seen:
                spec.load_errors.append(f"items.json: doppelter Eintrag {key[0]} ({key[1]})")
            seen.add(key)
            spec.all_entries.append(e)
            cur = spec.items.get(e.get("id"))
            if cur is None or priority.get(e.get("kind"), 2) < priority.get(cur.get("kind"), 2):
                spec.items[e.get("id")] = e
    doc = read("recipes.json")
    if doc:
        spec.recipes = _entries(doc, "recipes")
    doc = read("research.json")
    if doc:
        spec.tiers = _entries(doc, "tiers")
        spec.nodes = _entries(doc, "nodes")
    doc = read("planets.json")
    if doc:
        for e in _entries(doc, "entries") or _entries(doc, "planets"):
            spec.planets[e.get("id")] = e
    doc = read("rocket_parts.json")
    if doc:
        spec.rocket = doc
    bdir = data_dir / "balance"
    if bdir.exists():
        for p in sorted(bdir.glob("*.json")):
            doc = read(f"balance/{p.name}")
            if doc:
                spec.balance[p.stem] = doc.get("entries", {})
    return spec


# ---------------------------------------------------------------- Hilfen

def implemented_ids() -> dict:
    """common/implemented_ids.json (registrierte IDs, Meilenstein, Teilschritt)."""
    return load_json(ROOT / "common" / "implemented_ids.json")


def visible_nodes(spec: Spec, impl: dict) -> list:
    """Sichtbare Forschungsknoten (11.4): mindestens eine registrierte Freischaltung (Item, Block, Fluid, Multiblock)
    oder ein Sondenziel mit Planeten-Eintrag (= registrierte Dimension)."""
    registered = set(impl.get("blocks", {})) | set(impl.get("items", [])) | \
        {f"infinite_horizon:{f}" for f in impl.get("fluids", [])}
    dims = set(impl.get("dimensions", []))
    out = []
    for n in spec.nodes:
        for u in n.get("unlocks", []):
            if (u.get("type") in ("item", "multiblock") and u.get("id") in registered) or \
                    (u.get("type") == "probe_target" and u.get("id") in dims):
                out.append(n)
                break
    return out


def find_value(obj, *names, default=None):
    """Sucht den ersten Schlüssel aus `names` rekursiv in einem verschachtelten Objekt."""
    stack = [obj]
    while stack:
        cur = stack.pop(0)
        if isinstance(cur, dict):
            for n in names:
                if n in cur and not isinstance(cur[n], (dict, list)):
                    return cur[n]
            stack.extend(cur.values())
        elif isinstance(cur, list):
            stack.extend(cur)
    return default


def drop_list(drops) -> list[dict]:
    """Drops sind eine Liste von {id, count …} oder ein Schlüsselwort ("self", "none", "vanilla")."""
    return [d for d in drops if isinstance(d, dict)] if isinstance(drops, list) else []


def ingredient_ids(ing) -> list[str]:
    if "item" in ing:
        return [ing["item"]]
    if "tag" in ing:
        return [ing["tag"]]
    if "any_of" in ing:
        out = []
        for alt in ing["any_of"]:
            out.extend(ingredient_ids(alt))
        return out
    return []


def _find_key(obj, key):
    if isinstance(obj, dict):
        if key in obj:
            return obj[key]
        for v in obj.values():
            found = _find_key(v, key)
            if found is not None:
                return found
    elif isinstance(obj, list):
        for v in obj:
            found = _find_key(v, key)
            if found is not None:
                return found
    return None


_MIN_MULTIPLIERS: dict = {}


def min_multipliers(spec: Spec) -> dict:
    """Kleinster Mengen-Multiplikator je Montagetisch-Rezept (22.3, Entscheidung zu Kapitel 28 vom 19.11.2026).

    Unter 1 macht das Aufrunden (``max(1, aufgerundet(Menge × Faktor))``) manche Rezepte zutatengleich; dann wäre nur
    noch das erste herstellbar. Für jeden Faktor zwischen ``min`` aus ``misc.json → recipe_amount_multiplier`` und 1
    wird geprüft, welche Rezepte gleich würden; jedes außer dem mit der kleinsten Rezept-ID bekommt als Mindestfaktor
    den nächsten Knickpunkt darüber (Mengen sind zwischen den Knickpunkten k/n gleich). Wiederholt, bis nichts mehr
    zusammenfällt. Ergebnis: Rezept-ID -> Mindestfaktor (nur Rezepte, die einen brauchen).
    """
    key = id(spec)
    if key in _MIN_MULTIPLIERS:
        return _MIN_MULTIPLIERS[key]
    cfg = _find_key(spec.balance.get("misc") or {}, "recipe_amount_multiplier") or {}
    lo = float(cfg.get("min", 1.0))
    asm = sorted((r for r in spec.recipes if r.get("type") == "assembly"), key=lambda r: r["id"])
    by_id = {r["id"]: r for r in asm}

    def stackable(ing):
        ids = ingredient_ids(ing)
        e = spec.items.get(ids[0]) if ids else None
        return not (e and e.get("max_stack") == 1)

    counts = {i.get("count", 1) for r in asm for i in r.get("ingredients") or [] if stackable(i)}
    points = sorted({lo} | {k / n for n in counts for k in range(1, n + 1) if lo <= k / n < 1})

    def sig(r, f):
        parts = []
        for i in r.get("ingredients") or []:
            n = i.get("count", 1)
            if stackable(i):
                n = max(1, math.ceil(n * f - 1e-9))
            parts.append((json.dumps({k: v for k, v in i.items() if k != "count"}, sort_keys=True), n))
        return (tuple(sorted(parts)), json.dumps(r.get("fluid"), sort_keys=True), r.get("mold"))

    out: dict = {}
    changed = True
    while changed:
        changed = False
        for f in points:
            groups: dict = {}
            for r in asm:
                groups.setdefault(sig(r, max(f, out.get(r["id"], 0.0))), []).append(r["id"])
            for ids in groups.values():
                if len(ids) < 2:
                    continue
                # Ein Rezept anheben: das, dessen Mengen sich beim kleinsten Faktor ändern (bei Gleichstand die
                # spätere ID); wer schon unskaliert ist (1,0), bleibt. Weitere Gleiche folgen im nächsten Durchlauf.
                best = None
                for pos, rid in enumerate(ids):
                    e = max(f, out.get(rid, 0.0))
                    if e >= 1.0:
                        continue
                    here = sig(by_id[rid], e)
                    nxt = next((p for p in points if p > e and sig(by_id[rid], p) != here), 1.0)
                    if best is None or (nxt, -pos) < (best[0], -best[1]):
                        best = (nxt, pos, rid)
                if best:
                    out[best[2]] = best[0]
                    changed = True
    _MIN_MULTIPLIERS[key] = out
    return out


def recipe_results(r) -> list[str]:
    out = [x.get("id") for x in r.get("results") or []]
    out += [x.get("fluid") for x in r.get("fluid_results") or []]
    return [x for x in out if x]


def recipe_inputs(r) -> list[str]:
    out = []
    for ing in r.get("ingredients") or []:
        out.extend(ingredient_ids(ing))
    if r.get("type") in ("crafting_shaped",) and isinstance(r.get("key"), dict):
        for ing in r["key"].values():
            out.extend(ingredient_ids(ing))
    if r.get("fluid"):
        out.append(r["fluid"].get("fluid"))
    if r.get("mold"):
        out.append(r["mold"])
    return [x for x in out if x]


def assembly_slots(r) -> int:
    return sum(math.ceil(max(1, ing.get("count", 1)) / 64) for ing in r.get("ingredients") or [])


# ---------------------------------------------------------------- Treibstoff (Kapitel 12.2)

@dataclass
class FuelConstants:
    launch_per_g: float = 32_000
    transfer_per_dp: float = 6_000
    transfer_body_change: float = 2_000
    landing_factor: float = 0.25


def fuel_constants(spec: Spec) -> FuelConstants:
    sf = spec.balance.get("spaceflight", {})
    c = FuelConstants()
    c.launch_per_g = (sf.get("launch_cost") or {}).get("per_g", c.launch_per_g)
    c.transfer_per_dp = (sf.get("transfer_cost") or {}).get("per_dp", c.transfer_per_dp)
    c.transfer_body_change = (sf.get("transfer_cost") or {}).get("different_body", c.transfer_body_change)
    c.landing_factor = (sf.get("landing_cost") or {}).get("factor", c.landing_factor)
    return c


def launch_cost(p, c: FuelConstants) -> float:
    # Vorrang: Zone (z. B. Venus-Wolkenschicht, wo Raketen immer landen) → Planet → Formel.
    for z in p.get("zones") or []:
        if z.get("launch_cost_override") is not None:
            return z["launch_cost_override"]
    if p.get("launch_cost_override") is not None:
        ov = p["launch_cost_override"]
        return ov if isinstance(ov, (int, float)) else find_value(ov, "amount", "value", "cost", default=0)
    if p.get("type") == "orbit":
        return 0
    return c.launch_per_g * p.get("gravity", 1.0)


def landing_cost(p, c: FuelConstants) -> float:
    if p.get("type") == "orbit" or not p.get("landing_requires_fuel", False):
        return 0
    return c.landing_factor * c.launch_per_g * p.get("gravity", 1.0)


def trip_cost(a, b, c: FuelConstants) -> float:
    transfer = c.transfer_per_dp * abs(a.get("distance_points", 0) - b.get("distance_points", 0))
    if a.get("id") != b.get("id"):
        transfer += c.transfer_body_change
    return launch_cost(a, c) + transfer + landing_cost(b, c)


def round_trip(spec: Spec, origin_id: str, dest_id: str) -> float:
    c = fuel_constants(spec)
    a, b = spec.planets[origin_id], spec.planets[dest_id]
    return trip_cost(a, b, c) + trip_cost(b, a, c)


def min_config(p, need: float) -> str:
    """Kleinste Ausstattung nach 12.4 (T1 3–5 Tanks, T2 4–8 Tanks, Modul +128.000) mit Reserve > 0."""
    options = []
    if p.get("requires_long_range"):
        options = [("T2 + Modul", n, n * TANK + LONG_RANGE_DEFAULT) for n in range(4, 9)]
    else:
        if p.get("required_rocket_tier", 1) <= 1:
            options += [("T1", n, n * TANK) for n in range(3, 6)]
        options += [("T2", n, n * TANK) for n in range(4, 9)]
    for i, (label, n, cap) in enumerate(options):
        if cap > need:
            return f"{label}, {n} Tanks"
        if cap == need:
            nxt = next(((l2, n2) for l2, n2, c2 in options[i + 1:] if c2 > need), None)
            if nxt and nxt[0] == label:
                return f"{label}, {nxt[1]} Tanks ({n} ohne Reserve)"
            return f"{label}, {n} Tanks (ohne Reserve)"
    return "nicht erreichbar"


def fuel_table(spec: Spec):
    rows = []
    earth = "minecraft:overworld"
    station = MOD + "earth_orbit"
    for pid, p in spec.planets.items():
        if pid in (earth,):
            continue
        rt_earth = round_trip(spec, earth, pid)
        rt_station = round_trip(spec, station, pid) if station in spec.planets and pid != station else None
        rows.append({
            "id": pid,
            "name": p.get("name_de", pid),
            "from_earth": rt_earth,
            "from_station": rt_station,
            "min_config": min_config(p, rt_earth),
        })
    rows.sort(key=lambda r: (spec.planets[r["id"]].get("distance_points", 0), r["id"]))
    return rows
