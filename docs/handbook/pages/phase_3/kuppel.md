---
title: Die Kuppel
title_en: The Dome
order: 30
milestone: 3
requires: ["infinite_horizon:dome_generator_t1"]
icon: infinite_horizon:dome_generator_t1
items: ["infinite_horizon:dome_generator_t1", "infinite_horizon:dome_generator_t2", "infinite_horizon:dome_range_module", "infinite_horizon:dome_efficiency_module", "infinite_horizon:dome_shield_module"]
summary: Atemluft für deine Basis auf fremden Welten
---
Unter einer Kuppel kannst du ohne Anzug atmen und arbeiten. Der <item:infinite_horizon:dome_generator_t1> spannt eine unsichtbare Halbkugel um sich auf. Darin gibt es Atemluft, normalen Druck und angenehme {{value:domes:dome_rules.inside_temperature}} °C.

@machine infinite_horizon:dome_generator_t1

## So baust du eine Kuppel

1. Stell den Generator in die Mitte deiner Basis. Die Kuppel reicht {{value:domes:dome_generator_t1.radius}} Blöcke weit und beginnt einen Block unter dem Generator.
2. Versorge ihn mit **Strom**: {{value:domes:dome_generator_t1.energy_per_tick}} J/t.
3. Füll seinen **Sauerstofftank**: über Rohre, einen angrenzenden [[sauerstoffgenerator|Sauerstoffgenerator]] oder [[sauerstoff|Flaschen]] im Leerslot. Ihren Verbrauch zeigt der Steckbrief oben.
4. Liegen Strom und Sauerstoff an und passt die Außentemperatur, läuft die Kuppel sofort. Die Lampe am Generator wird grün, und am Rand der Kuppel funkeln Lichtpunkte.

Die Außentemperatur misst der Generator an seinem Standort. Sie muss zwischen {{value:domes:dome_generator_t1.temp_min}} und {{value:domes:dome_generator_t1.temp_max}} °C liegen, sonst startet die Kuppel nicht (rote Lampe). Überlappende Kuppeln bilden einen gemeinsamen Luftraum.

## Anzüge nachfüllen

Steht in der Kuppel ein Spieler mit [[raumanzug|Druckanzug]], füllt der Generator dessen Sauerstoff aus seinem Tank nach, solange dort mindestens {{value:suits:oxygen_supply.dome_refill_min_tank}} mB liegen.

## Ausfall

Fehlen Strom oder Sauerstoff oder passt die Temperatur nicht mehr, geht die Kuppel in den **Ausfall**: Die Lichtpunkte pulsieren rot, alle Spieler darin bekommen eine Warnung mit den verbleibenden Sekunden, und ein Warnton erklingt. Die Kuppel schützt noch {{value:domes:dome_rules.failure_ticks|t}}, dann ist sie aus. Kommt die Versorgung vorher zurück, läuft sie sofort weiter.

@warning Ist die Kuppel aus, gilt dort wieder das Vakuum des Planeten. Wer dann ohne Anzug drin steht, baut sofort Belastung auf. Halte immer einen gefüllten Anzug in Reichweite.

## Herstellung

@if infinite_horizon:research_terminal
@note Den Kuppelgenerator schaltet der Knoten **Kuppel I** in Stufe 2 frei ([[forschung|Forschung]]).
@endif

@recipes_for infinite_horizon:dome_generator_t1

@if infinite_horizon:dome_generator_t2
## Kuppel T2

Der <item:infinite_horizon:dome_generator_t2> spannt eine Kuppel mit Radius {{value:domes:infinite_horizon:dome_generator_t2.radius}} auf und hält Außentemperaturen von {{value:domes:infinite_horizon:dome_generator_t2.temp_min}} bis {{value:domes:infinite_horizon:dome_generator_t2.temp_max}} °C aus. Darunter schützt sie auch vor Staubsturm und Säureregen (siehe [[ereignisse|Weltraumwetter]]): Solarpanels liefern im Staubsturm vollen Strom.

@machine infinite_horizon:dome_generator_t2

## Kuppelmodule

Jeder Kuppelgenerator hat {{value:domes:infinite_horizon:dome_generator_t1.module_slots}} Modulslots. Jedes Modul passt einmal hinein.

- <item:infinite_horizon:dome_range_module>: Radius × {{value:domes:infinite_horizon:dome_range_module.radius_multiplier}}, dafür Strom und Sauerstoff × {{value:domes:infinite_horizon:dome_range_module.oxygen_multiplier}}.
- <item:infinite_horizon:dome_efficiency_module>: Sauerstoff × {{value:domes:infinite_horizon:dome_efficiency_module.oxygen_multiplier}}.
- <item:infinite_horizon:dome_shield_module>: fängt Meteoriten ab. Monster hält es nicht auf.

@recipes_for infinite_horizon:dome_generator_t2 infinite_horizon:dome_range_module infinite_horizon:dome_efficiency_module infinite_horizon:dome_shield_module
@endif

@tip Stell eine Druckflasche oder einen zweiten Sauerstoffgenerator an den Kuppelgenerator. Dann reicht der Vorrat auch, wenn du länger unterwegs bist.
