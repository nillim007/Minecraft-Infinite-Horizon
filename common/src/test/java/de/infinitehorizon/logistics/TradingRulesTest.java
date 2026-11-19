package de.infinitehorizon.logistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Reine Regeln des Handelspostens (23.3). */
class TradingRulesTest {

    @Test
    void availableCountsWholeTrades() {
        assertEquals(3, TradingRules.available(7, 2));
        assertEquals(0, TradingRules.available(1, 2));
        assertEquals(0, TradingRules.available(5, 0));
    }

    @Test
    void checkOrder() {
        assertEquals(TradingRules.Trade.NO_OFFER, TradingRules.check(0, 1, 10, 10, true));
        assertEquals(TradingRules.Trade.NO_ITEMS, TradingRules.check(4, 1, 3, 10, true));
        assertEquals(TradingRules.Trade.OUT_OF_STOCK, TradingRules.check(4, 2, 4, 1, true));
        assertEquals(TradingRules.Trade.NO_SPACE, TradingRules.check(4, 2, 4, 2, false));
        assertEquals(TradingRules.Trade.OK, TradingRules.check(4, 2, 4, 2, true));
    }

    @Test
    void patternClicks() {
        // links mit Item: ganzer Stapel; rechts: +1 (gleiches Item) bzw. 1 (anderes)
        assertEquals(16, TradingRules.patternCount(3, false, 16, false, 64));
        assertEquals(4, TradingRules.patternCount(3, true, 16, true, 64));
        assertEquals(1, TradingRules.patternCount(3, true, 16, false, 64));
        assertEquals(64, TradingRules.patternCount(64, true, 1, true, 64));
        // ohne Item: links leeren, rechts -1
        assertEquals(0, TradingRules.patternCount(5, false, 0, false, 64));
        assertEquals(4, TradingRules.patternCount(5, true, 0, false, 64));
        assertEquals(0, TradingRules.patternCount(1, true, 0, false, 64));
    }
}
