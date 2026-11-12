# 3. Technischer Rahmen

## 3.1 Versionen (MUSS)

| Komponente | Version |
| --- | --- |
| Minecraft | **26.3** (Java Edition, Versionsschema Jahr.Drop seit 2026) |
| Java | **25** |
| Gradle | 9.6.0 |
| NeoForge | neueste stabile **26.3.0.x** (vierteilige Versionsnummer, keine `-beta`-Builds) |
| NeoForge-Buildsystem | ModDevGradle 2.x |
| Fabric Loader | **0.19.5** oder neuer (stabil) |
| Fabric Loom | **1.17**, Plugin-ID `net.fabricmc.fabric-loom` (remappt nicht mehr) |
| Fabric API | neueste mit Suffix `+26.3` |
| Mappings | **keine.** Ab 26.1 ist Minecraft nicht mehr obfuskiert und enthält Mojangs Namen inklusive Parameternamen. Kein Yarn, kein Parchment, kein Intermediary. |
| Vorlage | `jaredlll08/MultiLoader-Template`, neuester 26.x-Branch. Versionen in `gradle.properties` auf die Werte dieser Tabelle setzen. Ab NeoForge 26.3 sind `modLoader` und `loaderVersion` in `neoforge.mods.toml` optional. |
| Team Reborn Energy (Fabric) | neueste Version für 26.x (verlangt Minecraft ≥ 26.1, Java 25, `fabric-transfer-api-v1` ≥ 5.1.0), **jar-in-jar eingebettet** (`include`), damit Spieler nichts extra installieren |
| JEI / REI / EMI | optionale Abhängigkeiten. Plugins nur laden, wenn die jeweilige Mod für 26.3 vorhanden ist. Fehlt eine davon für 26.3, Plugin weglassen und mit `// SPEC-GAP` markieren. |

Begründung: 26.3 ist die neueste stabile Version und wird von NeoForge und Fabric stabil unterstützt. 26.4 ist nur als Snapshot verfügbar und wird **nicht** verwendet.

**Was sich gegenüber 1.21.x geändert hat und hier beachtet werden MUSS:**

- Attribute haben kein `generic.`-Präfix mehr: `minecraft:gravity`, `minecraft:safe_fall_distance`, `minecraft:fall_damage_multiplier`, `minecraft:step_height`, `minecraft:jump_strength`.
- Worldgen (26.3): „Surface Rules“ heißen jetzt **Material Rules** (`worldgen/material_rule`, eigene Bedingungen unter `worldgen/material_condition`). Configured Features liegen unter `worldgen/feature`, `Feature` und `FeatureConfiguration` sind zu einem `Feature`-Interface zusammengelegt, Codecs registriert man in `BuiltInRegistries.FEATURE_TYPE`. Number Provider sind aufgeteilt in `context_float_provider` und `context_int_provider`. Block-State-Provider sind registrierbar (`worldgen/block_state_provider`).
- Brennwert, Kompostierbarkeit und Braubarkeit sind Item-Komponenten (`DataComponents.COOKING_FUEL`, `COMPOSTABLE`, `BREWING_FUEL`). Fabrics `FuelRegistry` und `CompostingChanceRegistry` gibt es nicht mehr.
- Rezepte und Advancements sind neu ladbare Registries.
- Eingabe läuft über SDL statt GLFW. Tasten nie hart codieren, sondern `InputConstants` bzw. `KeyMapping` verwenden. Eigene Texteingabefelder melden den Fokus an `textInputManager()`.

**Regel für alles Versionsabhängige:** Dieses Dokument nennt Datenfelder und Klassen so, wie sie zuletzt sicher bekannt waren (z. B. `dimension_type`-Felder wie `fixed_time`, `bed_works`, `has_skylight` oder die Sky-Renderer-Hooks der Loader). Die KI MUSS jeweils das **26.3-Format** verwenden: Vanilla-Datenpaketdateien aus dem 26.3-Client-JAR als Vorlage nehmen (`data/minecraft/dimension_type/overworld.json`, `worldgen/noise_settings/overworld.json` usw.) und Plattform-APIs gegen die Javadocs von NeoForge 26.3 und Fabric API `+26.3` prüfen. Gibt es ein genanntes Feld oder eine Klasse in 26.3 nicht mehr, wird das gleichwertige Mittel genutzt und mit `// SPEC-GAP: 26.3` markiert. Für Himmel und Nebel gilt: Plattform-Hook für Dimensions-Effekte verwenden. Gibt es keinen, Mixin in den Sky-Pass des Level-Renderers.

**Dimensionen, Biome und Himmel (ab M3):** Felder, die 13.1 nicht nennt, haben die Werte von `minecraft:overworld` aus 26.3. Führt 26.3 einen Wert als Umgebungsattribut (environment attribute) statt als Feld, gilt der Wert dort. Zu Beginn von M3 entsteht der Spickzettel `docs/dev/api-dimensions-26.3.md` (wie `api-fluids-26.3.md`, mit Belegen aus dem 26.3-JAR, NeoForge und Fabric API): Dimensions- und Biomformat, Umgebungsattribute, Himmel, HUD-Ebenen, `SavedData`, Spieler-Attachments und Attribut-Modifikatoren.

**Entities, Sitze und Menüs mit Zusatzdaten (ab M4):** Zu Beginn von M4 entsteht der Spickzettel `docs/dev/api-entities-26.3.md` (wie `api-dimensions-26.3.md`, mit Belegen aus dem 26.3-JAR, NeoForge und Fabric API): Registrierung von `EntityType` mit `ResourceKey`, Renderer mit Render-State, Blockmodelle im Entity, Sitze (`positionRider`), Dimensionswechsel mit Passagieren (`TeleportTransition`), Kamera und Bildschirmbeben, eigene Erfolgs-Auslöser, Menüs mit Entity- bzw. Zusatzdaten (Fabric `ExtendedMenuType`, NeoForge `openMenu` mit Puffer), Chunk-Attachments mit Sync und ein Strahl wie beim Leuchtfeuer.

## 3.2 Projektaufbau

```
infinite_horizon/
  common/     – gesamte Spiellogik, generierte Ressourcen, Netzwerk-Payloads, Services-Interfaces
  neoforge/   – Registrierung, Capabilities, Events, Sky-Renderer, Config-Pfad
  fabric/     – Registrierung, Team Reborn Energy, Fabric Transfer API, Sky-Renderer
  tools/      – gen_placeholders.py, balance_sheet.py (Kapitel 27)
```

- Plattformunterschiede laufen über `ServiceLoader`-Interfaces in `common` (`IPlatformHelper`, `IEnergyHelper`, `IFluidHelper`, `INetworkHelper`, `IRegistryHelper`).
- Registrierung über ein gemeinsames `RegistrationProvider`-Muster (Supplier-basiert), in beiden Loadern implementiert.
- Item-Daten über **Data Components**, nicht über NBT. Brennwert (`combustion_generator`) liest die Vanilla-Komponente `COOKING_FUEL`. Kompostierbare Mod-Items bekommen `COMPOSTABLE`.

## 3.3 Energie und Fluide auf den Plattformen

| Thema | common | NeoForge | Fabric |
| --- | --- | --- | --- |
| Energie | eigene Klasse `IhEnergyStorage` (long, J) | `Capabilities.Energy.BLOCK` (`EnergyHandler`, Transfer-API mit Transaktionen) | `team.reborn.energy.api.EnergyStorage.SIDED` (Team Reborn Energy 5.0.0, per `include` eingebettet) |
| Fluide | eigene Klasse `IhFluidTank` (mB) | `Capabilities.Fluid.BLOCK` mit `ResourceHandler<FluidResource>` | `FluidStorage.SIDED` (Droplets ↔ mB umrechnen) |
| Items | eigene Klasse `IhItemContainer` | `Capabilities.Item.BLOCK` mit `ResourceHandler<ItemResource>` | `ItemStorage.SIDED` |

- **NeoForge-Transfer-API** (seit 21.9, gilt für 26.x): `IItemHandler`, `IFluidHandler` und `FluidTank` gibt es nicht mehr. Ein- und Auslagern läuft über `ResourceHandler` in **Transaktionen**. Abgebrochene Transaktionen MÜSSEN den vorherigen Zustand wiederherstellen (Snapshot). Leere Ressourcen nie übergeben, sie werfen eine Exception.
- **Fabric-Transfer-API** arbeitet ebenfalls mit Transaktionen. Die Klassen in `common` MÜSSEN daher ein gemeinsames Transaktionsmodell anbieten (simulieren → bestätigen/zurückrollen), das beide Plattformschichten abbilden.
- Maschinen, die auch Vanilla-Trichter bedienen sollen, implementieren zusätzlich `Container` (Trichter prüfen das Interface vor der Capability).
- Die Präfix-Namen `Sw…` vermeiden Namenskonflikte mit Plattformklassen.

Gase sind Fluide mit dem Tag `infinite_horizon:gases`. Gase haben keine platzierbaren Quellblöcke und keine Eimer.

## 3.4 Netzwerk

`CustomPacketPayload` mit `StreamCodec`, registriert in beiden Loadern über `INetworkHelper`. Payloads: Forschungsstand, Atlas, Agentur, Kuppeln je Dimension, Umweltwerte für das HUD, Raketen-GUI-Aktionen, Logistik-GUI, Konfiguration.

## 3.5 Konfiguration

- Eigene JSON-Datei pro Welt: `<welt>/serverconfig/infinite_horizon.json`. Sie wird beim ersten Start mit Standardwerten angelegt und beim Join an Clients gesendet.
- Client-Datei `config/infinite_horizon-client.json` nur für HUD-Position und Lautstärke: `hud_offset_x` und `hud_offset_y` (Verschiebung des HUD in px, Standard 0) sowie `sound_volume` (0,0–1,0, Standard 1,0, gilt für die Warntöne).
- Schlüssel: Kapitel 24.4.

## 3.6 Code-Konstanten

`TICKS_PER_SECOND = 20`, `VANILLA_GRAVITY = 0.08`, `TANK_CAPACITY = 16000`, `HAZARD_WARN_TICKS = 200`. `HAZARD_WARN_TICKS` ist nur die Untergrenze der Warnzeit (27); die Belastungszeit selbst liest der Code aus `environment.json` (`buildup_ticks`).

## 3.7 Mixins (nur wo nötig)

| Ziel | Zweck |
| --- | --- |
| `Projectile` / `AbstractArrow` Schwerkraft | Projektile fallen mit `g` des Planeten (ab M5) |
| `Explosion` Rückstoß | vertikaler Rückstoß × (1 / g) bei g < 1 (ab M5) |
| `SoundEngine` Lautstärke | Dämpfung im Vakuum (24.6, ab M9) |
| `FireBlock`, `TorchBlock` (Platzierung, Random Tick), `IceBlock` (Schmelzen) | Feuer, Fackeln und Eis nach Regel 8.5 (ab M3) |
| `CropBlock`, `SaplingBlock` (Random Tick, Knochenmehl) | Pflanzenwachstum nach Regel 8.5 (ab M3) |

Alles andere über Events (NeoForge) bzw. Fabric-API-Callbacks.

## 3.8 Datagen (MUSS)

Ressourcen erzeugt `tools/gen_resources.py` aus `spec/data` für alle IDs in `common/implemented_ids.json`; Platzhalter-Texturen erzeugt `tools/gen_placeholders.py`. Ausgabe: `common/src/generated/resources`. Die Dateien werden committet, die CI prüft, dass sie aktuell sind. Gebaut wird nur in GitHub Actions. Es gibt kein Java-`runData`.

Erzeugt werden: Block-/Item-Modelle, Blockstates, Loot-Tables, Rezepte, Tags, Übersetzungen (de_de, en_us), Advancements (24.3), Worldgen (Biome, Features, Noise-Settings, Dimensionen), Forschungsstufen und die sichtbaren Forschungsknoten (11.4), Raketenteile (25.5), Planeten-Einträge, Klänge (`sounds.json`, 24.6). Ab M4 führt `common/implemented_ids.json` auch die registrierten Entities (Schlüssel `entities`, 27.6).

## 3.9 Rezeptanzeige (JEI/REI/EMI)

- Jeder eigene Rezepttyp (ab M2: `crushing`, `pressing`, `electric_smelting`, `refining`, `electrolysis`, `assembly`; später die übrigen aus 25.4) bekommt eine Kategorie. Vanilla-Schmelzrezepte laufen über die Vanilla-Kategorie.
- Eine Kategorie zeigt Eingabeslots links, einen Pfeil mit Dauer (`ticks` aus Rezept oder Maschine) und Energie (J/t × ticks) und Ausgaben rechts mit Chance. Fluide erscheinen als Tank mit Menge in mB. Der Katalysator ist der Maschinenblock. Die Pressform steht als Zutat mit dem Hinweis „wird nicht verbraucht“.
- Mengen werden mit `recipe_amount_multiplier` (24.4) angezeigt. Gesperrte Rezepte (ab M4, 11.3) zeigen in den eigenen Kategorien ein Schloss und einen Tooltip mit dem nötigen Knoten; der Client kennt den Forschungsstand aus `AgencyStatePayload` (3A.5). Für `suit_module_install` und `suit_module_remove` zeigt die Kategorie je Modul ein Beispiel (9.2).
- Keine Worldgen- oder Multiblock-Seiten in M2.
- Test: Ein JUnit-Test prüft, dass die Kategorienliste aus den Rezepttypen aufgebaut wird (ohne die Anzeige-Mod zu laden). Die Darstellung wird manuell geprüft.
- Gibt es JEI, REI oder EMI nicht für 26.3, fällt das Plugin weg und der Eintrag kommt in `docs/spec-gaps.md` (siehe 3.1).
