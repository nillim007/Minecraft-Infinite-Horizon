---
title: Upgrades
title_en: Upgrades
order: 110
milestone: 2
requires: ["sternenwerft:speed_upgrade", "sternenwerft:efficiency_upgrade"]
icon: sternenwerft:speed_upgrade
items: ["sternenwerft:speed_upgrade", "sternenwerft:efficiency_upgrade"]
summary: Maschinen schneller oder sparsamer machen
---
Jede Maschine hat unten rechts zwei Upgrade-Slots. Generatoren und Speicher haben keine.

- **Slot 1:** <item:sternenwerft:speed_upgrade> – jeder Vorgang dauert nur noch das {{value:machines:speed_upgrade.ticks_multiplier}}-Fache, braucht dafür aber das {{value:machines:speed_upgrade.energy_per_tick_multiplier}}-Fache an Strom pro Tick.
- **Slot 2:** <item:sternenwerft:efficiency_upgrade> – senkt den Stromverbrauch pro Tick auf das {{value:machines:efficiency_upgrade.energy_per_tick_multiplier}}-Fache.

Pro Slot passen bis zu {{value:machines:speed_upgrade.max_stack}} Upgrades. Die Wirkung multipliziert sich: Vier Geschwindigkeits-Upgrades machen einen Zerkleinerer mehr als dreimal so schnell.

@recipes_for sternenwerft:speed_upgrade sternenwerft:efficiency_upgrade

@tip Kombiniere beides: Geschwindigkeits-Upgrades für mehr Tempo, Effizienz-Upgrades, damit dein Kabelnetz nicht überlastet.

@note Du kannst Upgrades jederzeit ein- und ausbauen. Ein laufender Vorgang behält dabei seinen Fortschritt.
