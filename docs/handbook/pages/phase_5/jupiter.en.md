---
title: Jupiter Orbit and Gas Collector
---
Jupiter has no surface you can land on. A rocket only takes you into **Jupiter orbit**, an empty world above the gas giant. From there you fly with the [[landefaehre|lander]] to the moons [[io|Io]] and Europa.

## Dangers in orbit

Radiation in Jupiter orbit is at level {{value:planet:jupiter_orbit.radiation}}. Every space suit needs a <item:infinite_horizon:radiation_shield> there. Whoever falls below the station does not land on the planet: they are brought back to the nearest [[raumstation|station core]] and take {{value:stations:orbit_fall_rules.gas_giant_orbits.damage}} damage. If there is no core yet, a small platform of station hull is created.

@warning Without a radiation shield, Jupiter orbit is deadly, even in a heat or pressure suit.

## The gas collector

The <item:infinite_horizon:gas_collector> only works in the orbits of the gas giants. It needs no ingredients: every {{value:planet:jupiter_orbit.gas_collector_output.ticks}} ticks it puts {{value:planet:jupiter_orbit.gas_collector_output.amount}} mB of gas into its tank, hydrogen in Jupiter orbit. Empty containers in the fill slot get filled, pipes draw the gas off.

@machine infinite_horizon:gas_collector

@recipes_for infinite_horizon:gas_collector

@tip A gas collector next to the station's tank depot supplies rockets and the fusion reactor with hydrogen without you having to bring water to the station.
