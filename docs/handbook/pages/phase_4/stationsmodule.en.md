---
title: Station Modules
---
Around the [[raumstation|station core]] you expand your station with modules. All modules only work in orbit, except the tank depot, which works everywhere.

## Docking bay

The <item:infinite_horizon:docking_bay> is a landing pad for rockets. Like a landing beacon it appears in your agency's choice of landing sites and can be named with a right-click. If you choose it as the destination, the rocket touches down right on it. A rocket standing on the bay refuels from adjacent tanks, as fast as at a fuel pump.

## Tank depot

The <item:infinite_horizon:tank_depot> holds {{value:storage:infinite_horizon:tank_depot.capacity}} mB of one fluid. It outputs only through its output side; all other sides accept. Next to the docking bay it is your station's fuel store.

@machine infinite_horizon:tank_depot

## Solar wing

The <item:infinite_horizon:solar_wing> supplies {{value:generators:infinite_horizon:solar_wing.output_per_tick.orbit}} J/t times the solar factor in orbit, as long as it sees the Sun. On a planet it supplies nothing.

## Habitat module

The <item:infinite_horizon:habitation_module> is bed and storage in one: right-clicking sets your respawn point above the module and opens {{value:stations:infinite_horizon:habitation_module.storage_slots}} storage slots. The respawn point only applies under an active dome.

## Research lab

An analyzer directly next to the <item:infinite_horizon:research_lab> brings {{value:research:research_lab.analyzer_fp_multiplier}} times as many research points.

## Greenhouse

The <item:infinite_horizon:greenhouse> works like {{value:agriculture:infinite_horizon:greenhouse.behaves_like_basins}} [[hydrokultur|hydroponics basins]] at once. It needs power, water, light, an active dome and one <item:infinite_horizon:dry_ice> in the last input slot every {{value:agriculture:infinite_horizon:greenhouse.consumes.per_ticks|t}}.

@machine infinite_horizon:greenhouse

## Crafting

@recipes_for infinite_horizon:docking_bay infinite_horizon:tank_depot infinite_horizon:solar_wing infinite_horizon:habitation_module infinite_horizon:research_lab infinite_horizon:greenhouse

@warning The habitat module only sets the respawn point inside an active dome. If it fails, you reappear on Earth after death.

@tip Put a tank depot with hydrogen and oxygen right next to the docking bay. Then your rocket is quickly full again after every landing.
