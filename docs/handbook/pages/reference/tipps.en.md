---
title: Tips and FAQ
---
## Tips for the start

- **Crush first, then smelt.** That doubles every metal.
- **Keep a stock of machine frames.** Almost every machine needs one.
- **Keep titanium in mind.** Rockets eat titanium plates. Remember good spots deep underground.
- **Plan the refinery early.** Without plastic there are no circuits, and without circuits you cannot go to space.
- **One network, one purpose.** Separate pipes for water and crude oil save you a lot of searching.

## Troubleshooting

**The lamp is yellow and nothing happens.**
The machine is waiting for material or power. Check whether the energy bar is empty and whether the input matches a recipe.

**The lamp is red.**
The output is full, or a block is missing in the refinery. Hover over the lamp in the window; it shows the reason.

**The assembly table does not start.**
It needs every ingredient in at least the recipe amount and no foreign items in the slots. See [[montagetisch|Assembly Table]].

**The pipe transports nothing.**
A pipe network carries only one fluid. If another one is still inside, it waits until the network is empty. Also check that the source outputs on that side: the pump not at its front, machines only through their output side ("Side" button).

**My machines run slowly although the generator is full.**
A copper cable carries only {{value:transport:copper_cable.throughput}} J/t. If too many machines hang on one network, split it.

**I cannot find titanium.**
Titanium lies below y 0. Without an iron pickaxe the ore drops nothing.

**I cannot find crude oil.**
Oil pockets only exist under deserts and oceans. Explore caves in those biomes. See [[rohstoffe|Resources and Ores]].

## For modpacks and servers

- Almost all numbers (consumption, time, throughput) are in data packs under `data/<namespace>/infinite_horizon/balance/` and can be changed with `/reload`.
- `serverconfig/infinite_horizon.json` lets you set, among other things, recipe amounts, oxygen consumption and events.
