# 28. Offene Punkte (nicht von der KI zu entscheiden)

- [ ] Änderungen Ä1–Ä28 (Kapitel 1) bestätigen oder anpassen.
- [ ] Direkt vor dem Entwicklungsstart prüfen: Ist 26.4 inzwischen stabil und von NeoForge und Fabric stabil unterstützt? Wenn ja, Kapitel 3.1 auf 26.4 anheben und dessen Änderungsliste (Fabric-Blogpost, NeoForge-Primer) einarbeiten. Sonst bei 26.3 bleiben.
- [ ] Mengen und Zeiten nach Testplan im Spiel prüfen (3–5 Tester, Multiplikator 1,0 und 0,5).
- [ ] Echte Texturen, Modelle und Kreaturen in Blockbench nach 24.5 erstellen (ersetzt die Platzhalter).
- [ ] Englische Namen von einem Muttersprachler gegenlesen lassen.
- [ ] Musik komponieren oder lizenzieren.
- [ ] Gruppen-ID und Basispaket bestätigen (Vorschlag `de.infinitehorizon`, 3A.2).
- [x] Lizenz der Mod festlegen: **MIT** (siehe `LICENSE`; in `neoforge.mods.toml` und `fabric.mod.json` eintragen).
- [ ] Nach jedem Meilenstein `docs/spec-gaps.md` durchgehen: Annahmen übernehmen oder verwerfen.
- [ ] Sauerstoff-Balance (Lückentest M3, Lücke 32): Der Elektrolyseur liefert 100 mB O₂ in 40 t mit 40 J/t, der Sauerstoffgenerator 100 mB in 1.200 t mit 60 J/t (30-mal langsamer, 45-mal mehr Energie je mB). Wasser lässt sich in Eimern oder im `fluid_tank` zum Mond bringen. Gewollt, oder den Generator stärken bzw. den Elektrolyseur einschränken? Bis zur Entscheidung gelten die Daten.
- [ ] Mengen-Multiplikator unter 1 (Lückentest M4, Lücke 22): Bei `recipe_amount_multiplier` 0,25 werden Montagetisch-Rezepte zutatengleich, weil alle Mengen bis 4 zu 1 werden (`analyzer`/`launch_control`, `refinery_casing`/`fluid_port`, `oxygen_canister`/`pressure_bottle`). Bis zur Entscheidung gilt das erste freie Rezept nach Rezept-ID (22.3). Sollen Multiplikatoren unter 1 solche Paare trennen (z. B. Untergrenze je Rezept), oder bleibt es so?
- [x] Zutatengleiche Montagetisch-Rezepte (22.3) sind getrennt (entschieden 11.11.2026, Ä25): `research_terminal` +1 `minecraft:book`, `landing_beacon` mit `minecraft:redstone_lamp`, `rocket_fin_t2` mit 3 `lightweight_plate`.
- [x] Erdöl: Die Pumpe entfernt einen `crude_oil`-Quellblock nur mit 10 % Wahrscheinlichkeit (entschieden 11.11.2026, Ä25).
