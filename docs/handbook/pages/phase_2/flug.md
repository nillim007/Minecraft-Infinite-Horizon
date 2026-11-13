---
title: Der Flug
title_en: The Flight
order: 75
milestone: 4
requires: ["infinite_horizon:rocket_capsule_t1", "infinite_horizon:landing_beacon"]
icon: infinite_horizon:rocket_engine_t1
summary: Navigationskarte, Start, Landung und Rückflug
---
Steht die [[rakete|Rakete]] betankt auf der [[raumfahrt|Startrampe]] und sind alle Punkte der Checkliste erfüllt, kann es losgehen.

## Starten

1. Zieh den vollständigen [[raumanzug|Druckanzug]] an und nimm genug [[sauerstoff|Sauerstoffflaschen]] mit.
2. Setz dich in die Kapsel (Rechtsklick auf die Kapsel).
3. Drück die **Inventartaste**. Im Sitz öffnet sie den Raketen-Computer des Steuerpults. Du kannst das Pult auch von außen öffnen.
4. Ist die Checkliste vollständig, drück **Start**. Es öffnet sich die **Navigationskarte**.
5. Links stehen die Ziele mit ihren Treibstoffkosten hin und zurück: **grün** heißt hin und zurück, **gelb** nur hin, **grau** gesperrt oder zu weit. Wähle ein Ziel.
6. Rechts wählst du den Landeort: ein [[landefeuer|Landefeuer]] deiner Agentur auf diesem Himmelskörper oder eigene Koordinaten X und Z. Lässt du beides leer, landest du am Spawn des Ziels (auf der Erde am Weltspawn, sonst nahe X/Z 0/0).
7. Drück **Start**. Die Blöcke der Rakete werden zu einer fliegenden Rakete, alle Insassen sitzen darin.

## Der Flug

- **Countdown:** {{value:spaceflight:rocket_flight.countdown_ticks|t}}. Ab jetzt kannst du nicht mehr aussteigen.
- **Aufstieg:** {{value:spaceflight:rocket_flight.ascent_ticks|t}} senkrecht nach oben. Die Kamera zeigt die Rakete von außen.
- **Ankunft:** {{value:spaceflight:rocket_flight.arrival_height}} Blöcke über dem Landepunkt (höchstens bis zur Bauhöhe).
- **Landung:** {{value:spaceflight:rocket_flight.landing_ticks|t}}, dann steht die Rakete auf dem obersten festen Block.

Mit **Esc** überspringst du den Flug: Die Rakete landet sofort am Ziel.

Den Landeplatz sucht die Rakete spiralförmig im Umkreis von {{value:spaceflight:landing_search.radius}} Blöcken: fester Boden ohne Flüssigkeit und {{value:spaceflight:landing_search.air_blocks_above}} Blöcke Luft darüber.

@note Meldest du dich während des Flugs ab, landet die Rakete sofort. Beim nächsten Login sitzt du wieder in ihr.

## Nach der Landung

Die Rakete bleibt als Fahrzeug stehen.

- **Aussteigen:** Schleichen.
- **Einsteigen:** Rechtsklick, solange ein Platz frei ist.
- **Raketen-GUI:** Inventartaste im Sitz oder Rechtsklick von außen. Dort siehst du den Treibstoff, den Frachtraum und die Knöpfe **Start** und **Aus Frachtraum tanken**. Das Tanken leert <item:infinite_horizon:transport_tank> im Frachtraum in die Raketentanks.
- **Zerlegen:** Schleichen und Rechtsklick mit leerer Hand. Alle Teile und die Fracht fallen als Items heraus, der Treibstoff ist verloren.

## Der Rückflug

Eine gelandete Rakete startet ohne Rampe: Setz dich hinein, öffne das Raketen-GUI und drück **Start**. Die Kosten richten sich nach der Schwerkraft des Startorts, vom Mond aus ist der Rückflug viel billiger als der Hinflug.

@warning Gelbe Ziele erreichst du nur einmal. Ohne Treibstoff im Frachtraum oder eine Pumpe am Ziel sitzt du dort fest.

@tip Stell vor dem ersten Flug ein [[landefeuer|Landefeuer]] an deiner Basis auf der Erde auf. Dann landest du auf dem Rückflug zu Hause statt am Weltspawn.
