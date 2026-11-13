package de.infinitehorizon.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Inhalt der Komponente {@code infinite_horizon:sample} (11.2, 26): beprobter Block und Dimension. */
public record SampleData(Identifier block, Identifier dimension) {

    public static final Codec<SampleData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("block").forGetter(SampleData::block),
            Identifier.CODEC.fieldOf("dimension").forGetter(SampleData::dimension)
    ).apply(i, SampleData::new));

    public static final StreamCodec<ByteBuf, SampleData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SampleData::block,
            Identifier.STREAM_CODEC, SampleData::dimension,
            SampleData::new);
}
