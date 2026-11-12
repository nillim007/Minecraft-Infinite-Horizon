# API-Spickzettel: Entities, Fahrzeuge, Dimensionswechsel mit Passagieren, Rendering in Minecraft 26.3 (Mojang-Namen, Java 25)

Stand: 2026-11-12. Ergänzt `api-neoforge-26.3.md`, `api-fabric-26.3.md`, `api-dimensions-26.3.md`. Gebraucht für M4d/M4e (27.7): `rocket` (12.4, 12.5), `seat`, `probe_entity` (12.3).

**Markierungen:** **[J]** = per `javap` aus `minecraft_26.3_client.jar` gelesen (Signaturen, bei Verhalten teils per `javap -c`). **[NF]** = NeoForge 26.3.0.102 (universal-Jar). **[FAB]** = Fabric API (`fabric-rendering-v1` 27.3.0, `fabric-entity-events-v1` 6.0.4). **UNVERIFIZIERT** = nicht prüfbar, vor Gebrauch im GameTest ausprobieren.
Vorbilder in Vanilla: `world.entity.vehicle.boat.AbstractBoat` (Sitz, Passagier, Interaktion), `vehicle.VehicleEntity`, `client.renderer.entity.FallingBlockRenderer`, `DisplayRenderer$BlockDisplayRenderer` (Block zeichnen).

## 0. Die größten Änderungen gegenüber 1.21.x

1. **`EntityType.Builder#build` braucht einen `ResourceKey<EntityType<?>>`** (Name, Beschreibungs-ID und Loot-Tabelle leiten sich daraus ab). [J]
2. **Entity-Rendering läuft über Render-States:** `EntityRenderer<T, S extends EntityRenderState>`, `createRenderState()`, `extractRenderState(T, S, float)` (Server-/Spielwerte → State) und `submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)` (zeichnen). Es gibt kein `render(T, …, MultiBufferSource, …)` mehr. [J]
3. **Speichern über `ValueInput`/`ValueOutput`:** `readAdditionalSaveData(ValueInput)`, `addAdditionalSaveData(ValueOutput)`. [J]
4. **Schaden:** `hurtServer(ServerLevel, DamageSource, float)` ist **abstrakt**; es gibt kein `hurt(DamageSource, float)` zum Überschreiben. [J]
5. **`Entity#interact(Player, InteractionHand, Vec3)`** hat als drittes Argument die Trefferposition; Rückgabe `InteractionResult` (`SUCCESS`, `SUCCESS_SERVER`, `CONSUME`, `FAIL`, `PASS`, `TRY_WITH_EMPTY_HAND`). [J]
6. **`Entity#teleport(TeleportTransition)`** ersetzt `changeDimension`; bei Dimensionswechsel entsteht eine **neue Entity-Instanz** (Abschnitt 4). [J]
7. **Eine nicht speicherbare Entity (`noSave()`) kann man nicht reiten** (Abschnitt 3). [J]
8. `ResourceLocation` heißt `Identifier`, Block-Rendering geht über `BlockModelRenderState`/`MovingBlockRenderState` (Abschnitt 5).

## 1. EntityType registrieren [J]

```java
public static final Supplier<EntityType<RocketEntity>> ROCKET = Services.REGISTRY.register(
    BuiltInRegistries.ENTITY_TYPE, "rocket",
    () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
        .sized(1.0f, 4.0f)                 // Breite, Höhe (Kollisionsbox, 12.5: 1 x 1 x Raketenhöhe)
        .clientTrackingRange(10)           // Chunks
        .updateInterval(2)                 // Ticks zwischen Positionspaketen
        .fireImmune()
        .build(ResourceKey.create(Registries.ENTITY_TYPE, InfiniteHorizon.id("rocket"))));
```

| Builder-Methode | Bedeutung |
|---|---|
| `of(EntityFactory<T>, MobCategory)` | Fabrik `(EntityType<T>, Level) -> T` (= Konstruktor `Entity(EntityType<?>, Level)`); Kategorie `MISC` für Fahrzeuge, `createNothing(MobCategory)` für Platzhalter |
| `sized(w, h)`, `eyeHeight(f)` | Box und Augenhöhe |
| `passengerAttachments(float...)` / `(Vec3...)`, `vehicleAttachment(Vec3)`, `ridingOffset(float)`, `attach(EntityAttachment, x, y, z)` | Sitzpunkte; ersetzen `getPassengersRidingOffset`. `passengerAttachments(1.2f)` = ein Sitz in 1,2 Blöcken Höhe |
| `clientTrackingRange(int)`, `updateInterval(int)`, `noUpdateInterval()`, `dontTrackDeltas()` | Sync-Reichweite und -takt |
| `noSave()` | nie auf die Platte (siehe Abschnitt 3: **nicht** für Sitze) |
| `noSummon()` | nicht per `/summon` |
| `fireImmune()`, `immuneTo(TagKey<Block>)`, `canSpawnFarFromPlayer()`, `notInPeaceful()`, `noLootTable()`, `requiredFeatures(...)` | wie bisher |

- Der Schlüssel muss vor `build` bekannt sein. Mit unserem Helfer entsteht er im Supplier aus `InfiniteHorizon.id(name)` (derselbe Name wie bei `register`). NeoForge ruft den Supplier verzögert (DeferredRegister), Fabric sofort; beides funktioniert, solange der Schlüssel zum Registrierungsnamen passt.
- `EntityType` hat `spawn(ServerLevel, BlockPos, EntitySpawnReason)` und `create(Level, EntitySpawnReason)`; Fresh-Entity: `ServerLevel#addFreshEntity(Entity)`, mit Passagieren `tryAddFreshEntityWithPassengers`. [J]
- Nicht-Lebewesen brauchen **keine** Attribute (`EntityAttributeCreationEvent` entfällt).
- Anzeigename: Übersetzungsschlüssel `entity.infinite_horizon.rocket` (`getDescriptionId()`), aus `items.json` bzw. `lang_extra.json`.

## 2. Entity-Basisklasse [J]

Abstrakt sind genau drei Methoden: `defineSynchedData(SynchedEntityData.Builder)`, `readAdditionalSaveData(ValueInput)`, `addAdditionalSaveData(ValueOutput)`, dazu `hurtServer(ServerLevel, DamageSource, float)` (Rückgabe `true` = Schaden angewendet, für Unverwundbare `false`).

```java
public class RocketEntity extends Entity {
    private static final EntityDataAccessor<Integer> PHASE =
        SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);

    public RocketEntity(EntityType<?> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(PHASE, 0); }
    @Override protected void readAdditionalSaveData(ValueInput in) { entityData.set(PHASE, in.getIntOr("phase", 0)); }
    @Override protected void addAdditionalSaveData(ValueOutput out) { out.putInt("phase", entityData.get(PHASE)); }
    @Override public boolean hurtServer(ServerLevel l, DamageSource s, float a) { return false; }
    @Override public boolean isPickable() { return true; }          // Zielbar für interact (Rechtsklick)
    @Override public boolean canBeCollidedWith(Entity other) { return true; } // wie Boot: Spieler stehen auf der Box
    @Override public boolean isPushable() { return false; }
}
```

- **Synchronisierte Daten:** `SynchedEntityData.defineId(Class, EntityDataSerializer<T>)` (Klasse muss `SyncedDataHolder` sein, jede `Entity` ist es), `get`, `set`, `Builder#define(accessor, default)`. Serializer in `EntityDataSerializers`: `BYTE, INT, LONG, FLOAT, STRING, BOOLEAN, COMPONENT, ITEM_STACK, BLOCK_STATE, BLOCK_POS, OPTIONAL_BLOCK_POS, DIRECTION, ROTATIONS, VECTOR3, QUATERNION, OPTIONAL_GLOBAL_POS, POSE` u. a. Reaktion auf Änderung am Client: `onSyncedDataUpdated(EntityDataAccessor<?>)` überschreiben. Keine Liste/Map-Serializer für eigene Typen vorhanden; eigene Strukturen als `STRING` (JSON) oder gar nicht syncen (Abschnitt 6).
- **Speichern:** `ValueOutput#putInt/putBoolean/putIntArray/store(String, Codec, T)`, `ValueInput#getIntOr/getBooleanOr/getStringOr/read(String, Codec)` → `Optional`, `child(String)` → `Optional<ValueInput>`. Inventar/Frachtraum: `store("cargo", Codec, Liste)` mit eigenem Codec oder `ItemStackWithSlot.CODEC` (UNVERIFIZIERT, Name nach Vorbild `ContainerHelper` in 26.3 prüfen).
- **Tick:** `tick()` (ruft `baseTick()`); Server-Teil mit `if (!level().isClientSide())`. Bewegung: `setDeltaMovement(Vec3)` / `setDeltaMovement(x,y,z)`, dann `move(MoverType.SELF, getDeltaMovement())` (kollidiert mit Blöcken) oder direkt `setPos(x,y,z)` ohne Kollision. `noPhysics` (öffentliches Feld) schaltet Kollision ab; `setNoGravity(true)` für Raketen im Flug (`isNoGravity()`). `setPos` setzt die Box neu (`makeBoundingBox`). Gravitation: Entities fallen nur, wenn **du** `getDeltaMovement().add(0, -g, 0)` rechnest (Vanilla-Konstante hat jede Klasse selbst; unser `g` kommt aus 12.9).
- **Boden:** `onGround()`, `setOnGround(boolean)`; vor `move` ist `onGround` aus dem letzten Tick.
- **Interaktion:** `interact(Player, InteractionHand, Vec3)` läuft auf **beiden** Seiten; auf dem Server `player.startRiding(this)` bzw. `player.openMenu(...)`, Rückgabe `InteractionResult.SUCCESS_SERVER` bzw. `CONSUME`. Rechtsklick von außen mit Spieler im Sneak: Vanilla ruft `interact` nur, wenn der Spieler nicht schleicht oder die Hand leer ist (UNVERIFIZIERT, `Player#interactOn(Entity, InteractionHand, Vec3)` in `api` prüfen). `skipAttackInteraction(Entity)` gibt `true` zurück, wenn Linksklick nicht schlagen soll; `isAttackable()` `false` für Sitz und Sonde.
- **Menü aus einer Entity:** `HasCustomInventoryScreen#openCustomInventoryScreen(Player)` (Pferd, Kistenboot) wird von der Inventartaste im Sitz ausgelöst. Unser Menü öffnet `IRegistryHelper#openMenu(ServerPlayer, MenuProvider, D, StreamCodec)` mit der Entity-ID in `D` (siehe 12.5, „Rampen-Position bzw. Entity-ID“).
- **Entfernen:** `discard()` (final) für stilles Entfernen (Sonde nach `ascent_ticks`), `kill(ServerLevel)` für Zerstörung.
- **Immer ticken:** `isAlwaysTicking()` (Standard `false`). Eine Rakete im Flug ist durch den Spieler als Passagier geladen; die Sonde ohne Passagier nicht: Chunk-Ticket (Abschnitt 4) oder `isAlwaysTicking() = true` (UNVERIFIZIERT, ob der Chunk dann gehalten wird).

## 3. Passagiere und Sitze [J]

| Methode | Hinweis |
|---|---|
| `Entity#startRiding(Entity)` / `(Entity, boolean force, boolean sendEventAndTriggers)` | `force` überspringt `canRide`/`canAddPassenger`. Rückgabe `false` bei Fehlschlag |
| `protected boolean canAddPassenger(Entity)` | Standard `getPassengers().isEmpty()`; für 2 Sitze `getPassengers().size() < n` überschreiben (Boot: `getMaxPassengers()`) |
| `protected boolean canRide(Entity)` | gilt für den Fahrer (z. B. nur Spieler) |
| `protected void addPassenger(Entity)` / `removePassenger(Entity)` | Hooks; hier Sitzplatz zuordnen |
| `Entity#getPassengerRidingPosition(Entity)` / `protected getPassengerAttachmentPoint(Entity, EntityDimensions, float)` | Ort des Passagiers; steuert `EntityType.Builder#passengerAttachments`. Mehrere Sitze: eigene Punkte je Index |
| `protected void positionRider(Entity, Entity.MoveFunction)` | pro Tick; überschreiben für Offsets pro Sitz (Boot: `getSinglePassengerXOffset`) |
| `getControllingPassenger()` → `LivingEntity` | wer lenkt; für Rakete `null` (Flug ist Server-Skript) |
| `getDismountLocationForPassenger(LivingEntity)` | Ausstiegsposition (öffentlich) |
| `ejectPassengers()`, `Entity#stopRiding()`, `removeVehicle()`, `getVehicle()`, `getRootVehicle()`, `getPassengers()`, `getFirstPassenger()`, `isVehicle()`, `isPassenger()` | wie gehabt |
| `ServerPlayer#startRiding(Entity, boolean, boolean)` | eigene Überladung; sendet Mount-Paket |
| `Entity#dismountTo(double,double,double)` | Absteigen mit Zielposition |

- **Sitz-Entity ohne Speichern:** `noSave()` im Builder verhindert das Aufsteigen: `startRiding` bricht serverseitig mit `false` ab, wenn `vehicle.getType().canSerialize()` falsch ist (Bytecode: `aload_1.type.canSerialize()`, nur wenn `!level().isClientSide()`). Daher beim `seat` **kein `noSave()`**, sondern `shouldBeSaved()` auf `false` überschreiben (öffentlich, Vanilla prüft dort `removalReason.shouldSave()`, `isPassenger()`, `isVehicle()`, `hasExactlyOnePlayerPassenger()`). Dasselbe für die Rakete, solange Spieler in ihr sitzen sollen. (Spezifikation 12.5 „wird nicht gespeichert“: so umsetzen; in `spec-gaps.md` vermerken.)
- Unsichtbarer Sitz: Renderer, der nichts zeichnet (Abschnitt 5); `isInvisible()` allein blendet Passagiere nicht aus. Keine Kollision: `canBeCollidedWith` `false`, `isPushable` `false`, `isPickable` `false`, `noPhysics = true`, `setNoGravity(true)`.
- **Passagier mit Rakete bewegen:** Die Rakete setzt ihre Position; `positionRider` zieht den Passagier nach (Server: `Entity.rideTick`, Client genauso). Kein Eingriff nötig.
- **Absteigen verhindern (12.5: „Absteigen mit Schleichen; gesperrt vom Countdown bis zur Landung“):** Es gibt in 26.3 **keinen Fahrzeug-Hook** dafür. `Player#rideTick()` ruft `wantsToStopRiding()` (= `LivingEntity#isShiftKeyDown()`, `protected`, nicht überschrieben) und dann `stopRiding()`; Server-Seite, jeden Tick. Wege:
  1. **NeoForge [NF]:** `EntityMountEvent` (`ICancellableEvent`; `isDismounting()`, `getEntityMounting()`, `getEntityBeingMounted()`) → bei Flugphase `setCanceled(true)`. Bus: Spiel-Event-Bus (`NeoForge.EVENT_BUS`).
  2. **Fabric:** kein Mount-Ereignis in `fabric-entity-events-v1` (nur Elytra, Schlaf, Tod, Laden/Entladen; Liste per `jar tf` gesehen). Zwei Notlösungen: (a) Mixin auf `Entity#stopRiding` mit Abbruch, wenn `getVehicle() instanceof RocketEntity r && r.isLocked()`; (b) im `RocketEntity#tick()` (läuft **vor** den Passagier-Ticks, `ServerLevel#tickNonPassenger`/`tickPassenger`) `passenger.setShiftKeyDown(false)` für alle Spieler-Passagiere in der Sperrphase. (b) ist UNVERIFIZIERT (Reihenfolge und ob der Client die Taste erneut sendet).
  3. Gemeinsam über `common/`: ein kleines Mixin auf `Entity#stopRiding()` funktioniert für beide Loader und ist die einfachste Lösung; Bedingung als reine Funktion (`FlightPhase#dismountLocked`).
- Spieler-Dismount sonst: `Entity#getDismountLocationForPassenger`, `LivingEntity#getDismountPoses()`.

## 4. Dimensionswechsel mit Passagieren [J]

`Entity#teleport(TeleportTransition)` → `Entity` (Rückgabe ist bei Dimensionswechsel eine **neue** Instanz, `null` bei Fehlschlag):

```java
TeleportTransition t = new TeleportTransition(targetLevel, new Vec3(x, y, z), Vec3.ZERO, yRot, xRot,
        TeleportTransition.DO_NOTHING);       // (ServerLevel, Vec3 pos, Vec3 delta, float yRot, float xRot, PostTeleportTransition)
Entity moved = rocket.teleport(t);            // nimmt Passagiere mit
```

Konstruktoren: `(ServerLevel, Vec3, Vec3, float, float, PostTeleportTransition)`, mit zusätzlichem `Set<Relative>`, und mit `boolean missingRespawnBlock, boolean asPassenger`. Post-Aktionen: `DO_NOTHING`, `PLAY_PORTAL_SOUND`, `PLACE_PORTAL_TICKET`. `PostTeleportTransition#onTransition(Entity)` ist ein Funktionsinterface (eigene Lambda für „Landung beginnen“). `withPosition`, `withRotation`, `transitionAsPassenger()`.

Ablauf in `Entity#teleportCrossDimension` (per Bytecode gelesen):
1. Passagiere werden `ejectPassengers()`-t und **einzeln** mit `calculatePassengerTransition` (= `transitionAsPassenger()`) selbst teleportiert (Spieler über `ServerPlayer#teleport`, mit Respawn-Paket; sie steigen dabei nicht aus der Zieldimension).
2. Ist das Fahrzeug nicht Spieler: neue Instanz `getType().create(level, ...)`, `restoreFrom(alt)` (Speichern und Laden über `saveWithoutId`/`load`; Position, Portal-Abklingzeit), `removeAfterChangingDimensions()` für die alte, `teleportSetPosition`, `ServerLevel#addDuringTeleport(neu)`.
3. Passagiere `startRiding(neu, true, false)` (erzwungen).
4. `postTeleportTransition().onTransition(neu)`.

Folgen für die Rakete:
- **Alle Felder müssen über `addAdditionalSaveData`/`readAdditionalSaveData` laufen**, sonst gehen sie beim Wechsel verloren (Phase, Treibstoff, Fracht, Besitzer-Agentur, Teileliste). Synchronisierte Daten ebenso (sie werden aus den Speicherdaten neu aufgebaut).
- **Entity-ID ändert sich**, die UUID bleibt (vermutlich, weil sie im Save-Tag steht; UNVERIFIZIERT). Verweise in Daten (`pending_flight`, 3A.4) daher über die UUID, nie über die ID, und nach dem Wechsel die **Rückgabe** von `teleport` weiterverwenden.
- Ein Spieler-Passagier wird nicht aus dem Fahrzeug geworfen; `TeleportTransition(..., asPassenger = true)` gilt nur für die interne Passagierumleitung. Wer den Spieler **allein** schickt, ruft `player.teleport(t)` (stoppt Reiten, wenn `asPassenger` falsch).
- Zielhöhe 200 Blöcke über dem Landepunkt: `Vec3(x, ziel_y, z)`; Landung linear über `landing_ticks` im `tick()` der neuen Instanz (Phase aus `SynchedEntityData`/Speicherdaten).
- **Chunks laden:** Beim Eintreffen sind Chunks am Ziel unter Umständen nicht geladen (`teleport` erzwingt nur den Eintrittschunk). Ticket: `ServerChunkCache#addTicketWithRadius(TicketType, ChunkPos, int)` bzw. `addTicketAndLoadWithRadius(TicketType, ChunkPos, int)` → `CompletableFuture<?>` (wartet auf Laden), Gegenstück `removeTicketWithRadius`. `ServerLevel#setChunkForced(int, int, boolean)` / `ServerChunkCache#updateChunkForced(ChunkPos, boolean)` sind dauerhaft (`TicketType.FORCED`).
  `TicketType`-Konstanten: `PLAYER_SPAWN, SPAWN_SEARCH, DRAGON, PLAYER_LOADING, PLAYER_SIMULATION, FORCED, PORTAL, ENDER_PEARL, UNKNOWN`. Gelesen: `PORTAL` läuft nach **300 Ticks** ab und lädt und simuliert (Flags laden, simulieren, Dimension aktiv halten, speichern); `ENDER_PEARL` 40 Ticks, laden und simulieren; `FORCED` ohne Ablauf. Für `landing_ticks` (200 t, 12.5) passt `PORTAL` mit Radius 2–3; erneuern per erneutem Aufruf (`Ticket#resetTicksLeft` ist öffentlich). Eigener `TicketType` per Konstruktor `(long timeout, int flags)` ist öffentlich, die Registrierung geschieht in einer privaten Methode: eigener Typ UNVERIFIZIERT (Persistenz).
- **Vor dem Teleport:** Zielchunk laden: `level.getChunk(x >> 4, z >> 4)` blockiert den Server-Thread (für M4 akzeptabel, einmalig); besser `addTicketAndLoadWithRadius(...).thenRun(...)` auf dem Server-Thread (`MinecraftServer#execute`).
- Dimensionsereignisse: NeoForge [NF] `EntityTravelToDimensionEvent` (abbrechbar, vor dem Wechsel); Fabric: `ServerEntityWorldChangeEvents` UNVERIFIZIERT (Klasse nicht gefunden in `fabric-entity-events-v1`, eventuell in `fabric-dimensions-v1`).

## 5. Rendering (Client) [J]

```java
public class RocketRenderer extends EntityRenderer<RocketEntity, RocketRenderState> {
    private final BlockModelResolver blocks;
    public RocketRenderer(EntityRendererProvider.Context ctx) { super(ctx); blocks = ctx.getBlockModelResolver(); }
    @Override public RocketRenderState createRenderState() { return new RocketRenderState(); }
    @Override public void extractRenderState(RocketEntity e, RocketRenderState s, float partialTick) {
        super.extractRenderState(e, s, partialTick);        // x, y, z, lightCoords, ageInTicks, ...
        s.yRot = e.getYRot(); s.phase = e.getPhase();
        // Teile als BlockModelRenderState vorbereiten (einmal pro Teil und Frame):
        blocks.update(s.parts.get(i), partState, BlockDisplayContext.create());
    }
    @Override public void submit(RocketRenderState s, PoseStack ps, SubmitNodeCollector c, CameraRenderState cam) {
        ps.pushPose();
        // je Teil: ps.translate(0, i, 0);
        s.parts.get(i).submit(ps, c, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);   // (PoseStack, Collector, int light, int overlay, int outline)
        ps.popPose();
        super.submit(s, ps, c, cam);                        // Namensschild, Schatten
    }
}
public class RocketRenderState extends EntityRenderState {
    public final List<BlockModelRenderState> parts = new ArrayList<>();
    public float yRot; public int phase;
}
```

| Baustein | Signatur |
|---|---|
| `EntityRenderer<T, S>` | `abstract S createRenderState()`, `void extractRenderState(T, S, float)`, `void submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)`, `boolean shouldRender(T, Frustum, double, double, double, float)`, `AABB getBoundingBoxForCulling(T, float)`, `float getShadowRadius(S)` (`shadowRadius` Feld) |
| `EntityRenderState` | Felder `entityType, x, y, z, ageInTicks, boundingBoxWidth, boundingBoxHeight, eyeHeight, lightCoords, isInvisible, nameTag, passengerOffset, shadowRadius` u. a. |
| `EntityRendererProvider<T>` | `EntityRenderer<T, ?> create(Context)` |
| `Context` | `getBlockModelResolver()`, `getItemModelResolver()`, `getModelSet()`, `bakeLayer(ModelLayerLocation)`, `getFont()`, `getSprites()`, `getAtlas(Identifier)` |
| Block zeichnen (Einzelblock) | `BlockModelResolver#update(BlockModelRenderState, BlockState, BlockDisplayContext)`, dann `BlockModelRenderState#submit(PoseStack, SubmitNodeCollector, int light, int overlay, int outline)` (Vorbild `DisplayRenderer$BlockDisplayRenderer`). `BlockModelRenderState#clear()` vor erneutem Befüllen |
| Block „in der Welt“ (Fallender Block) | `MovingBlockRenderState` (`blockPos`, `blockState`, `biome`, `cardinalLighting`, `lightEngine`, `randomSeedPos`) + `SubmitNodeCollector#submitMovingBlock(PoseStack, MovingBlockRenderState, int)`; Vorbild `FallingBlockRenderer` |
| Eigene Modelle | `SubmitNodeCollector#submitModel(Model, S, PoseStack, RenderType|Identifier, int light, int overlay, int outline)`, `submitModelPart(ModelPart, PoseStack, RenderType, int, int, UvMapping)`, `submitCustomGeometry(PoseStack, RenderType, CustomGeometryRenderer)`, `submitBlockModel(...)` |
| Model-Layer | `new ModelLayerLocation(Identifier, String)`; `LayerDefinition.create(MeshDefinition, texW, texH)`; `bakeLayer(...)` im Renderer-Konstruktor |

- **Rakete aus Block-Zuständen:** Pro Teil ein `BlockModelRenderState` im Render-State (Anzahl wechselt, deshalb Liste pflegen), `update(...)` mit dem `BlockState` des Teils (Kapsel, Tank, Triebwerk, Flossen, Frachtraum). Die Teileliste muss am Client bekannt sein: als synchronisierte `STRING`/`INT`-Daten (z. B. Kodierung der Stufe + Zahl Tanks + Fracht ja/nein), nicht als Block-Entity-Daten. Flossen sind nur Darstellung (12.5): vier gedrehte `BlockState`s neben dem Triebwerk. `BlockDisplayContext` nur über `create()`.
- **Unsichtbarer Sitz:** `submit` leer lassen; `createRenderState()` liefert `new EntityRenderState()`. Registrierung trotzdem nötig (Vanilla meldet fehlende Renderer als Fehler).
- **Sonde (einfache Animation):** Zustand aus `ageInTicks` (Feld im State) ableiten, z. B. Drehung `ps.mulPose(Axis.YP.rotationDegrees(s.ageInTicks * 4))`; keine eigene Interpolation nötig.
- **Registrierung je Loader:**
  - **NeoForge [NF]:** im Mod-Bus `EntityRenderersEvent.RegisterRenderers#registerEntityRenderer(EntityType<? extends T>, EntityRendererProvider<T>)`; Layer: `EntityRenderersEvent.RegisterLayerDefinitions#registerLayerDefinition(ModelLayerLocation, Supplier<LayerDefinition>)`.
  - **Fabric [FAB]:** `EntityRendererRegistry.register(EntityType<? extends E>, EntityRendererProvider<E>)` (`fabric-rendering-v1`); Layer: `ModelLayerRegistry.registerModelLayer(ModelLayerLocation, TexturedLayerDefinitionProvider)` (`createLayerDefinition()`).
  - Gemeinsam: Renderer-Klassen in `common/.../client/`, nur die Registrierung in `InfiniteHorizonFabricClient`/`InfiniteHorizonNeoForgeClient`. Der Vanilla-Aufruf `EntityRenderers.register` ist `private`.
- **Texturen:** Für Block-Rendering keine eigene Textur nötig (Block-Modelle der Teile). Eigene Modelle brauchen `assets/<ns>/textures/entity/...` (Platzhalter über `tools/gen_placeholders.py`).

## 6. Netzwerk [J]

- **Spawn:** `Entity#getAddEntityPacket(ServerEntity)` liefert `ClientboundAddEntityPacket(Entity, ServerEntity)` (Standard); Client: `recreateFromPacket(ClientboundAddEntityPacket)`. Nichts zu überschreiben, solange alle Daten in `SynchedEntityData` stehen. Nach dem Spawn schickt der Server die nicht-standardmäßigen `SynchedEntityData`-Werte automatisch (`ServerEntity#sendPairingData`).
- **Eigene Felder:** `EntityDataAccessor` (Abschnitt 2). Änderung nur über `entityData.set(...)` auf dem **Server**; `set` markiert „dirty“, `ServerEntity#sendChanges` sendet beim nächsten Tracker-Tick. Große oder seltene Daten (Teileliste, Fracht-Inhalt): eigenes Payload beim Öffnen des Menüs (Menü-Zusatzdaten, 12.5), nicht per Entity-Daten.
- **Position:** `ServerEntity` sendet Deltas im `updateInterval`; mit `noUpdateInterval()`/hoher `updateInterval` wirkt eine schnelle Rakete ruckelig. Für Aufstieg (600 t) `updateInterval(1)` setzen. Client-Interpolation: `moveOrInterpolateTo(...)` (final), `InterpolationHandler` (`createInterpolationHandler()` überschreibbar). Beim Teleport kein Eingriff nötig.
- **Flugsequenz an den Spieler:** `FlightSequencePayload` an den Passagier (Spieler-Netzwerk), nicht über die Entity.

## 7. Kamera und Steuerung des Passagiers

- Die Kamera folgt dem **Spieler**; der sitzt per `positionRider` an der Rakete. Ein `Minecraft#setCameraEntity(Entity)` ist daher **nicht** nötig (Methode existiert: `getCameraEntity()`, `setCameraEntity(Entity)`; für Sonden-Ansicht denkbar, `ServerPlayer#setCamera(Entity)` ist die Server-Variante mit Paket).
- Dritte Person während der Sequenz: `Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK)`, danach wieder den alten Wert (`getCameraType()` vorher merken); Enum `FIRST_PERSON, THIRD_PERSON_BACK, THIRD_PERSON_FRONT`. Beben: Versatz im Client (kleiner Zufallsoffset, Quelle für Kamera-Event je Loader: UNVERIFIZIERT; einfach über `GuiGraphics`-Overlay oder `ViewportEvent.ComputeCameraAngles` bei NeoForge).
- **Steuerung:** Rakete hat keinen Fahrer (`getControllingPassenger() == null`); Eingaben (Start, Skip) laufen über Payloads (`RocketActionPayload`).
- Ob Bewegungstasten im Sitz ohne Fahrer wirkungslos bleiben, ist UNVERIFIZIERT (im Client-Test prüfen).

## 8. Stolperstellen gegenüber 1.21.x

1. `noSave()` am Sitz verhindert `startRiding` (Abschnitt 3); stattdessen `shouldBeSaved()` überschreiben.
2. Dimensionswechsel erzeugt eine neue Instanz: Referenzen nicht halten, Daten komplett speichern, die Rückgabe von `teleport` benutzen.
3. Kein Hook gegen Schleichen-Absteigen außer NeoForge-`EntityMountEvent` oder Mixin.
4. `EntityType.Builder#build` ohne Schlüssel kompiliert nicht; der Name muss zu `register` passen.
5. `hurt` heißt `hurtServer(ServerLevel, …)`; Client-seitig `hurtClient` nur bei `VehicleEntity`. Wer von `VehicleEntity` erbt, bekommt Schadenanzeige und `getDropItem()` (abstrakt) mit; für Raketen besser direkt von `Entity`.
6. Renderer: keine `render`-Methode mehr; Werte vorab in den State schreiben (nicht in `submit` auf die Entity zugreifen; `submit` bekommt nur den State).
7. `EntityRenderers.register` ist privat: je Loader registrieren (Abschnitt 5).
8. `interact` hat drei Parameter; `InteractionResult.SUCCESS_SERVER` statt `sidedSuccess`.
9. Passagier-Reihenfolge: `tickNonPassenger` (Rakete) vor `tickPassenger` (Spieler).
10. Server-Chunks am Ziel: ohne Ticket kann die Rakete in nicht simulierten Chunks hängen bleiben.

## Offene Fragen für M4d/M4e

- **Absteigen sperren:** Mixin auf `Entity#stopRiding` (gemeinsam) oder je Loader (NeoForge `EntityMountEvent`, Fabric Mixin / `setShiftKeyDown(false)`)? Im GameTest prüfen, dass Schleichen im Countdown nicht aussteigt.
- **UUID nach `teleport`:** bleibt sie erhalten? Wichtig für `pending_flight` und Agentur-Besitz (`AgencyData`).
- **Sitz und Rakete nicht speichern:** `shouldBeSaved() = false` genügt für Chunk-Speichern und Login (`pending_flight`, Wiedereinstieg, 12.5)? Passagiere werden mit dem Fahrzeug gespeichert (`saveAsPassenger`): ein Spieler auf einem nicht gespeicherten Fahrzeug steigt beim Abmelden aus.
- **Frachtraum-Codec:** `ItemStackWithSlot`/`ContainerHelper` in 26.3 prüfen (Name, Signatur).
- **Chunk-Ticket:** `TicketType.PORTAL` (300 t) reicht für `landing_ticks` = 200? `FORCED` mit manuellem Entfernen als Alternative; eigener Typ nur bei Bedarf.
- **Kamerabeben:** Hook je Loader (NeoForge `ViewportEvent`, Fabric: kein Hook gefunden) oder Mixin auf `Camera`.
- **Sonde:** als `VehicleEntity`-freie `Entity` ohne Passagier; bleibt sie geladen (`isAlwaysTicking`, Ticket) oder genügt „Block → Entity → entfernen nach `ascent_ticks`“ mit Spielern in der Nähe?
- **Fabric-Dimensionsereignis** nach `teleport` (für Gravity-Modifier, 12.9): vorhandene Event-Klasse ermitteln oder über Tick-Prüfung (alle 10 t, ohnehin vorgesehen).
