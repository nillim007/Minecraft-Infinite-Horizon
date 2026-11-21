<!-- Erzeugt von tools/gen_wiki.py aus docs/handbook/ und spec/data. Nicht von Hand ändern. -->

[📖 Wiki](README.md) › Nachschlagen › **Tipps und häufige Fragen**

# Tipps und häufige Fragen

## Tipps für den Anfang

- **Erst zerkleinern, dann schmelzen.** Das verdoppelt jedes Metall.
- **Bau Vorrat an Maschinenrahmen.** Fast jede Maschine braucht einen.
- **Behalte Titan im Kopf.** Raketen fressen Titanplatten. Merk dir gute Fundstellen tief unter der Erde.
- **Plane die Raffinerie früh.** Ohne Kunststoff gibt es keine Schaltkreise, und ohne Schaltkreise geht es nicht ins All.
- **Ein Netz, ein Zweck.** Getrennte Rohre für Wasser und Erdöl ersparen dir viel Suchen.

## Fehlersuche

**Die Lampe ist gelb und nichts passiert.** Die Maschine wartet auf Material oder Strom. Schau, ob die Energieleiste leer ist und ob die Eingabe zu einem Rezept passt.

**Die Lampe ist rot.** Die Ausgabe ist voll, oder bei der Raffinerie fehlt ein Block. Fahr im Fenster mit der Maus über die Lampe, dort steht der Grund.

**Der Montagetisch startet nicht.** Er braucht jede Zutat mindestens in der Rezeptmenge und keine fremden Items in den Slots. Siehe [Montagetisch](Montagetisch.md).

**Das Rohr transportiert nichts.** Ein Rohrnetz führt nur ein Fluid. Steckt noch ein anderes darin, wartet es, bis das Netz leer ist. Prüfe auch, ob die Quelle zu dieser Seite abgibt: die Pumpe nicht vorn, Maschinen nur über ihre Ausgangsseite (Knopf „Seite“).

**Meine Maschinen laufen langsam, obwohl der Generator voll ist.** Ein Kupferkabel trägt nur 256 J/t. Hängen zu viele Maschinen an einem Netz, teile es auf.

**Ich finde kein Titan.** Titan liegt unter y 0. Ohne Eisenspitzhacke droppt das Erz nichts.

**Ich finde kein Erdöl.** Ölpfützen gibt es nur an der Oberfläche von Wüsten und Tafelbergen. Halte nach schwarz glänzenden Mulden Ausschau. Siehe [Rohstoffe und Erze](Rohstoffe-und-Erze.md).

## Für Modpacks und Server

- Fast alle Zahlen (Verbrauch, Dauer, Durchsatz) stehen in Datenpaketen unter `data/<namespace>/infinite_horizon/balance/` und lassen sich mit `/reload` ändern.
- In `serverconfig/infinite_horizon.json` lassen sich unter anderem Rezeptmengen, Sauerstoffverbrauch und Ereignisse einstellen.

---

_Diese Seite steht auch im Spiel im **Ingenieurs-Handbuch**._

← [Erfolge](Erfolge.md)
