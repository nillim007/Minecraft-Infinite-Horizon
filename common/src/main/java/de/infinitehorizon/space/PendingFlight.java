package de.infinitehorizon.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/**
 * Spieler-Attachment {@code pending_flight} (3A.4, 12.5): Wer sich im Flug abmeldet, bekommt Dimension, Raketen-UUID und
 * Landepunkt; beim nächsten Login sitzt er wieder in der Rakete. {@link #NONE} = kein offener Flug.
 */
public record PendingFlight(Identifier dimension, UUID rocket, BlockPos landing) {

    public static final PendingFlight NONE = new PendingFlight(Identifier.withDefaultNamespace("overworld"),
            new UUID(0L, 0L), BlockPos.ZERO);

    public static final MapCodec<PendingFlight> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(PendingFlight::dimension),
            UUIDUtil.CODEC.fieldOf("rocket").forGetter(PendingFlight::rocket),
            BlockPos.CODEC.fieldOf("landing").forGetter(PendingFlight::landing)
    ).apply(i, PendingFlight::new));
    public static final Codec<PendingFlight> CODEC = MAP_CODEC.codec();

    public boolean isNone() {
        return rocket.getMostSignificantBits() == 0L && rocket.getLeastSignificantBits() == 0L;
    }
}
