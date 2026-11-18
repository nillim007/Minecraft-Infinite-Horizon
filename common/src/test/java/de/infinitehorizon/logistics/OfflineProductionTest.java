package de.infinitehorizon.logistics;

import com.google.gson.JsonObject;
import de.infinitehorizon.data.BalanceManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Offline-Produktion (20.1, Abnahme 27 M8): vergangene_Ticks × Rate × Faktor, begrenzt durch Eingaben und Platz. */
class OfflineProductionTest {

    private static double specFactor() throws Exception {
        BalanceManager b = BalanceManager.loadDirectory(Path.of(System.getProperty("infinite_horizon.specData"), "balance"));
        JsonObject o = b.entry("logistics", "offline_production").orElseThrow();
        return o.get("factor").getAsDouble();
    }

    @Test
    void acceptanceOneItemPerHundredTicks() throws Exception {
        assertEquals(120, OfflineProduction.compute(true, 1, 100, specFactor(), 24000));
    }

    @Test
    void notRunningProducesNothing() {
        assertEquals(0, OfflineProduction.compute(false, 1, 100, 0.5, 24000));
        assertEquals(0, OfflineProduction.compute(true, 1, 0, 0.5, 24000));
        assertEquals(0, OfflineProduction.compute(true, 1, 100, 0.0, 24000));
    }

    @Test
    void limitedByInputsAndSpace() {
        OfflineProduction.Snapshot inputs = new OfflineProduction.Snapshot(true, 1, 100, 3, OfflineProduction.UNLIMITED);
        assertEquals(3, OfflineProduction.compute(inputs, 0.5, 24000));
        // Platz für 250 mB bei 100 mB je Vorgang: 2 ganze Vorgänge
        OfflineProduction.Snapshot space = new OfflineProduction.Snapshot(true, 100, 20, OfflineProduction.UNLIMITED, 250);
        assertEquals(2, OfflineProduction.cycles(space, 0.5, 24000));
        assertEquals(200, OfflineProduction.compute(space, 0.5, 24000));
    }

    @Test
    void onlyWholeCyclesCount() {
        assertEquals(0, OfflineProduction.compute(true, 1, 100, 0.5, 199));
        assertEquals(1, OfflineProduction.compute(true, 1, 100, 0.5, 200));
        assertEquals(10, OfflineProduction.compute(true, 1, 100, 0.1, 10000));
    }
}
