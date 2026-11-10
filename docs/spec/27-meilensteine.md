# 27. Meilensteine und Abnahmekriterien

Jeder Meilenstein MUSS kompilieren, auf beiden Loadern starten (Client + dedizierter Server) und seine GameTests bestehen.

GameTests laufen in der Overworld. Umweltabhängige Tests (Vakuum, Hitze, Kuppel, Venus) prüfen deshalb `EnvironmentCalculator` (8.1) direkt oder nutzen einen Override „Planet + Zone für diesen Testbereich“, der nur in GameTests aktiv ist. Der Override setzt auch die Ortsbindung (z. B. „gilt als Orbit“ oder „nicht Erde“) für Maschinen mit Ortsbindung. Wo möglich, wird Logik als reine Funktion getestet. Zeitangaben in Tests dürfen bis 10 % Toleranz haben.

| # | Inhalt | Abnahmekriterien (GameTests oder manuelle Prüfung) |
| --- | --- | --- |
| 1 | Grundgerüst: Multiloader, Registrierung, Energie-/Fluid-Abstraktion mit Transaktionen, Netzwerk, Config, Datagen, Loader für Balance-Dateien (25.7), `machine_frame`, `crusher`, `combustion_generator`, `copper_cable`, Platzhalter-Skript | GT: Generator mit Kohle → Kabel → Zerkleinerer macht aus 1 `minecraft:raw_iron` 2 `iron_dust` in höchstens 220 t. GT: Wert in `machines.json` ändern + `/reload` ändert die Vorgangsdauer. GT: Ein Test-Verbraucher, der nur die Plattform-API nutzt (NeoForge: Energy-Capability, Fabric: Team Reborn Energy), erhält Strom aus dem Kabel. |
| 2 | Erdindustrie: alle Phase-1-Materialien, Erze, Worldgen-Ergänzungen, Maschinen, Generatoren, Speicher, Rohre, Raffinerie-Multiblock, Upgrades, JEI/REI/EMI | GT: jede Maschine verarbeitet ihr Beispielrezept. GT: Raffinerie erkennt die Struktur und meldet `error` bei fehlendem Block. Manuell: Bauxit spawnt in der Savanne. |
| 3 | Erste Welt: Planeten-Registry, `moon` mit Biomen und Himmel, Schwerkraft, Umweltsystem, HUD, Anzug T1, Sauerstoffgenerator, Kuppel T1, Füll- und Leerslots, **Agentur-Kern** (SavedData, automatische Agentur je Spieler, `owner_agency` an allen Block-Entities) | GT: Spieler ohne Anzug im Mond-Vakuum erreicht 100 % Belastung nach genau 200 t. GT: Spieler in aktiver Kuppel hat `breathable`. GT: Kuppel ohne O₂ fällt nach 600 t aus. |
| 4 | Erster Flug: Forschung, Proben, Analysator, Sonde, Startrampe, Rakete T1, Flugablauf, Landefeuer, Atlas. **Erste spielbare Version** | Manuell: neue Welt → Mond und zurück ohne Befehle. GT: Treibstoffkosten Erde → Mond = 35.360 mB. GT: Rakete mit 3 Flossen startet nicht. |
| 5 | Ring 1: Mars, Venus (Wolkenschicht, Aerostat, Aufzug), Rakete T2, Anzug T2, Rover, Kreaturen Ring 1, Ereignisse, Nahrung | GT: Venus-Hitzezeit 3.600 t. Manuell: Helium, Magnesium und Wolfram gewinnen öffnet Stufe 3. |
| 6 | Orbit: Orbit-Dimensionen, Station, Module, Schwerelosigkeit, Fusion, Landefähre, Fernflug-Modul | GT: Fusion schaltet 40 t nach Kühlungsausfall ab. GT: `zero_g_fabricator` außerhalb des Orbits = `error`. |
| 7 | Ring 2: alle Ziele Merkur bis Pluto, Kreaturen, Sender, Boss, Strahlung | Manuell: jedes Ziel ist mit der vorgesehenen Ausrüstung betretbar und ohne sie tödlich (mit Warnung). |
| 8 | Netzwerk und Finale: Außenposten, Bohrturm, Pumpstation, Frachtraketen, Logistik-Computer, Kernreaktor, Antriebe, Werft, T3, Finale | GT (reine Funktion): `OfflineProduction.compute(running = true, Rate 1 Item/100 t, Faktor 0,5, 24.000 t)` = 120 Items. GT mit Override „nicht Erde“: Bohrturm über 10 Eisenerz-Blöcken läuft (`running`). GT: Route „bis Bedarf gedeckt“ stoppt bei 0 Bedarf. |
| 9 | Feinschliff: Agentur-GUI, Rollen, Partner, Handel, Handbuch, Advancements, Klang, Balancing-Skript, Übersetzungen | `tools/balance_sheet.py` liest alle Rezept-JSONs und gibt die Rohstoffsumme je Rakete und Werftabschnitt aus. Regeln: Rohstoff = Item oder Fluid ohne eigenes Rezept. Bei mehreren Rezepten zählt das erste. Nebenprodukte werden ignoriert. Fluide werden in mB summiert. Rezepte werden alphabetisch nach ID sortiert; Mengen werden durch die Ausgabemenge geteilt; Pressformen (`mold`) werden ignoriert; bei Tags zählt das erste Element des Tags. |

**Balancing-Ziele** (Einzelspieler, Standardeinstellungen, gesamt 50–80 h):

| Phase | Zielzeit | Strombedarf am Ende |
| --- | --- | --- |
| 1 | 4–6 h | ~100 J/t |
| 2 | 3–4 h | ~200 J/t |
| 3 | 8–12 h | ~500 J/t |
| 4 | 6–8 h | ~2.000 J/t |
| 5 | 15–25 h | ~5.000 J/t |
| 6 | 15–25 h | ~10.000 J/t |

Grundregeln: Jede Raketenstufe ≈ 3× Material der vorigen, T3 ≈ 10× T2. Ab Phase 3 dauert ein Schlüsselmaterial von Hand ≥ 3× so lang wie mit einfacher Automatisierung. Etwa die Hälfte der FP aus Erkundung. Jede Gefahr warnt ≥ 200 t vorher. Kein Engpass nur durch Warten.
