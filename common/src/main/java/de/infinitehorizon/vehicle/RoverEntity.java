package de.infinitehorizon.vehicle;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.core.fluid.ComponentContainers;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.environment.VenusHeat;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
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
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rover (16): 2 Sitze, der erste lenkt (Blickrichtung, vor/zurück), Energie mit Verbrauch je Block, Solardach, Laden
 * neben {@code battery}/{@code lithium_battery_bank}, Frachtraum und Module. Werte aus vehicles.json. Gefahren wird wie
 * beim Boot auf dem Client des Fahrers; der Server rechnet Energie, Module und Hitzezeit.
 */
public class RoverEntity extends Entity implements HasCustomInventoryScreen {

    public static final Identifier ID = InfiniteHorizon.id("rover");
    public static final Identifier SCANNER = InfiniteHorizon.id("rover_scanner");
    public static final Identifier DRILL = InfiniteHorizon.id("rover_drill");
    public static final Identifier HEADLIGHT = InfiniteHorizon.id("rover_headlight");
    public static final Identifier CABIN = InfiniteHorizon.id("rover_pressure_cabin");
    public static final Identifier TRAILER = InfiniteHorizon.id("rover_trailer");

    private static final EntityDataAccessor<Long> ENERGY = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> STOPPED = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> MODULES = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.STRING);

    /** 12.9: {@code minecraft:gravity} = 0,08 × g (Basiswert der Vanilla-Schwerkraft). */
    private static final double BASE_GRAVITY = 0.08;

    private SimpleContainer cargo = new SimpleContainer(0);
    private final List<Identifier> modules = new ArrayList<>();
    private int oxygen;
    private long oxygenMilli;
    private @Nullable UUID owner;
    private double energyDebt;
    private @Nullable Vec3 lastPos;
    private int heatTicks;
    private int drillCooldown;
    private @Nullable BlockPos light;

    public RoverEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ---------------------------------------------------------------- Werte

    public static JsonObject rules() {
        return BalanceManager.getOrBundled().entry("vehicles", ID.toString()).orElseGet(JsonObject::new);
    }

    static JsonObject module(Identifier id) {
        return BalanceManager.getOrBundled().entry("vehicles", id.toString()).orElseGet(JsonObject::new);
    }

    private static double num(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsDouble() : 0;
    }

    /** Energie für eine gefahrene Strecke (reine Funktion): {@code energy_per_block} je Block. */
    public static double energyFor(double blocks, double perBlock) {
        return blocks * perBlock;
    }

    public long capacity() {
        return (long) num(rules(), "energy_capacity");
    }

    public long energy() {
        return entityData.get(ENERGY);
    }

    public void setEnergy(long value) {
        entityData.set(ENERGY, Math.max(0, Math.min(capacity(), value)));
    }

    public List<Identifier> modules() {
        String s = entityData.get(MODULES);
        List<Identifier> out = new ArrayList<>();
        if (!s.isEmpty()) {
            for (String part : s.split(",")) {
                out.add(Identifier.parse(part));
            }
        }
        return out;
    }

    public boolean has(Identifier module) {
        return modules().contains(module);
    }

    public SimpleContainer cargo() {
        return cargo;
    }

    public int oxygen() {
        return oxygen;
    }

    public boolean stopped() {
        return entityData.get(STOPPED);
    }

    public @Nullable UUID owner() {
        return owner;
    }

    /** Druckkabine aktiv (16): Modul eingebaut und Sauerstoff im Tank. */
    public boolean cabinActive() {
        return has(CABIN) && oxygen > 0;
    }

    /** Innentemperatur der Druckkabine. */
    public double cabinTemperature() {
        return num(module(CABIN), "temperature");
    }

    private void syncModules() {
        entityData.set(MODULES, String.join(",", modules.stream().map(Identifier::toString).toList()));
        int slots = (int) num(rules(), "cargo_slots") + (modules.contains(TRAILER) ? (int) num(module(TRAILER), "extra_cargo_slots") : 0);
        if (cargo.getContainerSize() != slots) {
            SimpleContainer next = new SimpleContainer(slots);
            List<ItemStack> overflow = new ArrayList<>();
            for (int i = 0; i < cargo.getContainerSize(); i++) {
                if (i < slots) {
                    next.setItem(i, cargo.getItem(i));
                } else if (!cargo.getItem(i).isEmpty()) {
                    overflow.add(cargo.getItem(i));
                }
            }
            cargo = next;
            if (level() instanceof ServerLevel sl) {
                overflow.forEach(s -> sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 1, getZ(), s)));
            }
        }
    }

    // ---------------------------------------------------------------- Bausatz

    /** Aus dem {@code rover_kit} (Komponente {@code rover_data}). */
    public void load(RoverData data, @Nullable UUID owner) {
        this.owner = owner;
        modules.clear();
        modules.addAll(data.modules());
        syncModules();
        for (int i = 0; i < data.cargo().size() && i < cargo.getContainerSize(); i++) {
            cargo.setItem(i, data.cargo().get(i).copy());
        }
        oxygen = data.oxygen();
        setEnergy(data.energy());
    }

    public RoverData data() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            items.add(cargo.getItem(i).copy());
        }
        return new RoverData(energy(), oxygen, List.copyOf(modules), items);
    }

    /** Abbau (16): {@code rover_kit} mit allen Daten fällt an der Position des Rovers. */
    public ItemStack dismantle() {
        ItemStack kit = new ItemStack(ModItems.ROVER_KIT.get());
        kit.set(ModDataComponents.ROVER_DATA.get(), data());
        ejectPassengers();
        if (level() instanceof ServerLevel sl) {
            sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), kit.copy()));
        }
        discard();
        return kit;
    }

    // ---------------------------------------------------------------- Entity

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ENERGY, 0L);
        builder.define(STOPPED, false);
        builder.define(MODULES, "");
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float maxUpStep() {
        return (float) num(rules(), "step_height");
    }

    /** Schwerkraft des Planeten (12.9: gilt auch für Rover). */
    @Override
    protected double getDefaultGravity() {
        PlanetRegistry reg = level().isClientSide() ? PlanetRegistry.client() : PlanetRegistry.get();
        Planet p = PlanetResolver.planetFor(level().dimension().identifier(), reg);
        return BASE_GRAVITY * (p == null ? 1.0 : p.gravity());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < (int) num(rules(), "seats");
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int index = getPassengers().indexOf(passenger);
        double side = dimensions.width() / 4 * (index == 0 ? -1 : 1);
        return new Vec3(side, dimensions.height() / 2, 0).yRot(-getYRot() * ((float) Math.PI / 180F));
    }

    @Override
    public void tick() {
        super.tick();
        if (isLocalInstanceAuthoritative()) {
            drive();
        }
        if (level() instanceof ServerLevel sl) {
            serverTick(sl);
        }
    }

    /** Fahren wie ein Pferd: Blickrichtung des Fahrers, vor/zurück mit seiner Eingabe; ohne Energie nur Schwerkraft. */
    private void drive() {
        LivingEntity driver = getControllingPassenger();
        double forward = 0;
        if (driver != null && energy() > 0 && !stopped()) {
            setYRot(driver.getYRot());
            forward = driver.zza;
        }
        Vec3 dir = Vec3.directionFromRotation(0, getYRot()).scale(forward * num(rules(), "speed"));
        double vy = onGround() ? 0 : getDeltaMovement().y - getGravity();
        setDeltaMovement(dir.x, vy, dir.z);
        move(MoverType.SELF, getDeltaMovement());
    }

    private void serverTick(ServerLevel level) {
        JsonObject r = rules();
        Vec3 now = position();
        double moved = lastPos == null ? 0 : Math.sqrt(Math.pow(now.x - lastPos.x, 2) + Math.pow(now.z - lastPos.z, 2));
        lastPos = now;
        // Verbrauch je gefahrenem Block
        energyDebt += energyFor(moved, num(r, "energy_per_block"));
        if (energyDebt >= 1) {
            long spend = (long) energyDebt;
            setEnergy(energy() - spend);
            energyDebt -= spend;
        }
        // Solardach bei sichtbarem Himmel
        if (level.canSeeSky(blockPosition().above((int) Math.ceil(getBbHeight())))) {
            setEnergy(energy() + (long) num(r, "solar_roof_per_tick"));
        }
        charge(level, r);
        venusHeat(level, r);
        if (has(CABIN)) {
            breathe(level);
        }
        if (has(DRILL) && moved > 0) {
            drill(level, false);
        }
        if (has(HEADLIGHT)) {
            headlight(level);
        } else if (light != null) {
            clearLight(level);
        }
        if (has(SCANNER) && tickCount % Math.max(1, (int) num(module(SCANNER), "interval_ticks")) == 0) {
            RoverScanner.scan(level, this);
        }
    }

    /** Laden neben {@code battery}/{@code lithium_battery_bank} mit {@code charge_rate} J/t. */
    private void charge(ServerLevel level, JsonObject r) {
        long room = capacity() - energy();
        if (room <= 0 || !r.has("charge_from")) {
            return;
        }
        List<String> from = new ArrayList<>();
        r.getAsJsonArray("charge_from").forEach(e -> from.add(e.getAsString()));
        AABB box = getBoundingBox().inflate(1);
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!from.contains(BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).toString())
                    || !(level.getBlockEntity(p) instanceof MachineBlockEntity be)) {
                continue;
            }
            long take = Math.min(Math.min((long) num(r, "charge_rate"), room), be.energyStorage().getAmount());
            if (take > 0) {
                be.energyStorage().setAmountUnchecked(be.energyStorage().getAmount() - take);
                be.setChanged();
                setEnergy(energy() + take);
            }
            return;
        }
    }

    /** Venus-Oberfläche (16): nach der Hitzezeit bleibt der Rover stehen, bis er über die Grenze gebracht wird. */
    private void venusHeat(ServerLevel level, JsonObject r) {
        VenusHeat heat = VenusHeat.rules();
        PlanetResolver.Resolved res = PlanetResolver.at(level, blockPosition());
        Identifier planet = res.planet() == null ? null : res.planet().dimension();
        if (heat != null && heat.dimension().equals(planet) && getBlockY() < num(r, "venus_heat_timer_reset_above_y")) {
            heatTicks++;
        } else {
            heatTicks = 0;
        }
        entityData.set(STOPPED, heatTicks >= num(r, "venus_heat_timer_ticks"));
    }

    /** Druckkabine: Verbrauch je Insasse wie Anzug T1 ({@code oxygen_per_occupant}). */
    private void breathe(ServerLevel level) {
        JsonObject c = module(CABIN);
        JsonObject per = c.has("oxygen_per_occupant") ? c.getAsJsonObject("oxygen_per_occupant") : new JsonObject();
        long occupants = getPassengers().stream().filter(e -> e instanceof Player).count();
        if (occupants == 0 || oxygen <= 0) {
            return;
        }
        oxygenMilli += Math.round(1000.0 * num(per, "amount") / Math.max(1, num(per, "per_ticks")) * occupants);
        int whole = (int) (oxygenMilli / 1000);
        if (whole > 0) {
            oxygen = Math.max(0, oxygen - whole);
            oxygenMilli -= whole * 1000L;
        }
    }

    /** Bohrer (16): baut im Abstand {@code ticks_per_block} einen Block der 3×3-Fläche vor dem Rover ab, Drops in die Fracht. */
    public boolean drill(ServerLevel level, boolean force) {
        JsonObject d = module(DRILL);
        if (!force && --drillCooldown > 0) {
            return false;
        }
        drillCooldown = (int) num(d, "ticks_per_block");
        JsonObject area = d.has("area") ? d.getAsJsonObject("area") : new JsonObject();
        int w = (int) num(area, "width");
        int h = (int) num(area, "height");
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        Vec3 right = new Vec3(-look.z, 0, look.x);
        Vec3 front = position().add(look.scale(getBbWidth() / 2 + 0.5));
        for (int dy = 0; dy < h; dy++) {
            for (int side = -(w / 2); side <= w / 2; side++) {
                BlockPos p = BlockPos.containing(front.add(right.scale(side)).add(0, dy, 0));
                BlockState s = level.getBlockState(p);
                if (s.isAir() || !level.getFluidState(p).isEmpty() || s.getDestroySpeed(level, p) < 0) {
                    continue;
                }
                List<ItemStack> drops = Block.getDrops(s, level, p, level.getBlockEntity(p), null, new ItemStack(Items.IRON_PICKAXE));
                level.destroyBlock(p, false, this);
                for (ItemStack drop : drops) {
                    ItemStack rest = cargo.addItem(drop);
                    if (!rest.isEmpty()) {
                        level.addFreshEntity(new ItemEntity(level, getX(), getY() + 1, getZ(), rest));
                    }
                }
                return true;
            }
        }
        return false;
    }

    /** Scheinwerfer (16): Lichtblock vor dem Rover wie {@code helmet_lamp}. */
    private void headlight(ServerLevel level) {
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        BlockPos p = BlockPos.containing(position().add(look.scale(getBbWidth() / 2 + 1)).add(0, 1, 0));
        if (p.equals(light) && level.getBlockState(p).is(Blocks.LIGHT)) {
            return;
        }
        clearLight(level);
        if (level.getBlockState(p).isAir()) {
            level.setBlock(p, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,
                    (int) num(module(HEADLIGHT), "light_level")), Block.UPDATE_ALL);
            light = p.immutable();
        }
    }

    private void clearLight(ServerLevel level) {
        if (light != null && level.getBlockState(light).is(Blocks.LIGHT)
                && !level.getBlockState(light).getValue(BlockStateProperties.WATERLOGGED)) {
            level.setBlock(light, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        light = null;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            clearLight(sl);
        }
        super.remove(reason);
    }

    // ---------------------------------------------------------------- Bedienung

    /**
     * Rechtsklick (16): Schleichen mit leerer Hand baut ab (Recht {@code BREAK}), Schleichen mit Item öffnet den
     * Frachtraum, ein Modul-Item wird eingebaut, ein Sauerstoffbehälter füllt die Druckkabine, sonst einsteigen.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!(player instanceof ServerPlayer sp) || !(level() instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (held.isEmpty()) {
                if (!Permissions.can(sp, owner, Permissions.Action.BREAK)) {
                    sp.sendOverlayMessage(Component.translatable("message.infinite_horizon.agency.protected"));
                    return InteractionResult.FAIL;
                }
                dismantle();
            } else {
                openCustomInventoryScreen(player);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(held.getItem());
        if (!held.isEmpty() && module(id).size() > 0 && rules().has("module_slots")) {
            if (modules.size() < (int) num(rules(), "module_slots") && !modules.contains(id)) {
                modules.add(id);
                syncModules();
                held.consume(1, player);
                sp.sendOverlayMessage(Component.translatable("message.infinite_horizon.rover.module", held.getHoverName()));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        if (has(CABIN) && ComponentContainers.spec(held) != null) {
            refillCabin(held);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!player.isPassenger() && canAddPassenger(player)) {
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Druckkabine aus einem Behälter mit Sauerstoff füllen (bis {@code o2_tank}). */
    private void refillCabin(ItemStack container) {
        IhFluidStack content = ComponentContainers.content(container);
        if (content.isEmpty() || !content.fluid().getPath().equals("oxygen")) {
            return;
        }
        int room = (int) num(module(CABIN), "o2_tank") - oxygen;
        int take = (int) Math.min(room, content.amount());
        if (take <= 0) {
            return;
        }
        oxygen += take;
        long left = content.amount() - take;
        if (left <= 0) {
            container.remove(ModDataComponents.FLUID.get());
        } else {
            container.set(ModDataComponents.FLUID.get(), IhFluidStack.of(content.fluid(), left));
        }
    }

    /** Frachtraum (Inventartaste im Sitz oder Schleichen mit Item). */
    @Override
    public void openCustomInventoryScreen(Player player) {
        int rows = cargo.getContainerSize() / 9;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> rows > 3 ? ChestMenu.sixRows(id, inv, cargo)
                : ChestMenu.threeRows(id, inv, cargo), Component.translatable("entity.infinite_horizon.rover")));
    }

    // ---------------------------------------------------------------- Speichern

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putLong("energy", energy());
        out.putInt("oxygen", oxygen);
        out.store("modules", Identifier.CODEC.listOf(), List.copyOf(modules));
        NonNullList<ItemStack> items = NonNullList.withSize(cargo.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            items.set(i, cargo.getItem(i));
        }
        ContainerHelper.saveAllItems(out.child("cargo"), items);
        out.storeNullable("owner_agency", UUIDUtil.CODEC, owner);
        out.putInt("heat_ticks", heatTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        modules.clear();
        modules.addAll(in.read("modules", Identifier.CODEC.listOf()).orElse(List.of()));
        cargo = new SimpleContainer(0);
        syncModules();
        NonNullList<ItemStack> items = NonNullList.withSize(cargo.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in.childOrEmpty("cargo"), items);
        for (int i = 0; i < items.size(); i++) {
            cargo.setItem(i, items.get(i));
        }
        oxygen = in.getIntOr("oxygen", 0);
        owner = in.read("owner_agency", UUIDUtil.CODEC).orElse(null);
        heatTicks = in.getIntOr("heat_ticks", 0);
        entityData.set(ENERGY, in.getLongOr("energy", 0));
    }

    /** Besitzer beim Aufstellen: Agentur des Spielers. */
    void claim(ServerPlayer player) {
        owner = AgencyManager.ensureFor(player).id();
    }
}
