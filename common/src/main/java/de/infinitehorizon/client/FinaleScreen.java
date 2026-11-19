package de.infinitehorizon.client;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.network.FinalePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Finale (21.3): Vollbild-Sequenz auf schwarzem Sternenhintergrund. Nacheinander die besuchten Planeten, je
 * {@code sequence_ticks ÷ Anzahl} mit langsamem Zoom, am Ende die Sonne als kleiner Punkt. Danach antwortet der Sender
 * (Ton und Text aus {@code texts/signal_reply.txt}), dann läuft der Abspann ({@code texts/credits.txt}) wie im
 * Vanilla-Abspann nach oben. Esc schließt jederzeit.
 */
public class FinaleScreen extends Screen {

    private static final int STARS = 220;
    private static final int REPLY_TICKS = 260;
    private static final float CREDITS_SPEED = 0.5f;
    private static final int LINE = 12;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFFA0A0C0;
    private static final int SUN = 0xFFFFF4D6;
    /** Seitenrand für umbrochene Textzeilen. */
    private static final int MARGIN = 20;

    private final List<Identifier> bodies;
    private final int sequenceTicks;
    private final int perBody;
    private final List<String> reply;
    private final List<String> credits;
    private int ticks;
    private boolean replyPlayed;

    public FinaleScreen(FinalePayload p) {
        super(Component.translatable("gui.infinite_horizon.finale.title"));
        List<Identifier> list = new ArrayList<>();
        for (Identifier d : p.visited()) {
            if (!list.contains(d)) {
                list.add(d);
            }
        }
        this.bodies = List.copyOf(list);
        this.sequenceTicks = p.sequenceTicks();
        this.perBody = de.infinitehorizon.shipyard.Finale.perBodyTicks(p.sequenceTicks(), Math.max(1, bodies.size()));
        this.reply = text("signal_reply");
        this.credits = text("credits");
    }

    public static void receive(FinalePayload p) {
        Minecraft.getInstance().setScreenAndShow(new FinaleScreen(p));
    }

    /** Für Tests: Phase vorspulen. */
    public void skipTo(int tick) {
        ticks = tick;
    }

    public int sequenceTicks() {
        return sequenceTicks;
    }

    public static int replyTicks() {
        return REPLY_TICKS;
    }

    /** Text aus {@code texts/<name>.<sprache>.txt}, sonst {@code texts/<name>.txt}. */
    private static List<String> text(String name) {
        Minecraft mc = Minecraft.getInstance();
        String lang = mc.getLanguageManager().getSelected();
        for (String file : List.of("texts/" + name + "." + lang + ".txt", "texts/" + name + ".txt")) {
            var res = mc.getResourceManager().getResource(InfiniteHorizon.id(file));
            if (res.isPresent()) {
                try (BufferedReader r = res.get().openAsReader()) {
                    return r.lines().toList();
                } catch (java.io.IOException e) {
                    InfiniteHorizon.LOG.warn("Text {} nicht lesbar", file, e);
                }
            }
        }
        return List.of();
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (!replyPlayed && ticks >= sequenceTicks) {
            replyPlayed = true;
            SoundEvent s = BuiltInRegistries.SOUND_EVENT.getValue(InfiniteHorizon.id("finale.signal_reply"));
            if (s != null && minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(s, 1.0f));
            }
        }
        int creditsEnd = sequenceTicks + REPLY_TICKS + (int) ((credits.size() * LINE + height) / CREDITS_SPEED);
        if (ticks > creditsEnd) {
            onClose();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF000000);
        Random r = new Random(42);
        for (int i = 0; i < STARS; i++) {
            int x = r.nextInt(Math.max(1, width));
            int y = r.nextInt(Math.max(1, height));
            int a = 0x60 + r.nextInt(0x9F);
            g.fill(x, y, x + 1, y + 1, (a << 24) | 0xFFFFFF);
        }
    }

    /** Textur des Himmelskörpers zur Dimension ({@code overworld} → earth, {@code *_orbit} → Planet), falls vorhanden. */
    private Identifier texture(Identifier dim) {
        String name = "overworld".equals(dim.getPath()) ? "earth" : dim.getPath().replace("_orbit", "");
        String ns = "minecraft".equals(dim.getNamespace()) ? InfiniteHorizon.MOD_ID : dim.getNamespace();
        return Identifier.fromNamespaceAndPath(ns, "textures/environment/celestial/" + name + ".png");
    }

    /** Farbe für Planeten ohne eigene Textur: Himmelsfarbe aus der Planeten-Registry. */
    private static int color(Identifier dim) {
        var p = de.infinitehorizon.data.PlanetRegistry.client().planet(dim).orElse(null);
        if (p != null && p.raw().has("sky") && p.raw().getAsJsonObject("sky").has("fog_color")) {
            try {
                return 0xFF000000 | Integer.parseInt(p.raw().getAsJsonObject("sky").get("fog_color").getAsString().replace("#", ""), 16);
            } catch (NumberFormatException ignored) {
                // Standardfarbe
            }
        }
        return 0xFF8890A0;
    }

    private static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.sqrt((double) r * r - dy * dy);
            g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        float t = ticks + partialTick;
        int cx = width / 2;
        int cy = height / 2;
        if (t < sequenceTicks) {
            int i = Math.min(bodies.size() - 1, (int) (t / Math.max(1, perBody)));
            if (i >= 0) {
                Identifier dim = bodies.get(i);
                float local = (t - i * perBody) / Math.max(1, perBody);
                int r = (int) (height * (0.12 + 0.18 * local));
                Identifier tex = texture(dim);
                if (minecraft != null && minecraft.getResourceManager().getResource(tex).isPresent()) {
                    g.blit(RenderPipelines.GUI_TEXTURED, tex, cx - r, cy - r, 0, 0, 2 * r, 2 * r, 2 * r, 2 * r);
                } else {
                    disc(g, cx, cy, r, color(dim));
                }
                g.centeredText(font, Component.translatable("planet." + dim.getNamespace() + "." + dim.getPath()),
                        cx, height - 30, DIM);
            }
            return;
        }
        // die Sonne als kleiner Punkt
        disc(g, cx, height / 4, 2, SUN);
        float rt = t - sequenceTicks;
        if (rt < REPLY_TICKS) {
            // Zeilen umbrechen, damit nichts über den Rand läuft
            int shown = (int) Math.min(reply.size(), rt / 20 + 1);
            List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
            for (int k = 0; k < shown; k++) {
                lines.addAll(font.split(Component.literal(reply.get(k)), Math.max(40, width - 2 * MARGIN)));
            }
            for (int k = 0; k < lines.size(); k++) {
                g.centeredText(font, lines.get(k), cx, cy - lines.size() * LINE / 2 + k * LINE, TEXT);
            }
            return;
        }
        float scroll = (rt - REPLY_TICKS) * CREDITS_SPEED;
        for (int k = 0; k < credits.size(); k++) {
            int y = (int) (height - scroll + k * LINE);
            if (y > -LINE && y < height) {
                g.centeredText(font, Component.literal(credits.get(k)), cx, y, k == 0 ? SUN : TEXT);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
