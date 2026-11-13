package de.infinitehorizon.food;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Data Component {@code infinite_horizon:nutrition} der {@code space_ration} (17). */
public record Nutrition(int nutrition, float saturation) {

    public static final Codec<Nutrition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("nutrition").forGetter(Nutrition::nutrition),
            Codec.FLOAT.fieldOf("saturation").forGetter(Nutrition::saturation)).apply(i, Nutrition::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Nutrition> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Nutrition::nutrition, ByteBufCodecs.FLOAT, Nutrition::saturation, Nutrition::new);
}
