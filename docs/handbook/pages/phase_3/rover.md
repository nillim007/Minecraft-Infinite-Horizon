---
title: Der Rover
title_en: The Rover
order: 110
milestone: 5
requires: ["infinite_horizon:rover_kit"]
icon: infinite_horizon:rover_kit
items: ["infinite_horizon:rover_kit", "infinite_horizon:rover_scanner", "infinite_horizon:rover_drill", "infinite_horizon:rover_headlight", "infinite_horizon:rover_pressure_cabin", "infinite_horizon:rover_trailer"]
summary: Fahrzeug für weite Strecken auf fremden Welten
---
Zu Fuß sind die Wege auf Mars und Mond lang. Der Rover bringt dich und einen Mitfahrer mit {{value:vehicles:infinite_horizon:rover.speed}} Blöcken pro Tick über das Gelände und steigt dabei bis zu {{value:vehicles:infinite_horizon:rover.step_height}} Blöcke hohe Stufen hinauf.

## Aufstellen und fahren

1. Rechtsklick mit dem <item:infinite_horizon:rover_kit> auf den Boden stellt den Rover auf.
2. Rechtsklick steigt ein. Wer zuerst einsteigt, fährt: Der Rover folgt deiner Blickrichtung, vorwärts und rückwärts fährst du mit den Bewegungstasten.
3. Schleichen lässt dich aussteigen.

## Energie

Der Rover speichert {{value:vehicles:infinite_horizon:rover.energy_capacity}} J und braucht {{value:vehicles:infinite_horizon:rover.energy_per_block}} J je gefahrenem Block.

- **Solardach:** unter freiem Himmel {{value:vehicles:infinite_horizon:rover.solar_roof_per_tick}} J pro Tick.
- **Laden:** Park den Rover direkt neben einer <item:infinite_horizon:battery>. Sie gibt {{value:vehicles:infinite_horizon:rover.charge_rate}} J pro Tick ab.

## Frachtraum und Module

Der Frachtraum hat {{value:vehicles:infinite_horizon:rover.cargo_slots}} Slots. Du öffnest ihn mit der Inventartaste im Sitz oder mit Schleichen und Rechtsklick (mit einem Item in der Hand).

Bis zu {{value:vehicles:infinite_horizon:rover.module_slots}} Module setzt du mit Rechtsklick ein:

- <item:infinite_horizon:rover_scanner>: zählt alle {{value:vehicles:infinite_horizon:rover_scanner.interval_ticks|t}} die Erze im Umkreis von {{value:vehicles:infinite_horizon:rover_scanner.ore_radius}} Blöcken und nennt Erkundungsziele in der Nähe.
- <item:infinite_horizon:rover_drill>: baut beim Fahren die 3 × 3 Blöcke vor dem Rover ab, die Drops landen im Frachtraum.
- <item:infinite_horizon:rover_headlight>: ein heller Scheinwerfer vor dem Rover.
- <item:infinite_horizon:rover_pressure_cabin>: Insassen atmen ohne Anzug bei angenehmer Temperatur, solange der Tank ({{value:vehicles:infinite_horizon:rover_pressure_cabin.o2_tank}} mB) Sauerstoff hat. Rechtsklick mit einer gefüllten [[sauerstoff|Sauerstoffflasche]] füllt ihn.
- <item:infinite_horizon:rover_trailer>: {{value:vehicles:infinite_horizon:rover_trailer.extra_cargo_slots}} zusätzliche Frachtslots.

## Abbauen

Schleichen und Rechtsklick mit leerer Hand: Der Rover wird wieder zum Bausatz. Energie, Sauerstoff, Module und Fracht bleiben darin erhalten.

@warning Auf der Venus-Oberfläche bleibt der Rover nach {{value:vehicles:infinite_horizon:rover.venus_heat_timer_ticks|t}} stehen, bis er wieder über y {{value:vehicles:infinite_horizon:rover.venus_heat_timer_reset_above_y}} ist. Der Wolkenaufzug nimmt Rover in der Nähe mit.

## Herstellung

@recipes_for infinite_horizon:rover_kit infinite_horizon:rover_scanner infinite_horizon:rover_drill infinite_horizon:rover_headlight infinite_horizon:rover_pressure_cabin infinite_horizon:rover_trailer

@tip Stell eine Batterie in deine Basis und park dort den Rover. So ist er vor jeder Fahrt voll.
