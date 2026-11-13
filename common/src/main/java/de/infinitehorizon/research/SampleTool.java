package de.infinitehorizon.research;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/**
 * Probenzange (11.2): Rechtsklick auf einen beprobbaren Block gibt 1 {@code sample} mit Block und Dimension; der Block
 * bleibt. Jede Position nur einmal (Chunk-Attachment {@code sampled_positions}); von Spielern gesetzte Blöcke sind nie
 * beprobbar. Abklingzeit aus research.json ({@code sample_tool.cooldown_ticks}) als Vanilla-Item-Abklingzeit.
 */
public final class SampleTool {

    /** Beprobbar in der Overworld (Ring 0, 11.2). */
    public static final TagKey<Block> SAMPLEABLE_RING0 = TagKey.create(Registries.BLOCK, InfiniteHorizon.id("sampleable/ring0"));
    /** Beprobbar in Planetendimensionen (11.2). */
    public static final TagKey<Block> SAMPLEABLE_PLANET = TagKey.create(Registries.BLOCK, InfiniteHorizon.id("sampleable/planet"));

    public static final String MESSAGE_NONE = "message." + InfiniteHorizon.MOD_ID + ".sample.none";
    public static final String MESSAGE_USED = "message." + InfiniteHorizon.MOD_ID + ".sample.used";

    /** Ergebnis eines Probenversuchs (für Tests und Meldungen). */
    public enum Result { SAMPLED, NOT_SAMPLEABLE, ALREADY_SAMPLED }

    private SampleTool() {
    }

    /** Tag der Dimension: Overworld → ring0, Planetendimension → planet, sonst keiner. */
    public static @Nullable TagKey<Block> tagFor(Identifier dimension) {
        if (dimension.equals(PlanetResolver.EARTH)) {
            return SAMPLEABLE_RING0;
        }
        return PlanetRegistry.get().planet(dimension).isPresent() ? SAMPLEABLE_PLANET : null;
    }

    /** Rechtsklick mit der Probenzange ({@code Item#useOn}). */
    public static InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel sl) || player == null) {
            return InteractionResult.PASS;
        }
        Result r = sample(sl, context.getClickedPos(), player);
        switch (r) {
            case NOT_SAMPLEABLE -> player.sendOverlayMessage(Component.translatable(MESSAGE_NONE));
            case ALREADY_SAMPLED -> player.sendOverlayMessage(Component.translatable(MESSAGE_USED));
            case SAMPLED -> player.getCooldowns().addCooldown(context.getItemInHand(), cooldownTicks());
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Nimmt eine Probe an {@code pos} und gibt sie dem Spieler (bei vollem Inventar fällt sie vor ihn). */
    public static Result sample(ServerLevel level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        Identifier dimension = level.dimension().identifier();
        TagKey<Block> tag = tagFor(dimension);
        LevelChunk chunk = level.getChunkAt(pos);
        SampledPositions marks = Services.ATTACHMENTS.sampledPositions(chunk);
        long packed = pos.asLong();
        if (tag == null || !state.is(tag) || marks.isPlaced(packed)) {
            return Result.NOT_SAMPLEABLE;
        }
        if (marks.isSampled(packed)) {
            return Result.ALREADY_SAMPLED;
        }
        marks.markSampled(packed);
        chunk.markUnsaved();
        ItemStack sample = create(BuiltInRegistries.BLOCK.getKey(state.getBlock()), dimension);
        if (!player.getInventory().add(sample)) {
            player.spawnAtLocation(level, sample);
        }
        return Result.SAMPLED;
    }

    /** Neue Probe mit Komponente {@code sample}. */
    public static ItemStack create(Identifier block, Identifier dimension) {
        ItemStack stack = new ItemStack(ModItems.SAMPLE.get());
        stack.set(ModDataComponents.SAMPLE.get(), new SampleData(block, dimension));
        return stack;
    }

    /**
     * Ein Spieler hat über ein {@code BlockItem} einen Block gesetzt (11.2): Blöcke aus den beiden
     * {@code sampleable}-Tags werden als gesetzt markiert. Kolben, Endermen und Befehle laufen nicht hierüber.
     */
    public static void onPlayerPlaced(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel sl) || !(state.is(SAMPLEABLE_RING0) || state.is(SAMPLEABLE_PLANET))) {
            return;
        }
        LevelChunk chunk = sl.getChunkAt(pos);
        if (Services.ATTACHMENTS.sampledPositions(chunk).markPlaced(pos.asLong())) {
            chunk.markUnsaved();
        }
    }

    /** Abklingzeit in Ticks (research.json {@code sample_tool.cooldown_ticks}). */
    public static int cooldownTicks() {
        return BalanceManager.get().entry("research", "sample_tool")
                .filter(o -> o.has("cooldown_ticks")).map(o -> o.get("cooldown_ticks").getAsInt()).orElse(0);
    }

    /** Item der Probenzange. */
    public static final class ToolItem extends Item {
        public ToolItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            return SampleTool.useOn(context);
        }
    }

    /** Item der Probe: Tooltip mit Block und Dimension (11.2). */
    public static final class SampleItem extends Item {
        public SampleItem(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                    java.util.function.Consumer<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
            SampleData data = stack.get(ModDataComponents.SAMPLE.get());
            if (data == null) {
                return;
            }
            Block block = BuiltInRegistries.BLOCK.getOptional(data.block()).orElse(null);
            Component blockName = block == null ? Component.literal(data.block().toString()) : block.getName();
            lines.accept(Component.translatable("gui." + InfiniteHorizon.MOD_ID + ".sample.block", blockName)
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.accept(Component.translatable("gui." + InfiniteHorizon.MOD_ID + ".sample.dimension",
                    Component.translatableWithFallback("planet." + data.dimension().getNamespace() + "." + data.dimension().getPath(),
                            data.dimension().toString())).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
