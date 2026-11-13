package de.infinitehorizon.client;

import de.infinitehorizon.network.RocketActionPayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.space.RocketEntity;
import de.infinitehorizon.space.RocketMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Raketen-GUI (12.5): Treibstoff je Komponente, Frachtraum, „Start“ (Navigationskarte) und „Aus Frachtraum tanken“.
 * Aus Rechtecken gezeichnet wie das Maschinen-GUI.
 */
public class RocketScreen extends AbstractContainerScreen<RocketMenu> {

    private static final int W = 176;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int TEXT = 0xFF404040;
    private static final int BUTTON_W = 78;
    private static final int BUTTON_H = 14;

    public RocketScreen(RocketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, height(menu));
        this.inventoryLabelY = RocketMenu.CARGO_Y + rows(menu) * RocketMenu.SLOT + 3;
    }

    private static int rows(RocketMenu menu) {
        return (menu.data().cargoSlots() + RocketMenu.COLUMNS - 1) / RocketMenu.COLUMNS;
    }

    private static int height(RocketMenu menu) {
        return RocketMenu.CARGO_Y + rows(menu) * RocketMenu.SLOT + 14 + 4 * RocketMenu.SLOT + 12;
    }

    @Override
    protected void init() {
        super.init();
        int id = menu.data().entityId();
        addRenderableWidget(Button.builder(Component.translatable("gui.infinite_horizon.rocket.start"),
                b -> Services.NETWORK.sendToServer(new RocketActionPayload(RocketActionPayload.START, BlockPos.ZERO, id)))
                .bounds(leftPos + 8, topPos + 40, BUTTON_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.infinite_horizon.rocket.refuel"),
                b -> Services.NETWORK.sendToServer(new RocketActionPayload(RocketActionPayload.REFUEL, BlockPos.ZERO, id)))
                .bounds(leftPos + W - 8 - BUTTON_W, topPos + 40, BUTTON_W, BUTTON_H).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, SLOT_DARK);
            g.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, SLOT_FILL);
        }
        if (minecraft != null && minecraft.level != null && minecraft.level.getEntity(menu.data().entityId()) instanceof RocketEntity r) {
            long cap = r.capacity();
            java.util.List<Identifier> fuels = de.infinitehorizon.data.RocketPartRegistry.client().rule(r.clientTier())
                    .map(de.infinitehorizon.data.RocketPartRegistry.Rule::fuel).orElse(java.util.List.of());
            for (int i = 0; i < fuels.size(); i++) {
                Identifier fluid = fuels.get(i);
                g.text(font, Component.translatable("gui.infinite_horizon.launch.fuel",
                        Component.translatable("block." + fluid.getNamespace() + "." + fluid.getPath()), r.fuel(i), cap),
                        leftPos + 8, topPos + 18 + i * 10, TEXT, false);
            }
        }
    }
}
