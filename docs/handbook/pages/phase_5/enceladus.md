---
title: Enceladus
title_en: Enceladus
order: 130
milestone: 7
requires: ["infinite_horizon:enceladus_ice"]
icon: infinite_horizon:enceladus_snow
items: ["infinite_horizon:enceladus_snow", "infinite_horizon:enceladus_ice", "infinite_horizon:ammonia_geyser"]
summary: Ein Eismond mit Ammoniak-Geysiren
planet: infinite_horizon:enceladus
---
Enceladus ist ein kleiner, eisiger Saturnmond ohne Luft. Mit {{value:planet:enceladus.temperature.day}} °C ist er so kalt, dass du einen [[hitzeanzug|Anzug der Stufe 2]] brauchst. Die Schwerkraft ist mit {{value:planet:enceladus.gravity}} g fast null.

## Gebiete

- **Schneefelder** aus <item:infinite_horizon:enceladus_snow>.
- **Tigerstreifen** aus <item:infinite_horizon:enceladus_ice>, mit einem Ammoniak-Geysir je Chunk.

## Ammoniak-Geysire

Alle {{value:items:infinite_horizon:ammonia_geyser.eruption_interval_ticks|t}} bricht ein <item:infinite_horizon:ammonia_geyser> aus und schießt eine Fontäne {{value:items:infinite_horizon:ammonia_geyser.fountain_height}} Blöcke hoch. Wer in der Fontäne steht, wird mit {{value:items:infinite_horizon:ammonia_geyser.entity_velocity_up}} Blöcken je Tick nach oben gerissen. Geysire lassen sich nicht abbauen.

@tip Bei der geringen Schwerkraft schwebst du nach einem Geysirsprung lange. So kommst du schnell auf hohe Eisrücken.

@warning Bau nicht direkt über einem Geysir. Die Fontäne hebt alles an, was darüber steht.

## Die Geysirmotte

Um die Geysire kreist die **Geysirmotte**. Sie ist harmlos; wo du sie siehst, ist ein Geysir nicht weit.
