# 8. Umweltsystem

## 8.1 Umweltwerte

Jede Dimension hat Werte aus ihrem Planeten-Eintrag (Kapitel 13 und 25.1). **Zonen** im Planeten-Eintrag (z. B. Venus-Wolkenschicht, Europa-Ozean) überschreiben diese Werte in ihrem Bereich. **Dimensionen ohne Planeten-Eintrag** (Nether, End, Dimensionen anderer Mods) gelten wie die Erde: keine Umweltgefahren.

Die Berechnung MUSS als reine Funktion `EnvironmentCalculator.compute(planet, zonen, position, tageszeit, kuppeln, ausrüstung)` (`zonen` = alle Zonen, die die Position enthalten; Vorrang siehe 25.1) umgesetzt werden, damit sie testbar ist (Kapitel 27). Pro Spieler werden alle 10 t berechnet:

| Wert | Berechnung |
| --- | --- |
| `breathable` | Atmosphäre `breathable` (Erde, Dimensionen ohne Planeten-Eintrag): ja. Sonst nur in aktiver Kuppel (auch während `failing`, 10.1) |
| `temperature` | Biom mit Override: fester Wert. Sonst `lerp(temp_night, temp_day, Tageslicht) + biome_offset`. In aktiver Kuppel mit passender Stufe: 20 °C |
| `pressure` | `vacuum`, `low`, `normal`, `high`, `deep` (Tiefseedruck) aus Planet bzw. Zone. In Kuppel: `normal` |
| `radiation` | Stufe aus Planet bzw. Zone, +2 bei Sonnensturm, −2 bei ≥ 3 festen Blöcken senkrecht über dem Spieler (fest = `BlockState#isSolidRender`, gezählt bis zur Weltobergrenze), 0 in Kuppel T3. **Ergebnis auf 0–3 begrenzt.** |

- **Planet einer Position:** `PlanetResolver` (3A.3); im GameTest-Betrieb gilt zuerst der Override (27).
- **Tageslicht** = Kurve aus 6.3 (`daylight_curve` in `environment.json`) ohne Wetterfaktor. Die Tageszeit einer Mod-Dimension ist die der Overworld (13.1).

## 8.2 Belastung (für alle Gefahren gleich)

- Für jede ungeschützte Gefahr (Sauerstoff, Hitze, Kälte, Druck, Strahlung) füllt sich eine **Belastung** von 0 auf 100 % in 200 t (= Pflicht-Warnzeit von 10 s).
- Ab 1 % erscheint die HUD-Leiste mit Warnton. Bei 100 % Schaden: 1 HP pro 20 t (Sauerstoff, Druck, Hitze über 300 °C), 1 HP pro 40 t (Kälte, Hitze, Strahlung 2), 1 HP pro 20 t (Strahlung 3).
- Ist die Gefahr weg, sinkt die Belastung in 100 t auf 0.
- **Zählweise:** Die Belastung zählt je Gefahr in ganzen Ticks (0 bis `buildup_ticks`). Jede Prüfung (alle `interval_ticks` = 10 t, Takt je Spieler ab Ankunft in der Dimension) addiert `interval_ticks`, solange die Gefahr besteht; ohne Gefahr zieht sie `interval_ticks × buildup_ticks / decay_ticks` ab. Prozent = Wert / `buildup_ticks`. Ein ungeschützter Spieler erreicht 100 % also mit der 20. Prüfung, nach 200 t (Toleranz in Tests: ein Takt). Regeln in `environment.json` (`hazard_rules`).
- Gefahren zählen unabhängig. Ohne Anzug im Mond-Vakuum steigen Sauerstoff und Druck, dazu je nach Tageszeit Hitze oder Kälte.
- `HazardTracker` läuft im Server-Tick über die Spielerliste. Spieler im Kreativ- oder Zuschauermodus bekommen keine Belastung und keinen Schaden.
- **Schaden:** Jede Gefahr hat eine eigene Schadensart (Datenpaket-Registry `damage_type`: `infinite_horizon:oxygen`, `pressure`, `heat`, `cold`, `radiation`; Tiefseedruck nutzt `pressure`) mit dem Tag `minecraft:bypasses_armor` und der Todesnachricht `death.attack.infinite_horizon.<gefahr>`. Schäden mehrerer Gefahren addieren sich. Die Belastung wird mit dem Spieler gespeichert und beim Tod auf 0 gesetzt.

## 8.3 Schutz ohne Anzug

Spieler ohne vollständigen Anzug vertragen −20 bis +45 °C, Druck `low` bis `normal`, Strahlung ≤ 1, und brauchen `breathable`.

## 8.4 HUD

Kleine Leisten oberhalb der Hungerleiste, je 81 × 5 px, nach oben gestapelt. Sind die Vanilla-Luftblasen sichtbar, stehen die Leisten darüber. Die Client-Datei verschiebt das HUD (3.5). Werte in `environment.json` (`hud`), Daten über `HazardStatePayload` (3A.5), Texte `hud.infinite_horizon.<name>` (6.4).

- **Belastung je Gefahr** (`oxygen`, `pressure`, `heat`, `cold`, `radiation`): sichtbar ab 1 %. Farben nach Belastung: grün, gelb ab 50 %, rot ab 80 %. Beim Erscheinen ertönt der Warnton der Gefahr (24.6; Druck nutzt `warning.oxygen`).
- **Anzugvorrat O₂** (mB von 1.200): sichtbar mit vollständigem Anzug außerhalb von `breathable`. Farben umgekehrt: gelb unter 50 %, rot unter 20 %.
- **Temperatur und Strahlung:** Symbol mit Zahl, sichtbar, wenn der Wert außerhalb des Schutzes ohne Anzug liegt (8.3).
- **Hitzezeit** (nur Venus-Oberfläche, ab M5): Leiste wie der Anzugvorrat.

## 8.5 Welt ohne Atmosphäre

**Dies ist die einzige gültige Regel; andere Kapitel verweisen hierher.** In allen Mod-Dimensionen (alle außer Overworld, Nether und End, dort gilt Vanilla) gilt außerhalb aktiver Kuppeln, unabhängig von der Atmosphäre:

- Fackeln, Feuer, Lagerfeuer und Laternen lassen sich nicht platzieren. Bestehende erlöschen beim nächsten Random Tick (Fackel → `extinguished_torch`, dekorativ).
- Pflanzen wachsen nicht (Random Tick abgebrochen).
- Platziertes Wasser wird sofort zu Eis (Planet `water_behavior: freeze`) oder verschwindet (`evaporate`).
- Elektrische Lampe `electric_lamp` (5 J/t, Licht 15) funktioniert überall.

**Umsetzung (ab M3, Werte in `misc.json` → `atmosphere_rules`):**

- Betroffene Blöcke stehen im erzeugten Block-Tag `infinite_horizon:needs_atmosphere` (für Addons erweiterbar): alle Fackeln außer Redstone-Fackeln (auch Wand-, Seelen- und Kupferfackeln), Feuer, Seelenfeuer, Laternen, Lagerfeuer und Kerzen. Setzen schlägt mit der Meldung `message.infinite_horizon.no_atmosphere` fehl.
- Bestehende Blöcke: Fackeln bekommen in Mod-Dimensionen Random Ticks (Mixin, 3.7) und werden zu `extinguished_torch` (Blockstate `facing`, auch an der Wand). Lagerfeuer und Kerzen gehen aus (`lit=false`), Laternen und Feuer verschwinden. `extinguished_torch` droppt sich selbst und hat kein Rezept.
- Pflanzen: Random Tick und Knochenmehl sind für `#minecraft:crops` und `#minecraft:saplings` abgebrochen.
- `freeze`: Platziertes Wasser (Eimer, Werfer) wird zu `minecraft:ice`; Eis schmilzt dort nicht.
- `electric_lamp`: Block-Entity mit Energiepuffer nach 4.1 (10.000 J) und Blockstate `lit`; ohne Strom Licht 0. Kein GUI, keine Statuslampe.
- In aktiven Kuppeln (auch während `failing`) gilt Vanilla.
