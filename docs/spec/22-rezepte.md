# 22. Rezepte

> **Maßgebliche Daten:** `spec/data/recipes.json` (alle Rezepte: Werkbank, Maschinen, Montagetisch, Raketenteile).
> **Lesefassung:** `docs/spec/generated/recipes.md`.

## 22.1 Werkbank

Nur Phase-1-Objekte, die vor dem Montagetisch gebraucht werden, sowie Handbuch und Atlas (Ersatz) entstehen an der Werkbank. Alles andere entsteht im Montagetisch oder in Maschinen.

## 22.2 Maschinenrezepte

- Regelrezepte (jedes Erz → 2 Staub, jede Form in der Presse) sind in `recipes.json` ausgeschrieben, ein Eintrag pro Metall.
- `smelting`-Rezepte gelten im Vanilla-Ofen und im Elektroofen; `electric_smelting` nur im Elektroofen.
- Ohne eigene `ticks`/`energy_per_tick` gelten die Werte der Maschine aus `balance/machines.json`.
- Ein Rezept wird erst mit seinen Items umgesetzt (`common/implemented_ids.json`). So entsteht `crushing/aluminum_dust_from_metal_concentrate` erst mit dem Item `metal_concentrate` (Meilenstein 5). Der Zerkleinerer verarbeitet in M2 nur `raw_*`-Items und `bauxite`.

## 22.3 Montagetisch (`assembly`)

- Formlos, 9 Eingabeslots. Eine Zutat darf mehrere Slots belegen; ihre Mengen werden summiert (z. B. 96 `titanium_plate` = 64 + 32). Ein Rezept darf höchstens 9 Slots brauchen.
- Optional 1 Fluid (max. 16.000 mB). „1 Tank X“ = 16.000 mB des Fluids X.
- Alternativrezepte (Endung `_alt`) werden vom selben Forschungsknoten freigeschaltet wie das Hauptrezept (11.3).
- Aufwertungen (Anzug T1 → T2 → T3, Frachter) übernehmen die Komponenten des Basisteils (`copy_components_from`).
- Raketenteil-Summen: T1 mit 4 Tanks = 200 `aluminum_plate`, 64 `titanium_plate`, 32 `circuit`; T2 mit 6 Tanks = 128 `lightweight_plate`, 64 `heat_shield`, 32 `advanced_circuit`, 4 `cooling_system`. `tools/validate_spec.py` prüft diese Summen und die Slotgrenze.
- **Abgleich:** Ein Rezept passt nur, wenn die belegten Eingabeslots **genau** die Zutatenmengen enthalten (gleiche Zutatenarten, gleiche Summen, nichts anderes). Überschuss in den Slots verhindert den Start. Verbraucht wird genau die Menge des Rezepts. Beispiel: 4 `iron_plate` + 1 `copper_pipe` ergeben `fluid_port`, nicht `refinery_casing` (2 + 1).
- **Mengen-Multiplikator:** `recipe_amount_multiplier` (24.4) gilt ab M2 im Rezept-Matcher: Menge = max(1, aufgerundet(Menge × Multiplikator)), höchstens so weit, dass die Zutaten in 9 Slots passen. Der Abgleich nutzt die vergrößerten Mengen. JUnit-Beispiel: Multiplikator 2,0 bei 4 `iron_plate` → 8.
- **Zutatengleiche Rezepte** sind getrennt (Ä25): `research_terminal` braucht zusätzlich 1 `minecraft:book`, `landing_beacon` nimmt `minecraft:redstone_lamp` statt Glowstone-Staub, `rocket_fin_t2` braucht 3 statt 2 `lightweight_plate`.
