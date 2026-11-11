# Stand Meilenstein 2 (Erdindustrie)

Stand: 11.11.2026. Umfang und Teilschritte stehen in `docs/spec/27-meilensteine.md`, Abschnitte 27.1 bis 27.3. Den Lückentest gibt es in `docs/dev/lueckentest-m2.md`.

## Erledigt

- **Spezifikation:** Ä23 (Nachzug M1), Ä24 (33 Lücken aus dem Lückentest) und Ä25:
  - Erdöl-Quellblöcke werden nur mit 10 % Wahrscheinlichkeit verbraucht (`source_consume_chance`).
  - Drei Rezeptpaare sind getrennt.
  - Die Rakete T2 braucht 132 statt 128 `lightweight_plate`.
- **M2a:** GUI, Presse, Elektroofen, Upgrades, Metallkette.
  - `MachineMenu` und `MachineScreen` für alle Maschinen: Das GUI wird aus Rechtecken gezeichnet, Energie und Fortschritt werden in 16-Bit-Teilen synchronisiert. Geöffnet wird je Loader über `IRegistryHelper.openMenu`.
  - `MachineLayout`: Eingänge, Formslot, Ausgänge, Upgrades, Brennstoff. Die Slot-Regeln stehen in `MachineBlockEntity`.
  - `UpgradeMath` mit JUnit-Test (63 t / 101 J/t). Der Fortschritt ist ein Anteil.
  - `ProcessingRecipe` mit `MachineRecipeInput` (mehrere Eingänge, Pressform). Rezeptarten: `crushing`, `pressing`, `electric_smelting`. Der Elektroofen nutzt zusätzlich Vanilla-`smelting`.
  - Maschinen haben eine Vorderseite mit Statuslampe (gelb/grün/rot) und werden nach `facing` gedreht (`orientable`-Modelle).
- **M2b:** Erze, Quarzsand, Loot, Tags, Worldgen.
  - `bauxite_ore`, `titanium_ore`, `deepslate_titanium_ore` und `quartz_sand` sind einfache Blöcke aus `sternenwerft/simple_blocks.json` (Eigenschaften wie der genannte Vanilla-Block).
  - Loot-Tables und Block-Provider im 26.3-Format.
  - Worldgen: `worldgen/feature` + `placed_feature`, Biom-Tags `has_<name>`. NeoForge nutzt Biome-Modifier, Fabric `BiomeModifications` aus `biome_features.json`.
  - GameTest: Werkzeugstufen und Drop.
  - Die Ölblase ist nach M2c verschoben (spec-gaps #21).
- **M2c:** Fluide, Rohre, Tank, Pumpe, Füll- und Leerslots, Ölblase.
  - **Kern:** `SwFluidStack` und `SwFluidTank` (ein Fluid je Tank, mit Transaktionen). `FluidGrid` ohne Puffer, ein Fluid je Netz, gilt nach 20 t ohne Übertragung als leer. `ConduitBlock` ist die Basis für `CableBlock` und `FluidPipeBlock`.
  - **Loader:**
    - Fluid-Registrierung: Fabric über `SwFlowingFluid`, NeoForge über `FluidType` + `BaseFlowingFluid`. Texturen per `FluidModel`.
    - Fluid-Schnittstellen nach außen: `FluidStorage.SIDED` bzw. `Capabilities.Fluid.BLOCK`.
  - **Fluide:** Erdöl als Weltfluid mit Eimer, Kerosin als reines Tank-Fluid. Die Behälterliste `fluid_containers.json` steuert die Füll- und Leerslots.
  - **Blöcke:**
    - `fluid_tank`, `fluid_pump` (verbraucht Erdöl-Quellen nur zu 10 %), `copper_pipe`.
    - Die Ölblase ist der Feature-Typ `sternenwerft:fluid_sphere`.
  - **GUI und Netzwerk:**
    - Fluidleisten, Füll- und Leerslot, Knopf „Seite“.
    - `MachineConfigPayload` als erster C→S-Payload; der Server prüft den Abstand.
  - **Tests:**
    - GameTests: Pumpe → Rohr → Tank, Rohre mischen nicht, Eimer im Füllslot.
    - JUnit: Tankregeln.
  - **Annahmen:** #22 (Werte nur im Text) und #23 (Zeitgrenze der Pumpe inklusive Rohrdurchsatz: 50 t statt 44 t).
  - **Noch offen:**
    - Behälter-Brennstoffe (Lavaeimer im Generator, #15). Sie gehören zu M2e.
    - Tank-Inhalt beim Abbauen auf dem Item behalten (4.1).
    - Knopf „Tank“, sobald es Maschinen mit mehreren Tanks gibt (M2d).
- **Prüfung:**
  - GitHub: 6/6 GameTests auf beiden Loadern.
  - Client-Test mit Screenshots (alle GUIs und ein Weltfoto) bei jedem CI-Lauf, Branch `ci-screenshots`.
  - Lokal: Fabric-Build und Fabric-GameTests mit JDK 25 unter `C:\Users\NickM\.jdks\jdk-25.0.5+8`. Im Firmennetz `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` setzen.
  - Details zu allem: Skill `.claude/skills/minecraft-mod-ci-screenshots/SKILL.md`.

- **Testrunde M1–M2c:** 31 Server-GameTests auf beiden Loadern, 18 JUnit-Tests, Client-Tests auf Fabric **und NeoForge** (Knopf-Klick, Config-Sync, alle GUIs mit Screenshots), alles grün. Bericht: `docs/dev/testbericht-m2abc.md`.

## Noch manuell zu prüfen

- Bauxit kommt in der Savanne vor (27.3).
- GUIs im echten Client bedienen (Shift-Klick, Trichter).

## Nächster Schritt

M2d: Montagetisch, Raffinerie (Multiblock), Elektrolyseur, Ports (27.3). Die Annahmen stehen in `docs/spec-gaps.md` bis #23.
