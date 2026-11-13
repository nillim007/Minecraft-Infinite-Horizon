---
title: Strom im All
title_en: Power in Space
order: 50
milestone: 4
requires: ["infinite_horizon:solar_panel"]
icon: infinite_horizon:solar_panel
items: ["infinite_horizon:solar_panel", "infinite_horizon:aluminum_cable"]
summary: Solarpanel und Aluminiumkabel
---
Auf dem Mond gibt es weder Kohle noch Kerosin vor der Tür. Das <item:infinite_horizon:solar_panel> macht Strom aus Sonnenlicht, und das <item:infinite_horizon:aluminum_cable> leitet mehr Strom als Kupfer.

## Das Solarpanel

Ein Solarpanel braucht **freien Himmel** über sich. Dann liefert es tagsüber {{value:generators:solar_panel.output_per_tick.surface}} J/t, multipliziert mit dem **Solarfaktor** des Ortes. In der Dämmerung sinkt die Leistung, nachts ist sie 0. Bei Regen bringt es nur noch {{value:generators:solar_panel.weather_multipliers.minecraft:rain|%}}.

@machine infinite_horizon:solar_panel

| Ort | Solarfaktor |
| --- | --- |
| Erde | {{value:planet:overworld.solar_factor}} |
| Mond | {{value:planet:moon.solar_factor}} |
| Polarkrater des Mondes | {{value:planet:moon.zones.0.solar_factor}} |

Das Panel schiebt seinen Strom in alle angrenzenden Kabel und Maschinen. Sein Puffer reicht für kurze Pausen; für die Nacht brauchst du einen [[strom|Akku]].

@warning In den Polarkratern des Mondes kommt kein Sonnenlicht an. Ein Solarpanel liefert dort nichts.

## Das Aluminiumkabel

Ein <item:infinite_horizon:aluminum_cable> leitet {{value:transport:aluminum_cable.throughput}} J/t, ein <item:infinite_horizon:copper_cable> nur {{value:transport:copper_cable.throughput}} J/t. Kabel verschiedener Sorten verbinden sich nicht direkt. Für den Übergang stellst du einen Akku dazwischen.

## Herstellung

@recipes_for infinite_horizon:solar_panel infinite_horizon:aluminum_cable

@tip Stell neben jede Mondbasis ein paar Solarpanels und einen Akku. Tagsüber laden sie den Akku, nachts versorgt er Kuppel und Maschinen.
