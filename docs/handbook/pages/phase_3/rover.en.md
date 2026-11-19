---
title: The Rover
---
On foot the distances on Mars and the Moon are long. The rover carries you and one passenger across the terrain at {{value:vehicles:infinite_horizon:rover.speed}} blocks per tick and climbs steps up to {{value:vehicles:infinite_horizon:rover.step_height}} blocks high.

## Setting up and driving

1. Right-clicking the ground with the <item:infinite_horizon:rover_kit> sets up the rover.
2. Right-click to get in. Whoever gets in first drives: the rover follows your view direction, and you drive forward and backward with the movement keys.
3. Sneaking lets you get out.

## Energy

The rover stores {{value:vehicles:infinite_horizon:rover.energy_capacity}} J and needs {{value:vehicles:infinite_horizon:rover.energy_per_block}} J per block driven.

- **Solar roof:** {{value:vehicles:infinite_horizon:rover.solar_roof_per_tick}} J per tick under the open sky.
- **Charging:** park the rover right next to a <item:infinite_horizon:battery>. It hands out {{value:vehicles:infinite_horizon:rover.charge_rate}} J per tick.

## Cargo hold and modules

The cargo hold has {{value:vehicles:infinite_horizon:rover.cargo_slots}} slots. You open it with the inventory key in the seat, or by sneaking and right-clicking (with an item in hand).

You insert up to {{value:vehicles:infinite_horizon:rover.module_slots}} modules with a right-click:

- <item:infinite_horizon:rover_scanner>: every {{value:vehicles:infinite_horizon:rover_scanner.interval_ticks|t}} it counts the ores within {{value:vehicles:infinite_horizon:rover_scanner.ore_radius}} blocks and names exploration targets nearby.
- <item:infinite_horizon:rover_drill>: while driving it mines the 3 × 3 blocks in front of the rover; the drops land in the cargo hold.
- <item:infinite_horizon:rover_headlight>: a bright headlight in front of the rover.
- <item:infinite_horizon:rover_pressure_cabin>: passengers breathe without a suit at a pleasant temperature as long as the tank ({{value:vehicles:infinite_horizon:rover_pressure_cabin.o2_tank}} mB) has oxygen. Right-clicking with a filled [[sauerstoff|oxygen canister]] fills it.
- <item:infinite_horizon:rover_trailer>: {{value:vehicles:infinite_horizon:rover_trailer.extra_cargo_slots}} extra cargo slots.

## Dismantling

Sneak and right-click with an empty hand: the rover becomes a kit again. Energy, oxygen, modules and cargo are kept inside.

@warning On the surface of Venus the rover stops after {{value:vehicles:infinite_horizon:rover.venus_heat_timer_ticks|t}} until it is back above y {{value:vehicles:infinite_horizon:rover.venus_heat_timer_reset_above_y}}. The cloud elevator takes rovers nearby along.

## Crafting

@recipes_for infinite_horizon:rover_kit infinite_horizon:rover_scanner infinite_horizon:rover_drill infinite_horizon:rover_headlight infinite_horizon:rover_pressure_cabin infinite_horizon:rover_trailer

@tip Put a battery in your base and park the rover there. That way it is full before every drive.
