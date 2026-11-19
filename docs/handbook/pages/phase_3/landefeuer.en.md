---
title: The Landing Beacon
---
The <item:infinite_horizon:landing_beacon> marks a landing site. On the navigation map ([[flug|The Flight]]) it appears as a landing site for all members of your agency.

## Placing and naming

1. Place the landing beacon where your rockets should land. It belongs to the agency of whoever places it.
2. **Right-click** opens a name field (up to {{value:spaceflight:landing_beacon.name_max_length}} characters). Without its own name it takes the name of the item renamed at an anvil, or "Landing beacon X/Z".
3. Under the open sky, a red beam up to height {{value:spaceflight:landing_beacon.beam_top_y}} shows where it stands.

The navigation map knows all landing beacons of your agency, even on bodies where nobody is right now. If you break it, it disappears from the list.

## Landing

If you choose a landing beacon as the landing site, the rocket lands at most {{value:spaceflight:rocket_flight.beacon_landing_radius}} blocks away from it, but never on the beacon itself. If there is no room, it searches the wider area as usual.

@tip Leave some space on flat, solid ground around the landing beacon. Then the rocket stands right next to it after landing.

## Crafting

@recipes_for infinite_horizon:landing_beacon
