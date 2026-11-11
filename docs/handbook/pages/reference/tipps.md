---
title: Tipps und häufige Fragen
title_en: Tips and FAQ
order: 30
milestone: 1
icon: sternenwerft:speed_upgrade
summary: Kniffe, Fehlersuche und Antworten
---
## Tipps für den Anfang

- **Erst zerkleinern, dann schmelzen.** Das verdoppelt jedes Metall.
- **Bau Vorrat an Maschinenrahmen.** Fast jede Maschine braucht einen.
- **Behalte Titan im Kopf.** Raketen fressen Titanplatten. Merk dir gute Fundstellen tief unter der Erde.
- **Plane die Raffinerie früh.** Ohne Kunststoff gibt es keine Schaltkreise, und ohne Schaltkreise geht es nicht ins All.
- **Ein Netz, ein Zweck.** Getrennte Rohre für Wasser und Erdöl ersparen dir viel Suchen.

## Fehlersuche

**Die Lampe ist gelb und nichts passiert.**
Die Maschine wartet auf Material oder Strom. Schau, ob die Energieleiste leer ist und ob die Eingabe zu einem Rezept passt.

**Die Lampe ist rot.**
Die Ausgabe ist voll, oder bei der Raffinerie fehlt ein Block. Fahr im Fenster mit der Maus über die Lampe, dort steht der Grund.

**Der Montagetisch startet nicht.**
Er braucht die Zutaten in genau der richtigen Menge. Nimm Überschuss heraus. Siehe [[montagetisch|Montagetisch]].

**Das Rohr transportiert nichts.**
Ein Rohrnetz führt nur ein Fluid. Steckt noch ein anderes darin, wartet es, bis das Netz leer ist. Prüfe auch die Ausgangsseite der Quelle (Knopf „Seite“).

**Meine Maschinen laufen langsam, obwohl der Generator voll ist.**
Ein Kupferkabel trägt nur {{value:transport:copper_cable.throughput}} J/t. Hängen zu viele Maschinen an einem Netz, teile es auf.

**Ich finde kein Titan.**
Titan liegt unter y 0. Ohne Eisenspitzhacke droppt das Erz nichts.

**Ich finde kein Erdöl.**
Ölblasen gibt es nur unter Wüsten und Ozeanen. Erkunde Höhlen in diesen Biomen. Siehe [[rohstoffe|Rohstoffe und Erze]].

## Für Modpacks und Server

- Fast alle Zahlen (Verbrauch, Dauer, Durchsatz) stehen in Datenpaketen unter `data/<namespace>/sternenwerft/balance/` und lassen sich mit `/reload` ändern.
- In `serverconfig/sternenwerft.json` lassen sich unter anderem Rezeptmengen, Sauerstoffverbrauch und Ereignisse einstellen.
