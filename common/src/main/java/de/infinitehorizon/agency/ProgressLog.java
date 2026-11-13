package de.infinitehorizon.agency;

import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Erfolgsprotokoll (24.3): je Erfolg eine Zeile „ISO-8601-Zeit Spieler Erfolg-ID“ in
 * {@code logs/infinite_horizon-progress.log} im Serverordner. Nur lokal, keine Telemetrie.
 */
public final class ProgressLog {

    public static final String FILE = "logs/" + InfiniteHorizon.MOD_ID + "-progress.log";

    private ProgressLog() {
    }

    /** Eine Zeile (reine Funktion, für Tests). */
    public static String line(OffsetDateTime time, String player, Identifier advancement) {
        return time.truncatedTo(ChronoUnit.SECONDS) + " " + player + " " + advancement + System.lineSeparator();
    }

    public static void write(ServerPlayer player, Identifier advancement) {
        Path file = player.level().getServer().getServerDirectory().resolve(FILE);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line(OffsetDateTime.now(), player.getGameProfile().name(), advancement),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            InfiniteHorizon.LOG.warn("[InfiniteHorizon] Erfolgsprotokoll nicht schreibbar: {}", e.getMessage());
        }
    }
}
