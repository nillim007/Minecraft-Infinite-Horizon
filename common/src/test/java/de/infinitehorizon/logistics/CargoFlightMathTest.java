package de.infinitehorizon.logistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Frachtflüge (20.3): Treibstoff je Strecke und Flugzeit mit Antrieben. */
class CargoFlightMathTest {

    @Test
    void fuelPerLeg() {
        assertEquals(2000, CargoFlightMath.fuelPerLeg(2000, 1500, 0, 0));
        assertEquals(29000, CargoFlightMath.fuelPerLeg(2000, 1500, 0, 18));
        assertEquals(29000, CargoFlightMath.fuelPerLeg(2000, 1500, 18, 0));
        assertEquals(4250, CargoFlightMath.fuelPerLeg(2000, 1500, 0, 1.5));
    }

    @Test
    void flightTicksWithDrives() {
        assertEquals(1000, CargoFlightMath.flightTicks(1000, 3950, 0, 0, 1));
        assertEquals(72100, CargoFlightMath.flightTicks(1000, 3950, 0, 18, 1));
        assertEquals(36050, CargoFlightMath.flightTicks(1000, 3950, 0, 18, 2));
        assertEquals(7210, CargoFlightMath.flightTicks(1000, 3950, 0, 18, 10));
        assertEquals(1000, CargoFlightMath.flightTicks(1000, 3950, 0, 0, 0.5), "Divisor unter 1 zählt nicht");
    }
}
