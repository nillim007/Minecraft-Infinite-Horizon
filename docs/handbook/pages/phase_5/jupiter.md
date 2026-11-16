---
title: Jupiter-Orbit und Gas-Sammler
title_en: Jupiter Orbit and Gas Collector
order: 70
milestone: 7
requires: ["infinite_horizon:gas_collector"]
icon: infinite_horizon:gas_collector
items: ["infinite_horizon:gas_collector"]
summary: Station über dem Gasriesen, Wasserstoff aus der Atmosphäre
---
Jupiter hat keine Oberfläche, auf der du landen kannst. Eine Rakete bringt dich nur in den **Jupiter-Orbit**, eine leere Welt über dem Gasriesen. Von dort fliegst du mit der [[landefaehre|Landefähre]] zu den Monden [[io|Io]] und Europa.

## Gefahren im Orbit

Die Strahlung im Jupiter-Orbit hat Stufe {{value:planet:jupiter_orbit.radiation}}. Jeder Raumanzug braucht dort einen <item:infinite_horizon:radiation_shield>. Wer unter die Station fällt, landet nicht auf dem Planeten: Er wird auf den nächsten [[raumstation|Stationskern]] zurückgeholt und nimmt dabei {{value:stations:orbit_fall_rules.gas_giant_orbits.damage}} Schaden. Gibt es noch keinen Kern, entsteht eine kleine Plattform aus Stationshülle.

@warning Ohne Strahlenschild ist der Jupiter-Orbit tödlich, auch in einem Hitze- oder Druckanzug.

## Der Gas-Sammler

Der <item:infinite_horizon:gas_collector> arbeitet nur in den Orbits der Gasriesen. Er braucht keine Zutaten: Alle {{value:planet:jupiter_orbit.gas_collector_output.ticks}} Ticks füllt er {{value:planet:jupiter_orbit.gas_collector_output.amount}} mB Gas in seinen Tank, im Jupiter-Orbit Wasserstoff. Leere Behälter im Füllslot werden gefüllt, Rohre ziehen das Gas ab.

@machine infinite_horizon:gas_collector

@recipes_for infinite_horizon:gas_collector

@tip Ein Gas-Sammler neben dem Tankdepot der Station versorgt Raketen und Fusionsreaktor mit Wasserstoff, ohne dass du Wasser zur Station bringen musst.
