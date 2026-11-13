package de.infinitehorizon.agency;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.environment.PlanetResolver;
import de.infinitehorizon.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Planetenatlas (24.2) und Entdecker (23.3): Geschenk beim Login, besuchte Biome bei der Umweltprüfung, Entdecker beim
 * ersten Betreten einer Dimension (ab M4) und die Atlas-Daten für {@code AgencyStatePayload}.
 */
public final class AtlasService {

    /** Entity-Tag: Atlas schon geschenkt (wie das Handbuch, spec-gaps #37). */
    public static final String ATLAS_GIVEN_TAG = InfiniteHorizon.MOD_ID + ".atlas_given";

    private AtlasService() {
    }

    /** Geschenk beim Login, wenn das Tag fehlt (auch für Spieler aus älteren Welten). */
    public static void giveOnce(ServerPlayer player) {
        if (player.entityTags().contains(ATLAS_GIVEN_TAG)) {
            return;
        }
        player.addTag(ATLAS_GIVEN_TAG);
        ItemStack atlas = new ItemStack(ModItems.PLANETARY_ATLAS.get());
        if (!player.getInventory().add(atlas)) {
            player.spawnAtLocation(player.level(), atlas);
        }
    }

    /** Umweltprüfung (8.2): Biom an der Position des Spielers in {@code visited_biomes} seiner Agentur. */
    public static void recordBiome(ServerPlayer player) {
        ServerLevel level = player.level();
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        biome.unwrapKey().ifPresent(key -> AgencyData.get(level.getServer()).agencyOf(player.getUUID()).ifPresent(a -> {
            if (a.progress().addVisitedBiome(level.dimension().identifier(), key.identifier())) {
                AgencyData.get(level.getServer()).setDirty();
            }
        }));
    }

    /**
     * Entdecker (23.3): Die erste Agentur, deren Mitglied eine Dimension ab M4 betritt, wird eingetragen und serverweit
     * angekündigt. SPEC-GAP #85: die Erde (Start aller Spieler) hat keinen Entdecker.
     */
    public static void onDimensionChange(ServerPlayer player, ResourceKey<Level> dimension) {
        Identifier dim = dimension.identifier();
        if (dim.equals(PlanetResolver.EARTH)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        AgencyData data = AgencyData.get(server);
        Agency agency = data.agencyOf(player.getUUID()).orElse(null);
        if (agency == null || data.discoverer(dim) != null) {
            return;
        }
        data.setDiscoverer(dim, agency.id());
        Component msg = Component.translatable("message." + InfiniteHorizon.MOD_ID + ".discovered",
                AgencyManager.displayName(server, agency),
                Component.translatable("planet." + dim.getNamespace() + "." + dim.getPath()));
        server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    /** Atlas-Daten der Agentur als JSON für den Client (24.2). */
    public static String json(MinecraftServer server, Agency agency) {
        AgencyData data = AgencyData.get(server);
        AgencyProgress p = agency.progress();
        JsonObject o = new JsonObject();
        o.add("visited", ids(agency.visitedDimensions()));
        o.add("probe", ids(p.probeReached()));
        JsonObject discoverers = new JsonObject();
        for (Map.Entry<Identifier, UUID> e : data.discoverers().entrySet()) {
            data.byId(e.getValue()).ifPresent(a -> discoverers.addProperty(e.getKey().toString(),
                    AgencyManager.displayName(server, a).getString()));
        }
        o.add("discoverers", discoverers);
        JsonObject biomes = new JsonObject();
        p.visitedBiomes().forEach((dim, set) -> biomes.add(dim.toString(), ids(set)));
        o.add("biomes", biomes);
        JsonArray structures = new JsonArray();
        for (AgencyProgress.FoundStructure f : p.foundStructures()) {
            JsonObject s = new JsonObject();
            s.addProperty("dimension", f.dimension().toString());
            s.addProperty("id", f.structure().toString());
            s.addProperty("x", new net.minecraft.world.level.ChunkPos(f.chunkX(), f.chunkZ()).getMiddleBlockX());
            s.addProperty("z", new net.minecraft.world.level.ChunkPos(f.chunkX(), f.chunkZ()).getMiddleBlockZ());
            structures.add(s);
        }
        o.add("structures", structures);
        JsonArray beacons = new JsonArray();
        for (AgencyProgress.Beacon b : p.beacons()) {
            JsonObject s = new JsonObject();
            s.addProperty("dimension", b.dimension().toString());
            s.addProperty("name", b.name());
            s.addProperty("x", b.pos().getX());
            s.addProperty("y", b.pos().getY());
            s.addProperty("z", b.pos().getZ());
            beacons.add(s);
        }
        o.add("beacons", beacons);
        return o.toString();
    }

    private static JsonArray ids(Set<Identifier> set) {
        JsonArray a = new JsonArray();
        set.forEach(id -> a.add(id.toString()));
        return a;
    }
}
