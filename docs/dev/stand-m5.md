# Stand Meilenstein 5 (Ring 1)

Teilschritte 5a–5i sind umgesetzt (Aufteilung in `docs/handbook/handbook.json`, Lücken #86, #89 und #95). Alle M5-Tests laufen auf beiden Loadern in einem Durchlauf (Server-GameTests je Loader, JUnit, Fabric- und NeoForge-Client-Test). `python tools/gen_resources.py` und `python tools/gen_wiki.py --check` erzeugen keine Änderungen.

## Abnahmekriterien (Kapitel 27)

| Kriterium | Test |
| --- | --- |
| **GT (Abnahme): Venus-Hitzezeit 3.600 t** | `venus_heattime_3600` |
| 5a Mars und Venus: Erzdrops, feste Landung | `ringone_oredrops`, `venus_landing_solidground` |
| 5b Industrie Stufe 2: Hochtemperaturofen, Regolith-Erhitzer mit Ortsbindung, Kryo-Verflüssiger, Tor zu Stufe 3 | `hightempfurnace_rawtungsten_ingotwithin440`, `regolithheater_overworld_errorlocation`, `regolithheater_moonoverride_50mbhelium`, `cryoliquefier_40helium_10liquid`, `research_tier3gate_magnesiumtungstenhelium` |
| 5c Rakete T2: Rampe 7×7, Flug zum Mars | `rocket_t2_fivepad_rejected`, `rocket_t2_sevenpad_flightmars` |
| 5d Venus: Aerostat, Aufzug, Gelände unter den Wolken | `venus_aerostat_landingplatform`, `venus_cloudelevator_ride`, `venus_terrain_belowclouds` |
| 5e Rover: Bausatz, Laden, Bohrer, Kabine | `rover_kit_roundtrip`, `rover_charges_atbattery`, `rover_drill_tocargo`, `rover_cabin_breathable` |
| 5f Kreaturen Ring 1 und Feldscanner | `creature_crawler_reveals`, `creature_crawler_eatscable`, `creature_burrower_seeksvibration`, `creature_snail_trail`, `creature_cloudray_tamesaddlemount`, `creature_fieldscanner_oncepertype`, `creature_wraith_projectileimmune`, `creature_monster_needsdarkness` |
| 5g Ereignisse: Sonnensturm, Meteoritenschauer, Säureregen | `event_solarstorm_radiationsolar`, `event_solarstorm_pausesuncovered`, `event_meteor_leavesore`, `event_acidrain_damagesarmor` |
| 5h Nahrung: Ration, Kompostboden, Hydrokultur | `food_packer_threefoods_ration`, `food_compostsoil_hoe_farmland`, `food_hydroponics_stageandharvest`, `food_hydroponics_sapling_logs`, `food_hydroponics_moonneedsdome` |
| 5i Kuppel T2, Kuppelmodule, Anzugmodule (Nachtrag laut 27.4) | `dome_t2_rangemodule_radiusenergy`, `dome_shieldmodule_stopsmeteor`, `dome_t2_dustacidprotection`, `suitmodule_cooling_armorplating` |
| JUnit: Ortsbindung, Anzüge, Nahrungsregeln, Kuppelmodule | `MachineLocationTest`, `SuitsTest`, `FoodRulesTest`, `DomeModulesTest` u. a. |

Screenshots (Branch `ci-screenshots`): Mars- und Venus-Gelände, Rover auf dem Mars, Hochtemperaturofen, Kryo-Verflüssiger, Regolith-Erhitzer, Nahrungspacker, Hydrokultur-Becken.

## Manuell zu prüfen (Abnahme)

**Helium, Magnesium und Wolfram gewinnen öffnet Stufe 3.** In einer Welt mit M4-Fortschritt (Stufen 1 und 2 offen), ohne Befehle:

1. Auf dem Mond einen Regolith-Erhitzer aufstellen und aus Regolith Helium gewinnen.
2. Rakete T2 auf einer 7×7-Rampe bauen und zum Mars fliegen; dort Magnesiumerz abbauen und zu `magnesium_ingot` verarbeiten.
3. Mit Hitzeanzug zur Venus (Landung auf der Wolkenschicht, Aufzug zur Oberfläche), Wolframerz abbauen, im Hochtemperaturofen zu `tungsten_ingot` schmelzen.
4. Im Forschungsterminal ist danach Stufe 3 offen.

Weitere Sichtprüfungen:

- Venus: Hitzeleiste im HUD läuft ohne Hitzeanzug ab, mit Anzug T2 nicht.
- Rover: Fahren, Laden am Akku, Bohrer füllt den Frachtraum.
- Kreaturen erscheinen in ihren Biomen; der Feldscanner gibt je Art einmal FP.
- Ein Ereignis (Server-Einstellung `event_frequency` hochsetzen und warten) warnt vorher im Chat und im HUD.
- Unter der Kuppel auf dem Mond: Kompostboden umgraben, Weizen säen; Becken mit Licht und Wasser liefert Ernte.

## Nicht enthalten (spätere Meilensteine)

`greenhouse` (Orbit, M6), `ammonia_fertilizer` (Ammoniak, M8), `space_weather_station` (Stufe 3), Fernflug-Modul (M6). Offene Annahmen: `docs/spec-gaps.md` #86–#95.
