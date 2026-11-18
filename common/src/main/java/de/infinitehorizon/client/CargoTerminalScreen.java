package de.infinitehorizon.client;

import de.infinitehorizon.core.fluid.FluidIds;
import de.infinitehorizon.logistics.CargoTerminalMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Frachtterminal-GUI (20.1): Lager mit Bildlaufleiste (Mausrad oder Ziehen), rechts der Treibstofftank und die
 * Frachttanks; Klick auf eine Leiste wählt den Tank für den Füllslot. Aus Rechtecken gezeichnet wie das Maschinen-GUI.
 */
public class CargoTerminalScreen extends AbstractContainerScreen<CargoTerminalMenu> {

    private static final int W = 260;
    private static final int H = 222;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int SELECTED = 0xFFFFFF60;
    private static final int KNOB = 0xFFE0E0E0;
    private static final int SCROLL_X = 174;
    private static final int SCROLL_W = 10;
    private static final int SCROLL_H = CargoTerminalMenu.VISIBLE_ROWS * CargoTerminalMenu.SLOT;
    private static final int KNOB_H = 15;
    private static final int FUEL_X = 190;
    private static final int CARGO_X = 200;
    private static final int TANK_STEP = 7;
    private static final int TANK_W = 6;
    private static final int TANK_Y = CargoTerminalMenu.GRID_Y;
    private static final int TANK_H = 74;

    private boolean dragging;

    public CargoTerminalScreen(CargoTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
        this.inventoryLabelY = CargoTerminalMenu.INVENTORY_Y - 11;
    }

    private int tankX(int t) {
        return t == 0 ? FUEL_X : CARGO_X + (t - 1) * TANK_STEP;
    }

    private void scrollTo(int row) {
        int r = Math.max(0, Math.min(row, menu.maxRow()));
        if (r != menu.firstRow()) {
            menu.setFirstRowClient(r);
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, r);
            }
        }
    }

    private void scrollToMouse(double my) {
        double rel = (my - (topPos + CargoTerminalMenu.GRID_Y) - KNOB_H / 2.0) / (SCROLL_H - KNOB_H);
        scrollTo((int) Math.round(Math.max(0, Math.min(1, rel)) * menu.maxRow()));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (menu.maxRow() > 0 && dy != 0) {
            scrollTo(menu.firstRow() - (int) Math.signum(dy));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x() - leftPos;
            double my = event.y() - topPos;
            if (menu.maxRow() > 0 && inside(mx, my, SCROLL_X, CargoTerminalMenu.GRID_Y, SCROLL_W, SCROLL_H)) {
                dragging = true;
                scrollToMouse(event.y());
                return true;
            }
            for (int t = 0; t < menu.data().tanks(); t++) {
                if (inside(mx, my, tankX(t), TANK_Y, TANK_W, TANK_H) && minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CargoTerminalMenu.SELECT_TANK + t);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            scrollToMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
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
        // Bildlaufleiste
        int sy = y + CargoTerminalMenu.GRID_Y;
        g.fill(x + SCROLL_X, sy - 1, x + SCROLL_X + SCROLL_W, sy + SCROLL_H + 1, SLOT_DARK);
        int knob = menu.maxRow() == 0 ? 0 : menu.firstRow() * (SCROLL_H - KNOB_H) / menu.maxRow();
        g.fill(x + SCROLL_X + 1, sy + knob, x + SCROLL_X + SCROLL_W - 1, sy + knob + KNOB_H, KNOB);

        List<Component> tip = null;
        int selected = menu.selectedTank();
        for (int t = 0; t < menu.data().tanks(); t++) {
            long cap = menu.value(3 * t + 2);
            long amount = menu.value(3 * t + 1);
            Identifier fluid = FluidIds.fromNetworkId(menu.value(3 * t));
            int fx = x + tankX(t);
            int fy = y + TANK_Y;
            g.fill(fx - 1, fy - 1, fx + TANK_W + 1, fy + TANK_H + 1, t == selected ? SELECTED : SLOT_DARK);
            g.fill(fx, fy, fx + TANK_W, fy + TANK_H, SLOT_FILL);
            int h = cap > 0 ? (int) Math.min(TANK_H, amount * TANK_H / cap) : 0;
            if (fluid != null && h > 0) {
                g.fill(fx, fy + TANK_H - h, fx + TANK_W, fy + TANK_H, FluidColors.of(fluid));
            }
            if (inside(mouseX - x, mouseY - y, tankX(t), TANK_Y, TANK_W, TANK_H)) {
                Component name = fluid == null ? Component.literal("-")
                        : Component.translatable("block." + fluid.getNamespace() + "." + fluid.getPath());
                tip = new ArrayList<>();
                tip.add(t == 0 ? Component.translatable("gui.infinite_horizon.cargo_terminal.fuel_tank")
                        : Component.translatable("gui.infinite_horizon.cargo_terminal.cargo_tank", t));
                tip.add(Component.translatable("gui.infinite_horizon.fluid", name, amount, cap));
            }
        }
        if (tip != null) {
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
