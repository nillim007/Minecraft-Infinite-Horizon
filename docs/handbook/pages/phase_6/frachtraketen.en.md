---
title: Cargo Rockets and Drives
---
Cargo rockets bring items and fluids from one [[aussenposten|cargo terminal]] to another. You decide when they fly at the [[logistik_computer|logistics computer]]. They do not fly visibly through the world: the game only remembers start, destination and arrival time. At launch and landing you see a small puff of smoke at the terminal.

## The hangar

Every cargo terminal has {{value:logistics:cargo_flight_rules.hangar_slots_per_terminal}} hangar slots below the tanks. A rocket in a hangar slot belongs to that terminal; its flights always start there. While it is on its way, the item stays in the slot, is highlighted red and cannot be taken out.

## The cargo rocket

The <item:infinite_horizon:cargo_rocket> has {{value:logistics:infinite_horizon:cargo_rocket.item_holds}} holds of {{value:logistics:infinite_horizon:cargo_rocket.slots_per_hold}} slots each and {{value:logistics:infinite_horizon:cargo_rocket.fluid_holds}} fluid holds. It flies on methane from its own tank ({{value:logistics:infinite_horizon:cargo_rocket.fuel_tank.capacity}} mB).

- Before every launch it refuels from its terminal's fuel tank.
- One leg costs {{value:logistics:infinite_horizon:cargo_rocket.fuel_per_flight.base}} mB plus {{value:logistics:infinite_horizon:cargo_rocket.fuel_per_flight.per_delta_dp}} mB per distance point between start and destination.
- At the destination it unloads and refuels for the return flight from the destination's fuel tank. If it carried methane, it keeps as much of it as the return flight needs.
- The return flight is empty. Whatever found no room at the destination, it brings back.

@warning If the fuel at the destination is not enough for the return flight, the rocket stays there and tries again every {{value:logistics:cargo_flight_rules.stranded_retry_ticks|t}}. Store some methane at the destination.

## The heavy freighter

The <item:infinite_horizon:heavy_freighter> carries twice as much. It needs no methane: one <item:infinite_horizon:fuel_rod> lasts for {{value:logistics:infinite_horizon:heavy_freighter.fuel.flights}} round trips. When it is empty, it takes a fuel rod from its terminal's storage by itself before launching. An atomic drive is already installed.

## Drives

| Drive | Flight time |
| --- | --- |
| none | {{value:logistics:flight_time.base_ticks|t}} plus {{value:logistics:flight_time.per_delta_dp_ticks|t}} per distance point |
| <item:infinite_horizon:atomic_drive> | divided by {{value:logistics:infinite_horizon:atomic_drive.flight_time_divisor}} |
| <item:infinite_horizon:antimatter_drive> | divided by {{value:logistics:infinite_horizon:antimatter_drive.flight_time_divisor}} |

To retrofit, put the rocket and the drive into the [[montagetisch|assembly table]] together. Fuel and drive stay stored in the rocket. Only the best drive counts; a weaker one does not replace a better one.

@tip For the antimatter drive you need charged antimatter cells from the magnetic trap. The assembly table does not accept empty cells.

## Crafting

@recipes_for infinite_horizon:cargo_rocket infinite_horizon:heavy_freighter infinite_horizon:atomic_drive infinite_horizon:antimatter_drive
