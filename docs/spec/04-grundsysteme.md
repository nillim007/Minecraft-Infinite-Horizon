# 4. Grundsysteme

## 4.1 Maschinen-Grundverhalten (alle Maschinen)

- **GUI** im Vanilla-Stil: Eingabeslots links, Ausgabe rechts, Energieleiste links außen, Fluidleisten rechts außen, 2 Upgrade-Slots unten rechts, Fortschrittspfeil in der Mitte. Einheitliches Layout für alle Menüs:
  - Breite 176 px, Höhe 166 px (bei mehr Slots 18 px je zusätzliche Zeile), Spieler-Inventar unten mit Vanilla-Abständen.
  - Energieleiste 8 × 60 px links (x 8). Fluidleisten 16 × 60 px rechts (x 152); bei mehreren Tanks nach links versetzt im Abstand von 18 px.
  - Eingabe links der Mitte, Ausgabe rechts, Fortschrittspfeil 24 × 17 px in der Mitte.
  - Upgrade-Slots nur, wenn `upgrade_slots` in den Daten größer als 0 ist (Generatoren, `battery` und `fluid_tank` haben keine). Die Presse hat zusätzlich einen Formslot, der Montagetisch 9 Eingabeslots, 3 Ausgabeslots und eine Fluidleiste. Generatoren zeigen die Brennzeit statt des Fortschrittspfeils, `battery` ihren Ladeslot.
  - Knopf „Seite“ schaltet die Ausgangsseite durch (absolute Seiten: Unten, Oben, Nord, Süd, West, Ost). Knopf „Tank“ schaltet Tank 1 bis n durch. Beide erscheinen nur, wenn es etwas zu wählen gibt.
  - Tooltips und Beschriftungen stehen in `lang_extra.json` (6.4). Das Feldformat von `MachineConfigPayload` steht in 3A.5.
- **Automatisierung:** Von jeder Seite aus gehen Items und Fluide in Eingabeslots und Eingabetanks. Von jeder Seite aus lassen sich nur Ausgaben entnehmen.
  - **Ausgangsseite für Fluide:** Fluid-Ausgaben von Maschinen, Controllern und Speichern gehen nur über eine gewählte Ausgangsseite in angrenzende Rohre und Tanks (Standard: `facing`, im GUI wählbar). Diese Seite nimmt selbst keine Fluide an; alle anderen Seiten nehmen nur an. So fließt z. B. der Wasserstoff des Elektrolyseurs nicht in das Wasserrohr zurück, das ihm Wasser liefert.
  - **Energie:** Generatoren schieben in alle Seiten (4.4), Speicher nur über ihre Ausgangsseite (4.4). Maschinen geben keine Energie ab.
  - **Trichter und Slots:** Eingaben füllen den ersten passenden Slot (erst Stapel auffüllen, dann der nächste leere Slot). Den Formslot der Presse füllt ein Trichter nur mit `press_mold_*`-Items, Upgrade-Slots nie. Der Brennstoffslot eines Generators zählt als Eingabeslot. Shift-Klick im GUI: Upgrade → Upgrade-Slot, Pressform → Formslot, sonst Eingabe.
- **Füll- und Leerslots:** Jeder Block mit Fluidtank (Maschinen mit Fluid-Ein- oder -Ausgang, `fluid_tank`, `pressure_bottle`, `gas_tank`, `tank_depot`, `oxygen_generator`, `dome_generator_*`, `cargo_terminal`, Raketen-GUI) hat ein Slotpaar:
  - **Füllslot:** Ein Behälter-Item (`transport_tank`, `oxygen_canister`, `large_oxygen_canister`, Eimer) wird *aus* dem Block gefüllt.
  - **Leerslot:** Ein gefülltes Behälter-Item wird *in* den Block entleert.
  - Der Behälter bleibt jeweils im Slot liegen, bis man ihn herausnimmt oder ein Trichter ihn abzieht.
  - Behälter speichern Fluid und Menge in der Data Component `sternenwerft:fluid`.
  - **Mehrere Tanks:** Der Füllslot nimmt aus dem ersten Ausgabetank, dessen Fluid zum Behälter passt; für einen leeren Behälter gilt der im GUI gewählte Tank (Knopf „Tank wählen“, Standard Tank 1). Der Leerslot gibt in den ersten passenden Eingangstank.
  - **Trichter** ziehen aus dem Füllslot nur volle und aus dem Leerslot nur leere Behälter.
  - **Eimer (ab M2):** Füll- und Leerslot nehmen `minecraft:bucket`, `minecraft:water_bucket`, `minecraft:lava_bucket`, `crude_oil_bucket` und `kerosene_bucket` an. Die Übergabe geschieht immer ganz (1.000 mB) und sofort (1 Tick). Füllslot: leerer Eimer + Tank mit mindestens 1.000 mB eines Fluids mit Eimer → gefüllter Eimer. Leerslot: gefüllter Eimer + Platz für 1.000 mB im Eingangstank → leerer Eimer (`minecraft:bucket`). Gase gibt es nicht im Eimer (3.3).
  - **Neue Eimer:** `crude_oil_bucket` und `kerosene_bucket` (`max_stack` 1) sind eigene Items ohne Komponente. `crude_oil_bucket` entsteht im Füllslot oder durch Rechtsklick eines leeren Eimers auf einen `crude_oil`-Quellblock und lässt sich platzieren. `kerosene` hat keinen Quellblock; `kerosene_bucket` entsteht nur im Füllslot und ist nicht platzierbar.
  - **Data Component `sternenwerft:fluid`** (Kanister, `transport_tank`): `{ "fluid": "<id>", "amount": <mB> }`. Ein leerer Behälter hat die Komponente nicht (sie wird entfernt).
  - **Sauerstoffflaschen (ab M3):** Die „Kanister“ aus Kapitel 27 sind `oxygen_canister` (600 mB) und `large_oxygen_canister` (1.200 mB). Sie nehmen nur `oxygen` an (`fluids` in `suits.json`). Die Übergabe geschieht sofort (1 Tick), so viel wie passt bzw. vorhanden ist; Teilfüllung ist erlaubt. „Voll“ heißt Kapazität erreicht, „leer“ heißt Komponente fehlt. Flaschen gehen in die Füll- und Leerslots aller Blöcke mit O₂-Tank (`electrolyzer`, `oxygen_generator`, `dome_generator_*`, `pressure_bottle`).
  - **Meilenstein:** Füll- und Leerslots für Eimer gehören zu M2, die für Sauerstoffflaschen zu M3 (27.4). `transport_tank` kommt mit M4.
- **Energiepuffer:** 20 × Verbrauch (J/t) × min(Vorgangsdauer, 100), mindestens 10.000 J, höchstens 1.000.000 J. Maschinen ohne Vorgangsdauer rechnen mit 100.
- **Energie und Fortschritt:** Eine Maschine nimmt so viel Energie auf, wie in den Puffer passt; über ein Kabel höchstens bis zu dessen Durchsatz. Verbrauch entsteht nur bei Fortschritt, nicht bei `idle` oder `error`. Enthält der Puffer weniger als den Verbrauch eines Ticks, pausiert der Fortschritt (`idle`); er verfällt nicht. Ist die Eingabe leer oder wechselt das Rezept, springt der Fortschritt auf 0.
- **Tanks:** Ein Tank enthält höchstens ein Fluid und wechselt es erst, wenn er leer ist. Ein Eingangstank nimmt nur Fluide an, die in mindestens einem Rezept dieses Maschinentyps als Eingabe stehen (bei Generatoren: der Brennstoff aus den Daten). `fluid_pump` und Ausgangstanks nehmen von außen nichts an.
- **Abbauen und Drops:** Alle Maschinen, Generatoren, Speicher, Ports, Gehäuse, Kabel und Rohre droppen sich selbst. Items im Inneren (auch Upgrades) fallen heraus. Fluid und Energie gehen verloren; nur `battery`, `fluid_tank` und `pressure_bottle` (ab M3) behalten ihren Inhalt in den Data Components `sternenwerft:energy` bzw. `sternenwerft:fluid` auf dem Item.
- **Blockeigenschaften:** Härte, Sprengwiderstand und Klang wie ein Vanilla-Block. Maschinen, Generatoren, Speicher, Ports und Gehäuse wie `minecraft:iron_block`; Kabel und Rohre wie `minecraft:iron_block` mit Kupfer-Klang, ohne Verdeckung (keine volle Würfelform). Abbau mit der Spitzhacke (Tag `minecraft:mineable/pickaxe`), Werkzeugstufe wie `iron_block` (`minecraft:needs_stone_tool`). Kein Block leuchtet in M2.
- **Statuslampe** (Blockstate `status`): `running` grün, `idle` gelb (wartet auf Material oder Strom), `error` rot (Ausgabe voll, falscher Ort, Struktur unvollständig).
- **Ausrichtung:** Blockstate `facing` (horizontal), zeigt zum Spieler.
- Sneak-Rechtsklick mit leerer Hand auf einen Multiblock-Controller zeigt 200 t lang eine Geisterprojektion der fehlenden Blöcke. Die Projektion rechnet rein clientseitig: `GhostPreview` liest das Muster aus den Multiblock-Daten (z. B. `refinery_multiblock`) und vergleicht es mit dem Client-Level. Es gibt keinen Payload.

## 4.2 Upgrades

| Item | Slot | Wirkung pro Stück | Max. Stapel |
| --- | --- | --- | --- |
| `speed_upgrade` | 1 | Vorgangsdauer × 0,75, Verbrauch J/t × 1,5 | 4 |
| `efficiency_upgrade` | 2 | Verbrauch J/t × 0,85 | 4 |

Die Wirkung multipliziert sich. Upgrades gelten für alle Maschinen, Generatoren nicht.

- Die Rechnung ist eine reine Funktion `UpgradeMath` in `double`, mit n = Zahl der `speed_upgrade` und m = Zahl der `efficiency_upgrade` (je 0 bis 4):
  - Dauer = max(1, round(`ticks` × 0,75^n))
  - Verbrauch = max(1, round(J/t × 1,5^n × 0,85^m))
  - `round` ist kaufmännisches Runden (`Math.round`).
- Beispiel (JUnit): n = 4 im Zerkleinerer ergibt 63 t (200 × 0,75^4 = 63,28) und 101 J/t (20 × 1,5^4 = 101,25).
- Die Menge je Vorgang bleibt gleich (Pumpe 1.000 mB, Elektrolyseur 200 mB).
- Die Puffergröße (4.1) wird nur beim Laden und bei `/reload` aus den Basiswerten ohne Upgrades berechnet.
- Beim Ein- und Ausbau eines Upgrades bleibt der Fortschritt eines laufenden Vorgangs als Anteil (0 bis 1) erhalten.
- Slot 1 nimmt nur `speed_upgrade`, Slot 2 nur `efficiency_upgrade` an. Ein Trichter füllt Upgrade-Slots nie.

## 4.3 Multiblöcke

- Ein Multiblock besteht aus einem **Controller** (teuer) und **Gehäuseblöcken** (billig). Gehäuseblöcke dürfen durch Port-Blöcke ersetzt werden: `item_port`, `fluid_port`, `energy_port`. Ein Port leitet nach außen an den Controller weiter. Ports brauchen keine Forschung (die Raffinerie nutzt sie schon in Phase 1). Der Controller nimmt an seinen freien Seiten ebenfalls Energie, Fluide und Items an und gibt Ausgaben ab (Fluide nur über die Ausgangsseite, 4.1).
- **Port-Modus:** Jeder Port ist auf `input` oder `output` eingestellt (Standard `input`). Rechtsklick mit leerer Hand schaltet um. Ein `output`-Port gibt nur Ausgaben des Controllers ab, ein `input`-Port nimmt nur an. Ein Port ohne gültigen Controller tut nichts.
- Der Controller prüft die Struktur beim Platzieren, bei Blockänderungen und alle 100 t. Gehäuse- und Port-Blöcke des Musters melden jede Änderung an Controller im Abstand von höchstens 5 Blöcken (Registrierliste im Level). Ist sie unvollständig, steht die Statuslampe auf `error`.

| Multiblock | Maße (B×T×H) | Aufbau |
| --- | --- | --- |
| `refinery` | 3×3×5 | massiv; Controller unten Mitte einer Seitenfläche; Rest `refinery_casing` oder Ports |
| `fusion_reactor` | 5×5×5 | hohle Hülle (98 Blöcke), Innenraum 3×3×3 Luft; Controller Mitte einer Seitenfläche; Rest `fusion_casing` oder Ports |
| `fission_reactor` | 5×5×5 | wie Fusion, mit `fission_casing` |
| `launch_pad_t1` | 5×5 Plattform | 25 `launch_pad`; dazu eine Säule `launch_tower` direkt neben der Plattform, mindestens so hoch wie die Rakete; `fuel_pump` an Plattform oder Turm angrenzend; `launch_control` im Umkreis von 8 Blöcken |
| `launch_pad_t2` | 7×7 Plattform | wie T1 mit 49 `launch_pad` |

**Muster der `refinery`:**

- Der Controller steht in der untersten Schicht in der Mitte der Vorderseite (`facing`-Seite). Das Muster reicht von ihm 1 Block nach links und rechts, 2 Blöcke gegen die Blickrichtung nach hinten und 4 Blöcke nach oben.
- Die übrigen 44 Positionen sind `refinery_casing` oder Ports (beliebig viele). Andere Blöcke im Muster sind nicht erlaubt. Der Aufbau darf frei stehen.
- Bei einem Fehler steht die Statuslampe auf `error`. Der Status-Tooltip nennt den ersten fehlenden Block mit Koordinate (`gui.sternenwerft.multiblock.missing`).

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
- **Kein Puffer:** Kabel und Rohre speichern nichts. Ausgaben gehen im selben Tick an die Empfänger, höchstens mit dem Durchsatz der Sorte (J/t bzw. mB/t). „Leer“ heißt: In den letzten 20 t wurde nichts übertragen.
- **Abgabeseiten:** Generatoren schieben Energie in alle Seiten. Fluid-Ausgaben von Maschinen, Controllern und Speichern gehen nur über die Ausgangsseite (4.1). Ein Port gibt nach seinem Modus ab (4.3).
