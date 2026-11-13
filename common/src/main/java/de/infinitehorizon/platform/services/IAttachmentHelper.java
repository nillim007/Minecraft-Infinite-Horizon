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

    /** Chunk-Attachment {@code sampled_positions} (11.2); legt bei Bedarf ein leeres an. */
    de.infinitehorizon.research.SampledPositions sampledPositions(net.minecraft.world.level.chunk.LevelChunk chunk);
}
