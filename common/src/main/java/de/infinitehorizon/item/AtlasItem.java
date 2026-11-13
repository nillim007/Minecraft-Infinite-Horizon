package de.infinitehorizon.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Planetenatlas (24.2): Rechtsklick öffnet das Atlas-GUI auf dem Client; die Daten kommen mit {@code AgencyStatePayload}. */
public class AtlasItem extends Item {

    /** Vom Client gesetzt (öffnet den Bildschirm). */
    public static Runnable opener = () -> { };

    public AtlasItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            opener.run();
        }
        return InteractionResult.SUCCESS;
    }
}
