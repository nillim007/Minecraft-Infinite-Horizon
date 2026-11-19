---
title: Living Without Air
---
On every alien world, special rules apply outside an active [[kuppel|dome]], no matter what the atmosphere is like. On Earth, in the Nether and in the End everything stays as usual.

## No fire

- **Torches, fire, campfires, lanterns and candles** cannot be placed outside. Redstone torches keep working.
- Existing torches go out one by one and turn into <item:infinite_horizon:extinguished_torch>. Campfires and candles go out, lanterns and fire disappear.

The <item:infinite_horizon:electric_lamp> brings light. It shines at full strength everywhere, but needs power: {{value:machines:electric_lamp.energy_per_tick}} J/t. Without power it stays dark.

@machine infinite_horizon:electric_lamp

## No plants, no water

- **Plants** do not grow outside; bone meal does not help either.
- **Water** freezes into ice at once on the Moon, and that ice does not melt there.

## Beds

On alien worlds a bed sets your spawn point only **inside an active dome**. Outside, "You can't breathe here" appears and you cannot sleep.

If the dome fails while you are dead, your spawn point is no longer safe. You then appear at Earth's world spawn.

@warning Build your base so that bed and dome generator stand close together. A bed at the edge of the dome quickly ends up outside.

## Crafting

@if infinite_horizon:research_terminal
@note The **Dome I** node in tier 2 unlocks the electric lamp ([[forschung|Research]]).
@endif

@recipes_for infinite_horizon:electric_lamp

@tip Inside the dome, torches and campfires work quite normally. Electric lamps are worth it mostly outside and in caves.
