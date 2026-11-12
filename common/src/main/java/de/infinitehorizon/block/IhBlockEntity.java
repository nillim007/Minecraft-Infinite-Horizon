package de.infinitehorizon.block;

import de.infinitehorizon.agency.AgencyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Basis aller eigenen Block-Entities (3A.3): speichert {@code owner_agency} (23.1). Gesetzt wird es, wenn ein Spieler
 * den Block platziert ({@link OwnedBlock#claim}); fehlt der Eintrag, ist der Block ohne Besitzer. {@link #owner()}
 * folgt {@code merged_into} und schreibt den gespeicherten Wert dabei um.
 */
public abstract class IhBlockEntity extends BlockEntity {

    public static final String OWNER_KEY = "owner_agency";

    private @Nullable UUID ownerAgency;

    protected IhBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Gespeicherter Wert ohne Auflösung (null = ohne Besitzer). */
    public @Nullable UUID storedOwner() {
        return ownerAgency;
    }

    /** Aktuelle Besitzer-Agentur (null = ohne Besitzer); folgt {@code merged_into} und merkt sich das Ergebnis. */
    public @Nullable UUID owner() {
        if (ownerAgency != null && level instanceof ServerLevel sl) {
            UUID current = AgencyManager.data(sl.getServer()).resolve(ownerAgency);
            if (!current.equals(ownerAgency)) {
                ownerAgency = current;
                setChanged();
            }
        }
        return ownerAgency;
    }

    public void setOwner(@Nullable UUID agency) {
        ownerAgency = agency;
        setChanged();
    }

    /** Der Spieler hat den Block gesetzt: Besitzer wird seine Agentur. */
    public void claimFor(ServerPlayer player) {
        setOwner(AgencyManager.ensureFor(player).id());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable(OWNER_KEY, UUIDUtil.CODEC, ownerAgency);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ownerAgency = input.read(OWNER_KEY, UUIDUtil.CODEC).orElse(null);
    }
}
