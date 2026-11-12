---
title: Der Mond
title_en: The Moon
order: 20
milestone: 3
requires: ["infinite_horizon:moon_rock"]
icon: infinite_horizon:moon_regolith
items: ["infinite_horizon:moon_rock", "infinite_horizon:moon_regolith", "infinite_horizon:moon_basalt", "infinite_horizon:moon_ice", "infinite_horizon:moon_aluminum_ore", "infinite_horizon:moon_titanium_ore"]
summary: Die erste fremde Welt: Gelände, Erze, Gefahren
---
Der Mond ist das erste Ziel deines Raumfahrtprogramms. Er ist nah, aber lebensfeindlich: keine Luft, Vakuum und starke Temperaturschwankungen zwischen Tag und Nacht. Dafür liegen hier Aluminium und Titan in großen Mengen.

## Die Welt

- **Schwerkraft:** das {{value:planet:moon.gravity}}-Fache der Erde. Du springst höher und fällst sanfter (siehe [[umwelt|Umwelt, Belastung und Schwerkraft]]).
- **Temperatur:** mittags {{value:planet:moon.temperature.day}} °C, nachts {{value:planet:moon.temperature.night}} °C. Im Polarkrater immer {{value:planet:moon.biome_temperature_overrides.moon_polar_crater}} °C.
- **Himmel:** schwarz, die Sterne leuchten auch am Tag, und die Erde steht groß am Himmel.
- **Tag und Nacht** laufen wie auf der Erde.

## Gelände und Biome

- **<item:infinite_horizon:moon_highlands>:** zerklüftetes Hochland aus <item:infinite_horizon:moon_regolith>.
- **<item:infinite_horizon:moon_mare>:** flache, dunkle Ebenen aus <item:infinite_horizon:moon_basalt>. Hier verlaufen auch die Lavaröhren, lange gerade Tunnel tief im Gestein.
- **<item:infinite_horizon:moon_great_crater>:** Hier liegen die Großkrater mit Zentralberg. In ihrem Inneren findest du doppelt so viel Erz.
- **<item:infinite_horizon:moon_polar_crater>:** selten und eisig kalt, mit Flecken aus <item:infinite_horizon:moon_ice>. Sonnenenergie gibt es hier nicht.

Überall verstreut liegen kleine Krater. Unter der Oberfläche besteht der Mond aus <item:infinite_horizon:moon_rock>.

## Erze

@ores infinite_horizon:moon

<item:infinite_horizon:moon_aluminum_ore> braucht mindestens eine Steinspitzhacke, <item:infinite_horizon:moon_titanium_ore> eine Eisenspitzhacke, genau wie auf der Erde. <item:infinite_horizon:moon_ice> ergibt beim Abbau gewöhnliches Eis.

## Monster

Nachts und im Dunkeln tauchen Zombies, Skelette, Spinnen und Creeper auf, aber seltener als auf der Erde. Sie tragen Atemmasken und brauchen keinen Sauerstoff.

@warning Ohne Schutz überlebst du auf dem Mond nur Sekunden: Sauerstoff, Druck und Temperatur bauen sofort Belastung auf.

@tip Lande am besten nachts oder in der Dämmerung. Dann ist es zwar kalt, aber die Mittagshitze von {{value:planet:moon.temperature.day}} °C bleibt dir erspart.
