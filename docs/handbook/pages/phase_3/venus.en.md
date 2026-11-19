---
title: Venus
---
Venus is the hottest place in the inner solar system. Beneath a dense, toxic cloud cover lie lava lakes and volcanoes, but also tungsten and sulfur.

## The world

- **Gravity:** {{value:planet:venus.gravity}} times Earth's.
- **Temperature:** {{value:planet:venus.temperature.day}} °C day and night.
- **Air:** toxic and under high pressure.
- **Lava sea:** up to height {{value:planet:venus.sea_level}} everything is filled with lava.

## The cloud layer

Between y {{value:spaceflight:venus_aerostat.check_y_min}} and {{value:spaceflight:venus_aerostat.check_y_max}} lies the cloud layer. It is pleasantly warm there and the pressure is normal; only the air stays toxic. All rockets land here: if there is no ground at the landing site yet, the server builds a platform of <item:infinite_horizon:aerostat_platform> at y {{value:spaceflight:venus_aerostat.platform_y}}.

- **<item:infinite_horizon:aerostat_platform>:** a floating block. Place more next to it to enlarge your platform.
- **<item:infinite_horizon:cloud_elevator>:** put it on the platform and right-click. After {{value:spaceflight:cloud_elevator.delay_ticks|t}} you stand on the surface below, where the lower station is created automatically. If there is lava, a small platform of <item:infinite_horizon:venus_rock> is built. Right-clicking below takes you back up.

@warning Down below, the [[hitzeanzug|heat time]] of your suit runs. Without a heat suit you will not survive the surface.

## Terrain and biomes

- **<item:infinite_horizon:venus_lava_plains>:** basalt plains with lava lakes and sulfur.
- **<item:infinite_horizon:venus_volcanoes>:** volcanic cones of <item:infinite_horizon:venus_rock> and magma blocks, with tungsten in the rock. Volcanic vents lie on their flanks; they are exploration targets.
- **<item:infinite_horizon:venus_plateau>:** flat highlands.

## Ores

@recipes_for infinite_horizon:aerostat_platform infinite_horizon:cloud_elevator


@ores infinite_horizon:venus

<item:infinite_horizon:venus_tungsten_ore> needs an iron pickaxe. Sulfur ore gives several pieces of <item:infinite_horizon:sulfur> at once.

@recipes_for infinite_horizon:tungsten_dust

@tip Build your base in the cloud layer and only go down to mine.
