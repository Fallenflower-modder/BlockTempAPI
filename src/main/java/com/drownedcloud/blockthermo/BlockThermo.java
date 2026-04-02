package com.drownedcloud.blockthermo;

import com.drownedcloud.blockthermo.command.TemperatureCommand;
import com.drownedcloud.blockthermo.config.ConfigLoader;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(BlockThermo.MOD_ID)
public class BlockThermo {
    public static final String MOD_ID = "block_thermo";

    public BlockThermo() {
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TemperatureCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        ConfigLoader.init();
    }
}
