package de.infinitehorizon.registry;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.CableBlock;
import de.infinitehorizon.block.FluidPipeBlock;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.multiblock.MultiblockPartBlock;
import de.infinitehorizon.multiblock.PortBlock;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.GeneratedData;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** Blöcke mit eigenem Verhalten (IDs laut spec/data/items.json); einfache Blöcke kommen aus simple_blocks.json. */
public final class ModBlocks {

    /** Lichtstufe der elektrischen Lampe aus machines.json ({@code light_level}). */
    static int lampLight() {
        return de.infinitehorizon.data.BalanceManager.getOrBundled().entry("machines", "infinite_horizon:electric_lamp")
                .filter(o -> o.has("light_level")).map(o -> o.get("light_level").getAsInt()).orElse(0);
    }

    public static final Supplier<Block> CRUSHER = register("crusher",
            p -> new MachineBlock(p, () -> ModBlockEntities.CRUSHER.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> COMBUSTION_GENERATOR = register("combustion_generator",
            p -> new MachineBlock(p, () -> ModBlockEntities.COMBUSTION_GENERATOR.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> ELECTRIC_FURNACE = register("electric_furnace",
            p -> new MachineBlock(p, () -> ModBlockEntities.ELECTRIC_FURNACE.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> PRESS = register("press",
            p -> new MachineBlock(p, () -> ModBlockEntities.PRESS.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> FLUID_TANK = register("fluid_tank",
            p -> new MachineBlock(p, () -> ModBlockEntities.FLUID_TANK.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> FLUID_PUMP = register("fluid_pump",
            p -> new MachineBlock(p, () -> ModBlockEntities.FLUID_PUMP.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> FUEL_GENERATOR = register("fuel_generator",
            p -> new MachineBlock(p, () -> ModBlockEntities.FUEL_GENERATOR.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> BATTERY = register("battery",
            p -> new MachineBlock(p, () -> ModBlockEntities.BATTERY.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> ASSEMBLY_TABLE = register("assembly_table",
            p -> new MachineBlock(p, () -> ModBlockEntities.ASSEMBLY_TABLE.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> ELECTROLYZER = register("electrolyzer",
            p -> new MachineBlock(p, () -> ModBlockEntities.ELECTROLYZER.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> REFINERY_CONTROLLER = register("refinery_controller",
            p -> new MachineBlock(p, () -> ModBlockEntities.REFINERY_CONTROLLER.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> OXYGEN_GENERATOR = register("oxygen_generator",
            p -> new MachineBlock(p, () -> ModBlockEntities.OXYGEN_GENERATOR.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> PRESSURE_BOTTLE = register("pressure_bottle",
            p -> new MachineBlock(p, () -> ModBlockEntities.PRESSURE_BOTTLE.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> DOME_GENERATOR_T1 = register("dome_generator_t1",
            p -> new MachineBlock(p, () -> ModBlockEntities.DOME_GENERATOR_T1.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> DOME_GENERATOR_T2 = register("dome_generator_t2",
            p -> new MachineBlock(p, () -> ModBlockEntities.DOME_GENERATOR_T2.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    /** Elektrische Lampe (8.5): Licht nur mit Strom (Lichtstufe aus machines.json). */
    public static final Supplier<Block> ELECTRIC_LAMP = register("electric_lamp",
            p -> new de.infinitehorizon.machine.ElectricLampBlock(p, () -> ModBlockEntities.ELECTRIC_LAMP.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(s ->
                    s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? lampLight() : 0));
    /** Forschungsterminal (11.3): GUI-Block ohne Strom. */
    public static final Supplier<Block> RESEARCH_TERMINAL = register("research_terminal",
            p -> new de.infinitehorizon.research.ResearchTerminalBlock(p, () -> ModBlockEntities.RESEARCH_TERMINAL.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    /** Erloschene Fackel (8.5): dekorativ wie eine Fackel ohne Licht. */
    public static final Supplier<Block> EXTINGUISHED_TORCH = register("extinguished_torch",
            de.infinitehorizon.world.ExtinguishedTorchBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH).lightLevel(s -> 0));
    public static final Supplier<Block> REFINERY_CASING = register("refinery_casing", MultiblockPartBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> ITEM_PORT = register("item_port",
            p -> new PortBlock(p, PortBlock.PortType.ITEM, () -> ModBlockEntities.PORT.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> FLUID_PORT = register("fluid_port",
            p -> new PortBlock(p, PortBlock.PortType.FLUID, () -> ModBlockEntities.PORT.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> ENERGY_PORT = register("energy_port",
            p -> new PortBlock(p, PortBlock.PortType.ENERGY, () -> ModBlockEntities.PORT.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> COPPER_PIPE = register("copper_pipe", FluidPipeBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.COPPER).noOcclusion());
    public static final Supplier<Block> COPPER_CABLE = register("copper_cable", CableBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.COPPER).noOcclusion());
    /** Aluminiumkabel (4.4): eigene Netzsorte, Durchsatz aus transport.json. */
    public static final Supplier<Block> ALUMINUM_CABLE = register("aluminum_cable", CableBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.METAL).noOcclusion());
    /** Startrampe (4.3): Plattform, Turm, Pumpe, Steuerpult. */
    public static final Supplier<Block> LAUNCH_PAD = register("launch_pad", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> LAUNCH_TOWER = register("launch_tower", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> FUEL_PUMP = register("fuel_pump",
            p -> new MachineBlock(p, () -> ModBlockEntities.FUEL_PUMP.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> LAUNCH_CONTROL = register("launch_control",
            p -> new de.infinitehorizon.space.LaunchControlBlock(p, () -> ModBlockEntities.LAUNCH_CONTROL.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    /** Ring 1 (M5b): Maschinen der Stufe 2 und Gastank. */
    public static final Supplier<Block> REGOLITH_HEATER = register("regolith_heater",
            p -> new MachineBlock(p, () -> ModBlockEntities.REGOLITH_HEATER.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> HIGH_TEMPERATURE_FURNACE = register("high_temperature_furnace",
            p -> new MachineBlock(p, () -> ModBlockEntities.HIGH_TEMPERATURE_FURNACE.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> CRYO_LIQUEFIER = register("cryo_liquefier",
            p -> new MachineBlock(p, () -> ModBlockEntities.CRYO_LIQUEFIER.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> GAS_TANK = register("gas_tank",
            p -> new MachineBlock(p, () -> ModBlockEntities.GAS_TANK.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    /** Venus-Wolkenschicht (12.8): Aufzug zwischen Wolkenschicht und Oberfläche. */
    public static final Supplier<Block> CLOUD_ELEVATOR = register("cloud_elevator",
            p -> new de.infinitehorizon.space.CloudElevatorBlock(p, () -> ModBlockEntities.CLOUD_ELEVATOR.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    /** Schwefelspur der Schwefelschnecke (18): dünne, nicht feste Schicht ohne Drop. */
    public static final Supplier<Block> SULFUR_TRAIL = register("sulfur_trail", de.infinitehorizon.creature.SulfurTrailBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SNOW).noCollision().noLootTable().replaceable());
    /** Nahrung (17): Nahrungspacker, Hydrokultur-Becken, Kompost-Ackerland. */
    public static final Supplier<Block> FOOD_PACKER = register("food_packer",
            p -> new MachineBlock(p, () -> ModBlockEntities.FOOD_PACKER.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> HYDROPONICS_BASIN = register("hydroponics_basin",
            p -> new MachineBlock(p, () -> ModBlockEntities.HYDROPONICS_BASIN.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> COMPOST_FARMLAND = register("compost_farmland",
            de.infinitehorizon.food.CompostFarmlandBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.FARMLAND));
    /** Raumstation (14.1): Kern mit eingebauter Kuppel, Hülle (Plattform mit generated=true), Fenster. */
    public static final Supplier<Block> STATION_CORE = register("station_core",
            p -> new de.infinitehorizon.station.StationCoreBlock(p, () -> ModBlockEntities.STATION_CORE.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> STATION_HULL = register("station_hull", de.infinitehorizon.station.StationHullBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> STATION_WINDOW = register("station_window",
            de.infinitehorizon.station.StationWindowBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    /** Landefeuer (12.6): roter Strahl, Landeziel in der Navigationskarte. */
    public static final Supplier<Block> LANDING_BEACON = register("landing_beacon",
            p -> new de.infinitehorizon.space.LandingBeaconBlock(p, () -> ModBlockEntities.LANDING_BEACON.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    /** Sonde als Block auf der Rampe (12.3). */
    public static final Supplier<Block> PROBE = register("probe", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    /** Raketenteile T2 (12.4, M5c): von der 7×7-Rampe, Wasserstoff und Sauerstoff, 2 Sitze. */
    public static final Supplier<Block> ROCKET_CAPSULE_T2 = register("rocket_capsule_t2",
            de.infinitehorizon.space.CapsuleBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).isSuffocating((s, l, p) -> false));
    public static final Supplier<Block> ROCKET_TANK_T2 = register("rocket_tank_t2",
            p -> new de.infinitehorizon.space.RocketPartBlock(p, () -> ModBlockEntities.ROCKET_TANK_T2.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> ROCKET_ENGINE_T2 = register("rocket_engine_t2", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> ROCKET_FIN_T2 = register("rocket_fin_t2", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    /** Raketenteile T1 (12.4). */
    public static final Supplier<Block> ROCKET_CAPSULE_T1 = register("rocket_capsule_t1",
            de.infinitehorizon.space.CapsuleBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).isSuffocating((s, l, p) -> false));
    public static final Supplier<Block> ROCKET_TANK_T1 = register("rocket_tank_t1",
            p -> new de.infinitehorizon.space.RocketPartBlock(p, () -> ModBlockEntities.ROCKET_TANK_T1.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> ROCKET_ENGINE_T1 = register("rocket_engine_t1", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> ROCKET_FIN_T1 = register("rocket_fin_t1", Block::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> ROCKET_CARGO_BAY = register("rocket_cargo_bay",
            p -> new de.infinitehorizon.space.RocketPartBlock(p, () -> ModBlockEntities.ROCKET_CARGO_BAY.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    /** Analysator (6.1, 11.1): Proben → FP. */
    public static final Supplier<Block> ANALYZER = register("analyzer",
            p -> new MachineBlock(p, () -> ModBlockEntities.ANALYZER.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    /** Solarpanel (6.3): Strom bei Tageslicht und freiem Himmel. */
    public static final Supplier<Block> SOLAR_PANEL = register("solar_panel",
            p -> new MachineBlock(p, () -> ModBlockEntities.SOLAR_PANEL.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));

    private static final Map<String, Supplier<Block>> SIMPLE = new LinkedHashMap<>();

    static {
        // Einfache Bloecke (Erze, Sand ...) mit Eigenschaften eines Vanilla-Blocks (4.1), Liste aus gen_resources
        for (JsonElement e : GeneratedData.array("simple_blocks", "blocks")) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            Identifier copy = Identifier.parse(o.get("copy").getAsString());
            SIMPLE.put(id, register(id, Block::new,
                    () -> BlockBehaviour.Properties.ofFullCopy(BuiltInRegistries.BLOCK.getValue(copy))));
        }
    }

    private ModBlocks() {
    }

    /** Einfacher Block nach Pfad (z. B. {@code "bauxite_ore"}). */
    public static Supplier<Block> simple(String path) {
        Supplier<Block> s = SIMPLE.get(path);
        if (s == null) {
            throw new IllegalArgumentException("Block nicht registriert: " + InfiniteHorizon.MOD_ID + ":" + path);
        }
        return s;
    }

    public static Map<String, Supplier<Block>> simpleBlocks() {
        return Collections.unmodifiableMap(SIMPLE);
    }

    public static void init() {
    }

    /** Registriert einen Block; die ID wird vor dem Konstruktor gesetzt (Pflicht in 26.3). */
    public static Supplier<Block> register(String name, Function<BlockBehaviour.Properties, Block> factory,
                                           Supplier<BlockBehaviour.Properties> properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, InfiniteHorizon.id(name));
        return Services.REGISTRY.register(BuiltInRegistries.BLOCK, name, () -> factory.apply(properties.get().setId(key)));
    }
}
