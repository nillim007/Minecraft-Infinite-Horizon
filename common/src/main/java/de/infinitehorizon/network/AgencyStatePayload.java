package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.UUID;

/**
 * S→C: Stand der eigenen Agentur (3A.5, 23.1) an alle Online-Mitglieder bei Join und bei jeder Änderung. M4a: Name,
 * Mitglieder mit Rolle, eigene Rolle, FP (Hundertstel), {@code researched}, {@code open_tiers}, {@code probe_targets},
 * {@code probe_reached}; ab M4c die offenen Einladungen an den Spieler. Der Atlas-Teil kommt mit M4f.
 *
 * @param name    eigener Name oder leer (dann „Agentur von &lt;founder&gt;“)
 * @param founder Name des Gründers
 */
public record AgencyStatePayload(UUID agency, String name, String founder, List<Member> members, String ownRole, int fp,
                                 List<Identifier> researched, List<Integer> openTiers, List<Identifier> probeTargets,
                                 List<Identifier> probeReached, List<Invite> invites, String atlas) implements CustomPacketPayload {

    /**
     * Eine andere Agentur mit Bezug zum Spieler: Agentur, eigener Name (oder leer), Gründer und Art: {@link #INVITE}
     * (Einladung an den Spieler), ab M9 {@link #PARTNER}, {@link #PARTNER_IN} (bietet an), {@link #PARTNER_OUT}
     * (eigenes Angebot) und {@link #CANDIDATE} (Agentur mit Mitgliedern online, 23.3).
     */
    public record Invite(UUID agency, String name, String founder, String kind) {
        public static final String INVITE = "invite";
        public static final String PARTNER = "partner";
        public static final String PARTNER_IN = "partner_in";
        public static final String PARTNER_OUT = "partner_out";
        public static final String CANDIDATE = "candidate";
        public static final StreamCodec<ByteBuf, Invite> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Invite::agency,
                ByteBufCodecs.STRING_UTF8, Invite::name,
                ByteBufCodecs.STRING_UTF8, Invite::founder,
                ByteBufCodecs.stringUtf8(16), Invite::kind,
                Invite::new);
    }

    /** Einträge einer Art aus {@link #invites()}. */
    public List<Invite> related(String kind) {
        return invites.stream().filter(i -> i.kind().equals(kind)).toList();
    }

    /** Ein Mitglied mit Anzeigename und Rolle ({@code leader}, {@code engineer}, {@code guest}). */
    public record Member(UUID player, String name, String role) {
        public static final StreamCodec<ByteBuf, Member> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Member::player,
                ByteBufCodecs.STRING_UTF8, Member::name,
                ByteBufCodecs.STRING_UTF8, Member::role,
                Member::new);
    }

    public static final Type<AgencyStatePayload> TYPE = new Type<>(InfiniteHorizon.id("agency_state"));
    public static final StreamCodec<ByteBuf, AgencyStatePayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, AgencyStatePayload::agency,
            ByteBufCodecs.STRING_UTF8, AgencyStatePayload::name,
            ByteBufCodecs.STRING_UTF8, AgencyStatePayload::founder,
            Member.CODEC.apply(ByteBufCodecs.list()), AgencyStatePayload::members,
            ByteBufCodecs.STRING_UTF8, AgencyStatePayload::ownRole,
            ByteBufCodecs.VAR_INT, AgencyStatePayload::fp,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), AgencyStatePayload::researched,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), AgencyStatePayload::openTiers,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), AgencyStatePayload::probeTargets,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), AgencyStatePayload::probeReached,
            Invite.CODEC.apply(ByteBufCodecs.list()), AgencyStatePayload::invites,
            ByteBufCodecs.STRING_UTF8, AgencyStatePayload::atlas,
            AgencyStatePayload::new);

    @Override
    public Type<AgencyStatePayload> type() {
        return TYPE;
    }
}
