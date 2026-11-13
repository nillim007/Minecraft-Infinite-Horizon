package de.infinitehorizon.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.function.Consumer;

/**
 * Alle Server-GameTests. ERZEUGT von tools/gen_gametests.py aus common/gametests.json – nicht von Hand ändern.
 */
public final class IhTestCatalog {

    /**
     * Name (Registry-Pfad), Zeitgrenze in Ticks, Struktur, Testumgebung und Dimension (null = Standard bzw.
     * Overworld), Testfunktion. Tests mit eigener Umgebung laufen in einem eigenen Durchgang (Leistungstest).
     */
    public record Test(String name, int maxTicks, String structure, String environment, String dimension,
                       Consumer<GameTestHelper> run) {
    }

    public static final List<Test> ALL = List.of(
            new Test("crusher_rawiron_twodustwithin220ticks", 220, null, null, null, IhGameTests::crusherRawIronTwoDust),  // 27 M1
            new Test("crusher_balancechange_changesduration", 20, null, null, null, IhGameTests::crusherBalanceChangesDuration),  // 27 M1
            new Test("cable_platformconsumer_receivesenergy", 100, null, null, null, IhGameTests::cablePlatformConsumerReceivesEnergy),  // 27 M1
            new Test("machines_rawtitanium_platewithin440ticks", 440, null, null, null, IhGameTests::machinesRawTitaniumToPlate),  // 27.3 M2a
            new Test("ores_tooltier_dropsonlywithcorrecttool", 20, null, null, null, IhGameTests::oresNeedCorrectTool),  // 27.3 M2b
            new Test("pump_crudeoil_tank1000within50ticks", 50, null, null, null, IhGameTests::pumpCrudeOilIntoTank),  // 27.3 M2c, spec-gaps #23
            new Test("pipes_twofluids_nevermixed", 60, null, null, null, IhGameTests::pipesNeverMix),  // 27.3 M2c
            new Test("tank_fillslot_bucketbecomescrudeoilbucket", 20, null, null, null, IhGameTests::bucketFill),  // 27.3 M2c
            new Test("generator_coal_burns1600ticks", 20, null, null, null, IhGameTestsExtended::generatorCoalBurnTime),  // 6.3
            new Test("generator_lavabucket_keepsbucket", 20, null, null, null, IhGameTestsExtended::generatorLavaBucketKeepsBucket),  // 6.3
            new Test("crusher_outputfull_statuserror", 20, null, null, null, IhGameTestsExtended::crusherOutputFullError),  // 4.1
            new Test("crusher_fourspeedupgrades_63ticks101jt", 20, null, null, null, IhGameTestsExtended::crusherFourSpeedUpgrades),  // 4.2
            new Test("press_withoutmold_noprogress", 40, null, null, null, IhGameTestsExtended::pressWithoutMoldIdle),  // 5.2
            new Test("furnace_rawiron_ironingotwithin110ticks", 110, null, null, null, IhGameTestsExtended::furnaceVanillaSmelting),  // 6.1
            new Test("crusher_status_idlerunningidle", 230, null, null, null, IhGameTestsExtended::crusherStatusCycle),  // 4.1
            new Test("hopper_feedsanddrains_crusher", 300, null, null, null, IhGameTestsExtended::hopperFeedsAndDrainsCrusher),  // 4.1
            new Test("hopper_neverfills_upgradeslot", 40, null, null, null, IhGameTestsExtended::hopperNeverFillsUpgradeSlot),  // 4.1
            new Test("containerslots_hopperrules", 20, null, null, null, IhGameTestsExtended::containerSlotHopperRules),  // 4.1
            new Test("tank_drainslot_waterbucket", 20, null, null, null, IhGameTestsExtended::tankDrainWaterBucket),  // 4.1
            new Test("tank_broken_keepscontents", 20, null, null, null, IhGameTestsExtended::tankKeepsContentsWhenBroken),  // 4.1
            new Test("menu_create_matcheslayout", 20, null, null, null, IhGameTestsExtended::menuMatchesLayout),  // 4.1
            new Test("menu_shiftclick_routing", 20, null, null, null, IhGameTestsExtended::shiftClickRouting),  // 4.1
            new Test("payload_machineconfig_onlyinrange", 20, null, null, null, IhGameTestsExtended::configPayloadRange),  // 3A.5
            new Test("cable_throughput_256jt", 20, null, null, null, IhGameTestsExtended::cableThroughputLimit),  // 4.4
            new Test("pipe_throughput_200mbt", 20, null, null, null, IhGameTestsExtended::pipeThroughputLimit),  // 4.4
            new Test("platformapi_energy_insertsintocrusher", 20, null, null, null, IhGameTestsExtended::energyPlatformApiInsert),  // 3.3
            new Test("platformapi_fluid_respectsoutputside", 20, null, null, null, IhGameTestsExtended::fluidPlatformApiInsert),  // 3.3, 4.1
            new Test("pump_water_removessource", 60, null, null, null, IhGameTestsExtended::pumpWaterRemovesSource),  // 6.1
            new Test("worldgen_oilbubble_feature", 20, null, null, null, IhGameTestsExtended::oilBubbleFeature),  // 13.4
            new Test("worldgen_biomes_containfeatures", 20, null, null, null, IhGameTestsExtended::worldgenBiomeFeatures),  // 13.4, 27.3 M2b
            new Test("config_defaults_loaded", 20, null, null, null, IhGameTestsExtended::configDefaults),  // 24.4
            new Test("refinery_crudeoil_kerosene600plastic1within220ticks", 220, null, null, null, IhGameTestsM2d::refineryCrudeOilToKerosene),  // 27.2, 27.3 M2d
            new Test("refinery_missingcasing_errorthenidle", 20, null, null, null, IhGameTestsM2d::refineryMissingCasingThenIdle),  // 27.3 M2d, 4.3
            new Test("refinery_wrongblock_ghostpreview", 20, null, null, null, IhGameTestsM2d::refineryWrongBlockAndGhostPreview),  // 4.1, 4.3
            new Test("refinery_ports_forwardtocontroller", 20, null, null, null, IhGameTestsM2d::refineryPortsForwardToController),  // 4.3
            new Test("port_mode_toggleemptyhand", 20, null, null, null, IhGameTestsM2d::portModeToggle),  // 4.3
            new Test("assembly_exactmatch_fluidportnotcasing", 20, null, null, null, IhGameTestsM2d::assemblyExactMatch),  // 22.3, 27.3 M2d
            new Test("assembly_circuit_within220ticks", 220, null, null, null, IhGameTestsM2d::assemblyCircuit),  // 27.2
            new Test("electrolyzer_water_h2o2within44ticks", 44, null, null, null, IhGameTestsM2d::electrolyzerWater),  // 27.2
            new Test("electrolyzer_fullgastank_error", 20, null, null, null, IhGameTestsM2d::electrolyzerFullError),  // 5.3
            new Test("electrolyzer_tankselection", 20, null, null, null, IhGameTestsM2d::electrolyzerTankSelection),  // 4.1
            new Test("tank_rejects_gas", 20, null, null, null, IhGameTestsM2d::tankRejectsGas),  // 6.3
            new Test("fuelgenerator_kerosenebucket_80jt", 20, null, null, null, IhGameTestsM2e::fuelGeneratorKeroseneBucket),  // 27.3 M2e
            new Test("fuelgenerator_4mb_16ticks", 30, null, null, null, IhGameTestsM2e::fuelGeneratorFourMbSixteenTicks),  // 27.2
            new Test("battery_outputsideonly", 20, null, null, null, IhGameTestsM2e::batteryOutputSideOnly),  // 27.3 M2e, 4.4
            new Test("battery_io512_platformapi", 20, null, null, null, IhGameTestsM2e::batteryIoLimitAndPlatform),  // 27.2, 3.3
            new Test("battery_chargeslot", 20, null, null, null, IhGameTestsM2e::batteryChargeSlot),  // 6.3
            new Test("battery_broken_keepsenergy", 20, null, null, null, IhGameTestsM2e::batteryKeepsEnergyWhenBroken),  // 4.1
            new Test("performance_100crushers_meantick", 420, "infinite_horizon:gametest/empty_large", "infinite_horizon:performance", null, IhGameTestsM2e::performanceHundredCrushers),  // 27.2
            new Test("handbook_joingift_once", 20, null, null, null, IhGameTestsM2f::handbookGiftOnce),  // 24.1, 27.3 M2f
            new Test("crusher_bauxite_twoaluminumdustwithin220ticks", 220, null, null, null, IhGameTestsM2f::crusherBauxite),  // 27.2
            new Test("furnace_quartzsand_siliconwithin110ticks", 110, null, null, null, IhGameTestsM2f::furnaceQuartzSand),  // 27.2
            new Test("press_titaniumplatewithin110ticks", 110, null, null, null, IhGameTestsM2f::pressTitaniumPlate),  // 27.2
            new Test("agency_placedcrusher_ownedbyplayeragency", 20, null, null, null, IhGameTestsM3a::placedCrusherHasOwner),  // 23.1, 27.5 M3a
            new Test("agency_setblockcrusher_noowner", 20, null, null, null, IhGameTestsM3a::setBlockCrusherHasNoOwner),  // 23.1, 27.5 M3a
            new Test("agency_mergechain_ownerrewritten", 20, null, null, null, IhGameTestsM3a::ownerFollowsMergeChain),  // 23.1, 27.5 M3a
            new Test("agency_transfercommand_setsmergedinto", 20, null, null, null, IhGameTestsM3a::transferCommand),  // 23.1, 27.5 M3a
            new Test("environment_moonoverride_hazardfullafter200ticks", 210, null, "infinite_horizon:moon_day", null, IhGameTestsM3b::moonVacuumHazardBuildup),  // 8.2, 27.5 M3b (Abnahme 1)
            new Test("gravity_moonoverride_gravityandsafefall", 40, null, null, null, IhGameTestsM3b::moonGravity),  // 12.9, 27.5 M3b
            new Test("environment_creative_nobuildup", 60, null, null, null, IhGameTestsM3b::creativeNoBuildup),  // 8.2
            new Test("moon_aluminumore_dropsonebauxite", 20, null, null, "infinite_horizon:moon", IhGameTestsM3c::moonAluminumOreDrops),  // 13.4, 27.5 M3c
            new Test("moon_player_gravitywithoutoverride", 20, null, null, "infinite_horizon:moon", IhGameTestsM3c::moonGravityWithoutOverride),  // 12.9, 27.5 M3c
            new Test("moon_goto_landsonsolidground", 200, null, null, null, IhGameTestsM3c::gotoLandsOnSolidGround),  // 12.10, 27.5 M3c
            new Test("suit_moonoverride_nostrain590mb", 220, null, null, null, IhGameTestsM3d::suitProtectsOnMoon),  // 9.1, 27.5 M3d
            new Test("suit_incomplete_noprotection", 20, null, null, null, IhGameTestsM3d::incompleteSuitDoesNotProtect),  // 9.1
            new Test("oxygengenerator_fillslot_canisterfullafter1tick", 20, null, null, null, IhGameTestsM3d::generatorFillsCanister),  // 4.1, 10.2, 27.5 M3d
            new Test("pressurebottle_onlygas", 20, null, null, null, IhGameTestsM3d::pressureBottleOnlyGas),  // 6.3
            new Test("dome_moonoverride_breathable", 60, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM3e::domeMakesAirBreathable),  // 10.1, 27.5 M3e (Abnahme 2)
            new Test("dome_1mb_failing24_off600", 720, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM3e::domeFailsThenTurnsOff),  // 10.1, 27.5 M3e (Abnahme 3)
            new Test("dome_torch_onlyinside", 40, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM3e::torchOnlyInsideDome),  // 8.5, 27.5 M3e
            new Test("atmosphere_torches_extinguish", 20, null, null, null, IhGameTestsM3e::torchesExtinguish),  // 8.5
            new Test("atmosphere_water_freezes", 20, null, null, null, IhGameTestsM3e::waterFreezes),  // 8.5
            new Test("lamp_lit_onlywithpower", 20, null, null, null, IhGameTestsM3e::lampNeedsPower),  // 8.5
            new Test("research_assemblytable_lockeduntilresearched", 250, null, null, null, IhGameTestsM4a::assemblyLockedUntilResearched),  // 11.3, 27.7 M4a
            new Test("research_notrequired_assemblesimmediately", 220, null, "infinite_horizon:research_free", null, IhGameTestsM4a::assemblyFreeWhenNotRequired),  // 11.3, 24.4, 27.7 M4a
            new Test("research_terminal_openstier1groundstation", 40, null, null, null, IhGameTestsM4a::terminalOpensTier1),  // 11.4, 24.3, 27.7 M4a
            new Test("sample_naturalbauxite_oncepereposition", 20, null, null, null, IhGameTestsM4b::sampleToolOncePerPosition),  // 11.2, 27.7 M4b
            new Test("analyzer_onesample_onefpwithin110ticks", 110, null, null, null, IhGameTestsM4b::analyzerOneSampleOneFp),  // 6.1, 11.1, 27.7 M4b
            new Test("solar_moonoverride_5jt_polarcrater0", 60, null, "infinite_horizon:moon_day", null, IhGameTestsM4b::solarPanelMoon),  // 6.3, 27.7 M4b
            new Test("exploration_lavatube_20fp", 60, null, null, "infinite_horizon:moon", IhGameTestsM4b::lavaTubeGivesFp),  // 11.1, 27.7 M4b
            new Test("agency_guest_noslotsnobreak_thenjoin", 20, null, null, null, IhGameTestsM4c::guestThenMember),  // 23.1, 27.7 M4c
            new Test("agency_moonoverride_domeprotection", 40, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM4c::domeProtection),  // 23.2, 10.3, 27.7 M4c
            new Test("agency_unownedcrusher_claimedonopen", 20, null, null, null, IhGameTestsM4c::unownedClaimedOnOpen),  // 23.1, 27.7 M4c
            new Test("fuel_earthtomoon_fromregistries_35360", 20, null, null, null, IhGameTestsM4d::fuelCostFromRegistries),  // 12.2, 27.7 M4d (Abnahme 2)
            new Test("launch_threefins_rejected_fourthfin_ready", 20, null, null, null, IhGameTestsM4d::checklistNeedsFourFins),  // 12.4, 27.7 M4d (Abnahme 3)
            new Test("fuelpump_threetanks_evenly_full", 300, null, null, null, IhGameTestsM4d::fuelPumpFillsEvenly),  // 12.4, 27.7 M4d
            new Test("probe_moon_arriveswithin1320ticks", 1320, null, null, null, IhGameTestsM4d::probeReachesMoon),  // 12.3, 27.7 M4d
            new Test("flight_t1_moonandback_fuel_disassemble", 20, null, null, null, IhGameTestsM4e::flightToMoonAndBack),  // 12.5, 27.7 M4e
            new Test("flight_beacon_moon100_within3", 20, null, null, null, IhGameTestsM4e::beaconLandingWithinRadius),  // 12.6, 27.7 M4e
            new Test("flight_logoutinflight_reloginseated", 20, null, null, null, IhGameTestsM4e::logoutInFlightRelogSeated),  // 12.5, 27.7 M4e
            new Test("atlas_gift_onceevenwithouttag", 20, null, null, null, IhGameTestsM4f::atlasGiftOnce),  // 24.2, 27.7 M4f
            new Test("atlas_moonvisit_discoverer", 40, null, null, null, IhGameTestsM4f::moonVisitDiscoverer),  // 23.3, 24.2, 27.7 M4f
            new Test("suitmodule_helmetlamp_installremove", 300, null, null, null, IhGameTestsM4f::moduleInstallRemove),  // 9.2, 27.7 M4f
            new Test("helmetlamp_light14_gonewhenremoved", 60, null, null, null, IhGameTestsM4f::helmetLampLight),  // 9.2, 27.7 M4f
            new Test("ringone_oredrops", 20, null, null, null, IhGameTestsM5a::ringOneOreDrops),  // 13.4, M5a
            new Test("venus_landing_solidground", 20, null, null, null, IhGameTestsM5a::venusLandingOnSolidGround),  // 12.5, 13.4, M5a
            new Test("hightempfurnace_rawtungsten_ingotwithin440", 440, null, null, null, IhGameTestsM5b::highTempFurnaceTungsten),  // 6.1, M5b
            new Test("regolithheater_overworld_errorlocation", 40, null, null, null, IhGameTestsM5b::regolithHeaterNeedsMoon),  // 6.1, M5b
            new Test("regolithheater_moonoverride_50mbhelium", 120, null, null, null, IhGameTestsM5b::regolithHeaterMoonHelium),  // 6.1, M5b
            new Test("cryoliquefier_40helium_10liquid", 40, null, null, null, IhGameTestsM5b::cryoLiquefiesHelium),  // 6.1, M5b
            new Test("research_tier3gate_magnesiumtungstenhelium", 20, null, null, null, IhGameTestsM5b::tierThreeGate),  // 11.4, M5b
            new Test("rocket_t2_fivepad_rejected", 20, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM5c::t2NeedsSevenPad),  // 12.4, M5c
            new Test("rocket_t2_sevenpad_flightmars", 20, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM5c::t2FlightToMars),  // 12.4, M5c
            new Test("venus_heattime_3600", 3700, null, null, null, IhGameTestsM5d::venusHeatTime),  // 9.1, 27 M5 (Abnahme)
            new Test("venus_aerostat_landingplatform", 20, null, null, null, IhGameTestsM5d::aerostatLandingPlatform),  // 12.8, M5d
            new Test("venus_cloudelevator_ride", 20, null, null, null, IhGameTestsM5d::cloudElevatorRide),  // 12.8, M5d
            new Test("venus_terrain_belowclouds", 20, null, null, null, IhGameTestsM5d::venusTerrainBelowClouds),  // 12.8, M5d
            new Test("rover_kit_roundtrip", 20, null, null, null, IhGameTestsM5e::kitRoundTrip),  // 16, M5e
            new Test("rover_charges_atbattery", 30, null, null, null, IhGameTestsM5e::chargesAtBattery),  // 16, M5e
            new Test("rover_drill_tocargo", 20, null, null, null, IhGameTestsM5e::drillToCargo),  // 16, M5e
            new Test("rover_cabin_breathable", 20, null, null, null, IhGameTestsM5e::cabinBreathable),  // 16, M5e
            new Test("creature_crawler_reveals", 30, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM5f::crawlerReveals),  // 18, M5f
            new Test("creature_crawler_eatscable", 600, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM5f::crawlerEatsCable),  // 18, M5f
            new Test("creature_burrower_seeksvibration", 400, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM5f::burrowerSeeksVibration),  // 18, M5f
            new Test("creature_snail_trail", 20, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM5f::snailTrail),  // 18, M5f
            new Test("creature_cloudray_tamesaddlemount", 20, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM5f::cloudRayMount),  // 18, M5f
            new Test("creature_fieldscanner_oncepertype", 20, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM5f::fieldScannerOnce),  // 11.1, 18, M5f
            new Test("creature_wraith_projectileimmune", 20, "infinite_horizon:gametest/empty_large", null, null, IhGameTestsM5f::wraithProjectileImmune),  // 18, M5f
            new Test("creature_monster_needsdarkness", 20, "infinite_horizon:gametest/empty_large", "infinite_horizon:moon_day", null, IhGameTestsM5f::monsterNeedsDarkness),  // 18, M5f
            new Test("event_solarstorm_radiationsolar", 20, null, null, null, IhGameTestsM5g::solarStormRadiationAndSolar),  // 19, M5g
            new Test("event_solarstorm_pausesuncovered", 20, null, null, null, IhGameTestsM5g::solarStormPausesUncovered),  // 19, M5g
            new Test("event_meteor_leavesore", 20, null, null, null, IhGameTestsM5g::meteorLeavesOre),  // 19, M5g
            new Test("event_acidrain_damagesarmor", 20, null, null, null, IhGameTestsM5g::acidRainDamagesArmor),  // 19, M5g
            new Test("food_packer_threefoods_ration", 110, null, null, null, IhGameTestsM5h::foodPackerRation),  // 17, M5h
            new Test("food_compostsoil_hoe_farmland", 20, null, null, null, IhGameTestsM5h::compostSoilTills),  // 17, M5h
            new Test("food_hydroponics_stageandharvest", 2200, null, null, null, IhGameTestsM5h::hydroponicsStageAndHarvest),  // 17, M5h
            new Test("food_hydroponics_sapling_logs", 20, null, null, null, IhGameTestsM5h::hydroponicsSaplingLogs),  // 17, M5h
            new Test("food_hydroponics_moonneedsdome", 40, null, null, null, IhGameTestsM5h::hydroponicsNeedsDome)  // 17, M5h
    );

    /** Testumgebung: ID und Setup-Funktion (null = leer). Fabric liest dieselben Werte als JSON. */
    public record Environment(String id, String setupFunction) {
    }

    public static final List<Environment> ENVIRONMENTS = List.of(
            new Environment("infinite_horizon:moon_day", "infinite_horizon:gametest/moon_day"),
            new Environment("infinite_horizon:performance", null),
            new Environment("infinite_horizon:research_free", null)
    );

    private IhTestCatalog() {
    }
}
