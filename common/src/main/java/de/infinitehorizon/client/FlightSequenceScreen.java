package de.infinitehorizon.client;

import de.infinitehorizon.network.FlightSequencePayload;
import de.infinitehorizon.network.RocketActionPayload;
import de.infinitehorizon.platform.Services;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Flugsequenz für die Insassen (12.5): Kamera von außen ({@code THIRD_PERSON_BACK}), HUD aus, Restzeit als Text.
 * Schließen (Esc) sendet {@code skip}; der Server schließt den Flug dann sofort ab. Danach gelten Kamera und HUD wie
 * vorher. SPEC-GAP #77: das Bildschirmbeben folgt mit dem grafischen Feinschliff.
 */
public class FlightSequenceScreen extends Screen {

    private static @Nullable CameraType previousCamera;
    private static boolean previousHideGui;

    private byte type;
    private int remaining;
    private final net.minecraft.resources.Identifier target;
    private boolean ended;

    private FlightSequenceScreen(FlightSequencePayload p) {
        super(Component.translatable("gui.infinite_horizon.flight.title"));
        this.type = p.kind();
        this.remaining = p.ticks();
        this.target = p.target();
    }

    /** Payload-Empfang (Client-Thread). */
    public static void receive(FlightSequencePayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (p.kind() == FlightSequencePayload.END) {
            if (mc.gui.screen() instanceof FlightSequenceScreen s) {
                s.ended = true;
                mc.gui.setScreen(null);
            }
            restore();
            return;
        }
        if (mc.gui.screen() instanceof FlightSequenceScreen s) {
            s.type = p.kind();
            s.remaining = p.ticks();
            return;
        }
        if (previousCamera == null) {
            previousCamera = mc.options.getCameraType();
            previousHideGui = mc.gui.hud.isHidden();
        }
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (!mc.gui.hud.isHidden()) {
            mc.gui.hud.toggle();
        }
        // Den Ladebildschirm nach dem Dimensionswechsel nicht verdrängen; Kamera und HUD gelten trotzdem
        Screen current = mc.gui.screen();
        if (current == null || current instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) {
            mc.setScreenAndShow(new FlightSequenceScreen(p));
        }
    }

    private static void restore() {
        Minecraft mc = Minecraft.getInstance();
        if (previousCamera != null) {
            mc.options.setCameraType(previousCamera);
            if (mc.gui.hud.isHidden() != previousHideGui) {
                mc.gui.hud.toggle();
            }
            previousCamera = null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (remaining > 0) {
            remaining--;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        String key = type == FlightSequencePayload.LANDING ? "gui.infinite_horizon.flight.landing" : "gui.infinite_horizon.flight.launch";
        g.centeredText(font, Component.translatable(key, targetName(), (remaining + 19) / 20), width / 2, height / 6, 0xFFFFFFFF);
        g.centeredText(font, Component.translatable("gui.infinite_horizon.flight.skip"), width / 2, height / 6 + 12, 0xFFB0B0B0);
    }

    private Component targetName() {
        return Component.translatable("planet." + target.getNamespace() + "." + target.getPath());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // keine Abdunklung: die Welt bleibt sichtbar
    }

    @Override
    public void onClose() {
        if (!ended) {
            Services.NETWORK.sendToServer(new RocketActionPayload(RocketActionPayload.SKIP, BlockPos.ZERO, -1));
        }
        restore();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
