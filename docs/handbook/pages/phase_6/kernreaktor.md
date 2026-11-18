---
title: Der Kernreaktor
title_en: The Fission Reactor
order: 20
milestone: 8
requires: ["infinite_horizon:fission_reactor"]
icon: infinite_horizon:fission_reactor
items: ["infinite_horizon:fission_reactor", "infinite_horizon:fission_casing", "infinite_horizon:fuel_rod"]
summary: Strom aus Uran, Hitze und Notabschaltung
---
Der Kernreaktor liefert {{value:generators:infinite_horizon:fission_reactor.output_per_tick}} J/t aus <item:infinite_horizon:fuel_rod>. Ein Brennstab hält {{value:generators:infinite_horizon:fission_reactor.fuel.0.per_ticks|t}}. Der Reaktor hat {{value:generators:infinite_horizon:fission_reactor.item_slots.fuel}} Slots für Brennstäbe und nimmt sich den nächsten von selbst.

@machine infinite_horizon:fission_reactor

## Der Aufbau

Der Reaktor ist ein Würfel von {{value:machines:fission_reactor_multiblock.size.width}} × {{value:machines:fission_reactor_multiblock.size.depth}} × {{value:machines:fission_reactor_multiblock.size.height}} Blöcken mit hohlem Innenraum, aufgebaut wie der [[fusionsreaktor|Fusionsreaktor]].

1. Setz den Controller (<item:infinite_horizon:fission_reactor>). Seine Vorderseite zeigt zu dir.
2. Der Controller ist die **Mitte einer Seitenfläche**: Der Würfel reicht von ihm zwei Blöcke nach links, rechts, oben und unten und vier Blöcke nach hinten.
3. Bau die Hülle aus <item:infinite_horizon:fission_casing>. Der Innenraum von {{value:machines:fission_reactor_multiblock.interior.width}} × {{value:machines:fission_reactor_multiblock.interior.depth}} × {{value:machines:fission_reactor_multiblock.interior.height}} Blöcken bleibt Luft.
4. Ersetze einzelne Gehäuseblöcke durch **Ports**, wo Rohre und Kabel andocken sollen.
5. Sobald alles stimmt, springt die Statuslampe von Rot auf Gelb.

@tip **Sneak-Rechtsklick** mit leerer Hand auf den Controller zeigt mit roten Partikeln, wo noch Blöcke fehlen.

## Kühlung und Hitze

Solange der Reaktor arbeitet, braucht er je Tick {{value:generators:infinite_horizon:fission_reactor.coolant.0.amount}} mB Wasser **oder** {{value:generators:infinite_horizon:fission_reactor.coolant.1.amount}} mB flüssigen Stickstoff im Kühltank. Die Hitze ändert sich je Tick um {{value:generators:infinite_horizon:fission_reactor.safety.heat_per_tick_cooled}} mit Kühlung und um {{value:generators:infinite_horizon:fission_reactor.safety.heat_per_tick_uncooled}} ohne. Der Fortschrittsbalken im Fenster zeigt die Hitze.

- Ab {{value:generators:infinite_horizon:fission_reactor.safety.warning_at}} warnt er mit einem Ton, und die Statuslampe blinkt.
- Bei {{value:generators:infinite_horizon:fission_reactor.safety.scram_at}} schaltet er sich ab. Er explodiert nicht, aber im Umkreis von {{value:generators:infinite_horizon:fission_reactor.safety.radiation_zone.radius}} Blöcken herrscht für {{value:generators:infinite_horizon:fission_reactor.safety.radiation_zone.duration_ticks|t}} Strahlung der Stufe {{value:generators:infinite_horizon:fission_reactor.safety.radiation_zone.radiation}}.
- Sobald die Hitze wieder unter {{value:generators:infinite_horizon:fission_reactor.safety.restart_below_heat}} liegt, startet er von selbst.

@warning Nach einer Notabschaltung brauchst du für die Strahlungszone einen Anzug mit Strahlenschild oder eine Kuppel der Stufe 3. Ohne Schutz nimmst du dort Schaden.

@tip Eine Wasserpumpe über einer Quelle neben dem Reaktor hält den Kühltank zuverlässig voll.

## Herstellung

@recipes_for infinite_horizon:fission_reactor infinite_horizon:fission_casing infinite_horizon:fuel_rod
