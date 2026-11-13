package de.infinitehorizon.space;

import de.infinitehorizon.block.OwnedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Raketenteil mit Block-Entity (Tank, Frachtraum, 12.4). Hat die Block-Entity ein Menü (Frachtraum), öffnet ein
 * Rechtsklick es; Gast-Rechte wie bei Maschinen (Slot-Klicks prüft das Vanilla-Menü nicht, daher nur Mitglieder).
 */
public class RocketPartBlock extends BaseEntityBlock implements OwnedBlock {

    private final Supplier<? extends BlockEntityType<?>> type;

    public RocketPartBlock(Properties properties, Supplier<? extends BlockEntityType<?>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        OwnedBlock.claim(level, pos, placer);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                OwnedBlock.claimOnOpen(level, pos, sp);
                if (level.getBlockEntity(pos) instanceof de.infinitehorizon.block.IhBlockEntity be
                        && !de.infinitehorizon.agency.Permissions.can(sp, be.storedOwner(),
                        de.infinitehorizon.agency.Permissions.Action.USE_SLOTS)) {
                    sp.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.infinite_horizon.agency.protected"));
                    return InteractionResult.FAIL;
                }
                sp.openMenu(provider);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }
}
