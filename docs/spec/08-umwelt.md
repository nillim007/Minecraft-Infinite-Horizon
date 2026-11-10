# 8. Umweltsystem

## 8.1 Umweltwerte

Jede Dimension hat Werte aus ihrem Planeten-Eintrag (Kapitel 13 und 25.1). **Zonen** im Planeten-Eintrag (z. B. Venus-Wolkenschicht, Europa-Ozean) überschreiben diese Werte in ihrem Bereich. **Dimensionen ohne Planeten-Eintrag** (Nether, End, Dimensionen anderer Mods) gelten wie die Erde: keine Umweltgefahren.

Die Berechnung MUSS als reine Funktion `EnvironmentCalculator.compute(planet, zonen, position, tageszeit, kuppeln, ausrüstung)` (`zonen` = alle Zonen, die die Position enthalten; Vorrang siehe 25.1) umgesetzt werden, damit sie testbar ist (Kapitel 27). Pro Spieler werden alle 10 t berechnet:

| Wert | Berechnung |
| --- | --- |
| `breathable` | Erde: ja. Sonst nur in aktiver Kuppel |
| `temperature` | Biom mit Override: fester Wert. Sonst `lerp(temp_night, temp_day, Tageslicht) + biome_offset`. In aktiver Kuppel mit passender Stufe: 20 °C |
| `pressure` | `vacuum`, `low`, `normal`, `high`, `deep` (Tiefseedruck) aus Planet bzw. Zone. In Kuppel: `normal` |
| `radiation` | Stufe aus Planet bzw. Zone, +2 bei Sonnensturm, −2 bei ≥ 3 festen Blöcken senkrecht über dem Spieler, 0 in Kuppel T3. **Ergebnis auf 0–3 begrenzt.** |

## 8.2 Belastung (für alle Gefahren gleich)

- Für jede ungeschützte Gefahr (Sauerstoff, Hitze, Kälte, Druck, Strahlung) füllt sich eine **Belastung** von 0 auf 100 % in 200 t (= Pflicht-Warnzeit von 10 s).
- Ab 1 % erscheint die HUD-Leiste mit Warnton. Bei 100 % Schaden: 1 HP pro 20 t (Sauerstoff, Druck, Hitze über 300 °C), 1 HP pro 40 t (Kälte, Hitze, Strahlung 2), 1 HP pro 20 t (Strahlung 3).
- Ist die Gefahr weg, sinkt die Belastung in 100 t auf 0.

## 8.3 Schutz ohne Anzug

Spieler ohne vollständigen Anzug vertragen −20 bis +45 °C, Druck `low` bis `normal`, Strahlung ≤ 1, und brauchen `breathable`.

## 8.4 HUD

Kleine Leisten oberhalb der Hungerleiste: Sauerstoff (Anzugvorrat), Temperatur, Strahlung, Hitzezeit (nur Venus-Oberfläche). Sie erscheinen nur, wenn der Wert relevant ist. Farben: grün, gelb ab 50 %, rot ab 80 %.

## 8.5 Welt ohne Atmosphäre

**Dies ist die einzige gültige Regel; andere Kapitel verweisen hierher.** In allen Mod-Dimensionen (alle außer Overworld, Nether und End, dort gilt Vanilla) gilt außerhalb aktiver Kuppeln, unabhängig von der Atmosphäre:

- Fackeln, Feuer, Lagerfeuer und Laternen lassen sich nicht platzieren. Bestehende erlöschen beim nächsten Random Tick (Fackel → `extinguished_torch`, dekorativ).
- Pflanzen wachsen nicht (Random Tick abgebrochen).
- Platziertes Wasser wird sofort zu Eis (Planet `water_behavior: freeze`) oder verschwindet (`evaporate`).
- Elektrische Lampe `electric_lamp` (5 J/t, Licht 15) funktioniert überall.
