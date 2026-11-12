package de.infinitehorizon.gametest;

import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.MachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/** GameTests für M3a (27.5): Agentur beim Login, Besitz beim Platzieren, {@code merged_into}, Befehl {@code transfer}. */
public final class IhGameTestsM3a {

    private IhGameTestsM3a() {
    }

    private static AgencyData data(GameTestHelper h) {
        return AgencyManager.data(h.getLevel().getServer());
    }

    /** Ein Mock-Spieler setzt einen Zerkleinerer: Besitzer ist seine Agentur; der Login legte sie an (23.1). */
    public static void placedCrusherHasOwner(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        AgencyManager.onLogin(player);
        Optional<Agency> agency = data(h).agencyOf(player.getUUID());
        h.assertTrue(agency.isPresent(), "Login hat keine Agentur angelegt");
        h.assertTrue(agency.get().visitedDimensions().contains(player.level().dimension().identifier()),
                "Login-Dimension fehlt in visited_dimensions");

        // Spieler über den Bauplatz stellen, damit er die Platzierung nicht blockiert
        player.setPos(h.absoluteVec(new Vec3(1.5, 4, 1.5)));
        ItemStack stack = new ItemStack(ModBlocks.CRUSHER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        h.placeAt(player, stack, new BlockPos(1, 0, 1), Direction.UP);
        BlockPos pos = new BlockPos(1, 1, 1);
        h.assertBlockPresent(ModBlocks.CRUSHER.get(), pos);
        MachineBlockEntity be = h.getBlockEntity(pos, MachineBlockEntity.class);
        h.assertTrue(agency.get().id().equals(be.owner()),
                "owner_agency " + be.storedOwner() + ", erwartet " + agency.get().id());
        h.succeed();
    }

    /** Ein per {@code setBlock} gesetzter Zerkleinerer hat keinen Besitzer (23.1). */
    public static void setBlockCrusherHasNoOwner(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.CRUSHER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        MachineBlockEntity be = h.getBlockEntity(pos, MachineBlockEntity.class);
        h.assertTrue(be.storedOwner() == null && be.owner() == null, "Besitzer gesetzt: " + be.storedOwner());
        h.succeed();
    }

    /** Besitz folgt {@code merged_into} über zwei Stufen (A → B → C) und wird beim Zugriff umgeschrieben. */
    public static void ownerFollowsMergeChain(GameTestHelper h) {
        AgencyData d = data(h);
        long tick = h.getLevel().getGameTime();
        UUID a = d.ensureFor(UUID.randomUUID(), tick).id();
        UUID b = d.ensureFor(UUID.randomUUID(), tick).id();
        UUID c = d.ensureFor(UUID.randomUUID(), tick).id();
        BlockPos pos = new BlockPos(1, 1, 1);
        h.setBlock(pos, ModBlocks.CRUSHER.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        MachineBlockEntity be = h.getBlockEntity(pos, MachineBlockEntity.class);
        be.setOwner(a);
        h.assertTrue(d.transfer(a, b) == AgencyData.TransferResult.OK, "Übertragung A → B");
        h.assertTrue(d.transfer(b, c) == AgencyData.TransferResult.OK, "Übertragung B → C");
        h.assertTrue(d.transfer(c, a) == AgencyData.TransferResult.CYCLE, "Kreis C → A nicht erkannt");
        h.assertTrue(c.equals(be.owner()), "Besitzer " + be.owner() + ", erwartet C");
        h.assertTrue(c.equals(be.storedOwner()), "owner_agency nicht umgeschrieben");
        h.succeed();
    }

    /** {@code /infinite_horizon agency transfer <von> <an>} mit Agentur-IDs (Rechtestufe 2). */
    public static void transferCommand(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        AgencyData d = data(h);
        long tick = h.getLevel().getGameTime();
        UUID from = d.ensureFor(UUID.randomUUID(), tick).id();
        UUID to = d.ensureFor(UUID.randomUUID(), tick).id();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                "/infinite_horizon agency transfer " + from + " " + to);
        UUID merged = d.byId(from).map(Agency::mergedInto).orElse(null);
        h.assertTrue(to.equals(merged), "merged_into " + merged + ", erwartet " + to);
        h.assertTrue(d.resolve(from).equals(to), "Auflösung zeigt nicht auf das Ziel");
        h.succeed();
    }
}
