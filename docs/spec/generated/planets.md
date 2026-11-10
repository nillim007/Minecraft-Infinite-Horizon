<!-- Erzeugt von tools/render_docs.py aus spec/data. Nicht von Hand ändern. -->

# Ziele und Planeten

| Ziel | Name | Typ | Ring | DP | g | Atmosphäre | Druck | Temp. Nacht/Tag | Strahlung | Solar | Rakete | Fernflug |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `minecraft:overworld` | Erde | surface | 0 | 0 | 1 | breathable | normal | 10 / 20 | 0 | 1 | 1 | nein |
| `earth_orbit` | Erdorbit | orbit | 0 | 0 | 0,005 | none | vacuum | -100 / 100 | 1 | 1 | 2 | nein |
| `moon` | Mond | surface | 1 | 0 | 0,17 | none | vacuum | -170 / 120 | 1 | 1 | 1 | nein |
| `venus` | Venus | surface | 1 | 1 | 0,9 | toxic | high | 465 / 465 | 1 | 0,1 | 1 | nein |
| `venus_orbit` | Venus-Orbit | orbit | 1 | 1 | 0,005 | none | vacuum | -100 / 100 | 1 | 2 | 2 | nein |
| `mars` | Mars | surface | 1 | 1,5 | 0,38 | thin | low | -120 / 20 | 1 | 0,5 | 1 | nein |
| `mars_orbit` | Mars-Orbit | orbit | 1 | 1,5 | 0,005 | none | vacuum | -100 / 100 | 1 | 0,5 | 2 | nein |
| `mercury` | Merkur | surface | 2 | 2 | 0,38 | none | vacuum | 20 / 20 | 2 | 3 | 2 | nein |
| `mercury_orbit` | Merkur-Orbit | orbit | 2 | 2 | 0,005 | none | vacuum | -100 / 100 | 1 | 3 | 2 | nein |
| `asteroid_belt` | Asteroidengürtel | surface | 2 | 3 | 0,03 | none | vacuum | -100 / -100 | 1 | 0,25 | 2 | nein |
| `ceres` | Ceres | surface | 2 | 3 | 0,03 | none | vacuum | -110 / -90 | 1 | 0,25 | 2 | nein |
| `europa` | Europa | surface | 2 | 5 | 0,13 | none | vacuum | -160 / -160 | 2 | 0,1 | 2 | nein |
| `io` | Io | surface | 2 | 5 | 0,18 | none | vacuum | -140 / -140 | 3 | 0,1 | 2 | nein |
| `jupiter_orbit` | Jupiter-Orbit | orbit | 2 | 5 | 0,005 | none | vacuum | -100 / 100 | 3 | 0,1 | 2 | nein |
| `enceladus` | Enceladus | surface | 2 | 8 | 0,01 | none | vacuum | -200 / -200 | 1 | 0,05 | 2 | ja |
| `saturn_orbit` | Saturn-Orbit | orbit | 2 | 8 | 0,005 | none | vacuum | -100 / 100 | 1 | 0,05 | 2 | ja |
| `titan` | Titan | surface | 2 | 8 | 0,14 | dense | normal | -180 / -180 | 1 | 0,05 | 2 | ja |
| `uranus_orbit` | Uranus-Orbit | orbit | 2 | 12 | 0,005 | none | vacuum | -100 / 100 | 1 | 0,02 | 2 | ja |
| `neptune_orbit` | Neptun-Orbit | orbit | 2 | 15 | 0,005 | none | vacuum | -100 / 100 | 1 | 0,02 | 2 | ja |
| `triton` | Triton | surface | 2 | 15 | 0,08 | thin | low | -235 / -235 | 1 | 0,02 | 2 | ja |
| `pluto` | Pluto | surface | 2 | 18 | 0,06 | thin | low | -230 / -230 | 1 | 0,02 | 2 | ja |

## Erde (`minecraft:overworld`)

| Erz | y | Ader | je Chunk | Drop |
| --- | --- | --- | --- | --- |
| `bauxite_ore` | 50–120 | 12 | 6 | `bauxite` |
| `titanium_ore` | -64–0 | 6 | 4 | `raw_titanium` |
| `quartz_sand` | ––– | – | 2 |  |

## Mond (`moon`)

**Zonen:** id polar_crater, y_min -64, y_max 400, biome sternenwerft:moon_polar_crater, temperature -190

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `moon_highlands` | Mond-Hochland | `moon_regolith` | mountainous | common |
| `moon_mare` | Mare | `moon_basalt` | flat | common |
| `moon_great_crater` | Mond-Großkrater | `moon_regolith` | hilly | common |
| `moon_polar_crater` | Mond-Polarkrater | `moon_regolith` | rare | rare |

| Erz | y | Ader | je Chunk | Drop |
| --- | --- | --- | --- | --- |
| `moon_titanium_ore` | 0–100 | 6 | 3 | `raw_titanium` |
| `moon_aluminum_ore` | 0–80 | 8 | 5 | `bauxite` |
| `moon_titanium_ore` | 0–80 | 6 | 4 | `raw_titanium` |
| `moon_ice` | ––– | 16 | – | `minecraft:ice` |

| Struktur | Erkundungsziel | Hinweis |
| --- | --- | --- |
| `great_crater` | nein | Radius 40–60, Zentralberg, Erze ×2 innerhalb der Struktur; SPEC-GAP: Abstand nicht angegeben |
| `lava_tube` | ja | Tunnel 5×5, Länge 60–120, y 30–50, 1 je 16 Chunks |

## Venus (`venus`)

**Zonen:** id cloud_layer, y_min 180, y_max 240, temperature 20, pressure normal, solar_factor 2.0, launch_cost_override 16000

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `venus_lava_plains` | Lavaebenen | `minecraft:basalt` | flat | common |
| `venus_volcanoes` | Venus-Vulkane | `venus_rock` | mountainous | common |
| `venus_plateau` | Venus-Hochebene | `venus_rock` | mountainous | common |

| Erz | y | Ader | je Chunk | Drop |
| --- | --- | --- | --- | --- |
| `venus_sulfur_ore` | 40–100 | 8 | 4 | `sulfur` |
| `venus_tungsten_ore` | 40–140 | 6 | 5 | `raw_tungsten` |

| Struktur | Erkundungsziel | Hinweis |
| --- | --- | --- |
| `volcanic_vent` | ja | an Vulkanen; SPEC-GAP: Häufigkeit nicht angegeben |

## Mars (`mars`)

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `mars_red_plains` | Rote Ebenen | `mars_sand` | flat | common |
| `mars_canyon` | Mars-Canyon | `mars_rock` | hilly | common |
| `mars_volcanic_highlands` | Vulkanisches Hochland | `mars_rock` | mountainous | common |
| `mars_dry_lake` | Ausgetrockneter See | `gypsum_block` | flat | common |
| `mars_polar_cap` | Mars-Polkappe | `dry_ice_block` | rare | rare |
| `mars_meteorite_field` | Mars-Meteoritenfeld | `mars_sand` | rare | rare |

| Erz | y | Ader | je Chunk | Drop |
| --- | --- | --- | --- | --- |
| `mars_magnesium_ore` | 0–80 | 8 | 5 | `raw_magnesium` |
| `mars_iron_ore` | 0–80 | 8 | 4 | `minecraft:raw_iron` |
| `mars_magnesium_ore` | 0–80 | 8 | 10 | `raw_magnesium` |
| `mars_iron_ore` | 0–80 | 8 | 8 | `minecraft:raw_iron` |

| Struktur | Erkundungsziel | Hinweis |
| --- | --- | --- |
| `giant_volcano` | nein | Radius 80, Höhe bis y 250 |
| `lava_tube` | ja | Hochland; Maße wie Mond (5×5, Länge 60–120, y 30–50); SPEC-GAP: Häufigkeit auf dem Mars nicht angegeben |
| `meteorite_field` | ja | 1 je 8 Chunks |

## Merkur (`mercury`)

**Zonen:** id scorched_plains, y_min -64, y_max 400, biome sternenwerft:mercury_scorched_plains, temperature 430; id frost_plains, y_min -64, y_max 400, biome sternenwerft:mercury_frost_plains, temperature -180; id twilight_belt, y_min -64, y_max 400, biome sternenwerft:mercury_twilight_belt, temperature 20; id shadow_crater, y_min -64, y_max 400, biome sternenwerft:mercury_shadow_crater, temperature -150

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `mercury_scorched_plains` | Glutebenen | `mercury_rock` | flat | common |
| `mercury_frost_plains` | Frostebenen | `mercury_rock` | flat | common |
| `mercury_twilight_belt` | Dämmerungsgürtel | `mercury_rock` | hilly | common |
| `mercury_shadow_crater` | Schattenkrater | `mercury_rock` | rare | rare |

| Erz | y | Ader | je Chunk | Drop |
| --- | --- | --- | --- | --- |
| `mercury_titanium_ore` | ––– | – | – | `raw_titanium` |
| `mercury_iron_ore` | ––– | – | – | `minecraft:raw_iron` |
| `mercury_lithium_salt` | ––– | – | 3 | `lithium_salt` |
| `minecraft:ice` | ––– | – | – |  |

## Asteroidengürtel (`asteroid_belt`)

| Erz | y | Ader | je Chunk | Drop |
| --- | --- | --- | --- | --- |
| `asteroid_platinum_ore` | 20–220 | – | – | `raw_platinum` |
| `asteroid_iridium_ore` | 20–220 | – | – | `raw_iridium` |
| `asteroid_nickel_ore` | 20–220 | – | – | `raw_nickel` |

| Struktur | Erkundungsziel | Hinweis |
| --- | --- | --- |
| `large_asteroid` | nein | Radius 13–20; dazu der Spawn-Asteroid (Radius 15 bei 0/80/0 mit allen drei Erzen); SPEC-GAP: Häufigkeit nicht angegeben |

## Ceres (`ceres`)

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `ceres_plains` | Ceres-Ebenen | `ceres_rock` | flat | common |
| `ceres_salt_flats` | Salzebenen | `salt_crust` | flat | common |

| Struktur | Erkundungsziel | Hinweis |
| --- | --- | --- |
| `ice_volcano` | ja | Kegel aus Eis, 1 je Dimension nahe Spawn |

## Europa (`europa`)

**Zonen:** id ocean, y_min -64, y_max 111, temperature 2, pressure normal, radiation 0, water_behavior normal; id deep_sea, y_min -64, y_max 79, pressure deep

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `europa_ice_plains` | Eisebenen | `europa_ice` | flat | common |
| `europa_rifts` | Eisspalten | `europa_ice` | hilly | common |
| `europa_ocean` | Europa-Ozean | `europa_seabed` | hilly | common |
| `europa_vents` | Hydrothermale Quellen | `europa_seabed` | hilly | common |

| Struktur | Erkundungsziel | Hinweis |
| --- | --- | --- |
| `hydrothermal_field` | ja | Quellen-Cluster aus hydrothermal_vent, 1 reef_keeper je Cluster; SPEC-GAP: Häufigkeit nicht angegeben |

## Io (`io`)

**Zonen:** id near_lava, y_min -64, y_max 400, near_block minecraft:lava, temperature 300

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `io_sulfur_plains` | Schwefelebenen | `io_crust` | flat | common |
| `io_lava_lakes` | Lavaseen | `io_crust` | flat | common |
| `io_volcanic_fields` | Vulkanfelder | `io_crust` | mountainous | common |

| Erz | y | Ader | je Chunk | Drop |
| --- | --- | --- | --- | --- |
| `io_sulfur_ore` | ––– | – | – | `sulfur` |

| Struktur | Erkundungsziel | Hinweis |
| --- | --- | --- |
| `volcanic_vent` | ja | Biom-Zuordnung angenommen (Vulkanfelder); SPEC-GAP: Häufigkeit nicht angegeben |

## Enceladus (`enceladus`)

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `enceladus_snowfields` | Schneefelder | `enceladus_snow` | flat | common |
| `enceladus_tiger_stripes` | Tigerstreifen | `enceladus_ice` | hilly | common |

## Titan (`titan`)

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `titan_methane_lakes` | Methanseen | `titan_ice_rock` | flat | common |
| `titan_dunes` | Titan-Dünen | `titan_dune_sand` | flat | common |
| `titan_ice_mountains` | Titan-Eisgebirge | `titan_ice_rock` | mountainous | common |

## Triton (`triton`)

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `triton_cantaloupe_terrain` | Cantaloupe-Terrain | `triton_ice` | hilly | common |
| `triton_polar_cap` | Triton-Polkappe | `triton_ice` | rare | rare |
| `triton_geyser_fields` | Geysirfelder | `triton_ice` | rare | rare |

## Pluto (`pluto`)

| Biom | Name | Oberfläche | Form | Häufigkeit |
| --- | --- | --- | --- | --- |
| `pluto_heart_glacier` | Herzgletscher | `pluto_nitrogen_ice` | flat | common |
| `pluto_ice_mountains` | Pluto-Eisgebirge | `pluto_water_ice` | mountainous | common |
| `pluto_dark_highlands` | Dunkles Hochland | `pluto_tholin` | mountainous | common |

| Erz | y | Ader | je Chunk | Drop |
| --- | --- | --- | --- | --- |
| `pluto_uranium_ore` | 0–60 | 4 | 3 | `raw_uranium` |

| Struktur | Erkundungsziel | Hinweis |
| --- | --- | --- |
| `signal_transmitter` | ja | Halle 32×32×16 aus ancient_alloy, transmitter_core in der Mitte, Öffnung 3×4 in der Südwand, Treppentunnel (3 breit, 4 hoch) aus pluto_water_ice zur Oberfläche, oben 5×5-Ring aus ancient_alloy |

