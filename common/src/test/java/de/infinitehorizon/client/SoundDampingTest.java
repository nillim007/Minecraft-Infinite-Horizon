package de.infinitehorizon.client;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Dämpfung im Vakuum (24.6): reine Regel {@link SoundDamping#factor(boolean, Identifier, SoundSource, double, List)}. */
class SoundDampingTest {

    private static final List<String> EXEMPT = List.of("suit.", "warning.");

    private static Identifier ih(String path) {
        return Identifier.fromNamespaceAndPath("infinite_horizon", path);
    }

    @Test
    void dampsOnlyInVacuum() {
        Identifier step = Identifier.withDefaultNamespace("block.stone.step");
        assertEquals(0.15, SoundDamping.factor(true, step, SoundSource.BLOCKS, 0.15, EXEMPT));
        assertEquals(1.0, SoundDamping.factor(false, step, SoundSource.BLOCKS, 0.15, EXEMPT));
        assertEquals(0.15, SoundDamping.factor(true, ih("machine.crusher"), SoundSource.BLOCKS, 0.15, EXEMPT));
    }

    @Test
    void suitWarningMusicAndMenuStay() {
        assertEquals(1.0, SoundDamping.factor(true, ih("suit.breath"), SoundSource.PLAYERS, 0.15, EXEMPT));
        assertEquals(1.0, SoundDamping.factor(true, ih("warning.oxygen"), SoundSource.MASTER, 0.15, EXEMPT));
        assertEquals(1.0, SoundDamping.factor(true, Identifier.withDefaultNamespace("music.game"), SoundSource.MUSIC, 0.15, EXEMPT));
        assertEquals(1.0, SoundDamping.factor(true, Identifier.withDefaultNamespace("ui.button.click"), SoundSource.UI, 0.15, EXEMPT));
        // nur eigene Anzugtöne sind ausgenommen
        assertEquals(0.15, SoundDamping.factor(true, Identifier.withDefaultNamespace("suit.breath"), SoundSource.PLAYERS, 0.15, EXEMPT));
    }
}
