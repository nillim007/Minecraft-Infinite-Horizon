package de.infinitehorizon.menu;

import de.infinitehorizon.machine.MachineLayout;

/**
 * Lage aller Bereiche im Maschinen-GUI (4.1), berechnet aus Slot-Aufbau und Tankzahl. Von rechts nach links:
 * Fluidleisten (rechts außen, x 152, weitere Tanks je 18 px nach links), Füll-/Leerslot, Ausgaben, Pfeil, Eingaben.
 * Mehr als 3 Eingaben (Raster 3 × 3) oder mehr als 2 Ausgaben brauchen eine Zusatzzeile (18 px höher).
 *
 * @param tanks      Zahl der Fluidleisten
 * @param fluidLeft  x der linken Fluidleiste (176 ohne Tanks)
 * @param containerX x von Füll- und Leerslot
 * @param outputX    x der Ausgabespalte
 * @param arrowX     x des Fortschrittspfeils
 * @param inputX     x der ersten Eingabespalte
 * @param upgradeX   x des rechten Upgrade-Slots
 * @param upgradeY   y der Upgrade-Slots
 * @param extraRows  Zusatzzeilen (je 18 px)
 */
public record MachineGeometry(int tanks, int fluidLeft, int containerX, int outputX, int arrowX, int inputX,
                              int upgradeX, int upgradeY, int extraRows) {

    public static final int SLOT = 18;
    public static final int FLUID_RIGHT = 152;
    public static final int FLUID_STEP = 18;
    /** Fluidleisten beginnen unter der Kopfzeile mit den Knöpfen „Seite“ und „Tank“. */
    public static final int FLUID_Y = 18;
    public static final int ROW_Y = 35;
    public static final int GRID_COLUMNS = 3;
    /** Ab hier frei von der Inventar-Beschriftung (x 8, etwa 50 px breit) in der Zeile der Upgrades. */
    public static final int LABEL_CLEAR_X = 62;

    // SPEC-GAP: Lage bei mehreren Tanks und 3 × 3-Raster (spec-gaps #27)
    public static MachineGeometry of(MachineLayout l, int tanks) {
        boolean containers = l.fill() + l.drain() > 0;
        int fluidLeft = tanks > 0 ? FLUID_RIGHT - (tanks - 1) * FLUID_STEP : 176;
        int containerX = containers ? fluidLeft - 22 : fluidLeft;
        int outputX = tanks > 0 || containers ? Math.min(MachineMenu.OUTPUT_X, containerX - 20) : MachineMenu.OUTPUT_X;
        int anchor = l.outputs() > 0 ? outputX : containerX;
        int arrowX = Math.min(MachineMenu.ARROW_X, anchor - 31);
        int inputX = l.inputs() > GRID_COLUMNS ? arrowX - 59 : MachineMenu.INPUT_X;
        int extraRows = l.inputs() > GRID_COLUMNS || l.outputs() > 2 ? 1 : 0;
        int upgradeX = tanks > 0 || containers ? containerX - 32 : MachineMenu.UPGRADE_X;
        int upgradeY = extraRows > 0 ? MachineMenu.UPGRADE_Y + 13 : MachineMenu.UPGRADE_Y;
        if (extraRows == 0 && upgradeX - (l.upgrades() - 1) * SLOT < LABEL_CLEAR_X) {
            // unten links stünden die Upgrades auf der Inventar-Beschriftung: oben neben den Füllslot (Elektrolyseur)
            upgradeX = containerX - 22;
            upgradeY = MachineMenu.FILL_Y;
        }
        return new MachineGeometry(tanks, fluidLeft, containerX, outputX, arrowX, inputX, upgradeX, upgradeY, extraRows);
    }

    /** x der Fluidleiste {@code t} (Reihenfolge wie {@code fluidTanks()}: Eingänge links, Ausgänge rechts). */
    public int tankX(int t) {
        return fluidLeft + t * FLUID_STEP;
    }

    public int imageHeight() {
        return 166 + extraRows * SLOT;
    }

    public int inventoryY() {
        return MachineMenu.INVENTORY_Y + extraRows * SLOT;
    }
}
