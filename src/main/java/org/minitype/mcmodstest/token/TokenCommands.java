package org.minitype.mcmodstest.token;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public final class TokenCommands {

    private TokenCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("unbound")
                            .then(literal("mode")
                                    .executes(context -> showMode(context.getSource()))
                                    .then(modeCommand("smp", TokenMode.SMP))
                                    .then(modeCommand("singleplayer", TokenMode.SINGLEPLAYER))
                                    .then(modeCommand("hybrid", TokenMode.HYBRID)))
                            .then(literal("tokens")
                                    .executes(context -> showCharges(context.getSource())))
            );

            dispatcher.register(modeAlias("smp", TokenMode.SMP));
            dispatcher.register(modeAlias("singleplayer", TokenMode.SINGLEPLAYER));
            dispatcher.register(modeAlias("hybrid", TokenMode.HYBRID));
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> modeCommand(
            String name,
            TokenMode mode
    ) {
        return literal(name)
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(context -> setMode(context.getSource(), mode));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> modeAlias(
            String name,
            TokenMode mode
    ) {
        return literal(name)
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(context -> setMode(context.getSource(), mode));
    }

    private static int setMode(ServerCommandSource source, TokenMode mode) {
        TokenEconomyState state = TokenEconomy.getState(source.getServer());
        state.setMode(mode);

        source.sendFeedback(
                () -> Text.literal("§6Unbound token mode set to §e" + mode.id().toUpperCase()),
                true
        );
        return 1;
    }

    private static int showMode(ServerCommandSource source) {
        TokenMode mode = TokenEconomy.getState(source.getServer()).getMode();
        source.sendMessage(Text.literal("§6Unbound token mode: §e" + mode.id().toUpperCase()));
        return 1;
    }

    private static int showCharges(ServerCommandSource source) {
        if (source.getPlayer() == null) {
            source.sendError(Text.literal("This command must be run by a player."));
            return 0;
        }

        int charges = TokenEconomy.getState(source.getServer()).getCharges(source.getPlayer().getUuid());
        source.sendMessage(Text.literal("§6Token Charges: §e" + charges));
        return charges;
    }
}
