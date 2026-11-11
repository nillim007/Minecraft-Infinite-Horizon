package de.sternenwerft.registry;

import de.sternenwerft.machine.CombustionGeneratorBlockEntity;
import de.sternenwerft.machine.ElectricFurnaceBlockEntity;
import de.sternenwerft.machine.ProcessingMachineBlockEntity;
import de.sternenwerft.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.function.Supplier;

/** Block-Entity-Typen; ihre Energie wird zusätzlich über die Loader-API angeboten. */
public final class ModBlockEntities {

    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> CRUSHER =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "crusher",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.CRUSHER.get(), pos, state,
                                    () -> List.of(ModRecipeTypes.CRUSHING)),
                            ModBlocks.CRUSHER.get()));

    public static final Supplier<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "electric_furnace",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new ElectricFurnaceBlockEntity(ModBlockEntities.ELECTRIC_FURNACE.get(), pos, state,
                                    () -> List.of(ModRecipeTypes.ELECTRIC_SMELTING)),
                            ModBlocks.ELECTRIC_FURNACE.get()));

    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> PRESS =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "press",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.PRESS.get(), pos, state,
                                    () -> List.of(ModRecipeTypes.PRESSING)),
                            ModBlocks.PRESS.get()));

    public static final Supplier<BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "combustion_generator",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new CombustionGeneratorBlockEntity(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, state),
                            ModBlocks.COMBUSTION_GENERATOR.get()));

    private ModBlockEntities() {
    }

    public static void init() {
        Services.ENERGY.expose(CRUSHER);
        Services.ENERGY.expose(COMBUSTION_GENERATOR);
        Services.ENERGY.expose(ELECTRIC_FURNACE);
        Services.ENERGY.expose(PRESS);
    }
}
