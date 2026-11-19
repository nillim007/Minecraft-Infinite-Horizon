---
title: Lithium
---
Lithium comes as a salt crust from [[merkur|Mercury]]. The crusher turns it into dust, the electric furnace into ingots. From the ingots you build the <item:infinite_horizon:lithium_battery>, a component of almost all devices of the fourth phase.

@recipes_for infinite_horizon:lithium_dust infinite_horizon:lithium_ingot infinite_horizon:lithium_battery

## Lithium battery bank

The <item:infinite_horizon:lithium_battery_bank> stores {{value:storage:infinite_horizon:lithium_battery_bank.capacity}} J and outputs up to {{value:storage:infinite_horizon:lithium_battery_bank.io_per_tick}} J/t, through its output side like the battery.

@machine infinite_horizon:lithium_battery_bank

## Rover battery

The <item:infinite_horizon:rover_lithium_pack> is a module for the [[rover|rover]]: with it, the rover holds {{value:vehicles:infinite_horizon:rover_lithium_pack.energy_multiplier}} times as much energy.

@recipes_for infinite_horizon:lithium_battery_bank infinite_horizon:rover_lithium_pack

@tip Lithium salt gives the same amount of lithium in the crusher and the electric furnace. Skip the crusher as long as you need it for ores.
