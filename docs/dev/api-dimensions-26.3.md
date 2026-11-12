# API-Spickzettel: Dimensionen, Himmel, Schwerkraft, Attachments in Minecraft 26.3 (Mojang-Namen, Java 25)

Stand: 2026-11-12. Ergänzt `api-neoforge-26.3.md`, `api-fabric-26.3.md`, `api-fluids-26.3.md`. Gebraucht für M3b–M3e (27.5).

**Markierungen:** **[J]** = per `javap`/JSON aus `minecraft_26.3_client.jar` gelesen. **[NF]** = NeoForge 26.3.0.102 (sources-Jar). **[FAB]** = Fabric API 0.166 (Jars: `javap`). **UNVERIFIZIERT** = nicht prüfbar, vor Gebrauch testen.
Vanilla-Daten nachschlagen: `unzip -p minecraft_26.3_client.jar data/minecraft/dimension_type/overworld.json`.

## 0. Die größten Änderungen gegenüber 1.21.x

1. **`dimension_type` hat Umgebungsattribute.** Himmel-, Nebel-, Wolkenfarbe, Bettenregeln, Respawn-Anker, Musik stehen im Feld `attributes` (Schlüssel wie `minecraft:visual/sky_color`). `fixed_time`, `has_raids`, `piglin_safe`, `natural`, `bed_works`, `respawn_anchor_works`, `effects` gibt es **nicht mehr**. [J]
2. **Tageszeit läuft über Uhren (`world_clock`) und Zeitleisten (`timeline`).** `Level#getDayTime` heißt nicht mehr so (`getDefaultClockTime()`, `getOverworldClockTime()`, `clockManager()`). Sonnenwinkel, Himmelsfarbe, Sterne, Mobs-brennen sind Tracks in `timeline/day.json`. [J]
3. **Biome tragen Attribute statt `effects`-Farben und `spawners`.** In `effects` steht nur noch `water_color`, Laub- und Grasfarben. Himmel/Nebel/Mobspawns sind Attribute im Biom. [J]
4. **Noise-Settings:** `surface_rule` heißt `material_rule` und ist ein Verweis auf die neue Registry `worldgen/material_rule` (Bedingungen: `worldgen/material_condition`). `aquifers_enabled`/`ore_veins_enabled`/`size_horizontal` usw. fehlen; `aquifers` ist ein optionales Objekt, `noise` hat nur `min_y`+`height`. [J]
5. **`DamageSources` hat kein `source(ResourceKey)`.** Eigene Quelle: `new DamageSource(holder)`. [J]
6. **Test-Umgebung:** Typ `clock_time` statt `time_of_day`. [J]
7. Sonst wie in den anderen Spickzetteln: `Identifier` statt `ResourceLocation`, `ValueInput/ValueOutput`, Features ohne `config`.

## 1. Eigene Dimension per Datapack

Dateien (Ordner = Registry-Pfad [J]): `data/<ns>/dimension/<id>.json` (= `LevelStem`), `dimension_type/`, `worldgen/noise_settings/`, `worldgen/biome/`, `worldgen/material_rule/`, `worldgen/feature/`, `worldgen/placed_feature/`, `worldgen/structure/`, `worldgen/structure_set/`, `damage_type/`, `timeline/`, `world_clock/`. Vanilla hat keine `dimension/*.json` (Overworld/Nether/End kommen aus Code); die Form ist nur aus den Codec-Feldern abgeleitet.

Dimensions-Schlüssel im Code: `ResourceKey.create(Registries.DIMENSION, id)` ergibt `ResourceKey<Level>`; Typ-Schlüssel `Registries.DIMENSION_TYPE`. [J]

### 1.1 `dimension/moon.json` [J: Felder `type`, `generator`; `settings`, `biome_source`; `biomes`, `biome`, `parameters`]
```json
{ "type": "infinite_horizon:moon",
  "generator": { "type": "minecraft:noise", "settings": "infinite_horizon:moon",
    "biome_source": { "type": "minecraft:multi_noise", "biomes": [
      { "biome": "infinite_horizon:moon_highlands",
        "parameters": { "temperature": [-0.8, 1.0], "humidity": [-1, 1], "continentalness": [-1, 1],
                        "erosion": [-1.0, -0.2], "depth": 0, "weirdness": [-1, 1], "offset": 0 } } ] } } }
```
- Jeder Parameter ist eine Zahl oder `[min, max]` (`ExtraCodecs.intervalCodec`). Alternative zu `biomes`: `"preset": "minecraft:overworld"|"minecraft:nether"` (nur diese zwei Presets). [J]
- Fester Einzelbiom: `{"type":"minecraft:fixed","biome":"..."}` (`FixedBiomeSource`, Feld `biome`). [J]
- Orbit-Leere: `generator` = `{"type":"minecraft:flat","settings":{"biome":"...","features":false,"lakes":false,"layers":[],"structure_overrides":[]}}` (Form aus `flat_level_generator_preset/classic_flat.json` [J]; leere `layers` UNVERIFIZIERT).
- Eigene `BiomeSource` (Merkur, Pluto-Radius) braucht Java: Registry `BuiltInRegistries.BIOME_SOURCE` (hält `MapCodec<? extends BiomeSource>`); eigener Generator analog `CHUNK_GENERATOR`. [J]

### 1.2 `dimension_type/moon.json` (alle Felder, Vanilla-Overworld als Vorlage) [J]
| Feld | Pflicht | Hinweis |
|---|---|---|
| `has_skylight`, `has_ceiling`, `has_ender_dragon_fight` | ja | Mond: `true`, `false`, `false` |
| `coordinate_scale` | ja | `1.0` |
| `min_y`, `height`, `logical_height` | ja | `-64`, `384`, `384`; Vielfache von 16, `logical_height <= height` |
| `infiniburn` | ja | `"#minecraft:infiniburn_overworld"` |
| `ambient_light` | ja | Float, Overworld `0.0` |
| `monster_spawn_light_level`, `monster_spawn_block_light_limit` | ja | Zahl oder IntProvider (Overworld `{"type":"minecraft:uniform","min_inclusive":0,"max_inclusive":7}`), Block-Limit `0` |
| `has_fixed_time` | nein (false) | ersetzt `fixed_time`; Nether und End `true` |
| `skybox` | nein | `none` / `overworld` / `end` (`DimensionType$Skybox`) |
| `cardinal_light` | nein | `default` / `nether` |
| `attributes` | nein | Map Attribut-ID → Wert (Abschnitt 1.3) |
| `timelines` | nein | Tag oder Liste von `timeline`-IDs; Overworld `"#minecraft:in_overworld"` |
| `default_clock` | nein | `world_clock`-ID; Overworld `minecraft:overworld`, Nether keine |

Mond-Gerüst: Overworld-JSON kopieren, `skybox: overworld`, `timelines: "#minecraft:in_overworld"`, `default_clock: "minecraft:overworld"` (Tag-Nacht wie Overworld, 13.1), `attributes` ersetzen. Der Overworld-Eintrag trägt `monster_spawn_*`, `bed_rule`, `straw_bed_rule`, `respawn_anchor_works false`, `nether_portal_spawns_piglin`; `has_raids`/`piglin_safe`/`natural` des Spec-Textes (13.1) sind jetzt Attribute (`gameplay/can_start_raid` usw.) oder entfallen. Wert aus Attribut-Liste unten, sonst weglassen (= Standard).

### 1.3 Umgebungsattribute [J]
Schreibweise: Direktwert `"minecraft:visual/sky_color": "#000000"` oder `{"argument": <wert>, "modifier": "overlay"|"append"|"multiply"|"maximum"|...}` (Vorbild: Biom `plains`, `timeline/day.json`). Alle Attribute (`EnvironmentAttributes`, Typ in Klammern):
- `visual/`: `fog_color`, `sky_color`, `water_fog_color` (Farbe); `fog_start_distance`, `fog_end_distance`, `sky_fog_end_distance`, `cloud_fog_end_distance`, `water_fog_start_distance`, `water_fog_end_distance`, `cloud_height`, `star_brightness`, `sun_angle`, `moon_angle`, `star_angle`, `sky_light_factor` (Float); `cloud_color`, `sunrise_sunset_color` (RGBA); `sky_light_color`, `block_light_tint`, `night_vision_color`, `ambient_light_color` (Farbe); `moon_phase`, `default_dripstone_particle`, `ambient_particles`.
- `audio/`: `background_music`, `music_volume`, `ambient_sounds`, `firefly_bush_sounds`.
- `gameplay/`: `sky_light_level`, `can_start_raid`, `water_evaporates`, `bed_rule`, `straw_bed_rule`, `respawn_anchor_works`, `nether_portal_spawns_piglin`, `fast_lava`, `increased_fire_burnout`, `eyeblossom_open`, `turtle_egg_hatch_chance`, `piglins_zombify`, `snow_golem_melts`, `creaking_active`, `surface_slime_spawn_chance`, `cat_waking_up_gift_chance`, `bees_stay_in_hive`, `monsters_burn`, `can_pillager_patrol_spawn`, `natural_mob_spawns`, `creature_world_gen_spawn_probability`, `villager_activity`, `baby_villager_activity`.
- Farben als `"#rrggbb"`/`"#aarrggbb"`.

Mond (schwarzer Himmel, Sterne am Tag): `visual/sky_color "#000000"`, `visual/fog_color "#000000"`, `visual/star_brightness 1.0`. Im Timeline-Track `day` ist `sky_color` `multiply`, `star_brightness` `maximum`; Wirkung auf dem Mond UNVERIFIZIERT (im Client-Test ansehen). Bettenregel Vorbild Overworld: `{"can_set_spawn":"always","can_sleep":"when_dark","error_message":{"translate":"block.minecraft.bed.no_sleep"}}`; Sauerstofflose Welt (14.4/8.5) per eigenem Code, nicht per Attribut.

Fabric-Hook für Mod-Dimensionen: `DimensionEvents.MODIFY_ATTRIBUTES.register((Holder<DimensionType>, EnvironmentAttributeMap.Builder, HolderLookup.Provider) -> ...)` (`fabric-dimensions-v1`). [FAB] NeoForge-eigenes Attribut `neoforge:custom_skybox` siehe 3.

### 1.4 `worldgen/noise_settings/moon.json` [J: Feldnamen aus `NoiseGeneratorSettings`; Vorlage `noise_settings/overworld.json`]
Felder: `noise {min_y,height}`, `default_block`, `default_fluid`, `noise_router`, `material_rule`, `spawn_target`, `sea_level`, `disable_mob_generation`, `legacy_random_source`, `aquifers` (optional, End hat keins), `debug_functions` (optional). `noise_router` hat nur `temperature, vegetation, continents, erosion, depth, ridges, chunk_surface_level, final_density` (Overworld verweist auf `minecraft:overworld/...`, Spec 13.1: unverändert lassen). `aquifers` weglassen = keine Aquifere; Erzadern sind Teil des Overworld-`material_rule` (`overworld/copper_ore_vein`), im eigenen Rule also schlicht nicht enthalten. Spec 13.1 (`aquifers_enabled false`, `ore_veins_enabled false`) ist damit erfüllt, siehe `docs/spec-gaps.md`.
Carver (`worldgen/carver/*.json`) haben in 26.3 weder `lava_level` noch `replaceable`; die Spec-Regel „`lava_level` = `min_y`“ (13.1) ist so nicht umsetzbar. UNVERIFIZIERT, wie Höhlen ohne Lava gesichert werden (vermutlich über `default_fluid`/fehlende Aquifere).

### 1.5 `worldgen/material_rule/moon.json` (Surface-Regeln) [J]
Typen: `minecraft:sequence {sequence:[...]}`, `minecraft:condition {if_true, then_run}`, `minecraft:block {result_state}`; Bedingungen u. a. `minecraft:biome {biome_is}`, `minecraft:y_above {anchor, surface_depth_multiplier, add_stone_depth}`, `minecraft:vertical_gradient {random_name, true_at_and_below, false_at_and_above}`, `minecraft:noise_threshold {noise, min_threshold, max_threshold}`, `minecraft:above_preliminary_surface`, `minecraft:stone_depth {surface_type, offset, add_surface_depth, secondary_depth_range}`. Verweise auf fertige Bausteine als Zeichenkette: `minecraft:on_floor` (oberste Schicht), `minecraft:under_floor` (die 3 Blöcke darunter), `minecraft:bedrock_floor`, `minecraft:under_ceiling`.
```json
{ "type": "minecraft:sequence", "sequence": [
  "minecraft:bedrock_floor",
  { "type": "minecraft:condition", "if_true": "minecraft:on_floor", "then_run": { "type": "minecraft:sequence", "sequence": [
      { "type": "minecraft:condition",
        "if_true": { "type": "minecraft:noise_threshold", "noise": "minecraft:surface", "min_threshold": 0.0, "max_threshold": 1.7976931348623157E308 },
        "then_run": { "type": "minecraft:block", "result_state": "infinite_horizon:moon_basalt" } },
      { "type": "minecraft:block", "result_state": "infinite_horizon:moon_regolith" } ] } },
  { "type": "minecraft:condition", "if_true": "minecraft:under_floor",
    "then_run": { "type": "minecraft:block", "result_state": "infinite_horizon:moon_regolith" } } ] }
```
In `noise_settings` dann `"material_rule": "infinite_horizon:moon"`. Blockzustand als `"ns:block"` oder `{"Name":..,"Properties":{..}}`. Der Overworld-Aufbau ist ein Beispiel für Biom-abhängige Regeln (`overworld/biome_surface/*.json`).

## 2. Biome, Features, Strukturen

### 2.1 Biom [J]
Pflicht: `temperature`, `downfall`, `has_precipitation`, `effects {water_color}`, `carvers` (Liste, darf `[]`), `features` (Liste von Listen, 0–11 Einträge, Vanilla 11 / End 11 / manche 0 oder 1). Optional: `attributes`, `temperature_modifier`, in `effects` `foliage_color`, `dry_foliage_color`, `grass_color`, `grass_color_modifier`. Der Index in `features` ist `GenerationStep.Decoration`: 0 RAW_GENERATION, 1 LAKES, 2 LOCAL_MODIFICATIONS, 3 UNDERGROUND_STRUCTURES, 4 SURFACE_STRUCTURES, 5 STRONGHOLDS, **6 UNDERGROUND_ORES**, 7 UNDERGROUND_DECORATION, 8 FLUID_SPRINGS, 9 VEGETAL_DECORATION, 10 TOP_LAYER_MODIFICATION.
```json
{ "temperature": 0.0, "downfall": 0.0, "has_precipitation": false, "effects": { "water_color": "#3f76e4" },
  "attributes": {
    "minecraft:visual/sky_color": "#000000", "minecraft:visual/fog_color": "#000000",
    "minecraft:gameplay/natural_mob_spawns": { "modifier": "overlay", "argument": { "spawn_costs": {},
      "spawns_by_category": { "monster": [ { "type": "minecraft:zombie", "count": 4, "weight": 90 } ] } } } },
  "carvers": [], "features": [ [], [], [], [], [], [], [ "infinite_horizon:moon_aluminum_ore" ] ] }
```
- **Mobspawns** stehen als Attribut `minecraft:gameplay/natural_mob_spawns` (Modifier `overlay`; Argument `spawns_by_category` + `spawn_costs`) im Biom, nicht mehr als `spawners`. Kategorien u. a. `monster`, `creature`, `ambient`, `underground_water_creature`. Die Vanilla-Biome der Spec (13.5, „Biome tragen die Spawngewichte der Overworld“) kopieren dort die `monster`-Liste von `plains`. [J]
- Himmel/Nebelfarbe pro Biom: Attribut im Biom (Beispiel `crimson_forest`: `minecraft:visual/fog_color`). Dimensionswert gilt, wenn das Biom nichts setzt. [J]
- Biom-Tags für Strukturen: `data/<ns>/tags/worldgen/biome/has_structure/<name>.json` (Vorbild `minecraft:has_structure/igloo`). [J]

### 2.2 Features und Placed Features [J]
Feature-JSON: `{"type":"minecraft:ore","size":9,"discard_chance_on_air_exposure":0.0,"targets":[{"state":"ns:block","target":{"predicate_type":"minecraft:tag_match","tag":"minecraft:stone_ore_replaceables"}}]}` (kein `config`). Für den Mond-`default_block` `moon_rock` statt Stein: `"predicate_type":"minecraft:block_match"` (kommt in Vanilla-Erzen vor, Feld `block`) oder ein eigener Tag `ns:moon_ore_replaceables` mit `tag_match`.
Placed Feature: `{"feature":"ns:x","placement":[{"type":"minecraft:count","count":6},{"type":"minecraft:in_square"},{"type":"minecraft:height_range","height":{"type":"minecraft:uniform","min_inclusive":{"absolute":50},"max_inclusive":{"absolute":120}}},{"type":"minecraft:biome"}]}`. Vorbild im Projekt: `common/src/generated/resources/data/infinite_horizon/worldgen/{feature,placed_feature}/bauxite_ore.json`. `trapezoid` statt `uniform` für Vanilla-Verteilung.
Eigener Feature-Typ (`small_crater`): Java-Klasse `Feature` mit Codec in `BuiltInRegistries.FEATURE_TYPE` (siehe `api-fluids-26.3.md` Abschnitt 0). [J]

### 2.3 Strukturen [J]
- `structure_set`: `{"placement":{"type":"minecraft:random_spread","salt":14357618,"separation":8,"spacing":32},"structures":[{"structure":"ns:great_crater","weight":1}]}`.
- `structure`: `{"type":"minecraft:<typ>","biomes":"#ns:has_structure/great_crater","step":"surface_structures","spawn_overrides":{}}`. Eigene Typen: `BuiltInRegistries.STRUCTURE_TYPE` (`StructureType<S>`, `Structure#findGenerationPoint`, `type()`). Jigsaw-Variante (Vorlage `village_plains.json`): `type minecraft:jigsaw`, `start_pool`, `size`, `max_distance_from_center`, `project_start_to_heightmap`, `start_height {absolute:0}`, `terrain_adaptation`, `use_expansion_hack`. Schritte: `underground_structures`, `surface_structures` usw.

## 3. Himmel: Erde am Mondhimmel
**Es gibt kein `DimensionSpecialEffects` mehr.** [J: Klasse fehlt] Vanilla zeichnet über `net.minecraft.client.renderer.SkyRenderer` (`extractRenderState(ClientLevel,float,Camera,SkyRenderState)`, `render(GpuBufferSlice, SkyRenderState)`; private `renderSunMoonAndStars`, `renderSun`, `renderMoon`, `drawCelestialBody(Supplier<String>, RenderPass, GpuBufferSlice, GpuBuffer, GpuBuffer, int)`, `applyCelestialBodyTransform(PoseStack,float,float)`). `SkyRenderState` hat öffentliche Felder `skybox, sunAngle, moonAngle, starAngle, starBrightness, skyColor, sunriseAndSunsetColor, moonPhase, rainBrightness, shouldRenderDarkDisc`. Die GPU-Typen heißen `com.mojang.renderpearl.api.*` (`GpuBuffer`, `RenderPass`). [J]
Himmelsscheiben (Sonne, Mond) liegen im Atlas `celestials` (Quelle `atlases/celestials.json`: Verzeichnis `textures/environment/celestial`). Eine Mod-Textur `assets/<ns>/textures/environment/celestial/earth.png` landet wahrscheinlich als Sprite `<ns>:earth` im Atlas (Verzeichnis-Quelle läuft über alle Namespaces) — UNVERIFIZIERT.

- **NeoForge [NF]:** `CustomSkyboxRenderer` (`net.neoforged.neoforge.client`) mit `default boolean renderSky(LevelRenderState, SkyRenderState, Matrix4fc modelViewMatrix, GpuBufferSlice skyFog)`; `true` = Vanilla-Himmel unterdrücken, `false` = zusätzlich zeichnen. Registrieren im Mod-Bus: `RegisterCustomEnvironmentEffectRendererEvent#registerSkyboxRenderer(Identifier, CustomSkyboxRenderer)`; aktivieren per Dimension-/Biom-Attribut `"neoforge:custom_skybox": "<ns>:moon_sky"` (`NeoForgeEnvironmentAttributes.CUSTOM_SKYBOX`, Typ `Identifier`, syncbar; für Vanilla-Clients gefiltert). Eigene Render-Daten per `ExtractLevelRenderStateEvent` (Felder `renderState`, `level`, `camera`). Der Aufruf im gepatchten `SkyRenderer` ist hier nicht einsehbar (Patches nicht im sources-Jar): UNVERIFIZIERT.
- **Fabric [FAB]:** Es gibt **keinen** Himmel-Hook (`fabric-rendering-v1` kennt nur `LevelRenderEvents`: `START_MAIN`, `AFTER_OPAQUE_TERRAIN`, `COLLECT_SUBMITS`, `BEFORE_TRANSLUCENT_TERRAIN` … `END_MAIN`, Kontext `LevelRenderContext` mit `poseStack()`, `submitNodeCollector()`). Der Himmel wird vor dem Gelände gezeichnet, ein Ereignis danach überdeckt das Gelände ohne Tiefenprüfung falsch. Empfohlen: **gemeinsamer Mixin** auf `SkyRenderer#render` (TAIL) in `common/.../mixin` (`infinite_horizon.mixins.json`, Abschnitt `client`, derzeit leer) — läuft auf beiden Loadern, dann braucht man den NeoForge-Hook nicht.
- Daten für den Himmel (Erde: `size 30`, `azimuth 60`, `elevation 40`, 25.1) kommen aus dem `PlanetRegistry`-Sync (13.1), nicht aus dem Dimension-JSON. Wie man ein Quad mit eigener Textur im `renderpearl`-Pass zeichnet, ist UNVERIFIZIERT; am einfachsten `buildCelestialQuad`/`drawCelestialBody` aus `SkyRenderer` nachbauen.

## 4. Teleport in eine Dimension [J]
```java
ServerLevel moon = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse("infinite_horizon:moon"))); // null wenn nicht geladen
moon.getChunk(x >> 4, z >> 4, ChunkStatus.FULL);                       // lädt synchron
int y = moon.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z); // erster freier Y über dem Boden
player.teleport(new TeleportTransition(moon, Vec3.atBottomCenterOf(new BlockPos(x, y, z)), Vec3.ZERO,
        player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
```
- `ServerPlayer#teleport(TeleportTransition)` gibt `ServerPlayer` zurück (kann bei Dimensionswechsel ein neues Objekt sein: danach mit dem Rückgabewert weiterarbeiten). Weitere Konstruktoren: mit `Set<Relative>`, mit `missingRespawnBlock`, `asPassenger`. `PostTeleportTransition`: `DO_NOTHING`, `PLAY_PORTAL_SOUND`, `PLACE_PORTAL_TICKET`. Direkt: `teleportTo(ServerLevel, x, y, z, Set<Relative>, yRot, xRot, boolean)`.
- `Heightmap.Types`: `WORLD_SURFACE_WG`, `WORLD_SURFACE`, `OCEAN_FLOOR_WG`, `OCEAN_FLOOR`, `MOTION_BLOCKING`, `MOTION_BLOCKING_NO_LEAVES`. Für Landeplatz-Suche ersten Block prüfen (`isAir`, nicht Lava), nach unten/zur Seite versetzen.
- Chunk-Ticket statt Dauer-Laden: `moon.getChunkSource().addTicketAndLoadWithRadius(TicketType, ChunkPos, int)` (gibt `CompletableFuture`). [J]
- Level-Schlüssel eines Spielers: `player.level().dimension()`; alle Level: `server.levelKeys()`, `getAllLevels()`.
- Befehl: Muster in `AgencyCommands.register(CommandDispatcher<CommandSourceStack>)`; Rechtestufe `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` (Stufe 2), Argumente `Commands.argument(name, StringArgumentType.word()).suggests(...)`, `IntegerArgumentType`. Registrierung beider Loader ruft dieselbe Methode (`InfiniteHorizon.java:102`).

## 5. Schwerkraft und Fallhöhe [J]
`LivingEntity.createLivingAttributes()` enthält `GRAVITY` (Basis 0,08, Min −1,0), `SAFE_FALL_DISTANCE` (3,0, −1024…1024), `FALL_DAMAGE_MULTIPLIER` (Max 100,0), `JUMP_STRENGTH`, `STEP_HEIGHT`, `AIR_DRAG_MODIFIER`, `FRICTION_MODIFIER`, `OXYGEN_BONUS`, `BURNING_TIME`. Alle `Attributes.*` sind `Holder<Attribute>`.
```java
public static final Identifier GRAVITY_ID = Identifier.fromNamespaceAndPath("infinite_horizon", "planet_gravity");
void apply(ServerPlayer p, double g) {           // g aus BalanceManager/PlanetRegistry
    set(p, Attributes.GRAVITY, g - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    set(p, Attributes.FALL_DAMAGE_MULTIPLIER, g - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    set(p, Attributes.SAFE_FALL_DISTANCE, 3 / Math.max(g, 0.05) - 3, AttributeModifier.Operation.ADD_VALUE);
}
void set(ServerPlayer p, Holder<Attribute> a, double v, AttributeModifier.Operation op) {
    AttributeInstance i = p.getAttribute(a);
    if (i != null) i.addOrUpdateTransientModifier(new AttributeModifier(GRAVITY_ID, v, op));
}
void clear(ServerPlayer p) { /* i.removeModifier(GRAVITY_ID) je Attribut, gibt boolean zurück */ }
```
- `AttributeModifier` ist ein Record `(Identifier id, double amount, Operation operation)`; Operationen `ADD_VALUE`, `ADD_MULTIPLIED_BASE`, `ADD_MULTIPLIED_TOTAL`. Dieselbe ID auf mehreren Attributen ist erlaubt (die ID zählt je `AttributeInstance`). Es gibt keinen „Name“ mehr.
- `AttributeInstance`: `addTransientModifier` (wirft bei doppelter ID, nicht speichern), `addOrUpdateTransientModifier`, `addOrReplacePermanentModifier` (wird gespeichert), `removeModifier(Identifier)`, `hasModifier(Identifier)`, `getValue()`. Transiente Modifikatoren werden nicht gespeichert und beim Respawn nicht übernommen (`AttributeMap.assignPermanentModifiers` kopiert nur permanente; Folgerung aus dem Namen, UNVERIFIZIERT): nach Login, Respawn, Dimensionswechsel und im 10-t-Takt neu setzen (Spec 12.9).
- Ereignisse: Fabric `ServerPlayerEvents.JOIN` (`onJoin(ServerPlayer)`), `AFTER_RESPAWN` (`afterRespawn(old, new, boolean)`), `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL` (`afterChangeLevel(ServerPlayer, ServerLevel from, ServerLevel to)`), `ServerTickEvents.END_SERVER_TICK`. NeoForge `PlayerEvent.PlayerLoggedInEvent`, `PlayerRespawnEvent`, `PlayerChangedDimensionEvent` (`getFrom()`, `getTo()`), `PlayerTickEvent`, `ServerTickEvent`. [FAB][NF]
- Fallschaden: `LivingEntity#calculateFallDamage(double, float)` (protected) liest Attribute; kein Eingriff nötig.

## 6. Spieler-Attachments (Hazard-Zustand)
| | NeoForge [NF] | Fabric [FAB] |
|---|---|---|
| Register | `DeferredRegister<AttachmentType<?>>.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MOD_ID)` | `AttachmentRegistry.create(Identifier, Consumer<Builder<A>>)` |
| Aufbau | `AttachmentType.builder(Supplier<T>)` bzw. `builder(Function<IAttachmentHolder,T>)` | `AttachmentRegistry.<A>builder().initializer(Supplier<A>)` … `.buildAndRegister(Identifier)` |
| Speichern | `.serialize(MapCodec<T>)` (**MapCodec**, auch mit `Predicate`) oder `IAttachmentSerializer`/`serializable(...)` mit `ValueInput/ValueOutput` | `.persistent(Codec<A>)` (**Codec**); Kurzform `createPersistent(Identifier, Codec)` |
| Tod/Respawn | `.copyOnDeath()` (nur mit Serializer, sonst `IllegalStateException`) | `.copyOnDeath()` |
| Sync | `.sync(StreamCodec<? super RegistryFriendlyByteBuf,T>)` oder mit `BiPredicate<IAttachmentHolder,ServerPlayer>`/`AttachmentSyncHandler` | `.syncWith(StreamCodec, AttachmentSyncPredicate)`; Prädikate `all()`, `targetOnly()`, `allButTarget()` |
| Zugriff | `entity.getData(type)` (legt Default an), `setData`, `hasData`, `removeData`, `getExistingDataOrNull` | `getAttached`, `getAttachedOrCreate`, `getAttachedOrElse`, `setAttached`, `modifyAttached(type, UnaryOperator)`, `hasAttached`, `removeAttached` |
Fabric: `Entity`, `BlockEntity`, `Level`, `ChunkAccess` sind `AttachmentTarget` per Class-Tweaker-Interface-Injection. [FAB]

Gemeinsame Schnittstelle (Muster `Services`/`platform/services`, ServiceLoader pro Loader, Eintrag in `META-INF/services/de.infinitehorizon.platform.services.IAttachmentHelper`, neues Feld in `Services`):
```java
public interface IAttachmentHelper {
    HazardState get(ServerPlayer p);                 // nie null: Default 100 % / 0
    void set(ServerPlayer p, HazardState s);         // Sync zum Spieler erledigt der Loader (Fabric: targetOnly)
}
```
`HazardState` (reine Daten, in `common`) liefert `MapCodec<HazardState>` (NeoForge: `.serialize(codec)`) und `codec.codec()` für Fabric (`MapCodec#codec()`); `StreamCodec` für den Sync gleich auf beiden Seiten. Der Spec-Entwurf (3A) nutzt für die HUD-Anzeige stattdessen `HazardStatePayload` über `INetworkHelper`; dann braucht das Attachment keinen Sync (nur Speichern + `copyOnDeath`).

## 7. Schadensarten
`data/<ns>/damage_type/<id>.json` [J]: `{"message_id":"ih_vacuum","scaling":"never","exhaustion":0.0,"effects":"hurt","death_message_type":"default"}`. Pflicht `message_id`, `exhaustion`; `scaling` (`never`/`when_caused_by_living_non_player`/`always`), `effects` (`hurt`/`thorns`/`drowning`/`burning`/`poking`/`freezing`) und `death_message_type` (`default`/`fall_variants`/`intentional_game_design`) haben Standardwerte (je `optionalFieldOf`: vier Felder gelesen, welche genau optional sind, nicht einzeln geprüft). Todesnachricht: Übersetzung `death.attack.<message_id>` (+ `.player`), aus Vanilla-Verhalten, nicht geprüft.
Tags unter `data/minecraft/tags/damage_type/` (nur Ergänzung mit `{"values":["ns:id"]}`, nie `replace`): `bypasses_armor` (Vakuum/Druck), `bypasses_effects`, `bypasses_enchantments`, `bypasses_invulnerability`, `bypasses_shield`, `no_knockback`, `is_fire`, `is_freezing`, `is_fall`, `is_explosion` u. a. (36 Tags in `tags/damage_type/`).
```java
public static final ResourceKey<DamageType> VACUUM = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "vacuum"));
DamageSource src = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(VACUUM));
player.hurtServer(level, src, amount);   // [J] boolean hurtServer(ServerLevel, DamageSource, float)
```
Konstruktoren `DamageSource(Holder<DamageType>[, Entity direct[, Entity causing]])` und `(Holder, Vec3)`; Prüfung `src.is(ResourceKey)` / `is(TagKey)`. [J]

## 8. GameTests mit Umgebung und Dimension
- **`test_environment/<id>.json`** [J: Typnamen `all_of`, `clock_time`, `difficulty`, `function`, `game_rules`, `weather`, `timeline_attributes`]:
```json
{ "type": "minecraft:all_of", "definitions": [
  { "type": "minecraft:clock_time", "clock": "minecraft:overworld", "time": 6000 },
  { "type": "minecraft:weather", "weather": "clear" },
  { "type": "minecraft:game_rules", "rules": { "minecraft:advance_time": false, "minecraft:advance_weather": false } } ] }
```
`weather` ∈ `clear|rain|thunder` (UNVERIFIZIERT, Namen aus `Weather$Type`); `difficulty`: Feld `difficulty`; `function`: Felder `setup`, `teardown`; `timeline_attributes`: Feld `timelines`. Es gibt **kein** `time_of_day`. Vanilla `test_environment/default.json` = leeres `all_of`.
- **Fabric:** Datei unter `fabric/src/main/resources/data/infinite_horizon/test_environment/` (Beispiel `performance.json`), `@GameTest(environment = "ns:id", dimension = "ns:moon", structure = ..., skyAccess, maxTicks ...)`; Standard `environment "minecraft:default"`, `dimension "minecraft:overworld"`, `structure "fabric-gametest-api-v1:empty"`. [FAB]
- **NeoForge:** `RegisterGameTestsEvent#registerEnvironment(Identifier, TestEnvironmentDefinition<?>...)` (auch `(Identifier, TestEnvironmentDefinition<?>)`); `new TestData<>(env, ResourceKey<Level> dimension, structure, maxTicks, setupTicks, required, Rotation, manualOnly, maxAttempts, requiredSuccesses, skyAccess, padding)`; `registerTest(id, new FunctionGameTestInstance(key, data))` (Projekt: `NeoForgeGameTests`, dort fest `Level.OVERWORLD`). Ob ein `data/<ns>/test_environment/*.json` auf NeoForge zusätzlich geladen wird: UNVERIFIZIERT. Direkt im Code: `new TestEnvironmentDefinition.ClockTime(Holder<WorldClock>, int)` braucht einen echten Holder (erst nach Registry-Laden) — einfacher im Testcode setzen: `((ServerClockManager) level.clockManager()).setTotalTicks(level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD), 6000)`; `GameTestHelper#setTime(long)` gibt es auch. [J]
- **Test in eigener Dimension:** Ja. `TestData.dimension` (JSON `dimension`) wählt das `ServerLevel` (`GameTestRunner` ruft `server.getLevel(dimension)`, `StructureGridSpawner` führt je Dimension ein eigenes Raster); `helper.getLevel()` ist dann der Mond. Voraussetzung: die Dimension ist im Datapack der Testwelt vorhanden und die Struktur-Vorlage (leer, `infinite_horizon:gametest/empty*`) wird dort gesetzt; Chunks lädt der Runner. Mock-Spieler: `helper.makeMockServerPlayerInLevel()`, `makeMockServerPlayer(GameType)`. Ob `required`-Tests dort stabil laufen: UNVERIFIZIERT, im ersten Test zuerst `helper.assertTrue(helper.getLevel().dimension().equals(MOON), ...)`.
- Tickrate: der Test-Server läuft ohne Begrenzung; `advance_time false` hält die Uhr (sonst läuft sie mit).

## 9. Stolperstellen
1. Alte Dimension-Type-Felder (`fixed_time`, `effects`, `bed_works`, `natural`, `has_raids`, `piglin_safe`, `respawn_anchor_works`) lösen beim Laden Fehler oder werden ignoriert: alles über `attributes` (1.2/1.3).
2. `monster_spawn_light_level`, `monster_spawn_block_light_limit`, `has_skylight`, `has_ceiling`, `has_ender_dragon_fight`, `ambient_light`, `coordinate_scale`, `min_y`, `height`, `logical_height`, `infiniburn` sind Pflichtfelder (`fieldOf`); ein fehlendes Feld lässt das Laden der Registry beim Serverstart scheitern.
3. `features` im Biom ist Liste von Listen; Index 6 sind Erze. `carvers` und `features` sind Pflichtfelder (`fieldOf`), `[]` ist erlaubt.
4. Spec-Gaps nötig: `aquifers_enabled`/`ore_veins_enabled`/`lava_level` (13.1) und `time_of_day` (27.5) gibt es in 26.3 nicht; `surface_rule` heißt `material_rule`; Spawns sind Attribute.
5. Biom-Mobspawns über `natural_mob_spawns` (Attribut), nicht `spawners`. Die „50 % der Spawns abbrechen“-Regel (13.5) ist Code (Spawn-Ereignis), nicht Datapack; Hook je Loader UNVERIFIZIERT.
6. Himmel: Kein `DimensionSpecialEffects`, kein Fabric-Hook; Fabric nur per Mixin. Die Typen hinter dem Renderer heißen `com.mojang.renderpearl.*`.
7. Schwerkraft: Modifikator ist transient, nach Respawn/Dimensionswechsel neu setzen; nie `setBaseValue` ändern. `Attributes.*` sind `Holder<Attribute>` (kein `.get()`).
8. Attachments: NeoForge verlangt `MapCodec`, Fabric `Codec`. `copyOnDeath()` ohne Serializer wirft bei NeoForge.
9. `ServerPlayer#teleport` kann ein neues Objekt liefern; danach Rückgabewert nutzen. Chunk vorher laden (`getChunk(..., ChunkStatus.FULL)`), sonst ist die Höhenkarte nicht zuverlässig (Folgerung, UNVERIFIZIERT).
10. `DamageSources` kennt eigene Typen nicht: `new DamageSource(Holder)`. Tag-Dateien nur ergänzen (`"replace": false` ist der Standard).
11. GameTest-Dimension: Fabric `dimension` ist ein `String`, NeoForge `TestData.dimension` ein `ResourceKey<Level>`; Standard-Uhr im Mond-Test per `default_clock` der Dimension, nicht per Overworld-Umgebung allein (die Umgebung setzt die benannte Uhr `minecraft:overworld`, die der Mond mitnutzt).
12. Windows/Git-Bash: Bei langen Python-/Shell-Skripten Dateien im Scratchpad statt Heredoc benutzen (siehe Skill).
