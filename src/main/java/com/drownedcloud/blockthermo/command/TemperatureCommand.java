package com.drownedcloud.blockthermo.command;

import com.drownedcloud.blockthermo.temperature.TemperatureCalculator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class TemperatureCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("blockthermo")
                .then(Commands.literal("temperature")
                    .then(Commands.literal("query")
                        .executes(TemperatureCommand::queryTemperature)
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                            .then(Commands.argument("y", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                    .executes(TemperatureCommand::queryTemperatureAtPosition))))))
        );
    }

    private static int queryTemperature(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Entity entity = source.getEntity();
        
        if (entity == null) {
            source.sendFailure(Component.translatable("blockthermo.query.no_world"));
            return 0;
        }

        Level level = entity.level();
        BlockPos pos = new BlockPos((int) Math.floor(entity.getX()), 
                                        (int) Math.floor(entity.getY()), 
                                        (int) Math.floor(entity.getZ()));

        return queryTemperatureAtPosition(context, level, pos);
    }

    private static int queryTemperatureAtPosition(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Level level = source.getLevel();
        
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        
        BlockPos pos = new BlockPos(x, y, z);

        return queryTemperatureAtPosition(context, level, pos);
    }

    private static int queryTemperatureAtPosition(CommandContext<CommandSourceStack> context, Level level, BlockPos pos) {
        CommandSourceStack source = context.getSource();
        
        if (!level.hasChunkAt(pos)) {
            source.sendFailure(Component.translatable("blockthermo.query.chunk_not_loaded", 
                Component.translatable("blockthermo.query.position", 
                    (double) pos.getX(), 
                    (double) pos.getY(), 
                    (double) pos.getZ())));
            return 0;
        }

        long tick = level.getGameTime();
        float temperature = TemperatureCalculator.getTemperature(level, pos, tick);
        
        source.sendSuccess(() -> Component.translatable("blockthermo.query.temperature", 
            Component.translatable("blockthermo.query.position", 
                (double) pos.getX(), 
                (double) pos.getY(), 
                (double) pos.getZ()), 
            temperature), true);

        return 1;
    }
}
