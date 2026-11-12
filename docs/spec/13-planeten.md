# 13. Planeten

## 13.1 Planeten-Registry

Datenpaket-Registry `infinite_horizon:planet` (Schema in 25.1). Jede Planetendimension ist eine Datenpaket-Dimension mit eigenem `dimension_type` und eigenen Noise-Settings.

**Umsetzung der Registry (ab M3):** `PlanetRegistry` ist ein Reload-Listener wie `BalanceManager`, keine dynamische Registry von Minecraft (die lädt `/reload` nicht neu). Dateien `data/<ns>/infinite_horizon/planet/*.json` aus allen Namespaces werden zusammengeführt und über den Planeten-Teil von `SyncDataPayload` an Clients gesendet (Join, `/reload`). `gen_resources.py` erzeugt Einträge nur für die Overworld und für die Dimensionen im neuen Schlüssel `dimensions` von `common/implemented_ids.json` (M3: `infinite_horizon:moon`). Verweise auf fehlende Einträge (`orbit`, `lander_connects_to`, `star_system`) sind erlaubt und liefern `null`. Die Laufzeitfelder stehen in 25.1; Biome, Erze, Blöcke, Strukturen, Features und Monster aus `planets.json` gehen nur in die Vanilla-Worldgen-Dateien. Den Planeten einer Position liefert `PlanetResolver` (3A.3).

**Gemeinsame Dimensionswerte (alle Planeten):** `min_y −64`, `height 384`, `bed_works true`, `respawn_anchor_works false`, `has_raids false`, `natural false`, `piglin_safe false`, Tag-Nacht-Zyklus wie Overworld (`fixed_time` nur wo angegeben; die Tageszeit ist die der Overworld). Alle hier nicht genannten Felder wie `minecraft:overworld` aus 26.3 (z. B. `has_skylight`, `has_ceiling`, `ambient_light`, Monster-Lichtwerte); steht ein Wert in 26.3 als Umgebungsattribut, gilt er dort (3.1). Noise-Settings: Kopie von `minecraft:overworld` **aus der Vanilla-Version 26.3** mit den Änderungen aus 13.4, `aquifers_enabled false`, `ore_veins_enabled false`, eigener `default_block`, `default_fluid` nach Tabelle. Die Oberflächenblöcke je Biom (Spalte „Oberfläche“) werden als **Material Rules** umgesetzt (26.3-Bezeichnung für die früheren Surface Rules). Erze und Strukturen sind Features im 26.3-Format (`worldgen/feature` + `worldgen/placed_feature`).

**Noise-Settings im Einzelnen (ab M3):**

- Material Rules: Grundgestein wie in der Overworld, kein Tiefenschiefer (überall `default_block`), die obersten 3 Blöcke aus dem Oberflächenblock des Bioms. `surface_secondary` ersetzt den Oberflächenblock dort, wo das Noise `minecraft:surface` größer als 0 ist.
- `sea_level` aus `planets.json` (ohne Fluid −64).
- Carver: eigene Kopien von `minecraft:cave`, `cave_extra_underground` und `canyon` mit `lava_level` = `min_y` (keine Lava), außer `default_fluid` ist Lava.
- Biome enthalten nur die Mod-Features aus `planets.json` (keine Vanilla-Erze, Quellen, Geoden, Pflanzen) und `has_precipitation false`; Wetter je Planet kommt mit Kapitel 19.
- Der unveränderte Noise-Router erzeugt Ozeanbecken. Ohne Fluid bleiben sie leer; das ist gewollt.

**Biome und Großformen (Umsetzungsregeln):**

- **Gelände:** Der Noise-Router der Overworld bleibt unverändert, Planeten unterscheiden sich über Blöcke, Biome, Carver und Strukturen. **Ausnahme Venus:** Eine Material Rule setzt alle Blöcke ab y 176 auf Luft, damit die Wolkenschicht (y 180–240) frei bleibt. Alle anderen Höhenangaben in 13.4 (z. B. Venus-Hochebenen y 90–110, Europa-Meeresboden) sind ungefähre Zielwerte, keine Pflicht.
- **Biomzuordnung** über `multi_noise`:
  - Bergige oder hohe Biome (Hochland, Vulkane, Gebirge, Hochebenen): `erosion < −0,2`.
  - Flache Biome (Mare, Ebenen, Seen, Dünen, Eisebenen): `erosion > 0,45`.
  - Übrige: dazwischen.
  - Seltene Biome (Polarkrater, Polkappe, Meteoritenfeld, Schattenkrater, Geysirfelder), je ca. 5 % Fläche: das erste seltene Biom eines Planeten (Reihenfolge in `planets.json`) über `temperature < −0,8`, ein zweites über `weirdness > 0,8`.
  - **Parameter:** Alle Werte von −1 bis 1, außer Erosion (bergig −1 bis −0,2, flach 0,45 bis 1, übrige −0,2 bis 0,45) und den Achsen seltener Biome: Belegt ein seltenes Biom eine Achse, nutzen alle anderen Biome des Planeten auf ihr nur den Rest (Temperatur −0,8 bis 1, Weirdness −1 bis 0,8). `offset` 0.
- **Eigene `BiomeSource`:**
  - Merkur: Biom nach X-Koordinate (13.4), Schattenkrater als seltene Einsprengsel.
  - Pluto: `pluto_heart_glacier` im Radius 400 um (0, 0), außerhalb `multi_noise`.
- **Europa:** Noise-Settings mit Meeresspiegel y 120 und `default_fluid` Wasser. Ein Feature `ice_shell` füllt y 112–124 mit `europa_ice`, außer in `europa_rifts` (Spalten 3–6 Blöcke breit).
- **Großformen als Strukturen** (nicht als Features, wegen der Chunk-Grenzen):
  - `great_crater` auf dem Mond (Radius 40–60, Zentralberg, Erze ×2 innerhalb der Struktur), `giant_volcano` auf dem Mars (Radius 80, Höhe bis y 250), `large_asteroid` im Asteroidengürtel (Radius 13–20, dazu der Spawn-Asteroid).
  - `lava_tube`, `volcanic_vent`, `meteorite_field`, `hydrothermal_field` (Quellen-Cluster), `ice_volcano`, `signal_transmitter`.
- **Erkundungsziele** sind genau die Strukturen `lava_tube`, `volcanic_vent`, `meteorite_field`, `hydrothermal_field`, `ice_volcano` und `signal_transmitter`; ihre Strukturinstanz ist die Erkundungsziel-ID (11.1): Dimension + Struktur-ID + Start-Chunk. Betreten heißt, in einem Teil der Struktur zu stehen (ab M4, 11.1). Großkrater und Riesenvulkan zählen nicht.
- **Mars-Canyon:** Vanilla-Canyon-Carver mit Tiefe bis y 0 und doppelter Breite.

**Orbit-Dimensionen** (`earth_orbit`, `venus_orbit`, `mars_orbit`, `mercury_orbit`, `jupiter_orbit`, `saturn_orbit`, `uranus_orbit`, `neptune_orbit`): Leere Welt (`minecraft:flat` mit leerer Schichtliste), `min_y 0`, `height 256`, `fixed_time 6000`, Himmel schwarz, Planet groß unter dem Spieler (Sky-Renderer zeichnet ihn auch unterhalb des Horizonts). Temperatur −100 bis +100 °C, Strahlung 1 (Jupiter-Orbit 3), Atmosphäre `none`. Solarfaktor im Orbit (ohne Atmosphäre): Erde 1,0, Venus 2,0, Merkur 3,0, Mars 0,5, Jupiter 0,1, Saturn 0,05, Uranus und Neptun 0,02.

## 13.2 Werte je Planet

> **Maßgebliche Daten:** `spec/data/planets.json` (Schwerkraft, Atmosphäre, Druck, Temperaturen, Strahlung, Solarfaktor, Wasserverhalten, Himmel, Zonen je Ziel).
> **Lesefassung:** `docs/spec/generated/planets.md`.

Gasplaneten (Jupiter, Saturn, Uranus, Neptun) haben nur Orbit-Dimensionen.

**Mindestausrüstung** (folgt aus 9.1, Grenzwerte inklusiv):

| Ort | Mindestens |
| --- | --- |
| Mond (ohne Polarkrater), Mars, Ceres, Asteroidengürtel, Titan | T1 |
| Mond-Polarkrater (−190 °C), Venus-Oberfläche, Enceladus (−200 °C) | T2 |
| Merkur: Dämmerungsgürtel, Frostebenen, Schattenkrater | T1 + `radiation_shield` oder T3 |
| Merkur: Glutebenen (+430 °C) | T2 + `radiation_shield` oder T3 |
| Io | T1 + `radiation_shield`; an Lava (+300 °C) T2 + `radiation_shield` |
| Europa | T1 + `radiation_shield` oder T3; im Ozean unter y 80 zusätzlich `diving_module` |
| Pluto (−230 °C) | T2 + `heating_module` |
| Triton (−235 °C) | T3 + `heating_module` |
| Venus-Wolkenschicht, Orbits außer Jupiter | T1 |
| Jupiter-Orbit | beliebiger Anzug + `radiation_shield` |

Sonnenstürme heben die Strahlung um 2 (Maximum 3, siehe 8.1). Dagegen schützen `radiation_shield`, Kuppel T3 oder 3 Blöcke Deckung.

## 13.3 Gas-Sammler je Orbit

| Orbit | Gas | Menge |
| --- | --- | --- |
| `jupiter_orbit` | `hydrogen` | 100 mB / 20 t |
| `saturn_orbit` | `helium` | 100 mB / 20 t |
| `uranus_orbit`, `neptune_orbit` | `methane` (flüssig) oder `hydrogen`, umschaltbar per Knopf im GUI, Standard `methane` | 100 mB / 20 t |

## 13.4 Biome, Blöcke, Erze, Strukturen

> **Maßgebliche Daten:** `spec/data/planets.json` (Biome, Blöcke mit Drops, Erze mit y-Bereich, Adergröße und Häufigkeit, Strukturen). Die Abschnitte unten beschreiben Aufbau und Sonderregeln; Zahlen darin dienen nur der Orientierung, bei Abweichung gelten die Daten.

**Erze-Notation:** `Block` (y-Bereich, Adergröße, Adern je Chunk).

**Drops (Pauschalregel, gilt wo nichts anderes steht):**
- `*_<metall>_ore` → 1 `raw_<metall>` (Glück wie bei Vanilla-Erzen).
- `bauxite_ore` und `*_aluminum_ore` → 1 `bauxite`.
- `*_sulfur_ore` → 2–4 `sulfur`.
- `deepslate_*`-Varianten droppen wie die normale Variante.
- Planetengestein und Böden (`*_rock`, `*_crust`, `*_ice`, `*_sand`, `*_regolith`, `*_snow`, `*_tholin`, `*_basalt`, `*_seabed`) droppen sich selbst.
- Unzerstörbar (wie Grundgestein): `hydrothermal_vent`, `ammonia_geyser`, `nitrogen_geyser`, `transmitter_core`, `ancient_alloy`.

### Erde (Ergänzungen zur Overworld)

| Element | Werte |
| --- | --- |
| `bauxite_ore` | in Savanne, Wüste, Badlands: y 50–120, Ader 12, 6 je Chunk |
| `titanium_ore` / `deepslate_titanium_ore` | alle Biome: y −64–0, Ader 6, 4 je Chunk |
| `quartz_sand` | Strand und Flussufer: Scheibe wie Ton, Radius 2–4, 2 je Chunk |
| Ölblase | Wüste und Ozeanboden: Kugel Radius 3–5 aus `crude_oil`-Quellblöcken, y −20–40, 1 je 8 Chunks |

**Regeln für die Erde (Meilenstein 2):**

- **Erze (`bauxite_ore`, `titanium_ore`):** wie Vanilla-Eisenerz. Höhe gleichmäßig (`uniform`), `discard_chance_on_air_exposure` 0, „je Chunk“ sind die Versuche je Chunk (`count`). `bauxite_ore` ersetzt Blöcke aus `#minecraft:stone_ore_replaceables`, auch in Wüste und Badlands (nicht Sandstein oder Terrakotta); bei y 50–120 liegt kein Tiefenschiefer. `titanium_ore` ersetzt `#minecraft:stone_ore_replaceables`, `deepslate_titanium_ore` ersetzt `#minecraft:deepslate_ore_replaceables`.
- **Biome:** „Savanne“ ist `#minecraft:is_savanna` (Savanne, Savannenplateau, Windgepeitschte Savanne), „Wüste“ ist `minecraft:desert`, „Badlands“ ist `#minecraft:is_badlands`. „Alle Biome“ ist `#minecraft:is_overworld`.
- **Eigenschaften und Drops:** `bauxite_ore` und `titanium_ore` wie `minecraft:iron_ore`, `deepslate_titanium_ore` wie `minecraft:deepslate_iron_ore` (Härte, Sprengwiderstand, Klang, Glück wie Vanilla-Eisen). Werkzeugstufe: `bauxite_ore` braucht eine Steinspitzhacke (Tag `minecraft:needs_stone_tool`), `titanium_ore` und `deepslate_titanium_ore` eine Eisenspitzhacke (`minecraft:needs_iron_tool`). Die Drops stehen in der Pauschalregel oben.
- **`quartz_sand`:** Feature `disk`: Radius 2–4 (`uniform`), Halbhöhe 1, Ziele `minecraft:sand`, `minecraft:dirt`, `minecraft:clay`, Platzierung `OCEAN_FLOOR_WG`, 2 Versuche je Chunk, Biome `#minecraft:is_beach` und `#minecraft:is_river`. Eigenschaften wie `minecraft:sand` (Schaufel), aber kein Fallblock. Der Block droppt sich selbst.
- **Ölblase:** nur in Wüste (`minecraft:desert`) und Ozeanen (`#minecraft:is_ocean`), gemessen am Biom des Mittelpunkts. Mittelpunkt `uniform` y −20 bis 40, Radius `uniform` 3 bis 5, Wahrscheinlichkeit 1/8 je Chunk. Die Kugel ersetzt alle Blöcke außer Bedrock, Fluiden und Luft; Luft bleibt Luft, damit Höhlen nicht fluten. Sie liegt damit teils frei in Höhlen. Es gibt keinen Hinweis an der Oberfläche (Handbuch, M9). Die Fluid-Eigenschaften von `crude_oil` stehen in 5.3.

### Mond `moon`

Blöcke: `moon_regolith` (Oberfläche, 3 Schichten), `moon_rock` (Gestein = `default_block`), `moon_basalt`, `moon_ice`. `default_fluid` = Luft.

| Biom-ID | Oberfläche | Besonderheit | Erze |
| --- | --- | --- | --- |
| `moon_highlands` | `moon_regolith` | zerklüftet, höhere Berge | `moon_titanium_ore` (y 0–100, 6, 3) |
| `moon_mare` | `moon_basalt` | flach, dunkel | `moon_aluminum_ore` (y 0–80, 8, 5), `moon_titanium_ore` (y 0–80, 6, 4) |
| `moon_great_crater` | `moon_regolith` | Struktur `great_crater` erscheint nur in diesem Biom | wie `moon_mare`; innerhalb der Struktur doppelt |
| `moon_polar_crater` | `moon_regolith` + `moon_ice` | selten (5 %), Temperatur fest −190 (braucht Anzug T2), keine Sonnenenergie (`solar_factor` 0) | `moon_ice` (y 40–100, 16, 4) |

Strukturen: `great_crater` (nur im Biom `moon_great_crater`, Radius 40–60, Zentralberg, Abstand 32 Chunks, Trennung 8; „Erze ×2“ heißt: Die Struktur setzt die Erzadern ihres Bioms in ihrem Volumen ein zweites Mal). `lava_tube` (Mare: gerader Tunnel 5×5 in zufälliger Himmelsrichtung, Länge 60–120, y 30–50, 1 je 16 Chunks, Trennung 4; zählt ab M4 als Erkundungsziel, 11.1). Feature `small_crater`: kleine Krater in allen Biomen, Radius 4–12, Tiefe = Radius / 3, Rand 1 Block hoch aus dem Oberflächenblock, 1 Versuch je 4 Chunks.
`moon_aluminum_ore` droppt `bauxite`, `moon_titanium_ore` droppt `raw_titanium`, `moon_ice` droppt `minecraft:ice`.

**Eigenschaften der Mondblöcke (ab M3):** `moon_rock` wie `minecraft:stone`, droppt sich selbst. `moon_basalt` wie `minecraft:basalt` (Tag `infinite_horizon:basalt`). `moon_regolith` wie `minecraft:gravel`, aber kein Fallblock (Schaufel). `moon_ice` mit Härte und Klang von `minecraft:packed_ice`, schmilzt nicht und droppt immer `minecraft:ice`. Die Erze wie `minecraft:iron_ore` (Härte, Glück): `moon_aluminum_ore` braucht eine Steinspitzhacke, `moon_titanium_ore` eine Eisenspitzhacke (wie auf der Erde). Platzierung wie auf der Erde (`uniform`, „je Chunk“ = `count`), Wirtsblock `moon_rock`.

### Mars `mars`

Blöcke: `mars_sand`, `mars_rock` (`default_block`), `mars_magnesium_ore`, `mars_iron_ore`, `gypsum_block`, `dry_ice_block`, `iron_nickel_meteorite`.

| Biom-ID | Oberfläche | Besonderheit | Erze |
| --- | --- | --- | --- |
| `mars_red_plains` | `mars_sand` | flach bis hügelig | `mars_magnesium_ore` (y 0–80, 8, 5), `mars_iron_ore` (y 0–80, 8, 4) |
| `mars_canyon` | `mars_rock` | tiefe Schluchten bis y 0, Erzschichten freiliegend | Erze ×2 |
| `mars_volcanic_highlands` | `mars_rock` | hohe Berge, eine Riesenvulkan-Struktur je 64 Chunks | – |
| `mars_dry_lake` | `gypsum_block` | flache Senke | `gypsum_block` Oberfläche |
| `mars_polar_cap` | `dry_ice_block` | selten | – |
| `mars_meteorite_field` | `mars_sand` | Meteoriten-Feature: kleine Krater mit `iron_nickel_meteorite` | – |

Strukturen: `lava_tube` (Hochland), `meteorite_field` (Struktur im Biom `mars_meteorite_field`, 1 je 8 Chunks). Drops: `iron_nickel_meteorite` → 1–2 `raw_nickel`, 50 % 1 `raw_iron`. `gypsum_block` → 4 `gypsum`. `dry_ice_block` → 4 `dry_ice`.

### Venus `venus`

Blöcke: `venus_rock` (`default_block`), `venus_tungsten_ore`, `venus_sulfur_ore`, Vanilla `basalt`, `magma_block`, Lava (`default_fluid` Lava, Meeresspiegel y 40).

| Biom-ID | Oberfläche | Besonderheit | Erze |
| --- | --- | --- | --- |
| `venus_lava_plains` | Vanilla `basalt` | Lavaseen | `venus_sulfur_ore` (y 40–100, 8, 4) |
| `venus_volcanoes` | `venus_rock`, `magma_block` | Vulkankegel mit Lavasee | `venus_tungsten_ore` (y 40–140, 6, 5) |
| `venus_plateau` | `venus_rock` | flache Hochebenen y 90–110 | – |

Strukturen: `volcanic_vent` (Erkundungsziel, an Vulkanen). Wolkenschicht y 180–240: keine Terrain-Generierung, dichter Nebel.

### Merkur `mercury`

Blöcke: `mercury_rock`, `mercury_lithium_salt`, `mercury_iron_ore`, `mercury_titanium_ore`.

| Biom-ID | Temperatur | Erze |
| --- | --- | --- |
| `mercury_scorched_plains` (Tagseite) | +430 | `mercury_titanium_ore` |
| `mercury_frost_plains` (Nachtseite) | −180 | `mercury_iron_ore` |
| `mercury_twilight_belt` | +20 | `mercury_lithium_salt` als Oberflächenkruste (Scheibe Radius 3–6, 3 je Chunk) |
| `mercury_shadow_crater` | −150 | `minecraft:ice` |

Biomverteilung über eine eigene `BiomeSource` nach X-Koordinate (13.1): Tagseite für X > 2.000, Nachtseite für X < −2.000, Dämmerungsgürtel dazwischen. Spawn bei (0, 0). `mercury_lithium_salt` → 2 `lithium_salt`.

### Asteroidengürtel `asteroid_belt`

Leere Dimension. Feature `asteroid`: Kugeln/Ellipsoide Radius 3–12 (größere als Struktur `large_asteroid`, 13.1) aus `asteroid_rock` mit Erzen (`asteroid_platinum_ore` 4 %, `asteroid_iridium_ore` 2 %, `asteroid_nickel_ore` 6 % der Blöcke), y 20–220, 1 je Chunk mit 40 % Chance. `asteroid_anchor`: Block auf einem Asteroiden setzt dort 1 g für Spieler im Radius 16 (Pseudo-Schwerkraft zum Basisbau).
**Spawn:** Ein garantierter Asteroid mit Radius 15 bei (0, 80, 0) mit allen drei Erzen. Fällt ein Spieler unter y 0, wird er mit Langsamem Fall (400 t) auf diesen Asteroiden zurückgesetzt.

### Ceres `ceres`

Blöcke: `ceres_rock`, `salt_crust` (Oberflächenflecken), `minecraft:ice`. Biome: `ceres_plains`, `ceres_salt_flats`. Struktur: `ice_volcano` (Kegel aus Eis, 1 je Dimension nahe Spawn, Erkundungsziel).

### Io `io`

Blöcke: `io_crust`, `io_sulfur_ore`, Lava. Biome: `io_sulfur_plains`, `io_lava_lakes`, `io_volcanic_fields`. Struktur `volcanic_vent`. Ereignis Vulkanausbruch (19).

### Europa `europa`

Blöcke: `europa_ice` (Härte 50, nur mit `ice_drill` effizient abbaubar), `europa_seabed`, `hydrothermal_vent`. Aufbau: Eisdecke y 112–124, darunter Wasser bis Meeresboden y −40 bis 0.

| Biom-ID | Besonderheit |
| --- | --- |
| `europa_ice_plains` | geschlossene Eisdecke |
| `europa_rifts` | Spalten mit Zugang zum Wasser |
| `europa_ocean` | Unterwasserbiom unter der Eisdecke, Tiefseedruck unter y 80 |
| `europa_vents` | Meeresboden mit `hydrothermal_vent`-Clustern (Struktur `hydrothermal_field`, Erkundungsziel) |

`hydrothermal_vent`: Rechtsklick mit einer leeren Glasflasche (`minecraft:glass_bottle`, wird verbraucht) gibt 1 `microbe_culture` (Abklingzeit 1.200 t pro Block). Das ist unabhängig von Proben (11.2), dort gilt weiter „pro Block-Position einmal“.
`ice_drill`: Werkzeug, Rechtsklick gedrückt halten bohrt einen 3×3-Schacht nach unten (1 Schicht / 10 t, 20 J pro Block aus internem Akku 100.000 J).

### Titan `titan`

Blöcke: `titan_dune_sand`, `titan_ice_rock`, `methane`-Quellblöcke (Meeresspiegel y 62, `default_fluid` = `methane`). Biome: `titan_methane_lakes`, `titan_dunes`, `titan_ice_mountains`. Die dichte Luft erlaubt Elytra-Gleiten ohne Feuerwerk (Sinkrate halbiert).

### Enceladus `enceladus`

Blöcke: `enceladus_snow`, `enceladus_ice`, `ammonia_geyser`. Biome: `enceladus_snowfields`, `enceladus_tiger_stripes` (blaue Streifen, Geysire 1 je Chunk). Geysir: alle 200 t Fontäne 40 Blöcke hoch. Entities darin bekommen Geschwindigkeit +2,0 nach oben.

### Triton `triton`

Blöcke: `triton_ice` (rosa-grau), `nitrogen_geyser`. Biome: `triton_cantaloupe_terrain` (wellige Hügel), `triton_polar_cap`, `triton_geyser_fields`.

### Pluto `pluto`

Blöcke: `pluto_nitrogen_ice`, `pluto_water_ice`, `pluto_tholin`, `pluto_uranium_ore` (nur in `pluto_dark_highlands`, y 0–60, Ader 4, 3 je Chunk). Biome: `pluto_heart_glacier`, `pluto_ice_mountains`, `pluto_dark_highlands`.
Struktur **`signal_transmitter`**: genau eine, unter dem Herzgletscher bei (0, 30, 0) ± 64. Halle 32×32×16 aus `ancient_alloy` (unzerstörbar wie Grundgestein), in der Mitte `transmitter_core`. **Zugang:** eine 3 breite, 4 hohe Öffnung in der Südwand. Von dort führt ein Treppentunnel (3 breit, 4 hoch) aus `pluto_water_ice` zur Oberfläche, oben markiert durch einen 5×5-Ring aus `ancient_alloy`. Rechtsklick auf den Kern beschwört `signal_sentinel`, wenn für die Agentur keiner lebt und keiner in den letzten 24.000 t besiegt wurde.

## 13.5 Monster mit Atemmaske

In Planetendimensionen spawnen diese Vanilla-Monster nachts bzw. bei Licht ≤ 7 mit einer Atemmasken-Render-Ebene (Kopf-Overlay):

| Dimension | Monster | Häufigkeit |
| --- | --- | --- |
| `moon` | Zombie, Skelett, Spinne, Creeper | 50 % Overworld |
| `mars` | Zombie, Skelett, Spinne, Creeper | 100 %, im Staubsturm 200 % |
| `venus` (Oberfläche) | Lohe, Magmawürfel | 100 % |
| `io` | Magmawürfel | 50 % |
| `titan` | Zombie, Skelett | 50 % |
| `triton`, `pluto` | Eiswanderer (Stray) | 50 % |
| andere | keine | |

Monster brauchen keinen Sauerstoff und nehmen keinen Umweltschaden. Mit `planet_mobs = false` spawnen keine.

**Umsetzung (ab M3, `misc.json` → `planet_mob_rules`):** Die Biome tragen die Spawngewichte der Overworld. Die Häufigkeit (`rate` in `planets.json`) ist die Wahrscheinlichkeit, dass ein natürlicher Spawn bestehen bleibt; bei 50 % bricht das Spawn-Ereignis jeden zweiten ab. Die Lichtregel ist die der Overworld; „Licht ≤ 7“ ist so zu lesen. Die Atemmaske ist eine Render-Ebene am Kopf dieser Arten in jeder Dimension mit Planeten-Eintrag außer der Overworld (Platzhalter 24.5). Die Monster verbrennen weiter im Sonnenlicht.
