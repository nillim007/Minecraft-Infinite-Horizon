package de.infinitehorizon.research;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.agency.AgencyProgress;
import de.infinitehorizon.agency.Role;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.ResearchRegistry;
import de.infinitehorizon.network.AgencyStatePayload;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

/**
 * Forschung auf dem Server (11.3, 11.4): Erforschen eines Knotens, Prüfung der Stufen-Tore und Sync des
 * Agentur-Stands ({@link AgencyStatePayload}). Die Regeln selbst sind reine Funktionen ({@link #state},
 * {@link #check}, {@link ResearchGates}).
 */
public final class ResearchService {

    /** Zustand eines Knotens im Forschungsbaum (11.3). */
    public enum NodeState { LOCKED, AVAILABLE, RESEARCHED }

    /** Ergebnis eines Forschungsversuchs; {@code key} = Teil des Übersetzungsschlüssels der Ablehnung. */
    public enum Result {
        OK("ok"), UNKNOWN_NODE("unknown"), NO_AGENCY("no_agency"), ROLE("role"), ALREADY("already"),
        TIER_CLOSED("tier_closed"), REQUIRES("requires"), NOT_ENOUGH_FP("fp");

        private final String key;

        Result(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private ResearchService() {
    }

    // ---------------------------------------------------------------- reine Regeln

    /**
     * Erforscht, verfügbar (Stufe offen, alle Voraussetzungen erforscht) oder gesperrt. SPEC-GAP #66: Voraussetzungen,
     * die (noch) unsichtbar sind, gelten als nicht erforscht.
     */
    public static NodeState state(ResearchRegistry.Node node, Set<Identifier> researched, Set<Integer> openTiers) {
        if (researched.contains(node.id())) {
            return NodeState.RESEARCHED;
        }
        return openTiers.contains(node.tier()) && researched.containsAll(node.requires()) ? NodeState.AVAILABLE
                : NodeState.LOCKED;
    }

    /** Prüft einen Forschungsversuch (ohne Menü-Prüfung). {@code scale} = Hundertstel je FP. */
    public static Result check(ResearchRegistry.Node node, Role role, AgencyProgress progress, int scale) {
        if (role != Role.LEADER && role != Role.ENGINEER) {
            return Result.ROLE;
        }
        if (progress.isResearched(node.id())) {
            return Result.ALREADY;
        }
        if (!progress.isTierOpen(node.tier())) {
            return Result.TIER_CLOSED;
        }
        if (!progress.researched().containsAll(node.requires())) {
            return Result.REQUIRES;
        }
        return progress.fp() < (long) node.cost() * scale ? Result.NOT_ENOUGH_FP : Result.OK;
    }

    /** Hundertstel je FP ({@code research.json → fp_storage.scale}, 11.1). */
    public static int scale() {
        return BalanceManager.get().entry("research", "fp_storage")
                .filter(o -> o.has("scale")).map(o -> o.get("scale").getAsInt()).orElse(1);
    }

    // ---------------------------------------------------------------- Erforschen

    /** Klick im Terminal ({@code ResearchUnlockPayload}): Menü muss ein gültiges {@code research_terminal} sein. */
    public static void researchFromTerminal(ServerPlayer player, Identifier node) {
        if (!(player.containerMenu instanceof ResearchTerminalMenu menu) || !menu.stillValid(player)) {
            return;
        }
        Result r = research(player.level().getServer(), player.getUUID(), node);
        if (r != Result.OK) {
            player.sendOverlayMessage(Component.translatable("gui." + InfiniteHorizon.MOD_ID + ".research.denied." + r.key()));
        }
    }

    /** Erforscht {@code nodeId} für die Agentur des Spielers: FP abziehen, Knoten merken, Sondenziele, Tore, Sync. */
    public static Result research(MinecraftServer server, UUID player, Identifier nodeId) {
        AgencyData data = AgencyData.get(server);
        Agency agency = data.agencyOf(player).orElse(null);
        if (agency == null) {
            return Result.NO_AGENCY;
        }
        ResearchRegistry.Node node = ResearchRegistry.get().node(nodeId).orElse(null);
        if (node == null) {
            return Result.UNKNOWN_NODE;
        }
        int scale = scale();
        Result r = check(node, agency.roleOf(player).orElse(Role.GUEST), agency.progress(), scale);
        if (r != Result.OK) {
            return r;
        }
        AgencyProgress p = agency.progress();
        p.addFp(-node.cost() * scale);
        p.research(node.id());
        for (ResearchRegistry.Unlock u : node.unlocks()) {
            if (u.type().equals(ResearchRegistry.Unlock.PROBE_TARGET)) {
                p.addProbeTarget(u.id());
            }
        }
        data.setDirty();
        checkGates(server, agency, Set.of());
        syncAgency(server, agency);
        return Result.OK;
    }

    // ---------------------------------------------------------------- Tore (11.4)

    /** Prüft die Tore mit den Fakten der Agentur (+ gerade gesetzte Blöcke); true, wenn eine Stufe neu offen ist. */
    public static boolean checkGates(MinecraftServer server, Agency agency, Set<Identifier> placedBlocks) {
        AgencyProgress p = agency.progress();
        // SPEC-GAP #62: has_block zählt beim Setzen; die Blockarten der Agentur werden nicht gespeichert
        ResearchGates.Facts facts = new ResearchGates.Facts(placedBlocks, agency.visitedDimensions(), p.crafted(), p.crafted(),
                p.completedMachines());
        SortedSet<Integer> open = ResearchGates.openTiers(ResearchRegistry.get().tiers().values(), p.openTiers(), facts);
        List<Integer> opened = new ArrayList<>();
        for (int tier : open) {
            if (p.openTier(tier)) {
                opened.add(tier);
            }
        }
        if (opened.isEmpty()) {
            return false;
        }
        AgencyData.get(server).setDirty();
        for (int tier : opened) {
            Component msg = Component.translatable("message." + InfiniteHorizon.MOD_ID + ".research.tier_open",
                    Component.translatable(ResearchRegistry.tierNameKey(tier)), tier);
            forEachOnlineMember(server, agency, sp -> sp.sendSystemMessage(msg));
        }
        syncAgency(server, agency);
        return true;
    }

    /** Eine Maschine der Agentur {@code owner} hat Items oder Fluide hergestellt (Tor {@code crafted_items}). */
    public static void onCrafted(MinecraftServer server, @org.jspecify.annotations.Nullable UUID owner, java.util.Collection<Identifier> ids) {
        if (owner == null || ids.isEmpty()) {
            return;
        }
        AgencyData data = AgencyData.get(server);
        data.byId(owner).ifPresent(agency -> {
            boolean changed = false;
            for (Identifier id : ids) {
                changed |= agency.progress().addCrafted(id);
            }
            if (changed) {
                data.setDirty();
                checkGates(server, agency, Set.of());
            }
        });
    }

    /** Eine Maschine der Agentur hat einen Vorgang abgeschlossen (Tor {@code machine_completed}, 11.4). */
    public static void onMachineCompleted(MinecraftServer server, @org.jspecify.annotations.Nullable UUID owner, String machine) {
        if (owner == null) {
            return;
        }
        AgencyData data = AgencyData.get(server);
        data.byId(owner).ifPresent(agency -> {
            if (agency.progress().addCompletedMachine(machine)) {
                data.setDirty();
                checkGates(server, agency, Set.of());
            }
        });
    }

    /** Ein Block mit Besitzer wurde gesetzt (Tor {@code has_block}). */
    public static void onOwnedBlockPlaced(ServerLevel level, BlockPos pos, UUID owner) {
        AgencyData data = AgencyData.get(level.getServer());
        data.byId(data.resolve(owner)).ifPresent(agency -> checkGates(level.getServer(), agency,
                Set.of(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()))));
    }

    /** Login oder Dimensionswechsel eines Mitglieds (Tor {@code enter_dimension}). */
    public static void onMemberMoved(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        AgencyData.get(server).agencyOf(player.getUUID()).ifPresent(a -> checkGates(server, a, Set.of()));
    }

    // ---------------------------------------------------------------- Sync (3A.5)

    /** Stand der Agentur aus Sicht von {@code viewer}. */
    public static AgencyStatePayload statePayload(MinecraftServer server, Agency agency, UUID viewer) {
        List<AgencyStatePayload.Member> members = new ArrayList<>();
        agency.members().forEach(m -> members.add(new AgencyStatePayload.Member(m.player(),
                AgencyManager.playerName(server, m.player()), m.role().getSerializedName())));
        String founder = agency.founder().map(m -> AgencyManager.playerName(server, m.player())).orElse("?");
        AgencyProgress p = agency.progress();
        List<AgencyStatePayload.Invite> invites = new ArrayList<>();
        for (Agency inviting : AgencyData.get(server).invitesFor(viewer)) {
            invites.add(new AgencyStatePayload.Invite(inviting.id(), inviting.name() == null ? "" : inviting.name(),
                    inviting.founder().map(m -> AgencyManager.playerName(server, m.player())).orElse("?")));
        }
        return new AgencyStatePayload(agency.id(), agency.name() == null ? "" : agency.name(), founder, members,
                agency.roleOf(viewer).map(Role::getSerializedName).orElse(Role.GUEST.getSerializedName()), p.fp(),
                List.copyOf(p.researched()), List.copyOf(p.openTiers()), List.copyOf(p.probeTargets()),
                List.copyOf(p.probeReached()), invites, de.infinitehorizon.agency.AtlasService.json(server, agency));
    }

    /** Schickt dem Spieler den Stand seiner Agentur. */
    public static void sendState(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        AgencyData.get(server).agencyOf(player.getUUID()).ifPresent(a ->
                Services.NETWORK.sendToPlayer(player, statePayload(server, a, player.getUUID())));
    }

    /** Schickt allen Online-Mitgliedern den Stand ihrer Agentur (bei jeder Änderung). */
    public static void syncAgency(MinecraftServer server, Agency agency) {
        forEachOnlineMember(server, agency, sp -> Services.NETWORK.sendToPlayer(sp, statePayload(server, agency, sp.getUUID())));
    }

    private static void forEachOnlineMember(MinecraftServer server, Agency agency, java.util.function.Consumer<ServerPlayer> action) {
        for (Agency.Member m : agency.members()) {
            ServerPlayer sp = server.getPlayerList().getPlayer(m.player());
            if (sp != null) {
                action.accept(sp);
            }
        }
    }
}
