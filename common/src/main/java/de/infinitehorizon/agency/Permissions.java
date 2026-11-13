package de.infinitehorizon.agency;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.infinitehorizon.block.IhBlockEntity;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.dome.DomeData;
import de.infinitehorizon.dome.DomeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Rechteprüfung (23.1, 23.2, ab M4, SPEC-GAP #71). Werte aus {@code misc.json → agency_rules}: Mitglieder dürfen
 * {@code member_actions}, Nicht-Mitglieder an fremden Block-Entities nur {@code guest_actions} ({@code OPEN}); ohne
 * Besitzer ist alles erlaubt; Operatoren ab {@code operator_bypass_permission_level} sind ausgenommen. Dazu der
 * Kuppelschutz: In einer aktiven fremden Kuppel setzen und bauen Nicht-Mitglieder keinen Block ab.
 */
public final class Permissions {

    /** Was ein Spieler an einem Block oder Fahrzeug tun will. */
    public enum Action { OPEN, USE_SLOTS, BUILD, BREAK, CONFIGURE, LAUNCH }

    private static volatile @Nullable BalanceManager source;
    private static volatile Rules rules = new Rules(EnumSet.allOf(Action.class), EnumSet.of(Action.OPEN), 2);

    private record Rules(Set<Action> member, Set<Action> guest, int operatorLevel) {
    }

    private Permissions() {
    }

    private static Rules rules() {
        BalanceManager b = BalanceManager.getOrBundled();
        if (b != source) {
            JsonObject r = b.entry("misc", "agency_rules").orElse(new JsonObject());
            rules = new Rules(actions(r, "member_actions", EnumSet.allOf(Action.class)), actions(r, "guest_actions",
                    EnumSet.of(Action.OPEN)), r.has("operator_bypass_permission_level")
                    ? r.get("operator_bypass_permission_level").getAsInt() : 2);
            source = b;
        }
        return rules;
    }

    private static Set<Action> actions(JsonObject r, String key, Set<Action> fallback) {
        if (!r.has(key) || !r.get(key).isJsonArray()) {
            return fallback;
        }
        Set<Action> out = EnumSet.noneOf(Action.class);
        JsonArray a = r.getAsJsonArray(key);
        a.forEach(e -> out.add(Action.valueOf(e.getAsString())));
        return out;
    }

    /** Reine Regel: Mitglied (Rolle) bzw. Nicht-Mitglied ({@code role} null) und Aktion. */
    public static boolean allowed(@Nullable Role role, Action action) {
        Rules r = rules();
        if (role == Role.LEADER || role == Role.ENGINEER) {
            return r.member().contains(action);
        }
        return r.guest().contains(action);
    }

    /** Operator mit mindestens {@code operator_bypass_permission_level} (wie beim Vanilla-Spawnschutz). */
    public static boolean isOperator(Player player) {
        return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(rules().operatorLevel())));
    }

    /** Darf {@code player} das an etwas tun, das {@code owner} gehört (null = ohne Besitzer)? */
    public static boolean can(Player player, @Nullable UUID owner, Action action) {
        if (owner == null || !(player instanceof ServerPlayer sp) || isOperator(player)) {
            return true;
        }
        AgencyData data = AgencyData.get(sp.level().getServer());
        Agency agency = data.byId(data.resolve(owner)).orElse(null);
        if (agency == null) {
            return true;
        }
        return allowed(agency.roleOf(player.getUUID()).orElse(null), action);
    }

    /** Darf {@code player} an {@code pos} einen Block abbauen (Block-Entity-Besitz und Kuppelschutz)? */
    public static boolean mayBreak(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (isOperator(player)) {
            return true;
        }
        if (level.getBlockEntity(pos) instanceof IhBlockEntity be && !can(player, be.storedOwner(), Action.BREAK)) {
            return false;
        }
        return mayChangeInDome(player, level, pos, Action.BREAK);
    }

    /** Darf {@code player} an {@code pos} einen Block setzen (Kuppelschutz)? */
    public static boolean mayBuild(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return isOperator(player) || mayChangeInDome(player, level, pos, Action.BUILD);
    }

    /** Kuppelschutz (23.2, 10.3): In einer aktiven Kuppel nur Mitglieder der Besitzer-Agentur. */
    private static boolean mayChangeInDome(ServerPlayer player, ServerLevel level, BlockPos pos, Action action) {
        DomeData.Entry dome = DomeManager.domeAt(level, pos).orElse(null);
        if (dome == null || dome.agency().isEmpty()) {
            return true;
        }
        return can(player, dome.agency().get(), action);
    }
}
