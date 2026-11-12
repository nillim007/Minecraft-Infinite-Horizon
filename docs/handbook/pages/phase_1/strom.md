---
title: Strom
title_en: Power
order: 30
milestone: 1
requires: ["infinite_horizon:combustion_generator", "infinite_horizon:copper_cable"]
icon: infinite_horizon:combustion_generator
items: ["infinite_horizon:combustion_generator", "infinite_horizon:copper_cable", "infinite_horizon:fuel_generator", "infinite_horizon:battery"]
summary: Generatoren, Kabel und Akkus
---
Alle Maschinen laufen mit Strom, gemessen in **Joule (J)**. Wie viel eine Maschine pro Tick braucht, steht als **J/t** auf ihrer Seite. Ein Tick ist eine Zwanzigstelsekunde.

## Verbrennungsgenerator

Der <item:infinite_horizon:combustion_generator> ist dein erstes Kraftwerk. Er verbrennt alles, was auch im Ofen brennt, und liefert {{value:generators:combustion_generator.output_per_tick}} J/t. Lavaeimer funktionieren auch; der leere Eimer bleibt im Slot liegen.

@machine infinite_horizon:combustion_generator

@image ../images/screenshots/gui-combustion-generator.png | Das Fenster des Verbrennungsgenerators: Brennstoffslot und Restbrennzeit.
@recipes_for infinite_horizon:combustion_generator

@if infinite_horizon:fuel_generator
## Treibstoffgenerator

Sobald deine [[raffinerie|Raffinerie]] läuft, lohnt sich der <item:infinite_horizon:fuel_generator>. Er verbrennt Kerosin und liefert {{value:generators:fuel_generator.output_per_tick}} J/t – ein Vielfaches des Verbrennungsgenerators.

@machine infinite_horizon:fuel_generator
@recipes_for infinite_horizon:fuel_generator
@endif

## Kupferkabel

Das <item:infinite_horizon:copper_cable> verbindet sich von selbst mit allem, was Strom erzeugt oder braucht. Ein Kabelnetz überträgt höchstens {{value:transport:copper_cable.throughput}} J/t, egal wie lang es ist. Generatoren schieben ihren Strom in alle Seiten, das Netz verteilt ihn gleichmäßig auf alle Maschinen.

@machine infinite_horizon:copper_cable
@recipes_for infinite_horizon:copper_cable

@if infinite_horizon:battery
## Akku

Der <item:infinite_horizon:battery> speichert Strom für die Nacht oder für Lastspitzen. Er nimmt auf allen Seiten Strom an und gibt ihn nur über seine **Ausgangsseite** ab (Knopf „Seite“ im Fenster). So kann er zwei Netze sauber trennen.

@machine infinite_horizon:battery
@recipes_for infinite_horizon:battery
@endif

@tip Ein Kupferkabel-Netz trägt {{value:transport:copper_cable.throughput}} J/t. Reicht das nicht, teile deine Maschinen auf mehrere Netze mit eigenem Generator auf.

@note Maschinen verbrauchen nur Strom, wenn sie arbeiten. Wird der Strom knapp, pausieren sie und machen später an derselben Stelle weiter. Nichts geht verloren.
