---
title: Maschinen bedienen
title_en: Using Machines
order: 20
milestone: 2
requires: ["infinite_horizon:crusher"]
summary: GUI, Statuslampe, Ausgangsseite, Trichter und Upgrades
---
Alle Maschinen von Infinite Horizon funktionieren nach denselben Regeln. Wer sie einmal verstanden hat, kommt mit jeder neuen Maschine sofort klar.

## Das Fenster

Rechtsklick auf eine Maschine öffnet ihr Fenster:

- **Links außen** die Energieleiste: So viel Strom hat die Maschine gespeichert.
- **Links** die Eingabeslots, **rechts** die Ausgabeslots, dazwischen der Fortschrittspfeil.
- **Rechts außen** die Fluidleisten, falls die Maschine Tanks hat. Darunter liegen Füll- und Leerslot für Eimer.
- **Unten rechts** die zwei Upgrade-Slots (siehe [[upgrades|Upgrades]]).
- Der Knopf **„Seite“** wählt, auf welcher Seite die Maschine Fluide abgibt. Der Knopf **„Tank“** wählt bei mehreren Ausgangstanks, welcher in den Eimer gefüllt wird.

## Die Statuslampe

Jede Maschine hat oben eine kleine Lampe. Sie verrät dir auf einen Blick, was los ist:

- **Grün** – die Maschine arbeitet.
- **Gelb** – sie wartet auf Material oder Strom.
- **Rot** – etwas stimmt nicht: Die Ausgabe ist voll, die Maschine steht am falschen Ort oder ein Aufbau ist unvollständig. Fahr mit der Maus über die Lampe im Fenster, dann steht dort der Grund.

## Automatisieren mit Trichtern und Rohren

- Items und Fluide kommen **von jeder Seite** in die Eingabe. Trichter füllen immer den ersten passenden Slot.
- Herausnehmen lassen sich von jeder Seite **nur die Ergebnisse**. So kannst du Maschinen einfach hintereinanderschalten.
- Fluide gibt eine Maschine nur über ihre **Ausgangsseite** ab (Knopf „Seite“). Diese Seite nimmt selbst nichts an. So fließt nichts zurück in das Rohr, das die Maschine versorgt.
- Shift-Klick im Fenster legt Upgrades in die Upgrade-Slots und Pressformen in den Formslot, alles andere in die Eingabe.

## Abbauen

Maschinen baust du mit einer **Spitzhacke** ab (mindestens Stein). Sie droppen sich selbst, Items im Inneren fallen heraus. Energie und Fluide gehen dabei verloren. Nur der <item:infinite_horizon:fluid_tank> behält seinen Inhalt.

@tip Strom verbraucht eine Maschine nur, solange sie wirklich arbeitet. Eine wartende Maschine kostet nichts. Du kannst also ruhig mehrere Maschinen an einen Generator hängen.
