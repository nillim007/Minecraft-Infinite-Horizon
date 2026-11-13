---
title: Die Venus
title_en: Venus
order: 80
milestone: 5
requires: ["infinite_horizon:venus_rock"]
icon: infinite_horizon:venus_rock
items: ["infinite_horizon:venus_rock", "infinite_horizon:venus_tungsten_ore", "infinite_horizon:venus_sulfur_ore", "infinite_horizon:raw_tungsten", "infinite_horizon:sulfur", "infinite_horizon:tungsten_dust", "infinite_horizon:aerostat_platform", "infinite_horizon:cloud_elevator"]
summary: Glühende Hölle unter giftigen Wolken: Wolfram und Schwefel
---
Die Venus ist der heißeste Ort im inneren Sonnensystem. Unter einer dichten, giftigen Wolkendecke liegen Lavaseen und Vulkane, aber auch Wolfram und Schwefel.

## Die Welt

- **Schwerkraft:** das {{value:planet:venus.gravity}}-Fache der Erde.
- **Temperatur:** Tag und Nacht {{value:planet:venus.temperature.day}} °C.
- **Luft:** giftig und unter hohem Druck.
- **Lavameer:** Bis zur Höhe {{value:planet:venus.sea_level}} ist alles mit Lava gefüllt.

## Die Wolkenschicht

Zwischen y {{value:spaceflight:venus_aerostat.check_y_min}} und {{value:spaceflight:venus_aerostat.check_y_max}} liegt die Wolkenschicht. Dort ist es angenehm warm und der Druck normal, nur die Luft bleibt giftig. Hier landen alle Raketen: Gibt es am Landeort noch keinen Boden, baut der Server eine Plattform aus <item:infinite_horizon:aerostat_platform> auf y {{value:spaceflight:venus_aerostat.platform_y}}.

- **<item:infinite_horizon:aerostat_platform>:** ein schwebender Block. Setz weitere daneben, um deine Plattform zu vergrößern.
- **<item:infinite_horizon:cloud_elevator>:** Stell ihn auf die Plattform und mach Rechtsklick. Nach {{value:spaceflight:cloud_elevator.delay_ticks|t}} stehst du auf der Oberfläche darunter, dort entsteht automatisch die untere Station. Liegt dort Lava, entsteht eine kleine Plattform aus <item:infinite_horizon:venus_rock>. Rechtsklick unten bringt dich zurück nach oben.

@warning Unten läuft die [[hitzeanzug|Hitzezeit]] deines Anzugs. Ohne Hitzeanzug überlebst du die Oberfläche nicht.

## Gelände und Biome

- **<item:infinite_horizon:venus_lava_plains>:** Basaltebenen mit Lavaseen und Schwefel.
- **<item:infinite_horizon:venus_volcanoes>:** Vulkankegel aus <item:infinite_horizon:venus_rock> und Magmablöcken, mit Wolfram im Gestein. An ihren Flanken liegen Vulkanschlote, sie sind Erkundungsziele.
- **<item:infinite_horizon:venus_plateau>:** flache Hochebenen.

## Erze

@recipes_for infinite_horizon:aerostat_platform infinite_horizon:cloud_elevator


@ores infinite_horizon:venus

<item:infinite_horizon:venus_tungsten_ore> braucht eine Eisenspitzhacke. Schwefelerz gibt mehrere Stücke <item:infinite_horizon:sulfur> auf einmal.

@recipes_for infinite_horizon:tungsten_dust

@tip Baue deine Basis in der Wolkenschicht und fahr nur zum Abbauen hinunter.
