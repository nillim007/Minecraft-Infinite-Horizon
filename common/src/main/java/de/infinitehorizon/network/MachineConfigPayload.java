package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.machine.MachineBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * C→S (3A.5): Ausgangsseite und Tankwahl einer Maschine. {@code side}: 0–5 = Unten, Oben, Nord, Süd, West, Ost,
 * -1 = unverändert; {@code tank}: Index, -1 = unverändert. Der Server prüft Abstand (≤ 8 Blöcke) und Block.
 */
public record MachineConfigPayload(BlockPos pos, int side, int tank) implements CustomPacketPayload {

    /** Höchstabstand Spieler ↔ Block für Änderungen aus dem GUI (3A.5). */
    public static final double MAX_DISTANCE = 8.0;

    public static final Type<MachineConfigPayload> TYPE = new Type<>(InfiniteHorizon.id("machine_config"));
    public static final StreamCodec<ByteBuf, MachineConfigPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MachineConfigPayload::pos,
            ByteBufCodecs.VAR_INT, MachineConfigPayload::side,
            ByteBufCodecs.VAR_INT, MachineConfigPayload::tank,
            MachineConfigPayload::new);

    @Override
    public Type<MachineConfigPayload> type() {
        return TYPE;
    }

    /** Serverseitige Verarbeitung: nur gültige Werte, nur in Reichweite. */
    public static void handle(MachineConfigPayload p, ServerPlayer player) {
        if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p.pos)) > MAX_DISTANCE * MAX_DISTANCE) {
            return;
        }
        if (!(player.level().getBlockEntity(p.pos) instanceof MachineBlockEntity be)) {
            return;
        }
        if (p.side >= 0 && p.side < Direction.values().length) {
            be.setOutputSide(Direction.values()[p.side]);
        }
        if (p.tank >= 0) {
            be.setSelectedTank(p.tank);
        }
    }
}
