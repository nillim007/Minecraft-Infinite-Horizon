---
name: minecraft-mod-ci-screenshots
description: Sternenwerft-Mod (Minecraft 26.3, NeoForge + Fabric) bauen, testen und GUIs per Screenshot prüfen – lokal mit Fabric (falls JDK 25 da ist) oder über GitHub Actions (Server- und Client-GameTests, Screenshots im Branch ci-screenshots). Verwenden bei jedem Build, nach jeder Code-Änderung, bei neuen GUIs oder Blöcken und wenn der Build rot ist.
---

# Bauen, testen, Screenshots prüfen

Repo: `nillim007/Minecraft-New-Horizon`. Alles Weitere zur Mod: `CLAUDE.md`, `docs/spec/`, `docs/dev/`.

## 1. Lokal (schnell, zum Iterieren)

Nur wenn `java -version` ein JDK 25 zeigt. Nur das Fabric-Projekt bauen, das ist am schnellsten:

```bash
./gradlew :fabric:build
```

```bash
./gradlew :fabric:runGameTestServer
```

- Ohne JDK 25 lokal nicht bauen. Ein JDK nur mit Zustimmung des Auftraggebers herunterladen.
- Der lokale Build ersetzt nicht die CI: NeoForge und der Client-Test laufen nur auf GitHub.

## 2. GitHub Actions (vollständig, vor jedem Abschluss)

Workflow: `.github/workflows/build.yml` mit drei Jobs.

| Job | Inhalt |
| --- | --- |
| `spec` | `validate_spec`, `render_docs`, `gen_resources` aktuell? |
| `build` | `./gradlew build` (beide Loader + JUnit), `:neoforge:runGameTestServer`, `:fabric:runGameTestServer` |
| `client_test` | Fabric-Client-GameTest mit Software-Grafik (Mesa); legt die Screenshots im Branch `ci-screenshots` ab |

Ablauf:

1. Committen und pushen (Autor laut `CLAUDE.md`).
2. Im Hintergrund starten: `python tools/ci_status.py --wait`. Das Skript meldet GRÜN/ROT, Compiler-Fehler mit Datei:Zeile und die Anmerkungen „GameTest-Zusammenfassung“ und „Screenshots-Zusammenfassung“.
3. Ohne Admin-Token sind Job-Logs nicht lesbar (403). Fehler kommen nur über Anmerkungen; `.github/scripts/annotate_log.py` erzeugt sie aus den Logs. Höchstens 60 GitHub-Abfragen pro Stunde.
4. Ein GameTest-Lauf gilt nur als grün, wenn im Log „All N required tests passed“ steht. Das prüft `annotate_log.py --summary`.

## 3. GUI-Screenshots ansehen

```bash
git clone --depth 1 -b ci-screenshots https://github.com/nillim007/Minecraft-New-Horizon.git <scratchpad>/shots
```

- Die Commit-Nachricht nennt den geprüften Commit („Screenshots für <sha>“). Stimmt sie nicht mit dem eigenen Commit überein, ist der Client-Job noch nicht fertig oder fehlgeschlagen.
- Die PNGs mit dem Read-Werkzeug ansehen (854 × 480). `clientgametest.log` liegt daneben.
- Prüfpunkte:
  - Überlappungen (Leisten, Beschriftungen, Slots)
  - Slot-Positionen und Stapelzahlen
  - Fortschrittspfeil und Brennzeit
  - Statuslampe und Ausrichtung der Blöcke in der Welt (`world_machines.png`)
  - fehlende Texturen (lila-schwarz)
  - Chat-Text im Bild

### NeoForge-Client-Test

`neoforge/.../client/NeoForgeClientTest.java` ist eine Ablaufsteuerung pro Client-Tick (Run `runClientTest`, CI-Job „Client-Test mit Screenshots (NeoForge)“ mit xvfb + Mesa). Er schreibt `runs/clienttest/sternenwerft-clienttest.txt` (PASS/FAIL, DONE). Die Bilder landen im Branch `ci-screenshots-neoforge`. Neue GUIs mit `openMachine(block, name, setup, extra)` ergänzen. Lokal nicht starten, weil sich dafür ein Fenster öffnet.

## 4. Neue GUIs und Blöcke in den Client-Test aufnehmen

Datei: `fabric/src/main/java/de/sternenwerft/gametest/SwClientGameTests.java`. Sie ist nur mit `-Dfabric.client.gametest` aktiv, Entry-Point `fabric-client-gametest`.

- **Neue Maschine:** Aufruf `machine(context, sp, ModBlocks.X.get(), "gui_x", be -> { …Beispielinhalt… })` ergänzen.
  - Die Methode setzt den Block per `setblock`, legt über `runOnServer` Items und Energie hinein, öffnet das GUI mit `pressKey(keyUse)`, leert den Chat und macht den Screenshot.
- **Neue einfache Blöcke:** Sie erscheinen automatisch in der zweiten Reihe von `world_machines`, weil die Liste aus `ModBlocks.simpleBlocks()` kommt.
- **Neue Server-GameTests:** in `common/gametests.json` eintragen (Name, Klasse, Methode, Ticks, Spez.-Bezug), dann `python tools/gen_gametests.py`; das erzeugt `SwTestCatalog` (NeoForge registriert per Schleife) und `SwFabricGameTests`. Die CI prüft, dass beides aktuell ist. Mock-Spieler (`makeMockServerPlayer`) haben keine Netzwerkverbindung – Menüs öffnen nur im Client-Test.
  - Testlogik steht in `common/.../gametest/SwGameTests*.java` (statische Methoden mit `GameTestHelper`).

## 5. Bekannte Stolperstellen in 26.3

- **Chat:** `client.gui.hud.getChat()`; `client.gui.getChat()` gibt es nicht mehr.
- **Creative-Tab:** `CreativeModeTab.Output` ist in Vanilla nicht öffentlich. Den Tab je Loader bauen (`IRegistryHelper.creativeTab`).
- **Kupferblock:** `Blocks.COPPER_BLOCK` ist eine `WeatheringCopperCollection`. Für Eigenschaften `Blocks.IRON_BLOCK` + `SoundType.COPPER` nehmen.
- **Menüs:**
  - `MenuType` mit Zusatzdaten je Loader: NeoForge `IMenuTypeExtension.create`, Fabric `ExtendedMenuType` + `ExtendedMenuProvider`.
  - Fabric überträgt Menüwerte nur mit 16 Bit; größere Zahlen in 16-Bit-Teile zerlegen (`MachineBlockEntity.dataAccess`).
- **Screens:**
  - `extractBackground(GuiGraphicsExtractor, …)` überschreiben, `g.fill(…)` und `g.setComponentTooltipForNextFrame(…)`.
  - Registrierung je Loader: NeoForge `RegisterMenuScreensEvent` in der Klasse mit `@Mod(dist = Dist.CLIENT)`; Fabric `MenuScreens.register` im Client-Entry-Point.
- **Worldgen:** Features liegen unter `data/<ns>/worldgen/feature/` (nicht `configured_feature`), die Werte direkt im Objekt ohne `"config"`, Blockzustände als String (`"state": "ns:block"`). Placed Features sind unverändert. Fehlerbild sonst: „Unbound values in registry worldgen/feature“ beim Serverstart.
- **Fluide:** Details in `docs/dev/api-fluids-26.3.md`.
  - Fabric nutzt eigene `FlowingFluid`-Klassen (`SwFlowingFluid`). NeoForge braucht `FluidType` + `BaseFlowingFluid`.
  - Texturen kommen über `FluidModel.Unbaked`: NeoForge `RegisterFluidModelsEvent`, Fabric `FluidRenderingRegistry`.
  - Der `LiquidBlock`-Konstruktor ist auf Fabric `protected`, dort eine anonyme Unterklasse bilden.
- **Kleinkram 26.3:**
  - `IntProviders.CODEC` statt `IntProvider.CODEC`.
  - `Vec3.atCenterOf(pos)` statt `BlockPos#getCenter()`.
  - Feature-Typen: `Feature` ist ein Interface mit Record-Feldern, registriert in `BuiltInRegistries.FEATURE_TYPE`.
- **Block-Provider:** Typ-IDs ohne Endung: `minecraft:simple` statt `simple_state_provider`, ebenso `weighted`, `rule_based`, `noise` usw.
- **Loot-Tables:** Pool und Eintrag nutzen `"condition": {"type": …}` statt `"conditions"`, `"modifier": [{"type": …}]` statt `"functions"`. Behührung: `"condition": "minecraft:tool/can_silk_touch"`. Alte Schlüssel werden still ignoriert, Erze droppen dann sich selbst.
- **Vanilla-Daten nachschlagen:** Nach dem ersten lokalen Build liegt das Jar unter `~/.gradle/caches/neoformruntime/artifacts/minecraft_26.3_client.jar`. Darin stehen `data/minecraft/...` (Loot, Worldgen) und die Klassen; mit Python `zipfile` lesen.
- **Lokaler Build im Firmennetz:** Bei „PKIX path building failed“ prüft das Netz TLS mit eigenem Zertifikat. Dann `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` nur für den Befehl setzen. JDK: `C:\Users\NickM\.jdks\jdk-25.0.5+8` (`JAVA_HOME`).
- **GameTest-Server:** Er läuft ohne Tick-Begrenzung. 400 Spielticks dauern wenige Sekunden, das ist kein Fehler.
- **Shell (Windows):** Lange Python-Skripte nicht als Inline-Heredoc in Bash schicken, das bricht gelegentlich ab. Besser als Datei im Scratchpad schreiben und ausführen.
- **Ressourcen:** Generierte Ressourcen nie von Hand ändern. Dafür gibt es `python tools/gen_resources.py`. Platzhalter-Texturen nur mit `python tools/gen_placeholders.py --only <ids>` erzeugen, sonst entstehen Texturen für alle IDs.
