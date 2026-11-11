# 25. Datenformate

Dateien **eigener Registries** liegen unter `data/<namespace>/sternenwerft/<typ>/`, damit andere Mods und Datenpakete Inhalte ergänzen können. Rezepte, Advancements, Loot-Tables, Tags und Worldgen liegen in den Vanilla-Ordnern (`recipe/`, `advancement/`, `loot_table/`, `tags/`, `worldgen/` …). Alle eigenen Registries sind neu ladbar (`/reload`) und werden an Clients synchronisiert.

## 25.1 Planet (`sternenwerft:planet`, Ordner `planet/`)

```json
{
  "dimension": "sternenwerft:moon",
  "type": "surface",
  "orbit": "sternenwerft:earth_orbit",
  "star_system": "sternenwerft:sol",
  "ring": 1,
  "distance_points": 0,
  "gravity": 0.17,
  "atmosphere": "none",
  "pressure": "vacuum",
  "temperature": { "day": 120, "night": -170 },
  "biome_temperature_offsets": {},
  "biome_temperature_overrides": { "sternenwerft:moon_polar_crater": -190 },
  "radiation": 1,
  "solar_factor": 1.0,
  "water_behavior": "freeze",
  "landing_requires_fuel": true,
  "required_rocket_tier": 1,
  "requires_long_range": false,
  "probe": "sternenwerft:probe",
  "sky": {
    "color": "#000000",
    "fog_color": "#000000",
    "fog_distance": null,
    "stars_in_daylight": true,
    "sun_scale": 1.0,
    "celestial_bodies": [
      { "texture": "sternenwerft:textures/sky/earth.png", "size": 30, "azimuth": 60, "elevation": 40 }
    ]
  },
  "gas_collector_output": null,
  "zones": [
    {
      "y_min": -64, "y_max": 400,
      "biome": "sternenwerft:moon_polar_crater",
      "near_block": null,
      "temperature": -190, "pressure": null, "radiation": null,
      "solar_factor": null, "atmosphere": null, "water_behavior": null
    }
  ]
}
```

- **Einträge:** Es gibt einen Eintrag pro Ziel aus 12.1: die Erde (`minecraft:overworld`, g 1,0, DP 0, Ring 0, atembar, ohne Gefahren, Temperatur Nacht/Tag 10/20 °C für die Kuppelprüfung), 12 weitere Oberflächen und 8 Orbits. `type` ist `surface` oder `orbit`. Orbits setzen `gravity: 0.005`, `atmosphere: "none"` und ihren Solarfaktor (13.1). Gas-Sammler-Orbits setzen `gas_collector_output` (`{ "fluid": "...", "amount": 100, "ticks": 20, "alternative": "..." }`).
- **Zonen** überschreiben einzelne Werte in einem Bereich. `null` heißt „Planetenwert behalten“. **Vorrang:** Planetenwert → `biome_temperature_overrides` → alle passenden Zonen in Listenreihenfolge; spätere Nicht-null-Werte gewinnen. Allgemeinere Zonen stehen deshalb vorn (z. B. Europa-Ozean vor Europa-Tiefsee). `biome` und `near_block` sind optional; `near_block` gilt im Umkreis von 4 Blöcken.
- **Pflicht-Zonen:**
  - Venus-Wolkenschicht: y 180–240, 20 °C, `normal`, Solar 2,0.
  - Europa-Ozean: y −64–111, +2 °C, `normal`, Strahlung 0, `water_behavior: normal`.
  - Europa-Tiefsee: y −64–79, Druck `deep`.
  - Io an Lava: `near_block: minecraft:lava`, +300 °C.
  - Mond-Polarkrater: −190 °C (wie im Beispiel oben).
  - Merkur-Biome laut 13.4.

## 25.2 Sonnensystem (`sternenwerft:star_system`, Ordner `star_system/`)

```json
{
  "name": "sternenwerft.star_system.sol",
  "star_color": "#FFF4D6",
  "rings": [
    { "index": 0, "planets": ["minecraft:overworld"] },
    { "index": 1, "planets": ["sternenwerft:moon", "sternenwerft:mars", "sternenwerft:venus"] }
  ],
  "starship_jump_cost": 1,
  "unlock_requirement": null
}
```

Weitere Systeme setzen `unlock_requirement` (z. B. ein Item oder Advancement) und erscheinen in der Zielliste der T3.

## 25.3 Forschungsknoten (Ordner `research/`)

```json
{
  "tier": 2,
  "branch": "industry",
  "cost": 25,
  "requires": ["sternenwerft:regolith_heater"],
  "unlocks": [
    { "type": "item", "id": "sternenwerft:cryo_liquefier" },
    { "type": "item", "id": "sternenwerft:cooling_system" }
  ],
  "icon": "sternenwerft:cryo_liquefier",
  "position": { "column": 0, "row": 2, "slot": 3 }
}
```

`position.column` = Ast (0 Industrie, 1 Raumfahrt, 2 Lebenserhaltung, 3 Logistik), `position.row` = Stufe (1–6), `position.slot` = Reihenfolge innerhalb der Zelle.

Stufen-Tore stehen in `research_tier/<n>.json`: `{ "tier": 2, "cost_default": 25, "gate": { "type": "enter_dimension", "dimension": "sternenwerft:moon" } }`. **Gate-Typen und Felder:**

| Typ | Felder | Erfüllt, wenn |
| --- | --- | --- |
| `has_block` | `block` | ein Block dieser Art mit `owner_agency` = Agentur existiert (zählt beim Platzieren) |
| `enter_dimension` | `dimension` | ein Mitglied die Dimension betreten hat |
| `crafted_items` | `items: [ID…]`, `fluids: [ID…]` | jedes Item bzw. Fluid wurde mindestens einmal hergestellt. Zählt für Spieler-Crafting und für Maschinen mit `owner_agency` = Agentur. |
| `machine_completed` | `block`, optional `recipe` | eine Maschine der Agentur hat einen Vorgang abgeschlossen |
| `all` | `gates: [Gate…]` | alle Teil-Gates erfüllt |

- Stufe n setzt immer Stufe n − 1 voraus.
- Ein einmal geöffnetes Tor bleibt offen, auch wenn z. B. das `research_terminal` abgebaut wird.

**Knoten:**
- `branch`: `industry`, `spaceflight`, `life_support`, `logistics`.
- `unlocks`: Liste von `{ "type": "item" | "probe_target" | "multiblock", "id": … }`. `item` schaltet alle Rezepte mit diesem Ergebnis frei (11.3). `probe_target` schaltet ein Sondenziel frei (z. B. `sternenwerft:mars`). Erde und `earth_orbit` sind immer bekannt.
- Der Knoten ist die **einzige** Quelle für Freischaltungen; Rezepte haben kein eigenes Forschungsfeld.

## 25.4 Rezepte

Eigene Rezepttypen (`RecipeType` + `RecipeSerializer` über Codecs): `sternenwerft:crushing`, `electric_smelting`, `high_temp_smelting`, `pressing`, `refining`, `electrolysis`, `cryo`, `zero_g`, `assembly`.

```json
{
  "type": "sternenwerft:assembly",
  "ingredients": [
    { "item": "sternenwerft:aluminum_plate", "count": 40 },
    { "tag": "c:plates/titanium", "count": 16 },
    { "item": "sternenwerft:circuit", "count": 16 }
  ],
  "fluid": null,
  "results": [ { "id": "sternenwerft:rocket_capsule_t1", "count": 1, "chance": 1.0 } ],
  "time": 200,
  "energy_per_tick": 30
}
```

Beim Erzeugen lässt `gen_resources.py` `ticks`/`energy_per_tick` weg, wenn sie dem Wert der Maschine in `machines.json` entsprechen; so wirkt eine Änderung in `machines.json` auf alle Standardrezepte.

Maschinenrezepte haben zusätzlich `fluid_inputs` und `fluid_outputs` (`{ "fluid": "...", "amount": n }`). `pressing` hat `mold`. `results` erlaubt mehrere Ausgaben mit `chance` (0–1), z. B. `metal_concentrate` → Aluminium- und Titanstaub oder `meteorite_ore` mit 10 % Platin. Beim Montagetisch steht ein „Tank X“ als `"fluid": { "fluid": "X", "amount": 16000 }`. `meteorite_ore` droppt beim Abbau sich selbst, Rohstoffe gibt es nur über den Zerkleinerer.

**Komponenten in Rezepten:**
- Eine Zutat darf `"components": { … }` tragen; dann passt nur ein Item mit genau diesen Werten (z. B. `antimatter_cell` mit `"sternenwerft:charge": 1`).
- Ein Ergebnis darf `"copy_components_from": <Zutat-Index>` tragen; dann übernimmt es alle Data Components dieser Zutat (Anzug aufwerten: `suit_modules`, `oxygen`; Antrieb nachrüsten: Ladung der Rakete; `heavy_freighter` aus `cargo_rocket`).
- Die Sondertypen `suit_module_install` (Anzugteil + Modul → Anzugteil mit Modul) und `suit_module_remove` (Anzugteil → Anzugteil ohne Module + Module in die Nebenausgaben) brauchen keine JSON-Einträge je Kombination; sie sind fest im Code und gelten für alle Anzugteile und Module aus 9.2.

## 25.5 Raketenteil (Ordner `rocket_part/`)

```json
{ "block": "sternenwerft:rocket_tank_t1", "role": "tank", "tier": 1, "mass": 2.0, "fuel_capacity": 16000, "fuel_mass_full": 2.0, "thrust": 0, "cargo_slots": 0, "mass_per_slot": 0.0, "seats": 0 }
```

Rollen: `capsule`, `tank`, `engine`, `fin`, `cargo`, `long_range`. `fuel_capacity` gilt je Komponente. Die Treibstoffmasse wächst linear mit dem Füllstand bis `fuel_mass_full`. Frachtraum: `mass` 1,0, `mass_per_slot` 0,05 je belegtem Slot. Kapsel: `seats` 1 (T1) bzw. 2 (T2). Fernflug-Modul: `mass` 4,0, `fuel_capacity` 128000, `fuel_mass_full` 4,0.

## 25.6 Handbuchseite (`assets/<ns>/handbook/<kapitel>/<seite>.json`)

`unlock`: `null` = immer sichtbar (Phase 1). Sonst die ID eines Stufen-Tors (`sternenwerft:research_tier/<n>`) oder eines Forschungsknotens.

```json
{
  "chapter": "sternenwerft:phase_1",
  "title": "sternenwerft.handbook.crusher.title",
  "unlock": null,
  "elements": [
    { "type": "text", "key": "sternenwerft.handbook.crusher.text" },
    { "type": "recipe", "recipe": "sternenwerft:crusher" },
    { "type": "image", "texture": "sternenwerft:textures/handbook/crusher_setup.png", "width": 120, "height": 60 }
  ]
}
```

---

## 25.7 Balance-Dateien (`data/<ns>/sternenwerft/balance/`)

Alle Zahlenwerte, die nicht in Rezepten, Planeten, Forschung oder Raketenteilen stehen, liegen in diesen Dateien. Geladen werden die Dateien `data/<ns>/sternenwerft/balance/*.json` aus allen Namespaces: zuerst `sternenwerft`, dann alphabetisch. Gleichnamige Dateien werden eintragsweise zusammengeführt (spätere ergänzen/überschreiben); `"replace": true` ersetzt die Datei ganz. Das gilt nach `/reload`. Sie werden beim Laden und bei `/reload` eingelesen und an Clients gesendet. `gen_resources.py` kopiert sie aus `spec/data/balance/` (Quelle, Format in `spec/data/SCHEMA.md`); dazu kommen `storage.json` (Speicherblöcke) und `spaceflight.json` (Treibstoffformel, Sonden, Flugablauf).

| Datei | Inhalt (Quelle) | Schlüssel |
| --- | --- | --- |
| `machines.json` | Verbrauch, Dauer, Puffer, Ortsbindung, Upgrades (4.1, 4.2, 6.1) | Block-ID |
| `generators.json` | Leistung, Brennstoff, Speicher, Reaktor-Sicherheit (6.3) | Block-ID |
| `transport.json` | Kabel- und Rohrdurchsatz (4.4) | Block-ID |
| `environment.json` | Belastungszeit, Schadensraten, Schutz ohne Anzug (8.2, 8.3) | Gefahr |
| `suits.json` | Anzugstufen und Module (9.1, 9.2) | Stufe bzw. Modul-ID |
| `domes.json` | Kuppelstufen, Module, Ausfallzeit (10.1, 10.2) | Block-ID |
| `spaceflight.json` | Treibstoffformel, Schub, Sondenzeiten, Landefähre (12.2–12.7) | Schlüssel aus der Formel |
| `vehicles.json` | Rover, Rover-Module (16) | Entity- bzw. Item-ID |
| `creatures.json` | Lebenspunkte, Schaden, Tempo, Spawn (18) | Entity-ID |
| `events.json` | Ereignisse, Chancen, Dauer, Vorwarnung (19) | Ereignis-ID |
| `logistics.json` | Frachtraketen, Flugzeit, Offline-Produktion (20) | Item-ID bzw. Schlüssel |
| `shipyard.json` | Bauabschnitte, Bauzeit (21) | Abschnittsnummer |
| `research.json` | FP-Werte je Quelle, Abnahme, Labor-Bonus, Abklingzeit `sample_tool` (11.1, 11.2) | Quelle |
| `agriculture.json` | Wachstumszeiten, Dünger, Gewächshaus, Ration (17, 14.2) | Block- bzw. Item-ID |
| `stations.json` | Stationskern (Solar, O₂-Tank), Schwerkraftplatte, Klon-Kammer (14) | Block-ID |
| `items.json` | Werkzeug- und Weltwerte: `ice_drill`, `asteroid_anchor`, `hydrothermal_vent`-Abklingzeit, Geysire (13.4) | Item- bzw. Block-ID |

Beispiel `machines.json`:

```json
{
  "sternenwerft:crusher": { "energy_per_tick": 20, "ticks": 200, "buffer": null, "location": null },
  "sternenwerft:zero_g_fabricator": { "energy_per_tick": 100, "ticks": 200, "buffer": null, "location": { "dimension_type": "orbit" } }
}
```

`buffer: null` heißt „Formel aus 4.1 verwenden“.

- **Zusammenführen:** Gleiche Dateien aus mehreren Datenpaketen werden pro Schlüssel zusammengeführt; bei gleichem Schlüssel gewinnt das Paket mit höherer Priorität.
- **`misc.json`** enthält alle übrigen Einzelwerte, z. B. Wartezeit des Wolkenaufzugs (100 t, 12.8), Schwerkraftformel (12.9), Langsamer Fall nach Sturz (400 t, 14.3), Vakuum-Dämpfung (0,15, 24.6), Belastungszeit (200 t).
