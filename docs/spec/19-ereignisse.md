# 19. Ereignisse

Pro Dimension prüft der Server alle 1.200 t mit der angegebenen Wahrscheinlichkeit (× Einstellung `event_frequency`: aus 0, selten 0,5, normal 1, häufig 2). Ereignisse laufen nur, wenn mindestens ein Spieler in der Dimension ist.

**Planung:**
- Ein erfolgreicher Wurf startet das Ereignis nicht sofort, sondern **plant es 2.400 t in die Zukunft**. Gespeichert wird es in einer `SavedData` je Dimension.
- Die Warnung kommt bei T − 2.400 (mit `space_weather_station`) bzw. T − 400 (ohne).
- Solange ein Ereignis geplant ist oder läuft, wird für diese Dimension nicht neu gewürfelt.

**Wetter je Dimension:**
- Mod-Dimensionen haben einen **eigenen Wetterzustand** in der `SavedData` der Dimension, nicht das Wetter der Overworld.
- Laufzeiten wie Vanilla: klar 12.000–180.000 t, Wetter 12.000–24.000 t.
- Planeten ohne Wetter-Ereignis in der Tabelle haben immer klares Wetter.

| Ereignis | Dimensionen | Chance pro 1.200 t | Dauer | Wirkung | Schutz | Belohnung |
| --- | --- | --- | --- | --- | --- | --- |
| Sonnensturm | alle außer Erde; Merkur ×3 | 1,5 % | 1.200–3.600 t | Strahlung +2; Maschinen ohne Deckung (≥ 3 Blöcke darüber) und ohne Kuppel T3 pausieren alle 200 t für 40 t | Kuppel T3, `radiation_shield`, ≥ 3 Blöcke Deckung | Solarpanels × 2 |
| Meteoritenschauer | `moon`, `mars`, `asteroid_belt`, alle Orbits | 1 % | 1.200 t | alle 100 t ein Meteorit im Radius 64 um jeden Spieler; Explosion Stärke 2 ohne Feuer (Blockschaden abschaltbar), hinterlässt 1 `meteorite_ore` | Kuppel mit `dome_shield_module` fängt ab | `meteorite_ore` → 2 `raw_iron` + 1 `raw_nickel`, 10 % 1 `raw_platinum`, 5 % 1 `raw_iridium` |
| Staubsturm | `mars` (statt Vanilla-Regen) | Vanilla-Wetter | Vanilla | Nebel Sichtweite 16, Solar × 0,25, Monster × 2 | Kuppel T2 | `dust_wraith` spawnt |
| Säureregen | `venus` (statt Regen) | Vanilla-Wetter | Vanilla | ungeschützte Rüstung und Werkzeuge in der Hand −1 Haltbarkeit pro 40 t | Kuppel T2, Anzug T2 | – |
| Vulkanausbruch | `io` | 3 % je `volcanic_vent` im Umkreis 128 eines Spielers | 200 t | fallende `io_sulfur_ore`-Blöcke im Radius 32 | Abstand | frischer Schwefel |
| Methanregen | `titan` (statt Regen) | Vanilla-Wetter | Vanilla | Sicht × 0,5 | – | `pump_station` +50 % |
| Ringtrümmer | `saturn_orbit` | 2 % | 600 t | wie Meteoritenschauer, ohne Erz | `dome_shield_module` | – |

**Vorwarnung:** Mit `space_weather_station` der Agentur in derselben Dimension kommt die Warnung 2.400 t vorher, sonst 400 t vorher (Chat-Nachricht + Ton + HUD-Symbol).
