# 10. Kuppel und Sauerstoff

## 10.1 Kuppelgenerator

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

## 10.2 Sauerstoffgenerator `oxygen_generator`

100 mB `oxygen` pro 1.200 t, 60 J/t, Upgrades wie Maschinen, funktioniert überall. Gibt O₂ an angrenzende Tanks, Kuppelgeneratoren und Rohre ab. Interner Puffer 2.000 mB. Ein Ladeslot füllt `oxygen_canister` und `large_oxygen_canister` aus diesem Puffer. Bedarf: Kuppel T1 = 1, T2 = 2, T3 = 8 Generatoren (ohne Upgrades). Das deckt nur die Kuppel selbst. Für das Nachfüllen der Anzüge (9.1) kommt 1 weiterer Generator je 2 Spieler hinzu, die regelmäßig draußen arbeiten.

Hinweis Energie: Kuppel T3 plus 8 Generatoren = 1.480 J/t. Das braucht 2 Fusionsreaktoren oder einen Kernreaktor.

## 10.3 Leben unter der Kuppel

- Fackeln, Feuer und Pflanzen funktionieren nur in einer aktiven Kuppel (siehe 8.5).
- Vanilla-Monster mit Atemmaske können die Kuppel betreten.
- Unter einer Kuppel dürfen nur Mitglieder der besitzenden Agentur Blöcke abbauen oder setzen (Kapitel 23).
