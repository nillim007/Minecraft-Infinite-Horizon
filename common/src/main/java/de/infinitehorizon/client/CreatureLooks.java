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
}
