package de.sternenwerft.core.fluid;

import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Beschreibung eines Mod-Fluids aus {@code sternenwerft/fluids.json} (gen_resources aus items.json und 5.3).
 * {@code sourceBlock}: hat einen Weltblock (Quelle + fließend); {@code bucket}: Pfad des Eimer-Items oder null.
 */
public record FluidDef(String id, int color, boolean sourceBlock, @Nullable String bucket, int tickDelay,
                       int slopeFindDistance, int dropOff, float explosionResistance, boolean gas) {

    public static FluidDef fromJson(JsonObject o) {
        String color = o.get("color").getAsString().replace("#", "");
        return new FluidDef(o.get("id").getAsString(), 0xFF000000 | Integer.parseInt(color, 16),
                o.has("source_block") && o.get("source_block").getAsBoolean(),
                // Eimer als BucketItem nur bei Weltfluiden; Tank-Fluide (Kerosin) haben ein einfaches Eimer-Item (4.1)
                o.has("source_block") && o.get("source_block").getAsBoolean() && o.has("bucket") && !o.get("bucket").isJsonNull()
                        ? o.get("bucket").getAsString() : null,
                o.get("tick_delay").getAsInt(), o.get("slope_find_distance").getAsInt(), o.get("drop_off").getAsInt(),
                o.has("explosion_resistance") ? o.get("explosion_resistance").getAsFloat() : 100f,
                o.has("gas") && o.get("gas").getAsBoolean());
    }

    /** Registrierte Teile eines Fluids; {@code block} und {@code bucket} sind null, wenn es sie nicht gibt. */
    public record Handle(Supplier<? extends Fluid> source, Supplier<? extends Fluid> flowing,
                         @Nullable Supplier<? extends Block> block, @Nullable Supplier<? extends Item> bucket) {
    }
}
