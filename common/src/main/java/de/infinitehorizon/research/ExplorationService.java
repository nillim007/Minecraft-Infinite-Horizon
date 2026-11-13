package de.infinitehorizon.research;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.AgencyProgress;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Erkundungsziele (11.1): Bei der Umweltprüfung (alle {@code interval_ticks}, 8.2) prüft der Server, ob der Spieler in
 * einem Teil einer Struktur aus {@code exploration_target.structures} steht. Je Agentur und Instanz
 * (Dimension + Struktur + Start-Chunk) einmal: FP nach der Abnahme je Strukturtyp, Eintrag in {@code found_structures}
 * und Meldung in der Aktionsleiste. Zuschauer zählen nicht.
 */
public final class ExplorationService {

    public static final String MESSAGE_FOUND = "message." + InfiniteHorizon.MOD_ID + ".exploration.found";

    private static volatile BalanceManager source;
    private static volatile Set<Identifier> targets = Set.of();

    private ExplorationService() {
    }

    /** Strukturen aus research.json ({@code exploration_target.structures}). */
    public static Set<Identifier> targets() {
        BalanceManager b = BalanceManager.get();
        if (b != source) {
            List<Identifier> ids = new ArrayList<>();
            b.entry("research", "exploration_target").filter(o -> o.has("structures"))
                    .ifPresent(o -> o.getAsJsonArray("structures").forEach(e -> ids.add(Identifier.parse(e.getAsString()))));
            targets = Set.copyOf(ids);
            source = b;
        }
        return targets;
    }

    /** Prüfung für einen Spieler (aus der Umweltprüfung). */
    public static void check(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        Set<Identifier> wanted = targets();
        if (wanted.isEmpty()) {
            return;
        }
        ServerLevel level = player.level();
        StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(),
                holder -> holder.unwrapKey().map(k -> wanted.contains(k.identifier())).orElse(false));
        if (start == null || !start.isValid()) {
            return;
        }
        Identifier structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
        if (structure == null) {
            return;
        }
        AgencyData data = AgencyData.get(level.getServer());
        Agency agency = data.agencyOf(player.getUUID()).orElse(null);
        if (agency == null) {
            return;
        }
        AgencyProgress.FoundStructure found = new AgencyProgress.FoundStructure(level.dimension().identifier(), structure,
                start.getChunkPos().x(), start.getChunkPos().z());
        if (!agency.progress().addFoundStructure(found)) {
            return;
        }
        int credited = FpLedger.creditStructure(agency.progress(), structure, FpLedger.rules());
        data.setDirty();
        player.sendOverlayMessage(Component.translatable(MESSAGE_FOUND,
                Component.translatableWithFallback("structure." + structure.getNamespace() + "." + structure.getPath(),
                        structure.toString()),
                FpLedger.format(credited)));
        ResearchService.syncAgency(level.getServer(), agency);
    }
}
