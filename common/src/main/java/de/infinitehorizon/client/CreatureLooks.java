package de.infinitehorizon.client;

import de.infinitehorizon.creature.CloudRay;
import de.infinitehorizon.creature.DuneBurrower;
import de.infinitehorizon.creature.DustWraith;
import de.infinitehorizon.creature.RubbleCrawler;
import de.infinitehorizon.creature.ShadeStrider;
import de.infinitehorizon.creature.SulfurSnail;
import de.infinitehorizon.registry.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Platzhalter-Aussehen der Kreaturen (18, 24.5): ein Block je Art; der getarnte Krabbler sieht aus wie Mondgestein. */
public final class CreatureLooks {

    private CreatureLooks() {
    }

    public static BlockState crawler(RubbleCrawler e) {
        return e.disguised() ? ModBlocks.simple("moon_rock").get().defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState();
    }

    public static BlockState strider(ShadeStrider e) {
        return Blocks.OBSIDIAN.defaultBlockState();
    }

    public static BlockState burrower(DuneBurrower e) {
        return ModBlocks.simple("mars_sand").get().defaultBlockState();
    }

    public static BlockState wraith(DustWraith e) {
        return Blocks.GLASS.defaultBlockState();
    }

    public static BlockState snail(SulfurSnail e) {
        return ModBlocks.simple("venus_sulfur_ore").get().defaultBlockState();
    }

    public static BlockState ray(CloudRay e) {
        return Blocks.PRISMARINE.defaultBlockState();
    }

    public static BlockState beetle(de.infinitehorizon.creature.MirrorBeetle e) {
        return Blocks.QUARTZ_BLOCK.defaultBlockState();
    }

    public static BlockState mite(de.infinitehorizon.creature.DriftMite e) {
        return Blocks.AMETHYST_BLOCK.defaultBlockState();
    }

    public static BlockState salamander(de.infinitehorizon.creature.SulfurSalamander e) {
        return Blocks.MAGMA_BLOCK.defaultBlockState();
    }

    public static BlockState jelly(de.infinitehorizon.creature.IceJelly e) {
        return Blocks.SEA_LANTERN.defaultBlockState();
    }

    public static BlockState keeper(de.infinitehorizon.creature.ReefKeeper e) {
        return Blocks.DARK_PRISMARINE.defaultBlockState();
    }

    public static BlockState skimmer(de.infinitehorizon.creature.MethaneSkimmer e) {
        return Blocks.TERRACOTTA.defaultBlockState();
    }

    public static BlockState moth(de.infinitehorizon.creature.GeyserMoth e) {
        return Blocks.PACKED_ICE.defaultBlockState();
    }

    public static BlockState lurker(de.infinitehorizon.creature.FrostLurker e) {
        return Blocks.BLUE_ICE.defaultBlockState();
    }
}
