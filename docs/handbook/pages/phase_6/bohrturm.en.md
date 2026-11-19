---
title: Drilling Rig and Pump Station
---
The drilling rig and the pump station are the producers of your outposts. They extract endlessly without changing the world.

## The drilling rig

When set up, the <item:infinite_horizon:drilling_rig> counts all ore blocks in the chunk below it, down to the bottom of the world. After that it produces one ore every {{value:machines:drilling_rig.ticks|t}}, weighted randomly by those counts: if there is much iron and little titanium below it, mostly iron comes out. The deposit never runs out.

@machine infinite_horizon:drilling_rig

@warning The drilling rig does not work on Earth. If it finds no ores below it, its lamp glows red.

@tip Put the drilling rig over an area with the ores you need, for example in the dark highlands on Pluto for uranium.

## The pump station

The <item:infinite_horizon:pump_station> extracts from the block directly below it, {{value:machines:pump_station.process.output_per_cycle}} mB every {{value:machines:pump_station.ticks|t}}. The source stays.

| Below the pump station | Fluid |
| --- | --- |
| <item:infinite_horizon:ammonia_geyser> (Enceladus) | <item:infinite_horizon:ammonia> |
| <item:infinite_horizon:nitrogen_geyser> (Triton) | <item:infinite_horizon:nitrogen> |
| Source block of water, lava, crude oil or methane | the same fluid |

In the methane rain on Titan it extracts {{value:events:infinite_horizon:methane_rain.reward.pump_station_multiplier}} times as much.

@machine infinite_horizon:pump_station

## The pontoon

You cannot set up the pump station directly on a lake. First place a <item:infinite_horizon:pontoon> on the fluid, like a lily pad. A pump station on the pontoon extracts from the fluid below.

## Nitrogen

In the [[helium|cryo liquefier]] nitrogen becomes <item:infinite_horizon:liquid_nitrogen>. It cools the [[kernreaktor|fission reactor]] with a much smaller amount than water.

@recipes_for infinite_horizon:liquid_nitrogen

## Crafting

@recipes_for infinite_horizon:drilling_rig infinite_horizon:pump_station infinite_horizon:pontoon
