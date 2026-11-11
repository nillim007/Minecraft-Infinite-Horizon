package de.sternenwerft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric-Hülle um die gemeinsamen GameTests (Entry-Point „fabric-gametest“).
 * ERZEUGT von tools/gen_gametests.py aus common/gametests.json – nicht von Hand ändern.
 */
public class SwFabricGameTests {

    @GameTest(maxTicks = 220)
    public void crusherRawironTwodustwithin220ticks(GameTestHelper helper) {
        SwGameTests.crusherRawIronTwoDust(helper);
    }

    @GameTest(maxTicks = 20)
    public void crusherBalancechangeChangesduration(GameTestHelper helper) {
        SwGameTests.crusherBalanceChangesDuration(helper);
    }

    @GameTest(maxTicks = 100)
    public void cablePlatformconsumerReceivesenergy(GameTestHelper helper) {
        SwGameTests.cablePlatformConsumerReceivesEnergy(helper);
    }

    @GameTest(maxTicks = 440)
    public void machinesRawtitaniumPlatewithin440ticks(GameTestHelper helper) {
        SwGameTests.machinesRawTitaniumToPlate(helper);
    }

    @GameTest(maxTicks = 20)
    public void oresTooltierDropsonlywithcorrecttool(GameTestHelper helper) {
        SwGameTests.oresNeedCorrectTool(helper);
    }

    @GameTest(maxTicks = 50)
    public void pumpCrudeoilTank1000within50ticks(GameTestHelper helper) {
        SwGameTests.pumpCrudeOilIntoTank(helper);
    }

    @GameTest(maxTicks = 60)
    public void pipesTwofluidsNevermixed(GameTestHelper helper) {
        SwGameTests.pipesNeverMix(helper);
    }

    @GameTest(maxTicks = 20)
    public void tankFillslotBucketbecomescrudeoilbucket(GameTestHelper helper) {
        SwGameTests.bucketFill(helper);
    }

    @GameTest(maxTicks = 20)
    public void generatorCoalBurns1600ticks(GameTestHelper helper) {
        SwGameTestsExtended.generatorCoalBurnTime(helper);
    }

    @GameTest(maxTicks = 20)
    public void generatorLavabucketKeepsbucket(GameTestHelper helper) {
        SwGameTestsExtended.generatorLavaBucketKeepsBucket(helper);
    }

    @GameTest(maxTicks = 20)
    public void crusherOutputfullStatuserror(GameTestHelper helper) {
        SwGameTestsExtended.crusherOutputFullError(helper);
    }

    @GameTest(maxTicks = 20)
    public void crusherFourspeedupgrades63ticks101jt(GameTestHelper helper) {
        SwGameTestsExtended.crusherFourSpeedUpgrades(helper);
    }

    @GameTest(maxTicks = 40)
    public void pressWithoutmoldNoprogress(GameTestHelper helper) {
        SwGameTestsExtended.pressWithoutMoldIdle(helper);
    }

    @GameTest(maxTicks = 110)
    public void furnaceRawironIroningotwithin110ticks(GameTestHelper helper) {
        SwGameTestsExtended.furnaceVanillaSmelting(helper);
    }

    @GameTest(maxTicks = 230)
    public void crusherStatusIdlerunningidle(GameTestHelper helper) {
        SwGameTestsExtended.crusherStatusCycle(helper);
    }

    @GameTest(maxTicks = 300)
    public void hopperFeedsanddrainsCrusher(GameTestHelper helper) {
        SwGameTestsExtended.hopperFeedsAndDrainsCrusher(helper);
    }

    @GameTest(maxTicks = 40)
    public void hopperNeverfillsUpgradeslot(GameTestHelper helper) {
        SwGameTestsExtended.hopperNeverFillsUpgradeSlot(helper);
    }

    @GameTest(maxTicks = 20)
    public void containerslotsHopperrules(GameTestHelper helper) {
        SwGameTestsExtended.containerSlotHopperRules(helper);
    }

    @GameTest(maxTicks = 20)
    public void tankDrainslotWaterbucket(GameTestHelper helper) {
        SwGameTestsExtended.tankDrainWaterBucket(helper);
    }

    @GameTest(maxTicks = 20)
    public void tankBrokenKeepscontents(GameTestHelper helper) {
        SwGameTestsExtended.tankKeepsContentsWhenBroken(helper);
    }

    @GameTest(maxTicks = 20)
    public void menuCreateMatcheslayout(GameTestHelper helper) {
        SwGameTestsExtended.menuMatchesLayout(helper);
    }

    @GameTest(maxTicks = 20)
    public void menuShiftclickRouting(GameTestHelper helper) {
        SwGameTestsExtended.shiftClickRouting(helper);
    }

    @GameTest(maxTicks = 20)
    public void payloadMachineconfigOnlyinrange(GameTestHelper helper) {
        SwGameTestsExtended.configPayloadRange(helper);
    }

    @GameTest(maxTicks = 20)
    public void cableThroughput256jt(GameTestHelper helper) {
        SwGameTestsExtended.cableThroughputLimit(helper);
    }

    @GameTest(maxTicks = 20)
    public void pipeThroughput200mbt(GameTestHelper helper) {
        SwGameTestsExtended.pipeThroughputLimit(helper);
    }

    @GameTest(maxTicks = 20)
    public void platformapiEnergyInsertsintocrusher(GameTestHelper helper) {
        SwGameTestsExtended.energyPlatformApiInsert(helper);
    }

    @GameTest(maxTicks = 20)
    public void platformapiFluidRespectsoutputside(GameTestHelper helper) {
        SwGameTestsExtended.fluidPlatformApiInsert(helper);
    }

    @GameTest(maxTicks = 60)
    public void pumpWaterRemovessource(GameTestHelper helper) {
        SwGameTestsExtended.pumpWaterRemovesSource(helper);
    }

    @GameTest(maxTicks = 20)
    public void worldgenOilbubbleFeature(GameTestHelper helper) {
        SwGameTestsExtended.oilBubbleFeature(helper);
    }

    @GameTest(maxTicks = 20)
    public void worldgenBiomesContainfeatures(GameTestHelper helper) {
        SwGameTestsExtended.worldgenBiomeFeatures(helper);
    }

    @GameTest(maxTicks = 20)
    public void configDefaultsLoaded(GameTestHelper helper) {
        SwGameTestsExtended.configDefaults(helper);
    }

    @GameTest(maxTicks = 220)
    public void refineryCrudeoilKerosene600plastic1within220ticks(GameTestHelper helper) {
        SwGameTestsM2d.refineryCrudeOilToKerosene(helper);
    }

    @GameTest(maxTicks = 20)
    public void refineryMissingcasingErrorthenidle(GameTestHelper helper) {
        SwGameTestsM2d.refineryMissingCasingThenIdle(helper);
    }

    @GameTest(maxTicks = 20)
    public void refineryWrongblockGhostpreview(GameTestHelper helper) {
        SwGameTestsM2d.refineryWrongBlockAndGhostPreview(helper);
    }

    @GameTest(maxTicks = 20)
    public void refineryPortsForwardtocontroller(GameTestHelper helper) {
        SwGameTestsM2d.refineryPortsForwardToController(helper);
    }

    @GameTest(maxTicks = 20)
    public void portModeToggleemptyhand(GameTestHelper helper) {
        SwGameTestsM2d.portModeToggle(helper);
    }

    @GameTest(maxTicks = 20)
    public void assemblyExactmatchFluidportnotcasing(GameTestHelper helper) {
        SwGameTestsM2d.assemblyExactMatch(helper);
    }

    @GameTest(maxTicks = 220)
    public void assemblyCircuitWithin220ticks(GameTestHelper helper) {
        SwGameTestsM2d.assemblyCircuit(helper);
    }

    @GameTest(maxTicks = 44)
    public void electrolyzerWaterH2o2within44ticks(GameTestHelper helper) {
        SwGameTestsM2d.electrolyzerWater(helper);
    }

    @GameTest(maxTicks = 20)
    public void electrolyzerFullgastankError(GameTestHelper helper) {
        SwGameTestsM2d.electrolyzerFullError(helper);
    }

    @GameTest(maxTicks = 20)
    public void electrolyzerTankselection(GameTestHelper helper) {
        SwGameTestsM2d.electrolyzerTankSelection(helper);
    }

    @GameTest(maxTicks = 20)
    public void tankRejectsGas(GameTestHelper helper) {
        SwGameTestsM2d.tankRejectsGas(helper);
    }
}
