package de.infinitehorizon.agency;

import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Erfolge, die eine Maschine oder ein Ereignis auslöst (24.3, {@code granted_by: code_all_members}): Alle Mitglieder
 * der Besitzer-Agentur bekommen sie; wer offline ist, beim nächsten Login (gespeichert in
 * {@code pending_advancements}, 23.1).
 */
public final class AgencyAdvancements {

    /** {@code own_air}: erster aktiver Kuppelgenerator (24.3). */
    public static final Identifier OWN_AIR = InfiniteHorizon.id("own_air");
    /** {@code three_minutes_of_hell}: Venus unter der Wolkenschicht betreten und wieder hinauf (24.3, ab M9). */
    public static final Identifier THREE_MINUTES_OF_HELL = InfiniteHorizon.id("three_minutes_of_hell");
    /** {@code sun_in_a_bottle}: Fusionsreaktor erzeugt Strom (24.3, ab M9). */
    public static final Identifier SUN_IN_A_BOTTLE = InfiniteHorizon.id("sun_in_a_bottle");
    /** {@code network}: genug Routen der Agentur gleichzeitig aktiv (24.3, ab M9). */
    public static final Identifier NETWORK = InfiniteHorizon.id("network");

    /** Eintrag eines Erfolgs aus {@code misc.json → advancements.by_id} (leer, wenn er fehlt). */
    public static com.google.gson.JsonObject rule(Identifier id) {
        var adv = de.infinitehorizon.data.BalanceManager.getOrBundled().entry("misc", "advancements").orElse(null);
        if (adv == null || !adv.has("by_id") || !adv.getAsJsonObject("by_id").has(id.toString())) {
            return new com.google.gson.JsonObject();
        }
        return adv.getAsJsonObject("by_id").getAsJsonObject(id.toString());
    }

    /** {@code network}: Zahl der gleichzeitig aktiven Routen ({@code active_routes}); 0 = Erfolg fehlt. */
    public static int networkRoutes() {
        var r = rule(NETWORK);
        return r.has("active_routes") ? r.get("active_routes").getAsInt() : 0;
    }

    private AgencyAdvancements() {
    }

    /** Vergibt den Erfolg an alle Mitglieder der (aufgelösten) Agentur {@code owner}. */
    public static void awardToAgency(MinecraftServer server, UUID owner, Identifier advancement) {
        AgencyData data = AgencyData.get(server);
        Agency agency = data.byId(data.resolve(owner)).orElse(null);
        if (agency == null || server.getAdvancements().get(advancement) == null) {
            return;
        }
        boolean dirty = false;
        for (Agency.Member m : agency.members()) {
            ServerPlayer sp = server.getPlayerList().getPlayer(m.player());
            if (sp != null) {
                award(sp, advancement);
            } else {
                agency.progress().addPendingAdvancement(m.player(), advancement);
                dirty = true;
            }
        }
        if (dirty) {
            data.setDirty();
        }
    }

    /** Vergibt alle offenen Kriterien des Erfolgs. */
    public static void award(ServerPlayer player, Identifier advancement) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(advancement);
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    /** Login: ausstehende Erfolge des Spielers vergeben. */
    public static void onLogin(ServerPlayer player) {
        AgencyData data = AgencyData.get(player.level().getServer());
        for (Agency agency : data.all()) {
            var pending = agency.progress().takePendingAdvancements(player.getUUID());
            if (!pending.isEmpty()) {
                pending.forEach(id -> award(player, id));
                data.setDirty();
            }
        }
    }
}
