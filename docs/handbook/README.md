# Handbuch und Wiki – Quelle und Format

Aus diesem Ordner entstehen **zwei Ausgaben aus einer Quelle** (Kapitel 24.1, 25.6):

| Ausgabe | Erzeugt von | Ziel |
| --- | --- | --- |
| Ingenieurs-Handbuch im Spiel | `python tools/gen_resources.py` | `common/src/generated/resources/assets/sternenwerft/handbook/…` und Übersetzungen in `lang/*.json` |
| Spieler-Wiki | `python tools/gen_wiki.py` | `docs/wiki/*.md`, veröffentlicht im GitHub-Wiki (`.github/workflows/wiki.yml`) |

Beide Ausgaben nie von Hand ändern. Die CI prüft, dass sie zur Quelle passen.

## Aufbau

```text
docs/handbook/
├─ handbook.json          Kapitel, Maschine je Rezeptart, Namen für Vanilla-IDs, Tags und Biome
├─ home.md                Einleitung der Wiki-Startseite (nur Wiki)
└─ pages/<kapitel>/<seite>.md       eine Seite; optional <seite>.en.md als englische Fassung
```

Kapitel-IDs stehen in `handbook.json` (`start`, `phase_1` … `phase_6`, `reference`). Ein Kapitel ohne sichtbare Seite fehlt in beiden Ausgaben.

## Kopf einer Seite

```markdown
---
title: Raffinerie
title_en: Refinery
order: 100
milestone: 2
requires: ["sternenwerft:refinery_controller"]
icon: sternenwerft:refinery_controller
items: ["sternenwerft:refinery_controller", "sternenwerft:plastic"]
unlock: null
targets: ["wiki", "game"]
summary: Dein erster Multiblock
---
```

| Feld | Bedeutung |
| --- | --- |
| `title`, `title_en` | Seitentitel; der Wiki-Dateiname entsteht aus `title` (oder `wiki: <Name>`) |
| `order` | Reihenfolge im Kapitel |
| `milestone` | Die Seite erscheint erst ab diesem Meilenstein (`milestone` in `common/implemented_ids.json`) |
| `requires` | Die Seite erscheint erst, wenn alle IDs registriert sind |
| `icon` | Item für Inhaltsverzeichnis und Lesezeichen im Spiel |
| `items` | IDs, die diese Seite erklärt. Links auf diese Items führen hierher; im Spiel öffnet ein Klick auf das Item die Seite |
| `unlock` | `null` oder Stufen-Tor/Forschungsknoten (25.6). Ohne Angabe gilt das `unlock` des Kapitels |
| `targets` | `wiki`, `game` oder beides (Standard) |
| `summary` | Kurzbeschreibung auf der Wiki-Startseite |

## Text

Normales Markdown in einfacher Form: Absätze, `##`-Überschriften, Listen (`-`, `1.`), Tabellen, `**fett**`.

| Schreibweise | Wirkung |
| --- | --- |
| `[[raffinerie]]`, `[[raffinerie\|Text]]` | Link auf eine Seite (Dateiname ohne `.md`, oder volle ID `sternenwerft:phase_1/raffinerie`) |
| `<item:sternenwerft:press>` | Name des Items in der jeweiligen Sprache, verlinkt auf seine Seite |
| `{{value:machines:crusher.ticks}}` | Wert aus `spec/data/balance/<datei>.json`; mit `\|%` als Prozent, mit `\|t` als Ticks mit Sekunden |
| `{{value:planet:overworld.ores.0.y_min}}` | Wert aus `planets.json` |

**Zahlen im Text immer über `{{value:…}}`**, damit Wiki und Buch bei Balance-Änderungen stimmen.

## Befehle (eigene Zeile)

| Befehl | Ausgabe |
| --- | --- |
| `@recipe <rezept-id>` | ein Rezept |
| `@recipes_for <id> …` | alle Rezepte, die eines der Items herstellen |
| `@recipes_type <typ> …` | alle Rezepte dieser Rezeptarten als Tabelle |
| `@uses <id> …` | alle Rezepte, die eines der Items als Zutat nutzen |
| `@machine <block-id>` | Steckbrief aus den Balance-Dateien (Verbrauch, Dauer, Slots, Tanks …) |
| `@ores <dimension>` | Tabelle der Erze aus `planets.json` |
| `@items` | alle registrierten Items, Blöcke und Fluide |
| `@tip …`, `@note …`, `@warning …` | Hinweiskasten |
| `@image <wiki-pfad> [<textur> <breite> <höhe>]` | Bild; im Spiel nur mit Textur |
| `@wiki <markdown>` | eine Zeile nur im Wiki |
| `@if <id> …` / `@ifnot <id> …` / `@endif` | Abschnitt nur, wenn die IDs registriert (bzw. nicht registriert) sind |

Rezepte, Steckbriefe und Tabellen zeigen nur, was registriert ist. Nicht eingebaute Inhalte bleiben unsichtbar, bis ihr Meilenstein sie einträgt.

## Ausgabe im Spiel (Vertrag für den Java-Code)

`assets/<ns>/handbook/<kapitel>/_chapter.json` beschreibt ein Kapitel, jede andere Datei dort eine Seite (Schema 25.6). Texte sind Übersetzungsschlüssel. Die übersetzten Texte können dieses Inline-Markup enthalten, das das Handbuch-GUI auswertet:

- `[[sternenwerft:phase_1/raffinerie|Text]]` → anklickbarer Link auf die Seite,
- `<item:sternenwerft:press>` → übersetzter Item-Name, anklickbar (öffnet die Seite, die das Item in `items` nennt), Tooltip mit Item,
- `**fett**`.

Rezepte sind als Anzeige-Daten eingebettet (`recipes[]`), weil der Client seit 1.21.2 nicht alle Rezepte kennt.

## Bei jedem Meilenstein

Siehe `CLAUDE.md`, Abschnitt „Handbuch und Wiki“.
