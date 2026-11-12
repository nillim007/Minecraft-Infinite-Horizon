package de.infinitehorizon.platform;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.platform.services.IRegistryHelper;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Fabric registriert sofort; der Supplier liefert das fertige Objekt. */
public class FabricRegistryHelper implements IRegistryHelper {

    @Override
    public <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory) {
        T value = Registry.register(registry, InfiniteHorizon.id(name), factory.get());
        return () -> value;
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.<T>create(factory::apply, blocks).build();
    }

    @Override
    public <M extends AbstractContainerMenu, D> MenuType<M> menuType(MenuFactory<M, D> factory,
                                                                    StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        return new ExtendedMenuType<>(factory::create, codec);
    }

    @Override
    public <D> void openMenu(ServerPlayer player, MenuProvider provider, D data,
                             StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        player.openMenu(new ExtendedMenuProvider<D>() {
            @Override
            public D getScreenOpeningData(ServerPlayer p) {
                return data;
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return provider.createMenu(id, inventory, p);
            }
        });
    }

    @Override
    public CreativeModeTab creativeTab(Component title, Supplier<ItemStack> icon, Supplier<List<ItemStack>> items) {
        return FabricCreativeModeTab.builder()
                .title(title)
                .icon(icon)
                .displayItems((params, output) -> items.get().forEach(output::accept))
                .build();
    }

    @Override
    public void syncRecipesToClient(net.minecraft.world.item.crafting.RecipeSerializer<?> serializer) {
        net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(serializer);
    }
}
