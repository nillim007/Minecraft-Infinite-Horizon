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

    /** Rover (16): Breite und Höhe aus vehicles.json ({@code size}); Länge wie Breite (quadratische Hitbox). */
    public static final Supplier<EntityType<de.infinitehorizon.vehicle.RoverEntity>> ROVER = Services.REGISTRY.register(
            BuiltInRegistries.ENTITY_TYPE, "rover", () -> {
                com.google.gson.JsonObject size = de.infinitehorizon.vehicle.RoverEntity.rules().has("size")
                        ? de.infinitehorizon.vehicle.RoverEntity.rules().getAsJsonObject("size") : new com.google.gson.JsonObject();
                float w = size.has("width") ? size.get("width").getAsFloat() : 1;
                float h = size.has("height") ? size.get("height").getAsFloat() : 1;
                return EntityType.Builder.<de.infinitehorizon.vehicle.RoverEntity>of(de.infinitehorizon.vehicle.RoverEntity::new,
                        MobCategory.MISC).sized(w, h).clientTrackingRange(10).updateInterval(1).noSummon().fireImmune()
                        .build(key("rover"));
            });

    // ---------------------------------------------------------------- Kreaturen Ring 1 (18, M5f)
    public static final Supplier<EntityType<de.infinitehorizon.creature.RubbleCrawler>> RUBBLE_CRAWLER =
            creature("rubble_crawler", de.infinitehorizon.creature.RubbleCrawler::new);
    public static final Supplier<EntityType<de.infinitehorizon.creature.ShadeStrider>> SHADE_STRIDER =
            creature("shade_strider", de.infinitehorizon.creature.ShadeStrider::new);
    public static final Supplier<EntityType<de.infinitehorizon.creature.DuneBurrower>> DUNE_BURROWER =
            creature("dune_burrower", de.infinitehorizon.creature.DuneBurrower::new);
    public static final Supplier<EntityType<de.infinitehorizon.creature.DustWraith>> DUST_WRAITH =
            creature("dust_wraith", de.infinitehorizon.creature.DustWraith::new);
    public static final Supplier<EntityType<de.infinitehorizon.creature.SulfurSnail>> SULFUR_SNAIL =
            creature("sulfur_snail", de.infinitehorizon.creature.SulfurSnail::new);
    public static final Supplier<EntityType<de.infinitehorizon.creature.CloudRay>> CLOUD_RAY =
            creature("cloud_ray", de.infinitehorizon.creature.CloudRay::new);

    /** Kreatur mit Größe, Kategorie und Feuerfestigkeit aus creatures.json und Attributen je Loader. */
    private static <T extends net.minecraft.world.entity.LivingEntity> Supplier<EntityType<T>> creature(String name,
            EntityType.EntityFactory<T> factory) {
        com.google.gson.JsonObject e = de.infinitehorizon.creature.CreatureRules.entry(InfiniteHorizon.id(name));
        float w = (float) de.infinitehorizon.creature.CreatureRules.num(e, "width");
        float h = (float) de.infinitehorizon.creature.CreatureRules.num(e, "height");
        MobCategory category = switch (e.has("category") ? e.get("category").getAsString() : "creature") {
            case "monster" -> MobCategory.MONSTER;
            case "ambient" -> MobCategory.AMBIENT;
            case "water_creature" -> MobCategory.WATER_CREATURE;
            default -> MobCategory.CREATURE;
        };
        boolean fireproof = e.has("behavior_params") && e.getAsJsonObject("behavior_params").has("fireproof");
        Supplier<EntityType<T>> type = Services.REGISTRY.register(BuiltInRegistries.ENTITY_TYPE, name, () -> {
            EntityType.Builder<T> b = EntityType.Builder.of(factory, category).sized(w, h).clientTrackingRange(8);
            if (fireproof) {
                b = b.fireImmune();
            }
            return b.build(key(name));
        });
        Services.REGISTRY.registerAttributes(type, () -> de.infinitehorizon.creature.CreatureRules.attributes(InfiniteHorizon.id(name)));
        return type;
    }

    private ModEntities() {
    }

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, InfiniteHorizon.id(name));
    }

    public static void init() {
    }
}
