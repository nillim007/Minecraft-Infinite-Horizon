package de.sternenwerft.registry;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/** Items aus Meilenstein 1; Reihenfolge = Reihenfolge im Creative-Tab. */
public final class ModItems {

    private static final List<Supplier<? extends Item>> ALL = new ArrayList<>();

    public static final Supplier<Item> CRUSHER = blockItem("crusher", ModBlocks.CRUSHER);
    public static final Supplier<Item> COMBUSTION_GENERATOR = blockItem("combustion_generator", ModBlocks.COMBUSTION_GENERATOR);
    public static final Supplier<Item> COPPER_CABLE = blockItem("copper_cable", ModBlocks.COPPER_CABLE);
    public static final Supplier<Item> MACHINE_FRAME = item("machine_frame");
    public static final Supplier<Item> IRON_DUST = item("iron_dust");

    private ModItems() {
    }

    public static void init() {
    }

    public static List<Supplier<? extends Item>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, Sternenwerft.id(name));
    }

    public static Supplier<Item> item(String name) {
        Supplier<Item> s = Services.REGISTRY.register(BuiltInRegistries.ITEM, name,
                () -> new Item(new Item.Properties().setId(key(name))));
        ALL.add(s);
        return s;
    }

    public static Supplier<Item> blockItem(String name, Supplier<Block> block) {
        Supplier<Item> s = Services.REGISTRY.register(BuiltInRegistries.ITEM, name,
                () -> new BlockItem(block.get(), new Item.Properties().setId(key(name)).useBlockDescriptionPrefix()));
        ALL.add(s);
        return s;
    }
}
