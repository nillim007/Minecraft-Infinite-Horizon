---
title: Der Sender
title_en: The Transmitter
order: 190
milestone: 7
requires: ["infinite_horizon:transmitter_core"]
icon: infinite_horizon:transmitter_core
items: ["infinite_horizon:ancient_alloy", "infinite_horizon:transmitter_core", "infinite_horizon:star_chart"]
summary: Eine fremde Halle unter dem Herzgletscher und ihr Wächter
---
Unter dem Herzgletscher auf [[pluto|Pluto]] liegt eine Halle aus <item:infinite_horizon:ancient_alloy>, die niemand gebaut hat, den du kennst. Die Legierung lässt sich nicht abbauen. In der Mitte steht der <item:infinite_horizon:transmitter_core>.

## Der Weg hinein

Nahe dem Landeplatz markiert ein Ring aus uralter Legierung den Eingang. Von dort führt ein Treppentunnel aus Eis hinab zu einer Öffnung in der Südwand der Halle. Wer die Halle betritt, bringt seiner Agentur Forschungspunkte als Erkundungsziel.

## Der Wächter des Senders

Rechtsklick auf den Senderkern weckt den **Wächter des Senders**, einen Boss mit {{value:creatures:infinite_horizon:signal_sentinel.hp}} Lebenspunkten:

1. Solange er mehr als {{value:creatures:infinite_horizon:signal_sentinel.phases.0.hp_to|%}} seiner Lebenspunkte hat, schlägt er zu ({{value:creatures:infinite_horizon:signal_sentinel.damage}} Schaden) und stößt alle {{value:creatures:infinite_horizon:signal_sentinel.phases.0.attacks.1.interval_ticks|t}} mit einer Schockwelle alles im Umkreis von {{value:creatures:infinite_horizon:signal_sentinel.phases.0.attacks.1.knockback_radius}} Blöcken weg.
2. Darunter ruft er {{value:creatures:infinite_horizon:signal_sentinel.phases.1.summon.count}} Frostkriecher herbei und trifft dich mit einem Strahl, wenn du zu lange in seinem Blick stehst.

Jede Agentur hat ihren eigenen Wächter. Nach einem Sieg schweigt der Sender für deine Agentur {{value:creatures:infinite_horizon:signal_sentinel.spawn.resummon_cooldown_ticks|t}}.

@warning Der Wächter ist der stärkste Gegner im Sonnensystem. Komm mit voller Rüstung, Heiltränken und Verstärkung.

## Die Sternenkarte

Besiegt lässt der Wächter die <item:infinite_horizon:star_chart> deiner Agentur fallen. Jede Agentur besitzt höchstens eine. Die Karte brennt nicht und verschwindet nicht, wenn sie herumliegt. Wird sie trotzdem zerstört, zum Beispiel in der Leere, kannst du beim nächsten Sieg eine neue erbeuten.

@tip Bewahr die Sternenkarte in einer Truhe deiner Basis auf. Mit ihr geht es später über das Sonnensystem hinaus.
