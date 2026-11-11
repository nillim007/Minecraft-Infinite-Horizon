---
title: Raffinerie
title_en: Refinery
order: 100
milestone: 2
requires: ["sternenwerft:refinery_controller", "sternenwerft:refinery_casing"]
icon: sternenwerft:refinery_controller
items: ["sternenwerft:refinery_controller", "sternenwerft:refinery_casing", "sternenwerft:plastic", "sternenwerft:kerosene", "sternenwerft:item_port", "sternenwerft:fluid_port", "sternenwerft:energy_port"]
summary: Dein erster Multiblock: Erdöl wird zu Kerosin und Kunststoff
---
Die Raffinerie ist dein erster **Multiblock**: ein Turm aus 45 Blöcken, der aus Erdöl **Kerosin** und **Kunststoff** macht. Kerosin ist Treibstoff, Kunststoff steckt in jedem Schaltkreis.

@image ../images/refinery.svg | Bauplan: Der Controller (orange) sitzt unten in der Mitte der Vorderseite, Ports (blau, gelb) ersetzen beliebige Gehäuseblöcke.

@machine sternenwerft:refinery_controller

@image ../images/screenshots/gui-refinery.png | Das Fenster der Raffinerie: Erdöl-Tank links, Kunststoff in der Mitte, Kerosin-Tank rechts.

## Was sie macht

@recipes_type refining

## Der Aufbau

Die Raffinerie ist **3 Blöcke breit, 3 Blöcke tief und 5 Blöcke hoch**, ganz massiv.

1. Setz den Controller (<item:sternenwerft:refinery_controller>) auf den Boden. Seine Vorderseite zeigt zu dir.
2. Der Controller ist die **Mitte der untersten Reihe auf der Vorderseite**. Der Turm reicht von ihm einen Block nach links und rechts, zwei Blöcke nach hinten und vier Blöcke nach oben.
3. Füll alle übrigen 44 Plätze mit <item:sternenwerft:refinery_casing>.
4. Ersetze einzelne Gehäuseblöcke durch **Ports**, wo Rohre und Kabel andocken sollen (siehe unten).
5. Sobald alles stimmt, springt die Statuslampe von Rot auf Gelb.

@tip **Sneak-Rechtsklick** mit leerer Hand auf den Controller zeigt dir {{value:misc:multiblock_rules.ghost_projection_ticks|t}} lang mit roten Partikeln, wo noch Blöcke fehlen oder falsch sind.

@image ../images/screenshots/refinery-ghost.png | Ein fertiger Raffinerie-Turm von unten gesehen. Ist er unvollständig, zeigen rote Partikel die fehlenden Plätze.

## Ports

Ports ersetzen Gehäuseblöcke und leiten nach außen an den Controller weiter. Rechtsklick mit leerer Hand schaltet zwischen **Eingang** und **Ausgang** um.

- <item:sternenwerft:fluid_port> – Erdöl hinein (Eingang), Kerosin heraus (Ausgang).
- <item:sternenwerft:item_port> – Kunststoff heraus (Ausgang).
- <item:sternenwerft:energy_port> – Strom hinein.

@recipes_for sternenwerft:refinery_controller sternenwerft:refinery_casing sternenwerft:fluid_port sternenwerft:item_port sternenwerft:energy_port

## Materialbedarf

Für 44 Gehäuseblöcke brauchst du 11 Montagevorgänge, also 22 Eisenplatten und 11 Kupferrohre. Jeder Port spart dir einen Gehäuseblock.

@warning Andere Blöcke im Aufbau sind nicht erlaubt. Auch ein einzelner falscher Block lässt die Lampe rot leuchten. Fahr im Fenster mit der Maus über die Lampe: Dort steht der erste fehlende Block mit Koordinate.
