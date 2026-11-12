# 10. Kuppel und Sauerstoff

## 10.1 Kuppelgenerator

| Stufe | ID | Radius | Strom | O₂ | Hält Außentemperatur | Zusätzlicher Schutz |
| --- | --- | --- | --- | --- | --- | --- |
| T1 | `dome_generator_t1` | 8 | 40 J/t | 50 mB/1.200 t | −180 bis +150 °C | Atemluft, Druck |
| T2 | `dome_generator_t2` | 16 | 200 J/t | 200 mB/1.200 t | −210 bis +250 °C | Staubsturm, Säureregen |
| T3 | `dome_generator_t3` | 32 | 1.000 J/t | 800 mB/1.200 t | −250 bis +300 °C | Strahlung (= 0) |

- **Form:** Halbkugel. Ein Punkt ist drin, wenn `abstand(punkt, mittelpunkt) ≤ radius` und `punkt.y ≥ mittelpunkt.y − 1`.
- **Liegt die Außentemperatur außerhalb des Bereichs**, funktioniert die Kuppel nicht: Eine aktive Kuppel geht auf `failing`, eine inaktive startet nicht (Status `error`). Gemessen wird an der Position des Generators (über `PlanetResolver`, 3A.3); ist die Kuppel aktiv, schützt sie ihr ganzes Volumen, auch wenn es in andere Zonen reicht.
- **Module** (3 Slots): `dome_range_module` (Radius × 1,5, Strom und O₂ × 2), `dome_efficiency_module` (O₂ × 0,75), `dome_shield_module` (fängt Meteoriten und Ringtrümmer ab; Mobs werden nicht abgehalten).
- **Verwaltung:** pro Dimension eine `SavedData`-Liste aktiver Kuppeln (Mittelpunkt, Radius, Stufe, Agentur, Zustand). Überlappende Kuppeln bilden einen gemeinsamen Luftraum.
- **O₂-Tank:** Jeder Kuppelgenerator hat einen internen Tank mit 2.000 mB, gefüllt über Rohre oder angrenzende `oxygen_generator`. Aus ihm kommen der Verbrauch der Kuppel und das Nachfüllen der Anzüge (9.1).
- **Meteoriten und Ringtrümmer** hält keine Kuppelstufe ab, nur das `dome_shield_module`.
- **Ausfall:** Fehlt Strom oder O₂ oder liegt die Temperatur außerhalb, geht die Kuppel in den Zustand `failing`: flackert rot, warnt alle Spieler darin und ist nach 600 t aus. Danach gilt die Welt dort als Atmosphäre des Planeten. Einzelheiten unter „Block und Zustände“.
- **Rendering:** halbtransparente Halbkugel (wie die Form, y ≥ Mittelpunkt − 1) mit Sechseckmuster-Textur, Farbe T1 weißlich, T2 blau, T3 violett, beim Ausfall rot pulsierend. Nur rendern, wenn innerhalb von 128 Blöcken. Daten über `DomeStatePayload` (3A.5).

**Block und Zustände (ab M3, Werte in `domes.json`):**

- **Block:** Energiepuffer nach 4.1 (ohne Vorgangsdauer; T1: 20 × 40 × 100 = 80.000 J). Der O₂-Tank (2.000 mB) nimmt nur `oxygen` an, von allen Seiten; es gibt keine Ausgangsseite. Füll- und Leerslot (4.1), 3 Modulslots (nur `dome_*_module`; bis die Module kommen, nehmen sie nichts an), keine Upgrade-Slots. GUI nach 4.1 mit Energie, O₂, Status und Radius (`gui.infinite_horizon.dome.radius`).
- **`running` (aktiv):** sobald der Puffer mindestens den Verbrauch eines Ticks, der Tank mindestens 1 mB und die Außentemperatur im Bereich liegt. Es gibt keine Anlaufzeit.
- **O₂-Verbrauch:** Zähler in 1/1.000 mB; je Tick kommen `oxygen_per_1200` / 1.200 mB dazu, ganze mB werden abgezogen (T1: 1 mB alle 24 t). Strom wird je Tick verbraucht, solange die Kuppel `running` oder `failing` ist und Strom da ist.
- **`failing`:** Puffer unter dem Verbrauch eines Ticks, Tank 0 mB oder Temperatur außerhalb. Die Kuppel schützt weiter. Alle 100 t erscheint bei den Spielern darin eine Warnung in der Aktionsleiste (`message.infinite_horizon.dome.failing` mit Restsekunden), dazu der Ton `warning.dome`. Nach 600 t ist die Kuppel aus. Kehrt die Versorgung vorher zurück, ist sie sofort wieder `running`; der Zähler beginnt neu.
- **Aus:** nicht mehr im `DomeManager`. Die Kuppel startet von selbst, sobald die Bedingungen für `running` wieder erfüllt sind.
- **Statuslampe:** `running` grün; `failing` rot (`error`); aus: `error` bei falscher Temperatur, sonst `idle`. Den Zustand `failing` speichert `DomeData`, nicht der Blockstate. War die Kuppel nie aktiv und liegt die Temperatur außerhalb, steht sie sofort auf `error`.

## 10.2 Sauerstoffgenerator `oxygen_generator`

100 mB `oxygen` pro 1.200 t, 60 J/t, Upgrades wie Maschinen, funktioniert überall. Gibt O₂ über seine Ausgangsseite (4.1, Standard `facing`) an angrenzende Tanks, Kuppelgeneratoren und Rohre ab. Interner Puffer 2.000 mB. Der Füllslot (4.1) füllt `oxygen_canister` und `large_oxygen_canister` aus diesem Puffer. Gasspeicher ist ab M3 die `pressure_bottle` (6.3). Bedarf: Kuppel T1 = 1, T2 = 2, T3 = 8 Generatoren (ohne Upgrades). Das deckt nur die Kuppel selbst. Für das Nachfüllen der Anzüge (9.1) kommt 1 weiterer Generator je 2 Spieler hinzu, die regelmäßig draußen arbeiten.

Hinweis Energie: Kuppel T3 plus 8 Generatoren = 1.480 J/t. Das braucht 2 Fusionsreaktoren oder einen Kernreaktor.

Hinweis Balance: Der Elektrolyseur (6.1) liefert O₂ deutlich günstiger als der Sauerstoffgenerator; ob das so bleibt, steht in Kapitel 28.

## 10.3 Leben unter der Kuppel

- Fackeln, Feuer und Pflanzen funktionieren nur in einer aktiven Kuppel (siehe 8.5).
- Vanilla-Monster mit Atemmaske können die Kuppel betreten.
- Unter einer aktiven Kuppel dürfen nur Mitglieder der besitzenden Agentur Blöcke abbauen oder setzen, auch Blöcke ohne Block-Entity; Operatoren sind ausgenommen (23.2; wirkt ab M4, M3 speichert nur den Besitz).
