package de.infinitehorizon.space;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Landefähre als Item (12.7): Rechtsklick wie ein Boot, nur in Orbit-Dimensionen oder auf einer Andockbucht. */
public class LanderItem extends Item {

    public LanderItem(Properties properties) {
        super(properties);
    }

    /** Darf die Fähre hier stehen? Auf einer Andockbucht oder in einem Orbit. */
    public static boolean placeable(ServerLevel level, BlockPos clicked) {
        if (level.getBlockState(clicked).is(ModBlocks.DOCKING_BAY.get())) {
            return true;
        }
        Planet planet = PlanetResolver.at(level, clicked).planet();
        return planet != null && planet.isOrbit();
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = ctx.getClickedPos();
        if (!placeable(level, clicked)) {
            if (ctx.getPlayer() != null) {
                ctx.getPlayer().sendOverlayMessage(Component.translatable("message." + InfiniteHorizon.MOD_ID + ".lander.place"));
            }
            return InteractionResult.FAIL;
        }
        BlockPos at = level.getBlockState(clicked).is(ModBlocks.DOCKING_BAY.get()) ? clicked.above()
                : clicked.relative(ctx.getClickedFace());
        LanderEntity lander = spawn(level, at, ctx.getPlayer() instanceof ServerPlayer sp ? sp : null);
        if (lander == null) {
            return InteractionResult.FAIL;
        }
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Setzt eine leere Fähre der Agentur des Spielers auf {@code at}. */
    public static @Nullable LanderEntity spawn(ServerLevel level, BlockPos at, @Nullable ServerPlayer owner) {
        LanderEntity lander = ModEntities.LANDER.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (lander == null) {
            return null;
        }
        lander.configure(LanderEntity.config(), null, List.of(), owner == null ? null : AgencyManager.ensureFor(owner).id());
        lander.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        level.addFreshEntity(lander);
        return lander;
    }
}
