---
title: Environment, Strain and Gravity
---
On Earth you never have to think about air, pressure or temperature. On other worlds you do: every world has its own **environment values**, and every hazard you are not protected against builds up **strain**.

## The environment values

Every {{value:environment:environment_update.interval_ticks|t}} the server checks where you stand and works out:

- **Breathable air:** Is there a breathable atmosphere? Not on the Moon.
- **Temperature:** It follows daylight. On the Moon it rises to {{value:planet:moon.temperature.day}} °C at noon and drops to {{value:planet:moon.temperature.night}} °C at night. At dusk and dawn it lies in between.
- **Pressure:** from vacuum through low and normal to high.
- **Radiation:** level {{value:environment:radiation_rules.min}} to {{value:environment:radiation_rules.max}}. With at least {{value:environment:radiation_rules.cover_blocks}} solid blocks above you, it drops by {{value:environment:radiation_rules.cover_reduction}} levels.

Some areas have their own values. In the Moon's polar crater it always stays at {{value:planet:moon.biome_temperature_overrides.moon_polar_crater}} °C, and there is no solar power there.

Dimensions without their own environment values, such as the Nether, the End or worlds from other mods, count as Earth.

## What you can bear without a suit

| Hazard | Safe without a suit |
| --- | --- |
| Temperature | {{value:environment:no_suit_tolerance.temp_min}} to {{value:environment:no_suit_tolerance.temp_max}} °C |
| Pressure | low to normal |
| Radiation | up to level {{value:environment:no_suit_tolerance.radiation_max}} |
| Breathable air | only in breathable air |

## Strain

Every hazard has its own strain. While you are unprotected, it fills from 0 to 100 % in {{value:environment:oxygen.buildup_ticks|t}}. Only at 100 % do you take damage, and armor does not help. Without oxygen or in a vacuum, that costs {{value:environment:oxygen.damage}} health point every {{value:environment:oxygen.damage_interval|t}}.

Once the hazard is gone, the strain drops back to 0 in {{value:environment:oxygen.decay_ticks|t}}. Several hazards count separately: without a suit on the Moon, oxygen and pressure rise at the same time, plus heat or cold depending on the time of day.

In creative and spectator mode there is no strain. After death it starts at 0 again.

@warning Strain gives you only a short warning: after {{value:environment:oxygen.buildup_ticks|t}} in a vacuum the damage begins. Never go outside without a plan.

## The HUD display

Above the hunger bar, each hazard gets its own bar as soon as its strain rises. It is green, turns yellow from {{value:environment:hud.yellow_from|%}} and red from {{value:environment:hud.red_from|%}}. A warning tone sounds when it appears.

If you wear a complete [[raumanzug|pressure suit]] outside breathable air, another bar shows its oxygen supply. If temperature or radiation lie outside what you can bear without a suit, their values are shown above.

## Gravity

Every world has its own gravity. On the Moon it is {{value:planet:moon.gravity}} times Earth's. You fall more slowly, jump much higher and survive deep falls without damage: the safe fall height ({{value:misc:gravity.safe_fall_distance_numerator}} blocks on Earth) is divided by the gravity, and fall damage shrinks by the same amount.

The jump strength itself stays the same. That is why you jump many times higher on the Moon than on Earth.

@tip Radiation can be shielded: just {{value:environment:radiation_rules.cover_blocks}} solid blocks overhead lower it by {{value:environment:radiation_rules.cover_reduction}} levels. On alien worlds, a cave or a roof is often the quickest cover.
