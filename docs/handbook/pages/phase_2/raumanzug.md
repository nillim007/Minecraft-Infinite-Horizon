---
title: Der Druckanzug
title_en: The Pressure Suit
order: 30
milestone: 3
requires: ["infinite_horizon:pressure_suit_chestplate"]
icon: infinite_horizon:pressure_suit_helmet
items: ["infinite_horizon:pressure_suit_helmet", "infinite_horizon:pressure_suit_chestplate", "infinite_horizon:pressure_suit_leggings", "infinite_horizon:pressure_suit_boots", "infinite_horizon:helmet_lamp"]
summary: Dein Schutz im Vakuum: vier Teile, ein Sauerstoffvorrat
---
Ohne Anzug überlebst du auf dem Mond nur Sekunden. Der **Druckanzug** ist die erste Stufe und schützt dich vor Vakuum, Hitze und Kälte, solange er Sauerstoff hat.

## Vier Teile, ein Anzug

Der Anzug wirkt nur, wenn du **alle vier Teile** trägst: <item:infinite_horizon:pressure_suit_helmet>, <item:infinite_horizon:pressure_suit_chestplate>, <item:infinite_horizon:pressure_suit_leggings> und <item:infinite_horizon:pressure_suit_boots>. Fehlt ein Teil, bist du ungeschützt.

| Schutz | Druckanzug |
| --- | --- |
| Temperatur | {{value:suits:pressure_suit.temp_min}} bis {{value:suits:pressure_suit.temp_max}} °C |
| Druck | Vakuum bis normal |
| Strahlung | bis Stufe {{value:suits:pressure_suit.radiation_max}} |
| Rüstung | wie Eisenrüstung |

Repariert wird der Anzug am Amboss mit <item:infinite_horizon:aluminum_plate>.

## Der Sauerstoffvorrat

Die Brustplatte trägt bis zu {{value:suits:oxygen_supply.chestplate_capacity}} mB Sauerstoff. Eine neue Brustplatte ist leer. Wie viel noch drin ist, zeigt der Tooltip und, sobald du draußen bist, eine Leiste im HUD.

- **Verbrauch:** {{value:suits:pressure_suit.oxygen_consumption.amount}} mB alle {{value:suits:pressure_suit.oxygen_consumption.per_ticks|t}}, aber nur außerhalb atembarer Luft.
- **Nachfüllen:** Trägst du die Brustplatte und hast eine [[sauerstoff|Sauerstoffflasche]] im Inventar, füllt sich der Vorrat von selbst aus der ersten Flasche.
- **Unter Wasser** hält der Anzug deinen Luftvorrat voll, solange er Sauerstoff hat.

@warning Ist der Sauerstoff aufgebraucht, schützt der Anzug weiter vor Druck und Temperatur, aber du erstickst. Behalte die Anzeige im Blick.

@if infinite_horizon:helmet_lamp
## Module

Jedes Anzugteil hat {{value:suits:suit_rules.module_slots_per_piece}} Modulslots. Module bauen sich im <item:infinite_horizon:assembly_table> ein und aus, Strom braucht er dafür wie für jedes andere Rezept:

1. **Einbau:** das Anzugteil und das passende Modul allein in die Eingänge legen. Heraus kommt das Teil mit eingebautem Modul.
2. **Ausbau:** das Anzugteil allein hineinlegen. Heraus kommen das Teil ohne Module und alle Module in den Nebenausgaben.

Eingebaute Module stehen im Tooltip des Teils und bleiben beim Aufwerten auf eine höhere Anzugstufe erhalten.

**Helmlampe:** Die <item:infinite_horizon:helmet_lamp> gehört in den Helm. Solange du ihn trägst, leuchtet es an deinem Kopf mit Lichtstufe {{value:items:infinite_horizon:helmet_lamp.light_level}}, auch unter Wasser. Setzt du den Helm ab, ist das Licht weg.

@recipes_for infinite_horizon:helmet_lamp

@endif
## Herstellung

@if infinite_horizon:research_terminal
@note Herstellen kann deine Agentur die Anzugteile erst, wenn sie den Knoten **Druckanzug** erforscht hat (Stufe 1, [[forschung|Forschung]]).
@endif

@recipes_for infinite_horizon:pressure_suit_helmet infinite_horizon:pressure_suit_chestplate infinite_horizon:pressure_suit_leggings infinite_horizon:pressure_suit_boots

@tip Nimm immer eine volle Flasche mehr mit, als du zu brauchen glaubst. Rechne nach: {{value:suits:infinite_horizon:oxygen_canister.capacity}} mB in einer Flasche, {{value:suits:pressure_suit.oxygen_consumption.amount}} mB Verbrauch alle {{value:suits:pressure_suit.oxygen_consumption.per_ticks|t}}.
