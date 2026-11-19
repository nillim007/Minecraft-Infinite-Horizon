# 21. Orbitalwerft und Rakete T3

## 21.1 Werft

- `shipyard_terminal` darf nur in einer Station in `earth_orbit` stehen. Dazu gehört mindestens ein `shipyard_dock` in derselben Station.
- `shipyard_dock`: Landeziel nur für Frachtraketen. Liefert direkt ins virtuelle Werftlager (unbegrenzt, aber nur benötigte Items/Fluide werden angenommen). Auch Rohre und Trichter am Dock liefern ein.
- Terminal-GUI: 8 Bauabschnitte mit Fortschrittsbalken und fehlenden Mengen. Meldet den Bedarf an den Logistik-Computer.
- Lieferungen werden für **alle** noch offenen Abschnitte angenommen. Ist ein Abschnitt vollständig beliefert, baut die Werft ihn in **48.000 t** (Overworld-Zeit, läuft auch offline). Gebaut wird der Reihe nach (1 → 8).
- Am Terminal sieht man ein wachsendes Gerüstmodell (pro Abschnitt eine Ausbaustufe).
- Mehrere Agenturen dürfen an derselben Werft liefern, wenn der Besitzer sie freigibt.

## 21.2 Bauabschnitte

> **Maßgebliche Daten:** `spec/data/balance/shipyard.json` (8 Abschnitte mit Material und Bauzeit).
> **Lesefassung:** `docs/spec/generated/shipyard.md`.

Abschnitte: 1 Rumpf, 2 Schilde, 3 Hauptantrieb, 4 Energie, 5 Bordcomputer, 6 Lebenserhaltung, 7 Kühlung, 8 Manövriertriebwerke. Jedes Schlüsselmaterial aus Ring 1 und Ring 2 kommt mindestens einmal vor.

## 21.3 Finale

1. Nach Abschluss von Abschnitt 8 erscheint im Terminal „Start“. Alle Agenturmitglieder in `earth_orbit` werden benachrichtigt.
2. Start: Vollbild-Sequenz von 1.200 t. Nacheinander erscheinen die `celestial_bodies`-Texturen (25.1) aller Planeten, die die Agentur besucht hat, je 1.200 t ÷ Anzahl, mit langsamem Zoom auf schwarzem Sternenhintergrund. Am Ende steht die Sonne als kleiner Punkt.
3. Der Sender auf Pluto antwortet (Ton und Text aus `assets/infinite_horizon/texts/signal_reply.txt`). Danach läuft der Abspann im Stil des Vanilla-`WinScreen` mit eigener Textdatei `assets/infinite_horizon/texts/credits.txt`. Advancement `infinite_horizon:starship` für alle Beteiligten; wer offline ist, bekommt es beim nächsten Login.
4. Danach erscheint das Entity `starship` neben der Station: 8 Sitze (4 Besatzung, 4 Passagiere). Es springt nur zwischen Orbit-Dimensionen (Sprungziel per Navigationskarte), unten kommt man mit der Landefähre weiter. Zielliste: alle Orbit-Dimensionen aus 12.1 (Sprung kostet 1 gefüllte `antimatter_cell`, keine Treibstoffprüfung) und alle Einträge aus der Registry `infinite_horizon:star_system` außer dem Sonnensystem (in v1.0 keine).

## Festlegungen aus der Umsetzung (M1–M8)

Annahmen aus der Umsetzung, die übernommen wurden. Die Nummer verweist auf den Eintrag in `docs/spec-gaps.md`, dort steht auch die Code-Stelle.

- **#115 (M8f, 21.1, 21.2, 14.2, 20.1, 20.3):** Orbitalwerft: offene Einzelheiten → Terminal lässt sich nur setzen, wo `terminal_location` passt und ein Stationskern im `station_core_radius` steht (sonst Meldung); Statuslampe: läuft = im Bau, Fehler = falscher Ort. Dock und Werft gehören zusammen, wenn derselbe Stationskern am nächsten liegt. Das Dock hat einen Eingangsslot für Trichter, der sofort ins Werftlager geht; ein Rechtsklick zeigt das Werft-GUI. Lieferungen füllen die frühesten offenen Abschnitte zuerst; ein Abschnitt baut ab dem späteren Zeitpunkt von „voll beliefert“ und „Vorgänger fertig“. Das GUI ist ein Bildschirm mit Fortschrittsbalken und Tooltip der fehlenden Mengen (kein Container-Menü). Gerüstmodell: Pfosten und Querstreben über dem Terminal, je Abschnitt 2 px höher (Blockzustand `stage`, höchstens 2 Blöcke). Sternenkarte ohne Besitzer an der Werft: jede eigene Karte. Partner-Lieferungen kommen mit M9
- **#116 (M8g, 21.3, 24.3, 24.6, 25.2):** Finale und Sternenschiff: offene Einzelheiten → Texte `signal_reply.txt` und `credits.txt` sind nicht vorgegeben: kurze eigene Texte, Deutsch als Standard, Englisch als `*.en_us.txt`. Ton der Antwort: Platzhalter (Leuchtfeuer). Der Erfolg `starship` fehlt in misc.json: erzeugt aus `finale.advancement`, Vater `own_air`, Symbol Werft-Terminal. Die Sequenz sehen die Mitglieder in der Dimension der Werft und der Startende; Planeten ohne eigene Himmelstextur erscheinen als Scheibe in ihrer Nebelfarbe; Reihenfolge wie das Besuchen. Das Sternenschiff erscheint 8 Blöcke vor dem Terminal (Blickrichtung), Besatzung = die ersten 4 Insassen; die Zelle kommt aus dem Inventar des Besatzungsmitglieds, das springt; Ankunft über der Mitte des Ziel-Orbits in derselben Höhe. Die Registry `star_system` ist noch nicht gebaut; in v1.0 gibt es ohnehin keine weiteren Systeme. Modell des Schiffs: Platzhalter aus Stationsblöcken (Feinschliff M9)
