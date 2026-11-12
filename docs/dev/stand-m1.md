# Stand Meilenstein 1 (Grundgerüst)

Stand: 11.11.2026. **M1 ist umgesetzt**, und der GitHub-Build ist grün:

- JUnit-Tests laufen.
- `runGameTestServer` meldet auf NeoForge und Fabric „All 4 required tests passed“.

## Umgesetzt

- **Plattform-Schnittstellen** (`platform/services`):
  - `IRegistryHelper`: Registrierung, BlockEntityType, Creative-Tab
  - `IEnergyHelper`: NeoForge `Capabilities.Energy.BLOCK`, Fabric Team Reborn Energy 5.0.0 (per `include` eingebettet)
  - `INetworkHelper`: Payloads
- **Blöcke:**
  - `crusher` (`ProcessingMachineBlockEntity` mit `ProcessingRecipe.Kind` „crushing“)
  - `combustion_generator` (Vanilla-Brennwert über `COOKING_FUEL`)
  - `copper_cable` (`CableBlock`, Netz in `core/grid`: Push-Prinzip, gleichmäßige Verteilung, Durchsatz aus `transport.json`)
- **Daten:**
  - Balance-Reload aus allen Namespaces (`data/<ns>/infinite_horizon/balance`)
  - Serverconfig aus `misc.json → config_defaults`
  - `SyncConfigPayload` beim Join
- **GameTests** (`gametest/IhGameTests`, Hüllen je Loader):
  - Kette Generator → Kabel → Zerkleinerer ≤ 220 t
  - Balance-Änderung ändert die Dauer
  - Plattform-Verbraucher bekommt Strom
- **Addon-Schnittstelle** `de.infinitehorizon.api`, beschrieben in `docs/dev/addon-api.md`.
- **Annahmen:** `docs/spec-gaps.md` #8–#16.

## Nicht automatisch geprüft

- **Client-Start** beider Loader: Kapitel 27 verlangt ihn, die CI kann ihn ohne Grafik nicht prüfen. Manuell prüfen mit `./gradlew :neoforge:runClient` bzw. `:fabric:runClient`.
- **Zahl der GameTests:** Der Server meldet 4 statt der 3 eigenen Tests. Welche das sind, zeigt die Anmerkung „GameTest-Zusammenfassung“.

## Nächster Meilenstein

M2 (Erdindustrie): Kapitel und Daten laut Matrix in `docs/spec/README.md`. Dazu gehören:

- **GUI** der Maschinen (nachzuholen, spec-gaps #10)
- **Upgrades** (spec-gaps #14)
