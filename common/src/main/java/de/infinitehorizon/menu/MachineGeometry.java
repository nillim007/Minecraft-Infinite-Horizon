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
                              int upgradeX, int upgradeY, int extraRows, int fillY, int drainY) {

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
        int outCols = outputColumns(l);
        // zwei Ausgabespalten und ein Tank (Gewächshaus, SPEC-GAP #97): Füll- und Leerslot unter die Fluidleiste
        boolean stacked = containers && outCols > 1 && tanks > 0;
        int containerX = containers && !stacked ? fluidLeft - 22 : fluidLeft;
        int fillY = stacked ? FLUID_Y + FLUID_H + 4 : MachineMenu.FILL_Y;
        int drainY = stacked ? fillY + SLOT : MachineMenu.DRAIN_Y;
        int outputX = tanks > 0 || containers ? Math.min(MachineMenu.OUTPUT_X, containerX - 20 - (outCols - 1) * SLOT)
                : MachineMenu.OUTPUT_X;
        if (stacked) {
            outputX = Math.min(MachineMenu.OUTPUT_X, fluidLeft - 6 - outCols * SLOT);
        }
        int anchor = l.outputs() > 0 ? outputX : containerX;
        int arrowX = Math.min(MachineMenu.ARROW_X, anchor - 31);
        int inputX = l.inputs() > GRID_COLUMNS ? arrowX - 59 : MachineMenu.INPUT_X;
        if (inputX < MIN_X) {
            // enger: Pfeil näher an die Ausgaben, damit das Raster nicht über den Rand ragt
            arrowX = anchor - 26;
            inputX = arrowX - 56;
        }
        int extraRows = Math.max(l.inputs() > GRID_COLUMNS || l.outputs() > 2 ? 1 : 0, rows(l) - 3);
        int upgradeX = tanks > 0 || containers ? containerX - 32 : MachineMenu.UPGRADE_X;
        int upgradeY = extraRows > 0 ? MachineMenu.UPGRADE_Y + 13 : MachineMenu.UPGRADE_Y;
        if (extraRows == 0 && upgradeX - (l.upgrades() - 1) * SLOT < LABEL_CLEAR_X) {
            // unten links stünden die Upgrades auf der Inventar-Beschriftung: oben neben den Füllslot (Elektrolyseur)
            upgradeX = containerX - 22;
            upgradeY = MachineMenu.FILL_Y;
        }
        return new MachineGeometry(tanks, fluidLeft, containerX, outputX, arrowX, inputX, upgradeX, upgradeY, extraRows,
                fillY, drainY);
    }

    /** Kleinste x-Position eines Slots: rechts neben der Energieleiste (x 8, 8 px breit). */
    public static final int MIN_X = 20;
    /** Höhe der Energie- und Fluidleisten. */
    public static final int FLUID_H = 60;

    /** Spalten der Ausgaben: 2 ab 4 Ausgaben, sonst 1. */
    public static int outputColumns(MachineLayout l) {
        return l.outputs() > 3 ? 2 : 1;
    }

    /** Zeilen des höchsten Rasters (Eingaben in 3 Spalten, Ausgaben in 1–2 Spalten). */
    public static int rows(MachineLayout l) {
        int in = l.inputs() > GRID_COLUMNS ? (l.inputs() + GRID_COLUMNS - 1) / GRID_COLUMNS : 1;
        int cols = outputColumns(l);
        int out = (l.outputs() + cols - 1) / cols;
        return Math.max(in, out);
    }

    /** y der obersten Rasterzeile: um die Mittelzeile zentriert, aber nie über der Kopfzeile. */
    public static int gridTop(int rows) {
        return Math.max(MachineMenu.FILL_Y, ROW_Y - (rows - 1) * SLOT / 2);
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
