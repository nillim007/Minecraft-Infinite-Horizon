package de.infinitehorizon.platform.services;

import de.infinitehorizon.environment.HazardState;
import net.minecraft.world.entity.player.Player;

/**
 * Spieler- und Chunk-Attachments je Loader (NeoForge {@code AttachmentType}, Fabric Data Attachment API). Gespeichert mit dem
 * Spieler, beim Tod nicht kopiert.
 */
public interface IAttachmentHelper {

    /** Belastungszustand (8.2); legt bei Bedarf einen leeren an. */
    HazardState hazardState(Player player);

    /** Spieler-Attachment {@code pending_flight} (12.5); {@code PendingFlight.NONE}, wenn keiner offen ist. */
    de.infinitehorizon.space.PendingFlight pendingFlight(Player player);

    void setPendingFlight(Player player, de.infinitehorizon.space.PendingFlight flight);

    /** Chunk-Attachment {@code sampled_positions} (11.2); legt bei Bedarf ein leeres an. */
    de.infinitehorizon.research.SampledPositions sampledPositions(net.minecraft.world.level.chunk.LevelChunk chunk);
}
