# API-Spickzettel: Fluide in Minecraft 26.3 (Mojang-Namen, Java 25)

Stand: 2026-11-11. Ergänzt `api-neoforge-26.3.md` und `api-fabric-26.3.md` (Registrierung, Capabilities-Grundlagen dort).

**Markierungen:** **[J]** = per `javap` aus `minecraft_26.3_client.jar` gelesen. **[V]** = steht so im 26.3-Quellcode von NeoForge (`NF`), Fabric API (`FAB`) oder Mekanism (`MEK`). **UNVERIFIZIERT** = in keiner Quelle gesehen.

Kürzel: `NF` = NeoForge `26.3.x` (`src/main/java/net/neoforged/neoforge/`, Client: `src/client/java/...`), `FAB` = FabricMC/fabric `26.3` (Testmods, Transfer-API), `MEK` = Mekanism `26.3`. Vanilla-Daten: `data/minecraft/worldgen/...` im Jar.

## 0. Die wichtigsten Unterschiede zu 1.21.x

1. **Features sind keine `Feature<C>` + `FeatureConfiguration` + `ConfiguredFeature` mehr.** `Feature` ist ein Interface, die Konfiguration steckt als Record-Felder im Feature selbst. Registry `FEATURE_TYPE` hält `MapCodec<? extends Feature>`. Das JSON unter `worldgen/feature/` hat `"type"` = Codec-ID und die Felder direkt daneben (kein `"config"`). [J]
2. **Fluid-Rendering ist datengetrieben über `FluidModel.Unbaked`** (Materialien + Tint). NeoForge: `RegisterFluidModelsEvent`, Fabric: `FluidRenderingRegistry.register(...)`. `IClientFluidTypeExtensions` kennt keine Texturen mehr, nur Overlay und Nebel. [V]
3. **NeoForge-Fluid-Capability** ist `ResourceHandler<FluidResource>` mit `Transaction` und `int` (mB), nicht mehr `IFluidHandler`/`FluidStack`. [V]
4. **`LiquidBlock`-Konstruktor ist `protected`.** NeoForge macht ihn per generiertem AT `public`; auf Fabric nutzt man eine anonyme Unterklasse `new LiquidBlock(...) {}`. [J][V]
5. `FlowingFluid`/`Fluid`-Hooks arbeiten mit `ServerLevel` (`canConvertToSource(ServerLevel)`, `tick(ServerLevel,...)`), `Item`/`Block`-Properties brauchen `setId(ResourceKey)`, `ResourceLocation` heißt `Identifier`, NBT über `ValueInput`/`ValueOutput`. [J][V]

---

## 1. Eigenes Weltfluid (Quelle + fließend, Block, Eimer)

### 1.1 Vanilla-Signaturen [J] (`net.minecraft.world.level.material`)

`Fluid` (abstrakt, müssen implementiert werden, wenn man nicht von `FlowingFluid` erbt):
`Item getBucket()`, `boolean canBeReplacedWith(FluidState, BlockGetter, BlockPos, Fluid, Direction)` (protected), `Vec3 getFlow(...)` (protected), `int getTickDelay(LevelReader)` (public), `float getExplosionResistance()` (protected), `float getHeight(FluidState, BlockGetter, BlockPos)`, `float getOwnHeight(FluidState)`, `BlockState createLegacyBlock(FluidState)` (protected), `boolean isSource(FluidState)`, `int getAmount(FluidState)`, `VoxelShape getShape(FluidState, BlockGetter, BlockPos)`.
Optional: `isSame(Fluid)`, `getPickupSound(): Optional<SoundEvent>`, `animateTick`, `getDripParticle`, `entityInside(Level, BlockPos, Entity, InsideBlockEffectApplier)`.

`FlowingFluid extends Fluid` übernimmt `getFlow/getHeight/getOwnHeight/getShape/tick/spread`. Zusätzlich **abstrakt**:

| Methode | Hinweis |
|---|---|
| `Fluid getFlowing()` / `Fluid getSource()` | die beiden registrierten Instanzen |
| `boolean canConvertToSource(ServerLevel)` | protected; Wasser: Gamerule, Lava: `false` |
| `void beforeDestroyingBlock(LevelAccessor, BlockPos, BlockState)` | protected; Wasser: `Block.dropResources(...)` |
| `int getSlopeFindDistance(LevelReader)` | protected abstract (Wasser 4, Lava 2/4) |
| `int getDropOff(LevelReader)` | protected abstract (Wasser 1, Lava 2/1) |
| `int getAmount(FluidState)` | Quelle: 8, fließend: `state.getValue(LEVEL)` |
| `boolean isSource(FluidState)` | Quelle `true`, fließend `false` |
| aus `Fluid` noch offen: `getBucket()`, `canBeReplacedWith`, `getTickDelay`, `getExplosionResistance`, `createLegacyBlock` | |

Die fließende Variante überschreibt `createFluidStateDefinition(StateDefinition.Builder<Fluid,FluidState>)`: `super...; builder.add(LEVEL);` (`FlowingFluid.LEVEL` / `FALLING` sind statisch). Referenz: `WaterFluid`, `WaterFluid$Source`, `WaterFluid$Flowing` (Jar), und `FAB .../fabric-rendering-fluids-v1/src/testmod/.../CustomFluid.java` (komplette Minimal-Implementierung inkl. `createLegacyBlock = BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state))`).

Block und Item [J]:
```java
protected LiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties props)       // protected!
public BucketItem(Fluid content, Item.Properties props)                          // public
// Properties: BlockBehaviour.Properties.of().setId(key).replaceable().noCollision().strength(100f)
//   .pushReaction(PushReaction.POPPED).noLootTable().liquid()  (so baut es MEK)
// Item.Properties: .setId(key).craftRemainder(Items.BUCKET).stacksTo(1)
```
`LiquidBlock` implementiert `BucketPickup`: Eimer-Aufnehmen ersetzt Quellblock durch Luft (Flag 11) und liefert `new ItemStack(fluid.getBucket())`. [J]

### 1.2 NeoForge [V]

Registry-Reihenfolge: `FluidType` (`NeoForgeRegistries.Keys.FLUID_TYPES`), Fluids (`Registries.FLUID`), Block, Item. Quelle: `NF tests/.../oldtest/fluid/FluidTypeTest.java`, `NF tests/.../debug/fluid/EntityFluidInteractionTests.java`, `MEK common/registration/impl/FluidDeferredRegister.java`.
```java
DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MODID);
DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, MODID);
DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

static BaseFlowingFluid.Properties props() {      // beide Fluids teilen sich dieselben Properties
  return new BaseFlowingFluid.Properties(OIL_TYPE, OIL, OIL_FLOWING).block(OIL_BLOCK).bucket(OIL_BUCKET)
      .slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(5).explosionResistance(100);
}
Holder<FluidType> OIL_TYPE = TYPES.register("oil", () -> new FluidType(FluidType.Properties.create()
      .density(1500).viscosity(3000).lightLevel(0).canSwim(false).supportsBoating(false)));
DeferredHolder<Fluid, FlowingFluid> OIL = FLUIDS.register("oil", () -> new BaseFlowingFluid.Source(props()));
DeferredHolder<Fluid, FlowingFluid> OIL_FLOWING = FLUIDS.register("oil_flowing", () -> new BaseFlowingFluid.Flowing(props()));
DeferredBlock<LiquidBlock> OIL_BLOCK = BLOCKS.registerBlock("oil",
      p -> new LiquidBlock(OIL.get(), p), p -> p.replaceable().noCollision().strength(100f).noLootTable().liquid());
DeferredItem<Item> OIL_BUCKET = ITEMS.registerItem("oil_bucket",
      p -> new BucketItem(OIL.get(), p.craftRemainder(Items.BUCKET).stacksTo(1)));
```
- `BaseFlowingFluid` implementiert alle abstrakten Methoden aus Properties; `getBucket()` liefert `Items.AIR` und `createLegacyBlock` `AIR`, wenn `bucket`/`block` fehlen (nullable). `BaseFlowingFluid.canBeReplacedWith` = `direction == DOWN && !isSame(...)`. [V `NF fluids/BaseFlowingFluid.java`]
- `FluidType.Properties`-Methoden (Auszug): `descriptionId, motionScale, canPushEntity, canSwim, canDrown, fallDistanceModifier, canExtinguish, canConvertToSource, supportsBoating, pathType, adjacentPathType, sound(SoundAction, SoundEvent), canHydrate, lightLevel, density, temperature, viscosity, rarity, addDripstoneDripping, isWaterLike`. [V `NF fluids/FluidType.java`]
- Anzeigename des FluidType: Default-Key `fluid_type.<modid>.<name>` (`FluidType#getDescriptionId`); MEK setzt `descriptionId(rl.toLanguageKey("block"))`, damit Block- und Fluidname gleich sind. [V]
- Eimer im Dispenser: `DispenserBlock.registerBehavior(bucket, DispenseFluidContainer.getInstance())` im `FMLCommonSetupEvent#enqueueWork`. [V `NewFluidTest`]
- Kein Fluid-Tag-Eintrag nötig fürs Fließen; zum Schwimmen/Wasser-Verhalten siehe `FluidType.Properties.isWaterLike` (UNVERIFIZIERT in der Wirkung).

**Client (NeoForge 26.3, geändert!)** [V `NF src/client/.../event/RegisterFluidModelsEvent.java`, `NF tests/.../FluidTypeTest.java`, `MEK client/ClientRegistrationUtil.java:195`]:
```java
// Mod-Bus, nur Client
static void models(RegisterFluidModelsEvent e) {
  e.register(new FluidModel.Unbaked(                       // net.minecraft.client.renderer.block.FluidModel
      new Material(Identifier.fromNamespaceAndPath(MODID, "block/oil_still")),   // net.minecraft.client.resources.model.sprite.Material
      new Material(Identifier.fromNamespaceAndPath(MODID, "block/oil_flow")),
      null,                                                // Overlay (nullable, FAB-Test übergibt null)
      FluidTintSources.constant(0xFF1A1A1A)),              // net.neoforged.neoforge.client.fluid.FluidTintSources
      OIL.get(), OIL_FLOWING.get());                       // register(model, still, flowing)
}
static void ext(RegisterClientExtensionsEvent e) {         // nur Overlay/Nebel
  e.registerFluidType(new IClientFluidTypeExtensions() { /* getRenderOverlayTexture, modifyFogColor, modifyFogRender */ }, OIL_TYPE.value());
}
```
Doppelte Registrierung für dasselbe Fluid wirft `IllegalStateException`. `FluidTintSources.constant(int)` nimmt ARGB. Die Texturen liegen als `assets/<modid>/textures/block/oil_still.png` (+ `.mcmeta` für Animation). Blockstate/Modell für den Fluidblock braucht es nicht (UNVERIFIZIERT; MEK/NF-Tests liefern keine). 3-Argument-Variante `FluidTintSources.constant(fluidColor, blockColor)` ist für Block-Tint getrennt. [V]

### 1.3 Fabric [V `FAB fabric-rendering-fluids-v1/src/testmod*`, `FAB fabric-block-api-v1/.../BlockTest.java`]

```java
// Common (ModInitializer)
Registry.register(BuiltInRegistries.FLUID, id("oil"), OIL);               // new Oil.Still()
Registry.register(BuiltInRegistries.FLUID, id("oil_flowing"), OIL_FLOWING);
ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id("oil"));
Registry.register(BuiltInRegistries.BLOCK, key,
    new LiquidBlock(OIL, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).setId(key)) {});   // anonyme Unterklasse
// Item: Registry.register(BuiltInRegistries.ITEM, itemKey, new BucketItem(OIL, new Item.Properties().setId(itemKey).craftRemainder(Items.BUCKET).stacksTo(1)));

// Client (ClientModInitializer), net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry
FluidRenderingRegistry.register(OIL, OIL_FLOWING, new FluidModel.Unbaked(
    new Material(id("block/oil_still")), new Material(id("block/oil_flow")), null, _ -> 0xFF1A1A1A));
// register(Fluid still, Fluid flow, FluidModel.Unbaked[, FluidRenderHandler custom]); einzeln: register(Fluid, FluidModel.Unbaked[, handler])
```
- Der Tint ist ein `BlockTintSource` (Lambda `state -> argb`). `FluidRenderingRegistry.setBlockTransparency(Block, boolean)` steuert Overlay-Seiten an Nachbarblöcken. Ein `FluidRenderHandler` ist nur für Sonderrendering (`renderFluid(...)`) nötig.
- Alte API `FluidRenderHandlerRegistry`/`SimpleFluidRenderHandler`: in 26.3 **nicht mehr vorhanden** (nur `FluidRenderingRegistry`, `FluidRenderHandler`, `FluidRendering` in `src/client`). Render-Layer (`ChunkSectionLayer`) liefert `FluidModel#layer()` beim Backen; `BlockRenderLayerMap` o. ä. ist nicht nötig (UNVERIFIZIERT, im Test nicht gesetzt).
- Namen im Tank/Tooltip: `FluidVariantAttributeHandler#getName` nimmt `createLegacyBlock().getBlock().getName()`, bei Luft den Key `block.<ns>.<fluid_path>` [V `FAB fabric-transfer-api-v1/.../FluidVariantAttributeHandler.java:42`]. Lang-Key also `block.<modid>.oil` anlegen.

### 1.4 Wie Mekanism es macht (26.3) [V `MEK common/registration/impl/FluidDeferredRegister.java`]
`FluidType` (Subklasse mit Textur-/Tint-Feldern) + `BaseFlowingFluid.Source/Flowing`, gemeinsame `BaseFlowingFluid.Properties` mit `DeferredHolder.create(Registries.FLUID, id)`-Verweisen (um Zyklen zu vermeiden), `.explosionResistance(100)`; `LiquidBlock` mit `.replaceable().noCollision().strength(100f).pushReaction(POPPED).noLootTable().liquid().sound(SoundType.EMPTY)`; `BucketItem` mit `stacksTo(1).craftRemainder(Items.BUCKET)`. Client: `RegisterFluidModelsEvent` -> `new FluidModel.Unbaked(still, flowing, overlay, FluidTintSources.constant(color))` (`MEK client/ClientRegistrationUtil.java:195`).

---

## 2. Fluid ohne Weltblock (nur Tanks, z. B. Kerosin)

- **NeoForge:** `BaseFlowingFluid.Properties` ohne `.block(...)` und `.bucket(...)` reicht. `bucket`/`block` sind nullable; `getBucket()` -> `Items.AIR`, `createLegacyBlock` -> `AIR`. [V `NF BaseFlowingFluid.java:104,125`] Mindestens nötig: ein registrierter `FluidType` und ein `Fluid` (Mod-Fluide müssen `getFluidType()` überschreiben, sonst `RuntimeException("Mod fluids must override getFluidType.")`, [V `NF CommonHooks.java:931`]). Quelle UND fließende Variante registrieren, da `getFlowing()` `flowing.get()` aufruft (nur Source zu registrieren ist UNVERIFIZIERT).
- **Fabric:** Jeder in `BuiltInRegistries.FLUID` registrierte `Fluid` ist als `FluidVariant.of(fluid)` nutzbar. Ohne Block muss `createLegacyBlock` `Blocks.AIR.defaultBlockState()` liefern (Fabric benennt dann über `block.<ns>.<path>`, siehe 1.3). Erbt man von `FlowingFluid`, bleiben die Pflichtmethoden aus 1.1; `getBucket()` darf `Items.AIR` liefern. [V/J]
- Ohne `FluidModel` (Rendering) sind Tanks/GUI nicht darstellbar: trotzdem ein `FluidModel.Unbaked` registrieren (Textur reicht), sonst fehlt Textur/Farbe (Fallback-Modell, UNVERIFIZIERT wie es aussieht).
- Einziges Verhalten ohne Block: Fluid wird nie in der Welt platziert (kein Eimer, kein Block). Datagen: Lang-Key `fluid_type.<modid>.<name>` (NeoForge) bzw. `block.<modid>.<name>` (Fabric-Namensableitung).

---

## 3. Fluid-Schnittstellen zu anderen Mods

### 3.1 NeoForge: `Capabilities.Fluid.BLOCK` [V `NF capabilities/Capabilities.java:30`, `NF transfer/**`]
`BlockCapability<ResourceHandler<FluidResource>, @Nullable Direction> Capabilities.Fluid.BLOCK` (auch `.ENTITY`, `.ITEM` mit `ItemAccess`). Mengen: `int` in **mB** (`FluidType.BUCKET_VOLUME = 1000`).

`ResourceHandler<T>`: `size()`, `getResource(i)`, `getAmountAsLong(i)`, `getCapacityAsLong(i, T)`, `isValid(i, T)`, `insert(i, T, int, tx)`, `extract(i, T, int, tx)`; Defaults: `insert(T, int, tx)`, `extract(T, int, tx)`, `getAmountAsInt`, `getCapacityAsInt`.

`FluidResource` (`transfer.fluid`): `EMPTY`, `of(FluidStack)`, `of(Fluid)`, `of(Fluid, DataComponentPatch)`, `of(Holder<Fluid>)`, `getFluid()`, `isEmpty()`, `toStack(int)`, `is(FluidType)`, `with(DataComponentType, data)`, `CODEC`.

Fertiger Tank: `FluidStacksResourceHandler(int size, int capacity)` (Basis `StacksResourceHandler`; `serialize(ValueOutput)`/`deserialize(ValueInput)`, `onContentsChanged(int, FluidStack previous)`, `isValid` überschreibbar):
```java
final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, 16_000) {
  @Override public boolean isValid(int i, FluidResource r) { return r.getFluid() == ModFluids.KEROSENE.get(); }
  @Override protected void onContentsChanged(int i, FluidStack prev) { setChanged(); }
};
// RegisterCapabilitiesEvent (Mod-Bus):
e.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.TANK.get(), (be, side) -> be.tank);   // null = Seite gesperrt
// Zugriff: level.getCapability(Capabilities.Fluid.BLOCK, pos, dir)
```
Eigener Handler mit `SnapshotJournal<T>` (`transfer.transaction`): vor jeder Änderung `journal.updateSnapshots(tx)`, `createSnapshot()` und `revertToSnapshot(T)` implementieren, optional `onRootCommit(T original)`:
```java
final class TankJournal extends SnapshotJournal<Integer> {
  int amount; FluidResource res = FluidResource.EMPTY;
  @Override protected Integer createSnapshot() { return amount; }
  @Override protected void revertToSnapshot(Integer s) { amount = s; if (amount == 0) res = FluidResource.EMPTY; }
  @Override protected void onRootCommit(Integer old) { setChanged(); }
}
// in insert(int i, FluidResource r, int a, TransactionContext tx):
//   int ins = Math.min(a, CAP - j.amount); if (ins > 0) { j.updateSnapshots(tx); j.res = r; j.amount += ins; } return ins;
```
Vorlage ist `StacksResourceHandler.StackJournal` (`NF transfer/StacksResourceHandler.java:278`). Bei Resource-Wechsel (res) ebenfalls im Snapshot sichern, z. B. als Record `(FluidResource, int)`.

### 3.2 Fabric: `FluidStorage.SIDED` [V `FAB fabric-transfer-api-v1/src/main/.../fluid/**`, `.../storage/base/SingleVariantStorage.java`]
`BlockApiLookup<Storage<FluidVariant>, @Nullable Direction> FluidStorage.SIDED` (Items: `FluidStorage.ITEM`). Mengen in **Droplets**: `FluidConstants.BUCKET = BLOCK = 81000`, `BOTTLE = BOWL = 27000`, `INGOT = 9000`, `NUGGET = 1000`, `DROPLET = 1`; `fromBucketFraction(num, den)`. 1 mB = 81 Droplets.

`Storage<T>`: `insert(T, long, TransactionContext)`, `extract(T, long, tx)`, `iterator()`, `supportsInsertion/Extraction()`. `FluidVariant`: `blank()`, `of(Fluid)`, `of(Fluid, DataComponentPatch)`, `getFluid()`, `isBlank()`, `isOf(Fluid)`.
```java
// Fertig: SingleFluidStorage.withFixedCapacity(long capacity, Runnable onChange), readValue(ValueInput), writeValue(ValueOutput)
final SingleFluidStorage tank = SingleFluidStorage.withFixedCapacity(16 * FluidConstants.BUCKET, this::setChanged);
// Eigene Variante:
new SingleVariantStorage<FluidVariant>() {
  @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
  @Override protected long getCapacity(FluidVariant v) { return 16 * FluidConstants.BUCKET; }
  @Override protected boolean canInsert(FluidVariant v) { return v.isOf(ModFluids.KEROSENE); }
  @Override protected void onFinalCommit() { setChanged(); }       // aus SnapshotParticipant
};
FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.tank, ModBlockEntities.TANK);   // siehe api-fabric-26.3.md Z.129
```
`SingleVariantStorage` hat `public T variant` und `public long amount` (Basis `SnapshotParticipant`). `registerForBlockEntity` ist im 26.3-Fabric-Checkout nicht lesbar gewesen (lookup-api nicht ausgecheckt), die Nutzung steht im vorhandenen Fabric-Spickzettel; `registerForBlocks((level,pos,state,be,dir) -> storage, blocks...)` ist belegt (`FAB fabric-transfer-api-v1/src/testmod/.../TransferTestInitializer.java:70`). Alternative ohne Registrierung: `SidedStorageBlockEntity#getFluidStorage(@Nullable Direction)` auf der BlockEntity (Fabric registriert den Fallback selbst).

---

## 4. Fluide in der Welt abfragen und entfernen [J]

```java
FluidState fs = level.getFluidState(pos);                       // BlockGetter#getFluidState(BlockPos)
fs.isEmpty(); fs.isSource(); fs.getType(); fs.isSourceOfType(fluid); fs.getAmount(); fs.is(FluidTags.WATER);
Fluid f = fs.getType();                       // Quelle und fließend sind verschiedene Instanzen: f.isSame(other) / FlowingFluid#getSource()
Item bucket = f.getBucket();                  // Fluid#getBucket() (Items.AIR bei Block-/eimerlosen Fluiden)
level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);   // setBlock(BlockPos, BlockState, int) / setBlockAndUpdate
level.removeBlock(pos, false);                // boolean movedByPiston; Vanilla-Eimer (LiquidBlock#pickupBlock) nutzt setBlock(pos, AIR, 11)
BlockState legacy = fs.createLegacyBlock();   // Block mit LEVEL, z. B. zum Platzieren
```
- Wasser/Lava-Eimer: `Items.WATER_BUCKET`, `Items.LAVA_BUCKET`, `Items.BUCKET`; Konstruktor `BucketItem(Fluid, Item.Properties)`; `BucketItem#getContent()` liefert das Fluid, `emptyContents(LivingEntity, Level, BlockPos, BlockHitResult)` platziert es.
- Nur Quellen einsammeln: `fs.isSource() && fs.getType().isSame(OIL)`; fließende Blöcke (`LEVEL > 0`) nimmt `LiquidBlock#pickupBlock` nicht auf (liefert `ItemStack.EMPTY`), Luft setzen mit `setBlock(pos, AIR, 11)` lässt die Nachbarn nachfließen.
- NeoForge zusätzlich: `FluidType#getBucket(FluidStack)`, `FluidType#getBlockForFluidState(getter, pos, state)`, `FluidUtil` in `transfer.fluid` (Eimer-Interaktion). [V]
- Fabric: `FluidVariant.of(fs.getType())` für Storage-Zugriffe; `FluidStorageUtil` für Eimer. [V]

## 5. Data Component registrieren [J]

```java
DataComponentType<T> t = DataComponentType.<T>builder()                  // static <T> Builder<T> builder()
    .persistent(Codec<T>)                                                // Speicherung/JSON
    .networkSynchronized(StreamCodec<? super RegistryFriendlyByteBuf, T>)// Client-Sync (optional)
    .cacheEncoding()                                                     // optional
    .ignoreSwapAnimation()                                               // optional (Builder-Methode)
    .build();
// Registry: BuiltInRegistries.DATA_COMPONENT_TYPE  (Registry<DataComponentType<?>>);  Key: Registries.DATA_COMPONENT_TYPE
Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("fluid_amount"), t);          // Fabric/vanilla; Identifier- oder ResourceKey-Überladung
// NeoForge:
DeferredRegister.DataComponents DC = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMOUNT =
    DC.registerComponentType("amount", b -> b.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
// Item: new Item.Properties().component(type, value)  oder  stack.set(type, value) / stack.get(type)
```
`Item.Properties#component(DataComponentType<T>, T)` [J]. Ohne `persistent` ist die Komponente `isTransient()`. Ohne `networkSynchronized` ist sie nur serverseitig (UNVERIFIZIERT, wie Vanilla das auf dem Client behandelt).

## 6. Worldgen: Fluidkugel / Ölblase

**Vanilla-Optionen (26.3, Jar):** `minecraft:lake` (`LakeFeature`, Record mit `fluid`, `barrier`, `can_place_feature`, `can_replace_with_air_or_fluid`, `can_replace_with_barrier`) und `minecraft:spring_feature` (`SpringFeature`: `state: FluidState`, `requires_block_below`, `rock_count`, `hole_count`, `valid_blocks`; kleine Quelle im Gestein, kein Pool). Mit `lake` geht eine Ölblase ohne eigenen Featuretyp, weil `fluid` ein beliebiger `BlockStateProvider` ist, auch der Fluidblock eines Mods:
```json
// data/<modid>/worldgen/feature/oil_pool.json   (Vorlage: data/minecraft/worldgen/feature/lake_lava.json)
{ "type": "minecraft:lake",
  "barrier": { "id": "minecraft:stone" },
  "can_place_feature": { "type": "minecraft:true" },
  "can_replace_with_air_or_fluid": { "type": "minecraft:not", "predicate": { "type": "minecraft:matching_block_tag", "tag": "minecraft:features_cannot_replace" } },
  "can_replace_with_barrier": { "type": "minecraft:not", "predicate": { "type": "minecraft:matching_block_tag", "tag": "minecraft:lava_pool_stone_cannot_replace" } },
  "fluid": { "id": "<modid>:oil", "properties": { "level": "0" } } }
```
Placed Feature (Vorlage `placed_feature/lake_lava_surface.json`): `{"feature":"<modid>:oil_pool","placement":[{"type":"minecraft:rarity_filter","chance":200},{"type":"minecraft:in_square"},{"type":"minecraft:heightmap","heightmap":"WORLD_SURFACE_WG"},{"type":"minecraft:biome"}]}`. Untergrundvariante: `count` + `in_square` + `height_range` (Muster `spring_water`: `count 25`, `height_range uniform` mit `above_bottom 0` bis `absolute 192`). Anschließend per Biome-Modifier/`BiomeModifications` in `GenerationStep.Decoration.UNDERGROUND_ORES` oder `LAKES` einhängen.
Form des Sees (Ellipsoid-Blasen, Fluid nur in der unteren Hälfte) und ob die Kugel in tiefen Schichten sauber gesetzt wird: UNVERIFIZIERT (`LakeFeature#place` nicht dekompiliert).

**Eigener Featuretyp (falls `lake` nicht reicht)** [J][V `FAB fabric-data-attachment-api-v1/.../SetAttachmentFeature.java`, `MEK registries/MekanismFeatureTypes.java`]:
```java
public record OilBlobFeature(BlockStateProvider fluid, IntProvider radius) implements Feature {      // Config = Record-Felder
  public static final MapCodec<OilBlobFeature> CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
      BlockStateProvider.CODEC.fieldOf("fluid").forGetter(OilBlobFeature::fluid),
      IntProviders.CODEC.fieldOf("radius").forGetter(OilBlobFeature::radius)).apply(b, OilBlobFeature::new));
  @Override public MapCodec<OilBlobFeature> codec() { return CODEC; }
  @Override public boolean place(WorldGenLevel level, ChunkGenerator gen, RandomSource random, BlockPos origin) { ... }
}
// Fabric: Registry.register(BuiltInRegistries.FEATURE_TYPE, id("oil_blob"), OilBlobFeature.CODEC);
// NeoForge: DeferredRegister.create(Registries.FEATURE_TYPE, MODID).register("oil_blob", () -> OilBlobFeature.CODEC);
```
Interface `Feature`: `MapCodec<? extends Feature> codec()`, `boolean place(WorldGenLevel, ChunkGenerator, RandomSource, BlockPos)`, Hilfen `setBlock(LevelWriter, BlockPos, BlockState)`, `safeSetBlock(WorldGenLevel, BlockPos, BlockState, Predicate<BlockState>)`, `markAboveForPostProcessing`. Kein `FeaturePlaceContext`, keine `FeatureConfiguration`, kein `ConfiguredFeature`-Wrapper. JSON dann: `{"type":"<modid>:oil_blob","fluid":{"id":"<modid>:oil"},"radius":{"type":"minecraft:uniform","min_inclusive":2,"max_inclusive":4}}` (Muster: `disk_clay.json`, `ore_clay.json`, `lake_lava.json`; `type` = Registry-ID des Codecs). Das Feature-JSON wird von `Feature.DIRECT_CODEC` über `codec()` dispatcht. [J]

## 7. Fluid-Farbe/Textur im GUI-Balken (Client)

Beide Loader nutzen dasselbe Vanilla-Objekt [V `NF client/color/item/FluidContentsTint.java:33`, `NF client/model/item/DynamicFluidContainerModel.java:114`, `FAB fabric-transfer-api-v1/src/client/.../FluidVariantRenderHandler.java:59`]:
```java
FluidModel m = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
TextureAtlasSprite sprite = m.stillMaterial().sprite();                  // Material$Baked#sprite()
BlockTintSource tint = m.tintSource();                                   // nullable! Farbe: tint.color(Blocks.AIR.defaultBlockState()) (FAB), NeoForge: m.fluidTintSource().colorAsStack(stack)
```
Fabric-Kurzform: `FluidVariantRendering.getColor(FluidVariant)` (`FAB fabric-transfer-api-v1/src/client/.../client/fluid/FluidVariantRendering.java`) und `FluidVariantAttributes.getAssociatedColor(variant)` für Server-/Name-Farbe. Das Zeichnen der Sprite-Kachel im GUI (`GuiGraphics`-Methode in 26.3) ist hier nicht geprüft: UNVERIFIZIERT. Einfachere Variante: GUI mit fester Farbe aus `items.json` zeichnen (`GuiGraphics#fill`), die Texturen erst später einbinden.
