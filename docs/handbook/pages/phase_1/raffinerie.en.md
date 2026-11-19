---
title: Refinery
---
The refinery is your first **multiblock**: a tower of 45 blocks that turns crude oil into **kerosene** and **plastic**. Kerosene is fuel, and plastic goes into every circuit.

@image ../images/refinery.svg | Blueprint: the controller (orange) sits at the bottom centre of the front, ports (blue, yellow) replace any casing blocks.

@machine infinite_horizon:refinery_controller

@image ../images/screenshots/gui-refinery.png | The refinery window: crude oil tank on the left, plastic in the middle, kerosene tank on the right.

## What it does

@recipes_type refining

## The structure

The refinery is **3 blocks wide, 3 blocks deep and 5 blocks tall**, completely solid.

1. Place the controller (<item:infinite_horizon:refinery_controller>) on the ground. Its front faces you.
2. The controller is the **middle of the bottom row on the front**. The tower extends one block left and right of it, two blocks back and four blocks up.
3. Fill all other 44 spots with <item:infinite_horizon:refinery_casing>.
4. Replace single casing blocks with **ports** where pipes and cables should connect (see below).
5. As soon as everything is right, the status lamp turns from red to yellow.

@tip **Sneak-right-click** the controller with an empty hand to see, for {{value:misc:multiblock_rules.ghost_projection_ticks|t}}, red particles where blocks are missing or wrong.

@image ../images/screenshots/refinery-ghost.png | A finished refinery tower seen from below. If it is incomplete, red particles show the missing spots.

## Ports

Ports replace casing blocks and pass things on to the controller. Right-clicking with an empty hand switches between **input** and **output**.

- <item:infinite_horizon:fluid_port> – crude oil in (input), kerosene out (output).
- <item:infinite_horizon:item_port> – plastic out (output).
- <item:infinite_horizon:energy_port> – power in.

@recipes_for infinite_horizon:refinery_controller infinite_horizon:refinery_casing infinite_horizon:fluid_port infinite_horizon:item_port infinite_horizon:energy_port

## Materials needed

For 44 casing blocks you need 11 assembly runs, that is 22 iron plates and 11 copper pipes. Every port saves you one casing block.

@warning Other blocks are not allowed in the structure. Even a single wrong block makes the lamp glow red. Hover over the lamp in the window: it shows the first missing block with its coordinates.
