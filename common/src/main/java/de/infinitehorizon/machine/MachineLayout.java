package de.infinitehorizon.machine;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * Slot-Aufbau einer Maschine laut Balance (4.1): Eingänge, Formslot, Ausgänge, Upgrades, Brennstoff, Füllslot,
 * Leerslot – in dieser Reihenfolge im Inventar. Wird beim Öffnen des GUI an den Client geschickt, weil der Client
 * keine Balance kennt.
 */
public record MachineLayout(int inputs, int molds, int outputs, int upgrades, int fuel, int fill, int drain) {

    public static final MachineLayout EMPTY = new MachineLayout(0, 0, 0, 0, 0, 0, 0);

    public static final StreamCodec<ByteBuf, MachineLayout> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .apply(ByteBufCodecs.list())
            .map(l -> new MachineLayout(l.get(0), l.get(1), l.get(2), l.get(3), l.get(4), l.get(5), l.get(6)),
                    m -> List.of(m.inputs, m.molds, m.outputs, m.upgrades, m.fuel, m.fill, m.drain));

    public enum Role { INPUT, MOLD, OUTPUT, UPGRADE, FUEL, FILL, DRAIN, NONE }

    /** Aufbau ohne Füll-/Leerslots (Maschinen ohne Fluid). */
    public MachineLayout(int inputs, int molds, int outputs, int upgrades, int fuel) {
        this(inputs, molds, outputs, upgrades, fuel, 0, 0);
    }

    public int total() {
        return inputs + molds + outputs + upgrades + fuel + fill + drain;
    }

    public int moldStart() {
        return inputs;
    }

    public int outputStart() {
        return inputs + molds;
    }

    public int upgradeStart() {
        return outputStart() + outputs;
    }

    public int fuelStart() {
        return upgradeStart() + upgrades;
    }

    public int fillStart() {
        return fuelStart() + fuel;
    }

    public int drainStart() {
        return fillStart() + fill;
    }

    public Role role(int slot) {
        if (slot < 0 || slot >= total()) {
            return Role.NONE;
        }
        if (slot < moldStart()) {
            return Role.INPUT;
        }
        if (slot < outputStart()) {
            return Role.MOLD;
        }
        if (slot < upgradeStart()) {
            return Role.OUTPUT;
        }
        if (slot < fuelStart()) {
            return Role.UPGRADE;
        }
        if (slot < fillStart()) {
            return Role.FUEL;
        }
        if (slot < drainStart()) {
            return Role.FILL;
        }
        return Role.DRAIN;
    }
}
