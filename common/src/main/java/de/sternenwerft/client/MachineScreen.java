package de.sternenwerft.client;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.block.MachineStatus;
import de.sternenwerft.core.fluid.FluidIds;
import de.sternenwerft.machine.MachineBlock;
import de.sternenwerft.machine.MachineBlockEntity;
import de.sternenwerft.machine.MachineLayout;
import de.sternenwerft.menu.MachineGeometry;
import de.sternenwerft.menu.MachineMenu;
import de.sternenwerft.network.MachineConfigPayload;
import de.sternenwerft.platform.Services;
import de.sternenwerft.registry.ModFluids;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI aller Maschinen (4.1), nur aus Rechtecken gezeichnet (Platzhalter-Optik ohne eigene Textur).
 * Energieleiste links (x 8, 8 × 60 px), Fortschrittspfeil 24 × 17 px, Fluidleisten rechts außen ({@link MachineGeometry});
 * Generatoren zeigen die Brennzeit. Kopfzeile: Statuslampe, Titel, Knöpfe „Tank“ und „Seite“.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int ENERGY = 0xFFD83A2A;
    private static final int PROGRESS = 0xFFFFFFFF;
    private static final int BURN = 0xFFF0A020;
    private static final int SELECTED = 0xFFFFFF60;
    // Farben der Statuslampe wie in der Welt (gelb/grün/rot, 4.1)
    private static final int LAMP_IDLE = 0xFFF0C020;
    private static final int LAMP_RUNNING = 0xFF40D040;
    private static final int LAMP_ERROR = 0xFFE04030;

    // Breite 176 px (4.1); die Inventar-Beschriftung steht wie in Vanilla 94 px über der Unterkante
    private static final int GUI_WIDTH = 176;
    private static final int INVENTORY_LABEL_OFFSET = 94;
    private static final int BAR_X = 8;
    // 60 px hoch (4.1), endet über der Inventar-Beschriftung (y 72); der Titel steht rechts daneben
    private static final int BAR_Y = 10;
    private static final int BAR_W = 8;
    private static final int BAR_H = 60;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;
    private static final int LAMP_X = 8;
    private static final int LAMP_Y = 2;
    private static final int LAMP_SIZE = 6;
    private static final int HEADER_Y = 4;
    private static final int HEADER_H = 12;
    private static final int SIDE_X = 124;
    private static final int SIDE_W = 44;
    private static final int TANK_X = 92;
    private static final int TANK_W = 30;
    private static final int FLUID_W = 16;
    // Platzhalterfarben der Vanilla-Fluide (GUI)
    private static final int WATER = 0xFF3F76E4;
    private static final int LAVA = 0xFFE05A10;

    private final MachineGeometry geometry;
    private Button sideButton;
    private Button tankButton;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GUI_WIDTH, menu.openData().geometry().imageHeight());
        this.geometry = menu.openData().geometry();
        this.inventoryLabelY = imageHeight - INVENTORY_LABEL_OFFSET;
        this.titleLabelX = BAR_X + BAR_W + 4;
    }

    @Override
    protected void init() {
        super.init();
        // Knopf „Seite“ (4.1): schaltet die Ausgangsseite durch Unten, Oben, Nord, Süd, West, Ost
        sideButton = Button.builder(Component.translatable("gui.sternenwerft.side"), b -> cycleOutputSide())
                .bounds(leftPos + SIDE_X, topPos + HEADER_Y, SIDE_W, HEADER_H).build();
        addRenderableWidget(sideButton);
        // Knopf „Tank“ (4.1): wählt den Tank für leere Behälter im Füllslot
        tankButton = Button.builder(Component.translatable("gui.sternenwerft.tank"), b -> cycleTank())
                .bounds(leftPos + TANK_X, topPos + HEADER_Y, TANK_W, HEADER_H).build();
        tankButton.visible = false;
        addRenderableWidget(tankButton);
    }

    /** Schickt die nächste Ausgangsseite (Unten, Oben, Nord, Süd, West, Ost) an den Server. */
    public void cycleOutputSide() {
        long current = menu.value(MachineBlockEntity.SYNC_SIDE);
        if (current <= 0) {
            return;
        }
        int next = (int) (current % Direction.values().length);
        Services.NETWORK.sendToServer(new MachineConfigPayload(menu.openData().pos(), next, -1));
    }

    /** Schickt den nächsten wählbaren Tank (1 … n, dann wieder 1) an den Server. */
    public void cycleTank() {
        long packed = menu.value(MachineBlockEntity.SYNC_TANK);
        int count = (int) ((packed >> 8) & 0xFF);
        if (count <= 1) {
            return;
        }
        int next = (int) ((packed & 0xFF) + 1) % count;
        Services.NETWORK.sendToServer(new MachineConfigPayload(menu.openData().pos(), -1, next));
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

        long sideValue = menu.value(MachineBlockEntity.SYNC_SIDE);
        if (sideButton != null) {
            sideButton.visible = sideValue > 0;
            if (sideValue > 0) {
                Direction d = Direction.values()[(int) sideValue - 1];
                sideButton.setMessage(Component.translatable("gui.sternenwerft.side." + d.getSerializedName()));
            }
        }
        long packed = menu.value(MachineBlockEntity.SYNC_TANK);
        int selected = (int) (packed & 0xFF);
        int choices = (int) ((packed >> 8) & 0xFF);
        int firstChoice = (int) ((packed >> 16) & 0xFF);
        if (tankButton != null) {
            tankButton.visible = choices > 1;
            tankButton.setMessage(Component.translatable("gui.sternenwerft.tank.n", firstChoice + selected + 1));
        }

        List<Component> tip = null;
        // Statuslampe (4.1) mit Tooltip; Multiblock: erster fehlender Block mit Koordinate (4.3)
        MachineStatus status = status();
        int lampColor = switch (status) {
            case RUNNING -> LAMP_RUNNING;
            case ERROR -> LAMP_ERROR;
            default -> LAMP_IDLE;
        };
        g.fill(x + LAMP_X - 1, y + LAMP_Y - 1, x + LAMP_X + LAMP_SIZE + 1, y + LAMP_Y + LAMP_SIZE + 1, SLOT_DARK);
        g.fill(x + LAMP_X, y + LAMP_Y, x + LAMP_X + LAMP_SIZE, y + LAMP_Y + LAMP_SIZE, lampColor);
        if (inside(mouseX, mouseY, x + LAMP_X - 1, y + LAMP_Y - 1, LAMP_SIZE + 2, LAMP_SIZE + 2)) {
            tip = new ArrayList<>();
            tip.add(Component.translatable("gui.sternenwerft.status." + status.getSerializedName()));
            if (menu.value(MachineBlockEntity.SYNC_MULTIBLOCK) == 2) {
                BlockPos missing = BlockPos.of(menu.value(MachineBlockEntity.SYNC_MISSING));
                tip.add(Component.translatable("gui.sternenwerft.multiblock.missing", missing.getX(), missing.getY(), missing.getZ()));
            }
        }

        long energy = menu.value(0);
        long capacity = menu.value(1);
        int bx = x + BAR_X;
        int by = y + BAR_Y;
        if (capacity > 0) {
            g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, SLOT_DARK);
        }
        int filled = capacity > 0 ? (int) Math.min(BAR_H, energy * BAR_H / capacity) : 0;
        g.fill(bx, by + BAR_H - filled, bx + BAR_W, by + BAR_H, ENERGY);

        long now = menu.value(2);
        long total = menu.value(3);
        MachineLayout l = menu.openData().layout();
        int ax = x + geometry.arrowX();
        int ay = y + MachineMenu.ROW_Y;
        boolean generator = l.fuel() > 0;
        int w = 0;
        if (total > 0) {
            long part = generator ? now : Math.min(now, total);
            w = (int) Math.min(ARROW_W, part * ARROW_W / total);
        }
        // Pfeil nur bei Blöcken mit Vorgang (Maschinen, Generatoren), nicht bei reinen Speichern
        if (l.inputs() + l.fuel() + l.upgrades() > 0) {
            drawArrow(g, ax, ay, w, generator ? BURN : PROGRESS);
        }

        // Fluidleisten rechts außen (x 152), weitere Tanks je 18 px nach links (4.1); der gewählte Tank ist hell umrandet
        int fy = y + MachineGeometry.FLUID_Y;
        for (int t = 0; t < MachineBlockEntity.MAX_TANKS; t++) {
            long cap = menu.value(6 + 3 * t);
            if (cap <= 0) {
                continue;
            }
            long amount = menu.value(5 + 3 * t);
            Identifier fluid = FluidIds.fromNetworkId(menu.value(4 + 3 * t));
            int fx = x + geometry.tankX(t);
            boolean chosen = choices > 1 && t == firstChoice + selected;
            g.fill(fx - 1, fy - 1, fx + FLUID_W + 1, fy + BAR_H + 1, chosen ? SELECTED : SLOT_DARK);
            g.fill(fx, fy, fx + FLUID_W, fy + BAR_H, SLOT_DARK);
            int h = (int) Math.min(BAR_H, amount * BAR_H / cap);
            if (fluid != null && h > 0) {
                g.fill(fx, fy + BAR_H - h, fx + FLUID_W, fy + BAR_H, fluidColor(fluid));
            }
            if (tip == null && inside(mouseX, mouseY, fx, fy, FLUID_W, BAR_H)) {
                Component name = fluid == null ? Component.literal("-")
                        : Component.translatable("block." + fluid.getNamespace() + "." + fluid.getPath());
                tip = List.of(Component.translatable("gui.sternenwerft.fluid", name, amount, cap));
            }
        }
        if (tip == null && capacity > 0 && inside(mouseX, mouseY, bx, by, BAR_W, BAR_H)) {
            tip = List.of(Component.translatable("gui.sternenwerft.energy", energy, capacity));
        } else if (tip == null && total > 0 && inside(mouseX, mouseY, ax, ay, ARROW_W, ARROW_H)) {
            String key = l.fuel() > 0 ? "gui.sternenwerft.burn_time" : "gui.sternenwerft.progress_ticks";
            tip = List.of(Component.translatable(key, now, total));
        }
        if (tip != null) {
            g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
    }

    /** Status aus dem Blockzustand in der Client-Welt (die Statuslampe des Blocks). */
    private MachineStatus status() {
        if (minecraft == null || minecraft.level == null) {
            return MachineStatus.IDLE;
        }
        BlockState s = minecraft.level.getBlockState(menu.openData().pos());
        return s.hasProperty(MachineBlock.STATUS) ? s.getValue(MachineBlock.STATUS) : MachineStatus.IDLE;
    }

    /** Pfeil 24 × 17 px: Schaft 16 px, Spitze 8 px; die ersten {@code filled} Spalten in {@code color}. */
    private static void drawArrow(GuiGraphicsExtractor g, int x, int y, int filled, int color) {
        int shaft = 16;
        int mid = ARROW_H / 2;
        for (int c = 0; c < ARROW_W; c++) {
            int top;
            int bottom;
            if (c < shaft) {
                top = mid - 3;
                bottom = mid + 4;
            } else {
                int k = c - shaft;
                top = k;
                bottom = ARROW_H - k;
            }
            if (bottom > top) {
                g.fill(x + c, y + top, x + c + 1, y + bottom, c < filled ? color : SLOT_DARK);
            }
        }
    }

    /** Farbe der Fluidleiste: Mod-Fluide aus fluids.json, Vanilla-Wasser/Lava fest, sonst grau. */
    private static int fluidColor(Identifier fluid) {
        if (fluid.getNamespace().equals(Sternenwerft.MOD_ID) && ModFluids.defs().containsKey(fluid.getPath())) {
            return ModFluids.defs().get(fluid.getPath()).color();
        }
        return switch (fluid.toString()) {
            case "minecraft:water" -> WATER;
            case "minecraft:lava" -> LAVA;
            default -> 0xFFA0A0A0;
        };
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
