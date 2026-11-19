---
title: Logistics Computer
---
At the <item:infinite_horizon:logistics_computer> you set up routes for your [[frachtraketen|cargo rockets]]. The routes belong to your agency: every logistics computer shows the same ones, no matter where it stands.

## Setting up a route

1. Set up at least two [[aussenposten|cargo terminals]] and put a cargo rocket into a hangar slot of the start terminal.
2. Click "New" at the logistics computer.
3. Choose start and destination with the buttons. The start is always the terminal whose hangar holds the rocket.
4. Choose the launch condition and enter N (and X if needed).
5. Enter the cargo: item or fluid IDs separated by commas. The "Hand" button adds the item in your hand. Empty means: everything.
6. Under "Rockets", choose the hangar slots whose rockets fly this route. A rocket always belongs to only one route.
7. Click "Save".

@if infinite_horizon:trading_post
## Destinations at partners

Your [[handelsposten|trading posts]] are also available as destinations and, prefixed with "Partner", the released terminals, shipyards and trading posts of your [[agentur|partners]]. If the partnership ends or the partner withdraws the release, the rockets of that route no longer launch. A rocket already on its way brings its cargo back.

@endif
## Launch conditions

| Condition | The rocket launches … |
| --- | --- |
| when full | as soon as matching cargo fills all holds (all item slots or all fluid holds) |
| every N ticks | at the latest N ticks after the last launch |
| when target has less than N of X | as soon as the destination stores less than N pieces (or mB) of X |
| until demand is met | as long as the [[werft|shipyard]] still needs some of the cargo; it only loads what is missing |

Without matching cargo in the start terminal, no route launches.

## The status list

Below the editor you see every rocket of the route: in flight (with time left), loaded (outbound with cargo), waiting or out of fuel. A rocket out of fuel tries again every {{value:logistics:cargo_flight_rules.stranded_retry_ticks|t}}.

@warning A red line warns you if the outbound and return flights together need more fuel than fits into the tank, and no methane is stored at the destination. The rocket then stays stuck at the destination.

@tip If an outpost is not loaded right now, the logistics computer only checks it every now and then. In doing so it calculates what the producers extracted in the meantime.

## Crafting

@recipes_for infinite_horizon:logistics_computer
