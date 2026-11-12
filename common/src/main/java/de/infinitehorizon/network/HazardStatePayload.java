package de.infinitehorizon.network;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.environment.Hazard;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Arrays;

/**
 * S→C: Umweltzustand für das HUD (3A.5, 8.4), bei Änderung und höchstens einmal je Prüftakt.
 *
 * @param percent    Belastung 0–100 je Leiste in der Reihenfolge {@link #BAR_IDS}
 * @param temperature Temperatur in °C (gerundet)
 * @param pressure   Druckstufe als {@code Pressure.ordinal()}
 * @param radiation  Strahlungsstufe 0–3
 * @param breathable atembar
 * @param suitOxygen O₂ im Anzug in mB, −1 ohne vollständigen Anzug
 */
public record HazardStatePayload(byte[] percent, int temperature, byte pressure, byte radiation, boolean breathable,
                                 int suitOxygen) implements CustomPacketPayload {

    /** Leisten des HUD (environment.json {@code hud.hazard_bars}); Tiefseedruck zählt zur Leiste Druck. */
    public static final String[] BAR_IDS = {"oxygen", "pressure", "heat", "cold", "radiation"};
    public static final Hazard[][] BARS = {{Hazard.OXYGEN}, {Hazard.PRESSURE, Hazard.DEEP_PRESSURE}, {Hazard.HEAT},
            {Hazard.COLD}, {Hazard.RADIATION}};

    public static final Type<HazardStatePayload> TYPE = new Type<>(InfiniteHorizon.id("hazard_state"));
    public static final StreamCodec<ByteBuf, HazardStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE_ARRAY, HazardStatePayload::percent,
            ByteBufCodecs.VAR_INT, HazardStatePayload::temperature,
            ByteBufCodecs.BYTE, HazardStatePayload::pressure,
            ByteBufCodecs.BYTE, HazardStatePayload::radiation,
            ByteBufCodecs.BOOL, HazardStatePayload::breathable,
            ByteBufCodecs.VAR_INT, HazardStatePayload::suitOxygen,
            HazardStatePayload::new);

    /** Belastung der Leiste {@code index} in Prozent. */
    public int percentOf(int index) {
        return index < percent.length ? percent[index] : 0;
    }

    @Override
    public Type<HazardStatePayload> type() {
        return TYPE;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HazardStatePayload p && Arrays.equals(percent, p.percent) && temperature == p.temperature
                && pressure == p.pressure && radiation == p.radiation && breathable == p.breathable
                && suitOxygen == p.suitOxygen;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(percent) * 31 + temperature * 7 + suitOxygen;
    }
}
