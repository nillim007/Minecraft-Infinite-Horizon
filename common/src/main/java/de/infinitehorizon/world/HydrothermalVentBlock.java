package de.infinitehorizon.world;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * {@code hydrothermal_vent} (13.4): unzerstörbare Quelle am Meeresboden der Europa. Rechtsklick mit {@code use_item}
 * (leere Glasflasche, wird verbraucht) gibt {@code result}; danach ruht diese Quelle {@code cooldown_ticks}. Werte aus
 * items.json. Unter Wasser steigen Blasen auf.
 */
public class HydrothermalVentBlock extends BaseEntityBlock {

    private final Supplier<? extends BlockEntityType<?>> type;

    public HydrothermalVentBlock(Properties properties, Supplier<? extends BlockEntityType<?>> type) {
        super(properties);
        this.type = type;
    }

    public static JsonObject config() {
        return BalanceManager.getOrBundled().entry("items", InfiniteHorizon.id("hydrothermal_vent").toString())
                .orElse(new JsonObject());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        JsonObject c = config();
        if (!c.has("use_item") || !stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(c.get("use_item").getAsString())))) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof Entity vent)) {
            return InteractionResult.PASS;
        }
        long now = level.getGameTime();
        if (now < vent.readyAt) {
            player.sendOverlayMessage(Component.translatable("message.infinite_horizon.vent.resting"));
            return InteractionResult.FAIL;
        }
        JsonObject r = c.getAsJsonObject("result");
        ItemStack result = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(r.get("id").getAsString())),
                r.get("count").getAsInt());
        boolean consume = !c.has("consumes_use_item") || c.get("consumes_use_item").getAsBoolean();
        player.setItemInHand(hand, consume ? ItemUtils.createFilledResult(stack, player, result) : stack);
        if (!consume && !player.addItem(result)) {
            net.minecraft.world.level.block.Block.popResource(level, pos.above(), result);
        }
        vent.readyAt = now + c.get("cooldown_ticks").getAsLong();
        vent.setChanged();
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 1f);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getFluidState(pos.above()).is(Fluids.WATER)) {
            level.addParticle(ParticleTypes.BUBBLE_COLUMN_UP, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    0, 0.04, 0);
        }
    }

    /** Ruhezeit der Quelle: wieder bereit ab Spielzeit {@code readyAt}. */
    public static class Entity extends BlockEntity {

        long readyAt;

        public Entity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        public long readyAt() {
            return readyAt;
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("ready_at", readyAt);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            readyAt = input.getLongOr("ready_at", 0L);
        }
    }
}
