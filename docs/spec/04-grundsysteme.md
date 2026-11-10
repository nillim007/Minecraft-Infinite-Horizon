# 4. Grundsysteme

## 4.1 Maschinen-Grundverhalten (alle Maschinen)

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

## 4.2 Upgrades

| Item | Slot | Wirkung pro Stück | Max. Stapel |
| --- | --- | --- | --- |
| `speed_upgrade` | 1 | Vorgangsdauer × 0,75, Verbrauch J/t × 1,5 | 4 |
| `efficiency_upgrade` | 2 | Verbrauch J/t × 0,85 | 4 |

Die Wirkung multipliziert sich. Upgrades gelten für alle Maschinen, Generatoren nicht.

## 4.3 Multiblöcke

- Ein Multiblock besteht aus einem **Controller** (teuer) und **Gehäuseblöcken** (billig). Gehäuseblöcke dürfen durch Port-Blöcke ersetzt werden: `item_port`, `fluid_port`, `energy_port`. Ein Port leitet nach außen an den Controller weiter. Ports brauchen keine Forschung (die Raffinerie nutzt sie schon in Phase 1). Der Controller nimmt an seinen freien Seiten ebenfalls Energie, Fluide und Items an und gibt Ausgaben ab.
- Der Controller prüft die Struktur beim Platzieren, bei Blockänderungen im Umkreis und alle 100 t. Ist sie unvollständig, steht die Statuslampe auf `error`.

| Multiblock | Maße (B×T×H) | Aufbau |
| --- | --- | --- |
| `refinery` | 3×3×5 | massiv; Controller unten Mitte einer Seitenfläche; Rest `refinery_casing` oder Ports |
| `fusion_reactor` | 5×5×5 | hohle Hülle (98 Blöcke), Innenraum 3×3×3 Luft; Controller Mitte einer Seitenfläche; Rest `fusion_casing` oder Ports |
| `fission_reactor` | 5×5×5 | wie Fusion, mit `fission_casing` |
| `launch_pad_t1` | 5×5 Plattform | 25 `launch_pad`; dazu eine Säule `launch_tower` direkt neben der Plattform, mindestens so hoch wie die Rakete; `fuel_pump` an Plattform oder Turm angrenzend; `launch_control` im Umkreis von 8 Blöcken |
| `launch_pad_t2` | 7×7 Plattform | wie T1 mit 49 `launch_pad` |

## 4.4 Kabel und Rohre

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
