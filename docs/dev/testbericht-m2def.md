# Testbericht: M2d bis M2f (Abschluss Meilenstein 2)

Stand: 11.11.2026. Client-Tests: NeoForge mit JEI auf 63a2337, Fabric mit REI auf c620fae (beide grün). Die Korrekturen aus der Sichtprüfung (JEI-Slotrahmen, REI-Fluide, Taste H) folgen in dc1f53b und dc527cc; deren CI-Läufe brachen ab, weil GitHub keinen Runner bereitstellte, und laufen mit dem nächsten Push erneut. Vorherige Runde: `docs/dev/testbericht-m2abc.md`.

## Ergebnis

| Bereich | NeoForge | Fabric |
| --- | --- | --- |
| Build (beide Loader) | grün | grün |
| JUnit (27 Tests, davon 13 neu) | grün | grün |
| Server-GameTests (53 eigene + 1 vom Loader, davon 22 neu) | 54/54 | 54/54 |
| Leistungstest 100 Zerkleinerer (Grenze 1 ms lokal, 3 ms auf GitHub) | lokal 0,43–0,50 ms | lokal 0,68–0,85 ms |
| Client-Test mit Screenshots | bestanden (23/23 Prüfungen, mit JEI) | bestanden (20 Bilder, mit REI) |
| `gen_resources.py`, `gen_wiki.py --check` | aktuell | aktuell |

Alle Beispielrezepte aus 27.2 laufen in einem GameTest-Durchlauf (Abschlusskriterium M2f): Zerkleinerer (Bauxit), Elektroofen (Quarzsand), Presse (Titanplatte), Elektrolyseur, Raffinerie, Montagetisch (Schaltkreis), Pumpe, Treibstoffgenerator, Akku, Kupferrohr, dazu die Kette Roh-Titan → Platte.

## M2d: neue Server-GameTests (`SwGameTestsM2d`)


| Test | Prüft | Spez. | Zeit |
| --- | --- | --- | --- |
| refinery_crudeoil_kerosene600plastic1within220ticks | fertige Raffinerie: 1.000 mB Erdöl → 600 mB Kerosin + 1 Kunststoff | 27.2, 27.3 M2d | 220 |
| refinery_missingcasing_errorthenidle | ein Gehäuse fehlt → `error` mit Koordinate; nach dem Einsetzen `idle` | 27.3 M2d, 4.3 | 20 |
| refinery_wrongblock_ghostpreview | fremder Block im Muster zählt als fehlend; die Geisterprojektion findet genau diesen Platz; Muster = 44 Plätze | 4.1, 4.3 | 20 |
| refinery_ports_forwardtocontroller | Fluid-Port (Eingang) füllt den Controller und lehnt Wasser ab, Energie-Port lädt ihn, Fluid-Port (Ausgang) schiebt Kerosin in einen Tank, Item-Port (Ausgang) gibt Kunststoff ab und nimmt nichts an, Port ohne Controller tut nichts | 4.3 | 20 |
| port_mode_toggleemptyhand | Rechtsklick mit leerer Hand schaltet `input` → `output`, mit Item in der Hand nicht | 4.3 | 20 |
| assembly_exactmatch_fluidportnotcasing | 4 Platten + 1 Rohr → `fluid_port`; 3 + 1 Platten (zwei Slots) ebenso; 2 + 1 → `refinery_casing`; Überschuss oder fremdes Item → nichts | 22.3, 27.3 M2d | 20 |
| assembly_circuit_within220ticks | 2 Silizium + 4 Kupferdraht (auf zwei Slots) + 1 Kunststoff → 1 Schaltkreis, Eingabeslots danach leer | 27.2 | 220 |
| electrolyzer_water_h2o2within44ticks | 200 mB Wasser → 200 mB Wasserstoff + 100 mB Sauerstoff; Erdöl wird abgelehnt | 27.2 | 44 |
| electrolyzer_fullgastank_error | voller Wasserstofftank → `error` | 5.3 | 20 |
| electrolyzer_tankselection | Tankwahl unter den 2 Ausgangstanks, begrenzt, Sync-Wert stimmt | 4.1 | 20 |
| tank_rejects_gas | Tank nimmt Kerosin, aber keinen Wasserstoff | 6.3 | 20 |

## M2e und M2f: neue Server-GameTests (`SwGameTestsM2e`, `SwGameTestsM2f`)

| Test | Prüft | Spez. | Zeit |
| --- | --- | --- | --- |
| fuelgenerator_kerosenebucket_80jt | Kerosin-Eimer im Leerslot: Tank gefüllt, leerer Eimer bleibt, genau 800 J in 10 t (80 J/t) | 27.3 M2e | 20 |
| fuelgenerator_4mb_16ticks | 4 mB Kerosin reichen 16 t mit 80 J/t, danach `idle` | 27.2 | 30 |
| battery_outputsideonly | Akku gibt nur über die Ausgangsseite ab: Zerkleinerer davor bekommt Energie, der seitliche nicht | 27.3 M2e, 4.4 | 20 |
| battery_io512_platformapi | höchstens 512 J/t Eingang je Tick über die Loader-API; Ausgangsseite nimmt nichts an; Verbraucher nur mit Loader-API bekommt 5.120 J in 10 t | 27.2, 3.3 | 20 |
| battery_chargeslot | Ladeslot nimmt nur ladbare Items; lädt das Loader-Test-Item und einen Akku als Item (Komponente) | 6.3 | 20 |
| battery_broken_keepsenergy | abgebauter Akku behält 123.456 J als `sternenwerft:energy`; Platzieren stellt sie wieder her | 4.1 | 20 |
| performance_100crushers_meantick | 100 laufende Zerkleinerer an 10 Kabelnetzen, Mittel der Tick-Zeit über 200 t ≤ 1 ms (GitHub 3 ms), eigene Testumgebung | 27.2 | 240 |
| handbook_joingift_once | Handbuch-Geschenk genau einmal je Spieler | 24.1, 27.3 M2f | 20 |
| crusher_bauxite_twoaluminumdustwithin220ticks | 1 Bauxit → 2 Aluminiumstaub | 27.2 | 220 |
| furnace_quartzsand_siliconwithin110ticks | 1 Quarzsand → 1 Silizium im Elektroofen | 27.2 | 110 |
| press_titaniumplatewithin110ticks | 1 Titanbarren + Pressform → 1 Titanplatte | 27.2 | 110 |

## Neue JUnit-Tests

Dazu kommen die 4 Tests aus `M2dRulesTest`:

| Test | Prüft |
| --- | --- |
| exactMatchSeparatesRecipesWithSameIngredients | 4 + 1 passt auf `fluid_port`, nicht auf `refinery_casing` |
| exactMatchSumsSlotsAndRejectsSurplus | 64 + 32 = 96 Platten passt; Überschuss, fremdes Item, zu wenig passen nicht |
| refineryPatternHas44PartsWithinNotifyRadius | Raffinerie-Muster aus `spec/data`: 44 Plätze, alle im Meldeabstand |
| hollowPatternHasAirInside | Fusionsreaktor-Muster (`hollow_shell`): 97 Hüllenplätze + 27 Luft |
| categoriesComeFromRecipeTypes | Kategorien der Rezeptanzeige aus den Rezeptarten, Katalysatoren aus machines.json (3.9) |
| addonKindGetsOwnNamespace | Rezeptart eines Addons bekommt `jei.<namespace>.<art>` und ihre Maschine als Katalysator |
| multiplierDoublesStackableIngredients | Multiplikator 2,0: 4 Platten → 8; unstapelbare bleiben; Begrenzung auf 9 Slots (22.3) |
| parsesLinksItemsAndBold, boldAppliesToLinksAndUnclosedMarkupStaysText | Handbuch-Markup `[[…]]`, `<item:…>`, `**…**` (25.6) |

## Client-Tests

Beide Client-Tests laufen bei jedem Push in der CI mit Software-Grafik (Mesa). Die Bilder liegen in den Branches `ci-screenshots` (Fabric) und `ci-screenshots-neoforge`.

**NeoForge** (`NeoForgeClientTest`, mit JEI über `-PwithJei`): 23 Prüfungen, alle bestanden (Lauf für 63a2337).

| Prüfung | Ergebnis |
| --- | --- |
| Config-Sync, Knopf „Seite“ (Server Nord → Süd), Knopf „Tank“ (Server wählt Tank 2) | ✔ |
| GUIs öffnen per Rechtsklick: Tank (leer, mit Erdöl), Zerkleinerer, Elektroofen, Presse, Verbrennungsgenerator, Pumpe, Montagetisch, Elektrolyseur, Raffinerie, Treibstoffgenerator, Akku | ✔ 12 × |
| Raffinerie per Daten-Muster aufgebaut, Aufbau vollständig; Sneak-Rechtsklick startet die Geisterprojektion ohne GUI | ✔ |
| Handbuch: öffnen, einem Link folgen, Seite des Zerkleinerers finden; Geschenk höchstens einmal | ✔ |
| JEI: Plugin aktiv, Rezepte vom Server angekommen, Kategorien Raffinieren und Montage öffnen sich | ✔ |

**Fabric** (`SwClientGameTests`, mit REI über `-PwithRei`): bestanden, 20 Bilder (Lauf für c620fae).

| Prüfung | Ergebnis |
| --- | --- |
| Handbuch-Geschenk beim ersten Join: genau eins im Inventar | ✔ |
| Config-Sync, Knopf „Seite“, Knopf „Tank“ | ✔ |
| GUIs aller Maschinen, Generatoren und Speicher, Raffinerie mit Geisterprojektion, Weltfoto mit allen Blöcken | ✔ 18 Bilder |
| Handbuch: Willkommen, gefolgter Link, Seite des Zerkleinerers | ✔ |
| REI: Plugin aktiv, Rezepte angekommen, Haken für Taste H gesetzt, Kategorien Raffinieren und Montage öffnen sich | ✔ |

**Sichtprüfung der Screenshots:**

- Handbuch: Inhaltsverzeichnis mit Kapiteln, Seite mit Fettdruck, Listen, Steckbrief, Rezept als Symbole (3 × 3-Raster der Werkbank), Bildlaufleiste. Im CI-Client (Englisch) stehen Titel auf Englisch, Seitentext auf Deutsch; englische Seiten kommen mit M9 (CLAUDE.md).
- JEI: Raffinieren zeigt Erdöl → Kunststoff + Kerosin mit „200 t · 8000 J“, Montage den Schaltkreis mit Mengen. **Gefunden:** Der große Ausgaberahmen überdeckte den Kerosin-Slot daneben; behoben (normaler Rahmen).
- REI: Kategorien mit Maschine als Werkbank, Mengen, Dauer und Energie. **Gefunden:** Fluid-Slots (Erdöl, Kerosin, Wasser) blieben leer, weil Architectury unter Fabric 26.3 nur eigene Fluide zeichnet; dazu ein überflüssiger „!“-Knopf. Behoben: eigener Zeichner (Farbfläche wie in den Maschinen-GUIs) und `removePlusButton`.
- Treibstoffgenerator: Füllslot hat aus dem Tank einen Kerosin-Eimer gemacht, Brennzeit-Pfeil, Statuslampe grün. Akku: Ladeslot mit Akku-Item, Knopf „Seite“, kein Pfeil.

## Gefundene und behobene Fehler

| Fehler | Gefunden durch | Behoben |
| --- | --- | --- |
| Maschinen, Kabel und Rohre ließen sich mit einer Holzspitzhacke abbauen (Tag `needs_stone_tool` fehlte, 4.1) | Prüfung der Tags beim Erweitern von `gen_resources` | M2d |
| Rezepte ohne Item-Zutat hätte der Rezeptmanager nie gefunden (`RecipeInput#isEmpty`) | Durchsicht der 26.3-API vor dem Test | `MachineRecipeInput#isEmpty` zählt Tanks mit |
| Upgrades des Elektrolyseurs lagen auf der Inventar-Beschriftung; Knopf „Tank“ zu schmal; Erdöl in der Leiste unsichtbar (schwarz auf dunkel) | Screenshots M2d | `MachineGeometry`, Knopf „T1–T3“ mit Tooltip, heller Leistenhintergrund |
| Geisterprojektion im Screenshot nicht zu sehen (fehlender Block hinten oben) | Screenshots M2d | Client-Tests entfernen ein Gehäuse in der Vorderseite |
| 100 Zerkleinerer brauchten 3,4 ms je Tick: Balance-JSON wurde jeden Tick gelesen, Maschinen jeden Tick neu eingerichtet, Komparator-Meldungen bei jedem `setChanged` | Leistungstest (27.2) mit Zeitmessung je Teil | Zwischenspeicher in `BalanceManager`, Neueinrichtung nur nach `/reload`, letztes Rezept zuerst, `setChanged` ohne Komparatoren: 0,4–0,9 ms |
| Leistungstest maß die anderen 50 Tests mit | Messung über `onEachTick` | eigene Testumgebung (eigener Durchgang) und Server-eigene Tick-Zeit |
| Kerosin-Eimer stapelte bis 64 statt 1 (4.1) | Prüfung von `items.json` beim Handbuch-Item | einfache Items übernehmen `max_stack` |
| Client-Tests scheiterten an einem Ausfall des Ubuntu-Paketspiegels | CI (99f069b) | `apt-get` mit bis zu 3 Versuchen |
| JEI: großer Ausgaberahmen überdeckte den Fluid-Slot daneben | Screenshot JEI (63a2337) | normaler Slotrahmen |
| REI: Fluid-Slots leer, „!“-Knopf ohne Funktion | Screenshot REI (c620fae) | eigener Fluid-Zeichner, Knopf entfernt |

## Nicht automatisch geprüft

- **JEI unter Fabric und REI unter NeoForge:** Geprüft sind JEI unter NeoForge und REI unter Fabric; die Plugin-Logik ist dieselbe.
- **EMI:** gibt es für 26.3 nicht (spec-gaps #33).
- **Taste `H` per echtem Tastendruck:** Die Client-Tests prüfen nur, dass die Haken gesetzt sind und kein Suchfeld aktiv ist (spec-gaps #36).
- **Aussehen der Geisterprojektion über längere Zeit** (nur Momentaufnahme).
- **Trichter an Item-Ports in der echten Welt** (geprüft über die Container-Schnittstelle).
- **Zusammenspiel mit echten fremden Mods** (Ports, Akku, Ladeslot): geprüft mit Test-Verbraucher und Test-Item über die Loader-APIs.
