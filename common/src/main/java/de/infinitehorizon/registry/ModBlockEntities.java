package de.infinitehorizon.registry;

import de.infinitehorizon.machine.BatteryBlockEntity;
import de.infinitehorizon.machine.CombustionGeneratorBlockEntity;
import de.infinitehorizon.machine.FluidFuelGeneratorBlockEntity;
import de.infinitehorizon.machine.ElectricFurnaceBlockEntity;
import de.infinitehorizon.machine.FluidPumpBlockEntity;
import de.infinitehorizon.machine.FluidTankBlockEntity;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.multiblock.MultiblockControllerBlockEntity;
import de.infinitehorizon.multiblock.PortBlockEntity;
import de.infinitehorizon.platform.Services;
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

    public static final Supplier<BlockEntityType<FluidTankBlockEntity>> FLUID_TANK =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "fluid_tank",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new FluidTankBlockEntity(ModBlockEntities.FLUID_TANK.get(), pos, state),
                            ModBlocks.FLUID_TANK.get()));

    public static final Supplier<BlockEntityType<FluidPumpBlockEntity>> FLUID_PUMP =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "fluid_pump",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new FluidPumpBlockEntity(ModBlockEntities.FLUID_PUMP.get(), pos, state),
                            ModBlocks.FLUID_PUMP.get()));

    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> ASSEMBLY_TABLE =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "assembly_table",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.ASSEMBLY_TABLE.get(), pos, state,
                                    () -> List.of(ModRecipeTypes.ASSEMBLY)),
                            ModBlocks.ASSEMBLY_TABLE.get()));

    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> ELECTROLYZER =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "electrolyzer",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.ELECTROLYZER.get(), pos, state,
                                    () -> List.of(ModRecipeTypes.ELECTROLYSIS)),
                            ModBlocks.ELECTROLYZER.get()));

    public static final Supplier<BlockEntityType<MultiblockControllerBlockEntity>> REFINERY_CONTROLLER =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "refinery_controller",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new MultiblockControllerBlockEntity(ModBlockEntities.REFINERY_CONTROLLER.get(), pos, state,
                                    () -> List.of(ModRecipeTypes.REFINING)),
                            ModBlocks.REFINERY_CONTROLLER.get()));

    public static final Supplier<BlockEntityType<FluidFuelGeneratorBlockEntity>> FUEL_GENERATOR =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "fuel_generator",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new FluidFuelGeneratorBlockEntity(ModBlockEntities.FUEL_GENERATOR.get(), pos, state),
                            ModBlocks.FUEL_GENERATOR.get()));

    public static final Supplier<BlockEntityType<BatteryBlockEntity>> BATTERY =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "battery",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new BatteryBlockEntity(ModBlockEntities.BATTERY.get(), pos, state),
                            ModBlocks.BATTERY.get()));

    /** Ein Typ für alle Ports; das Verhalten folgt aus {@link de.infinitehorizon.multiblock.PortBlock.PortType}. */
    public static final Supplier<BlockEntityType<PortBlockEntity>> PORT =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "port",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new PortBlockEntity(ModBlockEntities.PORT.get(), pos, state),
                            ModBlocks.ITEM_PORT.get(), ModBlocks.FLUID_PORT.get(), ModBlocks.ENERGY_PORT.get()));

    private ModBlockEntities() {
    }

    public static void init() {
        Services.ENERGY.expose(CRUSHER);
        Services.ENERGY.expose(COMBUSTION_GENERATOR);
        Services.ENERGY.expose(ELECTRIC_FURNACE);
        Services.ENERGY.expose(PRESS);
        Services.ENERGY.expose(FLUID_PUMP);
        Services.FLUID.expose(FLUID_TANK);
        Services.FLUID.expose(FLUID_PUMP);
        for (Supplier<? extends BlockEntityType<? extends ProcessingMachineBlockEntity>> t
                : List.of(ASSEMBLY_TABLE, ELECTROLYZER, REFINERY_CONTROLLER)) {
            Services.ENERGY.expose(t);
            Services.FLUID.expose(t);
        }
        Services.ENERGY.expose(PORT);
        Services.ENERGY.expose(FUEL_GENERATOR);
        Services.FLUID.expose(FUEL_GENERATOR);
        Services.ENERGY.expose(BATTERY);
        Services.FLUID.expose(PORT);
    }
}
