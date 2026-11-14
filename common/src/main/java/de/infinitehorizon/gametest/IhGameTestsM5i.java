package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.dome.DomeData;
import de.infinitehorizon.dome.DomeGeneratorBlockEntity;
import de.infinitehorizon.event.EventService;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.suit.SuitModules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** GameTests für M5i (10.1, 9.2): Kuppel T2, Kuppelmodule, Anzugmodule der Stufe 2. */
public final class IhGameTestsM5i {

    private static final Identifier OXYGEN = InfiniteHorizon.id("oxygen");
    private static final BlockPos DOME = new BlockPos(4, 1, 4);

    private IhGameTestsM5i() {
    }

    private static JsonObject domes(String id) {
        return BalanceManager.get().entry("domes", InfiniteHorizon.id(id).toString()).orElseThrow();
    }

    /** Kuppelgenerator mit Strom (jeden Tick aufgefüllt) und vollem O₂-Tank, optional mit Modulen. */
    private static DomeGeneratorBlockEntity dome(GameTestHelper h, Block block, String... modules) {
        h.setBlock(DOME, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        DomeGeneratorBlockEntity be = h.getBlockEntity(DOME, DomeGeneratorBlockEntity.class);
        be.refreshLayout();
        be.tank().setStackUnchecked(IhFluidStack.of(OXYGEN, be.tank().capacity()));
        for (int i = 0; i < modules.length; i++) {
            ItemStack m = new ItemStack(ModItems.simple(modules[i]).get());
            int slot = be.layout().upgradeStart() + i;
            h.assertTrue(be.canPlaceItem(slot, m), "Modul passt nicht: " + modules[i]);
            be.setItem(slot, m);
        }
        h.onEachTick(() -> be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity()));
        return be;
    }

    /** T2: Radius 16; Reichweitenmodul × 1,5 Radius, × 2 Strom; ein zweites gleiches Modul passt nicht. */
    public static void domeT2RangeModule(GameTestHelper h) {
        DomeGeneratorBlockEntity be = dome(h, ModBlocks.DOME_GENERATOR_T2.get(), "dome_range_module");
        JsonObject t2 = domes("dome_generator_t2");
        JsonObject range = domes("dome_range_module");
        h.assertTrue(be.radius() == (int) Math.floor(t2.get("radius").getAsInt() * range.get("radius_multiplier").getAsDouble()),
                "Radius " + be.radius());
        h.assertTrue(be.energyPerTick() == Math.round(t2.get("energy_per_tick").getAsLong() * range.get("energy_multiplier").getAsDouble()),
                "Strom " + be.energyPerTick());
        h.assertFalse(be.canPlaceItem(be.layout().upgradeStart() + 1, new ItemStack(ModItems.simple("dome_range_module").get())),
                "zweites Reichweitenmodul angenommen");
        h.assertFalse(be.canPlaceItem(be.layout().upgradeStart() + 1, new ItemStack(ModItems.simple("helmet_lamp").get())),
                "Nicht-Modul angenommen");
        h.succeedWhen(() -> {
            DomeData.Entry e = DomeData.get(h.getLevel()).at(h.absolutePos(DOME));
            h.assertTrue(e != null && e.tier() == t2.get("tier").getAsInt() && e.radius() == be.radius(), "Kuppel " + e);
        });
    }

    /** Schildmodul fängt Meteoriten ab (10.1): Schutz in der Kuppel, nicht darüber. */
    public static void domeShieldStopsMeteor(GameTestHelper h) {
        dome(h, ModBlocks.DOME_GENERATOR_T1.get(), "dome_shield_module");
        BlockPos inside = h.absolutePos(DOME.east(2));
        h.succeedWhen(() -> {
            h.assertTrue(DomeData.get(h.getLevel()).at(h.absolutePos(DOME)) != null, "Kuppel nicht aktiv");
            h.assertTrue(EventService.domeProtects(h.getLevel(), inside, EventService.METEOR_SHOWER), "Schild fängt nicht ab");
            h.assertFalse(EventService.domeProtects(h.getLevel(), inside.above(100), EventService.METEOR_SHOWER),
                    "Schild wirkt außerhalb");
        });
    }

    /** Kuppel T2 schützt vor Staubsturm (Solarfaktor) und Säureregen; T1 nicht. */
    public static void domeT2StormProtection(GameTestHelper h) {
        dome(h, ModBlocks.DOME_GENERATOR_T2.get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(6.5, 1, 6.5));
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
        h.succeedWhen(() -> {
            try {
                h.assertTrue(DomeData.get(h.getLevel()).at(h.absolutePos(DOME)) != null, "Kuppel nicht aktiv");
                EventService.force(h.getLevel(), EventService.DUST_STORM, 200);
                h.assertTrue(EventService.solarMultiplier(h.getLevel(), h.absolutePos(DOME.east(2))) == 1.0,
                        "Staubsturm wirkt unter Kuppel T2");
                h.assertTrue(EventService.solarMultiplier(h.getLevel(), h.absolutePos(DOME).above(100)) < 1.0,
                        "Staubsturm wirkt draußen nicht");
                h.assertFalse(EventService.acid(h.getLevel(), p, 1), "Säureregen unter Kuppel T2");
            } finally {
                EventService.clear(h.getLevel());
            }
            IhTestPlayers.remove(p);
        });
    }

    /** Anzugmodule (9.2): Kühlmodul + {@code temp_max_bonus}, Panzerung + {@code armor_bonus} Rüstung. */
    public static void suitModulesCoolingArmor(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 1, 2.5));
        try {
            Identifier cooling = InfiniteHorizon.id("cooling_module");
            Identifier plating = InfiniteHorizon.id("armor_plating");
            ItemStack chest = new ItemStack(ModItems.HEAT_SUIT_CHESTPLATE.get());
            chest.set(ModDataComponents.SUIT_MODULES.get(), List.of(cooling));
            ItemStack legs = new ItemStack(ModItems.HEAT_SUIT_LEGGINGS.get());
            legs.set(ModDataComponents.SUIT_MODULES.get(), List.of(plating));
            double before = p.getAttributeValue(Attributes.ARMOR);
            p.setItemSlot(EquipmentSlot.CHEST, chest);
            p.setItemSlot(EquipmentSlot.LEGS, legs);
            double wantTemp = BalanceManager.get().entry("suits", cooling.toString()).orElseThrow().get("temp_max_bonus").getAsDouble();
            double wantArmor = BalanceManager.get().entry("suits", plating.toString()).orElseThrow().get("armor_bonus").getAsDouble();
            h.assertTrue(SuitModules.bonus(p, "temp_max_bonus") == wantTemp, "Kühlung " + SuitModules.bonus(p, "temp_max_bonus"));
            double plain = p.getAttributeValue(Attributes.ARMOR);
            SuitModules.tick(h.getLevel().getServer());
            h.assertTrue(p.getAttributeValue(Attributes.ARMOR) == plain + wantArmor,
                    "Rüstung " + before + " → " + plain + " → " + p.getAttributeValue(Attributes.ARMOR));
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }
}
