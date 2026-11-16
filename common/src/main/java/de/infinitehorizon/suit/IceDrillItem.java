package de.infinitehorizon.suit;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.EnergyItems;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code ice_drill} (13.4): Rechtsklick gedrückt halten bohrt einen Schacht ({@code shaft.width} × {@code shaft.width})
 * unter dem Spieler nach unten, eine Schicht je {@code ticks_per_layer}, {@code energy_per_block} J je Block aus dem
 * internen Akku (Komponente {@code energy}, {@code energy_capacity}). Werte aus items.json.
 */
public class IceDrillItem extends Item {

    public IceDrillItem(Properties properties) {
        super(properties);
    }

    public static JsonObject config() {
        return BalanceManager.getOrBundled().entry("items", InfiniteHorizon.id("ice_drill").toString()).orElse(new JsonObject());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (EnergyItems.stored(player.getItemInHand(hand)) < config().get("energy_per_block").getAsLong()) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return APPROXIMATELY_INFINITE_USE_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof Player player)) {
            return;
        }
        int used = getUseDuration(stack, entity) - remaining;
        int every = Math.max(1, config().get("ticks_per_layer").getAsInt());
        if (used > 0 && used % every == 0 && drillLayer(sl, player, stack) == 0 && EnergyItems.stored(stack)
                < config().get("energy_per_block").getAsLong()) {
            player.releaseUsingItem();
        }
    }

    /**
     * Bohrt die Schicht unter den Füßen ({@code width} × {@code width} um die Spalte des Spielers) und gibt die Zahl der
     * entfernten Blöcke zurück. Flüssigkeiten, Luft, unzerstörbare Blöcke und Block-Entities bleiben.
     */
    public static int drillLayer(ServerLevel level, Player player, ItemStack stack) {
        JsonObject c = config();
        long per = c.get("energy_per_block").getAsLong();
        int half = c.getAsJsonObject("shaft").get("width").getAsInt() / 2;
        BlockPos below = player.blockPosition().below();
        int n = 0;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                BlockPos p = below.offset(dx, 0, dz);
                BlockState s = level.getBlockState(p);
                if (s.isAir() || !s.getFluidState().isEmpty() && s.getFluidState().isSource() || s.hasBlockEntity()
                        || s.getDestroySpeed(level, p) < 0 || !level.mayInteract(player, p)) {
                    continue;
                }
                long energy = EnergyItems.stored(stack);
                if (energy < per) {
                    return n;
                }
                if (level.destroyBlock(p, true, player)) {
                    stack.set(ModDataComponents.ENERGY.get(), energy - per);
                    n++;
                }
            }
        }
        if (n > 0) {
            de.infinitehorizon.creature.ReefKeeper.provoke(level, player, below,
                    de.infinitehorizon.creature.ReefKeeper.BLOCK_BREAK);
        }
        return n;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long cap = EnergyItems.componentCapacity(stack);
        return cap <= 0 ? 0 : (int) Math.round(MAX_BAR_WIDTH * (double) EnergyItems.stored(stack) / cap);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x4FC3F7;
    }
}
