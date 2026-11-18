---
title: Bohrturm und Pumpstation
title_en: Drilling Rig and Pump Station
order: 30
milestone: 8
requires: ["infinite_horizon:drilling_rig"]
icon: infinite_horizon:drilling_rig
items: ["infinite_horizon:drilling_rig", "infinite_horizon:pump_station", "infinite_horizon:pontoon", "infinite_horizon:ammonia", "infinite_horizon:nitrogen", "infinite_horizon:liquid_nitrogen"]
summary: Rohstoffe fördern, ohne die Welt abzubauen
---
Bohrturm und Pumpstation sind die Produzenten deiner Außenposten. Sie fördern ohne Ende, ohne die Welt zu verändern.

## Der Bohrturm

Der <item:infinite_horizon:drilling_rig> zählt beim Aufstellen alle Erzblöcke im Chunk unter sich, bis hinunter zum Grund der Welt. Danach erzeugt er alle {{value:machines:drilling_rig.ticks|t}} ein Erz, zufällig gewichtet nach diesen Anzahlen: Liegt unter ihm viel Eisen und wenig Titan, kommt meist Eisen heraus. Die Lagerstätte erschöpft sich nie.

@machine infinite_horizon:drilling_rig

@warning Auf der Erde arbeitet der Bohrturm nicht. Findet er keine Erze unter sich, leuchtet seine Lampe rot.

@tip Stell den Bohrturm über ein Gebiet mit den Erzen, die du brauchst, zum Beispiel ins dunkle Hochland auf Pluto für Uran.

## Die Pumpstation

Die <item:infinite_horizon:pump_station> fördert aus dem Block direkt unter sich, alle {{value:machines:pump_station.ticks|t}} {{value:machines:pump_station.process.output_per_cycle}} mB. Die Quelle bleibt erhalten.

| Unter der Pumpstation | Fluid |
| --- | --- |
| <item:infinite_horizon:ammonia_geyser> (Enceladus) | <item:infinite_horizon:ammonia> |
| <item:infinite_horizon:nitrogen_geyser> (Triton) | <item:infinite_horizon:nitrogen> |
| Quellblock aus Wasser, Lava, Erdöl oder Methan | dasselbe Fluid |

Im Methanregen auf Titan fördert sie {{value:events:infinite_horizon:methane_rain.reward.pump_station_multiplier}}-mal so viel.

@machine infinite_horizon:pump_station

## Der Ponton

Auf einem See kannst du die Pumpstation nicht direkt aufstellen. Setz zuerst einen <item:infinite_horizon:pontoon> auf das Fluid, wie eine Seerose. Eine Pumpstation auf dem Ponton fördert aus dem Fluid darunter.

## Stickstoff

Im [[helium|Kryo-Verflüssiger]] wird aus Stickstoff <item:infinite_horizon:liquid_nitrogen>. Er kühlt den [[kernreaktor|Kernreaktor]] mit viel weniger Menge als Wasser.

@recipes_for infinite_horizon:liquid_nitrogen

## Herstellung

@recipes_for infinite_horizon:drilling_rig infinite_horizon:pump_station infinite_horizon:pontoon
