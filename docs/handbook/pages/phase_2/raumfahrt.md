---
title: Startrampe und Sonde
title_en: Launch Pad and Probe
order: 60
milestone: 4
requires: ["infinite_horizon:launch_pad", "infinite_horizon:launch_control", "infinite_horizon:probe"]
icon: infinite_horizon:launch_control
items: ["infinite_horizon:launch_pad", "infinite_horizon:launch_tower", "infinite_horizon:fuel_pump", "infinite_horizon:launch_control", "infinite_horizon:probe"]
summary: Startrampe bauen, betanken und die erste Sonde schicken
---
Bevor Menschen fliegen, fliegt eine Sonde. Erst wenn sie ein Ziel erreicht hat, kennt deine Agentur den Weg dorthin, und du kannst mit der [[rakete|Rakete]] folgen. Beides startet von derselben **Startrampe**.

## Die Startrampe bauen

@machine infinite_horizon:launch_pad

1. **Plattform:** Lege eine Fläche aus <item:infinite_horizon:launch_pad>, 5 × 5 Blöcke auf gleicher Höhe. Ihre Mitte ist der Block, auf dem später das Triebwerk oder die Sonde steht. Weitere Rampenblöcke daneben stören nicht.
2. **Turm:** Stell eine senkrechte Säule <item:infinite_horizon:launch_tower> direkt neben den Rand der Fläche, nicht an eine Ecke. Ihr unterster Block steht auf Plattformhöhe. Der Turm muss mindestens so hoch sein wie die Rakete (Flossen zählen nicht), für eine Sonde reicht ein Block.
3. **Pumpe:** Mindestens eine <item:infinite_horizon:fuel_pump> berührt Plattform oder Turm mit einer Fläche.
4. **Steuerpult:** Setz das <item:infinite_horizon:launch_control> höchstens {{value:machines:launch_pad_t1_multiblock.control_radius}} Blöcke von der Mitte entfernt. Es steuert immer die nächste gültige Rampe.

Das Steuerpult prüft die Rampe, wenn du es öffnest, beim Start und alle {{value:misc:multiblock_rules.structure_check_interval_ticks|t}}. Stimmt etwas nicht, leuchtet seine Lampe rot, und die Checkliste nennt den ersten Fehler.

## Betanken

Die Treibstoffpumpe hat zwei Tanks: einen für Brennstoff (<item:infinite_horizon:kerosene>) und einen für Sauerstoff. Sie nimmt von allen Seiten an, aus Rohren, Tanks und einer <item:infinite_horizon:pressure_bottle>, und über Füll- und Leerslot auch aus einem <item:infinite_horizon:transport_tank>. Strom braucht sie nicht.

@machine infinite_horizon:fuel_pump

Jeden Tick gibt sie von jeder Komponente bis zu {{value:machines:fuel_pump.transfer_per_tick}} mB ab und verteilt sie gleichmäßig auf alle Raketentanks ihrer Rampe. Ein Raketentank fasst je Komponente {{value:rocket_parts:parts.rocket_tank_t1.fuel_capacity}} mB.

## Die Sonde

1. Erforsche den Knoten **Mondsonde** ([[forschung|Forschung]]). Er macht den Mond zum Sondenziel.
2. Setz eine <item:infinite_horizon:probe> auf die Mitte der Rampe.
3. Öffne das Steuerpult. Unter „Sonde auf der Rampe“ steht für jedes erforschte Ziel ein Knopf. Ein Klick startet den Countdown von {{value:spaceflight:probe_flight.countdown_ticks|t}}.
4. Die Sonde steigt auf und verschwindet. Ihr Flug dauert {{value:spaceflight:probe_flight.base_ticks|t}} plus {{value:spaceflight:probe_flight.per_dp_ticks|t}} je Distanzpunkt des Ziels. Er läuft weiter, auch wenn du dich abmeldest.
5. Kommt sie an, bekommen alle Mitglieder eine Nachricht und den Erfolg „Erstes Signal“, und deine Agentur bekommt beim ersten Mal {{value:research:probe_new_target.fp}} FP. Ab jetzt ist das Ziel ein Raketenziel.

@note Eine Sonde kehrt nie zurück. Für jedes Ziel brauchst du eine eigene.

## Herstellung

@recipes_for infinite_horizon:launch_pad infinite_horizon:launch_tower infinite_horizon:fuel_pump infinite_horizon:launch_control infinite_horizon:probe

@tip Bau den Turm gleich hoch genug für die größte Rakete deiner Stufe. Dann musst du später nicht umbauen.
