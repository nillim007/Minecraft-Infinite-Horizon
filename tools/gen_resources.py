"""Erzeugt Mod-Ressourcen aus spec/data für alle registrierten IDs (common/implemented_ids.json).

Aufruf:  python tools/gen_resources.py
Ausgabe: common/src/generated/resources/ (wird vorher geleert, außer textures/)

Erzeugt: Übersetzungen, Blockzustände, Block- und Item-Modelle, Item-Definitionen (26.x-Format), Loot-Tables,
Rezepte (Vanilla und eigene Typen), Tags und die Balance-Dateien (Kapitel 25.7).
Dies ersetzt die Java-Datagen (Abweichung von 3.8/3A.6, siehe docs/spec-gaps.md).
"""
from __future__ import annotations

import json
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import specdata as sd  # noqa: E402

NS = "sternenwerft"
OUT = sd.ROOT / "common" / "src" / "generated" / "resources"
IMPL = sd.ROOT / "common" / "implemented_ids.json"
LANG_EXTRA = sd.ROOT / "common" / "lang_extra.json"

MACHINE_FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
STATUSES = ["idle", "running", "error"]
CABLE_DIRS = ["north", "east", "south", "west", "up", "down"]


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
    write_json(f"assets/{NS}/models/block/{name}.json",
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{name}"}})
    variants = {}
    for facing, rot in MACHINE_FACINGS.items():
        for status in STATUSES:
            v = {"model": f"{NS}:block/{name}"}
            if rot:
                v["y"] = rot
            variants[f"facing={facing},status={status}"] = v
    write_json(f"assets/{NS}/blockstates/{name}.json", {"variants": variants})


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


def block_loot(name):
    write_json(f"data/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{NS}:blocks/{name}",
    })


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
    for k in ("fluid", "fluid_results", "ticks", "energy_per_tick", "mold"):
        if r.get(k) not in (None, []):
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
    extra = json.loads(LANG_EXTRA.read_text(encoding="utf-8"))
    for lang, field in (("de_de", "name_de"), ("en_us", "name_en")):
        data = {k: v for k, v in extra.get(lang, {}).items()}
        for i in sorted(registered):
            kind = "block" if i in blocks else "item"
            data[f"{kind}.{NS}.{short(i)}"] = spec.items[i][field]
        write_json(f"assets/{NS}/lang/{lang}.json", dict(sorted(data.items())))

    # Blöcke
    for i, shape in sorted(blocks.items()):
        name = short(i)
        inv_model = f"{NS}:block/{name}"
        if shape == "machine":
            machine_block(name)
        elif shape == "cable":
            inv_model = cable_block(name)
        else:
            sys.exit(f"Unbekannte Blockform {shape} für {i}")
        write_json(f"assets/{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": inv_model}})
        block_loot(name)

    # Items
    for i in sorted(items):
        name = short(i)
        write_json(f"assets/{NS}/models/item/{name}.json",
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
        write_json(f"assets/{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})

    # Tags
    write_json("data/minecraft/tags/block/mineable/pickaxe.json", {"values": sorted(blocks)})
    tags = {}
    for i in sorted(items):
        n = short(i)
        for form in ("dust", "ingot", "plate", "rod", "wire"):
            if n.endswith("_" + form):
                tags.setdefault(f"{form}s/{n[:-len(form) - 1]}", []).append(i)
                tags.setdefault(f"{form}s", []).append(i)
    for tag, values in sorted(tags.items()):
        write_json(f"data/c/tags/item/{tag}.json", {"values": sorted(values)})

    # Rezepte: nur wenn alle eigenen IDs registriert sind
    n_rec = 0
    for r in spec.recipes:
        if not recipe_ids(r) or not recipe_ids(r) <= registered:
            continue
        if r["type"] not in ("crafting_shaped", "crafting_shapeless", "smelting", "crushing"):
            continue  # weitere Typen folgen mit ihren Maschinen
        write_json(f"data/{NS}/recipe/{r['id'].split(':', 1)[1].replace('/', '_')}.json", recipe_json(r, registered))
        n_rec += 1

    # Balance-Dateien (Kapitel 25.7)
    for p in sorted((sd.DATA / "balance").glob("*.json")):
        target = OUT / "data" / NS / NS / "balance" / p.name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(p.read_bytes().replace(b"\r\n", b"\n"))

    print(f"{len(blocks)} Blöcke, {len(items)} Items, {n_rec} Rezepte, {len(tags)} Tags → {OUT.relative_to(sd.ROOT)}")


if __name__ == "__main__":
    main()
