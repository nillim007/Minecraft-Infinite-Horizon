---
title: Long Range Flight and Jetpack
---
A T2 rocket only reaches the outer destinations beyond the asteroid belt with more fuel. That is what the long-range module is for, and the station in orbit becomes the launch pad.

## The long-range module

The <item:infinite_horizon:long_range_module> is a tier 2 rocket part. It sits between the tanks and the capsule and holds an extra {{value:rocket_parts:parts.long_range_module.fuel_capacity}} mB per fuel component. The distant destinations from Saturn on only become selectable with it.

@recipes_for infinite_horizon:long_range_module

## Launching from the docking bay

You can build a T2 rocket directly on a [[stationsmodule|docking bay]]: engine on the bay, fins around it, tanks, long-range module and capsule above, a control panel nearby. You need neither tower nor fuel pump there. The bay fills the tanks from tanks adjacent to it. T1 rockets do not launch from the bay.

@tip A launch from the station costs no fuel for the ascent. So build the big rockets in orbit.

## The jetpack

The <item:infinite_horizon:jetpack> is a module for the chestplate. It stores {{value:suits:infinite_horizon:jetpack.energy_capacity}} J; charge it in the charging slot of a battery, also while installed in the chestplate.

- Hold **jump**: gravity is cancelled and you rise at up to {{value:suits:infinite_horizon:jetpack.max_speed_up}} blocks per tick.
- **Sneak** in the air: thrust downwards.
- **Forward**: {{value:suits:infinite_horizon:jetpack.horizontal_speed}} blocks per tick in your view direction.

While it pushes, it uses {{value:suits:infinite_horizon:jetpack.energy_per_tick}} J per tick.

@recipes_for infinite_horizon:jetpack

@warning When the energy is empty you fall again, slowly in orbit, with full gravity on a planet.
