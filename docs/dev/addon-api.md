# Addon-Schnittstelle

Ziel: Andere Mods sollen Sternenwerft mit wenig Code erweitern können, und vieles soll sich ganz ohne Code über Daten einstellen lassen.

Einstieg im Code ist `de.sternenwerft.api.SternenwerftApi`. Was im Paket `de.sternenwerft.api` liegt, bleibt zwischen kleinen Versionen gleich.

## Ohne Code: Werte über Daten einstellen

Balance-Werte stehen unter `data/<namespace>/sternenwerft/balance/<datei>.json`, im gleichen Format wie `spec/data/balance` (siehe `spec/data/SCHEMA.md`).

- **Reihenfolge beim Laden:** Zuerst kommt der Namespace `sternenwerft`, danach alle anderen alphabetisch.
- **Zusammenführen:** Gleichnamige Dateien werden Eintrag für Eintrag zusammengeführt. Ein Addon fügt so neue Einträge hinzu oder überschreibt einzelne Werte von uns.
- **Ganz ersetzen:** Steht `"replace": true` im Dokument, ersetzt es die Datei vollständig.
- **Neu laden:** Nach `/reload` gelten die neuen Werte. Maschinen passen Slots und Puffer selbst an.

```json
// data/meinaddon/sternenwerft/balance/machines.json
{ "entries": {
    "sternenwerft:crusher": { "energy_per_tick": 20, "ticks": 100, "buffer": null,
                              "item_slots": { "input": 1, "output": 4 }, "upgrade_slots": 2 },
    "meinaddon:mill":       { "energy_per_tick": 30, "ticks": 160, "buffer": null,
                              "item_slots": { "input": 1, "output": 2 }, "upgrade_slots": 2 }
} }
```

**Rezepte** für unsere Maschinen sind normale Datapack-Rezepte, z. B. `"type": "sternenwerft:crushing"` (Format 25.4). Fehlen `ticks` oder `energy_per_tick`, gelten die Werte der Maschine.

**Serverkonfiguration:** `<welt>/serverconfig/sternenwerft.json`. Schlüssel, Standardwerte und Grenzen kommen aus `misc.json → config_defaults`.

## Mit wenig Code: eigene Inhalte

| Was | Bausteine | Daten |
| --- | --- | --- |
| Verarbeitungsmaschine | `SternenwerftApi.newRecipeKind("pressing")` (Typ und Serializer selbst registrieren), BE-Typ mit `(pos, state) -> new ProcessingMachineBlockEntity(TYP, pos, state, () -> KIND)`, Block `new MachineBlock(props, () -> TYP)` | Eintrag in `machines.json` unter der Block-ID; Tanks über `fluid_tanks` (`role` input/output), Füll-/Leerslots über `container_slots` |
| Rezepte mit Fluiden | dieselbe `ProcessingRecipe.Kind`; im Rezept `"fluid": {"fluid", "amount"}` und `"fluid_results": [...]` | Eingangstanks nehmen nur Fluide aus den Rezepten an |
| Rezeptart mit exaktem Abgleich | `SternenwerftApi.newExactRecipeKind("meinaddon:art")` wie der Montagetisch (22.3) | – |
| Rezepte in JEI/REI | `SternenwerftApi.showInRecipeViewers(kind)` beim Mod-Start: eigene Kategorie, Rezepte gehen zum Client | Titel `jei.<namespace>.<art>` in den Übersetzungen; Katalysatoren = Maschinen mit der Art in `recipe_types` |
| Handbuchseiten | Dateien nach Schema 25.6 unter `assets/<namespace>/handbook/<kapitel>/` (eigene Kapitel mit `_chapter.json`) | Seiten nennen ihre Items unter `items`; Taste `H` findet sie dann |
| Multiblock | Controller-BE `new MultiblockControllerBlockEntity(TYP, pos, state, () -> List.of(KIND))`, Gehäuse `new MultiblockPartBlock(props)`, Ports `new PortBlock(props, PortType, () -> PORT_TYP)` | Muster-Eintrag in `machines.json` (`size`, `shape` solid/hollow_shell, `controller_position`, `casing`, `casing_replaceable_by`) und beim Controller `"multiblock": "<eintrag>"` |
| Generator mit Item-Brennstoff | `CombustionGeneratorBlockEntity` (Vanilla-Brennwert) | Eintrag in `generators.json` |
| Generator mit Fluid-Brennstoff | `FluidFuelGeneratorBlockEntity` | `fuel: [{"fluid", "amount", "per_ticks"}]` und `fluid_tanks` in `generators.json` |
| Energiespeicher | `BatteryBlockEntity` (Ausgangsseite, Ladeslot, Tick-Grenze) | `capacity`, `io_per_tick`, `charge_slots` in `storage.json` |
| Eigene Maschine | von `MachineBlockEntity` erben: `slotCount()`, `configureEnergy(...)`, `serverTick(...)`, `getSlotsForFace`, `canTakeItemThroughFace` | beliebig über `SternenwerftApi.balance()` |
| Kabelsorte | `new CableBlock(properties)`. Gleiche Sorten bilden ein Netz. | `throughput` in `transport.json` |

Alle Werte liest der Code per Block-ID aus der Balance. Im Code steht keine Zahl.

## Energie

- **Eigene Blöcke ans Netz:** `de.sternenwerft.api.energy.EnergyNode` implementieren. Das sind die Methoden `energyStorage()`, `acceptsEnergyFrom(side)`, `providesEnergyTo(side)` und `onEnergyChanged()`; `platformEnergyHandle(...)` gibt den gespeicherten Wert zurück.
- **Fremde Mods:** Kabel und Generatoren erkennen auch Blöcke, die nur die Energie-API des Loaders anbieten. Auf NeoForge ist das `Capabilities.Energy.BLOCK`, auf Fabric Team Reborn Energy (`EnergyStorage.SIDED`).
- **Unsere Blöcke von außen:** Sie bieten ihre Energie ebenfalls über diese APIs an.
- **Netz abfragen:** `SternenwerftApi.energyGrid(level, pos)` liefert das Netz eines Kabels mit Endpunkten und Durchsatz.
- **Seiten:** Die Loader-APIs bekommen je Seite einen eigenen Adapter. `acceptsEnergyFrom(side)` und `providesEnergyTo(side)` gelten deshalb auch für fremde Mods (ein Akku nimmt an seiner Ausgangsseite nichts an).
- **Grenze je Tick:** `SwEnergyStorage#setPerTickLimit(true)` macht aus `maxInsert`/`maxExtract` eine Grenze je Tick; der Block ruft zu Beginn seines Ticks `newTick()` auf.
- **Items laden:** Ladeslots laden Items mit der Item-Energie-API des Loaders oder mit der Komponente `sternenwerft:energy` (Kapazität aus `storage.json` unter der Item-ID).

## Leistung

- Die typisierten Zugriffe von `BalanceManager` (`machine`, `tanks`, `generator` …) sind zwischengespeichert. Maschinen richten Slots und Puffer nur nach `/reload` neu ein.
- `MachineBlockEntity#setChanged()` markiert nur den Chunk und meldet keine Komparatoren. Eigene Blöcke mit Komparator-Signal überschreiben die Methode.

## Regeln für die Schnittstelle (gelten für alle Meilensteine)

1. **Einstellwerte gehören in Daten:** Jeder neue Einstellwert kommt in eine Balance- oder Config-Datei, nicht in den Code.
2. **Basisklassen statt Kopien:** Jede neue Inhaltsart bekommt eine Basisklasse oder einen Baustein, den Addons ohne Kopieren nutzen können.
3. **Öffentliche Typen in `de.sternenwerft.api`:** Typen, die Addons brauchen, kommen in dieses Paket. Interne Klassen dürfen sich ändern.
