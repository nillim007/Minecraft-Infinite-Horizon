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
                            (pos, state) -> new de.infinitehorizon.machine.AssemblyTableBlockEntity(
                                    ModBlockEntities.ASSEMBLY_TABLE.get(), pos, state, () -> List.of(ModRecipeTypes.ASSEMBLY)),
                            ModBlocks.ASSEMBLY_TABLE.get()));

    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> REGOLITH_HEATER =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "regolith_heater",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.REGOLITH_HEATER.get(), pos, state,
                                    () -> List.of(ModRecipeTypes.REGOLITH_HEATING)),
                            ModBlocks.REGOLITH_HEATER.get()));
    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> HIGH_TEMPERATURE_FURNACE =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "high_temperature_furnace",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.HIGH_TEMPERATURE_FURNACE.get(), pos,
                                    state, () -> List.of(ModRecipeTypes.HIGH_TEMP_SMELTING)),
                            ModBlocks.HIGH_TEMPERATURE_FURNACE.get()));
    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> CRYO_LIQUEFIER =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "cryo_liquefier",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.CRYO_LIQUEFIER.get(), pos, state,
                                    () -> List.of(ModRecipeTypes.CRYO)),
                            ModBlocks.CRYO_LIQUEFIER.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.space.CloudElevatorBlockEntity>> CLOUD_ELEVATOR =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "cloud_elevator",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.space.CloudElevatorBlockEntity(
                                    ModBlockEntities.CLOUD_ELEVATOR.get(), pos, state),
                            ModBlocks.CLOUD_ELEVATOR.get()));
    /** Gastank (6.3): wie der Flüssigkeitstank, nimmt nur Gase ({@code accepts} = gas). */
    public static final Supplier<BlockEntityType<FluidTankBlockEntity>> GAS_TANK =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "gas_tank",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new FluidTankBlockEntity(ModBlockEntities.GAS_TANK.get(), pos, state),
                            ModBlocks.GAS_TANK.get()));

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

    public static final Supplier<BlockEntityType<de.infinitehorizon.machine.OxygenGeneratorBlockEntity>> OXYGEN_GENERATOR =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "oxygen_generator",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.machine.OxygenGeneratorBlockEntity(
                                    ModBlockEntities.OXYGEN_GENERATOR.get(), pos, state),
                            ModBlocks.OXYGEN_GENERATOR.get()));

    /** Druckflasche (6.3): Tank nur für Gase, Werte aus storage.json; behält beim Abbauen den Inhalt. */
    public static final Supplier<BlockEntityType<FluidTankBlockEntity>> PRESSURE_BOTTLE =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "pressure_bottle",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new FluidTankBlockEntity(ModBlockEntities.PRESSURE_BOTTLE.get(), pos, state),
                            ModBlocks.PRESSURE_BOTTLE.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.dome.DomeGeneratorBlockEntity>> DOME_GENERATOR_T1 =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "dome_generator_t1",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.dome.DomeGeneratorBlockEntity(
                                    ModBlockEntities.DOME_GENERATOR_T1.get(), pos, state),
                            ModBlocks.DOME_GENERATOR_T1.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.dome.DomeGeneratorBlockEntity>> DOME_GENERATOR_T2 =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "dome_generator_t2",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.dome.DomeGeneratorBlockEntity(
                                    ModBlockEntities.DOME_GENERATOR_T2.get(), pos, state),
                            ModBlocks.DOME_GENERATOR_T2.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.machine.ElectricLampBlockEntity>> ELECTRIC_LAMP =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "electric_lamp",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.machine.ElectricLampBlockEntity(
                                    ModBlockEntities.ELECTRIC_LAMP.get(), pos, state),
                            ModBlocks.ELECTRIC_LAMP.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.research.ResearchTerminalBlockEntity>> RESEARCH_TERMINAL =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "research_terminal",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.research.ResearchTerminalBlockEntity(
                                    ModBlockEntities.RESEARCH_TERMINAL.get(), pos, state),
                            ModBlocks.RESEARCH_TERMINAL.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.research.AnalyzerBlockEntity>> ANALYZER =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "analyzer",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.research.AnalyzerBlockEntity(
                                    ModBlockEntities.ANALYZER.get(), pos, state),
                            ModBlocks.ANALYZER.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.machine.SolarPanelBlockEntity>> SOLAR_PANEL =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "solar_panel",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.machine.SolarPanelBlockEntity(
                                    ModBlockEntities.SOLAR_PANEL.get(), pos, state),
                            ModBlocks.SOLAR_PANEL.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.space.FuelPumpBlockEntity>> FUEL_PUMP =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "fuel_pump",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.space.FuelPumpBlockEntity(ModBlockEntities.FUEL_PUMP.get(), pos, state),
                            ModBlocks.FUEL_PUMP.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.space.LaunchControlBlockEntity>> LAUNCH_CONTROL =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "launch_control",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.space.LaunchControlBlockEntity(
                                    ModBlockEntities.LAUNCH_CONTROL.get(), pos, state),
                            ModBlocks.LAUNCH_CONTROL.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.machine.FusionReactorBlockEntity>> FUSION_REACTOR =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "fusion_reactor",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.machine.FusionReactorBlockEntity(
                                    ModBlockEntities.FUSION_REACTOR.get(), pos, state),
                            ModBlocks.FUSION_REACTOR.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.station.GravityPlateBlockEntity>> GRAVITY_PLATE =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "gravity_plate",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.station.GravityPlateBlockEntity(
                                    ModBlockEntities.GRAVITY_PLATE.get(), pos, state),
                            ModBlocks.GRAVITY_PLATE.get()));
    public static final Supplier<BlockEntityType<ProcessingMachineBlockEntity>> ZERO_G_FABRICATOR =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "zero_g_fabricator",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new ProcessingMachineBlockEntity(ModBlockEntities.ZERO_G_FABRICATOR.get(), pos, state,
                                    () -> List.of(ModRecipeTypes.ZERO_G)),
                            ModBlocks.ZERO_G_FABRICATOR.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.event.SpaceWeatherStationBlockEntity>> SPACE_WEATHER_STATION =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "space_weather_station",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.event.SpaceWeatherStationBlockEntity(
                                    ModBlockEntities.SPACE_WEATHER_STATION.get(), pos, state),
                            ModBlocks.SPACE_WEATHER_STATION.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.station.DockingBayBlockEntity>> DOCKING_BAY =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "docking_bay",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.station.DockingBayBlockEntity(ModBlockEntities.DOCKING_BAY.get(), pos, state),
                            ModBlocks.DOCKING_BAY.get()));
    public static final Supplier<BlockEntityType<FluidTankBlockEntity>> TANK_DEPOT =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "tank_depot",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new FluidTankBlockEntity(ModBlockEntities.TANK_DEPOT.get(), pos, state),
                            ModBlocks.TANK_DEPOT.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.machine.SolarPanelBlockEntity>> SOLAR_WING =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "solar_wing",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.machine.SolarPanelBlockEntity(ModBlockEntities.SOLAR_WING.get(), pos, state),
                            ModBlocks.SOLAR_WING.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.station.HabitationModuleBlockEntity>> HABITATION_MODULE =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "habitation_module",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.station.HabitationModuleBlockEntity(ModBlockEntities.HABITATION_MODULE.get(), pos, state),
                            ModBlocks.HABITATION_MODULE.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.food.GreenhouseBlockEntity>> GREENHOUSE =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "greenhouse",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.food.GreenhouseBlockEntity(ModBlockEntities.GREENHOUSE.get(), pos, state),
                            ModBlocks.GREENHOUSE.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.station.StationCoreBlockEntity>> STATION_CORE =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "station_core",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.station.StationCoreBlockEntity(
                                    ModBlockEntities.STATION_CORE.get(), pos, state),
                            ModBlocks.STATION_CORE.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.food.FoodPackerBlockEntity>> FOOD_PACKER =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "food_packer",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.food.FoodPackerBlockEntity(
                                    ModBlockEntities.FOOD_PACKER.get(), pos, state),
                            ModBlocks.FOOD_PACKER.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.food.HydroponicsBlockEntity>> HYDROPONICS_BASIN =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "hydroponics_basin",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.food.HydroponicsBlockEntity(
                                    ModBlockEntities.HYDROPONICS_BASIN.get(), pos, state),
                            ModBlocks.HYDROPONICS_BASIN.get()));
    public static final Supplier<BlockEntityType<de.infinitehorizon.space.LandingBeaconBlockEntity>> LANDING_BEACON =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "landing_beacon",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.space.LandingBeaconBlockEntity(
                                    ModBlockEntities.LANDING_BEACON.get(), pos, state),
                            ModBlocks.LANDING_BEACON.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.space.RocketTankBlockEntity>> ROCKET_TANK_T1 =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "rocket_tank_t1",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.space.RocketTankBlockEntity(
                                    ModBlockEntities.ROCKET_TANK_T1.get(), pos, state),
                            ModBlocks.ROCKET_TANK_T1.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.space.RocketTankBlockEntity>> ROCKET_TANK_T2 =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "rocket_tank_t2",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.space.RocketTankBlockEntity(
                                    ModBlockEntities.ROCKET_TANK_T2.get(), pos, state),
                            ModBlocks.ROCKET_TANK_T2.get()));

    public static final Supplier<BlockEntityType<de.infinitehorizon.space.CargoBayBlockEntity>> ROCKET_CARGO_BAY =
            Services.REGISTRY.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, "rocket_cargo_bay",
                    () -> Services.REGISTRY.blockEntityType(
                            (pos, state) -> new de.infinitehorizon.space.CargoBayBlockEntity(
                                    ModBlockEntities.ROCKET_CARGO_BAY.get(), pos, state),
                            ModBlocks.ROCKET_CARGO_BAY.get()));

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
        Services.FLUID.expose(GAS_TANK);
        Services.FLUID.expose(FLUID_PUMP);
        for (Supplier<? extends BlockEntityType<? extends ProcessingMachineBlockEntity>> t
                : List.of(ASSEMBLY_TABLE, ELECTROLYZER, REFINERY_CONTROLLER, REGOLITH_HEATER, HIGH_TEMPERATURE_FURNACE,
                CRYO_LIQUEFIER, FOOD_PACKER, HYDROPONICS_BASIN, GREENHOUSE, ZERO_G_FABRICATOR, SPACE_WEATHER_STATION, FUSION_REACTOR, GRAVITY_PLATE)) {
            Services.ENERGY.expose(t);
            Services.FLUID.expose(t);
        }
        Services.ENERGY.expose(PORT);
        Services.ENERGY.expose(FUEL_GENERATOR);
        Services.FLUID.expose(FUEL_GENERATOR);
        Services.ENERGY.expose(BATTERY);
        Services.ENERGY.expose(OXYGEN_GENERATOR);
        Services.FLUID.expose(OXYGEN_GENERATOR);
        Services.FLUID.expose(PRESSURE_BOTTLE);
        Services.ENERGY.expose(DOME_GENERATOR_T1);
        Services.FLUID.expose(DOME_GENERATOR_T1);
        Services.ENERGY.expose(DOME_GENERATOR_T2);
        Services.FLUID.expose(DOME_GENERATOR_T2);
        Services.ENERGY.expose(STATION_CORE);
        Services.FLUID.expose(TANK_DEPOT);
        Services.ENERGY.expose(SOLAR_WING);
        Services.FLUID.expose(STATION_CORE);
        Services.ENERGY.expose(ELECTRIC_LAMP);
        Services.ENERGY.expose(ANALYZER);
        Services.ENERGY.expose(SOLAR_PANEL);
        Services.FLUID.expose(FUEL_PUMP);
        Services.FLUID.expose(PORT);
    }
}
