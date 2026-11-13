package de.infinitehorizon.space;

import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.block.IhBlockEntity;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.network.LaunchStatePayload;
import de.infinitehorizon.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Raketen-Computer {@code launch_control} (4.3, 12.4): prüft die nächste Rampe beim Öffnen, beim Start und alle
 * {@code structure_check_interval_ticks}; Statuslampe {@code error} bei ungültiger Rampe. Offene GUIs bekommen den
 * Zustand jede Sekunde ({@code LaunchStatePayload}). Startet Sonden nach dem Countdown (12.3); Raketen fliegen ab M4e.
 */
public class LaunchControlBlockEntity extends IhBlockEntity implements MenuProvider {

    /** Takt, in dem offene GUIs den Zustand bekommen. */
    private static final int SYNC_TICKS = 20;

    private int countdown;
    private @Nullable Identifier countdownTarget;
    private @Nullable UUID countdownAgency;
    private int untilCheck;
    private LaunchChecklist.@Nullable State last;

    public LaunchControlBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static int checkInterval() {
        return BalanceManager.get().entry("misc", "multiblock_rules")
                .filter(o -> o.has("structure_check_interval_ticks"))
                .map(o -> o.get("structure_check_interval_ticks").getAsInt()).orElse(SYNC_TICKS);
    }

    /** Agentur, deren Fortschritt die Ziele bestimmt: Besitzer, sonst keine. */
    private @Nullable Agency agency(ServerLevel level) {
        UUID owner = owner();
        if (owner == null) {
            return null;
        }
        return AgencyData.get(level.getServer()).byId(owner).orElse(null);
    }

    /** Neu prüfen (Öffnen, Start, Takt) und Statuslampe setzen. */
    public LaunchChecklist.State evaluate(ServerLevel level) {
        last = LaunchChecklist.evaluate(level, worldPosition, agency(level));
        MachineStatus status = countdown > 0 ? MachineStatus.RUNNING
                : last.pad() == null || !last.ok(LaunchChecklist.STRUCTURE) ? MachineStatus.ERROR : MachineStatus.IDLE;
        BlockState s = getBlockState();
        if (s.hasProperty(MachineBlock.STATUS) && s.getValue(MachineBlock.STATUS) != status) {
            level.setBlock(worldPosition, s.setValue(MachineBlock.STATUS, status), 3);
        }
        return last;
    }

    public LaunchChecklist.@Nullable State lastState() {
        return last;
    }

    public int countdown() {
        return countdown;
    }

    /** Sendet den Zustand an einen Spieler mit offenem GUI. */
    public void sendState(ServerPlayer player) {
        if (last == null && level instanceof ServerLevel sl) {
            evaluate(sl);
        }
        if (last != null) {
            Services.NETWORK.sendToPlayer(player, new LaunchStatePayload(worldPosition,
                    LaunchChecklist.toJson(last, countdown).toString()));
        }
    }

    /**
     * Sondenstart (12.3) nach Prüfung: Rampe mit Sonde gültig, Ziel in {@code probe_targets}. Startet den Countdown;
     * gibt den Fehlerschlüssel zurück oder null.
     */
    public @Nullable String requestProbeLaunch(ServerLevel level, Agency agency, Identifier target) {
        if (countdown > 0) {
            return "busy";
        }
        LaunchChecklist.State s = evaluate(level);
        if (s.load() != LaunchPad.Load.PROBE || !s.ok(LaunchChecklist.STRUCTURE)) {
            return "structure";
        }
        if (!ProbeService.canTarget(agency, target)) {
            return "target";
        }
        countdown = Math.max(1, ProbeService.countdownTicks());
        countdownTarget = target;
        countdownAgency = agency.id();
        setChanged();
        return null;
    }

    public void serverTick(ServerLevel level) {
        if (countdown > 0) {
            countdown--;
            if (countdown == 0) {
                finishCountdown(level);
            }
            setChanged();
        }
        if (--untilCheck <= 0) {
            untilCheck = checkInterval();
            evaluate(level);
        }
        if (level.getGameTime() % SYNC_TICKS == 0) {
            for (ServerPlayer p : level.players()) {
                if (p.containerMenu instanceof LaunchControlMenu m && m.pos().equals(worldPosition)) {
                    if (last == null || untilCheck != checkInterval()) {
                        evaluate(level);
                    }
                    sendState(p);
                }
            }
        }
    }

    private void finishCountdown(ServerLevel level) {
        LaunchChecklist.State s = evaluate(level);
        Agency agency = countdownAgency == null ? null : AgencyData.get(level.getServer()).byId(countdownAgency).orElse(null);
        if (s.pad() != null && s.load() == LaunchPad.Load.PROBE && agency != null && countdownTarget != null) {
            ProbeService.launch(level, s.pad().stack().getFirst(), agency, countdownTarget);
        }
        countdownTarget = null;
        countdownAgency = null;
        evaluate(level);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LaunchControlMenu(id, inventory, worldPosition);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("countdown", countdown);
        output.storeNullable("countdown_target", Identifier.CODEC, countdownTarget);
        output.storeNullable("countdown_agency", net.minecraft.core.UUIDUtil.CODEC, countdownAgency);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        countdown = input.getIntOr("countdown", 0);
        countdownTarget = input.read("countdown_target", Identifier.CODEC).orElse(null);
        countdownAgency = input.read("countdown_agency", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }
}
