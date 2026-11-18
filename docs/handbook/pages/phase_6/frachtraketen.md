---
title: Frachtraketen und Antriebe
title_en: Cargo Rockets and Drives
order: 50
milestone: 8
requires: ["infinite_horizon:cargo_rocket"]
icon: infinite_horizon:cargo_rocket
items: ["infinite_horizon:cargo_rocket", "infinite_horizon:heavy_freighter", "infinite_horizon:atomic_drive", "infinite_horizon:antimatter_drive"]
summary: Fracht zwischen deinen Außenposten fliegen lassen
---
Frachtraketen bringen Items und Fluide von einem [[aussenposten|Frachtterminal]] zum anderen. Wann sie fliegen, legst du am [[logistik_computer|Logistik-Computer]] fest. Sie fliegen nicht sichtbar durch die Welt: Das Spiel merkt sich nur Start, Ziel und Ankunftszeit. Beim Start und bei der Landung siehst du am Terminal eine kleine Rauchwolke.

## Der Hangar

Jedes Frachtterminal hat {{value:logistics:cargo_flight_rules.hangar_slots_per_terminal}} Hangar-Slots unter den Tanks. Eine Rakete in einem Hangar-Slot gehört zu diesem Terminal; ihre Flüge starten immer dort. Solange sie unterwegs ist, bleibt das Item im Slot liegen, ist rot hinterlegt und lässt sich nicht herausnehmen.

## Die Frachtrakete

Die <item:infinite_horizon:cargo_rocket> hat {{value:logistics:infinite_horizon:cargo_rocket.item_holds}} Laderäume zu je {{value:logistics:infinite_horizon:cargo_rocket.slots_per_hold}} Slots und {{value:logistics:infinite_horizon:cargo_rocket.fluid_holds}} Fluid-Laderäume. Sie fliegt mit Methan aus ihrem eigenen Tank ({{value:logistics:infinite_horizon:cargo_rocket.fuel_tank.capacity}} mB).

- Vor jedem Start tankt sie aus dem Treibstofftank ihres Terminals nach.
- Eine Strecke kostet {{value:logistics:infinite_horizon:cargo_rocket.fuel_per_flight.base}} mB plus {{value:logistics:infinite_horizon:cargo_rocket.fuel_per_flight.per_delta_dp}} mB je Entfernungspunkt zwischen Start und Ziel.
- Am Ziel lädt sie aus und tankt für den Rückflug aus dem Treibstofftank des Ziels. Hat sie Methan geladen, behält sie davon so viel, wie der Rückflug braucht.
- Der Rückflug ist leer. Was am Ziel keinen Platz fand, bringt sie wieder mit.

@warning Reicht der Treibstoff am Ziel nicht für den Rückflug, bleibt die Rakete dort und versucht es alle {{value:logistics:cargo_flight_rules.stranded_retry_ticks|t}} erneut. Lagere am Ziel etwas Methan.

## Der Schwerlast-Frachter

Der <item:infinite_horizon:heavy_freighter> trägt doppelt so viel. Er braucht kein Methan: Ein <item:infinite_horizon:fuel_rod> reicht für {{value:logistics:infinite_horizon:heavy_freighter.fuel.flights}} Flüge hin und zurück. Ist er leer, nimmt er vor dem Start selbst einen Brennstab aus dem Lager seines Terminals. Ein Atomantrieb ist schon eingebaut.

## Antriebe

| Antrieb | Flugzeit |
| --- | --- |
| ohne | {{value:logistics:flight_time.base_ticks|t}} plus {{value:logistics:flight_time.per_delta_dp_ticks|t}} je Entfernungspunkt |
| <item:infinite_horizon:atomic_drive> | geteilt durch {{value:logistics:infinite_horizon:atomic_drive.flight_time_divisor}} |
| <item:infinite_horizon:antimatter_drive> | geteilt durch {{value:logistics:infinite_horizon:antimatter_drive.flight_time_divisor}} |

Zum Nachrüsten legst du Rakete und Antrieb zusammen in den [[montagetisch|Montagetisch]]. Treibstoff und Antrieb bleiben in der Rakete gespeichert. Es zählt nur der beste Antrieb, ein schwächerer ersetzt keinen besseren.

@tip Für den Antimaterie-Antrieb brauchst du geladene Antimaterie-Zellen aus der Magnetfalle. Leere Zellen nimmt der Montagetisch nicht an.

## Herstellung

@recipes_for infinite_horizon:cargo_rocket infinite_horizon:heavy_freighter infinite_horizon:atomic_drive infinite_horizon:antimatter_drive
