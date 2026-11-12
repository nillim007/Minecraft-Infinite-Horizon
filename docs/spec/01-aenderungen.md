# 1. Änderungen gegenüber dem Design-Dokument

Diese Entscheidungen lösen Widersprüche und Sackgassen. **Bitte prüfen**, bevor die Umsetzung beginnt.

| # | Problem im Design-Dokument | Lösung in dieser Spezifikation |
| --- | --- | --- |
| Ä1 | Anzug T2 brauchte Wolfram, Wolfram gibt es nur auf der Venus-Oberfläche, dafür braucht man Anzug T2 | Anzug T2 nutzt **Keramikplatten** (Basalt + Aluminiumstaub im Hochtemperaturofen). Säureschutz bleibt. |
| Ä2 | Kühlsystem (Stufe 2) brauchte flüssiges Helium, der Kryo-Verflüssiger kam erst in Stufe 3 | **Kryo-Verflüssiger nach Stufe 2** verschoben. Das Kühlsystem braucht 1.000 mB statt eines ganzen Tanks flüssiges Helium. |
| Ä3 | Anzug T3 brauchte Isoliergewebe vom Frostkriecher (Triton, nur mit T3 betretbar) | Isoliergewebe ist **herstellbar** (Kunststoff, Silizium, Lithium). Der Frostkriecher lässt es zusätzlich fallen. |
| Ä4 | Der Boss sollte optional sein, die Sternenkarte aber Pflicht | Der Wächter des Senders ist **Pflicht** für die Sternenkarte. Er kann pro Agentur erneut beschworen werden. |
| Ä5 | Säureregen: Schutz nur durch Kuppel T3 (Stufe 4), Venus kommt aber in Stufe 2 | **Kuppel T2 schützt vor Säureregen**. |
| Ä6 | Kältegrenzen der Anzüge passten nicht zu Merkur, Europa und dem Mond | Neue Grenzwerte in Kapitel 9 und Planetentemperaturen in Kapitel 13. |
| Ä7 | Unklar, welche Rakete welche Ziele erreicht | Feste Zieltabelle (12.1) und Treibstoffformel (12.2). |
| Ä8 | Begründung für Wasserstoff beim Fernflug-Modul war nicht schlüssig | Entfernt. Das Fernflug-Modul nutzt Wasserstoff und Sauerstoff wie die T2. |
| Ä9 | „Kühlmodul im Umkehrbetrieb“ und „Heizmodul“ | Zwei getrennte Module: `cooling_module` (Stufe 2) und `heating_module` (Stufe 5). |
| Ä10 | „Kleiner Kernreaktor“ nicht definiert | Entfernt. Außenposten nutzen `fission_reactor` oder Solar + `lithium_battery_bank`. |
| Ä11 | Orbit für jeden Planeten und Mond (zu viele Dimensionen) | Orbit-Dimensionen nur für 8 Planeten (ohne Pluto). Monde haben keinen eigenen Orbit: Raketen fliegen direkt zu ihnen, die Landefähre verbindet sie mit dem Orbit ihres Planeten. |
| Ä12 | Erdöl-Förderung in Phase 1 fehlte | Neue Maschine `fluid_pump` (Phase 1). |
| Ä13 | Energiebedarf von Kuppel T3 (1.480 J/t inkl. Sauerstoff) über einem Fusionsreaktor | Beibehalten, aber dokumentiert. Neues `superconductor_cable` (32.768 J/t), weil Aluminiumkabel für das Endgame nicht reicht. |
| Ä14 | Venus: wie man in die Wolkenschicht kommt und wieder hinunter | Automatische Aerostat-Plattform bei der ersten Landung, dazu ein `cloud_elevator` für Abstiege (12.8). |
| Ä15 | Proben waren nicht definiert | Proben entstehen mit der `sample_tool`. Probenart = Block-ID (Kapitel 11). |
| Ä16 | Drops ohne Verwendung | Jeder Drop hat jetzt ein Rezept oder einen Zweck (Kapitel 18). |
| Ä17 | Nickel und Gips ohne Verwendung | Nickel in Lithium-Akku, Bohrturm und Magnetstiefeln, Gips im Kompost-Boden. |
| Ä18 | Treibstoff reichte für Mars (T1), Io, Europa und das äußere System nicht für Hin- und Rückflug | T1 3–5 Tanks, T2 4–8 Tanks, Fernflug-Modul +128.000 mB je Komponente (Lesart „8 zusätzliche Tanks je Komponente“), Schub T2 = 48. Rückflugtabelle in 12.2. |
| Ä19 | Fortgeschrittene Sonde brauchte Iridium-Chips, Iridium gibt es aber erst dort, wo die Sonde hinführt | Sonde braucht 4 Fortgeschrittene Schaltkreise statt 2 Iridium-Chips. |
| Ä20 | Säureregen und Staubsturm (Stufe 2) brauchten Kuppel T2 (Stufe 3) | Kuppel T2 nach Stufe 2 verschoben. |
| Ä21 | Bewegung in der Station ohne Jetpack (Stufe 4) war undefiniert | Magnetstiefel kommen mit dem Stationskern (Stufe 3) und brauchen Nickel statt Supraleiter. |
| Ä22 | Frachtraketen (32.000 mB) schafften Hin- und Rückflug ins äußere System nicht, und die Werft konnte nicht betanken | Frachtrakete mit 64.000 mB Methan. `cargo_terminal` mit 216 Slots, 8 Frachttanks und eigenem Treibstofftank. |
| Ä23 | Nachzug nach Meilenstein 1 | Ressourcen erzeugt `tools/gen_resources.py` statt Java-`runData` (3.8, 3A.6); Rezeptwerte entfallen bei Maschinenstandard (25.4); Balance-Dateien aller Namespaces werden zusammengeführt (25.7); Addon-Schnittstelle `api/` (3A.10); Energie-API 26.3 (3.3); GameTest-Aufbau (3A.8, 27); M1 ohne GUI, GUI und Upgrades in M2 (27); Behälter-Brennstoffe (6.3). |
| Ä24 | Lückentest Meilenstein 2 (`docs/dev/lueckentest-m2.md`): 33 Lücken, die bei M2 zu Rückfragen geführt hätten | In die Kapitel 3.9, 3A, 4, 5, 6, 13.4, 22, 24 und 27 eingearbeitet (Stichpunkte unten). Zwei Punkte bleiben in Kapitel 28 offen. |
| Ä25 | Entscheidungen zu Kapitel 28 (11.11.2026) | Erdöl-Quellblöcke verbraucht die Pumpe nur mit 10 % Wahrscheinlichkeit (6.1); drei zutatengleiche Montagetisch-Rezepte getrennt (22.3) |
| Ä26 | Handbuch-GUI erst in M9, kein Spieler-Wiki (11.11.2026) | Wiki und Ingenieurs-Handbuch aus einer Quelle `docs/handbook/` (24.1, 25.6). Buch-GUI mit Links, Rezeptanzeige, Suche und Taste `H` sowie Join-Geschenk vorgezogen nach M2f; jeder Meilenstein ergänzt seine Seiten (27). |
| Ä27 | Lückentest Meilenstein 3 (`docs/dev/lueckentest-m3.md`, 11.11.2026): 34 Lücken, die bei M3 zu Rückfragen geführt hätten | In die Kapitel 3, 3A, 4, 5, 6, 8, 9, 10, 12, 13, 14.4, 23, 24, 25, 26, 27 und 28 eingearbeitet; Mondwerte in `planets.json` und Regeln in `domes.json`, `suits.json`, `environment.json`, `misc.json`, `machines.json` und `stations.json` ergänzt (Stichpunkte unten). Die Sauerstoff-Balance bleibt in Kapitel 28 offen. |
| Ä28 | Lückentest Meilenstein 4 (`docs/dev/lueckentest-m4.md`, 12.11.2026): 26 Lücken, die bei M4 zu Rückfragen geführt hätten | In die Kapitel 3, 3A, 4, 6, 9, 10, 11, 12, 13, 22, 23, 24, 25, 26, 27 und 28 eingearbeitet; Rampenmuster und `fuel_pump` in `machines.json`, Flug-, Rundungs- und Landefeuerwerte in `spaceflight.json`, Probenregeln in `balance/research.json`, Agentur-Regeln und Erfolge in `misc.json` sowie Regeln in `storage.json`, `balance/items.json` und `suits.json` ergänzt (Stichpunkte unten). Ob Mengen-Multiplikatoren unter 1 zutatengleiche Rezepte trennen, bleibt in Kapitel 28 offen. |

**Ä24: Lückentest Meilenstein 2**

- Umfang von M2 = alle Einträge mit `phase = 1` (27.1); die Spalte „Stufe“ in Kapitel 4 und 6 ist nur Orientierung.
- Neue Items `crude_oil_bucket` und `kerosene_bucket`; Füll-/Leerslots für Eimer gehören zu M2, Kanister und `transport_tank` kommen später (4.1, 27).
- Ausgangsseite für Fluide, Port-Modus, Netze ohne Puffer, „leer“ definiert (4.1, 4.3, 4.4); Muster der Raffinerie (4.3).
- Tank-Regeln, Energie und Fortschritt, Trichter-Regeln, Abbauen und Blockeigenschaften (4.1); `UpgradeMath` mit Rundung (4.2).
- Exakter Abgleich im Montagetisch und Mengen-Multiplikator ab M2 (22.3).
- Regeln für Pumpe, Elektroofen, Zerkleinerer und Akku (6.1, 6.3); Solar-Vorgabe für später (6.3); GUI-Schlüssel (6.4).
- Worldgen der Erde (13.4) und Eigenschaften von `crude_oil` (5.3); Tag-Muster (5.1); Farbtabelle (24.5); Rezeptanzeige (3.9).
- Forschungssperre als Schnittstelle, `IhBlockEntity` ab M2, `MachineConfigPayload`-Format (3A).
- Beispielrezepte mit Zeitgrenzen, Leistungstest und Teilschritte M2a–M2f (27.1–27.3).
- Offen (Kapitel 28): drei zutatengleiche Montagetisch-Paare, endliches Erdöl.

**Ä25: Entscheidungen zu Kapitel 28**

- Erdöl: Die Pumpe entfernt einen `crude_oil`-Quellblock nur mit 10 % Wahrscheinlichkeit (`source_consume_chance` in `machines.json`, 6.1).
- Zutatengleiche Rezepte getrennt: `research_terminal` +1 `minecraft:book`, `landing_beacon` mit `minecraft:redstone_lamp` statt Glowstone-Staub, `rocket_fin_t2` mit 3 statt 2 `lightweight_plate` (22.3); dadurch braucht die Rakete T2 132 statt 128 `lightweight_plate` (22.3, `validate_spec.py`).

**Ä26: Handbuch und Wiki**

- Quelle `docs/handbook/` (Markdown mit Befehlen wie `@recipes_for`, `@machine`, `{{value:…}}`), Generator `tools/handbook.py`; Ausgaben über `tools/gen_resources.py` (Spiel) und `tools/gen_wiki.py` (`docs/wiki/`, Ordner mit Markdown-Seiten und Bildern) (24.1).
- Schema 25.6 erweitert: Kapiteldatei `_chapter.json`, Seiten-`id`, `icon`, `order`, `items`, Elemente `heading`, `list`, `callout`, `machine`, `table`, eingebettete `recipes`, Inline-Markup für Links und Items.
- Buch-GUI, Links, Taste `H` und Join-Geschenk des Handbuchs in M2f (27.3); Blaupausen-Optik und englische Texte bleiben in M9.
- Pflicht je Meilenstein: Seiten für neue Inhalte, `gen_wiki.py --check` grün, README-Status aktuell (27).

**Ä27: Lückentest Meilenstein 3**

- Umfang von M3 als Liste: 18 Items und Blöcke, 11 Rezepte, Dimension `moon`. `pressure_bottle` gehört dazu; `helmet_lamp`, `transport_tank`, Kuppelmodule und `advanced_circuit` kommen später. Teilschritte M3a–M3e (27.4, 27.5, Matrix).
- Abnahme M3 präzisiert: Belastung mit Prüftakt statt „genau 200 t“, Kuppel-Ausfall mit festem Startzustand; Umwelt-Override `EnvironmentOverrides` und Dimensionsfeld von `TestData` (27, 3A.8).
- Agentur-Kern: Format von `AgencyData`, Agentur bei jedem Login ohne Agentur, Befehle `agency info` und `agency transfer`, Blöcke ohne Besitzer, Rechte erst ab M4, `visited_dimensions` (23.1, 3A). `IhBlockEntity` als Basis von Maschinen und Ports; Kabel, Rohre und Gehäuse ohne Block-Entity (3A.3, 26).
- Kuppelgenerator als Block mit den Zuständen `running`, `failing` und aus, O₂-Rechnung in 1/1.000 mB, Darstellung als Halbkugel (10.1, `domes.json`). Der Sauerstoffgenerator gibt nur über die Ausgangsseite ab (10.2).
- Belastung: Zählweise, Schadensarten, Kreativ ausgenommen (8.2). HUD mit Maßen, Leisten und Payload-Feldern (8.4, 3.5, 3A.5). Tageslicht-Kurve und Deckung (8.1). Welten ohne Atmosphäre im Einzelnen (8.5, 3.7).
- Anzug: O₂-Verbrauch, Startwert, Nachfüllen, Rüstungswerte (9.1). Sauerstoffflaschen in Füll- und Leerslots (4.1); `pressure_bottle` behält beim Abbauen ihren Inhalt (4.1, 6.3).
- Planeten-Registry als Reload-Listener mit Sync, Laufzeitfelder, `dimensions` in `implemented_ids.json` (13.1, 25). Noise-Settings, Biom-Parameter, Himmelsfelder (13.1, 25.1). Mondwerte für `great_crater`, `moon_ice`, `small_crater`, `lava_tube`, Polarkrater ohne Sonnenenergie und Eigenschaften der Mondblöcke (13.4, `planets.json`). Monster mit Atemmaske (13.5).
- Schwerkraft-Modifikatoren (12.9), Befehl `goto` (12.10), Betten (14.4), Platzhalter und Warntöne (24.5, 24.6), Schlüssel für Oberflächentexte (6.4), Spickzettel `api-dimensions-26.3.md` zu Beginn von M3 (3.1).
- Offen (Kapitel 28): Sauerstoff-Balance.

**Ä28: Lückentest Meilenstein 4**

- Umfang von M4 als Liste: 19 Items und Blöcke, 3 Entities, 18 Rezepte, 9 sichtbare Forschungsknoten; Schlüssel `entities` in `implemented_ids.json`. Teilschritte M4a–M4f (27.6, 27.7, Matrix). Abnahme „Treibstoffkosten“ zweifach (JUnit und GameTest), manuelle Prüfung in `docs/dev/stand-m4.md`.
- Agentur: Reiter „Agentur“ mit Einladen, Annehmen und Austreten, feste Rollen `leader`/`engineer`, Gast-Recht nur `OPEN`, Kuppelschutz für alle Blöcke, Operatoren ausgenommen, Felder von `AgencyData` ab M4 und `AgencyStatePayload` (23.1–23.3, 3A, `misc.json` `agency_rules`). Rollenwechsel, Umbenennen, Partner und Handel bleiben in M9 (Matrix-Zeile 9).
- Forschung: Rezeptsperre mit `ResearchRecipeLock` (erstes freies Rezept nach Rezept-ID, Status `locked`, M3-Objekte betroffen), sichtbare Knoten, Forschungsbaum, Prüfung von `ResearchUnlockPayload`, Tore und ihr Prüfzeitpunkt, `probe_targets` und `probe_reached` getrennt (11, 25.3, 3A).
- Proben und FP: Inhalt der Tags `sampleable/*` (`ring0_blocks`), Markieren nur bei Spieler-Platzierung, Meldungen, Stapeln; FP-Empfänger und Rechnung des Analysators; Erkundungsziel mit Instanz und Prüftakt (11.1, 11.2, 6.1, 13.1).
- Startrampe als Muster mit `launch_control` als Controller; `fuel_pump` als Maschine ohne Strom mit 2 Tanks und `transfer_per_tick`, „zieht“ → „nimmt an“; Checkliste mit Schlüsseln und Anzugregel (4.3, 4.4, 12.4, `machines.json`).
- Sonde mit Flug in `probe_flights` (12.3). Flug: Einsteigen, Absteigen, GUI im Sitz, Zerlegen, Startrecht, Navigationskarte mit Zusatzdaten, A = B, Rundung der Kosten, Aufstieg ohne Kollision mit `ascent_height`, Zielhöhe, `skip`, Tod, Abmelden mit `pending_flight`, Kamera (12.1, 12.2, 12.5, `spaceflight.json`).
- Landefeuer mit Speicher in `AgencyData`, Name, Strahl bis `beam_top_y` und Landung (12.6). Atlas mit Aufbau, Status, Geschenk und Entdecker; Erfolge mit Auslösern und Protokoll; „Planetenseiten füllen sich“ nach M9 (24.1–24.3, 23.3, `misc.json` `advancements`).
- `transport_tank` in allen Füll- und Leerslots (4.1, 12.2). Anzugmodule: Format von `suit_modules`, Abgleich, Lichtblock der Helmlampe (9.2). Mehrere passende Rezepte und Multiplikator (22.3). Handbuch-Kapitel `phase_2` ohne `unlock` und neue Seiten (24.1, 27.6). Darstellung der Entities, Raketenklänge (24.5, 24.6). Paketnamen `space/` (3A.2). Spickzettel `api-entities-26.3.md` zu Beginn von M4 (3.1). Schlüssel für Oberflächentexte (6.4).
- Offen (Kapitel 28): Mengen-Multiplikatoren unter 1.
