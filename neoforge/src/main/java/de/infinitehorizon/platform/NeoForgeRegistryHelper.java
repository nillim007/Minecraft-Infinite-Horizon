package de.infinitehorizon.platform;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.platform.services.IRegistryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Ein DeferredRegister je Registry; {@link #registerAll(IEventBus)} im Mod-Konstruktor nach {@code InfiniteHorizon.init()}. */
public class NeoForgeRegistryHelper implements IRegistryHelper {

    private static final Map<ResourceKey<?>, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory) {
        DeferredRegister<R> register = (DeferredRegister<R>) REGISTERS.computeIfAbsent(registry.key(),
                k -> DeferredRegister.create(registry, InfiniteHorizon.MOD_ID));
        return register.register(name, factory);
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return new BlockEntityType<T>(factory::apply, blocks);
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> menuType(MenuFactory<M, D> factory,
                                                                    StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        return IMenuTypeExtension.create((id, inventory, buf) -> factory.create(id, inventory, codec.decode(buf)));
    }

    @Override
    public <D> void openMenu(ServerPlayer player, MenuProvider provider, D data,
                             StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        // Clients ohne NeoForge-Kanal (GameTest-Mock-Spieler) bekommen das Vanilla-Paket; das Menü entsteht trotzdem
        if (!player.connection.hasChannel(net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload.TYPE)) {
            player.openMenu(provider);
            return;
        }
        player.openMenu(provider, buf -> codec.encode(buf, data));
    }

    @Override
    public CreativeModeTab creativeTab(Component title, Supplier<ItemStack> icon, Supplier<List<ItemStack>> items) {
        return CreativeModeTab.builder()
                .title(title)
                .icon(icon)
                .displayItems((params, output) -> items.get().forEach(output::accept))
                .build();
    }

    public static void registerAll(IEventBus modBus) {
        REGISTERS.values().forEach(r -> r.register(modBus));
    }
}
