---
title: Hydrokultur und Kompost
title_en: Hydroponics and Compost
order: 140
milestone: 5
requires: ["infinite_horizon:hydroponics_basin"]
icon: infinite_horizon:hydroponics_basin
items: ["infinite_horizon:hydroponics_basin", "infinite_horizon:compost_soil", "infinite_horizon:compost_farmland", "infinite_horizon:compost"]
summary: Pflanzen anbauen auf fremden Welten
---
Auf Mond und Mars wächst im Boden nichts. Regolith und Marssand lassen sich nicht umgraben. Für Nahrung hast du zwei Wege: Kompostboden für klassische Felder oder das Hydrokultur-Becken. Beide schaltet der Forschungsknoten „Hydrokultur“ frei.

Alle Pflanzen brauchen außerhalb der Erde eine aktive [[kuppel|Kuppel]] und genug Licht.

## Kompostboden

<item:infinite_horizon:compost_soil> entsteht im Montagetisch aus Regolith oder Marssand, Knochenmehl und Gips oder <item:infinite_horizon:compost>. Mit der Hacke wird daraus <item:infinite_horizon:compost_farmland>. Das verhält sich wie Ackerland auf der Erde: Es braucht Wasser in der Nähe und trocknet sonst wieder zu Kompostboden aus.

@recipes_for infinite_horizon:compost_soil infinite_horizon:compost

## Das Hydrokultur-Becken

Das <item:infinite_horizon:hydroponics_basin> braucht Strom, Wasser und Licht (mindestens {{value:agriculture:infinite_horizon:hydroponics_basin.light_min}}). Leg Samen oder einen Setzling in den Pflanzslot. Die Pflanze wächst alle {{value:agriculture:infinite_horizon:hydroponics_basin.ticks_per_growth_stage|t}} eine Stufe weiter, etwa doppelt so schnell wie auf gutem Ackerland. Ist sie reif, landet die Ernte im Ausgabeslot und der Samen bleibt im Pflanzslot. Ein Setzling braucht {{value:agriculture:saplings.growth_stages}} Stufen und gibt {{value:agriculture:saplings.yield.0.count}} Stämme seiner Holzart.

@machine infinite_horizon:hydroponics_basin

@recipes_for infinite_horizon:hydroponics_basin

@warning Fällt die Kuppel aus oder wird es zu dunkel, bleibt die Pflanze stehen. Die Statuslampe zeigt dann „Kein Wachstum“.

@tip Eine elektrische Lampe über dem Becken liefert genug Licht, auch in der Mondnacht.
