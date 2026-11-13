---
title: Der Hitzeanzug
title_en: The Heat Suit
order: 105
milestone: 5
requires: ["infinite_horizon:heat_suit_chestplate"]
icon: infinite_horizon:heat_suit_helmet
items: ["infinite_horizon:heat_suit_helmet", "infinite_horizon:heat_suit_chestplate", "infinite_horizon:heat_suit_leggings", "infinite_horizon:heat_suit_boots"]
summary: Anzug der Stufe 2 für die Venus: Hitze, hoher Druck, Säureregen
---
Der **Hitzeanzug** ist die zweite Anzugstufe. Du baust ihn aus deinem [[raumanzug|Druckanzug]]: Jedes Teil wird im Montagetisch aufgewertet und behält dabei seinen Sauerstoff und seine Module.

## Schutz

- **Temperatur:** von {{value:suits:heat_suit.temp_min}} °C bis {{value:suits:heat_suit.temp_max}} °C.
- **Druck:** auch hoher Druck wie auf der Venus.
- **Sauerstoff:** wie beim Druckanzug aus der Brustplatte, Verbrauch {{value:suits:heat_suit.oxygen_consumption.amount}} mB alle {{value:suits:heat_suit.oxygen_consumption.per_ticks|t}}.

## Die Hitzezeit

Auf der Oberfläche der Venus, unterhalb der Wolkenschicht (y {{value:suits:venus_heat_timer.applies_below_y}}), hält auch der Hitzeanzug nicht ewig durch. Eine eigene Leiste im HUD zeigt die **Hitzezeit**: {{value:suits:venus_heat_timer.duration_ticks|t}}. Ist sie abgelaufen, schützt der Anzug nicht mehr vor Hitze.

Oberhalb der Wolkenschicht füllt sich die Hitzezeit in {{value:suits:venus_heat_timer.refill_ticks|t}} wieder auf. Fahr also rechtzeitig mit dem [[venus|Wolkenaufzug]] nach oben.

@warning Die Hitzezeit läuft auch, wenn du nur kurz hinunter willst. Prüf vor jedem Abstieg die Leiste.

## Herstellung

@recipes_for infinite_horizon:heat_suit_helmet infinite_horizon:heat_suit_chestplate infinite_horizon:heat_suit_leggings infinite_horizon:heat_suit_boots

@tip Das <item:infinite_horizon:cooling_system> in der Brustplatte braucht flüssiges Helium, siehe [[helium|Helium und Kryotechnik]].
