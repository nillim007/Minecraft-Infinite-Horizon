---
title: Der Orbit
title_en: Orbit
order: 20
milestone: 6
requires: ["infinite_horizon:station_core"]
icon: infinite_horizon:magnet_boots
items: ["infinite_horizon:magnet_boots"]
summary: Schwerelosigkeit, Ankunft und Absturz
---
Über der Erde, dem Mars und der Venus liegt je ein Orbit. Dort gibt es keinen Boden, nur Leere, Sonne und den Planeten tief unter dir. Die Schwerkraft beträgt nur {{value:planet:earth_orbit.gravity}} g: Laufen auf Blöcken geht normal, aber wer springt, treibt davon.

## Ankunft

Eine Rakete T2 fliegt in den Orbit. Ohne Andockbucht schwebt sie auf Höhe {{value:spaceflight:orbit_arrival.y}} an der gewählten Stelle. Liegt dort schon etwas im Weg oder ist ein fremder Stationskern näher als {{value:spaceflight:orbit_arrival.min_distance_foreign_station_core}} Blöcke, sucht der Bordcomputer eine freie Stelle in der Nähe. Ein Start aus dem Orbit kostet keinen Treibstoff für den Aufstieg.

## Magnetstiefel

<item:infinite_horizon:magnet_boots> sind ein Anzugmodul für die Stiefel. Steht ein Block direkt unter dir, gilt {{value:suits:infinite_horizon:magnet_boots.gravity_g}} g. Springst du ab, schwebst du wieder.

@recipes_for infinite_horizon:magnet_boots

## Absturz

@warning Fällst du unter Höhe {{value:stations:orbit_fall_rules.trigger_below_y}}, stürzt du auf den Planeten darunter: auf die Erde, den Mars oder in die Wolkenschicht der Venus, an derselben Stelle. Ein Langsamer Fall von {{value:misc:slow_fall.ticks|t}} fängt dich ab. Die Rakete und deine Station bleiben oben.

@tip Bau zuerst ein Geländer aus Hüllblöcken um die Plattform. Dann treibst du beim Springen nicht so leicht ab.
