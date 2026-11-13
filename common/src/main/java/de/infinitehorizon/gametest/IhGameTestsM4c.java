package de.infinitehorizon.gametest;

import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.core.fluid.IhFluidStack;
import de.infinitehorizon.dome.DomeData;
import de.infinitehorizon.dome.DomeGeneratorBlockEntity;
import de.infinitehorizon.environment.EnvironmentOverrides;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.menu.MachineMenu;
import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GameTests für M4c (27.7): Gast-Rechte an einem fremden Zerkleinerer, Beitritt mit {@code merged_into},
 * Kuppelschutz im Override Mond, Besitzübernahme beim ersten Öffnen.
 */
public final class IhGameTestsM4c {

    private static final Identifier MOON = Identifier.fromNamespaceAndPath("infinite_horizon", "moon");
    private static final Identifier OXYGEN = Identifier.fromNamespaceAndPath("infinite_horizon", "oxygen");
    private static final BlockPos CRUSHER = new BlockPos(1, 1, 1);
    private static final BlockPos DOME = new BlockPos(4, 1, 4);
    /** Wartezeit, bis die Kuppel aktiv ist. */
    private static final int DOME_READY_TICKS = 5;

    private IhGameTestsM4c() {
    }

    private static MachineBlockEntity crusher(GameTestHelper h) {
        h.setBlock(CRUSHER, ModBlocks.CRUSHER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        MachineBlockEntity be = h.getBlockEntity(CRUSHER, MachineBlockEntity.class);
        be.refreshLayout();
        return be;
    }

    /**
     * 27.7 M4c: Spieler B kann in einem Zerkleinerer von A keinen Slot ändern und ihn nicht abbauen; nach Einladung und
     * Beitritt kann er beides; die leere alte Agentur von B hat {@code merged_into} = A.
     */
    public static void guestThenMember(GameTestHelper h) {
        ServerPlayer a = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(3.5, 1, 1.5));
        ServerPlayer b = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 1, 3.5));
        AgencyData data = AgencyData.get(h.getLevel().getServer());
        Agency agencyA = AgencyManager.ensureFor(a);
        Agency agencyB = AgencyManager.ensureFor(b);
        h.assertFalse(agencyA.id().equals(agencyB.id()), "A und B in derselben Agentur");
        MachineBlockEntity be = crusher(h);
        be.setOwner(agencyA.id());
        be.setItem(0, new ItemStack(Items.RAW_IRON, 4));

        MachineMenu menuB = new MachineMenu(1, b.getInventory(), be);
        menuB.clicked(0, 0, ContainerInput.PICKUP, b);
        h.assertTrue(be.getItem(0).getCount() == 4 && menuB.getCarried().isEmpty(), "Gast konnte einen Slot ändern");
        h.assertFalse(b.gameMode.destroyBlock(h.absolutePos(CRUSHER)), "Gast konnte den Zerkleinerer abbauen");
        h.assertBlockPresent(ModBlocks.CRUSHER.get(), CRUSHER);

        h.assertTrue(data.invite(a.getUUID(), b.getUUID()) == AgencyData.ActionResult.OK, "Einladung abgelehnt");
        h.assertTrue(data.accept(b.getUUID(), agencyA.id(), 0L) == AgencyData.ActionResult.OK, "Beitritt abgelehnt");
        h.assertTrue(agencyA.isMember(b.getUUID()), "B ist nicht Mitglied von A");
        h.assertTrue(agencyA.id().equals(agencyB.mergedInto()), "alte Agentur von B: merged_into " + agencyB.mergedInto());

        menuB.clicked(0, 0, ContainerInput.PICKUP, b);
        h.assertTrue(be.getItem(0).isEmpty() && menuB.getCarried().getCount() == 4, "Mitglied konnte den Slot nicht ändern");
        menuB.setCarried(ItemStack.EMPTY);
        h.assertTrue(b.gameMode.destroyBlock(h.absolutePos(CRUSHER)), "Mitglied konnte den Zerkleinerer nicht abbauen");
        h.assertBlockNotPresent(ModBlocks.CRUSHER.get(), CRUSHER);
        IhTestPlayers.remove(a);
        IhTestPlayers.remove(b);
        h.succeed();
    }

    /** 27.7 M4c (Override Mond): B setzt in der aktiven Kuppel von A keinen Block, außerhalb schon. */
    public static void domeProtection(GameTestHelper h) {
        EnvironmentOverrides.Entry override = EnvironmentOverrides.set(h, MOON, null, null, 60);
        // Spieler abseits der Bauplätze (sonst blockiert ihr Körper das Setzen)
        ServerPlayer a = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 1, 6.5));
        ServerPlayer b = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(2.5, 1, 7.5));
        h.setBlock(DOME, ModBlocks.DOME_GENERATOR_T1.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        DomeGeneratorBlockEntity dome = h.getBlockEntity(DOME, DomeGeneratorBlockEntity.class);
        dome.refreshLayout();
        dome.setOwner(AgencyManager.ensureFor(a).id());
        dome.energyStorage().setAmountUnchecked(dome.energyStorage().getCapacity());
        dome.tank().setStackUnchecked(IhFluidStack.of(OXYGEN, 2000));
        BlockPos inside = new BlockPos(6, 0, 6);
        BlockPos outside = new BlockPos(20, 0, 20);
        h.setBlock(inside, Blocks.STONE);
        h.setBlock(outside, Blocks.STONE);
        h.runAtTickTime(DOME_READY_TICKS, () -> {
            h.assertTrue(DomeData.get(h.getLevel()).at(h.absolutePos(DOME)) != null, "Kuppel nicht aktiv");
            ItemStack stone = new ItemStack(Items.COBBLESTONE, 8);
            b.setItemInHand(InteractionHand.MAIN_HAND, stone);
            h.placeAt(b, stone, inside, Direction.UP);
            h.assertBlockNotPresent(Blocks.COBBLESTONE, inside.above());
            h.placeAt(b, stone, outside, Direction.UP);
            h.assertBlockPresent(Blocks.COBBLESTONE, outside.above());
            // der Besitzer darf in seiner Kuppel bauen
            ItemStack own = new ItemStack(Items.COBBLESTONE, 8);
            a.setItemInHand(InteractionHand.MAIN_HAND, own);
            h.placeAt(a, own, inside, Direction.UP);
            h.assertBlockPresent(Blocks.COBBLESTONE, inside.above());
            EnvironmentOverrides.remove(override);
            IhTestPlayers.remove(a);
            IhTestPlayers.remove(b);
            h.succeed();
        });
    }

    /** 27.7 M4c: Ein per {@code setBlock} gesetzter Zerkleinerer gehört nach dem ersten Öffnen der Agentur des Öffners. */
    public static void unownedClaimedOnOpen(GameTestHelper h) {
        MachineBlockEntity be = crusher(h);
        h.assertTrue(be.storedOwner() == null, "setBlock hat einen Besitzer gesetzt");
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 1, 3.5));
        BlockPos abs = h.absolutePos(CRUSHER);
        h.getLevel().getBlockState(abs).useWithoutItem(h.getLevel(), p,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.SOUTH, abs, false));
        Agency agency = AgencyManager.ensureFor(p);
        h.assertTrue(agency.id().equals(be.storedOwner()), "Besitzer nach dem Öffnen: " + be.storedOwner());
        IhTestPlayers.remove(p);
        h.succeed();
    }
}
