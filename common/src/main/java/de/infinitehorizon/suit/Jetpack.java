package de.infinitehorizon.suit;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.registry.ModDataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Jetpack (9.2, 14.3): Modul der Brustplatte mit Energie (Komponente {@code energy} auf der Brustplatte). Solange Springen
 * gedrückt ist und Energie da ist, hebt es die Schwerkraft auf und beschleunigt nach oben; Schleichen schiebt nach unten,
 * Vorwärts in Blickrichtung. Der Server zieht die Energie ab, der Client bewegt den Spieler (Bewegung ist clientseitig).
 */
public final class Jetpack {

    public static final Identifier ID = InfiniteHorizon.id("jetpack");

    private Jetpack() {
    }

    public static JsonObject config() {
        return BalanceManager.getOrBundled().entry("suits", ID.toString()).orElse(new JsonObject());
    }

    private static double num(String key) {
        JsonObject c = config();
        return c.has(key) ? c.get(key).getAsDouble() : 0;
    }

    /** Brustplatte mit eingebautem Jetpack, sonst leer. */
    public static ItemStack chest(Player player) {
        ItemStack c = player.getItemBySlot(EquipmentSlot.CHEST);
        return SuitModules.modules(c).contains(ID) ? c : ItemStack.EMPTY;
    }

    public static long energy(ItemStack chest) {
        Long v = chest.get(ModDataComponents.ENERGY.get());
        return v == null ? 0 : v;
    }

    /** Kann das Jetpack in diesem Tick schieben? */
    public static boolean ready(Player player) {
        ItemStack c = chest(player);
        return !c.isEmpty() && energy(c) >= (long) num("energy_per_tick") && !player.getAbilities().flying;
    }

    public static Vec3 thrust(Vec3 v, double gravity, boolean up, boolean down, boolean forward, Vec3 look,
                              double accel, double maxUp, double horizontal) {
        return JetpackMath.thrust(v, gravity, up, down, forward, look, accel, maxUp, horizontal);
    }

    /** Client: Bewegung des eigenen Spielers in diesem Tick. */
    public static void applyClient(Player player, boolean jump, boolean shift, boolean forward) {
        if (!(jump || shift && !player.onGround()) || !ready(player)) {
            return;
        }
        player.setDeltaMovement(thrust(player.getDeltaMovement(), player.getGravity(), jump, shift, forward,
                player.getLookAngle(), num("acceleration_up"), num("max_speed_up"), num("horizontal_speed")));
        player.resetFallDistance();
    }

    /** Server: Energie je Tick, solange der Spieler Springen hält (bzw. in der Luft schleicht). */
    public static void tick(MinecraftServer server) {
        long per = (long) num("energy_per_tick");
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            var in = p.getLastClientInput();
            if (!(in.jump() || in.shift() && !p.onGround()) || !ready(p) || p.isSpectator()) {
                continue;
            }
            ItemStack c = chest(p);
            c.set(ModDataComponents.ENERGY.get(), Math.max(0, energy(c) - per));
            p.resetFallDistance();
        }
    }
}
