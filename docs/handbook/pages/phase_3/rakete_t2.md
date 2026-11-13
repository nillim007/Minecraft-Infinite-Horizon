---
title: Die Rakete T2
title_en: The T2 Rocket
order: 100
milestone: 5
requires: ["infinite_horizon:rocket_engine_t2"]
icon: infinite_horizon:rocket_capsule_t2
items: ["infinite_horizon:rocket_capsule_t2", "infinite_horizon:rocket_tank_t2", "infinite_horizon:rocket_engine_t2", "infinite_horizon:rocket_fin_t2", "infinite_horizon:heat_shield", "infinite_horizon:tungsten_plate"]
summary: Größere Rakete für Mars und Venus, mit Wasserstoff
---
Mars und Venus sind zu weit für die erste [[rakete|Rakete]]. Die Rakete T2 fliegt mit **Wasserstoff und Sauerstoff**, hat Platz für {{value:rocket_parts:rules.2.seats}} Personen und braucht eine größere Startrampe.

## Startrampe T2

Die Rakete T2 startet nur von einer Plattform aus {{value:rocket_parts:rules.2.pad_size.0}} × {{value:rocket_parts:rules.2.pad_size.1}} Rampenblöcken, sonst ist alles wie bei der [[raumfahrt|Startrampe]]. Der Turm muss so hoch sein wie die Rakete. Eine gelandete Rakete T2 startet auch ohne Rampe.

## Aufbau

1. <item:infinite_horizon:rocket_engine_t2> auf der Mitte, daneben {{value:rocket_parts:rules.2.fins}} <item:infinite_horizon:rocket_fin_t2>.
2. {{value:rocket_parts:rules.2.tanks.0}} bis {{value:rocket_parts:rules.2.tanks.1}} <item:infinite_horizon:rocket_tank_t2>, bei Bedarf Frachträume.
3. Oben die <item:infinite_horizon:rocket_capsule_t2>.

Ein Tank fasst je {{value:rocket_parts:parts.rocket_tank_t2.fuel_capacity}} mB Wasserstoff und Sauerstoff. Beides liefert der [[elektrolyseur|Elektrolyseur]], die Pumpe füllt die Tanks wie bei T1.

## Neue Ziele

Erforsche **Planetensonden** und schick je eine [[raumfahrt|Sonde]] zum Mars und zur Venus. Danach erscheinen beide in der [[flug|Navigationskarte]]. Siehe [[mars|Der Mars]] und [[venus|Die Venus]].

## Herstellung

@recipes_for infinite_horizon:rocket_capsule_t2 infinite_horizon:rocket_tank_t2 infinite_horizon:rocket_engine_t2 infinite_horizon:rocket_fin_t2 infinite_horizon:heat_shield infinite_horizon:tungsten_plate

@tip Erweitere deine 5×5-Rampe einfach um einen Ring aus Rampenblöcken. Die Rakete T1 startet weiterhin von der Mitte.
