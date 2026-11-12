package de.infinitehorizon.agency;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Rolle eines Mitglieds in einer Agentur (23.1). */
public enum Role implements StringRepresentable {
    LEADER("leader"), ENGINEER("engineer"), GUEST("guest");

    public static final Codec<Role> CODEC = StringRepresentable.fromEnum(Role::values);

    private final String name;

    Role(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
