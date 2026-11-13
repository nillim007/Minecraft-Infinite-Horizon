package de.infinitehorizon.platform;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.environment.HazardState;
import de.infinitehorizon.platform.services.IAttachmentHelper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** Spieler-Attachments über NeoForge {@link AttachmentType}; {@link #TYPES} wird im Mod-Konstruktor angemeldet. */
public class NeoForgeAttachmentHelper implements IAttachmentHelper {

    public static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, InfiniteHorizon.MOD_ID);

    /** Ohne {@code copyOnDeath}: nach dem Tod beginnt die Belastung bei 0 (8.2). */
    public static final Supplier<AttachmentType<HazardState>> HAZARD_STATE = TYPES.register("hazard_state",
            () -> AttachmentType.builder(HazardState::new).serialize(HazardState.MAP_CODEC).build());

    /** Chunk-Attachment (11.2): beprobte und von Spielern gesetzte Positionen. */
    public static final Supplier<AttachmentType<de.infinitehorizon.research.SampledPositions>> SAMPLED_POSITIONS =
            TYPES.register("sampled_positions", () -> AttachmentType.builder(de.infinitehorizon.research.SampledPositions::new)
                    .serialize(de.infinitehorizon.research.SampledPositions.MAP_CODEC).build());

    /** Offener Flug nach dem Abmelden (12.5); gespeichert, beim Tod nicht kopiert. */
    public static final Supplier<AttachmentType<de.infinitehorizon.space.PendingFlight>> PENDING_FLIGHT =
            TYPES.register("pending_flight", () -> AttachmentType.builder(() -> de.infinitehorizon.space.PendingFlight.NONE)
                    .serialize(de.infinitehorizon.space.PendingFlight.MAP_CODEC).build());

    @Override
    public de.infinitehorizon.space.PendingFlight pendingFlight(Player player) {
        return player.getData(PENDING_FLIGHT);
    }

    @Override
    public void setPendingFlight(Player player, de.infinitehorizon.space.PendingFlight flight) {
        player.setData(PENDING_FLIGHT, flight);
    }

    @Override
    public HazardState hazardState(Player player) {
        return player.getData(HAZARD_STATE);
    }

    @Override
    public de.infinitehorizon.research.SampledPositions sampledPositions(net.minecraft.world.level.chunk.LevelChunk chunk) {
        return chunk.getData(SAMPLED_POSITIONS);
    }
}
