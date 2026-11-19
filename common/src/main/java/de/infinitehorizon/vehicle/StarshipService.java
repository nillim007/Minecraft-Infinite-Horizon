package de.infinitehorizon.vehicle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.network.StarshipPayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sternenschiff (21.3): Erscheinen nach dem Finale, Sprungziele (alle Orbit-Dimensionen; weitere Sternensysteme gibt es in
 * v1.0 nicht) und Sprung. Ein Sprung kostet {@code jump_cost} (1 gefüllte Antimaterie-Zelle) aus dem Inventar des
 * Besatzungsmitglieds, ohne Treibstoffprüfung; Schiff und Insassen wechseln gemeinsam die Dimension.
 */
public final class StarshipService {

    private StarshipService() {
    }

    /** Ergebnis eines Sprungs. */
    public enum Jump { OK, NOT_CREW, BAD_TARGET, NO_CELL }

    /** Sprungziele: alle Orbit-Dimensionen außer der aktuellen (Reihenfolge der Planeten-Registry). */
    public static List<Identifier> targets(Identifier current) {
        List<Identifier> out = new ArrayList<>();
        for (var p : PlanetRegistry.get().all()) {
            if (p.isOrbit() && !p.dimension().equals(current)) {
                out.add(p.dimension());
            }
        }
        return out;
    }

    public static void openTargets(ServerPlayer player, StarshipEntity ship) {
        Services.NETWORK.sendToPlayer(player, new StarshipPayload.Targets(ship.getId(),
                targets(ship.level().dimension().identifier())));
    }

    /** Erscheinen neben der Station (21.3, Schritt 4). */
    public static @Nullable StarshipEntity spawn(ServerLevel level, BlockPos at, @Nullable UUID owner) {
        StarshipEntity s = ModEntities.STARSHIP.get().create(level, EntitySpawnReason.TRIGGERED);
        if (s == null) {
            return null;
        }
        s.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        s.setOwner(owner);
        level.addFreshEntity(s);
        return s;
    }

    /** Passt der Stapel zur Angabe {@code jump_cost} (Item, ganzzahlige Komponenten)? */
    @SuppressWarnings("unchecked")
    private static boolean matches(ItemStack stack, JsonObject cost) {
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(cost.get("item").getAsString())) {
            return false;
        }
        if (cost.has("components")) {
            for (Map.Entry<String, JsonElement> c : cost.getAsJsonObject("components").entrySet()) {
                DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(c.getKey()));
                Object v = type == null ? null : stack.get(type);
                if (!(v instanceof Integer i) || i != c.getValue().getAsInt()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Springt mit dem Schiff des Spielers nach {@code target} (Orbit-Dimension). */
    public static Jump jump(ServerPlayer player, StarshipEntity ship, Identifier target) {
        if (!ship.isCrew(player)) {
            return Jump.NOT_CREW;
        }
        ServerLevel to = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, target));
        if (to == null || !targets(ship.level().dimension().identifier()).contains(target)) {
            return Jump.BAD_TARGET;
        }
        JsonObject cost = StarshipEntity.rules().has("jump_cost") ? StarshipEntity.rules().getAsJsonObject("jump_cost") : null;
        if (cost != null) {
            int need = cost.has("count") ? cost.get("count").getAsInt() : 1;
            var inv = player.getInventory();
            int have = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (matches(inv.getItem(i), cost)) {
                    have += inv.getItem(i).getCount();
                }
            }
            if (have < need && !player.getAbilities().instabuild) {
                return Jump.NO_CELL;
            }
            for (int i = 0; i < inv.getContainerSize() && need > 0 && !player.getAbilities().instabuild; i++) {
                ItemStack s = inv.getItem(i);
                if (matches(s, cost)) {
                    int take = Math.min(need, s.getCount());
                    s.shrink(take);
                    need -= take;
                }
            }
        }
        // SPEC-GAP #116: Ankunft über dem Mittelpunkt des Ziel-Orbits in der bisherigen Höhe
        double y = Math.max(to.getMinY() + 8, Math.min(to.getMaxY() - 8, ship.getY()));
        Entity moved = ship.teleport(new TeleportTransition(to, new Vec3(0.5, y, 0.5), Vec3.ZERO, ship.getYRot(), ship.getXRot(),
                TeleportTransition.DO_NOTHING));
        if (moved != null) {
            to.playSound(null, moved.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 0.6f);
        }
        player.sendSystemMessage(Component.translatable("message.infinite_horizon.starship.jumped",
                Component.translatable("planet." + target.getNamespace() + "." + target.getPath())));
        return Jump.OK;
    }
}
