package de.sternenwerft.multiblock;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Controller eines Multiblocks (4.3, 3A.3). Umgesetzt von {@link MultiblockControllerBlockEntity}; Addons mit eigener
 * Basisklasse können das Interface direkt umsetzen und sich bei {@link MultiblockRegistry} eintragen.
 */
public interface MultiblockController {

    /** Muster laut Daten; null, wenn es fehlt. */
    @Nullable MultiblockPattern pattern();

    /** War die letzte Prüfung vollständig? */
    boolean structureComplete();

    /** Erster falscher Platz der letzten Prüfung; null, wenn vollständig. */
    @Nullable BlockPos firstMissing();

    /** Gehäuse oder Port in der Nähe hat sich geändert: im nächsten Tick neu prüfen. */
    void markStructureDirty();

    /** Gehört {@code pos} zum zuletzt vollständig erkannten Aufbau (Gehäuse oder Port)? */
    boolean ownsPart(BlockPos pos);
}
