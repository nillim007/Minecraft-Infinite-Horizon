---
title: Assembly Table
---
The <item:infinite_horizon:assembly_table> is the heart of your workshop. Almost everything after the first phase is made here: circuits, machines, rocket parts, space suits.

The assembly table has **9 input slots** without a fixed pattern, a fluid tank and 3 output slots.

@machine infinite_horizon:assembly_table

@image ../images/screenshots/gui-assembly-table.png | The assembly table window: 3 × 3 input slots without a fixed pattern, outputs, bucket slots and tank on the right.

## Building

@recipes_for infinite_horizon:assembly_table

## Measure exactly

The assembly table is picky: a recipe only starts if the slots hold **exactly** the ingredients in **exactly** the right amount. If a slot holds a little too much or something else, nothing happens.

- One ingredient may occupy several slots. The amounts are added up.
- Exactly the amount from the recipe is used.
- Example: 4 <item:infinite_horizon:iron_plate> and 1 <item:infinite_horizon:copper_pipe> give a <item:infinite_horizon:fluid_port>. With 2 plates and 1 pipe you get <item:infinite_horizon:refinery_casing> instead.

## The circuit

The <item:infinite_horizon:circuit> goes into almost every machine from phase 2 on. Set up a small production for it early.

@recipes_for infinite_horizon:circuit

## All assembly table recipes

@recipes_type assembly

@tip If the arrow does not start although everything seems to be there: take everything out and put the ingredients back in exactly the recipe amounts.
