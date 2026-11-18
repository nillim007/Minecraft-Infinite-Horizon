package de.infinitehorizon.food;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * {@code ammonia_fertilizer} (17): Rechtsklick auf eine Anlage aus {@code applies_to} (Hydrokultur-Becken, Gewächshaus)
 * düngt sie für {@code duration_ticks}; ein Dünger wird verbraucht.
 */
public class AmmoniaFertilizerItem extends Item {

    public AmmoniaFertilizerItem(Properties properties) {
        super(properties);
    }

    private static boolean applies(String block) {
        JsonObject f = BalanceManager.get().entry("agriculture", InfiniteHorizon.id("ammonia_fertilizer").toString())
                .orElse(new JsonObject());
        if (!f.has("applies_to")) {
            return false;
        }
        for (JsonElement e : f.getAsJsonArray("applies_to")) {
            if (e.getAsString().equals(block)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        String block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(ctx.getClickedPos()).getBlock()).toString();
        if (!applies(block) || !(level.getBlockEntity(ctx.getClickedPos()) instanceof HydroponicsBlockEntity basin)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel sl) {
            basin.fertilize(sl);
            ctx.getItemInHand().consume(1, ctx.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }
}
