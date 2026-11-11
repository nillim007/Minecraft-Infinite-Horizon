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

NS = "sternenwerft"
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
    write_json("sternenwerft/fluids.json", {"fluids": entries})
    write_json("sternenwerft/fluid_containers.json", {"containers": containers})


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
COPY_COMPONENTS = {"fluid_tank": [f"{NS}:fluid"], "battery": [f"{NS}:energy"]}


def block_loot(name):
    entry = {"type": "minecraft:item", "name": f"{NS}:{name}"}
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
}


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
        else:
            sys.exit(f"Unbekannte Blockform {shape} für {i}")
        write_json(f"assets/{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": inv_model}})
        if shape == "simple_block" and SIMPLE_BLOCKS[name]["ore"]:
            ore_loot(name, SIMPLE_BLOCKS[name]["ore"][1])
        else:
            block_loot(name)

    # Fluide (M2c): Weltfluid-Block, Eimer, Übersetzungen, Listen für den Java-Code
    fluid_resources(spec, impl.get("fluids", []), items)

    # Liste der einfachen Blöcke für die Java-Registrierung (Reihenfolge wie in implemented_ids.json)
    write_json("sternenwerft/simple_blocks.json", {"blocks": [
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
    write_json("sternenwerft/simple_items.json", {
        "items": [short(i) for i in simple_ids],
        # Stapelgröße aus items.json, nur wenn sie von 64 abweicht (z. B. Eimer 1, 4.1)
        "max_stack": {short(i): spec.items[i]["max_stack"] for i in simple_ids
                      if spec.items[i].get("max_stack") not in (None, 64)}})

    # Tags
    simple = {short(i): SIMPLE_BLOCKS[short(i)] for i, shape in blocks.items() if shape == "simple_block"}
    for tool in ("pickaxe", "shovel"):
        vals = [i for i in sorted(blocks) if (simple[short(i)]["tool"] if short(i) in simple else "pickaxe") == tool]
        if vals:
            write_json(f"data/minecraft/tags/block/mineable/{tool}.json", {"values": vals})
    for lvl in ("stone", "iron"):
        vals = [f"{NS}:{n}" for n, v in sorted(simple.items()) if v["needs"] == lvl]
        if lvl == "stone":
            # Maschinen, Generatoren, Speicher, Ports, Gehäuse, Kabel, Rohre: Werkzeugstufe wie iron_block (4.1)
            vals = sorted(vals + [i for i in blocks if short(i) not in simple])
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
        write_json("sternenwerft/biome_features.json", {"features": fabric})
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
                             "refining", "electrolysis", "assembly"):
            continue  # weitere Typen folgen mit ihren Maschinen
        write_json(f"data/{NS}/recipe/{r['id'].split(':', 1)[1].replace('/', '_')}.json", recipe_json(r, registered))
        n_rec += 1

    # Leere GameTest-Struktur
    for sname, size in GAMETEST_STRUCTURES.items():
        nbt = OUT / "data" / NS / "structure" / "gametest" / f"{sname}.nbt"
        nbt.parent.mkdir(parents=True, exist_ok=True)
        nbt.write_bytes(empty_structure_nbt(size))

    # Balance-Dateien (Kapitel 25.7)
    for p in sorted((sd.DATA / "balance").glob("*.json")):
        target = OUT / "data" / NS / NS / "balance" / p.name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(p.read_bytes().replace(b"\r\n", b"\n"))

    print(f"{len(blocks)} Blöcke, {len(items)} Items, {n_rec} Rezepte, {len(tags)} Tags → {OUT.relative_to(sd.ROOT)}")


if __name__ == "__main__":
    main()
