---
title: Die Rakete
title_en: The Rocket
order: 70
milestone: 4
requires: ["infinite_horizon:rocket_capsule_t1", "infinite_horizon:rocket_engine_t1"]
icon: infinite_horizon:rocket_capsule_t1
items: ["infinite_horizon:rocket_capsule_t1", "infinite_horizon:rocket_tank_t1", "infinite_horizon:rocket_engine_t1", "infinite_horizon:rocket_fin_t1", "infinite_horizon:rocket_cargo_bay"]
summary: Raketenteile, Aufbau, Schub und Treibstoff
---
Eine Rakete baust du Block für Block auf der [[raumfahrt|Startrampe]]. Alle Teile sind einen Block groß.

## Aufbau

Von unten nach oben über der Mitte der Plattform:

1. <item:infinite_horizon:rocket_engine_t1> direkt auf der Mitte.
2. {{value:rocket_parts:rules.1.fins}} <item:infinite_horizon:rocket_fin_t1> waagerecht neben dem Triebwerk, je eine im Norden, Osten, Süden und Westen.
3. {{value:rocket_parts:rules.1.tanks.0}} bis {{value:rocket_parts:rules.1.tanks.1}} <item:infinite_horizon:rocket_tank_t1> übereinander.
4. Wenn du willst, ein <item:infinite_horizon:rocket_cargo_bay> für Fracht ({{value:rocket_parts:parts.rocket_cargo_bay.cargo_slots}} Slots, Rechtsklick öffnet ihn).
5. Ganz oben die <item:infinite_horizon:rocket_capsule_t1>. Rechtsklick darauf setzt dich hinein, Schleichen lässt dich aussteigen.

## Masse und Schub

| Teil | Masse |
| --- | --- |
| Kapsel | {{value:rocket_parts:parts.rocket_capsule_t1.mass}} t |
| Triebwerk | {{value:rocket_parts:parts.rocket_engine_t1.mass}} t |
| Flosse | {{value:rocket_parts:parts.rocket_fin_t1.mass}} t |
| Tank, leer | {{value:rocket_parts:parts.rocket_tank_t1.mass}} t |
| Tank, voller Treibstoff | + {{value:rocket_parts:parts.rocket_tank_t1.fuel_mass_full}} t |
| Frachtraum | {{value:rocket_parts:parts.rocket_cargo_bay.mass}} t + {{value:rocket_parts:parts.rocket_cargo_bay.mass_per_slot}} t je belegtem Slot |

Das Triebwerk T1 hat den Schub {{value:rocket_parts:parts.rocket_engine_t1.thrust}}. Starten darf die Rakete nur, wenn **Schub / (Masse × Schwerkraft)** mindestens {{value:spaceflight:thrust_to_weight.min_ratio}} ist. Viele volle Tanks und ein voller Frachtraum zusammen sind zu schwer: Der Raketen-Computer zeigt das Verhältnis an.

## Treibstoff

Jeder Tank fasst {{value:rocket_parts:parts.rocket_tank_t1.fuel_capacity}} mB Kerosin **und** {{value:rocket_parts:parts.rocket_tank_t1.fuel_capacity}} mB Sauerstoff. Ein Flug kostet von beiden gleich viel:

- **Start:** {{value:spaceflight:launch_cost.per_g}} mB mal die Schwerkraft des Startorts,
- **Weg:** {{value:spaceflight:transfer_cost.per_dp}} mB je Distanzpunkt Unterschied, dazu {{value:spaceflight:transfer_cost.different_body}} mB, wenn das Ziel ein anderer Himmelskörper ist,
- **Landung:** {{value:spaceflight:landing_cost.factor|%}} der Startkosten am Ziel (auf der Erde nichts).

Der Raketen-Computer im Steuerpult rechnet das für jedes Ziel aus: **grün** heißt hin und zurück, **gelb** nur hin, **grau** nicht erreichbar oder noch ohne Sonde.

## Die Checkliste

Im Steuerpult stehen diese Punkte, die alle einen Haken brauchen:

1. **Struktur gültig:** Rampe und Rakete sind richtig aufgebaut (z. B. alle Flossen am Triebwerk).
2. **Schub ≥ Gewicht.**
3. **Treibstoff für ein Ziel.**
4. **Ziel freigeschaltet:** die Erde und jedes Ziel, das eine Sonde deiner Agentur erreicht hat.
5. **Besatzung:** Ein Spieler im vollständigen [[raumanzug|Druckanzug]] sitzt in der Kapsel.

Wie Start, Flug und Landung ablaufen, steht unter [[flug|Der Flug]].

@warning Plane den Rückflug ein: Gelbe Ziele erreichst du, kommst aber ohne Nachtanken nicht zurück. Nimm Treibstoff im Frachtraum mit oder baue am Ziel eine Pumpe.

## Herstellung

@recipes_for infinite_horizon:rocket_capsule_t1 infinite_horizon:rocket_tank_t1 infinite_horizon:rocket_engine_t1 infinite_horizon:rocket_fin_t1 infinite_horizon:rocket_cargo_bay

@tip Für den Mond reicht schon die kleinste Rakete mit vollen Tanks für Hin- und Rückflug. Mehr Tanks bringen Reserve, aber auch Gewicht.
