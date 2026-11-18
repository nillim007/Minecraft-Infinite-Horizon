# Stand Meilenstein 7 (Ring 2: Merkur bis Pluto)

Teilschritte 7a–7f sind umgesetzt (Aufteilung in `docs/handbook/handbook.json`, Lücke #101). Alle M7-Tests laufen auf beiden Loadern in einem Durchlauf (Server-GameTests je Loader, JUnit, Fabric- und NeoForge-Client-Test). `python tools/gen_resources.py` und `python tools/gen_wiki.py --check` erzeugen keine Änderungen.

## Abnahmekriterium (Kapitel 27)

| Kriterium | Test |
| --- | --- |
| **Jedes Ziel ist mit der vorgesehenen Ausrüstung betretbar und ohne sie tödlich (mit Warnung)** | `m7_alltargets_equipment`: alle 20 Ziele (12 Oberflächen, 8 Orbits) und ihre Zonen (Mond-Polarkrater, Venus-Wolkenschicht, die vier Merkur-Bänder, Io an Lava, Europa-Tiefsee) nach der Tabelle in 13.2: mit der Mindestausrüstung keine Gefahr, ohne Anzug bzw. ohne das entscheidende Teil (Stufe, Strahlenschild, Heizmodul, Tauchmodul) mindestens eine. Jede Gefahr baut die Belastung über 200 t auf und zeigt sie im HUD (Warnung, seit M3 getestet). |

Der Test prüft die Umweltrechnung über den Override „Planet + Zone“ (27). Dass jede Welt erreichbar ist und sich betreten lässt, zeigen die Client-Screenshots (Befehl `goto` in jede Dimension) und die Tests der Landefähre.

## Teilschritte

| Teilschritt | Tests |
| --- | --- |
| 7a Merkur, Lithium, erweiterte Sonde, Spiegelkäfer, Strahlenschild, Hitzereflektor | `mercury_biomebands_x`, `mercury_drops_lithiumsalt_titanium`, `lithium_batterybank_capacity`, `advancedprobe_ringtwo_targets`, `rover_lithiumpack_capacity`, `suitmodule_heatreflector_radiationshield`, `creature_mirrorbeetle_blinds` |
| 7b Asteroidengürtel, Ceres, Anker, Driftmilbe, Platin, Iridium, Fernflug-Modul, Start von der Andockbucht, Jetpack | `asteroidbelt_spawnasteroid`, `asteroidbelt_asteroidsgenerate`, `asteroidbelt_voidreset`, `asteroid_anchor_onegee`, `asteroid_anchor_needsasteroid`, `ceres_icevolcano_atspawn`, `creature_driftmite_swarm`, `asteroid_iridiumore_drops`, `dockingbay_launch_t2`, `dockingbay_rejects_t1`, `rocket_longrangemodule_capacity`, `dockingbay_fills_rockettanks`, `jetpack_energy_installdrain` |
| 7c Jupiter-System: Orbit mit Gas-Sammler, Io, Europa, Eisbohrer, Tauchmodul, Klon-Kammer | `gascollector_jupiterorbit_hydrogen`, `gascollector_overworld_error`, `jupiterorbit_fall_tostation`, `salamander_glob_nofire`, `io_eruption_sulfurblocks`, `io_nearlava_zone`, `lander_jupiterorbit_toio`, `europa_iceshell_generated`, `europa_deeppressure_divingmodule`, `vent_bottle_microbeculture`, `icedrill_layer_energy`, `clonechamber_respawn_culture`, `reefkeeper_provoke_pacify`, `icejelly_seeks_vent`, `europa_hydrothermalfield_keeper` |
| 7d Saturn-System: Orbit und Stufe 5, Titan, Enceladus, Ringtrümmer, Magnetfalle, Kuppel T3 | `saturnorbit_gascollector_helium`, `tier5_enter_saturnorbit`, `ringdebris_impact_noore`, `titan_skimmer_methane_flee`, `titan_elytra_sinkrate`, `enceladus_geyser_launch`, `enceladus_geysermoth_circles`, `magnetictrap_jupiterorbit_cell`, `magnetictrap_overworld_error`, `dome_t3_values`, `lander_saturnorbit_moons`, `saturnmoons_worldgen` |
| 7e Äußeres System: Uranus- und Neptun-Orbit, Triton, Tiefraumanzug, Heizmodul, Pluto, Sender, Wächter, Sternenkarte | `uranusorbit_collector_switch`, `triton_frostlurker_emerges`, `triton_deepspacesuit_heating`, `lander_neptuneorbit_triton`, `pluto_heartglacier_center`, `pluto_uraniumore_drops`, `pluto_transmitter_hall`, `sentinel_summon_starchart`, `sentinel_phases_shockwave` |
| 7f Abnahme | `m7_alltargets_equipment` |
| JUnit | `XBandsTest`, `AsteroidShapeTest`, `JetpackMathTest`, `EuropaRiftsTest` u. a. |

Screenshots (Branch `ci-screenshots`): alle Welten von Merkur bis Pluto, dazu die GUIs von Gas-Sammler, Klon-Kammer und Magnetfalle.

## Spieltest im Client

Den Rundgang spielt ein automatischer Spieltest im echten Fabric-Client nach (`fabric/.../gametest/IhClientPlaytestM7.java`, läuft
in der CI mit dem Client-Test; lokal allein: `./gradlew :fabric:runClientGameTest -PclientTestOnly=playtest_m7`). Ein
Spieler im Überlebensmodus prüft je Schritt das Ergebnis und macht ein Bild (`play_*`):

1. Alle 14 Ziele von Ring 2: mit der vorgesehenen Ausrüstung 5 s ohne Gefahr und ohne Schaden, nicht in einem Block, nicht
   unter der Welt; ohne Anzug Gefahr und Warnung im HUD; auf Pluto ohne Anzug Schaden nach der Aufbauzeit.
2. Merkur-Tagseite (Glutebenen) nur mit Hitzeanzug und Strahlenschild.
3. Asteroidengürtel: Jetpack mit gehaltener Sprungtaste (hebt, verbraucht Energie); Fall unter y 0 zurück auf den
   Spawn-Asteroiden mit Langsamem Fall.
4. Europa: im Ozean unter y 80 nur mit Tauchmodul sicher; Eisqualle im Bild.
5. Titan: Gleiten mit Elytren (Sprungtaste in der Luft) sinkt langsamer als auf der Erde (Client-Physik).
6. Enceladus: der Ammoniak-Geysir hebt den Spieler.
7. Klon-Kammer: gebunden in der Overworld, Tod auf Europa, „Wiederbeleben“ im Client, Erwachen an der Kammer.
8. Pluto: Sender gesetzt, Wächter beschworen (Bossleiste), Phase 2 mit Frostkriechern, besiegt, Sternenkarte aufgehoben.

Was der Spieltest nicht selbst spielt, decken die Server-GameTests ab: erweiterte Sonde, Lithium- und Iridium-Abbau, Anker,
Gas-Sammler und Magnetfalle, Vulkanausbruch, Eisbohrer, Mikrobenkultur aus der Quelle, Riffwächter mit Ration, Umschalten
des Gas-Sammlers.

Gefunden und behoben: Geysir und Schockwelle des Wächters bewegten Spieler nicht (26.3 schickt die neue Geschwindigkeit nur
mit `syncVelocity` an den Spieler selbst; mit Mobs fiel das in den Server-Tests nicht auf).

## Nebenbei behoben

- 26.3 zählt in den Bewegungs-Heightmaps (`MOTION_BLOCKING`, `OCEAN_FLOOR`) nur Blöcke aus `#minecraft:blocks_motion_no_leaves`. Alle festen Mod-Blöcke stehen jetzt darin; vorher galten sie dort als Luft (Ursache von Lücke #54).
- Testspieler, die in eine andere Dimension wechseln, bestätigen den Wechsel sofort (`IhTestPlayers`).

## Nicht enthalten (spätere Meilensteine)

Verwendung der Sternenkarte und Sternensysteme (T3, M8), Pumpstation auf Geysiren und ihr Bonus im Methanregen (M8), Nebel bei Staubsturm und Methanregen (grafischer Feinschliff), Erfolge der Ziele (`under_the_ice`, `at_the_edge`, `the_signal`, M9), Modelle für Kreaturen und den Wächter (Feinschliff). Offene Annahmen: `docs/spec-gaps.md` #101–#108.
