# Stand Meilenstein 6 (Orbit)

Teilschritte 6a–6e sind umgesetzt (Aufteilung in `docs/handbook/handbook.json`, Lücke #96). Alle M6-Tests laufen auf beiden Loadern in einem Durchlauf (Server-GameTests je Loader, JUnit, Fabric- und NeoForge-Client-Test). `python tools/gen_resources.py` und `python tools/gen_wiki.py --check` erzeugen keine Änderungen.

## Abnahmekriterien (Kapitel 27)

| Kriterium | Test |
| --- | --- |
| **GT (Abnahme): Fusion schaltet 40 t nach Kühlungsausfall ab** | `fusion_shutdown_40ticks_nocooling` |
| **GT (Abnahme): `zero_g_fabricator` außerhalb des Orbits = `error`** | `zerog_overworld_error` |
| 6a Orbit: Schwerelosigkeit, Magnetstiefel, Absturz, Stationskern, Ankunft | `orbit_gravity_magnetboots`, `orbit_fall_tosurface_slowfall`, `station_core_platform_builtindome`, `station_core_airplacement`, `station_core_onlyinorbit`, `orbit_arrival_hover_foreigncore` |
| 6b Stationsmodule | `station_tankdepot_capacity`, `station_solarwing_orbitoutput`, `station_solarwing_surfacezero`, `station_habitation_spawnindome`, `station_researchlab_analyzerbonus`, `station_greenhouse_harvestdryice`, `station_greenhouse_onlyinorbit`, `station_dockingbay_landrefuel` |
| 6c Schwerelos-Fertiger, Tor zu Stufe 4, Weltraumwetter-Station | `zerog_orbit_wire_opentier4`, `weatherstation_earlywarning`, `weatherstation_notonearth` |
| 6d Fusionsreaktor, Schwerkraftplatte | `fusion_needsstructure`, `gravityplate_orbit_onegee` |
| 6e Landefähre | `lander_notonearth`, `lander_orbit_tomoon_disassemble` |
| JUnit: Reaktor-Sicherheit, Plattenbereich, Kosten und Wege der Fähre | `ReactorSafetyTest`, `LanderTest` u. a. |

Screenshots (Branch `ci-screenshots`): Erdorbit mit Erde unter dem Spieler, Gewächshaus, Schwerelos-Fertiger, Fusionsreaktor.

## Manuell zu prüfen

In einer Welt mit Stufe 3:

1. Rakete T2 mit `station_core` im Frachtraum zum Erdorbit; aus der Kapsel den Kern unter die Rakete setzen, Plattform entsteht, aussteigen.
2. Kuppel des Kerns mit Sauerstoff füllen; mit Magnetstiefeln über die Plattform laufen, abspringen und treiben.
3. Andockbucht, Tankdepot, Solarflügel und Wohnmodul bauen; im Wohnmodul den Respawnpunkt setzen.
4. Schwerelos-Fertiger mit flüssigem Helium: Supraleiterdraht fertigen, Stufe 4 öffnet sich.
5. Fusionsreaktor (5×5×5) bauen, mit Wasserstoff, Helium und flüssigem Helium betreiben; Kühlung abziehen, nach 2 s Abschaltung.
6. Landefähre auf der Andockbucht absetzen, betanken, zum Mond und zurück zur Bucht fliegen.
7. Unter der Plattform in die Tiefe springen: Absturz auf die Erde mit Langsamem Fall.

## Nicht enthalten (spätere Meilensteine)

`long_range_module` und Raketenbau auf der Andockbucht (M7), Orbits der Gasriesen mit Gas-Sammler und Magnetfalle (M7), Modelle für Fähre und Kreaturen (Feinschliff). Offene Annahmen: `docs/spec-gaps.md` #96–#100.
