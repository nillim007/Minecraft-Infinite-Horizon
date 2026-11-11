package de.sternenwerft.gametest;

import de.sternenwerft.Sternenwerft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Dünne NeoForge-Hülle um {@link SwGameTests}: Testfunktionen + Testinstanzen. */
public final class NeoForgeGameTests {

    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, Sternenwerft.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRUSHER_CHAIN =
            FUNCTIONS.register(SwGameTests.CRUSHER_CHAIN, () -> SwGameTests::crusherRawIronTwoDust);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BALANCE_RELOAD =
            FUNCTIONS.register(SwGameTests.BALANCE_RELOAD, () -> SwGameTests::crusherBalanceChangesDuration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PLATFORM_CONSUMER =
            FUNCTIONS.register(SwGameTests.PLATFORM_CONSUMER, () -> SwGameTests::cablePlatformConsumerReceivesEnergy);

    private NeoForgeGameTests() {
    }

    public static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(Sternenwerft.id("default"));
        add(event, env, CRUSHER_CHAIN, SwGameTests.CRUSHER_CHAIN_TICKS);
        add(event, env, BALANCE_RELOAD, SwGameTests.BALANCE_RELOAD_TICKS);
        add(event, env, PLATFORM_CONSUMER, SwGameTests.PLATFORM_CONSUMER_TICKS);
    }

    private static void add(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env,
                            DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function, int maxTicks) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, Level.OVERWORLD,
                Identifier.parse(SwGameTests.STRUCTURE), maxTicks, 0, true, Rotation.NONE, false, 1, 1, false, 0);
        event.registerTest(function.key().identifier(), new FunctionGameTestInstance(function.key(), data));
    }
}
