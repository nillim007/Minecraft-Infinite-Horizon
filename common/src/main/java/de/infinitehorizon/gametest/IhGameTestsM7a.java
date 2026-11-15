package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.environment.VenusHeat;
import de.infinitehorizon.machine.BatteryBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModEntities;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.ProbeService;
import de.infinitehorizon.suit.SuitModules;
import de.infinitehorizon.vehicle.RoverData;
import de.infinitehorizon.vehicle.RoverEntity;
import de.infinitehorizon.world.XBandsBiomeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** GameTests für M7a (13.4, 12.3, 18, 9.2): Merkur, Lithium, erweiterte Sonde, Spiegelkäfer, Module. */
public final class IhGameTestsM7a {

    private static final Identifier MERCURY = InfiniteHorizon.id("mercury");

    private IhGameTestsM7a() {
    }

    /** Merkur: Tagseite ab X 2.000, Nachtseite unter X −2.000, Dämmerungsgürtel dazwischen (13.4). */
    public static void mercuryBiomeBands(GameTestHelper h) {
        BiomeSource src = h.getLevel().getChunkSource().getGenerator().getBiomeSource();
        h.assertTrue(src instanceof XBandsBiomeSource, "Biomquelle " + src);
        XBandsBiomeSource x = (XBandsBiomeSource) src;
        // abseits der Einsprengsel-Mitten prüfen: z so wählen, dass keine Zelle trifft
        int z = findZ(0) ;
        h.assertTrue(x.getNoiseBiome(0, 16, z >> 2).is(InfiniteHorizon.id("mercury_twilight_belt")), "x 0");
        z = findZ(3000);
        h.assertTrue(x.getNoiseBiome(3000 >> 2, 16, z >> 2).is(InfiniteHorizon.id("mercury_scorched_plains")), "x 3000");
        z = findZ(-3000);
        h.assertTrue(x.getNoiseBiome(-3000 >> 2, 16, z >> 2).is(InfiniteHorizon.id("mercury_frost_plains")), "x -3000");
        h.succeed();
    }

    /** Ein z, an dem bei {@code x} kein Einsprengsel liegt (Werte wie in der erzeugten Dimension). */
    private static int findZ(int x) {
        for (int z = 0; ; z += 64) {
            if (!XBandsBiomeSource.inclusionAt(x, z, 256, 48, 0.08)) {
                return z;
            }
        }
    }

    /** Lithiumsalz droppt 2 {@code lithium_salt}; Titanerz ohne Eisenspitzhacke nichts (13.4). */
    public static void mercuryDrops(GameTestHelper h) {
        BlockPos salt = new BlockPos(1, 1, 1);
        BlockPos ti = new BlockPos(3, 1, 1);
        h.setBlock(salt, ModBlocks.simple("mercury_lithium_salt").get());
        h.setBlock(ti, ModBlocks.simple("mercury_titanium_ore").get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 3.5));
        try {
            List<ItemStack> a = Block.getDrops(h.getBlockState(salt), h.getLevel(), h.absolutePos(salt), null, p,
                    new ItemStack(Items.IRON_PICKAXE));
            h.assertTrue(a.size() == 1 && a.getFirst().is(ModItems.simple("lithium_salt").get()) && a.getFirst().getCount() == 2,
                    "Lithiumsalz: " + a);
            ItemStack stone = new ItemStack(Items.STONE_PICKAXE);
            h.assertTrue(h.getBlockState(ti).requiresCorrectToolForDrops() && !stone.isCorrectToolForDrops(h.getBlockState(ti)),
                    "Titanerz mit Steinspitzhacke abbaubar");
            h.assertTrue(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(h.getBlockState(ti)), "Titanerz nicht mit Eisen");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Lithium-Akkubank: Speicher und Durchsatz aus storage.json (6.3). */
    public static void lithiumBatteryBank(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.LITHIUM_BATTERY_BANK.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        BatteryBlockEntity be = h.getBlockEntity(pos, BatteryBlockEntity.class);
        be.refreshLayout();
        var cfg = BalanceManager.get().entry("storage", InfiniteHorizon.id("lithium_battery_bank").toString()).orElseThrow();
        h.assertTrue(be.energyStorage().getCapacity() == cfg.get("capacity").getAsLong(), "Kapazität " + be.energyStorage().getCapacity());
        h.succeed();
    }

    /** Erweiterte Sonde (12.3): Ring-2-Ziele nur mit ihr, Ring-1-Ziele mit beiden Sonden. */
    public static void advancedProbeTargets(GameTestHelper h) {
        Agency a = AgencyData.get(h.getLevel().getServer()).ensureFor(UUID.randomUUID(), 0);
        a.progress().addProbeTarget(MERCURY);
        a.progress().addProbeTarget(InfiniteHorizon.id("moon"));
        Identifier probe = InfiniteHorizon.id("probe");
        Identifier advanced = InfiniteHorizon.id("advanced_probe");
        h.assertFalse(ProbeService.canTarget(a, MERCURY, probe), "Merkur mit einfacher Sonde");
        h.assertTrue(ProbeService.canTarget(a, MERCURY, advanced), "Merkur mit erweiterter Sonde");
        h.assertTrue(ProbeService.canTarget(a, InfiniteHorizon.id("moon"), advanced), "Mond mit erweiterter Sonde");
        h.succeed();
    }

    /** Rover-Lithium-Akku (16): Energie × {@code energy_multiplier}. */
    public static void roverLithiumPack(GameTestHelper h) {
        RoverEntity r = ModEntities.ROVER.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        long base = r.capacity();
        r.load(new RoverData(0, 0, List.of(RoverEntity.LITHIUM_PACK), List.of()), null);
        double mult = BalanceManager.get().entry("vehicles", RoverEntity.LITHIUM_PACK.toString()).orElseThrow()
                .get("energy_multiplier").getAsDouble();
        h.assertTrue(r.capacity() == Math.round(base * mult), "Kapazität " + base + " → " + r.capacity());
        r.discard();
        h.succeed();
    }

    /** Hitzereflektor (Hitzezeit × 1,5) und Strahlenschild (Strahlung bis 3) aus suits.json (9.2). */
    public static void heatReflectorRadiationShield(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            ItemStack chest = new ItemStack(ModItems.HEAT_SUIT_CHESTPLATE.get());
            chest.set(ModDataComponents.SUIT_MODULES.get(), List.of(InfiniteHorizon.id("heat_reflector")));
            ItemStack helmet = new ItemStack(ModItems.HEAT_SUIT_HELMET.get());
            helmet.set(ModDataComponents.SUIT_MODULES.get(), List.of(InfiniteHorizon.id("radiation_shield")));
            p.setItemSlot(EquipmentSlot.CHEST, chest);
            p.setItemSlot(EquipmentSlot.HEAD, helmet);
            VenusHeat base = VenusHeat.rules();
            double mult = BalanceManager.get().entry("suits", InfiniteHorizon.id("heat_reflector").toString()).orElseThrow()
                    .get("venus_heat_timer_multiplier").getAsDouble();
            h.assertTrue(VenusHeat.forPlayer(p).duration() == Math.round(base.duration() * mult),
                    "Hitzezeit " + VenusHeat.forPlayer(p).duration());
            int shield = BalanceManager.get().entry("suits", InfiniteHorizon.id("radiation_shield").toString()).orElseThrow()
                    .get("radiation_max").getAsInt();
            h.assertTrue(SuitModules.max(p, "radiation_max", 0) == shield, "Strahlenschild " + SuitModules.max(p, "radiation_max", 0));
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Spiegelkäfer (18): Wer ihn bei Tageslicht ansieht, wird geblendet. */
    public static void mirrorBeetleBlinds(GameTestHelper h) {
        h.setBlock(new BlockPos(5, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        var beetle = ModEntities.MIRROR_BEETLE.get().create(h.getLevel(), EntitySpawnReason.TRIGGERED);
        Vec3 at = h.absoluteVec(new Vec3(5.5, 2, 2.5));
        beetle.setPos(at.x, at.y, at.z);
        beetle.setNoAi(false);
        beetle.setPersistenceRequired();
        h.getLevel().addFreshEntity(beetle);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 2.5));
        h.succeedWhen(() -> {
            Vec3 eye = p.getEyePosition();
            Vec3 d = beetle.getEyePosition().subtract(eye);
            p.setYRot((float) (Math.toDegrees(Math.atan2(-d.x, d.z))));
            p.setYHeadRot(p.getYRot()); // Blickrichtung lebender Entities folgt dem Kopf
            p.setXRot((float) (-Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)))));
            h.assertTrue(p.hasEffect(MobEffects.BLINDNESS), "keine Blindheit: Käfer " + beetle.isAlive() + " t" + beetle.tickCount
                    + " bei " + beetle.position() + ", Spieler " + p.position() + " in " + p.level().dimension().identifier()
                    + ", Blick " + de.infinitehorizon.creature.MirrorBeetle.looksAt(p.getEyePosition(), p.getViewVector(1f),
                    beetle.getEyePosition(), 16) + ", Sicht " + p.hasLineOfSight(beetle));
            beetle.discard();
            IhTestPlayers.remove(p);
        });
    }
}
