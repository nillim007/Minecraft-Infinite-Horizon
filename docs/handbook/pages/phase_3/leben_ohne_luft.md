---
title: Leben ohne Luft
title_en: Living Without Air
order: 40
milestone: 3
requires: ["infinite_horizon:electric_lamp"]
icon: infinite_horizon:electric_lamp
items: ["infinite_horizon:electric_lamp", "infinite_horizon:extinguished_torch"]
summary: Was auf fremden Welten außerhalb der Kuppel nicht funktioniert
---
Auf allen fremden Welten gelten außerhalb einer aktiven [[kuppel|Kuppel]] eigene Regeln, egal wie die Atmosphäre aussieht. Auf der Erde, im Nether und im End bleibt alles wie gewohnt.

## Kein Feuer

- **Fackeln, Feuer, Lagerfeuer, Laternen und Kerzen** lassen sich draußen nicht setzen. Redstone-Fackeln funktionieren weiter.
- Schon vorhandene Fackeln erlöschen nach und nach und werden zu <item:infinite_horizon:extinguished_torch>. Lagerfeuer und Kerzen gehen aus, Laternen und Feuer verschwinden.

Licht bringt die <item:infinite_horizon:electric_lamp>. Sie leuchtet überall mit voller Stärke, braucht aber Strom: {{value:machines:electric_lamp.energy_per_tick}} J/t. Ohne Strom bleibt sie dunkel.

@machine infinite_horizon:electric_lamp

## Keine Pflanzen, kein Wasser

- **Pflanzen** wachsen draußen nicht, auch Knochenmehl hilft nicht.
- **Wasser** gefriert auf dem Mond sofort zu Eis, und dieses Eis schmilzt dort nicht.

## Betten

Ein Bett setzt deinen Spawnpunkt auf fremden Welten nur **innerhalb einer aktiven Kuppel**. Draußen erscheint „Hier kannst du nicht atmen“, und du kannst nicht schlafen.

Fällt die Kuppel aus, während du tot bist, ist dein Spawnpunkt nicht mehr sicher. Du erscheinst dann am Weltspawn der Erde.

@warning Baue deine Basis so, dass Bett und Kuppelgenerator dicht beieinander stehen. Ein Bett am Kuppelrand liegt schnell draußen.

## Herstellung

@recipes_for infinite_horizon:electric_lamp

@tip In der Kuppel funktionieren Fackeln und Lagerfeuer ganz normal. Elektrische Lampen lohnen sich vor allem draußen und in Höhlen.
