# 3A. Architektur

Dieses Kapitel legt fest, **wie der Code gegliedert ist**. Jede Sitzung MUSS sich daran halten, damit spätere Meilensteine auf früheren aufbauen können. Abweichungen nur mit Eintrag in `docs/spec-gaps.md`.

## 3A.1 Grundprinzipien

1. **Der Server entscheidet alles.** Der Client rendert, zeigt HUD und Zwischensequenzen und dämpft Klänge. Keine Spielentscheidung fällt auf dem Client.
2. **Reine Funktionen für Regeln.** Jede Berechnung mit Spielregeln (Umwelt, Treibstoff, Raketenprüfung, Forschungstore, Offline-Produktion, Fracht) steht in einer zustandslosen Klasse ohne Minecraft-Weltzugriff. Block-Entities und Events rufen sie nur auf. So sind die Regeln mit JUnit testbar.
3. **Alle Zahlen aus Daten.** Kein Balance-Wert steht im Code. Code liest Werte über `BalanceManager` (3A.6) bzw. die eigenen Registries.
4. **Plattformcode bleibt dünn.** `neoforge/` und `fabric/` enthalten nur Registrierung, Capability-/API-Adapter, Event-Weiterleitung und Render-Hooks. Logik gehört nach `common/`.
5. **Besitz ist überall.** Jede Block-Entity und jedes Fahrzeug kennt `owner_agency` (23.1). Das gilt ab M3 (Agentur-Kern); in M1 und M2 gibt es noch keinen Besitzer.

## 3A.2 Paketstruktur (`common`)

Gruppen-ID und Basispaket: `de.sternenwerft` (vor dem Start änderbar, dann überall gleich).

```
de.sternenwerft
├─ Sternenwerft                 Einstieg: Konstanten (MOD_ID), init() ruft alle Module in fester Reihenfolge
├─ platform/                    Service-Interfaces (3.2), Lader per ServiceLoader
├─ registry/                    ModBlocks, ModItems, ModFluids, ModEntities, ModBlockEntities, ModMenus,
│                               ModRecipeTypes, ModRecipeSerializers, ModDataComponents, ModSounds,
│                               ModCreativeTabs, ModStructures, ModFeatures, ModAttachments, ModKeys (Dimensionen)
├─ data/                        BalanceManager, PlanetRegistry, ResearchRegistry, RocketPartRegistry,
│                               StarSystemRegistry, HandbookLoader (Client-Reload-Listener für assets/<ns>/handbook, 25.6)
├─ core/
│  ├─ transaction/              Transaction (Snapshot → commit/rollback), TransactionAdapter
│  ├─ energy/                   SwEnergyStorage
│  ├─ fluid/                    SwFluidTank, SwFluidStack, FluidContainerItem (Füll-/Leerslot-Logik)
│  ├─ item/                     SwItemContainer
│  └─ grid/                     GridManager, EnergyGrid, FluidGrid (Netze aus 4.4)
├─ block/                       SwBlockEntity (Basis mit owner_agency), OwnedBlock, Statuslampe
├─ machine/                     MachineBlock, MachineBlockEntity, MachineMenu, Upgrades, je Maschine eine Unterklasse
├─ multiblock/                  MultiblockController, MultiblockPattern, PortBlockEntity, GhostPreview
├─ environment/                 EnvironmentCalculator, EnvironmentInput, EnvironmentResult, Hazard, HazardTracker,
│                               AtmosphereRules (8.5), GravityService (12.9)
├─ suit/                        SuitItem, SuitProtection, SuitModules, OxygenService
├─ dome/                        DomeGeneratorBlockEntity, DomeManager, DomeVolume
├─ research/                    FpLedger, ResearchGates, SampleTool, AnalyzerBlockEntity, FieldScanner
├─ agency/                      Agency, AgencyManager, Role, Permissions, OwnershipResolver (merged_into)
├─ space/                       FuelCalculator, RocketAssembler, RocketConfig, RocketEntity, LaunchControlBlockEntity,
│                               FlightService, LandingService, ProbeService, LanderEntity, Destinations
├─ world/                       Dimensionen, MercuryBiomeSource, PlutoBiomeSource, Features, Structures, Carver,
│                               DimensionWeatherManager, EventScheduler, MeteorEntity
├─ station/                     StationCoreBlockEntity, StationModules, ZeroGMovement, GravityPlateBlockEntity
├─ vehicle/                     RoverEntity, RoverTrailerEntity, StarshipEntity, SeatEntity
├─ creature/                    je Kreatur Entity + Goals, CreatureSpawns
├─ logistics/                   CargoTerminalBlockEntity, OutpostManager, OfflineProduction, CargoFlightManager,
│                               Route, LogisticsComputerBlockEntity, TradingPostBlockEntity
├─ shipyard/                    ShipyardTerminalBlockEntity, ShipyardDockBlockEntity, ShipyardState
├─ food/                        FoodPackerBlockEntity, HydroponicsBlockEntity, GreenhouseBlockEntity
├─ recipe/                      eigene Recipe-Klassen + Serializer (Codecs), ResearchRecipeLock
├─ network/                     alle Payloads (3A.4), PacketHandlers
├─ client/                      HudOverlay, Screens, SkyRenderer-Logik, DomeRenderer, Cutscenes, SoundDamping,
│                               handbook/ (HandbookScreen, HandbookMarkup als reine Funktion, HandbookHistory)
├─ compat/                      jei/, rei/, emi/ (nur laden, wenn Mod vorhanden)
├─ api/                         stabile Schnittstelle für Addons (3A.10)
└─ util/
```

## 3A.3 Zentrale Klassen

| Klasse | Aufgabe | Wichtige Regeln |
| --- | --- | --- |
| `SwBlockEntity` | Basis aller eigenen Block-Entities | speichert `owner_agency`; setzt es in `setPlacedBy`; liefert `OwnershipResolver.current(owner)` (berücksichtigt `merged_into`). Ab M2 Basisklasse für Speicher, Ports sowie Kabel- und Rohrblöcke; `owner_agency` und `setPlacedBy` kommen erst mit M3. Maschinen erben von `MachineBlockEntity`. |
| `MachineBlockEntity` | Basis aller Maschinen | liest Werte aus `BalanceManager` per Block-ID; Zustände `idle`/`running`/`error` → Blockstate `status`; Upgrades; Füll-/Leerslots; Rezeptsuche ruft `ResearchRecipeLock.isUnlocked(recipe, owner)` auf (Standard `true`; die Prüfung der Forschung der Besitzer-Agentur kommt mit M4, die Agentur mit M3) |
| `MultiblockController` | Basis für Raffinerie, Fusion, Kernreaktor, Startrampe | Musterprüfung beim Setzen, bei Nachbaränderungen und alle 100 t; nimmt an freien Seiten I/O an |
| `Transaction` | gemeinsames Transaktionsmodell | `simulate → commit/rollback`; Adapter auf NeoForge-Transfer-API und Fabric-Transfer-API |
| `EnvironmentCalculator` | reine Funktion (8.1) | Eingabe: Planet, Zonenliste, Position, Tageszeit, Kuppeln, Ausrüstung; Ausgabe: Werte + Schutz je Gefahr |
| `HazardTracker` | Belastung je Spieler | Spieler-Attachment; alle 10 t; ruft `EnvironmentCalculator`; sendet `HazardStatePayload` nur bei Änderung |
| `FuelCalculator` | reine Funktion (12.2) | Kosten A→B, Hin- und Rückflug, erreichbare Ziele für eine `RocketConfig` |
| `RocketAssembler` | reine Funktion (12.4) | Block-Schnappschuss der Rampe → `RocketConfig` oder Fehlerliste (für die Checkliste) |
| `ResearchGates` | reine Funktion (11.4, 25.3) | Agentur-Fortschritt + Tor-Definition → offen/zu |
| `OfflineProduction` | reine Funktion (20.1) | Snapshot (running, Rate, Eingaben, Platz) + vergangene Ticks → Ausbeute |
| `FpLedger` | FP-Buchhaltung | Hundertstel-FP, Abnahme je Probenart, Labor-Bonus |
| `GridManager` | Kabel- und Rohrnetze | Topologie nur bei Blockänderung neu berechnen; Push-Verteilung nach 4.4 |
| `DomeManager` | Kuppeln je Dimension | räumlicher Index (Chunk → Kuppeln); Abfrage `isInsideActiveDome(pos)` |
| `FlightService` | Raketenflug serverseitig | Prüfen, Entity umwandeln, Treibstoff abziehen, nach fester Zeit teleportieren und landen; Client-Sequenz ist nur Darstellung |

## 3A.4 Gespeicherte Daten

| Name | Ort | Inhalt |
| --- | --- | --- |
| `AgencyData` | Overworld, `SavedData` | Agenturen, Mitglieder, Rollen, FP, erforschte Knoten, offene Stufen, Sondenziele, Atlas, Entdecker-Einträge, `chart_uuid`, `merged_into`, Partner, entdeckte Strukturinstanzen |
| `DomeData` | je Dimension, `SavedData` | aktive Kuppeln (Mittelpunkt, Radius, Stufe, Agentur, Zustand) |
| `WeatherData` | je Mod-Dimension, `SavedData` | Wetterzustand, Restdauer, geplante Ereignisse |
| `OutpostData` | Overworld, `SavedData` | Außenposten, Produzenten-Snapshots, letzter Tick |
| `CargoData` | Overworld, `SavedData` | Routen, Raketen im Flug (Start, Ankunft, Fracht) |
| `ShipyardData` | Overworld, `SavedData` | Fortschritt und Lager je Werft |
| `CloneData` | Overworld, `SavedData` | Bindung je Spieler, gemeldeter Kammerzustand |
| `WorldFlags` | je Dimension, `SavedData` | einmalige Weltereignisse (z. B. Venus-Plattformen, Boss-Beschwörungen) |
| Chunk-Attachment `sampled_positions` | je Chunk | beprobte bzw. von Spielern gesetzte Positionen (11.2) |
| Spieler-Attachment `hazard_state` | je Spieler | Belastung je Gefahr, Venus-Hitzezeit, Klon-Bindung (Kopie) |

Jede `SavedData` hat ein Feld `data_version` (Start: 1) und eine Migrationsmethode für spätere Versionen.

## 3A.5 Netzwerk-Payloads

Alle Payloads sind `CustomPacketPayload` mit `StreamCodec`. Jeder Client→Server-Payload wird serverseitig geprüft (Abstand ≤ 8 Blöcke zum Block, Rechte der Agentur, gültige IDs).

| Payload | Richtung | Inhalt | Wann |
| --- | --- | --- | --- |
| `SyncDataPayload` | S→C | Balance, Planeten, Forschung, Raketenteile (komprimiert, ggf. in Teilen) | Join, `/reload` |
| `SyncConfigPayload` | S→C | Serverkonfiguration (24.4) | Join, Änderung |
| `AgencyStatePayload` | S→C | Agentur, Rolle, FP, Knoten, offene Stufen, Sondenziele, Atlas | Join, Änderung |
| `DomeStatePayload` | S→C | Kuppeln der aktuellen Dimension | Dimensionswechsel, Änderung |
| `HazardStatePayload` | S→C | Werte und Belastungen für das HUD | bei Änderung, höchstens alle 10 t |
| `WeatherStatePayload` | S→C | Wetter und Ereigniswarnung der Dimension | Dimensionswechsel, Änderung |
| `FlightSequencePayload` | S→C | Ziel, Dauer, Sequenztyp (Start, Landung, Finale) | Flugbeginn |
| `ResearchUnlockPayload` | C→S | Knoten-ID | Klick im Terminal |
| `AgencyActionPayload` | C→S | Aktion (gründen, einladen, beitreten, Rolle, Partner) + Argumente | GUI |
| `LaunchRequestPayload` | C→S | Rampen- bzw. Raketenposition, Ziel, Landeort | Klick auf „Start“ |
| `RocketActionPayload` | C→S | Aktion (tanken aus Frachtraum, zerlegen) | Raketen-GUI |
| `LogisticsEditPayload` | C→S | Route anlegen/ändern/löschen | Logistik-GUI |
| `MachineConfigPayload` | C→S | `pos`, `side` (0–5 in der Reihenfolge Unten, Oben, Nord, Süd, West, Ost; −1 = unverändert), `tank` (Index ab 0; −1 = unverändert); der Gas-Sammler-Modus kommt als weiteres Feld mit M6 | Maschinen-GUI |
| `NamePayload` | C→S | Name für Landefeuer | GUI |
| `TradingPostEditPayload` | C→S | Angebote | GUI |
| `ShipyardStartPayload` | C→S | Werft-Position | Klick auf „Start“ |

Kein Payload brauchen: die Geisterprojektion (rein clientseitig, 4.1) und der Port-Modus (Rechtsklick auf den Block, 4.3).

## 3A.6 Datenfluss

```
spec/data/*.json ──(tools/gen_resources.py, nur IDs aus common/implemented_ids.json; Texturen: tools/gen_placeholders.py)──▶ Generator
   ├─▶ Rezepte, Tags, Loot-Tables, Advancements, Worldgen, Dimensionen (Vanilla-Ordner, 26.3-Format)
   ├─▶ lang/de_de.json, lang/en_us.json
   └─▶ data/sternenwerft/sternenwerft/{planet,research,research_tier,rocket_part,star_system,balance}/…
Laufzeit: Reload-Listener ─▶ BalanceManager / Registries ─▶ SyncDataPayload ─▶ Client
```

- Generierte Dateien liegen in `common/src/generated/resources` und werden committet. Gebaut wird nur in GitHub Actions.
- Ändert sich `spec/data`, MUSS `python tools/gen_resources.py` erneut laufen. Der CI-Build prüft, dass die generierten Dateien aktuell sind.

## 3A.7 Tick-Modell und Leistung

- Maschinen ticken nur serverseitig und nur in geladenen Chunks; Client-Ticks nur für Animation.
- Netze berechnen ihre Topologie nur bei Blockänderungen neu und verteilen pro Tick ohne Objekt-Neuanlage.
- Umwelt, Kuppelzugehörigkeit und Schwerkraft je Spieler alle 10 t; Ereignisse alle 1.200 t je Dimension.
- Außenposten und Frachtflüge werden nur bei Bedarf nachgerechnet (20.1).
- Budget: Eine Basis mit 100 Maschinen darf im Mittel höchstens 1 ms pro Server-Tick kosten. Ein GameTest misst das ab Meilenstein 2 (Aufbau in 27.2).

## 3A.8 Tests

- **JUnit 5** in `common/src/test` für alle reinen Funktionen (`EnvironmentCalculator`, `FuelCalculator`, `RocketAssembler`, `ResearchGates`, `OfflineProduction`, `FpLedger`). Testdaten werden aus `spec/data` geladen, damit Tests und Spezifikation dieselben Zahlen nutzen.
- **GameTests** in `common` (Registrierung je Loader) für Verhalten in der Welt: Maschinen, Netze, Multiblöcke, Kuppeln, Raketenbau. Strukturvorlagen unter `data/sternenwerft/structure/gametest/`.
- Teststruktur: `data/sternenwerft/structure/gametest/empty.nbt` (leer, 8×8×8, von `gen_resources.py` erzeugt). Logik in `common` (`gametest/SwGameTests`), Registrierung je Loader: NeoForge `RegisterGameTestsEvent` + `TEST_FUNCTION`, Fabric `@GameTest` + Entry-Point `fabric-gametest`. Die CI wertet „All N required tests passed“ aus.
- Testnamen: `<System>_<Verhalten>_<Erwartung>`, z. B. `Crusher_RawIron_TwoDustWithin220Ticks`.

## 3A.9 Code-Regeln

- Java 25; `record` für unveränderliche Daten; kein statischer veränderlicher Zustand außer Registries.
- Übersetzungsschlüssel nach Vanilla-Muster (`block.sternenwerft.<id>`, `item.sternenwerft.<id>`, `gui.sternenwerft.<name>`); kein fest eingebauter Text.
- Logging über SLF4J mit Präfix `[Sternenwerft]`; keine Ausgaben in jedem Tick.
- Jede Annahme bei fehlender Vorgabe: Kommentar `// SPEC-GAP: <Beschreibung>` **und** Eintrag in `docs/spec-gaps.md`.

## 3A.10 Addon-Schnittstelle

- Ziel: Addons bauen eigene Maschinen, Generatoren, Kabel und Rezeptarten mit wenig Code und vielen Einstellmöglichkeiten.
- Paket `de.sternenwerft.api` ist stabil; öffentliche Typen für Addons liegen nur dort.
- Bausteine: `MachineBlock`, `MachineBlockEntity`, `ProcessingMachineBlockEntity` (mit `ProcessingRecipe.Kind`), `CombustionGeneratorBlockEntity`, `CableBlock`, `EnergyNode`.
- Einstellwerte stehen in Daten (25.7), nicht im Code.
- Addons erweitern die Basisklassen, statt sie zu kopieren.
- Details: `docs/dev/addon-api.md`.
