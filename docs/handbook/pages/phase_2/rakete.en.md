---
title: The Rocket
---
You build a rocket block by block on the [[raumfahrt|launch pad]]. Every part is one block in size.

## Structure

From bottom to top above the centre of the platform:

1. <item:infinite_horizon:rocket_engine_t1> right on the centre.
2. {{value:rocket_parts:rules.1.fins}} <item:infinite_horizon:rocket_fin_t1> horizontally next to the engine, one each to the north, east, south and west.
3. {{value:rocket_parts:rules.1.tanks.0}} to {{value:rocket_parts:rules.1.tanks.1}} <item:infinite_horizon:rocket_tank_t1> stacked on top of each other.
4. If you like, a <item:infinite_horizon:rocket_cargo_bay> for cargo ({{value:rocket_parts:parts.rocket_cargo_bay.cargo_slots}} slots, right-click opens it).
5. At the very top the <item:infinite_horizon:rocket_capsule_t1>. Right-clicking it seats you inside, sneaking lets you get out.

## Mass and thrust

| Part | Mass |
| --- | --- |
| Capsule | {{value:rocket_parts:parts.rocket_capsule_t1.mass}} t |
| Engine | {{value:rocket_parts:parts.rocket_engine_t1.mass}} t |
| Fin | {{value:rocket_parts:parts.rocket_fin_t1.mass}} t |
| Tank, empty | {{value:rocket_parts:parts.rocket_tank_t1.mass}} t |
| Tank, full of fuel | + {{value:rocket_parts:parts.rocket_tank_t1.fuel_mass_full}} t |
| Cargo bay | {{value:rocket_parts:parts.rocket_cargo_bay.mass}} t + {{value:rocket_parts:parts.rocket_cargo_bay.mass_per_slot}} t per occupied slot |

The T1 engine has a thrust of {{value:rocket_parts:parts.rocket_engine_t1.thrust}}. The rocket may only launch if **thrust / (mass × gravity)** is at least {{value:spaceflight:thrust_to_weight.min_ratio}}. Many full tanks plus a full cargo bay are too heavy: the rocket computer shows the ratio.

## Fuel

Every tank holds {{value:rocket_parts:parts.rocket_tank_t1.fuel_capacity}} mB of kerosene **and** {{value:rocket_parts:parts.rocket_tank_t1.fuel_capacity}} mB of oxygen. A flight costs the same amount of both:

- **Launch:** {{value:spaceflight:launch_cost.per_g}} mB times the gravity of the launch site,
- **Transfer:** {{value:spaceflight:transfer_cost.per_dp}} mB per distance point of difference, plus {{value:spaceflight:transfer_cost.different_body}} mB if the destination is another body,
- **Landing:** {{value:spaceflight:landing_cost.factor|%}} of the launch cost at the destination (nothing on Earth).

The rocket computer in the control panel works this out for every destination: **green** means there and back, **yellow** one way only, **grey** unreachable or no probe yet.

## The checklist

The control panel lists these points, which all need a check mark:

1. **Structure valid:** pad and rocket are built correctly (e.g. all fins at the engine).
2. **Thrust ≥ weight.**
3. **Fuel for a destination.**
4. **Destination unlocked:** Earth and every destination a probe of your agency has reached.
5. **Crew:** a player in a complete [[raumanzug|pressure suit]] sits in the capsule.

How launch, flight and landing work is explained under [[flug|The Flight]].

@warning Plan the return flight: you can reach yellow destinations, but you will not get back without refuelling. Take fuel in the cargo bay or build a pump at the destination.

## Crafting

@recipes_for infinite_horizon:rocket_capsule_t1 infinite_horizon:rocket_tank_t1 infinite_horizon:rocket_engine_t1 infinite_horizon:rocket_fin_t1 infinite_horizon:rocket_cargo_bay

@tip For the Moon even the smallest rocket with full tanks is enough for the round trip. More tanks bring reserves, but also weight.
