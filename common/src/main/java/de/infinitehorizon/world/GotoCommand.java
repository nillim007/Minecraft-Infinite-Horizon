package de.infinitehorizon.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.Planet;
import de.infinitehorizon.data.PlanetRegistry;
import de.infinitehorizon.spaceflight.LandingService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code /infinite_horizon goto <planet> [<x> <z>]} (12.10, Rechtestufe 2): bringt den Spieler ohne Rakete in eine
 * Planetendimension, Landeplatz wie in 12.5 (Standard X/Z 0/0). {@code goto overworld} führt zum Weltspawn.
 * {@code <planet>} darf ohne Namespace stehen ({@code moon}).
 */
public final class GotoCommand {

    private static final String KEY = "command." + InfiniteHorizon.MOD_ID + ".goto.";

    private GotoCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(InfiniteHorizon.MOD_ID)
                .then(Commands.literal("goto")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("planet", IdentifierArgument.id())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(
                                        PlanetRegistry.get().all().stream().map(Planet::dimension), b))
                                .executes(ctx -> run(ctx, null, null))
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "x"),
                                                        IntegerArgumentType.getInteger(ctx, "z"))))))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, @Nullable Integer x, @Nullable Integer z)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer server = ctx.getSource().getServer();
        Identifier arg = IdentifierArgument.getId(ctx, "planet");
        ServerLevel target = resolve(server, arg);
        if (target == null) {
            ctx.getSource().sendFailure(Component.translatable(KEY + "unknown", arg.toString()));
            return 0;
        }
        BlockPos feet;
        if (target.dimension() == Level.OVERWORLD && x == null) {
            feet = teleport(player, target, server.getRespawnData().pos().getX(), server.getRespawnData().pos().getZ());
        } else {
            feet = teleport(player, target, x == null ? 0 : x, z == null ? 0 : z);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "ok", target.dimension().identifier().toString(),
                feet.getX(), feet.getY(), feet.getZ()), true);
        return 1;
    }

    /** Landeplatz suchen (12.5) und den Spieler dorthin bringen; liefert die Fußposition. */
    public static BlockPos teleport(ServerPlayer player, ServerLevel target, int x, int z) {
        BlockPos feet = LandingService.find(target, x, z);
        player.teleport(new TeleportTransition(target, Vec3.atBottomCenterOf(feet), Vec3.ZERO, player.getYRot(),
                player.getXRot(), TeleportTransition.DO_NOTHING));
        return feet;
    }

    /** Level zu einer Planeten-ID; ohne Namespace wird {@code infinite_horizon:} ergänzt, falls es so ein Level gibt. */
    public static @Nullable ServerLevel resolve(MinecraftServer server, Identifier id) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level == null && Identifier.DEFAULT_NAMESPACE.equals(id.getNamespace())) {
            level = server.getLevel(ResourceKey.create(Registries.DIMENSION, InfiniteHorizon.id(id.getPath())));
        }
        return level;
    }
}
