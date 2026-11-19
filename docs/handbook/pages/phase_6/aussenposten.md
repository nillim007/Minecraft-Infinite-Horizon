---
title: Außenposten und Frachtterminal
title_en: Outposts and Cargo Terminal
order: 40
milestone: 8
requires: ["infinite_horizon:cargo_terminal"]
icon: infinite_horizon:cargo_terminal
items: ["infinite_horizon:cargo_terminal"]
summary: Ein Terminal sammelt, was deine Produzenten fördern, auch wenn du weit weg bist
---
Ein Außenposten ist ein <item:infinite_horizon:cargo_terminal> mit allen Produzenten im Umkreis von {{value:logistics:outpost.radius}} Blöcken. Produzenten sind [[bohrturm|Bohrturm und Pumpstation]], der Gassammler und die Magnetfalle. Sie geben ihre Ausbeute direkt ins Terminal, du musst nichts verbinden.

## Das Frachtterminal

Das Terminal hat {{value:machines:infinite_horizon:cargo_terminal.item_slots.storage}} Lagerslots. Mit dem Mausrad oder der Leiste neben dem Lager blätterst du durch alle Zeilen. Rechts stehen die Tanks: ganz links der Treibstofftank, daneben die Frachttanks.

- Der Treibstofftank nimmt nur Methan auf. Eingehendes Methan füllt immer zuerst ihn, erst der Rest geht in die Frachttanks.
- Ein Klick auf eine Tankleiste wählt den Tank, aus dem der Füllslot leere Behälter füllt.
- Trichter und Rohre dürfen Items hinein- und herausholen. Über seine Ausgangsseite gibt das Terminal nur Fracht ab, nie den Treibstoff.

@tip Gehört ein Produzent zu zwei Terminals, liefert er immer an das nähere.

@if infinite_horizon:trading_post
Der Knopf „Partner“ im Terminal gibt es für deine [[agentur|Partner]] frei. Dann dürfen deren Frachtraketen hierher liefern. Abholen können sie nichts.
@endif

## Produktion, während du fort bist

Ist der Außenposten nicht geladen, weil du auf einem anderen Planeten bist, rechnet das Spiel die Förderung nach, sobald du zurückkommst. Du bekommst dann das {{value:logistics:offline_production.factor}}-Fache der normalen Menge für die vergangene Zeit.

- Es zählt nur ein Produzent, der beim Verlassen lief. Strom wird dabei nicht gerechnet, also sorg vorher für eine sichere Versorgung, zum Beispiel mit dem [[kernreaktor|Kernreaktor]].
- Die Förderung endet, wenn das Terminal voll ist oder die Eingaben fehlen, etwa leere Antimaterie-Zellen in der Magnetfalle.

@warning Auf Servern kann die Offline-Produktion abgeschaltet sein (Einstellung `outposts_offline`).

## Herstellung

@recipes_for infinite_horizon:cargo_terminal
