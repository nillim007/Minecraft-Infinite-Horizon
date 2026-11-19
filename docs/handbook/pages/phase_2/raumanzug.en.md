---
title: The Pressure Suit
---
Without a suit you survive on the Moon only for seconds. The **pressure suit** is the first tier and protects you from vacuum, heat and cold as long as it has oxygen.

## Four pieces, one suit

The suit only works if you wear **all four pieces**: <item:infinite_horizon:pressure_suit_helmet>, <item:infinite_horizon:pressure_suit_chestplate>, <item:infinite_horizon:pressure_suit_leggings> and <item:infinite_horizon:pressure_suit_boots>. If one piece is missing, you are unprotected.

| Protection | Pressure suit |
| --- | --- |
| Temperature | {{value:suits:pressure_suit.temp_min}} to {{value:suits:pressure_suit.temp_max}} °C |
| Pressure | vacuum to normal |
| Radiation | up to level {{value:suits:pressure_suit.radiation_max}} |
| Armor | like iron armor |

You repair the suit at an anvil with <item:infinite_horizon:aluminum_plate>.

## The oxygen supply

The chestplate holds up to {{value:suits:oxygen_supply.chestplate_capacity}} mB of oxygen. A new chestplate is empty. Its tooltip shows how much is left and, as soon as you are outside, a bar in the HUD.

- **Use:** {{value:suits:pressure_suit.oxygen_consumption.amount}} mB every {{value:suits:pressure_suit.oxygen_consumption.per_ticks|t}}, but only outside breathable air.
- **Refilling:** if you wear the chestplate and carry an [[sauerstoff|oxygen canister]], the supply refills by itself from the first canister.
- **Under water** the suit keeps your air full as long as it has oxygen.

@warning When the oxygen is used up, the suit still protects you from pressure and temperature, but you suffocate. Keep an eye on the display.

@if infinite_horizon:helmet_lamp
## Silence in the vacuum

In a vacuum without a dome you hear your breathing and faint radio static. All other sounds are muffled to only {{value:misc:vacuum_sound.volume_multiplier|%}} of their volume: footsteps, machines, creatures. The suit's warning tones stay loud.

@tip If it suddenly gets loud around you, you have stepped into a dome. There you breathe freely, and the suit refills.

## Modules

Every suit piece has {{value:suits:suit_rules.module_slots_per_piece}} module slots. Modules are installed and removed in the <item:infinite_horizon:assembly_table>, which needs power for this like for any other recipe:

1. **Install:** put the suit piece and the matching module alone into the inputs. Out comes the piece with the module installed.
2. **Remove:** put the suit piece in alone. Out come the piece without modules and all modules in the secondary outputs.

Installed modules are listed in the piece's tooltip and stay when you upgrade to a higher suit tier.

**Helmet lamp:** the <item:infinite_horizon:helmet_lamp> goes into the helmet. As long as you wear it, there is light at your head with light level {{value:items:infinite_horizon:helmet_lamp.light_level}}, even under water. Take the helmet off and the light is gone.

@recipes_for infinite_horizon:helmet_lamp

@endif
## Crafting

@if infinite_horizon:research_terminal
@note Your agency can only craft the suit pieces once it has researched the **Pressure suit** node (tier 1, [[forschung|Research]]).
@endif

@recipes_for infinite_horizon:pressure_suit_helmet infinite_horizon:pressure_suit_chestplate infinite_horizon:pressure_suit_leggings infinite_horizon:pressure_suit_boots

@tip Always take one full canister more than you think you need. Do the maths: {{value:suits:infinite_horizon:oxygen_canister.capacity}} mB in one canister, {{value:suits:pressure_suit.oxygen_consumption.amount}} mB used every {{value:suits:pressure_suit.oxygen_consumption.per_ticks|t}}.
