---
title: The Dome
---
Under a dome you can breathe and work without a suit. The <item:infinite_horizon:dome_generator_t1> spreads an invisible hemisphere around itself. Inside there is breathable air, normal pressure and a pleasant {{value:domes:dome_rules.inside_temperature}} °C.

@machine infinite_horizon:dome_generator_t1

## How to build a dome

1. Put the generator in the middle of your base. The dome reaches {{value:domes:dome_generator_t1.radius}} blocks and starts one block below the generator.
2. Supply it with **power**: {{value:domes:dome_generator_t1.energy_per_tick}} J/t.
3. Fill its **oxygen tank**: through pipes, an adjacent [[sauerstoffgenerator|oxygen generator]] or [[sauerstoff|canisters]] in the drain slot. The fact sheet above shows how much it uses.
4. With power and oxygen supplied and a suitable outside temperature, the dome runs at once. The lamp on the generator turns green, and points of light sparkle at the edge of the dome.

The generator measures the outside temperature where it stands. It must be between {{value:domes:dome_generator_t1.temp_min}} and {{value:domes:dome_generator_t1.temp_max}} °C, otherwise the dome does not start (red lamp). Overlapping domes form a shared air space.

## Refilling suits

If a player in a [[raumanzug|pressure suit]] stands in the dome, the generator refills their oxygen from its tank as long as at least {{value:suits:oxygen_supply.dome_refill_min_tank}} mB are left there.

## Failure

If power or oxygen run out or the temperature no longer fits, the dome goes into **failure**: the points of light pulse red, all players inside get a warning with the seconds left, and a warning tone sounds. The dome still protects for {{value:domes:dome_rules.failure_ticks|t}}, then it is off. If the supply returns before that, it keeps running right away.

@warning Once the dome is off, the planet's vacuum applies there again. Anyone standing inside without a suit builds up strain immediately. Always keep a filled suit within reach.

## Crafting

@if infinite_horizon:research_terminal
@note The **Dome I** node in tier 2 unlocks the dome generator ([[forschung|Research]]).
@endif

@recipes_for infinite_horizon:dome_generator_t1

@if infinite_horizon:dome_generator_t2
## Dome T2

The <item:infinite_horizon:dome_generator_t2> spreads a dome with radius {{value:domes:infinite_horizon:dome_generator_t2.radius}} and withstands outside temperatures from {{value:domes:infinite_horizon:dome_generator_t2.temp_min}} to {{value:domes:infinite_horizon:dome_generator_t2.temp_max}} °C. Beneath it you are also protected from dust storms and acid rain (see [[ereignisse|Space Weather]]): solar panels supply full power in a dust storm.

@machine infinite_horizon:dome_generator_t2
@endif

@if infinite_horizon:dome_generator_t3
## Dome T3

The <item:infinite_horizon:dome_generator_t3> spreads a dome with radius {{value:domes:infinite_horizon:dome_generator_t3.radius}} and withstands {{value:domes:infinite_horizon:dome_generator_t3.temp_min}} to {{value:domes:infinite_horizon:dome_generator_t3.temp_max}} °C. It also shields against radiation and solar storms: machines beneath it do not pause in a solar storm.

@machine infinite_horizon:dome_generator_t3

@recipes_for infinite_horizon:dome_generator_t3
@endif

@if infinite_horizon:dome_generator_t2

## Dome modules

Every dome generator has {{value:domes:infinite_horizon:dome_generator_t1.module_slots}} module slots. Each module fits in once.

- <item:infinite_horizon:dome_range_module>: radius × {{value:domes:infinite_horizon:dome_range_module.radius_multiplier}}, but power and oxygen × {{value:domes:infinite_horizon:dome_range_module.oxygen_multiplier}}.
- <item:infinite_horizon:dome_efficiency_module>: oxygen × {{value:domes:infinite_horizon:dome_efficiency_module.oxygen_multiplier}}.
- <item:infinite_horizon:dome_shield_module>: intercepts meteorites. It does not stop monsters.

@recipes_for infinite_horizon:dome_generator_t2 infinite_horizon:dome_range_module infinite_horizon:dome_efficiency_module infinite_horizon:dome_shield_module
@endif

@tip Connect a pressure bottle or a second oxygen generator to the dome generator. Then the supply lasts even when you are away for a while.
