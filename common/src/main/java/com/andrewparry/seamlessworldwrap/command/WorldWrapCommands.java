package com.andrewparry.seamlessworldwrap.command;

import com.andrewparry.seamlessworldwrap.SeamlessWorldWrap;
import com.andrewparry.seamlessworldwrap.debug.SeamMapRenderer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** {@code /worldwrap map [blocksPerPixel]}: render the current dimension's generator to PNGs. */
public final class WorldWrapCommands {

    private WorldWrapCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("worldwrap")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("map")
                        .executes(context -> renderMap(context, 4))
                        .then(Commands.argument("blocksPerPixel", IntegerArgumentType.integer(1, 16))
                                .executes(context -> renderMap(context, IntegerArgumentType.getInteger(context, "blocksPerPixel"))))));
    }

    private static int renderMap(CommandContext<CommandSourceStack> context, int blocksPerPixel) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Path outDir = source.getServer().getWorldPath(LevelResource.ROOT).resolve(SeamlessWorldWrap.MOD_ID).resolve("maps");
        source.sendSuccess(() -> Component.literal("Rendering seam map (" + blocksPerPixel + " blocks per pixel)..."), false);

        // Sampling the generator is thread-safe (worldgen does it off-thread), so keep the server responsive.
        CompletableFuture.supplyAsync(() -> {
            try {
                return SeamMapRenderer.render(level, SeamlessWorldWrap.settings(), blocksPerPixel, outDir,
                        percent -> source.getServer().execute(() ->
                                source.sendSuccess(() -> Component.literal("  " + percent + "%"), false)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((result, error) -> source.getServer().execute(() -> {
            if (error != null) {
                SeamlessWorldWrap.LOG.error("Seam map render failed", error);
                source.sendFailure(Component.literal("Seam map failed: " + error.getMessage()));
                return;
            }
            source.sendSuccess(() -> Component.literal("Saved " + result.widthPx() + "x" + result.heightPx()
                    + " seam map in " + result.millis() / 1000.0 + "s:"), false);
            source.sendSuccess(() -> pathLine(result.terrain()), false);
            source.sendSuccess(() -> pathLine(result.biomes()), false);
        }));
        return 1;
    }

    private static Component pathLine(Path path) {
        String text = path.toAbsolutePath().normalize().toString();
        return Component.literal("  " + text).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy path"))));
    }
}
