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
