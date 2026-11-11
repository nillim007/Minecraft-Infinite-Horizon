---
title: Fluide und Rohre
title_en: Fluids and Pipes
order: 70
milestone: 2
requires: ["sternenwerft:copper_pipe", "sternenwerft:fluid_tank", "sternenwerft:fluid_pump"]
icon: sternenwerft:fluid_tank
items: ["sternenwerft:crude_oil", "sternenwerft:copper_pipe", "sternenwerft:fluid_tank", "sternenwerft:fluid_pump", "sternenwerft:crude_oil_bucket", "sternenwerft:kerosene_bucket"]
summary: Pumpe, Kupferrohr, Tank und Eimer
---
Erdöl, Kerosin, Wasserstoff und Sauerstoff sind **Fluide**. Sie werden in Millibuckets gemessen: 1.000 mB sind ein Eimer.

## Pumpe

Die <item:sternenwerft:fluid_pump> saugt Quellblöcke unter sich ab: Wasser, Lava oder Erdöl, bis zu {{value:machines:fluid_pump.process.search_radius}} Blöcke tief und im gleichen Umkreis zur Seite. Jeder Vorgang bringt {{value:machines:fluid_pump.process.output_per_cycle}} mB. Bei Erdöl verschwindet der Quellblock nur mit {{value:machines:fluid_pump.process.source_consume_chance.crude_oil|%}} Wahrscheinlichkeit. Eine Ölblase hält also sehr lange.

@machine sternenwerft:fluid_pump

@image ../images/screenshots/gui-fluid-pump.png | Das Fenster der Pumpe: Tank und Eimerslots, Knopf „Seite“ für die Ausgangsseite.
@recipes_for sternenwerft:fluid_pump

## Kupferrohr

Das <item:sternenwerft:copper_pipe> transportiert Flüssigkeiten und Gase, {{value:transport:copper_pipe.throughput}} mB pro Tick. Zwei wichtige Regeln:

- Ein Rohrnetz führt **immer nur ein Fluid**. Erst wenn es leer ist, kann ein anderes hinein. Für Wasser und Erdöl brauchst du also getrennte Rohre.
- Rohre ziehen nie selbst. Maschinen und Tanks **schieben** ihre Ausgabe über ihre Ausgangsseite hinein.

@recipes_for sternenwerft:copper_pipe

## Tank

Der <item:sternenwerft:fluid_tank> fasst {{value:storage:fluid_tank.capacity}} mB einer Flüssigkeit. Er gibt nur über seine Ausgangsseite ab und behält seinen Inhalt, wenn du ihn abbaust. Gase passen nicht hinein; sie bleiben vorerst im Tank der Maschine.

@machine sternenwerft:fluid_tank

@image ../images/screenshots/gui-fluid-tank.png | Das Fenster des Tanks mit Füll- und Leerslot.
@recipes_for sternenwerft:fluid_tank

## Eimer, Füll- und Leerslot

Jede Maschine mit Tank hat zwei besondere Slots:

- **Füllslot:** Leg einen leeren Eimer hinein, und die Maschine füllt ihn aus ihrem Tank.
- **Leerslot:** Leg einen vollen Eimer hinein, und die Maschine leert ihn in ihren Tank.

Ein <item:sternenwerft:crude_oil_bucket> entsteht auch, wenn du mit einem leeren Eimer auf einen Erdöl-Quellblock klickst. Einen <item:sternenwerft:kerosene_bucket> füllst du nur im Füllslot der Raffinerie oder eines Tanks.

@tip Stell die Pumpe direkt über eine Ölblase und den Tank daneben. Mit einem Kupferrohr dazwischen läuft das Erdöl von allein.

@warning Ein Rohr, das schon Wasser führt, nimmt kein Erdöl an. Wenn nichts fließt, prüfe, ob noch ein altes Fluid im Netz steckt.
