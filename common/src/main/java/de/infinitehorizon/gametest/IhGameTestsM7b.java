package de.infinitehorizon.gametest;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.GravityService;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.station.AsteroidAnchorBlock;
import de.infinitehorizon.station.ZeroGMovement;
import de.infinitehorizon.world.AsteroidFeature;
import de.infinitehorizon.world.AsteroidShape;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** GameTests für M7b (13.4, 18): Asteroidengürtel, Ceres, Asteroidenanker, Driftmilbe, Platin und Iridium. */
public final class IhGameTestsM7b {

    private IhGameTestsM7b() {
    }

    /**
     * Spawn-Asteroid (13.4): Struktur mit Radius 15 auf Höhe 80 aus Asteroidengestein mit Erzen. Der GameTest-Server
     * erzeugt keine Strukturen; der Test setzt sie wie {@code /place structure}.
     */
    public static void spawnAsteroidExists(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(4, 1, 4));
        var start = IhGameTestsM4b.placeStructure(level, InfiniteHorizon.id("spawn_large_asteroid"), at);
        h.assertTrue(start.isValid(), "Spawn-Asteroid nicht gesetzt");
        BlockPos c = new BlockPos(net.minecraft.world.level.ChunkPos.containing(at).getMiddleBlockX(), 80,
                net.minecraft.world.level.ChunkPos.containing(at).getMiddleBlockZ());
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(c).getBlock()).getPath();
        h.assertTrue(id.startsWith("asteroid_"), "Mitte " + c + ": " + id);
        // Größe aus dem Strukturteil (darüber kann zufällig ein Asteroid des Features liegen)
        var box = start.getBoundingBox();
        h.assertTrue(box.maxY() - c.getY() == 15 && c.getY() - box.minY() == 15, "Radius " + (box.maxY() - c.getY()));
        h.succeed();
    }

    /** Feature asteroid (13.4): in 25 Chunks abseits des Spawns liegen Asteroiden (40 % je Chunk). */
    public static void asteroidsGenerate(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int found = 0;
        for (int cx = 40; cx < 45; cx++) {
            for (int cz = 40; cz < 45; cz++) {
                level.getChunk(cx, cz, ChunkStatus.FULL);
                boolean hit = false;
                for (int x = 0; x < 16 && !hit; x += 3) {
                    for (int z = 0; z < 16 && !hit; z += 3) {
                        for (int y = 20; y <= 220 && !hit; y += 3) {
                            hit = !level.getBlockState(new BlockPos(cx * 16 + x, y, cz * 16 + z)).isAir();
                        }
                    }
                }
                if (hit) {
                    found++;
                }
            }
        }
        h.assertTrue(found > 0, "keine Asteroiden in 25 Chunks");
        h.assertTrue(level.getBlockState(new BlockPos(40 * 16, -10, 40 * 16)).isAir(), "Gelände unter den Asteroiden");
        h.succeed();
    }

    /** Fall unter y 0 im Asteroidengürtel (13.4): zurück auf den Spawn-Asteroiden mit Langsamem Fall. */
    public static void voidResetToSpawnAsteroid(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 2.5));
        try {
            p.teleportTo(p.getX(), -5, p.getZ());
            ZeroGMovement.tick(h.getLevel().getServer());
            h.assertTrue(Math.abs(p.getX() - 0.5) < 1 && Math.abs(p.getZ() - 0.5) < 1 && p.getY() > 80,
                    "nicht auf dem Spawn-Asteroiden: " + p.position());
            h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "kein Langsamer Fall");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Asteroidenanker (13.4): auf Asteroidengestein; im Umkreis gilt gravity_g, weiter weg die Schwerkraft des Gürtels. */
    public static void anchorGravity(GameTestHelper h) {
        BlockPos rock = new BlockPos(3, 1, 3);
        h.setBlock(rock, ModBlocks.simple("asteroid_rock").get());
        h.setBlock(rock.above(), ModBlocks.ASTEROID_ANCHOR.get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        JsonObject misc = BalanceManager.get().entry("misc", "gravity").orElseThrow();
        double perG = misc.get("gravity_per_g").getAsDouble();
        h.succeedWhen(() -> {
            GravityService.apply(p);
            h.assertTrue(Math.abs(p.getAttributeValue(Attributes.GRAVITY) - perG * AsteroidAnchorBlock.config().get("gravity_g")
                    .getAsDouble()) < 1e-9, "beim Anker " + p.getAttributeValue(Attributes.GRAVITY));
            h.assertFalse(AsteroidAnchorBlock.covered(h.getLevel(), h.absoluteVec(new Vec3(3.5, 2, 40))), "Anker wirkt zu weit");
            IhTestPlayers.remove(p);
        });
    }

    /** Ohne Asteroid darunter hält der Anker nicht (13.4). */
    public static void anchorNeedsAsteroid(GameTestHelper h) {
        BlockPos stone = new BlockPos(3, 1, 3);
        h.setBlock(stone, net.minecraft.world.level.block.Blocks.STONE);
        h.assertFalse(ModBlocks.ASTEROID_ANCHOR.get().defaultBlockState().canSurvive(h.getLevel(), h.absolutePos(stone.above())),
                "Anker auf Stein");
        h.succeed();
    }

    /** Ceres (13.4): der Eisvulkan ist ein Kegel aus Eis (gesetzt wie {@code /place structure}). */
    public static void ceresIceVolcano(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(4, 1, 4));
        var start = IhGameTestsM4b.placeStructure(level, InfiniteHorizon.id("ice_volcano"), at);
        h.assertTrue(start.isValid(), "Eisvulkan nicht gesetzt");
        var box = start.getBoundingBox();
        int ice = 0;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.ICE)) {
                ice++;
            }
        }
        h.assertTrue(ice > 500, "Eisblöcke " + ice);
        // feste Lage: der Strukturset ist für den Generator der Ceres möglich (Biomfilter), Chunk 0/0
        boolean known = level.getChunkSource().getGeneratorState().possibleStructureSets().stream()
                .anyMatch(s -> s.is(InfiniteHorizon.id("ice_volcano")));
        h.assertTrue(known, "Strukturset ice_volcano gilt für Ceres nicht");
        h.succeed();
    }

    /** Feature asteroid mit Schwarm (18): um das Platinerz schweben Driftmilben. */
    public static void driftMiteSwarm(GameTestHelper h) {
        AsteroidFeature f = new AsteroidFeature(InfiniteHorizon.id("asteroid_rock"), ConstantInt.of(2), 1.0, false,
                List.of(new AsteroidShape.OreShare(InfiniteHorizon.id("asteroid_platinum_ore"), 1.0)),
                Optional.of(new AsteroidFeature.Swarm(InfiniteHorizon.id("drift_mite"), 5, 5, 6)));
        BlockPos center = h.absolutePos(new BlockPos(12, 4, 12));
        f.place(h.getLevel(), h.getLevel().getChunkSource().getGenerator(), h.getLevel().getRandom(), center);
        h.assertTrue(h.getLevel().getBlockState(center).is(ModBlocks.simple("asteroid_platinum_ore").get()), "kein Platinerz");
        var mites = h.getLevel().getEntitiesOfClass(de.infinitehorizon.creature.DriftMite.class, new AABB(center).inflate(10));
        h.assertTrue(!mites.isEmpty(), "keine Driftmilben");
        h.assertTrue(mites.getFirst().isNoGravity(), "Driftmilbe fällt");
        mites.forEach(net.minecraft.world.entity.Entity::discard);
        h.succeed();
    }

    /** Asteroiden-Iridiumerz gibt mit Eisenspitzhacke {@code raw_iridium}, mit Stein nichts (13.4). */
    public static void asteroidOreDrops(GameTestHelper h) {
        BlockPos ore = new BlockPos(1, 1, 1);
        h.setBlock(ore, ModBlocks.simple("asteroid_iridium_ore").get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 3.5));
        try {
            List<ItemStack> d = Block.getDrops(h.getBlockState(ore), h.getLevel(), h.absolutePos(ore), null, p,
                    new ItemStack(Items.IRON_PICKAXE));
            h.assertTrue(d.size() == 1 && d.getFirst().is(ModItems.simple("raw_iridium").get()), "Iridiumerz: " + d);
            h.assertFalse(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(h.getBlockState(ore)), "mit Stein abbaubar");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }
}
