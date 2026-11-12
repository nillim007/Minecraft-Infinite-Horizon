package de.infinitehorizon.environment;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.dome.DomeManager;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.world.ExtinguishedTorchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Welt ohne Atmosphäre (8.5, misc.json → {@code atmosphere_rules}): In Planetendimensionen (alle mit Planeten-Eintrag
 * außer der Erde) gilt außerhalb aktiver Kuppeln: Blöcke im Tag {@code infinite_horizon:needs_atmosphere} lassen sich
 * nicht setzen, bestehende erlöschen beim Random Tick, Pflanzen wachsen nicht, Wasser gefriert oder verdampft, Eis
 * schmilzt nicht. Die Mixins rufen nur diese Klasse.
 */
public final class AtmosphereRules {

    public static final TagKey<Block> NEEDS_ATMOSPHERE = TagKey.create(Registries.BLOCK, InfiniteHorizon.id("needs_atmosphere"));

    private static volatile @Nullable BalanceManager source;
    private static volatile List<TagKey<Block>> plantTags = List.of();

    private AtmosphereRules() {
    }

    private static JsonObject rules() {
        return BalanceManager.getOrBundled().entry("misc", "atmosphere_rules").orElse(new JsonObject());
    }

    private static List<TagKey<Block>> plantTags() {
        BalanceManager b = BalanceManager.getOrBundled();
        if (b != source) {
            List<TagKey<Block>> out = new ArrayList<>();
            JsonObject r = rules();
            if (r.has("plant_tags")) {
                r.getAsJsonArray("plant_tags").forEach(e ->
                        out.add(TagKey.create(Registries.BLOCK, Identifier.parse(e.getAsString().replace("#", "")))));
            }
            plantTags = List.copyOf(out);
            source = b;
        }
        return plantTags;
    }

    /**
     * Schnelle Vorprüfung für heiße Pfade (Random Ticks): Kann es in diesem Level überhaupt Orte ohne Atmosphäre geben?
     * Nein, wenn die Dimension keinen eigenen Planeten-Eintrag hat und kein GameTest-Override aktiv ist.
     */
    public static boolean mayBeAirless(ServerLevel level) {
        if (EnvironmentOverrides.anyActive()) {
            return true;
        }
        var id = level.dimension().identifier();
        return !id.equals(PlanetResolver.EARTH) && de.infinitehorizon.data.PlanetRegistry.get().planet(id).isPresent();
    }

    /** Planet an der Position, falls es eine Planetendimension ist (nicht die Erde), sonst null. */
    private static @Nullable Planet airlessPlanet(ServerLevel level, BlockPos pos) {
        Planet planet = PlanetResolver.at(level, pos).planet();
        return planet == null || planet.dimension().equals(PlanetResolver.EARTH) ? null : planet;
    }

    /** Ohne Atmosphäre: Planetendimension und nicht in einer aktiven Kuppel ({@code failing} zählt als aktiv). */
    public static boolean airless(ServerLevel level, BlockPos pos) {
        return airlessPlanet(level, pos) != null && !DomeManager.isInsideActiveDome(level, pos);
    }

    /** Setzen verweigern (Meldung {@code denied_message})? */
    public static boolean denyPlacement(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        if (!(level instanceof ServerLevel sl) || !state.is(NEEDS_ATMOSPHERE) || !mayBeAirless(sl) || !airless(sl, pos)) {
            return false;
        }
        if (player != null) {
            JsonObject r = rules();
            player.sendOverlayMessage(Component.translatable(r.has("denied_message") ? r.get("denied_message").getAsString()
                    : "message." + InfiniteHorizon.MOD_ID + ".no_atmosphere"));
        }
        return true;
    }

    public static boolean isPlant(BlockState state) {
        for (TagKey<Block> t : plantTags()) {
            if (state.is(t)) {
                return true;
            }
        }
        return false;
    }

    /** Random Tick: true = erledigt (Vanilla-Verhalten abbrechen). */
    public static boolean randomTick(BlockState state, ServerLevel level, BlockPos pos) {
        if (!mayBeAirless(level)) {
            return false;
        }
        if (state.is(NEEDS_ATMOSPHERE)) {
            if (airless(level, pos)) {
                extinguish(state, level, pos);
                return true;
            }
            return false;
        }
        return isPlant(state) && airless(level, pos);
    }

    /**
     * Erlöschen (8.5): Fackeln werden zu {@code extinguished_torch} (auch an der Wand), Lagerfeuer und Kerzen gehen aus,
     * Laternen und Feuer verschwinden.
     */
    public static void extinguish(BlockState state, ServerLevel level, BlockPos pos) {
        Block ext = ModBlocks.EXTINGUISHED_TORCH.get();
        if (state.getBlock() instanceof WallTorchBlock) {
            Direction facing = state.getValue(WallTorchBlock.FACING);
            level.setBlock(pos, ext.defaultBlockState().setValue(ExtinguishedTorchBlock.FACING, facing), Block.UPDATE_ALL);
        } else if (state.getBlock() instanceof TorchBlock) {
            level.setBlock(pos, ext.defaultBlockState().setValue(ExtinguishedTorchBlock.FACING, Direction.UP), Block.UPDATE_ALL);
        } else if (state.hasProperty(BlockStateProperties.LIT)) {
            if (state.getValue(BlockStateProperties.LIT)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
            }
        } else {
            level.removeBlock(pos, false);
        }
    }

    /** Knochenmehl an Pflanzen abbrechen? */
    public static boolean blocksBoneMeal(Level level, BlockPos pos) {
        return level instanceof ServerLevel sl && mayBeAirless(sl) && isPlant(level.getBlockState(pos)) && airless(sl, pos);
    }

    /** Wasser wurde gesetzt (8.5): {@code freeze} → Eis, {@code evaporate} → weg. */
    public static void waterPlaced(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl) || !mayBeAirless(sl)) {
            return;
        }
        String behavior = waterBehavior(sl, pos);
        if (behavior == null) {
            return;
        }
        switch (behavior) {
            case "freeze" -> level.setBlock(pos, Blocks.ICE.defaultBlockState(), Block.UPDATE_ALL);
            case "evaporate" -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            default -> {
            }
        }
    }

    /** Eis schmilzt in {@code freeze}-Gebieten ohne Atmosphäre nicht. */
    public static boolean keepIce(Level level, BlockPos pos) {
        return level instanceof ServerLevel sl && mayBeAirless(sl) && "freeze".equals(waterBehavior(sl, pos));
    }

    private static @Nullable String waterBehavior(ServerLevel level, BlockPos pos) {
        if (!airless(level, pos)) {
            return null;
        }
        PlanetResolver.Resolved res = PlanetResolver.at(level, pos);
        String behavior = res.planet().waterBehavior();
        for (Planet.Zone z : res.zones()) {
            if (z.waterBehavior() != null) {
                behavior = z.waterBehavior();
            }
        }
        return behavior;
    }
}
