# 24. Handbuch, Atlas, Erfolge, Einstellungen, Klang, Grafik

## 24.1 Ingenieurs-Handbuch

- `engineers_handbook`: jeder Spieler bekommt es beim ersten Join, Ersatz per Werkbank (22.1).
- Eigenes Buch-GUI im Notizbuch-Stil mit Blaupausen-Optik. Kapitel nach Phasen. Ein Kapitel öffnet sich mit der passenden Stufe.
- Seiten sind Ressourcen-Dateien unter `assets/<namespace>/handbook/` (Schema 25.6), damit andere Mods Kapitel ergänzen können.
- Maschinenseiten werden automatisch aus Rezept, Verbrauch und Kapitel 6 erzeugt. Planetenseiten füllen sich beim ersten Besuch.
- Suchfeld (filtert Seitentitel und Item-Namen).

## 24.2 Planetenatlas

`planetary_atlas`: Item, das jeder Spieler beim ersten Join bekommt (Ersatz per Werkbank, 22.1). Rechtsklick öffnet die GUI mit allen Zielen. Pro Ziel: Status (unbekannt, Sonde angekommen, besucht), Entdecker-Agentur, besuchte Biome, gefundene Erkundungsziele, Landefeuer. Inhalte gehören der Agentur.

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

## 24.5 Grafik

- Vanilla-nah: 16×16-Texturen, Blockmodelle, klare Farben.
- **Platzhalter-Grundfarben nach Phase:** Phase 1 `#6E6E6E` (Stahlgrau) mit Gelb-Schwarz-Akzent; Phase 2–3 `#E8E8E8` (Weiß) mit Orange `#E07B20`; Phase 4–5 `#3A3F47` (Dunkelgrau) mit Kühlblau `#4FB3E8`; Phase 6 `#1A1420` (Schwarz) mit Violett `#9B4FE8`.
- Planetenblöcke (Grundfarbe): Mond `#8C8C8C`, Mond-Basalt `#3A3A3A`, Mars `#B5502A`, Gips `#E6DFD0`, Trockeneis `#F0F4F8`, Venus `#3B2F2A`, Merkur `#5A5550`, Asteroiden `#6B6B6B`, Ceres `#7A776F`, Salzkruste `#E8E8E0`, Io `#E8C440`, Europa-Eis `#E8EEF2` mit Rissen `#8A5A3A`, Titan `#A0622A`, Enceladus `#F4F8FF`, Triton `#C9A4A4`, Pluto-Eis `#D8C8A8`, Tholin `#5A3A2A`.
- Erze: Gesteinsfarbe mit Flecken der Metallfarbe (Aluminium `#C8CCD0`, Titan `#8A94A6`, Magnesium `#D8D8C0`, Nickel `#A8A890`, Wolfram `#4A4E58`, Lithium `#F0E8F0`, Platin `#E0E0E8`, Iridium `#B0C0D8`, Uran `#7AC040`, Schwefel `#E8D020`).
- Kabel: Kupfer orange, Aluminium silbern, Supraleiter hellblau. Rohre in der Fluidfarbe aus 5.3.
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
