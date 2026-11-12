package de.infinitehorizon.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Testspieler in der Spielerliste. {@code GameTestHelper#makeMockServerPlayerInLevel} meldet in 26.3 immer den
 * Kreativmodus (feste Überschreibung von {@code gameMode()}); Umwelt-Tests brauchen aber einen Spieler im
 * Überlebensmodus, den der {@code HazardTracker} wie jeden anderen über die Spielerliste sieht. Aufbau wie in Vanilla,
 * nur ohne die Überschreibung. SPEC-GAP #43.
 */
public final class IhTestPlayers {

    private IhTestPlayers() {
    }

    /** Neuer Spieler in der Spielerliste mit Spielmodus {@code mode}, an {@code relative} im Testbereich. */
    public static ServerPlayer inList(GameTestHelper h, GameType mode, Vec3 relative) {
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "test-player");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(mode);
        Vec3 pos = h.absoluteVec(relative);
        if (player.level() != level) {
            // placeNewPlayer setzt neue Spieler in die Overworld; Tests in anderen Dimensionen holen ihn herüber
            return (ServerPlayer) player.teleport(new net.minecraft.world.level.portal.TeleportTransition(level, pos,
                    Vec3.ZERO, 0, 0, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        }
        player.teleportTo(pos.x, pos.y, pos.z);
        return player;
    }

    /** Spieler wieder aus der Spielerliste nehmen (am Testende). */
    public static void remove(ServerPlayer player) {
        player.level().getServer().getPlayerList().remove(player);
    }
}
