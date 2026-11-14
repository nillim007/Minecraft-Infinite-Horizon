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
    public static final Supplier<Item> DOME_GENERATOR_T1 = blockItem("dome_generator_t1", ModBlocks.DOME_GENERATOR_T1);
    public static final Supplier<Item> DOME_GENERATOR_T2 = blockItem("dome_generator_t2", ModBlocks.DOME_GENERATOR_T2);
    public static final Supplier<Item> ELECTRIC_LAMP = blockItem("electric_lamp", ModBlocks.ELECTRIC_LAMP);
    public static final Supplier<Item> EXTINGUISHED_TORCH = blockItem("extinguished_torch", ModBlocks.EXTINGUISHED_TORCH);
    public static final Supplier<Item> RESEARCH_TERMINAL = blockItem("research_terminal", ModBlocks.RESEARCH_TERMINAL);
    public static final Supplier<Item> ANALYZER = blockItem("analyzer", ModBlocks.ANALYZER);
    public static final Supplier<Item> SOLAR_PANEL = blockItem("solar_panel", ModBlocks.SOLAR_PANEL);
    public static final Supplier<Item> ALUMINUM_CABLE = blockItem("aluminum_cable", ModBlocks.ALUMINUM_CABLE);
    public static final Supplier<Item> LAUNCH_PAD = blockItem("launch_pad", ModBlocks.LAUNCH_PAD);
    public static final Supplier<Item> LAUNCH_TOWER = blockItem("launch_tower", ModBlocks.LAUNCH_TOWER);
    public static final Supplier<Item> FUEL_PUMP = blockItem("fuel_pump", ModBlocks.FUEL_PUMP);
    public static final Supplier<Item> LAUNCH_CONTROL = blockItem("launch_control", ModBlocks.LAUNCH_CONTROL);
    public static final Supplier<Item> PROBE = blockItem("probe", ModBlocks.PROBE);
    public static final Supplier<Item> LANDING_BEACON = blockItem("landing_beacon", ModBlocks.LANDING_BEACON);
    public static final Supplier<Item> STATION_CORE = register("station_core",
            p -> new de.infinitehorizon.station.StationCoreItem(ModBlocks.STATION_CORE.get(), p.useBlockDescriptionPrefix()));
    public static final Supplier<Item> STATION_HULL = blockItem("station_hull", ModBlocks.STATION_HULL);
    public static final Supplier<Item> FUSION_REACTOR = blockItem("fusion_reactor", ModBlocks.FUSION_REACTOR);
    public static final Supplier<Item> FUSION_CASING = blockItem("fusion_casing", ModBlocks.FUSION_CASING);
    public static final Supplier<Item> SUPERCONDUCTOR_CABLE = blockItem("superconductor_cable", ModBlocks.SUPERCONDUCTOR_CABLE);
    public static final Supplier<Item> GRAVITY_PLATE = blockItem("gravity_plate", ModBlocks.GRAVITY_PLATE);
    public static final Supplier<Item> ZERO_G_FABRICATOR = blockItem("zero_g_fabricator", ModBlocks.ZERO_G_FABRICATOR);
    public static final Supplier<Item> SPACE_WEATHER_STATION = blockItem("space_weather_station", ModBlocks.SPACE_WEATHER_STATION);
    public static final Supplier<Item> DOCKING_BAY = blockItem("docking_bay", ModBlocks.DOCKING_BAY);
    public static final Supplier<Item> TANK_DEPOT = blockItem("tank_depot", ModBlocks.TANK_DEPOT);
    public static final Supplier<Item> SOLAR_WING = blockItem("solar_wing", ModBlocks.SOLAR_WING);
    public static final Supplier<Item> HABITATION_MODULE = blockItem("habitation_module", ModBlocks.HABITATION_MODULE);
    public static final Supplier<Item> RESEARCH_LAB = blockItem("research_lab", ModBlocks.RESEARCH_LAB);
    public static final Supplier<Item> GREENHOUSE = blockItem("greenhouse", ModBlocks.GREENHOUSE);
    public static final Supplier<Item> STATION_WINDOW = blockItem("station_window", ModBlocks.STATION_WINDOW);
    public static final Supplier<Item> FOOD_PACKER = blockItem("food_packer", ModBlocks.FOOD_PACKER);
    public static final Supplier<Item> HYDROPONICS_BASIN = blockItem("hydroponics_basin", ModBlocks.HYDROPONICS_BASIN);
    public static final Supplier<Item> COMPOST_FARMLAND = blockItem("compost_farmland", ModBlocks.COMPOST_FARMLAND);
    public static final Supplier<Item> REGOLITH_HEATER = blockItem("regolith_heater", ModBlocks.REGOLITH_HEATER);
    public static final Supplier<Item> HIGH_TEMPERATURE_FURNACE = blockItem("high_temperature_furnace", ModBlocks.HIGH_TEMPERATURE_FURNACE);
    public static final Supplier<Item> CRYO_LIQUEFIER = blockItem("cryo_liquefier", ModBlocks.CRYO_LIQUEFIER);
    public static final Supplier<Item> GAS_TANK = blockItem("gas_tank", ModBlocks.GAS_TANK);
    public static final Supplier<Item> CLOUD_ELEVATOR = blockItem("cloud_elevator", ModBlocks.CLOUD_ELEVATOR);
    public static final Supplier<Item> ROCKET_CAPSULE_T1 = blockItem("rocket_capsule_t1", ModBlocks.ROCKET_CAPSULE_T1);
    public static final Supplier<Item> ROCKET_TANK_T1 = blockItem("rocket_tank_t1", ModBlocks.ROCKET_TANK_T1);
    public static final Supplier<Item> ROCKET_ENGINE_T1 = blockItem("rocket_engine_t1", ModBlocks.ROCKET_ENGINE_T1);
    public static final Supplier<Item> ROCKET_FIN_T1 = blockItem("rocket_fin_t1", ModBlocks.ROCKET_FIN_T1);
    public static final Supplier<Item> ROCKET_CAPSULE_T2 = blockItem("rocket_capsule_t2", ModBlocks.ROCKET_CAPSULE_T2);
    public static final Supplier<Item> ROCKET_TANK_T2 = blockItem("rocket_tank_t2", ModBlocks.ROCKET_TANK_T2);
    public static final Supplier<Item> ROCKET_ENGINE_T2 = blockItem("rocket_engine_t2", ModBlocks.ROCKET_ENGINE_T2);
    public static final Supplier<Item> ROCKET_FIN_T2 = blockItem("rocket_fin_t2", ModBlocks.ROCKET_FIN_T2);
    public static final Supplier<Item> ROCKET_CARGO_BAY = blockItem("rocket_cargo_bay", ModBlocks.ROCKET_CARGO_BAY);
    /** Probenzange (11.2): Rechtsklick auf einen Block gibt eine Probe. */
    public static final Supplier<Item> SAMPLE_TOOL = register("sample_tool",
            p -> new de.infinitehorizon.research.SampleTool.ToolItem(p.stacksTo(1)));
    /** Probe (11.2) mit Komponente {@code sample}; gleiche Proben stapeln bis 64. */
    public static final Supplier<Item> SAMPLE = register("sample", de.infinitehorizon.research.SampleTool.SampleItem::new);

    /** Druckanzug T1 (9.1): Rüstung wie Eisen, Brustplatte mit O₂-Vorrat (Komponente {@code infinite_horizon:oxygen}). */
    public static final Supplier<Item> PRESSURE_SUIT_HELMET = suitPiece("pressure_suit", "helmet",
            net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final Supplier<Item> PRESSURE_SUIT_CHESTPLATE = suitPiece("pressure_suit", "chestplate",
            net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final Supplier<Item> PRESSURE_SUIT_LEGGINGS = suitPiece("pressure_suit", "leggings",
            net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final Supplier<Item> PRESSURE_SUIT_BOOTS = suitPiece("pressure_suit", "boots",
            net.minecraft.world.item.equipment.ArmorType.BOOTS);
    /** Hitzeanzug T2 (9.1): Rüstung wie Diamant, Hitzezeit auf der Venus-Oberfläche. */
    public static final Supplier<Item> HEAT_SUIT_HELMET = suitPiece("heat_suit", "helmet",
            net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final Supplier<Item> HEAT_SUIT_CHESTPLATE = suitPiece("heat_suit", "chestplate",
            net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final Supplier<Item> HEAT_SUIT_LEGGINGS = suitPiece("heat_suit", "leggings",
            net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final Supplier<Item> HEAT_SUIT_BOOTS = suitPiece("heat_suit", "boots",
            net.minecraft.world.item.equipment.ArmorType.BOOTS);

    /** Weltraumration (17): Nahrung und Sättigung aus der Komponente nutrition; Stapelgröße aus agriculture.json. */
    public static final Supplier<Item> SPACE_RATION = register("space_ration",
            p -> new Item(p.food(new net.minecraft.world.food.FoodProperties(0, 0f, false))
                    .stacksTo(de.infinitehorizon.data.BalanceManager.getOrBundled().entry("agriculture",
                            de.infinitehorizon.InfiniteHorizon.id("space_ration").toString()).orElseThrow()
                            .get("max_stack").getAsInt())));

    /** Feldscanner (11.1, 18): Kreaturen analysieren. */
    public static final Supplier<Item> FIELD_SCANNER = register("field_scanner",
            p -> new de.infinitehorizon.creature.FieldScannerItem(p.stacksTo(1)));

    /** Rover-Bausatz (16): Rechtsklick auf den Boden stellt den Rover auf. */
    public static final Supplier<Item> ROVER_KIT = register("rover_kit",
            p -> new de.infinitehorizon.vehicle.RoverKitItem(p.stacksTo(1)));

    /** Planetenatlas (24.2): Rechtsklick öffnet das Atlas-GUI. */
    public static final Supplier<Item> PLANETARY_ATLAS = register("planetary_atlas",
            p -> new de.infinitehorizon.item.AtlasItem(p.stacksTo(1)));

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
