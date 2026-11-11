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
- `./gradlew :neoforge:runGameTestServer` geht lokal auch (etwa 1 min) und lohnt sich vor dem Push.
- Der lokale Build ersetzt nicht die CI: Die Client-Tests mit Screenshots laufen nur auf GitHub.

## 2. GitHub Actions (vollständig, vor jedem Abschluss)

Workflow: `.github/workflows/build.yml` mit vier Jobs.

| Job | Inhalt |
| --- | --- |
| `spec` | `validate_spec`, `render_docs`, `gen_resources` und `gen_gametests` aktuell? |
| `build` | `./gradlew build` (beide Loader + JUnit), `:neoforge:runGameTestServer`, `:fabric:runGameTestServer` |
| `client_test` | Fabric-Client-GameTest mit Software-Grafik (Mesa); legt die Screenshots im Branch `ci-screenshots` ab |
| `neoforge_client_test` | NeoForge-Client mit xvfb + Mesa (`NeoForgeClientTest`); Screenshots im Branch `ci-screenshots-neoforge` |

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
  - Optionale Felder `structure` (z. B. `sternenwerft:gametest/empty_large`, 24 × 6 × 24, aus `gen_resources`) und `environment`. Ein Test mit eigener Umgebung läuft allein in einem eigenen Durchgang. Fabric braucht die Umgebung als JSON unter `fabric/src/main/resources/data/<ns>/test_environment/`; NeoForge registriert sie per `registerEnvironment`.
  - **Leistung messen:** `getServer().getAverageTickTimeNanos()` (Server-eigene Messung, Mittel der letzten 100 Ticks). Abstände zwischen `onEachTick`-Aufrufen enthalten alle anderen Tests und den Testrahmen.
- **JEI/REI:** Nur `compileOnly` über `build-logic` (die Loader-Projekte übersetzen den gemeinsamen Code mit, daher dort und nicht in `common/build.gradle`). JEI läuft zur Laufzeit nur mit `-PwithJei` (CI-Job des NeoForge-Client-Tests). ModDevGradle kennt in 26.3 keinen Zusatz-Classpath je Run mehr. JEI braucht die Gruppe `net.mezzdev` aus maven.blamejared.com.
- **Rezepte auf dem Client:** Seit 1.21.2 schickt der Server nur ausgewählte Rezepte. Eigene Arten über `RecipeKinds.register` (NeoForge `OnDatapackSyncEvent#sendRecipes`, Fabric `RecipeSynchronization`); lesen über `ClientRecipes`.
- **Wiki-Pflicht:** Neue Blöcke, Items oder Fluide müssen auf einer Handbuchseite stehen (`docs/handbook/pages`). Nach jedem Teilschritt `python tools/gen_wiki.py`, sonst wird der Job `spec` rot (Details in `CLAUDE.md`).

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
- **Screens:** `imageWidth`/`imageHeight` sind in 26.3 `final`. Höhere GUIs über den Konstruktor `super(menu, inv, title, breite, höhe)`.
- **Rezepte ohne Items:** `RecipeInput#isEmpty()` ist standardmäßig „alle Slots leer“, dann findet der Rezeptmanager nichts. `MachineRecipeInput` zählt deshalb die Eingangstanks mit.
- **Leere Hand im Client-Test:** Abgebaute Maschinen werfen ihren Inhalt aus, der Spieler sammelt ihn auf. Vor Prüfungen mit leerer Hand (Port umschalten, Geisterprojektion) `clear @a` bzw. `getInventory().clearContent()`.
- **Sneak im Client-Test:** Fabric `getInput().holdKey(o -> o.keyShift)`, NeoForge-Test `mc.options.keyShift.setDown(true)` und ein paar Ticks warten.
- **Multiblock im Test:** Aufbau mit `fill … sternenwerft:refinery_casing`, dann den Controller per `setblock …[facing=west]` mit Blick zum Spieler setzen, sonst trifft der Rechtsklick ein Gehäuse.
- **Ressourcen:** Generierte Ressourcen nie von Hand ändern. Dafür gibt es `python tools/gen_resources.py`. Platzhalter-Texturen nur mit `python tools/gen_placeholders.py --only <ids>` erzeugen, sonst entstehen Texturen für alle IDs.
