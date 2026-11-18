package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.creature.FrostLurker;
import de.infinitehorizon.creature.SignalData;
import de.infinitehorizon.creature.SignalSentinel;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.world.SignalTransmitterStructure;
import de.infinitehorizon.world.TransmitterCoreBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** GameTests für M7e, Teil Pluto (13.1, 13.4, 18): Herzgletscher, Uran, Sender, Wächter, Sternenkarte. */
public final class IhGameTestsM7e2 {

    private IhGameTestsM7e2() {
    }

    /** Biomquelle {@code center_radius} (13.1): bei (0, 0) der Herzgletscher, weit draußen nicht. */
    public static void heartGlacier(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var inner = ResourceKey.create(Registries.BIOME, InfiniteHorizon.id("pluto_heart_glacier"));
        h.assertTrue(level.getBiome(new BlockPos(0, 64, 0)).is(inner), "kein Herzgletscher bei 0/0");
        h.assertFalse(level.getBiome(new BlockPos(2000, 64, 2000)).is(inner), "Herzgletscher weit draußen");
        h.succeed();
    }

    /** Pluto-Uranerz gibt mit Eisenspitzhacke Rohuran (13.4). */
    public static void uraniumOre(GameTestHelper h) {
        BlockPos ore = new BlockPos(1, 1, 1);
        h.setBlock(ore, ModBlocks.simple("pluto_uranium_ore").get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 2, 3.5));
        try {
            List<ItemStack> d = Block.getDrops(h.getBlockState(ore), h.getLevel(), h.absolutePos(ore), null, p,
                    new ItemStack(Items.IRON_PICKAXE));
            h.assertTrue(!d.isEmpty() && d.getFirst().is(ModItems.simple("raw_uranium").get()), "Uranerz: " + d);
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Sender (13.4): Halle aus Legierung, Kern in der Mitte, Öffnung in der Südwand. */
    public static void transmitterHall(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var start = IhGameTestsM4b.placeStructure(level, InfiniteHorizon.id("signal_transmitter"), h.absolutePos(new BlockPos(4, 1, 4)));
        h.assertTrue(start.isValid(), "Sender nicht gesetzt");
        SignalTransmitterStructure.Piece piece = (SignalTransmitterStructure.Piece) start.getPieces().getFirst();
        BlockPos c = piece.center();
        h.assertTrue(level.getBlockState(piece.corePos()).is(ModBlocks.TRANSMITTER_CORE.get()), "kein Kern");
        h.assertTrue(level.getBlockState(c.above(3)).isAir(), "Halle nicht hohl");
        h.assertTrue(level.getBlockState(c.offset(-16, 5, 0)).is(ModBlocks.ANCIENT_ALLOY.get()), "keine Westwand");
        h.assertTrue(level.getBlockState(c.offset(0, 2, 15)).isAir(), "keine Öffnung in der Südwand");
        h.assertTrue(level.getBlockState(c.offset(3, 2, 15)).is(ModBlocks.ANCIENT_ALLOY.get()), "Südwand fehlt");
        h.assertTrue(level.getBlockState(c.offset(0, 1, 16)).isAir() && level.getBlockState(c.offset(0, 0, 16))
                .is(ModBlocks.simple("pluto_water_ice").get()), "kein Tunnel");
        h.assertTrue(ModBlocks.ANCIENT_ALLOY.get().defaultBlockState().getDestroySpeed(level, c) < 0, "Legierung zerstörbar");
        h.succeed();
    }

    /**
     * Wächter (18): Kern beschwört ihn einmal je Agentur; besiegt droppt er die Sternenkarte (Komponente chart_owner,
     * feuerfest) und sperrt neue Beschwörungen für die Wartezeit. Eine zerstörte Karte gibt die Agentur wieder frei.
     */
    public static void sentinelChart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos core = new BlockPos(6, 1, 6);
        h.setBlock(core, ModBlocks.TRANSMITTER_CORE.get());
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 1, 1.5));
        try {
            Agency agency = AgencyManager.ensureFor(p);
            SignalData data = SignalData.get(level.getServer());
            SignalSentinel s = TransmitterCoreBlock.summon(level, h.absolutePos(core), p);
            h.assertTrue(s != null && agency.id().equals(s.summoner()), "nicht beschworen");
            h.assertTrue(TransmitterCoreBlock.summon(level, h.absolutePos(core), p) == null, "zweiter Wächter");
            s.kill(level);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(s.blockPosition()).inflate(4),
                    e -> e.getItem().is(ModItems.STAR_CHART.get()));
            h.assertTrue(drops.size() == 1, "Karten " + drops.size());
            ItemStack chart = drops.getFirst().getItem();
            var owner = chart.get(ModDataComponents.CHART_OWNER.get());
            h.assertTrue(owner != null && owner.agency().equals(agency.id()) && owner.uuid().equals(data.chart(agency.id())),
                    "Besitzer " + owner);
            h.assertTrue(chart.has(DataComponents.DAMAGE_RESISTANT), "nicht feuerfest");
            h.assertTrue(TransmitterCoreBlock.summon(level, h.absolutePos(core), p) == null, "keine Wartezeit");
            drops.getFirst().remove(Entity.RemovalReason.KILLED);
            h.assertTrue(data.chart(agency.id()) == null, "zerstörte Karte gilt noch");
            data.defeated(agency.id(), Long.MIN_VALUE / 2);
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }

    /** Wächter (18): unter halber Gesundheit Phase 2 mit drei Frostkriechern; die Schockwelle stößt Spieler weg. */
    public static void sentinelPhases(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        SignalSentinel s = h.spawn(de.infinitehorizon.registry.ModEntities.SIGNAL_SENTINEL.get(), new Vec3(4.5, 1, 4.5));
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(6.5, 1, 4.5));
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        s.shockwave(level, 6);
        double vx = p.getDeltaMovement().x;
        s.setHealth(s.getMaxHealth() * 0.4f);
        h.runAfterDelay(3, () -> {
            try {
                h.assertTrue(vx > 0, "Schockwelle stößt nicht weg: " + vx);
                h.assertTrue(s.phase() == 2, "Phase " + s.phase());
                List<FrostLurker> lurkers = level.getEntitiesOfClass(FrostLurker.class, new AABB(s.blockPosition()).inflate(6));
                h.assertTrue(lurkers.size() == 3 && lurkers.stream().noneMatch(FrostLurker::hidden), "Frostkriecher " + lurkers.size());
                lurkers.forEach(Entity::discard);
            } finally {
                IhTestPlayers.remove(p);
                s.discard();
            }
            h.succeed();
        });
    }
}
