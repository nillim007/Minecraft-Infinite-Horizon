package de.infinitehorizon.space;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.RocketPartRegistry;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Landefähre (12.7): Raketen-Entity mit eigenem Tank ({@code tank_per_component}, Wasserstoff und Sauerstoff),
 * {@code seats} Sitzen und den Zielen aus {@link LanderRoutes}. Darstellung vorerst als Triebwerk und Kapsel T2
 * (SPEC-GAP #100); Zerlegen gibt das Item {@code lander} zurück.
 */
public class LanderEntity extends RocketEntity {

    public LanderEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Aufbau der Fähre aus spaceflight.json ({@code lander}) mit leerem Tank. */
    public static RocketConfig config() {
        JsonObject r = LanderRoutes.rules();
        List<Identifier> fuels = new ArrayList<>();
        if (r.has("fuel")) {
            for (JsonElement e : r.getAsJsonArray("fuel")) {
                fuels.add(Identifier.parse(e.getAsString()));
            }
        }
        List<RocketPartRegistry.Part> look = new ArrayList<>();
        RocketPartRegistry.get().part(InfiniteHorizon.id("rocket_engine_t2")).ifPresent(look::add);
        RocketPartRegistry.get().part(InfiniteHorizon.id("rocket_capsule_t2")).ifPresent(look::add);
        return new RocketConfig(0, look, 0, 0, 0, 0, 0, List.copyOf(fuels), new long[fuels.size()],
                r.has("tank_per_component") ? r.get("tank_per_component").getAsLong() : 0,
                r.has("seats") ? r.get("seats").getAsInt() : 0);
    }

    @Override
    public List<ItemStack> partItems() {
        return List.of(new ItemStack(ModItems.LANDER.get()));
    }
}
