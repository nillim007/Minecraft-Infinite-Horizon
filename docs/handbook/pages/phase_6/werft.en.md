---
title: Orbital Shipyard
---
Your starship takes shape in the orbital shipyard. It needs huge amounts of material from every corner of the solar system, so you supply it with [[frachtraketen|cargo rockets]].

## Setup

1. Build a [[raumstation|station]] in Earth orbit.
2. Put the <item:infinite_horizon:shipyard_terminal> into the station, at most {{value:shipyard:shipyard_rules.station_core_radius}} blocks from the station core. It cannot be placed anywhere else.
3. Put at least one <item:infinite_horizon:shipyard_dock> into the same station. It is the landing target of your cargo rockets.

@warning Without a shipyard dock in the station, the shipyard cannot accept anything. The terminal then shows a red notice.

## The shipyard store

Whatever arrives at the dock moves straight into the shipyard store. The store is unlimited, but only takes what the shipyard still needs. Everything else stays in the rocket and flies back. Pipes and hoppers at the dock deliver too.

- Methane first fills the dock's fuel tank. Your rockets refuel from it for the return flight. Only the rest goes into the shipyard store.
- For the flight computer, the shipyard only accepts a star chart of your own agency.

@tip At the [[logistik_computer|logistics computer]], set up routes with the condition "until demand is met" to the dock. The rockets then only load what the shipyard still needs and stop by themselves.

@if infinite_horizon:trading_post
## Building together

The "Partners" button in the shipyard GUI releases the shipyard for your [[agentur|partners]]. Their cargo rockets may then supply the docks. Every agency that delivered at least one item or fluid also gets the advancement when the starship launches.

@endif
## The construction sections

The terminal shows all eight sections with progress bars. Hover over a bar to see what is still missing. The shipyard accepts deliveries for all open sections, the earliest first.

After the eighth section the "Launch" button appears at the terminal ([[sternenschiff|Launch and Starship]]).

Once a section is fully supplied, the shipyard builds it in {{value:shipyard:1.build_ticks|t}}. Sections are built in order, from 1 to 8, and the time keeps running even when you are not nearby. With every finished section the scaffold above the terminal grows.

| Section | Main materials |
| --- | --- |
| 1 Hull | lightweight, titanium and aluminium plates |
| 2 Shields | tungsten plates, heat shields |
| 3 Main drive | antimatter drives |
| 4 Power | fission reactor, lithium batteries, fuel rods |
| 5 Flight computer | iridium chips, platinum, star chart |
| 6 Life support | dome generator III, oxygen generators, greenhouse, clone chamber |
| 7 Cooling | liquid nitrogen, liquid helium, ammonia |
| 8 Maneuvering thrusters | methane, hydrogen, sulfur |

## Crafting

@recipes_for infinite_horizon:shipyard_terminal infinite_horizon:shipyard_dock
