package de.infinitehorizon.platform.services;

import de.infinitehorizon.environment.HazardState;
import net.minecraft.world.entity.player.Player;

/**
 * Spieler-Attachments je Loader (NeoForge {@code AttachmentType}, Fabric Data Attachment API). Gespeichert mit dem
 * Spieler, beim Tod nicht kopiert.
 */
public interface IAttachmentHelper {

    /** Belastungszustand (8.2); legt bei Bedarf einen leeren an. */
    HazardState hazardState(Player player);
}
