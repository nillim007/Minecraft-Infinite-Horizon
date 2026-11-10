# 6. Maschinen

## 6.1 Steckbriefe

| ID | Name DE | Stufe | Verbrauch | Dauer | Ein → Aus | Ort |
| --- | --- | --- | --- | --- | --- | --- |
| `crusher` | Zerkleinerer | – | 20 J/t | 200 t | 1 Erz/Raw → 2 Staub; `metal_concentrate` → 1 `aluminum_dust` + 1 `titanium_dust` | überall |
| `electric_furnace` | Elektroofen | – | 20 J/t | 100 t | alle Vanilla-Schmelzrezepte + Rezepttyp `electric_smelting`; 2 Eingabeslots für Legierungen | überall |
| `press` | Presse | – | 30 J/t | 100 t | Barren + Pressform → Produkt (6.2) | überall |
| `refinery_controller` | Raffinerie (Multiblock `refinery`) | – | 40 J/t | 200 t | 1.000 mB `crude_oil` → 600 mB `kerosene` + 1 `plastic` | überall |
| `electrolyzer` | Elektrolyseur | – | 40 J/t | 40 t | 200 mB Wasser → 200 mB `hydrogen` + 100 mB `oxygen` | überall mit Wasser |
| `fluid_pump` | Pumpe | – | 20 J/t | 40 t | saugt einen Fluid-Quellblock im Umkreis von 8 Blöcken unterhalb ab → 1.000 mB; Quellblock wird entfernt; wählt die nächste Quelle mit dem Fluid, das schon im Tank ist (bei leerem Tank die nächste überhaupt) | überall |
| `assembly_table` | Montagetisch | – | 30 J/t | 200 t | Rezepttyp `assembly`, `suit_module_install`, `suit_module_remove`: 9 Eingabeslots + 1 Fluid-Eingang (16.000 mB) → 3 Ausgabeslots (Hauptergebnis + Nebenausgaben, z. B. ausgebaute Module) | überall |
| `analyzer` | Analysator | – | 20 J/t | 100 t | 1 `sample` → FP (Kapitel 11) | überall |
| `high_temperature_furnace` | Hochtemperaturofen | 2 | 80 J/t | 400 t | Rezepttyp `high_temp_smelting`, 2 Eingabeslots | überall |
| `regolith_heater` | Regolith-Erhitzer | 2 | 40 J/t | 100 t | 1 `moon_regolith` → 50 mB `helium` | nur `moon` |
| `cryo_liquefier` | Kryo-Verflüssiger | 2 | 120 J/t | 20 t | 40 mB Gas → 10 mB Flüssigkeit (`helium`, `nitrogen`) | überall |
| `hydroponics_basin` | Hydrokultur-Becken | 2 | 10 J/t + 1 mB Wasser/20 t | – | Pflanze im Becken wächst doppelt so schnell; braucht Lichtlevel ≥ 9, in Mod-Dimensionen zusätzlich eine aktive Kuppel (8.5) | überall |
| `food_packer` | Nahrungspacker | 1 | 10 J/t | 100 t | 3 Nahrungsmittel → 1 `space_ration` (Kapitel 17) | überall |
| `zero_g_fabricator` | Schwerelos-Fertigung | 3 | 100 J/t | 200 t | Rezepttyp `zero_g` | nur Orbit-Dimensionen |
| `space_weather_station` | Weltraumwetter-Station | 3 | 50 J/t | – | Frühwarnung (Kapitel 19) | überall außer Erde |
| `gravity_plate` | Schwerkraftplatte | 3 | 400 J/t | – | 1 g im Bereich 9×9 über der Platte, bis 8 Blöcke hoch | überall |
| `gas_collector` | Gas-Sammler | 4 | 200 J/t | 20 t | → 100 mB Gas des Planeten (13.3) | nur Orbits von Jupiter, Saturn, Uranus, Neptun |
| `magnetic_trap` | Magnetfalle | 4 | 2.000 J/t | 24.000 t | 1 leere `antimatter_cell` → 1 gefüllte | nur `jupiter_orbit` |
| `pump_station` | Pumpstation | 5 | 100 J/t | 20 t | → 100 mB aus Fluid-Quellblock oder Geysir direkt darunter; Quelle wird nicht verbraucht | überall |
| `drilling_rig` | Bohrturm | 5 | 200 J/t | 100 t | → 1 Erz-Item (20.2) | überall außer Erde |
| `clone_chamber` | Klon-Kammer | 4 | 200 J/t | – | Respawn-Punkt (14.4) | überall |

## 6.2 Pressen

| Eingabe | Pressform | Ausgabe |
| --- | --- | --- |
| 1 Barren | `press_mold_plate` | 1 Platte |
| 1 Barren | `press_mold_rod` | 2 Stangen |
| 1 Barren | `press_mold_wire` | 3 Draht |
| 1 `magnesium_alloy` | `press_mold_plate` | 1 `lightweight_plate` |

## 6.3 Generatoren und Speicher

| ID | Name DE | Stufe | Leistung | Brennstoff / Bedingung | Speicher |
| --- | --- | --- | --- | --- | --- |
| `combustion_generator` | Verbrennungsgenerator | – | 20 J/t | Vanilla-Brennwert in Ticks (Kohle 1.600 t) | 20.000 J |
| `fuel_generator` | Treibstoffgenerator | – | 80 J/t | 1 mB `kerosene` pro 4 t | 80.000 J |
| `solar_panel` | Solarpanel | 1 | 5 J/t × Solarfaktor × Tageslicht (Oberfläche); 20 J/t × Solarfaktor (Orbit) | Himmel sichtbar | 5.000 J |
| `solar_wing` | Solarflügel | 3 | wie Solarpanel im Orbit | nur Orbit | 5.000 J |
| `fusion_reactor` | Fusionsreaktor | 3 | 1.000 J/t | 10 mB `hydrogen` + 5 mB `helium` pro 20 t; Kühlung 2 mB `liquid_helium` pro 20 t, gibt 2 mB `helium` aus | 1.000.000 J |
| `fission_reactor` | Kernreaktor | 5 | 4.000 J/t | 1 `fuel_rod` pro 48.000 t (4 Slots); Kühlung 50 mB Wasser/t **oder** 5 mB `liquid_nitrogen`/t | 4.000.000 J |
| `battery` | Akku | – | – | Ein/Aus 512 J/t, 1 Ladeslot für Items | 500.000 J |
| `lithium_battery_bank` | Lithium-Akkubank | 4 | – | Ein/Aus 4.096 J/t | 4.000.000 J |
| `fluid_tank` | Tank | – | – | nur Flüssigkeiten | 16.000 mB |
| `pressure_bottle` | Druckflasche | 1 | – | nur Gase | 8.000 mB |
| `gas_tank` | Gastank | 2 | – | nur Gase | 64.000 mB |
| `tank_depot` | Tankdepot | 3 | – | alle Fluide | 256.000 mB |

**Tageslicht** = 1 bei Tag, 0 bei Nacht, linear in der Dämmerung. Regen und Staubsturm: × 0,25. Sonnensturm: × 2.

**Reaktor-Sicherheit**

- Fusion: Fehlt die Kühlung 40 t lang, schaltet der Reaktor ab (Status `error`). Keine Explosion. Er startet automatisch neu, sobald die Kühlung 100 t lang ohne Unterbrechung läuft.
- Kernreaktor: interne Hitze 0–1.000. Mit Kühlung −5/t, ohne Kühlung +2/t. Ab 800 Warnung (Ton, Statuslampe blinkt). Bei 1.000 Notabschaltung und **Strahlungszone** (Radius 16, Strahlung 3) für 72.000 t. Keine Explosion. Nach einer Notabschaltung startet er automatisch wieder, sobald die Hitze unter 200 liegt.
