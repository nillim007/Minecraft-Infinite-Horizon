---
title: Grundbegriffe
title_en: Basics
order: 15
milestone: 1
icon: infinite_horizon:copper_cable
summary: Strom, Ticks, Millibuckets, Statuslampe – alles in fünf Minuten
---
Diese Seite erklärt die wenigen Begriffe, die überall im Handbuch vorkommen. Mehr musst du nicht wissen, um loszulegen.

## Ticks und Sekunden

Minecraft rechnet in **Ticks (t)**. Eine Sekunde hat 20 Ticks. Wenn eine Maschine „200 t (10 s)“ braucht, dauert ein Vorgang zehn Sekunden.

## Strom: J und J/t

- Strom wird in **Joule (J)** gemessen.
- **J/t** heißt „Joule pro Tick“. Ein Generator mit 20 J/t erzeugt in jedem Tick 20 J, eine Maschine mit 20 J/t verbraucht so viel, solange sie arbeitet.
- Strom fließt durch **Kabel** von Generatoren zu Maschinen. Jedes Kabel hat einen **Durchsatz**: So viel J/t kann es höchstens tragen.
- Jede Maschine hat einen kleinen **Puffer**. Fehlt kurz Strom, pausiert sie und macht danach weiter. Es geht nichts verloren.

Mehr dazu unter [[strom|Strom]].

## Fluide: mB

Flüssigkeiten und Gase heißen **Fluide**. Sie werden in **Millibuckets (mB)** gemessen: 1.000 mB sind genau ein Eimer. Fluide wandern durch **Rohre** und lagern in **Tanks**. Mehr dazu unter [[fluide|Fluide und Rohre]].

## Die Statuslampe

Jede Maschine hat eine kleine Lampe:

- **Grün:** arbeitet.
- **Gelb:** wartet auf Material oder Strom.
- **Rot:** Problem. Die Ausgabe ist voll, ein Aufbau ist unvollständig oder die Maschine steht am falschen Ort. Im Fenster verrät der Tooltip der Lampe den Grund.

## Ein- und Ausgänge

- Maschinen nehmen Items und Fluide **von allen Seiten** an. Trichter und Rohre kannst du also anschließen, wo du willst.
- Fluide und gespeicherten Strom geben Maschinen und Speicher nur über **eine Seite** ab, die **Ausgangsseite**. Du wählst sie im Fenster mit dem Knopf „Seite“.

## Multiblöcke

Manche Maschinen sind so groß, dass sie aus vielen Blöcken bestehen, zum Beispiel die [[raffinerie|Raffinerie]]. Du setzt einen **Controller** und baut aus **Gehäuseblöcken** die vorgegebene Form drumherum. **Ports** ersetzen einzelne Gehäuseblöcke und sind die Anschlüsse für Rohre, Kabel und Trichter.

## Phasen

Infinite Horizon ist in sechs **Phasen** gegliedert, von der Industrie auf der Erde bis zum Sternenschiff. Jede Phase hat im Handbuch ein eigenes Kapitel mit einer Seite „Erste Schritte“. Ab Phase 2 schaltest du neue Technik mit **Forschung** frei.

@tip Wenn du nur eins behältst: **Grün arbeitet, gelb wartet, rot hat ein Problem.** Ein Blick auf die Lampen zeigt dir sofort, wo deine Fabrik hakt.
