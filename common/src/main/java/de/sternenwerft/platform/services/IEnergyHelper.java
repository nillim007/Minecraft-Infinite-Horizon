package de.sternenwerft.platform.services;

import de.sternenwerft.api.energy.EnergyNode;
import de.sternenwerft.api.energy.EnergyTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Anbindung an die Energie-API des Loaders (NeoForge: Energy-Capability, Fabric: Team Reborn Energy). */
public interface IEnergyHelper {

    /** Fremder Energie-Empfänger an {@code pos}, angesprochen über dessen Seite {@code side}; sonst null. */
    @Nullable EnergyTarget findTarget(Level level, BlockPos pos, Direction side);

    /** Bietet die Energie eigener Block-Entities über die Loader-API an. */
    void expose(Supplier<? extends BlockEntityType<? extends EnergyNode>> type);

    /**
     * Nur für GameTests: Energiemenge im Test-Verbraucher an {@code pos} (absolute Position),
     * der ausschließlich die Loader-API nutzt. -1, wenn keiner existiert.
     */
    long testConsumerAmount(Level level, BlockPos pos);
}
