package de.infinitehorizon.platform.services;

import de.infinitehorizon.api.fluid.FluidNode;
import de.infinitehorizon.api.fluid.FluidTarget;
import de.infinitehorizon.core.fluid.FluidDef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Anbindung an die Fluid-API des Loaders (NeoForge: Fluid-Capability, Fabric: FluidStorage). Mengen in mB. */
public interface IFluidHelper {

    /** Registriert ein Mod-Fluid (Quelle, fließend, ggf. Weltblock und Eimer; NeoForge zusätzlich FluidType). */
    FluidDef.Handle registerFluid(FluidDef def);

    /** Fremder Fluid-Empfänger an {@code pos}, angesprochen über dessen Seite {@code side}; sonst null. */
    @Nullable FluidTarget findTarget(Level level, BlockPos pos, Direction side);

    /** Bietet die Tanks eigener Block-Entities über die Loader-API an. */
    void expose(Supplier<? extends BlockEntityType<? extends FluidNode>> type);
}
