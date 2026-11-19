package de.infinitehorizon.client;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.machine.MachineBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Klangschleifen (24.6): Atem und Funkrauschen im Vakuum ohne Kuppel ({@code suit.breath}, {@code suit.radio_static})
 * und Maschinengeräusche ({@code machine.<id>}) bei Status {@code running}. Nur auf dem Client.
 */
public final class AmbientSounds {

    public static final String BREATH = "suit.breath";
    public static final String RADIO = "suit.radio_static";

    private static @Nullable LoopSound breath;
    private static @Nullable LoopSound radio;
    private static final Map<BlockPos, LoopSound> MACHINES = new HashMap<>();
    /** Neuer Versuch, wenn die Klang-Engine eine Schleife verworfen hat (z. B. nach dem Neuladen), höchstens alle 20 Ticks. */
    private static final int RETRY_TICKS = 20;
    private static int ticks;

    private AmbientSounds() {
    }

    private static @Nullable SoundEvent event(String name) {
        return BuiltInRegistries.SOUND_EVENT.getOptional(InfiniteHorizon.id(name)).orElse(null);
    }

    private static boolean suitActive() {
        Minecraft mc = Minecraft.getInstance();
        return SoundDamping.vacuum() && mc.player != null && mc.player.isAlive();
    }

    /** Jeden Client-Tick: Anzug-Schleifen starten bzw. von selbst enden lassen. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            MACHINES.clear();
            breath = null;
            radio = null;
            return;
        }
        ticks++;
        if (suitActive()) {
            breath = ensure(breath, BREATH);
            radio = ensure(radio, RADIO);
        } else {
            if (breath != null) {
                breath.update();
            }
            if (radio != null) {
                radio.update();
            }
        }
        MACHINES.values().forEach(LoopSound::update);
        MACHINES.values().removeIf(LoopSound::isStopped);
    }

    private static @Nullable LoopSound ensure(@Nullable LoopSound current, String name) {
        Minecraft mc = Minecraft.getInstance();
        if (current != null && !current.isStopped()
                && (mc.getSoundManager().isActive(current) || ticks % RETRY_TICKS != 0)) {
            return current;
        }
        if (current != null) {
            current.end();
        }
        SoundEvent e = event(name);
        if (e == null) {
            return null;
        }
        LoopSound s = LoopSound.atListener(e, AmbientSounds::suitActive);
        mc.getSoundManager().play(s);
        return s;
    }

    /** Sind Atem- und Funkschleife angefordert und nicht beendet? (für Tests; ohne Audiogerät spielt die Engine nichts) */
    public static boolean suitLoopsRequested() {
        return breath != null && !breath.isStopped() && radio != null && !radio.isStopped();
    }

    /** Zahl der laufenden Maschinengeräusche (für Tests). */
    public static int machineLoops() {
        return (int) MACHINES.values().stream().filter(s -> !s.isStopped()).count();
    }

    private static boolean running(Level level, BlockPos pos, Block block) {
        BlockState st = level.getBlockState(pos);
        return Minecraft.getInstance().level == level && st.is(block) && st.hasProperty(MachineBlock.STATUS)
                && st.getValue(MachineBlock.STATUS) == MachineStatus.RUNNING;
    }

    /** Client-Ticker einer Maschine: bei {@code running} die Schleife {@code machine.<id>} am Block starten. */
    public static void machineTick(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(MachineBlock.STATUS) || state.getValue(MachineBlock.STATUS) != MachineStatus.RUNNING) {
            return;
        }
        LoopSound current = MACHINES.get(pos);
        if (current != null && !current.isStopped()) {
            return;
        }
        Block block = state.getBlock();
        SoundEvent e = event("machine." + BuiltInRegistries.BLOCK.getKey(block).getPath());
        if (e == null) {
            return;
        }
        BlockPos at = pos.immutable();
        LoopSound s = LoopSound.atBlock(e, at, () -> running(level, at, block));
        MACHINES.put(at, s);
        Minecraft.getInstance().getSoundManager().play(s);
    }
}
