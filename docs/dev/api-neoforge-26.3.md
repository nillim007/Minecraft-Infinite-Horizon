# API-Spickzettel: NeoForge 26.3 (Minecraft 26.3, Java 25, Mojang-Namen)

Stand: 2026-11-10. Alles unten wurde aus echtem Quellcode für 26.3 gelesen, nicht aus dem Gedächtnis.

**Markierungen**

- **[V]** steht so in kompiliertem 26.3-Quellcode (NeoForge selbst, NeoForge-Tests oder eine auf 26.3 portierte Mod).
- **[D]** steht nur in der NeoForge-Doku. Deren Stand ist 26.1, es ist also Prosa und nicht kompiliert.
- **[P]** steht nur im Migrations-Primer (Prosa).
- **UNVERIFIZIERT**: in keiner Quelle gesehen.

## Quellen

| Kürzel | Repository @ Branch | Basis-URL |
|---|---|---|
| NF | neoforged/NeoForge @ `26.3.x`, Commit `085df70` (2026-11-09) | https://github.com/neoforged/NeoForge/blob/26.3.x/ |
| MDK | NeoForgeMDKs/MDK-26.3-ModDevGradle @ main (neo 26.3.0.101, MDG 2.0.148) | https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle |
| FAB | FabricMC/fabric @ `26.3`. Die Testmods rufen nur Vanilla mit Mojang-Namen auf. | https://github.com/FabricMC/fabric/blob/26.3/ |
| FB | XFactHD/FramedBlocks @ `26.3` | https://github.com/XFactHD/FramedBlocks/blob/26.3/ |
| MEK | mekanism/Mekanism @ `26.3` | https://github.com/mekanism/Mekanism/blob/26.3/ |
| IC | progwml6/ironchest @ `26.3-neoforge` | https://github.com/progwml6/ironchest/blob/26.3-neoforge/ |
| DOC | neoforged/Documentation `docs/` (Label „26.1“) | https://github.com/neoforged/Documentation |
| PRI | neoforged/.github `primers/26.1`, `26.2`, `26.3` | https://github.com/neoforged/.github/tree/main/primers |

Pfadabkürzungen für NF:

- `nf/` = `src/main/java/net/neoforged/neoforge/`
- `nfc/` = `src/client/java/net/neoforged/neoforge/client/`
- `pat/` = `patches/net/minecraft/`
- `nft/` = `tests/src/main/java/net/neoforged/neoforge/`
- `tf/` = `testframework/src/main/java/net/neoforged/testframework/`

---

## 0. Die größten Unterschiede zu 1.21.1

1. `ResourceLocation` heißt jetzt **`net.minecraft.resources.Identifier`**. Aus `ResourceKey#location()` wurde **`identifier()`**. `TagKey#location()` heißt weiter so.
2. `Block`/`Item` brauchen die ID schon im Konstruktor: `Properties#setId(ResourceKey)`. Die `DeferredRegister`-Helfer setzen sie automatisch.
3. **`Block#codec()` gibt es in 26.3 nicht mehr.** Keine `MapCodec` für Blöcke, kein `codec()`-Override, auch nicht bei `BaseEntityBlock`.
4. BlockEntity-NBT läuft über `saveAdditional(ValueOutput)` und `loadAdditional(ValueInput)`, nicht mehr über `CompoundTag` und `HolderLookup`.
5. Die Capabilities `IEnergyStorage`, `IItemHandler` und `IFluidHandler` sind weg. Es gibt jetzt **`EnergyHandler`**, **`ResourceHandler<ItemResource>`** und **`ResourceHandler<FluidResource>`**, alle mit `Transaction`. Die Caps heißen `Capabilities.Energy.BLOCK`, `Capabilities.Item.BLOCK` und `Capabilities.Fluid.BLOCK`.
6. GUI: `GuiGraphics` heißt `GuiGraphicsExtractor`. Aus `render` wurde `extractRenderState`, aus `renderBg` wurde `Screen#extractBackground`, aus `drawString` wurde `text`. `imageWidth` und `imageHeight` sind `final` und werden im Konstruktor gesetzt.
7. Rezepte:
   - `RecipeSerializer` ist ein Record: `new RecipeSerializer<>(MapCodec, StreamCodec)`.
   - `assemble(I)` hat keinen `HolderLookup` mehr.
   - Ergebnisse sind `ItemStackTemplate`.
   - **Rezepte sind in 26.3 eine neu ladbare Datapack-Registry** (`Registries.RECIPE`).
8. GameTests: `@GameTest` aus Vanilla und `@GameTestHolder` sind weg. Ein Test besteht jetzt aus:
   - einer Testfunktion (Registry `TEST_FUNCTION`),
   - einer `GameTestInstance`,
   - `TestData` mit 12 Feldern, darunter Dimension und Padding,
   - einer `TestEnvironmentDefinition`.
9. **Brennwert: `FuelValues` und NeoForges Datamap `FURNACE_FUELS` sind weg.** Er steht jetzt in `DataComponents.COOKING_FUEL` (`CookingFuel`) und wird per `LootContext` aufgelöst.
10. `ClientPacketDistributor.sendToServer` ist rein clientseitig (`nfc/network/`). Client-Handler registriert man über `RegisterClientPayloadHandlersEvent`.

---

## 1. Identifier und ResourceKey

[V] Quelle: über 300 Treffer in NF, z. B. `nf/network/codec/NeoForgeStreamCodecs.java`, `nf/common/Tags.java`.

```java
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

Identifier id   = Identifier.fromNamespaceAndPath("sternenwerft", "crusher");
Identifier mc   = Identifier.withDefaultNamespace("stone");
Identifier p    = Identifier.parse("sternenwerft:crusher");   // Identifier.tryParse(..) -> @Nullable
ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
ResourceKey<Registry<Foo>> regKey = ResourceKey.createRegistryKey(id);
Identifier back = key.identifier();                            // NICHT location()
TagKey<Item> tag = TagKey.create(Registries.ITEM, id);         // oder ItemTags.create(id) / BlockTags.create(id) [V]
Identifier tagId = tag.location();                             // bei TagKey bleibt location()
buf.writeIdentifier(id); Identifier r = buf.readIdentifier();  // FriendlyByteBuf [V] NeoForgeStreamCodecs
```

---

## 2. Blöcke, Items, BlockItem, Creative-Tab

[V] Quellen: `nf/registries/DeferredRegister.java` (Z. 146–764) und MDK `ExampleMod.java`.

- `DeferredRegister.Blocks#registerBlock(String, Function<BlockBehaviour.Properties, B>, UnaryOperator<Properties>)` ruft intern `properties.setId(ResourceKey.create(Registries.BLOCK, key))` auf.
- `DeferredRegister.Items#registerItem(...)` macht dasselbe mit `Item.Properties#setId(ResourceKey<Item>)`.
- `registerSimpleBlockItem(...)` setzt zusätzlich `.useBlockDescriptionPrefix()`.

```java
public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems(MODID);
public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

public static final DeferredBlock<CrusherBlock> CRUSHER = BLOCKS.registerBlock("crusher", CrusherBlock::new,
        p -> p.mapColor(MapColor.METAL).strength(3.5f).requiresCorrectToolForDrops());
public static final DeferredItem<BlockItem> CRUSHER_ITEM = ITEMS.registerSimpleBlockItem("crusher", CRUSHER);
public static final DeferredItem<Item> IRON_PLATE = ITEMS.registerSimpleItem("iron_plate");
public static final DeferredItem<WrenchItem> WRENCH = ITEMS.registerItem("wrench", WrenchItem::new, p -> p.stacksTo(1));

public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.sternenwerft"))
        .withTabsBefore(CreativeModeTabs.COMBAT)            // NeoForge-Patch, nimmt ResourceKey<CreativeModeTab>... oder Identifier...
        .icon(() -> CRUSHER_ITEM.get().getDefaultInstance())
        .displayItems((params, output) -> { output.accept(CRUSHER_ITEM.get()); output.accept(IRON_PLATE.get()); })
        .build());

// Konstruktor der Mod: BLOCKS.register(modBus); ITEMS.register(modBus); TABS.register(modBus);
```

**Manuell, ohne Helfer:** `new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id)))` und `new BlockItem(block, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix())`.

**Blockklassen in 26.3** [V] (FAB `fabric-menu-api-v1/.../BoxBlock.java`, FAB `fabric-api-lookup-api-v1/.../ChuteBlock.java`, PRI 26.3 „The Death of the Unused Block Types“):

- kein `codec()`-Override, auch nicht bei `BaseEntityBlock`
- `public InteractionResult useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)`
- `protected InteractionResult useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)`
- `affectNeighborsAfterRemoval(BlockState, ServerLevel, BlockPos, boolean)`
- `getAnalogOutputSignal(BlockState, Level, BlockPos, Direction)`
- `level.isClientSide()` ist eine Methode.

---

## 3. BlockEntityType, Speichern/Laden, Sync, Ticken

[V] Quellen:

- `pat/world/level/block/entity/BlockEntityType.java.patch`: NeoForge-Konstruktor `BlockEntityType(BlockEntitySupplier<? extends T>, Block...)`. Es gibt kein `Builder` mehr; ein leeres Varargs-Array wirft eine Exception.
- `nft/oldtest/client/model/CustomItemDisplayContextTest.java`: Speichern, Laden und Sync.
- FB `common/blockentity/special/PoweredFramingSawBlockEntity.java`
- FAB `fabric-renderer-api-v1/.../FrameBlockEntity.java`
- FAB `ChuteBlock.java`: Ticker

```java
public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
public static final Supplier<BlockEntityType<CrusherBlockEntity>> CRUSHER_BE =
        BLOCK_ENTITIES.register("crusher", () -> new BlockEntityType<>(CrusherBlockEntity::new, ModBlocks.CRUSHER.get()));

public class CrusherBlock extends BaseEntityBlock {               // kein codec() mehr!
    public CrusherBlock(Properties p) { super(p); }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrusherBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CRUSHER_BE.get(), CrusherBlockEntity::serverTick);
    }
}

public class CrusherBlockEntity extends BlockEntity {
    public CrusherBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.CRUSHER_BE.get(), pos, state); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrusherBlockEntity be) { /* ... */ }

    @Override protected void saveAdditional(ValueOutput out) {          // protected (so in NF-Test + FB)
        super.saveAdditional(out);
        out.putInt("progress", progress);
        out.store("item", ItemStack.CODEC, stack);                         // beliebiger Codec
        out.putChild("energy", energy);                                    // NeoForge: ValueIOSerializable -> out.child(key)
    }
    @Override public void loadAdditional(ValueInput in) {               // public ist sicher (NF-Test + FB)
        super.loadAdditional(in);
        progress = in.getIntOr("progress", 0);
        stack = in.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);  // Optional<T>
        in.readChild("energy", energy);                                    // NeoForge-Extension
    }

    // Sync zum Client (Chunk-Load + Block-Update)
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); } // oder saveCustomOnly(registries)
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    // NeoForge-Defaults: onDataPacket(Connection, ValueInput) und handleUpdateTag(ValueInput) (nf/common/extensions/IBlockEntityExtension.java)

    void markDirtyAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }
}
```

- **`ValueOutput`** [V]:
  - `putInt`, `putLong`, `putFloat`, `putDouble`, `putBoolean`, `putByte`, `putString`, `putIntArray`
  - `store(String, Codec<T>, T)`, `storeNullable(...)`
  - `child(String)`, `childrenList(String)`, `list(String, Codec)`, `discard(String)`
  - NeoForge: `putChild(String, ValueIOSerializable)`, `store(CompoundTag)`
- **`ValueInput`** [V]:
  - `getIntOr`, `getLongOr`, `getFloatOr`, `getDoubleOr`, `getBooleanOr`, `getByteOr`, `getStringOr`
  - `getInt(String)` und `getString(String)` geben `Optional` zurück
  - `read(String, Codec)` gibt `Optional` zurück
  - `child(String)` gibt `Optional<ValueInput>` zurück, `childOrEmpty(String)` nicht
  - `listOrEmpty`, `childrenListOrEmpty`
  - NeoForge: `readChild(String, ValueIOSerializable)`, `keySet()`
- Weitere BE-Hooks [V]: `onLoad()` (NeoForge), `preRemoveSideEffects(BlockPos, BlockState)` (Vanilla, z. B. Inhalt droppen), `invalidateCapabilities()`.

---

## 4. Transfer-API (Energie, Items, Fluids) und Capabilities

[V] Quellen: `nf/transfer/**`, `nf/capabilities/Capabilities.java`, `RegisterCapabilitiesEvent.java`, `BlockCapabilityCache.java`, `nf/common/extensions/ILevelExtension.java`; FB `common/capability/CapabilitySetup.java`, FB `PoweredFramingSawBlockEntity.java`.

| Zweck | Typ (Paket) |
|---|---|
| Transaktion | `net.neoforged.neoforge.transfer.transaction.Transaction` (`openRoot()`, `open(@Nullable TransactionContext)`, `commit()`, `close()`); `TransactionContext` (sealed) |
| Energie | `net.neoforged.neoforge.transfer.energy.EnergyHandler`: `long getAmountAsLong()`, `int getAmountAsInt()`, `long getCapacityAsLong()`, `int getCapacityAsInt()`, `int insert(int, TransactionContext)`, `int extract(int, TransactionContext)` |
| Energie-Impl. | `SimpleEnergyHandler(int capacity[, int maxTransfer \| int maxInsert, int maxExtract[, int energy]])`, `set(int)`, `protected onEnergyChanged(int previous)`, `serialize/deserialize` (Key `"energy"`) |
| Ressourcen | `net.neoforged.neoforge.transfer.ResourceHandler<T extends Resource>`: `size()`, `getResource(int)`, `getAmountAsLong(int)`, `getAmountAsInt(int)`, `getCapacityAsLong(int,T)`, `isValid(int,T)`, `insert(int,T,int,tx)`, `insert(T,int,tx)`, `extract(int,T,int,tx)`, `extract(T,int,tx)` |
| Items | `transfer.item.ItemResource` (`of(ItemStack)`, `of(ItemLike)`, `toStack(int)`, `isEmpty()`, `is(ItemLike)`); `ItemStacksResourceHandler(int size)`; `ItemUtil.getStack(handler, index)` |
| Fluids | `transfer.fluid.FluidResource` (`of(FluidStack)`, `of(Fluid)`, `toStack(int)`); `FluidStacksResourceHandler(int size, int capacity)`; `FluidType.BUCKET_VOLUME = 1000` |
| Stacks-Basis | `StacksResourceHandler<S,T>`: `set(int, T, int)` (= `IndexModifier`), `protected onContentsChanged(int, S previous)`, `isValid` überschreibbar, `serialize/deserialize` (Key `"stacks"`) |
| Hilfen | `ResourceHandlerUtil.move(from, to, Predicate<T>, int, @Nullable tx)`, `moveStacking`, `isEmpty`, `isFull`; `EnergyHandlerUtil.move(from, to, int, @Nullable tx)` (bei `null` als tx wird eine eigene Transaktion geöffnet und committet) |

```java
final ItemStacksResourceHandler items = new ItemStacksResourceHandler(2) {
    @Override protected void onContentsChanged(int index, ItemStack previous) { setChanged(); } // erst nach Commit aufgerufen
    @Override public boolean isValid(int index, ItemResource res) { return index == 0; }
};
final SimpleEnergyHandler energy = new SimpleEnergyHandler(10_000, 100, 0) {   // Zahlen nur Beispiel -> BalanceManager
    @Override protected void onEnergyChanged(int previousAmount) { setChanged(); }
};

// Intern verbrauchen (maxExtract=0 sperrt nur externe Entnahme):
energy.set(energy.getAmountAsInt() - cost);

// Transaktional einfügen:
try (Transaction tx = Transaction.openRoot()) {
    int inserted = items.insert(0, ItemResource.of(Items.IRON_ORE), 1, tx);
    if (inserted == 1) tx.commit();          // ohne commit -> Rollback beim close()
}

// Capabilities registrieren (Mod-Bus, RegisterCapabilitiesEvent implements IModBusEvent)
static void onCaps(RegisterCapabilitiesEvent e) {
    e.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.CRUSHER_BE.get(), (be, side) -> be.energy);
    e.registerBlockEntity(Capabilities.Item.BLOCK,   ModBlockEntities.CRUSHER_BE.get(), (be, side) -> be.items);
    // Fluid: Capabilities.Fluid.BLOCK -> ResourceHandler<FluidResource>; null zurückgeben = Seite gesperrt
}

// Nachbar abfragen (Rückgabe @Nullable), Kontext = Seite des Nachbarn
EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, pos.relative(dir), dir.getOpposite());
EnergyHandlerUtil.move(energy, target, 100, null);
ResourceHandler<ItemResource> inv = level.getCapability(Capabilities.Item.BLOCK, pos.relative(dir), dir.getOpposite());
ResourceHandlerUtil.move(items, inv, r -> true, 64, null);
// Gecacht: BlockCapabilityCache.create(Capabilities.Energy.BLOCK, serverLevel, pos, side) -> cache.getCapability()
// Nach Änderung der angebotenen Caps: be.invalidateCapabilities() bzw. level.invalidateCapabilities(pos)
```

Speichern mit Child-Key, weil beide Handler feste Keys benutzen: `out.putChild("items", items)` und `in.readChild("items", items)`. FB macht das auch, nur über `serialize(out.child("inventory"))` bzw. `deserialize(in.childOrEmpty("inventory"))`.

Weitere Caps-Konstanten [V]:

- Items: `Capabilities.Item.ENTITY` (`Void`-Kontext), `Item.ENTITY_AUTOMATION`, `Item.ITEM` (Kontext `ItemAccess`)
- Energie: `Energy.ENTITY`, `Energy.ITEM`
- Fluids: `Fluid.ENTITY`, `Fluid.ITEM`

---

## 5. Menüs und Screens

[V] Quellen:

- `nf/common/extensions/IMenuTypeExtension.java`: `static MenuType<T> create(IContainerFactory<T>)`
- `nf/network/IContainerFactory.java`: `T create(int windowId, Inventory inv, RegistryFriendlyByteBuf data)`
- `nf/common/extensions/IPlayerExtension.java`:
  - `openMenu(@Nullable MenuProvider, BlockPos)` schreibt die Position.
  - `openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)`
- `nft/oldtest/misc/ContainerTypeTest.java`, `nft/oldtest/recipebook/RecipeBook*.java`
- `nfc/event/RegisterMenuScreensEvent.java`
- `pat/client/gui/screens/inventory/AbstractContainerScreen.java.patch`
- IC `client/screen/IronChestScreen.java`
- FB `common/menu/PoweredFramingSawMenu.java`: `ResourceHandlerSlot`, `DataSlot`

```java
public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
public static final DeferredHolder<MenuType<?>, MenuType<CrusherMenu>> CRUSHER_MENU =
        MENUS.register("crusher", () -> IMenuTypeExtension.create(CrusherMenu::new));

public class CrusherMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access; private final ContainerData data;
    // Client-Seite (IContainerFactory): Dummy-Speicher, Server synct Inhalte über Slots/DataSlots
    public CrusherMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, new ItemStacksResourceHandler(2), new SimpleContainerData(3), ContainerLevelAccess.NULL); // buf.readBlockPos() falls Pos gebraucht
    }
    public CrusherMenu(int id, Inventory inv, ItemStacksResourceHandler items, ContainerData data, ContainerLevelAccess access) {
        super(ModMenus.CRUSHER_MENU.get(), id);
        this.access = access; this.data = data;
        addSlot(new ResourceHandlerSlot(items, items::set, 0, 56, 35));   // (handler, IndexModifier, index, x, y) [V]
        addSlot(new ResourceHandlerSlot(items, items::set, 1, 116, 35));
        addStandardInventorySlots(inv, 8, 84);                            // [D] (FB/IC legen Spieler-Slots per Schleife an [V])
        addDataSlots(data);                                               // [D]; einzeln: addDataSlot(DataSlot.standalone()) [V]
    }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, ModBlocks.CRUSHER.get()); } // [V]
    @Override public ItemStack quickMoveStack(Player player, int index) { /* moveItemStackTo(stack, from, toExcl, reverse) [V] */ return ItemStack.EMPTY; }
}

// Öffnen (Server): BE implementiert MenuProvider { Component getDisplayName(); AbstractContainerMenu createMenu(int, Inventory, Player); }
@Override public InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrusherBlockEntity be) player.openMenu(be, pos);
    return InteractionResult.SUCCESS;
}
// Server-Menü in createMenu: new CrusherMenu(id, inv, items, data, ContainerLevelAccess.create(level, worldPosition))
```

**ContainerData:** `ContainerData#get(int)` [V] (Furnace-Patch). `set(int,int)`, `getCount()`, `SimpleContainerData(int)` und `checkContainerDataCount(...)` stehen nur in [D].

Vanilla schickt nur 16 Bit (short). NeoForge patcht `ServerPlayer#broadcastDataValue`, sodass zwischen zwei NeoForge-Seiten **volle ints** übertragen werden [V] `pat/server/level/ServerPlayer.java.patch`. Fabric-Clients bekommen weiter nur short. Für Energie >32767 also skalieren oder ein Payload benutzen.

```java
// Client: Mod-Bus (RegisterMenuScreensEvent implements IModBusEvent)
@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent static void screens(RegisterMenuScreensEvent e) { e.register(ModMenus.CRUSHER_MENU.get(), CrusherScreen::new); }
}

public class CrusherScreen extends AbstractContainerScreen<CrusherMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(MODID, "textures/gui/container/crusher.png");
    public CrusherScreen(CrusherMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);            // optional: super(menu, inv, title, imageWidth, imageHeight)  (Felder sind final)
        this.inventoryLabelY = this.imageHeight - 94;                 // titleLabelX/Y, inventoryLabelX/Y sind setzbar [V]
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);      // abgedunkelter Hintergrund (IC macht das so)
        g.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos + 79, topPos + 34, 176, 14, getMenu().progressPx(), 17, 256, 256);
    }
    // Labels: protected void extractLabels(GuiGraphicsExtractor, int, int); Text: g.text(font, Component|String, x, y, 0xFF404040)
    // Tooltip: g.setComponentTooltipForNextFrame(font, List<Component>, mouseX, mouseY); Rechteck: g.fill(x1, y1, x2, y2, argb)
}
```

- Die Basisklasse ruft in `extractRenderState` schon `extractContents`, `extractCarriedItem` und `extractTooltip` auf. `extractRenderState` muss man also nicht überschreiben ([P] 26.1, [V] Patch).
- Textfarben brauchen Alpha (`0xFF......`), sonst ist der Text unsichtbar ([P] 1.21.6).
- Imports:
  - `net.minecraft.client.gui.GuiGraphicsExtractor`
  - `net.minecraft.client.renderer.RenderPipelines`
  - `net.minecraft.client.gui.screens.inventory.AbstractContainerScreen`
  - `net.neoforged.neoforge.transfer.item.ResourceHandlerSlot`

---

## 6. Eigene Rezepte

[V] Quellen:

- `nft/oldtest/recipebook/RecipeBookTestRecipe.java` und `RecipeBookExtensionTest.java`
- FB `common/crafting/saw/FramingSawRecipe.java`, `FramingSawRecipeCache.java`, `PoweredFramingSawBlockEntity.java`
- `pat/world/item/crafting/RecipeType.java.patch`: NeoForge `RecipeType.simple(Identifier)`
- `pat/.../RecipeManager.java.patch`
- PRI 26.1 (Serializer-Record, `assemble`), PRI 26.3 (Rezepte als Datapack-Registry)

```java
public record CrushingRecipe(Ingredient input, ItemStackTemplate result, int time) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<CrushingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("input").forGetter(CrushingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
            Codec.INT.fieldOf("time").forGetter(CrushingRecipe::time)).apply(i, CrushingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(MAP_CODEC.codec());           // [V] wie im NF-Test

    @Override public boolean matches(SingleRecipeInput in, Level level) { return input.test(in.getItem(0)); }
    @Override public ItemStack assemble(SingleRecipeInput in) { return result.create(); }   // KEIN HolderLookup mehr
    @Override public boolean showNotification() { return false; }    // nicht mehr default
    @Override public String group() { return ""; }                  // nicht mehr default
    @Override public boolean isSpecial() { return true; }           // aus dem Rezeptbuch heraushalten (FB)
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return ModRecipes.BOOK_CATEGORY.value(); }
    @Override public RecipeSerializer<CrushingRecipe> getSerializer() { return ModRecipes.CRUSHING_SERIALIZER.get(); }
    @Override public RecipeType<CrushingRecipe> getType() { return ModRecipes.CRUSHING.get(); }
    // display() hat einen Default (MEK überschreibt ihn absichtlich abstrakt) -> List<RecipeDisplay> für JEI/Rezeptbuch
}

public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, MODID);
public static final DeferredHolder<RecipeType<?>, RecipeType<CrushingRecipe>> CRUSHING =
        TYPES.register("crushing", () -> RecipeType.simple(Identifier.fromNamespaceAndPath(MODID, "crushing")));
public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, MODID);
public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrushingRecipe>> CRUSHING_SERIALIZER =
        SERIALIZERS.register("crushing", () -> new RecipeSerializer<>(CrushingRecipe.MAP_CODEC, CrushingRecipe.STREAM_CODEC));
public static final DeferredRegister<RecipeBookCategory> BOOK = DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, MODID);
public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> BOOK_CATEGORY = BOOK.register("crushing", RecipeBookCategory::new);
```

**Suche auf dem Server** [V]: `ServerLevel#recipeAccess()` ist der `RecipeManager`.

```java
if (level instanceof ServerLevel sl) {
    SingleRecipeInput in = new SingleRecipeInput(ItemUtil.getStack(items, 0));
    Optional<RecipeHolder<CrushingRecipe>> r = sl.recipeAccess().getRecipeFor(ModRecipes.CRUSHING.get(), in, sl);
    r.ifPresent(h -> { ItemStack out = h.value().assemble(in); ResourceKey<Recipe<?>> id = h.id(); });
}
sl.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id));              // Optional<RecipeHolder<?>> (FB)
sl.recipeAccess().recipeMap().byType(ModRecipes.CRUSHING.get());                 // Collection<RecipeHolder<T>> (FB/MEK)
sl.getServer().getRecipeManager().getRecipeFor(type, in, sl, @Nullable RecipeHolder hint);   // [V] NF-Test
```

- `RecipeManager.createCheck(type)` liefert `CachedCheck` [V] (Furnace-Patch). Die genaue `getRecipeFor`-Signatur von `CachedCheck` ist **UNVERIFIZIERT**.
- Der Client bekommt Rezepte nicht automatisch. Senden mit `OnDatapackSyncEvent#sendRecipes(RecipeType<?>...)` (Game-Bus), empfangen mit `RecipesReceivedEvent#getRecipeMap()` (Client) [V] FB.
- JSON liegt unter `data/<ns>/recipe/<name>.json` mit `"type": "<ns>:crushing"` (Registry `Registries.RECIPE`). **UNVERIFIZIERT** ist das genaue JSON-Format von `ItemStackTemplate` (vermutlich `{"id":…, "count":…}`) und von `Ingredient`.

---

## 7. GameTests (NeoForge 26.3)

[V] Quellen:

- `nf/event/RegisterGameTestsEvent.java` (Mod-Bus): `registerEnvironment(Identifier, TestEnvironmentDefinition<?>...)` liefert `Holder`; `registerTest(Identifier, GameTestInstance)`
- `nf/gametest/GameTestHooks.java`
- `tf/impl/GameTestRegistration.java`: `TestData` mit 12 Argumenten
- FAB `fabric-gametest-api-v1/.../TestAnnotationLocator.java`: `FunctionGameTestInstance(ResourceKey<Consumer<GameTestHelper>>, TestData)`, `Registries.TEST_FUNCTION`
- MDK `build.gradle`: Run-Typ

```java
public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRUSHER_SMOKE =
        TEST_FUNCTIONS.register("crusher_smoke", () -> SwGameTests::crusherSmoke);

@SubscribeEvent // Mod-Bus
static void onRegisterTests(RegisterGameTestsEvent event) {
    Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(id("default"));   // leeres AllOf, wie im testframework
    event.registerTest(id("crusher_smoke"), new FunctionGameTestInstance(
            CRUSHER_SMOKE.key(),                       // DeferredHolder ist ein Holder.Reference -> key() [V]; NICHT getKey() (Doku-Fehler)
            new TestData<>(env, Level.OVERWORLD,       // Dimension: neu in 26.3
                    id("empty"),                       // Struktur: data/<ns>/structure/empty.nbt
                    200, 0, true,                      // maxTicks, setupTicks, required
                    Rotation.NONE, false,              // rotation, manualOnly
                    1, 1, false, 0)));                 // maxAttempts, requiredSuccesses, skyAccess, padding (neu in 26.1)
}

static void crusherSmoke(GameTestHelper helper) {
    BlockPos pos = new BlockPos(1, 1, 1);                                  // relativ zur Struktur
    helper.setBlock(pos, ModBlocks.CRUSHER.get());                         // auch (x,y,z, Block|BlockState)
    CrusherBlockEntity be = helper.getBlockEntity(pos, CrusherBlockEntity.class);
    helper.assertTrue(be != null, "BE fehlt");                             // (boolean, String) und (boolean, Component)
    helper.runAfterDelay(20, () -> helper.assertValueEqual(be.progress(), 0, "progress"));
    helper.succeedWhen(() -> helper.assertBlockPresent(ModBlocks.CRUSHER.get(), pos));
}
```

- **GameTestHelper** [V] (FAB-Testmods und NF-Tests; alles, was `ExtendedGameTestHelper` überschreibt, ist Vanilla):
  - `succeed()`, `succeedIf(Runnable)`, `succeedWhen(Runnable)`, `fail(String)`
  - `runAfterDelay(n, Runnable)`, `runAtTickTime(n, Runnable)`
  - `assertTrue` / `assertFalse(boolean, String)`, `assertValueEqual(T, T, String)`, `assertBlockPresent(Block, BlockPos)`, `assertBlock(BlockPos, Predicate<Block>, Function<Block, Component>)`
  - `getBlockState(BlockPos)`, `getLevel()` (`ServerLevel`), `absolutePos(BlockPos)`
  - `makeMockPlayer(GameType)`, `spawnItem`
  - `startSequence()` mit `thenExecute(Runnable)`, `thenExecuteAfter(int, Runnable)`, `thenWaitUntil(Runnable)`, `thenIdle(int)`, `thenSucceed()`
- **Struktur:** Pflicht ist ein Template unter `data/<modid>/structure/<name>.nbt` (Vanilla-`StructureTemplateManager`). NeoForge liefert **keine** leere Struktur und lädt **kein** SNBT aus Ressourcen. Fabric hat dafür einen eigenen Mixin und `fabric-gametest-api-v1:empty`.
  - Inhalt und Format einer selbst erzeugten `.nbt` (gzip-NBT mit `DataVersion`, `size`, `palette`, `blocks`, `entities`) sind **UNVERIFIZIERT**. Am sichersten erzeugt man sie einmal im Spiel mit `/test create` bzw. einem Strukturblock.
  - Alternative [V]: Mekanism 26.3 nutzt `net.neoforged:testframework:${neo_version}` (`gameTestImplementation`, `transitive = false`). Das bringt `@EmptyTemplate`, `@GameTest`, `@TestHolder` und `ExtendedGameTestHelper` mit, verlangt aber ein Framework-Setup (MEK `src/gameTest/.../MekanismTests.java`).
- **Gradle (ModDevGradle 2.0.148)** [V] MDK und MEK:

  ```groovy
  neoForge { runs { gameTestServer { type = "gameTestServer"; gameDirectory = project.file('runs/gametest') } } }
  ```

  - Aufruf: `./gradlew :neoforge:runGameTestServer`.
  - Ohne registrierte Tests stürzt der Server ab (MDK-Kommentar).
  - GameTests sind aktiv, wenn nicht Produktion und eines davon gilt: IDE, GameTestServer oder `-Dneoforge.enableGameTest=true` (`GameTestHooks`).
  - Die Property `neoforge.enabledGameTestNamespaces` wird im 26.3-Quellcode **nicht mehr ausgewertet** (0 Treffer). Schadet aber nicht.
  - In GitHub Actions schreibt NeoForge bei Fehlern eine `::error`-Annotation (Patch `GameTestServer`).

---

## 8. Reload-Listener und Netzwerk-Payloads

**Reload-Listener** [V]

Quellen: `nf/event/AddServerReloadListenersEvent.java` (Game-Bus `NeoForge.EVENT_BUS`; `addListener(Identifier, PreparableReloadListener)` aus `SortedReloadListenerEvent`, `addDependency(Identifier, Identifier)`, `getServerResources()`), `nft/debug/resources/LoadingConditionsTest.java`, FB `FramingSawRecipeCache.java`.

```java
NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> e.addListener(
        Identifier.fromNamespaceAndPath(MODID, "planets"),
        new SimpleJsonResourceReloadListener<>(PlanetDef.CODEC, FileToIdConverter.json("sternenwerft/planets")) {
            @Override protected void apply(Map<Identifier, PlanetDef> data, ResourceManager rm, ProfilerFiller profiler) { /* ... */ }
        }));
// Einfacher: record R(ReloadableServerResources res) implements ResourceManagerReloadListener { onResourceManagerReload(ResourceManager) }
```

Imports:

- `net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener`
- `net.minecraft.resources.FileToIdConverter`
- `net.minecraft.util.profiling.ProfilerFiller`
- `net.minecraft.server.packs.resources.ResourceManagerReloadListener`

**Payloads** [V]

Quellen:

- `nf/network/event/RegisterPayloadHandlersEvent.java` (Mod-Bus, `registrar(String version)`)
- `nf/network/registration/PayloadRegistrar.java`: `playToServer`, `playToClient` (mit und ohne Handler), `playBidirectional`, `versioned`, `optional`, `executesOn`. Standard ist `HandlerThread.MAIN`, man braucht also kein `enqueueWork`.
- `nf/network/PacketDistributor.java`
- `nfc/network/ClientPacketDistributor.java`
- `nfc/network/event/RegisterClientPayloadHandlersEvent.java`
- IC `common/network/TopStacksSyncPacket.java`

```java
public record SetModePayload(BlockPos pos, int mode) implements CustomPacketPayload {
    public static final Type<SetModePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MODID, "set_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetModePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetModePayload::pos, ByteBufCodecs.VAR_INT, SetModePayload::mode, SetModePayload::new);
    @Override public Type<SetModePayload> type() { return TYPE; }
}
// Alternative: CustomPacketPayload.codec(Payload::write, Payload::new) (IC); leere Payloads: StreamCodec.unit(INSTANCE) (NF-Test)

static void onPayloads(RegisterPayloadHandlersEvent e) {             // Mod-Bus
    PayloadRegistrar r = e.registrar("1");
    r.playToServer(SetModePayload.TYPE, SetModePayload.STREAM_CODEC, (payload, ctx) -> {
        if (ctx.player() instanceof ServerPlayer sp) { /* Server entscheidet: Reichweite/Berechtigung prüfen */ }
    });
    r.playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC);       // Client-Handler separat:
}
// Client (Mod-Bus): RegisterClientPayloadHandlersEvent#register(SyncPayload.TYPE, (p, ctx) -> ...)
// Senden: PacketDistributor.sendToPlayer(serverPlayer, payload) | sendToPlayersTrackingChunk(level, chunkPos, p) | sendToAllPlayers(p)
//         ClientPacketDistributor.sendToServer(payload)   (nur Client-Code)
// IPayloadContext: player(), reply(payload), enqueueWork(Runnable), disconnect(Component)
```

---

## 9. Brenndauer eines Items (26.3)

[V] Quellen:

- `pat/world/level/block/entity/AbstractFurnaceBlockEntity.java.patch` und `BaseContainerBlockEntity.java.patch`
- MEK `common/util/MekanismUtils.java#getBurnTime`
- FAB `fabric-content-registries-v0/.../ContentRegistryTest.java`: `new CookingFuel(ResourceKey<ContextIntProvider>, ResourceKey<ContextFloatProvider>)`
- PRI 26.3 „Cooking Fuel Component“

Ob ein Item Brennstoff ist, steht nur in **`DataComponents.COOKING_FUEL`** (`net.minecraft.world.item.component.CookingFuel`: `burnTime()` und `speedMultiplier()` als `ResolvableInt`/`ResolvableFloat`). Den Wert braucht man aufgelöst gegen einen `LootContext` mit `LootContextParamSets.CONTAINER_PROCESS`. Vanilla halbiert ihn z. B. in Räucherofen und Schmelzofen über Bedingungen.

```java
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;   // [V]
import net.minecraft.world.entity.SlotProvider; import net.minecraft.world.entity.SlotAccess;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;

private static final SlotProvider NO_SLOTS = new SlotProvider() {
    @Override public @Nullable SlotAccess getSlot(int slot) { return null; }
};

public static int burnTime(ServerLevel level, BlockEntity be, ItemStack fuel) {
    LootContext ctx = new LootContext.Builder(new LootParams.Builder(level)
            .withParameter(LootContextParams.BLOCK_STATE, be.getBlockState())
            .withParameter(LootContextParams.BLOCK_ENTITY, be)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(be.getBlockPos()))
            .withParameter(LootContextParams.CONTAINER, NO_SLOTS)                       // Typ SlotProvider
            .withOptionalParameter(NeoForgeLootContextParams.QUERIED_STACK, fuel.isEmpty() ? null : fuel)
            .create(LootContextParamSets.CONTAINER_PROCESS)).create(Optional.empty());
    return ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, ctx, 0);   // 0 = kein Brennstoff
}
// Schnelltest ohne Kontext: fuel.has(DataComponents.COOKING_FUEL) / itemResource.has(DataComponents.COOKING_FUEL) (MEK)
// Geschwindigkeit: ResolvableFloat.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::speedMultiplier, ctx, 1.0F)
```

- Wer von `BaseContainerBlockEntity` erbt, kann einfach `getLootContext(serverLevel, fuelStack)` (protected, NeoForge-Overload) benutzen. So macht es der Ofen.
- Das Paket von `ResolvableFloat` (`…providers.number.floats`) folgt nur aus [P] und dem Patch. Ein eigener Import wurde nicht gesehen, also **UNVERIFIZIERT**.
- `ResolvableInt.Constant(int)` und `ResolvableFloat.Constant(float)` für eigene Brennstoffe stehen nur in [P]. Verifiziert ist der Konstruktor mit Registry-Keys, z. B. `new CookingFuel(ContextIntProviders.COOKING_TIME_BAMBOO, ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER)`.
- `IItemStackExtension#getBurnTime(...)` gibt es in NeoForge 26.3 nicht mehr (0 Treffer).

---

## Offen / UNVERIFIZIERT (Zusammenfassung)

- `addStandardInventorySlots(Inventory,int,int)`, `addDataSlots(ContainerData)`, `SimpleContainerData`, `ContainerData#set/getCount`: nur [D] (Stand 26.1). Belegt sind nur `addDataSlot(DataSlot.standalone())` und manuelle Spieler-Slots.
- Binärformat und Pflichtfelder einer selbst erzeugten leeren GameTest-Struktur (`.nbt`). In 26.3 gibt es keine eingebaute leere Struktur.
- JSON-Form von `ItemStackTemplate` und `Ingredient` in Rezept-Dateien. Genaue Signatur von `RecipeManager.CachedCheck#getRecipeFor`.
- `ResolvableInt.Constant` und `ResolvableFloat.Constant` sowie das Paket von `ResolvableFloat`.
- `GuiGraphicsExtractor#text`-Overload mit `boolean shadow`. Gesehen wurde nur `text(Font, Component|String, int, int, int)`.
