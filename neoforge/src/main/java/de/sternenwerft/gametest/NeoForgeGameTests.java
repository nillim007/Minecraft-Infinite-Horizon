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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Dünne NeoForge-Hülle: registriert alle Tests aus {@link SwTestCatalog} (Testfunktion + Testinstanz). */
public final class NeoForgeGameTests {

    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, Sternenwerft.MOD_ID);

    private static final Map<SwTestCatalog.Test, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> HOLDERS =
            new LinkedHashMap<>();

    static {
        for (SwTestCatalog.Test t : SwTestCatalog.ALL) {
            HOLDERS.put(t, FUNCTIONS.register(t.name(), t::run));
        }
    }

    private NeoForgeGameTests() {
    }

    public static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(Sternenwerft.id("default"));
        HOLDERS.forEach((test, function) -> {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, Level.OVERWORLD,
                    Identifier.parse(SwGameTests.STRUCTURE), test.maxTicks(), 0, true, Rotation.NONE, false, 1, 1, false, 0);
            event.registerTest(function.key().identifier(), new FunctionGameTestInstance(function.key(), data));
        });
    }
}
