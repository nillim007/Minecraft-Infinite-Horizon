# 18. Kreaturen

Spawn-Gewicht relativ zu anderen Kreaturen im Biom. „Analyse“ = +15 FP mit `field_scanner` (einmal pro Typ und Agentur).

**Spawnregeln:**
- Alle Kreaturen nutzen eigene Spawn-Prädikate statt der Vanilla-Bodenregeln (auf Planeten gibt es kein Gras).
- Feindliche Kreaturen: Kategorie `monster`, Licht ≤ 7.
- Passive und neutrale Kreaturen: Kategorie `creature`, beliebiges Licht, auf jedem festen Planetenblock.
- Wasser- und Methanbewohner: Kategorie `water_creature`.
- Fliegende Kreaturen (`cloud_ray`, `geyser_moth`): Kategorie `ambient`.
- `reef_keeper` (1 je `hydrothermal_field`) und `drift_mite` (Schwarm je Platinerz-Asteroid) werden beim Generieren der Struktur bzw. des Asteroiden gesetzt und spawnen nicht nach.

> **Maßgebliche Daten:** `spec/data/balance/creatures.json` (je Kreatur: Lebenspunkte, Rüstung, Schaden, Tempo, Größe, Kategorie, Spawn, Verhalten, Drops).
> **Lesefassung:** `docs/spec/generated/creatures.md`.

Der Boss `signal_sentinel` hat eine Bossleiste und zwei Phasen (Details in `creatures.json`). Er wird am `transmitter_core` beschworen (13.4) und droppt die `star_chart` nach den Besitzregeln in `creatures.json` (Komponente `chart_owner`, Neu-Drop nur ohne gültige Karte der Agentur).

**Verwendung der Drops:** `metal_concentrate` → Zerkleinerer. `armor_plate` → `armor_plating`-Modul. `mirror_shell` → `heat_reflector`. `heat_scale` → `cooling_module` (alternatives Rezept). `skimmer_leg` → `pontoon`. `insulation_fabric` → Anzug T3.

`pontoon`: Block, wie Seerose auf Flüssigkeiten setzbar, trägt Maschinen (z. B. `pump_station` auf Methanseen).

`sulfur_trail`: dekorative, nicht feste Schicht (wie eine Schneeschicht), droppt nichts und verschwindet nach 6.000 t.
