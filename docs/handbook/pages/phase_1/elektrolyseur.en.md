---
title: Electrolyzer
---
The <item:infinite_horizon:electrolyzer> uses power to split water into **hydrogen** and **oxygen**. You will urgently need both later: hydrogen as fuel, oxygen for breathing on alien worlds.

@machine infinite_horizon:electrolyzer

@image ../images/screenshots/gui-electrolyzer.png | The electrolyzer window: one input tank for water, two output tanks for the gases.

## Building

@recipes_for infinite_horizon:electrolyzer

## What it does

@recipes_type electrolysis

## How to connect it

1. Put a <item:infinite_horizon:fluid_pump> above water, or put water buckets into the drain slot.
2. Lead the water with a <item:infinite_horizon:copper_pipe> to a side of the electrolyzer that is not an output side.
3. Each gas has its own **output side**: hydrogen at the front, oxygen on the right. The "Tank" button picks the gas, "Side" changes its side. Pressure bottles right at these sides collect the gases.

@note Gases fit neither in the normal tank nor in buckets. For now they collect in the electrolyzer itself. Pressure bottles and canisters come in a later phase.
