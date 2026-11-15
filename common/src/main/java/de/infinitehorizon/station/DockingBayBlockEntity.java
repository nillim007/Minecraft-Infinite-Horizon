package de.infinitehorizon.station;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.core.fluid.IhFluidTank;
import de.infinitehorizon.core.transaction.Transaction;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.space.LandingBeaconBlockEntity;
import de.infinitehorizon.space.RocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Andockbucht (14.2, 12.2): Landeziel in der Landeortwahl (Eintrag wie ein Landefeuer), tankt eine darauf stehende Rakete
 * aus angrenzenden Tanks. Tankrate wie die Treibstoffpumpe ({@code transfer_per_tick}, SPEC-GAP #97).
 */
public class DockingBayBlockEntity extends LandingBeaconBlockEntity {

    public DockingBayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static long rate() {
        return BalanceManager.get().entry("machines", InfiniteHorizon.id("fuel_pump").toString())
                .filter(o -> o.has("transfer_per_tick")).map(o -> o.get("transfer_per_tick").getAsLong()).orElse(0L);
    }

    /** Rakete, die auf der Bucht steht (nicht im Flug), sonst null. */
    public @Nullable RocketEntity dockedRocket(ServerLevel level) {
        List<RocketEntity> list = level.getEntitiesOfClass(RocketEntity.class, new AABB(worldPosition.above()));
        return list.stream().filter(r -> !r.inFlight()).findFirst().orElse(null);
    }

    /** Rakete im Bau auf der Bucht (12.4): Tanks und Fernflug-Modul aus angrenzenden Tanks füllen. */
    private void fillStack(ServerLevel level, long rate) {
        var check = de.infinitehorizon.space.LaunchPad.dockCheck(level, worldPosition);
        if (check.stack().isEmpty()) {
            return;
        }
        var reg = de.infinitehorizon.data.RocketPartRegistry.get();
        var rule = reg.part(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(check.stack().getFirst())
                .getBlock())).flatMap(p -> reg.rule(p.tier())).orElse(null);
        if (rule == null) {
            return;
        }
        for (int c = 0; c < rule.fuel().size(); c++) {
            Identifier fluid = rule.fuel().get(c);
            for (net.minecraft.core.BlockPos p : check.stack()) {
                if (!(level.getBlockEntity(p) instanceof de.infinitehorizon.space.RocketTankBlockEntity tank)) {
                    continue;
                }
                long want = Math.min(rate, tank.capacity() - tank.fuel()[c]);
                for (Direction d : Direction.values()) {
                    if (want <= 0) {
                        break;
                    }
                    if (!(level.getBlockEntity(worldPosition.relative(d)) instanceof MachineBlockEntity source)) {
                        continue;
                    }
                    for (IhFluidTank t : source.fluidTanks()) {
                        if (want <= 0 || !t.stack().is(fluid)) {
                            continue;
                        }
                        try (Transaction tx = Transaction.open()) {
                            long took = t.extract(fluid, want, tx).amount();
                            long accepted = tank.fill(c, took);
                            if (accepted == took) {
                                tx.commit();
                                want -= accepted;
                            }
                        }
                    }
                }
            }
        }
    }

    public void serverTick(ServerLevel level) {
        if (!StationModules.works(level, worldPosition, "docking_bay")) {
            return;
        }
        RocketEntity rocket = dockedRocket(level);
        long rate = rate();
        if (rate <= 0) {
            return;
        }
        if (rocket == null) {
            fillStack(level, rate);
            return;
        }
        List<Identifier> fuels = rocket.fuelIds();
        for (int c = 0; c < fuels.size(); c++) {
            long want = Math.min(rate, rocket.capacity() - rocket.fuel(c));
            for (Direction d : Direction.values()) {
                if (want <= 0) {
                    break;
                }
                if (!(level.getBlockEntity(worldPosition.relative(d)) instanceof MachineBlockEntity tank)) {
                    continue;
                }
                for (IhFluidTank t : tank.fluidTanks()) {
                    if (want <= 0 || !t.stack().is(fuels.get(c))) {
                        continue;
                    }
                    try (Transaction tx = Transaction.open()) {
                        long took = t.extract(fuels.get(c), want, tx).amount();
                        long accepted = rocket.refuel(c, took);
                        if (accepted == took) {
                            tx.commit();
                            want -= accepted;
                        }
                    }
                }
            }
        }
    }
}
