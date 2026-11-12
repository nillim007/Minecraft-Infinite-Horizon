package de.infinitehorizon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Blöcke mit {@link IhBlockEntity}, deren Besitz beim Platzieren gesetzt wird (3A.2, 23.1). Eigene Blöcke (auch von
 * Addons) rufen {@link #claim} in {@code setPlacedBy} auf. Gesetzt per Befehl, Werfer, Struktur oder GameTest-
 * {@code setBlock} gibt es keinen Platzierer, der Block bleibt ohne Besitzer.
 */
public interface OwnedBlock {

    static void claim(Level level, BlockPos pos, @Nullable LivingEntity placer) {
        if (!level.isClientSide() && placer instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof IhBlockEntity be) {
            be.claimFor(player);
            // Tor has_block (11.4): zählt beim Setzen mit Besitzer
            if (be.storedOwner() != null && level instanceof net.minecraft.server.level.ServerLevel sl) {
                de.infinitehorizon.research.ResearchService.onOwnedBlockPlaced(sl, pos, be.storedOwner());
            }
        }
    }
}
