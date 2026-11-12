package de.infinitehorizon.agency;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.infinitehorizon.InfiniteHorizon;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Befehle der Agentur (23.1): {@code /infinite_horizon agency info [<spieler>]} für alle,
 * {@code /infinite_horizon agency transfer <von> <an>} ab Rechtestufe 2. {@code <von>} und {@code <an>} sind eine
 * Agentur-ID oder ein Spielername (auch offline, aus dem Namens-Cache).
 */
public final class AgencyCommands {

    private static final String KEY = "command." + InfiniteHorizon.MOD_ID + ".agency.";

    private AgencyCommands() {
    }

    private static final SuggestionProvider<CommandSourceStack> AGENCIES = (ctx, builder) -> {
        MinecraftServer server = ctx.getSource().getServer();
        Stream<String> ids = AgencyManager.data(server).all().stream()
                .filter(a -> a.mergedInto() == null).map(a -> a.id().toString());
        return SharedSuggestionProvider.suggest(Stream.concat(Stream.of(server.getPlayerNames()), ids), builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(InfiniteHorizon.MOD_ID)
                .then(Commands.literal("agency")
                        .then(Commands.literal("info")
                                .executes(ctx -> info(ctx, ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> info(ctx, EntityArgument.getPlayer(ctx, "player")))))
                        .then(Commands.literal("transfer")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("from", StringArgumentType.word()).suggests(AGENCIES)
                                        .then(Commands.argument("to", StringArgumentType.word()).suggests(AGENCIES)
                                                .executes(AgencyCommands::transfer))))));
    }

    private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        MinecraftServer server = ctx.getSource().getServer();
        Agency agency = AgencyManager.ensureFor(player);
        List<Component> members = agency.members().stream()
                .map(m -> (Component) Component.translatable(KEY + "info.member", AgencyManager.playerName(server, m.player()),
                        Component.translatable("agency." + InfiniteHorizon.MOD_ID + ".role." + m.role().getSerializedName())))
                .toList();
        List<Component> dims = agency.visitedDimensions().stream()
                .map(Identifier::toString).sorted().map(s -> (Component) Component.literal(s)).toList();
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "info.header",
                AgencyManager.displayName(server, agency), agency.id().toString()), false);
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "info.members",
                ComponentUtils.formatList(members, Component.literal(", "))), false);
        ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "info.dimensions",
                ComponentUtils.formatList(dims, Component.literal(", "))), false);
        return members.size();
    }

    private static int transfer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        MinecraftServer server = ctx.getSource().getServer();
        AgencyData data = AgencyManager.data(server);
        String fromArg = StringArgumentType.getString(ctx, "from");
        String toArg = StringArgumentType.getString(ctx, "to");
        Optional<Agency> from = find(server, data, fromArg);
        Optional<Agency> to = find(server, data, toArg);
        if (from.isEmpty() || to.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable(KEY + "unknown", from.isEmpty() ? fromArg : toArg));
            return 0;
        }
        List<UUID> formerMembers = from.get().members().stream().map(Agency.Member::player).toList();
        AgencyData.TransferResult result = data.transfer(from.get().id(), to.get().id());
        switch (result) {
            case OK -> {
                // wer gerade online ist, bekommt sofort eine neue Agentur (sonst beim nächsten Login)
                formerMembers.stream().map(server.getPlayerList()::getPlayer).filter(p -> p != null)
                        .forEach(AgencyManager::ensureFor);
                Component a = AgencyManager.displayName(server, from.get());
                Component b = AgencyManager.displayName(server, to.get());
                ctx.getSource().sendSuccess(() -> Component.translatable(KEY + "transfer.ok", a, b), true);
                return 1;
            }
            case SAME -> ctx.getSource().sendFailure(Component.translatable(KEY + "transfer.same"));
            case CYCLE -> ctx.getSource().sendFailure(Component.translatable(KEY + "transfer.cycle"));
            default -> ctx.getSource().sendFailure(Component.translatable(KEY + "unknown", fromArg));
        }
        return 0;
    }

    /** Agentur-ID, Spieler-UUID oder Spielername (online oder aus dem Namens-Cache). */
    static Optional<Agency> find(MinecraftServer server, AgencyData data, String arg) {
        try {
            UUID id = UUID.fromString(arg);
            return data.byId(id).or(() -> data.agencyOf(id));
        } catch (IllegalArgumentException notUuid) {
            ServerPlayer online = server.getPlayerList().getPlayer(arg);
            if (online != null) {
                return Optional.of(AgencyManager.ensureFor(online));
            }
            return server.services().nameToIdCache().get(arg).flatMap(n -> data.agencyOf(n.id()));
        }
    }
}
