# 9. Raumanzug

## 9.1 Stufen

Ein Anzug schützt nur, wenn **alle 4 Teile dieselbe oder eine höhere Stufe** haben. Es gilt die niedrigste Stufe der 4 Teile.

| Stufe | IDs | Rüstung wie | Temperatur | Druck | Sonst | O₂-Verbrauch |
| --- | --- | --- | --- | --- | --- | --- |
| T1 `pressure_suit` | `pressure_suit_helmet/chestplate/leggings/boots` | Eisen | −180 bis +150 °C | `vacuum`–`normal` | – | 1 mB / 20 t |
| T2 `heat_suit` | `heat_suit_*` | Diamant | −200 bis +500 °C | alle außer Tiefseedruck | Säureregen, Hitzezeit Venus 3.600 t | 1 mB / 20 t |
| T3 `deep_space_suit` | `deep_space_suit_*` | Netherit | −220 bis +500 °C | alle außer Tiefseedruck | Strahlung ≤ 2 | 1 mB / 40 t |

Alle Grenzwerte sind **inklusiv** (−200 °C ist für T2 noch sicher). **Tiefseedruck** (Europa-Ozean unter y 80) ist eine eigene Gefahr, gegen die nur `diving_module` schützt.

- **Sauerstoff:** Die Brustplatte hat einen Vorrat (`oxygen`, max. 1.200 mB). Sie füllt sich automatisch aus `oxygen_canister`/`large_oxygen_canister` im Inventar (die Flasche wird dabei geleert) und in aktiven Kuppeln mit 1 mB/t aus dem O₂-Tank des Kuppelgenerators (10.1). Liegen weniger als 500 mB im Tank, wird nicht nachgefüllt, damit die Kuppel nicht ausfällt. Eine Flasche mit 600 mB = 10 min (T1/T2) bzw. 20 min (T3).
  - **Verbrauch** nur mit vollständigem Anzug und `breathable = false`. Zähler in 1/1.000 mB; je Tick `amount / per_ticks × oxygen_consumption` (24.4). Der Faktor `oxygen_consumption` gilt nur für Anzüge, nicht für Kuppeln. Kreativ und Zuschauer verbrauchen nichts.
  - **Neue Brustplatte:** 0 mB (ohne Komponente `infinite_horizon:oxygen`).
  - **Ohne O₂:** Bei 0 mB schützt der Anzug weiter gegen Druck und Temperatur; nur die Gefahr Sauerstoff entsteht.
  - **Nachfüllen aus Flaschen:** bei jeder Umweltprüfung (alle 10 t), wenn der Vorrat unter 1.200 mB liegt, aus der ersten Flasche in Slot-Reihenfolge (Inventar 0–35, dann Zweithand), so viel wie passt. Eine leere Flasche verliert die Komponente.
  - **Anzeige:** Tooltip der Brustplatte „O₂: 600 / 1.200 mB“ (`gui.infinite_horizon.oxygen`) und HUD (8.4). Werte in `suits.json` (`oxygen_supply`).
- **Ertrinken:** Ein vollständiger Anzug mit O₂ > 0 hält den Vanilla-Luftvorrat unter Wasser voll; stattdessen sinkt der Anzugvorrat normal weiter.
- **Hitzezeit Venus-Oberfläche:** Außerhalb der Wolkenschicht (y < 180 in `venus`) läuft ein Timer von 3.600 t. Danach gilt der Spieler als ungeschützt gegen Hitze. Der Timer füllt sich oberhalb y 180 in 1.200 t wieder auf.
- **Rüstung:** Alle Werte wie bei der Vanilla-Rüstung aus der Spalte „Rüstung wie“ (T1 Eisen: Schutz 2/6/5/2, Haltbarkeit, Verzauberbarkeit). Reparatur-Item steht in `suits.json` (`repair_item`, T1: `aluminum_plate`). Der Haltbarkeitsbalken bleibt.

## 9.2 Module

Module, die Komponente `suit_modules` und die Rezepttypen `suit_module_install`/`suit_module_remove` kommen mit M4 (27.6; in M4 nur `helmet_lamp`). Jedes Anzugteil hat 2 Modulslots (Data Component `suit_modules`). Einbau: Anzugteil + Modul im Montagetisch. Ausbau: Anzugteil allein im Montagetisch (Rezepttyp `suit_module_remove`) gibt das Anzugteil ohne Module und die Module in die 3 Ausgabeslots. Einbau ist Rezepttyp `suit_module_install`. Beim Aufwerten übernimmt das Ergebnis alle Komponenten des Basisteils (`copy_components_from`, 25.4). Module bleiben beim Aufwerten auf die nächste Anzugstufe erhalten. Die Spalte „Stufe“ ist die Forschungsstufe. Module passen auf jede Anzugstufe.

- **Komponente `suit_modules`:** Liste von Modul-IDs, höchstens `module_slots_per_piece` (`suits.json`).
- **Einbau** passt im exakten Abgleich (22.3) nur bei genau 1 Anzugteil + 1 Modul, das zu diesem Teil passt (Spalte „Teil“), einem freien Modulslot und einem Modul, das noch nicht eingebaut ist. Sonst passt kein Rezept.
- **Ausbau** passt nur bei genau 1 Anzugteil mit mindestens 1 Modul.
- JEI/REI zeigen für beide Rezepttypen je Modul ein Beispiel (3.9).
- **`helmet_lamp`:** Der Lichtblock entsteht nur in Luft, in Wasser (als `waterlogged`) oder an der Stelle eines `minecraft:light`-Blocks. Der alte Lichtblock verschwindet auch beim Absetzen des Helms, beim Tod, beim Abmelden und beim Dimensionswechsel (`places_in`, `removed_on` in `balance/items.json`).

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
