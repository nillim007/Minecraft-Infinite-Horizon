# 22. Rezepte

> **Maßgebliche Daten:** `spec/data/recipes.json` (alle Rezepte: Werkbank, Maschinen, Montagetisch, Raketenteile).
> **Lesefassung:** `docs/spec/generated/recipes.md`.

## 22.1 Werkbank

Nur Phase-1-Objekte, die vor dem Montagetisch gebraucht werden, sowie Handbuch und Atlas (Ersatz) entstehen an der Werkbank. Alles andere entsteht im Montagetisch oder in Maschinen.

## 22.2 Maschinenrezepte

- Regelrezepte (jedes Erz → 2 Staub, jede Form in der Presse) sind in `recipes.json` ausgeschrieben, ein Eintrag pro Metall.
- `smelting`-Rezepte gelten im Vanilla-Ofen und im Elektroofen; `electric_smelting` nur im Elektroofen.
- Ohne eigene `ticks`/`energy_per_tick` gelten die Werte der Maschine aus `balance/machines.json`.

## 22.3 Montagetisch (`assembly`)

- Formlos, 9 Eingabeslots. Eine Zutat darf mehrere Slots belegen; ihre Mengen werden summiert (z. B. 96 `titanium_plate` = 64 + 32). Ein Rezept darf höchstens 9 Slots brauchen.
- Optional 1 Fluid (max. 16.000 mB). „1 Tank X“ = 16.000 mB des Fluids X.
- Alternativrezepte (Endung `_alt`) werden vom selben Forschungsknoten freigeschaltet wie das Hauptrezept (11.3).
- Aufwertungen (Anzug T1 → T2 → T3, Frachter) übernehmen die Komponenten des Basisteils (`copy_components_from`).
- Raketenteil-Summen: T1 mit 4 Tanks = 200 `aluminum_plate`, 64 `titanium_plate`, 32 `circuit`; T2 mit 6 Tanks = 128 `lightweight_plate`, 64 `heat_shield`, 32 `advanced_circuit`, 4 `cooling_system`. `tools/validate_spec.py` prüft diese Summen und die Slotgrenze.
