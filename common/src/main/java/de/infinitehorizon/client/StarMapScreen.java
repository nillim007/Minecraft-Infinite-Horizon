package de.infinitehorizon.client;

import de.infinitehorizon.network.LaunchRequestPayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.space.StarMapMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Navigationskarte {@code star_map} (12.5): links die Ziele mit Kosten hin/zurück (grün, gelb, grau), rechts der Landeort
 * (Landefeuer der Agentur in dieser Dimension oder X/Z, leer = Spawn der Dimension) und „Start“. Alle Werte vom Server.
 */
public class StarMapScreen extends AbstractContainerScreen<StarMapMenu> {

    private static final int W = 320;
    private static final int H = 200;
    private static final int PAD = 8;
    private static final int ROW = 16;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int TEXT = 0xFF404040;

    private @Nullable Identifier selected;
    private @Nullable BlockPos beacon;
    private EditBox xField;
    private EditBox zField;
    private final List<AbstractWidget> landingWidgets = new ArrayList<>();

    public StarMapScreen(StarMapMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private static Component planet(Identifier dim) {
        return Component.translatable("planet." + dim.getNamespace() + "." + dim.getPath());
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + PAD + 2 * ROW;
        for (StarMapMenu.Dest d : menu.data().destinations()) {
            ChatFormatting color = !d.unlocked() || d.reach() == 2 ? ChatFormatting.GRAY : d.reach() == 0 ? ChatFormatting.DARK_GREEN
                    : ChatFormatting.GOLD;
            MutableComponent label = planet(d.dimension()).copy().withStyle(color);
            Button b = Button.builder(label, btn -> select(d.dimension()))
                    .bounds(leftPos + PAD, y, W / 2 - 2 * PAD, ROW - 2).build();
            b.active = d.unlocked() && d.reach() != 2;
            addRenderableWidget(b);
            y += ROW + 8;
        }
        rebuildLanding();
    }

    /** Ziel wählen (auch für Tests). */
    public void select(Identifier dimension) {
        selected = dimension;
        beacon = null;
        rebuildLanding();
    }

    private void rebuildLanding() {
        landingWidgets.forEach(this::removeWidget);
        landingWidgets.clear();
        if (selected == null) {
            return;
        }
        int x = leftPos + W / 2 + PAD;
        int y = topPos + PAD + 2 * ROW;
        for (StarMapMenu.BeaconInfo b : menu.data().beacons()) {
            if (!b.dimension().equals(selected)) {
                continue;
            }
            Component name = b.name().isEmpty() ? Component.translatable("gui.infinite_horizon.beacon.default_name",
                    b.pos().getX(), b.pos().getZ()) : Component.literal(b.name());
            landingWidgets.add(addRenderableWidget(Button.builder(name, btn -> beacon = b.pos())
                    .bounds(x, y, W / 2 - 2 * PAD, ROW - 2).build()));
            y += ROW;
        }
        y += 14;
        int fieldW = (W / 2 - 3 * PAD) / 2;
        xField = new EditBox(font, x, y, fieldW, ROW - 2, Component.literal("X"));
        zField = new EditBox(font, x + fieldW + PAD, y, fieldW, ROW - 2, Component.literal("Z"));
        xField.setHint(Component.literal("X"));
        zField.setHint(Component.literal("Z"));
        xField.setResponder(s -> beacon = null);
        zField.setResponder(s -> beacon = null);
        landingWidgets.add(addRenderableWidget(xField));
        landingWidgets.add(addRenderableWidget(zField));
        landingWidgets.add(addRenderableWidget(Button.builder(Component.translatable("gui.infinite_horizon.star_map.launch"),
                btn -> launch()).bounds(x, topPos + H - PAD - ROW, W / 2 - 2 * PAD, ROW).build()));
    }

    private static int parse(@Nullable EditBox box) {
        if (box == null || box.getValue().isBlank()) {
            return LaunchRequestPayload.NO_COORD;
        }
        try {
            return Integer.parseInt(box.getValue().strip());
        } catch (NumberFormatException e) {
            return LaunchRequestPayload.NO_COORD;
        }
    }

    /** Start mit dem gewählten Ziel und Landeort (auch für Tests). */
    public void launch() {
        if (selected == null) {
            return;
        }
        StarMapMenu.Data d = menu.data();
        Services.NETWORK.sendToServer(new LaunchRequestPayload(d.control(), d.entityId(), selected,
                beacon == null ? LaunchRequestPayload.NO_BEACON : beacon, parse(xField), parse(zField)));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + W, y + H, PANEL);
        g.text(font, title, x + PAD, y + PAD, TEXT, false);
        g.text(font, Component.translatable("gui.infinite_horizon.star_map.fuel", menu.data().fuel()), x + PAD, y + PAD + ROW, TEXT, false);
        int ry = y + PAD + 2 * ROW + ROW - 2;
        for (StarMapMenu.Dest d : menu.data().destinations()) {
            Component cost = Component.translatable("gui.infinite_horizon.star_map.cost", d.out(), d.back());
            if (!d.unlocked()) {
                cost = Component.translatable("gui.infinite_horizon.star_map.locked");
            }
            g.text(font, font.substrByWidth(cost, W / 2 - 2 * PAD).getString(), x + PAD, ry, TEXT, false);
            ry += ROW + 8;
        }
        if (selected != null) {
            g.text(font, Component.translatable("gui.infinite_horizon.star_map.landing", planet(selected)), x + W / 2 + PAD,
                    y + PAD + ROW, TEXT, false);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // eigener Kopf
    }
}
