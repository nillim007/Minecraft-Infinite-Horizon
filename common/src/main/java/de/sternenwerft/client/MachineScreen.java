package de.sternenwerft.client;

import de.sternenwerft.machine.MachineLayout;
import de.sternenwerft.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * GUI aller Maschinen (4.1), nur aus Rechtecken gezeichnet (Platzhalter-Optik ohne eigene Textur).
 * Energieleiste links (x 8, 8 × 60 px), Fortschrittspfeil 24 × 17 px in der Mitte; Generatoren zeigen die Brennzeit.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int ENERGY = 0xFFD83A2A;
    private static final int PROGRESS = 0xFFFFFFFF;
    private static final int BURN = 0xFFF0A020;

    private static final int BAR_X = 8;
    private static final int BAR_Y = 17;
    private static final int BAR_W = 8;
    private static final int BAR_H = 60;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT_DARK);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT_FILL);
        }

        long energy = menu.value(0);
        long capacity = menu.value(1);
        int bx = x + BAR_X;
        int by = y + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, SLOT_DARK);
        int filled = capacity > 0 ? (int) Math.min(BAR_H, energy * BAR_H / capacity) : 0;
        g.fill(bx, by + BAR_H - filled, bx + BAR_W, by + BAR_H, ENERGY);

        long now = menu.value(2);
        long total = menu.value(3);
        MachineLayout l = menu.openData().layout();
        int ax = x + MachineMenu.ARROW_X;
        int ay = y + MachineMenu.ROW_Y;
        g.fill(ax, ay, ax + ARROW_W, ay + ARROW_H, SLOT_DARK);
        if (total > 0) {
            boolean generator = l.fuel() > 0;
            long part = generator ? now : Math.min(now, total);
            int w = (int) Math.min(ARROW_W, part * ARROW_W / total);
            g.fill(ax, ay, ax + w, ay + ARROW_H, generator ? BURN : PROGRESS);
        }

        if (inside(mouseX, mouseY, bx, by, BAR_W, BAR_H)) {
            g.setComponentTooltipForNextFrame(font,
                    List.of(Component.translatable("gui.sternenwerft.energy", energy, capacity)), mouseX, mouseY);
        } else if (total > 0 && inside(mouseX, mouseY, ax, ay, ARROW_W, ARROW_H)) {
            String key = l.fuel() > 0 ? "gui.sternenwerft.burn_time" : "gui.sternenwerft.progress_ticks";
            g.setComponentTooltipForNextFrame(font, List.of(Component.translatable(key, now, total)), mouseX, mouseY);
        }
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
