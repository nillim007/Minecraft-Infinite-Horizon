# Annahmen und offene Fragen aus der Umsetzung

Jede Annahme, die eine KI-Sitzung trifft, weil die Spezifikation etwas offen lässt, kommt hierher, zusätzlich zum `// SPEC-GAP`-Kommentar im Code. Nach jedem Meilenstein werden die Einträge geprüft: Bestätigte Annahmen wandern in die Spezifikation, verworfene werden im Code korrigiert.

## Format

| # | Meilenstein | Kapitel | Lücke | Gewählte Lösung | Code-Stelle | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | M1 | 4.4 | Beispiel: Reihenfolge der Empfänger bei gleichmäßiger Verteilung | Reihenfolge nach Blockposition, Rest im nächsten Tick | `core/grid/EnergyGrid.java` | offen |

Status: `offen` (noch nicht geprüft), `übernommen` (in Spezifikation eingearbeitet), `verworfen` (Code anpassen).

## Einträge

| # | Meilenstein | Kapitel | Lücke | Gewählte Lösung | Code-Stelle | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Datenaufbau | 13.4 | Keine y-Bereiche/Häufigkeiten für `moon_ice`, Merkur-Titan, Merkur-Eisen, Merkur-Eis, `io_sulfur_ore` | Werte in `planets.json` auf `null`; vor M5/M7 festlegen | `spec/data/planets.json` | offen |
| 2 | Datenaufbau | 13.4 | Kein Abstand (`spacing_chunks`) für `great_crater`, Mars-`lava_tube`, `volcanic_vent`, `hydrothermal_field`, `large_asteroid` | `null`; vor M5/M7 festlegen | `spec/data/planets.json` | offen |
| 3 | Datenaufbau | 13.4 | Meeresspiegel Io (Lava) fehlt | `null` | `spec/data/planets.json` | offen |
| 4 | Datenaufbau | 13.1 | Orbits und Asteroidengürtel ohne eigenes Biom | `minecraft:the_void` | `spec/data/planets.json` | offen |
| 5 | Datenaufbau | 13.4 | `default_block` für Merkur, Ceres, Io, Europa, Titan, Enceladus, Triton, Pluto nicht genannt | jeweils das Hauptgestein (z. B. `mercury_rock`) | `spec/data/planets.json` | offen |
| 6 | Datenaufbau | 19 | Ereignis-IDs nicht genannt | `solar_storm`, `meteor_shower`, `dust_storm`, `acid_rain`, `volcanic_eruption`, `methane_rain`, `ring_debris` | `spec/data/items.json`, `balance/events.json` | offen |
| 7 | Datenaufbau | 6.1 | Tankgröße der Pumpstation nicht genannt | 16.000 mB | `balance/machines.json` | offen |
| 8 | M1 | 3.8, 3A.6 | Datagen soll in Java laufen (`runData`); gebaut wird aber nur auf GitHub, und die Ressourcen sind reine Datenumwandlung | Ressourcen erzeugt `tools/gen_resources.py` (plus `tools/gen_placeholders.py`) aus `spec/data` nach `common/src/generated/resources`; die CI prüft, dass sie aktuell sind. Welche IDs registriert sind, steht in `common/implemented_ids.json`. | `tools/gen_resources.py` | in Spezifikation übernommen |
| 9 | M1 | 4.1 | Zahl der Ausgabeslots des Zerkleinerers nicht genannt | 4 (aus `balance/machines.json`) | `balance/machines.json` | offen |
| 10 | M1 | 4.1 | GUI der Maschinen gehört zu 4.1, ist für die Abnahme von M1 aber nicht nötig | In M1 keine GUI; Bedienung über Trichter (`WorldlyContainer`: Eingänge von allen Seiten, nur Ausgaben entnehmbar). GUI ab M2. | `machine/MachineBlockEntity.java` | in Spezifikation übernommen |
| 11 | M1 | 27 | Test-Verbraucher „nur Plattform-API“ braucht einen Block | Energie-Capability bzw. Team-Reborn-API am Vanilla-Block `minecraft:target`, nur im GameTest-Betrieb aktiv; kein eigener Testblock | `NeoForgeEnergyHelper`, `FabricEnergyHelper` | in Spezifikation übernommen |
| 12 | M1 | 27 | GameTest „Wert in machines.json ändern + `/reload`“: Dateien lassen sich im Test nicht ändern | Test setzt eine geänderte Kopie genau wie der Reload-Listener (`BalanceManager.set`) und stellt sie im selben Tick zurück. Manuell: Datapack mit `data/<ns>/sternenwerft/balance/machines.json` + `/reload` | `gametest/SwGameTests.java` | in Spezifikation übernommen |
| 13 | M1 | 25.4 | Rezept `crushing/iron_dust` nennt `ticks`/`energy_per_tick` explizit; dann wirkt machines.json nicht | `gen_resources.py` lässt Werte weg, die der Maschine entsprechen (laut SCHEMA gelten dann die Maschinenwerte) | `tools/gen_resources.py` | in Spezifikation übernommen |
| 14 | M1 | 4.2 | Upgrade-Items erst in M2 | Upgrade-Slots existieren, nehmen aber nichts an; Rechnung mit 0 Upgrades | `ProcessingMachineBlockEntity.java` | in Spezifikation übernommen, Umsetzung M2 |
| 15 | M1 | 6.3 | Brennstoffe mit Behälter (Lavaeimer) | In M1 geht der Eimer verloren; Füll-/Leerslots kommen mit den Fluiden | `CombustionGeneratorBlockEntity.java` | in Spezifikation übernommen, Umsetzung M2 |
| 16 | M1 | 3A.2, 25.7 | Wunsch des Auftraggebers: einfache Schnittstelle für Addons mit vielen Einstellmöglichkeiten | Paket `de.sternenwerft.api`; Balance-Dateien aus allen Namespaces werden zusammengeführt (Einträge ergänzen/überschreiben, `"replace": true` ersetzt); Basisklassen für Maschinen, Generatoren, Kabel, Rezeptarten. Beschreibung: `docs/dev/addon-api.md` | `api/`, `data/BalanceReloadListener.java` | in Spezifikation übernommen |
| 17 | M2 | 24.5 | Platzhalterfarben für Metalle und `quartz_sand` standen nur im Skript | Tabelle in 24.5 aus `tools/gen_placeholders.py` übernommen | `tools/gen_placeholders.py` | in Spezifikation übernommen |
| 18 | M2 | 3A.2, 3A.3 | `SwBlockEntity` ist vorgeschrieben, fehlt aber im Code aus M1; Speicher, Ports, Kabel und Rohre brauchen eine Basis | `SwBlockEntity` als Basis für Speicher, Ports, Kabel und Rohre, ohne `owner_agency` bis M3; Maschinen erben weiter von `MachineBlockEntity` | `block/SwBlockEntity.java` | in Spezifikation übernommen |
| 19 | M2a | 4.1, 24.5 | Für das Maschinen-GUI ist keine Textur vorgegeben | Das GUI wird aus Rechtecken gezeichnet (Hintergrund, Slots, Energieleiste, Pfeil) nach den Maßen aus 4.1; keine Bilddatei nötig | `client/MachineScreen.java` | offen |
| 20 | M2a | 3A.2, 26 | Einfache Items (Materialien, Upgrades, Pressformen) einzeln in Java aufzulisten verdoppelt `implemented_ids.json` | `gen_resources.py` schreibt `sternenwerft/simple_items.json`; `ModItems` registriert daraus alle Items ohne eigenes Verhalten | `registry/ModItems.java` | offen |
| 21 | M2b | 27.3 | Ölblase braucht das Fluid `crude_oil`, das erst M2c registriert | Ölblase wandert nach M2c; M2b enthält Erze, Quarzsand, Loot, Tags, Worldgen | `docs/spec/27-meilensteine.md` | offen |
