package de.sternenwerft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.function.Consumer;

/**
 * Alle Server-GameTests. ERZEUGT von tools/gen_gametests.py aus common/gametests.json – nicht von Hand ändern.
 */
public final class SwTestCatalog {

    /**
     * Name (Registry-Pfad), Zeitgrenze in Ticks, Struktur und Testumgebung (null = Standard), Testfunktion. Tests mit
     * eigener Umgebung laufen in einem eigenen Durchgang (Leistungstest).
     */
    public record Test(String name, int maxTicks, String structure, String environment, Consumer<GameTestHelper> run) {
    }

    public static final List<Test> ALL = List.of(
            new Test("crusher_rawiron_twodustwithin220ticks", 220, null, null, SwGameTests::crusherRawIronTwoDust),  // 27 M1
            new Test("crusher_balancechange_changesduration", 20, null, null, SwGameTests::crusherBalanceChangesDuration),  // 27 M1
            new Test("cable_platformconsumer_receivesenergy", 100, null, null, SwGameTests::cablePlatformConsumerReceivesEnergy),  // 27 M1
            new Test("machines_rawtitanium_platewithin440ticks", 440, null, null, SwGameTests::machinesRawTitaniumToPlate),  // 27.3 M2a
            new Test("ores_tooltier_dropsonlywithcorrecttool", 20, null, null, SwGameTests::oresNeedCorrectTool),  // 27.3 M2b
            new Test("pump_crudeoil_tank1000within50ticks", 50, null, null, SwGameTests::pumpCrudeOilIntoTank),  // 27.3 M2c, spec-gaps #23
            new Test("pipes_twofluids_nevermixed", 60, null, null, SwGameTests::pipesNeverMix),  // 27.3 M2c
            new Test("tank_fillslot_bucketbecomescrudeoilbucket", 20, null, null, SwGameTests::bucketFill),  // 27.3 M2c
            new Test("generator_coal_burns1600ticks", 20, null, null, SwGameTestsExtended::generatorCoalBurnTime),  // 6.3
            new Test("generator_lavabucket_keepsbucket", 20, null, null, SwGameTestsExtended::generatorLavaBucketKeepsBucket),  // 6.3
            new Test("crusher_outputfull_statuserror", 20, null, null, SwGameTestsExtended::crusherOutputFullError),  // 4.1
            new Test("crusher_fourspeedupgrades_63ticks101jt", 20, null, null, SwGameTestsExtended::crusherFourSpeedUpgrades),  // 4.2
            new Test("press_withoutmold_noprogress", 40, null, null, SwGameTestsExtended::pressWithoutMoldIdle),  // 5.2
            new Test("furnace_rawiron_ironingotwithin110ticks", 110, null, null, SwGameTestsExtended::furnaceVanillaSmelting),  // 6.1
            new Test("crusher_status_idlerunningidle", 230, null, null, SwGameTestsExtended::crusherStatusCycle),  // 4.1
            new Test("hopper_feedsanddrains_crusher", 300, null, null, SwGameTestsExtended::hopperFeedsAndDrainsCrusher),  // 4.1
            new Test("hopper_neverfills_upgradeslot", 40, null, null, SwGameTestsExtended::hopperNeverFillsUpgradeSlot),  // 4.1
            new Test("containerslots_hopperrules", 20, null, null, SwGameTestsExtended::containerSlotHopperRules),  // 4.1
            new Test("tank_drainslot_waterbucket", 20, null, null, SwGameTestsExtended::tankDrainWaterBucket),  // 4.1
            new Test("tank_broken_keepscontents", 20, null, null, SwGameTestsExtended::tankKeepsContentsWhenBroken),  // 4.1
            new Test("menu_create_matcheslayout", 20, null, null, SwGameTestsExtended::menuMatchesLayout),  // 4.1
            new Test("menu_shiftclick_routing", 20, null, null, SwGameTestsExtended::shiftClickRouting),  // 4.1
            new Test("payload_machineconfig_onlyinrange", 20, null, null, SwGameTestsExtended::configPayloadRange),  // 3A.5
            new Test("cable_throughput_256jt", 20, null, null, SwGameTestsExtended::cableThroughputLimit),  // 4.4
            new Test("pipe_throughput_200mbt", 20, null, null, SwGameTestsExtended::pipeThroughputLimit),  // 4.4
            new Test("platformapi_energy_insertsintocrusher", 20, null, null, SwGameTestsExtended::energyPlatformApiInsert),  // 3.3
            new Test("platformapi_fluid_respectsoutputside", 20, null, null, SwGameTestsExtended::fluidPlatformApiInsert),  // 3.3, 4.1
            new Test("pump_water_removessource", 60, null, null, SwGameTestsExtended::pumpWaterRemovesSource),  // 6.1
            new Test("worldgen_oilbubble_feature", 20, null, null, SwGameTestsExtended::oilBubbleFeature),  // 13.4
            new Test("worldgen_biomes_containfeatures", 20, null, null, SwGameTestsExtended::worldgenBiomeFeatures),  // 13.4, 27.3 M2b
            new Test("config_defaults_loaded", 20, null, null, SwGameTestsExtended::configDefaults),  // 24.4
            new Test("refinery_crudeoil_kerosene600plastic1within220ticks", 220, null, null, SwGameTestsM2d::refineryCrudeOilToKerosene),  // 27.2, 27.3 M2d
            new Test("refinery_missingcasing_errorthenidle", 20, null, null, SwGameTestsM2d::refineryMissingCasingThenIdle),  // 27.3 M2d, 4.3
            new Test("refinery_wrongblock_ghostpreview", 20, null, null, SwGameTestsM2d::refineryWrongBlockAndGhostPreview),  // 4.1, 4.3
            new Test("refinery_ports_forwardtocontroller", 20, null, null, SwGameTestsM2d::refineryPortsForwardToController),  // 4.3
            new Test("port_mode_toggleemptyhand", 20, null, null, SwGameTestsM2d::portModeToggle),  // 4.3
            new Test("assembly_exactmatch_fluidportnotcasing", 20, null, null, SwGameTestsM2d::assemblyExactMatch),  // 22.3, 27.3 M2d
            new Test("assembly_circuit_within220ticks", 220, null, null, SwGameTestsM2d::assemblyCircuit),  // 27.2
            new Test("electrolyzer_water_h2o2within44ticks", 44, null, null, SwGameTestsM2d::electrolyzerWater),  // 27.2
            new Test("electrolyzer_fullgastank_error", 20, null, null, SwGameTestsM2d::electrolyzerFullError),  // 5.3
            new Test("electrolyzer_tankselection", 20, null, null, SwGameTestsM2d::electrolyzerTankSelection),  // 4.1
            new Test("tank_rejects_gas", 20, null, null, SwGameTestsM2d::tankRejectsGas),  // 6.3
            new Test("fuelgenerator_kerosenebucket_80jt", 20, null, null, SwGameTestsM2e::fuelGeneratorKeroseneBucket),  // 27.3 M2e
            new Test("fuelgenerator_4mb_16ticks", 30, null, null, SwGameTestsM2e::fuelGeneratorFourMbSixteenTicks),  // 27.2
            new Test("battery_outputsideonly", 20, null, null, SwGameTestsM2e::batteryOutputSideOnly),  // 27.3 M2e, 4.4
            new Test("battery_io512_platformapi", 20, null, null, SwGameTestsM2e::batteryIoLimitAndPlatform),  // 27.2, 3.3
            new Test("battery_chargeslot", 20, null, null, SwGameTestsM2e::batteryChargeSlot),  // 6.3
            new Test("battery_broken_keepsenergy", 20, null, null, SwGameTestsM2e::batteryKeepsEnergyWhenBroken),  // 4.1
            new Test("performance_100crushers_meantick", 240, "sternenwerft:gametest/empty_large", "sternenwerft:performance", SwGameTestsM2e::performanceHundredCrushers),  // 27.2
            new Test("handbook_joingift_once", 20, null, null, SwGameTestsM2f::handbookGiftOnce),  // 24.1, 27.3 M2f
            new Test("crusher_bauxite_twoaluminumdustwithin220ticks", 220, null, null, SwGameTestsM2f::crusherBauxite),  // 27.2
            new Test("furnace_quartzsand_siliconwithin110ticks", 110, null, null, SwGameTestsM2f::furnaceQuartzSand),  // 27.2
            new Test("press_titaniumplatewithin110ticks", 110, null, null, SwGameTestsM2f::pressTitaniumPlate)  // 27.2
    );

    private SwTestCatalog() {
    }
}
