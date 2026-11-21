---
title: Fluids and Pipes
---
Crude oil, kerosene, hydrogen and oxygen are **fluids**. They are measured in millibuckets: 1,000 mB are one bucket.

## Pump

The <item:infinite_horizon:fluid_pump> drains source blocks below it: water, lava or crude oil, up to {{value:machines:fluid_pump.process.search_radius}} blocks deep and the same distance to the sides. Each operation brings {{value:machines:fluid_pump.process.output_per_cycle}} mB. With crude oil, the source block disappears only with {{value:machines:fluid_pump.process.source_consume_chance.crude_oil|%}} probability, so an oil pocket lasts a very long time.

@machine infinite_horizon:fluid_pump

@image ../images/screenshots/gui-fluid-pump.png | The pump window: tank and bucket slots. The pump outputs on every side except its front.
@recipes_for infinite_horizon:fluid_pump

## Copper pipe

The <item:infinite_horizon:copper_pipe> carries liquids and gases, {{value:transport:copper_pipe.throughput}} mB per tick. Two important rules:

- A pipe network always carries **only one fluid**. Only once it is empty can another one enter. So water and crude oil need separate pipes.
- Pipes never pull by themselves. Machines and tanks **push** their output in: the pump on every side except its front, tanks on every side, other machines through their output side.

@recipes_for infinite_horizon:copper_pipe

## Tank

The <item:infinite_horizon:fluid_tank> holds {{value:storage:fluid_tank.capacity}} mB of one liquid. It accepts and outputs on every side, but never into another tank. When broken it keeps its contents; the tooltip shows them. Gases do not fit; for now they stay in the machine's tank.

@machine infinite_horizon:fluid_tank

@image ../images/screenshots/gui-fluid-tank.png | The tank window with fill and drain slots.
@recipes_for infinite_horizon:fluid_tank

## Buckets, fill and drain slots

Every machine with a tank has two special slots:

- **Fill slot:** put an empty bucket in, and the machine fills it from its tank.
- **Drain slot:** put a full bucket in, and the machine empties it into its tank.

You also get a <item:infinite_horizon:crude_oil_bucket> by clicking a crude oil source block with an empty bucket. A <item:infinite_horizon:kerosene_bucket> can only be filled in the fill slot of the refinery or a tank.

@tip Put the pump directly above an oil puddle and the tank next to it. With a copper pipe in between, the crude oil flows by itself.

@warning A pipe that already carries water does not accept crude oil. If nothing flows, check whether an old fluid is still stuck in the network.
