package de.sternenwerft.machine;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/** Vanilla-Brennwert eines Items in Ticks ({@code minecraft:cooking_fuel}, generators.json). */
public final class FuelHelper {

    private static final SlotProvider NO_SLOTS = new SlotProvider() {
        @Override
        public @Nullable SlotAccess getSlot(int slot) {
            return null;
        }
    };

    private FuelHelper() {
    }

    public static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.COOKING_FUEL);
    }

    /** Brenndauer wie im Ofen (Kontext CONTAINER_PROCESS); 0 = kein Brennstoff. */
    public static int burnTime(ServerLevel level, BlockEntity be, ItemStack fuel) {
        if (!isFuel(fuel)) {
            return 0;
        }
        LootContext ctx = new LootContext.Builder(new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, be.getBlockState())
                .withParameter(LootContextParams.BLOCK_ENTITY, be)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(be.getBlockPos()))
                .withParameter(LootContextParams.CONTAINER, NO_SLOTS)
                .create(LootContextParamSets.CONTAINER_PROCESS)).create(Optional.empty());
        return ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, ctx, 0);
    }
}
