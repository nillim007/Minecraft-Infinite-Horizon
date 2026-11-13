package de.infinitehorizon.client;

import de.infinitehorizon.network.NamePayload;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.space.LandingBeaconBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Textfeld des Landefeuers (12.6): Name bis {@code name_max_length} Zeichen, Bestätigen sendet {@link NamePayload}. */
public class BeaconNameScreen extends Screen {

    private static final int FIELD_W = 200;
    private static final int FIELD_H = 20;
    /** Fallback, falls der Client keine Balance kennt (Amboss-Grenze). */
    private static final int FALLBACK_MAX = 50;

    private final BlockPos pos;
    private final String initial;
    private EditBox field;

    private BeaconNameScreen(BlockPos pos, String initial) {
        super(Component.translatable("block.infinite_horizon.landing_beacon"));
        this.pos = pos;
        this.initial = initial;
    }

    /** Öffnet das Textfeld (nur Client). */
    public static void open(BlockPos pos, String name) {
        Minecraft.getInstance().setScreenAndShow(new BeaconNameScreen(pos, name));
    }

    @Override
    protected void init() {
        int x = (width - FIELD_W) / 2;
        int y = height / 2 - FIELD_H;
        field = new EditBox(font, x, y, FIELD_W, FIELD_H, Component.translatable("gui.infinite_horizon.beacon.name"));
        int max = LandingBeaconBlockEntity.maxLength();
        field.setMaxLength(max > 0 ? max : FALLBACK_MAX);
        field.setValue(initial);
        field.setHint(Component.translatable("gui.infinite_horizon.beacon.default_hint"));
        addRenderableWidget(field);
        setInitialFocus(field);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> confirm())
                .bounds(x, y + FIELD_H + 6, FIELD_W, FIELD_H).build());
    }

    private void confirm() {
        Services.NETWORK.sendToServer(new NamePayload(pos, field.getValue()));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, title, width / 2, height / 2 - FIELD_H - 14, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
