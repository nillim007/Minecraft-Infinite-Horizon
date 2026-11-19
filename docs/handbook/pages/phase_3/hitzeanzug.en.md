---
title: The Heat Suit
---
The **heat suit** is the second suit tier. You build it from your [[raumanzug|pressure suit]]: every piece is upgraded in the assembly table and keeps its oxygen and modules.

## Protection

- **Temperature:** from {{value:suits:heat_suit.temp_min}} °C to {{value:suits:heat_suit.temp_max}} °C.
- **Pressure:** high pressure as on Venus as well.
- **Oxygen:** from the chestplate as with the pressure suit, using {{value:suits:heat_suit.oxygen_consumption.amount}} mB every {{value:suits:heat_suit.oxygen_consumption.per_ticks|t}}.

## The heat time

On the surface of Venus, below the cloud layer (y {{value:suits:venus_heat_timer.applies_below_y}}), even the heat suit does not last forever. A separate bar in the HUD shows the **heat time**: {{value:suits:venus_heat_timer.duration_ticks|t}}. Once it has run out, the suit no longer protects you from heat.

Above the cloud layer the heat time refills in {{value:suits:venus_heat_timer.refill_ticks|t}}. So take the [[venus|cloud elevator]] up in good time.

@warning The heat time runs even if you only want to go down briefly. Check the bar before every descent.

## Crafting

@recipes_for infinite_horizon:heat_suit_helmet infinite_horizon:heat_suit_chestplate infinite_horizon:heat_suit_leggings infinite_horizon:heat_suit_boots

## Tier 2 suit modules

You install modules in the assembly table: put the suit piece and the module in together. Every piece has {{value:suits:suit_rules.module_slots_per_piece}} module slots, and modules fit every suit tier.

- <item:infinite_horizon:cooling_module> (chestplate): upper temperature limit + {{value:suits:infinite_horizon:cooling_module.temp_max_bonus}} °C.
- <item:infinite_horizon:armor_plating> (leggings or boots): + {{value:suits:infinite_horizon:armor_plating.armor_bonus}} armor.

@recipes_for infinite_horizon:cooling_module infinite_horizon:armor_plating infinite_horizon:nickel_plate

@tip The <item:infinite_horizon:cooling_system> in the chestplate needs liquid helium, see [[helium|Helium and Cryogenics]].
