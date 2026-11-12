package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
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

/** Dünne NeoForge-Hülle: registriert alle Tests aus {@link IhTestCatalog} (Testfunktion + Testinstanz). */
public final class NeoForgeGameTests {

    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, InfiniteHorizon.MOD_ID);

    private static final Map<IhTestCatalog.Test, DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>>> HOLDERS =
            new LinkedHashMap<>();

    static {
        for (IhTestCatalog.Test t : IhTestCatalog.ALL) {
            HOLDERS.put(t, FUNCTIONS.register(t.name(), t::run));
        }
    }

    private NeoForgeGameTests() {
    }

    public static void onRegisterTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> standard = event.registerEnvironment(InfiniteHorizon.id("default"));
        Map<String, Holder<TestEnvironmentDefinition<?>>> envs = new LinkedHashMap<>();
        HOLDERS.forEach((test, function) -> {
            // eigene Umgebung = eigener Durchgang (Leistungstest misst sonst die anderen Tests mit)
            Holder<TestEnvironmentDefinition<?>> env = test.environment() == null ? standard
                    : envs.computeIfAbsent(test.environment(), id -> event.registerEnvironment(Identifier.parse(id)));
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, Level.OVERWORLD,
                    Identifier.parse(test.structure() != null ? test.structure() : IhGameTests.STRUCTURE), test.maxTicks(), 0, true,
                    Rotation.NONE, false, 1, 1, false, 0);
            event.registerTest(function.key().identifier(), new FunctionGameTestInstance(function.key(), data));
        });
    }
}
