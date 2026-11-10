# Annahmen und offene Fragen aus der Umsetzung

Jede Annahme, die eine KI-Sitzung trifft, weil die Spezifikation etwas offen lässt, kommt hierher, zusätzlich zum `// SPEC-GAP`-Kommentar im Code. Nach jedem Meilenstein werden die Einträge geprüft: Bestätigte Annahmen wandern in die Spezifikation, verworfene werden im Code korrigiert.

## Format

| # | Meilenstein | Kapitel | Lücke | Gewählte Lösung | Code-Stelle | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | M1 | 4.4 | Beispiel: Reihenfolge der Empfänger bei gleichmäßiger Verteilung | Reihenfolge nach Blockposition, Rest im nächsten Tick | `core/grid/EnergyGrid.java` | offen |

Status: `offen` (noch nicht geprüft), `übernommen` (in Spezifikation eingearbeitet), `verworfen` (Code anpassen).

## Einträge

| # | Meilenstein | Kapitel | Lücke | Gewählte Lösung | Code-Stelle | Status |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Datenaufbau | 13.4 | Keine y-Bereiche/Häufigkeiten für `moon_ice`, Merkur-Titan, Merkur-Eisen, Merkur-Eis, `io_sulfur_ore` | Werte in `planets.json` auf `null`; vor M5/M7 festlegen | `spec/data/planets.json` | offen |
| 2 | Datenaufbau | 13.4 | Kein Abstand (`spacing_chunks`) für `great_crater`, Mars-`lava_tube`, `volcanic_vent`, `hydrothermal_field`, `large_asteroid` | `null`; vor M5/M7 festlegen | `spec/data/planets.json` | offen |
| 3 | Datenaufbau | 13.4 | Meeresspiegel Io (Lava) fehlt | `null` | `spec/data/planets.json` | offen |
| 4 | Datenaufbau | 13.1 | Orbits und Asteroidengürtel ohne eigenes Biom | `minecraft:the_void` | `spec/data/planets.json` | offen |
| 5 | Datenaufbau | 13.4 | `default_block` für Merkur, Ceres, Io, Europa, Titan, Enceladus, Triton, Pluto nicht genannt | jeweils das Hauptgestein (z. B. `mercury_rock`) | `spec/data/planets.json` | offen |
| 6 | Datenaufbau | 19 | Ereignis-IDs nicht genannt | `solar_storm`, `meteor_shower`, `dust_storm`, `acid_rain`, `volcanic_eruption`, `methane_rain`, `ring_debris` | `spec/data/items.json`, `balance/events.json` | offen |
| 7 | Datenaufbau | 6.1 | Tankgröße der Pumpstation nicht genannt | 16.000 mB | `balance/machines.json` | offen |
