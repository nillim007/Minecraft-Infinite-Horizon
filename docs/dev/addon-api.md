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
| Verarbeitungsmaschine | `SternenwerftApi.newRecipeKind("pressing")` (Typ und Serializer selbst registrieren), BE-Typ mit `(pos, state) -> new ProcessingMachineBlockEntity(TYP, pos, state, () -> KIND)`, Block `new MachineBlock(props, () -> TYP)` | Eintrag in `machines.json` unter der Block-ID |
| Generator mit Item-Brennstoff | `CombustionGeneratorBlockEntity` (Vanilla-Brennwert) | Eintrag in `generators.json` |
| Eigene Maschine | von `MachineBlockEntity` erben: `slotCount()`, `configureEnergy(...)`, `serverTick(...)`, `getSlotsForFace`, `canTakeItemThroughFace` | beliebig über `SternenwerftApi.balance()` |
| Kabelsorte | `new CableBlock(properties)`. Gleiche Sorten bilden ein Netz. | `throughput` in `transport.json` |

Alle Werte liest der Code per Block-ID aus der Balance. Im Code steht keine Zahl.

## Energie

- **Eigene Blöcke ans Netz:** `de.sternenwerft.api.energy.EnergyNode` implementieren. Das sind die Methoden `energyStorage()`, `acceptsEnergyFrom(side)`, `providesEnergyTo(side)` und `onEnergyChanged()`; `platformEnergyHandle(...)` gibt den gespeicherten Wert zurück.
- **Fremde Mods:** Kabel und Generatoren erkennen auch Blöcke, die nur die Energie-API des Loaders anbieten. Auf NeoForge ist das `Capabilities.Energy.BLOCK`, auf Fabric Team Reborn Energy (`EnergyStorage.SIDED`).
- **Unsere Blöcke von außen:** Sie bieten ihre Energie ebenfalls über diese APIs an.
- **Netz abfragen:** `SternenwerftApi.energyGrid(level, pos)` liefert das Netz eines Kabels mit Endpunkten und Durchsatz.

## Regeln für die Schnittstelle (gelten für alle Meilensteine)

1. **Einstellwerte gehören in Daten:** Jeder neue Einstellwert kommt in eine Balance- oder Config-Datei, nicht in den Code.
2. **Basisklassen statt Kopien:** Jede neue Inhaltsart bekommt eine Basisklasse oder einen Baustein, den Addons ohne Kopieren nutzen können.
3. **Öffentliche Typen in `de.sternenwerft.api`:** Typen, die Addons brauchen, kommen in dieses Paket. Interne Klassen dürfen sich ändern.
