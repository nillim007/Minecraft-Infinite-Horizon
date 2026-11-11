# Sternenwerft – Spezifikation

**Version 1.1** · 10. November 2026 · Autor: Nick Monger · Grundlage: „Sternenwerft – Design-Dokument“ (9. November 2026)

Raumfahrt-Mod für Minecraft 26.3 (NeoForge + Fabric) im echten Sonnensystem: Industrie auf der Erde aufbauen, Ring für Ring hinausfliegen, ein interplanetares Frachtnetz errichten und in der Orbitalwerft das Sternenschiff bauen.

## Aufbau

| Ort | Inhalt | Rolle |
| --- | --- | --- |
| `docs/spec/*.md` | Regeln, Verhalten, Abläufe, Architektur | was und wie |
| `spec/data/*.json` | IDs, Namen, Rezepte, Forschung, Planeten, Raketenteile, Balance-Werte | **maßgebliche Zahlen und Listen** |
| `spec/data/SCHEMA.md` | Format der Datendateien | Vertrag für Ressourcen-Generator und Tools |
| `docs/spec/generated/` | Lesefassungen der Daten als Tabellen | erzeugt, nie von Hand ändern |
| `tools/validate_spec.py` | prüft Daten auf Vollständigkeit und Widersprüche | vor jedem Commit an `spec/data` |
| `tools/render_docs.py` | erzeugt `docs/spec/generated/` | nach jeder Änderung an `spec/data` |
| `docs/spec-gaps.md` | Annahmen und offene Fragen aus der Umsetzung | wird während der Umsetzung gefüllt |

## Kapitel

| Nr. | Datei | Thema |
| --- | --- | --- |
| 0 | [00-lesehinweise.md](00-lesehinweise.md) | Verbindlichkeit, Einheiten, Konventionen |
| 1 | [01-aenderungen.md](01-aenderungen.md) | Änderungen gegenüber dem Design-Dokument (Ä1–Ä25) |
| 2 | [02-glossar.md](02-glossar.md) | Begriffe, Phasen |
| 3 | [03-technik.md](03-technik.md) | Versionen, Projekt, Plattformen, Netzwerk, Config, Mixins, Ressourcen-Generator, Rezeptanzeige (3.9) |
| 3A | [03a-architektur.md](03a-architektur.md) | Paketstruktur, zentrale Klassen, gespeicherte Daten, Payloads, Datenfluss, Tests, Addon-Schnittstelle |
| 4 | [04-grundsysteme.md](04-grundsysteme.md) | Maschinen-Grundverhalten, Upgrades, Multiblöcke, Kabel und Rohre |
| 5 | [05-materialien.md](05-materialien.md) | Metalle, Items, Fluide, Verarbeitungsketten |
| 6 | [06-maschinen.md](06-maschinen.md) | Maschinen, Pressen, Generatoren und Speicher, GUI-Schlüssel (6.4) |
| 7 | [07-generatoren.md](07-generatoren.md) | Verweis auf 6.3 |
| 8 | [08-umwelt.md](08-umwelt.md) | Umweltwerte, Belastung, Atmosphäre-Regeln |
| 9 | [09-raumanzug.md](09-raumanzug.md) | Anzugstufen und Module |
| 10 | [10-kuppel.md](10-kuppel.md) | Kuppelgenerator, Sauerstoff |
| 11 | [11-forschung.md](11-forschung.md) | FP, Proben, Terminal, Stufen und Knoten |
| 12 | [12-raumfahrt.md](12-raumfahrt.md) | Ziele, Treibstoff, Sonde, Raketenbau, Flug, Landefähre, Venus, Schwerkraft |
| 13 | [13-planeten.md](13-planeten.md) | Planeten-Registry, Worldgen, Biome, Strukturen, Monster |
| 14 | [14-station.md](14-station.md) | Stationskern, Module, Schwerelosigkeit, Klon-Kammer |
| 15 | [15-reaktoren.md](15-reaktoren.md) | Verweis auf 6.3 und 4.3 |
| 16 | [16-rover.md](16-rover.md) | Rover und Module |
| 17 | [17-nahrung.md](17-nahrung.md) | Nahrung, Hydrokultur |
| 18 | [18-kreaturen.md](18-kreaturen.md) | Spawnregeln, Drops |
| 19 | [19-ereignisse.md](19-ereignisse.md) | Ereignisse, Wetter je Dimension |
| 20 | [20-logistik.md](20-logistik.md) | Außenposten, Frachtraketen, Logistik-Computer |
| 21 | [21-werft.md](21-werft.md) | Orbitalwerft, Bauabschnitte, Finale |
| 22 | [22-rezepte.md](22-rezepte.md) | Rezeptregeln |
| 23 | [23-mehrspieler.md](23-mehrspieler.md) | Agenturen, Besitz, Rechte, Handel |
| 24 | [24-handbuch-grafik.md](24-handbuch-grafik.md) | Handbuch, Atlas, Erfolge, Einstellungen, Grafik, Klang |
| 25 | [25-datenformate.md](25-datenformate.md) | Formate der Mod-Datenpakete |
| 26 | [26-registry.md](26-registry.md) | Registry-Übersicht |
| 27 | [27-meilensteine.md](27-meilensteine.md) | Meilensteine, Abnahmekriterien, Umfang und Teilschritte von M2, Balancing-Ziele |
| 28 | [28-offene-punkte.md](28-offene-punkte.md) | Entscheidungen, die nicht bei der KI liegen |
| A | [anhang-a-namen.md](anhang-a-namen.md) | Namen DE/EN (Verweis auf Daten) |
| B | [anhang-b-stimmung.md](anhang-b-stimmung.md) | Stimmung (nicht verbindlich) |

## Meilenstein-Matrix

Für einen Meilenstein liest die umsetzende KI **immer** die Kapitel 0, 2, 3, 3A und 27 und zusätzlich die Spalten unten. Kapitel früherer Meilensteine gelten weiter, müssen aber nur bei Bedarf gelesen werden.

| # | Inhalt | Kapitel | Daten (`spec/data/…`) |
| --- | --- | --- | --- |
| 1 | Grundgerüst (ohne GUI; Bedienung per Trichter) | 4.1–4.4, 6.1 (`crusher`), 6.3 (`combustion_generator`), 25.7 | `items.json`, `recipes.json` (nur benötigte Einträge), `balance/machines.json`, `balance/generators.json`, `balance/transport.json`, `balance/storage.json` |
| 2 | Erdindustrie (alle Einträge mit `phase = 1`, siehe 27.1); GUI für alle Maschinen und Generatoren (auch aus M1), Upgrade-Items aktivieren die Upgrade-Slots, Füll- und Leerslots für Eimer | 3.9, 4, 5, 6, 7, 13.4 (Erde), 22, 24.4–24.5, 27.1–27.3 | `items.json`, `recipes.json`, `planets.json` (Overworld), `balance/machines.json`, `balance/generators.json`, `balance/transport.json`, `balance/storage.json` |
| 3 | Erste Welt | 8, 9, 10, 12.9, 13.1–13.2, 13.4 (Mond), 13.5, 14.4 (Betten), 23.1 (Agentur-Kern), 25.1 | `planets.json` (Mond, Erdorbit), `balance/environment.json`, `balance/suits.json`, `balance/domes.json`, `balance/misc.json` |
| 4 | Erster Flug | 11, 12.1–12.6, 24.2, 24.3 | `research.json`, `rocket_parts.json`, `balance/spaceflight.json`, `balance/research.json` |
| 5 | Ring 1 | 12.8, 13.4 (Mars, Venus), 16, 17, 18, 19 | `planets.json`, `balance/vehicles.json`, `balance/creatures.json`, `balance/events.json`, `balance/agriculture.json` |
| 6 | Orbit | 12.7, 13.1 (Orbits), 13.3, 14, 15 | `planets.json` (Orbits), `balance/stations.json`, `balance/generators.json` (Fusion) |
| 7 | Ring 2 | 13.4 (Merkur bis Pluto), 18, 19 | `planets.json`, `balance/creatures.json`, `balance/items.json` |
| 8 | Netzwerk und Finale | 20, 21 | `balance/logistics.json`, `balance/shipyard.json`, `balance/generators.json` (Kernreaktor) |
| 9 | Feinschliff | 23, 24, 27 (Balancing) | alle |

## Werkzeuge

```bash
python tools/validate_spec.py
```

```bash
python tools/render_docs.py
```

`validate_spec.py` endet mit Code 1, wenn Fehler gefunden wurden, und listet Warnungen getrennt.
