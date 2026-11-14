---
title: Der Fusionsreaktor
title_en: The Fusion Reactor
order: 70
milestone: 6
requires: ["infinite_horizon:fusion_reactor"]
icon: infinite_horizon:fusion_reactor
items: ["infinite_horizon:fusion_reactor", "infinite_horizon:fusion_casing", "infinite_horizon:superconductor_cable", "infinite_horizon:gravity_plate"]
summary: Strom aus Wasserstoff und Helium, Supraleiterkabel, Schwerkraftplatte
---
Der Fusionsreaktor ist das Kraftwerk der Station: {{value:generators:infinite_horizon:fusion_reactor.output_per_tick}} J/t aus Wasserstoff und Helium. Er ist ein großer Multiblock und braucht ständig Kühlung mit flüssigem Helium.

@machine infinite_horizon:fusion_reactor

## Der Aufbau

Der Reaktor ist ein Würfel von {{value:machines:fusion_reactor_multiblock.size.width}} × {{value:machines:fusion_reactor_multiblock.size.depth}} × {{value:machines:fusion_reactor_multiblock.size.height}} Blöcken mit hohlem Innenraum.

1. Setz den Controller (<item:infinite_horizon:fusion_reactor>). Seine Vorderseite zeigt zu dir.
2. Der Controller ist die **Mitte einer Seitenfläche**: Der Würfel reicht von ihm zwei Blöcke nach links, rechts, oben und unten und vier Blöcke nach hinten.
3. Bau die Hülle aus <item:infinite_horizon:fusion_casing>. Der Innenraum von {{value:machines:fusion_reactor_multiblock.interior.width}} × {{value:machines:fusion_reactor_multiblock.interior.depth}} × {{value:machines:fusion_reactor_multiblock.interior.height}} Blöcken bleibt Luft.
4. Ersetze einzelne Gehäuseblöcke durch **Ports**, wo Rohre und Kabel andocken sollen, wie bei der [[raffinerie|Raffinerie]].
5. Sobald alles stimmt, springt die Statuslampe von Rot auf Gelb.

@tip **Sneak-Rechtsklick** mit leerer Hand auf den Controller zeigt mit roten Partikeln, wo noch Blöcke fehlen.

## Betrieb

Der Reaktor verbrennt alle {{value:generators:infinite_horizon:fusion_reactor.fuel.0.per_ticks|t}} {{value:generators:infinite_horizon:fusion_reactor.fuel.0.amount}} mB Wasserstoff und {{value:generators:infinite_horizon:fusion_reactor.fuel.1.amount}} mB Helium. Zur Kühlung braucht er im selben Takt {{value:generators:infinite_horizon:fusion_reactor.coolant.0.amount}} mB flüssiges Helium aus dem [[helium|Kryo-Verflüssiger]]. Das verdampfte Helium landet im Ausgabetank und kann zurück in den Kreislauf.

@warning Fehlt die Kühlung {{value:generators:infinite_horizon:fusion_reactor.safety.shutdown_after_ticks_without_cooling|t}} lang, schaltet der Reaktor ab und die Lampe wird rot. Er explodiert nicht. Er startet von selbst wieder, wenn die Kühlung {{value:generators:infinite_horizon:fusion_reactor.safety.restart_after_ticks_with_cooling|t}} ohne Unterbrechung läuft.

## Supraleiterkabel

Ein Fusionsreaktor liefert mehr Strom, als Aluminiumkabel tragen. Das <item:infinite_horizon:superconductor_cable> leitet {{value:transport:infinite_horizon:superconductor_cable.throughput}} J/t.

## Schwerkraftplatte

Die <item:infinite_horizon:gravity_plate> braucht viel Strom. Dafür gilt im Orbit über ihr wieder normale Schwerkraft: in einem Bereich von {{value:stations:infinite_horizon:gravity_plate.area.width}} × {{value:stations:infinite_horizon:gravity_plate.area.depth}} Blöcken und {{value:stations:infinite_horizon:gravity_plate.area.height}} Blöcken Höhe.

@machine infinite_horizon:gravity_plate

## Herstellung

@recipes_for infinite_horizon:fusion_reactor infinite_horizon:fusion_casing infinite_horizon:superconductor_cable infinite_horizon:gravity_plate

@tip Stell einen Kryo-Verflüssiger neben den Ausgabeport: Er macht aus dem verdampften Helium wieder Kühlmittel.
