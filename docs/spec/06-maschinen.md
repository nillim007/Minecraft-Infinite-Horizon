# 6. Maschinen

## 6.1 Steckbriefe

| ID | Name DE | Stufe | Verbrauch | Dauer | Ein → Aus | Ort |
| --- | --- | --- | --- | --- | --- | --- |
| `crusher` | Zerkleinerer | – | 20 J/t | 200 t | 1 Erz/Raw → 2 Staub; `metal_concentrate` → 1 `aluminum_dust` + 1 `titanium_dust` | überall |
| `electric_furnace` | Elektroofen | – | 20 J/t | 100 t | alle Vanilla-Schmelzrezepte + Rezepttyp `electric_smelting`; 2 Eingabeslots für Legierungen | überall |
| `press` | Presse | – | 30 J/t | 100 t | Barren + Pressform → Produkt (6.2) | überall |
| `refinery_controller` | Raffinerie (Multiblock `refinery`) | – | 40 J/t | 200 t | 1.000 mB `crude_oil` → 600 mB `kerosene` + 1 `plastic` | überall |
| `electrolyzer` | Elektrolyseur | – | 40 J/t | 40 t | 200 mB Wasser → 200 mB `hydrogen` + 100 mB `oxygen` | überall mit Wasser |
| `fluid_pump` | Pumpe | – | 20 J/t | 40 t | saugt einen Fluid-Quellblock im Umkreis von 8 Blöcken unterhalb ab → 1.000 mB; der Quellblock wird entfernt, bei `crude_oil` nur mit 10 % Wahrscheinlichkeit (`source_consume_chance` in `machines.json`); wählt die nächste Quelle mit dem Fluid, das schon im Tank ist (bei leerem Tank die nächste überhaupt) | überall |
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

**Zusatzregeln (Meilenstein 2)**

- **Umfang:** Die Spalte „Stufe“ in 6.1 und 6.3 ist nur eine Orientierung. Welche Objekte zu einem Meilenstein gehören, bestimmt `phase` in `items.json` (27.1). Objekte mit `phase` ≥ 2 kommen mit ihrem Meilenstein, auch wenn hier „Stufe 1“ steht (z. B. `solar_panel`, `pressure_bottle`).
- **`crusher`:** Er verarbeitet `raw_*`-Items und `bauxite`. Erzblöcke zu zerkleinern ist kein Ziel von M2; „Erz/Raw“ in der Tabelle ist als „Rohform“ zu lesen.
- **`electric_furnace`:** „Alle Vanilla-Schmelzrezepte“ heißt nur `minecraft:smelting`, nicht Hochofen (`blasting`) oder Räucherofen (`smoking`). Die Dauer ist immer die der Maschine (100 t); die `cookingtime` des Rezepts wird ignoriert. Es gibt keine Erfahrungspunkte.
- **`fluid_pump`:** Suchbereich: Radius 8 horizontal um die Pumpe, 1 bis 8 Blöcke darunter. Es zählen nur Quellblöcke (`isSource`) von Wasser, Lava und `crude_oil`; Fließblöcke zählen nicht. Die nächste Quelle (euklidischer Abstand) kommt zuerst, bei Gleichstand die mit der kleineren Koordinate (x, dann y, dann z). Das Auffüllen der Quelle bleibt wie in Vanilla (Wasser ist unendlich; Erdöl verbraucht seinen Quellblock nur mit der Wahrscheinlichkeit `source_consume_chance`, also wird eine Ölblase etwa zehnmal so ergiebig). Ohne Quelle steht die Pumpe auf `idle`.

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

Brennstoffe mit Behälter (z. B. Lavaeimer) hinterlassen den leeren Behälter im Brennstoffslot, wie im Vanilla-Ofen; ein Trichter kann ihn abziehen.

- **`battery`:** Der Ladeslot nimmt jedes Item an, das eine Energie-Schnittstelle der Plattform (NeoForge, Team Reborn) oder die Komponente `sternenwerft:energy` trägt, und lädt es mit höchstens `io_per_tick` (512 J/t). In M2 gibt es keine eigenen ladbaren Items; der Test nutzt ein Plattform-Testitem.
- **`fluid_tank`:** nur Flüssigkeiten. Gase (`hydrogen`, `oxygen`) bleiben in M2 im Maschinentank (5.3).

**Tageslicht** = 1 bei Tag, 0 bei Nacht, linear in der Dämmerung. Regen und Staubsturm: × 0,25. Sonnensturm: × 2.

**Vorgabe für später (ab dem Meilenstein des `solar_panel`):** Tag 0–12.000 t = 1, 12.000–13.800 t linear auf 0, 13.800–22.200 t = 0, 22.200–24.000 t linear auf 1. „Himmel sichtbar“ heißt `canSeeSky` (Wolken zählen nicht). Gewitter zählt wie Regen.

**Reaktor-Sicherheit**

- Fusion: Fehlt die Kühlung 40 t lang, schaltet der Reaktor ab (Status `error`). Keine Explosion. Er startet automatisch neu, sobald die Kühlung 100 t lang ohne Unterbrechung läuft.
- Kernreaktor: interne Hitze 0–1.000. Mit Kühlung −5/t, ohne Kühlung +2/t. Ab 800 Warnung (Ton, Statuslampe blinkt). Bei 1.000 Notabschaltung und **Strahlungszone** (Radius 16, Strahlung 3) für 72.000 t. Keine Explosion. Nach einer Notabschaltung startet er automatisch wieder, sobald die Hitze unter 200 liegt.

## 6.4 Schlüssel für Oberflächentexte (GUI)

Alle Texte stehen in `common/lang_extra.json`. Namen von Blöcken und Items kommen aus `items.json` (`name_de`, `name_en`). Schlüssel für Meilenstein 2:

| Schlüssel | Verwendung |
| --- | --- |
| `gui.sternenwerft.energy` | Tooltip der Energieleiste (Wert und Kapazität in J) |
| `gui.sternenwerft.fluid` | Tooltip der Fluidleiste, z. B. „Wasser: 4.000 / 16.000 mB“ |
| `gui.sternenwerft.tank` | Knopf „Tank wählen“ |
| `gui.sternenwerft.side` | Knopf „Ausgangsseite“ |
| `gui.sternenwerft.side.down`, `.up`, `.north`, `.south`, `.west`, `.east` | die sechs Seiten (Unten, Oben, Nord, Süd, West, Ost) |
| `gui.sternenwerft.upgrade.speed`, `gui.sternenwerft.upgrade.efficiency` | Hinweis an den Upgrade-Slots |
| `gui.sternenwerft.port.input`, `gui.sternenwerft.port.output` | Port-Modus |
| `gui.sternenwerft.multiblock.missing` | fehlender Block im Multiblock (mit Koordinate) |
| `gui.sternenwerft.status.running`, `.idle`, `.error` | Tooltip der Statuslampe |
| `jei.sternenwerft.<rezepttyp>` | Name der Rezeptkategorie je Rezepttyp (3.9) |
