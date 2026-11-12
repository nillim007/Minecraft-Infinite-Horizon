---
title: Elektrolyseur
title_en: Electrolyzer
order: 90
milestone: 2
requires: ["infinite_horizon:electrolyzer"]
icon: infinite_horizon:electrolyzer
items: ["infinite_horizon:electrolyzer", "infinite_horizon:hydrogen", "infinite_horizon:oxygen"]
summary: Wasser wird zu Wasserstoff und Sauerstoff
---
Der <item:infinite_horizon:electrolyzer> spaltet Wasser mit Strom in **Wasserstoff** und **Sauerstoff**. Beides wirst du später dringend brauchen: Wasserstoff als Treibstoff, Sauerstoff zum Atmen auf fremden Welten.

@machine infinite_horizon:electrolyzer

@image ../images/screenshots/gui-electrolyzer.png | Das Fenster des Elektrolyseurs: ein Eingangstank für Wasser, zwei Ausgangstanks für die Gase.

## Bauen

@recipes_for infinite_horizon:electrolyzer

## Was er macht

@recipes_type electrolysis

## So schließt du ihn an

1. Stell eine <item:infinite_horizon:fluid_pump> über Wasser, oder leg Wassereimer in den Leerslot.
2. Führe das Wasser mit einem <item:infinite_horizon:copper_pipe> an eine beliebige Seite des Elektrolyseurs.
3. Die Gase kommen über die **Ausgangsseite** heraus. Mit dem Knopf „Tank“ wählst du, welches Gas der Füllslot nimmt.

@note Gase passen nicht in den normalen Tank und nicht in Eimer. Vorerst sammeln sie sich im Elektrolyseur selbst. Druckflaschen und Kanister kommen in einer späteren Phase.
