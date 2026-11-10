# Sternenwerft – Technische Spezifikation v1.0

Stand: 9. November 2026 · Autor: Nick Monger · Grundlage: „Sternenwerft – Design-Dokument“

---

## 0. Lesehinweise für die umsetzende KI

Dieses Dokument ist die **einzige Quelle** für die Umsetzung. Es soll ohne Rückfragen umsetzbar sein.

**Verbindlichkeit**

- **MUSS** = Pflicht. **SOLL** = umsetzen, wenn es den Aufwand nicht sprengt. **KANN** = optional.
- Bei Widerspruch gilt: Tabelle vor Fließtext, späteres Kapitel vor früherem, Kapitel 25 (Datenformate) vor allem.
- Alle Zahlen sind **Startwerte**. Sie MÜSSEN in den Datendateien aus Kapitel 25 (Balance-Werte in 25.7) oder in der Konfiguration stehen, nie fest im Code (Ausnahme: Konstanten in Kapitel 3.6).
- Fehlt trotzdem ein Detail, wähle die einfachste Lösung im Stil von Vanilla und markiere die Stelle im Code mit `// SPEC-GAP: <Beschreibung>`.

**Einheiten**

| Größe | Einheit | Hinweis |
| --- | --- | --- |
| Zeit | Ticks (t) | 20 t = 1 s, 1.200 t = 1 min, 24.000 t = 1 Ingame-Tag, 1.000 t = 1 Ingame-Stunde |
| Energie | J, J/t | 1 J = 1 FE |
| Fluide/Gase | mB | 1.000 mB = 1 Eimer. Fabric: 1 mB = 81 Droplets |
| Strecke | Blöcke | |
| Temperatur | °C | spielinterne Werte, nicht exakt real |
| Schwerkraft | g | 1 g = Vanilla-Schwerkraft 0,08 |

**Konventionen**

- Mod-ID `sternenwerft`. Alle IDs englisch, `snake_case`. Im Text stehen IDs in `code`, z. B. `sternenwerft:crusher` oder kurz `crusher`.
- Ein **„Tank X“** in Rezepten bedeutet 16.000 mB des Fluids X und wird als `fluid`-Zutat kodiert (25.4). Der Montagetisch bekommt das Fluid über Rohre oder über seinen Leerslot (4.1), zum Beispiel aus einem gefüllten `transport_tank`.
- Mengen wie „4 Aluminiumplatten“ beziehen sich auf die Item-IDs aus Kapitel 5.
- **Assets:** Die KI erzeugt **Platzhalter-Texturen** (16×16 PNG, Grundfarbe nach Kapitel 24.5 mit 1 px dunklerem Rand, Items mit hellerem Mittelpunkt) per Skript `tools/gen_placeholders.py`. Modelle für Blöcke: Würfel (`cube_all` bzw. `orientable` für Maschinen). Kreaturen: einfache Quader-Modelle nach Größe in Kapitel 18.

---

## 1. Änderungen gegenüber dem Design-Dokument

Diese Entscheidungen lösen Widersprüche und Sackgassen. **Bitte prüfen**, bevor die Umsetzung beginnt.

| # | Problem im Design-Dokument | Lösung in dieser Spezifikation |
| --- | --- | --- |
| Ä1 | Anzug T2 brauchte Wolfram, Wolfram gibt es nur auf der Venus-Oberfläche, dafür braucht man Anzug T2 | Anzug T2 nutzt **Keramikplatten** (Basalt + Aluminiumstaub im Hochtemperaturofen). Säureschutz bleibt. |
| Ä2 | Kühlsystem (Stufe 2) brauchte flüssiges Helium, der Kryo-Verflüssiger kam erst in Stufe 3 | **Kryo-Verflüssiger nach Stufe 2** verschoben. Das Kühlsystem braucht 1.000 mB statt eines ganzen Tanks flüssiges Helium. |
| Ä3 | Anzug T3 brauchte Isoliergewebe vom Frostkriecher (Triton, nur mit T3 betretbar) | Isoliergewebe ist **herstellbar** (Kunststoff, Silizium, Lithium). Der Frostkriecher lässt es zusätzlich fallen. |
| Ä4 | Der Boss sollte optional sein, die Sternenkarte aber Pflicht | Der Wächter des Senders ist **Pflicht** für die Sternenkarte. Er kann pro Agentur erneut beschworen werden. |
| Ä5 | Säureregen: Schutz nur durch Kuppel T3 (Stufe 4), Venus kommt aber in Stufe 2 | **Kuppel T2 schützt vor Säureregen**. |
| Ä6 | Kältegrenzen der Anzüge passten nicht zu Merkur, Europa und dem Mond | Neue Grenzwerte in Kapitel 9 und Planetentemperaturen in Kapitel 13. |
| Ä7 | Unklar, welche Rakete welche Ziele erreicht | Feste Zieltabelle (12.1) und Treibstoffformel (12.2). |
| Ä8 | Begründung für Wasserstoff beim Fernflug-Modul war nicht schlüssig | Entfernt. Das Fernflug-Modul nutzt Wasserstoff und Sauerstoff wie die T2. |
| Ä9 | „Kühlmodul im Umkehrbetrieb“ und „Heizmodul“ | Zwei getrennte Module: `cooling_module` (Stufe 2) und `heating_module` (Stufe 5). |
| Ä10 | „Kleiner Kernreaktor“ nicht definiert | Entfernt. Außenposten nutzen `fission_reactor` oder Solar + `lithium_battery_bank`. |
| Ä11 | Orbit für jeden Planeten und Mond (zu viele Dimensionen) | Orbit-Dimensionen nur für 8 Planeten (ohne Pluto). Monde haben keinen eigenen Orbit: Raketen fliegen direkt zu ihnen, die Landefähre verbindet sie mit dem Orbit ihres Planeten. |
| Ä12 | Erdöl-Förderung in Phase 1 fehlte | Neue Maschine `fluid_pump` (Phase 1). |
| Ä13 | Energiebedarf von Kuppel T3 (1.480 J/t inkl. Sauerstoff) über einem Fusionsreaktor | Beibehalten, aber dokumentiert. Neues `superconductor_cable` (32.768 J/t), weil Aluminiumkabel für das Endgame nicht reicht. |
| Ä14 | Venus: wie man in die Wolkenschicht kommt und wieder hinunter | Automatische Aerostat-Plattform bei der ersten Landung, dazu ein `cloud_elevator` für Abstiege (12.8). |
| Ä15 | Proben waren nicht definiert | Proben entstehen mit der `sample_tool`. Probenart = Block-ID (Kapitel 11). |
| Ä16 | Drops ohne Verwendung | Jeder Drop hat jetzt ein Rezept oder einen Zweck (Kapitel 18). |
| Ä17 | Nickel und Gips ohne Verwendung | Nickel in Lithium-Akku, Bohrturm und Magnetstiefeln, Gips im Kompost-Boden. |
| Ä18 | Treibstoff reichte für Mars (T1), Io, Europa und das äußere System nicht für Hin- und Rückflug | T1 3–5 Tanks, T2 4–8 Tanks, Fernflug-Modul +128.000 mB je Komponente (Lesart „8 zusätzliche Tanks je Komponente“), Schub T2 = 48. Rückflugtabelle in 12.2. |
| Ä19 | Fortgeschrittene Sonde brauchte Iridium-Chips, Iridium gibt es aber erst dort, wo die Sonde hinführt | Sonde braucht 4 Fortgeschrittene Schaltkreise statt 2 Iridium-Chips. |
| Ä20 | Säureregen und Staubsturm (Stufe 2) brauchten Kuppel T2 (Stufe 3) | Kuppel T2 nach Stufe 2 verschoben. |
| Ä21 | Bewegung in der Station ohne Jetpack (Stufe 4) war undefiniert | Magnetstiefel kommen mit dem Stationskern (Stufe 3) und brauchen Nickel statt Supraleiter. |
| Ä22 | Frachtraketen (32.000 mB) schafften Hin- und Rückflug ins äußere System nicht, und die Werft konnte nicht betanken | Frachtrakete mit 64.000 mB Methan. `cargo_terminal` mit 216 Slots, 8 Frachttanks und eigenem Treibstofftank. |

---

## 2. Glossar

| Begriff | Bedeutung |
| --- | --- |
| **Ring** | Gruppe von Zielen nach Entfernung, festgelegt in der Spalte „Ring“ von 12.1: Ring 0 = Erde und Erdorbit, Ring 1 = Mond, Mars, Venus und ihre Orbits, Ring 2 = alle übrigen. Bestimmt den FP-Wert von Proben. |
| **Stufe** | Forschungsstufe 1–6. Bestimmt, was freigeschaltet werden kann. |
| **Phase** | Spielabschnitt 1–6 aus Spielersicht (nur für Handbuch, Balancing und Meilensteine). |
| **Tor** | Bedingung, die die nächste Stufe öffnet (Spalte „Öffnet bei“ in 11.4). |
| **DP** | Distanzpunkte eines Ziels, Grundlage für Treibstoff und Flugzeit. |
| **Agentur** | Spielergruppe, die Forschung, Atlas und Logistik teilt. Jeder Spieler ist in genau einer. |
| **Belastung** | Anzeige 0–100 % je Gefahr. Bei 100 % beginnt Schaden (Kapitel 8). |
| **Außenposten** | Ein `cargo_terminal` mit allen Produzenten im Umkreis von 16 Blöcken. |

| Phase | Inhalt | Stufen |
| --- | --- | --- |
| 1 Erdindustrie | Rohstoffe, Maschinen, Strom | keine Forschung |
| 2 Raumfahrtprogramm | Forschung, Sonde, Rakete T1, Mond | 1 |
| 3 Ring 1 | Mond, Mars, Venus, Kuppel, T2 | 2 |
| 4 Orbit | Station, Fusion | 3 |
| 5 Ring 2 | Merkur bis Pluto | 4–5 |
| 6 Netzwerk und Finale | Logistik, Werft, T3 | 5–6 |

---

## 3. Technischer Rahmen

### 3.1 Versionen (MUSS)

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

### 3.2 Projektaufbau

```
sternenwerft/
  common/     – gesamte Spiellogik, Datagen, Netzwerk-Payloads, Services-Interfaces
  neoforge/   – Registrierung, Capabilities, Events, Sky-Renderer, Config-Pfad
  fabric/     – Registrierung, Team Reborn Energy, Fabric Transfer API, Sky-Renderer
  tools/      – gen_placeholders.py, balance_sheet.py (Kapitel 27)
```

- Plattformunterschiede laufen über `ServiceLoader`-Interfaces in `common` (`IPlatformHelper`, `IEnergyHelper`, `IFluidHelper`, `INetworkHelper`, `IRegistryHelper`).
- Registrierung über ein gemeinsames `RegistrationProvider`-Muster (Supplier-basiert), in beiden Loadern implementiert.
- Item-Daten über **Data Components**, nicht über NBT. Brennwert (`combustion_generator`) liest die Vanilla-Komponente `COOKING_FUEL`. Kompostierbare Mod-Items bekommen `COMPOSTABLE`.

### 3.3 Energie und Fluide auf den Plattformen

| Thema | common | NeoForge | Fabric |
| --- | --- | --- | --- |
| Energie | eigene Klasse `SwEnergyStorage` (long, J) | `Capabilities.Energy.BLOCK` (Transfer-API) | `team.reborn.energy.api.EnergyStorage.SIDED` |
| Fluide | eigene Klasse `SwFluidTank` (mB) | `Capabilities.Fluid.BLOCK` mit `ResourceHandler<FluidResource>` | `FluidStorage.SIDED` (Droplets ↔ mB umrechnen) |
| Items | eigene Klasse `SwItemContainer` | `Capabilities.Item.BLOCK` mit `ResourceHandler<ItemResource>` | `ItemStorage.SIDED` |

- **NeoForge-Transfer-API** (seit 21.9, gilt für 26.x): `IItemHandler`, `IFluidHandler` und `FluidTank` gibt es nicht mehr. Ein- und Auslagern läuft über `ResourceHandler` in **Transaktionen**. Abgebrochene Transaktionen MÜSSEN den vorherigen Zustand wiederherstellen (Snapshot). Leere Ressourcen nie übergeben, sie werfen eine Exception.
- **Fabric-Transfer-API** arbeitet ebenfalls mit Transaktionen. Die Klassen in `common` MÜSSEN daher ein gemeinsames Transaktionsmodell anbieten (simulieren → bestätigen/zurückrollen), das beide Plattformschichten abbilden.
- Maschinen, die auch Vanilla-Trichter bedienen sollen, implementieren zusätzlich `Container` (Trichter prüfen das Interface vor der Capability).
- Die Präfix-Namen `Sw…` vermeiden Namenskonflikte mit Plattformklassen.

Gase sind Fluide mit dem Tag `sternenwerft:gases`. Gase haben keine platzierbaren Quellblöcke und keine Eimer.

### 3.4 Netzwerk

`CustomPacketPayload` mit `StreamCodec`, registriert in beiden Loadern über `INetworkHelper`. Payloads: Forschungsstand, Atlas, Agentur, Kuppeln je Dimension, Umweltwerte für das HUD, Raketen-GUI-Aktionen, Logistik-GUI, Konfiguration.

### 3.5 Konfiguration

- Eigene JSON-Datei pro Welt: `<welt>/serverconfig/sternenwerft.json`. Sie wird beim ersten Start mit Standardwerten angelegt und beim Join an Clients gesendet.
- Client-Datei `config/sternenwerft-client.json` nur für HUD-Position und Lautstärke.
- Schlüssel: Kapitel 24.4.

### 3.6 Code-Konstanten

`TICKS_PER_SECOND = 20`, `VANILLA_GRAVITY = 0.08`, `TANK_CAPACITY = 16000`, `HAZARD_WARN_TICKS = 200`.

### 3.7 Mixins (nur wo nötig)

| Ziel | Zweck |
| --- | --- |
| `Projectile` / `AbstractArrow` Schwerkraft | Projektile fallen mit `g` des Planeten |
| `Explosion` Rückstoß | vertikaler Rückstoß × (1 / g) bei g < 1 |
| `SoundEngine` Lautstärke | Dämpfung im Vakuum (24.6) |
| `FireBlock`, `TorchBlock`-Platzierung | Feuer und Fackeln nach Regel 8.5 |
| `CropBlock` Random Tick | Pflanzenwachstum nach Regel 8.5 |

Alles andere über Events (NeoForge) bzw. Fabric-API-Callbacks.

### 3.8 Datagen (MUSS)

Datagen erzeugt: Block-/Item-Modelle, Blockstates, Loot-Tables, Rezepte, Tags, Übersetzungen (de_de, en_us), Advancements, Worldgen (Biome, Features, Noise-Settings, Dimensionen), Forschungsknoten, Planeten-Einträge.

---

## 4. Grundsysteme

### 4.1 Maschinen-Grundverhalten (alle Maschinen)

- **GUI** im Vanilla-Stil: Eingabeslots links, Ausgabe rechts, Energieleiste links außen, Fluidleisten rechts außen, 2 Upgrade-Slots unten rechts, Fortschrittspfeil in der Mitte.
- **Automatisierung:** Von jeder Seite aus gehen Items und Fluide in Eingabeslots und Eingabetanks. Von jeder Seite aus lassen sich nur Ausgaben entnehmen.
- **Füll- und Leerslots:** Jeder Block mit Fluidtank (Maschinen mit Fluid-Ein- oder -Ausgang, `fluid_tank`, `pressure_bottle`, `gas_tank`, `tank_depot`, `oxygen_generator`, `cargo_terminal`, Raketen-GUI) hat ein Slotpaar:
  - **Füllslot:** Ein Behälter-Item (`transport_tank`, `oxygen_canister`, `large_oxygen_canister`, Eimer) wird *aus* dem Block gefüllt.
  - **Leerslot:** Ein gefülltes Behälter-Item wird *in* den Block entleert.
  - Der Behälter bleibt jeweils im Slot liegen, bis man ihn herausnimmt oder ein Trichter ihn abzieht.
  - Behälter speichern Fluid und Menge in der Data Component `sternenwerft:fluid`.
  - **Mehrere Tanks:** Der Füllslot nimmt aus dem ersten Ausgabetank, dessen Fluid zum Behälter passt; für einen leeren Behälter gilt der im GUI gewählte Tank (Knopf „Tank wählen“, Standard Tank 1). Der Leerslot gibt in den ersten passenden Eingangstank.
  - **Trichter** ziehen aus dem Füllslot nur volle und aus dem Leerslot nur leere Behälter.
- **Energiepuffer:** 20 × Verbrauch (J/t) × min(Vorgangsdauer, 100), mindestens 10.000 J, höchstens 1.000.000 J. Maschinen ohne Vorgangsdauer rechnen mit 100.
- **Statuslampe** (Blockstate `status`): `running` grün, `idle` gelb (wartet auf Material oder Strom), `error` rot (Ausgabe voll, falscher Ort, Struktur unvollständig).
- **Ausrichtung:** Blockstate `facing` (horizontal), zeigt zum Spieler.
- Sneak-Rechtsklick mit leerer Hand auf einen Multiblock-Controller zeigt 200 t lang eine Geisterprojektion der fehlenden Blöcke.

### 4.2 Upgrades

| Item | Slot | Wirkung pro Stück | Max. Stapel |
| --- | --- | --- | --- |
| `speed_upgrade` | 1 | Vorgangsdauer × 0,75, Verbrauch J/t × 1,5 | 4 |
| `efficiency_upgrade` | 2 | Verbrauch J/t × 0,85 | 4 |

Die Wirkung multipliziert sich. Upgrades gelten für alle Maschinen, Generatoren nicht.

### 4.3 Multiblöcke

- Ein Multiblock besteht aus einem **Controller** (teuer) und **Gehäuseblöcken** (billig). Gehäuseblöcke dürfen durch Port-Blöcke ersetzt werden: `item_port`, `fluid_port`, `energy_port`. Ein Port leitet nach außen an den Controller weiter. Ports brauchen keine Forschung (die Raffinerie nutzt sie schon in Phase 1). Der Controller nimmt an seinen freien Seiten ebenfalls Energie, Fluide und Items an und gibt Ausgaben ab.
- Der Controller prüft die Struktur beim Platzieren, bei Blockänderungen im Umkreis und alle 100 t. Ist sie unvollständig, steht die Statuslampe auf `error`.

| Multiblock | Maße (B×T×H) | Aufbau |
| --- | --- | --- |
| `refinery` | 3×3×5 | massiv; Controller unten Mitte einer Seitenfläche; Rest `refinery_casing` oder Ports |
| `fusion_reactor` | 5×5×5 | hohle Hülle (98 Blöcke), Innenraum 3×3×3 Luft; Controller Mitte einer Seitenfläche; Rest `fusion_casing` oder Ports |
| `fission_reactor` | 5×5×5 | wie Fusion, mit `fission_casing` |
| `launch_pad_t1` | 5×5 Plattform | 25 `launch_pad`; dazu eine Säule `launch_tower` direkt neben der Plattform, mindestens so hoch wie die Rakete; `fuel_pump` an Plattform oder Turm angrenzend; `launch_control` im Umkreis von 8 Blöcken |
| `launch_pad_t2` | 7×7 Plattform | wie T1 mit 49 `launch_pad` |

### 4.4 Kabel und Rohre

| Block | Durchsatz | Hinweis |
| --- | --- | --- |
| `copper_cable` | 256 J/t | |
| `aluminum_cable` | 2.048 J/t | Stufe 1 |
| `superconductor_cable` | 32.768 J/t | Stufe 3 |
| `copper_pipe` | 200 mB/t | Fluide und Gase |
| `titanium_pipe` | 1.000 mB/t | Stufe 2 |

Kabel und Rohre verbinden sich automatisch mit Nachbarn, die Energie bzw. Fluide anbieten.

**Netzlogik:**
- Ein **Netz** ist ein zusammenhängender Strang **gleicher** Kabel- bzw. Rohrsorte. Verschiedene Sorten verbinden sich nicht direkt; zum Übergang dient ein Speicherblock (`battery`, `fluid_tank` usw.).
- Durchsatz des Netzes = Durchsatz der Sorte (siehe Tabelle) pro Tick, unabhängig von der Länge.
- **Push-Prinzip:**
  - Erzeuger, Speicher und Maschinen-Ausgaben schieben jeden Tick in angrenzende Netze.
  - Das Netz verteilt gleichmäßig auf alle angeschlossenen Empfänger, die noch aufnehmen können (Rest im nächsten Tick).
  - Verbraucher ziehen nie selbst.
- **Speicherblöcke** (`battery`, `lithium_battery_bank`, `fluid_tank`, `pressure_bottle`, `gas_tank`, `tank_depot`) haben genau eine **Ausgangsseite** (Standard: die `facing`-Seite, im GUI wählbar). Nur dort schieben sie; alle anderen Seiten sind Eingänge. So gibt es kein Hin- und Herschieben zwischen Speichern, und ein Speicher verbindet zwei verschiedene Netze.
- **Rohre** ziehen nie selbst; Fluide kommen nur über Schieben von Ausgabeseiten ins Netz.
- Ein Rohrnetz führt nur ein Fluid gleichzeitig. Es wechselt erst, wenn es leer ist.

---

## 5. Materialien

### 5.1 Metalle und Formen

Pro Metall gibt es die angekreuzten Formen. IDs: `raw_<m>`, `<m>_dust`, `<m>_ingot`, `<m>_plate`, `<m>_rod`, `<m>_wire`. Bei Vanilla-Metallen fügt die Mod nur die fehlenden Formen hinzu.

| Metall `<m>` | raw | dust | ingot | plate | rod | wire | Fundort |
| --- | --- | --- | --- | --- | --- | --- | --- |
| aluminum | `bauxite` | ✓ | ✓ | ✓ | – | ✓ | Erde, Mond |
| titanium | ✓ | ✓ | ✓ | ✓ | ✓ | – | Erde, Mond, Merkur |
| iron | Vanilla | ✓ | Vanilla | ✓ | ✓ | – | Erde, Mars, Merkur, Meteoriten |
| copper | Vanilla | ✓ | Vanilla | – | – | ✓ | Erde |
| gold | Vanilla | ✓ | Vanilla | – | – | ✓ | Erde |
| magnesium | ✓ | ✓ | ✓ | – | – | – | Mars |
| nickel | ✓ | ✓ | ✓ | ✓ | – | – | Mars (Meteoriten), Asteroiden |
| tungsten | ✓ | ✓ | ✓ | ✓ | – | – | Venus |
| lithium | `lithium_salt` | ✓ | ✓ | – | – | – | Merkur |
| platinum | ✓ | ✓ | ✓ | – | – | – | Asteroiden, Meteoriten (selten) |
| iridium | ✓ | ✓ | ✓ | – | – | – | Asteroiden, Meteoriten (selten) |
| uranium | ✓ | ✓ | ✓ | – | – | – | Pluto |

Tags nach Konvention `c:` (z. B. `c:ingots/aluminum`, `c:plates/titanium`, `c:dusts/tungsten`, `c:raw_materials/nickel`, `c:ores/titanium`).

### 5.2 Weitere Items

| ID | Name DE | Herkunft |
| --- | --- | --- |
| `silicon` | Silizium | `quartz_sand` im Elektroofen |
| `plastic` | Kunststoff | Raffinerie |
| `ceramic_plate` | Keramikplatte | Hochtemperaturofen |
| `magnesium_alloy` | Magnesium-Legierung | Elektroofen (2 Eingaben) |
| `lightweight_plate` | Leichtbau-Platte | Presse aus `magnesium_alloy` |
| `circuit`, `advanced_circuit`, `iridium_chip` | Schaltkreise | Montagetisch / Schwerelos-Fertigung |
| `pressure_hull`, `heat_shield`, `cooling_system` | Bauteile | Montagetisch |
| `superconductor_wire` | Supraleiterdraht | Schwerelos-Fertigung |
| `lithium_battery` | Lithium-Akku (Bauteil) | Montagetisch |
| `insulation_fabric` | Isoliergewebe | Montagetisch, Drop Frostkriecher |
| `fuel_rod` | Brennstab | Montagetisch |
| `antimatter_cell` | Antimaterie-Zelle (leer/gefüllt per Data Component `charge` 0/1) | Montagetisch, gefüllt in `magnetic_trap` |
| `sulfur` | Schwefel | Venus, Io, Schwefelschnecke |
| `dry_ice` | Trockeneis | Mars-Polkappen |
| `gypsum` | Gips | Mars, ausgetrocknete Seen |
| `microbe_culture` | Mikrobenkultur | Europa, hydrothermale Quellen |
| `metal_concentrate` | Metallkonzentrat | Drop Geröllkrabbler |
| `armor_plate` | Panzerplatte | Drop Dünenwühler |
| `mirror_shell` | Spiegelpanzer | Drop Spiegelkäfer |
| `heat_scale` | Hitzeschuppe | Drop Schwefelsalamander |
| `skimmer_leg` | Läuferbein | Drop Methanläufer |
| `star_chart` | Sternenkarte | Drop Wächter des Senders |
| `compost` | Kompost | Montagetisch aus 8 Pflanzen-Items |
| `transport_tank` | Transporttank, 16.000 mB, ein Fluid, Data Component `fluid` | Montagetisch |
| `oxygen_canister` | Sauerstoffflasche, 600 mB | Montagetisch |
| `large_oxygen_canister` | große Sauerstoffflasche, 1.200 mB | Montagetisch |
| `press_mold_plate`, `press_mold_rod`, `press_mold_wire` | Pressformen (werden nicht verbraucht) | Werkbank |
| `sample_tool` | Probenzange | Montagetisch |
| `sample` | Probe (Data Component `sternenwerft:sample` mit den Feldern `block` und `dimension`) | erzeugt durch `sample_tool` |
| `engineers_handbook` | Ingenieurs-Handbuch | beim ersten Join, Ersatz per Werkbank |
| `planetary_atlas` | Planetenatlas | beim ersten Join, Ersatz per Werkbank |
| `field_scanner` | Feldscanner | Montagetisch |
| `space_ration` | Weltraumration | Nahrungspacker |
| `ammonia_fertilizer` | Ammoniak-Dünger | Montagetisch |

### 5.3 Fluide

| ID | Typ | Farbe (Rohre) | Quelle |
| --- | --- | --- | --- |
| `crude_oil` | flüssig, Quellblock + Eimer | schwarz | Erde, Ölblasen |
| `kerosene` | flüssig, Eimer | braun | Raffinerie |
| `hydrogen` | Gas | weiß | Elektrolyseur, Gas-Sammler |
| `oxygen` | Gas | hellblau | Elektrolyseur, Sauerstoffgenerator |
| `helium` | Gas | rosa | Regolith-Erhitzer, Gas-Sammler (Saturn) |
| `liquid_helium` | flüssig | rosa, heller | Kryo-Verflüssiger |
| `nitrogen` | Gas | grau | Pumpstation auf `nitrogen_geyser` |
| `liquid_nitrogen` | flüssig | grau, heller | Kryo-Verflüssiger |
| `methane` | flüssig, Quellblock auf Titan | gelb | Titan-Seen, Gas-Sammler (Uranus, Neptun; gibt es direkt flüssig aus) |
| `ammonia` | flüssig | grün | Pumpstation auf `ammonia_geyser` |

Wasser ist Vanilla.

### 5.4 Verarbeitungsketten (max. 3 Schritte)

```
Erz → Zerkleinerer (×2 Staub) → Elektroofen  → Barren → Presse (+Pressform) → Platte / Stange / Draht
Erz ───────────────────────────→ Vanilla-Ofen → Barren          (nur aluminum, titanium, iron, copper, gold)
raw_tungsten / tungsten_dust ──→ Hochtemperaturofen → tungsten_ingot
crude_oil → Raffinerie → kerosene + plastic
Wasser → Elektrolyseur → hydrogen + oxygen
moon_regolith → Regolith-Erhitzer → helium → Kryo-Verflüssiger → liquid_helium
```

---

## 6. Maschinen

### 6.1 Steckbriefe

| ID | Name DE | Stufe | Verbrauch | Dauer | Ein → Aus | Ort |
| --- | --- | --- | --- | --- | --- | --- |
| `crusher` | Zerkleinerer | – | 20 J/t | 200 t | 1 Erz/Raw → 2 Staub; `metal_concentrate` → 1 `aluminum_dust` + 1 `titanium_dust` | überall |
| `electric_furnace` | Elektroofen | – | 20 J/t | 100 t | alle Vanilla-Schmelzrezepte + Rezepttyp `electric_smelting`; 2 Eingabeslots für Legierungen | überall |
| `press` | Presse | – | 30 J/t | 100 t | Barren + Pressform → Produkt (6.2) | überall |
| `refinery_controller` | Raffinerie (Multiblock `refinery`) | – | 40 J/t | 200 t | 1.000 mB `crude_oil` → 600 mB `kerosene` + 1 `plastic` | überall |
| `electrolyzer` | Elektrolyseur | – | 40 J/t | 40 t | 200 mB Wasser → 200 mB `hydrogen` + 100 mB `oxygen` | überall mit Wasser |
| `fluid_pump` | Pumpe | – | 20 J/t | 40 t | saugt einen Fluid-Quellblock im Umkreis von 8 Blöcken unterhalb ab → 1.000 mB; Quellblock wird entfernt; wählt die nächste Quelle mit dem Fluid, das schon im Tank ist (bei leerem Tank die nächste überhaupt) | überall |
| `assembly_table` | Montagetisch | – | 30 J/t | 200 t | Rezepttyp `assembly`, `suit_module_install`, `suit_module_remove`: 9 Eingabeslots + 1 Fluid-Eingang (16.000 mB) → 3 Ausgabeslots (Hauptergebnis + Nebenausgaben, z. B. ausgebaute Module) | überall |
| `analyzer` | Analysator | – | 20 J/t | 100 t | 1 `sample` → FP (Kapitel 11) | überall |
| `high_temperature_furnace` | Hochtemperaturofen | 2 | 80 J/t | 400 t | Rezepttyp `high_temp_smelting`, 2 Eingabeslots | überall |
| `regolith_heater` | Regolith-Erhitzer | 2 | 40 J/t | 100 t | 1 `moon_regolith` → 50 mB `helium` | nur `moon` |
| `cryo_liquefier` | Kryo-Verflüssiger | 2 | 120 J/t | 20 t | 40 mB Gas → 10 mB Flüssigkeit (`helium`, `nitrogen`) | überall |
| `hydroponics_basin` | Hydrokultur-Becken | 2 | 10 J/t + 1 mB Wasser/20 t | – | Pflanze im Becken wächst doppelt so schnell; braucht Lichtlevel ≥ 9, in Mod-Dimensionen zusätzlich eine aktive Kuppel (8.5) | überall |
| `food_packer` | Nahrungspacker | 1 | 10 J/t | 100 t | 3 Nahrungsmittel → 1 `space_ration` (Kapitel 17) | überall |
| `zero_g_fabricator` | Schwerelos-Fertigung | 3 | 100 J/t | 200 t | Rezepttyp `zero_g` | nur Orbit-Dimensionen |
| `space_weather_station` | Weltraumwetter-Station | 3 | 50 J/t | – | Frühwarnung (Kapitel 19) | überall außer Erde |
| `gravity_plate` | Schwerkraftplatte | 3 | 400 J/t | – | 1 g im Bereich 9×9 über der Platte, bis 8 Blöcke hoch | überall |
| `gas_collector` | Gas-Sammler | 4 | 200 J/t | 20 t | → 100 mB Gas des Planeten (13.3) | nur Orbits von Jupiter, Saturn, Uranus, Neptun |
| `magnetic_trap` | Magnetfalle | 4 | 2.000 J/t | 24.000 t | 1 leere `antimatter_cell` → 1 gefüllte | nur `jupiter_orbit` |
| `pump_station` | Pumpstation | 5 | 100 J/t | 20 t | → 100 mB aus Fluid-Quellblock oder Geysir direkt darunter; Quelle wird nicht verbraucht | überall |
| `drilling_rig` | Bohrturm | 5 | 200 J/t | 100 t | → 1 Erz-Item (20.2) | überall außer Erde |
| `clone_chamber` | Klon-Kammer | 4 | 200 J/t | – | Respawn-Punkt (14.4) | überall |

### 6.2 Pressen

| Eingabe | Pressform | Ausgabe |
| --- | --- | --- |
| 1 Barren | `press_mold_plate` | 1 Platte |
| 1 Barren | `press_mold_rod` | 2 Stangen |
| 1 Barren | `press_mold_wire` | 3 Draht |
| 1 `magnesium_alloy` | `press_mold_plate` | 1 `lightweight_plate` |

### 6.3 Generatoren und Speicher

| ID | Name DE | Stufe | Leistung | Brennstoff / Bedingung | Speicher |
| --- | --- | --- | --- | --- | --- |
| `combustion_generator` | Verbrennungsgenerator | – | 20 J/t | Vanilla-Brennwert in Ticks (Kohle 1.600 t) | 20.000 J |
| `fuel_generator` | Treibstoffgenerator | – | 80 J/t | 1 mB `kerosene` pro 4 t | 80.000 J |
| `solar_panel` | Solarpanel | 1 | 5 J/t × Solarfaktor × Tageslicht (Oberfläche); 20 J/t × Solarfaktor (Orbit) | Himmel sichtbar | 5.000 J |
| `solar_wing` | Solarflügel | 3 | wie Solarpanel im Orbit | nur Orbit | 5.000 J |
| `fusion_reactor` | Fusionsreaktor | 3 | 1.000 J/t | 10 mB `hydrogen` + 5 mB `helium` pro 20 t; Kühlung 2 mB `liquid_helium` pro 20 t, gibt 2 mB `helium` aus | 1.000.000 J |
| `fission_reactor` | Kernreaktor | 5 | 4.000 J/t | 1 `fuel_rod` pro 48.000 t (4 Slots); Kühlung 50 mB Wasser/t **oder** 5 mB `liquid_nitrogen`/t | 4.000.000 J |
| `battery` | Akku | – | – | Ein/Aus 512 J/t, 1 Ladeslot für Items | 500.000 J |
| `lithium_battery_bank` | Lithium-Akkubank | 4 | – | Ein/Aus 4.096 J/t | 4.000.000 J |
| `fluid_tank` | Tank | – | – | nur Flüssigkeiten | 16.000 mB |
| `pressure_bottle` | Druckflasche | 1 | – | nur Gase | 8.000 mB |
| `gas_tank` | Gastank | 2 | – | nur Gase | 64.000 mB |
| `tank_depot` | Tankdepot | 3 | – | alle Fluide | 256.000 mB |

**Tageslicht** = 1 bei Tag, 0 bei Nacht, linear in der Dämmerung. Regen und Staubsturm: × 0,25. Sonnensturm: × 2.

**Reaktor-Sicherheit**

- Fusion: Fehlt die Kühlung 40 t lang, schaltet der Reaktor ab (Status `error`). Keine Explosion. Er startet automatisch neu, sobald die Kühlung 100 t lang ohne Unterbrechung läuft.
- Kernreaktor: interne Hitze 0–1.000. Mit Kühlung −5/t, ohne Kühlung +2/t. Ab 800 Warnung (Ton, Statuslampe blinkt). Bei 1.000 Notabschaltung und **Strahlungszone** (Radius 16, Strahlung 3) für 72.000 t. Keine Explosion. Nach einer Notabschaltung startet er automatisch wieder, sobald die Hitze unter 200 liegt.

---

## 7. Generatoren und Speicher

Siehe Kapitel 6.3. (Die Kapitelnummer bleibt frei, damit Verweise stabil bleiben.)

---

## 8. Umweltsystem

### 8.1 Umweltwerte

Jede Dimension hat Werte aus ihrem Planeten-Eintrag (Kapitel 13 und 25.1). **Zonen** im Planeten-Eintrag (z. B. Venus-Wolkenschicht, Europa-Ozean) überschreiben diese Werte in ihrem Bereich. **Dimensionen ohne Planeten-Eintrag** (Nether, End, Dimensionen anderer Mods) gelten wie die Erde: keine Umweltgefahren.

Die Berechnung MUSS als reine Funktion `EnvironmentCalculator.compute(planet, zonen, position, tageszeit, kuppeln, ausrüstung)` (`zonen` = alle Zonen, die die Position enthalten; Vorrang siehe 25.1) umgesetzt werden, damit sie testbar ist (Kapitel 27). Pro Spieler werden alle 10 t berechnet:

| Wert | Berechnung |
| --- | --- |
| `breathable` | Erde: ja. Sonst nur in aktiver Kuppel |
| `temperature` | Biom mit Override: fester Wert. Sonst `lerp(temp_night, temp_day, Tageslicht) + biome_offset`. In aktiver Kuppel mit passender Stufe: 20 °C |
| `pressure` | `vacuum`, `low`, `normal`, `high`, `deep` (Tiefseedruck) aus Planet bzw. Zone. In Kuppel: `normal` |
| `radiation` | Stufe aus Planet bzw. Zone, +2 bei Sonnensturm, −2 bei ≥ 3 festen Blöcken senkrecht über dem Spieler, 0 in Kuppel T3. **Ergebnis auf 0–3 begrenzt.** |

### 8.2 Belastung (für alle Gefahren gleich)

- Für jede ungeschützte Gefahr (Sauerstoff, Hitze, Kälte, Druck, Strahlung) füllt sich eine **Belastung** von 0 auf 100 % in 200 t (= Pflicht-Warnzeit von 10 s).
- Ab 1 % erscheint die HUD-Leiste mit Warnton. Bei 100 % Schaden: 1 HP pro 20 t (Sauerstoff, Druck, Hitze über 300 °C), 1 HP pro 40 t (Kälte, Hitze, Strahlung 2), 1 HP pro 20 t (Strahlung 3).
- Ist die Gefahr weg, sinkt die Belastung in 100 t auf 0.

### 8.3 Schutz ohne Anzug

Spieler ohne vollständigen Anzug vertragen −20 bis +45 °C, Druck `low` bis `normal`, Strahlung ≤ 1, und brauchen `breathable`.

### 8.4 HUD

Kleine Leisten oberhalb der Hungerleiste: Sauerstoff (Anzugvorrat), Temperatur, Strahlung, Hitzezeit (nur Venus-Oberfläche). Sie erscheinen nur, wenn der Wert relevant ist. Farben: grün, gelb ab 50 %, rot ab 80 %.

### 8.5 Welt ohne Atmosphäre

**Dies ist die einzige gültige Regel; andere Kapitel verweisen hierher.** In allen Mod-Dimensionen (alle außer Overworld, Nether und End, dort gilt Vanilla) gilt außerhalb aktiver Kuppeln, unabhängig von der Atmosphäre:

- Fackeln, Feuer, Lagerfeuer und Laternen lassen sich nicht platzieren. Bestehende erlöschen beim nächsten Random Tick (Fackel → `extinguished_torch`, dekorativ).
- Pflanzen wachsen nicht (Random Tick abgebrochen).
- Platziertes Wasser wird sofort zu Eis (Planet `water_behavior: freeze`) oder verschwindet (`evaporate`).
- Elektrische Lampe `electric_lamp` (5 J/t, Licht 15) funktioniert überall.

---

## 9. Raumanzug

### 9.1 Stufen

Ein Anzug schützt nur, wenn **alle 4 Teile dieselbe oder eine höhere Stufe** haben. Es gilt die niedrigste Stufe der 4 Teile.

| Stufe | IDs | Rüstung wie | Temperatur | Druck | Sonst | O₂-Verbrauch |
| --- | --- | --- | --- | --- | --- | --- |
| T1 `pressure_suit` | `pressure_suit_helmet/chestplate/leggings/boots` | Eisen | −180 bis +150 °C | `vacuum`–`normal` | – | 1 mB / 20 t |
| T2 `heat_suit` | `heat_suit_*` | Diamant | −200 bis +500 °C | alle außer Tiefseedruck | Säureregen, Hitzezeit Venus 3.600 t | 1 mB / 20 t |
| T3 `deep_space_suit` | `deep_space_suit_*` | Netherit | −220 bis +500 °C | alle außer Tiefseedruck | Strahlung ≤ 2 | 1 mB / 40 t |

Alle Grenzwerte sind **inklusiv** (−200 °C ist für T2 noch sicher). **Tiefseedruck** (Europa-Ozean unter y 80) ist eine eigene Gefahr, gegen die nur `diving_module` schützt.

- **Sauerstoff:** Die Brustplatte hat einen Vorrat (`oxygen`, max. 1.200 mB). Sie füllt sich automatisch aus `oxygen_canister`/`large_oxygen_canister` im Inventar (die Flasche wird dabei geleert) und in aktiven Kuppeln mit 1 mB/t aus dem O₂-Tank des Kuppelgenerators (10.1). Liegen weniger als 500 mB im Tank, wird nicht nachgefüllt, damit die Kuppel nicht ausfällt. Eine Flasche mit 600 mB = 10 min (T1/T2) bzw. 20 min (T3).
- **Ertrinken:** Ein vollständiger Anzug mit O₂ > 0 hält den Vanilla-Luftvorrat unter Wasser voll; stattdessen sinkt der Anzugvorrat normal weiter.
- **Hitzezeit Venus-Oberfläche:** Außerhalb der Wolkenschicht (y < 180 in `venus`) läuft ein Timer von 3.600 t. Danach gilt der Spieler als ungeschützt gegen Hitze. Der Timer füllt sich oberhalb y 180 in 1.200 t wieder auf.

### 9.2 Module

Jedes Anzugteil hat 2 Modulslots (Data Component `suit_modules`). Einbau: Anzugteil + Modul im Montagetisch. Ausbau: Anzugteil allein im Montagetisch (Rezepttyp `suit_module_remove`) gibt das Anzugteil ohne Module und die Module in die 3 Ausgabeslots. Einbau ist Rezepttyp `suit_module_install`. Beim Aufwerten übernimmt das Ergebnis alle Komponenten des Basisteils (`copy_components_from`, 25.4). Module bleiben beim Aufwerten auf die nächste Anzugstufe erhalten. Die Spalte „Stufe“ ist die Forschungsstufe. Module passen auf jede Anzugstufe.

| ID | Teil | Stufe | Wirkung |
| --- | --- | --- | --- |
| `helmet_lamp` | Helm | 1 | setzt alle 5 t einen Vanilla-`light`-Block (Level 14) an die Kopfposition, entfernt den alten |
| `radiation_shield` | Helm | 4 | schützt gegen Strahlung ≤ 3 |
| `cooling_module` | Brust | 2 | obere Temperaturgrenze +100 °C |
| `heat_reflector` | Brust | 4 | Hitzezeit Venus × 1,5 |
| `heating_module` | Brust | 5 | untere Temperaturgrenze −30 °C |
| `jetpack` | Brust | 4 | 200.000 J, 40 J/t solange Springen gedrückt; hebt die Schwerkraft auf und beschleunigt zusätzlich um 0,12 Blöcke/t² nach oben, max. 0,4 Blöcke/t; laden im `battery`-Ladeslot |
| `diving_module` | Hose | 4 | Schutz gegen Tiefseedruck (Europa unter y 80), Schwimmtempo +50 % |
| `armor_plating` | Hose/Stiefel | 2 | +3 Rüstung |
| `magnet_boots` | Stiefel | 3 | in Schwerelosigkeit 1 g, solange ein Block direkt unter dem Spieler ist |

---

## 10. Kuppel und Sauerstoff

### 10.1 Kuppelgenerator

| Stufe | ID | Radius | Strom | O₂ | Hält Außentemperatur | Zusätzlicher Schutz |
| --- | --- | --- | --- | --- | --- | --- |
| T1 | `dome_generator_t1` | 8 | 40 J/t | 50 mB/1.200 t | −180 bis +150 °C | Atemluft, Druck |
| T2 | `dome_generator_t2` | 16 | 200 J/t | 200 mB/1.200 t | −210 bis +250 °C | Staubsturm, Säureregen |
| T3 | `dome_generator_t3` | 32 | 1.000 J/t | 800 mB/1.200 t | −250 bis +300 °C | Strahlung (= 0) |

- **Form:** Halbkugel. Ein Punkt ist drin, wenn `abstand(punkt, mittelpunkt) ≤ radius` und `punkt.y ≥ mittelpunkt.y − 1`.
- **Liegt die Außentemperatur außerhalb des Bereichs**, funktioniert die Kuppel nicht (Status `error`). Gemessen wird an der Position des Generators; ist die Kuppel aktiv, schützt sie ihr ganzes Volumen, auch wenn es in andere Zonen reicht.
- **Module** (3 Slots): `dome_range_module` (Radius × 1,5, Strom und O₂ × 2), `dome_efficiency_module` (O₂ × 0,75), `dome_shield_module` (fängt Meteoriten und Ringtrümmer ab; Mobs werden nicht abgehalten).
- **Verwaltung:** pro Dimension eine `SavedData`-Liste aktiver Kuppeln (Mittelpunkt, Radius, Stufe, Agentur, Zustand). Überlappende Kuppeln bilden einen gemeinsamen Luftraum.
- **O₂-Tank:** Jeder Kuppelgenerator hat einen internen Tank mit 2.000 mB, gefüllt über Rohre oder angrenzende `oxygen_generator`. Aus ihm kommen der Verbrauch der Kuppel und das Nachfüllen der Anzüge (9.1).
- **Meteoriten und Ringtrümmer** hält keine Kuppelstufe ab, nur das `dome_shield_module`.
- **Ausfall:** Fehlt Strom oder O₂, geht die Kuppel in den Zustand `failing`: flackert rot, Warnung an alle Spieler darin, nach 600 t aus. Danach gilt die Welt dort als Atmosphäre des Planeten.
- **Rendering:** halbtransparente Kugel mit Sechseckmuster-Textur, Farbe T1 weißlich, T2 blau, T3 violett, beim Ausfall rot pulsierend. Nur rendern, wenn innerhalb von 128 Blöcken.

### 10.2 Sauerstoffgenerator `oxygen_generator`

100 mB `oxygen` pro 1.200 t, 60 J/t, Upgrades wie Maschinen, funktioniert überall. Gibt O₂ an angrenzende Tanks, Kuppelgeneratoren und Rohre ab. Interner Puffer 2.000 mB. Ein Ladeslot füllt `oxygen_canister` und `large_oxygen_canister` aus diesem Puffer. Bedarf: Kuppel T1 = 1, T2 = 2, T3 = 8 Generatoren (ohne Upgrades). Das deckt nur die Kuppel selbst. Für das Nachfüllen der Anzüge (9.1) kommt 1 weiterer Generator je 2 Spieler hinzu, die regelmäßig draußen arbeiten.

Hinweis Energie: Kuppel T3 plus 8 Generatoren = 1.480 J/t. Das braucht 2 Fusionsreaktoren oder einen Kernreaktor.

### 10.3 Leben unter der Kuppel

- Fackeln, Feuer und Pflanzen funktionieren nur in einer aktiven Kuppel (siehe 8.5).
- Vanilla-Monster mit Atemmaske können die Kuppel betreten.
- Unter einer Kuppel dürfen nur Mitglieder der besitzenden Agentur Blöcke abbauen oder setzen (Kapitel 23).

---

## 11. Forschung

### 11.1 Forschungspunkte (FP)

| Quelle | FP | Häufigkeit |
| --- | --- | --- |
| Probe aus Ring 0 | 1 | |
| Probe aus Ring 1 | 5 | |
| Probe aus Ring 2 | 10 | |
| Sonde erreicht ein neues Ziel | 10 | einmal pro Ziel und Agentur |
| Neue Kreatur mit `field_scanner` analysiert | 15 | einmal pro Kreaturtyp und Agentur, nur die 15 Kreaturen aus Kapitel 18 |
| Erkundungsziel betreten (Struktur aus 13.4) | 20 | pro Strukturinstanz, Abnahme wie Proben pro Strukturtyp |

- **Abnahme:** Pro Probenart bzw. Strukturtyp zählen die ersten 10 voll, danach je 1/10. FP werden intern in **Hundertsteln** (int) gespeichert, Bruchteile werden abgerundet.
- **Forschungslabor** (`research_lab`): Analysatoren, die an ein Forschungslabor angrenzen, geben +50 % FP.
- Der Ring einer Probe folgt aus ihrer `dimension`: Overworld = Ring 0, sonst der `ring` aus dem Planet-Eintrag (25.1). Proben aus Nether, End und Dimensionen ohne Planeten-Eintrag geben keine FP.

### 11.2 Proben

- `sample_tool`: Rechtsklick auf einen Block → 1 `sample` mit `block` = Block-ID und `dimension`. Der Block bleibt. 40 t Abklingzeit. Pro Block-Position nur einmal (gespeichert in Chunk-Daten).
- **Probenart** = Block-ID + Dimension. `deepslate_`-Varianten zählen als dieselbe Art wie die normale Variante.
- **Von Spielern gesetzte Blöcke** sind nie beprobbar: Beim Setzen wird die Position in den Chunk-Daten als „beprobt“ markiert.
- **Beprobbar** sind in der Overworld nur Blöcke im Tag `sternenwerft:sampleable/ring0`, in Planetendimensionen nur Blöcke im Tag `sternenwerft:sampleable/planet`:
  - In Ring 0 nur die 10 Erzarten `bauxite_ore`, `titanium_ore`, `quartz_sand`, Kohle-, Eisen-, Kupfer-, Gold-, Redstone-, Lapis- und Diamanterz (max. 100 FP voll).
  - In Ring 1 und 2 alle natürlichen Blöcke aus 13.4 dieser Dimension, auch dort generierte Vanilla-Blöcke wie `ice`, `basalt`, `magma_block`.
- Ein Rechtsklick auf einen nicht beprobbaren Block zeigt „Keine verwertbare Probe“.

### 11.3 Forschungsterminal

`research_terminal`: GUI mit Forschungsbaum (4 Spalten = Äste, 6 Zeilen = Stufen). Mehrere Knoten derselben Zelle stehen nebeneinander, geordnet nach `position.slot` (25.3). Das GUI scrollt in beide Richtungen. Knoten sind gesperrt, verfügbar (Stufe offen, Voraussetzung erfüllt) oder erforscht. Erforschen zieht die FP der Agentur sofort ab. Hier gründet und verwaltet man auch Agenturen (Kapitel 23).

Ein Knoten schaltet Rezepte frei: Nicht freigeschaltete Rezepte lassen sich im Montagetisch und in Maschinen nicht ausführen und sind in JEI/REI/EMI als gesperrt markiert. Mit `research_required = false` ist alles frei. Ein Knoten schaltet **alle** Rezepte frei, deren Ergebnis eines der genannten Items ist, auch Alternativrezepte. **Items, die in keinem Knoten von 11.4 stehen, sind immer herstellbar** (z. B. `circuit`, `advanced_circuit`, `pressure_hull`, `heat_shield`, Upgrades, Ports, `analyzer`, `research_terminal`, `sample_tool`, `field_scanner`). Ob man sie herstellen kann, hängt dann nur von den Zutaten ab.

### 11.4 Stufen und Knoten

| Stufe | Öffnet bei | Kosten je Knoten |
| --- | --- | --- |
| 1 Raumfahrtprogramm | ein `research_terminal` der Agentur steht | 10 FP |
| 2 Nahe Welten | ein Agenturmitglied hat `moon` betreten | 25 FP |
| 3 Orbit | Agentur hat je einmal `helium` erzeugt (Regolith-Erhitzer) sowie `magnesium_ingot` und `tungsten_ingot` hergestellt | 50 FP |
| 4 Inneres System | ein `zero_g_fabricator` der Agentur hat einen Vorgang abgeschlossen | 80 FP (Ausnahme `advanced_probe`: 20 FP) |
| 5 Äußeres System | ein Agenturmitglied hat `saturn_orbit` erreicht | 120 FP |
| 6 Endgame | Agentur hat `uranium_ingot` hergestellt und eine `antimatter_cell` gefüllt | 250 FP |

Äste: **I** Industrie und Energie, **R** Raumfahrt, **L** Lebenserhaltung, **G** Logistik.

| Knoten-ID | Stufe | Ast | Voraussetzung | Schaltet frei |
| --- | --- | --- | --- | --- |
| `solar_power` | 1 | I | – | `solar_panel` |
| `aluminum_wiring` | 1 | G | – | `aluminum_cable` |
| `gas_storage` | 1 | G | – | `pressure_bottle`, `transport_tank` |
| `launch_pad` | 1 | R | – | `launch_pad`, `launch_tower`, `fuel_pump`, `launch_control` |
| `moon_probe` | 1 | R | `launch_pad`, `solar_power`, `gas_storage` | `probe`, Sondenziel `moon` |
| `rocket_t1` | 1 | R | `moon_probe` | Raketenteile T1 (12.4) |
| `suit_t1` | 1 | L | – | `pressure_suit_*`, `oxygen_canister`, `large_oxygen_canister`, `helmet_lamp` |
| `space_food` | 1 | L | – | `food_packer` |
| `regolith_heater` | 2 | I | – | `regolith_heater` |
| `high_temperature` | 2 | I | – | `high_temperature_furnace`, `ceramic_plate`, `titanium_pipe` |
| `magnesium_alloy` | 2 | I | – | `magnesium_alloy`, `lightweight_plate` |
| `cryogenics` | 2 | I | `regolith_heater` | `cryo_liquefier`, `cooling_system` |
| `planet_probes` | 2 | R | – | Sondenziele `mars`, `venus`, `mars_orbit`, `venus_orbit` |
| `landing_beacon` | 2 | R | – | `landing_beacon`, `aerostat_platform`, `cloud_elevator` |
| `rover` | 2 | R | – | `rover_kit`, Rover-Module (16) außer `rover_lithium_pack` |
| `rocket_t2` | 2 | R | `magnesium_alloy`, `cryogenics`, `high_temperature` | Raketenteile T2 (Startregeln in 12.4) |
| `dome_t1` | 2 | L | – | `dome_generator_t1`, `oxygen_generator`, Kuppelmodule, `electric_lamp` |
| `suit_t2` | 2 | L | `high_temperature`, `cryogenics` | `heat_suit_*`, `cooling_module`, `armor_plating` |
| `dome_t2` | 2 | L | `dome_t1`, `magnesium_alloy`, `cryogenics` | `dome_generator_t2` |
| `hydroponics` | 2 | L | – | `hydroponics_basin`, `compost_soil`, `compost` |
| `gas_tanks` | 2 | G | – | `gas_tank` |
| `fusion` | 3 | I | `superconductors` | `fusion_reactor`, `fusion_casing` |
| `superconductors` | 3 | I | `station_modules` | `superconductor_wire`, `superconductor_cable` |
| `space_weather` | 3 | I | – | `space_weather_station` |
| `station_core` | 3 | R | `dome_t1`, `magnesium_alloy` | `station_core`, `station_hull`, `station_window`, `docking_bay`, `magnet_boots` |
| `station_modules` | 3 | R | `station_core` | `habitation_module`, `research_lab`, `zero_g_fabricator`, `solar_wing` |
| `lander` | 3 | R | `station_core` | `lander` |
| `gravity` | 3 | L | `fusion` | `gravity_plate` |
| `greenhouse` | 3 | L | `station_modules`, `hydroponics` | `greenhouse` |
| `tank_depot` | 3 | G | – | `tank_depot` |
| `lithium_storage` | 4 | I | – | `lithium_battery`, `lithium_battery_bank`, `rover_lithium_pack` |
| `gas_collection` | 4 | I | – | `gas_collector` |
| `magnetic_trap` | 4 | I | `gas_collection`, `iridium_chips` | `magnetic_trap`, `antimatter_cell` |
| `advanced_probe` | 4 | R | – | `advanced_probe`, Sondenziele: alle Ziele mit Ring 2 aus 12.1 |
| `asteroid_anchor` | 4 | R | – | `asteroid_anchor` |
| `long_range` | 4 | R | `lithium_storage`, `iridium_chips` | `long_range_module` |
| `dome_t3` | 4 | L | `dome_t2`, `iridium_chips` | `dome_generator_t3` |
| `eva` | 4 | L | `lithium_storage` | `jetpack`, `heat_reflector` |
| `radiation_shield` | 4 | L | – | `radiation_shield` |
| `ice_drilling` | 4 | L | `lithium_storage` | `ice_drill`, `diving_module` |
| `clone_chamber` | 4 | L | `iridium_chips` | `clone_chamber` |
| `iridium_chips` | 4 | G | – | `iridium_chip` |
| `fission` | 5 | I | `iridium_chips` | `fission_reactor`, `fission_casing`, `fuel_rod` |
| `pump_station` | 5 | I | – | `pump_station`, `pontoon` |
| `drilling_rig` | 5 | I | – | `drilling_rig` |
| `atomic_drive` | 5 | R | `fission` | `atomic_drive` |
| `suit_t3` | 5 | L | `suit_t2`, `superconductors`, `lithium_storage` | `deep_space_suit_*`, `insulation_fabric`, `heating_module` |
| `fertilizer` | 5 | L | `pump_station` | `ammonia_fertilizer` |
| `cargo_logistics` | 5 | G | `iridium_chips`, `lithium_storage` | `cargo_terminal`, `cargo_rocket`, `logistics_computer`, `trading_post` |
| `antimatter_drive` | 6 | R | `magnetic_trap`, `fission` | `antimatter_drive` |
| `orbital_shipyard` | 6 | R | `antimatter_drive` | `shipyard_terminal`, `shipyard_dock` |
| `heavy_freighter` | 6 | G | `cargo_logistics`, `atomic_drive` | `heavy_freighter` |

---

## 12. Raumfahrt

### 12.1 Ziele und Raketenstufen

| Ziel-ID | Ring | DP | Erreichbar mit | Landung kostet Treibstoff |
| --- | --- | --- | --- | --- |
| `minecraft:overworld` (Erde) | 0 | 0 | alle | nein (Fallschirm) |
| `earth_orbit` | 0 | 0 | T2 | nein |
| `moon` | 1 | 0 | T1 | ja |
| `venus_orbit` | 1 | 1 | T2 | nein |
| `venus` | 1 | 1 | T1 | nein (Wolkenschicht) |
| `mars_orbit` | 1 | 1,5 | T2 | nein |
| `mars` | 1 | 1,5 | T1 | ja |
| `mercury_orbit` | 2 | 2 | T2 | nein |
| `mercury` | 2 | 2 | T2 | ja |
| `asteroid_belt` | 2 | 3 | T2 | nein |
| `ceres` | 2 | 3 | T2 | ja |
| `jupiter_orbit` | 2 | 5 | T2 | nein |
| `io`, `europa` | 2 | 5 | T2 | ja |
| `saturn_orbit` | 2 | 8 | T2 + `long_range_module` | nein |
| `titan` | 2 | 8 | T2 + Modul | nein (Fallschirm) |
| `enceladus` | 2 | 8 | T2 + Modul | ja |
| `uranus_orbit` | 2 | 12 | T2 + Modul | nein |
| `neptune_orbit` | 2 | 15 | T2 + Modul | nein |
| `triton` | 2 | 15 | T2 + Modul | ja |
| `pluto` | 2 | 18 | T2 + Modul | ja |

Ein Ziel ist wählbar, wenn (1) die Agentur dort eine Sonde ankommen ließ, (2) die Rakete die Stufe hat und (3) der Treibstoff reicht. Erde und Erdorbit sind immer bekannt.

### 12.2 Treibstoffformel

Kosten in mB **je Treibstoffkomponente** (also gleich viel Brennstoff und Sauerstoff):

```
kosten(A → B) = start(A) + transfer(A, B) + landung(B)
start(A)      = 32.000 × g(A)              (Orbit: 0; Venus aus der Wolkenschicht: 16.000)
transfer(A,B) = 6.000 × |DP(A) − DP(B)| + (A ≠ B ? 2.000 : 0)
landung(B)    = 0,25 × 32.000 × g(B)        (0, wenn Spalte „Landung kostet“ = nein)
```

Beispiele: Erde → Mond = 32.000 + 2.000 + 1.360 = 35.360 mB. Mond → Erde = 5.440 + 2.000 + 0 = 7.440 mB.

**Tankgrößen je Komponente:** T1 3–5 Tanks = 48.000–80.000 mB. T2 4–8 Tanks = 64.000–128.000 mB, mit `long_range_module` +128.000 mB (max. 256.000 mB).

**Hin- und Rückflug (Summe je Komponente, nach der Formel oben):**

| Ziel | ab Erdoberfläche | ab Station im `earth_orbit` | Mindestausstattung ab Erde |
| --- | --- | --- | --- |
| Mond | 42.800 | 10.800 | T1, 3 Tanks |
| Venus (Wolkenschicht) | 64.000 | 32.000 | T1, 5 Tanks (4 Tanks ohne Reserve) |
| Mars | 69.200 | 37.200 | T1, 5 Tanks |
| Merkur | 75.200 | 43.200 | T2, 5 Tanks |
| Asteroidengürtel / Ceres | 72.960 / 73.200 | 40.960 / 41.200 | T2, 5 Tanks |
| Jupiter-Orbit | 96.000 | 64.000 | T2, 7 Tanks (6 Tanks ohne Reserve) |
| Io / Europa | 103.200 / 101.200 | 71.200 / 69.200 | T2, 7 Tanks |
| Saturn-Orbit / Titan / Enceladus | 132.000 / 136.480 / 132.400 | 100.000 / 104.480 / 100.400 | T2 + Modul, 4 Tanks |
| Uranus-Orbit | 180.000 | 148.000 | T2 + Modul, 4 Tanks |
| Neptun-Orbit / Triton | 216.000 / 219.200 | 184.000 / 187.200 | T2 + Modul, 6 Tanks |
| Pluto | 254.400 | 222.400 | T2 + Modul, 8 Tanks (ab Station: 6 Tanks) |

**Nachtanken und Rückflug:**

- Der Raketen-Computer zeigt jedes Ziel **grün** (Hin- und Rückflug reichen), **gelb** (nur Hinflug, Warnung „Rückflug nur mit Nachtanken“) oder **grau** (Hinflug reicht nicht, nicht wählbar).
- Nachtanken ist möglich:
  - über das Slotpaar im Raketen-GUI (4.1) oder den Knopf „Aus Frachtraum tanken“, der passende `transport_tank` aus dem Frachtraum leert,
  - an einer `docking_bay` aus angrenzenden Tanks,
  - an einer `fuel_pump` im Umkreis von 3 Blöcken um das gelandete Raketen-Entity.
- Ab Stufe 3 ist die Station im Erdorbit das vorgesehene Drehkreuz: Start von der Station spart 32.000 mB je Komponente.

### 12.3 Sonde

| ID | Stufe | Ziele | Flugzeit |
| --- | --- | --- | --- |
| `probe` | 1 (Mond), 2 (Mars, Venus) | Ring 1 | 1.200 + 1.200 × DP Ticks |
| `advanced_probe` | 4 | alle Ziele in Ring 2 | 1.200 + 1.200 × DP Ticks |

Ablauf: Sonde als Block in die Mitte der Startplattform setzen → in `launch_control` Ziel wählen → Start (Countdown 200 t, Sonde fliegt als Entity nach oben und verschwindet). Nach der Flugzeit: Ziel für die Agentur freigeschaltet, +10 FP, Nachricht an die Agentur, Atlas-Eintrag. Eine Sonde kehrt nie zurück. Orbit-Ziele brauchen eine eigene Sonde.

### 12.4 Raketenbau

Eine Rakete wird aus **Raketenblöcken** auf der Startplattform gebaut. Alle Teile sind 1 Block groß.

**Aufbau (von unten nach oben, über der Plattformmitte):**

1. Triebwerk direkt auf dem mittleren `launch_pad`
2. 4 Flossen waagerecht neben dem Triebwerk (N, O, S, W)
3. Treibstofftanks
4. optional Frachtraum
5. optional `long_range_module` (nur T2, direkt unter der Kapsel)
6. Kapsel ganz oben

| Teil | T1-ID | T2-ID | Masse (t) | Anzahl T1 | Anzahl T2 |
| --- | --- | --- | --- | --- | --- |
| Kapsel | `rocket_capsule_t1` | `rocket_capsule_t2` | 4 / 5 | 1 | 1 |
| Tank (16.000 mB + 16.000 mB) | `rocket_tank_t1` | `rocket_tank_t2` | 2 / 1,5 leer, +2 voll | 3–5 | 4–8 |
| Triebwerk | `rocket_engine_t1` | `rocket_engine_t2` | 3 / 4 | 1 | 1 |
| Flossen | `rocket_fin_t1` | `rocket_fin_t2` | 0,5 | 4 | 4 |
| Frachtraum (27 Slots) | `rocket_cargo_bay` | `rocket_cargo_bay` | 1 + 0,05 je belegtem Slot | 0–1 | 0–2 |
| Fernflug-Modul (+128.000 mB je Komponente) | – | `long_range_module` | 8 voll | – | 0–1 |

- Schub: T1 = 30, T2 = 48. **Start nur bei Schub / (Masse × g(Start)) ≥ 1,0.** Orbit: immer erlaubt.
  - Beispiel T1: 4 Tanks + voller Frachtraum = 27,35 t → 1,10 ✓. 5 Tanks + voller Frachtraum = 31,35 t → 0,96 ✗ (5 Tanks nur ohne oder mit leerem Frachtraum).
  - Beispiel T2: 6 Tanks + Modul + 2 volle Frachträume = 44,7 t → 1,07 ✓. 8 Tanks + Modul = 47 t → 1,02 ✓, mit Fracht ✗.
- T1 startet von der 5×5-Plattform (`launch_pad_t1`), T2 nur von der 7×7-Plattform (`launch_pad_t2`), einer `docking_bay` oder als gelandetes Raketen-Entity.
- Treibstoff T1: `kerosene` + `oxygen`. T2: `hydrogen` + `oxygen`.
- Stufen dürfen nicht gemischt werden (außer Frachtraum).
- **Raketen-Computer** (GUI der Kapsel und von `launch_control`): zeigt Teileliste, Masse, Schub-Gewicht-Verhältnis, Treibstoff, erreichbare Ziele (nach 12.1/12.2).
- **Checkliste** in `launch_control`: Struktur gültig ✓, Schub ≥ Gewicht ✓, Treibstoff für mindestens ein Ziel ✓, mindestens ein Ziel freigeschaltet ✓, Spieler mit vollständigem Anzug sitzt in der Kapsel ✓.
- **Betanken:** `fuel_pump` zieht aus angrenzenden Tanks und Rohren und verteilt gleichmäßig auf alle Raketentanks.

### 12.5 Flugablauf

1. Spieler sitzt in der Kapsel, startet über `launch_control` oder das Kapsel-GUI.
2. **Server:** prüft die Checkliste, wandelt die Raketenblöcke in ein `rocket`-Entity um (Data: Teileliste, Treibstoff, Fracht, Stufe), entfernt die Blöcke.
3. **Client:** Countdown 200 t mit Ton, Aufstieg 600 t mit Kamera von außen, Bildschirmbeben.
4. **Navigationskarte** (GUI `star_map`; nicht verwechseln mit dem Item `star_chart`): Ziele mit Treibstoffkosten, gesperrte Ziele grau. Auswahl des Landeorts: Landefeuer der Agentur in dieser Dimension oder Koordinaten X/Z (Standard: Spawn der Dimension).
5. **Server:** zieht den Treibstoff ab, teleportiert Rakete und Passagiere in die Zieldimension 200 Blöcke über dem Landepunkt. Landung in 200 t auf dem obersten festen Block (Landefeuer: innerhalb 3 Blöcken).
6. Danach steht das `rocket`-Entity am Boden. Rechtsklick öffnet das Raketen-GUI (Treibstoff aus `transport_tank`, Fracht, Start). Sneak-Rechtsklick mit leerer Hand zerlegt die Rakete in ihre Teil-Items (Treibstoff geht verloren).
**Sonderfälle (MUSS):**

- **Reihenfolge:** Ziel und Landeort wählt man **vor** dem Start. Die Navigationskarte öffnet sich beim Klick auf „Start“, danach laufen Countdown, Aufstieg und Landung ohne Eingabe.
- **Mehrere Insassen:** Ziel und Landeort wählt der Spieler, der „Start“ drückt.
- **Disconnect, Tod oder Schließen des Bildschirms während des Flugs:** Der Server schließt den Flug sofort ab. Rakete und verbliebene Insassen landen am gewählten Ort; ein abgemeldeter Spieler erscheint beim nächsten Login in der Rakete.
- **Sitzen:** Rechtsklick auf den Kapselblock setzt den Spieler auf ein unsichtbares Sitz-Entity. Nach der Umwandlung sitzt er im `rocket`-Entity.
- **Landepunkt:**
  - Gesucht wird spiralförmig im Radius 32 um die gewählte X/Z-Position ein fester Block ohne Fluid darüber mit 3 Luftblöcken über sich.
  - Findet sich keiner (Lava, Methan, Wasser), entsteht eine 3×3-Plattform aus dem Gestein des Planeten auf der Fluidoberfläche.
  - Venus: immer in der Wolkenschicht (12.8); Landefeuer auf der Venus gelten nur in der Wolkenschicht.
- **Neustart:** Ein gelandetes `rocket`-Entity darf überall erneut starten (ohne Rampe).
- **Besitz:** Das `rocket`-Entity gehört der Agentur des Spielers, der den Start ausgelöst hat (23.2).
- **Mehrere Rampen:** `launch_control` steuert genau die nächstgelegene gültige Rampe im Umkreis von 8 Blöcken.
- **Asteroidengürtel:** Es gibt keinen Boden. Gelandet wird auf dem höchsten Asteroidenblock der Spalte; gibt es keinen, schwebt die Rakete auf y 100 wie im Orbit (Schritt 7).

7. **Orbit ohne `docking_bay` als Ziel:** Die Rakete schwebt auf y 100 an der gewählten X/Z-Position (Standard 0/0). Ist dort keine freie Säule von 5×5×12 Blöcken oder liegt ein fremder `station_core` näher als 64 Blöcke, sucht der Server spiralförmig bis Radius 256 weiter. Klickt der Spieler in der Kapsel mit einem `station_core` in der Hand, wird der Kern direkt unter die Rakete gesetzt. Die 5×5-Plattform entsteht, die Rakete steht darauf, und der Spieler kann sicher aussteigen.

### 12.6 Landefeuer

`landing_beacon`: Block, Rechtsklick → Namen vergeben. Erscheint in Atlas und Landeortwahl der Agentur. Rotes Leuchtsignal bis Höhe 256.

### 12.7 Landefähre

`lander`: Entity, 2 Sitze, Tank 32.000 mB je Komponente (`hydrogen` + `oxygen`). Fliegt nur zwischen einer Orbit-Dimension und den Körpern darunter (Erdorbit ↔ Erde/Mond, Jupiter-Orbit ↔ Io/Europa, Saturn-Orbit ↔ Titan/Enceladus, Neptun-Orbit ↔ Triton, sonst Planetenorbit ↔ Planet). Kosten nach 12.2 ohne `transfer`. Start von der Erdoberfläche ist verboten. Das Ziel wählt man in der Navigationskarte, die nur die erlaubten Ziele zeigt. Sonst gilt der Flugablauf aus 12.5. Das `lander`-Item lässt sich nur in Orbit-Dimensionen oder auf einer `docking_bay` setzen (Rechtsklick wie ein Boot).

### 12.8 Venus-Wolkenschicht

- Die Wolkenschicht ist y 180–240 in `venus`. Dort: 20 °C, Druck `normal`, Atmosphäre `toxic` (nicht atembar).
- Landet eine Rakete auf der Venus und es gibt am Landepunkt keinen festen Block zwischen y 180 und 240, erzeugt der Server dort einmalig eine 5×5-Plattform aus `aerostat_platform` auf y 200.
- `aerostat_platform` ist ein normaler Block, der in der Luft gesetzt werden darf, wenn er an einen anderen `aerostat_platform` angrenzt.
- `cloud_elevator`: Rechtsklick auf die obere Station → nach 100 t Teleport auf die Oberfläche: erster fester Block unter y 180 an gleicher X/Z, der kein Fluid ist und 2 Luftblöcke über sich hat. Liegt an dieser Stelle Lava, entsteht eine 3×3-Plattform aus `venus_rock` auf der Lavaoberfläche. Dort entsteht automatisch eine verknüpfte untere Station. Rechtsklick unten → zurück.

### 12.9 Schwerkraft

Beim Betreten einer Dimension und alle 10 t:

- `minecraft:gravity` = 0,08 × g
- `minecraft:fall_damage_multiplier` = g
- `minecraft:safe_fall_distance` = 3 / max(g, 0,05)
- Umsetzung als `AttributeModifier` mit fester ID `sternenwerft:planet_gravity` (nie den Basiswert ändern), damit andere Mods ihre eigenen Modifikatoren behalten.
- In Orbit-Dimensionen g = 0,005 (fast schwerelos). Mit `magnet_boots` auf festem Grund oder über einer `gravity_plate`: g = 1.
- Rover und Raketen-Entities nutzen dasselbe g.

---

## 13. Planeten

### 13.1 Planeten-Registry

Datenpaket-Registry `sternenwerft:planet` (Schema in 25.1). Jede Planetendimension ist eine Datenpaket-Dimension mit eigenem `dimension_type` und eigenen Noise-Settings.

**Gemeinsame Dimensionswerte (alle Planeten):** `min_y −64`, `height 384`, `bed_works true`, `respawn_anchor_works false`, `has_raids false`, `natural false`, `piglin_safe false`, Tag-Nacht-Zyklus wie Overworld (`fixed_time` nur wo angegeben). Noise-Settings: Kopie von `minecraft:overworld` **aus der Vanilla-Version 26.3** mit den Änderungen aus 13.4, `aquifers_enabled false`, `ore_veins_enabled false`, eigener `default_block`, `default_fluid` nach Tabelle. Die Oberflächenblöcke je Biom (Spalte „Oberfläche“) werden als **Material Rules** umgesetzt (26.3-Bezeichnung für die früheren Surface Rules). Erze und Strukturen sind Features im 26.3-Format (`worldgen/feature` + `worldgen/placed_feature`).

**Biome und Großformen (Umsetzungsregeln):**

- **Gelände:** Der Noise-Router der Overworld bleibt unverändert, Planeten unterscheiden sich über Blöcke, Biome, Carver und Strukturen. **Ausnahme Venus:** Eine Material Rule setzt alle Blöcke ab y 176 auf Luft, damit die Wolkenschicht (y 180–240) frei bleibt. Alle anderen Höhenangaben in 13.4 (z. B. Venus-Hochebenen y 90–110, Europa-Meeresboden) sind ungefähre Zielwerte, keine Pflicht.
- **Biomzuordnung** über `multi_noise`:
  - Bergige oder hohe Biome (Hochland, Vulkane, Gebirge, Hochebenen): `erosion < −0,2`.
  - Flache Biome (Mare, Ebenen, Seen, Dünen, Eisebenen): `erosion > 0,45`.
  - Übrige: dazwischen.
  - Seltene Biome (Polarkrater, Polkappe, Meteoritenfeld, Schattenkrater, Geysirfelder): `temperature < −0,8` oder `weirdness > 0,8`, ergibt ca. 5–10 % Fläche.
- **Eigene `BiomeSource`:**
  - Merkur: Biom nach X-Koordinate (13.4), Schattenkrater als seltene Einsprengsel.
  - Pluto: `pluto_heart_glacier` im Radius 400 um (0, 0), außerhalb `multi_noise`.
- **Europa:** Noise-Settings mit Meeresspiegel y 120 und `default_fluid` Wasser. Ein Feature `ice_shell` füllt y 112–124 mit `europa_ice`, außer in `europa_rifts` (Spalten 3–6 Blöcke breit).
- **Großformen als Strukturen** (nicht als Features, wegen der Chunk-Grenzen):
  - `great_crater` auf dem Mond (Radius 40–60, Zentralberg, Erze ×2 innerhalb der Struktur), `giant_volcano` auf dem Mars (Radius 80, Höhe bis y 250), `large_asteroid` im Asteroidengürtel (Radius 13–20, dazu der Spawn-Asteroid).
  - `lava_tube`, `volcanic_vent`, `meteorite_field`, `hydrothermal_field` (Quellen-Cluster), `ice_volcano`, `signal_transmitter`.
- **Erkundungsziele** sind genau die Strukturen `lava_tube`, `volcanic_vent`, `meteorite_field`, `hydrothermal_field`, `ice_volcano` und `signal_transmitter`; ihre Strukturinstanz ist die Erkundungsziel-ID (11.1). Großkrater und Riesenvulkan zählen nicht.
- **Mars-Canyon:** Vanilla-Canyon-Carver mit Tiefe bis y 0 und doppelter Breite.

**Orbit-Dimensionen** (`earth_orbit`, `venus_orbit`, `mars_orbit`, `mercury_orbit`, `jupiter_orbit`, `saturn_orbit`, `uranus_orbit`, `neptune_orbit`): Leere Welt (`minecraft:flat` mit leerer Schichtliste), `min_y 0`, `height 256`, `fixed_time 6000`, Himmel schwarz, Planet groß unter dem Spieler (Sky-Renderer zeichnet ihn auch unterhalb des Horizonts). Temperatur −100 bis +100 °C, Strahlung 1 (Jupiter-Orbit 3), Atmosphäre `none`. Solarfaktor im Orbit (ohne Atmosphäre): Erde 1,0, Venus 2,0, Merkur 3,0, Mars 0,5, Jupiter 0,1, Saturn 0,05, Uranus und Neptun 0,02.

### 13.2 Werte je Planet

| ID | g | Atmosphäre | Druck | Temp. Nacht / Tag (°C) | Strahlung | Solarfaktor | Wasser | Himmel / Nebel |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Erde (Overworld) | 1,0 | atembar | normal | Vanilla | 0 | 1,0 | normal | Vanilla |
| `moon` | 0,17 | none | vacuum | −170 / +120 | 1 | 1,0 | freeze | schwarz, Sterne auch am Tag, Erde groß |
| `mars` | 0,38 | thin | low | −120 / +20 | 1 | 0,5 | freeze | gelblich-braun, Sonnenuntergang blau, Phobos und Deimos |
| `venus` | 0,9 | toxic | high (Wolkenschicht: normal) | +465 / +465 (Wolkenschicht 20) | 1 | 0,1 (Wolkenschicht 2,0) | evaporate | dichter orange-gelber Nebel, Sichtweite 24 Blöcke am Boden |
| `mercury` | 0,38 | none | vacuum | biomabhängig (13.4) | 2 | 3,0 | evaporate | schwarz, Sonne 3× so groß, `fixed_time 6000` |
| `asteroid_belt` | 0,03 | none | vacuum | −100 / −100 | 1 | 0,25 | freeze | schwarz, Sonne klein |
| `ceres` | 0,03 | none | vacuum | −110 / −90 | 1 | 0,25 | freeze | schwarz, Sonne klein |
| `io` | 0,18 | none | vacuum | −140 / −140 (an Lava +300) | 3 | 0,1 | freeze | schwarz, Jupiter riesig |
| `europa` | 0,13 | none | vacuum (Ozean: normal) | −160 / −160 (Ozean +2) | 2 (Ozean 0) | 0,1 | freeze | schwarz, Jupiter riesig |
| `titan` | 0,14 | dense | normal | −180 / −180 | 1 | 0,05 | freeze | dichter oranger Nebel, Sichtweite 48 |
| `enceladus` | 0,01 | none | vacuum | −200 / −200 | 1 | 0,05 | freeze | Saturn mit Ringen |
| `triton` | 0,08 | thin | low | −235 / −235 | 1 | 0,02 | freeze | Neptun blau |
| `pluto` | 0,06 | thin | low | −230 / −230 | 1 | 0,02 | freeze | dämmrig, Sonne als heller Stern |

Gasplaneten (Jupiter, Saturn, Uranus, Neptun) haben nur Orbit-Dimensionen.

**Mindestausrüstung** (folgt aus 9.1, Grenzwerte inklusiv):

| Ort | Mindestens |
| --- | --- |
| Mond (ohne Polarkrater), Mars, Ceres, Asteroidengürtel, Titan | T1 |
| Mond-Polarkrater (−190 °C), Venus-Oberfläche, Enceladus (−200 °C) | T2 |
| Merkur: Dämmerungsgürtel, Frostebenen, Schattenkrater | T1 + `radiation_shield` oder T3 |
| Merkur: Glutebenen (+430 °C) | T2 + `radiation_shield` oder T3 |
| Io | T1 + `radiation_shield`; an Lava (+300 °C) T2 + `radiation_shield` |
| Europa | T1 + `radiation_shield` oder T3; im Ozean unter y 80 zusätzlich `diving_module` |
| Pluto (−230 °C) | T2 + `heating_module` |
| Triton (−235 °C) | T3 + `heating_module` |
| Venus-Wolkenschicht, Orbits außer Jupiter | T1 |
| Jupiter-Orbit | beliebiger Anzug + `radiation_shield` |

Sonnenstürme heben die Strahlung um 2 (Maximum 3, siehe 8.1). Dagegen schützen `radiation_shield`, Kuppel T3 oder 3 Blöcke Deckung.

### 13.3 Gas-Sammler je Orbit

| Orbit | Gas | Menge |
| --- | --- | --- |
| `jupiter_orbit` | `hydrogen` | 100 mB / 20 t |
| `saturn_orbit` | `helium` | 100 mB / 20 t |
| `uranus_orbit`, `neptune_orbit` | `methane` (flüssig) oder `hydrogen`, umschaltbar per Knopf im GUI, Standard `methane` | 100 mB / 20 t |

### 13.4 Biome, Blöcke, Erze, Strukturen

**Erze-Notation:** `Block` (y-Bereich, Adergröße, Adern je Chunk).

**Drops (Pauschalregel, gilt wo nichts anderes steht):**
- `*_<metall>_ore` → 1 `raw_<metall>` (Glück wie bei Vanilla-Erzen).
- `bauxite_ore` und `*_aluminum_ore` → 1 `bauxite`.
- `*_sulfur_ore` → 2–4 `sulfur`.
- `deepslate_*`-Varianten droppen wie die normale Variante.
- Planetengestein und Böden (`*_rock`, `*_crust`, `*_ice`, `*_sand`, `*_regolith`, `*_snow`, `*_tholin`, `*_basalt`, `*_seabed`) droppen sich selbst.
- Unzerstörbar (wie Grundgestein): `hydrothermal_vent`, `ammonia_geyser`, `nitrogen_geyser`, `transmitter_core`, `ancient_alloy`.

#### Erde (Ergänzungen zur Overworld)

| Element | Werte |
| --- | --- |
| `bauxite_ore` | in Savanne, Wüste, Badlands: y 50–120, Ader 12, 6 je Chunk |
| `titanium_ore` / `deepslate_titanium_ore` | alle Biome: y −64–0, Ader 6, 4 je Chunk |
| `quartz_sand` | Strand und Flussufer: Scheibe wie Ton, Radius 2–4, 2 je Chunk |
| Ölblase | Wüste und Ozeanboden: Kugel Radius 3–5 aus `crude_oil`-Quellblöcken, y −20–40, 1 je 8 Chunks |

#### Mond `moon`

Blöcke: `moon_regolith` (Oberfläche, 3 Schichten), `moon_rock` (Gestein = `default_block`), `moon_basalt`, `moon_ice`. `default_fluid` = Luft.

| Biom-ID | Oberfläche | Besonderheit | Erze |
| --- | --- | --- | --- |
| `moon_highlands` | `moon_regolith` | zerklüftet, höhere Berge | `moon_titanium_ore` (y 0–100, 6, 3) |
| `moon_mare` | `moon_basalt` | flach, dunkel | `moon_aluminum_ore` (y 0–80, 8, 5), `moon_titanium_ore` (y 0–80, 6, 4) |
| `moon_great_crater` | `moon_regolith` | Struktur `great_crater` erscheint bevorzugt in diesem Biom | Erze ×2 nur innerhalb der Struktur |
| `moon_polar_crater` | `moon_regolith` + `moon_ice` | selten (5 %), Temperatur fest −190 (braucht Anzug T2), Licht 0 | `moon_ice` (Ader 16) |

Strukturen: `lava_tube` (Mare: Tunnel 5×5, Länge 60–120, y 30–50, 1 je 16 Chunks; zählt als Erkundungsziel). Kleine Krater (Radius 4–12) in allen Biomen.
`moon_aluminum_ore` droppt `bauxite`, `moon_titanium_ore` droppt `raw_titanium`, `moon_ice` droppt `minecraft:ice`.

#### Mars `mars`

Blöcke: `mars_sand`, `mars_rock` (`default_block`), `mars_magnesium_ore`, `mars_iron_ore`, `gypsum_block`, `dry_ice_block`, `iron_nickel_meteorite`.

| Biom-ID | Oberfläche | Besonderheit | Erze |
| --- | --- | --- | --- |
| `mars_red_plains` | `mars_sand` | flach bis hügelig | `mars_magnesium_ore` (y 0–80, 8, 5), `mars_iron_ore` (y 0–80, 8, 4) |
| `mars_canyon` | `mars_rock` | tiefe Schluchten bis y 0, Erzschichten freiliegend | Erze ×2 |
| `mars_volcanic_highlands` | `mars_rock` | hohe Berge, eine Riesenvulkan-Struktur je 64 Chunks | – |
| `mars_dry_lake` | `gypsum_block` | flache Senke | `gypsum_block` Oberfläche |
| `mars_polar_cap` | `dry_ice_block` | selten | – |
| `mars_meteorite_field` | `mars_sand` | Meteoriten-Feature: kleine Krater mit `iron_nickel_meteorite` | – |

Strukturen: `lava_tube` (Hochland), `meteorite_field` (Struktur im Biom `mars_meteorite_field`, 1 je 8 Chunks). Drops: `iron_nickel_meteorite` → 1–2 `raw_nickel`, 50 % 1 `raw_iron`. `gypsum_block` → 4 `gypsum`. `dry_ice_block` → 4 `dry_ice`.

#### Venus `venus`

Blöcke: `venus_rock` (`default_block`), `venus_tungsten_ore`, `venus_sulfur_ore`, Vanilla `basalt`, `magma_block`, Lava (`default_fluid` Lava, Meeresspiegel y 40).

| Biom-ID | Oberfläche | Besonderheit | Erze |
| --- | --- | --- | --- |
| `venus_lava_plains` | Vanilla `basalt` | Lavaseen | `venus_sulfur_ore` (y 40–100, 8, 4) |
| `venus_volcanoes` | `venus_rock`, `magma_block` | Vulkankegel mit Lavasee | `venus_tungsten_ore` (y 40–140, 6, 5) |
| `venus_plateau` | `venus_rock` | flache Hochebenen y 90–110 | – |

Strukturen: `volcanic_vent` (Erkundungsziel, an Vulkanen). Wolkenschicht y 180–240: keine Terrain-Generierung, dichter Nebel.

#### Merkur `mercury`

Blöcke: `mercury_rock`, `mercury_lithium_salt`, `mercury_iron_ore`, `mercury_titanium_ore`.

| Biom-ID | Temperatur | Erze |
| --- | --- | --- |
| `mercury_scorched_plains` (Tagseite) | +430 | `mercury_titanium_ore` |
| `mercury_frost_plains` (Nachtseite) | −180 | `mercury_iron_ore` |
| `mercury_twilight_belt` | +20 | `mercury_lithium_salt` als Oberflächenkruste (Scheibe Radius 3–6, 3 je Chunk) |
| `mercury_shadow_crater` | −150 | `minecraft:ice` |

Biomverteilung über eine eigene `BiomeSource` nach X-Koordinate (13.1): Tagseite für X > 2.000, Nachtseite für X < −2.000, Dämmerungsgürtel dazwischen. Spawn bei (0, 0). `mercury_lithium_salt` → 2 `lithium_salt`.

#### Asteroidengürtel `asteroid_belt`

Leere Dimension. Feature `asteroid`: Kugeln/Ellipsoide Radius 3–12 (größere als Struktur `large_asteroid`, 13.1) aus `asteroid_rock` mit Erzen (`asteroid_platinum_ore` 4 %, `asteroid_iridium_ore` 2 %, `asteroid_nickel_ore` 6 % der Blöcke), y 20–220, 1 je Chunk mit 40 % Chance. `asteroid_anchor`: Block auf einem Asteroiden setzt dort 1 g für Spieler im Radius 16 (Pseudo-Schwerkraft zum Basisbau).
**Spawn:** Ein garantierter Asteroid mit Radius 15 bei (0, 80, 0) mit allen drei Erzen. Fällt ein Spieler unter y 0, wird er mit Langsamem Fall (400 t) auf diesen Asteroiden zurückgesetzt.

#### Ceres `ceres`

Blöcke: `ceres_rock`, `salt_crust` (Oberflächenflecken), `minecraft:ice`. Biome: `ceres_plains`, `ceres_salt_flats`. Struktur: `ice_volcano` (Kegel aus Eis, 1 je Dimension nahe Spawn, Erkundungsziel).

#### Io `io`

Blöcke: `io_crust`, `io_sulfur_ore`, Lava. Biome: `io_sulfur_plains`, `io_lava_lakes`, `io_volcanic_fields`. Struktur `volcanic_vent`. Ereignis Vulkanausbruch (19).

#### Europa `europa`

Blöcke: `europa_ice` (Härte 50, nur mit `ice_drill` effizient abbaubar), `europa_seabed`, `hydrothermal_vent`. Aufbau: Eisdecke y 112–124, darunter Wasser bis Meeresboden y −40 bis 0.

| Biom-ID | Besonderheit |
| --- | --- |
| `europa_ice_plains` | geschlossene Eisdecke |
| `europa_rifts` | Spalten mit Zugang zum Wasser |
| `europa_ocean` | Unterwasserbiom unter der Eisdecke, Tiefseedruck unter y 80 |
| `europa_vents` | Meeresboden mit `hydrothermal_vent`-Clustern (Struktur `hydrothermal_field`, Erkundungsziel) |

`hydrothermal_vent`: Rechtsklick mit einer leeren Glasflasche (`minecraft:glass_bottle`, wird verbraucht) gibt 1 `microbe_culture` (Abklingzeit 1.200 t pro Block). Das ist unabhängig von Proben (11.2), dort gilt weiter „pro Block-Position einmal“.
`ice_drill`: Werkzeug, Rechtsklick gedrückt halten bohrt einen 3×3-Schacht nach unten (1 Schicht / 10 t, 20 J pro Block aus internem Akku 100.000 J).

#### Titan `titan`

Blöcke: `titan_dune_sand`, `titan_ice_rock`, `methane`-Quellblöcke (Meeresspiegel y 62, `default_fluid` = `methane`). Biome: `titan_methane_lakes`, `titan_dunes`, `titan_ice_mountains`. Die dichte Luft erlaubt Elytra-Gleiten ohne Feuerwerk (Sinkrate halbiert).

#### Enceladus `enceladus`

Blöcke: `enceladus_snow`, `enceladus_ice`, `ammonia_geyser`. Biome: `enceladus_snowfields`, `enceladus_tiger_stripes` (blaue Streifen, Geysire 1 je Chunk). Geysir: alle 200 t Fontäne 40 Blöcke hoch. Entities darin bekommen Geschwindigkeit +2,0 nach oben.

#### Triton `triton`

Blöcke: `triton_ice` (rosa-grau), `nitrogen_geyser`. Biome: `triton_cantaloupe_terrain` (wellige Hügel), `triton_polar_cap`, `triton_geyser_fields`.

#### Pluto `pluto`

Blöcke: `pluto_nitrogen_ice`, `pluto_water_ice`, `pluto_tholin`, `pluto_uranium_ore` (nur in `pluto_dark_highlands`, y 0–60, Ader 4, 3 je Chunk). Biome: `pluto_heart_glacier`, `pluto_ice_mountains`, `pluto_dark_highlands`.
Struktur **`signal_transmitter`**: genau eine, unter dem Herzgletscher bei (0, 30, 0) ± 64. Halle 32×32×16 aus `ancient_alloy` (unzerstörbar wie Grundgestein), in der Mitte `transmitter_core`. **Zugang:** eine 3 breite, 4 hohe Öffnung in der Südwand. Von dort führt ein Treppentunnel (3 breit, 4 hoch) aus `pluto_water_ice` zur Oberfläche, oben markiert durch einen 5×5-Ring aus `ancient_alloy`. Rechtsklick auf den Kern beschwört `signal_sentinel`, wenn für die Agentur keiner lebt und keiner in den letzten 24.000 t besiegt wurde.

### 13.5 Monster mit Atemmaske

In Planetendimensionen spawnen diese Vanilla-Monster nachts bzw. bei Licht ≤ 7 mit einer Atemmasken-Render-Ebene (Kopf-Overlay):

| Dimension | Monster | Häufigkeit |
| --- | --- | --- |
| `moon` | Zombie, Skelett, Spinne, Creeper | 50 % Overworld |
| `mars` | Zombie, Skelett, Spinne, Creeper | 100 %, im Staubsturm 200 % |
| `venus` (Oberfläche) | Lohe, Magmawürfel | 100 % |
| `io` | Magmawürfel | 50 % |
| `titan` | Zombie, Skelett | 50 % |
| `triton`, `pluto` | Eiswanderer (Stray) | 50 % |
| andere | keine | |

Monster brauchen keinen Sauerstoff und nehmen keinen Umweltschaden. Mit `planet_mobs = false` spawnen keine.

---

## 14. Raumstation

### 14.1 Stationskern

- `station_core` darf nur in Orbit-Dimensionen gesetzt werden, auch in der Luft: Ohne Zielblock setzt ein Rechtsklick den Kern 3 Blöcke vor den Spieler (aus der Kapsel siehe 12.5, Schritt 7).
- Beim Setzen entsteht eine 5×5-Plattform aus `station_hull` mit dem Kern in der Mitte, nur an Luftpositionen. Diese Blöcke haben den Blockstate `generated=true` und droppen beim Abbau nichts. Der Kern enthält einen eingebauten Kuppelgenerator T1 (Radius 8) mit eigenem Puffer: eingebaute Solarzellen mit 4 × 20 J/t × Solarfaktor des Orbits (Erdorbit: 80 J/t) und 2.000 mB O₂-Tank. Der Kern nimmt zusätzlich Strom über Kabel an. Ab dem Jupiter-Orbit reicht die eingebaute Solarleistung nicht mehr für die Kuppel (40 J/t); dort braucht die Station eine eigene Stromquelle.
- Eine **Station** = alle Blöcke im Umkreis von 48 Blöcken um einen `station_core`.

### 14.2 Stationsmodule

Alle Module sind Blöcke und funktionieren nur in Orbit-Dimensionen (weitere Ortsbindungen in 6.1). Einen `station_core` im Umkreis von 48 Blöcken brauchen nur `shipyard_terminal` und `shipyard_dock` (21.1).

| ID | Funktion |
| --- | --- |
| `station_hull`, `station_window` | Bauteile, Fenster durchsichtig |
| `docking_bay` | Landeplatz für Rakete und Landefähre (Ziel in der Landeortwahl), tankt aus angrenzenden Tanks |
| `tank_depot` | 256.000 mB; Ausnahme: funktioniert auch außerhalb von Orbit-Dimensionen (6.3) |
| `solar_wing` | 20 J/t × Solarfaktor |
| `habitation_module` | Bett-Funktion (Respawn) + 27 Slots |
| `research_lab` | +50 % FP für angrenzende Analysatoren |
| `zero_g_fabricator` | Maschine, Kapitel 6 |
| `greenhouse` | 9 Pflanzslots, wächst wie Hydrokultur, braucht 1 `dry_ice` pro 6.000 t; mit `ammonia_fertilizer` +50 % |
| `gas_collector` | Kapitel 6 |
| `magnetic_trap` | Kapitel 6 |
| `shipyard_terminal`, `shipyard_dock` | Kapitel 21 |

### 14.3 Schwerelosigkeit

- In Orbit-Dimensionen ist g = 0,005. Bewegung: Laufen auf Blöcken normal, Springen löst sich vom Boden.
- `jetpack`: Springen gedrückt = Schub nach oben, Sneak = nach unten, Blickrichtung + Vorwärts = 0,1 Blöcke/t.
- Innen schwebt man ebenfalls, bis man `gravity_plate`s baut.
- **Fall unter y 0** in einer Orbit-Dimension:
  - Planeten mit Oberfläche (Erde, Venus, Mars, Merkur): Teleport auf die Oberfläche an gleicher X/Z (Venus: Wolkenschicht) mit Langsamem Fall für 400 t.
  - Gasriesen: Teleport auf den nächsten `station_core` (sonst (0, 101, 0) auf einer neuen 3×3-Plattform aus `station_hull`) und 4 Schaden.
- Vor dem Jetpack (Stufe 4) bewegt man sich mit `magnet_boots` (Stufe 3, kommt mit dem Stationskern): Auf Blöcken gilt 1 g, wer abspringt, treibt mit 0,005 g.

### 14.4 Klon-Kammer

`clone_chamber`: Rechtsklick bindet den Spieler (ein Bindungsort pro Spieler). Bei Tod respawnt er dort, dimensionsübergreifend, wenn die Kammer Strom hat und eine `microbe_culture` im Slot liegt (wird verbraucht). Sonst normaler Vanilla-Respawn.

- Die Klon-Kammer meldet ihren Zustand (Strom ja/nein, Anzahl Kulturen) bei jeder Änderung an eine globale `SavedData`. Beim Tod wird nur dieser gespeicherte Zustand geprüft, der Chunk muss nicht geladen sein. Die Kultur wird beim nächsten Laden abgezogen.
- **Betten** und `habitation_module` setzen in Mod-Dimensionen den Spawnpunkt nur innerhalb einer aktiven Kuppel. Außerhalb erscheint die Meldung „Hier kannst du nicht atmen“.
- **Prüfung beim Respawn:** Der Server prüft den Zielpunkt mit `EnvironmentCalculator` (Schutz ohne Anzug, 8.3). Ist er unsicher, etwa weil die Kuppel inzwischen ausgefallen ist, gilt die Reihenfolge Klon-Kammer → Bett → Weltspawn der Overworld, jeweils mit Meldung. Das verhindert Todesschleifen.

---

## 15. Reaktoren

Siehe Kapitel 6.3 (Werte, Brennstoff, Sicherheit) und 4.3 (Multiblock-Aufbau). Der Kernreaktor verbraucht jeweils einen Brennstab aus seinen 4 Slots, 1 Stab pro 48.000 t.

---

## 16. Rover

| Eigenschaft | Wert |
| --- | --- |
| Entity | `rover`, 2 Sitze, Größe 3×2×4 Blöcke |
| Aufbau | `rover_kit` auf den Boden setzen (Rechtsklick) |
| Energie | 100.000 J, Verbrauch 50 J pro gefahrenem Block, Solardach +2 J/t bei sichtbarem Himmel |
| Tempo | 0,4 Blöcke/t |
| Stufenhöhe | 2 Blöcke |
| Frachtraum | 27 Slots, Sneak-Rechtsklick öffnet |
| Module | 6 Slots, Rechtsklick mit Modul-Item |
| Laden | neben `battery`/`lithium_battery_bank` parken: 512 J/t |
| Abbau | Sneak-Rechtsklick mit leerer Hand → `rover_kit`. Fracht, Module und Energie bleiben als Data Components erhalten. |

| Modul-ID | Wirkung |
| --- | --- |
| `rover_scanner` | alle 100 t: Erzblöcke im Radius 32 werden 100 t lang durch Wände sichtbar umrandet; Erkundungsziele im Radius 128 werden im Atlas markiert |
| `rover_drill` | baut beim Fahren 3×3 Blöcke vor dem Rover ab (1 Block / 10 t), Drops in den Frachtraum; gilt als Vibrationsquelle |
| `rover_headlight` | Licht 15 vor dem Rover (gleiche Technik wie `helmet_lamp`) |
| `rover_pressure_cabin` | Insassen atmen ohne Anzug, eigener O₂-Tank 1.800 mB (30 min für 1 Person, 15 min für 2; Verbrauch wie Anzug T1 pro Insasse), Temperatur 20 °C |
| `rover_lithium_pack` | Energie × 2 (Stufe 4) |
| `rover_trailer` | +27 Slots (Anhänger-Entity folgt) |

Venus-Oberfläche: Der Rover hat dieselbe Hitzezeit von 3.600 t, danach bleibt er stehen, bis er wieder über y 180 gebracht wird (z. B. per `cloud_elevator` – Fahrzeuge in 3 Blöcken Umkreis werden mitgenommen).

---

## 17. Nahrung

- Hunger wie Vanilla.
- `space_ration`: Nahrung und Sättigung = Summe der 3 Eingaben (max. 20). Stapelbar bis 16. Data Component `nutrition`.
- Planetenboden ist nicht bepflanzbar. `compost_soil` wird mit der Hacke zu `compost_farmland`, das sich wie Vanilla-Ackerland verhält.
- `hydroponics_basin`: ein Pflanzslot (Vanilla-Samen/Setzlinge). Die Pflanze wächst um 1 Altersstufe pro 2.000 t, das ist etwa doppelt so schnell wie auf idealem Vanilla-Ackerland (mit `ammonia_fertilizer` pro 1.333 t). Ausgewachsen wird nach der Vanilla-Loot-Table des Pflanzenblocks geerntet, Ertrag in den Ausgabeslot, Samen zurück in den Pflanzslot. `greenhouse` verhält sich wie 9 Becken. Setzlinge: 2 Stufen à 2.000 t, Ertrag 4 Stämme der Holzart + 1 Setzling.
- `ammonia_fertilizer`: Rechtsklick auf Becken oder Gewächshaus → +50 % Wachstum für 24.000 t.
- Alle Pflanzen brauchen Licht und eine aktive Kuppel (außer auf der Erde).

---

## 18. Kreaturen

Spawn-Gewicht relativ zu anderen Kreaturen im Biom. „Analyse“ = +15 FP mit `field_scanner` (einmal pro Typ und Agentur).

**Spawnregeln:**
- Alle Kreaturen nutzen eigene Spawn-Prädikate statt der Vanilla-Bodenregeln (auf Planeten gibt es kein Gras).
- Feindliche Kreaturen: Kategorie `monster`, Licht ≤ 7.
- Passive und neutrale Kreaturen: Kategorie `creature`, beliebiges Licht, auf jedem festen Planetenblock.
- Wasser- und Methanbewohner: Kategorie `water_creature`.
- Fliegende Kreaturen (`cloud_ray`, `geyser_moth`): Kategorie `ambient`.
- `reef_keeper` (1 je `hydrothermal_field`) und `drift_mite` (Schwarm je Platinerz-Asteroid) werden beim Generieren der Struktur bzw. des Asteroiden gesetzt und spawnen nicht nach.

| ID | Name DE | Ort (Biom) | Verhalten | HP | Schaden | Tempo | Größe (B×H) | Drop |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `rubble_crawler` | Geröllkrabbler | Mond: Hochland, Mare; Gruppe 1–2, Gewicht 10 | neutral; bewegungslos und als Stein getarnt, bis ein Spieler < 3 Blöcke ist; sucht Kabel und Metallblöcke (Tag `sternenwerft:crawler_food`) außerhalb von Kuppeln im Radius 16 und baut alle 200 t einen ab | 16 | 3 | 0,2 | 0,8×0,6 | 1–2 `metal_concentrate` |
| `shade_strider` | Schattenläufer | Mond: Polarkrater; Gewicht 5 | scheu; flieht vor Licht > 7 und Spielern < 8; folgt ihm ein Spieler 10 s, läuft er zum nächsten `moon_ice` oder Erz im Radius 48 | 20 | – | 0,35 | 0,6×1,8 | – |
| `dune_burrower` | Dünenwühler | Mars: Rote Ebenen; Gewicht 4 | feindlich; gräbt unsichtbar im Sand; Vibrationsquellen im Radius 32 (Tag `sternenwerft:vibrating`: `crusher`, `drilling_rig`, `rover_drill`) ziehen ihn an; greift Spieler in der Nähe an | 60 (Rüstung 10) | 8 | 0,25 | 1,5×1,2 | 1–3 `armor_plate` |
| `dust_wraith` | Staubgeist | Mars: nur im Staubsturm; Gewicht 3 | passiv, schwebend, immun gegen Projektile; führt den Spieler zu 75 % zur nächsten `lava_tube`/`meteorite_field`-Struktur, zu 25 % 64 Blöcke in zufällige Richtung, dann verschwindet er | 10 | – | 0,3 | 0,6×1,6 | – |
| `sulfur_snail` | Schwefelschnecke | Venus: Lavaebenen; Gewicht 8 | passiv, feuerfest, säurefest; legt alle 100 t eine dünne `sulfur_trail`-Schicht; legt wie ein Huhn alle 6.000 t 1 `sulfur` ab; vermehrt sich mit `sulfur` | 12 | – | 0,1 | 0,6×0,5 | 1 `sulfur` |
| `cloud_ray` | Wolkenrochen | Venus: Wolkenschicht y 180–240; Gewicht 6 | passiv, fliegend; zähmbar mit `sulfur` (33 % pro Versuch); gezähmt + Sattel = fliegendes Reittier (0,6 Blöcke/t) | 30 | – | 0,3 | 2,5×0,6 | – |
| `mirror_beetle` | Spiegelkäfer | Merkur: Dämmerungsgürtel; Gewicht 8 | neutral; schaut ein Spieler ihn bei Tageslicht < 16 Blöcke an: Blindheit 60 t; angegriffen greift er zurück | 20 | 4 | 0,25 | 0,8×0,5 | 1 `mirror_shell` |
| `drift_mite` | Schwebemilbe | Asteroidengürtel: spawnt in Schwärmen (5–10) im Radius 8 um `asteroid_platinum_ore` | harmlos, schwebt | 4 | – | 0,2 | 0,3×0,3 | – |
| `sulfur_salamander` | Schwefelsalamander | Io: Lavaseen, `volcanic_vent`; Gewicht 6 | feindlich, feuerfest; Fernkampf: Schwefelklumpen wie Lohen-Feuerball ohne Feuer | 30 | 5 | 0,25 | 0,8×0,6 | 0–2 `heat_scale` |
| `ice_jelly` | Eisqualle | Europa: Ozean; Gewicht 10 | passiv, leuchtet (emissive Textur), schwimmt langsam Richtung nächster `hydrothermal_vent` im Radius 64 | 8 | – | 0,1 | 0,8×0,8 | – |
| `reef_keeper` | Riffwächter | Europa: Quellen; 1 je Quellen-Cluster | neutral, groß; wird feindlich gegen Spieler, die im Radius 16 Blöcke abbauen oder Proben nehmen, außer sie haben ihn mit 1 `space_ration` gefüttert (dann 12.000 t friedlich zu diesem Spieler) | 120 | 12 | 0,2 | 2×2 | – |
| `methane_skimmer` | Methanläufer | Titan: Methanseen; Gewicht 8 | passiv, läuft auf `methane`-Oberfläche, flieht vor Spielern < 12 | 10 | – | 0,45 | 0,9×1,2 | 1–2 `skimmer_leg` |
| `geyser_moth` | Geysirmotte | Enceladus: kreist im Radius 6 um `ammonia_geyser` | harmlos, fliegend | 4 | – | 0,3 | 0,5×0,3 | – |
| `frost_lurker` | Frostkriecher | Triton: alle Biome; Gewicht 5 | feindlich; unsichtbar im Eis, bricht hervor, wenn ein Spieler < 4 Blöcke ist | 40 | 7 | 0,3 | 1×0,8 | 0–2 `insulation_fabric` |
| `signal_sentinel` | Wächter des Senders | Pluto: `transmitter_core` | **Boss**, Bossleiste. Phase 1 (100–50 % HP): Nahkampf 10 + Schockwelle (Rückstoß Radius 6, alle 100 t). Phase 2 (< 50 %): beschwört 3 `frost_lurker`, Strahl wie Wächter (6 Schaden über 40 t) | 400 | 10 | 0,25 | 1,5×3 | 1 `star_chart` mit Data Component `chart_owner {agency, uuid}` für die Agentur des Beschwörers, aber nur, wenn deren Agentur-SavedData keine gültige `chart_uuid` hat. Die Karte ist feuerfest und despawnt nie. Als verloren gilt sie erst, wenn ihr Item-Entity zerstört wird; dann wird `chart_uuid` gelöscht und der Boss kann neu droppen. Die Werft nimmt nur Karten der eigenen Agentur. Admin-Befehl: `/sternenwerft agency reset_chart <agentur>`. |

**Verwendung der Drops:** `metal_concentrate` → Zerkleinerer. `armor_plate` → `armor_plating`-Modul. `mirror_shell` → `heat_reflector`. `heat_scale` → `cooling_module` (alternatives Rezept). `skimmer_leg` → `pontoon`. `insulation_fabric` → Anzug T3.

`pontoon`: Block, wie Seerose auf Flüssigkeiten setzbar, trägt Maschinen (z. B. `pump_station` auf Methanseen).

`sulfur_trail`: dekorative, nicht feste Schicht (wie eine Schneeschicht), droppt nichts und verschwindet nach 6.000 t.

---

## 19. Ereignisse

Pro Dimension prüft der Server alle 1.200 t mit der angegebenen Wahrscheinlichkeit (× Einstellung `event_frequency`: aus 0, selten 0,5, normal 1, häufig 2). Ereignisse laufen nur, wenn mindestens ein Spieler in der Dimension ist.

**Planung:**
- Ein erfolgreicher Wurf startet das Ereignis nicht sofort, sondern **plant es 2.400 t in die Zukunft**. Gespeichert wird es in einer `SavedData` je Dimension.
- Die Warnung kommt bei T − 2.400 (mit `space_weather_station`) bzw. T − 400 (ohne).
- Solange ein Ereignis geplant ist oder läuft, wird für diese Dimension nicht neu gewürfelt.

**Wetter je Dimension:**
- Mod-Dimensionen haben einen **eigenen Wetterzustand** in der `SavedData` der Dimension, nicht das Wetter der Overworld.
- Laufzeiten wie Vanilla: klar 12.000–180.000 t, Wetter 12.000–24.000 t.
- Planeten ohne Wetter-Ereignis in der Tabelle haben immer klares Wetter.

| Ereignis | Dimensionen | Chance pro 1.200 t | Dauer | Wirkung | Schutz | Belohnung |
| --- | --- | --- | --- | --- | --- | --- |
| Sonnensturm | alle außer Erde; Merkur ×3 | 1,5 % | 1.200–3.600 t | Strahlung +2; Maschinen ohne Deckung (≥ 3 Blöcke darüber) und ohne Kuppel T3 pausieren alle 200 t für 40 t | Kuppel T3, `radiation_shield`, ≥ 3 Blöcke Deckung | Solarpanels × 2 |
| Meteoritenschauer | `moon`, `mars`, `asteroid_belt`, alle Orbits | 1 % | 1.200 t | alle 100 t ein Meteorit im Radius 64 um jeden Spieler; Explosion Stärke 2 ohne Feuer (Blockschaden abschaltbar), hinterlässt 1 `meteorite_ore` | Kuppel mit `dome_shield_module` fängt ab | `meteorite_ore` → 2 `raw_iron` + 1 `raw_nickel`, 10 % 1 `raw_platinum`, 5 % 1 `raw_iridium` |
| Staubsturm | `mars` (statt Vanilla-Regen) | Vanilla-Wetter | Vanilla | Nebel Sichtweite 16, Solar × 0,25, Monster × 2 | Kuppel T2 | `dust_wraith` spawnt |
| Säureregen | `venus` (statt Regen) | Vanilla-Wetter | Vanilla | ungeschützte Rüstung und Werkzeuge in der Hand −1 Haltbarkeit pro 40 t | Kuppel T2, Anzug T2 | – |
| Vulkanausbruch | `io` | 3 % je `volcanic_vent` im Umkreis 128 eines Spielers | 200 t | fallende `io_sulfur_ore`-Blöcke im Radius 32 | Abstand | frischer Schwefel |
| Methanregen | `titan` (statt Regen) | Vanilla-Wetter | Vanilla | Sicht × 0,5 | – | `pump_station` +50 % |
| Ringtrümmer | `saturn_orbit` | 2 % | 600 t | wie Meteoritenschauer, ohne Erz | `dome_shield_module` | – |

**Vorwarnung:** Mit `space_weather_station` der Agentur in derselben Dimension kommt die Warnung 2.400 t vorher, sonst 400 t vorher (Chat-Nachricht + Ton + HUD-Symbol).

---

## 20. Logistik und Endgame

### 20.1 Außenposten

- Ein **Außenposten** = ein `cargo_terminal` plus alle Produzenten (`drilling_rig`, `pump_station`, `gas_collector`, `magnetic_trap`) im Umkreis von 16 Blöcken.
- Das Terminal hat 216 Item-Slots (8 × 27, GUI scrollbar), 8 Frachttanks à 64.000 mB und einen separaten Treibstofftank (64.000 mB, nur `methane`). Produzenten geben direkt ins Terminal. Damit passt die volle Ladung eines `heavy_freighter` (8 × 27 Slots, 8 × 64.000 mB) hinein. Eingehendes Methan (Rohr, Leerslot, Produzent) füllt zuerst den Treibstofftank, erst der Überschuss geht in die Frachttanks. Am `shipyard_dock` gilt dasselbe: zuerst Treibstofftank, dann Werftlager.
- **Offline-Produktion:** Jeder Außenposten wird in einer globalen `SavedData` (Overworld) registriert: Position, Dimension, Produzenten, Rate, letzter Tick. Ist der Chunk nicht geladen, wird die Produktion beim nächsten Zugriff (Laden, Logistik-Abfrage, Frachtlandung) nachgerechnet: `vergangene_Ticks × Rate × outpost_offline_factor (0,5)`, begrenzt durch den freien Platz und durch vorhandene Eingaben (z. B. leere `antimatter_cell` der Magnetfalle). Energie wird offline nicht simuliert: Ein Produzent zählt offline nur, wenn er beim Entladen des Chunks den Status `running` hatte. „Rate“ = Ausgabe pro Vorgang ÷ Vorgangsdauer laut 6.1. Ein Produzent gehört immer zum nächstgelegenen `cargo_terminal`. Mit `outposts_offline = false` gibt es keine Offline-Produktion.

### 20.2 Bohrturm

Beim Platzieren zählt der Bohrturm alle Erzblöcke (Tag `c:ores` und Planeten-Erze) im Chunk unter sich. Er erzeugt alle 100 t 1 Erz-Item, zufällig gewichtet nach diesen Anzahlen. Die Welt wird nicht verändert (unendliche Lagerstätte). Ohne Erze: Status `error`.

### 20.3 Frachtraketen

| ID | Laderaum | Tank | Treibstoff pro Flug | Stufe |
| --- | --- | --- | --- | --- |
| `cargo_rocket` | 4 × 27 Slots | 64.000 mB `methane` | 2.000 + 1.500 × \|ΔDP\| mB | 5 |
| `heavy_freighter` | 8 × 27 Slots | 1 `fuel_rod` = 10 Flüge | – | 6 |

- **Zuordnung:** Jedes `cargo_terminal` hat 4 Hangar-Slots. Eine Frachtrakete im Hangar-Slot gehört zu diesem Terminal und kann im Logistik-Computer Routen zugewiesen bekommen. Vor jedem Start tankt sie automatisch aus dem Treibstofftank des Terminals. Routen starten immer am Heimatterminal der Rakete; der Logistik-Computer bietet für eine Route nur Raketen dieses Terminals an. Während des Flugs bleibt das Item im Hangar-Slot und ist gesperrt.
- **`heavy_freighter`:** Data Component `flights_left` (0–10); ein Flug ist Hin- und Rückflug. Steht der Wert auf 0, nimmt der Frachter vor dem Start automatisch 1 `fuel_rod` aus dem Item-Lager des Heimatterminals. Gibt es keinen, bekommt er den Status „ohne Treibstoff“.
- **Fluidfracht:** Neben den Item-Slots hat die `cargo_rocket` 4 Fluid-Laderäume à 64.000 mB (`heavy_freighter`: 8). Fluide lädt sie direkt aus den Fluidtanks des Start-Terminals (Filter der Route) und entlädt in die des Ziel-Terminals bzw. ins Werftlager.
- **Rückflug** immer leer.
- Gestrandete Raketen („ohne Treibstoff“) versuchen alle 1.200 t erneut zu tanken.
- Ein `trading_post` als Ziel nimmt nur Items an; Fluidfracht bleibt in der Rakete.
- **Antriebe nachrüsten:** Rakete + `atomic_drive` bzw. `antimatter_drive` im Montagetisch. Gespeichert in der Data Component `sternenwerft:drive`; es zählt nur der beste Antrieb.
- **Flugzeit** = 1.000 + 3.950 × |ΔDP| Ticks (Erde → Mond 1.000 t, Erde → Pluto ca. 72.000 t). `atomic_drive` (eingebaut im `heavy_freighter`, nachrüstbar in `cargo_rocket`): Zeit ÷ 2. `antimatter_drive` (nachrüstbar): Zeit ÷ 10.
- Frachtflüge sind **keine fliegenden Entities**. Ein Flug ist ein Datensatz (Rakete, Route, Startzeit, Ankunftszeit, Fracht). Beim Start und bei der Landung spielt das Terminal eine kurze Animation, wenn der Chunk geladen ist.
- Mit vollem Tank (64.000 mB) schafft jede Frachtrakete Hin- und Rückflug zu jedem Ziel (längster: Pluto, 58.000 mB).
- Vor dem Rückflug tankt die Rakete am Ziel aus dessen Treibstofftank nach (`cargo_terminal` und `shipyard_dock` haben je einen, 64.000 mB `methane`). Hat sie Methan als Fracht geladen, behält sie beim Entladen so viel, wie der Rückflug braucht. Reicht es dann nicht, bleibt sie dort (Status „ohne Treibstoff“).
- Der Logistik-Computer warnt schon beim Anlegen einer Route, wenn Hin- plus Rückflug (z. B. Pluto: 2 × 29.000 = 58.000 mB) den Tank übersteigt und am Ziel kein Methan gelagert ist.

### 20.4 Logistik-Computer

`logistics_computer`: GUI mit allen Raketen und Routen der Agentur.

| Feld | Werte |
| --- | --- |
| Start / Ziel | beliebiges `cargo_terminal`, `shipyard_dock` oder freigegebenes Terminal bzw. `trading_post` einer Partneragentur |
| Fracht | Filterliste (Items/Fluide) oder „alles“ |
| Startbedingung | „wenn voll“ · „alle N Ticks“ · „wenn Ziel weniger als N von X hat“ · „bis der Bedarf der Werft gedeckt ist“ |
| Rakete | eine oder mehrere zugewiesene Frachtraketen |

Statusliste: unterwegs (mit Restzeit), geladen, wartend, ohne Treibstoff.

### 20.5 Antriebe

| ID | Wirkung |
| --- | --- |
| `atomic_drive` | Frachtraketen: Flugzeit ÷ 2 |
| `antimatter_drive` | Frachtraketen: Flugzeit ÷ 10; T3 braucht 4 Stück |

---

## 21. Orbitalwerft und Rakete T3

### 21.1 Werft

- `shipyard_terminal` darf nur in einer Station in `earth_orbit` stehen. Dazu gehört mindestens ein `shipyard_dock` in derselben Station.
- `shipyard_dock`: Landeziel nur für Frachtraketen. Liefert direkt ins virtuelle Werftlager (unbegrenzt, aber nur benötigte Items/Fluide werden angenommen). Auch Rohre und Trichter am Dock liefern ein.
- Terminal-GUI: 8 Bauabschnitte mit Fortschrittsbalken und fehlenden Mengen. Meldet den Bedarf an den Logistik-Computer.
- Lieferungen werden für **alle** noch offenen Abschnitte angenommen. Ist ein Abschnitt vollständig beliefert, baut die Werft ihn in **48.000 t** (Overworld-Zeit, läuft auch offline). Gebaut wird der Reihe nach (1 → 8).
- Am Terminal sieht man ein wachsendes Gerüstmodell (pro Abschnitt eine Ausbaustufe).
- Mehrere Agenturen dürfen an derselben Werft liefern, wenn der Besitzer sie freigibt.

### 21.2 Bauabschnitte

| # | Abschnitt | Material |
| --- | --- | --- |
| 1 | Rumpf | 512 `lightweight_plate`, 256 `titanium_plate`, 128 `aluminum_plate` |
| 2 | Schilde | 192 `tungsten_plate`, 64 `heat_shield` |
| 3 | Hauptantrieb | 4 `antimatter_drive` |
| 4 | Energie | 1 `fission_reactor`, 64 `lithium_battery`, 32 `fuel_rod` |
| 5 | Bordcomputer | 128 `iridium_chip`, 64 `platinum_ingot`, 1 `star_chart` |
| 6 | Lebenserhaltung | 1 `dome_generator_t3`, 8 `oxygen_generator`, 1 `greenhouse`, 1 `clone_chamber` |
| 7 | Kühlung | 1.024.000 mB `liquid_nitrogen`, 512.000 mB `liquid_helium`, 512.000 mB `ammonia` (= 64 / 32 / 32 Tanks) |
| 8 | Manövriertriebwerke | 512.000 mB `methane`, 256.000 mB `hydrogen`, 64 `sulfur` |

### 21.3 Finale

1. Nach Abschluss von Abschnitt 8 erscheint im Terminal „Start“. Alle Agenturmitglieder in `earth_orbit` werden benachrichtigt.
2. Start: Vollbild-Sequenz von 1.200 t. Nacheinander erscheinen die `celestial_bodies`-Texturen (25.1) aller Planeten, die die Agentur besucht hat, je 1.200 t ÷ Anzahl, mit langsamem Zoom auf schwarzem Sternenhintergrund. Am Ende steht die Sonne als kleiner Punkt.
3. Der Sender auf Pluto antwortet (Ton und Text aus `assets/sternenwerft/texts/signal_reply.txt`). Danach läuft der Abspann im Stil des Vanilla-`WinScreen` mit eigener Textdatei `assets/sternenwerft/texts/credits.txt`. Advancement `sternenwerft:starship` für alle Beteiligten; wer offline ist, bekommt es beim nächsten Login.
4. Danach erscheint das Entity `starship` neben der Station: 8 Sitze (4 Besatzung, 4 Passagiere). Es springt nur zwischen Orbit-Dimensionen (Sprungziel per Navigationskarte), unten kommt man mit der Landefähre weiter. Zielliste: alle Orbit-Dimensionen aus 12.1 (Sprung kostet 1 gefüllte `antimatter_cell`, keine Treibstoffprüfung) und alle Einträge aus der Registry `sternenwerft:star_system` außer dem Sonnensystem (in v1.0 keine).

---

## 22. Rezepte

### 22.1 Werkbank (Phase 1, vor dem Montagetisch)

Legende: `I` Eisenbarren, `C` Kupferbarren, `R` Redstone, `B` Redstone-Block, `F` Feuerstein, `P` Kolben, `M` `machine_frame`, `K` Ofen, `W` Werkbank, `G` Glas, `E` Eimer, `X` Eisenblock, `T` `copper_pipe`.

| Ergebnis | Muster | Menge |
| --- | --- | --- |
| `machine_frame` | `ICI` / `CRC` / `ICI` | 1 |
| `crusher` | `FPF` / `IMI` / `III` | 1 |
| `electric_furnace` | `CCC` / `CMC` / `CKC` | 1 |
| `press` | `IPI` / `IMI` / `IXI` | 1 |
| `press_mold_plate` | `II` / `II` | 1 |
| `press_mold_rod` | `I` / `I` / `I` | 1 |
| `press_mold_wire` | `I I` / ` I ` / `I I` | 1 |
| `combustion_generator` | `III` / `IMI` / `CKC` | 1 |
| `battery` | `CBC` / `BMB` / `CBC` | 1 |
| `copper_cable` | `CCC` | 6 |
| `copper_pipe` | `CGC` | 4 |
| `fluid_tank` | `IGI` / `G G` / `IGI` | 1 |
| `fluid_pump` | `IEI` / `IMI` / `ITI` | 1 |
| `assembly_table` | `CRC` / `IMI` / `IWI` | 1 |
| `engineers_handbook` | formlos: Buch + Kupferbarren | 1 |
| `planetary_atlas` | formlos: Buch + Kompass | 1 |

### 22.2 Maschinenrezepte

| Typ | Eingabe | Ausgabe | Hinweis |
| --- | --- | --- | --- |
| `crushing` | 1 Erz/Raw eines Metalls aus 5.1 | 2 `<m>_dust` | |
| `crushing` | 1 `metal_concentrate` | 1 `aluminum_dust` + 1 `titanium_dust` | |
| `crushing` | 1 `lithium_salt` | 2 `lithium_dust` | |
| `crushing` | 1 `meteorite_ore` | Drops aus 19 | |
| Vanilla-Schmelzen | `bauxite`, `raw_titanium`, Staub von Al/Ti/Fe/Cu/Au | Barren | auch im Elektroofen |
| `electric_smelting` | Staub/Raw von Mg, Ni, Li, Pt, Ir, U | Barren | nur Elektroofen |
| `electric_smelting` | 1 `quartz_sand` | 1 `silicon` | |
| `electric_smelting` | 1 `magnesium_ingot` + 1 `aluminum_ingot` | 1 `magnesium_alloy` | 2 Slots |
| `high_temp_smelting` | 1 `raw_tungsten` oder `tungsten_dust` | 1 `tungsten_ingot` | |
| `high_temp_smelting` | 1 Basalt (Tag `sternenwerft:basalt`: Vanilla-Basalt, `moon_basalt`) + 1 `aluminum_dust` | 1 `ceramic_plate` | |
| `pressing` | siehe 6.2 | | |
| `refining` | 1.000 mB `crude_oil` | 600 mB `kerosene` + 1 `plastic` | |
| `electrolysis` | 200 mB Wasser | 200 mB `hydrogen` + 100 mB `oxygen` | |
| `cryo` | 40 mB `helium` / `nitrogen` | 10 mB `liquid_helium` / `liquid_nitrogen` | |
| `zero_g` | 2 `magnesium_ingot` + 2 `copper_wire` + 100 mB `liquid_helium` | 1 `superconductor_wire` | |
| `zero_g` | 1 `advanced_circuit` + 2 `iridium_ingot` + 1 `platinum_ingot` | 1 `iridium_chip` | |

### 22.3 Montagetisch (`assembly`)

Formlos. Der Montagetisch hat 9 Eingabeslots; eine Zutat darf mehrere Slots belegen, ihre Mengen werden über alle Slots summiert (z. B. 96 `titanium_plate` = 64 + 32). Ein Rezept darf höchstens 9 Slots brauchen. Optional 1 Fluid. Ergebnis 1, wenn nicht angegeben.

**Bauteile**

| Ergebnis | Zutaten |
| --- | --- |
| `circuit` | 2 `silicon`, 4 `copper_wire`, 1 `plastic` |
| `advanced_circuit` | 2 `circuit`, 4 `gold_wire`, 2 `silicon` |
| `pressure_hull` | 4 `aluminum_plate`, 1 `titanium_plate` |
| `heat_shield` | 4 `tungsten_plate`, 2 `titanium_plate` |
| `cooling_system` | 4 `copper_pipe`, 1 `pressure_hull`, 1.000 mB `liquid_helium` |
| `lithium_battery` | 4 `lithium_ingot`, 2 `aluminum_plate`, 1 `nickel_plate`, 1 `circuit` |
| `insulation_fabric` ×2 | 4 `plastic`, 2 `silicon`, 1 `lithium_ingot` |
| `fuel_rod` | 4 `uranium_ingot`, 1 `titanium_plate` |
| `antimatter_cell` | 2 `superconductor_wire`, 2 `lightweight_plate`, 1 Glas |
| `transport_tank` | 4 `aluminum_plate`, 1 `pressure_hull` |
| `oxygen_canister` | 2 `aluminum_plate`, 1 `copper_pipe` |
| `large_oxygen_canister` | 2 `oxygen_canister`, 1 `titanium_plate` |
| `speed_upgrade` | 1 `circuit`, 4 Redstone, 2 `copper_wire` |
| `efficiency_upgrade` | 1 `circuit`, 2 `gold_wire`, 2 `aluminum_plate` |
| `compost` | 8 beliebige Pflanzen-Items (Tag `sternenwerft:compostable`) |
| `compost_soil` ×4 | 4 Planetenboden (`moon_regolith` oder `mars_sand`), 2 Knochenmehl, 1 `gypsum` *oder* 2 `compost` |
| `ammonia_fertilizer` ×8 | 1 Knochenmehl, 1.000 mB `ammonia` |
| `pontoon` ×4 | 2 `skimmer_leg`, 2 `plastic` |

**Werkzeuge und Ausrüstung**

| Ergebnis | Zutaten |
| --- | --- |
| `sample_tool` | 2 `titanium_rod`, 1 `iron_plate` |
| `field_scanner` | 2 `aluminum_plate`, 1 `circuit`, 1 Glas, 1 Redstone |
| `pressure_suit_helmet` | 4 `aluminum_plate`, 1 `plastic`, 4 Glas, 1 `circuit` |
| `pressure_suit_chestplate` | 10 `aluminum_plate`, 3 `plastic`, 1 `circuit` |
| `pressure_suit_leggings` | 6 `aluminum_plate`, 2 `plastic` |
| `pressure_suit_boots` | 4 `aluminum_plate`, 2 `plastic` |
| `heat_suit_helmet` | `pressure_suit_helmet`, 3 `ceramic_plate`, 2 `titanium_plate` |
| `heat_suit_chestplate` | `pressure_suit_chestplate`, 6 `ceramic_plate`, 2 `titanium_plate`, 2 `cooling_system` |
| `heat_suit_leggings` | `pressure_suit_leggings`, 4 `ceramic_plate`, 2 `titanium_plate` |
| `heat_suit_boots` | `pressure_suit_boots`, 3 `ceramic_plate`, 2 `titanium_plate` |
| `deep_space_suit_helmet` | `heat_suit_helmet`, 2 `superconductor_wire`, 2 `insulation_fabric` |
| `deep_space_suit_chestplate` | `heat_suit_chestplate`, 2 `superconductor_wire`, 4 `lithium_battery`, 2 `insulation_fabric` |
| `deep_space_suit_leggings` | `heat_suit_leggings`, 2 `superconductor_wire`, 2 `insulation_fabric` |
| `deep_space_suit_boots` | `heat_suit_boots`, 2 `superconductor_wire`, 2 `insulation_fabric` |
| `helmet_lamp` | 2 `aluminum_plate`, 1 Glowstone, 1 `circuit` |
| `cooling_module` | 4 `copper_pipe`, 1 `cooling_system` *oder* 4 `copper_pipe`, 2 `heat_scale` |
| `heat_reflector` | 2 `mirror_shell`, 2 `aluminum_plate` |
| `heating_module` | 4 `copper_wire`, 2 `lithium_battery`, 2 `insulation_fabric` |
| `armor_plating` | 2 `armor_plate`, 1 `titanium_plate` |
| `magnet_boots` | 4 `iron_plate`, 2 `nickel_plate`, 1 `circuit` |
| `jetpack` | 8 `lightweight_plate`, 4 `lithium_battery`, 2 `copper_pipe`, 1 `advanced_circuit` |
| `radiation_shield` | 8 `tungsten_plate`, 1 `plastic` |
| `diving_module` | 4 `lightweight_plate`, 2 Glas, 1 `oxygen_canister` |
| `ice_drill` | 4 `tungsten_plate`, 2 `titanium_rod`, 1 `lithium_battery`, 1 `advanced_circuit` |

**Maschinen und Blöcke**

| Ergebnis | Zutaten |
| --- | --- |
| `electrolyzer` | `machine_frame`, 4 `copper_wire`, 2 `iron_plate`, 2 `copper_pipe`, 1 Glas |
| `refinery_controller` | `machine_frame`, 16 `iron_plate`, 8 `copper_pipe`, 4 Redstone |
| `refinery_casing` ×4 | 2 `iron_plate`, 1 `copper_pipe` |
| `item_port` / `fluid_port` / `energy_port` | 4 `iron_plate`, 1 Trichter / 1 `copper_pipe` / 1 `copper_cable` |
| `fuel_generator` | `machine_frame`, 1 `combustion_generator`, 4 `copper_pipe`, 4 `iron_plate` |
| `solar_panel` | 4 `silicon`, 2 `aluminum_plate`, 4 `copper_wire`, 1 Glas |
| `aluminum_cable` ×8 | 3 `aluminum_wire`, 1 `plastic` |
| `pressure_bottle` | 4 `aluminum_plate`, 1 `copper_pipe` |
| `analyzer` | `machine_frame`, 4 `aluminum_plate`, 2 `circuit`, 1 Glas |
| `research_terminal` | `machine_frame`, 4 `aluminum_plate`, 4 `circuit`, 2 Glas |
| `food_packer` | `machine_frame`, 2 `aluminum_plate`, 1 Kolben, 1 `circuit` |
| `launch_pad` ×2 | 2 `iron_plate`, 1 `titanium_plate` |
| `launch_tower` ×2 | 4 `iron_rod`, 1 `iron_plate` |
| `fuel_pump` | `machine_frame`, 4 `copper_pipe`, 2 `aluminum_plate`, 1 `circuit` |
| `launch_control` | `machine_frame`, 4 `circuit`, 4 `aluminum_plate`, 2 Glas |
| `probe` | 8 `aluminum_plate`, 2 `circuit`, 1 `solar_panel`, 1 Tank `kerosene` |
| `electric_lamp` ×4 | 1 `aluminum_plate`, 1 Glowstone, 1 `copper_wire` |
| `regolith_heater` | `machine_frame`, 8 `aluminum_plate`, 4 `copper_wire`, 2 `circuit`, 1 `pressure_bottle` |
| `high_temperature_furnace` | `electric_furnace`, 8 `titanium_plate`, 4 Ziegel, 2 `advanced_circuit` |
| `cryo_liquefier` | `machine_frame`, 8 `aluminum_plate`, 4 `copper_pipe`, 2 `advanced_circuit`, 1 `pressure_hull` |
| `titanium_pipe` ×4 | 3 `titanium_plate`, 1 Glas |
| `gas_tank` | 8 `aluminum_plate`, 2 `pressure_hull` |
| `oxygen_generator` | 8 `aluminum_plate`, 4 `copper_pipe`, 2 `circuit`, 1 `pressure_hull` |
| `dome_generator_t1` | 16 `aluminum_plate`, 8 `titanium_plate`, 4 `circuit`, 4 Glas |
| `dome_generator_t2` | `dome_generator_t1`, 16 `lightweight_plate`, 4 `cooling_system`, 4 `advanced_circuit` |
| `dome_generator_t3` | `dome_generator_t2`, 16 `superconductor_wire`, 16 `tungsten_plate`, 4 `iridium_chip` |
| `dome_range_module` | 4 `aluminum_plate`, 2 `advanced_circuit` |
| `dome_efficiency_module` | 4 `aluminum_plate`, 2 `advanced_circuit`, 2 `copper_pipe` |
| `dome_shield_module` | 8 `titanium_plate`, 2 `advanced_circuit` |
| `landing_beacon` | 2 `aluminum_plate`, 1 `circuit`, 1 Glowstone |
| `aerostat_platform` ×4 | 2 `aluminum_plate`, 1 `plastic` |
| `cloud_elevator` | 8 `aluminum_plate`, 4 `titanium_plate`, 2 `circuit`, 1 `plastic` |
| `rover_kit` | 32 `aluminum_plate`, 16 `titanium_plate`, 4 `circuit`, 4 `solar_panel`, 1 `battery` |
| `rover_scanner` | 4 `aluminum_plate`, 2 `advanced_circuit`, 1 Glas |
| `rover_drill` | 4 `titanium_plate`, 2 `titanium_rod`, 1 Diamant |
| `rover_headlight` | 2 `aluminum_plate`, 2 Glowstone |
| `rover_pressure_cabin` | 8 `aluminum_plate`, 4 Glas, 2 `oxygen_canister`, 1 `pressure_hull` |
| `rover_lithium_pack` | 4 `lithium_battery`, 2 `aluminum_plate` |
| `rover_trailer` | 16 `aluminum_plate`, 1 Truhe |
| `hydroponics_basin` | 4 `aluminum_plate`, 2 Glas, 1 Wassereimer, 1 `circuit` |
| `station_core` | 32 `lightweight_plate`, 8 `advanced_circuit`, 4 `solar_panel`, 1 `dome_generator_t1` |
| `station_hull` ×4 | 2 `lightweight_plate` |
| `station_window` ×2 | 1 `lightweight_plate`, 2 Glas |
| `docking_bay` | 16 `lightweight_plate`, 4 `advanced_circuit`, 2 `copper_pipe` |
| `tank_depot` | 8 `lightweight_plate`, 4 `copper_pipe`, 1 `pressure_hull` |
| `solar_wing` | 2 `solar_panel`, 1 `lightweight_plate` |
| `habitation_module` | 8 `lightweight_plate`, 1 Bett, 1 Truhe, 1 `advanced_circuit` |
| `research_lab` | 8 `lightweight_plate`, 4 `advanced_circuit`, 1 `analyzer` |
| `zero_g_fabricator` | 16 `lightweight_plate`, 4 `advanced_circuit`, 2 `cooling_system`, 1 `assembly_table` |
| `greenhouse` | 8 `lightweight_plate`, 8 Glas, 1 `hydroponics_basin`, 2 `advanced_circuit` |
| `lander` | 32 `lightweight_plate`, 8 `heat_shield`, 8 `advanced_circuit`, 2 `cooling_system` |
| `fusion_reactor` (Controller) | 64 `titanium_plate`, 16 `superconductor_wire`, 8 `cooling_system`, 4 `advanced_circuit` |
| `fusion_casing` ×4 | 2 `titanium_plate`, 1 `copper_wire` |
| `superconductor_cable` ×4 | 1 `superconductor_wire`, 2 `plastic` |
| `gravity_plate` | 2 `lightweight_plate`, 2 `superconductor_wire`, 1 `advanced_circuit` |
| `space_weather_station` | 8 `aluminum_plate`, 4 `advanced_circuit`, 1 `solar_panel` |
| `lithium_battery_bank` | `machine_frame`, 8 `lithium_battery` |
| `gas_collector` | 16 `lightweight_plate`, 4 `superconductor_wire`, 2 `advanced_circuit`, 4 `copper_pipe` |
| `magnetic_trap` | 16 `superconductor_wire`, 8 `tungsten_plate`, 4 `iridium_chip`, 2 `cooling_system` |
| `advanced_probe` | 8 `lightweight_plate`, 4 `advanced_circuit`, 2 `solar_panel`, 1 Tank `hydrogen` |
| `asteroid_anchor` | 8 `titanium_rod`, 4 `tungsten_plate`, 1 `advanced_circuit` |
| `clone_chamber` | 16 `titanium_plate`, 4 `iridium_chip`, 8 `microbe_culture`, 1 `pressure_hull`, 4 Glas |
| `fission_reactor` (Controller) | 96 `titanium_plate`, 32 `tungsten_plate`, 8 `iridium_chip`, 8 `cooling_system` |
| `fission_casing` ×4 | 2 `titanium_plate`, 1 `tungsten_plate` |
| `pump_station` | `machine_frame`, 1 `fluid_pump`, 8 `titanium_plate`, 2 `advanced_circuit` |
| `drilling_rig` | `machine_frame`, 16 `titanium_plate`, 8 `nickel_plate`, 4 `tungsten_plate`, 4 `advanced_circuit` |
| `atomic_drive` | 16 `tungsten_plate`, 8 `titanium_plate`, 4 `fuel_rod`, 2 `cooling_system` |
| `antimatter_drive` | 16 gefüllte `antimatter_cell`, 2 `fuel_rod`, 4 `superconductor_wire`, 8 `tungsten_plate` |
| `cargo_terminal` | 16 `lightweight_plate`, 4 `advanced_circuit`, 2 Truhe, 4 `copper_pipe` |
| `cargo_rocket` | 64 `lightweight_plate`, 2 `iridium_chip`, 8 `lithium_battery` |
| `heavy_freighter` | 2 `cargo_rocket`, 1 `atomic_drive` |
| `logistics_computer` | `machine_frame`, 4 `iridium_chip`, 8 `advanced_circuit`, 4 Glas |
| `trading_post` | 4 `lightweight_plate`, 1 Truhe, 2 `advanced_circuit` |
| `shipyard_terminal` | 64 `lightweight_plate`, 16 `iridium_chip`, 8 `advanced_circuit` |
| `shipyard_dock` | 32 `lightweight_plate`, 4 `iridium_chip` |

**Raketenteile** (Summen entsprechen dem Design-Dokument: T1 = 200 Al, 64 Ti, 32 Schaltkreise bei 4 Tanks; T2 = 128 LW, 64 Hitzeschilde, 32 Adv., 4 Kühlsysteme bei 6 Tanks)

| Ergebnis | Zutaten |
| --- | --- |
| `rocket_capsule_t1` | 40 `aluminum_plate`, 16 `titanium_plate`, 16 `circuit` |
| `rocket_tank_t1` | 30 `aluminum_plate`, 4 `titanium_plate`, 2 `circuit` |
| `rocket_engine_t1` | 8 `aluminum_plate`, 32 `titanium_plate`, 8 `circuit` |
| `rocket_fin_t1` | 8 `aluminum_plate` |
| `rocket_cargo_bay` | 16 `aluminum_plate`, 2 `titanium_plate`, 1 Truhe |
| `rocket_capsule_t2` | 32 `lightweight_plate`, 16 `heat_shield`, 16 `advanced_circuit` |
| `rocket_tank_t2` | 12 `lightweight_plate`, 4 `heat_shield` |
| `rocket_engine_t2` | 16 `lightweight_plate`, 24 `heat_shield`, 16 `advanced_circuit`, 4 `cooling_system` |
| `rocket_fin_t2` | 2 `lightweight_plate` |
| `long_range_module` | 32 `lightweight_plate`, 16 `superconductor_wire`, 8 `iridium_chip`, 8 `lithium_battery` |

---

## 23. Mehrspieler und Agenturen

### 23.1 Agentur

- Globale `SavedData` (Overworld): Agenturen mit ID, Name, Mitgliedern (UUID, Rolle), FP, erforschten Knoten, Atlas, Sondenzielen, Routen, Terminals, Partnerschaften.
- Jeder Spieler bekommt beim ersten Join eine eigene Agentur („Agentur von <Name>“).
- Gründen, Einladen, Beitreten, Austreten und Rollen vergeben am `research_terminal` (Reiter „Agentur“). Beim Beitritt verlässt man die alte Agentur, ihre Fortschritte bleiben dort.
- **Geteilt:** FP, Forschung, Atlas, Sondenergebnisse, Routen, Stationen, Außenposten.
- **Pro Spieler:** Inventar, Anzug, Klon-Kammer-Bindung, Advancements.
- **Besitz:** Jede Block-Entity und jedes Fahrzeug speichert beim Platzieren `owner_agency` (Agentur des platzierenden Spielers). Darauf beziehen sich Kuppelschutz, Tore (11.4) und Logistik.
- **Lebenszyklus:**
  - Tritt der Leiter aus, wird das am längsten beigetretene `engineer`-Mitglied Leiter, sonst das am längsten beigetretene Mitglied.
  - **Beitritt:** Wird die alte Agentur durch einen Beitritt leer, geht ihr gesamter Besitz automatisch an die neue Agentur über (Block-Entities, Fahrzeuge, Kuppeln, Stationen, Terminals, Routen). Technisch speichert die alte Agentur `merged_into`, und `owner_agency` wird beim nächsten Zugriff umgeschrieben. FP, Forschung und Atlas werden **nicht** übertragen, es gilt der Stand der neuen Agentur.
  - Eine Agentur, die auf andere Weise leer wird (alle verlassen den Server dauerhaft), bleibt ruhend bestehen und kann über den Server-Befehl `/sternenwerft agency transfer <von> <an>` einer anderen Agentur übertragen werden.
  - Ehemalige Mitglieder kann man wieder einladen.
- **Fremde:** Nicht-Mitglieder haben an Block-Entities fremder Agenturen überall Gast-Rechte, auch außerhalb von Kuppeln.

| Rolle | Rechte |
| --- | --- |
| `leader` | alles, einladen, Rollen vergeben, Partner freigeben |
| `engineer` | bauen, Maschinen bedienen, Raketen starten, Routen ändern, forschen |
| `guest` | Kuppeln betreten, Container öffnen (Slot-Klicks werden serverseitig verworfen), nichts abbauen oder setzen |

### 23.2 Schutz

- In aktiven Kuppeln dürfen nur Mitglieder der Besitzer-Agentur (ab `engineer`) abbauen und setzen. Alle Spieler können in jeder Kuppel atmen.
- Raketen und Rover darf jeder besteigen, starten nur Mitglieder der Besitzer-Agentur.

### 23.3 Zwischen Agenturen

- `trading_post`: Block mit bis zu 8 Angeboten (gib X, erhalte Y) und eigenem Lager (54 Slots). Jeder Spieler kann tauschen, auch Gäste und Nicht-Mitglieder (Ausnahme von den Gast-Rechten in 23.1). Frachtraketen von Partneragenturen können ihn als Ziel nutzen.
- **Partnerschaft:** Leiter beider Agenturen bestätigen am Terminal. Partner dürfen freigegebene `cargo_terminal`s und Werften beliefern.
- **Gemeinsame Werft:** Alle Agenturen, die mindestens 1 Item geliefert haben, bekommen beim Start das Advancement.
- **Wettrennen:** Die erste Agentur, deren Mitglied ein Ziel betritt, wird serverweit angekündigt und im Atlas als Entdecker eingetragen.

### 23.4 Sitze

T1: 1, T2: 2, Landefähre: 2, Rover: 2, T3: 8.

### 23.5 Serverleistung

- Planetendimensionen werden nur geladen, solange dort ein Spieler ist oder eine Rakete landet (keine Force-Loads durch die Mod, außer 1 Chunk für 200 t bei Landungen).
- Kuppelprüfung: alle 10 t pro Spieler zusammen mit den Umweltwerten (8.1), nur gegen Kuppeln derselben Dimension.
- Frachtflüge und Außenposten nur rechnerisch (Kapitel 20).

---

## 24. Handbuch, Atlas, Erfolge, Einstellungen, Klang, Grafik

### 24.1 Ingenieurs-Handbuch

- `engineers_handbook`: jeder Spieler bekommt es beim ersten Join, Ersatz per Werkbank (22.1).
- Eigenes Buch-GUI im Notizbuch-Stil mit Blaupausen-Optik. Kapitel nach Phasen. Ein Kapitel öffnet sich mit der passenden Stufe.
- Seiten sind Ressourcen-Dateien unter `assets/<namespace>/handbook/` (Schema 25.6), damit andere Mods Kapitel ergänzen können.
- Maschinenseiten werden automatisch aus Rezept, Verbrauch und Kapitel 6 erzeugt. Planetenseiten füllen sich beim ersten Besuch.
- Suchfeld (filtert Seitentitel und Item-Namen).

### 24.2 Planetenatlas

`planetary_atlas`: Item, das jeder Spieler beim ersten Join bekommt (Ersatz per Werkbank, 22.1). Rechtsklick öffnet die GUI mit allen Zielen. Pro Ziel: Status (unbekannt, Sonde angekommen, besucht), Entdecker-Agentur, besuchte Biome, gefundene Erkundungsziele, Landefeuer. Inhalte gehören der Agentur.

### 24.3 Erfolge (Advancements, Reiter `sternenwerft:root`)

| ID | Titel DE | Bedingung |
| --- | --- | --- |
| `root` | Sternenwerft | Handbuch erhalten |
| `ground_station` | Bodenstation | `research_terminal` setzen |
| `first_signal` | Erstes Signal | Sonde erreicht `moon` |
| `small_step` | Ein kleiner Schritt | `moon` betreten |
| `red_earth` | Rote Erde | `mars` betreten |
| `three_minutes_of_hell` | Drei Minuten Hölle | `venus` unter y 180 betreten und danach über y 180 zurückkehren |
| `own_air` | Eigene Luft | ersten Kuppelgenerator aktivieren |
| `home_in_orbit` | Zuhause im Orbit | `station_core` setzen |
| `sun_in_a_bottle` | Sonne in der Flasche | Fusionsreaktor erzeugt Strom |
| `under_the_ice` | Unter dem Eis | `europa` unter y 112 im Wasser |
| `at_the_edge` | Am Rand | `pluto` betreten |
| `the_signal` | Der Sender | `signal_sentinel` besiegen |
| `network` | Netzwerk | 5 Routen der Agentur gleichzeitig aktiv |
| `starship` | Sternenwerft | Rakete T3 starten |

Erfolge, die eine Maschine oder ein Ereignis auslöst (`first_signal`, `own_air`, `sun_in_a_bottle`, `network`), bekommen alle Mitglieder der Besitzer-Agentur, wer offline ist beim nächsten Login. Eine Route gilt für `network` als aktiv, wenn ihr mindestens 1 Rakete zugewiesen ist und keine davon „ohne Treibstoff“ meldet.

Jeder Erfolg wird mit Zeitstempel in `logs/sternenwerft-progress.log` geschrieben (lokal, keine Telemetrie).

### 24.4 Einstellungen (`serverconfig/sternenwerft.json`)

| Schlüssel | Standard | Bereich |
| --- | --- | --- |
| `recipe_amount_multiplier` | 1.0 | 0.25–4.0 (gilt nur für Item-Zutaten mit Stapelgröße > 1 in Montagetisch-Rezepten; rundet auf, min. 1; ein Rezept wird nur so weit vergrößert, dass es noch in 9 Slots passt. Fluide, unstapelbare Zutaten, Pressformen und Werftabschnitte bleiben unverändert) |
| `oxygen_consumption` | 1.0 | 0.5–2.0 |
| `event_frequency` | `normal` | `off`, `rare`, `normal`, `frequent` |
| `meteor_block_damage` | true | |
| `planet_mobs` | true | |
| `outposts_offline` | true | |
| `outpost_offline_factor` | 0.5 | 0.0–1.0 |
| `research_required` | true | |

### 24.5 Grafik

- Vanilla-nah: 16×16-Texturen, Blockmodelle, klare Farben.
- **Platzhalter-Grundfarben nach Phase:** Phase 1 `#6E6E6E` (Stahlgrau) mit Gelb-Schwarz-Akzent; Phase 2–3 `#E8E8E8` (Weiß) mit Orange `#E07B20`; Phase 4–5 `#3A3F47` (Dunkelgrau) mit Kühlblau `#4FB3E8`; Phase 6 `#1A1420` (Schwarz) mit Violett `#9B4FE8`.
- Planetenblöcke (Grundfarbe): Mond `#8C8C8C`, Mond-Basalt `#3A3A3A`, Mars `#B5502A`, Gips `#E6DFD0`, Trockeneis `#F0F4F8`, Venus `#3B2F2A`, Merkur `#5A5550`, Asteroiden `#6B6B6B`, Ceres `#7A776F`, Salzkruste `#E8E8E0`, Io `#E8C440`, Europa-Eis `#E8EEF2` mit Rissen `#8A5A3A`, Titan `#A0622A`, Enceladus `#F4F8FF`, Triton `#C9A4A4`, Pluto-Eis `#D8C8A8`, Tholin `#5A3A2A`.
- Erze: Gesteinsfarbe mit Flecken der Metallfarbe (Aluminium `#C8CCD0`, Titan `#8A94A6`, Magnesium `#D8D8C0`, Nickel `#A8A890`, Wolfram `#4A4E58`, Lithium `#F0E8F0`, Platin `#E0E0E8`, Iridium `#B0C0D8`, Uran `#7AC040`, Schwefel `#E8D020`).
- Kabel: Kupfer orange, Aluminium silbern, Supraleiter hellblau. Rohre in der Fluidfarbe aus 5.3.
- Statuslampe als kleines Overlay oben auf jeder Maschine.

### 24.6 Klang

| Sound-Event | Verwendung |
| --- | --- |
| `sternenwerft:suit.breath` | Schleife im Vakuum ohne Kuppel |
| `sternenwerft:suit.radio_static` | leise Schleife im Vakuum |
| `sternenwerft:warning.oxygen`, `.heat`, `.cold`, `.radiation`, `.dome` | eigene, klar unterscheidbare Signaltöne |
| `sternenwerft:rocket.countdown`, `.launch`, `.landing` | Raketen |
| `sternenwerft:machine.<id>` | Maschinengeräusch (Schleife bei `running`) |
| `sternenwerft:music.<planet>` | ruhige Hintergrundmusik je Planet (Platzhalter: Vanilla-Musik `minecraft:music.game`) |

Im Vakuum ohne Kuppel werden alle Geräusche außer Anzug- und Warntönen mit Lautstärke × 0,15 abgespielt.

---

## 25. Datenformate

Dateien **eigener Registries** liegen unter `data/<namespace>/sternenwerft/<typ>/`, damit andere Mods und Datenpakete Inhalte ergänzen können. Rezepte, Advancements, Loot-Tables, Tags und Worldgen liegen in den Vanilla-Ordnern (`recipe/`, `advancement/`, `loot_table/`, `tags/`, `worldgen/` …). Alle eigenen Registries sind neu ladbar (`/reload`) und werden an Clients synchronisiert.

### 25.1 Planet (`sternenwerft:planet`, Ordner `planet/`)

```json
{
  "dimension": "sternenwerft:moon",
  "type": "surface",
  "orbit": "sternenwerft:earth_orbit",
  "star_system": "sternenwerft:sol",
  "ring": 1,
  "distance_points": 0,
  "gravity": 0.17,
  "atmosphere": "none",
  "pressure": "vacuum",
  "temperature": { "day": 120, "night": -170 },
  "biome_temperature_offsets": {},
  "biome_temperature_overrides": { "sternenwerft:moon_polar_crater": -190 },
  "radiation": 1,
  "solar_factor": 1.0,
  "water_behavior": "freeze",
  "landing_requires_fuel": true,
  "required_rocket_tier": 1,
  "requires_long_range": false,
  "probe": "sternenwerft:probe",
  "sky": {
    "color": "#000000",
    "fog_color": "#000000",
    "fog_distance": null,
    "stars_in_daylight": true,
    "sun_scale": 1.0,
    "celestial_bodies": [
      { "texture": "sternenwerft:textures/sky/earth.png", "size": 30, "azimuth": 60, "elevation": 40 }
    ]
  },
  "gas_collector_output": null,
  "zones": [
    {
      "y_min": -64, "y_max": 400,
      "biome": "sternenwerft:moon_polar_crater",
      "near_block": null,
      "temperature": -190, "pressure": null, "radiation": null,
      "solar_factor": null, "atmosphere": null, "water_behavior": null
    }
  ]
}
```

- **Einträge:** Es gibt einen Eintrag pro Ziel aus 12.1: die Erde (`minecraft:overworld`, g 1,0, DP 0, Ring 0, atembar, ohne Gefahren, Temperatur Nacht/Tag 10/20 °C für die Kuppelprüfung), 12 weitere Oberflächen und 8 Orbits. `type` ist `surface` oder `orbit`. Orbits setzen `gravity: 0.005`, `atmosphere: "none"` und ihren Solarfaktor (13.1). Gas-Sammler-Orbits setzen `gas_collector_output` (`{ "fluid": "...", "amount": 100, "ticks": 20, "alternative": "..." }`).
- **Zonen** überschreiben einzelne Werte in einem Bereich. `null` heißt „Planetenwert behalten“. **Vorrang:** Planetenwert → `biome_temperature_overrides` → alle passenden Zonen in Listenreihenfolge; spätere Nicht-null-Werte gewinnen. Allgemeinere Zonen stehen deshalb vorn (z. B. Europa-Ozean vor Europa-Tiefsee). `biome` und `near_block` sind optional; `near_block` gilt im Umkreis von 4 Blöcken.
- **Pflicht-Zonen:**
  - Venus-Wolkenschicht: y 180–240, 20 °C, `normal`, Solar 2,0.
  - Europa-Ozean: y −64–111, +2 °C, `normal`, Strahlung 0, `water_behavior: normal`.
  - Europa-Tiefsee: y −64–79, Druck `deep`.
  - Io an Lava: `near_block: minecraft:lava`, +300 °C.
  - Mond-Polarkrater: −190 °C (wie im Beispiel oben).
  - Merkur-Biome laut 13.4.

### 25.2 Sonnensystem (`sternenwerft:star_system`, Ordner `star_system/`)

```json
{
  "name": "sternenwerft.star_system.sol",
  "star_color": "#FFF4D6",
  "rings": [
    { "index": 0, "planets": ["minecraft:overworld"] },
    { "index": 1, "planets": ["sternenwerft:moon", "sternenwerft:mars", "sternenwerft:venus"] }
  ],
  "starship_jump_cost": 1,
  "unlock_requirement": null
}
```

Weitere Systeme setzen `unlock_requirement` (z. B. ein Item oder Advancement) und erscheinen in der Zielliste der T3.

### 25.3 Forschungsknoten (Ordner `research/`)

```json
{
  "tier": 2,
  "branch": "industry",
  "cost": 25,
  "requires": ["sternenwerft:regolith_heater"],
  "unlocks": [
    { "type": "item", "id": "sternenwerft:cryo_liquefier" },
    { "type": "item", "id": "sternenwerft:cooling_system" }
  ],
  "icon": "sternenwerft:cryo_liquefier",
  "position": { "column": 0, "row": 2, "slot": 3 }
}
```

`position.column` = Ast (0 Industrie, 1 Raumfahrt, 2 Lebenserhaltung, 3 Logistik), `position.row` = Stufe (1–6), `position.slot` = Reihenfolge innerhalb der Zelle.

Stufen-Tore stehen in `research_tier/<n>.json`: `{ "tier": 2, "cost_default": 25, "gate": { "type": "enter_dimension", "dimension": "sternenwerft:moon" } }`. **Gate-Typen und Felder:**

| Typ | Felder | Erfüllt, wenn |
| --- | --- | --- |
| `has_block` | `block` | ein Block dieser Art mit `owner_agency` = Agentur existiert (zählt beim Platzieren) |
| `enter_dimension` | `dimension` | ein Mitglied die Dimension betreten hat |
| `crafted_items` | `items: [ID…]`, `fluids: [ID…]` | jedes Item bzw. Fluid wurde mindestens einmal hergestellt. Zählt für Spieler-Crafting und für Maschinen mit `owner_agency` = Agentur. |
| `machine_completed` | `block`, optional `recipe` | eine Maschine der Agentur hat einen Vorgang abgeschlossen |
| `all` | `gates: [Gate…]` | alle Teil-Gates erfüllt |

- Stufe n setzt immer Stufe n − 1 voraus.
- Ein einmal geöffnetes Tor bleibt offen, auch wenn z. B. das `research_terminal` abgebaut wird.

**Knoten:**
- `branch`: `industry`, `spaceflight`, `life_support`, `logistics`.
- `unlocks`: Liste von `{ "type": "item" | "probe_target" | "multiblock", "id": … }`. `item` schaltet alle Rezepte mit diesem Ergebnis frei (11.3). `probe_target` schaltet ein Sondenziel frei (z. B. `sternenwerft:mars`). Erde und `earth_orbit` sind immer bekannt.
- Der Knoten ist die **einzige** Quelle für Freischaltungen; Rezepte haben kein eigenes Forschungsfeld.

### 25.4 Rezepte

Eigene Rezepttypen (`RecipeType` + `RecipeSerializer` über Codecs): `sternenwerft:crushing`, `electric_smelting`, `high_temp_smelting`, `pressing`, `refining`, `electrolysis`, `cryo`, `zero_g`, `assembly`.

```json
{
  "type": "sternenwerft:assembly",
  "ingredients": [
    { "item": "sternenwerft:aluminum_plate", "count": 40 },
    { "tag": "c:plates/titanium", "count": 16 },
    { "item": "sternenwerft:circuit", "count": 16 }
  ],
  "fluid": null,
  "results": [ { "id": "sternenwerft:rocket_capsule_t1", "count": 1, "chance": 1.0 } ],
  "time": 200,
  "energy_per_tick": 30
}
```

Maschinenrezepte haben zusätzlich `fluid_inputs` und `fluid_outputs` (`{ "fluid": "...", "amount": n }`). `pressing` hat `mold`. `results` erlaubt mehrere Ausgaben mit `chance` (0–1), z. B. `metal_concentrate` → Aluminium- und Titanstaub oder `meteorite_ore` mit 10 % Platin. Beim Montagetisch steht ein „Tank X“ als `"fluid": { "fluid": "X", "amount": 16000 }`. `meteorite_ore` droppt beim Abbau sich selbst, Rohstoffe gibt es nur über den Zerkleinerer.

**Komponenten in Rezepten:**
- Eine Zutat darf `"components": { … }` tragen; dann passt nur ein Item mit genau diesen Werten (z. B. `antimatter_cell` mit `"sternenwerft:charge": 1`).
- Ein Ergebnis darf `"copy_components_from": <Zutat-Index>` tragen; dann übernimmt es alle Data Components dieser Zutat (Anzug aufwerten: `suit_modules`, `oxygen`; Antrieb nachrüsten: Ladung der Rakete; `heavy_freighter` aus `cargo_rocket`).
- Die Sondertypen `suit_module_install` (Anzugteil + Modul → Anzugteil mit Modul) und `suit_module_remove` (Anzugteil → Anzugteil ohne Module + Module in die Nebenausgaben) brauchen keine JSON-Einträge je Kombination; sie sind fest im Code und gelten für alle Anzugteile und Module aus 9.2.

### 25.5 Raketenteil (Ordner `rocket_part/`)

```json
{ "block": "sternenwerft:rocket_tank_t1", "role": "tank", "tier": 1, "mass": 2.0, "fuel_capacity": 16000, "fuel_mass_full": 2.0, "thrust": 0, "cargo_slots": 0, "mass_per_slot": 0.0, "seats": 0 }
```

Rollen: `capsule`, `tank`, `engine`, `fin`, `cargo`, `long_range`. `fuel_capacity` gilt je Komponente. Die Treibstoffmasse wächst linear mit dem Füllstand bis `fuel_mass_full`. Frachtraum: `mass` 1,0, `mass_per_slot` 0,05 je belegtem Slot. Kapsel: `seats` 1 (T1) bzw. 2 (T2). Fernflug-Modul: `mass` 4,0, `fuel_capacity` 128000, `fuel_mass_full` 4,0.

### 25.6 Handbuchseite (`assets/<ns>/handbook/<kapitel>/<seite>.json`)

`unlock`: `null` = immer sichtbar (Phase 1). Sonst die ID eines Stufen-Tors (`sternenwerft:research_tier/<n>`) oder eines Forschungsknotens.

```json
{
  "chapter": "sternenwerft:phase_1",
  "title": "sternenwerft.handbook.crusher.title",
  "unlock": null,
  "elements": [
    { "type": "text", "key": "sternenwerft.handbook.crusher.text" },
    { "type": "recipe", "recipe": "sternenwerft:crusher" },
    { "type": "image", "texture": "sternenwerft:textures/handbook/crusher_setup.png", "width": 120, "height": 60 }
  ]
}
```

---

### 25.7 Balance-Dateien (`data/<ns>/sternenwerft/balance/`)

Alle Zahlenwerte, die nicht in Rezepten, Planeten, Forschung oder Raketenteilen stehen, liegen in diesen Dateien. Sie werden beim Laden und bei `/reload` eingelesen und an Clients gesendet. Datagen erzeugt sie aus den Tabellen dieses Dokuments.

| Datei | Inhalt (Quelle) | Schlüssel |
| --- | --- | --- |
| `machines.json` | Verbrauch, Dauer, Puffer, Ortsbindung, Upgrades (4.1, 4.2, 6.1) | Block-ID |
| `generators.json` | Leistung, Brennstoff, Speicher, Reaktor-Sicherheit (6.3) | Block-ID |
| `transport.json` | Kabel- und Rohrdurchsatz (4.4) | Block-ID |
| `environment.json` | Belastungszeit, Schadensraten, Schutz ohne Anzug (8.2, 8.3) | Gefahr |
| `suits.json` | Anzugstufen und Module (9.1, 9.2) | Stufe bzw. Modul-ID |
| `domes.json` | Kuppelstufen, Module, Ausfallzeit (10.1, 10.2) | Block-ID |
| `spaceflight.json` | Treibstoffformel, Schub, Sondenzeiten, Landefähre (12.2–12.7) | Schlüssel aus der Formel |
| `vehicles.json` | Rover, Rover-Module (16) | Entity- bzw. Item-ID |
| `creatures.json` | Lebenspunkte, Schaden, Tempo, Spawn (18) | Entity-ID |
| `events.json` | Ereignisse, Chancen, Dauer, Vorwarnung (19) | Ereignis-ID |
| `logistics.json` | Frachtraketen, Flugzeit, Offline-Produktion (20) | Item-ID bzw. Schlüssel |
| `shipyard.json` | Bauabschnitte, Bauzeit (21) | Abschnittsnummer |
| `research.json` | FP-Werte je Quelle, Abnahme, Labor-Bonus, Abklingzeit `sample_tool` (11.1, 11.2) | Quelle |
| `agriculture.json` | Wachstumszeiten, Dünger, Gewächshaus, Ration (17, 14.2) | Block- bzw. Item-ID |
| `stations.json` | Stationskern (Solar, O₂-Tank), Schwerkraftplatte, Klon-Kammer (14) | Block-ID |
| `items.json` | Werkzeug- und Weltwerte: `ice_drill`, `asteroid_anchor`, `hydrothermal_vent`-Abklingzeit, Geysire (13.4) | Item- bzw. Block-ID |

Beispiel `machines.json`:

```json
{
  "sternenwerft:crusher": { "energy_per_tick": 20, "ticks": 200, "buffer": null, "location": null },
  "sternenwerft:zero_g_fabricator": { "energy_per_tick": 100, "ticks": 200, "buffer": null, "location": { "dimension_type": "orbit" } }
}
```

`buffer: null` heißt „Formel aus 4.1 verwenden“.

- **Zusammenführen:** Gleiche Dateien aus mehreren Datenpaketen werden pro Schlüssel zusammengeführt; bei gleichem Schlüssel gewinnt das Paket mit höherer Priorität.
- **`misc.json`** enthält alle übrigen Einzelwerte, z. B. Wartezeit des Wolkenaufzugs (100 t, 12.8), Schwerkraftformel (12.9), Langsamer Fall nach Sturz (400 t, 14.3), Vakuum-Dämpfung (0,15, 24.6), Belastungszeit (200 t).

## 26. Registry-Übersicht

**Dimensionen:** `moon`, `mars`, `venus`, `mercury`, `asteroid_belt`, `ceres`, `io`, `europa`, `titan`, `enceladus`, `triton`, `pluto`, `earth_orbit`, `venus_orbit`, `mars_orbit`, `mercury_orbit`, `jupiter_orbit`, `saturn_orbit`, `uranus_orbit`, `neptune_orbit`.

**Entities:** `rocket`, `lander`, `rover`, `rover_trailer`, `probe_entity`, `meteor`, `starship`, `seat` (unsichtbarer Sitz, 12.5), `sulfur_glob` (Projektil des Schwefelsalamanders), alle 15 Kreaturen aus Kapitel 18.

**Block-Entities:** alle Maschinen, Generatoren, Speicher, Kabel, Rohre, Multiblock-Controller, Ports, `research_terminal`, `launch_control`, `landing_beacon`, `cloud_elevator`, `hydroponics_basin`, Stationsmodule, `cargo_terminal`, `logistics_computer`, `trading_post`, `shipyard_terminal`, `shipyard_dock`, `transmitter_core`. Raketenteile sind normale Blöcke ohne Block-Entity, außer `rocket_cargo_bay`, `rocket_tank_*` und `long_range_module` (speichern Fracht bzw. Treibstoff). Rover-Teile sind Items.

**Weitere Blöcke ohne eigenes Kapitel:** `sulfur_trail` (18), `extinguished_torch` (8.5), `electric_lamp` (8.5), `pontoon` (18), `meteorite_ore` (19), `ancient_alloy` (13.4), `compost_soil` und `compost_farmland` (17).

**Menüs (GUI):** je Maschine eines, dazu `research_terminal`, `launch_control`, `star_map`, `rocket`, `rover`, `logistics_computer`, `shipyard_terminal`, `trading_post`, `planetary_atlas`, `engineers_handbook`.

**Data Components:** `sternenwerft:fluid` (Fluid + Menge), `oxygen`, `energy`, `suit_modules`, `sample` (Block + Dimension), `charge`, `nutrition`, `linked_pos`, `drive` (Antrieb einer Frachtrakete), `rover_data` (Fracht, Module und Energie eines abgebauten Rovers), `flights_left` (`heavy_freighter`), `chart_owner` (`star_chart`).

**Tags:** `sternenwerft:gases`, `sampleable/ring0`, `sampleable/planet`, `analyzable` (die 15 Kreaturen aus 18), `basalt`, `crawler_food`, `vibrating`, `compostable`, `planet_soil`, alle `c:`-Materialtags.

---

## 27. Meilensteine und Abnahmekriterien

Jeder Meilenstein MUSS kompilieren, auf beiden Loadern starten (Client + dedizierter Server) und seine GameTests bestehen.

GameTests laufen in der Overworld. Umweltabhängige Tests (Vakuum, Hitze, Kuppel, Venus) prüfen deshalb `EnvironmentCalculator` (8.1) direkt oder nutzen einen Override „Planet + Zone für diesen Testbereich“, der nur in GameTests aktiv ist. Der Override setzt auch die Ortsbindung (z. B. „gilt als Orbit“ oder „nicht Erde“) für Maschinen mit Ortsbindung. Wo möglich, wird Logik als reine Funktion getestet. Zeitangaben in Tests dürfen bis 10 % Toleranz haben.

| # | Inhalt | Abnahmekriterien (GameTests oder manuelle Prüfung) |
| --- | --- | --- |
| 1 | Grundgerüst: Multiloader, Registrierung, Energie-/Fluid-Abstraktion mit Transaktionen, Netzwerk, Config, Datagen, Loader für Balance-Dateien (25.7), `machine_frame`, `crusher`, `combustion_generator`, `copper_cable`, Platzhalter-Skript | GT: Generator mit Kohle → Kabel → Zerkleinerer macht aus 1 `minecraft:raw_iron` 2 `iron_dust` in höchstens 220 t. GT: Wert in `machines.json` ändern + `/reload` ändert die Vorgangsdauer. GT: Ein Test-Verbraucher, der nur die Plattform-API nutzt (NeoForge: Energy-Capability, Fabric: Team Reborn Energy), erhält Strom aus dem Kabel. |
| 2 | Erdindustrie: alle Phase-1-Materialien, Erze, Worldgen-Ergänzungen, Maschinen, Generatoren, Speicher, Rohre, Raffinerie-Multiblock, Upgrades, JEI/REI/EMI | GT: jede Maschine verarbeitet ihr Beispielrezept. GT: Raffinerie erkennt die Struktur und meldet `error` bei fehlendem Block. Manuell: Bauxit spawnt in der Savanne. |
| 3 | Erste Welt: Planeten-Registry, `moon` mit Biomen und Himmel, Schwerkraft, Umweltsystem, HUD, Anzug T1, Sauerstoffgenerator, Kuppel T1, Füll- und Leerslots, **Agentur-Kern** (SavedData, automatische Agentur je Spieler, `owner_agency` an allen Block-Entities) | GT: Spieler ohne Anzug im Mond-Vakuum erreicht 100 % Belastung nach genau 200 t. GT: Spieler in aktiver Kuppel hat `breathable`. GT: Kuppel ohne O₂ fällt nach 600 t aus. |
| 4 | Erster Flug: Forschung, Proben, Analysator, Sonde, Startrampe, Rakete T1, Flugablauf, Landefeuer, Atlas. **Erste spielbare Version** | Manuell: neue Welt → Mond und zurück ohne Befehle. GT: Treibstoffkosten Erde → Mond = 35.360 mB. GT: Rakete mit 3 Flossen startet nicht. |
| 5 | Ring 1: Mars, Venus (Wolkenschicht, Aerostat, Aufzug), Rakete T2, Anzug T2, Rover, Kreaturen Ring 1, Ereignisse, Nahrung | GT: Venus-Hitzezeit 3.600 t. Manuell: Helium, Magnesium und Wolfram gewinnen öffnet Stufe 3. |
| 6 | Orbit: Orbit-Dimensionen, Station, Module, Schwerelosigkeit, Fusion, Landefähre, Fernflug-Modul | GT: Fusion schaltet 40 t nach Kühlungsausfall ab. GT: `zero_g_fabricator` außerhalb des Orbits = `error`. |
| 7 | Ring 2: alle Ziele Merkur bis Pluto, Kreaturen, Sender, Boss, Strahlung | Manuell: jedes Ziel ist mit der vorgesehenen Ausrüstung betretbar und ohne sie tödlich (mit Warnung). |
| 8 | Netzwerk und Finale: Außenposten, Bohrturm, Pumpstation, Frachtraketen, Logistik-Computer, Kernreaktor, Antriebe, Werft, T3, Finale | GT (reine Funktion): `OfflineProduction.compute(running = true, Rate 1 Item/100 t, Faktor 0,5, 24.000 t)` = 120 Items. GT mit Override „nicht Erde“: Bohrturm über 10 Eisenerz-Blöcken läuft (`running`). GT: Route „bis Bedarf gedeckt“ stoppt bei 0 Bedarf. |
| 9 | Feinschliff: Agentur-GUI, Rollen, Partner, Handel, Handbuch, Advancements, Klang, Balancing-Skript, Übersetzungen | `tools/balance_sheet.py` liest alle Rezept-JSONs und gibt die Rohstoffsumme je Rakete und Werftabschnitt aus. Regeln: Rohstoff = Item oder Fluid ohne eigenes Rezept. Bei mehreren Rezepten zählt das erste. Nebenprodukte werden ignoriert. Fluide werden in mB summiert. Rezepte werden alphabetisch nach ID sortiert; Mengen werden durch die Ausgabemenge geteilt; Pressformen (`mold`) werden ignoriert; bei Tags zählt das erste Element des Tags. |

**Balancing-Ziele** (Einzelspieler, Standardeinstellungen, gesamt 50–80 h):

| Phase | Zielzeit | Strombedarf am Ende |
| --- | --- | --- |
| 1 | 4–6 h | ~100 J/t |
| 2 | 3–4 h | ~200 J/t |
| 3 | 8–12 h | ~500 J/t |
| 4 | 6–8 h | ~2.000 J/t |
| 5 | 15–25 h | ~5.000 J/t |
| 6 | 15–25 h | ~10.000 J/t |

Grundregeln: Jede Raketenstufe ≈ 3× Material der vorigen, T3 ≈ 10× T2. Ab Phase 3 dauert ein Schlüsselmaterial von Hand ≥ 3× so lang wie mit einfacher Automatisierung. Etwa die Hälfte der FP aus Erkundung. Jede Gefahr warnt ≥ 200 t vorher. Kein Engpass nur durch Warten.

---

## 28. Offene Punkte (nicht von der KI zu entscheiden)

- [ ] Änderungen Ä1–Ä22 (Kapitel 1) bestätigen oder anpassen.
- [ ] Direkt vor dem Entwicklungsstart prüfen: Ist 26.4 inzwischen stabil und von NeoForge und Fabric stabil unterstützt? Wenn ja, Kapitel 3.1 auf 26.4 anheben und dessen Änderungsliste (Fabric-Blogpost, NeoForge-Primer) einarbeiten. Sonst bei 26.3 bleiben.
- [ ] Mengen und Zeiten nach Testplan im Spiel prüfen (3–5 Tester, Multiplikator 1,0 und 0,5).
- [ ] Echte Texturen, Modelle und Kreaturen in Blockbench nach 24.5 erstellen (ersetzt die Platzhalter).
- [ ] Englische Namen von einem Muttersprachler gegenlesen lassen.
- [ ] Musik komponieren oder lizenzieren.

---

## Anhang A: Namen DE / EN

Übersetzungen für `de_de` und `en_us`. Alle hier nicht gelisteten IDs: englischer Name = ID in Title Case, deutscher Name aus den Tabellen dieses Dokuments.

| ID | Deutsch | Englisch |
| --- | --- | --- |
| `crusher` | Zerkleinerer | Crusher |
| `electric_furnace` | Elektroofen | Electric Furnace |
| `press` | Presse | Press |
| `refinery_controller` | Raffinerie | Refinery |
| `electrolyzer` | Elektrolyseur | Electrolyzer |
| `fluid_pump` | Pumpe | Fluid Pump |
| `assembly_table` | Montagetisch | Assembly Table |
| `high_temperature_furnace` | Hochtemperaturofen | High-Temperature Furnace |
| `battery`, `fluid_tank`, `gas_tank`, `pressure_bottle` | Akku, Tank, Gastank, Druckflasche | Battery, Fluid Tank, Gas Tank, Pressure Bottle |
| `combustion_generator` | Verbrennungsgenerator | Combustion Generator |
| `fuel_generator` | Treibstoffgenerator | Fuel Generator |
| `solar_panel` | Solarpanel | Solar Panel |
| `analyzer` | Analysator | Analyzer |
| `research_terminal` | Forschungsterminal | Research Terminal |
| `launch_pad`, `launch_tower`, `launch_control`, `fuel_pump` | Startrampe, Startturm, Startkontrolle, Treibstoffpumpe | Launch Pad, Launch Tower, Launch Control, Fuel Pump |
| `oxygen_generator` | Sauerstoffgenerator | Oxygen Generator |
| `dome_generator_t1..t3` | Kuppelgenerator I–III | Dome Generator I–III |
| `regolith_heater` | Regolith-Erhitzer | Regolith Heater |
| `cryo_liquefier` | Kryo-Verflüssiger | Cryo Liquefier |
| `fusion_reactor` | Fusionsreaktor | Fusion Reactor |
| `fission_reactor` | Kernreaktor | Fission Reactor |
| `station_core` | Stationskern | Station Core |
| `zero_g_fabricator` | Schwerelos-Fertigung | Zero-G Fabricator |
| `gas_collector` | Gas-Sammler | Gas Collector |
| `magnetic_trap` | Magnetfalle | Magnetic Trap |
| `drilling_rig` | Bohrturm | Drilling Rig |
| `pump_station` | Pumpstation | Pump Station |
| `cargo_terminal` | Frachtterminal | Cargo Terminal |
| `logistics_computer` | Logistik-Computer | Logistics Computer |
| `shipyard_terminal`, `shipyard_dock` | Werft-Terminal, Werft-Andockbucht | Shipyard Terminal, Shipyard Dock |
| `clone_chamber` | Klon-Kammer | Clone Chamber |
| `hydroponics_basin` | Hydrokultur-Becken | Hydroponics Basin |
| `food_packer` | Nahrungspacker | Food Packer |
| `space_weather_station` | Weltraumwetter-Station | Space Weather Station |
| `trading_post` | Handelsposten | Trading Post |
| `landing_beacon` | Landefeuer | Landing Beacon |
| `asteroid_anchor` | Asteroiden-Anker | Asteroid Anchor |
| `cloud_elevator` | Wolkenaufzug | Cloud Elevator |
| `aerostat_platform` | Aerostat-Plattform | Aerostat Platform |
| `circuit`, `advanced_circuit`, `iridium_chip` | Schaltkreis, Fortgeschrittener Schaltkreis, Iridium-Chip | Circuit, Advanced Circuit, Iridium Chip |
| `superconductor_wire` | Supraleiterdraht | Superconductor Wire |
| `pressure_hull` | Druckhülle | Pressure Hull |
| `magnesium_alloy`, `lightweight_plate` | Magnesium-Legierung, Leichtbau-Platte | Magnesium Alloy, Lightweight Plate |
| `heat_shield`, `cooling_system`, `ceramic_plate` | Hitzeschild, Kühlsystem, Keramikplatte | Heat Shield, Cooling System, Ceramic Plate |
| `lithium_battery` | Lithium-Akku | Lithium Battery |
| `oxygen_canister` | Sauerstoffflasche | Oxygen Canister |
| `pressure_suit`, `heat_suit`, `deep_space_suit` | Druckanzug, Hitzeanzug, Tiefraumanzug | Pressure Suit, Heat Suit, Deep Space Suit |
| `space_ration` | Weltraumration | Space Ration |
| `star_chart` | Sternenkarte | Star Chart |
| `engineers_handbook` | Ingenieurs-Handbuch | Engineer's Handbook |
| `planetary_atlas` | Planetenatlas | Planetary Atlas |
| `probe`, `advanced_probe` | Sonde, Fortgeschrittene Sonde | Probe, Advanced Probe |
| `long_range_module` | Fernflug-Modul | Long-Range Module |
| `starship` | Sternenschiff (Rakete T3) | Starship |
| `lander` | Landefähre | Lander |
| `rover_kit` | Rover-Bausatz | Rover Kit |
| `cargo_rocket`, `heavy_freighter` | Frachtrakete, Schwerlast-Frachter | Cargo Rocket, Heavy Freighter |
| `rubble_crawler` | Geröllkrabbler | Rubble Crawler |
| `shade_strider` | Schattenläufer | Shade Strider |
| `dune_burrower` | Dünenwühler | Dune Burrower |
| `dust_wraith` | Staubgeist | Dust Wraith |
| `sulfur_snail` | Schwefelschnecke | Sulfur Snail |
| `cloud_ray` | Wolkenrochen | Cloud Ray |
| `mirror_beetle` | Spiegelkäfer | Mirror Beetle |
| `drift_mite` | Schwebemilbe | Drift Mite |
| `sulfur_salamander` | Schwefelsalamander | Sulfur Salamander |
| `ice_jelly` | Eisqualle | Ice Jelly |
| `reef_keeper` | Riffwächter | Reef Keeper |
| `methane_skimmer` | Methanläufer | Methane Skimmer |
| `geyser_moth` | Geysirmotte | Geyser Moth |
| `frost_lurker` | Frostkriecher | Frost Lurker |
| `signal_sentinel` | Wächter des Senders | Signal Sentinel |

## Anhang B: Stimmung (nur Kontext, nicht verbindlich)

- **Mond:** still, leer, einsam. Grauer Regolith, schwarzer Himmel, die Erde groß am Himmel.
- **Mars:** wild und weit. Roter Sand, rostige Felsen, blaue Sonnenuntergänge.
- **Venus:** feindselig und bedrückend. Die Prüfung am Ende von Ring 1.
- **Merkur:** riesige Sonne, verbrannter Fels, Leben nur auf der Dämmerungslinie.
- **Europa:** das erste echte Ökosystem, leuchtendes Leben unter dem Eis.
- **Pluto:** das Ende des Systems. Der Sender ist der Abschluss der Erkundung und der Anschluss für spätere Sternenreisen.
- **Grafikstil:** Man sieht jeder Maschine an, aus welcher Phase sie stammt: raue Industrie → saubere Raumfahrt → glatte Titanhüllen → schimmernde Antimaterie-Technik.
