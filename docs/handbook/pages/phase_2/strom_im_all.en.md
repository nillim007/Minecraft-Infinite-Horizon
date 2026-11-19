---
title: Power in Space
---
On the Moon there is neither coal nor kerosene at your doorstep. The <item:infinite_horizon:solar_panel> makes power from sunlight, and <item:infinite_horizon:aluminum_cable> carries more power than copper.

## The solar panel

A solar panel needs **open sky** above it. Then it supplies {{value:generators:solar_panel.output_per_tick.surface}} J/t during the day, multiplied by the **solar factor** of the location. At dusk the output drops, at night it is 0. In rain it only brings {{value:generators:solar_panel.weather_multipliers.minecraft:rain|%}}.

@machine infinite_horizon:solar_panel

| Location | Solar factor |
| --- | --- |
| Earth | {{value:planet:overworld.solar_factor}} |
| Moon | {{value:planet:moon.solar_factor}} |
| Moon polar crater | {{value:planet:moon.zones.0.solar_factor}} |

The panel pushes its power into all adjacent cables and machines. Its buffer covers short breaks; for the night you need a [[strom|battery]].

@warning No sunlight reaches the Moon's polar craters. A solar panel supplies nothing there.

## The aluminium cable

An <item:infinite_horizon:aluminum_cable> carries {{value:transport:aluminum_cable.throughput}} J/t, a <item:infinite_horizon:copper_cable> only {{value:transport:copper_cable.throughput}} J/t. Cables of different kinds do not connect directly. For the transition, put a battery in between.

## Crafting

@recipes_for infinite_horizon:solar_panel infinite_horizon:aluminum_cable

@tip Put a few solar panels and a battery next to every Moon base. During the day they charge the battery, at night it supplies dome and machines.
