# Minecraft New Horizon – Sternenwerft

Raumfahrt-Mod für **Minecraft 26.3** (NeoForge und Fabric) im echten Sonnensystem: Auf der Erde eine Industrie aufbauen, Ring für Ring hinausfliegen, ein interplanetares Frachtnetz errichten und in der Orbitalwerft das Sternenschiff bauen.

## Stand

Die **Spezifikation ist fertig** (Version 1.1). Der Code entsteht als Nächstes, Meilenstein für Meilenstein nach `docs/spec/27-meilensteine.md`.

| Meilenstein | Inhalt | Stand |
| --- | --- | --- |
| 1 | Grundgerüst (Multiloader, Energie, erste Maschine) | fertig (GameTests grün auf NeoForge und Fabric; Client-Start nur manuell prüfbar) |
| 2 | Erdindustrie | offen |
| 3 | Erste Welt (Mond, Anzug, Kuppel) | offen |
| 4 | Erster Flug (Forschung, Sonde, Rakete T1) | offen |
| 5 | Ring 1 (Mars, Venus, Rover, Kreaturen) | offen |
| 6 | Orbit (Station, Fusion) | offen |
| 7 | Ring 2 (Merkur bis Pluto) | offen |
| 8 | Netzwerk und Finale (Logistik, Werft, Sternenschiff) | offen |
| 9 | Feinschliff (Mehrspieler, Handbuch, Balancing) | offen |

## Aufbau

| Ort | Inhalt |
| --- | --- |
| [`docs/spec/`](docs/spec/README.md) | Spezifikation in Kapiteln, mit Meilenstein-Matrix |
| [`docs/spec/03a-architektur.md`](docs/spec/03a-architektur.md) | Architektur: Pakete, zentrale Klassen, Datenfluss, Tests |
| [`spec/data/`](spec/data/SCHEMA.md) | maßgebliche Daten: IDs, Rezepte, Forschung, Planeten, Balance |
| [`docs/spec/generated/`](docs/spec/generated/) | lesbare Tabellen, aus den Daten erzeugt |
| [`docs/spec-gaps.md`](docs/spec-gaps.md) | offene Annahmen aus der Umsetzung |
| [`tools/`](tools/) | Prüf- und Generator-Skripte |
| [`CLAUDE.md`](CLAUDE.md) | Arbeitsanweisung für KI-Sitzungen |

## Werkzeuge

Benötigt Python 3.11 oder neuer, keine weiteren Pakete.

```bash
python tools/validate_spec.py
```

```bash
python tools/render_docs.py
```

## Voraussetzungen für den Mod-Build (ab Meilenstein 1)

- JDK 25 (z. B. Eclipse Temurin)
- Gradle kommt über den Wrapper im Projekt

## Lizenz

MIT, siehe [LICENSE](LICENSE).
