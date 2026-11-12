package de.infinitehorizon.platform.services;

import de.infinitehorizon.api.energy.EnergyNode;
import de.infinitehorizon.api.energy.EnergyTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
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

    /** Hat das Item eine Energie-Schnittstelle des Loaders, die Energie annimmt (Ladeslot, 6.3)? */
    boolean isPlatformChargeable(ItemStack stack);

    /**
     * Lädt das Item in Slot {@code slot} über die Loader-API mit höchstens {@code max} J; Rückgabe: angenommene Menge.
     * Geänderte Item-Daten landen wieder im Slot.
     */
    long chargeItem(Container container, int slot, long max);

    /**
     * Nur für GameTests: Energie im Test-Item (im GameTest-Betrieb trägt {@code minecraft:redstone_block} eine
     * Loader-Energie-Schnittstelle). -1 ohne Test-Item.
     */
    long testItemAmount();
}
