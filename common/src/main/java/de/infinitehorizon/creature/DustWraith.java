package de.infinitehorizon.creature;

import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code dust_wraith} (18): passiv, schwebend, immun gegen Projektile. Kommt ein Spieler nah, führt er ihn mit
 * {@code guide_structure_chance} zur nächsten Struktur aus {@code guide_structures} (Tag {@code wraith_guide}), sonst
 * {@code random_distance} Blöcke in eine zufällige Richtung; am Ziel verschwindet er.
 */
public class DustWraith extends PlanetCreature {

    public static final TagKey<Structure> GUIDE = TagKey.create(Registries.STRUCTURE, InfiniteHorizon.id("wraith_guide"));
    /** Sucht Strukturen in diesem Umkreis (Chunks), wie Vanilla-Karten. */
    private static final int SEARCH_CHUNKS = 50;
    /** Schwebehöhe über dem Boden und Reisegeschwindigkeit (SPEC-GAP #92). */
    private static final double HOVER = 1.5;

    private @Nullable Vec3 target;

    public DustWraith(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public @Nullable Vec3 target() {
        return target;
    }

    @Override
    protected void registerGoals() {
    }

    /** Ziel wählen (18): Struktur oder zufällige Richtung. */
    public void chooseTarget(ServerLevel level) {
        if (random.nextDouble() < param("guide_structure_chance")) {
            BlockPos s = level.findNearestMapStructure(GUIDE, blockPosition(), SEARCH_CHUNKS, false);
            if (s != null) {
                target = Vec3.atCenterOf(s);
                return;
            }
        }
        double angle = random.nextDouble() * Math.PI * 2;
        double d = param("random_distance");
        target = position().add(Math.cos(angle) * d, 0, Math.sin(angle) * d);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (target == null) {
            Player p = level.getNearestPlayer(this, 6);
            if (p != null) {
                chooseTarget(level);
            }
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 flat = new Vec3(target.x - getX(), 0, target.z - getZ());
        if (flat.lengthSqr() < 9) {
            if (params().has("despawn_after_guiding")) {
                discard();
            }
            return;
        }
        double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ()) + HOVER;
        Vec3 step = flat.normalize().scale(CreatureRules.num(CreatureRules.entry(getType()), "speed"));
        setDeltaMovement(step.x, (ground - getY()) * 0.1, step.z);
        setYRot((float) (Math.atan2(-step.x, step.z) * 180 / Math.PI));
    }

    @Override
    public void travel(Vec3 input) {
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
    }
}
