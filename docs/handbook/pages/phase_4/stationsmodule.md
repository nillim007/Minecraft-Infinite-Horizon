---
title: Stationsmodule
title_en: Station Modules
order: 40
milestone: 6
requires: ["infinite_horizon:docking_bay"]
icon: infinite_horizon:docking_bay
items: ["infinite_horizon:docking_bay", "infinite_horizon:tank_depot", "infinite_horizon:solar_wing", "infinite_horizon:habitation_module", "infinite_horizon:research_lab", "infinite_horizon:greenhouse"]
summary: Andockbucht, Tankdepot, Solarflügel, Wohnmodul, Forschungslabor, Gewächshaus
---
Rund um den [[raumstation|Stationskern]] baust du deine Station mit Modulen aus. Alle Module funktionieren nur im Orbit, das Tankdepot auch überall sonst.

## Andockbucht

Die <item:infinite_horizon:docking_bay> ist ein Landeplatz für Raketen. Sie erscheint wie ein Landefeuer in der Landeortwahl deiner Agentur und lässt sich mit Rechtsklick benennen. Wählst du sie als Ziel, setzt die Rakete direkt auf ihr auf. Steht eine Rakete auf der Bucht, tankt sie aus angrenzenden Tanks nach, so schnell wie an einer Treibstoffpumpe.

## Tankdepot

Das <item:infinite_horizon:tank_depot> fasst {{value:storage:infinite_horizon:tank_depot.capacity}} mB eines Fluids. Es gibt nur über seine Ausgangsseite ab, alle anderen Seiten nehmen auf. Neben der Andockbucht ist es der Treibstoffspeicher deiner Station.

@machine infinite_horizon:tank_depot

## Solarflügel

Der <item:infinite_horizon:solar_wing> liefert im Orbit {{value:generators:infinite_horizon:solar_wing.output_per_tick.orbit}} J/t mal Solarfaktor, solange er die Sonne sieht. Auf einem Planeten liefert er nichts.

## Wohnmodul

Das <item:infinite_horizon:habitation_module> ist Bett und Lager zugleich: Rechtsklick setzt deinen Respawnpunkt über das Modul und öffnet {{value:stations:infinite_horizon:habitation_module.storage_slots}} Lagerplätze. Der Respawnpunkt gilt nur unter einer aktiven Kuppel.

## Forschungslabor

Ein Analysator direkt neben dem <item:infinite_horizon:research_lab> bringt {{value:research:research_lab.analyzer_fp_multiplier}}-mal so viele Forschungspunkte.

## Gewächshaus

Das <item:infinite_horizon:greenhouse> arbeitet wie {{value:agriculture:infinite_horizon:greenhouse.behaves_like_basins}} [[hydrokultur|Hydrokultur-Becken]] auf einmal. Es braucht Strom, Wasser, Licht, eine aktive Kuppel und je {{value:agriculture:infinite_horizon:greenhouse.consumes.per_ticks|t}} ein <item:infinite_horizon:dry_ice> im letzten Eingabeslot.

@machine infinite_horizon:greenhouse

## Herstellung

@recipes_for infinite_horizon:docking_bay infinite_horizon:tank_depot infinite_horizon:solar_wing infinite_horizon:habitation_module infinite_horizon:research_lab infinite_horizon:greenhouse

@warning Das Wohnmodul setzt den Respawnpunkt nur in einer aktiven Kuppel. Fällt sie aus, erscheinst du nach dem Tod wieder auf der Erde.

@tip Stell ein Tankdepot mit Wasserstoff und Sauerstoff direkt neben die Andockbucht. Dann ist deine Rakete nach jeder Landung schnell wieder voll.
