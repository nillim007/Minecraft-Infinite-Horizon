package de.infinitehorizon.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Ingenieurs-Handbuch (24.1): Rechtsklick öffnet das Buch-GUI. Das GUI ist reine Client-Sache; der Client-Einstieg
 * jedes Loaders setzt {@link #opener}. Auf dem Server tut der Rechtsklick nichts.
 */
public class HandbookItem extends Item {

    /** Vom Client gesetzt: öffnet das Handbuch (Argument: Seiten-ID oder null = zuletzt offene Seite). */
    public static Consumer<String> opener = page -> { };

    public HandbookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            opener.accept(null);
        }
        return InteractionResult.SUCCESS;
    }
}
