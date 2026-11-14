---
title: Die Raumstation
title_en: The Space Station
order: 30
milestone: 6
requires: ["infinite_horizon:station_core"]
icon: infinite_horizon:station_core
items: ["infinite_horizon:station_core", "infinite_horizon:station_hull", "infinite_horizon:station_window"]
summary: Stationskern, Hülle und Fenster
---
Jede Station beginnt mit einem <item:infinite_horizon:station_core>. Er lässt sich nur im Orbit setzen, auch mitten in der Luft: Ohne Zielblock landet er {{value:stations:infinite_horizon:station_core.air_placement_distance}} Blöcke vor dir, aus der Kapsel einer Rakete direkt unter ihr. Alles im Umkreis von {{value:stations:infinite_horizon:station_core.station_radius}} Blöcken gehört zur Station.

## Was der Kern kann

- Beim Setzen entsteht eine Plattform von {{value:stations:infinite_horizon:station_core.platform.size}} × {{value:stations:infinite_horizon:station_core.platform.size}} Blöcken aus <item:infinite_horizon:station_hull>. Diese Plattformblöcke droppen beim Abbau nichts.
- Eine eingebaute Kuppel mit Radius {{value:stations:infinite_horizon:station_core.builtin_dome.radius}} schafft Atemluft. Sie braucht Sauerstoff im Tank ({{value:stations:infinite_horizon:station_core.o2_tank}} mB) und Strom.
- Den Strom liefern {{value:stations:infinite_horizon:station_core.builtin_solar.panels}} eingebaute Solarzellen mit je {{value:stations:infinite_horizon:station_core.builtin_solar.output_per_panel}} J/t mal Solarfaktor des Orbits. Über Kabel nimmt der Kern zusätzlich Strom an.

@machine infinite_horizon:station_core

## Bauen

<item:infinite_horizon:station_hull> ist das Bauteil für Wände und Böden, <item:infinite_horizon:station_window> ist durchsichtig wie Glas.

@recipes_for infinite_horizon:station_core infinite_horizon:station_hull infinite_horizon:station_window

@warning Ohne Sauerstoff im Tank geht die Kuppel des Kerns aus, wie jede andere Kuppel. Halte den Tank über Rohre oder Druckflaschen gefüllt.

@tip Den Sauerstofftank füllst du wie beim Kuppelgenerator über den Füllslot oder einen angrenzenden Sauerstoffgenerator.
