package de.infinitehorizon.space;

import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.AgencyProgress;
import de.infinitehorizon.block.IhBlockEntity;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Landefeuer (12.6, spaceflight.json → {@code landing_beacon}): Name; Eintrag in {@code beacons} der Besitzer-Agentur beim
 * Setzen und Umbenennen, Löschen beim Entfernen (Abbau, Explosion). Leerer Name = Standard „Landefeuer X/Z“ (der Client
 * übersetzt ihn).
 */
public class LandingBeaconBlockEntity extends IhBlockEntity {

    private String name = "";

    public LandingBeaconBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public String name() {
        return name;
    }

    /** Größte Namenslänge ({@code name_max_length}). */
    public static int maxLength() {
        return BalanceManager.getOrBundled().entry("spaceflight", "landing_beacon")
                .filter(o -> o.has("name_max_length")).map(o -> o.get("name_max_length").getAsInt()).orElse(0);
    }

    /** Höchste Höhe des Strahls ({@code beam_top_y}, absolut). */
    public static int beamTop() {
        return BalanceManager.getOrBundled().entry("spaceflight", "landing_beacon")
                .filter(o -> o.has("beam_top_y")).map(o -> o.get("beam_top_y").getAsInt()).orElse(0);
    }

    /** Setzt den Namen (gekürzt auf {@code name_max_length}) und trägt das Landefeuer neu ein. */
    public void rename(String newName) {
        int max = maxLength();
        String n = newName.strip();
        this.name = max > 0 && n.length() > max ? n.substring(0, max) : n;
        setChanged();
        register();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private @Nullable Agency agency() {
        if (!(level instanceof ServerLevel sl) || owner() == null) {
            return null;
        }
        return AgencyData.get(sl.getServer()).byId(owner()).orElse(null);
    }

    /** Eintrag in {@code AgencyData.beacons} (12.6). */
    public void register() {
        Agency a = agency();
        if (a != null && level instanceof ServerLevel sl) {
            a.progress().putBeacon(new AgencyProgress.Beacon(sl.dimension().identifier(), worldPosition, name));
            AgencyData.get(sl.getServer()).setDirty();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Agency a = agency();
        if (a != null && level instanceof ServerLevel sl && a.progress().removeBeacon(sl.dimension().identifier(), pos)) {
            AgencyData.get(sl.getServer()).setDirty();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("name", name);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        name = input.getStringOr("name", "");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
