---
title: Der Asteroidengürtel
title_en: The Asteroid Belt
order: 40
milestone: 7
requires: ["infinite_horizon:asteroid_rock"]
icon: infinite_horizon:asteroid_platinum_ore
items: ["infinite_horizon:asteroid_rock", "infinite_horizon:asteroid_platinum_ore", "infinite_horizon:asteroid_iridium_ore", "infinite_horizon:asteroid_nickel_ore", "infinite_horizon:asteroid_anchor", "infinite_horizon:platinum_dust", "infinite_horizon:platinum_ingot", "infinite_horizon:iridium_dust", "infinite_horizon:iridium_ingot", "infinite_horizon:iridium_chip"]
summary: Schwebende Felsbrocken voller Platin, Iridium und Nickel
---
Im Asteroidengürtel gibt es keinen Boden, nur Felsbrocken im leeren Raum. Die Schwerkraft beträgt {{value:planet:asteroid_belt.gravity}} g: Ein Sprung trägt dich weit, und wer danebenspringt, treibt davon.

## Asteroiden

Die kleinen Asteroiden haben einen Radius von {{value:planet:asteroid_belt.features.0.params.radius.0}} bis {{value:planet:asteroid_belt.features.0.params.radius.1}} Blöcken, dazu gibt es große mit {{value:planet:asteroid_belt.structures.0.params.radius.0}} bis {{value:planet:asteroid_belt.structures.0.params.radius.1}} Blöcken. Sie bestehen aus <item:infinite_horizon:asteroid_rock>, darin stecken Erze:

- <item:infinite_horizon:asteroid_platinum_ore> ({{value:planet:asteroid_belt.ores.0.fraction|%}} der Blöcke),
- <item:infinite_horizon:asteroid_iridium_ore> ({{value:planet:asteroid_belt.ores.1.fraction|%}}),
- <item:infinite_horizon:asteroid_nickel_ore> ({{value:planet:asteroid_belt.ores.2.fraction|%}}).

Beim Spawn bei X/Z 0/0 schwebt immer ein großer Asteroid mit allen drei Erzen.

@warning Fällst du unter Höhe {{value:planet:asteroid_belt.special.void_reset.below_y}}, holt dich die Navigation mit Langsamem Fall auf den Spawn-Asteroiden zurück. Was du dabei fallen lässt, ist verloren.

## Platin und Iridium

Platin und Iridium verarbeitest du wie andere Erze: Zerkleinerer und Elektroofen. Der <item:infinite_horizon:iridium_chip> entsteht im [[fertigung_im_orbit|Schwerelos-Fertiger]] und steckt in vielen Geräten der fünften Phase.

@recipes_for infinite_horizon:platinum_ingot infinite_horizon:iridium_ingot infinite_horizon:iridium_chip

## Asteroidenanker

Der <item:infinite_horizon:asteroid_anchor> wird auf einen Asteroiden gesetzt. Im Umkreis von {{value:items:infinite_horizon:asteroid_anchor.radius}} Blöcken gilt dann für Spieler {{value:items:infinite_horizon:asteroid_anchor.gravity_g}} g: So baust du eine Basis, ohne davonzuschweben.

@recipes_for infinite_horizon:asteroid_anchor

## Driftmilben

Um Asteroiden mit Platinerz schweben Schwärme von Driftmilben. Sie sind harmlos und zeigen dir, wo Platin steckt. Sie entstehen nur mit der Welt und kommen nicht wieder.

@tip Folge einem Driftmilbenschwarm: Er verrät den Asteroiden mit Platin, noch bevor du ihn abbaust.
