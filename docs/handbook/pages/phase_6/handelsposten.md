---
title: Handelsposten
title_en: Trading Post
order: 65
milestone: 9
requires: ["infinite_horizon:trading_post"]
icon: infinite_horizon:trading_post
items: ["infinite_horizon:trading_post"]
summary: Tauschen mit anderen Agenturen, auch wenn niemand online ist
---
Am <item:infinite_horizon:trading_post> bietest du anderen Spielern Tauschgeschäfte an: „Gib mir X, du bekommst Y“. Er hat bis zu {{value:logistics:infinite_horizon:trading_post.offers_max}} Angebote und ein eigenes Lager mit {{value:logistics:infinite_horizon:trading_post.storage_slots}} Slots. Tauschen darf **jeder**, auch Spieler fremder Agenturen. Lager und Angebote verwalten nur die Mitglieder deiner Agentur.

## Angebote einrichten

1. Stell den Handelsposten auf und öffne ihn mit Rechtsklick. Links stehen die Angebote, rechts das Lager.
2. Leg die Ware, die du hergeben willst, ins Lager.
3. Klick in einer Angebotszeile mit einem Item auf den **linken** Musterslot: Das ist X, was der Händler dir gibt. Ein Linksklick übernimmt den ganzen Stapel, ein Rechtsklick zählt ein Stück hoch.
4. Klick genauso auf den **rechten** Musterslot: Das ist Y, was der Händler dafür bekommt.
5. Linksklick ohne Item leert einen Musterslot, Rechtsklick ohne Item zählt ein Stück herunter.

Die Muster sind nur Vorlagen. Getauscht wird immer echte Ware aus dem Lager: Neben jedem Angebot steht, wie oft es noch geht.

@tip Was Händler dir geben, landet im Lager. Hol es regelmäßig ab, sonst ist das Lager irgendwann voll und niemand kann mehr tauschen.

## Tauschen

Leg X ins Inventar und klick beim Angebot auf „Tauschen“. Der Handelsposten nimmt X aus deinem Inventar, legt es ins Lager und gibt dir Y. Spieler fremder Agenturen sehen das Lager nicht, nur die Angebote.

## Beliefern

Frachtraketen deiner Agentur und deiner [[agentur|Partner]] können den Handelsposten am [[logistik_computer|Logistik-Computer]] als Ziel wählen. Sie laden nur Items ab. Fluide bleiben in der Rakete und fliegen zurück.

@warning Ein Handelsposten ist nie Startpunkt einer Route. Er hat keinen Hangar und keinen Treibstofftank: Plane den Rückflug mit genug Treibstoff ab dem Start-Terminal.

## Herstellung

@recipes_for infinite_horizon:trading_post
