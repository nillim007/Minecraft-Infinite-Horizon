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

    @Override
    public HazardState hazardState(Player player) {
        return player.getAttachedOrCreate(HAZARD_STATE);
    }
}
