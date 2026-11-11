package de.sternenwerft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Dünne Fabric-Hülle um {@link SwGameTests} (Entry-Point „fabric-gametest“). */
public class SwFabricGameTests {

    @GameTest(maxTicks = SwGameTests.CRUSHER_CHAIN_TICKS)
    public void crusherRawIronTwoDustWithin220Ticks(GameTestHelper helper) {
        SwGameTests.crusherRawIronTwoDust(helper);
    }

    @GameTest(maxTicks = SwGameTests.BALANCE_RELOAD_TICKS)
    public void crusherBalanceChangeChangesDuration(GameTestHelper helper) {
        SwGameTests.crusherBalanceChangesDuration(helper);
    }

    @GameTest(maxTicks = SwGameTests.METAL_CHAIN_TICKS)
    public void machinesRawTitaniumToPlateWithin440Ticks(GameTestHelper helper) {
        SwGameTests.machinesRawTitaniumToPlate(helper);
    }

    @GameTest(maxTicks = SwGameTests.ORE_TOOLS_TICKS)
    public void oresDropOnlyWithCorrectTool(GameTestHelper helper) {
        SwGameTests.oresNeedCorrectTool(helper);
    }

    @GameTest(maxTicks = SwGameTests.PUMP_TANK_TICKS)
    public void pumpCrudeOilIntoTankWithin50Ticks(GameTestHelper helper) {
        SwGameTests.pumpCrudeOilIntoTank(helper);
    }

    @GameTest(maxTicks = SwGameTests.PIPES_NO_MIX_TICKS)
    public void pipesTwoFluidsNeverMix(GameTestHelper helper) {
        SwGameTests.pipesNeverMix(helper);
    }

    @GameTest(maxTicks = SwGameTests.BUCKET_FILL_TICKS)
    public void tankFillSlotBucketBecomesCrudeOilBucket(GameTestHelper helper) {
        SwGameTests.bucketFill(helper);
    }

    @GameTest(maxTicks = SwGameTests.PLATFORM_CONSUMER_TICKS)
    public void cablePlatformConsumerReceivesEnergy(GameTestHelper helper) {
        SwGameTests.cablePlatformConsumerReceivesEnergy(helper);
    }
}
