---
title: Logistik-Computer
title_en: Logistics Computer
order: 60
milestone: 8
requires: ["infinite_horizon:logistics_computer"]
icon: infinite_horizon:logistics_computer
items: ["infinite_horizon:logistics_computer"]
summary: Routen für deine Frachtraketen anlegen
---
Am <item:infinite_horizon:logistics_computer> legst du Routen für deine [[frachtraketen|Frachtraketen]] an. Die Routen gehören deiner Agentur: Jeder Logistik-Computer zeigt dieselben, egal wo er steht.

## Eine Route anlegen

1. Stell mindestens zwei [[aussenposten|Frachtterminals]] auf und leg eine Frachtrakete in einen Hangar-Slot des Start-Terminals.
2. Klick am Logistik-Computer auf „Neu“.
3. Wähle mit den Knöpfen Start und Ziel. Der Start ist immer das Terminal, in dessen Hangar die Rakete liegt.
4. Wähle die Startbedingung und trag N (und bei Bedarf X) ein.
5. Trag die Fracht ein: Item- oder Fluid-IDs mit Komma getrennt. Der Knopf „Hand“ fügt das Item in deiner Hand hinzu. Leer heißt: alles.
6. Wähle unter „Raketen“ die Hangar-Slots, deren Raketen auf dieser Route fliegen. Eine Rakete gehört immer nur zu einer Route.
7. Klick auf „Speichern“.

## Startbedingungen

| Bedingung | Die Rakete startet … |
| --- | --- |
| wenn voll | sobald die passende Fracht alle Laderäume füllt (alle Item-Slots oder alle Fluid-Laderäume) |
| alle N Ticks | spätestens N Ticks nach dem letzten Start |
| wenn Ziel weniger als N von X hat | sobald das Ziel weniger als N Stück (oder mB) von X lagert |
| bis der Bedarf gedeckt ist | solange die [[werft|Werft]] noch etwas von der Fracht braucht; sie lädt nur, was fehlt |

Ohne passende Fracht im Start-Terminal startet keine Route.

## Die Statusliste

Unter dem Editor siehst du jede Rakete der Route: unterwegs (mit Restzeit), geladen (auf dem Hinflug mit Fracht), wartend oder ohne Treibstoff. Eine Rakete ohne Treibstoff versucht es alle {{value:logistics:cargo_flight_rules.stranded_retry_ticks|t}} erneut.

@warning Eine rote Zeile warnt dich, wenn Hin- und Rückflug zusammen mehr Treibstoff brauchen, als in den Tank passt, und am Ziel kein Methan lagert. Dann bleibt die Rakete am Ziel liegen.

@tip Ist ein Außenposten gerade nicht geladen, prüft der Logistik-Computer ihn nur ab und zu. Dabei rechnet er nach, was die Produzenten in der Zwischenzeit gefördert haben.

## Herstellung

@recipes_for infinite_horizon:logistics_computer
