---
title: Storing Oxygen
---
Oxygen is made in the <item:infinite_horizon:electrolyzer> or the [[sauerstoffgenerator|oxygen generator]]. Canisters and pressure bottles let you carry and store it.

## Oxygen canisters

| Canister | Contents |
| --- | --- |
| <item:infinite_horizon:oxygen_canister> | {{value:suits:infinite_horizon:oxygen_canister.capacity}} mB |
| <item:infinite_horizon:large_oxygen_canister> | {{value:suits:infinite_horizon:large_oxygen_canister.capacity}} mB |

- **Filling:** put a canister into the **fill slot** of a machine with an oxygen tank, such as the electrolyzer, oxygen generator or pressure bottle. It fills at once with as much as there is.
- **Emptying:** in the **drain slot** the canister empties its contents into the tank.
- **On the go:** canisters in your inventory refill your [[raumanzug|pressure suit]] by themselves.

The tooltip shows the contents. Canisters only take oxygen.

## The pressure bottle

The <item:infinite_horizon:pressure_bottle> is a tank for gases only and holds {{value:storage:pressure_bottle.capacity}} mB. Like the fluid tank it accepts and outputs on every side, never into other storage, and has fill and drain slots. When broken, it keeps its contents.

@machine infinite_horizon:pressure_bottle

@if infinite_horizon:transport_tank
## The transport tank

The <item:infinite_horizon:transport_tank> is a container for any fluid and holds {{value:storage:transport_tank.capacity}} mB, but only one fluid at a time. It works in all fill and drain slots, including the [[raumfahrt|fuel pump]]. That way you bring kerosene or oxygen to the launch pad without laying pipes.
@endif

## Crafting

@if infinite_horizon:research_terminal
@note The **Pressure suit** node unlocks the canisters, the **Gas storage** node the pressure bottle (both tier 1, [[forschung|Research]]).
@endif

@recipes_for infinite_horizon:oxygen_canister infinite_horizon:large_oxygen_canister infinite_horizon:pressure_bottle infinite_horizon:transport_tank

@tip A pressure bottle next to the oxygen generator collects its surplus. Then you always have a supply for new canisters.
