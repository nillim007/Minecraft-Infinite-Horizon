---
title: The Fusion Reactor
---
The fusion reactor is the station's power plant: {{value:generators:infinite_horizon:fusion_reactor.output_per_tick}} J/t from hydrogen and helium. It is a large multiblock and constantly needs cooling with liquid helium.

@machine infinite_horizon:fusion_reactor

## The structure

The reactor is a cube of {{value:machines:fusion_reactor_multiblock.size.width}} × {{value:machines:fusion_reactor_multiblock.size.depth}} × {{value:machines:fusion_reactor_multiblock.size.height}} blocks with a hollow interior.

1. Place the controller (<item:infinite_horizon:fusion_reactor>). Its front faces you.
2. The controller is the **middle of one side face**: the cube extends two blocks left, right, up and down from it and four blocks back.
3. Build the shell from <item:infinite_horizon:fusion_casing>. The interior of {{value:machines:fusion_reactor_multiblock.interior.width}} × {{value:machines:fusion_reactor_multiblock.interior.depth}} × {{value:machines:fusion_reactor_multiblock.interior.height}} blocks stays air.
4. Replace single casing blocks with **ports** where pipes and cables should connect, as with the [[raffinerie|refinery]].
5. As soon as everything is right, the status lamp turns from red to yellow.

@tip **Sneak-right-click** the controller with an empty hand to see red particles where blocks are still missing.

## Operation

Every {{value:generators:infinite_horizon:fusion_reactor.fuel.0.per_ticks|t}} the reactor burns {{value:generators:infinite_horizon:fusion_reactor.fuel.0.amount}} mB of hydrogen and {{value:generators:infinite_horizon:fusion_reactor.fuel.1.amount}} mB of helium. For cooling it needs {{value:generators:infinite_horizon:fusion_reactor.coolant.0.amount}} mB of liquid helium from the [[helium|cryo liquefier]] at the same rate. The evaporated helium ends up in the output tank and can go back into the cycle.

@warning If cooling is missing for {{value:generators:infinite_horizon:fusion_reactor.safety.shutdown_after_ticks_without_cooling|t}}, the reactor shuts down and the lamp turns red. It does not explode. It restarts by itself once cooling has run for {{value:generators:infinite_horizon:fusion_reactor.safety.restart_after_ticks_with_cooling|t}} without interruption.

## Superconductor cable

A fusion reactor supplies more power than aluminium cables can carry. The <item:infinite_horizon:superconductor_cable> carries {{value:transport:infinite_horizon:superconductor_cable.throughput}} J/t.

## Gravity plate

The <item:infinite_horizon:gravity_plate> needs a lot of power. In return, normal gravity applies above it in orbit: in an area of {{value:stations:infinite_horizon:gravity_plate.area.width}} × {{value:stations:infinite_horizon:gravity_plate.area.depth}} blocks and {{value:stations:infinite_horizon:gravity_plate.area.height}} blocks high.

@machine infinite_horizon:gravity_plate

## Crafting

@recipes_for infinite_horizon:fusion_reactor infinite_horizon:fusion_casing infinite_horizon:superconductor_cable infinite_horizon:gravity_plate

@tip Put a cryo liquefier next to the output port: it turns the evaporated helium back into coolant.
