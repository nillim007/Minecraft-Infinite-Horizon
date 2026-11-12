package de.infinitehorizon.core.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Fluidmenge in mB, loaderunabhängig über die Fluid-ID ({@code minecraft:water}, {@code infinite_horizon:crude_oil} …).
 * Leer ist {@link #EMPTY} (ID null, Menge 0).
 */
public record IhFluidStack(@Nullable Identifier fluid, long amount) {

    public static final IhFluidStack EMPTY = new IhFluidStack(null, 0);

    public static final Codec<IhFluidStack> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("fluid").forGetter(s -> s.fluid),
            Codec.LONG.fieldOf("amount").forGetter(IhFluidStack::amount)
    ).apply(i, IhFluidStack::of));

    public static final StreamCodec<ByteBuf, IhFluidStack> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, s -> s.fluid == null ? "" : s.fluid.toString(),
            ByteBufCodecs.VAR_LONG, IhFluidStack::amount,
            (id, amount) -> id.isEmpty() ? EMPTY : of(Identifier.parse(id), amount));

    public static IhFluidStack of(@Nullable Identifier fluid, long amount) {
        return fluid == null || amount <= 0 ? EMPTY : new IhFluidStack(fluid, amount);
    }

    public boolean isEmpty() {
        return fluid == null || amount <= 0;
    }

    public boolean is(@Nullable Identifier id) {
        return !isEmpty() && fluid.equals(id);
    }

    public IhFluidStack withAmount(long newAmount) {
        return of(fluid, newAmount);
    }
}
