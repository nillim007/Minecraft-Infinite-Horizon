---
title: Europa
---
Europa is wrapped in a thick ice shell. Beneath it lies an ocean with warm vents bubbling on its floor. Gravity is {{value:planet:europa.gravity}} g, the surface is {{value:planet:europa.temperature.day}} °C cold, and Jupiter's radiation is at level {{value:planet:europa.radiation}}.

## Layout

- **Ice shell** of <item:infinite_horizon:europa_ice> between y {{value:planet:europa.features.0.params.y_min}} and {{value:planet:europa.features.0.params.y_max}}. It splits open in the **ice rifts**; there you reach the water directly.
- **Ocean** below. Under the ice shell the water is {{value:planet:europa.zones.0.temperature}} °C warm, the pressure normal and the radiation shielded.
- **Sea floor** of <item:infinite_horizon:europa_seabed>.

## Equipment

- At the surface: a space suit with a <item:infinite_horizon:radiation_shield>.
- In the ocean from y {{value:planet:europa.zones.1.y_max}} downwards there is deep-sea pressure. For that you need the <item:infinite_horizon:diving_module> on the leggings. It also makes you swim faster (×{{value:suits:infinite_horizon:diving_module.swim_speed_multiplier}}).

@recipes_for infinite_horizon:diving_module

@warning Without a dive module the deep-sea pressure crushes you. Stay above the limit until you have one.

## Through the ice

Europa ice is as hard as obsidian. With a pickaxe a shaft through the ice shell takes a very long time. The <item:infinite_horizon:ice_drill> is faster: hold the right mouse button, and it drills a shaft of {{value:items:infinite_horizon:ice_drill.shaft.width}} × {{value:items:infinite_horizon:ice_drill.shaft.width}} blocks downwards below you, one layer every {{value:items:infinite_horizon:ice_drill.ticks_per_layer|t}}. Each block costs {{value:items:infinite_horizon:ice_drill.energy_per_block}} J from the battery (full: {{value:items:infinite_horizon:ice_drill.energy_capacity}} J). Charge it in the charging slot of a battery.

@recipes_for infinite_horizon:ice_drill

@tip Only drill where you can get out again. A staircase of blocks or a jetpack brings you back up onto the ice shell.

## Hydrothermal vents

On the sea floor lie **vent fields**: groups of <item:infinite_horizon:hydrothermal_vent> with bubbles rising from them. A vent field is an exploration target. Right-clicking a vent with an empty glass bottle gives a <item:infinite_horizon:microbe_culture>. Afterwards that vent rests for {{value:items:infinite_horizon:hydrothermal_vent.cooldown_ticks|t}}.

@uses infinite_horizon:microbe_culture

## Life in the ocean

- The **ice jelly** glows and drifts slowly towards the nearest vent. Follow it when you are looking for a vent field.
- A **reef keeper** guards every vent field. It is peaceful until someone near it (radius {{value:creatures:infinite_horizon:reef_keeper.behavior_params.aggro_radius}} blocks) mines blocks or takes samples. Then it attacks ({{value:creatures:infinite_horizon:reef_keeper.damage}} damage).

@tip Give the reef keeper a space ration (right-click). It then stays peaceful towards you for {{value:creatures:infinite_horizon:reef_keeper.behavior_params.pacify_ticks|t}}, and you can take samples in peace.
