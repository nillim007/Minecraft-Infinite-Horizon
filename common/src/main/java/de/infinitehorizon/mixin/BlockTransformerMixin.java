package de.infinitehorizon.mixin;

import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 17: Die Hacke macht aus {@code compost_soil} {@code compost_farmland} (wie Vanilla Erde → Ackerland). */
@Mixin(BlockTransformer.class)
public abstract class BlockTransformerMixin {

    @Inject(method = "transformBlock", at = @At("HEAD"), cancellable = true)
    private void infinite_horizon$tillCompost(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!ctx.getItemInHand().is(ItemTags.HOES) || ctx.getClickedFace() == Direction.DOWN
                || !level.getBlockState(pos).is(ModBlocks.simple("compost_soil").get())
                || !level.getBlockState(pos.above()).isAir()) {
            return;
        }
        if (!level.isClientSide()) {
            var farmland = ModBlocks.COMPOST_FARMLAND.get().defaultBlockState();
            level.setBlock(pos, farmland, net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(ctx.getPlayer(), farmland));
            level.playSound(null, pos, SoundEvents.HOE_TILL.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            if (ctx.getPlayer() != null) {
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), ctx.getHand());
            }
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
