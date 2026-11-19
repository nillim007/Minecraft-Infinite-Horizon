package de.infinitehorizon.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.function.BooleanSupplier;

/**
 * Klangschleife (24.6), die sich selbst beendet, sobald {@code keep} false liefert: am Spieler (Anzug, ohne
 * Abschwächung) oder an einem Block (Maschine, mit Entfernung leiser). Die Lautstärke steht in {@code sounds.json}.
 */
public final class LoopSound extends AbstractTickableSoundInstance {

    private final BooleanSupplier keep;

    private LoopSound(SoundEvent event, SoundSource source, BooleanSupplier keep) {
        super(event, source, SoundInstance.createUnseededRandom());
        this.keep = keep;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0f;
    }

    /** Am Spieler (Anzug): folgt dem Hörer, keine Abschwächung. */
    public static LoopSound atListener(SoundEvent event, BooleanSupplier keep) {
        LoopSound s = new LoopSound(event, SoundSource.PLAYERS, keep);
        s.relative = true;
        s.attenuation = Attenuation.NONE;
        return s;
    }

    /** An einem Block (Maschine). */
    public static LoopSound atBlock(SoundEvent event, BlockPos pos, BooleanSupplier keep) {
        LoopSound s = new LoopSound(event, SoundSource.BLOCKS, keep);
        s.x = pos.getX() + 0.5;
        s.y = pos.getY() + 0.5;
        s.z = pos.getZ() + 0.5;
        s.attenuation = Attenuation.LINEAR;
        return s;
    }

    @Override
    public void tick() {
        update();
    }

    /**
     * Beendet die Schleife, wenn die Bedingung nicht mehr gilt. Auch von {@code AmbientSounds} jeden Tick aufgerufen: Die
     * Klang-Engine tickt nur Klänge, die sie wirklich spielt (ohne Audiogerät oder nach dem Verwerfen gar keine).
     */
    public void update() {
        if (!isStopped() && !keep.getAsBoolean()) {
            stop();
        }
    }

    public void end() {
        stop();
    }
}
