package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * S→C (21.1): Werft-GUI. Abschnitte mit Materialien (benötigt, geliefert), fertige Abschnitte, Restzeit des laufenden
 * Baus, ob der Ort gültig ist und ob ein Dock in der Station steht. C→S {@link Request} holt den Stand neu.
 */
public record ShipyardStatePayload(BlockPos terminal, int built, long remaining, List<Section> sections, boolean valid,
                                   boolean hasDock, boolean launched) implements CustomPacketPayload {

    public record Material(String id, long needed, long delivered, boolean fluid) {
        public static final StreamCodec<ByteBuf, Material> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Material::id,
                ByteBufCodecs.VAR_LONG, Material::needed,
                ByteBufCodecs.VAR_LONG, Material::delivered,
                ByteBufCodecs.BOOL, Material::fluid,
                Material::new);
    }

    public record Section(List<Material> materials) {
        public static final StreamCodec<ByteBuf, Section> STREAM_CODEC =
                Material.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Section::new, Section::materials);

        /** Anteil geliefert (0–1). */
        public double progress() {
            long need = 0;
            long have = 0;
            for (Material m : materials) {
                need += m.needed();
                have += m.delivered();
            }
            return need <= 0 ? 1 : (double) have / need;
        }
    }

    public static final Type<ShipyardStatePayload> TYPE = new Type<>(InfiniteHorizon.id("shipyard_state"));
    public static final StreamCodec<ByteBuf, ShipyardStatePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShipyardStatePayload::terminal,
            ByteBufCodecs.VAR_INT, ShipyardStatePayload::built,
            ByteBufCodecs.VAR_LONG, ShipyardStatePayload::remaining,
            Section.STREAM_CODEC.apply(ByteBufCodecs.list()), ShipyardStatePayload::sections,
            ByteBufCodecs.BOOL, ShipyardStatePayload::valid,
            ByteBufCodecs.BOOL, ShipyardStatePayload::hasDock,
            ByteBufCodecs.BOOL, ShipyardStatePayload::launched,
            ShipyardStatePayload::new);

    /** Alle Abschnitte gebaut und noch nicht gestartet: Knopf „Start“ (21.3). */
    public boolean canStart() {
        return valid && !launched && built >= sections.size() && !sections.isEmpty();
    }

    /** C→S (21.3): Finale am Werft-Terminal starten. */
    public record Start(BlockPos terminal) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(InfiniteHorizon.id("shipyard_start"));
        public static final StreamCodec<ByteBuf, Start> CODEC = BlockPos.STREAM_CODEC.map(Start::new, Start::terminal);

        @Override
        public Type<Start> type() {
            return TYPE;
        }

        public static void handle(Start s, ServerPlayer player) {
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(s.terminal())) > 8 * 8) {
                return;
            }
            de.infinitehorizon.shipyard.Finale.Start r = de.infinitehorizon.shipyard.Finale.start(player, s.terminal());
            if (r != de.infinitehorizon.shipyard.Finale.Start.OK) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.infinite_horizon.finale."
                        + r.name().toLowerCase(java.util.Locale.ROOT)));
                de.infinitehorizon.shipyard.ShipyardService.send(player, s.terminal());
            }
        }
    }

    @Override
    public Type<ShipyardStatePayload> type() {
        return TYPE;
    }

    /** C→S: Stand der Werft neu anfordern (offenes GUI, Restzeit). */
    public record Request(BlockPos terminal) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(InfiniteHorizon.id("shipyard_request"));
        public static final StreamCodec<ByteBuf, Request> CODEC = BlockPos.STREAM_CODEC.map(Request::new, Request::terminal);

        @Override
        public Type<Request> type() {
            return TYPE;
        }

        public static void handle(Request r, ServerPlayer player) {
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(r.terminal())) <= 64 * 64
                    && player.level().getBlockEntity(r.terminal()) instanceof de.infinitehorizon.shipyard.ShipyardTerminalBlockEntity) {
                de.infinitehorizon.shipyard.ShipyardService.send(player, r.terminal());
            }
        }
    }
}
