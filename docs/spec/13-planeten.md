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
| `quartz_sand` | Strand und Flussufer: Scheibe mit Radius 1 in einer Schicht (höchstens 5 Blöcke, Ä31), 2 je Chunk |
| Ölpfütze (Ä31, statt Ölblase) | Wüste und Tafelberge: runde Mulde an der Oberfläche aus `crude_oil`-Quellblöcken, Radius 2–3, innen bis 2 Blöcke tief, 1 je 8 Chunks |

**Regeln für die Erde (Meilenstein 2):**

- **Erze (`bauxite_ore`, `titanium_ore`):** wie Vanilla-Eisenerz. Höhe gleichmäßig (`uniform`), `discard_chance_on_air_exposure` 0, „je Chunk“ sind die Versuche je Chunk (`count`). `bauxite_ore` ersetzt Blöcke aus `#minecraft:stone_ore_replaceables`, auch in Wüste und Badlands (nicht Sandstein oder Terrakotta); bei y 50–120 liegt kein Tiefenschiefer. `titanium_ore` ersetzt `#minecraft:stone_ore_replaceables`, `deepslate_titanium_ore` ersetzt `#minecraft:deepslate_ore_replaceables`.
- **Biome:** „Savanne“ ist `#minecraft:is_savanna` (Savanne, Savannenplateau, Windgepeitschte Savanne), „Wüste“ ist `minecraft:desert`, „Badlands“ ist `#minecraft:is_badlands`. „Alle Biome“ ist `#minecraft:is_overworld`.
- **Eigenschaften und Drops:** `bauxite_ore` und `titanium_ore` wie `minecraft:iron_ore`, `deepslate_titanium_ore` wie `minecraft:deepslate_iron_ore` (Härte, Sprengwiderstand, Klang, Glück wie Vanilla-Eisen). Werkzeugstufe: `bauxite_ore` braucht eine Steinspitzhacke (Tag `minecraft:needs_stone_tool`), `titanium_ore` und `deepslate_titanium_ore` eine Eisenspitzhacke (`minecraft:needs_iron_tool`). Die Drops stehen in der Pauschalregel oben.
- **`quartz_sand`:** Feature `disk`: Radius 2–4 (`uniform`), Halbhöhe 1, Ziele `minecraft:sand`, `minecraft:dirt`, `minecraft:clay`, Platzierung `OCEAN_FLOOR_WG`, 2 Versuche je Chunk, Biome `#minecraft:is_beach` und `#minecraft:is_river`. Eigenschaften wie `minecraft:sand` (Schaufel), aber kein Fallblock. Der Block droppt sich selbst. Seit Ä31 Radius 1 und Halbhöhe 0 (`half_height` in planets.json): eine Ader hat höchstens 5 Blöcke, die Häufigkeit bleibt.
- **Ölpfütze (Ä31, ersetzt die Ölblase):** Feature `fluid_pool` (Feature-Typ `infinite_horizon:fluid_pool`), nur in Wüste (`minecraft:desert`) und Tafelbergen (`#minecraft:is_badlands`). Platzierung über `WORLD_SURFACE_WG`; runde Mulde mit Radius `uniform` 2–3, Felder im inneren halben Radius `uniform` 1–2 Blöcke tief, sonst 1; Wahrscheinlichkeit 1/8 je Chunk. Fehlt ein fester Rand oder Boden (Abhang, Höhle, Wasser), entsteht nichts; Pflanzen über der Pfütze verschwinden. Die Kugel `fluid_sphere` bleibt für Addons registriert.

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

## Festlegungen aus der Umsetzung (M1–M8)

Annahmen aus der Umsetzung, die übernommen wurden. Die Nummer verweist auf den Eintrag in `docs/spec-gaps.md`, dort steht auch die Code-Stelle.

- **#3 (Datenaufbau, 13.4):** Meeresspiegel Io (Lava) fehlt → `null`
- **#4 (Datenaufbau, 13.1):** Orbits und Asteroidengürtel ohne eigenes Biom → `minecraft:the_void`
- **#5 (Datenaufbau, 13.4):** `default_block` für Merkur, Ceres, Io, Europa, Titan, Enceladus, Triton, Pluto nicht genannt → jeweils das Hauptgestein (z. B. `mercury_rock`)
- **#46 (M3c, 13.4):** Form des Großkraters: Tiefe und Zentralberg stehen nicht in planets.json → Tiefe = Radius / 3 und Rand 1 wie `small_crater`; Zentralberg mit Radius / 5, er ragt vom Kraterboden bis 2/3 der Tiefe auf. Die Werte stehen in `structure/great_crater.json` (`gen_resources.py`, `GREAT_CRATER_SHAPE`). „Erze ×2“: In jedem Chunk, dessen Mitte im Krater liegt, setzt die Struktur die Erz-Features des Bioms noch einmal
- **#48 (M3c, 13.1):** 26.3 hat kein `lava_level` an Carvern, keine `aquifers_enabled`/`ore_veins_enabled` und kein `bed_works`, `has_raids`, `natural`, `piglin_safe`, `fixed_time` am Dimensionstyp → Vanilla-Carver `cave`, `cave_extra_underground`, `canyon` unverändert: Lava entsteht nur unter min(−54, `sea_level`), mit `sea_level` −64 also nie. Ohne `aquifers`-Eintrag keine Aquifere; Erzadern gehören zur Overworld-Material-Rule und fehlen in unserer. Dimensionstyp: Felder der Overworld 26.3, dazu die Attribute `gameplay/bed_rule` (Overworld-Regel), `respawn_anchor_works`, `can_start_raid`, `nether_portal_spawns_piglin` aus `dimension_defaults`, Himmel- und Nebelfarbe und `star_brightness` 1 bei `stars_in_daylight`. Oberflächenregel wie Vanilla nur über `above_preliminary_surface`, obere 3 Blöcke per `stone_depth` (offset 2)
- **#49 (M3c, 13.5):** Spawngewichte „wie Overworld“ und die Spawnrate brauchen Daten, die in den Laufzeit-Planetendateien (25.1) nicht vorkommen → Gewichte und Gruppengrößen der vier Arten aus Vanilla 26.3 `plains` (`OVERWORLD_MONSTER_SPAWNS` im Generator). Die Planetendatei bekommt zusätzlich `vanilla_mobs`; ein Mixin auf `NaturalSpawner.isValidPositionForMob` bricht natürliche Spawns mit Wahrscheinlichkeit 1 − `rate` ab, mit `planet_mobs = false` alle Monster
- **#51 (M3c, 13.1):** Vanilla stuft Dimensionen außerhalb von `minecraft` als experimentell ein (`WorldDimensions.checkStability`); NeoForge zeigt dann beim Erstellen jeder Welt den Hinweis „Experimentelle Einstellungen“ (Fabric API hebt das auf) → Mixin `WorldDimensionsMixin`: Dimensionen im Namespace `infinite_horizon` gelten als stabil
- **#53 (M3c, 13.5, 24.5):** Atemmaske „64 × 32“: Zombies haben eine 64 × 64-Textur, die Spinne ihren Kopf an anderer Stelle → Eine Masken-Textur je Art in der Größe ihres Modells (`textures/entity/breathing_mask/<art>.png`), untere Hälfte des Kopfs `#6E6E6E` mit Filter; die Ebene zeichnet das Modell der Kreatur noch einmal mit dieser Textur (wie das Wolfshalsband). Die Arten kommen aus `vanilla_mobs` der Planeten (`infinite_horizon/planet_mobs.json`)
- **#87 (M5a, 13.4):** Form von Riesenvulkan, Vulkanschlot und Meteoritenfeld; Häufigkeit von Mars-Lavaröhre und Vulkanschlot; Meteoritenkrater und Mars-Canyon ohne Werte → Eine Strukturklasse `SurfaceStructure`: Vulkan als Kegel bis `max_y` mit Lavakrater (Radius / 8), Schlot als Basalthügel (Radius 4–6, Höhe 3) mit Magmarand und Lava, Meteoritenfeld mit 3–5 Kratern (Radius 3–6) im Umkreis 20 und Meteoritenblock. Ohne Abstand gilt der der Mond-Lavaröhre, Trennung = Abstand / 4. Meteoritenkrater als Feature: Radius 2–4, je 2 Chunks. Canyon: Vanilla-Canyon mit doppelter Breite, Mitte y 0–67, Chance 0,1 je Chunk. Werte in `RING1_SHAPES` (gen_resources.py)
- **#102 (M7b, 13.4, 18):** Asteroidengürtel und Ceres: Häufigkeit großer Asteroiden, Maße des Eisvulkans, feste Lage → Leere Welt als Noise-Generator mit Dichte −1 und fester Biomquelle `asteroid_field` (Feature `asteroid` darin; der flache Generator setzt in leeren Welten nur beim Biom `the_void` Features, das den Himmel blau färbt). Große Asteroiden als Struktur mit dem Abstand der Mond-Lavaröhre, Mitte auf y 20–220; Spawn-Asteroid und Eisvulkan als Strukturen mit fester Lage (neue Platzierung `infinite_horizon:fixed`: der Chunk mit dem Block x/z, Mitte = Chunkmitte, also 8/80/8). Eisvulkan Radius 16–20, 24 Blöcke hoch, Krater Radius / 5, aus Eis mit Packeis-Rand. Asteroiden als Kugel oder (bei jedem zweiten) Ellipsoid mit Achsen 70–100 % des Radius; Erz je Block über einen festen Hash. Driftmilben: 5–10 je Asteroid mit Platinerz, nur mit der Welt, schweben, verschwinden nicht. Asteroidenanker nur auf `asteroid_*`-Blöcken, wirkt als Kugel um den Block. Landung im Gürtel: höchster Block der Spalte, sonst Schweben wie im Orbit. Der GameTest-Server erzeugt keine Strukturen; Tests setzen sie wie `/place structure`
- **#104 (M7c, 13.3, 13.4, 14.3, 18, 19):** Jupiter-Orbit, Gas-Sammler, Io: offene Werte → Gas-Sammler: Menge und Dauer aus `gas_collector_output` des Planeten (machines.json nennt dieselben Werte), ohne Zutat. Fall im Gasriesen-Orbit: nächster Stationskern der Dimension ohne Reichweitengrenze, Stehplatz über dem Kern; Ersatzplattform nur aus Luftblöcken, Schaden als Fallschaden. Io: Lava-Meeresspiegel 40 wie die Venus; Schwefelerz y 40–100, Ader 8, 4 je Chunk wie auf der Venus; `io_crust` mit Eigenschaften von gelber Keramik. Schwefelsalamander: Angriff alle 60 t im Umkreis 16 und Streuung wie die Lohe; der Klumpen fliegt wie der kleine Feuerball, Schaden = Angriffsschaden des Salamanders als Projektilschaden. In `volcanic_vent` überschreibt die Struktur die Monster-Spawns (`spawn.structures`), deshalb spawnen Kreaturen nur in ihrer eigenen Dimension (`spawn.dimension`); auf dem Mars spawnen in Schloten keine Monster mehr. Vulkanausbruch: `chance_per` = „<struktur>_within_<r>_of_a_player“, je Schlot in geladenen Chunks; das Ereignis liegt mit Ort im Ereignisplan der Dimension (Warnung wie andere Ereignisse an alle in der Dimension); alle 5 t fällt ein Brocken aus 24 Blöcken über dem Boden, Schaden 1 je Block Fall, höchstens 6 (`interval_ticks`, `drop_height`, `hurt_per_block`, `hurt_max` in den Effekten änderbar). Jupiter am Himmel in Beige #C9A27A (24.5 nennt keine Farbe)
- **#105 (M7c, 13.1, 13.4, 9.2, 14.4, 18):** Europa, Eisbohrer, Tauchmodul, Klon-Kammer, Kreaturen der Europa: offene Werte → Eisdecke: Feature `ice_shell` läuft auch in `europa_rifts` und lässt dort Risse offen (Nulllinie eines Rauschens im Maßstab 48, Breite 3–6 aus einem zweiten Rauschen im Maßstab 96); gefüllt werden nur Luft und Wasser. Gelände aus dem Overworld-Router (Meeresboden etwa y 30–110 statt −40–0, laut 13.1 erlaubt); unter Wasser ist die Oberfläche `default_block`. `europa_ice` mit Eigenschaften von Obsidian (Härte 50), mit jeder Spitzhacke abbaubar. Quellenfeld: 4–7 Quellen im Umkreis 5–8 auf Schloten 0–2 hoch, Abstand wie die Lavaröhre; der Riffwächter entsteht mit dem Feld (`spawn.structure`). Eisbohrer: Schicht unter den Füßen, Luft, Quellblöcke von Fluiden, Block-Entities und unzerstörbare Blöcke bleiben; Drops wie beim Abbau. Tauchmodul: `swim_speed_multiplier` − 1 als Attribut `water_movement_efficiency`. Klon-Kammer: Rechtsklick bindet und öffnet das GUI; „hat Strom“ = konnte in diesem Tick `energy_per_tick` ziehen; Respawn-Ziel ist der Block über der Kammer. Eisqualle: Ziel alle 40 t neu, Quellen im Würfel ±8 direkt, weiter weg die Mitte des nächsten Quellenfelds in geladenen Chunks. Riffwächter: Angriff wie der Delfin (Nahkampf, Wasser-Navigation), wehrt sich gegen Angreifer, Beruhigung je Spieler gespeichert. 26.3 zählt in den Bewegungs-Heightmaps nur Blöcke aus `#minecraft:blocks_motion_no_leaves`; alle festen Mod-Blöcke stehen jetzt darin (vorher galten sie dort als Luft)
- **#106 (M7d, 13.4, 6.1, 10.1, 18, 19):** Saturn-System, Magnetfalle, Kuppel T3: offene Werte → Titan: Methan ist ein Weltfluid mit Quellblock (Fließwerte wie Erdöl), Meeresspiegel aus planets.json; Elytren: solange der Spieler gleitet, gilt Schwerkraft × `elytra_sink_rate_multiplier` (Takt der Schwerkraft, also mit kurzer Verzögerung); „ohne Feuerwerk“ braucht keine eigene Regel. Methanregen schaltet nur das Wetter; die Sicht × 0,5 folgt mit dem grafischen Feinschliff wie der Nebel des Staubsturms (#93), der Bonus der Pumpstation mit M8. Methanläufer: steht wie der Schreiter auf dem Fluid (halbe Blockhöhe), flieht geradlinig, sonst alle 60 t eine neue Richtung oder Stillstand. Enceladus: Geysir im obersten Block der Oberfläche (eigenes Feature, 1 je Chunk); die Fontäne dauert Höhe ÷ Steiggeschwindigkeit Ticks, jeder Geysir mit eigenem Versatz, Entities in der Säule (1×1, etwas breiter) steigen mit `entity_velocity_up`. Geysirmotte: Suche alle 100 t im Würfel ±16/±8, kreist 3 Blöcke über dem Geysir. Ringtrümmer: Einschläge wie der Meteoritenschauer ohne Erz. Magnetfalle: Ein- und Ausgabe aus `process` in machines.json (ganzzahlige Komponenten, fehlend = 0); gefüllte Zellen schimmern, Stapel bis 16. Kuppel T3 nutzt den vorhandenen Kuppelgenerator mit den Werten aus domes.json. Saturn am Himmel in #D8C08A ohne Ringe (Platzhalter)
- **#107 (M7e, 13.3, 13.4, 9.2, 18):** Uranus/Neptun, Triton, Tiefraumanzug, Heizmodul: offene Werte → „Knopf im GUI“ = allgemeine Betriebsart der Maschine (Sync-Wert `SYNC_MODE`, Knopf „Modus“ an der Stelle des Tank-Knopfs); der Gas-Sammler hat zwei Arten, wenn der Planet `alternative` nennt, die Wahl bleibt beim Wechsel erhalten; ist noch das andere Gas im Tank, meldet er `error`. Stickstoff-Geysire: einfacher unzerstörbarer Block ohne Fontäne (keine Werte in items.json), 1 je Chunk in den Geysirfeldern wie beim Enceladus. Frostkriecher: versteckt = unsichtbar und reglos; bricht auch bei Treffer hervor; danach Nahkampf mit Zielwahl wie ein Zombie. Heizmodul: `temp_min_bonus` wird zur unteren Grenze des Anzugs addiert (wie das Kühlmodul oben). Uranus Türkis #9FD8E0, Neptun Blau #3F5FD0 am Himmel (Platzhalter)
- **#108 (M7e, 13.1, 13.4, 18, 3A, Ä4):** Pluto, Sender, Wächter, Sternenkarte: offene Werte → Biomquelle `center_radius`: Kreis um `center`, außerhalb multi_noise über die übrigen Biome. Sender: Halle an fester Lage im Chunk (0, 0) auf Sohlenhöhe `position.y`; liegt die Oberfläche über der Halle (Ecken und Mitte laut Generator) zu tief, sinkt die Halle, bis 3 Blöcke Gelände über dem Dach liegen, höchstens um `tolerance`; Hülle 1 Block dick; Tunnel steigt je Block nach Süden um 1, bis die Sohle die Oberfläche laut Generator erreicht; Ring um das obere Tunnelende auf Oberflächenhöhe, ohne den Gang zu versperren; der Senderkern leuchtet (Licht 10). Wächter: erscheint 4 Blöcke nördlich des Kerns; Bossleiste blau; Schockwelle stößt mit Stärke 1,5 waagerecht weg; Strahl auf das Ziel bis 16 Blöcke mit Sichtkontakt, Schaden nach `duration_ticks`; Phase 2 ruft die Frostkriecher sofort sichtbar. Ein ohne Sieg entfernter Wächter (z. B. `/kill` ohne Tod) gibt den Kern ohne Wartezeit frei. `chart_uuid`, lebender Wächter und letzter Sieg stehen in einer eigenen SavedData `infinite_horizon:signal` je Agentur statt in `AgencyData` (deren Codec ist voll); Wartezeit in Spielzeit der Overworld. Sternenkarte: Item-Entity mit unbegrenzter Lebenszeit; „zerstört“ = Item-Entity entfernt (getötet oder verworfen), solange sie noch die Karte trägt (nicht beim Aufheben); Beute mit Komponenten oder Bedingung setzt der Code, nicht die Loot-Tabelle. Admin-Befehl `/infinite_horizon agency reset_chart <agentur>`
