# 27. Meilensteine und Abnahmekriterien

Jeder Meilenstein MUSS kompilieren, auf beiden Loadern starten (Client + dedizierter Server) und seine GameTests bestehen.

**Handbuch und Wiki (jeder Meilenstein, ab M2f mit Buch-GUI):** Jedes neue spielbare Objekt hat eine Seite in `docs/handbook/pages/` (Ablauf, Rezepte über `@recipes_for`/`@recipes_type`, Steckbrief über `@machine`, mindestens ein Tipp) oder steht in `items` einer bestehenden Seite. Jede neue Phase bekommt eine Seite mit dem Ablauf („Erste Schritte“). `python tools/gen_wiki.py --check` ist grün; es schlägt fehl, wenn ein registriertes Objekt in keiner Seite unter `items` steht oder Wiki bzw. README-Statustabelle veraltet sind. Die Statustabelle entsteht aus `milestone` und `step` in `common/implemented_ids.json` (`step` = zuletzt fertiger Teilschritt). Details in `CLAUDE.md`.

GameTests laufen in der Overworld. Umweltabhängige Tests (Vakuum, Hitze, Kuppel, Venus) prüfen deshalb `EnvironmentCalculator` (8.1) direkt oder nutzen einen Override „Planet + Zone für diesen Testbereich“, der nur in GameTests aktiv ist. Der Override setzt auch die Ortsbindung (z. B. „gilt als Orbit“ oder „nicht Erde“) für Maschinen mit Ortsbindung. Wo möglich, wird Logik als reine Funktion getestet. Zeitangaben in Tests dürfen bis 10 % Toleranz haben.

| # | Inhalt | Abnahmekriterien (GameTests oder manuelle Prüfung) |
| --- | --- | --- |
| 1 | Grundgerüst: Multiloader, Registrierung, Energie-/Fluid-Abstraktion mit Transaktionen, Netzwerk, Config, Datagen, Loader für Balance-Dateien (25.7), `machine_frame`, `crusher`, `combustion_generator`, `copper_cable`, Platzhalter-Skript (ohne GUI; Bedienung per Trichter) | GT: Generator mit Kohle → Kabel → Zerkleinerer macht aus 1 `minecraft:raw_iron` 2 `iron_dust` in höchstens 220 t. GT: Wert in `machines.json` ändern + `/reload` ändert die Vorgangsdauer; der Test setzt die geänderten Werte auf demselben Weg wie der Reload-Listener, manuell per Datapack prüfbar. GT: Ein Test-Verbraucher, der nur die Plattform-API nutzt (NeoForge: Energy-Capability, Fabric: Team Reborn Energy), erhält Strom aus dem Kabel; im GameTest-Betrieb bietet dafür der Vanilla-Block `minecraft:target` eine Loader-Energie-Schnittstelle an. |
| 2 | Erdindustrie: alle Einträge mit `phase = 1` (Materialien, Erze, Worldgen-Ergänzungen, Maschinen, Generatoren, Speicher, Rohre, Raffinerie-Multiblock, Upgrades, Eimer; Umfang in 27.1), JEI/REI/EMI; GUI für alle Maschinen und Generatoren (auch aus M1), Upgrade-Items aktivieren die Upgrade-Slots; Füll- und Leerslots für Eimer | GT: jede Maschine verarbeitet ihr Beispielrezept innerhalb der Zeitgrenze aus 27.2. GT: Raffinerie erkennt die Struktur und meldet `error` bei fehlendem Block. GT: Leistungstest mit 100 Zerkleinerern (27.2). Manuell: Bauxit spawnt in der Savanne. Teilschritte: 27.3. |
| 3 | Erste Welt: Planeten-Registry, `moon` mit Biomen und Himmel, Schwerkraft, Umweltsystem, HUD, Anzug T1, Sauerstoffgenerator, Kuppel T1, Füll- und Leerslots für Kanister (die für Eimer kamen mit M2), **Agentur-Kern** (SavedData, automatische Agentur je Spieler, `owner_agency` an allen Block-Entities) | GT: Spieler ohne Anzug im Mond-Vakuum erreicht 100 % Belastung nach genau 200 t. GT: Spieler in aktiver Kuppel hat `breathable`. GT: Kuppel ohne O₂ fällt nach 600 t aus. |
| 4 | Erster Flug: Forschung, Proben, Analysator, Sonde, Startrampe, Rakete T1, Flugablauf, Landefeuer, Atlas. **Erste spielbare Version** | Manuell: neue Welt → Mond und zurück ohne Befehle. GT: Treibstoffkosten Erde → Mond = 35.360 mB. GT: Rakete mit 3 Flossen startet nicht. |
| 5 | Ring 1: Mars, Venus (Wolkenschicht, Aerostat, Aufzug), Rakete T2, Anzug T2, Rover, Kreaturen Ring 1, Ereignisse, Nahrung | GT: Venus-Hitzezeit 3.600 t. Manuell: Helium, Magnesium und Wolfram gewinnen öffnet Stufe 3. |
| 6 | Orbit: Orbit-Dimensionen, Station, Module, Schwerelosigkeit, Fusion, Landefähre, Fernflug-Modul | GT: Fusion schaltet 40 t nach Kühlungsausfall ab. GT: `zero_g_fabricator` außerhalb des Orbits = `error`. |
| 7 | Ring 2: alle Ziele Merkur bis Pluto, Kreaturen, Sender, Boss, Strahlung | Manuell: jedes Ziel ist mit der vorgesehenen Ausrüstung betretbar und ohne sie tödlich (mit Warnung). |
| 8 | Netzwerk und Finale: Außenposten, Bohrturm, Pumpstation, Frachtraketen, Logistik-Computer, Kernreaktor, Antriebe, Werft, T3, Finale | GT (reine Funktion): `OfflineProduction.compute(running = true, Rate 1 Item/100 t, Faktor 0,5, 24.000 t)` = 120 Items. GT mit Override „nicht Erde“: Bohrturm über 10 Eisenerz-Blöcken läuft (`running`). GT: Route „bis Bedarf gedeckt“ stoppt bei 0 Bedarf. |
| 9 | Feinschliff: Agentur-GUI, Rollen, Partner, Handel, Handbuch-Feinschliff (Blaupausen-Optik, englische Texte), Advancements, Klang, Balancing-Skript, Übersetzungen | `tools/balance_sheet.py` liest alle Rezept-JSONs und gibt die Rohstoffsumme je Rakete und Werftabschnitt aus. Regeln: Rohstoff = Item oder Fluid ohne eigenes Rezept. Bei mehreren Rezepten zählt das erste. Nebenprodukte werden ignoriert. Fluide werden in mB summiert. Rezepte werden alphabetisch nach ID sortiert; Mengen werden durch die Ausgabemenge geteilt; Pressformen (`mold`) werden ignoriert; bei Tags zählt das erste Element des Tags. |

**Balancing-Ziele** (Einzelspieler, Standardeinstellungen, gesamt 50–80 h):

| Phase | Zielzeit | Strombedarf am Ende |
| --- | --- | --- |
| 1 | 4–6 h | ~100 J/t |
| 2 | 3–4 h | ~200 J/t |
| 3 | 8–12 h | ~500 J/t |
| 4 | 6–8 h | ~2.000 J/t |
| 5 | 15–25 h | ~5.000 J/t |
| 6 | 15–25 h | ~10.000 J/t |

Grundregeln: Jede Raketenstufe ≈ 3× Material der vorigen, T3 ≈ 10× T2. Ab Phase 3 dauert ein Schlüsselmaterial von Hand ≥ 3× so lang wie mit einfacher Automatisierung. Etwa die Hälfte der FP aus Erkundung. Jede Gefahr warnt ≥ 200 t vorher. Kein Engpass nur durch Warten.

## 27.1 Meilenstein 2: Umfang und Festlegungen

- **Umfang:** M2 umfasst genau die Einträge mit `phase = 1` in `items.json`: 51 Items und Blöcke (21 Blöcke, 30 Items, darunter `crude_oil_bucket` und `kerosene_bucket`) und 4 Fluide (`crude_oil`, `hydrogen`, `kerosene`, `oxygen`), dazu die 47 Rezepte, deren Ergebnisse nur aus solchen Einträgen bestehen. 5 der Einträge stammen schon aus M1. Alles mit `phase` ≥ 2 kommt mit dem Meilenstein, der es braucht, auch wenn Kapitel 4 und 6 „Stufe 1“ nennen (z. B. `solar_panel`, `aluminum_cable`, `pressure_bottle`, `transport_tank`, `analyzer`, `food_packer`). `common/implemented_ids.json` führt die Liste.
- **Forschung und Agentur:** In M2 ist nichts gesperrt. Die Rezeptsperre ist die Schnittstelle `ResearchRecipeLock.isUnlocked(recipe, owner)` mit dem Standardwert `true`. `owner_agency` und `SwBlockEntity.setPlacedBy` kommen mit M3, die echte Sperre mit M4. Die Config `research_required` wird gelesen, wirkt aber erst ab M4.
- **Füll- und Leerslots:** M2 enthält sie für Eimer (4.1). Kanister und `transport_tank` kommen mit dem Meilenstein ihrer Items.
- **Handbuch und Atlas:** `engineers_handbook` bekommt in M2f sein Buch-GUI mit Links, Rezeptanzeige, Suche und Taste `H` und wird beim ersten Join verschenkt (24.1, Ä26). `planetary_atlas` wird in M2 nur registriert (Item, Rezept, Platzhalter); GUI und Geschenk kommen mit M4.
- **Nicht in M2:** Erzblöcke zerkleinern, das Rezept `crushing/aluminum_dust_from_metal_concentrate` (Item `metal_concentrate`, M5), Gasspeicher (5.3), Worldgen- und Multiblock-Seiten in JEI/REI/EMI (3.9).
- **Offen:** Der exakte Abgleich im Montagetisch steht in 22.3; drei zutatengleiche Rezeptpaare und die Endlichkeit des Erdöls stehen in Kapitel 28.

## 27.2 Beispielrezepte und Leistungstest (Abnahme M2)

Jede Maschine verarbeitet ihr Beispielrezept innerhalb der Zeitgrenze. Grenze = aufgerundet(Vorgangsdauer × 1,1) je Vorgang, mit der Dauer aus `machines.json` bzw. dem Rezept; bei Ketten die Summe der Grenzen. Alle Rezepte stehen in `recipes.json`.

| Block | Beispiel (Rezept-ID) | Grenze |
| --- | --- | --- |
| `crusher` | 1 `bauxite` → 2 `aluminum_dust` (`crushing/aluminum_dust`) | 220 t (Dauer 200 t) |
| `electric_furnace` | 1 `quartz_sand` → 1 `silicon` (`electric_smelting/silicon`) | 110 t (Dauer 100 t) |
| `press` | 1 `titanium_ingot` + `press_mold_plate` → 1 `titanium_plate` (`pressing/titanium_plate`) | 110 t (Dauer 100 t) |
| `electrolyzer` | 200 mB Wasser → 200 mB `hydrogen` + 100 mB `oxygen` (`electrolysis/hydrogen`) | 44 t (Dauer 40 t) |
| `refinery_controller` | fertiger Aufbau; 1.000 mB `crude_oil` → 600 mB `kerosene` + 1 `plastic` (`refining/kerosene`) | 220 t (Dauer 200 t) |
| `assembly_table` | 2 `silicon` + 4 `copper_wire` + 1 `plastic` → 1 `circuit` (`assembly/circuit`) | 220 t (Dauer 200 t) |
| `fluid_pump` | ein `crude_oil`-Quellblock 4 Blöcke unter der Pumpe → 1.000 mB im Tank | 44 t (Dauer 40 t) |
| `fuel_generator` | 4 mB `kerosene` → 80 J/t | 16 t Laufzeit |
| `battery` | nimmt bis 512 J/t an und gibt nur über die Ausgangsseite ab | je Tick geprüft |
| `copper_pipe` | schiebt 200 mB/t | je Tick geprüft |

Kettenbeispiel: `raw_titanium` → `titanium_dust` (Zerkleinerer, `crushing/titanium_dust`) → `titanium_ingot` (Elektroofen, `smelting/titanium_ingot_from_dust`) → `titanium_plate` (Presse): 220 + 110 + 110 = 440 t.

**Leistungstest:** 100 laufende `crusher`, über `copper_cable`-Netze versorgt (Netze so geteilt, dass der Durchsatz reicht). Ein GameTest misst 200 t lang die Zeit je Server-Tick (`System.nanoTime`); der Mittelwert darf höchstens 1 ms betragen. Auf GitHub-Runnern gilt die dreifache Toleranz (3 ms), lokal 1 ms.

## 27.3 Teilschritte Meilenstein 2

Jeder Teilschritt baut auf dem vorigen auf, endet mit grünem Build auf beiden Loadern und hat einen eigenen GameTest. Zeitgrenzen nach 27.2.

1. **M2a – Maschinenrahmen, GUI, Upgrades und Metallkette.** `MachineMenu` und Screens (4.1) für `crusher` und `combustion_generator` aus M1, dazu `electric_furnace`, `press`, `machine_frame`, Pressformen, die 2 Upgrade-Items, `UpgradeMath`, alle Metallformen und ihre Rezepte (Zerkleinern, Schmelzen, Pressen). GameTest: `raw_titanium` → `titanium_dust` → `titanium_ingot` → `titanium_plate` in Zerkleinerer, Elektroofen und Presse in höchstens 440 t. JUnit: 4 `speed_upgrade` ergeben im Zerkleinerer 63 t und 101 J/t.
2. **M2b – Erze und Worldgen.** `bauxite_ore`, `titanium_ore`, `deepslate_titanium_ore`, `quartz_sand`, Loot-Tables, Tags (13.4, 4.1, 5.1). GameTest: Abbau von `bauxite_ore` mit Steinspitzhacke gibt 1 `bauxite`, `titanium_ore` ohne Eisenspitzhacke nichts. JUnit: die erzeugten Features enthalten die Werte aus `planets.json`. Manuell: Bauxit in der Savanne (mindestens 6 Adern in 25 Chunks nach `/locate biome`).
3. **M2c – Fluide, Rohre, Tanks, Füll-/Leerslots, Pumpe, Ölblase.** Ölblase (`crude_oil`-Quellblock, Worldgen aus `planets.json`), `SwFluidTank`, `FluidGrid` mit Einzelfluid-Regel und Ausgangsseiten (4.1, 4.4), `copper_pipe`, `fluid_tank`, `fluid_pump`, die Eimer-Items. GameTest: Die Pumpe saugt einen `crude_oil`-Quellblock in 4 Blöcken Tiefe ab, `copper_pipe` führt in einen `fluid_tank`, der nach höchstens 44 t 1.000 mB hat. Ein Wasser- und ein `crude_oil`-Rohr mischen sich nicht. Ein leerer Eimer im Füllslot wird zu `crude_oil_bucket`.
4. **M2d – Montagetisch, Raffinerie, Elektrolyseur, Ports.** `assembly_table` mit exaktem Abgleich (22.3), `MultiblockController`, `MultiblockPattern`, Ports mit Modus, `refinery_casing`, `refinery_controller`, `electrolyzer`, die Rezepte für `circuit`, Ports und Gehäuse. GameTest: 1.000 mB `crude_oil` → 600 mB `kerosene` + 1 `plastic` in höchstens 220 t. Eine Raffinerie ohne einen Gehäuseblock meldet `error`, nach dem Einsetzen `idle` oder `running`. 4 `iron_plate` + 1 `copper_pipe` ergeben `fluid_port`, nicht `refinery_casing`.
5. **M2e – Generatoren, Speicher, Leistung.** `fuel_generator`, `battery` mit Ausgangsseite, Behälter-Brennstoffe (Lavaeimer), Energie-Plattformtest für Speicher. GameTest: Kerosin aus einem `kerosene_bucket` im Leerslot lässt den `fuel_generator` 80 J/t liefern. Die `battery` gibt nur über die Ausgangsseite ab. Der Leistungstest mit 100 `crusher` (27.2) läuft.
6. **M2f – JEI/REI/EMI, Handbuch und Abschluss.** Kategorien für `crushing`, `pressing`, `electric_smelting`, `refining`, `electrolysis` und `assembly` (3.9), `recipe_amount_multiplier` (22.3), Übersetzungen (6.4), Platzhalter und Handbuch-Items. **Ingenieurs-Handbuch** (24.1, 25.6): `HandbookLoader` liest Kapitel und Seiten aus allen Namespaces, `HandbookScreen` zeigt Inhaltsverzeichnis, Seiten mit Bildlauf, Links, Item-Links, Rezeptanzeige, Steckbriefe, Tabellen, Hinweiskästen, Suche und Zurück-Knopf; Rechtsklick mit `engineers_handbook` öffnet es, Taste `H` über einem Item öffnet dessen Seite; Geschenk beim ersten Join. Test: Ein JUnit-Test baut die Kategorienliste aus den Rezepttypen; ein JUnit-Test zerlegt das Inline-Markup (`[[…|…]]`, `<item:…>`, `**…**`) mit der reinen Funktion `HandbookMarkup`; ein GameTest prüft, dass das Join-Geschenk genau einmal kommt; `python tools/gen_resources.py` und `python tools/gen_wiki.py --check` erzeugen keine Änderungen mehr. Client-Test: Handbuch öffnen, einem Link folgen, Screenshot. Abschluss: Alle Beispielrezepte aus 27.2 laufen in einem GameTest-Durchlauf.
