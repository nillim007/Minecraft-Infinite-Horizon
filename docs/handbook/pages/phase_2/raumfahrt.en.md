---
title: Launch Pad and Probe
---
Before people fly, a probe flies. Only once it has reached a destination does your agency know the way there, and you can follow with the [[rakete|rocket]]. Both launch from the same **launch pad**.

## Building the launch pad

@machine infinite_horizon:launch_pad

1. **Platform:** lay an area of <item:infinite_horizon:launch_pad>, 5 × 5 blocks at the same height. Its centre is the block where the engine or the probe will later stand. Additional pad blocks next to it do no harm.
2. **Tower:** place a vertical column of <item:infinite_horizon:launch_tower> directly next to the edge of the area, not at a corner. Its lowest block stands at platform height. The tower must be at least as tall as the rocket (fins do not count); for a probe one block is enough.
3. **Pump:** at least one <item:infinite_horizon:fuel_pump> touches the platform or tower with one face.
4. **Control panel:** place the <item:infinite_horizon:launch_control> at most {{value:machines:launch_pad_t1_multiblock.control_radius}} blocks from the centre. It always controls the nearest valid pad.

The control panel checks the pad when you open it, at launch and every {{value:misc:multiblock_rules.structure_check_interval_ticks|t}}. If something is wrong, its lamp glows red and the checklist names the first error.

## Fuelling

The fuel pump has two tanks: one for fuel (<item:infinite_horizon:kerosene>) and one for oxygen. It accepts from all sides, from pipes, tanks and a <item:infinite_horizon:pressure_bottle>, and through the fill and drain slots also from a <item:infinite_horizon:transport_tank>. It needs no power.

@machine infinite_horizon:fuel_pump

Every tick it hands out up to {{value:machines:fuel_pump.transfer_per_tick}} mB of each component and spreads it evenly over all rocket tanks on its pad. A rocket tank holds {{value:rocket_parts:parts.rocket_tank_t1.fuel_capacity}} mB per component.

## The probe

1. Research the **Moon probe** node ([[forschung|Research]]). It makes the Moon a probe destination.
2. Place a <item:infinite_horizon:probe> on the centre of the pad.
3. Open the control panel. Under "Probe on the pad" there is a button for every researched destination. One click starts the {{value:spaceflight:probe_flight.countdown_ticks|t}} countdown.
4. The probe rises and disappears. Its flight takes {{value:spaceflight:probe_flight.base_ticks|t}} plus {{value:spaceflight:probe_flight.per_dp_ticks|t}} per distance point of the destination. It keeps going even if you log out.
5. When it arrives, all members get a message and the advancement "First Signal", and the first time your agency gets {{value:research:probe_new_target.fp}} RP. From now on the destination is a rocket destination.

@note A probe never returns. You need a separate one for every destination.

## Crafting

@recipes_for infinite_horizon:launch_pad infinite_horizon:launch_tower infinite_horizon:fuel_pump infinite_horizon:launch_control infinite_horizon:probe

@tip Build the tower tall enough for the biggest rocket of your tier right away. Then you do not have to rebuild later.
