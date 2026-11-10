# Fabric-API-Spickzettel für Minecraft 26.3

Stand: 2026-11-10. Ziel: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.166.0+26.3, Loom 1.18.1 (`net.fabricmc.fabric-loom`, ohne Remapping), Java 25, Mojang-Namen.

**Geprüfte Quellen** (alles unten stammt aus diesem Code, sonst steht `UNVERIFIZIERT` dran):

| Kürzel | Repository / Stand |
|---|---|
| `FAPI` | github.com/FabricMC/fabric, Branch `26.3`, Commit `324b6d3` (`gradle.properties`: `version=0.166.0`, `minecraft_version=26.3`) |
| `LOOM` | github.com/FabricMC/fabric-loom, Tag `v1.18.1` |
| `TRE` | github.com/TechReborn/Energy, `master` @ `fdc4144` (= Version 5.0.0) |
| `NEO` | github.com/neoforged/NeoForge, Branch `26.3.x` @ `085df70` (nur als Beleg für Vanilla-Sichtbarkeiten) |

Maven geprüft: `net.fabricmc.fabric-api:fabric-api:0.166.0+26.3`, `net.fabricmc:fabric-loader:0.19.5` und `teamreborn:energy:5.0.0` liegen auf maven.fabricmc.net.

---

## 0. Was sich gegenüber 1.21.x geändert hat (Überblick)

| Alt (1.21.x / Yarn-Gewohnheit) | Neu in 26.3 | Beleg |
|---|---|---|
| `ResourceLocation` | `net.minecraft.resources.Identifier` (`fromNamespaceAndPath`, `withDefaultNamespace`, `parse`) | überall in FAPI |
| `ResourceKey#location()` | `ResourceKey#identifier()` | `FAPI fabric-menu-api-v1/src/testmod/.../MenuTest.java` |
| `fabric-item-group-api-v1`, `FabricItemGroup` | `fabric-creative-tab-api-v1`, `FabricCreativeModeTab` | `FAPI settings.gradle` |
| `fabric-screen-handler-api-v1`, `ExtendedScreenHandlerType` | `fabric-menu-api-v1`, `ExtendedMenuType` / `ExtendedMenuProvider` | `FAPI fabric-menu-api-v1` |
| `PayloadTypeRegistry.playS2C()/playC2S()` | `clientboundPlay()` / `serverboundPlay()` | `FAPI .../networking/v1/PayloadTypeRegistry.java` |
| `ResourceManagerHelper`, `IdentifiableResourceReloadListener` | `ResourceLoader` / `DataResourceLoader` (`fabric-resource-loader-v1`); v0 liegt unter `deprecated/` | `FAPI fabric-resource-loader-v1` |
| `FuelRegistry` / `FuelRegistryEvents` | **entfernt**; Brennwert ist Item-Komponente `DataComponents.COOKING_FUEL` | Abschnitt 9 |
| `modImplementation` | `implementation` (Loom ohne Remap legt keine `mod*`-Konfigurationen an) | `LOOM configuration/LoomConfigurations.java` |
| `Screen#render(GuiGraphics…)`, `renderBg` | `extractRenderState(GuiGraphicsExtractor, int, int, float)`, `extractBackground(...)`, `extractTooltip(...)` | `FAPI fabric-menu-api-v1/src/testmodClient/.../PositionedScreen.java` |
| `ClickType` in `AbstractContainerMenu#clicked` | `clicked(int slotId, int clickData, ContainerInput input, Player player)` | `FAPI .../test/menu/menu/BagMenu.java` |
| NBT `saveAdditional(CompoundTag, …)` | `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)` | `FAPI .../test/transfer/ingame/FluidChuteBlockEntity.java` |

Weitere belegte Vanilla-Signaturen: `Block#useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)` → `InteractionResult`; `Block#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)` → `InteractionResult`; `EntityBlock#getTicker(Level, BlockState, BlockEntityType<T>)`; `BaseEntityBlock`-Unterklassen kompilieren **ohne** `codec()`-Override (`BoxBlock.java`). Fabric-Code nutzt Java-25-Lambdas mit `_` als unbenanntem Parameter.

---

## 1. Blöcke, Items, BlockEntityTypes, Creative-Tab

**Pflicht:** Jedes `Item.Properties` und `BlockBehaviour.Properties` braucht `.setId(ResourceKey)` **vor** dem Konstruktor-Aufruf (alle Testmods machen das; die genaue Vanilla-Fehlermeldung ohne ID ist UNVERIFIZIERT).

```java
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "machine"));
Block MACHINE = new MachineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).setId(KEY));
// oder: BlockBehaviour.Properties.of().setId(KEY)

Registry.register(BuiltInRegistries.BLOCK, KEY, MACHINE);                         // Overload mit ResourceKey
Registry.register(BuiltInRegistries.ITEM, KEY.identifier(),                       // Overload mit Identifier
        new BlockItem(MACHINE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, KEY.identifier()))));
```
Quelle: `FAPI fabric-transfer-api-v1/src/testmod/.../TransferTestInitializer.java` (`registerBlock`), `fabric-object-builder-api-v1/src/testmod/.../BlockEntityTypeBuilderTest.java`.

### BlockEntityType

Der Vanilla-Konstruktor `BlockEntityType(BlockEntitySupplier, Set<Block>)` und das Interface `BlockEntityType$BlockEntitySupplier` sind in 26.3 **nicht öffentlich**: Fabric macht sie nur für sich selbst zugänglich (`accessible`, nicht `transitive-accessible` in `FAPI fabric-object-builder-api-v1/src/main/resources/fabric-object-builder-api-v1.classtweaker`), NeoForge per AT (`NEO src/main/resources/META-INF/accesstransformer.cfg`, Zeilen 263–264). Auf Fabric deshalb den Builder nehmen – er existiert weiter:

```java
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public static final BlockEntityType<MachineBlockEntity> MACHINE_BE =
        FabricBlockEntityTypeBuilder.create(MachineBlockEntity::new, MACHINE).build();   // build() ohne Type-Argument
Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "machine"), MACHINE_BE);
```
Signaturen (`FAPI .../object/builder/v1/block/entity/FabricBlockEntityTypeBuilder.java`): `static <T extends BlockEntity> FabricBlockEntityTypeBuilder<T> create(Factory<? extends T>, Block...)`, `addBlock(Block)`, `addBlocks(Block...)`, `addBlocks(Collection)`, `canPotentiallyExecuteCommands(boolean)`, `BlockEntityType<T> build()`; `build(Type<?>)` ist `@Deprecated`. `Factory#create(BlockPos, BlockState)`. Zusätzlich injiziert Fabric `FabricBlockEntityType#addValidBlock(Block)` in jeden `BlockEntityType`.

> **MultiLoader-Hinweis:** `new BlockEntityType<>(…)` in `common` kompiliert nur mit eigenem Access Widener. Empfehlung: BE-Typen über eine Plattform-Schnittstelle je Loader erzeugen (Fabric: Builder oben; NeoForge: Konstruktor ist dort per AT public).

### Creative-Tab

```java
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "main"));
Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()   // liefert CreativeModeTab.Builder
        .title(Component.translatable("itemGroup.sternenwerft"))                              // Pflicht, sonst IllegalStateException
        .icon(() -> new ItemStack(MACHINE))
        .displayItems((params, output) -> output.accept(MACHINE))
        .build());

// In vorhandene Tabs einfügen:
CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
        .register(output -> output.insertAfter(Blocks.OAK_DOOR, MACHINE));
```
Quelle: `FAPI fabric-creative-tab-api-v1/src/testmod/.../CreativeModeTabTest.java`, `.../api/creativetab/v1/FabricCreativeModeTabOutput.java` (`accept`, `prepend`, `insertBefore`, `insertAfter`).

---

## 2. Transfer API (Items/Fluide)

Paket-Wurzel `net.fabricmc.fabric.api.transfer.v1` (Modul `fabric-transfer-api-v1` 8.0.27):

| Klasse | Wichtig |
|---|---|
| `item.ItemStorage` | `BlockApiLookup<Storage<ItemVariant>, @Nullable Direction> SIDED`, `ItemApiLookup<…, ContainerItemContext> ITEM` |
| `fluid.FluidStorage` | `SIDED`, `ITEM` analog mit `FluidVariant` |
| `transaction.Transaction` | `static Transaction openOuter()`, `openNested(@Nullable TransactionContext)`, `commit()`, `abort()`, `close()` (AutoCloseable) |
| `storage.base.SingleVariantStorage<T>` | Felder `public T variant`, `public long amount`; abstrakt `getBlankVariant()`, `getCapacity(T)`; optional `canInsert/canExtract`; `onFinalCommit()` (aus `SnapshotParticipant`) |
| `fluid.base.SingleFluidStorage` | `static withFixedCapacity(long capacity, Runnable onChange)`, `readValue(ValueInput)`, `writeValue(ValueOutput)` |
| `item.ContainerStorage` | `static ContainerStorage of(Container, @Nullable Direction)` (ehem. `InventoryStorage`) |
| `storage.StorageUtil` | `move(from, to, Predicate<T>, long maxAmount, @Nullable TransactionContext)`, `simulateInsert/simulateExtract`, `findStoredResource` |
| `storage.base.SidedStorageBlockEntity` | optional auf BE: `getItemStorage(@Nullable Direction)`, `getFluidStorage(@Nullable Direction)` – Fabric registriert Fallback-Provider automatisch |
| `fluid.FluidVariant` / `item.ItemVariant` | `of(Fluid)` / `of(ItemLike)`, `of(ItemStack)`, `blank()`, `CODEC`, `PACKET_CODEC` |

**Einheiten** (`fluid/FluidConstants.java`): `BUCKET = 81000`, `BOTTLE = 27000`, `INGOT = 9000`, `NUGGET = 1000`, `DROPLET = 1` → **1 mB = 81 Droplets** (`FluidConstants.BUCKET / 1000`). Helfer: `fromBucketFraction(long num, long denom)`.

```java
public class TankBlockEntity extends BlockEntity {
    final SingleFluidStorage tank = SingleFluidStorage.withFixedCapacity(FluidConstants.BUCKET * 4, this::setChanged);

    public TankBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.TANK, pos, state); }

    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); tank.writeValue(out); }
    @Override public void loadAdditional(ValueInput in) { super.loadAdditional(in); tank.readValue(in); }
}

// Registrierung (ModInitializer): BiFunction<? super T, @Nullable Direction, Storage<…>>
FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.tank, ModBlockEntities.TANK);
ItemStorage.SIDED.registerForBlockEntity((be, dir) -> ContainerStorage.of(be, dir), ModBlockEntities.CHEST_LIKE);

// Nutzung
Storage<FluidVariant> target = FluidStorage.SIDED.find(level, pos.relative(Direction.DOWN), Direction.UP);
try (Transaction tx = Transaction.openOuter()) {
    long moved = target == null ? 0 : target.insert(FluidVariant.of(Fluids.WATER), 81 * 100, tx);   // 100 mB
    if (moved > 0) tx.commit();                                                                     // ohne commit → Rollback
}
```
Quelle: `FAPI fabric-transfer-api-v1/src/testmod/.../ingame/FluidChuteBlockEntity.java`, `TransferTestInitializer.java`, `fabric-api-lookup-api-v1/.../block/BlockApiLookup.java` (`registerForBlockEntity`, `registerForBlockEntities`, `registerForBlocks`, `registerSelf`, `registerFallback`, `find(Level, BlockPos, C)`).

**Mehrere Tanks in einem BE:** `SingleFluidStorage#writeValue` schreibt fest die Schlüssel `"variant"` und `"amount"` – zwei Tanks würden kollidieren. Sicher belegt sind `ValueOutput#store(String, Codec, T)`, `ValueOutput#putLong(String, long)`, `ValueInput#read(String, Codec)` → `Optional`, `ValueInput#getLongOr(String, long)` (aus `SingleVariantStorage.readValue/writeValue`). Damit eigene Schlüssel schreiben, z. B. `out.store("in_variant", FluidVariant.CODEC, tankIn.variant); out.putLong("in_amount", tankIn.amount);`. `ValueOutput#child(String)` ist UNVERIFIZIERT.

---

## 3. Team Reborn Energy

- **Maven:** `teamreborn:energy:5.0.0` (neueste Version laut `maven.fabricmc.net/teamreborn/energy/maven-metadata.xml`, veröffentlicht 2026-01-01). Repository: `https://maven.fabricmc.net/` – Loom fügt es selbst als Repository „Fabric“ hinzu (`LOOM LoomRepositoryPlugin.java`, `Constants.FABRIC_REPOSITORY`).
- **Mod-ID:** `team_reborn_energy`. `fabric.mod.json` von TRE: `"minecraft": ">=26.1-"`, `"java": ">=25"`, `"fabric-transfer-api-v1": ">=5.1.0"` → lädt formal auf 26.3.
- Gebaut gegen `26.1-snapshot-1` / Fabric API `0.140.3+26.1`, POM verlangt `fabric-transfer-api-v1:7.0.0+…` (Gradle löst auf die höhere 26.3-Version auf). **Laufzeit-Kompatibilität mit 26.3: UNVERIFIZIERT** (die von TRE genutzten APIs – `BlockApiLookup.get`, `ItemApiLookup.get`, `ContainerItemContext`, `SnapshotParticipant`, `DataComponentType.builder().persistent().networkSynchronized()` – existieren in 26.3 mit passenden Signaturen; nicht im Spiel getestet).

**Gradle (`fabric/build.gradle`), Loom 1.18 ohne Remap:**
```groovy
repositories {
    // Optional explizit; Loom fügt maven.fabricmc.net ohnehin hinzu.
    maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
}
dependencies {
    implementation "teamreborn:energy:5.0.0"   // KEIN modImplementation – existiert ohne Remap nicht
    include "teamreborn:energy:5.0.0"          // Jar-in-Jar
}
```
Belege: Mit Plugin `net.fabricmc.fabric-loom` ist `disableObfuscation` fest `true` (`LOOM extension/LoomGradleExtensionImpl.java`), dann wird `createRemapConfigurations` nicht aufgerufen (`LOOM configuration/LoomConfigurations.java`). `include` wird nicht-transitiv verschachtelt (`LOOM configuration/IncludeConfigurations.java`, `copy.setTransitive(false)`), d. h. nur das Energy-Jar landet im Mod-Jar (Fabric API bringt der Spieler selbst mit). Empfehlung: in unserer `fabric.mod.json` `"depends": { "team_reborn_energy": ">=5.0.0" }`.

**API** (`TRE src/main/java/team/reborn/energy/api/`):
```java
import team.reborn.energy.api.EnergyStorage;            // SIDED: BlockApiLookup<EnergyStorage, @Nullable Direction> (id teamreborn:sided_energy)
import team.reborn.energy.api.EnergyStorageUtil;        // static long move(@Nullable EnergyStorage from, @Nullable EnergyStorage to, long max, @Nullable TransactionContext tx)
import team.reborn.energy.api.base.SimpleEnergyStorage; // (long capacity, long maxInsert, long maxExtract); public long amount

public final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, MAX_IN, MAX_OUT) {
    @Override protected void onFinalCommit() { setChanged(); }
};
// speichern/laden
out.putLong("energy", energy.amount);
energy.amount = in.getLongOr("energy", 0L);

// Registrierung
EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be.energy, ModBlockEntities.GENERATOR);

// Abfrage + Übertragung
EnergyStorage neighbour = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
EnergyStorageUtil.move(energy, neighbour, Long.MAX_VALUE, null);
```
Weitere: `EnergyStorage#insert(long, TransactionContext)`, `extract(long, TransactionContext)`, `getAmount()`, `getCapacity()`, `supportsInsertion()`, `supportsExtraction()`, `EnergyStorage.ITEM` (Item-Lookup), `EnergyStorage.EMPTY`; Basisklassen `SimpleSidedEnergyContainer` (`getSideStorage(@Nullable Direction)`), `SimpleEnergyItem`, `LimitingEnergyStorage`, `InfiniteEnergyStorage`, `DelegatingEnergyStorage`.

---

## 4. Menüs und Screens

Modul `fabric-menu-api-v1`, Paket `net.fabricmc.fabric.api.menu.v1`:
- `ExtendedMenuType<T extends AbstractContainerMenu, D>(ExtendedFactory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> codec)`; `ExtendedFactory#create(int containerId, Inventory inv, D data)`.
- `ExtendedMenuProvider<D> extends MenuProvider` mit `D getScreenOpeningData(ServerPlayer)`.
- Öffnen: `player.openMenu(provider)` (normales Vanilla-`Player#openMenu`).
- Ohne Zusatzdaten: `new MenuType<>(MyMenu::new, FeatureFlags.VANILLA_SET)` – Konstruktor ist durch `fabric-transitive-access-wideners-v1` für Mods zugänglich (`transitive-accessible method net/minecraft/world/inventory/MenuType <init>`).

```java
public static final MenuType<MachineMenu> MACHINE_MENU =
        new ExtendedMenuType<>(MachineMenu::new, BlockPos.STREAM_CODEC.cast());   // Client-Ctor: (int, Inventory, BlockPos)
Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(MOD_ID, "machine"), MACHINE_MENU);

// BlockEntity
public class MachineBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {
    @Override public BlockPos getScreenOpeningData(ServerPlayer player) { return worldPosition; }
    @Override public Component getDisplayName() { return Component.translatable("container.sternenwerft.machine"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new MachineMenu(id, inv, this); }
}
// Block (extends BaseEntityBlock) #useWithoutItem:
//   if (!level.isClientSide()) { MenuProvider m = state.getMenuProvider(level, pos); if (m != null) player.openMenu(m); }
//   return InteractionResult.SUCCESS;   (so in FAPI .../test/menu/block/BoxBlock.java)
```
Für zusammengesetzte Daten: `StreamCodec.composite(ByteBufCodecs.VAR_INT, Data::a, Data::new)` (Beispiel im Javadoc von `ExtendedMenuType.java`; `composite` mit 2 Feldern in `fabric-transfer-api-v1/.../impl/transfer/VariantCodecs.java`).

**Client** (`ClientModInitializer`, Entry-Point `"client"`):
```java
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class SternenwerftFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        MenuScreens.register(ModMenus.MACHINE_MENU, MachineScreen::new);   // Ctor (M menu, Inventory, Component)
    }
}
```
`MenuScreens.register` und `MenuScreens$ScreenConstructor` sind über `fabric-transitive-access-wideners-v1` zugänglich (`.../fabric-transitive-access-wideners-v1.classtweaker`, Zeilen 11–12). Es gibt **kein** `HandledScreens` mehr.

Screen-Gerüst 26.3 (`FAPI fabric-menu-api-v1/src/testmodClient/.../PositionedScreen.java`):
```java
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/machine.png");
    public MachineScreen(MachineMenu menu, Inventory inv, Component title) { super(menu, inv, title); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        extractBackground(g, mouseX, mouseY, delta);
        super.extractRenderState(g, mouseX, mouseY, delta);
        extractTooltip(g, mouseX, mouseY);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = (width - imageWidth) / 2, y = (height - imageHeight) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }
}
```
Imports: `net.minecraft.client.gui.GuiGraphicsExtractor`, `net.minecraft.client.renderer.RenderPipelines`, `net.minecraft.client.gui.screens.inventory.AbstractContainerScreen`.

`fabric.mod.json`:
```json
"entrypoints": {
  "main":   ["de.sternenwerft.fabric.SternenwerftFabric"],
  "client": ["de.sternenwerft.fabric.SternenwerftFabricClient"]
}
```

---

## 5. Networking

`net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry<B>` – statische Registries: `clientboundPlay()` (S2C, `RegistryFriendlyByteBuf`), `serverboundPlay()` (C2S), `clientboundConfiguration()`, `serverboundConfiguration()` (`FriendlyByteBuf`). Methoden: `register(Type<T>, StreamCodec<? super B, T>)`, Varianten mit `Predicate<PacketContext>` und `registerLarge(…, int maxPacketSize)`.

```java
public record SyncEnergyPayload(BlockPos pos, long amount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncEnergyPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MOD_ID, "sync_energy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEnergyPayload> CODEC =
            CustomPacketPayload.codec(SyncEnergyPayload::write, SyncEnergyPayload::new);

    public SyncEnergyPayload(RegistryFriendlyByteBuf buf) { this(buf.readBlockPos(), buf.readVarLong()); }   // read*/write*: UNVERIFIZIERT
    public void write(RegistryFriendlyByteBuf buf) { buf.writeBlockPos(pos); buf.writeVarLong(amount); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

// Gemeinsamer ModInitializer (beide Seiten!):
PayloadTypeRegistry.clientboundPlay().register(SyncEnergyPayload.TYPE, SyncEnergyPayload.CODEC);
PayloadTypeRegistry.serverboundPlay().register(ButtonPayload.TYPE, ButtonPayload.CODEC);

// Server empfängt (läuft auf dem Server-Thread):
ServerPlayNetworking.registerGlobalReceiver(ButtonPayload.TYPE, (payload, ctx) -> { ServerPlayer p = ctx.player(); /* … */ });
// Server sendet:
ServerPlayNetworking.send(serverPlayer, new SyncEnergyPayload(pos, amount));

// Client (ClientModInitializer), Handler läuft auf dem Render-Thread:
ClientPlayNetworking.registerGlobalReceiver(SyncEnergyPayload.TYPE, (payload, ctx) -> { /* ctx.client(), ctx.player() */ });
ClientPlayNetworking.send(new ButtonPayload(1));
```
Kontext: `ServerPlayNetworking.Context` → `server()`, `player()`, `responseSender()`; `ClientPlayNetworking.Context` → `client()`, `player()` (`LocalPlayer`), `responseSender()`. Weitere: `ServerPlayNetworking.canSend(ServerPlayer, Type)`, `PlayerLookup` (Spieler im Umkreis/tracking). Alternativer Codec ohne Lese-/Schreibmethoden: `StreamCodec.composite(...)` oder `ByteBufCodecs.STRING_UTF8.map(Rec::new, Rec::data).cast()` (`NetworkingPlayPacketTest.java`).
Quelle: `FAPI fabric-networking-api-v1/src/main/.../PayloadTypeRegistry.java`, `ServerPlayNetworking.java`, `src/client/.../ClientPlayNetworking.java`, `src/testmod/.../play/NetworkingPlayPacketTest.java`, `common/NetworkingCommonTest.java`.

---

## 6. Reload-Listener für Serverdaten (`data/…`)

Modul `fabric-resource-loader-v1`, Paket `net.fabricmc.fabric.api.resource.v1`:
- `DataResourceLoader.get()` – Serverdaten. `registerReloadListener(Identifier id, PreparableReloadListener listener)` **oder** `registerReloadListener(Identifier id, Function<HolderLookup.Provider, PreparableReloadListener> factory)` (bekommt die Registries, z. B. für `SimpleJsonResourceReloadListener`).
- `ResourceLoader.get(PackType.CLIENT_RESOURCES)` – Client-Assets.
- `addListenerOrdering(Identifier first, Identifier second)`; Vanilla-IDs in `reloader.ResourceReloaderKeys` (`BEFORE_VANILLA`, `AFTER_VANILLA`, `Server.*`, `Client.*`).
- Shared-State-Schlüssel: `ResourceLoader.REGISTRY_LOOKUP_KEY`, `ResourceLoader.FEATURE_FLAG_SET_KEY`, `DataResourceLoader.DATA_RESOURCE_STORE_KEY`.
- Bequeme Basisklasse: `reloader.SimpleReloadListener<T>` mit `protected abstract T prepare(SharedState)` (beliebiger Thread) und `protected abstract void apply(T, SharedState)` (Game-Thread). Den `ResourceManager` liefert `state.resourceManager()` – UNVERIFIZIERT (nicht im Fabric-Code gesehen).
- Kein `IdentifiableResourceReloadListener` mehr nötig: die ID wird bei der Registrierung übergeben.

```java
DataResourceLoader.get().registerReloadListener(
        Identifier.fromNamespaceAndPath(MOD_ID, "balance"),
        (ResourceManagerReloadListener) manager -> BalanceManager.reload(manager));   // synchron, einfachste Variante
```
Quelle: `FAPI fabric-resource-loader-v1/src/testmod/.../ResourceReloaderTestMod.java`, `src/main/.../DataResourceLoader.java`, `ResourceLoader.java`, `reloader/SimpleReloadListener.java`. Der alte `ResourceManagerHelper` (v0) liegt noch unter `deprecated/fabric-resource-loader-v0` und ist `@Deprecated` – nicht mehr verwenden.

---

## 7. GameTests

Modul `fabric-gametest-api-v1` (in `fabric-api` enthalten).

- Annotation: `net.fabricmc.fabric.api.gametest.v1.GameTest` (im Paket-Javadoc steht noch veraltet `@FabricGameTest` – ignorieren). Attribute: `environment` (Default `"minecraft:default"`), `dimension`, `structure` (Default `"fabric-gametest-api-v1:empty"` = leere 8×8-Struktur), `maxTicks` (20), `setupTicks`, `required`, `rotation`, `manualOnly`, `maxAttempts`, `requiredSuccesses`, `skyAccess`, `padding`.
- Testmethoden: `public`, **nicht** `static`, `void`, genau ein Parameter `net.minecraft.gametest.framework.GameTestHelper`. Testklasse braucht einen öffentlichen No-Arg-Konstruktor (wird als Entry-Point instanziert).
- Registrierung über Entry-Point **`"fabric-gametest"`** in `fabric.mod.json`. Test-ID = `<modid>:<snake_case(Klassenname + "_" + Methodenname)>` (`impl/gametest/TestAnnotationLocator.java`).
- Tests werden nur geladen, wenn `-Dfabric-api.gametest` gesetzt ist **oder** Entwicklungsumgebung (`FabricGameTestModInitializer.java`) – kein Ballast im Release.
- Eigene Strukturen: `src/main/resources/data/<modid>/gametest/structure/<name>.snbt`, Referenz `structure = "<modid>:<name>"` (nur `.snbt`, siehe `FabricGameTestRunner.GAMETEST_STRUCTURE_FINDER`).
- Optional `CustomTestMethodInvoker` (Testklasse implementiert `invokeTestMethod(GameTestHelper, Method)`) für Methoden mit Zusatzparametern.

```java
public class MachineGameTests {
    @GameTest(maxTicks = 110)
    public void tankAcceptsWater(GameTestHelper helper) {
        BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, ModBlocks.TANK.defaultBlockState());
        TankBlockEntity be = helper.getBlockEntity(pos, TankBlockEntity.class);
        helper.assertTrue(be != null, "Tank fehlt");
        helper.runAfterDelay(20, () -> helper.succeed());
    }
}
```
Belegte `GameTestHelper`-Methoden (`FAPI .../test/gametest/ExampleFabricTestSuite.java`, `fabric-content-registries-v0/src/testmod/.../ContentRegistryGameTest.java`): `succeed()`, `succeedWhen(Runnable)`, `assertTrue(boolean, String)`, `assertTrue(boolean, Component)`, `assertBlock(BlockPos, Predicate<Block>, Function<Block, Component>)`, `setBlock(int, int, int, Block)`, `setBlock(BlockPos, BlockState)`, `getBlockEntity(BlockPos, Class<T>)`, `runAfterDelay(<ticks>, Runnable)` (mit int-Literal aufgerufen; genauer Parametertyp ungeprüft), `getLevel()`, `getTestRotation()`.

`fabric.mod.json`: `"entrypoints": { "fabric-gametest": ["de.sternenwerft.fabric.test.MachineGameTests"] }`

**Headless in CI – System-Properties** (`impl/gametest/GameTestSystemProperties.java`): `fabric-api.gametest` (aktiviert; EULA wird dann automatisch übersprungen, `mixin/gametest/server/MainMixin.java`), `fabric-api.gametest.report-file=<pfad>` (JUnit-XML), `fabric-api.gametest.filter=<text>`, `fabric-api.gametest.verify` (jede Drehung 100×). Startfehler beenden mit `System.exit(-1)`.

**Variante A – Loom-Helfer** (`LOOM api/fabricapi/FabricApiExtension.java`, `configuration/fabricapi/FabricApiTesting.java`, `@ApiStatus.Experimental`):
```groovy
fabricApi {
    configureTests {
        enableGameTests = true          // Run-Config "gameTest" → Task runGameTest, hängt an "check"
        enableClientGameTests = false   // sonst zusätzlich clientGameTest (braucht Grafik)
        createSourceSet = false         // Tests im main-SourceSet
    }
}
```
Erbt von der Run-Config `server`, setzt `fabric-api.gametest`, Run-Verzeichnis `build/run/gameTest`. Task-Name = `"run" + Großbuchstabe + Name` (`LOOM task/LoomTasks.java#getRunConfigTaskName`).

**Variante B – passend zu CLAUDE.md (`runGameTestServer`)**, analog zu Fabric-APIs eigenem `fabric-api.root-testing.gradle`:
```groovy
loom {
    runs {
        gameTestServer {
            server()
            setConfigName('Fabric GameTest Server')
            ideConfigGenerated(false)
            vmArg '-Dfabric-api.gametest'
            vmArg "-Dfabric-api.gametest.report-file=${project.layout.buildDirectory.file('gametest/junit.xml').get().asFile}"
            runDir('build/gametest')
        }
    }
}
```
Methoden geprüft in `LOOM configuration/ide/RunConfigSettings.java` (`server()`, `vmArg`, `runDir`, `setConfigName`, `ideConfigGenerated`, `property`, `source`, `inherit`). Ob der Gradle-Task bei fehlgeschlagenen Tests rot wird, hängt am Exit-Code von Vanillas `GameTestServer` – UNVERIFIZIERT (Fabric-CI verlässt sich darauf).

**JUnit ohne Spielstart:** `testImplementation "net.fabricmc:fabric-loader-junit:${fabric_loader_version}"` + `test { useJUnitPlatform() }` (so in `TRE build.gradle`).

---

## 8. Datagen (Kurz)

- Entry-Point `"fabric-datagen"`, Klasse implementiert `net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint#onInitializeDataGenerator(FabricDataGenerator gen)`; `FabricDataGenerator.Pack pack = gen.createPack(); pack.addProvider(MyRecipeProvider::new);`
- Provider unter `net.fabricmc.fabric.api.datagen.v1.provider`: `FabricRecipeProvider`, `FabricTagsProvider`, `FabricLanguageProvider`, `FabricBlockLootSubProvider`, `FabricAdvancementProvider`, `FabricCodecDataProvider`, `FabricDynamicRegistryProvider`. **Modelle jetzt clientseitig:** `net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider`.
- Loom: `fabricApi { configureDataGeneration { client = true; modId = 'sternenwerft' } }` → Task `runDatagen`, Ausgabe `src/main/generated` (Default, wird den Ressourcen hinzugefügt).
- Für dieses Projekt nicht nötig: Ressourcen erzeugt `tools/gen_resources.py`.

---

## 9. Brennwert (Ofen-Brennstoff)

- `FuelRegistry`/`FuelRegistryEvents` gibt es in Fabric 26.3 **nicht mehr** (kein Treffer im ganzen Branch). `FuelValues` taucht in FAPI ebenfalls nicht auf.
- Brennwert ist die Item-Komponente `net.minecraft.core.component.DataComponents.COOKING_FUEL` vom Typ `net.minecraft.world.item.component.CookingFuel` (Konstruktor mit 2 Argumenten: Brenndauer, Geschwindigkeits-Multiplikator). Der Ofen liest `CookingFuel::burnTime` per `ResolvableInt.getFromItem(…)` (`NEO patches/.../AbstractFurnaceBlockEntity.java.patch`, Kontextzeilen = Vanilla).
- Belegte Wege:

```java
// a) Eigenes Item: Item.Properties#cookingFuel(...) mit Vanilla-Konstante
new Item(new Item.Properties().cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_ITEMS_LARGE).setId(key));
// (FAPI fabric-item-api-v1/src/testmod/.../CustomDamageTest.java)

// b) Fremdes/eigenes Item nachträglich: DefaultItemComponentEvents (fabric-item-api-v1)
DefaultItemComponentEvents.MODIFY.register(ctx -> ctx.modify(MY_ITEM, b -> b.set(DataComponents.COOKING_FUEL,
        new CookingFuel(ContextIntProviders.COOKING_TIME_BAMBOO, ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER))));
// Entfernen: b.set(DataComponents.COOKING_FUEL, null)
// (FAPI fabric-content-registries-v0/src/testmod/.../ContentRegistryTest.java)
```
Imports: `net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders`, `...number.floats.ContextFloatProviders`. Belegte Konstanten: `COOKING_TIME_BAMBOO` (= 50 Ticks laut Testkommentar), `COOKING_TIME_DRY_PLANTS`, `COOKING_TIME_WOOD_ITEMS_LARGE`, `COOKING_DEFAULT_SPEED_MULTIPLIER`. Zahlen-Provider sind jetzt eine Registry (`Registries.CONTEXT_INT_PROVIDER`, Werte z. B. `new ConstantValue(123)` aus `...number.ints.ConstantValue`).
- **UNVERIFIZIERT:** der genaue Parametertyp von `cookingFuel(...)`/`CookingFuel` (vermutlich `ResolvableInt`/Holder) und wie man einen *eigenen* festen Wert übergibt (z. B. `ContextIntProviders.exactly(n)` – existiert, liefert etwas für `LootPool#setRolls`, Kompatibilität mit `cookingFuel` ungeprüft). Erst per CI-Compiler prüfen; Notlösung: nächstpassende Vanilla-Konstante + `// SPEC-GAP`.

---

## UNVERIFIZIERT – Sammelliste

1. Laufzeit-Kompatibilität von `teamreborn:energy:5.0.0` (gebaut gegen 26.1-snapshot-1) mit 26.3.
2. Eigener Brennwert-Zahlenwert (`cookingFuel`-Parametertyp, `ContextIntProviders.exactly` als Brenndauer).
3. `ValueOutput#child(String)` für verschachtelte Speicherung.
4. `SharedState#resourceManager()` in `SimpleReloadListener`.
5. `RegistryFriendlyByteBuf#readBlockPos/writeVarLong` & Co. im Payload-Beispiel (Klasse belegt, Einzelmethoden nicht).
6. Exit-Code von Vanillas `GameTestServer` bei fehlgeschlagenen Tests (→ roter Gradle-Task).
7. Genaue Vanilla-Fehlermeldung, wenn `setId` fehlt.
