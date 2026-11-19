package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.logistics.TradingPostBlockEntity;
import de.infinitehorizon.logistics.TradingPostMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * C→S (3A.5, 23.3): Muster eines Angebots am Handelsposten setzen. {@code slot} = {@code 2 × Angebot} (X) bzw.
 * {@code 2 × Angebot + 1} (Y); leerer Stapel = löschen. Der Server prüft, dass das Menü des Postens offen ist und der
 * Spieler {@code CONFIGURE} darf. Ein Muster erzeugt keine Items: getauscht wird nur, was im Lager liegt.
 */
public record TradingPostEditPayload(int slot, ItemStack pattern) implements CustomPacketPayload {

    public static final Type<TradingPostEditPayload> TYPE = new Type<>(InfiniteHorizon.id("trading_post_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TradingPostEditPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TradingPostEditPayload::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, TradingPostEditPayload::pattern,
            TradingPostEditPayload::new);

    @Override
    public Type<TradingPostEditPayload> type() {
        return TYPE;
    }

    public static void handle(TradingPostEditPayload p, ServerPlayer player) {
        if (!(player.containerMenu instanceof TradingPostMenu menu) || !menu.stillValid(player)
                || !(player.level().getBlockEntity(menu.pos()) instanceof TradingPostBlockEntity post)
                || !Permissions.can(player, post.storedOwner(), Permissions.Action.CONFIGURE)) {
            return;
        }
        post.setOffer(p.slot(), p.pattern());
        menu.broadcastChanges();
    }
}
