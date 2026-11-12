package de.infinitehorizon.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.block.MachineStatus;
import de.infinitehorizon.data.ServerConfig;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.recipe.ResearchRecipeLock;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.research.ResearchService;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GameTests für M4a (27.7): Rezeptsperre im Montagetisch (Knoten {@code suit_t1}), {@code research_required = false},
 * Forschungsterminal öffnet Stufe 1 und vergibt {@code ground_station}.
 */
public final class IhGameTestsM4a {

    private static final Identifier SUIT_T1 = Identifier.fromNamespaceAndPath("infinite_horizon", "suit_t1");
    private static final Identifier GROUND_STATION = Identifier.fromNamespaceAndPath("infinite_horizon", "ground_station");
    /** Wartezeit, in der ein gesperrter Montagetisch nichts tun darf. */
    private static final int LOCKED_CHECK_TICKS = 20;
    /** Zeitgrenze für den Helm nach dem Erforschen (27.7: aufgerundet(200 × 1,1)). */
    private static final int HELMET_TICKS = 220;

    private IhGameTestsM4a() {
    }

    /** Neue Agentur ohne Forschung mit offener Stufe 1 (wie nach dem Setzen eines Terminals); gibt den Leiter zurück. */
    private static UUID agencyWithTier1(MinecraftServer server) {
        UUID leader = UUID.randomUUID();
        AgencyData data = AgencyData.get(server);
        Agency a = data.ensureFor(leader, 0L);
        a.progress().openTier(1);
        data.setDirty();
        return leader;
    }

    /** Montagetisch der Agentur von {@code leader} mit vollem Strom und den Zutaten von {@code assembly/pressure_suit_helmet}. */
    private static ProcessingMachineBlockEntity helmetTable(GameTestHelper h, BlockPos pos, UUID leader) {
        h.setBlock(pos, ModBlocks.ASSEMBLY_TABLE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        ProcessingMachineBlockEntity t = h.getBlockEntity(pos, ProcessingMachineBlockEntity.class);
        t.refreshLayout();
        t.setOwner(AgencyData.get(h.getLevel().getServer()).agencyOf(leader).orElseThrow().id());
        t.energyStorage().setAmountUnchecked(t.energyStorage().getCapacity());
        t.setItem(0, new ItemStack(ModItems.simple("aluminum_plate").get(), 4));
        t.setItem(1, new ItemStack(ModItems.simple("plastic").get(), 1));
        t.setItem(2, new ItemStack(Items.GLASS, 4));
        t.setItem(3, new ItemStack(ModItems.simple("circuit").get(), 1));
        return t;
    }

    private static MachineStatus status(ProcessingMachineBlockEntity t) {
        return t.getBlockState().getValue(MachineBlock.STATUS);
    }

    /**
     * 27.7 M4a: Ein Montagetisch einer Agentur ohne {@code suit_t1} mit den Zutaten des Helms bleibt {@code idle}; nach
     * Gutschrift von 10 FP und Erforschen (FP danach 0) ist der Helm in höchstens 220 t fertig.
     */
    public static void assemblyLockedUntilResearched(GameTestHelper h) {
        ServerConfig.overrideForTest(ResearchRecipeLock.CONFIG_KEY, new JsonPrimitive(true));
        MinecraftServer server = h.getLevel().getServer();
        UUID leader = agencyWithTier1(server);
        ProcessingMachineBlockEntity t = helmetTable(h, new BlockPos(1, 1, 1), leader);
        Item helmet = ModItems.PRESSURE_SUIT_HELMET.get();
        AtomicInteger researchedAt = new AtomicInteger(-1);
        h.runAtTickTime(LOCKED_CHECK_TICKS, () -> {
            h.assertTrue(t.countOutput(helmet) == 0, "Helm trotz Sperre hergestellt");
            h.assertTrue(status(t) == MachineStatus.IDLE, "Status " + status(t) + " statt idle");
            h.assertTrue(t.recipeLocked(), "Sperre wird nicht gemeldet (Tooltip status.locked)");
            Agency a = AgencyData.get(server).agencyOf(leader).orElseThrow();
            a.progress().addFp(10 * ResearchService.scale());
            ResearchService.Result r = ResearchService.research(server, leader, SUIT_T1);
            h.assertTrue(r == ResearchService.Result.OK, "Erforschen: " + r);
            h.assertTrue(a.progress().fp() == 0, "FP nach dem Erforschen " + a.progress().fp());
            researchedAt.set((int) h.getTick());
        });
        h.succeedWhen(() -> {
            h.assertTrue(researchedAt.get() >= 0, "noch nicht erforscht");
            h.assertTrue(t.countOutput(helmet) == 1, "Helme " + t.countOutput(helmet));
            h.assertTrue(h.getTick() - researchedAt.get() <= HELMET_TICKS, "Helm erst nach " + (h.getTick() - researchedAt.get()) + " t");
            h.assertFalse(t.recipeLocked(), "Sperre nach dem Erforschen");
        });
    }

    /** 27.7 M4a: Mit {@code research_required = false} baut derselbe Tisch den Helm sofort (ohne Forschung). */
    public static void assemblyFreeWhenNotRequired(GameTestHelper h) {
        JsonElement previous = ServerConfig.overrideForTest(ResearchRecipeLock.CONFIG_KEY, new JsonPrimitive(false));
        UUID leader = agencyWithTier1(h.getLevel().getServer());
        ProcessingMachineBlockEntity t = helmetTable(h, new BlockPos(1, 1, 1), leader);
        Item helmet = ModItems.PRESSURE_SUIT_HELMET.get();
        h.runAtTickTime(2, () -> h.assertTrue(status(t) == MachineStatus.RUNNING, "läuft nicht sofort: " + status(t)));
        h.succeedWhen(() -> {
            h.assertTrue(t.countOutput(helmet) == 1, "Helme " + t.countOutput(helmet));
            ServerConfig.overrideForTest(ResearchRecipeLock.CONFIG_KEY, previous == null ? new JsonPrimitive(true) : previous);
        });
    }

    /** 27.7 M4a: Ein Spieler setzt ein {@code research_terminal}; Stufe 1 ist offen, er hat {@code ground_station}. */
    public static void terminalOpensTier1(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 1, 4.5));
        MinecraftServer server = h.getLevel().getServer();
        ItemStack terminal = new ItemStack(ModItems.RESEARCH_TERMINAL.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, terminal);
        h.placeAt(p, terminal, new BlockPos(2, 0, 2), Direction.UP);
        h.succeedWhen(() -> {
            h.assertBlockPresent(ModBlocks.RESEARCH_TERMINAL.get(), new BlockPos(2, 1, 2));
            Agency a = AgencyData.get(server).agencyOf(p.getUUID()).orElseThrow();
            h.assertTrue(a.progress().isTierOpen(1), "Stufe 1 nicht offen: " + a.progress().openTiers());
            h.assertFalse(a.progress().isTierOpen(2), "Stufe 2 ohne Mondbesuch offen");
            AdvancementHolder adv = server.getAdvancements().get(GROUND_STATION);
            h.assertTrue(adv != null, "Erfolg ground_station fehlt");
            h.assertTrue(p.getAdvancements().getOrStartProgress(adv).isDone(), "ground_station nicht vergeben");
            IhTestPlayers.remove(p);
        });
    }
}
