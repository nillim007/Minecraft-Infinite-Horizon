---
title: Forschung
title_en: Research
order: 15
milestone: 4
requires: ["infinite_horizon:research_terminal"]
icon: infinite_horizon:research_terminal
items: ["infinite_horizon:research_terminal"]
summary: Forschungsterminal, Forschungspunkte, Stufen und Knoten
---
Mit der Forschung beginnt dein Raumfahrtprogramm. Am <item:infinite_horizon:research_terminal> gibt deine [[agentur|Agentur]] **Forschungspunkte (FP)** aus und schaltet damit neue Bauteile und Ziele frei. Die Forschung gehört der ganzen Agentur: Was ein Mitglied erforscht, können alle bauen.

## Das Forschungsterminal

1. Bau das Terminal am Montagetisch (Rezept unten) und stell es in deine Basis. Es braucht keinen Strom.
2. Sobald es steht, öffnet sich **Stufe 1** der Forschung für deine Agentur, und du bekommst den Erfolg „Bodenstation“.
3. Rechtsklick öffnet den **Forschungsbaum**: vier Spalten für die Äste Industrie und Energie, Raumfahrt, Lebenserhaltung und Logistik, sechs Zeilen für die Stufen. Die Fläche lässt sich mit der Maus ziehen und scrollen (mit Umschalt waagerecht).
4. Oben rechts stehen die FP deiner Agentur.

Jeder Knoten zeigt sein Symbol mit einem farbigen Rahmen:

| Rahmen | Bedeutung |
| --- | --- |
| grau | gesperrt: Stufe noch zu oder Voraussetzung fehlt |
| gelb | verfügbar: ein Klick erforscht ihn |
| grün | erforscht |

Der Tooltip nennt Kosten, Freischaltungen und fehlende Voraussetzungen. Erforschen zieht die FP sofort ab. Forschen dürfen Leiter und Ingenieure der Agentur.

## Stufen

Eine Stufe öffnet sich, wenn die vorige offen ist und ihr **Tor** erfüllt ist. Offene Stufen schließen sich nie wieder, auch wenn du das Terminal abbaust. Die Zeilenköpfe im Baum zeigen jedes Tor und einen Haken, sobald die Stufe offen ist.

| Stufe | Tor | Standardkosten je Knoten |
| --- | --- | --- |
| 1 Raumfahrtprogramm | ein eigenes Forschungsterminal setzen | {{value:tier:1.cost_default}} FP |
| 2 Nahe Welten | ein Mitglied betritt den Mond | {{value:tier:2.cost_default}} FP |

Die Stufen öffnen auch die Kapitel dieses Handbuchs: Die dritte Phase („Ring 1“) liest du ab Stufe 2.

## Gesperrte Rezepte

Ein Knoten schaltet **alle Rezepte** frei, deren Ergebnis er nennt. Solange deine Agentur ihn nicht erforscht hat, bauen deine Maschinen und dein Montagetisch diese Dinge nicht: Die Statuslampe bleibt gelb, und ihr Tooltip meldet „Gesperrt“. Was in keinem Knoten steht, kannst du immer herstellen, zum Beispiel Schaltkreise, Upgrades, Ports, das Terminal selbst und die Sonde.

Das betrifft auch zehn Dinge, die du vielleicht schon gebaut hast:

| Knoten | Stufe | Kosten | Schaltet frei |
| --- | --- | --- | --- |
| Druckanzug | 1 | {{value:node:suit_t1.cost}} FP | <item:infinite_horizon:pressure_suit_helmet>, <item:infinite_horizon:pressure_suit_chestplate>, <item:infinite_horizon:pressure_suit_leggings>, <item:infinite_horizon:pressure_suit_boots>, <item:infinite_horizon:oxygen_canister>, <item:infinite_horizon:large_oxygen_canister> |
| Gasspeicher | 1 | {{value:node:gas_storage.cost}} FP | <item:infinite_horizon:pressure_bottle> |
| Kuppel I | 2 | {{value:node:dome_t1.cost}} FP | <item:infinite_horizon:dome_generator_t1>, <item:infinite_horizon:oxygen_generator>, <item:infinite_horizon:electric_lamp> |

@note Schon gebaute Blöcke und fertige Anzüge laufen weiter. Gesperrt ist nur das Herstellen.

@warning Für die erste Reise zum Mond brauchst du den Druckanzug. Erforsche den Knoten **Druckanzug** früh, sonst baut dein Montagetisch keine neuen Anzugteile.

## Forschungspunkte

FP gehören der Agentur und werden mit zwei Nachkommastellen angezeigt. Du sammelst sie durch Erkundung und Analyse.
@if infinite_horizon:sample_tool
Wie das geht, steht im Abschnitt über Proben und den Analysator.
@endif

Auf Servern kann die Forschungspflicht abgeschaltet sein (Einstellung `research_required`). Dann sind alle Rezepte und Handbuch-Kapitel frei, und der Baum zeigt einen Hinweis.

## Herstellung

@recipes_for infinite_horizon:research_terminal

@tip Stell das Terminal gleich am Anfang auf. Es kostet nur Material, öffnet Stufe 1 und zeigt dir im Baum, was als Nächstes kommt.
