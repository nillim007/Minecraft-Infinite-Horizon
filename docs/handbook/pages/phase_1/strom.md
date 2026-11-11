---
title: Strom
title_en: Power
order: 30
milestone: 1
requires: ["sternenwerft:combustion_generator", "sternenwerft:copper_cable"]
icon: sternenwerft:combustion_generator
items: ["sternenwerft:combustion_generator", "sternenwerft:copper_cable", "sternenwerft:fuel_generator", "sternenwerft:battery"]
summary: Generatoren, Kabel und Akkus
---
Alle Maschinen laufen mit Strom, gemessen in **Joule (J)**. Wie viel eine Maschine pro Tick braucht, steht als **J/t** auf ihrer Seite. Ein Tick ist eine Zwanzigstelsekunde.

## Verbrennungsgenerator

Der <item:sternenwerft:combustion_generator> ist dein erstes Kraftwerk. Er verbrennt alles, was auch im Ofen brennt, und liefert {{value:generators:combustion_generator.output_per_tick}} J/t. Lavaeimer funktionieren auch; der leere Eimer bleibt im Slot liegen.

@machine sternenwerft:combustion_generator
@recipes_for sternenwerft:combustion_generator

@if sternenwerft:fuel_generator
## Treibstoffgenerator

Sobald deine [[raffinerie|Raffinerie]] läuft, lohnt sich der <item:sternenwerft:fuel_generator>. Er verbrennt Kerosin und liefert {{value:generators:fuel_generator.output_per_tick}} J/t – ein Vielfaches des Verbrennungsgenerators.

@machine sternenwerft:fuel_generator
@recipes_for sternenwerft:fuel_generator
@endif

## Kupferkabel

Das <item:sternenwerft:copper_cable> verbindet sich von selbst mit allem, was Strom erzeugt oder braucht. Ein Kabelnetz überträgt höchstens {{value:transport:copper_cable.throughput}} J/t, egal wie lang es ist. Generatoren schieben ihren Strom in alle Seiten, das Netz verteilt ihn gleichmäßig auf alle Maschinen.

@machine sternenwerft:copper_cable
@recipes_for sternenwerft:copper_cable

@if sternenwerft:battery
## Akku

Der <item:sternenwerft:battery> speichert Strom für die Nacht oder für Lastspitzen. Er nimmt auf allen Seiten Strom an und gibt ihn nur über seine **Ausgangsseite** ab (Knopf „Seite“ im Fenster). So kann er zwei Netze sauber trennen.

@machine sternenwerft:battery
@recipes_for sternenwerft:battery
@endif

@tip Ein Kupferkabel-Netz trägt {{value:transport:copper_cable.throughput}} J/t. Reicht das nicht, teile deine Maschinen auf mehrere Netze mit eigenem Generator auf.

@note Maschinen verbrauchen nur Strom, wenn sie arbeiten. Wird der Strom knapp, pausieren sie und machen später an derselben Stelle weiter. Nichts geht verloren.
