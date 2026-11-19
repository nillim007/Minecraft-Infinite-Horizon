---
title: The Flight
---
Once the [[rakete|rocket]] stands fuelled on the [[raumfahrt|launch pad]] and every point of the checklist is met, you are ready to go.

## Launching

1. Put on the complete [[raumanzug|pressure suit]] and take enough [[sauerstoff|oxygen canisters]].
2. Sit down in the capsule (right-click the capsule).
3. Press the **inventory key**. In the seat it opens the rocket computer of the control panel. You can also open the panel from outside.
4. When the checklist is complete, press **Launch**. The **navigation map** opens.
5. On the left are the destinations with their fuel cost there and back: **green** means there and back, **yellow** one way only, **grey** locked or too far. Choose a destination.
6. On the right you choose the landing site: a [[landefeuer|landing beacon]] of your agency on that body, or your own X and Z coordinates. If you leave both empty, you land at the destination's spawn (on Earth at world spawn, elsewhere near X/Z 0/0).
7. Press **Launch**. The rocket's blocks become a flying rocket with all passengers inside.

## The flight

- **Countdown:** {{value:spaceflight:rocket_flight.countdown_ticks|t}}. From now on you cannot get out.
- **Ascent:** {{value:spaceflight:rocket_flight.ascent_ticks|t}} straight up. The camera shows the rocket from outside.
- **Arrival:** {{value:spaceflight:rocket_flight.arrival_height}} blocks above the landing point (at most up to the build height).
- **Landing:** {{value:spaceflight:rocket_flight.landing_ticks|t}}, then the rocket stands on the highest solid block.

Press **Esc** to skip the flight: the rocket lands at the destination immediately.

The rocket searches for a landing spot in a spiral within {{value:spaceflight:landing_search.radius}} blocks: solid ground without liquid and {{value:spaceflight:landing_search.air_blocks_above}} blocks of air above.

@note If you log out during the flight, the rocket lands immediately. At your next login you are sitting in it again.

## After landing

The rocket stays as a vehicle.

- **Get out:** sneak.
- **Get in:** right-click while a seat is free.
- **Rocket GUI:** inventory key in the seat or right-click from outside. There you see the fuel, the cargo hold and the buttons **Launch** and **Refuel**. Refuel empties <item:infinite_horizon:transport_tank> from the cargo hold into the rocket tanks.
- **Dismantle:** sneak and right-click with an empty hand. All parts and the cargo drop as items, the fuel is lost.

## The return flight

A landed rocket launches without a pad: sit down, open the rocket GUI and press **Launch**. The cost depends on the gravity of the launch site; from the Moon the return flight is much cheaper than the outbound one.

@warning You reach yellow destinations only once. Without fuel in the cargo hold or a pump at the destination you are stuck there.

@tip Set up a [[landefeuer|landing beacon]] at your base on Earth before the first flight. Then the return flight lands you at home instead of world spawn.
