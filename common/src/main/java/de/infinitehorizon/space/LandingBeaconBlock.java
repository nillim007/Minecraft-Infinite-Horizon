package de.infinitehorizon.space;

import de.infinitehorizon.block.OwnedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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
 * Landefeuer (12.6): Beim Setzen trägt der Server es in die Agentur ein (Standardname: der am Amboss vergebene
 * Item-Name, sonst „Landefeuer X/Z“); Rechtsklick öffnet ein Textfeld (Client), Umbenennen prüft der Server
 * ({@code NamePayload}, Recht {@code CONFIGURE}).
 */
public class LandingBeaconBlock extends BaseEntityBlock implements OwnedBlock {

    private final Supplier<? extends BlockEntityType<? extends LandingBeaconBlockEntity>> type;

    public LandingBeaconBlock(Properties properties, Supplier<? extends BlockEntityType<? extends LandingBeaconBlockEntity>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        OwnedBlock.claim(level, pos, placer);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LandingBeaconBlockEntity be) {
            Component custom = stack.get(DataComponents.CUSTOM_NAME);
            be.rename(custom == null ? "" : custom.getString());
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof LandingBeaconBlockEntity be) {
            de.infinitehorizon.client.BeaconNameScreen.open(pos, be.name());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }
}
