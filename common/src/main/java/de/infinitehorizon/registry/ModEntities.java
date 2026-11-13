package de.infinitehorizon.registry;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.space.ProbeEntity;
import de.infinitehorizon.vehicle.SeatEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

/** Eigene Entities (26, ab M4): Sitz, Sonde und Rakete. */
public final class ModEntities {

    /** Unsichtbarer Sitz in der Kapsel (12.5); der Spieler sitzt auf Höhe des Kapselbodens. */
    public static final Supplier<EntityType<SeatEntity>> SEAT = Services.REGISTRY.register(BuiltInRegistries.ENTITY_TYPE, "seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).passengerAttachments(0.0f).clientTrackingRange(8).noSummon()
                    .build(key("seat")));

    /** Startende Sonde (12.3). */
    public static final Supplier<EntityType<ProbeEntity>> PROBE_ENTITY = Services.REGISTRY.register(BuiltInRegistries.ENTITY_TYPE,
            "probe_entity", () -> EntityType.Builder.<ProbeEntity>of(ProbeEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(16).updateInterval(1).noSummon().fireImmune()
                    .build(key("probe_entity")));

    /**
     * Rakete nach dem Start (12.5); die Größe hängt vom Stapel ab ({@code RocketEntity#getDimensions}), der Typ trägt
     * nur die Grundfläche eines Blocks.
     */
    public static final Supplier<EntityType<de.infinitehorizon.space.RocketEntity>> ROCKET = Services.REGISTRY.register(
            BuiltInRegistries.ENTITY_TYPE, "rocket", () -> EntityType.Builder.<de.infinitehorizon.space.RocketEntity>of(
                            de.infinitehorizon.space.RocketEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(16).updateInterval(1).noSummon().fireImmune()
                    .build(key("rocket")));

    private ModEntities() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, InfiniteHorizon.id(name));
    }

    public static void init() {
    }
}
