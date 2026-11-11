package de.sternenwerft.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.sternenwerft.Sternenwerft;
import de.sternenwerft.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Items; Reihenfolge = Reihenfolge im Creative-Tab. Einfache Items ohne eigenes Verhalten (Materialien, Upgrades,
 * Pressformen) kommen aus {@code sternenwerft/simple_items.json}, die gen_resources aus implemented_ids.json erzeugt.
 */
public final class ModItems {

    private static final List<Supplier<? extends Item>> ALL = new ArrayList<>();
    private static final Map<String, Supplier<Item>> SIMPLE = new LinkedHashMap<>();

    public static final Supplier<Item> CRUSHER = blockItem("crusher", ModBlocks.CRUSHER);
    public static final Supplier<Item> ELECTRIC_FURNACE = blockItem("electric_furnace", ModBlocks.ELECTRIC_FURNACE);
    public static final Supplier<Item> PRESS = blockItem("press", ModBlocks.PRESS);
    public static final Supplier<Item> COMBUSTION_GENERATOR = blockItem("combustion_generator", ModBlocks.COMBUSTION_GENERATOR);
    public static final Supplier<Item> COPPER_CABLE = blockItem("copper_cable", ModBlocks.COPPER_CABLE);
    public static final Supplier<Item> COPPER_PIPE = blockItem("copper_pipe", ModBlocks.COPPER_PIPE);
    public static final Supplier<Item> FLUID_TANK = blockItem("fluid_tank", ModBlocks.FLUID_TANK);
    public static final Supplier<Item> FLUID_PUMP = blockItem("fluid_pump", ModBlocks.FLUID_PUMP);
    public static final Supplier<Item> FUEL_GENERATOR = blockItem("fuel_generator", ModBlocks.FUEL_GENERATOR);
    public static final Supplier<Item> BATTERY = blockItem("battery", ModBlocks.BATTERY);
    public static final Supplier<Item> ASSEMBLY_TABLE = blockItem("assembly_table", ModBlocks.ASSEMBLY_TABLE);
    public static final Supplier<Item> ELECTROLYZER = blockItem("electrolyzer", ModBlocks.ELECTROLYZER);
    public static final Supplier<Item> REFINERY_CONTROLLER = blockItem("refinery_controller", ModBlocks.REFINERY_CONTROLLER);
    public static final Supplier<Item> REFINERY_CASING = blockItem("refinery_casing", ModBlocks.REFINERY_CASING);
    public static final Supplier<Item> ITEM_PORT = blockItem("item_port", ModBlocks.ITEM_PORT);
    public static final Supplier<Item> FLUID_PORT = blockItem("fluid_port", ModBlocks.FLUID_PORT);
    public static final Supplier<Item> ENERGY_PORT = blockItem("energy_port", ModBlocks.ENERGY_PORT);

    static {
        ModBlocks.simpleBlocks().forEach((id, block) -> blockItem(id, block));
        for (String id : loadSimpleIds()) {
            SIMPLE.put(id, item(id));
        }
    }

    private ModItems() {
    }

    public static void init() {
    }

    /** Nimmt ein anderswo registriertes Item (z. B. Eimer) in den Creative-Tab auf. */
    public static void track(Supplier<? extends Item> item) {
        ALL.add(item);
    }

    public static List<Supplier<? extends Item>> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** Einfaches Item nach Pfad (z. B. {@code "iron_dust"}). */
    public static Supplier<Item> simple(String path) {
        Supplier<Item> s = SIMPLE.get(path);
        if (s == null) {
            throw new IllegalArgumentException("Item nicht registriert: " + Sternenwerft.MOD_ID + ":" + path);
        }
        return s;
    }

    private static List<String> loadSimpleIds() {
        List<String> ids = new ArrayList<>();
        try (InputStream in = ModItems.class.getResourceAsStream("/" + Sternenwerft.MOD_ID + "/simple_items.json")) {
            if (in == null) {
                Sternenwerft.LOG.error("simple_items.json fehlt – gen_resources.py ausführen");
                return ids;
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                for (JsonElement e : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("items")) {
                    ids.add(e.getAsString());
                }
            }
        } catch (IOException e) {
            Sternenwerft.LOG.error("simple_items.json nicht lesbar: {}", e.getMessage());
        }
        return ids;
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
