---
title: Pluto
title_en: Pluto
order: 180
milestone: 7
requires: ["infinite_horizon:pluto_water_ice"]
icon: infinite_horizon:pluto_nitrogen_ice
items: ["infinite_horizon:pluto_nitrogen_ice", "infinite_horizon:pluto_water_ice", "infinite_horizon:pluto_tholin", "infinite_horizon:pluto_uranium_ore", "infinite_horizon:raw_uranium", "infinite_horizon:uranium_dust", "infinite_horizon:uranium_ingot"]
summary: Der Zwergplanet am Rand mit Herzgletscher und Uran
---
Pluto ist das fernste Ziel im Sonnensystem. Die Rakete braucht das [[fernflug|Fernflug-Modul]] und landet direkt auf der Oberfläche. Es ist {{value:planet:pluto.temperature.day}} °C kalt, die Schwerkraft beträgt {{value:planet:pluto.gravity}} g.

## Ausrüstung

Ein [[hitzeanzug|Anzug der Stufe 2]] mit [[tiefraumanzug|Heizmodul]] reicht, besser ist der Tiefraumanzug.

@warning Ohne Heizmodul erfrierst du auf Pluto in jedem Anzug unter Stufe 3.

## Gebiete

- **Herzgletscher**: eine weite Ebene aus <item:infinite_horizon:pluto_nitrogen_ice> im Umkreis von {{value:planet:pluto.generator.biome_source.radius}} Blöcken um den Landeplatz bei X/Z 0/0. Unter ihm liegt der [[sender|Sender]].
- **Eisgebirge** aus <item:infinite_horizon:pluto_water_ice>.
- **Dunkles Hochland**, bedeckt mit <item:infinite_horizon:pluto_tholin>.

## Uran

Nur im dunklen Hochland steckt <item:infinite_horizon:pluto_uranium_ore>, zwischen y {{value:planet:pluto.ores.0.y_min}} und {{value:planet:pluto.ores.0.y_max}}. Du brauchst eine Eisenspitzhacke. Aus <item:infinite_horizon:raw_uranium> wird im Zerkleinerer <item:infinite_horizon:uranium_dust>, und den schmilzt nur der Elektroofen zu <item:infinite_horizon:uranium_ingot>.

@recipes_for infinite_horizon:uranium_dust infinite_horizon:uranium_ingot

@tip Der erste Uranbarren ist ein Schritt zur letzten Forschungsstufe. Bring genug Rohuran für einen ganzen Ofen mit.
