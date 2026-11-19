---
title: Outposts and Cargo Terminal
---
An outpost is a <item:infinite_horizon:cargo_terminal> with all producers within {{value:logistics:outpost.radius}} blocks. Producers are the [[bohrturm|drilling rig and pump station]], the gas collector and the magnetic trap. They hand their yield straight to the terminal; you do not need to connect anything.

## The cargo terminal

The terminal has {{value:machines:infinite_horizon:cargo_terminal.item_slots.storage}} storage slots. Use the mouse wheel or the bar next to the storage to scroll through all rows. The tanks are on the right: the fuel tank on the far left, the cargo tanks next to it.

- The fuel tank only takes methane. Incoming methane always fills it first; only the rest goes into the cargo tanks.
- Clicking a tank bar selects the tank from which the fill slot fills empty containers.
- Hoppers and pipes may put items in and take them out. Through its output side the terminal only hands out cargo, never the fuel.

@tip If a producer belongs to two terminals, it always delivers to the nearer one.

@if infinite_horizon:trading_post
The "Partners" button in the terminal releases it for your [[agentur|partners]]. Their cargo rockets may then deliver here. They cannot take anything away.
@endif

## Production while you are away

If the outpost is not loaded because you are on another planet, the game calculates the extraction as soon as you come back. You then get {{value:logistics:offline_production.factor}} times the normal amount for the time that passed.

- Only a producer that was running when you left counts. Power is not calculated, so make sure the supply is safe beforehand, for example with the [[kernreaktor|fission reactor]].
- Extraction stops when the terminal is full or inputs are missing, such as empty antimatter cells in the magnetic trap.

@warning On servers, offline production can be switched off (setting `outposts_offline`).

## Crafting

@recipes_for infinite_horizon:cargo_terminal
