package de.infinitehorizon.client.handbook;

import com.mojang.blaze3d.platform.InputConstants;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.item.HandbookItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import de.infinitehorizon.compat.RecipeViewerHooks;
import net.minecraft.client.gui.components.EditBox;
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
    public static final KeyMapping KEY = new KeyMapping("key." + InfiniteHorizon.MOD_ID + ".handbook", InputConstants.KEY_H,
            KeyMapping.Category.INVENTORY);

    private static HandbookData.@Nullable Handbook cache;
    /** Zuletzt offene Seite (merkt sich der Client, 24.1). */
    static @Nullable String lastPage;
    private static @Nullable Field hoveredSlot;

    private HandbookClient() {
    }

    public static void init() {
        HandbookItem.opener = HandbookClient::open;
        de.infinitehorizon.item.AtlasItem.opener = de.infinitehorizon.client.AtlasScreen::open;
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
            InfiniteHorizon.LOG.warn("Handbuch nicht ladbar: {}", e.getMessage());
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

    /**
     * Tastendruck in einem Bildschirm: H über einem Item im Inventar oder in JEI/REI öffnet dessen Seite (24.1).
     * Nicht, solange ein Textfeld (Kreativ-Suche, Suchfeld der Rezeptanzeige) die Eingabe hat. True = verbraucht.
     */
    public static boolean onScreenKey(Screen screen, KeyEvent event) {
        if (!KEY.matches(event) || isTyping(screen) || RecipeViewerHooks.isTyping()) {
            return false;
        }
        ItemStack viewer = RecipeViewerHooks.hovered();
        if (!viewer.isEmpty() && openForItem(viewer)) {
            return true;
        }
        if (!(screen instanceof AbstractContainerScreen<?> acs)) {
            return false;
        }
        Slot slot = hovered(acs);
        return slot != null && slot.hasItem() && openForItem(slot.getItem());
    }

    /** Hat ein Textfeld des Bildschirms die Eingabe? */
    static boolean isTyping(Screen screen) {
        for (var child : screen.children()) {
            if (child instanceof EditBox box && box.isFocused() && box.canConsumeInput()) {
                return true;
            }
        }
        return false;
    }

    /** Hinweis „H: Handbuch“ an Items mit eigener Seite. */
    public static void appendTooltip(ItemStack stack, List<Component> lines) {
        oxygenTooltip(stack, lines);
        for (net.minecraft.resources.Identifier m : de.infinitehorizon.suit.SuitModules.modules(stack)) {
            lines.add(Component.translatable("tooltip." + InfiniteHorizon.MOD_ID + ".suit_module",
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(m).getDefaultInstance().getHoverName()).withStyle(ChatFormatting.AQUA));
        }
        if (pageFor(stack) != null) {
            lines.add(Component.translatable("gui." + InfiniteHorizon.MOD_ID + ".handbook.hint", KEY.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** O₂ in Anzug-Brustplatten und Sauerstoffflaschen (9.1, {@code gui.infinite_horizon.oxygen}). */
    private static void oxygenTooltip(ItemStack stack, List<Component> lines) {
        String key = "gui." + InfiniteHorizon.MOD_ID + ".oxygen";
        if (de.infinitehorizon.environment.Suits.tierOf(stack, 1) != null) {
            lines.add(Component.translatable(key, de.infinitehorizon.environment.Suits.oxygen(stack),
                    de.infinitehorizon.environment.Suits.supply().capacity()).withStyle(ChatFormatting.AQUA));
            return;
        }
        var spec = de.infinitehorizon.core.fluid.ComponentContainers.spec(stack);
        if (spec != null) {
            lines.add(Component.translatable(key, de.infinitehorizon.core.fluid.ComponentContainers.content(stack).amount(),
                    spec.capacity()).withStyle(ChatFormatting.AQUA));
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
