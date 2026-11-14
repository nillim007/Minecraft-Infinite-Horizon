package de.infinitehorizon.space;

import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.agency.AgencyProgress;
import de.infinitehorizon.platform.Services;
import de.infinitehorizon.registry.ModMenus;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Navigationskarte {@code star_map} (12.5): Menü, das der Server mit Zusatzdaten öffnet (Rampe bzw. Raketen-Entity,
 * Treibstoff, Ziele mit Kosten hin und zurück und Farbe, Landefeuer der Agentur je Dimension). Der Client rechnet nichts;
 * die Auswahl sendet {@code LaunchRequestPayload}.
 */
public class StarMapMenu extends AbstractContainerMenu {

    /** Ein Ziel: {@code reach} 0 = hin und zurück, 1 = nur hin, 2 = nicht erreichbar. */
    public record Dest(Identifier dimension, long out, long back, int reach, boolean unlocked) {
        public static final StreamCodec<ByteBuf, Dest> CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Dest::dimension,
                ByteBufCodecs.VAR_LONG, Dest::out,
                ByteBufCodecs.VAR_LONG, Dest::back,
                ByteBufCodecs.VAR_INT, Dest::reach,
                ByteBufCodecs.BOOL, Dest::unlocked,
                Dest::new);
    }

    public record BeaconInfo(Identifier dimension, BlockPos pos, String name) {
        public static final StreamCodec<ByteBuf, BeaconInfo> CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, BeaconInfo::dimension,
                BlockPos.STREAM_CODEC, BeaconInfo::pos,
                ByteBufCodecs.STRING_UTF8, BeaconInfo::name,
                BeaconInfo::new);
    }

    /** Öffnungsdaten. {@code entityId} −1 = Start von der Rampe an {@code control}. */
    public record Data(BlockPos control, int entityId, long fuel, List<Dest> destinations, List<BeaconInfo> beacons) {
        public static final StreamCodec<ByteBuf, Data> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Data::control,
                ByteBufCodecs.VAR_INT, Data::entityId,
                ByteBufCodecs.VAR_LONG, Data::fuel,
                Dest.CODEC.apply(ByteBufCodecs.list()), Data::destinations,
                BeaconInfo.CODEC.apply(ByteBufCodecs.list()), Data::beacons,
                Data::new);
    }

    private final Data data;

    public StarMapMenu(int id, Inventory inventory, Data data) {
        super(ModMenus.STAR_MAP.get(), id);
        this.data = data;
    }

    public Data data() {
        return data;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide()) {
            return true;
        }
        if (data.entityId() >= 0) {
            return player.level().getEntity(data.entityId()) instanceof RocketEntity r && !r.isRemoved()
                    && (player.getVehicle() == r || player.distanceToSqr(r) <= 64);
        }
        return player.getVehicle() instanceof de.infinitehorizon.vehicle.SeatEntity
                || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(data.control())) <= 64;
    }

    private static List<Dest> dests(List<LaunchChecklist.Destination> list) {
        List<Dest> out = new ArrayList<>();
        for (LaunchChecklist.Destination d : list) {
            out.add(new Dest(d.dimension(), d.outbound(), d.back(), d.reach().ordinal(), d.unlocked()));
        }
        return out;
    }

    private static List<BeaconInfo> beacons(@Nullable Agency agency) {
        List<BeaconInfo> out = new ArrayList<>();
        if (agency != null) {
            for (AgencyProgress.Beacon b : agency.progress().beacons()) {
                out.add(new BeaconInfo(b.dimension(), b.pos(), b.name()));
            }
        }
        return out;
    }

    /** Start von der Rampe: Ziele aus der Checkliste (nur, wenn alles bereit ist). */
    public static void openForPad(ServerPlayer player, ServerLevel level, LaunchControlBlockEntity control) {
        LaunchChecklist.State s = control.evaluate(level);
        if (!s.ready() || s.rocket() == null) {
            player.sendOverlayMessage(Component.translatable("gui.infinite_horizon.launch.denied.checklist"));
            return;
        }
        Agency agency = AgencyManager.ensureFor(player);
        List<LaunchChecklist.Destination> list = FlightService.destinations(level, s.rocket().usableFuel(), s.rocket().tier(), agency);
        open(player, new Data(control.getBlockPos(), -1, s.rocket().usableFuel(), dests(list), beacons(agency)));
    }

    /** Neustart einer gelandeten Rakete. */
    public static void openForRocket(ServerPlayer player, RocketEntity rocket) {
        Agency agency = AgencyManager.ensureFor(player);
        List<LaunchChecklist.Destination> list = FlightService.destinationsFor((ServerLevel) rocket.level(), rocket, agency);
        open(player, new Data(rocket.blockPosition(), rocket.getId(), rocket.usableFuel(), dests(list), beacons(agency)));
    }

    private static void open(ServerPlayer player, Data data) {
        Services.REGISTRY.openMenu(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.infinite_horizon.star_map");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return new StarMapMenu(id, inventory, data);
            }
        }, data, Data.CODEC);
    }
}
