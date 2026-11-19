package de.infinitehorizon.client;

import de.infinitehorizon.network.StarshipPayload;
import de.infinitehorizon.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Sprungziele des Sternenschiffs (21.3): je Orbit ein Knopf; ein Sprung kostet eine gefüllte Antimaterie-Zelle. */
public class StarshipJumpScreen extends Screen {

    private static final int W = 260;
    private static final int ROW = 22;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int TEXT = 0xFF404040;

    private final StarshipPayload.Targets targets;
    private int left;
    private int top;

    public StarshipJumpScreen(StarshipPayload.Targets targets) {
        super(Component.translatable("gui.infinite_horizon.starship.title"));
        this.targets = targets;
    }

    public static void receive(StarshipPayload.Targets t) {
        Minecraft.getInstance().setScreenAndShow(new StarshipJumpScreen(t));
    }

    private int panelHeight() {
        return 40 + targets.targets().size() * ROW;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - panelHeight()) / 2;
        for (int i = 0; i < targets.targets().size(); i++) {
            Identifier t = targets.targets().get(i);
            addRenderableWidget(Button.builder(Component.translatable("planet." + t.getNamespace() + "." + t.getPath()), b -> {
                Services.NETWORK.sendToServer(new StarshipPayload.Jump(targets.entityId(), t));
                onClose();
            }).bounds(left + 10, top + 30 + i * ROW, W - 20, ROW - 2).build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        g.fill(left, top, left + W, top + panelHeight(), PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.text(font, title, left + 8, top + 6, TEXT, false);
        g.text(font, Component.translatable("gui.infinite_horizon.starship.cost"), left + 8, top + 17, TEXT, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
