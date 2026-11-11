# Testbericht: M2d (Montagetisch, Elektrolyseur, Raffinerie, Ports)

Stand: 11.11.2026, Commit `3e18d9f`. Vorherige Runde: `docs/dev/testbericht-m2abc.md`.

## Ergebnis

| Bereich | NeoForge | Fabric |
| --- | --- | --- |
| Build (beide Loader) | grün | grün |
| JUnit (22 Tests, davon 4 neu) | grün | grün |
| Server-GameTests (42 eigene + 1 vom Loader, davon 11 neu) | 43/43 (lokal) | 43/43 (lokal) |
| Client-Test mit Screenshots | CI_NEOFORGE | CI_FABRIC |

## Neue Server-GameTests

Liste in `common/gametests.json`, Testlogik in `SwGameTestsM2d`.

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

## Neue JUnit-Tests (`M2dRulesTest`)

| Test | Prüft |
| --- | --- |
| exactMatchSeparatesRecipesWithSameIngredients | 4 + 1 passt auf `fluid_port`, nicht auf `refinery_casing` |
| exactMatchSumsSlotsAndRejectsSurplus | 64 + 32 = 96 Platten passt; Überschuss, fremdes Item, zu wenig passen nicht |
| refineryPatternHas44PartsWithinNotifyRadius | Raffinerie-Muster aus `spec/data`: 44 Plätze, alle im Meldeabstand |
| hollowPatternHasAirInside | Fusionsreaktor-Muster (`hollow_shell`): 97 Hüllenplätze + 27 Luft |

## Client-Tests

CLIENT_SECTION

## Gefundene und behobene Fehler

| Fehler | Gefunden durch | Behoben |
| --- | --- | --- |
| Maschinen, Kabel und Rohre ließen sich mit einer Holzspitzhacke abbauen (Tag `needs_stone_tool` fehlte, 4.1) | Prüfung der Tags beim Erweitern von `gen_resources` | M2d |
| Rezepte ohne Item-Zutat hätte der Rezeptmanager nie gefunden (`RecipeInput#isEmpty`) | Durchsicht der 26.3-API vor dem Test | `MachineRecipeInput#isEmpty` zählt Tanks mit |

## Nicht automatisch geprüft

- Aussehen der Geisterprojektion über längere Zeit (nur Momentaufnahme).
- Trichter an Item-Ports in der echten Welt (geprüft über die Container-Schnittstelle).
- Zusammenspiel der Ports mit Rohren und Kabeln fremder Mods.
