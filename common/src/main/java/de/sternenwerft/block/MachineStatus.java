package de.sternenwerft.block;

import net.minecraft.util.StringRepresentable;

/** Statuslampe (4.1): running grün, idle gelb, error rot. */
public enum MachineStatus implements StringRepresentable {
    IDLE("idle"),
    RUNNING("running"),
    ERROR("error");

    private final String name;

    MachineStatus(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
