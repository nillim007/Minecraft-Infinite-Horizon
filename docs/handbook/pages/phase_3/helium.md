---
title: Helium und Kryotechnik
title_en: Helium and Cryogenics
order: 95
milestone: 5
requires: ["infinite_horizon:regolith_heater"]
icon: infinite_horizon:cryo_liquefier
items: ["infinite_horizon:regolith_heater", "infinite_horizon:cryo_liquefier", "infinite_horizon:gas_tank", "infinite_horizon:helium", "infinite_horizon:liquid_helium", "infinite_horizon:cooling_system"]
summary: Helium aus Mondregolith gewinnen und verflüssigen
---
Der Mondboden enthält Helium, das der Sonnenwind über Milliarden Jahre dort abgelegt hat. Mit Hitze holst du es heraus, mit Kälte machst du es flüssig.

## Regolith-Erhitzer

Der <item:infinite_horizon:regolith_heater> erhitzt <item:infinite_horizon:moon_regolith> und gibt Helium in seinen Tank ab. Er arbeitet **nur auf dem Mond**. An jedem anderen Ort leuchtet seine Lampe rot, und der Tooltip sagt dir, warum.

@machine infinite_horizon:regolith_heater

@recipes_type infinite_horizon:regolith_heating

## Kryo-Verflüssiger

Der <item:infinite_horizon:cryo_liquefier> kühlt Helium so stark, dass es flüssig wird. Flüssiges Helium braucht das <item:infinite_horizon:cooling_system> der Hitzeanzüge.

@machine infinite_horizon:cryo_liquefier

@recipes_type infinite_horizon:cryo

## Gastank

Gase lagerst du im <item:infinite_horizon:gas_tank>. Er nimmt nur Gase auf und gibt sie über eine wählbare Seite ab, wie der Flüssigkeitstank.

@machine infinite_horizon:gas_tank

## Herstellung

@recipes_for infinite_horizon:regolith_heater infinite_horizon:cryo_liquefier infinite_horizon:gas_tank infinite_horizon:cooling_system

@tip Stell den Regolith-Erhitzer direkt in deine Mondbasis und leite das Helium mit Rohren in einen Gastank. Den Verflüssiger kannst du dann auch auf der Erde betreiben.
