package de.infinitehorizon.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Komponente {@code infinite_horizon:rover_data} am {@code rover_kit} (16): Energie, O₂ der Druckkabine, Module und
 * Fracht bleiben beim Abbau erhalten.
 */
public record RoverData(long energy, int oxygen, List<Identifier> modules, List<ItemStack> cargo) {

    public static final RoverData EMPTY = new RoverData(0, 0, List.of(), List.of());

    public static final Codec<RoverData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("energy", 0L).forGetter(RoverData::energy),
            Codec.INT.optionalFieldOf("oxygen", 0).forGetter(RoverData::oxygen),
            Identifier.CODEC.listOf().optionalFieldOf("modules", List.of()).forGetter(RoverData::modules),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("cargo", List.of()).forGetter(RoverData::cargo)
    ).apply(i, RoverData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoverData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public RoverData {
        modules = List.copyOf(modules);
        cargo = cargo.stream().map(ItemStack::copy).toList();
    }

    /** Stimmen Energie, O₂, Module und Fracht überein (für Tests)? */
    public boolean sameAs(RoverData o) {
        if (energy != o.energy || oxygen != o.oxygen || !modules.equals(o.modules) || cargo.size() != o.cargo.size()) {
            return false;
        }
        for (int k = 0; k < cargo.size(); k++) {
            if (!ItemStack.matches(cargo.get(k), o.cargo.get(k))) {
                return false;
            }
        }
        return true;
    }
}
