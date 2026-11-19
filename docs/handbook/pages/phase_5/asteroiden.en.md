---
title: The Asteroid Belt
---
In the asteroid belt there is no ground, only boulders in empty space. Gravity is {{value:planet:asteroid_belt.gravity}} g: a jump carries you far, and whoever misses drifts away.

## Asteroids

The small asteroids have a radius of {{value:planet:asteroid_belt.features.0.params.radius.0}} to {{value:planet:asteroid_belt.features.0.params.radius.1}} blocks, and there are big ones with {{value:planet:asteroid_belt.structures.0.params.radius.0}} to {{value:planet:asteroid_belt.structures.0.params.radius.1}} blocks. They consist of <item:infinite_horizon:asteroid_rock> with ores inside:

- <item:infinite_horizon:asteroid_platinum_ore> ({{value:planet:asteroid_belt.ores.0.fraction|%}} of the blocks),
- <item:infinite_horizon:asteroid_iridium_ore> ({{value:planet:asteroid_belt.ores.1.fraction|%}}),
- <item:infinite_horizon:asteroid_nickel_ore> ({{value:planet:asteroid_belt.ores.2.fraction|%}}).

At spawn at X/Z 0/0 there is always a big asteroid with all three ores.

@warning If you fall below height {{value:planet:asteroid_belt.special.void_reset.below_y}}, navigation brings you back to the spawn asteroid with slow falling. Whatever you drop on the way is lost.

## Platinum and iridium

You process platinum and iridium like other ores: crusher and electric furnace. The <item:infinite_horizon:iridium_chip> is made in the [[fertigung_im_orbit|zero-g fabricator]] and goes into many devices of the fifth phase.

@recipes_for infinite_horizon:platinum_ingot infinite_horizon:iridium_ingot infinite_horizon:iridium_chip

## Asteroid anchor

The <item:infinite_horizon:asteroid_anchor> is placed on an asteroid. Within {{value:items:infinite_horizon:asteroid_anchor.radius}} blocks, {{value:items:infinite_horizon:asteroid_anchor.gravity_g}} g then applies to players: that is how you build a base without floating away.

@recipes_for infinite_horizon:asteroid_anchor

## Drift mites

Swarms of drift mites float around asteroids with platinum ore. They are harmless and show you where platinum is. They only spawn with the world and do not come back.

@tip Follow a drift mite swarm: it gives away the asteroid with platinum before you even mine it.
