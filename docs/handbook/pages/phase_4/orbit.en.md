---
title: Orbit
---
Above Earth, Mars and Venus lies an orbit each. There is no ground there, only emptiness, the Sun and the planet far below you. Gravity is only {{value:planet:earth_orbit.gravity}} g: walking on blocks works normally, but whoever jumps drifts away.

## Arrival

A T2 rocket flies into orbit. Without a docking bay it hovers at height {{value:spaceflight:orbit_arrival.y}} at the chosen spot. If something is already in the way there, or a foreign station core is closer than {{value:spaceflight:orbit_arrival.min_distance_foreign_station_core}} blocks, the on-board computer looks for a free spot nearby. A launch from orbit costs no fuel for the ascent.

## Magnet boots

<item:infinite_horizon:magnet_boots> are a suit module for the boots. If a block is directly below you, {{value:suits:infinite_horizon:magnet_boots.gravity_g}} g applies. Jump off and you float again.

@recipes_for infinite_horizon:magnet_boots

## Falling

@warning If you fall below height {{value:stations:orbit_fall_rules.trigger_below_y}}, you plunge onto the planet below: onto Earth, Mars or into the cloud layer of Venus, at the same spot. A slow fall of {{value:misc:slow_fall.ticks|t}} catches you. The rocket and your station stay up there.

@tip First build a railing of hull blocks around the platform. Then you do not drift off so easily when jumping.
