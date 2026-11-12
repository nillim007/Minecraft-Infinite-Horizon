# 14. Raumstation

## 14.1 Stationskern

- `station_core` darf nur in Orbit-Dimensionen gesetzt werden, auch in der Luft: Ohne Zielblock setzt ein Rechtsklick den Kern 3 Blöcke vor den Spieler (aus der Kapsel siehe 12.5, Schritt 7).
- Beim Setzen entsteht eine 5×5-Plattform aus `station_hull` mit dem Kern in der Mitte, nur an Luftpositionen. Diese Blöcke haben den Blockstate `generated=true` und droppen beim Abbau nichts. Der Kern enthält einen eingebauten Kuppelgenerator T1 (Radius 8) mit eigenem Puffer: eingebaute Solarzellen mit 4 × 20 J/t × Solarfaktor des Orbits (Erdorbit: 80 J/t) und 2.000 mB O₂-Tank. Der Kern nimmt zusätzlich Strom über Kabel an. Ab dem Jupiter-Orbit reicht die eingebaute Solarleistung nicht mehr für die Kuppel (40 J/t); dort braucht die Station eine eigene Stromquelle.
- Eine **Station** = alle Blöcke im Umkreis von 48 Blöcken um einen `station_core`.

## 14.2 Stationsmodule

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

## 14.3 Schwerelosigkeit

- In Orbit-Dimensionen ist g = 0,005. Bewegung: Laufen auf Blöcken normal, Springen löst sich vom Boden.
- `jetpack`: Springen gedrückt = Schub nach oben, Sneak = nach unten, Blickrichtung + Vorwärts = 0,1 Blöcke/t.
- Innen schwebt man ebenfalls, bis man `gravity_plate`s baut.
- **Fall unter y 0** in einer Orbit-Dimension:
  - Planeten mit Oberfläche (Erde, Venus, Mars, Merkur): Teleport auf die Oberfläche an gleicher X/Z (Venus: Wolkenschicht) mit Langsamem Fall für 400 t.
  - Gasriesen: Teleport auf den nächsten `station_core` (sonst (0, 101, 0) auf einer neuen 3×3-Plattform aus `station_hull`) und 4 Schaden.
- Vor dem Jetpack (Stufe 4) bewegt man sich mit `magnet_boots` (Stufe 3, kommt mit dem Stationskern): Auf Blöcken gilt 1 g, wer abspringt, treibt mit 0,005 g.

## 14.4 Klon-Kammer

`clone_chamber`: Rechtsklick bindet den Spieler (ein Bindungsort pro Spieler). Bei Tod respawnt er dort, dimensionsübergreifend, wenn die Kammer Strom hat und eine `microbe_culture` im Slot liegt (wird verbraucht). Sonst normaler Vanilla-Respawn.

- Die Klon-Kammer meldet ihren Zustand (Strom ja/nein, Anzahl Kulturen) bei jeder Änderung an eine globale `SavedData`. Beim Tod wird nur dieser gespeicherte Zustand geprüft, der Chunk muss nicht geladen sein. Die Kultur wird beim nächsten Laden abgezogen.
- **Betten** und `habitation_module` setzen in Mod-Dimensionen den Spawnpunkt nur innerhalb einer aktiven Kuppel. Außerhalb erscheint die Meldung „Hier kannst du nicht atmen“ (`message.infinite_horizon.bed.no_air`). Schlafen (Nacht überspringen) geht nur, wo der Spawnpunkt gesetzt werden darf.
- **Prüfung beim Respawn:** Der Server prüft den Zielpunkt mit `EnvironmentCalculator` (Schutz ohne Anzug, 8.3). Ist er unsicher, etwa weil die Kuppel inzwischen ausgefallen ist, gilt die Reihenfolge Klon-Kammer → Bett → Weltspawn der Overworld, jeweils mit Meldung. Das verhindert Todesschleifen. Fehlt ein Glied (in M3 die Klon-Kammer), wird es übersprungen: Bett → Weltspawn der Overworld.
