"""Erzeugt Mod-Ressourcen aus spec/data für alle registrierten IDs (common/implemented_ids.json).

Aufruf:  python tools/gen_resources.py
Ausgabe: common/src/generated/resources/ (wird vorher geleert, außer textures/)

Erzeugt: Übersetzungen, Blockzustände, Block- und Item-Modelle, Item-Definitionen (26.x-Format), Loot-Tables,
Rezepte (Vanilla und eigene Typen), Tags und die Balance-Dateien (Kapitel 25.7).
Dies ersetzt die Java-Datagen (Abweichung von 3.8/3A.6, siehe docs/spec-gaps.md).
"""
from __future__ import annotations

import gzip
import json
import shutil
import struct
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import handbook  # noqa: E402
import specdata as sd  # noqa: E402

NS = "infinite_horizon"
OUT = sd.ROOT / "common" / "src" / "generated" / "resources"
IMPL = sd.ROOT / "common" / "implemented_ids.json"
LANG_EXTRA = sd.ROOT / "common" / "lang_extra.json"

MACHINE_FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
STATUSES = ["idle", "running", "error"]
CABLE_DIRS = ["north", "east", "south", "west", "up", "down"]


# Rezeptart -> Maschine (spec/data/SCHEMA.md, Rezepttabelle)
RECIPE_MACHINE = {
    "electric_smelting": "electric_furnace",
    "crushing": "crusher",
    "pressing": "press",
    "high_temp_smelting": "high_temperature_furnace",
    "refining": "refinery_controller",
    "electrolysis": "electrolyzer",
    "cryo": "cryo_liquefier",
    "regolith_heating": "regolith_heater",
    "zero_g": "zero_g_fabricator",
    "assembly": "assembly_table",
}


def machine_defaults(typ):
    m = RECIPE_MACHINE.get(typ)
    if not m:
        return {}
    path = sd.DATA / "balance" / "machines.json"
    return json.loads(path.read_text(encoding="utf-8")).get("entries", {}).get(f"{NS}:{m}") or {}


def _nbt_name(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _nbt_int_list(vals):
    return b"\x03" + struct.pack(">i", len(vals)) + b"".join(struct.pack(">i", v) for v in vals)


EMPTY_LIST = b"\x00" + struct.pack(">i", 0)


# Leere GameTest-Strukturen: Name -> Größe (x, y, z). „empty_large“ für den Leistungstest mit 100 Zerkleinerern (27.2)
GAMETEST_STRUCTURES = {"empty": [8, 8, 8], "empty_large": [24, 6, 24]}


def empty_structure_nbt(size=(8, 8, 8)):
    """Leere Strukturvorlage als gzip-NBT (deterministisch)."""
    body = b"\x0a" + _nbt_name("")
    body += b"\x03" + _nbt_name("DataVersion") + struct.pack(">i", 4440)
    body += b"\x09" + _nbt_name("size") + _nbt_int_list(list(size))
    for key in ("palette", "blocks", "entities"):
        body += b"\x09" + _nbt_name(key) + EMPTY_LIST
    body += b"\x00"
    return gzip.compress(body, mtime=0)


def short(i):
    return i.split(":", 1)[1]


def write_json(rel, data):
    p = OUT / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2, sort_keys=False) + "\n", encoding="utf-8", newline="\n")


def ingredient(ing):
    if "item" in ing:
        return ing["item"]
    if "tag" in ing:
        return ing["tag"]
    if "any_of" in ing:
        return [ingredient(a) for a in ing["any_of"]]
    raise ValueError(ing)


# ---------------------------------------------------------------- Blöcke

def machine_block(name):
    for status in STATUSES:
        write_json(f"assets/{NS}/models/block/{name}_{status}.json",
                   {"parent": "minecraft:block/orientable",
                    "textures": {"front": f"{NS}:block/{name}_front_{status}",
                                 "side": f"{NS}:block/{name}",
                                 "top": f"{NS}:block/{name}_top"}})
    variants = {}
    for facing, rot in MACHINE_FACINGS.items():
        for status in STATUSES:
            v = {"model": f"{NS}:block/{name}_{status}"}
            if rot:
                v["y"] = rot
            variants[f"facing={facing},status={status}"] = v
    write_json(f"assets/{NS}/blockstates/{name}.json", {"variants": variants})
    return f"{NS}:block/{name}_idle"


def cube_block(name):
    """Würfel mit einer Textur (Gehäuse eines Multiblocks)."""
    write_json(f"assets/{NS}/models/block/{name}.json",
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{name}"}})
    write_json(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    return f"{NS}:block/{name}"


PORT_MODES = ["input", "output"]


def port_block(name):
    """Port (4.3): Blockstate mode=input|output mit je eigener Textur."""
    variants = {}
    for mode in PORT_MODES:
        write_json(f"assets/{NS}/models/block/{name}_{mode}.json",
                   {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{name}_{mode}"}})
        variants[f"mode={mode}"] = {"model": f"{NS}:block/{name}_{mode}"}
    write_json(f"assets/{NS}/blockstates/{name}.json", {"variants": variants})
    return f"{NS}:block/{name}_input"


def cable_block(name):
    tex = f"{NS}:block/{name}"
    core = {"from": [6, 6, 6], "to": [10, 10, 10],
            "faces": {f: {"texture": "#cable"} for f in ("north", "east", "south", "west", "up", "down")}}
    write_json(f"assets/{NS}/models/block/{name}_core.json",
               {"parent": "minecraft:block/block", "textures": {"cable": tex, "particle": tex}, "elements": [core]})
    arm = {"from": [6, 6, 0], "to": [10, 10, 6],
           "faces": {f: {"texture": "#cable"} for f in ("north", "east", "west", "up", "down")}}
    write_json(f"assets/{NS}/models/block/{name}_side.json",
               {"parent": "minecraft:block/block", "textures": {"cable": tex, "particle": tex}, "elements": [arm]})
    write_json(f"assets/{NS}/models/block/{name}_inventory.json",
               {"parent": "minecraft:block/block", "textures": {"cable": tex, "particle": tex},
                "elements": [{"from": [0, 6, 6], "to": [16, 10, 10],
                              "faces": {f: {"texture": "#cable"} for f in ("north", "east", "south", "west", "up", "down")}}]})
    rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    parts = [{"apply": {"model": f"{NS}:block/{name}_core"}}]
    for d in CABLE_DIRS:
        parts.append({"when": {d: "true"}, "apply": {"model": f"{NS}:block/{name}_side", **rot[d]}})
    write_json(f"assets/{NS}/blockstates/{name}.json", {"multipart": parts})
    return f"{NS}:block/{name}_inventory"


# 5.3, nicht in spec/data: Fließwerte (Erdöl wie Wasser, aber Tickverzögerung 30, Fließweite 4, Pegelabfall 2);
# Kerosin übernimmt die Erdölwerte (Lücke #22)
FLUID_FLOW = {"tick_delay": 30, "slope_find_distance": 4, "drop_off": 2, "explosion_resistance": 100}
# Befüllung der Eimer in mB (4.1: 1.000 mB je Eimer)
BUCKET_AMOUNT = 1000


def fluid_resources(spec, fluids, items):
    """Weltfluid-Blockzustand, Eimer-Item, fluids.json und fluid_containers.json."""
    entries = []
    containers = [
        {"full": "minecraft:water_bucket", "fluid": "minecraft:water", "amount": BUCKET_AMOUNT, "empty": "minecraft:bucket"},
        {"full": "minecraft:lava_bucket", "fluid": "minecraft:lava", "amount": BUCKET_AMOUNT, "empty": "minecraft:bucket"},
    ]
    for f in fluids:
        e = spec.items[f"{NS}:{f}"]
        source = bool(e.get("has_source_block"))
        bucket = f"{f}_bucket" if e.get("has_bucket") else None
        if source:
            # wie Vanilla-Wasser: Blockzustand mit einem Modell, das nur die Partikeltextur setzt
            write_json(f"assets/{NS}/blockstates/{f}.json", {"variants": {"": {"model": f"{NS}:block/{f}"}}})
            write_json(f"assets/{NS}/models/block/{f}.json", {"textures": {"particle": f"{NS}:block/{f}_still"}})
        if bucket and source:  # Eimer des Weltfluids registriert der Fluid-Code; Kerosin-Eimer ist ein einfaches Item
            write_json(f"assets/{NS}/models/item/{bucket}.json",
                       {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{bucket}"}})
            write_json(f"assets/{NS}/items/{bucket}.json",
                       {"model": {"type": "minecraft:model", "model": f"{NS}:item/{bucket}"}})
        entries.append({"id": f, "color": e["color"], "source_block": source, "bucket": bucket,
                        "gas": e.get("fluid_type") == "gas", **FLUID_FLOW})
        if bucket:
            containers.append({"full": f"{NS}:{bucket}", "fluid": f"{NS}:{f}", "amount": BUCKET_AMOUNT,
                               "empty": "minecraft:bucket"})
    write_json("infinite_horizon/fluids.json", {"fluids": entries})
    write_json("infinite_horizon/fluid_containers.json", {"containers": containers})


def oil_bubble_worldgen():
    """Ölblase der Erde (13.4): Feature fluid_sphere, Placed Feature, Biom-Tag; NeoForge-Modifier und Fabric-Eintrag."""
    planets = json.loads((sd.DATA / "planets.json").read_text(encoding="utf-8"))["entries"]
    earth = next(p for p in planets if p["id"] == "minecraft:overworld")
    f = next((x for x in earth["features"] if x["type"] == "fluid_sphere"), None)
    if not f:
        return None
    pr = f["params"]
    write_json(f"data/{NS}/worldgen/feature/oil_bubble.json", {
        "type": f"{NS}:fluid_sphere", "fluid": f["fluid"],
        "radius": {"type": "minecraft:uniform", "min_inclusive": pr["radius"][0], "max_inclusive": pr["radius"][1]}})
    write_json(f"data/{NS}/worldgen/placed_feature/oil_bubble.json", {
        "feature": f"{NS}:oil_bubble",
        "placement": [{"type": "minecraft:rarity_filter", "chance": pr["rarity_chunks"]},
                      {"type": "minecraft:in_square"},
                      {"type": "minecraft:height_range", "height": {
                          "type": "minecraft:uniform", "min_inclusive": {"absolute": pr["y_min"]},
                          "max_inclusive": {"absolute": pr["y_max"]}}},
                      {"type": "minecraft:biome"}]})
    write_json(f"data/{NS}/tags/worldgen/biome/has_oil_bubble.json", {"values": f["biomes"]})
    write_json(f"data/{NS}/neoforge/biome_modifier/oil_bubble.json",
               {"type": "neoforge:add_features", "biomes": f"#{NS}:has_oil_bubble",
                "features": f"{NS}:oil_bubble", "step": "lakes"})
    return {"feature": f"{NS}:oil_bubble", "biomes": f"{NS}:has_oil_bubble", "step": "lakes"}


# Blöcke, deren Inhalt beim Abbauen als Komponente am Item bleibt (4.1: fluid_tank, später battery)
COPY_COMPONENTS = {"fluid_tank": [f"{NS}:fluid"], "pressure_bottle": [f"{NS}:fluid"], "battery": [f"{NS}:energy"]}


def block_loot(name, drop=None):
    entry = {"type": "minecraft:item", "name": drop or f"{NS}:{name}"}
    if name in COPY_COMPONENTS:
        # 26.3-Format wie Vanilla furnace: "modifier" mit copy_components aus der Block-Entity
        entry["modifier"] = {"type": "minecraft:copy_components", "include": COPY_COMPONENTS[name],
                             "source": "block_entity"}
    write_json(f"data/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        # 26.3-Format wie Vanilla iron_block: "condition" mit "type"
        "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                   "entries": [entry],
                   "rolls": 1}],
        "random_sequence": f"{NS}:blocks/{name}",
    })


# Eigenschaften-Vorlage, Fallblock-Flag und Werkzeugart der einfachen Blöcke (4.1, Lücke #21)
SIMPLE_BLOCKS = {
    "bauxite_ore": {"copy": "minecraft:iron_ore", "falls": False, "tool": "pickaxe", "needs": "stone", "ore": ("aluminum", "bauxite")},
    "titanium_ore": {"copy": "minecraft:iron_ore", "falls": False, "tool": "pickaxe", "needs": "iron", "ore": ("titanium", "raw_titanium")},
    "deepslate_titanium_ore": {"copy": "minecraft:deepslate_iron_ore", "falls": False, "tool": "pickaxe", "needs": "iron", "ore": ("titanium", "raw_titanium")},
    "quartz_sand": {"copy": "minecraft:sand", "falls": False, "tool": "shovel", "needs": None, "ore": None},
    # Mond (13.4): Gestein wie Stein, Basalt wie Basalt, Regolith wie Kies ohne Fallen, Eis wie Packeis (droppt Eis)
    "moon_rock": {"copy": "minecraft:stone", "falls": False, "tool": "pickaxe", "needs": None, "ore": None},
    "moon_basalt": {"copy": "minecraft:basalt", "falls": False, "tool": "pickaxe", "needs": None, "ore": None,
                    "tags": ["infinite_horizon:basalt"]},
    "moon_regolith": {"copy": "minecraft:gravel", "falls": False, "tool": "shovel", "needs": None, "ore": None},
    "moon_ice": {"copy": "minecraft:packed_ice", "falls": False, "tool": "pickaxe", "needs": None, "ore": None,
                 "drop": "minecraft:ice"},
    "moon_aluminum_ore": {"copy": "minecraft:iron_ore", "falls": False, "tool": "pickaxe", "needs": "stone",
                          "ore": ("aluminum", "bauxite")},
    "moon_titanium_ore": {"copy": "minecraft:iron_ore", "falls": False, "tool": "pickaxe", "needs": "iron",
                          "ore": ("titanium", "raw_titanium")},
    # M5 (13.4): Mars und Venus; Drops aus planets.json (planet_drops). SPEC-GAP #86: Härte und Werkzeugstufe wie die
    # Vanilla-Vorlage, Erze wie Eisenerz (Stein), Wolfram wie Titan (Eisen)
    "mars_sand": {"copy": "minecraft:red_sand", "falls": False, "tool": "shovel", "needs": None, "ore": None},
    "mars_rock": {"copy": "minecraft:stone", "falls": False, "tool": "pickaxe", "needs": None, "ore": None},
    "mars_magnesium_ore": {"copy": "minecraft:iron_ore", "falls": False, "tool": "pickaxe", "needs": "stone", "ore": None,
                           "planet_drops": True},
    "mars_iron_ore": {"copy": "minecraft:iron_ore", "falls": False, "tool": "pickaxe", "needs": "stone", "ore": None,
                      "planet_drops": True},
    "gypsum_block": {"copy": "minecraft:calcite", "falls": False, "tool": "pickaxe", "needs": None, "ore": None,
                     "planet_drops": True},
    "dry_ice_block": {"copy": "minecraft:packed_ice", "falls": False, "tool": "pickaxe", "needs": None, "ore": None,
                      "planet_drops": True},
    "iron_nickel_meteorite": {"copy": "minecraft:iron_ore", "falls": False, "tool": "pickaxe", "needs": "stone", "ore": None,
                              "planet_drops": True},
    "venus_rock": {"copy": "minecraft:blackstone", "falls": False, "tool": "pickaxe", "needs": None, "ore": None},
    "venus_tungsten_ore": {"copy": "minecraft:deepslate_iron_ore", "falls": False, "tool": "pickaxe", "needs": "iron",
                           "ore": None, "planet_drops": True},
    "meteorite_ore": {"copy": "minecraft:iron_ore", "falls": False, "tool": "pickaxe", "needs": "stone", "ore": None},
    "aerostat_platform": {"copy": "minecraft:iron_block", "falls": False, "tool": "pickaxe", "needs": None, "ore": None},
    "venus_sulfur_ore": {"copy": "minecraft:nether_quartz_ore", "falls": False, "tool": "pickaxe", "needs": "stone",
                         "ore": None, "planet_drops": True},
    "compost_soil": {"copy": "minecraft:dirt", "falls": False, "tool": "shovel", "needs": None, "ore": None},
}

# Kompost-Ackerland (17): Vanilla-Tags für Feldfrüchte und Pflanzen
FARMLAND_TAGS = ["minecraft:supports_crops", "minecraft:grows_crops", "minecraft:supports_vegetation"]
# SPEC-GAP: Inhalt von #infinite_horizon:compostable fehlt in spec/data; pflanzliche Vanilla-Abfälle wie im Komposter
COMPOSTABLE = ["#minecraft:leaves", "#minecraft:saplings", "#minecraft:flowers", "minecraft:short_grass",
               "minecraft:fern", "minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:melon_seeds",
               "minecraft:pumpkin_seeds", "minecraft:wheat", "minecraft:carrot", "minecraft:potato", "minecraft:beetroot",
               "minecraft:apple", "minecraft:melon_slice", "minecraft:kelp", "minecraft:sugar_cane", "minecraft:cactus"]


def drops_loot(name, drops, fortune):
    """Loot aus den Drops eines Planetenblocks (13.4): je Drop ein Pool, Mengenbereich, Chance, Glück; der erste Pool
    gibt mit Behuehrung den Block selbst."""
    pools = []
    for k, dr in enumerate(drops):
        mods = []
        if dr.get("count_max"):
            mods.append({"type": "minecraft:set_count",
                         "count": {"type": "minecraft:uniform", "min": dr["count"], "max": dr["count_max"]}})
        elif dr.get("count", 1) != 1:
            mods.append({"type": "minecraft:set_count", "count": dr["count"]})
        if fortune:
            mods.append({"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"})
        mods.append({"type": "minecraft:explosion_decay"})
        item = {"type": "minecraft:item", "modifier": mods, "name": dr["id"]}
        if dr.get("chance"):
            item["condition"] = {"type": "minecraft:random_chance", "chance": dr["chance"]}
        entry = item if k else {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"{NS}:{name}"}, item]}
        pools.append({"entries": [entry], "rolls": 1})
    write_json(f"data/{NS}/loot_table/blocks/{name}.json",
               {"type": "minecraft:block", "pools": pools, "random_sequence": f"{NS}:blocks/{name}"})


def simple_block(name):
    model = f"{NS}:block/{name}"
    write_json(f"assets/{NS}/models/block/{name}.json",
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{name}"}})
    write_json(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": model}}})
    return model


def ore_loot(name, raw):
    """Vanilla-Eisenerz-Muster (26.3-Format): Behuehrung -> Block, sonst Rohstoff mit Glueck."""
    write_json(f"data/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"entries": [{
            "type": "minecraft:alternatives",
            "children": [
                {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"{NS}:{name}"},
                {"type": "minecraft:item", "modifier": [
                    {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                    {"type": "minecraft:explosion_decay"}], "name": f"{NS}:{raw}"}]}],
            "rolls": 1}],
        "random_sequence": f"{NS}:blocks/{name}",
    })


def earth_worldgen(blocks):
    """Erzadern und Quarzsand der Erde aus planets.json (ohne Ölblase)."""
    planets = json.loads((sd.DATA / "planets.json").read_text(encoding="utf-8"))["entries"]
    earth = next(p for p in planets if p["id"] == "minecraft:overworld")
    feats = []
    for ore in earth["ores"]:
        bid = ore["block"]
        if bid not in blocks:
            continue
        name = short(bid)
        if ore["placement"] == "vein":
            dbid = ore.get("deepslate_block") or bid
            targets = [
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
                 "state": bid},
                {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
                 "state": dbid}]
            # 26.3: Feature-Werte stehen direkt im Objekt (kein "config"), Blockzustand als String
            conf = {"type": "minecraft:ore", "size": ore["vein_size"], "discard_chance_on_air_exposure": 0.0,
                    "targets": targets}
            mods = [{"type": "minecraft:count", "count": ore["per_chunk"]}, {"type": "minecraft:in_square"},
                    {"type": "minecraft:height_range", "height": {
                        "type": "minecraft:uniform", "min_inclusive": {"absolute": ore["y_min"]},
                        "max_inclusive": {"absolute": ore["y_max"]}}},
                    {"type": "minecraft:biome"}]
        elif ore["placement"] == "disk":
            conf = {"type": "minecraft:disk",
                "state_provider": {"type": "minecraft:simple", "state": bid},  # 26.3: Typ heißt "simple"
                "target": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:sand", "minecraft:dirt", "minecraft:clay"]},
                "radius": {"type": "minecraft:uniform", "min_inclusive": ore["radius"][0], "max_inclusive": ore["radius"][1]},
                "half_height": 1}
            mods = [{"type": "minecraft:count", "count": ore["per_chunk"]}, {"type": "minecraft:in_square"},
                    {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"}, {"type": "minecraft:biome"}]
        else:
            sys.exit(f"Unbekannte Platzierung {ore['placement']} für {bid}")
        feats.append((name, conf, mods, ore["biomes"]))
    return feats


# ---------------------------------------------------------------- Rezepte

def recipe_json(r, registered):
    typ = r["type"]
    res = r["results"][0] if r.get("results") else None
    if typ == "crafting_shaped":
        return {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": r["pattern"],
                "key": {k: ingredient(v) for k, v in r["key"].items()},
                "result": {"id": res["id"], "count": res.get("count", 1)}}
    if typ == "crafting_shapeless":
        ings = []
        for i in r["ingredients"]:
            ings += [ingredient(i)] * i.get("count", 1)
        return {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ings,
                "result": {"id": res["id"], "count": res.get("count", 1)}}
    if typ == "smelting":
        return {"type": "minecraft:smelting", "category": "misc", "ingredient": ingredient(r["ingredients"][0]),
                "result": {"id": res["id"], "count": res.get("count", 1)}, "experience": 0.1, "cookingtime": 200}
    # Eigene Rezepttypen: gleiche Felder wie spec/data (Kapitel 25.4)
    out = {"type": f"{NS}:{typ}",
           "ingredients": [{"ingredient": ingredient(i), "count": i.get("count", 1)} for i in r.get("ingredients") or []],
           "results": [{"id": x["id"], "count": x.get("count", 1), "chance": x.get("chance", 1.0)} for x in r.get("results") or []]}
    defaults = machine_defaults(typ)
    for k in ("fluid", "fluid_results", "ticks", "energy_per_tick", "mold"):
        if r.get(k) not in (None, []):
            if k in ("ticks", "energy_per_tick") and defaults.get(k) == r[k]:
                continue  # gleich dem Maschinenwert: weglassen, damit machines.json wirkt
            out[k] = r[k]
    return out


def recipe_ids(r):
    ids = set(sd.recipe_results(r)) | set(sd.recipe_inputs(r))
    return {i for i in ids if i.startswith(NS + ":")}


# Laufzeitfelder eines Planeten (25.1); Zonen mit ihrer ID (für den GameTest-Override, 27)
PLANET_FIELDS = ["dimension", "type", "orbit", "star_system", "ring", "distance_points", "gravity", "atmosphere",
                 "pressure", "temperature", "biome_temperature_offsets", "biome_temperature_overrides", "radiation",
                 "solar_factor", "water_behavior", "landing_requires_fuel", "launch_cost_override", "required_rocket_tier",
                 "requires_long_range",
                 "probe", "sky", "gas_collector_output", "zones", "vanilla_mobs"]
ZONE_FIELDS = ["id", "y_min", "y_max", "biome", "near_block", "temperature", "pressure", "radiation", "solar_factor",
               "atmosphere", "water_behavior"]


def planet_key(pid):
    ns, path = pid.split(":", 1)
    return f"planet.{ns}.{path}"


def planet_runtime(p):
    out = {k: p.get(k) for k in PLANET_FIELDS}
    out["zones"] = [{k: z.get(k) for k in ZONE_FIELDS} for z in (p.get("zones") or [])]
    out["name"] = planet_key(p["id"])
    return out


def planet_resources(spec, impl):
    """Planeten der Overworld und aller Dimensionen in implemented_ids.json (13.1)."""
    ids = ["minecraft:overworld"] + list(impl.get("dimensions", []))
    for pid in ids:
        p = spec.planets[pid]
        write_json(f"data/{NS}/{NS}/planet/{pid.split(':', 1)[1]}.json", planet_runtime(p))
    return ids


def damage_types(spec):
    """Schadensarten der Umweltgefahren (8.2) aus environment.json, mit Tag bypasses_armor."""
    rules = spec.balance["environment"]["hazard_rules"]
    types = sorted(set(rules["damage_types"].values()))
    for t in types:
        ns, path = t.split(":", 1)
        write_json(f"data/{ns}/damage_type/{path}.json",
                   {"exhaustion": 0.0, "message_id": f"{ns}.{path}", "scaling": "never"})
    tag_ns, tag_path = rules["damage_type_tag"].split(":", 1)
    write_json(f"data/{tag_ns}/tags/damage_type/{tag_path}.json", {"values": types})
    return types


# Vanilla 26.3 minecraft:overworld (dimension_type ohne attributes, noise_settings ohne aquifers/material_rule):
# Grundlage der Planetendimensionen (13.1 "alle hier nicht genannten Felder wie minecraft:overworld")
OVERWORLD_DIMENSION_TYPE = {
    "ambient_light": 0.0, "coordinate_scale": 1.0, "default_clock": "minecraft:overworld", "has_ceiling": False,
    "has_ender_dragon_fight": False, "has_skylight": True, "infiniburn": "#minecraft:infiniburn_overworld",
    "monster_spawn_block_light_limit": 0,
    "monster_spawn_light_level": {"type": "minecraft:uniform", "max_inclusive": 7, "min_inclusive": 0},
    "timelines": "#minecraft:in_overworld",
}
OVERWORLD_BED_RULE = {"can_set_spawn": "always", "can_sleep": "when_dark",
                      "error_message": {"translate": "block.minecraft.bed.no_sleep"}}
OVERWORLD_NOISE_ROUTER = {k: f"minecraft:overworld/{k}" for k in (
    "chunk_surface_level", "continents", "depth", "erosion", "final_density", "ridges", "temperature", "vegetation")}
OVERWORLD_SPAWN_TARGET = [
    {"minecraft:overworld/continents": [-0.11, 1.0], "minecraft:overworld/erosion": [-1.0, 1.0],
     "minecraft:overworld/ridges": [-1.0, -0.16], "minecraft:overworld/temperature": [-1.0, 1.0],
     "minecraft:overworld/vegetation": [-1.0, 1.0]},
    {"minecraft:overworld/continents": [-0.11, 1.0], "minecraft:overworld/erosion": [-1.0, 1.0],
     "minecraft:overworld/ridges": [0.16, 1.0], "minecraft:overworld/temperature": [-1.0, 1.0],
     "minecraft:overworld/vegetation": [-1.0, 1.0]},
]
# Spawngewichte der Overworld (Vanilla 26.3 plains, Kategorie monster); 13.5 "Biome tragen die Spawngewichte der Overworld"
OVERWORLD_MONSTER_SPAWNS = {
    "minecraft:spider": {"count": 4, "weight": 100}, "minecraft:zombie": {"count": 4, "weight": 90},
    "minecraft:skeleton": {"count": 4, "weight": 100}, "minecraft:creeper": {"count": 4, "weight": 100},
    "minecraft:slime": {"count": 4, "weight": 100}, "minecraft:witch": {"count": 1, "weight": 5},
}
VANILLA_CARVERS = ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]
# SPEC-GAP #87: Werte, die planets.json für Ring 1 offen lässt (Form der Strukturen, Häufigkeit, Krater)
RING1_SHAPES = {
    # Riesenvulkan: Krater oben mit Radius radius / divisor
    "giant_volcano": {"divisor": 8, "block": "default_block", "core": "default_block", "fluid": "minecraft:lava"},
    # Vulkanschlot: Hügel aus Basalt mit Magmarand und Lavaschlot
    "volcanic_vent": {"radius": [4, 6], "height": 3, "divisor": 4, "block": "minecraft:basalt",
                      "core": "minecraft:magma_block", "fluid": "minecraft:lava"},
    # Meteoritenfeld: 3–5 Krater mit Radius 3–6 im Umkreis 20, Meteoritenblock in der Mitte
    "meteorite_field": {"radius": [20, 20], "height": 0, "divisor": 3, "count": [3, 5], "part_radius": [3, 6],
                        "block": "default_block", "core": "infinite_horizon:iron_nickel_meteorite",
                        "fluid": "minecraft:air"},
}
# Meteoritenkrater als Feature im Biom: Radius 2–4, 1 Versuch je 2 Chunks
METEORITE_CRATER = {"radius": [2, 4], "rarity_chunks": 2, "depth_divisor": 2, "rim_height": 1}
# Mars-Canyon: Häufigkeit je Chunk (Vanilla 0,01)
CANYON_PROBABILITY = 0.1
FEATURE_STEPS = 11  # GenerationStep.Decoration
STEP_LOCAL_MODIFICATIONS = 2
STEP_UNDERGROUND_ORES = 6
# Biom-Parameter (13.1): Erosion je Form, seltene Biome über Temperatur bzw. Weirdness
EROSION = {"mountainous": [-1.0, -0.2], "flat": [0.45, 1.0], "hilly": [-0.2, 0.45]}
RARE_AXES = [("temperature", [-1.0, -0.8], [-0.8, 1.0]), ("weirdness", [0.8, 1.0], [-1.0, 0.8])]
# SPEC-GAP #46: Form des Großkraters (Tiefe, Zentralberg) steht nicht in planets.json; Werte wie small_crater
GREAT_CRATER_SHAPE = {"depth_divisor": 3, "rim_height": 1, "peak_radius_divisor": 5, "peak_height_divisor": 3}


def noise_router(p):
    """Overworld-Router; mit generator.air_above_y (Venus-Wolkenschicht, 12.8) darüber nur Luft."""
    top = (p.get("generator") or {}).get("air_above_y")
    if top is None:
        return OVERWORLD_NOISE_ROUTER
    router = dict(OVERWORLD_NOISE_ROUTER)
    router["final_density"] = {"type": "minecraft:min", "left": OVERWORLD_NOISE_ROUTER["final_density"],
                               "right": {"type": "minecraft:gradient", "axis": "y", "from_coordinate": top, "to_coordinate": top + 1,
                                             "from_value": 1.0, "to_value": -1.0}}
    return router


def short_id(i):
    return i.split(":", 1)[1]


def biome_parameters(biomes):
    rare = [b for b in biomes if b.get("rarity") == "rare"]
    used = {axis: (b["id"], inside, rest) for (axis, inside, rest), b in zip(RARE_AXES, rare)}
    out = []
    for b in biomes:
        prm = {"temperature": [-1.0, 1.0], "humidity": [-1.0, 1.0], "continentalness": [-1.0, 1.0],
               "erosion": [-1.0, 1.0], "depth": [-1.0, 1.0], "weirdness": [-1.0, 1.0], "offset": 0.0}
        if b.get("rarity") != "rare":
            prm["erosion"] = EROSION.get(b.get("shape"), EROSION["hilly"])
        for axis, (rid, inside, rest) in used.items():
            prm[axis] = inside if b["id"] == rid else rest
        out.append({"biome": b["id"], "parameters": prm})
    return out


def surface_rule(biomes):
    """Material Rule (13.1): Grundgestein wie Overworld, obere 3 Blöcke aus dem Oberflächenblock des Bioms,
    surface_secondary dort, wo das Noise minecraft:surface > 0 ist."""
    top3 = {"type": "minecraft:stone_depth", "offset": 2, "add_surface_depth": False, "secondary_depth_range": 0,
            "surface_type": "floor"}
    seq = []
    for b in biomes:
        layers = []
        if b.get("surface_secondary"):
            layers.append({"type": "minecraft:condition",
                           "if_true": {"type": "minecraft:noise_threshold", "noise": "minecraft:surface",
                                       "min_threshold": 0.0, "max_threshold": 1.7976931348623157e308},
                           "then_run": {"type": "minecraft:block", "result_state": b["surface_secondary"]}})
        layers.append({"type": "minecraft:block", "result_state": b["surface"]})
        seq.append({"type": "minecraft:condition",
                    "if_true": {"type": "minecraft:biome", "biome_is": b["id"]},
                    "then_run": {"type": "minecraft:condition", "if_true": top3,
                                 "then_run": {"type": "minecraft:sequence", "sequence": layers}}})
    # wie Vanilla: Oberflächenblöcke nur über der vorläufigen Oberfläche, nicht auf Höhlenböden
    return {"type": "minecraft:sequence", "sequence": [
        "minecraft:bedrock_floor",
        {"type": "minecraft:condition", "if_true": {"type": "minecraft:above_preliminary_surface"},
         "then_run": {"type": "minecraft:sequence", "sequence": seq}}]}


def ore_feature_name(dim, i, ore):
    return f"{short_id(dim)}_ore_{i}_{short_id(ore['block'])}"


def dimension_attributes(defaults, sky):
    attrs = {"minecraft:gameplay/respawn_anchor_works": defaults["respawn_anchor_works"],
             "minecraft:gameplay/can_start_raid": defaults["has_raids"],
             "minecraft:gameplay/nether_portal_spawns_piglin": defaults["natural"]}
    if defaults["bed_works"]:
        attrs["minecraft:gameplay/bed_rule"] = OVERWORLD_BED_RULE
    if sky.get("color"):
        attrs["minecraft:visual/sky_color"] = sky["color"]
    if sky.get("fog_color"):
        attrs["minecraft:visual/fog_color"] = sky["fog_color"]
    if sky.get("stars_in_daylight"):
        attrs["minecraft:visual/star_brightness"] = 1.0
    return attrs


def orbit_dimension(dim, p, defaults):
    """Orbit-Dimension (13.1, 14): leere Welt (flach ohne Schichten, Biom the_void ohne Features)."""
    ns, path = dim.split(":", 1)
    gen = p.get("generator") or {}
    dt = dict(OVERWORLD_DIMENSION_TYPE)
    dt.update({"min_y": defaults["min_y"], "height": defaults["height"], "logical_height": defaults["height"],
               "attributes": dimension_attributes(defaults, p.get("sky") or {})})
    write_json(f"data/{ns}/dimension_type/{path}.json", dt)
    write_json(f"data/{ns}/dimension/{path}.json", {"type": dim, "generator": {
        "type": defaults["generator"], "settings": {
            "biome": gen.get("biome", "minecraft:the_void"), "features": False, "lakes": False,
            "layers": gen.get("layers", defaults["flat_layers"]), "structure_overrides": []}}})


def planet_worldgen(spec, impl, blocks):
    """Dimension, Typ, Noise-Settings, Material Rule, Biome, Erze, Krater und Strukturen je Planetendimension."""
    all_defaults = json.loads((sd.DATA / "planets.json").read_text(encoding="utf-8"))["dimension_defaults"]
    defaults = all_defaults["surface"]
    out = {}
    extra_carvers = {}
    struct_biomes = {}
    moon_tube = next((s for s in spec.planets[f"{NS}:moon"].get("structures", []) if s["id"] == f"{NS}:lava_tube"), {})
    for dim in impl.get("dimensions", []):
        p = spec.planets[dim]
        ns, path = dim.split(":", 1)
        if p.get("type") == "orbit":
            orbit_dimension(dim, p, all_defaults["orbit"])
            continue
        sky = p.get("sky") or {}
        # dimension_type (13.1, 26.3-Attribute statt bed_works/has_raids/natural/piglin_safe)
        attrs = {"minecraft:gameplay/respawn_anchor_works": defaults["respawn_anchor_works"],
                 "minecraft:gameplay/can_start_raid": defaults["has_raids"],
                 "minecraft:gameplay/nether_portal_spawns_piglin": defaults["natural"]}
        if defaults["bed_works"]:
            attrs["minecraft:gameplay/bed_rule"] = OVERWORLD_BED_RULE
        if sky.get("color"):
            attrs["minecraft:visual/sky_color"] = sky["color"]
        if sky.get("fog_color"):
            attrs["minecraft:visual/fog_color"] = sky["fog_color"]
        if sky.get("stars_in_daylight"):
            attrs["minecraft:visual/star_brightness"] = 1.0
        dt = dict(OVERWORLD_DIMENSION_TYPE)
        dt.update({"min_y": defaults["min_y"], "height": defaults["height"], "logical_height": defaults["height"],
                   "attributes": attrs})
        write_json(f"data/{ns}/dimension_type/{path}.json", dt)
        # Noise-Settings: Overworld-Router unverändert, ohne Aquifere und Erzadern (13.1)
        write_json(f"data/{ns}/worldgen/noise_settings/{path}.json", {
            "noise": {"min_y": defaults["min_y"], "height": defaults["height"]},
            "default_block": p["default_block"], "default_fluid": p["default_fluid"] or "minecraft:air",
            "noise_router": noise_router(p), "material_rule": dim, "spawn_target": OVERWORLD_SPAWN_TARGET,
            "sea_level": p["sea_level"] if p.get("sea_level") is not None else defaults["min_y"], "disable_mob_generation": False, "legacy_random_source": False})
        write_json(f"data/{ns}/worldgen/material_rule/{path}.json", surface_rule(p["biomes"]))
        write_json(f"data/{ns}/dimension/{path}.json", {
            "type": dim,
            "generator": {"type": "minecraft:noise", "settings": dim,
                          "biome_source": {"type": "minecraft:multi_noise", "biomes": biome_parameters(p["biomes"])}}})
        # Erze (13.4): uniform, "je Chunk" = count, Wirtsblock = default_block
        per_biome = {b["id"]: [[] for _ in range(FEATURE_STEPS)] for b in p["biomes"]}
        ore_features = []
        for i, ore in enumerate(p.get("ores", [])):
            if ore["block"] not in blocks:
                continue
            name = ore_feature_name(dim, i, ore)
            host = ore.get("host_block") or p["default_block"]
            write_json(f"data/{NS}/worldgen/feature/{name}.json", {
                "type": "minecraft:ore", "size": ore["vein_size"], "discard_chance_on_air_exposure": 0.0,
                "targets": [{"target": {"predicate_type": "minecraft:block_match", "block": host},
                             "state": ore["block"]}]})
            write_json(f"data/{NS}/worldgen/placed_feature/{name}.json", {
                "feature": f"{NS}:{name}", "placement": [
                    {"type": "minecraft:count", "count": ore["per_chunk"]}, {"type": "minecraft:in_square"},
                    {"type": "minecraft:height_range", "height": {
                        "type": "minecraft:uniform", "min_inclusive": {"absolute": ore["y_min"]},
                        "max_inclusive": {"absolute": ore["y_max"]}}},
                    {"type": "minecraft:biome"}]})
            ore_features.append({"feature": f"{NS}:{name}", "biomes": ore["biomes"]})
            for b in ore["biomes"]:
                per_biome[b][STEP_UNDERGROUND_ORES].append(f"{NS}:{name}")
        # Eigene Features (13.4): Krater
        for f in p.get("features", []):
            if f["type"] == "canyon_carver":
                # 13.4: Vanilla-Canyon bis y min_y, Breite × width_multiplier
                prm = f["params"]
                w = prm["width_multiplier"]
                write_json(f"data/{NS}/worldgen/carver/{path}_canyon.json", {
                    "type": "minecraft:canyon", "probability": CANYON_PROBABILITY,
                    "shape": {"distance_factor": {"type": "minecraft:uniform", "min_inclusive": 0.75, "max_exclusive": 1.0},
                              "horizontal_radius_factor": {"type": "minecraft:uniform", "min_inclusive": 0.75 * w,
                                                           "max_exclusive": 1.0 * w},
                              "thickness": {"type": "minecraft:trapezoid", "min": 0.0, "max": 6.0, "plateau": 2.0},
                              "vertical_radius_center_factor": 0.0, "vertical_radius_default_factor": 1.0,
                              "width_smoothness": 3, "y_scale": 3.0},
                    "vertical_rotation": {"type": "minecraft:uniform", "min_inclusive": -0.125, "max_exclusive": 0.125},
                    "y": {"type": "minecraft:uniform", "min_inclusive": {"absolute": prm["min_y"]},
                          "max_inclusive": {"absolute": 67}}})
                for b in f["biomes"]:
                    extra_carvers.setdefault(b, []).append(f"{NS}:{path}_canyon")
                continue
            if f["type"] == "meteorite_crater":
                f = dict(f, id=f"{NS}:meteorite_crater", params=dict(METEORITE_CRATER))
            elif f["type"] != "crater":
                continue
            name = short_id(f["id"])
            prm = f["params"]
            body = {"type": f"{NS}:crater",
                    "radius": {"type": "minecraft:uniform", "min_inclusive": prm["radius"][0], "max_inclusive": prm["radius"][1]},
                    "depth_divisor": prm["depth_divisor"], "rim_height": prm["rim_height"]}
            if f.get("block"):
                body["core"] = f["block"]
            write_json(f"data/{NS}/worldgen/feature/{name}.json", body)
            write_json(f"data/{NS}/worldgen/placed_feature/{name}.json", {
                "feature": f"{NS}:{name}", "placement": [
                    {"type": "minecraft:rarity_filter", "chance": prm["rarity_chunks"]}, {"type": "minecraft:in_square"},
                    {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
            for b in f["biomes"]:
                per_biome[b][STEP_LOCAL_MODIFICATIONS].append(f"{NS}:{name}")
        # Biome (13.1): nur Mod-Features, kein Niederschlag, Spawngewichte der Overworld für die Monster aus planets.json
        monsters = [dict(type=m["entity"], **OVERWORLD_MONSTER_SPAWNS[m["entity"]])
                    for m in p.get("vanilla_mobs", []) if m["entity"] in OVERWORLD_MONSTER_SPAWNS]
        creatures = spec.balance.get("creatures", {})
        for b in p["biomes"]:
            attrs = {}
            by_cat = {"monster": list(monsters)} if monsters else {}
            for eid in impl.get("entities", []):
                c = creatures.get(eid)
                sp = (c or {}).get("spawn") or {}
                if not c or sp.get("dimension") != dim or b["id"] not in (sp.get("biomes") or []) or not sp.get("weight"):
                    continue
                count = sp["group_min"] if sp["group_min"] == sp["group_max"] else {
                    "type": "minecraft:uniform", "min_inclusive": sp["group_min"], "max_inclusive": sp["group_max"]}
                by_cat.setdefault(c["category"], []).append({"type": eid, "count": count, "weight": sp["weight"]})
            if by_cat:
                attrs["minecraft:gameplay/natural_mob_spawns"] = {"modifier": "overlay", "argument": {
                    "spawn_costs": {}, "spawns_by_category": by_cat}}
            write_json(f"data/{NS}/worldgen/biome/{short_id(b['id'])}.json", {
                "temperature": 0.5, "downfall": 0.0, "has_precipitation": False,
                "effects": {"water_color": "#3f76e4"}, "attributes": attrs,
                "carvers": VANILLA_CARVERS + extra_carvers.get(b["id"], []), "features": per_biome[b["id"]]})
        # Strukturen (13.4): Großkrater und Lavaröhre als eigene Strukturtypen
        for st in p.get("structures", []):
            name = short_id(st["id"])
            prm = st["params"]
            # gleiche Struktur auf mehreren Planeten (Lavaröhre): Biome zusammenführen
            struct_biomes.setdefault(name, [])
            struct_biomes[name] += [b for b in st["biomes"] if b not in struct_biomes[name]]
            write_json(f"data/{NS}/tags/worldgen/biome/has_structure/{name}.json", {"values": struct_biomes[name]})
            body = {"type": f"{NS}:{name}", "biomes": f"#{NS}:has_structure/{name}", "spawn_overrides": {}}
            if name == "great_crater":
                body.update({"step": "local_modifications",
                             "radius": {"type": "minecraft:uniform", "min_inclusive": prm["radius"][0],
                                        "max_inclusive": prm["radius"][1]},
                             "ore_multiplier": prm["ore_multiplier"], **GREAT_CRATER_SHAPE})
            elif name in RING1_SHAPES:
                shape = RING1_SHAPES[name]
                rad = prm.get("radius", shape.get("radius"))
                rad = rad if isinstance(rad, list) else [rad, rad]
                pick = lambda v: p["default_block"] if v == "default_block" else v
                body.update({"step": "surface_structures", "kind": name,
                             "radius": {"type": "minecraft:uniform", "min_inclusive": rad[0], "max_inclusive": rad[1]},
                             "height": prm.get("max_y", shape.get("height", 0)), "divisor": shape["divisor"],
                             "block": pick(shape["block"]), "core": pick(shape["core"]), "fluid": shape["fluid"]})
                for key in ("count", "part_radius"):
                    if key in shape:
                        body[key] = {"type": "minecraft:uniform", "min_inclusive": shape[key][0],
                                     "max_inclusive": shape[key][1]}
            elif name == "lava_tube":
                body.update({"step": "underground_structures", "size": prm["cross_section"][0],
                             "length": {"type": "minecraft:uniform", "min_inclusive": prm["length"][0],
                                        "max_inclusive": prm["length"][1]},
                             "y": {"type": "minecraft:uniform", "min_inclusive": prm["y_min"],
                                   "max_inclusive": prm["y_max"]}})
            else:
                continue
            write_json(f"data/{NS}/worldgen/structure/{name}.json", body)
            salt = sum(ord(c) * 31 ** k for k, c in enumerate(st["id"])) % 2_000_000_000
            # SPEC-GAP #87: ohne Angabe Abstand wie die Lavaröhre des Mondes, Trennung = Abstand / 4
            spacing = st.get("spacing_chunks") or moon_tube.get("spacing_chunks")
            separation = st.get("separation_chunks") or max(1, spacing // 4)
            write_json(f"data/{NS}/worldgen/structure_set/{name}.json", {
                "placement": {"type": "minecraft:random_spread", "salt": salt,
                              "separation": separation, "spacing": spacing},
                "structures": [{"structure": f"{NS}:{name}", "weight": 1}]})
        out[dim] = ore_features
    mobs = sorted({m['entity'] for dim in impl.get('dimensions', []) for m in spec.planets[dim].get('vanilla_mobs', [])})
    write_json("infinite_horizon/planet_mobs.json", {"entities": mobs})
    return out


# Warntöne (24.6): M3 registriert warning.oxygen/heat/cold/radiation/dome; Platzhalter = Vanilla-Klangdateien
WARNING_SOUNDS = {"warning.oxygen": "minecraft:note/pling", "warning.heat": "minecraft:note/bit",
                  "warning.cold": "minecraft:note/icechime", "warning.radiation": "minecraft:note/didgeridoo",
                  "warning.dome": "minecraft:note/bell"}
# Raketen (24.6): M4 registriert rocket.countdown/launch/landing, Platzhalter wie oben (spec-gaps #58)
ROCKET_SOUNDS = {"rocket.countdown": "minecraft:note/hat", "rocket.launch": "minecraft:random/explode1",
                 "rocket.landing": "minecraft:random/anvil_land"}


def sounds(impl):
    all_sounds = dict(WARNING_SOUNDS)
    if f"{NS}:rocket" in impl.get("entities", []):
        all_sounds.update(ROCKET_SOUNDS)
    write_json(f"assets/{NS}/sounds.json", {name: {"subtitle": f"subtitles.{NS}.{name}", "sounds": [file]}
                                            for name, file in all_sounds.items()})
    write_json(f"{NS}/sound_events.json", {"events": sorted(all_sounds)})


def suits(spec, items):
    """Anzüge (9.1): Equipment-Asset und Reparatur-Tag je Anzug, dessen Teile registriert sind."""
    for sid, e in spec.balance.get("suits", {}).items():
        if not isinstance(e, dict) or "pieces" not in e or not set(e["pieces"]) <= items:
            continue
        tex = f"{NS}:{sid}"
        write_json(f"assets/{NS}/equipment/{sid}.json",
                   {"layers": {"humanoid": [{"texture": tex}], "humanoid_leggings": [{"texture": tex}]}})
        if e.get("repair_item"):
            write_json(f"data/{NS}/tags/item/repairs_{sid}.json", {"values": [e["repair_item"]]})


def farmland_block(name, soil):
    """Ackerland wie Vanilla (17): Modell template_farmland, feucht ab moisture 7."""
    for suffix in ("", "_moist"):
        write_json(f"assets/{NS}/models/block/{name}{suffix}.json",
                   {"parent": "minecraft:block/template_farmland",
                    "textures": {"dirt": f"{NS}:block/{soil}", "top": f"{NS}:block/{name}"}})
    write_json(f"assets/{NS}/blockstates/{name}.json", {"variants": {
        f"moisture={m}": {"model": f"{NS}:block/{name}{'_moist' if m == 7 else ''}"} for m in range(8)}})
    return f"{NS}:block/{name}"


def hull_block(name):
    """Stationshülle (14.1): gleiches Modell für generated=true/false."""
    model = cube_block(name)
    write_json(f"assets/{NS}/blockstates/{name}.json", {"variants": {
        f"generated={v}": {"model": model} for v in ("false", "true")}})
    return model


def hull_loot(name):
    """Generierte Plattformblöcke (generated=true) droppen nichts (14.1)."""
    write_json(f"data/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        # 26.3-Format wie Vanilla white_bed: "condition" mit "type", Blockstate per match_block
        "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                   "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}",
                                "condition": {"type": "minecraft:match_block", "blocks": f"{NS}:{name}",
                                              "state": {"generated": "false"}}}], "rolls": 1}],
        "random_sequence": f"{NS}:blocks/{name}"})


def window_block(name):
    """Fenster (14.2): durchsichtig wie Glas (26.3: force_translucent an der Textur)."""
    write_json(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {
        "all": {"force_translucent": True, "sprite": f"{NS}:block/{name}"}}})
    write_json(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    return f"{NS}:block/{name}"


def layer_block(name):
    """Dünne Schicht wie ein Teppich (18: sulfur_trail), ohne Loot."""
    tex = f"{NS}:block/{name}"
    write_json(f"assets/{NS}/models/block/{name}.json",
               {"parent": "minecraft:block/carpet", "textures": {"wool": tex, "particle": tex}})
    write_json(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    return f"{NS}:block/{name}"


def creature_data(spec, impl):
    """Kreaturen (18): Loot aus drops, Tags analyzable, crawler_food, vibrating, wraith_guide (SPEC-GAP #92)."""
    creatures = spec.balance.get("creatures", {})
    ents = [e for e in impl.get("entities", []) if e in creatures]
    for eid in ents:
        pools = []
        for dr in creatures[eid].get("drops") or []:
            mods = []
            if dr.get("count_max"):
                mods.append({"type": "minecraft:set_count",
                             "count": {"type": "minecraft:uniform", "min": dr["count"], "max": dr["count_max"]}})
            elif dr.get("count", 1) != 1:
                mods.append({"type": "minecraft:set_count", "count": dr["count"]})
            pools.append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": dr["id"], "modifier": mods}]})
        write_json(f"data/{NS}/loot_table/entities/{short_id(eid)}.json",
                   {"type": "minecraft:entity", "pools": pools, "random_sequence": f"{NS}:entities/{short_id(eid)}"})
    write_json(f"data/{NS}/tags/entity_type/analyzable.json", {"values": ents})
    reg = set(impl["blocks"])
    food = [b for b in CRAWLER_FOOD if b.startswith("minecraft:") or b in reg]
    write_json(f"data/{NS}/tags/block/crawler_food.json", {"values": food})
    vib = []
    for eid in ents:
        for s in (creatures[eid].get("behavior_params") or {}).get("vibration_sources", []):
            if s in reg:
                vib.append(s)
    write_json(f"data/{NS}/tags/block/vibrating.json", {"values": sorted(set(vib))})
    guide = []
    for eid in ents:
        for s in (creatures[eid].get("behavior_params") or {}).get("guide_structures", []):
            guide.append(s)
    write_json(f"data/{NS}/tags/worldgen/structure/wraith_guide.json", {"values": sorted(set(guide))})


# SPEC-GAP #92: Inhalt von crawler_food steht nicht in spec/data (Kabel und Metallblöcke)
CRAWLER_FOOD = ["infinite_horizon:copper_cable", "infinite_horizon:aluminum_cable", "minecraft:iron_block",
                "minecraft:copper_block"]


def lamp_block(name):
    """Elektrische Lampe (8.5): Blockstate lit, ein Würfelmodell (Licht kommt aus dem Blockstate)."""
    model = cube_block(name)
    write_json(f"assets/{NS}/blockstates/{name}.json",
               {"variants": {"lit=false": {"model": model}, "lit=true": {"model": model}}})
    return model


def torch_block(name):
    """Erloschene Fackel (8.5): stehend und an der Wand wie Vanilla-Fackeln."""
    tex = f"{NS}:block/{name}"
    write_json(f"assets/{NS}/models/block/{name}.json",
               {"parent": "minecraft:block/template_torch", "textures": {"torch": tex}})
    write_json(f"assets/{NS}/models/block/{name}_wall.json",
               {"parent": "minecraft:block/template_torch_wall", "textures": {"torch": tex}})
    write_json(f"assets/{NS}/models/item/{name}.json",
               {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
    wall = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {"facing=up": {"model": f"{NS}:block/{name}"}}
    for facing, rot in wall.items():
        v = {"model": f"{NS}:block/{name}_wall"}
        if rot:
            v["y"] = rot
        variants[f"facing={facing}"] = v
    write_json(f"assets/{NS}/blockstates/{name}.json", {"variants": variants})
    return f"{NS}:item/{name}"


def atmosphere_tag(spec):
    """Block-Tag needs_atmosphere (8.5) aus misc.json; für Addons erweiterbar."""
    rules = spec.balance["misc"]["atmosphere_rules"]
    tns, tpath = rules["block_tag"].split(":", 1)
    write_json(f"data/{tns}/tags/block/{tpath}.json", {"values": rules["tag_contents"]})


# ---------------------------------------------------------------- Forschung (11.4, 25.3) und Erfolge (24.3)

def research_resources(spec, nodes):
    """Alle 6 Stufen und die sichtbaren Knoten als Datenpaket-Dateien (25.3)."""
    for t in spec.tiers:
        write_json(f"data/{NS}/{NS}/research_tier/{t['tier']}.json",
                   {"tier": t["tier"], "cost_default": t["cost_default"], "gate": t["gate"]})
    for n in nodes:
        ns, path = n["id"].split(":", 1)
        write_json(f"data/{ns}/{NS}/research/{path}.json",
                   {k: n[k] for k in ("tier", "branch", "cost", "requires", "unlocks", "icon", "position") if k in n})


def sampleable_tags(spec, impl, registered):
    """Tags infinite_horizon:sampleable/ring0 und sampleable/planet (11.2): Ring 0 aus research.json, Planeten aus
    blocks[].id und ores[].block in planets.json für alle Dimensionen in implemented_ids.json."""
    def ok(i):
        return not i.startswith(NS + ":") or i in registered
    ring0 = [b for b in spec.balance["research"]["sample_ring_rules"]["ring0_blocks"] if ok(b)]
    planet = set()
    for dim in impl.get("dimensions", []):
        p = spec.planets[dim]
        planet.update(b["id"] for b in p.get("blocks", []))
        planet.update(o["block"] for o in p.get("ores", []))
    write_json(f"data/{NS}/tags/block/sampleable/ring0.json", {"values": ring0})
    write_json(f"data/{NS}/tags/block/sampleable/planet.json", {"values": sorted(b for b in planet if ok(b))})


def rocket_resources(spec, registered):
    """Raketenteile (25.5) in rocket_part/ und Regeln je Stufe in rocket_rules/ (SPEC-GAP #72), nur für registrierte Teile
    bzw. Stufen mit mindestens einem registrierten Teil."""
    tiers = set()
    for part in spec.rocket.get("parts", []):
        if part["block"] in registered:
            ns, path = part["block"].split(":", 1)
            write_json(f"data/{ns}/{NS}/rocket_part/{path}.json", {k: v for k, v in part.items() if k not in ("spec_ref", "notes")})
            tiers.add(part["tier"])
    for rule in spec.rocket.get("rules", []):
        if rule["tier"] in tiers:
            write_json(f"data/{NS}/{NS}/rocket_rules/tier_{rule['tier']}.json",
                       {k: v for k, v in rule.items() if k not in ("spec_ref", "notes")})


ADVANCEMENT_BACKGROUND = "minecraft:gui/advancements/backgrounds/stone"


def advancement_criterion(a):
    trigger = a["trigger"]
    if trigger == "minecraft:inventory_changed":
        return {"trigger": trigger, "conditions": {"items": [{"items": a["item"]}]}}
    if trigger == "minecraft:placed_block":
        return {"trigger": trigger, "conditions": {"location": {"type": "minecraft:match_block", "blocks": a["block"]}}}
    if trigger == "minecraft:changed_dimension":
        return {"trigger": trigger, "conditions": {"to": a["to"]}}
    if trigger == "minecraft:impossible":
        return {"trigger": trigger}
    sys.exit(f"Unbekannter Erfolgs-Auslöser {trigger}")


def advancements(spec, impl, registered):
    """Reiter infinite_horizon:root (24.3): nur die Erfolge aus implemented_ids.json → advancements.
    SPEC-GAP #64: Fehlt der Elternteil, gilt der nächste erzeugte Vorfahr."""
    by_id = spec.balance["misc"]["advancements"]["by_id"]
    wanted = list(impl.get("advancements", []))
    generated = set(wanted)
    for aid in wanted:
        a = by_id[aid]
        for key in ("icon", "item", "block"):
            if a.get(key) and a[key] not in registered:
                sys.exit(f"Erfolg {aid}: {key} {a[key]} ist nicht registriert")
        parent = a.get("parent")
        while parent and parent not in generated:
            parent = by_id[parent].get("parent")
        ns, path = aid.split(":", 1)
        display = {"icon": {"id": a["icon"]},
                   "title": {"translate": f"advancements.{ns}.{path}.title"},
                   "description": {"translate": f"advancements.{ns}.{path}.description"}}
        if parent is None:
            display.update({"background": ADVANCEMENT_BACKGROUND, "show_toast": False, "announce_to_chat": False})
        doc = {}
        if parent:
            doc["parent"] = parent
        doc["criteria"] = {"done": advancement_criterion(a)}
        doc["display"] = display
        doc["requirements"] = [["done"]]
        write_json(f"data/{ns}/advancement/{path}.json", doc)
    return wanted


def main():
    spec = sd.load()
    impl = json.loads(IMPL.read_text(encoding="utf-8"))
    blocks = impl["blocks"]
    items = set(impl["items"])
    registered = set(blocks) | items

    unknown = [i for i in registered if i not in spec.items]
    if unknown:
        sys.exit(f"Unbekannte IDs in implemented_ids.json: {unknown}")

    # Ausgabe leeren (Texturen erzeugt gen_placeholders.py)
    if OUT.exists():
        for child in OUT.rglob("*"):
            if child.is_file() and "textures" not in child.parts:
                child.unlink()

    # Übersetzungen
    fluids = impl.get("fluids", [])
    extra = json.loads(LANG_EXTRA.read_text(encoding="utf-8"))
    hb = handbook.build(spec, impl)
    for w in hb.warnings:
        print(f"Warnung (Handbuch): {w}")
    nodes = sd.visible_nodes(spec, impl)  # 11.4
    for lang, field in (("de_de", "name_de"), ("en_us", "name_en")):
        data = {k: v for k, v in extra.get(lang, {}).items()}
        for i in sorted(registered):
            kind = "block" if i in blocks else "item"
            data[f"{kind}.{NS}.{short(i)}"] = spec.items[i][field]
        for f in fluids:
            fe = spec.items[f"{NS}:{f}"]
            data[f"block.{NS}.{f}"] = fe[field]
            data[f"fluid_type.{NS}.{f}"] = fe[field]
            if fe.get("has_source_block") and fe.get("has_bucket"):
                data[f"item.{NS}.{f}_bucket"] = spec.items[f"{NS}:{f}_bucket"][field]
        data.update(hb.lang[lang])  # Ingenieurs-Handbuch (24.1, 25.6)
        for pid in ["minecraft:overworld"] + list(impl.get("dimensions", [])):
            data[planet_key(pid)] = spec.planets[pid][field]
            # Strukturen der Dimension (Meldung „Erkundungsziel gefunden“, Atlas)
            for st in spec.planets[pid].get("structures", []) or []:
                e = spec.items.get(st["id"])
                if e:
                    sns, spath = st["id"].split(":", 1)
                    data[f"structure.{sns}.{spath}"] = e[field]
        # Entities (M4): Namen aus items.json (kind entity)
        for eid in impl.get("entities", []):
            ent = next((x for x in spec.all_entries if x.get("id") == eid and x.get("kind") == "entity"), None)
            if ent:
                ens, epath = eid.split(":", 1)
                data[f"entity.{ens}.{epath}"] = ent[field]
        # Forschung (11.3): Stufen und sichtbare Knoten
        for t in spec.tiers:
            data[f"research.{NS}.tier.{t['tier']}"] = t[field]
        for n in nodes:
            ns, path = n["id"].split(":", 1)
            data[f"research.{ns}.{path.replace('/', '.')}"] = n[field]
        write_json(f"assets/{NS}/lang/{lang}.json", dict(sorted(data.items())))
    # Handbuchseiten aus docs/handbook (dieselbe Quelle wie das Wiki, tools/gen_wiki.py)
    for rel, page in sorted(hb.files.items()):
        write_json(rel, page)

    # Blöcke
    for i, shape in sorted(blocks.items()):
        name = short(i)
        inv_model = f"{NS}:block/{name}"
        if shape == "machine":
            inv_model = machine_block(name)
        elif shape in ("cable", "pipe"):  # Rohr: gleiche Multipart-Form wie Kabel, andere Textur
            inv_model = cable_block(name)
        elif shape == "simple_block":
            inv_model = simple_block(name)
        elif shape == "casing":
            inv_model = cube_block(name)
        elif shape == "port":
            inv_model = port_block(name)
        elif shape == "lamp":
            inv_model = lamp_block(name)
        elif shape == "torch":
            inv_model = torch_block(name)
        elif shape == "layer":
            inv_model = layer_block(name)
        elif shape == "farmland":
            inv_model = farmland_block(name, "compost_soil")
        elif shape == "hull":
            inv_model = hull_block(name)
        elif shape == "window":
            inv_model = window_block(name)
        else:
            sys.exit(f"Unbekannte Blockform {shape} für {i}")
        write_json(f"assets/{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": inv_model}})
        if shape == "simple_block" and SIMPLE_BLOCKS[name].get("planet_drops"):
            pb = next(b for pl in spec.planets.values() for b in (pl.get("blocks") or []) if b["id"] == i)
            drops_loot(name, pb["drops"], pb.get("fortune", False))
        elif shape == "simple_block" and SIMPLE_BLOCKS[name]["ore"]:
            ore_loot(name, SIMPLE_BLOCKS[name]["ore"][1])
        elif shape == "simple_block" and SIMPLE_BLOCKS[name].get("drop"):
            block_loot(name, SIMPLE_BLOCKS[name]["drop"])
        elif shape == "farmland":
            block_loot(name, f"{NS}:compost_soil")  # wie Vanilla-Ackerland, das Erde droppt
        elif shape == "hull":
            hull_loot(name)
        elif shape != "layer":
            block_loot(name)

    # Fluide (M2c): Weltfluid-Block, Eimer, Übersetzungen, Listen für den Java-Code
    fluid_resources(spec, impl.get("fluids", []), items)

    # Liste der einfachen Blöcke für die Java-Registrierung (Reihenfolge wie in implemented_ids.json)
    write_json("infinite_horizon/simple_blocks.json", {"blocks": [
        {"id": short(i), "copy": SIMPLE_BLOCKS[short(i)]["copy"], "falls": SIMPLE_BLOCKS[short(i)]["falls"]}
        for i, shape in blocks.items() if shape == "simple_block"]})

    # Items
    for i in sorted(items):
        name = short(i)
        write_json(f"assets/{NS}/models/item/{name}.json",
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
        write_json(f"assets/{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})

    # Liste der einfachen Items für die Java-Registrierung (Reihenfolge wie in implemented_ids.json)
    special = {k for k in impl.get("item_classes", {}) if not k.startswith("_")}
    simple_ids = [i for i in impl["items"] if i not in special]
    write_json("infinite_horizon/simple_items.json", {
        "items": [short(i) for i in simple_ids],
        # Stapelgröße aus items.json, nur wenn sie von 64 abweicht (z. B. Eimer 1, 4.1)
        "max_stack": {short(i): spec.items[i]["max_stack"] for i in simple_ids
                      if spec.items[i].get("max_stack") not in (None, 64)}})

    # Tags
    simple = {short(i): SIMPLE_BLOCKS[short(i)] for i, shape in blocks.items() if shape == "simple_block"}
    for tool in ("pickaxe", "shovel"):
        vals = [i for i in sorted(blocks) if (simple[short(i)]["tool"] if short(i) in simple
                                              else "shovel" if blocks[i] == "farmland" else "pickaxe") == tool]
        if vals:
            write_json(f"data/minecraft/tags/block/mineable/{tool}.json", {"values": vals})
    for lvl in ("stone", "iron"):
        vals = [f"{NS}:{n}" for n, v in sorted(simple.items()) if v["needs"] == lvl]
        if lvl == "stone":
            # Maschinen, Generatoren, Speicher, Ports, Gehäuse, Kabel, Rohre: Werkzeugstufe wie iron_block (4.1)
            vals = sorted(vals + [i for i in blocks if short(i) not in simple and blocks[i] not in ("farmland", "window")])
        if vals:
            write_json(f"data/minecraft/tags/block/needs_{lvl}_tool.json", {"values": vals})
    ore_tags = {}
    for n, v in sorted(simple.items()):
        if v["ore"]:
            ore_tags.setdefault(v["ore"][0], []).append(f"{NS}:{n}")
    for metal, vals in sorted(ore_tags.items()):
        write_json(f"data/c/tags/block/ores/{metal}.json", {"values": vals})
        write_json(f"data/c/tags/item/ores/{metal}.json", {"values": vals})
    if ore_tags:
        agg = [f"#c:ores/{m}" for m in sorted(ore_tags)]
        write_json("data/c/tags/block/ores.json", {"values": agg})
        write_json("data/c/tags/item/ores.json", {"values": agg})
    raw = {}
    for n, v in simple.items():
        if v["ore"] and f"{NS}:{v['ore'][1]}" in items:
            raw.setdefault(v["ore"][0], set()).add(f"{NS}:{v['ore'][1]}")
    for metal, vals in sorted(raw.items()):
        write_json(f"data/c/tags/item/raw_materials/{metal}.json", {"values": sorted(vals)})
    if raw:
        write_json("data/c/tags/item/raw_materials.json", {"values": [f"#c:raw_materials/{m}" for m in sorted(raw)]})

    # Worldgen der Erde (13.4): Features, Biom-Tags, NeoForge-Biome-Modifier, Fabric-Liste
    fabric = []
    for name, conf, mods, biomes in earth_worldgen(blocks):
        write_json(f"data/{NS}/worldgen/feature/{name}.json", conf)  # 26.3: Ordner "feature"
        write_json(f"data/{NS}/worldgen/placed_feature/{name}.json",
                   {"feature": f"{NS}:{name}", "placement": mods})
        write_json(f"data/{NS}/tags/worldgen/biome/has_{name}.json", {"values": biomes})
        write_json(f"data/{NS}/neoforge/biome_modifier/{name}.json",
                   {"type": "neoforge:add_features", "biomes": f"#{NS}:has_{name}",
                    "features": f"{NS}:{name}", "step": "underground_ores"})
        fabric.append({"feature": f"{NS}:{name}", "biomes": f"{NS}:has_{name}", "step": "underground_ores"})
    if "crude_oil" in impl.get("fluids", []):
        oil = oil_bubble_worldgen()
        if oil:
            fabric.append(oil)
    if fabric:
        write_json("infinite_horizon/biome_features.json", {"features": fabric})
    tags = {}
    for i in sorted(items):
        n = short(i)
        for form in ("dust", "ingot", "plate", "rod", "wire"):
            if n.endswith("_" + form):
                tags.setdefault(f"{form}s/{n[:-len(form) - 1]}", []).append(i)
                tags.setdefault(f"{form}s", []).append(i)
    for tag, values in sorted(tags.items()):
        write_json(f"data/c/tags/item/{tag}.json", {"values": sorted(values)})

    molds = sorted(i for i in items if short(i).startswith("press_mold_"))
    if molds:
        write_json(f"data/{NS}/tags/item/press_molds.json", {"values": molds})

    # Rezepte: nur wenn alle eigenen IDs registriert sind
    n_rec = 0
    with_fluids = registered | {f"{NS}:{f}" for f in fluids}
    for r in spec.recipes:
        if not recipe_ids(r) or not recipe_ids(r) <= with_fluids:
            continue
        if r["type"] not in ("crafting_shaped", "crafting_shapeless", "smelting", "crushing", "electric_smelting", "pressing",
                             "refining", "electrolysis", "assembly", "high_temp_smelting", "cryo",
                             "regolith_heating"):
            continue  # weitere Typen folgen mit ihren Maschinen
        write_json(f"data/{NS}/recipe/{r['id'].split(':', 1)[1].replace('/', '_')}.json", recipe_json(r, registered))
        n_rec += 1

    # Leere GameTest-Struktur
    for sname, size in GAMETEST_STRUCTURES.items():
        nbt = OUT / "data" / NS / "structure" / "gametest" / f"{sname}.nbt"
        nbt.parent.mkdir(parents=True, exist_ok=True)
        nbt.write_bytes(empty_structure_nbt(size))

    # Forschung (11.4) und Erfolge (24.3)
    research_resources(spec, nodes)
    advancements(spec, impl, registered)
    sampleable_tags(spec, impl, registered)
    rocket_resources(spec, registered)

    # Warntöne (24.6), Anzüge (9.1), Welt ohne Atmosphäre (8.5)
    atmosphere_tag(spec)
    sounds(impl)
    creature_data(spec, impl)
    suits(spec, items)

    # Planeten (13.1), ihre Weltgenerierung (13.4) und Schadensarten der Umwelt (8.2)
    planet_resources(spec, impl)
    planet_worldgen(spec, impl, blocks)
    extra_tags = {}
    for i, shape in blocks.items():
        if shape == "simple_block":
            for t in SIMPLE_BLOCKS[short(i)].get("tags", []):
                extra_tags.setdefault(t, []).append(i)
    farmland = sorted(i for i, shape in blocks.items() if shape == "farmland")
    for t in FARMLAND_TAGS if farmland else []:  # nur Block-Tags
        tns, tpath = t.split(":", 1)
        write_json(f"data/{tns}/tags/block/{tpath}.json", {"values": farmland})
    if f"{NS}:compost" in items:
        write_json(f"data/{NS}/tags/item/compostable.json", {"values": COMPOSTABLE})
    for t, vals in sorted(extra_tags.items()):
        tns, tpath = t.split(":", 1)
        write_json(f"data/{tns}/tags/block/{tpath}.json", {"values": sorted(vals)})
        write_json(f"data/{tns}/tags/item/{tpath}.json", {"values": sorted(vals)})
    damage_types(spec)

    # Balance-Dateien (Kapitel 25.7)
    for p in sorted((sd.DATA / "balance").glob("*.json")):
        target = OUT / "data" / NS / NS / "balance" / p.name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(p.read_bytes().replace(b"\r\n", b"\n"))

    print(f"{len(blocks)} Blöcke, {len(items)} Items, {n_rec} Rezepte, {len(tags)} Tags → {OUT.relative_to(sd.ROOT)}")


if __name__ == "__main__":
    main()
