# Stand Meilenstein 4 (Raumfahrt)

Teilschritte 4a–4f sind umgesetzt (27.7). Alle M4-Tests laufen auf beiden Loadern in einem Durchlauf (Server-GameTests je Loader, JUnit, Fabric- und NeoForge-Client-Test). `python tools/gen_resources.py` und `python tools/gen_wiki.py --check` erzeugen keine Änderungen.

## Abnahmekriterien (Kapitel 27)

| Kriterium | Test |
| --- | --- |
| Forschung: Terminal öffnet Stufe 1, gesperrte und freie Rezepte | `research_terminal_openstier1groundstation`, `research_assemblytable_lockeduntilresearched`, `research_notrequired_assemblesimmediately` |
| Proben, Analysator, Solarpanel, Lavaröhre | `sample_naturalbauxite_oncepereposition`, `analyzer_onesample_onefpwithin110ticks`, `solar_moonoverride_5jt_polarcrater0`, `exploration_lavatube_20fp` |
| Agentur: Gast-Rechte, Beitritt, Kuppelschutz, Besitzübernahme | `agency_guest_noslotsnobreak_thenjoin`, `agency_moonoverride_domeprotection`, `agency_unownedcrusher_claimedonopen` |
| Abnahme 2: Treibstoff Erde → Mond 35.360 mB aus den geladenen Daten | `fuel_earthtomoon_fromregistries_35360` |
| Abnahme 3: 3 Flossen abgelehnt, 4 bereit | `launch_threefins_rejected_fourthfin_ready` |
| Pumpe füllt 3 Tanks gleichmäßig; Sonde erreicht den Mond | `fuelpump_threetanks_evenly_full`, `probe_moon_arriveswithin1320ticks` |
| Flug zum Mond (12.640 mB je Komponente) und zurück (5.200 mB), Zerlegen 9 Teile | `flight_t1_moonandback_fuel_disassemble` |
| Landefeuer: Landung höchstens 3 Blöcke daneben; Abmelden im Flug | `flight_beacon_moon100_within3`, `flight_logoutinflight_reloginseated` |
| Atlas-Geschenk genau einmal (auch M3-Spieler); Mond besucht und Entdecker | `atlas_gift_onceevenwithouttag`, `atlas_moonvisit_discoverer` |
| Helm + Lampe → Helm mit Modul; Ausbau; Licht Level 14 an der Kopfposition, nach dem Absetzen weg | `suitmodule_helmetlamp_installremove`, `helmetlamp_light14_gonewhenremoved` |
| JUnit: Treibstoffkosten, Masse und Schub, Nachfolge des Leiters, Moduleinbau, Protokollzeile | `FuelCalculatorTest`, `AgencyDataTest`, `SuitModulesTest` u. a. |

Screenshots (Branch `ci-screenshots`): Forschungsbaum, Agentur-Reiter, Analysator, Solarpanel, Startrampe, Raketen-Computer, Navigationskarte, Countdown, Rakete auf dem Mond, Raketen-GUI, Atlas.

## Manuell zu prüfen (Abnahme 1)

In einer neuen Welt ohne Befehle:

1. Erdindustrie aufbauen (Phase 1), Forschungsterminal setzen.
2. Mit der Probenzange Proben nehmen und im Analysator 60 FP sammeln.
3. Sechs Knoten erforschen, darunter Druckanzug, Startrampe und Mondsonde.
4. Startrampe mit Pumpe und Steuerpult bauen, eine Sonde zum Mond schicken; nach der Ankunft steht der Mond im Atlas auf „Sonde angekommen“.
5. Rakete T1 mit 3 Tanks bauen und über die Pumpe mit Kerosin und Sauerstoff betanken.
6. Mit Anzug und Flaschen in die Kapsel, Inventartaste, „Start“, Mond wählen, starten; Countdown von außen, Landung auf dem Mond.
7. Auf dem Mond: Atlas zeigt „besucht“ und die eigene Agentur als Entdecker; Helmlampe im Krater testen.
8. Rückflug zum Weltspawn bzw. zu einem Landefeuer auf der Erde.
