package de.sternenwerft.gametest;

import de.sternenwerft.Sternenwerft;
import de.sternenwerft.api.energy.EnergyTarget;
import de.sternenwerft.api.fluid.FluidTarget;
import de.sternenwerft.block.MachineStatus;
import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.data.ServerConfig;
import de.sternenwerft.machine.CombustionGeneratorBlockEntity;
import de.sternenwerft.machine.FluidPumpBlockEntity;
import de.sternenwerft.machine.FluidTankBlockEntity;
import de.sternenwerft.machine.MachineBlock;
import de.sternenwerft.machine.MachineBlockEntity;
import de.sternenwerft.machine.MachineLayout;
import de.sternenwerft.machine.ProcessingMachineBlockEntity;
import de.sternenwerft.machine.ProcessingMath;
import de.sternenwerft.menu.MachineMenu;
import de.sternenwerft.network.MachineConfigPayload;
import de.sternenwerft.platform.Services;
import de.sternenwerft.registry.ModBlocks;
import de.sternenwerft.registry.ModDataComponents;
import de.sternenwerft.registry.ModFluids;
import de.sternenwerft.registry.ModItems;
import de.sternenwerft.world.FluidSphereFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

/**
 * Ausführliche GameTests für M1–M2c (Testbericht {@code docs/dev/testbericht-m2abc.md}). Registrierung über
 * {@link SwTestCatalog} (erzeugt aus {@code common/gametests.json}).
 */
public final class SwGameTestsExtended {

    private SwGameTestsExtended() {
    }

    // ------------------------------------------------------------------ Hilfen

    private static <T extends BlockEntity> T place(GameTestHelper h, BlockPos pos, Block block, Direction facing, Class<T> type) {
        BlockState s = block.defaultBlockState();
        if (s.hasProperty(MachineBlock.FACING)) {
            s = s.setValue(MachineBlock.FACING, facing);
        }
        h.setBlock(pos, s);
        T be = h.getBlockEntity(pos, type);
        if (be instanceof MachineBlockEntity m) {
            m.refreshLayout();
        }
        return be;
    }

    private static void fullEnergy(MachineBlockEntity be) {
        be.energyStorage().setAmountUnchecked(be.energyStorage().getCapacity());
    }

    private static Item simple(String path) {
        return ModItems.simple(path).get();
    }

    private static final Identifier WATER = Identifier.withDefaultNamespace("water");
    private static final Identifier OIL = Sternenwerft.id("crude_oil");

    // ------------------------------------------------------------------ Generator

    /** Kohle brennt im Verbrennungsgenerator 1.600 t (Vanilla-Brennwert, generators.json). */
    public static void generatorCoalBurnTime(GameTestHelper h) {
        CombustionGeneratorBlockEntity g = place(h, new BlockPos(1, 1, 1), ModBlocks.COMBUSTION_GENERATOR.get(), Direction.NORTH,
                CombustionGeneratorBlockEntity.class);
        g.setItem(0, new ItemStack(Items.COAL));
        h.runAfterDelay(3, () -> {
            h.assertTrue(g.burnTotal() == 1600, "Brennzeit Kohle " + g.burnTotal() + ", erwartet 1600");
            h.assertTrue(g.status() == MachineStatus.RUNNING, "Status " + g.status());
            h.assertTrue(g.energyStorage().getAmount() > 0, "keine Energie erzeugt");
            h.succeed();
        });
    }

    /** Ein Lavaeimer hinterlässt den leeren Eimer im Brennstoffslot (6.3). */
    public static void generatorLavaBucketKeepsBucket(GameTestHelper h) {
        CombustionGeneratorBlockEntity g = place(h, new BlockPos(1, 1, 1), ModBlocks.COMBUSTION_GENERATOR.get(), Direction.NORTH,
                CombustionGeneratorBlockEntity.class);
        g.setItem(0, new ItemStack(Items.LAVA_BUCKET));
        h.runAfterDelay(3, () -> {
            h.assertTrue(g.getItem(0).is(Items.BUCKET), "Brennstoffslot: " + g.getItem(0));
            h.assertTrue(g.burnTotal() > 0, "Lava brennt nicht");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Maschinen

    /** Volle Ausgabeslots → Statuslampe error (4.1). */
    public static void crusherOutputFullError(GameTestHelper h) {
        ProcessingMachineBlockEntity c = place(h, new BlockPos(1, 1, 1), ModBlocks.CRUSHER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        fullEnergy(c);
        MachineLayout l = c.layout();
        c.setItem(0, new ItemStack(Items.RAW_IRON));
        for (int i = l.outputStart(); i < l.upgradeStart(); i++) {
            c.setItem(i, new ItemStack(Items.STONE, 64));
        }
        h.runAfterDelay(3, () -> {
            h.assertTrue(c.status() == MachineStatus.ERROR, "Status " + c.status() + ", erwartet error");
            h.succeed();
        });
    }

    /** 4 speed_upgrade im Zerkleinerer: 63 t und 101 J/t (4.2). */
    public static void crusherFourSpeedUpgrades(GameTestHelper h) {
        ProcessingMachineBlockEntity c = place(h, new BlockPos(1, 1, 1), ModBlocks.CRUSHER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        c.setItem(c.layout().upgradeStart(), new ItemStack(simple("speed_upgrade"), 4));
        ProcessingMath.Effective eff = c.effectiveFor(h.getLevel(), new ItemStack(Items.RAW_IRON));
        h.assertTrue(eff != null && eff.ticks() == 63 && eff.energyPerTick() == 101,
                "Wirkung " + eff + ", erwartet 63 t / 101 J/t");
        h.succeed();
    }

    /** Presse ohne Pressform macht nichts (5.2). */
    public static void pressWithoutMoldIdle(GameTestHelper h) {
        ProcessingMachineBlockEntity p = place(h, new BlockPos(1, 1, 1), ModBlocks.PRESS.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        fullEnergy(p);
        p.setItem(0, new ItemStack(simple("titanium_ingot")));
        h.runAfterDelay(20, () -> {
            h.assertTrue(p.progress() == 0 && p.status() == MachineStatus.IDLE, "Fortschritt " + p.progress() + ", Status " + p.status());
            h.assertTrue(p.countOutput(simple("titanium_plate")) == 0, "Platte ohne Form gepresst");
            h.succeed();
        });
    }

    /** Elektroofen schmilzt Vanilla-Rezepte: raw_iron → iron_ingot in höchstens 110 t. */
    public static void furnaceVanillaSmelting(GameTestHelper h) {
        ProcessingMachineBlockEntity f = place(h, new BlockPos(1, 1, 1), ModBlocks.ELECTRIC_FURNACE.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        fullEnergy(f);
        f.setItem(0, new ItemStack(Items.RAW_IRON));
        h.succeedWhen(() -> h.assertTrue(f.countOutput(Items.IRON_INGOT) == 1, "Kein Eisenbarren"));
    }

    /** Statuslampe: idle → running → idle, wenn nichts mehr zu tun ist. */
    public static void crusherStatusCycle(GameTestHelper h) {
        ProcessingMachineBlockEntity c = place(h, new BlockPos(1, 1, 1), ModBlocks.CRUSHER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        h.assertTrue(c.status() == MachineStatus.IDLE, "Startstatus " + c.status());
        fullEnergy(c);
        c.setItem(0, new ItemStack(Items.RAW_IRON));
        h.runAfterDelay(3, () -> h.assertTrue(c.status() == MachineStatus.RUNNING, "Status beim Arbeiten " + c.status()));
        h.succeedWhen(() -> h.assertTrue(c.countOutput(simple("iron_dust")) == 2 && c.status() == MachineStatus.IDLE,
                "Status nach dem Vorgang " + c.status()));
    }

    // ------------------------------------------------------------------ Trichter und Slots

    /** Trichter oben füllt den Zerkleinerer, Trichter unten zieht die Ausgaben ab (4.1). */
    public static void hopperFeedsAndDrainsCrusher(GameTestHelper h) {
        BlockPos crusherPos = new BlockPos(2, 2, 2);
        h.setBlock(new BlockPos(2, 3, 2), Blocks.HOPPER);
        HopperBlockEntity top = h.getBlockEntity(new BlockPos(2, 3, 2), HopperBlockEntity.class);
        top.setItem(0, new ItemStack(Items.RAW_IRON));
        ProcessingMachineBlockEntity c = place(h, crusherPos, ModBlocks.CRUSHER.get(), Direction.NORTH, ProcessingMachineBlockEntity.class);
        fullEnergy(c);
        h.setBlock(new BlockPos(2, 1, 2), Blocks.HOPPER);
        HopperBlockEntity bottom = h.getBlockEntity(new BlockPos(2, 1, 2), HopperBlockEntity.class);
        Item dust = simple("iron_dust");
        h.succeedWhen(() -> h.assertTrue(bottom.countItem(dust) == 2, "Trichter unten: " + bottom.countItem(dust) + " iron_dust"));
    }

    /** Trichter legen nie etwas in Upgrade-Slots (4.1). */
    public static void hopperNeverFillsUpgradeSlot(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 3, 2), Blocks.HOPPER);
        HopperBlockEntity top = h.getBlockEntity(new BlockPos(2, 3, 2), HopperBlockEntity.class);
        top.setItem(0, new ItemStack(simple("speed_upgrade")));
        ProcessingMachineBlockEntity c = place(h, new BlockPos(2, 2, 2), ModBlocks.CRUSHER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        h.runAfterDelay(30, () -> {
            MachineLayout l = c.layout();
            for (int i = l.upgradeStart(); i < l.fuelStart(); i++) {
                h.assertTrue(c.getItem(i).isEmpty(), "Upgrade-Slot " + i + " befüllt: " + c.getItem(i));
            }
            h.succeed();
        });
    }

    /** Regeln für Füll-/Leerslot an Trichtern (4.1): aus dem Füllslot nur volle, aus dem Leerslot nur leere Behälter. */
    public static void containerSlotHopperRules(GameTestHelper h) {
        FluidTankBlockEntity t = place(h, new BlockPos(1, 1, 1), ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        MachineLayout l = t.layout();
        ItemStack empty = new ItemStack(Items.BUCKET);
        ItemStack full = new ItemStack(Items.WATER_BUCKET);
        h.assertTrue(t.canPlaceItemThroughFace(l.fillStart(), empty, Direction.UP), "Füllslot muss leere Eimer annehmen");
        h.assertFalse(t.canPlaceItemThroughFace(l.fillStart(), full, Direction.UP), "Füllslot darf volle Eimer nicht annehmen");
        h.assertTrue(t.canPlaceItemThroughFace(l.drainStart(), full, Direction.UP), "Leerslot muss volle Eimer annehmen");
        h.assertFalse(t.canTakeItemThroughFace(l.fillStart(), empty, Direction.DOWN), "Füllslot: leerer Eimer darf nicht raus");
        h.assertTrue(t.canTakeItemThroughFace(l.fillStart(), full, Direction.DOWN), "Füllslot: voller Eimer muss raus");
        h.assertTrue(t.canTakeItemThroughFace(l.drainStart(), empty, Direction.DOWN), "Leerslot: leerer Eimer muss raus");
        h.assertFalse(t.canTakeItemThroughFace(l.drainStart(), full, Direction.DOWN), "Leerslot: voller Eimer darf nicht raus");
        h.succeed();
    }

    /** Wassereimer im Leerslot → 1.000 mB Wasser im Tank, leerer Eimer bleibt (4.1). */
    public static void tankDrainWaterBucket(GameTestHelper h) {
        FluidTankBlockEntity t = place(h, new BlockPos(1, 1, 1), ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        int drain = t.layout().drainStart();
        t.setItem(drain, new ItemStack(Items.WATER_BUCKET));
        h.succeedWhen(() -> h.assertTrue(t.tank().stack().is(WATER) && t.tank().stack().amount() == 1000 && t.getItem(drain).is(Items.BUCKET),
                "Tank " + t.tank().stack() + ", Leerslot " + t.getItem(drain)));
    }

    /** Abgebauter Tank behält den Inhalt als Komponente; Platzieren stellt ihn wieder her (4.1). */
    public static void tankKeepsContentsWhenBroken(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        FluidTankBlockEntity t = place(h, pos, ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        t.tank().setStackUnchecked(SwFluidStack.of(OIL, 3000));
        List<ItemStack> drops = Block.getDrops(h.getBlockState(pos), h.getLevel(), h.absolutePos(pos), t, null, ItemStack.EMPTY);
        h.assertTrue(drops.size() == 1, "Drops: " + drops);
        SwFluidStack stored = drops.getFirst().get(ModDataComponents.FLUID.get());
        h.assertTrue(stored != null && stored.is(OIL) && stored.amount() == 3000, "Komponente am Item: " + stored);
        FluidTankBlockEntity t2 = place(h, new BlockPos(3, 1, 1), ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        t2.applyComponentsFromItemStack(drops.getFirst());
        h.assertTrue(t2.tank().stack().is(OIL) && t2.tank().stack().amount() == 3000, "Wiederhergestellt: " + t2.tank().stack());
        h.succeed();
    }

    // ------------------------------------------------------------------ GUI-Menü und Netzwerk

    /**
     * Die Block-Entity erzeugt ein Maschinenmenü mit ihrem Slot-Aufbau; die Öffnungsdaten passen dazu.
     * (Das echte Öffnen per Rechtsklick prüft der Client-Test – Mock-Spieler haben keine Netzwerkverbindung.)
     */
    public static void menuMatchesLayout(GameTestHelper h) {
        ProcessingMachineBlockEntity c = place(h, new BlockPos(1, 1, 1), ModBlocks.CRUSHER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        ServerPlayer player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var menu = c.createMenu(1, player.getInventory(), player);
        h.assertTrue(menu instanceof MachineMenu, "Menü: " + menu);
        MachineMenu m = (MachineMenu) menu;
        h.assertTrue(m.openData().layout().equals(c.layout()), "Layout " + m.openData().layout() + " ≠ " + c.layout());
        h.assertTrue(m.slots.size() == c.layout().total() + 36, "Slots im Menü: " + m.slots.size());
        h.succeed();
    }

    /** Shift-Klick (4.1): Upgrade → Upgrade-Slot, Pressform → Formslot, sonst Eingabe. */
    public static void shiftClickRouting(GameTestHelper h) {
        ProcessingMachineBlockEntity press = place(h, new BlockPos(1, 1, 1), ModBlocks.PRESS.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        ServerPlayer player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        player.getInventory().add(new ItemStack(simple("speed_upgrade"), 2));
        player.getInventory().add(new ItemStack(simple("press_mold_plate")));
        player.getInventory().add(new ItemStack(simple("titanium_ingot"), 5));
        MachineMenu menu = new MachineMenu(1, player.getInventory(), press);
        for (Item item : List.of(simple("speed_upgrade"), simple("press_mold_plate"), simple("titanium_ingot"))) {
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot s = menu.slots.get(i);
                if (s.container == player.getInventory() && s.getItem().is(item)) {
                    menu.quickMoveStack(player, i);
                    break;
                }
            }
        }
        MachineLayout l = press.layout();
        h.assertTrue(press.getItem(l.upgradeStart()).is(simple("speed_upgrade")), "Upgrade-Slot: " + press.getItem(l.upgradeStart()));
        h.assertTrue(press.getItem(l.moldStart()).is(simple("press_mold_plate")), "Formslot: " + press.getItem(l.moldStart()));
        h.assertTrue(press.getItem(0).is(simple("titanium_ingot")) && press.getItem(0).getCount() == 5, "Eingabe: " + press.getItem(0));
        h.succeed();
    }

    /** MachineConfigPayload ändert die Ausgangsseite nur in Reichweite (3A.5). */
    public static void configPayloadRange(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        FluidTankBlockEntity t = place(h, pos, ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        BlockPos abs = h.absolutePos(pos);
        ServerPlayer player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(abs.getX() + 2.5, abs.getY(), abs.getZ() + 0.5);
        MachineConfigPayload.handle(new MachineConfigPayload(abs, Direction.EAST.ordinal(), -1), player);
        h.assertTrue(t.outputSide() == Direction.EAST, "In Reichweite nicht geändert: " + t.outputSide());
        player.setPos(abs.getX() + 30.5, abs.getY(), abs.getZ() + 0.5);
        MachineConfigPayload.handle(new MachineConfigPayload(abs, Direction.WEST.ordinal(), -1), player);
        h.assertTrue(t.outputSide() == Direction.EAST, "Aus der Ferne geändert: " + t.outputSide());
        h.succeed();
    }

    // ------------------------------------------------------------------ Netze

    /** Kupferkabel begrenzt auf 256 J/t (transport.json). */
    public static void cableThroughputLimit(GameTestHelper h) {
        CombustionGeneratorBlockEntity g = place(h, new BlockPos(1, 1, 1), ModBlocks.COMBUSTION_GENERATOR.get(), Direction.NORTH,
                CombustionGeneratorBlockEntity.class);
        fullEnergy(g);
        h.setBlock(new BlockPos(2, 1, 1), ModBlocks.COPPER_CABLE.get());
        ProcessingMachineBlockEntity c = place(h, new BlockPos(3, 1, 1), ModBlocks.CRUSHER.get(), Direction.NORTH,
                ProcessingMachineBlockEntity.class);
        h.runAfterDelay(5, () -> {
            long got = c.energyStorage().getAmount();
            h.assertTrue(got > 0 && got <= 6 * 256, "Nach 5 t angekommen: " + got + " J (höchstens 6 × 256)");
            h.succeed();
        });
    }

    /** Kupferrohr begrenzt auf 200 mB/t (transport.json). */
    public static void pipeThroughputLimit(GameTestHelper h) {
        FluidTankBlockEntity a = place(h, new BlockPos(1, 1, 1), ModBlocks.FLUID_TANK.get(), Direction.EAST, FluidTankBlockEntity.class);
        a.tank().setStackUnchecked(SwFluidStack.of(WATER, 8000));
        h.setBlock(new BlockPos(2, 1, 1), ModBlocks.COPPER_PIPE.get());
        FluidTankBlockEntity b = place(h, new BlockPos(3, 1, 1), ModBlocks.FLUID_TANK.get(), Direction.SOUTH, FluidTankBlockEntity.class);
        h.runAfterDelay(5, () -> {
            long got = b.tank().stack().amount();
            h.assertTrue(got > 0 && got <= 6 * 200, "Nach 5 t angekommen: " + got + " mB (höchstens 6 × 200)");
            h.succeed();
        });
    }

    // ------------------------------------------------------------------ Loader-Schnittstellen

    /** Energie über die Loader-API in den Zerkleinerer (NeoForge Capability, Fabric Team Reborn). */
    public static void energyPlatformApiInsert(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ProcessingMachineBlockEntity c = place(h, pos, ModBlocks.CRUSHER.get(), Direction.NORTH, ProcessingMachineBlockEntity.class);
        EnergyTarget target = Services.ENERGY.findTarget(h.getLevel(), h.absolutePos(pos), Direction.UP);
        h.assertTrue(target != null, "Keine Energie-Schnittstelle am Zerkleinerer");
        long inserted = target.insert(100, false);
        h.assertTrue(inserted == 100 && c.energyStorage().getAmount() == 100, "Eingefügt " + inserted + ", Speicher " + c.energyStorage().getAmount());
        h.succeed();
    }

    /** Fluid über die Loader-API in den Tank; an der Ausgangsseite nimmt er nichts an. */
    public static void fluidPlatformApiInsert(GameTestHelper h) {
        BlockPos pos = new BlockPos(1, 1, 1);
        FluidTankBlockEntity t = place(h, pos, ModBlocks.FLUID_TANK.get(), Direction.NORTH, FluidTankBlockEntity.class);
        FluidTarget side = Services.FLUID.findTarget(h.getLevel(), h.absolutePos(pos), Direction.WEST);
        h.assertTrue(side != null, "Keine Fluid-Schnittstelle an der Westseite");
        long in = side.insert(SwFluidStack.of(WATER, 1000), false);
        h.assertTrue(in == 1000 && t.tank().stack().amount() == 1000, "Eingefügt " + in + ", Tank " + t.tank().stack());
        FluidTarget out = Services.FLUID.findTarget(h.getLevel(), h.absolutePos(pos), Direction.NORTH);
        long rejected = out == null ? 0 : out.insert(SwFluidStack.of(WATER, 1000), false);
        h.assertTrue(rejected == 0 && t.tank().stack().amount() == 1000, "Ausgangsseite nahm " + rejected + " mB an");
        h.succeed();
    }

    // ------------------------------------------------------------------ Pumpe

    /** Pumpe saugt eine Wasserquelle ab; die Quelle verschwindet (source_consume_chance fehlt → 1). */
    public static void pumpWaterRemovesSource(GameTestHelper h) {
        for (int x = 2; x <= 4; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 2; z <= 4; z++) {
                    h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        BlockPos water = new BlockPos(3, 2, 3);
        h.setBlock(water, Blocks.WATER);
        FluidPumpBlockEntity p = place(h, new BlockPos(3, 5, 3), ModBlocks.FLUID_PUMP.get(), Direction.EAST, FluidPumpBlockEntity.class);
        fullEnergy(p);
        h.succeedWhen(() -> {
            h.assertTrue(p.tank().stack().is(WATER) && p.tank().stack().amount() == 1000, "Pumpentank " + p.tank().stack());
            h.assertTrue(h.getBlockState(water).isAir(), "Wasserquelle noch da: " + h.getBlockState(water));
        });
    }

    // ------------------------------------------------------------------ Worldgen und Config

    /** Fluidkugel ersetzt Gestein durch Quellen, lässt Grundgestein und Luft unverändert; oil_bubble ist registriert. */
    public static void oilBubbleFeature(GameTestHelper h) {
        for (int x = 1; x <= 6; x++) {
            for (int y = 1; y <= 6; y++) {
                for (int z = 1; z <= 6; z++) {
                    h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        BlockPos center = new BlockPos(3, 3, 3);
        h.setBlock(center.east(), Blocks.BEDROCK);
        h.setBlock(center.west(), Blocks.AIR);
        BlockState oil = ModFluids.get("crude_oil").source().get().defaultFluidState().createLegacyBlock();
        ServerLevel level = h.getLevel();
        new FluidSphereFeature(oil, ConstantInt.of(2)).place(level, level.getChunkSource().getGenerator(), level.getRandom(),
                h.absolutePos(center));
        int sources = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x * x + y * y + z * z <= 4 && level.getFluidState(h.absolutePos(center.offset(x, y, z))).isSource()) {
                        sources++;
                    }
                }
            }
        }
        // Kugel mit r = 2 hat 33 Positionen; Grundgestein und Luft werden nicht ersetzt
        h.assertTrue(sources == 31, "Quellen in der Kugel: " + sources + ", erwartet 31");
        h.assertTrue(h.getBlockState(center.east()).is(Blocks.BEDROCK), "Grundgestein ersetzt");
        Feature registered = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Sternenwerft.id("oil_bubble"));
        h.assertTrue(registered instanceof FluidSphereFeature, "Feature oil_bubble: " + registered);
        h.succeed();
    }

    private static boolean biomeHas(ServerLevel level, ResourceKey<Biome> biome, String feature) {
        Biome b = level.registryAccess().lookupOrThrow(Registries.BIOME).getValue(biome);
        ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Sternenwerft.id(feature));
        for (HolderSet<PlacedFeature> set : b.getGenerationSettings().features()) {
            for (Holder<PlacedFeature> holder : set) {
                if (holder.is(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Biom-Ergänzungen greifen auf beiden Loadern (13.4): Bauxit in der Savanne, Ölblase in der Wüste usw. */
    public static void worldgenBiomeFeatures(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        h.assertTrue(biomeHas(l, Biomes.SAVANNA, "bauxite_ore"), "Savanne ohne bauxite_ore");
        h.assertTrue(biomeHas(l, Biomes.DESERT, "bauxite_ore"), "Wüste ohne bauxite_ore");
        h.assertFalse(biomeHas(l, Biomes.PLAINS, "bauxite_ore"), "Ebene mit bauxite_ore");
        h.assertTrue(biomeHas(l, Biomes.PLAINS, "titanium_ore"), "Ebene ohne titanium_ore");
        h.assertTrue(biomeHas(l, Biomes.BEACH, "quartz_sand"), "Strand ohne quartz_sand");
        h.assertTrue(biomeHas(l, Biomes.DESERT, "oil_bubble"), "Wüste ohne oil_bubble");
        h.assertFalse(biomeHas(l, Biomes.PLAINS, "oil_bubble"), "Ebene mit oil_bubble");
        h.succeed();
    }

    /** Serverconfig ist beim Start geladen und hat die Standardwerte aus misc.json (24.4). */
    public static void configDefaults(GameTestHelper h) {
        ServerConfig c = ServerConfig.get();
        h.assertTrue("normal".equals(c.getString("event_frequency")), "event_frequency " + c.getString("event_frequency"));
        h.assertTrue(c.getDouble("recipe_amount_multiplier") == 1.0, "recipe_amount_multiplier " + c.getDouble("recipe_amount_multiplier"));
        h.assertTrue(c.getBoolean("planet_mobs"), "planet_mobs false");
        h.succeed();
    }
}
