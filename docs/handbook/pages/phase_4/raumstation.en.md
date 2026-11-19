---
title: The Space Station
---
Every station starts with a <item:infinite_horizon:station_core>. It can only be placed in orbit, even in mid-air: without a target block it lands {{value:stations:infinite_horizon:station_core.air_placement_distance}} blocks in front of you, and from a rocket's capsule right below it. Everything within {{value:stations:infinite_horizon:station_core.station_radius}} blocks belongs to the station.

## What the core does

- Placing it creates a platform of {{value:stations:infinite_horizon:station_core.platform.size}} × {{value:stations:infinite_horizon:station_core.platform.size}} blocks of <item:infinite_horizon:station_hull>. These platform blocks drop nothing when mined.
- A built-in dome with radius {{value:stations:infinite_horizon:station_core.builtin_dome.radius}} creates breathable air. It needs oxygen in the tank ({{value:stations:infinite_horizon:station_core.o2_tank}} mB) and power.
- The power comes from {{value:stations:infinite_horizon:station_core.builtin_solar.panels}} built-in solar cells with {{value:stations:infinite_horizon:station_core.builtin_solar.output_per_panel}} J/t each times the orbit's solar factor. The core also accepts power through cables.

@machine infinite_horizon:station_core

## Building

<item:infinite_horizon:station_hull> is the building block for walls and floors, <item:infinite_horizon:station_window> is transparent like glass.

@recipes_for infinite_horizon:station_core infinite_horizon:station_hull infinite_horizon:station_window

@warning Without oxygen in the tank, the core's dome goes out like any other dome. Keep the tank filled through pipes or pressure bottles.

@tip You fill the oxygen tank like the dome generator's, through the fill slot or an adjacent oxygen generator.
