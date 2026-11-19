---
title: Mercury
---
Mercury turns so slowly that one side always glows and the other always freezes. Where you land depends on the X coordinate:

- **Scorched plains** (day side, X above 2,000): {{value:planet:mercury.zones.0.temperature}} °C, with <item:infinite_horizon:mercury_titanium_ore>.
- **Frost plains** (night side, X below −2,000): {{value:planet:mercury.zones.1.temperature}} °C, with <item:infinite_horizon:mercury_iron_ore>.
- **Twilight belt** in between: a pleasant {{value:planet:mercury.zones.2.temperature}} °C. Here <item:infinite_horizon:mercury_lithium_salt> lies as a crust at the surface.
- **Shadow craters**: rare, {{value:planet:mercury.zones.3.temperature}} °C, with ice in the rock.

Gravity is {{value:planet:mercury.gravity}} g, radiation level {{value:planet:mercury.radiation}}.

@ores infinite_horizon:mercury

## Equipment

On the day side only the [[hitzeanzug|heat suit]] helps. The suit alone does not protect against the radiation: install the <item:infinite_horizon:radiation_shield> in the helmet; it keeps out radiation up to level {{value:suits:infinite_horizon:radiation_shield.radiation_max}}. The <item:infinite_horizon:heat_reflector> in the chestplate extends the heat time on Venus by {{value:suits:infinite_horizon:heat_reflector.venus_heat_timer_multiplier}} times.

@recipes_for infinite_horizon:radiation_shield infinite_horizon:heat_reflector

## The mirror beetle

The mirror beetle crawls around the twilight belt. It only attacks if you attack it. Its shell reflects the sunlight: whoever looks at it in daylight from less than {{value:creatures:infinite_horizon:mirror_beetle.behavior_params.gaze_distance}} blocks is blinded for {{value:creatures:infinite_horizon:mirror_beetle.behavior_params.blindness_ticks|t}}. It drops a <item:infinite_horizon:mirror_shell>, which makes the heat reflector.

@warning Do not land on the day side without a heat suit. The twilight belt around X 0 is the safe landing site.

@tip Do not look straight at the mirror beetle; approach it from the side. One hit from close range is usually enough.
