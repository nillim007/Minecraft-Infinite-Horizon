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
- **Ertrinken:** Ein vollständiger Anzug mit O₂ > 0 hält den Vanilla-Luftvorrat unter Wasser voll; stattdessen sinkt der Anzugvorrat normal weiter.
- **Hitzezeit Venus-Oberfläche:** Außerhalb der Wolkenschicht (y < 180 in `venus`) läuft ein Timer von 3.600 t. Danach gilt der Spieler als ungeschützt gegen Hitze. Der Timer füllt sich oberhalb y 180 in 1.200 t wieder auf.

## 9.2 Module

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
