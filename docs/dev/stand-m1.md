# Stand Meilenstein 1 (Grundgerüst)

Übergabe-Notiz für die nächste Sitzung. Stand: 10.11.2026.

## Fertig (alles auf `main`, GitHub-Build grün)

- MultiLoader-Gerüst 26.3 (Vorlage jaredlll08 Branch 26.3): `common`, `fabric`, `neoforge`; Paket `de.sternenwerft`, Mod-ID `sternenwerft`, MIT.
  Versionen in `gradle.properties`: NeoForge 26.3.0.102, Fabric API 0.166.0+26.3, Loader 0.19.5, Loom 1.18.1, ModDevGradle 2.0.147, NeoForm 26.3-1.
- GitHub Actions `.github/workflows/build.yml`: Job „Spezifikation prüfen“ (validate_spec, render_docs, gen_resources aktuell?) und „Mod bauen“ (`./gradlew build`, inkl. JUnit). Fehler werden per `.github/scripts/annotate_log.py` als Anmerkungen ausgegeben.
- `tools/ci_status.py --wait`: wartet auf den Build und zeigt Fehler (Datei:Zeile). Job-Logs sind ohne Admin-Token nicht abrufbar, nur Anmerkungen.
- Kernklassen (`common`): `core/transaction/Transaction`, `core/energy/SwEnergyStorage`, `data/BalanceManager` (Records MachineStats, GeneratorStats, UpgradeStats, BufferFormula), `machine/ProcessingMath`. JUnit-Tests gegen `spec/data` grün.
- Ressourcen: `tools/gen_resources.py` (aus `spec/data` für `common/implemented_ids.json`) → `common/src/generated/resources`; `tools/gen_placeholders.py`; UI-Texte in `common/lang_extra.json`. Registriert sind bisher nur Ressourcen für: crusher, combustion_generator, copper_cable (Blöcke), machine_frame, iron_dust (Items).
- API-Spickzettel: `docs/dev/api-fabric-26.3.md` (fertig), `docs/dev/api-neoforge-26.3.md` (Recherche lief noch).

## Offen für M1 (Reihenfolge)

1. NeoForge-Spickzettel lesen (falls fehlend: NeoForge-Branch `26.3.x` per sparse clone prüfen: Registrierung, `BlockEntityType`-Konstruktor per AT, Transfer-API Energie, `Capabilities.*.BLOCK`, RecipeSerializer/RecipeType, GameTest-Registrierung, `AddServerReloadListenersEvent`, `RegisterPayloadHandlersEvent`).
2. Plattform-Schnittstellen in `common/platform/services`: `IRegistryHelper` (register(Registry, name, Function<ResourceKey, R>) → Supplier; createBlockEntityType), `IEnergyHelper` (Energie an Nachbarn schieben/abfragen), `INetworkHelper`, Reload-Listener-Hook. NeoForge: DeferredRegister + Bus aus dem Mod-Konstruktor; Fabric: sofort `Registry.register`.
3. Blöcke/Block-Entities: `crusher` (WorldlyContainer: Eingang oben/seitlich, Ausgang alle Seiten nur Ausgaben; Upgrades), `combustion_generator` (Brennwert über `DataComponents.COOKING_FUEL`), `copper_cable` (6 Boolean-Properties, Netz per BFS, Push-Prinzip 4.4, 256 J/t). Blockstate `facing` + `status` (idle/running/error). Kein GUI in M1 (Hopper-Bedienung) → in `docs/spec-gaps.md` eintragen.
4. Rezepttyp `sternenwerft:crushing` (JSON-Format siehe `common/src/generated/resources/data/sternenwerft/recipe/crushing_iron_dust.json`).
5. Energie-Anbindung: NeoForge Transfer-API Energy-Capability; Fabric `teamreborn:energy:5.0.0` (`implementation` + `include`), Adapter auf `SwEnergyStorage` per SnapshotParticipant.
6. Config `<welt>/serverconfig/sternenwerft.json` (24.4) beim Serverstart laden; ein Payload `SyncConfigPayload` beim Join.
7. GameTests (Logik in `common`, dünne Hüllen je Loader): (a) Generator mit Kohle → Kabel → Zerkleinerer: 1 `minecraft:raw_iron` → 2 `iron_dust` in ≤ 220 t; (b) geänderter Balance-Wert ändert die Vorgangsdauer. CI-Schritte `:neoforge:runGameTestServer` und `:fabric:runGameTestServer` ergänzen (Fabric: `loom.runs.gameTestServer` mit `-Dfabric-api.gametest`).
8. Commit `M1: …`, README-Tabelle Meilenstein 1 auf „fertig“.

## Wichtige Fakten zu 26.3 (aus dem Fabric-Spickzettel)

`Identifier` statt ResourceLocation; `Properties#setId(ResourceKey)` Pflicht; `saveAdditional(ValueOutput)`/`loadAdditional(ValueInput)`; Screens `extractRenderState(GuiGraphicsExtractor, …)`; `BlockEntityType`-Konstruktor nicht öffentlich (Fabric: `FabricBlockEntityTypeBuilder`); Fabric-Payloads `PayloadTypeRegistry.clientboundPlay()`; Reload `DataResourceLoader.get().registerReloadListener(id, listener)`; Brennwert `DataComponents.COOKING_FUEL`.
