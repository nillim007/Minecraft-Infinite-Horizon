---
title: Space Weather and Events
---
Alien worlds have their own weather. Every {{value:events:event_planning.check_interval_ticks|t}} the server rolls for every world with players whether an event comes. It starts {{value:events:event_planning.schedule_ahead_ticks|t}} later, and {{value:events:event_planning.warning_ticks_without_station|t}} before that you get a warning in chat, a warning tone and a display in the HUD. The server sets how often events come with `event_frequency`.

@if infinite_horizon:space_weather_station
With a running [[wetterstation|space weather station]] of your agency in the same world, the warning comes {{value:events:event_planning.warning_ticks_with_station|t}} in advance.
@endif

## Solar storm

On every world except Earth. Radiation rises by {{value:events:infinite_horizon:solar_storm.effects.radiation_bonus}}. Machines without a roof of at least {{value:events:infinite_horizon:solar_storm.effects.machine_pause.exempt_cover_blocks}} blocks stop every {{value:events:infinite_horizon:solar_storm.effects.machine_pause.interval_ticks|t}} for {{value:events:infinite_horizon:solar_storm.effects.machine_pause.pause_ticks|t}}. In return, solar panels supply {{value:events:infinite_horizon:solar_storm.reward.solar_multiplier}} times as much.

## Meteor shower

On the Moon and Mars. Every {{value:events:infinite_horizon:meteor_shower.effects.impact_interval_ticks|t}} a meteorite strikes within {{value:events:infinite_horizon:meteor_shower.effects.impact_radius}} blocks of every player. It explodes without fire (the server decides whether blocks are destroyed) and leaves <item:infinite_horizon:meteorite_ore>. In the [[zerkleinerer|crusher]] that gives iron, nickel and with luck <item:infinite_horizon:raw_platinum> or <item:infinite_horizon:raw_iridium>.

@recipes_for infinite_horizon:raw_platinum infinite_horizon:raw_iridium

## Dust storm (Mars)

Instead of rain, dust storms sweep over Mars. Solar panels only supply {{value:events:infinite_horizon:dust_storm.effects.solar_multiplier}} times as much, monsters come twice as often, and dust wraiths appear that can lead you to exploration targets (see [[kreaturen|Creatures]]).

## Acid rain (Venus)

On Venus it rains acid. Anyone standing unprotected under the open sky loses durability on armor and tools in hand every {{value:events:infinite_horizon:acid_rain.effects.interval_ticks|t}}. The [[hitzeanzug|heat suit]] protects you.

@warning Do not leave valuable machines unprotected under the open sky on Mars and the Moon. A roof of three blocks protects them in a solar storm.

@tip A meteor shower is a good opportunity: collect the impacts while it lasts.
