---
title: Orbitalwerft
title_en: Orbital Shipyard
order: 70
milestone: 8
requires: ["infinite_horizon:shipyard_terminal"]
icon: infinite_horizon:shipyard_terminal
items: ["infinite_horizon:shipyard_terminal", "infinite_horizon:shipyard_dock"]
summary: Abschnitt für Abschnitt zum Sternenschiff
---
In der Orbitalwerft entsteht dein Sternenschiff. Sie braucht riesige Mengen an Material aus allen Ecken des Sonnensystems, deshalb belieferst du sie mit [[frachtraketen|Frachtraketen]].

## Aufbau

1. Bau eine [[raumstation|Station]] im Erdorbit.
2. Stell das <item:infinite_horizon:shipyard_terminal> in die Station, höchstens {{value:shipyard:shipyard_rules.station_core_radius}} Blöcke vom Stationskern entfernt. An einem anderen Ort lässt es sich nicht setzen.
3. Stell mindestens eine <item:infinite_horizon:shipyard_dock> in dieselbe Station. Sie ist das Landeziel deiner Frachtraketen.

@warning Ohne Werft-Andockbucht in der Station kann die Werft nichts annehmen. Das Terminal zeigt dann einen roten Hinweis.

## Das Werftlager

Was an der Andockbucht ankommt, wandert sofort ins Werftlager. Das Lager ist unbegrenzt, nimmt aber nur an, was die Werft noch braucht. Alles andere bleibt in der Rakete und fliegt zurück. Auch Rohre und Trichter an der Andockbucht liefern ein.

- Methan füllt zuerst den Treibstofftank der Andockbucht. Daraus tanken deine Raketen für den Rückflug. Erst der Rest geht ins Werftlager.
- Für den Bordcomputer nimmt die Werft nur eine Sternenkarte deiner eigenen Agentur an.

@tip Leg am [[logistik_computer|Logistik-Computer]] Routen mit der Bedingung „bis der Bedarf gedeckt ist“ zur Andockbucht an. Die Raketen laden dann nur, was die Werft noch braucht, und hören von selbst auf.

@if infinite_horizon:trading_post
## Gemeinsam bauen

Der Knopf „Partner“ im Werft-GUI gibt die Werft für deine [[agentur|Partner]] frei. Ihre Frachtraketen dürfen dann die Andockbuchten beliefern. Jede Agentur, die mindestens ein Item oder Fluid geliefert hat, bekommt beim Start des Sternenschiffs den Erfolg mit.

@endif
## Die Bauabschnitte

Das Terminal zeigt alle acht Abschnitte mit Fortschrittsbalken. Fährst du mit der Maus über einen Balken, siehst du, was noch fehlt. Lieferungen nimmt die Werft für alle offenen Abschnitte an, die frühesten zuerst.

Nach dem achten Abschnitt erscheint am Terminal der Knopf „Start“ ([[sternenschiff|Start und Sternenschiff]]).

Ist ein Abschnitt voll beliefert, baut die Werft ihn in {{value:shipyard:1.build_ticks|t}}. Gebaut wird der Reihe nach, von 1 bis 8, und die Zeit läuft auch weiter, wenn du nicht in der Nähe bist. Mit jedem fertigen Abschnitt wächst das Gerüst über dem Terminal.

| Abschnitt | Wichtigste Materialien |
| --- | --- |
| 1 Rumpf | Leichtbau-, Titan- und Aluminiumplatten |
| 2 Schilde | Wolframplatten, Hitzeschilde |
| 3 Hauptantrieb | Antimaterie-Antriebe |
| 4 Energie | Kernreaktor, Lithium-Akkus, Brennstäbe |
| 5 Bordcomputer | Iridium-Chips, Platin, Sternenkarte |
| 6 Lebenserhaltung | Kuppelgenerator III, Sauerstoffgeneratoren, Gewächshaus, Klon-Kammer |
| 7 Kühlung | flüssiger Stickstoff, flüssiges Helium, Ammoniak |
| 8 Manövriertriebwerke | Methan, Wasserstoff, Schwefel |

## Herstellung

@recipes_for infinite_horizon:shipyard_terminal infinite_horizon:shipyard_dock
