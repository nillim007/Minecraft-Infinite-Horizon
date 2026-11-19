---
title: Oxygen Generator
---
The <item:infinite_horizon:oxygen_generator> makes oxygen from power alone. Unlike the electrolyzer it needs no water and therefore works everywhere, even on the dry Moon.

@machine infinite_horizon:oxygen_generator

## How to use it

1. Set up the generator and supply it with power.
2. Every {{value:machines:oxygen_generator.ticks|t}} it produces {{value:machines:oxygen_generator.process.output_per_cycle}} mB of oxygen into its internal buffer.
3. Through its **output side** it hands the oxygen to tanks, pressure bottles or pipes. You choose the side with the "Side" button.
4. An empty [[sauerstoff|oxygen canister]] in the **fill slot** is filled at once.

Like all machines it takes [[upgrades|upgrades]].

## Crafting

@if infinite_horizon:research_terminal
@note The **Dome I** node in tier 2 unlocks the oxygen generator ([[forschung|Research]]).
@endif

It needs a <item:infinite_horizon:pressure_hull>. The pressure hull is a component for everything under pressure.

@recipes_for infinite_horizon:oxygen_generator infinite_horizon:pressure_hull

@tip The electrolyzer supplies oxygen much more cheaply as long as you have water. On the Moon the generator is the simplest source.
