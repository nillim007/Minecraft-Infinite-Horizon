package de.infinitehorizon.space;

import com.mojang.serialization.Codec;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.data.RocketPartRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rakete als Entity (12.5): entsteht beim Start aus den Raketenblöcken (Teileliste, Treibstoff, Fracht, Stufe) und
 * steht nach der Landung am Boden. Alle Felder laufen über die Speicherdaten, weil {@code teleport} bei einem
 * Dimensionswechsel eine neue Instanz erzeugt (api-entities-26.3.md, Abschnitt 4). Den Flug steuert
 * {@link FlightService}; Passagiere sitzen auf Höhe der Kapsel.
 */
public class RocketEntity extends Entity implements HasCustomInventoryScreen {

    /** Flugphasen (12.5). */
    public enum Phase { LANDED, COUNTDOWN, ASCENT, LANDING }

    /** Teile von unten nach oben, dazu Flossen-Block und Zahl, als Text für den Client ({@code id,id,…|fin|n}). */
    private static final EntityDataAccessor<String> PARTS = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> FUEL0 = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> FUEL1 = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CAPACITY = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.LONG);

    private List<Identifier> parts = List.of();
    private @Nullable Identifier finPart;
    private int fins;
    private int tier;
    private int seats;
    private List<Identifier> fuelIds = List.of();
    private final long[] fuel = new long[RocketTankBlockEntity.COMPONENTS];
    private long capacity;
    private SimpleContainer cargo = new SimpleContainer(0);
    private @Nullable UUID owner;
    /** Laufender Flug ({@code null} = am Boden). */
    private FlightService.@Nullable Flight flight;
    /** Interner Ausstieg (Abmelden, Zerlegen) trotz Flug. */
    boolean allowDismount;

    public RocketEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ---------------------------------------------------------------- Aufbau

    /** Aus dem Raketen-Computer: Teile, Flossen, Treibstoff, Fracht. */
    public void configure(RocketConfig config, @Nullable Identifier fin, List<ItemStack> cargoItems, @Nullable UUID owner) {
        List<Identifier> ids = new ArrayList<>();
        config.stack().forEach(p -> ids.add(p.block()));
        this.parts = List.copyOf(ids);
        this.finPart = fin;
        this.fins = config.fins();
        this.tier = config.tier();
        this.seats = config.seats();
        this.fuelIds = config.fuelIds();
        for (int i = 0; i < fuel.length && i < config.fuel().length; i++) {
            fuel[i] = config.fuel()[i];
        }
        this.capacity = config.capacity();
        int slots = config.stack().stream().mapToInt(RocketPartRegistry.Part::cargoSlots).sum();
        this.cargo = new SimpleContainer(slots);
        for (int i = 0; i < cargoItems.size() && i < slots; i++) {
            cargo.setItem(i, cargoItems.get(i));
        }
        this.owner = owner;
        syncData();
    }

    private void syncData() {
        entityData.set(PARTS, String.join(",", parts.stream().map(Identifier::toString).toList()) + "|"
                + (finPart == null ? "" : finPart.toString()) + "|" + fins + "|" + tier);
        entityData.set(FUEL0, fuel[0]);
        entityData.set(FUEL1, fuel[1]);
        entityData.set(CAPACITY, capacity);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PARTS, "");
        builder.define(PHASE, Phase.LANDED.ordinal());
        builder.define(FUEL0, 0L);
        builder.define(FUEL1, 0L);
        builder.define(CAPACITY, 0L);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PARTS.equals(key)) {
            refreshDimensions();
        }
    }

    // ---------------------------------------------------------------- Zugriff

    /** Teile aus den synchronisierten Daten (auch auf dem Client). */
    public List<Identifier> clientParts() {
        String[] split = entityData.get(PARTS).split("\\|", -1);
        List<Identifier> out = new ArrayList<>();
        if (split.length > 0 && !split[0].isEmpty()) {
            for (String s : split[0].split(",")) {
                out.add(Identifier.parse(s));
            }
        }
        return out;
    }

    public @Nullable Identifier clientFin() {
        String[] split = entityData.get(PARTS).split("\\|", -1);
        return split.length > 1 && !split[1].isEmpty() ? Identifier.parse(split[1]) : null;
    }

    public int clientFins() {
        String[] split = entityData.get(PARTS).split("\\|", -1);
        try {
            return split.length > 2 ? Integer.parseInt(split[2]) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Stufe aus den synchronisierten Daten (Client). */
    public int clientTier() {
        String[] split = entityData.get(PARTS).split("\\|", -1);
        try {
            return split.length > 3 ? Integer.parseInt(split[3]) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public int height() {
        return Math.max(1, clientParts().size());
    }

    public List<Identifier> parts() {
        return parts;
    }

    public int tier() {
        return tier;
    }

    public int seats() {
        return seats;
    }

    public List<Identifier> fuelIds() {
        return fuelIds;
    }

    public long fuel(int component) {
        return level().isClientSide() ? (component == 0 ? entityData.get(FUEL0) : entityData.get(FUEL1)) : fuel[component];
    }

    public long capacity() {
        return level().isClientSide() ? entityData.get(CAPACITY) : capacity;
    }

    /** Kleinster Vorrat aller Komponenten. */
    public long usableFuel() {
        return Math.min(fuel[0], fuel[1]);
    }

    /** Zieht {@code amount} mB von jeder Komponente ab. */
    public void burn(long amount) {
        for (int i = 0; i < fuel.length; i++) {
            fuel[i] = Math.max(0, fuel[i] - amount);
        }
        syncData();
    }

    /** Tankt Komponente {@code c} um bis zu {@code amount}; gibt die angenommene Menge zurück. */
    public long refuel(int c, long amount) {
        long accepted = Math.max(0, Math.min(amount, capacity - fuel[c]));
        fuel[c] += accepted;
        syncData();
        return accepted;
    }

    public SimpleContainer cargo() {
        return cargo;
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public FlightService.@Nullable Flight flight() {
        return flight;
    }

    void setFlight(FlightService.@Nullable Flight flight) {
        this.flight = flight;
        entityData.set(PHASE, (flight == null ? Phase.LANDED : flight.phase()).ordinal());
        noPhysics = flight != null;
        setNoGravity(true);
    }

    public Phase phase() {
        int p = entityData.get(PHASE);
        return p >= 0 && p < Phase.values().length ? Phase.values()[p] : Phase.LANDED;
    }

    public boolean inFlight() {
        return phase() != Phase.LANDED;
    }

    /** Absteigen gesperrt (12.5: vom Countdown bis zur Landung), außer bei internem Ausstieg (Abmelden, Zerlegen, Teleport). */
    public boolean dismountLocked() {
        return inFlight() && !allowDismount;
    }

    /** Gesamtmasse in t (Teile, Treibstoff, Fracht) wie {@link RocketAssembler}. */
    public double mass() {
        RocketPartRegistry reg = RocketPartRegistry.get();
        double m = 0;
        double perSlot = 0;
        for (Identifier id : parts) {
            RocketPartRegistry.Part p = reg.part(id).orElse(null);
            if (p == null) {
                continue;
            }
            m += p.mass();
            if (p.fuelCapacity() > 0 && capacity > 0) {
                m += p.fuelMassFull() * (fuel[0] + fuel[1]) / (2.0 * capacity);
            }
            perSlot = Math.max(perSlot, p.massPerSlot());
        }
        if (finPart != null) {
            m += fins * reg.part(finPart).map(RocketPartRegistry.Part::mass).orElse(0.0);
        }
        int used = 0;
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            if (!cargo.getItem(i).isEmpty()) {
                used++;
            }
        }
        return m + perSlot * used;
    }

    public int thrust() {
        RocketPartRegistry reg = RocketPartRegistry.get();
        int t = 0;
        for (Identifier id : parts) {
            t += reg.part(id).map(RocketPartRegistry.Part::thrust).orElse(0);
        }
        return t;
    }

    // ---------------------------------------------------------------- Entity

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(1.0f, height());
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // Passagiere sitzen in der Kapsel (oberstes Teil)
        return new Vec3(0, height() - 1, 0);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < Math.max(1, seats);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return !inFlight();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl) {
            FlightService.tick(sl, this);
        }
    }

    /**
     * Rechtsklick (12.5): Schleichen mit leerer Hand zerlegt (Recht {@code BREAK}, nicht im Flug); mit freiem Sitz
     * einsteigen (darf jeder); sonst das Raketen-GUI.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!(player instanceof ServerPlayer sp) || !(level() instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        if (inFlight()) {
            return InteractionResult.FAIL;
        }
        if (player.isSecondaryUseActive() && player.getItemInHand(hand).isEmpty()) {
            if (!Permissions.can(sp, owner, Permissions.Action.BREAK)) {
                sp.sendOverlayMessage(Component.translatable("message.infinite_horizon.agency.protected"));
                return InteractionResult.FAIL;
            }
            FlightService.disassemble(sl, this);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!player.isPassenger() && canAddPassenger(player)) {
            player.startRiding(this);
            return InteractionResult.SUCCESS_SERVER;
        }
        openCustomInventoryScreen(player);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Inventartaste im Sitz bzw. Rechtsklick von außen: Raketen-GUI. */
    @Override
    public void openCustomInventoryScreen(Player player) {
        if (player instanceof ServerPlayer sp && !inFlight()) {
            RocketMenu.open(sp, this);
        }
    }

    /** Teil-Items beim Zerlegen (Teile, Flossen). */
    public List<ItemStack> partItems() {
        List<ItemStack> out = new ArrayList<>();
        for (Identifier id : parts) {
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(i -> out.add(new ItemStack(i)));
        }
        if (finPart != null) {
            BuiltInRegistries.ITEM.getOptional(finPart).ifPresent(i -> {
                for (int n = 0; n < fins; n++) {
                    out.add(new ItemStack(i));
                }
            });
        }
        return out;
    }

    // ---------------------------------------------------------------- Speichern

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.store("parts", Identifier.CODEC.listOf(), parts);
        out.storeNullable("fin", Identifier.CODEC, finPart);
        out.putInt("fins", fins);
        out.putInt("tier", tier);
        out.putInt("seats", seats);
        out.store("fuel_ids", Identifier.CODEC.listOf(), fuelIds);
        out.store("fuel", Codec.LONG.listOf(), List.of(fuel[0], fuel[1]));
        out.putLong("capacity", capacity);
        out.putInt("cargo_slots", cargo.getContainerSize());
        NonNullList<ItemStack> items = NonNullList.withSize(cargo.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, cargo.getItem(i));
        }
        ContainerHelper.saveAllItems(out.child("cargo"), items);
        out.storeNullable("owner_agency", UUIDUtil.CODEC, owner);
        out.storeNullable("flight", FlightService.Flight.CODEC, flight);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        parts = in.read("parts", Identifier.CODEC.listOf()).orElse(List.of());
        finPart = in.read("fin", Identifier.CODEC).orElse(null);
        fins = in.getIntOr("fins", 0);
        tier = in.getIntOr("tier", 0);
        seats = in.getIntOr("seats", 1);
        fuelIds = in.read("fuel_ids", Identifier.CODEC.listOf()).orElse(List.of());
        List<Long> f = in.read("fuel", Codec.LONG.listOf()).orElse(List.of());
        for (int i = 0; i < fuel.length; i++) {
            fuel[i] = i < f.size() ? f.get(i) : 0;
        }
        capacity = in.getLongOr("capacity", 0);
        int slots = in.getIntOr("cargo_slots", 0);
        NonNullList<ItemStack> items = NonNullList.withSize(slots, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in.childOrEmpty("cargo"), items);
        cargo = new SimpleContainer(slots);
        for (int i = 0; i < slots; i++) {
            cargo.setItem(i, items.get(i));
        }
        owner = in.read("owner_agency", UUIDUtil.CODEC).orElse(null);
        setFlight(in.read("flight", FlightService.Flight.CODEC).orElse(null));
        syncData();
    }

    /** Position des Kapselblocks (für Sitzplatz und Abstieg). */
    public BlockPos capsulePos() {
        return BlockPos.containing(getX(), getY() + height() - 1, getZ());
    }
}
