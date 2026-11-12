package de.infinitehorizon.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.platform.Services;
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
 * Pressformen) kommen aus {@code infinite_horizon/simple_items.json}, die gen_resources aus implemented_ids.json erzeugt.
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
    public static final Supplier<Item> OXYGEN_GENERATOR = blockItem("oxygen_generator", ModBlocks.OXYGEN_GENERATOR);
    public static final Supplier<Item> PRESSURE_BOTTLE = blockItem("pressure_bottle", ModBlocks.PRESSURE_BOTTLE);

    /** Druckanzug T1 (9.1): Rüstung wie Eisen, Brustplatte mit O₂-Vorrat (Komponente {@code infinite_horizon:oxygen}). */
    public static final Supplier<Item> PRESSURE_SUIT_HELMET = suitPiece("pressure_suit", "helmet",
            net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final Supplier<Item> PRESSURE_SUIT_CHESTPLATE = suitPiece("pressure_suit", "chestplate",
            net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final Supplier<Item> PRESSURE_SUIT_LEGGINGS = suitPiece("pressure_suit", "leggings",
            net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final Supplier<Item> PRESSURE_SUIT_BOOTS = suitPiece("pressure_suit", "boots",
            net.minecraft.world.item.equipment.ArmorType.BOOTS);

    /** Ingenieurs-Handbuch (24.1): Rechtsklick öffnet das Buch-GUI. */
    public static final Supplier<Item> ENGINEERS_HANDBOOK = register("engineers_handbook",
            p -> new de.infinitehorizon.item.HandbookItem(p.stacksTo(1)));

    static {
        ModBlocks.simpleBlocks().forEach((id, block) -> blockItem(id, block));
        JsonObject simple = de.infinitehorizon.data.GeneratedData.read("simple_items");
        JsonObject stacks = simple.has("max_stack") ? simple.getAsJsonObject("max_stack") : new JsonObject();
        for (String id : loadSimpleIds()) {
            int max = stacks.has(id) ? stacks.get(id).getAsInt() : 0;
            SIMPLE.put(id, max > 0 ? register(id, p -> new Item(p.stacksTo(max))) : item(id));
        }
    }

    /** Anzugteil {@code <anzug>_<teil>} mit dem Rüstungsmaterial aus suits.json. */
    public static Supplier<Item> suitPiece(String suit, String piece, net.minecraft.world.item.equipment.ArmorType type) {
        return register(suit + "_" + piece, p -> new Item(p.stacksTo(1).humanoidArmor(
                de.infinitehorizon.item.SuitArmor.material(suit), type)));
    }

    /** Registriert ein Item mit eigener Fabrik (Eigenschaften mit gesetzter ID). */
    public static Supplier<Item> register(String name, java.util.function.Function<Item.Properties, Item> factory) {
        Supplier<Item> s = Services.REGISTRY.register(BuiltInRegistries.ITEM, name,
                () -> factory.apply(new Item.Properties().setId(key(name))));
        ALL.add(s);
        return s;
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
            throw new IllegalArgumentException("Item nicht registriert: " + InfiniteHorizon.MOD_ID + ":" + path);
        }
        return s;
    }

    private static List<String> loadSimpleIds() {
        List<String> ids = new ArrayList<>();
        try (InputStream in = ModItems.class.getResourceAsStream("/" + InfiniteHorizon.MOD_ID + "/simple_items.json")) {
            if (in == null) {
                InfiniteHorizon.LOG.error("simple_items.json fehlt – gen_resources.py ausführen");
                return ids;
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                for (JsonElement e : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("items")) {
                    ids.add(e.getAsString());
                }
            }
        } catch (IOException e) {
            InfiniteHorizon.LOG.error("simple_items.json nicht lesbar: {}", e.getMessage());
        }
        return ids;
    }

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, InfiniteHorizon.id(name));
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
