---
title: Sauerstoffgenerator
title_en: Oxygen Generator
order: 20
milestone: 3
requires: ["infinite_horizon:oxygen_generator"]
icon: infinite_horizon:oxygen_generator
items: ["infinite_horizon:oxygen_generator", "infinite_horizon:pressure_hull"]
summary: Sauerstoff aus Strom, überall im Sonnensystem
---
Der <item:infinite_horizon:oxygen_generator> erzeugt Sauerstoff nur aus Strom. Anders als der Elektrolyseur braucht er kein Wasser und funktioniert deshalb überall, auch auf dem trockenen Mond.

@machine infinite_horizon:oxygen_generator

## So benutzt du ihn

1. Stell den Generator auf und versorge ihn mit Strom.
2. Er erzeugt alle {{value:machines:oxygen_generator.ticks|t}} {{value:machines:oxygen_generator.process.output_per_cycle}} mB Sauerstoff in seinen internen Puffer.
3. Über seine **Ausgangsseite** gibt er den Sauerstoff an Tanks, Druckflaschen oder Rohre ab. Die Seite wählst du mit dem Knopf „Seite“.
4. Eine leere [[sauerstoff|Sauerstoffflasche]] im **Füllslot** wird sofort gefüllt.

Wie alle Maschinen nimmt er [[upgrades|Upgrades]].

## Herstellung

@if infinite_horizon:research_terminal
@note Den Sauerstoffgenerator schaltet der Knoten **Kuppel I** in Stufe 2 frei ([[forschung|Forschung]]).
@endif

Er braucht eine <item:infinite_horizon:pressure_hull>. Die Druckhülle ist ein Bauteil für alles, was unter Druck steht.

@recipes_for infinite_horizon:oxygen_generator infinite_horizon:pressure_hull

@tip Der Elektrolyseur liefert Sauerstoff deutlich günstiger, solange du Wasser hast. Auf dem Mond ist der Generator die einfachste Quelle.
