package de.infinitehorizon.platform;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.environment.HazardState;
import de.infinitehorizon.platform.services.IAttachmentHelper;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;

/** Spieler-Attachments über die Fabric Data Attachment API. */
public class FabricAttachmentHelper implements IAttachmentHelper {

    /** Persistent, ohne {@code copyOnDeath}: nach dem Tod beginnt die Belastung bei 0 (8.2). */
    public static final AttachmentType<HazardState> HAZARD_STATE = AttachmentRegistry.create(
            InfiniteHorizon.id("hazard_state"), b -> b.initializer(HazardState::new).persistent(HazardState.CODEC));

    /** Chunk-Attachment (11.2): beprobte und von Spielern gesetzte Positionen, gespeichert mit dem Chunk. */
    public static final AttachmentType<de.infinitehorizon.research.SampledPositions> SAMPLED_POSITIONS = AttachmentRegistry.create(
            InfiniteHorizon.id("sampled_positions"), b -> b.initializer(de.infinitehorizon.research.SampledPositions::new)
                    .persistent(de.infinitehorizon.research.SampledPositions.CODEC));

    @Override
    public HazardState hazardState(Player player) {
        return player.getAttachedOrCreate(HAZARD_STATE);
    }

    @Override
    public de.infinitehorizon.research.SampledPositions sampledPositions(net.minecraft.world.level.chunk.LevelChunk chunk) {
        return chunk.getAttachedOrCreate(SAMPLED_POSITIONS);
    }
}
