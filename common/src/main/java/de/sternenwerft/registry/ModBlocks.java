package de.sternenwerft.registry;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.block.CableBlock;
import de.sternenwerft.machine.MachineBlock;
import de.sternenwerft.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;
import java.util.function.Supplier;

/** Blöcke aus Meilenstein 1 (IDs laut spec/data/items.json). */
public final class ModBlocks {

    public static final Supplier<Block> CRUSHER = register("crusher",
            p -> new MachineBlock(p, () -> ModBlockEntities.CRUSHER.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> COMBUSTION_GENERATOR = register("combustion_generator",
            p -> new MachineBlock(p, () -> ModBlockEntities.COMBUSTION_GENERATOR.get()),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final Supplier<Block> COPPER_CABLE = register("copper_cable", CableBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.COPPER).noOcclusion());

    private ModBlocks() {
    }

    public static void init() {
    }

    /** Registriert einen Block; die ID wird vor dem Konstruktor gesetzt (Pflicht in 26.3). */
    public static Supplier<Block> register(String name, Function<BlockBehaviour.Properties, Block> factory,
                                           Supplier<BlockBehaviour.Properties> properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Sternenwerft.id(name));
        return Services.REGISTRY.register(BuiltInRegistries.BLOCK, name, () -> factory.apply(properties.get().setId(key)));
    }
}
