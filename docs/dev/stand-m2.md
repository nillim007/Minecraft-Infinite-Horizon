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
  - `bauxite_ore`, `titanium_ore`, `deepslate_titanium_ore` und `quartz_sand` sind einfache Blöcke aus `infinite_horizon/simple_blocks.json` (Eigenschaften wie der genannte Vanilla-Block).
  - Loot-Tables und Block-Provider im 26.3-Format.
  - Worldgen: `worldgen/feature` + `placed_feature`, Biom-Tags `has_<name>`. NeoForge nutzt Biome-Modifier, Fabric `BiomeModifications` aus `biome_features.json`.
  - GameTest: Werkzeugstufen und Drop.
  - Die Ölblase ist nach M2c verschoben (spec-gaps #21).
- **M2c:** Fluide, Rohre, Tank, Pumpe, Füll- und Leerslots, Ölblase.
  - **Kern:** `IhFluidStack` und `IhFluidTank` (ein Fluid je Tank, mit Transaktionen). `FluidGrid` ohne Puffer, ein Fluid je Netz, gilt nach 20 t ohne Übertragung als leer. `ConduitBlock` ist die Basis für `CableBlock` und `FluidPipeBlock`.
  - **Loader:**
    - Fluid-Registrierung: Fabric über `IhFlowingFluid`, NeoForge über `FluidType` + `BaseFlowingFluid`. Texturen per `FluidModel`.
    - Fluid-Schnittstellen nach außen: `FluidStorage.SIDED` bzw. `Capabilities.Fluid.BLOCK`.
  - **Fluide:** Erdöl als Weltfluid mit Eimer, Kerosin als reines Tank-Fluid. Die Behälterliste `fluid_containers.json` steuert die Füll- und Leerslots.
  - **Blöcke:**
    - `fluid_tank`, `fluid_pump` (verbraucht Erdöl-Quellen nur zu 10 %), `copper_pipe`.
    - Die Ölblase ist der Feature-Typ `infinite_horizon:fluid_sphere`.
  - **GUI und Netzwerk:**
    - Fluidleisten, Füll- und Leerslot, Knopf „Seite“.
    - `MachineConfigPayload` als erster C→S-Payload; der Server prüft den Abstand.
  - **Tests:**
    - GameTests: Pumpe → Rohr → Tank, Rohre mischen nicht, Eimer im Füllslot.
    - JUnit: Tankregeln.
  - **Annahmen:** #22 (Werte nur im Text) und #23 (Zeitgrenze der Pumpe inklusive Rohrdurchsatz: 50 t statt 44 t).
  - **Nachgezogen:** Lavaeimer im Generator, Tank-Inhalt auf dem Item (Testrunde) und Knopf „Tank“ (M2d).
- **Prüfung:**
  - GitHub: 6/6 GameTests auf beiden Loadern.
  - Client-Test mit Screenshots (alle GUIs und ein Weltfoto) bei jedem CI-Lauf, Branch `ci-screenshots`.
  - Lokal: Fabric-Build und Fabric-GameTests mit JDK 25 unter `C:\Users\NickM\.jdks\jdk-25.0.5+8`. Im Firmennetz `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` setzen.
  - Details zu allem: Skill `.claude/skills/minecraft-mod-ci-screenshots/SKILL.md`.

- **M2d:** Montagetisch, Elektrolyseur, Raffinerie (Multiblock), Ports.
  - **Rezepte:** `ProcessingRecipe` kennt eine Fluid-Zutat und Fluid-Ergebnisse. Neue Rezeptarten `refining`, `electrolysis` und `assembly`; `assembly` nutzt den exakten Abgleich (22.3, reine Funktion `ExactMatch`).
  - **Maschinen:** `ProcessingMachineBlockEntity` legt Tanks aus `fluid_tanks` an. Eingangstanks nehmen nur Fluide aus den eigenen Rezepten. Ergebnisse gehen in den Ausgangstank mit demselben Fluid, sonst in den ersten leeren; ist keiner frei, steht die Lampe auf `error`.
  - **Multiblock** (Paket `multiblock`):
    - `MultiblockPattern` liest das Muster aus machines.json (auch `hollow_shell` für spätere Reaktoren).
    - `MultiblockControllerBlockEntity` prüft beim Setzen, bei Meldungen aus `MultiblockRegistry` und alle 100 t. Bei Erfolg verknüpft sie die Ports.
    - `PortBlock`/`PortBlockEntity` mit Modus `input`/`output` (Rechtsklick mit leerer Hand).
    - `GhostPreview`: Sneak-Rechtsklick zeigt Partikel an fehlenden Plätzen.
  - **GUI:** `MachineGeometry` ordnet bis zu 3 Tanks, das 3 × 3-Raster des Montagetischs und die Knöpfe „Tank“ und „Seite“ an. Statuslampe links oben; bei Multiblöcken nennt der Tooltip den fehlenden Block.
  - **Fluide:** `hydrogen` und `oxygen` als Gase; der Tank nimmt nur Flüssigkeiten (`accepts` in storage.json).
  - **Korrektur:** Maschinen, Ports, Gehäuse, Kabel und Rohre brauchen jetzt mindestens eine Steinspitzhacke (`needs_stone_tool`, 4.1).
  - **Annahmen:** spec-gaps #24 bis #29.

- **M2e:** Treibstoffgenerator, Akku, Leistung.
  - `FluidFuelGeneratorBlockEntity`: Kerosin 1 mB je 4 t ergibt 80 J/t, Füll- und Leerslot. Das GUI zeigt die Brennzeit (Flag `generator` in `OpenData`).
  - `BatteryBlockEntity`:
    - Ein- und Ausgang zusammen höchstens 512 J/t je Tick (`IhEnergyStorage` mit Tick-Grenze); Abgabe nur über die Ausgangsseite.
    - Ladeslot für Items mit Loader-Item-Energie oder `infinite_horizon:energy`.
    - Die Energie bleibt beim Abbauen am Item.
  - **Loader:** Energie-Adapter je Seite; Item-Energie (NeoForge `Capabilities.Energy.ITEM`, Fabric `EnergyStorage.ITEM`). Test-Item im GameTest-Betrieb ist `minecraft:redstone_block`.
  - **Leistung:**
    - `BalanceManager` speichert die typisierten Werte zwischen, Maschinen richten sich nur nach `/reload` neu ein.
    - Das letzte passende Rezept wird zuerst geprüft; `setChanged` meldet keine Komparatoren mehr.
    - Leistungstest (100 Zerkleinerer, eigene Testumgebung): lokal 0,84 ms (Fabric) bzw. 0,50 ms (NeoForge) je Tick.
  - `gen_placeholders.py` überschreibt vorhandene Texturen nur noch mit `--force`, damit fertige Grafiken sicher sind.
  - **Annahmen:** spec-gaps #30 bis #32.

- **M2f:** Rezeptanzeige, Handbuch, Abschluss.
  - **Mengen-Multiplikator** (`AmountMultiplier`, reine Funktion) im exakten Abgleich des Montagetischs und in allen Anzeigen.
  - **Rezeptanzeige:**
    - Der Server schickt die eigenen Rezeptarten zum Client (`ClientRecipes`). `RecipeView` bereitet sie für JEI und REI gleich auf.
    - Plugins für JEI (31.17.0.109) und REI (26.3.824), nur `compileOnly`. EMI gibt es für 26.3 nicht.
    - Addons zeigen eigene Arten mit `InfiniteHorizonApi.showInRecipeViewers(kind)`.
  - **Handbuch:**
    - `HandbookMarkup` (reine Funktion) und `HandbookData` (Seiten aller Namespaces).
    - `HandbookScreen` mit Inhaltsverzeichnis, Suche, Links, Item-Links, Rezepten, Steckbriefen, Tabellen, Hinweiskästen, Zurück und Lesezeichen.
    - Taste `H` über Items in Inventaren, Tooltip-Hinweis, Geschenk beim ersten Join.
  - **Items:** `engineers_handbook` und `planetary_atlas` registriert; einfache Items übernehmen `max_stack` aus items.json (Kerosin-Eimer stapelt bis 1).
  - **Tests:** Join-Geschenk, die drei noch fehlenden Beispielrezepte aus 27.2, JUnit für Kategorien, Multiplikator und Markup. Client-Tests öffnen das Handbuch und folgen einem Link; der NeoForge-Client-Test lädt JEI (`-PwithJei`) und fotografiert die Kategorien.
  - **Annahmen:** spec-gaps #33 bis #37.

- **Testrunde M1–M2c:** 31 Server-GameTests auf beiden Loadern, 18 JUnit-Tests, Client-Tests auf Fabric **und NeoForge** (Knopf-Klick, Config-Sync, alle GUIs mit Screenshots), alles grün. Bericht: `docs/dev/testbericht-m2abc.md`.

## Noch manuell zu prüfen (Abnahme M2)

Alles andere prüfen die GameTests und Client-Tests bei jedem Push (`docs/dev/testbericht-m2def.md`).

- Bauxit kommt in der Savanne vor: mindestens 6 Adern in 25 Chunks nach `/locate biome minecraft:savanna` (27.3 M2b).
- GUIs mit Maus bedienen: Ziehen und Ablegen, Shift-Klick, Trichter an Item-Ports.
- Taste `H` per echtem Tastendruck im Inventar, in JEI und in REI; beim Tippen in Suchfeldern darf das Handbuch nicht aufgehen.
- Geisterprojektion über die volle Dauer ansehen (200 t).

## Nächster Schritt

Meilenstein 3 (Erste Welt): Planeten-Registry, Mond, Schwerkraft, Umweltsystem, HUD, Anzug T1, Sauerstoffgenerator, Kuppel T1, Kanister in Füll- und Leerslots, Agentur-Kern (27). Die Annahmen stehen in `docs/spec-gaps.md` bis #37.
