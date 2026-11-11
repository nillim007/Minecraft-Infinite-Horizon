package de.sternenwerft.machine;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Slot-Aufbau einer Maschine laut Balance (4.1): Eingänge, Formslot, Ausgänge, Upgrades, Brennstoff – in dieser
 * Reihenfolge im Inventar. Wird beim Öffnen des GUI an den Client geschickt, weil der Client keine Balance kennt.
 */
public record MachineLayout(int inputs, int molds, int outputs, int upgrades, int fuel) {

    public static final MachineLayout EMPTY = new MachineLayout(0, 0, 0, 0, 0);

    public static final StreamCodec<ByteBuf, MachineLayout> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MachineLayout::inputs,
            ByteBufCodecs.VAR_INT, MachineLayout::molds,
            ByteBufCodecs.VAR_INT, MachineLayout::outputs,
            ByteBufCodecs.VAR_INT, MachineLayout::upgrades,
            ByteBufCodecs.VAR_INT, MachineLayout::fuel,
            MachineLayout::new);

    public enum Role { INPUT, MOLD, OUTPUT, UPGRADE, FUEL, NONE }

    public int total() {
        return inputs + molds + outputs + upgrades + fuel;
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
        return Role.FUEL;
    }
}
