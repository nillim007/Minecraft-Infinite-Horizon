# Stand Meilenstein 8 (Netzwerk und Finale)

Teilschritte 8a–8h sind umgesetzt (Aufteilung in `docs/handbook/handbook.json`, Lücke #109). Alle M8-Tests laufen auf beiden Loadern in einem Durchlauf (Server-GameTests je Loader, JUnit, Fabric- und NeoForge-Client-Test, Spieltest M8 im Fabric-Client). `python tools/gen_resources.py` und `python tools/gen_wiki.py --check` erzeugen keine Änderungen.

## Abnahmekriterien (Kapitel 27)

| Kriterium | Test |
| --- | --- |
| **GT (reine Funktion): `OfflineProduction.compute(running = true, Rate 1 Item/100 t, Faktor 0,5, 24.000 t)` = 120 Items** | `offline_production_compute_120`, JUnit `OfflineProductionTest` (Faktor aus `logistics.json`) |
| **GT mit Override „nicht Erde“: Bohrturm über 10 Eisenerz-Blöcken läuft (`running`)** | `drillingrig_notearth_tenironore_running` |
| **GT: Route „bis Bedarf gedeckt“ stoppt bei 0 Bedarf** | `route_demand_stops_at_zero` (reine Funktion und Route zu einem Terminal ohne Bedarf), `shipyard_route_until_demand_met` (echte Werft: lädt nur Benötigtes, startet bei 0 Bedarf nicht mehr), JUnit `RouteRulesTest` |

## Teilschritte

| Teilschritt | Tests |
| --- | --- |
| 8a Kernreaktor (Multiblock, Brennstäbe, Kühlung, Hitze, Notabschaltung mit Strahlungszone) | `fission_runs_cooled`, `fission_scram_radiationzone`, `fission_needsstructure`, JUnit `FissionHeatTest` |
| 8b Bohrturm, Pumpstation mit Ponton, Ammoniak, Stickstoff, Ammoniak-Dünger | `drillingrig_notearth_tenironore_running`, `drillingrig_earth_error`, `drillingrig_noores_error`, `pumpstation_ammoniageyser`, `pumpstation_pontoon_methanerain`, `ammonia_fertilizer_hydroponics` |
| 8c Frachtterminal, Außenposten, Offline-Produktion | `offline_production_compute_120`, `cargoterminal_tanks_methanefirst`, `outpost_producer_delivers`, `outpost_nearest_terminal`, `outpost_offline_catchup`, `outpost_offline_rules`, `outpost_offline_inputs`, `cargoterminal_removed`, JUnit `OfflineProductionTest` |
| 8d Frachtraketen, Hangar, Frachtflüge, Antriebe | `cargorocket_flightmath`, `cargorocket_retrofit_bestdrive`, `cargoflight_roundtrip`, `cargoflight_outoffuel`, `cargoflight_freighter_fuelrod`, `cargoflight_keeps_methane`, `cargoflight_stranded_retry`, JUnit `CargoFlightMathTest` |
| 8e Logistik-Computer, Routen, Startbedingungen | `route_demand_stops_at_zero`, `route_every_n_ticks`, `route_target_below`, `route_when_full`, `logistics_computer_edit_routes`, JUnit `RouteRulesTest` |
| 8f Orbitalwerft (Terminal, Dock, Werftlager, 8 Abschnitte) | `shipyard_terminal_station_only`, `shipyard_dock_demand_methane`, `shipyard_star_chart_own_agency`, `shipyard_build_section_scaffold`, `shipyard_route_until_demand_met`, JUnit `ShipyardMathTest` |
| 8g Finale und Sternenschiff | `finale_start_after_section_8`, `starship_jump_orbits` |
| 8h Abnahme | die drei Kriterien oben, Spieltest M8 |

## Spieltest im Client

Ein automatischer Spieltest im echten Fabric-Client (`fabric/.../gametest/IhClientPlaytestM8.java`, läuft in der CI mit dem Client-Test; lokal allein: `./gradlew :fabric:runClientGameTest -PclientTestOnly=playtest_m8`) prüft je Schritt das Ergebnis und macht Bilder (`play8_*`):

1. Außenposten auf dem Mars: Bohrturm über Eisenerz liefert ins Frachtterminal. Nach einem Ausflug in die Overworld (30 s) hat das Terminal die Offline-Ausbeute.
2. Logistik-Computer: Route nur über die Knöpfe des GUI anlegen (Neu, Start, Ziel, Bedingung, Rakete, Speichern); die Rakete startet, die Statusliste zeigt „geladen“, die Fracht kommt am zweiten Terminal an.
3. Werft im Erdorbit: Abschnitt 1 im Bau, dann alle fertig; „Start“ im Werft-GUI startet das Finale, das bis zum Ende läuft; Erfolg „Infinite Horizon“ und das Sternenschiff neben der Station.
4. Sternenschiff: Einsteigen per Rechtsklick, Inventartaste öffnet die Sprungziele, Sprung in den Mars-Orbit; Spieler und Schiff kommen zusammen an, die Antimaterie-Zelle ist verbraucht.

Gefunden und behoben:

- Ein geladener, aber nicht mehr getickter Chunk (Spieler in einer anderen Dimension) verlor die Produktion der Pause. Produzenten rechnen jetzt jede Tick-Pause wie offline nach.
- „Alle N Ticks“ wartete bei einer neuen Route N Ticks ab Weltbeginn; eine neue Route startet jetzt sofort.
- Nach dem Speichern einer neuen Route blieb der Editor leer (keine Statusliste, doppeltes Speichern möglich); der Server übernimmt jetzt die ID des Clients.
- Das Gerüstmodell der Werft hatte Texturkoordinaten außerhalb der Textur und ließ sich nicht laden.
- In 26.3 ist die linke Maustaste `MOUSE_BUTTON_LEFT = 1` (API-Spickzettel).

## Nebenbei behoben

- Geysir und Schockwelle des Wächters bewegten Spieler nicht (26.3: `syncVelocity`, gefunden im Spieltest M7).
- Dock und Werft: Cache-Überlauf (`Long.MIN_VALUE`) und Zuordnung zur nächstgelegenen Werft derselben Station.

## Nicht enthalten (spätere Meilensteine)

Handelsposten und Partner-Lieferungen (M9), Erfolg `network` (fehlt in `misc.json`), Registry der Sternensysteme (in v1.0 keine weiteren Systeme), Modelle für Sternenschiff und Gerüst, eigene Töne (Feinschliff). Offene Annahmen: `docs/spec-gaps.md` #109–#116.
