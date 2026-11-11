# 24. Handbuch, Atlas, Erfolge, Einstellungen, Klang, Grafik

## 24.1 Ingenieurs-Handbuch und Spieler-Wiki

- `engineers_handbook`: jeder Spieler bekommt es beim ersten Join, Ersatz per Werkbank (22.1).
- **Eine Quelle, zwei Ausgaben (Ä26):** Die Seiten stehen als Markdown in `docs/handbook/` (Format in `docs/handbook/README.md`). `tools/gen_resources.py` erzeugt daraus die Handbuchseiten im Spiel (Schema 25.6) samt Übersetzungsschlüsseln, `tools/gen_wiki.py` das Spieler-Wiki in `docs/wiki/`. Beide Ausgaben werden nie von Hand geändert; die CI prüft, dass sie aktuell sind, und veröffentlicht das Wiki im GitHub-Wiki.
- **Inhalt:** Ablauf jeder Phase, alle Rezepte, Steckbriefe der Maschinen und Tipps. Rezepte, Steckbriefe, Erz- und Item-Tabellen sowie Zahlen im Text (`{{value:…}}`) kommen aus `spec/data`. Seiten und Abschnitte erscheinen erst, wenn ihr Meilenstein erreicht und ihre IDs registriert sind (`common/implemented_ids.json`). So wächst das Handbuch mit jedem Meilenstein.
- **Buch-GUI:** eigener Bildschirm im Notizbuch-Stil (Blaupausen-Optik ab M9). Links ein Inhaltsverzeichnis nach Kapiteln, rechts die Seite mit Bildlauf. Kapitel nach Phasen; ein Kapitel öffnet sich mit der passenden Stufe (`unlock`, ab M4 aus der Forschung; bis dahin ist alles offen).
- **Verlinkungen:** Links `[[<seiten-id>|Text]]` im Text sind anklickbar und öffnen die Seite. `<item:<id>>` zeigt den übersetzten Item-Namen, beim Überfahren das Item als Tooltip, und öffnet per Klick die Seite, die das Item in `items` nennt. Rezeptanzeigen zeigen Items als Symbole; ein Klick auf ein Symbol öffnet ebenfalls dessen Seite. Ein Zurück-Knopf führt durch den Verlauf; die zuletzt offene Seite merkt sich der Client.
- **Aus dem Spiel ins Handbuch:** Taste (Standard `H`, änderbar) über einem Item im Inventar oder in JEI/REI/EMI öffnet das Handbuch auf der Seite dieses Items, sofern eine existiert. Item-Tooltips zeigen dann den Hinweis „H: Handbuch“.
- Seiten sind Ressourcen-Dateien unter `assets/<namespace>/handbook/` (Schema 25.6), damit andere Mods Kapitel und Seiten ergänzen können (Ressourcen aller Namespaces werden zusammengeführt).
- Suchfeld (filtert Seitentitel und Item-Namen).
- Planetenseiten füllen sich ab M4 beim ersten Besuch (Atlas-Daten der Agentur).
- **Meilensteine:** Das Buch-GUI mit Kapiteln, Links, Rezeptanzeige, Suche und Taste `H` sowie das Geschenk beim ersten Join kommen mit **M2f**. Ab dann ergänzt **jeder Meilenstein** die Seiten seiner Inhalte (27). Freischaltung nach Forschung kommt mit M4, Blaupausen-Optik und vollständige englische Texte mit M9.

## 24.2 Planetenatlas

`planetary_atlas`: Item, das jeder Spieler beim ersten Join bekommt (Ersatz per Werkbank, 22.1). Rechtsklick öffnet die GUI mit allen Zielen. Pro Ziel: Status (unbekannt, Sonde angekommen, besucht), Entdecker-Agentur, besuchte Biome, gefundene Erkundungsziele, Landefeuer. Inhalte gehören der Agentur. In M2 wird `planetary_atlas` nur registriert (Item, Rezept, Platzhalter); GUI und Geschenk beim ersten Join kommen mit M4.

## 24.3 Erfolge (Advancements, Reiter `sternenwerft:root`)

| ID | Titel DE | Bedingung |
| --- | --- | --- |
| `root` | Sternenwerft | Handbuch erhalten |
| `ground_station` | Bodenstation | `research_terminal` setzen |
| `first_signal` | Erstes Signal | Sonde erreicht `moon` |
| `small_step` | Ein kleiner Schritt | `moon` betreten |
| `red_earth` | Rote Erde | `mars` betreten |
| `three_minutes_of_hell` | Drei Minuten Hölle | `venus` unter y 180 betreten und danach über y 180 zurückkehren |
| `own_air` | Eigene Luft | ersten Kuppelgenerator aktivieren |
| `home_in_orbit` | Zuhause im Orbit | `station_core` setzen |
| `sun_in_a_bottle` | Sonne in der Flasche | Fusionsreaktor erzeugt Strom |
| `under_the_ice` | Unter dem Eis | `europa` unter y 112 im Wasser |
| `at_the_edge` | Am Rand | `pluto` betreten |
| `the_signal` | Der Sender | `signal_sentinel` besiegen |
| `network` | Netzwerk | 5 Routen der Agentur gleichzeitig aktiv |
| `starship` | Sternenwerft | Rakete T3 starten |

Erfolge, die eine Maschine oder ein Ereignis auslöst (`first_signal`, `own_air`, `sun_in_a_bottle`, `network`), bekommen alle Mitglieder der Besitzer-Agentur, wer offline ist beim nächsten Login. Eine Route gilt für `network` als aktiv, wenn ihr mindestens 1 Rakete zugewiesen ist und keine davon „ohne Treibstoff“ meldet.

Jeder Erfolg wird mit Zeitstempel in `logs/sternenwerft-progress.log` geschrieben (lokal, keine Telemetrie).

## 24.4 Einstellungen (`serverconfig/sternenwerft.json`)

| Schlüssel | Standard | Bereich |
| --- | --- | --- |
| `recipe_amount_multiplier` | 1.0 | 0.25–4.0 (gilt nur für Item-Zutaten mit Stapelgröße > 1 in Montagetisch-Rezepten; rundet auf, min. 1; ein Rezept wird nur so weit vergrößert, dass es noch in 9 Slots passt. Fluide, unstapelbare Zutaten, Pressformen und Werftabschnitte bleiben unverändert) |
| `oxygen_consumption` | 1.0 | 0.5–2.0 |
| `event_frequency` | `normal` | `off`, `rare`, `normal`, `frequent` |
| `meteor_block_damage` | true | |
| `planet_mobs` | true | |
| `outposts_offline` | true | |
| `outpost_offline_factor` | 0.5 | 0.0–1.0 |
| `research_required` | true | |

`recipe_amount_multiplier` gilt ab M2 (22.3). `research_required` wird in M2 gelesen, wirkt aber erst ab M4; bis dahin ist nichts gesperrt (3A.3).

## 24.5 Grafik

- Vanilla-nah: 16×16-Texturen, Blockmodelle, klare Farben.
- **Platzhalter-Grundfarben nach Phase:** Phase 1 `#6E6E6E` (Stahlgrau) mit Gelb-Schwarz-Akzent; Phase 2–3 `#E8E8E8` (Weiß) mit Orange `#E07B20`; Phase 4–5 `#3A3F47` (Dunkelgrau) mit Kühlblau `#4FB3E8`; Phase 6 `#1A1420` (Schwarz) mit Violett `#9B4FE8`.
- Planetenblöcke (Grundfarbe): Mond `#8C8C8C`, Mond-Basalt `#3A3A3A`, Mars `#B5502A`, Gips `#E6DFD0`, Trockeneis `#F0F4F8`, Venus `#3B2F2A`, Merkur `#5A5550`, Asteroiden `#6B6B6B`, Ceres `#7A776F`, Salzkruste `#E8E8E0`, Io `#E8C440`, Europa-Eis `#E8EEF2` mit Rissen `#8A5A3A`, Titan `#A0622A`, Enceladus `#F4F8FF`, Triton `#C9A4A4`, Pluto-Eis `#D8C8A8`, Tholin `#5A3A2A`.
- Erze: Gesteinsfarbe mit Flecken der Metallfarbe (Aluminium `#C8CCD0`, Titan `#8A94A6`, Magnesium `#D8D8C0`, Nickel `#A8A890`, Wolfram `#4A4E58`, Lithium `#F0E8F0`, Platin `#E0E0E8`, Iridium `#B0C0D8`, Uran `#7AC040`, Schwefel `#E8D020`).
- Kabel: Kupfer orange, Aluminium silbern, Supraleiter hellblau. Rohre in der Fluidfarbe aus 5.3.
- **Metall- und Materialfarben** (aus `tools/gen_placeholders.py`; Metallformen sind Rohform, Staub, Barren, Platte, Stange und Draht der Kategorie `material`):

| Material | Farbe |
| --- | --- |
| Eisen | `#D8D0C8` |
| Kupfer | `#D88050` |
| Gold | `#F0D040` |
| Bauxit (Rohform von Aluminium) | `#B07850` |
| Aluminium, Titan, Magnesium, Nickel, Wolfram, Lithium, Platin, Iridium, Uran, Schwefel | wie bei den Erzen oben |
| `quartz_sand` | `#E8DCB0` |

| Gruppe | Grundfarbe | Akzent |
| --- | --- | --- |
| Metallformen | Metallfarbe | Metallfarbe × 1,25 |
| `quartz_sand` | `#E8DCB0` | `#E8DCB0` |
| Fluidtexturen | Fluidfarbe aus `items.json` (5.3) | – |
| Eimer | Phasenfarbe, Inhalt in der Fluidfarbe | Phasen-Akzent |
| alle übrigen Items und Blöcke (`silicon`, `plastic`, Bauteile, Maschinen, Generatoren, Speicher, Kabel, Rohre, Ports, Gehäuse, Upgrades, Pressformen) | Phasenfarbe | Phasen-Akzent |

  Der Rand ist die Grundfarbe × 0,6, dazu kommt Helligkeitsrauschen von ± 10 %. Items haben 2 px transparenten Rand.
- Statuslampe als kleines Overlay oben auf jeder Maschine.

## 24.6 Klang

| Sound-Event | Verwendung |
| --- | --- |
| `sternenwerft:suit.breath` | Schleife im Vakuum ohne Kuppel |
| `sternenwerft:suit.radio_static` | leise Schleife im Vakuum |
| `sternenwerft:warning.oxygen`, `.heat`, `.cold`, `.radiation`, `.dome` | eigene, klar unterscheidbare Signaltöne |
| `sternenwerft:rocket.countdown`, `.launch`, `.landing` | Raketen |
| `sternenwerft:machine.<id>` | Maschinengeräusch (Schleife bei `running`) |
| `sternenwerft:music.<planet>` | ruhige Hintergrundmusik je Planet (Platzhalter: Vanilla-Musik `minecraft:music.game`) |

Im Vakuum ohne Kuppel werden alle Geräusche außer Anzug- und Warntönen mit Lautstärke × 0,15 abgespielt.
