package de.sternenwerft.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Registrierung je Loader (3.2): NeoForge verzögert über DeferredRegister, Fabric sofort. */
public interface IRegistryHelper {

    /** Registriert {@code factory} unter {@code sternenwerft:name}. Der Supplier ist erst nach der Registrierung gültig. */
    <R, T extends R> Supplier<T> register(Registry<R> registry, String name, Supplier<T> factory);

    /** Der Vanilla-Konstruktor von BlockEntityType ist nicht öffentlich; jeder Loader baut den Typ selbst. */
    <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks);

    /** Creative-Tab; {@code CreativeModeTab.Output} ist in Vanilla nicht öffentlich, daher baut jeder Loader selbst. */
    /** Client-Fabrik eines Menüs mit Zusatzdaten vom Server. */
    @FunctionalInterface
    interface MenuFactory<M, D> {
        M create(int containerId, Inventory inventory, D data);
    }

    /** MenuType mit Zusatzdaten (NeoForge: IMenuTypeExtension, Fabric: ExtendedMenuType). */
    <M extends AbstractContainerMenu, D> MenuType<M> menuType(MenuFactory<M, D> factory,
                                                             StreamCodec<? super RegistryFriendlyByteBuf, D> codec);

    /** Öffnet ein Menü und schickt {@code data} an den Client. */
    <D> void openMenu(ServerPlayer player, MenuProvider provider, D data, StreamCodec<? super RegistryFriendlyByteBuf, D> codec);

    CreativeModeTab creativeTab(Component title, Supplier<ItemStack> icon, Supplier<List<ItemStack>> items);

    /**
     * Schickt die Rezepte dieses Serializers zum Client (JEI/REI, 3.9). Fabric braucht das je Serializer
     * ({@code RecipeSynchronization}); NeoForge schickt die eingetragenen Arten beim Datapack-Sync selbst.
     */
    default void syncRecipesToClient(net.minecraft.world.item.crafting.RecipeSerializer<?> serializer) {
    }
}
