package de.infinitehorizon.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.UUID;

/**
 * {@code star_chart} (18): Beute des Wächters, Komponente {@code chart_owner} (Agentur und Kennung der Karte). Feuerfest;
 * als Item-Entity verschwindet sie nicht von selbst. Wird sie zerstört, verliert die Agentur ihre Karte
 * ({@link de.infinitehorizon.creature.SignalData#chartLost}).
 */
public class StarChartItem extends Item {

    /** Besitzer der Karte (26: {@code chart_owner}). */
    public record ChartOwner(UUID agency, UUID uuid) {
        public static final Codec<ChartOwner> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("agency").forGetter(ChartOwner::agency),
                UUIDUtil.CODEC.fieldOf("uuid").forGetter(ChartOwner::uuid)
        ).apply(i, ChartOwner::new));
        public static final StreamCodec<io.netty.buffer.ByteBuf, ChartOwner> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, ChartOwner::agency, UUIDUtil.STREAM_CODEC, ChartOwner::uuid, ChartOwner::new);
    }

    public StarChartItem(Properties properties) {
        super(properties);
    }
}
