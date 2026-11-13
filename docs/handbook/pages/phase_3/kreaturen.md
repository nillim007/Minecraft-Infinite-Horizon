---
title: Kreaturen des inneren Sonnensystems
title_en: Creatures of the Inner Solar System
order: 120
milestone: 5
requires: ["infinite_horizon:field_scanner"]
icon: infinite_horizon:field_scanner
items: ["infinite_horizon:field_scanner", "infinite_horizon:metal_concentrate", "infinite_horizon:armor_plate", "infinite_horizon:sulfur_trail"]
summary: Was auf Mond, Mars und Venus lebt, und wie du es erforschst
---
Auf den fremden Welten lebt mehr, als man denkt. Die meisten Kreaturen sind harmlos, einige nützlich, einer ist gefährlich. Mit dem <item:infinite_horizon:field_scanner> analysierst du sie: Rechtsklick auf eine Kreatur gibt deiner Agentur beim ersten Mal je Art {{value:research:creature_analysis.fp}} FP.

## Mond

- **Geröllkrabbler:** liegt getarnt wie ein Stein herum, bis du näher als {{value:creatures:infinite_horizon:rubble_crawler.behavior_params.reveal_distance}} Blöcke kommst. Er frisst Kabel und Metallblöcke außerhalb von Kuppeln, alle {{value:creatures:infinite_horizon:rubble_crawler.behavior_params.eat_interval_ticks|t}} einen. Greifst du ihn an, wehrt er sich. Er lässt <item:infinite_horizon:metal_concentrate> fallen, das im Zerkleinerer Metall gibt.
- **Schattenläufer:** scheu, flieht vor Licht und vor dir. Folgst du ihm lange genug, führt er dich zu Eis oder Erz in der Nähe.

## Mars

- **Dünengräber:** gräbt sich unsichtbar durch den Sand und wird von Vibrationen angelockt, etwa von einem laufenden Zerkleinerer oder dem Bohrer deines [[rover|Rovers]]. Er greift Spieler an und lässt <item:infinite_horizon:armor_plate> fallen.
- **Staubgeist:** schwebt friedlich und ist gegen Pfeile immun. Kommst du ihm nahe, führt er dich meist zu einer Lavaröhre oder einem Meteoritenfeld und verschwindet dann.

## Venus

- **Schwefelschnecke:** feuerfest, hinterlässt eine dünne <item:infinite_horizon:sulfur_trail>, die nach einer Weile verschwindet, und legt ab und zu <item:infinite_horizon:sulfur>. Mit Schwefel vermehrt sie sich.
- **Wolkenrochen:** fliegt friedlich umher. Mit <item:infinite_horizon:sulfur> kannst du ihn zähmen (nicht jeder Versuch gelingt), mit einem Sattel wird er zum fliegenden Reittier. Er fliegt in deine Blickrichtung.

@warning Dünengräber kommen nur im Dunkeln heraus. Eine laufende Maschine auf dem Mars lockt sie aus großer Entfernung an.

## Herstellung

@recipes_for infinite_horizon:field_scanner

@tip Analysiere jede Art einmal: Das sind leichte Forschungspunkte auf jeder neuen Welt.
