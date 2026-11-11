package de.sternenwerft.core.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Fluidmenge in mB, loaderunabhängig über die Fluid-ID ({@code minecraft:water}, {@code sternenwerft:crude_oil} …).
 * Leer ist {@link #EMPTY} (ID null, Menge 0).
 */
public record SwFluidStack(@Nullable Identifier fluid, long amount) {

    public static final SwFluidStack EMPTY = new SwFluidStack(null, 0);

    public static final Codec<SwFluidStack> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("fluid").forGetter(s -> s.fluid),
            Codec.LONG.fieldOf("amount").forGetter(SwFluidStack::amount)
    ).apply(i, SwFluidStack::of));

    public static final StreamCodec<ByteBuf, SwFluidStack> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, s -> s.fluid == null ? "" : s.fluid.toString(),
            ByteBufCodecs.VAR_LONG, SwFluidStack::amount,
            (id, amount) -> id.isEmpty() ? EMPTY : of(Identifier.parse(id), amount));

    public static SwFluidStack of(@Nullable Identifier fluid, long amount) {
        return fluid == null || amount <= 0 ? EMPTY : new SwFluidStack(fluid, amount);
    }

    public boolean isEmpty() {
        return fluid == null || amount <= 0;
    }

    public boolean is(@Nullable Identifier id) {
        return !isEmpty() && fluid.equals(id);
    }

    public SwFluidStack withAmount(long newAmount) {
        return of(fluid, newAmount);
    }
}
