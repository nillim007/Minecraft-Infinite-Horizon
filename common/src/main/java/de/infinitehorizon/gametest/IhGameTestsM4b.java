package de.infinitehorizon.gametest;

import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.AgencyProgress;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.SolarPanelBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.research.AnalyzerBlockEntity;
import de.infinitehorizon.research.SampleData;
import de.infinitehorizon.research.SampleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GameTests für M4b (27.7): Probenzange, Analysator, Solarpanel im Override Mond, Erkundungsziel {@code lava_tube}.
 */
public final class IhGameTestsM4b {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier BAUXITE = Identifier.fromNamespaceAndPath("infinite_horizon", "bauxite_ore");
    private static final Identifier OVERWORLD = Identifier.withDefaultNamespace("overworld");
    private static final Identifier LAVA_TUBE = Identifier.fromNamespaceAndPath("infinite_horizon", "lava_tube");
    /** Messfenster des Solartests. */
    private static final int MEASURE_TICKS = 20;

    private IhGameTestsM4b() {
    }

    private static int samples(ServerPlayer p) {
        return p.getInventory().countItem(ModItems.SAMPLE.get());
    }

    /** Rechtsklick mit der Probenzange auf {@code rel} (wie {@code ServerPlayerGameMode#useItemOn}, ohne Abklingzeit-Sperre). */
    private static void useTool(GameTestHelper h, ServerPlayer p, ItemStack tool, BlockPos rel) {
        BlockPos abs = h.absolutePos(rel);
        tool.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
    }

    /**
     * 27.7 M4b: Probenzange auf natürlichem {@code bauxite_ore} gibt 1 Probe (Block {@code bauxite_ore}, Dimension
     * Overworld), ein zweiter Klick nichts; ein vom Spieler gesetztes {@code bauxite_ore} gibt „Keine verwertbare Probe“.
     */
    public static void sampleToolOncePerPosition(GameTestHelper h) {
        BlockPos natural = new BlockPos(1, 1, 1);
        h.setBlock(natural, ModBlocks.simple("bauxite_ore").get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 3.5));
        ItemStack tool = new ItemStack(ModItems.SAMPLE_TOOL.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, tool);
        useTool(h, p, tool, natural);
        h.assertTrue(samples(p) == 1, "Proben nach dem ersten Klick: " + samples(p));
        SampleData data = null;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(ModItems.SAMPLE.get())) {
                data = p.getInventory().getItem(i).get(ModDataComponents.SAMPLE.get());
            }
        }
        h.assertTrue(data != null && data.block().equals(BAUXITE) && data.dimension().equals(OVERWORLD),
                "Komponente sample: " + data);
        h.assertBlockPresent(ModBlocks.simple("bauxite_ore").get(), natural);
        useTool(h, p, tool, natural);
        h.assertTrue(samples(p) == 1, "zweiter Klick gab eine Probe");
        h.assertTrue(SampleTool.sample(h.getLevel(), h.absolutePos(natural), p) == SampleTool.Result.ALREADY_SAMPLED,
                "zweiter Klick nicht als „schon beprobt“ erkannt");

        // vom Spieler gesetzt: nie beprobbar
        BlockPos placed = new BlockPos(4, 1, 1);
        h.setBlock(placed.below(), net.minecraft.world.level.block.Blocks.STONE);
        ItemStack ore = new ItemStack(ModBlocks.simple("bauxite_ore").get().asItem());
        p.setItemInHand(InteractionHand.MAIN_HAND, ore);
        h.placeAt(p, ore, placed.below(), Direction.UP);
        h.assertBlockPresent(ModBlocks.simple("bauxite_ore").get(), placed);
        h.assertTrue(SampleTool.sample(h.getLevel(), h.absolutePos(placed), p) == SampleTool.Result.NOT_SAMPLEABLE,
                "vom Spieler gesetztes Erz ist beprobbar");
        h.assertTrue(samples(p) == 1, "gesetztes Erz gab eine Probe");
        IhTestPlayers.remove(p);
        h.succeed();
    }

    /** 27.7 M4b: Analysator mit Strom macht aus 1 Probe +1 FP in höchstens 110 t. */
    public static void analyzerOneSampleOneFp(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        UUID leader = UUID.randomUUID();
        AgencyData data = AgencyData.get(server);
        Agency agency = data.ensureFor(leader, 0L);
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.ANALYZER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        AnalyzerBlockEntity a = h.getBlockEntity(pos, AnalyzerBlockEntity.class);
        a.refreshLayout();
        a.setOwner(agency.id());
        a.energyStorage().setAmountUnchecked(a.energyStorage().getCapacity());
        h.assertTrue(a.canPlaceItem(0, SampleTool.create(BAUXITE, OVERWORLD)), "Eingangsslot nimmt die Probe nicht");
        h.assertFalse(a.canPlaceItem(0, SampleTool.create(BAUXITE, Identifier.withDefaultNamespace("the_nether"))),
                "Eingangsslot nimmt eine Probe ohne FP");
        a.setItem(0, SampleTool.create(BAUXITE, OVERWORLD));
        int before = agency.progress().fp();
        h.succeedWhen(() -> {
            h.assertTrue(agency.progress().fp() == before + 100, "FP " + agency.progress().fp() + " statt " + (before + 100));
            h.assertTrue(a.getItem(0).isEmpty(), "Probe nicht verbraucht");
        });
    }

    /**
     * 27.7 M4b (Override Mond, Tageszeit 6.000): Das Solarpanel liefert 5 J/t, in der Zone {@code polar_crater} 0.
     */
    public static void solarPanelMoon(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        // Uhr wie im Mond-Hitze-Test selbst setzen (die Testumgebung allein war auf Fabric einmal nicht angekommen)
        server.registryAccess().get(net.minecraft.world.clock.WorldClocks.OVERWORLD)
                .ifPresent(clock -> server.clockManager().setTotalTicks(clock, 6000L));
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.SOLAR_PANEL.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        SolarPanelBlockEntity panel = h.getBlockEntity(pos, SolarPanelBlockEntity.class);
        panel.refreshLayout();
        EnvironmentOverrides.Entry moon = EnvironmentOverrides.set(h, MOON, null, null, 100);
        AtomicLong start = new AtomicLong();
        h.runAtTickTime(5, () -> start.set(panel.energyStorage().getAmount()));
        h.runAtTickTime(5 + MEASURE_TICKS, () -> {
            h.assertFalse(h.getLevel().isRainingAt(h.absolutePos(pos).above()), "Regen im Test");
            long delta = panel.energyStorage().getAmount() - start.get();
            h.assertTrue(delta == 5L * MEASURE_TICKS, "Mond: " + delta + " J in " + MEASURE_TICKS + " t (erwartet 5 J/t)");
            h.assertTrue(panel.lastOutput() == 5.0, "Leistung " + panel.lastOutput());
            EnvironmentOverrides.remove(moon);
            EnvironmentOverrides.set(h, MOON, "polar_crater", null, 100);
            start.set(panel.energyStorage().getAmount());
        });
        h.runAtTickTime(10 + 2 * MEASURE_TICKS, () -> {
            long delta = panel.energyStorage().getAmount() - start.get();
            h.assertTrue(delta == 0 && panel.lastOutput() == 0.0, "polar_crater: " + delta + " J, Leistung " + panel.lastOutput());
            h.succeed();
        });
    }

    /**
     * 27.7 M4b (in {@code infinite_horizon:moon}): Ein Spieler in einer gesetzten {@code lava_tube} bringt seiner Agentur
     * 20 FP. SPEC-GAP #68: {@code /place structure} legt keinen Strukturstart an; der Test setzt die Röhre wie der Befehl
     * und trägt Start und Verweise wie die Weltgenerierung ein.
     */
    public static void lavaTubeGivesFp(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        StructureStart start = placeStructure(level, LAVA_TUBE, h.absolutePos(new BlockPos(1, 1, 1)));
        h.assertTrue(start != null && start.isValid(), "lava_tube nicht gesetzt");
        BoundingBox box = start.getPieces().getFirst().getBoundingBox();
        BlockPos center = box.getCenter();
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        p.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        Agency agency = AgencyData.get(level.getServer()).agencyOf(p.getUUID()).orElseThrow();
        int before = agency.progress().fp();
        h.succeedWhen(() -> {
            AgencyProgress pr = agency.progress();
            h.assertTrue(pr.fp() == before + 2000, "FP " + pr.fp() + " statt " + (before + 2000));
            h.assertTrue(pr.foundStructures().stream().anyMatch(f -> f.structure().equals(LAVA_TUBE)), "found_structures leer");
            IhTestPlayers.remove(p);
        });
    }

    /** Setzt eine Struktur wie {@code /place structure} und trägt Start und Verweise in die Chunks ein. */
    static StructureStart placeStructure(ServerLevel level, Identifier id, BlockPos pos) {
        Holder.Reference<Structure> holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(ResourceKey.create(Registries.STRUCTURE, id));
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        ChunkPos origin = ChunkPos.containing(pos);
        StructureStart start = holder.value().generate(holder, level.dimension(), level.registryAccess(), generator,
                generator.getBiomeSource(), level.getChunkSource().randomState().createClimateSampler(
                        net.minecraft.world.level.levelgen.densityfunction.SamplerContext.EMPTY_UNCACHED),
                level.getChunkSource().randomState(), level.getStructureTemplateManager(), level.getSeed(), origin, 0, level,
                biome -> true);
        if (!start.isValid()) {
            return start;
        }
        BoundingBox box = start.getBoundingBox();
        ChunkPos min = new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
        ChunkPos max = new ChunkPos(box.maxX() >> 4, box.maxZ() >> 4);
        level.getChunk(origin.x(), origin.z()).setStartForStructure(holder.value(), start);
        ChunkPos.rangeClosed(min, max).forEach(c -> {
            var chunk = level.getChunk(c.x(), c.z());
            start.placeInChunk(level, level.structureManager(), generator, level.getRandom(),
                    new BoundingBox(c.getMinBlockX(), level.getMinY(), c.getMinBlockZ(), c.getMaxBlockX(), level.getMaxY(),
                            c.getMaxBlockZ()), c);
            chunk.addReferenceForStructure(holder.value(), origin.pack());
            chunk.markUnsaved();
        });
        return start;
    }
}
