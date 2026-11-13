package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.environment.HazardTracker;
import de.infinitehorizon.event.EventService;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** GameTests für M5g (19): Sonnensturm, Meteoritenschauer, Säureregen. */
public final class IhGameTestsM5g {

    private IhGameTestsM5g() {
    }

    private static JsonObject event(String id) {
        return BalanceManager.get().entry("events", InfiniteHorizon.id(id).toString()).orElseThrow();
    }

    /** Sonnensturm: Strahlung + {@code radiation_bonus}, Solarfaktor × {@code solar_multiplier}. */
    public static void solarStormRadiationAndSolar(GameTestHelper h) {
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, InfiniteHorizon.id("moon"), null, null, 20);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 1, 2.5));
        try {
            EventService.clear(h.getLevel());
            int before = HazardTracker.evaluate(p, HazardTracker.rules()).radiation();
            EventService.force(h.getLevel(), EventService.SOLAR_STORM, 200);
            int after = HazardTracker.evaluate(p, HazardTracker.rules()).radiation();
            JsonObject storm = event("solar_storm");
            int bonus = storm.getAsJsonObject("effects").get("radiation_bonus").getAsInt();
            h.assertTrue(after == before + bonus, "Strahlung " + before + " → " + after);
            double solar = storm.getAsJsonObject("reward").get("solar_multiplier").getAsDouble();
            h.assertTrue(EventService.solarMultiplier(h.getLevel()) == solar, "Solarfaktor " + EventService.solarMultiplier(h.getLevel()));
        } finally {
            EventService.clear(h.getLevel());
            IhTestPlayers.remove(p);
            EnvironmentOverrides.remove(override);
        }
        h.succeed();
    }

    /** Sonnensturm: Maschine ohne Deckung pausiert, mit 3 Blöcken darüber nicht. */
    public static void solarStormPausesUncovered(GameTestHelper h) {
        BlockPos open = h.absolutePos(new BlockPos(1, 1, 1));
        BlockPos covered = new BlockPos(4, 1, 1);
        for (int y = 2; y <= 4; y++) {
            h.setBlock(covered.atY(y), Blocks.STONE);
        }
        try {
            EventService.force(h.getLevel(), EventService.SOLAR_STORM, 200);
            h.assertTrue(EventService.machinePaused(h.getLevel(), open), "offene Maschine läuft im Sturm");
            h.assertFalse(EventService.machinePaused(h.getLevel(), h.absolutePos(covered)), "gedeckte Maschine pausiert");
        } finally {
            EventService.clear(h.getLevel());
        }
        h.succeed();
    }

    /** Meteoriteneinschlag hinterlässt {@code meteorite_ore}. */
    public static void meteorLeavesOre(GameTestHelper h) {
        JsonObject fx = event("meteor_shower").getAsJsonObject("effects").deepCopy();
        fx.addProperty("impact_radius", 0);
        BlockPos land = EventService.impact(h.getLevel(), h.absolutePos(new BlockPos(3, 1, 3)), fx);
        h.assertTrue(h.getLevel().getBlockState(land).is(ModBlocks.simple("meteorite_ore").get()),
                "kein Meteoritenerz bei " + land + ": " + h.getLevel().getBlockState(land));
        h.getLevel().setBlockAndUpdate(land, Blocks.AIR.defaultBlockState());
        h.succeed();
    }

    /** Säureregen: Rüstung unter freiem Himmel verliert Haltbarkeit, der Hitzeanzug (T2) schützt. */
    public static void acidRainDamagesArmor(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 1, 2.5));
        int loss = event("acid_rain").getAsJsonObject("effects").get("durability_loss").getAsInt();
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        EventService.acid(h.getLevel(), p, loss);
        h.assertTrue(p.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == loss,
                "Helm ohne Schaden: " + p.getItemBySlot(EquipmentSlot.HEAD).getDamageValue());
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HEAT_SUIT_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HEAT_SUIT_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.HEAT_SUIT_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.HEAT_SUIT_BOOTS.get()));
        h.assertFalse(EventService.acid(h.getLevel(), p, loss), "Hitzeanzug schützt nicht");
        IhTestPlayers.remove(p);
        h.succeed();
    }
}
