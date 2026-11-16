---
title: Saturn-Orbit
title_en: Saturn Orbit
order: 110
milestone: 7
requires: ["infinite_horizon:titan_ice_rock"]
icon: infinite_horizon:gas_collector
summary: Das Tor zum äußeren System, Helium und Ringtrümmer
---
Der Saturn liegt so weit draußen, dass eine Rakete T2 nur mit dem [[fernflug|Fernflug-Modul]] dorthin kommt. Wie beim Jupiter landest du im **Saturn-Orbit** über dem Gasriesen. Von dort bringt dich die [[landefaehre|Landefähre]] zu den Monden [[titan|Titan]] und [[enceladus|Enceladus]].

## Stufe 5

Sobald ein Mitglied deiner Agentur den Saturn-Orbit betritt, öffnet sich Stufe 5 der Forschung, das äußere System.

## Helium aus dem Saturn

Ein [[jupiter|Gas-Sammler]] im Saturn-Orbit sammelt alle {{value:planet:saturn_orbit.gas_collector_output.ticks}} Ticks {{value:planet:saturn_orbit.gas_collector_output.amount}} mB Helium.

## Ringtrümmer

Die Ringe des Saturn streuen Trümmer. Bei einem Trümmerschauer schlagen alle {{value:events:infinite_horizon:ring_debris.effects.impact_interval_ticks|t}} Brocken im Umkreis von {{value:events:infinite_horizon:ring_debris.effects.impact_radius}} Blöcken um jeden Spieler ein, wie bei einem Meteoritenschauer, aber ohne Erz. Nur das <item:infinite_horizon:dome_shield_module> einer Kuppel fängt sie ab.

@warning Ohne Schildmodul trifft ein Trümmerschauer auch deine Station.

@tip Fall im Saturn-Orbit nicht unter die Station: Wie beim Jupiter holt dich der nächste Stationskern zurück, und das tut weh.
