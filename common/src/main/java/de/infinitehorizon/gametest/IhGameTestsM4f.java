package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.agency.AtlasService;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModDataComponents;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.space.LandingService;
import de.infinitehorizon.suit.HelmetLamp;
import de.infinitehorizon.suit.SuitModules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** GameTests für M4f (27.7): Atlas-Geschenk, Entdecker, Modul-Ein- und -Ausbau, Helmlampe. */
public final class IhGameTestsM4f {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");

    private IhGameTestsM4f() {
    }

    private static int atlases(ServerPlayer p) {
        return p.getInventory().countItem(ModItems.PLANETARY_ATLAS.get());
    }

    /** Das Atlas-Geschenk kommt genau einmal, auch für einen Spieler ohne Tag aus einer M3-Welt. */
    public static void atlasGiftOnce(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 1, 1.5));
        InfiniteHorizon.onPlayerJoin(p);
        InfiniteHorizon.onPlayerJoin(p);
        h.assertTrue(atlases(p) == 1, "nach zwei Logins " + atlases(p) + " Atlanten");
        // Spieler aus einer M3-Welt: Handbuch schon erhalten, Atlas-Tag fehlt
        p.removeTag(AtlasService.ATLAS_GIVEN_TAG);
        p.getInventory().clearContent();
        InfiniteHorizon.onPlayerJoin(p);
        h.assertTrue(atlases(p) == 1, "M3-Spieler: " + atlases(p) + " Atlanten");
        h.assertTrue(p.getInventory().countItem(ModItems.ENGINEERS_HANDBOOK.get()) == 0, "Handbuch doppelt geschenkt");
        IhTestPlayers.remove(p);
        h.succeed();
    }

    /** Ein Spieler betritt {@code moon}: Im Atlas seiner Agentur steht „besucht“, und sie ist Entdecker. */
    public static void moonVisitDiscoverer(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.CREATIVE, new Vec3(1.5, 1, 1.5));
        Agency agency = AgencyManager.ensureFor(p);
        AgencyData data = AgencyData.get(h.getLevel().getServer());
        data.setDiscoverer(MOON, null);
        ServerLevel moon = h.getLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, MOON));
        h.assertTrue(moon != null, "Dimension Mond fehlt");
        BlockPos at = LandingService.find(moon, 0, 0);
        p.teleport(new TeleportTransition(moon, Vec3.atBottomCenterOf(at), Vec3.ZERO, 0, 0, TeleportTransition.DO_NOTHING));
        h.succeedWhen(() -> {
            h.assertTrue(agency.visitedDimensions().contains(MOON), "Mond nicht besucht");
            h.assertTrue(agency.id().equals(data.discoverer(MOON)), "Entdecker: " + data.discoverer(MOON));
            String atlas = AtlasService.json(h.getLevel().getServer(), agency);
            h.assertTrue(atlas.contains("\"visited\":[") && atlas.contains(MOON.toString()), "Atlas: " + atlas);
            IhTestPlayers.remove(p);
        });
    }

    private static ProcessingMachineBlockEntity table(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.ASSEMBLY_TABLE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        ProcessingMachineBlockEntity be = h.getBlockEntity(pos, ProcessingMachineBlockEntity.class);
        be.refreshLayout();
        return be;
    }

    private static List<ItemStack> outputs(ProcessingMachineBlockEntity be) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = be.layout().outputStart(); i < be.layout().upgradeStart(); i++) {
            if (!be.getItem(i).isEmpty()) {
                out.add(be.getItem(i));
            }
        }
        return out;
    }

    /** Helm + {@code helmet_lamp} im Montagetisch → Helm mit Modul; Helm allein → Helm + Lampe in den Nebenausgaben. */
    public static void moduleInstallRemove(GameTestHelper h) {
        ProcessingMachineBlockEntity install = table(h, new BlockPos(1, 1, 1));
        ProcessingMachineBlockEntity remove = table(h, new BlockPos(3, 1, 1));
        install.setItem(0, new ItemStack(ModItems.PRESSURE_SUIT_HELMET.get()));
        install.setItem(1, new ItemStack(ModItems.simple("helmet_lamp").get()));
        ItemStack fitted = new ItemStack(ModItems.PRESSURE_SUIT_HELMET.get());
        fitted.set(ModDataComponents.SUIT_MODULES.get(), List.of(HelmetLamp.MODULE));
        remove.setItem(0, fitted);
        h.onEachTick(() -> {
            install.energyStorage().setAmountUnchecked(install.energyStorage().getCapacity());
            remove.energyStorage().setAmountUnchecked(remove.energyStorage().getCapacity());
        });
        h.succeedWhen(() -> {
            List<ItemStack> a = outputs(install);
            h.assertTrue(a.size() == 1 && a.getFirst().is(ModItems.PRESSURE_SUIT_HELMET.get())
                    && SuitModules.modules(a.getFirst()).equals(List.of(HelmetLamp.MODULE)), "Einbau: " + a);
            h.assertTrue(install.getItem(0).isEmpty() && install.getItem(1).isEmpty(), "Einbau: Zutaten nicht verbraucht");
            List<ItemStack> b = outputs(remove);
            h.assertTrue(b.size() == 2 && b.get(0).is(ModItems.PRESSURE_SUIT_HELMET.get())
                    && SuitModules.modules(b.get(0)).isEmpty() && b.get(1).is(ModItems.simple("helmet_lamp").get()), "Ausbau: " + b);
        });
    }

    /** Mit Lampe steht an der Kopfposition ein {@code light}-Block (Level 14), nach dem Absetzen nicht mehr. */
    public static void helmetLampLight(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.CREATIVE, new Vec3(2.5, 1, 2.5));
        p.getAbilities().flying = true;
        ItemStack helmet = new ItemStack(ModItems.PRESSURE_SUIT_HELMET.get());
        helmet.set(ModDataComponents.SUIT_MODULES.get(), List.of(HelmetLamp.MODULE));
        p.setItemSlot(EquipmentSlot.HEAD, helmet);
        int level = BalanceManager.get().entry("items", HelmetLamp.MODULE.toString())
                .map(o -> o.get("light_level").getAsInt()).orElseThrow();
        AtomicReference<BlockPos> lit = new AtomicReference<>();
        h.onEachTick(() -> {
            if (lit.get() == null) {
                BlockPos head = BlockPos.containing(p.getEyePosition());
                BlockState s = p.level().getBlockState(head);
                if (s.is(Blocks.LIGHT) && s.getValue(LightBlock.LEVEL) == level) {
                    lit.set(head);
                    p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                }
            }
        });
        h.succeedWhen(() -> {
            h.assertTrue(lit.get() != null, "kein Lichtblock Level " + level + " an der Kopfposition");
            BlockState s = p.level().getBlockState(lit.get());
            h.assertFalse(s.is(Blocks.LIGHT), "Lichtblock bleibt nach dem Absetzen: "
                    + BuiltInRegistries.BLOCK.getKey(s.getBlock()));
            IhTestPlayers.remove(p);
        });
    }
}
