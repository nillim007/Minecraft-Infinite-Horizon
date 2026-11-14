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

    @GameTest(maxTicks = 420, structure = "infinite_horizon:gametest/empty_large", environment = "infinite_horizon:performance")
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

    @GameTest(maxTicks = 20)
    public void agencyPlacedcrusherOwnedbyplayeragency(GameTestHelper helper) {
        IhGameTestsM3a.placedCrusherHasOwner(helper);
    }

    @GameTest(maxTicks = 20)
    public void agencySetblockcrusherNoowner(GameTestHelper helper) {
        IhGameTestsM3a.setBlockCrusherHasNoOwner(helper);
    }

    @GameTest(maxTicks = 20)
    public void agencyMergechainOwnerrewritten(GameTestHelper helper) {
        IhGameTestsM3a.ownerFollowsMergeChain(helper);
    }

    @GameTest(maxTicks = 20)
    public void agencyTransfercommandSetsmergedinto(GameTestHelper helper) {
        IhGameTestsM3a.transferCommand(helper);
    }

    @GameTest(maxTicks = 210, environment = "infinite_horizon:moon_day")
    public void environmentMoonoverrideHazardfullafter200ticks(GameTestHelper helper) {
        IhGameTestsM3b.moonVacuumHazardBuildup(helper);
    }

    @GameTest(maxTicks = 40)
    public void gravityMoonoverrideGravityandsafefall(GameTestHelper helper) {
        IhGameTestsM3b.moonGravity(helper);
    }

    @GameTest(maxTicks = 60)
    public void environmentCreativeNobuildup(GameTestHelper helper) {
        IhGameTestsM3b.creativeNoBuildup(helper);
    }

    @GameTest(maxTicks = 20, dimension = "infinite_horizon:moon")
    public void moonAluminumoreDropsonebauxite(GameTestHelper helper) {
        IhGameTestsM3c.moonAluminumOreDrops(helper);
    }

    @GameTest(maxTicks = 20, dimension = "infinite_horizon:moon")
    public void moonPlayerGravitywithoutoverride(GameTestHelper helper) {
        IhGameTestsM3c.moonGravityWithoutOverride(helper);
    }

    @GameTest(maxTicks = 200)
    public void moonGotoLandsonsolidground(GameTestHelper helper) {
        IhGameTestsM3c.gotoLandsOnSolidGround(helper);
    }

    @GameTest(maxTicks = 220)
    public void suitMoonoverrideNostrain590mb(GameTestHelper helper) {
        IhGameTestsM3d.suitProtectsOnMoon(helper);
    }

    @GameTest(maxTicks = 20)
    public void suitIncompleteNoprotection(GameTestHelper helper) {
        IhGameTestsM3d.incompleteSuitDoesNotProtect(helper);
    }

    @GameTest(maxTicks = 20)
    public void oxygengeneratorFillslotCanisterfullafter1tick(GameTestHelper helper) {
        IhGameTestsM3d.generatorFillsCanister(helper);
    }

    @GameTest(maxTicks = 20)
    public void pressurebottleOnlygas(GameTestHelper helper) {
        IhGameTestsM3d.pressureBottleOnlyGas(helper);
    }

    @GameTest(maxTicks = 60, structure = "infinite_horizon:gametest/empty_large")
    public void domeMoonoverrideBreathable(GameTestHelper helper) {
        IhGameTestsM3e.domeMakesAirBreathable(helper);
    }

    @GameTest(maxTicks = 720, structure = "infinite_horizon:gametest/empty_large")
    public void dome1mbFailing24Off600(GameTestHelper helper) {
        IhGameTestsM3e.domeFailsThenTurnsOff(helper);
    }

    @GameTest(maxTicks = 40, structure = "infinite_horizon:gametest/empty_large")
    public void domeTorchOnlyinside(GameTestHelper helper) {
        IhGameTestsM3e.torchOnlyInsideDome(helper);
    }

    @GameTest(maxTicks = 20)
    public void atmosphereTorchesExtinguish(GameTestHelper helper) {
        IhGameTestsM3e.torchesExtinguish(helper);
    }

    @GameTest(maxTicks = 20)
    public void atmosphereWaterFreezes(GameTestHelper helper) {
        IhGameTestsM3e.waterFreezes(helper);
    }

    @GameTest(maxTicks = 20)
    public void lampLitOnlywithpower(GameTestHelper helper) {
        IhGameTestsM3e.lampNeedsPower(helper);
    }

    @GameTest(maxTicks = 250)
    public void researchAssemblytableLockeduntilresearched(GameTestHelper helper) {
        IhGameTestsM4a.assemblyLockedUntilResearched(helper);
    }

    @GameTest(maxTicks = 220, environment = "infinite_horizon:research_free")
    public void researchNotrequiredAssemblesimmediately(GameTestHelper helper) {
        IhGameTestsM4a.assemblyFreeWhenNotRequired(helper);
    }

    @GameTest(maxTicks = 40)
    public void researchTerminalOpenstier1groundstation(GameTestHelper helper) {
        IhGameTestsM4a.terminalOpensTier1(helper);
    }

    @GameTest(maxTicks = 20)
    public void sampleNaturalbauxiteOncepereposition(GameTestHelper helper) {
        IhGameTestsM4b.sampleToolOncePerPosition(helper);
    }

    @GameTest(maxTicks = 110)
    public void analyzerOnesampleOnefpwithin110ticks(GameTestHelper helper) {
        IhGameTestsM4b.analyzerOneSampleOneFp(helper);
    }

    @GameTest(maxTicks = 60, environment = "infinite_horizon:moon_day")
    public void solarMoonoverride5jtPolarcrater0(GameTestHelper helper) {
        IhGameTestsM4b.solarPanelMoon(helper);
    }

    @GameTest(maxTicks = 60, dimension = "infinite_horizon:moon")
    public void explorationLavatube20fp(GameTestHelper helper) {
        IhGameTestsM4b.lavaTubeGivesFp(helper);
    }

    @GameTest(maxTicks = 20)
    public void agencyGuestNoslotsnobreakThenjoin(GameTestHelper helper) {
        IhGameTestsM4c.guestThenMember(helper);
    }

    @GameTest(maxTicks = 40, structure = "infinite_horizon:gametest/empty_large")
    public void agencyMoonoverrideDomeprotection(GameTestHelper helper) {
        IhGameTestsM4c.domeProtection(helper);
    }

    @GameTest(maxTicks = 20)
    public void agencyUnownedcrusherClaimedonopen(GameTestHelper helper) {
        IhGameTestsM4c.unownedClaimedOnOpen(helper);
    }

    @GameTest(maxTicks = 20)
    public void fuelEarthtomoonFromregistries35360(GameTestHelper helper) {
        IhGameTestsM4d.fuelCostFromRegistries(helper);
    }

    @GameTest(maxTicks = 20)
    public void launchThreefinsRejectedFourthfinReady(GameTestHelper helper) {
        IhGameTestsM4d.checklistNeedsFourFins(helper);
    }

    @GameTest(maxTicks = 300)
    public void fuelpumpThreetanksEvenlyFull(GameTestHelper helper) {
        IhGameTestsM4d.fuelPumpFillsEvenly(helper);
    }

    @GameTest(maxTicks = 1320)
    public void probeMoonArriveswithin1320ticks(GameTestHelper helper) {
        IhGameTestsM4d.probeReachesMoon(helper);
    }

    @GameTest(maxTicks = 20)
    public void flightT1MoonandbackFuelDisassemble(GameTestHelper helper) {
        IhGameTestsM4e.flightToMoonAndBack(helper);
    }

    @GameTest(maxTicks = 20)
    public void flightBeaconMoon100Within3(GameTestHelper helper) {
        IhGameTestsM4e.beaconLandingWithinRadius(helper);
    }

    @GameTest(maxTicks = 20)
    public void flightLogoutinflightReloginseated(GameTestHelper helper) {
        IhGameTestsM4e.logoutInFlightRelogSeated(helper);
    }

    @GameTest(maxTicks = 20)
    public void atlasGiftOnceevenwithouttag(GameTestHelper helper) {
        IhGameTestsM4f.atlasGiftOnce(helper);
    }

    @GameTest(maxTicks = 40)
    public void atlasMoonvisitDiscoverer(GameTestHelper helper) {
        IhGameTestsM4f.moonVisitDiscoverer(helper);
    }

    @GameTest(maxTicks = 300)
    public void suitmoduleHelmetlampInstallremove(GameTestHelper helper) {
        IhGameTestsM4f.moduleInstallRemove(helper);
    }

    @GameTest(maxTicks = 60)
    public void helmetlampLight14Gonewhenremoved(GameTestHelper helper) {
        IhGameTestsM4f.helmetLampLight(helper);
    }

    @GameTest(maxTicks = 20)
    public void ringoneOredrops(GameTestHelper helper) {
        IhGameTestsM5a.ringOneOreDrops(helper);
    }

    @GameTest(maxTicks = 20)
    public void venusLandingSolidground(GameTestHelper helper) {
        IhGameTestsM5a.venusLandingOnSolidGround(helper);
    }

    @GameTest(maxTicks = 440)
    public void hightempfurnaceRawtungstenIngotwithin440(GameTestHelper helper) {
        IhGameTestsM5b.highTempFurnaceTungsten(helper);
    }

    @GameTest(maxTicks = 40)
    public void regolithheaterOverworldErrorlocation(GameTestHelper helper) {
        IhGameTestsM5b.regolithHeaterNeedsMoon(helper);
    }

    @GameTest(maxTicks = 120)
    public void regolithheaterMoonoverride50mbhelium(GameTestHelper helper) {
        IhGameTestsM5b.regolithHeaterMoonHelium(helper);
    }

    @GameTest(maxTicks = 40)
    public void cryoliquefier40helium10liquid(GameTestHelper helper) {
        IhGameTestsM5b.cryoLiquefiesHelium(helper);
    }

    @GameTest(maxTicks = 20)
    public void researchTier3gateMagnesiumtungstenhelium(GameTestHelper helper) {
        IhGameTestsM5b.tierThreeGate(helper);
    }

    @GameTest(maxTicks = 20, structure = "infinite_horizon:gametest/empty_large")
    public void rocketT2FivepadRejected(GameTestHelper helper) {
        IhGameTestsM5c.t2NeedsSevenPad(helper);
    }

    @GameTest(maxTicks = 20, structure = "infinite_horizon:gametest/empty_large")
    public void rocketT2SevenpadFlightmars(GameTestHelper helper) {
        IhGameTestsM5c.t2FlightToMars(helper);
    }

    @GameTest(maxTicks = 3700)
    public void venusHeattime3600(GameTestHelper helper) {
        IhGameTestsM5d.venusHeatTime(helper);
    }

    @GameTest(maxTicks = 20)
    public void venusAerostatLandingplatform(GameTestHelper helper) {
        IhGameTestsM5d.aerostatLandingPlatform(helper);
    }

    @GameTest(maxTicks = 20)
    public void venusCloudelevatorRide(GameTestHelper helper) {
        IhGameTestsM5d.cloudElevatorRide(helper);
    }

    @GameTest(maxTicks = 20)
    public void venusTerrainBelowclouds(GameTestHelper helper) {
        IhGameTestsM5d.venusTerrainBelowClouds(helper);
    }

    @GameTest(maxTicks = 20)
    public void roverKitRoundtrip(GameTestHelper helper) {
        IhGameTestsM5e.kitRoundTrip(helper);
    }

    @GameTest(maxTicks = 30)
    public void roverChargesAtbattery(GameTestHelper helper) {
        IhGameTestsM5e.chargesAtBattery(helper);
    }

    @GameTest(maxTicks = 20)
    public void roverDrillTocargo(GameTestHelper helper) {
        IhGameTestsM5e.drillToCargo(helper);
    }

    @GameTest(maxTicks = 20)
    public void roverCabinBreathable(GameTestHelper helper) {
        IhGameTestsM5e.cabinBreathable(helper);
    }

    @GameTest(maxTicks = 30, structure = "infinite_horizon:gametest/empty_large")
    public void creatureCrawlerReveals(GameTestHelper helper) {
        IhGameTestsM5f.crawlerReveals(helper);
    }

    @GameTest(maxTicks = 600, structure = "infinite_horizon:gametest/empty_large")
    public void creatureCrawlerEatscable(GameTestHelper helper) {
        IhGameTestsM5f.crawlerEatsCable(helper);
    }

    @GameTest(maxTicks = 400, structure = "infinite_horizon:gametest/empty_large")
    public void creatureBurrowerSeeksvibration(GameTestHelper helper) {
        IhGameTestsM5f.burrowerSeeksVibration(helper);
    }

    @GameTest(maxTicks = 20, structure = "infinite_horizon:gametest/empty_large")
    public void creatureSnailTrail(GameTestHelper helper) {
        IhGameTestsM5f.snailTrail(helper);
    }

    @GameTest(maxTicks = 20, structure = "infinite_horizon:gametest/empty_large")
    public void creatureCloudrayTamesaddlemount(GameTestHelper helper) {
        IhGameTestsM5f.cloudRayMount(helper);
    }

    @GameTest(maxTicks = 20, structure = "infinite_horizon:gametest/empty_large")
    public void creatureFieldscannerOncepertype(GameTestHelper helper) {
        IhGameTestsM5f.fieldScannerOnce(helper);
    }

    @GameTest(maxTicks = 20, structure = "infinite_horizon:gametest/empty_large")
    public void creatureWraithProjectileimmune(GameTestHelper helper) {
        IhGameTestsM5f.wraithProjectileImmune(helper);
    }

    @GameTest(maxTicks = 20, structure = "infinite_horizon:gametest/empty_large", environment = "infinite_horizon:moon_day")
    public void creatureMonsterNeedsdarkness(GameTestHelper helper) {
        IhGameTestsM5f.monsterNeedsDarkness(helper);
    }

    @GameTest(maxTicks = 20)
    public void eventSolarstormRadiationsolar(GameTestHelper helper) {
        IhGameTestsM5g.solarStormRadiationAndSolar(helper);
    }

    @GameTest(maxTicks = 20)
    public void eventSolarstormPausesuncovered(GameTestHelper helper) {
        IhGameTestsM5g.solarStormPausesUncovered(helper);
    }

    @GameTest(maxTicks = 20)
    public void eventMeteorLeavesore(GameTestHelper helper) {
        IhGameTestsM5g.meteorLeavesOre(helper);
    }

    @GameTest(maxTicks = 20)
    public void eventAcidrainDamagesarmor(GameTestHelper helper) {
        IhGameTestsM5g.acidRainDamagesArmor(helper);
    }

    @GameTest(maxTicks = 110)
    public void foodPackerThreefoodsRation(GameTestHelper helper) {
        IhGameTestsM5h.foodPackerRation(helper);
    }

    @GameTest(maxTicks = 20)
    public void foodCompostsoilHoeFarmland(GameTestHelper helper) {
        IhGameTestsM5h.compostSoilTills(helper);
    }

    @GameTest(maxTicks = 2200)
    public void foodHydroponicsStageandharvest(GameTestHelper helper) {
        IhGameTestsM5h.hydroponicsStageAndHarvest(helper);
    }

    @GameTest(maxTicks = 20)
    public void foodHydroponicsSaplingLogs(GameTestHelper helper) {
        IhGameTestsM5h.hydroponicsSaplingLogs(helper);
    }

    @GameTest(maxTicks = 40)
    public void foodHydroponicsMoonneedsdome(GameTestHelper helper) {
        IhGameTestsM5h.hydroponicsNeedsDome(helper);
    }

    @GameTest(maxTicks = 40, environment = "infinite_horizon:domes")
    public void domeT2RangemoduleRadiusenergy(GameTestHelper helper) {
        IhGameTestsM5i.domeT2RangeModule(helper);
    }

    @GameTest(maxTicks = 40, environment = "infinite_horizon:domes")
    public void domeShieldmoduleStopsmeteor(GameTestHelper helper) {
        IhGameTestsM5i.domeShieldStopsMeteor(helper);
    }

    @GameTest(maxTicks = 40, environment = "infinite_horizon:domes")
    public void domeT2Dustacidprotection(GameTestHelper helper) {
        IhGameTestsM5i.domeT2StormProtection(helper);
    }

    @GameTest(maxTicks = 20)
    public void suitmoduleCoolingArmorplating(GameTestHelper helper) {
        IhGameTestsM5i.suitModulesCoolingArmor(helper);
    }

    @GameTest(maxTicks = 20, dimension = "infinite_horizon:earth_orbit")
    public void orbitGravityMagnetboots(GameTestHelper helper) {
        IhGameTestsM6a.orbitGravityMagnetBoots(helper);
    }

    @GameTest(maxTicks = 20, dimension = "infinite_horizon:earth_orbit")
    public void orbitFallTosurfaceSlowfall(GameTestHelper helper) {
        IhGameTestsM6a.orbitFallToSurface(helper);
    }

    @GameTest(maxTicks = 60, dimension = "infinite_horizon:earth_orbit")
    public void stationCorePlatformBuiltindome(GameTestHelper helper) {
        IhGameTestsM6a.stationCorePlatformDome(helper);
    }

    @GameTest(maxTicks = 20, dimension = "infinite_horizon:earth_orbit")
    public void stationCoreAirplacement(GameTestHelper helper) {
        IhGameTestsM6a.stationCoreAirPlacement(helper);
    }

    @GameTest(maxTicks = 20)
    public void stationCoreOnlyinorbit(GameTestHelper helper) {
        IhGameTestsM6a.stationCoreOnlyInOrbit(helper);
    }

    @GameTest(maxTicks = 20, dimension = "infinite_horizon:earth_orbit")
    public void orbitArrivalHoverForeigncore(GameTestHelper helper) {
        IhGameTestsM6a.orbitArrivalHover(helper);
    }
}
