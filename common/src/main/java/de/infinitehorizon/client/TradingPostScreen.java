package de.infinitehorizon.client;

import de.infinitehorizon.logistics.TradingPostMenu;
import de.infinitehorizon.logistics.TradingRules;
import de.infinitehorizon.network.TradingPostEditPayload;
import de.infinitehorizon.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Handelsposten-GUI (23.3): links je Angebot Muster X → Y, Knopf „Tauschen“ und der Vorrat; rechts das Lager (nur
 * Mitglieder) und das Inventar. Mitglieder setzen Muster per Klick mit einem Item (links: ganzer Stapel, rechts: ein
 * Stück mehr; ohne Item links leeren, rechts ein Stück weniger). Aus Rechtecken gezeichnet wie das Maschinen-GUI.
 */
public class TradingPostScreen extends AbstractContainerScreen<TradingPostMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int PATTERN_FILL = 0xFFA8B4C8;
    private static final int TEXT = 0xFF404040;
    private static final int GREY = 0xFF707070;
    private static final int BUTTON_H = 16;

    private final List<Button> tradeButtons = new ArrayList<>();

    public TradingPostScreen(TradingPostMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TradingPostMenu.WIDTH, TradingPostMenu.HEIGHT);
        this.inventoryLabelX = TradingPostMenu.STORAGE_X;
        this.inventoryLabelY = TradingPostMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        tradeButtons.clear();
        for (int i = 0; i < menu.data().offers(); i++) {
            int offer = i;
            Button b = Button.builder(Component.translatable("gui.infinite_horizon.trading_post.trade"), x -> trade(offer))
                    .bounds(leftPos + TradingPostMenu.TRADE_X, topPos + TradingPostMenu.OFFER_Y + i * TradingPostMenu.OFFER_ROW,
                            TradingPostMenu.TRADE_W, BUTTON_H).build();
            tradeButtons.add(addRenderableWidget(b));
        }
    }

    /** Für Tests: Angebot {@code offer} tauschen wie mit dem Knopf. */
    public void trade(int offer) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, offer);
        }
    }

    public Button tradeButton(int offer) {
        return tradeButtons.get(offer);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < tradeButtons.size(); i++) {
            tradeButtons.get(i).active = menu.available(i) > 0;
        }
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int button, ContainerInput input) {
        if (slot != null && menu.isPattern(slotId)) {
            if (menu.canManage() && minecraft != null && minecraft.player != null && input == ContainerInput.PICKUP) {
                editPattern(menu.patternIndex(slotId), slot.getItem(), menu.getCarried(), button != 0);
            }
            return;
        }
        super.slotClicked(slot, slotId, button, input);
    }

    /** Neues Muster berechnen (reine Regel) und an den Server schicken. */
    private void editPattern(int index, ItemStack current, ItemStack carried, boolean right) {
        boolean same = !carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, current);
        ItemStack base = carried.isEmpty() ? current : carried;
        int n = TradingRules.patternCount(current.getCount(), right, carried.getCount(), same,
                base.isEmpty() ? 1 : base.getMaxStackSize());
        ItemStack pattern = n <= 0 || base.isEmpty() ? ItemStack.EMPTY : base.copyWithCount(n);
        Services.NETWORK.sendToServer(new TradingPostEditPayload(index, pattern));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            boolean pattern = menu.isPattern(slot.index);
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT_DARK);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, pattern ? PATTERN_FILL : SLOT_FILL);
        }
        // Pfeil X → Y und Vorrat je Angebot
        for (int i = 0; i < menu.data().offers(); i++) {
            int ry = y + TradingPostMenu.OFFER_Y + i * TradingPostMenu.OFFER_ROW;
            g.text(font, "→", x + TradingPostMenu.OFFER_X + 20, ry + 4, TEXT, false);
            int n = menu.available(i);
            String stock = n >= TradingPostMenu.MAX_SHOWN ? TradingPostMenu.MAX_SHOWN + "+" : "×" + n;
            g.text(font, stock, x + TradingPostMenu.TRADE_X + TradingPostMenu.TRADE_W + 4, ry + 4, n > 0 ? TEXT : GREY, false);
        }
        g.text(font, Component.translatable("gui.infinite_horizon.trading_post.storage"), x + TradingPostMenu.STORAGE_X,
                y + TradingPostMenu.STORAGE_Y - 11, TEXT, false);
        if (!menu.canManage()) {
            Component hint = Component.translatable("gui.infinite_horizon.trading_post.members_only");
            int w = 9 * TradingPostMenu.SLOT;
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(hint, w - 8);
            int ly = y + TradingPostMenu.STORAGE_Y + 40;
            for (var line : lines) {
                g.text(font, line, x + TradingPostMenu.STORAGE_X + 4, ly, GREY, false);
                ly += 10;
            }
        }
    }
}
