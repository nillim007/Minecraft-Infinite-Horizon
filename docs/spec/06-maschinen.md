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

- **Umfang:** Die Spalte „Stufe“ in 6.1 und 6.3 ist nur eine Orientierung. Welche Objekte zu M2 gehören, bestimmt `phase` in `items.json` (27.1); ab M3 stehen die Objekte als Liste im Umfang des Meilensteins (27.4, 27.6). Objekte mit `phase` ≥ 2 kommen mit ihrem Meilenstein, auch wenn hier „Stufe 1“ steht (z. B. `pressure_bottle` mit M3, `solar_panel` und `analyzer` mit M4).
- **`crusher`:** Er verarbeitet `raw_*`-Items und `bauxite`. Erzblöcke zu zerkleinern ist kein Ziel von M2; „Erz/Raw“ in der Tabelle ist als „Rohform“ zu lesen.
- **`electric_furnace`:** „Alle Vanilla-Schmelzrezepte“ heißt nur `minecraft:smelting`, nicht Hochofen (`blasting`) oder Räucherofen (`smoking`). Die Dauer ist immer die der Maschine (100 t); die `cookingtime` des Rezepts wird ignoriert. Es gibt keine Erfahrungspunkte.
- **`analyzer` (ab M4):** Die FP gehen am Ende des Vorgangs an die Agentur `OwnershipResolver.current(owner)` (11.1). Ohne Besitzer steht er auf `error` (Tooltip `gui.infinite_horizon.status.no_owner`) und macht keinen Fortschritt; wer das GUI zuerst öffnet, wird Besitzer (23.1). Der Eingangsslot nimmt nur Proben an, die FP geben (Ring aus `PlanetRegistry` zur `dimension` der Probe, 11.1). Er hat keinen Ausgabeslot; die Probe wird verbraucht.
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

- **`battery`:** Der Ladeslot nimmt jedes Item an, das eine Energie-Schnittstelle der Plattform (NeoForge, Team Reborn) oder die Komponente `infinite_horizon:energy` trägt, und lädt es mit höchstens `io_per_tick` (512 J/t). In M2 gibt es keine eigenen ladbaren Items; der Test nutzt ein Plattform-Testitem.
- **`fluid_tank`:** nur Flüssigkeiten. Gase (`hydrogen`, `oxygen`) bleiben in M2 im Maschinentank (5.3).
- **`pressure_bottle` (ab M3):** nur Gase; Ausgangsseite und Füll-/Leerslot wie beim `fluid_tank`; behält beim Abbauen ihren Inhalt (`infinite_horizon:fluid`).

**Tageslicht** = 1 bei Tag, 0 bei Nacht, linear in der Dämmerung. Regen und Staubsturm: × 0,25. Sonnensturm: × 2.

**Tageslicht-Kurve** (ab M3 für die Temperatur, 8.1, dort ohne Wetterfaktor; ab M4 mit dem `solar_panel` auch für Solarstrom, dort mit Wetterfaktor): Tag 0–12.000 t = 1, 12.000–13.800 t linear auf 0, 13.800–22.200 t = 0, 22.200–24.000 t linear auf 1 (`daylight_curve` in `environment.json`). „Himmel sichtbar“ heißt `canSeeSky` (Wolken zählen nicht). Gewitter zählt wie Regen.

**Reaktor-Sicherheit**

- Fusion: Fehlt die Kühlung 40 t lang, schaltet der Reaktor ab (Status `error`). Keine Explosion. Er startet automatisch neu, sobald die Kühlung 100 t lang ohne Unterbrechung läuft.
- Kernreaktor: interne Hitze 0–1.000. Mit Kühlung −5/t, ohne Kühlung +2/t. Ab 800 Warnung (Ton, Statuslampe blinkt). Bei 1.000 Notabschaltung und **Strahlungszone** (Radius 16, Strahlung 3) für 72.000 t. Keine Explosion. Nach einer Notabschaltung startet er automatisch wieder, sobald die Hitze unter 200 liegt.

## 6.4 Schlüssel für Oberflächentexte (GUI)

Alle Texte stehen in `common/lang_extra.json`. Namen von Blöcken und Items kommen aus `items.json` (`name_de`, `name_en`). Schlüssel für Meilenstein 2:

| Schlüssel | Verwendung |
| --- | --- |
| `gui.infinite_horizon.energy` | Tooltip der Energieleiste (Wert und Kapazität in J) |
| `gui.infinite_horizon.fluid` | Tooltip der Fluidleiste, z. B. „Wasser: 4.000 / 16.000 mB“ |
| `gui.infinite_horizon.tank` | Knopf „Tank wählen“ |
| `gui.infinite_horizon.side` | Knopf „Ausgangsseite“ |
| `gui.infinite_horizon.side.down`, `.up`, `.north`, `.south`, `.west`, `.east` | die sechs Seiten (Unten, Oben, Nord, Süd, West, Ost) |
| `gui.infinite_horizon.upgrade.speed`, `gui.infinite_horizon.upgrade.efficiency` | Hinweis an den Upgrade-Slots |
| `gui.infinite_horizon.port.input`, `gui.infinite_horizon.port.output` | Port-Modus |
| `gui.infinite_horizon.multiblock.missing` | fehlender Block im Multiblock (mit Koordinate) |
| `gui.infinite_horizon.status.running`, `.idle`, `.error` | Tooltip der Statuslampe |
| `jei.infinite_horizon.<rezepttyp>` | Name der Rezeptkategorie je Rezepttyp (3.9) |

Schlüssel für Meilenstein 3:

| Schlüssel | Verwendung |
| --- | --- |
| `hud.infinite_horizon.oxygen`, `.pressure`, `.heat`, `.cold`, `.radiation` | Belastungsleisten (8.4) |
| `hud.infinite_horizon.suit_oxygen`, `.temperature` | Anzugvorrat und Temperatur im HUD (8.4) |
| `gui.infinite_horizon.oxygen` | Tooltip der Brustplatte, z. B. „O₂: 600 / 1.200 mB“ (9.1) |
| `gui.infinite_horizon.dome.radius` | Radius im Kuppel-GUI (10.1) |
| `message.infinite_horizon.no_atmosphere` | Fackel, Feuer usw. lassen sich nicht setzen (8.5) |
| `message.infinite_horizon.dome.failing` | Kuppel fällt aus, mit Restsekunden (10.1) |
| `message.infinite_horizon.bed.no_air` | „Hier kannst du nicht atmen“ (14.4) |
| `death.attack.infinite_horizon.<gefahr>` | Todesnachricht je Schadensart (8.2) |
| `agency.infinite_horizon.default_name` | „Agentur von <Name>“ (23.1) |
| `commands.infinite_horizon.goto.*`, `commands.infinite_horizon.agency.*` | Rückmeldungen der Befehle (12.10, 23.1) |

Schlüssel für Meilenstein 4:

| Schlüssel | Verwendung |
| --- | --- |
| `gui.infinite_horizon.status.locked` | Rezept durch Forschung gesperrt (11.3) |
| `gui.infinite_horizon.status.no_owner` | Analysator ohne Besitzer (6.1) |
| `gui.infinite_horizon.research.*` | Forschungsbaum: Reiter, FP, Kosten, Tor-Texte, Zustände (11.3) |
| `gui.infinite_horizon.agency.*` | Reiter „Agentur“: Mitglieder, Rollen, Einladen, Annehmen, Austreten (23.1) |
| `gui.infinite_horizon.launch.check.<punkt>` | Zeilen der Startcheckliste (12.4) |
| `gui.infinite_horizon.star_map.*`, `gui.infinite_horizon.rocket.*` | Navigationskarte und Raketen-GUI (12.5) |
| `gui.infinite_horizon.atlas.*` | Atlas (24.2) |
| `message.infinite_horizon.sample.none`, `.used` | „Keine verwertbare Probe“, Position schon beprobt (11.2) |
| `message.infinite_horizon.exploration.found` | Erkundungsziel gefunden (11.1) |
| `message.infinite_horizon.probe.arrived` | Sonde angekommen (12.3) |
| `message.infinite_horizon.agency.invited` | Chat-Hinweis an den Eingeladenen (23.1) |
| `message.infinite_horizon.discovered` | Ankündigung des Entdeckers an alle Spieler (23.3) |
| `advancements.infinite_horizon.<id>.title`, `.description` | Erfolge (24.3) |
