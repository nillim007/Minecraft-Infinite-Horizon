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
