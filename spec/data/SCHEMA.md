# Infinite Horizon – Schema der Quelldaten

Die Dateien in `spec/data/` sind die **einzige maßgebliche Quelle** für alle IDs, Rezepte, Forschungsknoten, Planeten- und Balance-Werte. Die Kapitel in `docs/spec/` beschreiben Regeln und Verhalten und verweisen auf diese Dateien. Bei Abweichungen gelten die Daten.

- Datagen liest diese Dateien und erzeugt daraus die Minecraft-Datenpakete im 26.3-Format (Rezepte, Tags, Loot-Tables, Worldgen) und die Balance-Dateien aus Kapitel 25.7.
- `tools/validate_spec.py` prüft sie auf Vollständigkeit und Widerspruchsfreiheit. `tools/render_docs.py` erzeugt daraus lesbare Tabellen in `docs/spec/generated/`.
- Alle Dateien sind UTF-8-JSON mit 2 Leerzeichen Einrückung. Jede Datei hat oben `"schema_version": 1`.

## Allgemeine Regeln

- **IDs:** immer mit Namespace. Eigene IDs: `infinite_horizon:<snake_case>`. Vanilla: `minecraft:<id>`. Konventions-Tags: `#c:ingots/aluminum` (Tags beginnen mit `#`).
- **Vanilla-IDs** (`minecraft:*`) gelten als vorhanden und stehen nicht in `items.json`.
- **Mengen:** Items als ganze Zahl, Fluide in mB, Zeiten in Ticks, Energie in J bzw. J/t.
- **`spec_ref`:** Kapitel/Abschnitt der Spezifikation, aus dem der Eintrag stammt (z. B. `"6.1"`). Pflicht in jedem Eintrag.
- **`notes`:** optional, kurzer Freitext. Nie Regeln, die nur dort stehen.

---

## `items.json` – alle eigenen IDs

```json
{
  "schema_version": 1,
  "entries": [
    {
      "id": "infinite_horizon:crusher",
      "kind": "block",
      "category": "machine",
      "name_de": "Zerkleinerer",
      "name_en": "Crusher",
      "max_stack": 64,
      "phase": 1,
      "obtained_by": ["recipe"],
      "spec_ref": "6.1",
      "notes": null
    }
  ]
}
```

| Feld | Werte |
| --- | --- |
| `kind` | `item`, `block` (hat auch ein Item), `fluid`, `entity`, `dimension`, `structure`, `biome`, `data_component`, `event` (Ereignisse aus Kapitel 19), `tag`, `sound`. Dieselbe ID darf in verschiedenen `kind` vorkommen (z. B. `lander` als Item und Entity). |
| `category` | `material`, `component`, `machine`, `generator`, `storage`, `cable`, `pipe`, `multiblock_part`, `port`, `tool`, `armor`, `suit_module`, `dome_module`, `rover_module`, `machine_upgrade`, `rocket_part`, `vehicle`, `station_module`, `logistics`, `food`, `ore`, `planet_block`, `decorative`, `creature`, `projectile`, `special`, `world` |
| `phase` | Phase 1–6 (Glossar), in der das Objekt zuerst gebraucht wird; `null` für Welt-Objekte ohne Phase |
| `obtained_by` | Liste aus `recipe`, `worldgen`, `mob_drop`, `machine_output`, `block_drop`, `join_gift`, `event`, `generated` (z. B. Plattform des Stationskerns), `not_obtainable` (Admin/Struktur), `none` (Entities, Dimensionen, Fluide ohne Item) |
| `max_stack` | 1, 16 oder 64; `null` bei Nicht-Items |

Fluide (`kind: fluid`) haben zusätzlich `"fluid_type": "liquid" | "gas"`, `"color": "#RRGGBB"`, `"has_source_block": bool`, `"has_bucket": bool`.

---

## `recipes.json` – alle Rezepte

```json
{
  "schema_version": 1,
  "recipes": [
    {
      "id": "infinite_horizon:assembly/rocket_capsule_t1",
      "type": "assembly",
      "ingredients": [
        { "item": "infinite_horizon:aluminum_plate", "count": 40 },
        { "tag": "#c:plates/titanium", "count": 16 },
        { "any_of": [ { "item": "minecraft:basalt" }, { "item": "infinite_horizon:moon_basalt" } ], "count": 1 }
      ],
      "fluid": { "fluid": "infinite_horizon:kerosene", "amount": 16000 },
      "results": [ { "id": "infinite_horizon:rocket_capsule_t1", "count": 1, "chance": 1.0 } ],
      "fluid_results": [],
      "ticks": 200,
      "energy_per_tick": 30,
      "mold": null,
      "copy_components_from": null,
      "pattern": null,
      "key": null,
      "spec_ref": "22.3"
    }
  ]
}
```

| `type` | Bedeutung | Pflichtfelder |
| --- | --- | --- |
| `crafting_shaped` | Werkbank mit Muster | `pattern` (Liste von Zeilen), `key` (`{"I": {"item": "minecraft:iron_ingot"}}`), `results` |
| `crafting_shapeless` | Werkbank ohne Muster | `ingredients`, `results` |
| `smelting` | Vanilla-Ofen (gilt auch im Elektroofen) | 1 Zutat, `results` |
| `electric_smelting` | nur Elektroofen, bis 2 Zutaten | `ingredients`, `results`, `ticks`, `energy_per_tick` |
| `crushing` | Zerkleinerer | 1 Zutat, `results` (mit `chance` für Nebenausgaben) |
| `pressing` | Presse | 1 Zutat, `mold`, `results` |
| `high_temp_smelting` | Hochtemperaturofen, bis 2 Zutaten | |
| `refining` | Raffinerie | `fluid`, `results`, `fluid_results` |
| `electrolysis` | Elektrolyseur | `fluid`, `fluid_results` |
| `cryo` | Kryo-Verflüssiger | `fluid`, `fluid_results` |
| `regolith_heating` | Regolith-Erhitzer | 1 Zutat, `fluid_results` |
| `zero_g` | Schwerelos-Fertigung | `ingredients`, optional `fluid`, `results` |
| `assembly` | Montagetisch | `ingredients` (max. 9 Slots, eine Zutat darf mehrere Slots belegen: Slots = Summe aufgerundeter `count / 64`), optional `fluid` (max. 16.000 mB), `results` |

- Eine Zutat hat genau eines von `item`, `tag`, `any_of`. Optional `"components": { "infinite_horizon:charge": 1 }` (exakter Abgleich).
- `results` ist immer eine Liste. `chance` ist 0–1, Standard 1.0. Ein Ergebnis darf `"components": { … }` tragen; diese Werte werden nach `copy_components_from` gesetzt (z. B. Frachtrakete + `atomic_drive` → Frachtrakete mit `"infinite_horizon:drive": "infinite_horizon:atomic_drive"`).
- `copy_components_from` steht auf Rezeptebene und nennt den Index der Zutat, deren Komponenten übernommen werden. Hat diese Zutat `count` > 1, wird die erste passende Einheit genommen.
- `ticks` und `energy_per_tick` dürfen `null` sein; dann gelten die Werte der Maschine aus `balance/machines.json`.
- **Regelrezepte** (z. B. „jedes Erz → 2 Staub“) werden **ausgeschrieben**, ein Eintrag pro Metall.
- Feste Sonderrezepte im Code (`suit_module_install`, `suit_module_remove`, `food_packing`) stehen **nicht** in dieser Datei, sondern in `balance/misc.json` unter `"code_recipes"` als kurze Beschreibung.

---

## `research.json` – Stufen und Knoten

```json
{
  "schema_version": 1,
  "tiers": [
    {
      "tier": 2,
      "name_de": "Nahe Welten",
      "name_en": "Nearby Worlds",
      "cost_default": 25,
      "gate": { "type": "enter_dimension", "dimension": "infinite_horizon:moon" },
      "spec_ref": "11.4"
    }
  ],
  "nodes": [
    {
      "id": "infinite_horizon:cryogenics",
      "tier": 2,
      "branch": "industry",
      "cost": 25,
      "requires": ["infinite_horizon:regolith_heater"],
      "unlocks": [
        { "type": "item", "id": "infinite_horizon:cryo_liquefier" },
        { "type": "probe_target", "id": "infinite_horizon:mars" }
      ],
      "position": { "column": 0, "row": 2, "slot": 0 },
      "name_de": "Kryotechnik",
      "name_en": "Cryogenics",
      "spec_ref": "11.4"
    }
  ]
}
```

- `branch`: `industry`, `spaceflight`, `life_support`, `logistics` (Spalte 0–3).
- `gate`-Typen und Felder wie in Kapitel 25.3.
- `unlocks.type`: `item` (alle Rezepte mit diesem Ergebnis), `probe_target`, `multiblock`.

---

## `planets.json` – Ziele, Dimensionen, Worldgen

Liste `"entries"` mit einem Eintrag pro Ziel aus Kapitel 12.1 (Overworld, 12 Oberflächen, 8 Orbits); dazu oben `"dimension_defaults"` mit den gemeinsamen Dimensionswerten aus 13.1. Felder wie Kapitel 25.1 und zusätzlich:

```json
{
  "id": "infinite_horizon:moon",
  "dimension": "infinite_horizon:moon",
  "type": "surface",
  "name_de": "Mond",
  "name_en": "Moon",
  "ring": 1,
  "distance_points": 0,
  "gravity": 0.17,
  "atmosphere": "none",
  "pressure": "vacuum",
  "temperature": { "day": 120, "night": -170 },
  "radiation": 1,
  "solar_factor": 1.0,
  "water_behavior": "freeze",
  "landing_requires_fuel": true,
  "launch_cost_override": null,
  "required_rocket_tier": 1,
  "requires_long_range": false,
  "probe": "infinite_horizon:probe",
  "orbit": "infinite_horizon:earth_orbit",
  "lander_connects_to": "infinite_horizon:earth_orbit",
  "gas_collector_output": null,
  "default_block": "infinite_horizon:moon_rock",
  "default_fluid": "minecraft:air",
  "sea_level": null,
  "fixed_time": null,
  "sky": { "color": "#000000", "fog_color": "#000000", "fog_distance": null, "stars_in_daylight": true, "sun_scale": 1.0, "celestial_bodies": [] },
  "zones": [],
  "biome_temperature_overrides": {},
  "biomes": [
    { "id": "infinite_horizon:moon_mare", "name_de": "Mare", "surface": "infinite_horizon:moon_basalt", "shape": "flat", "rarity": "common", "notes": "flach, dunkel" }
  ],
  "ores": [
    { "block": "infinite_horizon:moon_titanium_ore", "y_min": 0, "y_max": 80, "vein_size": 6, "per_chunk": 4, "biomes": ["infinite_horizon:moon_mare"], "drops": [ { "id": "infinite_horizon:raw_titanium", "count": 1 } ] }
  ],
  "blocks": [
    { "id": "infinite_horizon:moon_regolith", "drops": "self" }
  ],
  "structures": [
    { "id": "infinite_horizon:lava_tube", "exploration_target": true, "biomes": ["infinite_horizon:moon_mare"], "spacing_chunks": 16, "notes": "Tunnel 5×5, Länge 60–120, y 30–50" }
  ],
  "vanilla_mobs": [ { "entity": "minecraft:zombie", "rate": 0.5 } ],
  "events": ["infinite_horizon:solar_storm", "infinite_horizon:meteor_shower"],
  "spec_ref": "13"
}
```

- `shape`: `flat`, `hilly`, `mountainous`, `rare` (Zuordnung zu `multi_noise` nach 13.1).
- `drops` bei Blöcken: `"self"`, `"none"` oder Liste `{id, count, count_max?, chance?}`.
- Strukturen: `spacing_chunks` und `separation_chunks` wie bei Vanilla-`random_spread`. Features: `id`, `type`, `params`.
- Die Overworld hat `"id": "minecraft:overworld"` und listet nur die Ergänzungen der Mod (Bauxit, Titan, Quarzsand, Ölblasen).

---

## `rocket_parts.json`

Wie Kapitel 25.5, als Liste `"parts"`. Dazu `"rules"` je Stufe:

```json
{ "tier": 1, "thrust": 30, "pad": "launch_pad_t1", "fuel": ["infinite_horizon:kerosene", "infinite_horizon:oxygen"], "tanks": [3, 5], "fins": 4, "cargo_bays": [0, 1], "long_range": [0, 0] }
```

---

## `balance/*.json`

Jede Datei ist ein Objekt `{ "schema_version": 1, "entries": { "<schlüssel>": { … } } }`. Schlüssel sind IDs oder die in Kapitel 25.7 genannten Namen. Jeder Eintrag hat `spec_ref`.

| Datei | Schlüssel | Felder (Mindestumfang) |
| --- | --- | --- |
| `machines.json` | Block-ID bzw. `<name>_multiblock` | `energy_per_tick`, `ticks`, `buffer`, `location` (`null` oder `{dimension_type, dimensions[]}`), `upgrade_slots`, `fluid_tanks` (Liste `{role, capacity}`), `item_slots` (`{input, output}`); `fuel_pump` zusätzlich `transfer_per_tick`, `distribution`, `targets[]` und je Tank `fuel_component`. Multiblock-Muster: `multiblock_id`, `controller`, Maße bzw. bei der Startrampe `center`, `platform`, `tower`, `fuel_pump`, `control_radius`, `check_on[]` |
| `generators.json` | Block-ID | `output_per_tick`, `fuel` (`{fluid|item, amount, per_ticks}`), `buffer`, `coolant`, `safety` |
| `transport.json` | Block-ID | `throughput`, `unit` (`J/t` oder `mB/t`) |
| `storage.json` | Block-ID | `capacity`, `unit`, `io_per_tick`, `accepts` (`liquid`, `gas`, `all`, `energy`); Behälter-Items (`transport_tank`) zusätzlich `transfer`, `accepted_in` |
| `environment.json` | Gefahr | `buildup_ticks`, `damage`, `damage_interval` |
| `suits.json` | Stufe bzw. Modul-ID | Grenzwerte, Verbrauch, Slots, Wirkung |
| `domes.json` | Block-ID bzw. Modul-ID | `radius`, `energy_per_tick`, `oxygen_per_1200`, `temp_min`, `temp_max`, `protects[]`, `o2_tank`, `buffer`, `o2_fluids`, `container_slots`, `upgrade_slots`; Regeln in `dome_rules` |
| `spaceflight.json` | Name | Treibstoffformel und Rundung (`fuel_calculation`), Sondenzeiten, Flugablauf (`rocket_flight` mit `ascent_height`), Landeplatzsuche, Landefeuer (`landing_beacon`), Landefähre |
| `vehicles.json` | Entity- bzw. Item-ID | Rover und Module |
| `creatures.json` | Entity-ID | `hp`, `armor`, `damage`, `speed`, `width`, `height`, `category`, `spawn` (`{dimension, biomes[], weight, group_min, group_max, light_max}`), `behavior` (Text), `drops[]` |
| `events.json` | Ereignis-ID | `dimensions[]`, `chance_per_1200`, `duration_min`, `duration_max`, `effects`, `protection[]`, `reward` |
| `logistics.json` | Item-ID bzw. Name | Frachtraketen, Terminal, Flugzeit, Offline-Produktion |
| `shipyard.json` | Abschnittsnummer | `name_de`, `materials[]` (`{id, count}` bzw. `{fluid, amount}`), `build_ticks` |
| `research.json` | Quelle | FP-Werte, Abnahme, Labor-Bonus, Abklingzeiten; Probenregeln (`sample_ring_rules` mit `ring0_blocks` = Inhalt des Tags `sampleable/ring0`), Erkundungsziele (`exploration_target` mit `instance_key`) |
| `agriculture.json` | Block- bzw. Item-ID | Wachstum, Dünger, Ration |
| `stations.json` | Block-ID | Stationskern, Schwerkraftplatte, Klon-Kammer |
| `items.json` | Item- bzw. Block-ID | Werkzeug- und Weltwerte |
| `misc.json` | Name | übrige Einzelwerte, `code_recipes` (mit `match`), `agency_rules` (Beitritt und Rechte, 23.1), `advancements` (Reiter, Protokoll und je Erfolg in `by_id`: `parent`, `icon`, `trigger` und dessen Felder) |
