---
title: The Fission Reactor
---
The fission reactor supplies {{value:generators:infinite_horizon:fission_reactor.output_per_tick}} J/t from <item:infinite_horizon:fuel_rod>. One fuel rod lasts {{value:generators:infinite_horizon:fission_reactor.fuel.0.per_ticks|t}}. The reactor has {{value:generators:infinite_horizon:fission_reactor.item_slots.fuel}} slots for fuel rods and takes the next one by itself.

@machine infinite_horizon:fission_reactor

## The structure

The reactor is a cube of {{value:machines:fission_reactor_multiblock.size.width}} × {{value:machines:fission_reactor_multiblock.size.depth}} × {{value:machines:fission_reactor_multiblock.size.height}} blocks with a hollow interior, built like the [[fusionsreaktor|fusion reactor]].

1. Place the controller (<item:infinite_horizon:fission_reactor>). Its front faces you.
2. The controller is the **middle of one side face**: the cube extends two blocks left, right, up and down from it and four blocks back.
3. Build the shell from <item:infinite_horizon:fission_casing>. The interior of {{value:machines:fission_reactor_multiblock.interior.width}} × {{value:machines:fission_reactor_multiblock.interior.depth}} × {{value:machines:fission_reactor_multiblock.interior.height}} blocks stays air.
4. Replace single casing blocks with **ports** where pipes and cables should connect.
5. As soon as everything is right, the status lamp turns from red to yellow.

@tip **Sneak-right-click** the controller with an empty hand to see red particles where blocks are still missing.

## Cooling and heat

While the reactor works, it needs {{value:generators:infinite_horizon:fission_reactor.coolant.0.amount}} mB of water **or** {{value:generators:infinite_horizon:fission_reactor.coolant.1.amount}} mB of liquid nitrogen per tick in the coolant tank. Heat changes by {{value:generators:infinite_horizon:fission_reactor.safety.heat_per_tick_cooled}} per tick with cooling and by {{value:generators:infinite_horizon:fission_reactor.safety.heat_per_tick_uncooled}} without. The progress bar in the window shows the heat.

- From {{value:generators:infinite_horizon:fission_reactor.safety.warning_at}} it warns with a tone, and the status lamp flashes.
- At {{value:generators:infinite_horizon:fission_reactor.safety.scram_at}} it shuts itself down. It does not explode, but within {{value:generators:infinite_horizon:fission_reactor.safety.radiation_zone.radius}} blocks there is level {{value:generators:infinite_horizon:fission_reactor.safety.radiation_zone.radiation}} radiation for {{value:generators:infinite_horizon:fission_reactor.safety.radiation_zone.duration_ticks|t}}.
- As soon as the heat is back below {{value:generators:infinite_horizon:fission_reactor.safety.restart_below_heat}}, it restarts by itself.

@warning After an emergency shutdown you need a suit with a radiation shield or a tier 3 dome for the radiation zone. Without protection you take damage there.

@tip A water pump over a source next to the reactor reliably keeps the coolant tank full.

## Crafting

@recipes_for infinite_horizon:fission_reactor infinite_horizon:fission_casing infinite_horizon:fuel_rod
