---
title: Rohstoffe und Erze
title_en: Resources and Ores
order: 20
milestone: 2
requires: ["sternenwerft:bauxite_ore"]
icon: sternenwerft:bauxite_ore
items: ["sternenwerft:bauxite_ore", "sternenwerft:bauxite", "sternenwerft:titanium_ore", "sternenwerft:deepslate_titanium_ore", "sternenwerft:raw_titanium", "sternenwerft:quartz_sand"]
summary: Wo du Bauxit, Titan, Quarzsand und Erdöl findest
---
Neben den Vanilla-Erzen bringt Sternenwerft drei neue Rohstoffe auf die Erde. Dazu kommt Erdöl tief im Boden.

## Fundorte

@ores minecraft:overworld

## Bauxit – das Aluminium der Erde

<item:sternenwerft:bauxite> ist die Rohform von Aluminium. Du findest es weit oben, zwischen y {{value:planet:overworld.ores.0.y_min}} und {{value:planet:overworld.ores.0.y_max}}, aber nur in heißen, trockenen Gegenden: Savanne, Wüste und Tafelberge. Eine Steinspitzhacke reicht zum Abbauen.

## Titan – tief und hart

<item:sternenwerft:titanium_ore> liegt tief im Boden, unter y 0 meist als Tiefenschiefer-Variante. Du brauchst mindestens eine **Eisenspitzhacke**, sonst bekommst du nichts. Titan wirst du für Raketen in großen Mengen brauchen, also merk dir gute Fundstellen.

## Quarzsand – für Silizium

<item:sternenwerft:quartz_sand> liegt in kleinen Scheiben am Strand und an Flussufern, ähnlich wie Ton. Im Elektroofen wird daraus <item:sternenwerft:silicon>.

## Erdöl

@if sternenwerft:crude_oil
Erdöl liegt in **Ölblasen** unter Wüsten und Ozeanen, zwischen y {{value:planet:overworld.features.0.params.y_min}} und {{value:planet:overworld.features.0.params.y_max}}. Etwa eine von {{value:planet:overworld.features.0.params.rarity_chunks}} Chunks hat eine. An der Oberfläche siehst du nichts davon; oft findest du sie, wenn eine Höhle plötzlich schwarz glänzt. Wie du Erdöl förderst, steht unter [[fluide|Fluide und Rohre]].
@endif

@tip Bauxit kannst du auch direkt im Ofen schmelzen. Zerkleinerst du es vorher, bekommst du aber doppelt so viel Aluminium.
