---
title: Sauerstoff speichern
title_en: Storing Oxygen
order: 40
milestone: 3
requires: ["infinite_horizon:oxygen_canister"]
icon: infinite_horizon:oxygen_canister
items: ["infinite_horizon:oxygen_canister", "infinite_horizon:large_oxygen_canister", "infinite_horizon:pressure_bottle"]
summary: Flaschen für unterwegs, Druckflaschen als Lager
---
Sauerstoff entsteht im <item:infinite_horizon:electrolyzer> oder im [[sauerstoffgenerator|Sauerstoffgenerator]]. Damit du ihn mitnehmen und lagern kannst, gibt es Flaschen und Druckflaschen.

## Sauerstoffflaschen

| Flasche | Inhalt |
| --- | --- |
| <item:infinite_horizon:oxygen_canister> | {{value:suits:infinite_horizon:oxygen_canister.capacity}} mB |
| <item:infinite_horizon:large_oxygen_canister> | {{value:suits:infinite_horizon:large_oxygen_canister.capacity}} mB |

- **Füllen:** Leg eine Flasche in den **Füllslot** einer Maschine mit Sauerstofftank, etwa Elektrolyseur, Sauerstoffgenerator oder Druckflasche. Sie füllt sich sofort mit so viel, wie da ist.
- **Leeren:** Im **Leerslot** gibt die Flasche ihren Inhalt in den Tank ab.
- **Unterwegs:** Flaschen im Inventar füllen deinen [[raumanzug|Druckanzug]] von selbst nach.

Den Inhalt zeigt der Tooltip. Flaschen nehmen nur Sauerstoff.

## Die Druckflasche

Die <item:infinite_horizon:pressure_bottle> ist ein Tank nur für Gase und fasst {{value:storage:pressure_bottle.capacity}} mB. Sie hat wie der Fluidtank eine Ausgangsseite und Füll- und Leerslots. Abgebaut behält sie ihren Inhalt.

@machine infinite_horizon:pressure_bottle

## Herstellung

@if infinite_horizon:research_terminal
@note Die Flaschen schaltet der Knoten **Druckanzug** frei, die Druckflasche der Knoten **Gasspeicher** (beide Stufe 1, [[forschung|Forschung]]).
@endif

@recipes_for infinite_horizon:oxygen_canister infinite_horizon:large_oxygen_canister infinite_horizon:pressure_bottle

@tip Eine Druckflasche neben dem Sauerstoffgenerator sammelt seinen Überschuss. So hast du immer einen Vorrat für neue Flaschen.
