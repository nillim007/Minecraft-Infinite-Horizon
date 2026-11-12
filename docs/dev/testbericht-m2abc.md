# Testbericht: Meilenstein 1 bis M2c

Stand: 11.11.2026, Commit `f683a24`, GitHub-Lauf [43018888655](https://github.com/nillim007/Minecraft-Infinite-Horizon/actions/runs/43018888655).

## Ergebnis

| Bereich | NeoForge | Fabric |
| --- | --- | --- |
| Build (beide Loader) | grün | grün |
| JUnit (18 Tests, gemeinsamer Code) | grün | grün |
| Server-GameTests (31 eigene + 1 vom Loader) | 32/32 bestanden | 32/32 bestanden |
| Client-Test (echter Client, Software-Grafik) | bestanden (9/9), 7 Screenshots | bestanden, 8 Screenshots |
| Spezifikation (`validate_spec`), erzeugte Dateien aktuell | grün | grün |

Gefunden und behoben wurden in dieser Runde ein echter Fehler im Spielverhalten (Shift-Klick) und zwei offene Punkte. Dazu kommen die Fehler aus den vorigen Runden (Tabelle unten).

## Server-GameTests

Alle Tests laufen auf **beiden** Loadern. Die Liste steht in `common/gametests.json`. Die Spalte „Zeit“ ist die Grenze in Ticks; überschreitet ein Test sie, gilt er als fehlgeschlagen.

| Test | Prüft | Spez. | Zeit | Ergebnis |
| --- | --- | --- | --- | --- |
| crusher_rawiron_twodustwithin220ticks | Generator mit Kohle → Kabel → Zerkleinerer: 1 Roheisen → 2 Eisenstaub | 27 M1 | 220 | ✔ |
| crusher_balancechange_changesduration | geänderter Wert in machines.json ändert die Dauer | 27 M1 | 20 | ✔ |
| cable_platformconsumer_receivesenergy | Verbraucher nur mit Loader-API bekommt Strom aus dem Kabel | 27 M1 | 100 | ✔ |
| machines_rawtitanium_platewithin440ticks | Roh-Titan → Staub → Barren → Platte (Zerkleinerer, Elektroofen, Presse) | 27.3 M2a | 440 | ✔ |
| ores_tooltier_dropsonlywithcorrecttool | Bauxiterz: Steinspitzhacke gibt 1 Bauxit; Titanerz braucht Eisenspitzhacke | 27.3 M2b | 20 | ✔ |
| pump_crudeoil_tank1000within50ticks | Pumpe → Kupferrohr → Tank: 1.000 mB Erdöl | 27.3 M2c, #23 | 50 | ✔ |
| pipes_twofluids_nevermixed | Wasser und Erdöl an einem Rohrnetz: beide Empfänger erhalten dasselbe Fluid | 27.3 M2c | 60 | ✔ |
| tank_fillslot_bucketbecomescrudeoilbucket | leerer Eimer im Füllslot wird zum Erdöleimer | 27.3 M2c | 20 | ✔ |
| generator_coal_burns1600ticks | Kohle brennt 1.600 t, Status running, Energie entsteht | 6.3 | 20 | ✔ |
| generator_lavabucket_keepsbucket | Lavaeimer hinterlässt den leeren Eimer im Brennstoffslot | 6.3 | 20 | ✔ |
| crusher_outputfull_statuserror | volle Ausgabeslots → Statuslampe error | 4.1 | 20 | ✔ |
| crusher_fourspeedupgrades_63ticks101jt | 4 Speed-Upgrades → 63 t und 101 J/t | 4.2 | 20 | ✔ |
| press_withoutmold_noprogress | Presse ohne Pressform macht nichts | 5.2 | 40 | ✔ |
| furnace_rawiron_ironingotwithin110ticks | Elektroofen schmilzt Vanilla-Rezepte (Roheisen → Eisenbarren) | 6.1 | 110 | ✔ |
| crusher_status_idlerunningidle | Statuslampe idle → running → idle | 4.1 | 230 | ✔ |
| hopper_feedsanddrains_crusher | Trichter oben füllt, Trichter unten zieht 2 Eisenstaub ab | 4.1 | 300 | ✔ |
| hopper_neverfills_upgradeslot | Trichter legen nie etwas in Upgrade-Slots | 4.1 | 40 | ✔ |
| containerslots_hopperrules | Füllslot: nur volle Eimer raus; Leerslot: nur leere Eimer raus | 4.1 | 20 | ✔ |
| tank_drainslot_waterbucket | Wassereimer im Leerslot → 1.000 mB Wasser, leerer Eimer bleibt | 4.1 | 20 | ✔ |
| tank_broken_keepscontents | abgebauter Tank behält 3.000 mB als Komponente; Platzieren stellt sie wieder her | 4.1 | 20 | ✔ |
| menu_create_matcheslayout | Block-Entity erzeugt das Maschinenmenü mit passendem Slot-Aufbau | 4.1 | 20 | ✔ |
| menu_shiftclick_routing | Shift-Klick: Upgrade → Upgrade-Slot, Pressform → Formslot, Barren → Eingabe | 4.1 | 20 | ✔ (nach Fehlerbehebung) |
| payload_machineconfig_onlyinrange | Ausgangsseite ändert sich nur, wenn der Spieler höchstens 8 Blöcke entfernt ist | 3A.5 | 20 | ✔ |
| cable_throughput_256jt | Kupferkabel liefert höchstens 256 J/t | 4.4 | 20 | ✔ |
| pipe_throughput_200mbt | Kupferrohr liefert höchstens 200 mB/t | 4.4 | 20 | ✔ |
| platformapi_energy_insertsintocrusher | Energie über die Loader-API (Capability bzw. Team Reborn) kommt an | 3.3 | 20 | ✔ |
| platformapi_fluid_respectsoutputside | Fluid über die Loader-API kommt an; die Ausgangsseite nimmt nichts an | 3.3, 4.1 | 20 | ✔ |
| pump_water_removessource | Pumpe saugt eine Wasserquelle ab, die Quelle verschwindet | 6.1 | 60 | ✔ |
| worldgen_oilbubble_feature | Fluidkugel ersetzt Gestein durch Quellen, lässt Grundgestein und Luft; `oil_bubble` ist registriert | 13.4 | 20 | ✔ |
| worldgen_biomes_containfeatures | Bauxit in Savanne und Wüste (nicht in der Ebene), Titan in der Ebene, Quarzsand am Strand, Ölblase in der Wüste | 13.4, 27.3 M2b | 20 | ✔ |
| config_defaults_loaded | Serverconfig beim Start geladen, Standardwerte aus misc.json | 24.4 | 20 | ✔ |

## JUnit (gemeinsamer Code)

| Klasse | Tests | Prüft |
| --- | --- | --- |
| TransactionTest | 4 | Commit, Rücknahme, verschachtelte Teilnehmer |
| BalanceManagerTest | 6 | Werte aus spec/data, Puffer-Formel, Upgrade-Rechnung (63 t / 101 J/t, 10 J/t bei 4 Effizienz) |
| PureRulesTest | 5 | gleichmäßige Netzverteilung, Zusammenführen der Balance aus mehreren Namespaces, Config-Prüfung |
| FluidTankTest | 3 | ein Fluid je Tank, Rücknahme per Transaktion, Filter gegen Selbstbefüllung |

## Client-Test NeoForge (echter Client, xvfb + Mesa)

`NeoForgeClientTest` läuft nur mit `-Dinfinite_horizon.clienttest=true` (Run `runClientTest`). Er legt selbst eine Flachland-Welt an, öffnet jedes GUI per echtem Rechtsklick über das Netzwerk und schreibt eine Ergebnisdatei. Die Bilder liegen im Branch `ci-screenshots-neoforge`.

| Prüfung | Ergebnis |
| --- | --- |
| Welt anlegen und betreten (CreateWorldScreen, Flachland, Kreativ) | ✔ |
| SyncConfigPayload beim Client angekommen | ✔ PASS config_sync |
| Knopf „Seite“: Server-Ausgangsseite Nord → Süd | ✔ PASS side_button |
| GUIs von Tank (leer und mit Erdöl), Zerkleinerer, Elektroofen, Presse, Generator und Pumpe öffnen sich | ✔ 7 × PASS |
| Sichtprüfung: Darstellung wie unter Fabric (Leisten, Knopf, Slots) | ✔ |
| Nebenbefund: Abgebaute Maschinen werfen ihren Inhalt aus, der Spieler sammelt ihn auf (sichtbar in der Hotbar) | ✔ wie vorgesehen |

## Client-Test Fabric (echter Client)

Er öffnet eine Testwelt, setzt die Blöcke, öffnet jedes GUI per Rechtsklick und fotografiert es. Die Bilder liegen im Branch `ci-screenshots`.

| Prüfung | Ergebnis |
| --- | --- |
| SyncConfigPayload: Client kennt `event_frequency = normal` nach dem Beitritt | ✔ |
| Knopf „Seite“ im Tank-GUI: Server-Ausgangsseite wechselt von Nord auf Süd, Knopf zeigt „South“ | ✔ |
| GUIs von Zerkleinerer, Elektroofen, Presse, Tank, Pumpe und Generator öffnen sich | ✔ |
| Sichtprüfung der Screenshots: keine Überlappungen, Leisten, Pfeil, Slots, Stapelzahlen | ✔ |
| Weltfoto: Vorderseiten mit Statuslampe, Ausrichtung, Kabelverbindung, Erz- und Quarzsand-Texturen | ✔ |

## Gefundene und behobene Fehler

| Fehler | Gefunden durch | Behoben in |
| --- | --- | --- |
| Shift-Klick legte Upgrades und Pressformen in den Eingabeslot statt in Upgrade- bzw. Formslot | GameTest `menu_shiftclick_routing` | `MachineMenu` (Vorrang nach Slot-Rolle) |
| Energieleiste überlappte „Inventory“; kein Pfeil; Chat im Bild | Screenshots | M2a |
| Statuslampe und Ausrichtung in der Welt unsichtbar | Screenshots | M2a (`orientable`-Modelle) |
| Erzsprenkel alle gelb statt Metallfarbe | Screenshots | M2b |
| Worldgen, Loot-Tables, Block-Provider im alten 1.21-Format | Server-GameTests (Welt lud nicht, Erz droppte sich selbst) | M2b (26.3-Formate) |
| Upgrade-Slots überlappten den Leerslot der Pumpe; Tank zeigte leere Energieleiste und Pfeil | Screenshots | M2c |
| Kerosin-Eimer wäre doppelt registriert worden | Prüfung der Daten | M2c |

## Offene Punkte dieser Runde

| Punkt | Entscheidung |
| --- | --- |
| Lavaeimer im Generator | umgesetzt: der leere Eimer bleibt im Slot (6.3) |
| Tank-Inhalt beim Abbauen | umgesetzt: Komponente `infinite_horizon:fluid`; der Loot kopiert sie (4.1) |
| Knopf „Tank“ | erst mit Maschinen mit mehreren Tanks (Elektrolyseur, M2d). Payload und Sync-Wert sind schon vorhanden |
| Zeitgrenze der Pumpe | 50 t statt 44 t wegen des Rohrdurchsatzes (spec-gaps #23) |

## Nicht automatisch geprüft

- **Aussehen der Ölblasen und Erzadern** in einer echten, generierten Welt. Geprüft sind nur die Biomzuordnung und die Platzierungslogik.
- **Bedienung mit Maus und Spielerhand im GUI:** Shift-Klick ist auf dem Server getestet, Drag & Drop nicht.
- **Zusammenspiel mit echten fremden Mods:** Die Loader-APIs sind mit einem Test-Verbraucher bzw. über die eigene Schnittstelle getestet.

## Wiederholen

```bash
python tools/gen_gametests.py
```

```bash
./gradlew :fabric:runGameTestServer
```

Die vollständige Prüfung beider Loader mit Screenshots läuft bei jedem Push in GitHub Actions. Die Ergebnisse zeigt `python tools/ci_status.py --wait`, die Bilder liegen im Branch `ci-screenshots`.
