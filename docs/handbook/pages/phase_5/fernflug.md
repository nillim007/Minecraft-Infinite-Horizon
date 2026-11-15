---
title: Fernflug und Jetpack
title_en: Long Range Flight and Jetpack
order: 60
milestone: 7
requires: ["infinite_horizon:long_range_module"]
icon: infinite_horizon:long_range_module
items: ["infinite_horizon:long_range_module", "infinite_horizon:jetpack"]
summary: Fernflug-Modul, Start von der Andockbucht, Jetpack
---
Die äußeren Ziele hinter dem Asteroidengürtel erreicht eine Rakete T2 nur mit mehr Treibstoff. Dafür gibt es das Fernflug-Modul, und die Station im Orbit wird zur Startrampe.

## Das Fernflug-Modul

Das <item:infinite_horizon:long_range_module> ist ein Raketenteil der Stufe 2. Es sitzt zwischen den Tanks und der Kapsel und fasst {{value:rocket_parts:parts.long_range_module.fuel_capacity}} mB je Treibstoffkomponente zusätzlich. Die fernen Ziele ab Saturn werden erst mit ihm wählbar.

@recipes_for infinite_horizon:long_range_module

## Start von der Andockbucht

Eine Rakete T2 kannst du direkt auf einer [[stationsmodule|Andockbucht]] bauen: Triebwerk auf die Bucht, Flossen darum, Tanks, Fernflug-Modul und Kapsel darüber, ein Steuerpult in der Nähe. Turm und Treibstoffpumpe brauchst du dort nicht. Die Bucht füllt die Tanks aus Tanks, die an sie grenzen. Raketen T1 starten nicht von der Bucht.

@tip Ein Start von der Station kostet keinen Treibstoff für den Aufstieg. Bau die großen Raketen deshalb im Orbit.

## Das Jetpack

Das <item:infinite_horizon:jetpack> ist ein Modul für die Brustplatte. Es speichert {{value:suits:infinite_horizon:jetpack.energy_capacity}} J; lade es im Ladeslot eines Akkus, auch eingebaut in der Brustplatte.

- **Springen** gedrückt halten: Die Schwerkraft ist aufgehoben, und du steigst bis {{value:suits:infinite_horizon:jetpack.max_speed_up}} Blöcke je Tick.
- **Schleichen** in der Luft: Schub nach unten.
- **Vorwärts**: {{value:suits:infinite_horizon:jetpack.horizontal_speed}} Blöcke je Tick in Blickrichtung.

Solange es schiebt, verbraucht es {{value:suits:infinite_horizon:jetpack.energy_per_tick}} J je Tick.

@recipes_for infinite_horizon:jetpack

@warning Ist die Energie leer, fällst du wieder, im Orbit langsam, auf einem Planeten mit voller Schwerkraft.
