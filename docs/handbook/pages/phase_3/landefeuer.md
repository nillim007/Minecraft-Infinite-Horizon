---
title: Das Landefeuer
title_en: The Landing Beacon
order: 60
milestone: 4
requires: ["infinite_horizon:landing_beacon"]
icon: infinite_horizon:landing_beacon
items: ["infinite_horizon:landing_beacon"]
summary: Ein Landeziel für deine Raketen
---
Das <item:infinite_horizon:landing_beacon> markiert einen Landeplatz. In der Navigationskarte ([[flug|Der Flug]]) erscheint es als Landeort für alle Mitglieder deiner Agentur.

## Aufstellen und benennen

1. Setz das Landefeuer an die Stelle, an der deine Raketen landen sollen. Es gehört der Agentur dessen, der es setzt.
2. **Rechtsklick** öffnet ein Namensfeld (bis zu {{value:spaceflight:landing_beacon.name_max_length}} Zeichen). Ohne eigenen Namen heißt es wie das am Amboss umbenannte Item oder „Landefeuer X/Z“.
3. Unter freiem Himmel zeigt ein roter Strahl bis zur Höhe {{value:spaceflight:landing_beacon.beam_top_y}}, wo es steht.

Die Navigationskarte kennt alle Landefeuer deiner Agentur, auch auf Himmelskörpern, auf denen gerade niemand ist. Baust du es ab, verschwindet es aus der Liste.

## Landen

Wählst du ein Landefeuer als Landeort, landet die Rakete höchstens {{value:spaceflight:rocket_flight.beacon_landing_radius}} Blöcke davon entfernt, aber nie auf dem Landefeuer selbst. Ist dort kein Platz, sucht sie wie üblich im weiteren Umkreis.

@tip Lass um das Landefeuer etwas Platz auf ebenem, festem Boden. Dann steht die Rakete nach der Landung direkt daneben.

## Herstellung

@recipes_for infinite_horizon:landing_beacon
