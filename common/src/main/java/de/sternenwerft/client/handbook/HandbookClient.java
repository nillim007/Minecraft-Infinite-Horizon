package de.sternenwerft.client.handbook;

import com.mojang.blaze3d.platform.InputConstants;
import de.sternenwerft.Sternenwerft;
import de.sternenwerft.item.HandbookItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Client-Seite des Ingenieurs-Handbuchs (24.1): Öffnen, Lesezeichen, Taste {@code H} über einem Item, Tooltip-Hinweis.
 * Die Loader rufen {@link #init()}, registrieren {@link #KEY} und leiten Tastendruck in Bildschirmen und Tooltips hierher.
 */
public final class HandbookClient {

    /** Taste „Handbuch“ (Standard H, änderbar). */
    public static final KeyMapping KEY = new KeyMapping("key." + Sternenwerft.MOD_ID + ".handbook", InputConstants.KEY_H,
            KeyMapping.Category.INVENTORY);

    private static HandbookData.@Nullable Handbook cache;
    /** Zuletzt offene Seite (merkt sich der Client, 24.1). */
    static @Nullable String lastPage;
    private static @Nullable Field hoveredSlot;

    private HandbookClient() {
    }

    public static void init() {
        HandbookItem.opener = HandbookClient::open;
    }

    public static HandbookData.Handbook handbook() {
        if (cache == null) {
            reload();
        }
        return cache;
    }

    /** Liest die Seiten neu (bei jedem Öffnen; Ressourcenpakete können sich geändert haben). */
    public static void reload() {
        try {
            cache = HandbookData.load(Minecraft.getInstance().getResourceManager());
        } catch (RuntimeException e) {
            Sternenwerft.LOG.warn("Handbuch nicht ladbar: {}", e.getMessage());
            cache = HandbookData.Handbook.EMPTY;
        }
    }

    /** Öffnet das Handbuch auf {@code page} (null = zuletzt offene Seite). */
    public static void open(@Nullable String page) {
        reload();
        Minecraft.getInstance().setScreenAndShow(new HandbookScreen(page != null ? page : lastPage));
    }

    /** Seite, die das Item unter {@code items} nennt; null ohne Seite. */
    public static @Nullable String pageFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return handbook().itemPage().get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static boolean openForItem(ItemStack stack) {
        String page = pageFor(stack);
        if (page == null) {
            return false;
        }
        open(page);
        return true;
    }

    /** Tastendruck in einem Bildschirm: H über einem Item im Inventar öffnet dessen Seite. True = verbraucht. */
    public static boolean onScreenKey(Screen screen, KeyEvent event) {
        if (!KEY.matches(event) || !(screen instanceof AbstractContainerScreen<?> acs)) {
            return false;
        }
        Slot slot = hovered(acs);
        return slot != null && slot.hasItem() && openForItem(slot.getItem());
    }

    /** Hinweis „H: Handbuch“ an Items mit eigener Seite. */
    public static void appendTooltip(ItemStack stack, List<Component> lines) {
        if (pageFor(stack) != null) {
            lines.add(Component.translatable("gui." + Sternenwerft.MOD_ID + ".handbook.hint", KEY.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** Slot unter der Maus. Das Feld ist geschützt; 26.x ist unverschleiert, daher per Reflection mit festem Namen. */
    private static @Nullable Slot hovered(AbstractContainerScreen<?> screen) {
        try {
            if (hoveredSlot == null) {
                hoveredSlot = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
                hoveredSlot.setAccessible(true);
            }
            return (Slot) hoveredSlot.get(screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
