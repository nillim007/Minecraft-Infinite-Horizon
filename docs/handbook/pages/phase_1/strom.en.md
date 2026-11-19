---
title: Power
---
All machines run on power, measured in **joules (J)**. How much a machine needs per tick is shown as **J/t** on its page. One tick is a twentieth of a second.

## Combustion generator

The <item:infinite_horizon:combustion_generator> is your first power plant. It burns anything that burns in a furnace and supplies {{value:generators:combustion_generator.output_per_tick}} J/t. Lava buckets work too; the empty bucket stays in the slot.

@machine infinite_horizon:combustion_generator

@image ../images/screenshots/gui-combustion-generator.png | The combustion generator window: fuel slot and remaining burn time.
@recipes_for infinite_horizon:combustion_generator

@if infinite_horizon:fuel_generator
## Fuel generator

Once your [[raffinerie|refinery]] runs, the <item:infinite_horizon:fuel_generator> is worth it. It burns kerosene and supplies {{value:generators:fuel_generator.output_per_tick}} J/t – many times the combustion generator.

@machine infinite_horizon:fuel_generator
@recipes_for infinite_horizon:fuel_generator
@endif

## Copper cable

<item:infinite_horizon:copper_cable> connects by itself to everything that produces or needs power. A cable network carries at most {{value:transport:copper_cable.throughput}} J/t, no matter how long it is. Generators push their power out of all sides, and the network spreads it evenly over all machines.

@machine infinite_horizon:copper_cable
@recipes_for infinite_horizon:copper_cable

@if infinite_horizon:battery
## Battery

The <item:infinite_horizon:battery> stores power for the night or for load peaks. It accepts power on all sides and outputs it only through its **output side** (button "Side" in the window). That way it can cleanly separate two networks.

@machine infinite_horizon:battery
@recipes_for infinite_horizon:battery
@endif

@tip A copper cable network carries {{value:transport:copper_cable.throughput}} J/t. If that is not enough, split your machines into several networks, each with its own generator.

@note Machines only use power while they work. If power runs short, they pause and later continue where they left off. Nothing is lost.
