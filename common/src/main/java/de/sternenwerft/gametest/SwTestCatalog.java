package de.sternenwerft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.function.Consumer;

/**
 * Alle Server-GameTests. ERZEUGT von tools/gen_gametests.py aus common/gametests.json – nicht von Hand ändern.
 */
public final class SwTestCatalog {

    /** Name (Registry-Pfad), Zeitgrenze in Ticks, Testfunktion. */
    public record Test(String name, int maxTicks, Consumer<GameTestHelper> run) {
    }

    public static final List<Test> ALL = List.of(
            new Test("crusher_rawiron_twodustwithin220ticks", 220, SwGameTests::crusherRawIronTwoDust),  // 27 M1
            new Test("crusher_balancechange_changesduration", 20, SwGameTests::crusherBalanceChangesDuration),  // 27 M1
            new Test("cable_platformconsumer_receivesenergy", 100, SwGameTests::cablePlatformConsumerReceivesEnergy),  // 27 M1
            new Test("machines_rawtitanium_platewithin440ticks", 440, SwGameTests::machinesRawTitaniumToPlate),  // 27.3 M2a
            new Test("ores_tooltier_dropsonlywithcorrecttool", 20, SwGameTests::oresNeedCorrectTool),  // 27.3 M2b
            new Test("pump_crudeoil_tank1000within50ticks", 50, SwGameTests::pumpCrudeOilIntoTank),  // 27.3 M2c, spec-gaps #23
            new Test("pipes_twofluids_nevermixed", 60, SwGameTests::pipesNeverMix),  // 27.3 M2c
            new Test("tank_fillslot_bucketbecomescrudeoilbucket", 20, SwGameTests::bucketFill),  // 27.3 M2c
            new Test("generator_coal_burns1600ticks", 20, SwGameTestsExtended::generatorCoalBurnTime),  // 6.3
            new Test("generator_lavabucket_keepsbucket", 20, SwGameTestsExtended::generatorLavaBucketKeepsBucket),  // 6.3
            new Test("crusher_outputfull_statuserror", 20, SwGameTestsExtended::crusherOutputFullError),  // 4.1
            new Test("crusher_fourspeedupgrades_63ticks101jt", 20, SwGameTestsExtended::crusherFourSpeedUpgrades),  // 4.2
            new Test("press_withoutmold_noprogress", 40, SwGameTestsExtended::pressWithoutMoldIdle),  // 5.2
            new Test("furnace_rawiron_ironingotwithin110ticks", 110, SwGameTestsExtended::furnaceVanillaSmelting),  // 6.1
            new Test("crusher_status_idlerunningidle", 230, SwGameTestsExtended::crusherStatusCycle),  // 4.1
            new Test("hopper_feedsanddrains_crusher", 300, SwGameTestsExtended::hopperFeedsAndDrainsCrusher),  // 4.1
            new Test("hopper_neverfills_upgradeslot", 40, SwGameTestsExtended::hopperNeverFillsUpgradeSlot),  // 4.1
            new Test("containerslots_hopperrules", 20, SwGameTestsExtended::containerSlotHopperRules),  // 4.1
            new Test("tank_drainslot_waterbucket", 20, SwGameTestsExtended::tankDrainWaterBucket),  // 4.1
            new Test("tank_broken_keepscontents", 20, SwGameTestsExtended::tankKeepsContentsWhenBroken),  // 4.1
            new Test("menu_create_matcheslayout", 20, SwGameTestsExtended::menuMatchesLayout),  // 4.1
            new Test("menu_shiftclick_routing", 20, SwGameTestsExtended::shiftClickRouting),  // 4.1
            new Test("payload_machineconfig_onlyinrange", 20, SwGameTestsExtended::configPayloadRange),  // 3A.5
            new Test("cable_throughput_256jt", 20, SwGameTestsExtended::cableThroughputLimit),  // 4.4
            new Test("pipe_throughput_200mbt", 20, SwGameTestsExtended::pipeThroughputLimit),  // 4.4
            new Test("platformapi_energy_insertsintocrusher", 20, SwGameTestsExtended::energyPlatformApiInsert),  // 3.3
            new Test("platformapi_fluid_respectsoutputside", 20, SwGameTestsExtended::fluidPlatformApiInsert),  // 3.3, 4.1
            new Test("pump_water_removessource", 60, SwGameTestsExtended::pumpWaterRemovesSource),  // 6.1
            new Test("worldgen_oilbubble_feature", 20, SwGameTestsExtended::oilBubbleFeature),  // 13.4
            new Test("worldgen_biomes_containfeatures", 20, SwGameTestsExtended::worldgenBiomeFeatures),  // 13.4, 27.3 M2b
            new Test("config_defaults_loaded", 20, SwGameTestsExtended::configDefaults),  // 24.4
            new Test("refinery_crudeoil_kerosene600plastic1within220ticks", 220, SwGameTestsM2d::refineryCrudeOilToKerosene),  // 27.2, 27.3 M2d
            new Test("refinery_missingcasing_errorthenidle", 20, SwGameTestsM2d::refineryMissingCasingThenIdle),  // 27.3 M2d, 4.3
            new Test("refinery_wrongblock_ghostpreview", 20, SwGameTestsM2d::refineryWrongBlockAndGhostPreview),  // 4.1, 4.3
            new Test("refinery_ports_forwardtocontroller", 20, SwGameTestsM2d::refineryPortsForwardToController),  // 4.3
            new Test("port_mode_toggleemptyhand", 20, SwGameTestsM2d::portModeToggle),  // 4.3
            new Test("assembly_exactmatch_fluidportnotcasing", 20, SwGameTestsM2d::assemblyExactMatch),  // 22.3, 27.3 M2d
            new Test("assembly_circuit_within220ticks", 220, SwGameTestsM2d::assemblyCircuit),  // 27.2
            new Test("electrolyzer_water_h2o2within44ticks", 44, SwGameTestsM2d::electrolyzerWater),  // 27.2
            new Test("electrolyzer_fullgastank_error", 20, SwGameTestsM2d::electrolyzerFullError),  // 5.3
            new Test("electrolyzer_tankselection", 20, SwGameTestsM2d::electrolyzerTankSelection),  // 4.1
            new Test("tank_rejects_gas", 20, SwGameTestsM2d::tankRejectsGas)  // 6.3
    );

    private SwTestCatalog() {
    }
}
