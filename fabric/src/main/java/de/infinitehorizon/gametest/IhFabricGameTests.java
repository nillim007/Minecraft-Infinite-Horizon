package de.infinitehorizon.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric-Hülle um die gemeinsamen GameTests (Entry-Point „fabric-gametest“).
 * ERZEUGT von tools/gen_gametests.py aus common/gametests.json – nicht von Hand ändern.
 */
public class IhFabricGameTests {

    @GameTest(maxTicks = 220)
    public void crusherRawironTwodustwithin220ticks(GameTestHelper helper) {
        IhGameTests.crusherRawIronTwoDust(helper);
    }

    @GameTest(maxTicks = 20)
    public void crusherBalancechangeChangesduration(GameTestHelper helper) {
        IhGameTests.crusherBalanceChangesDuration(helper);
    }

    @GameTest(maxTicks = 100)
    public void cablePlatformconsumerReceivesenergy(GameTestHelper helper) {
        IhGameTests.cablePlatformConsumerReceivesEnergy(helper);
    }

    @GameTest(maxTicks = 440)
    public void machinesRawtitaniumPlatewithin440ticks(GameTestHelper helper) {
        IhGameTests.machinesRawTitaniumToPlate(helper);
    }

    @GameTest(maxTicks = 20)
    public void oresTooltierDropsonlywithcorrecttool(GameTestHelper helper) {
        IhGameTests.oresNeedCorrectTool(helper);
    }

    @GameTest(maxTicks = 50)
    public void pumpCrudeoilTank1000within50ticks(GameTestHelper helper) {
        IhGameTests.pumpCrudeOilIntoTank(helper);
    }

    @GameTest(maxTicks = 60)
    public void pipesTwofluidsNevermixed(GameTestHelper helper) {
        IhGameTests.pipesNeverMix(helper);
    }

    @GameTest(maxTicks = 20)
    public void tankFillslotBucketbecomescrudeoilbucket(GameTestHelper helper) {
        IhGameTests.bucketFill(helper);
    }

    @GameTest(maxTicks = 20)
    public void generatorCoalBurns1600ticks(GameTestHelper helper) {
        IhGameTestsExtended.generatorCoalBurnTime(helper);
    }

    @GameTest(maxTicks = 20)
    public void generatorLavabucketKeepsbucket(GameTestHelper helper) {
        IhGameTestsExtended.generatorLavaBucketKeepsBucket(helper);
    }

    @GameTest(maxTicks = 20)
    public void crusherOutputfullStatuserror(GameTestHelper helper) {
        IhGameTestsExtended.crusherOutputFullError(helper);
    }

    @GameTest(maxTicks = 20)
    public void crusherFourspeedupgrades63ticks101jt(GameTestHelper helper) {
        IhGameTestsExtended.crusherFourSpeedUpgrades(helper);
    }

    @GameTest(maxTicks = 40)
    public void pressWithoutmoldNoprogress(GameTestHelper helper) {
        IhGameTestsExtended.pressWithoutMoldIdle(helper);
    }

    @GameTest(maxTicks = 110)
    public void furnaceRawironIroningotwithin110ticks(GameTestHelper helper) {
        IhGameTestsExtended.furnaceVanillaSmelting(helper);
    }

    @GameTest(maxTicks = 230)
    public void crusherStatusIdlerunningidle(GameTestHelper helper) {
        IhGameTestsExtended.crusherStatusCycle(helper);
    }

    @GameTest(maxTicks = 300)
    public void hopperFeedsanddrainsCrusher(GameTestHelper helper) {
        IhGameTestsExtended.hopperFeedsAndDrainsCrusher(helper);
    }

    @GameTest(maxTicks = 40)
    public void hopperNeverfillsUpgradeslot(GameTestHelper helper) {
        IhGameTestsExtended.hopperNeverFillsUpgradeSlot(helper);
    }

    @GameTest(maxTicks = 20)
    public void containerslotsHopperrules(GameTestHelper helper) {
        IhGameTestsExtended.containerSlotHopperRules(helper);
    }

    @GameTest(maxTicks = 20)
    public void tankDrainslotWaterbucket(GameTestHelper helper) {
        IhGameTestsExtended.tankDrainWaterBucket(helper);
    }

    @GameTest(maxTicks = 20)
    public void tankBrokenKeepscontents(GameTestHelper helper) {
        IhGameTestsExtended.tankKeepsContentsWhenBroken(helper);
    }

    @GameTest(maxTicks = 20)
    public void menuCreateMatcheslayout(GameTestHelper helper) {
        IhGameTestsExtended.menuMatchesLayout(helper);
    }

    @GameTest(maxTicks = 20)
    public void menuShiftclickRouting(GameTestHelper helper) {
        IhGameTestsExtended.shiftClickRouting(helper);
    }

    @GameTest(maxTicks = 20)
    public void payloadMachineconfigOnlyinrange(GameTestHelper helper) {
        IhGameTestsExtended.configPayloadRange(helper);
    }

    @GameTest(maxTicks = 20)
    public void cableThroughput256jt(GameTestHelper helper) {
        IhGameTestsExtended.cableThroughputLimit(helper);
    }

    @GameTest(maxTicks = 20)
    public void pipeThroughput200mbt(GameTestHelper helper) {
        IhGameTestsExtended.pipeThroughputLimit(helper);
    }

    @GameTest(maxTicks = 20)
    public void platformapiEnergyInsertsintocrusher(GameTestHelper helper) {
        IhGameTestsExtended.energyPlatformApiInsert(helper);
    }

    @GameTest(maxTicks = 20)
    public void platformapiFluidRespectsoutputside(GameTestHelper helper) {
        IhGameTestsExtended.fluidPlatformApiInsert(helper);
    }

    @GameTest(maxTicks = 60)
    public void pumpWaterRemovessource(GameTestHelper helper) {
        IhGameTestsExtended.pumpWaterRemovesSource(helper);
    }

    @GameTest(maxTicks = 20)
    public void worldgenOilbubbleFeature(GameTestHelper helper) {
        IhGameTestsExtended.oilBubbleFeature(helper);
    }

    @GameTest(maxTicks = 20)
    public void worldgenBiomesContainfeatures(GameTestHelper helper) {
        IhGameTestsExtended.worldgenBiomeFeatures(helper);
    }

    @GameTest(maxTicks = 20)
    public void configDefaultsLoaded(GameTestHelper helper) {
        IhGameTestsExtended.configDefaults(helper);
    }

    @GameTest(maxTicks = 220)
    public void refineryCrudeoilKerosene600plastic1within220ticks(GameTestHelper helper) {
        IhGameTestsM2d.refineryCrudeOilToKerosene(helper);
    }

    @GameTest(maxTicks = 20)
    public void refineryMissingcasingErrorthenidle(GameTestHelper helper) {
        IhGameTestsM2d.refineryMissingCasingThenIdle(helper);
    }

    @GameTest(maxTicks = 20)
    public void refineryWrongblockGhostpreview(GameTestHelper helper) {
        IhGameTestsM2d.refineryWrongBlockAndGhostPreview(helper);
    }

    @GameTest(maxTicks = 20)
    public void refineryPortsForwardtocontroller(GameTestHelper helper) {
        IhGameTestsM2d.refineryPortsForwardToController(helper);
    }

    @GameTest(maxTicks = 20)
    public void portModeToggleemptyhand(GameTestHelper helper) {
        IhGameTestsM2d.portModeToggle(helper);
    }

    @GameTest(maxTicks = 20)
    public void assemblyExactmatchFluidportnotcasing(GameTestHelper helper) {
        IhGameTestsM2d.assemblyExactMatch(helper);
    }

    @GameTest(maxTicks = 220)
    public void assemblyCircuitWithin220ticks(GameTestHelper helper) {
        IhGameTestsM2d.assemblyCircuit(helper);
    }

    @GameTest(maxTicks = 44)
    public void electrolyzerWaterH2o2within44ticks(GameTestHelper helper) {
        IhGameTestsM2d.electrolyzerWater(helper);
    }

    @GameTest(maxTicks = 20)
    public void electrolyzerFullgastankError(GameTestHelper helper) {
        IhGameTestsM2d.electrolyzerFullError(helper);
    }

    @GameTest(maxTicks = 20)
    public void electrolyzerTankselection(GameTestHelper helper) {
        IhGameTestsM2d.electrolyzerTankSelection(helper);
    }

    @GameTest(maxTicks = 20)
    public void tankRejectsGas(GameTestHelper helper) {
        IhGameTestsM2d.tankRejectsGas(helper);
    }

    @GameTest(maxTicks = 20)
    public void fuelgeneratorKerosenebucket80jt(GameTestHelper helper) {
        IhGameTestsM2e.fuelGeneratorKeroseneBucket(helper);
    }

    @GameTest(maxTicks = 30)
    public void fuelgenerator4mb16ticks(GameTestHelper helper) {
        IhGameTestsM2e.fuelGeneratorFourMbSixteenTicks(helper);
    }

    @GameTest(maxTicks = 20)
    public void batteryOutputsideonly(GameTestHelper helper) {
        IhGameTestsM2e.batteryOutputSideOnly(helper);
    }

    @GameTest(maxTicks = 20)
    public void batteryIo512Platformapi(GameTestHelper helper) {
        IhGameTestsM2e.batteryIoLimitAndPlatform(helper);
    }

    @GameTest(maxTicks = 20)
    public void batteryChargeslot(GameTestHelper helper) {
        IhGameTestsM2e.batteryChargeSlot(helper);
    }

    @GameTest(maxTicks = 20)
    public void batteryBrokenKeepsenergy(GameTestHelper helper) {
        IhGameTestsM2e.batteryKeepsEnergyWhenBroken(helper);
    }

    @GameTest(maxTicks = 240, structure = "infinite_horizon:gametest/empty_large", environment = "infinite_horizon:performance")
    public void performance100crushersMeantick(GameTestHelper helper) {
        IhGameTestsM2e.performanceHundredCrushers(helper);
    }

    @GameTest(maxTicks = 20)
    public void handbookJoingiftOnce(GameTestHelper helper) {
        IhGameTestsM2f.handbookGiftOnce(helper);
    }

    @GameTest(maxTicks = 220)
    public void crusherBauxiteTwoaluminumdustwithin220ticks(GameTestHelper helper) {
        IhGameTestsM2f.crusherBauxite(helper);
    }

    @GameTest(maxTicks = 110)
    public void furnaceQuartzsandSiliconwithin110ticks(GameTestHelper helper) {
        IhGameTestsM2f.furnaceQuartzSand(helper);
    }

    @GameTest(maxTicks = 110)
    public void pressTitaniumplatewithin110ticks(GameTestHelper helper) {
        IhGameTestsM2f.pressTitaniumPlate(helper);
    }
}
