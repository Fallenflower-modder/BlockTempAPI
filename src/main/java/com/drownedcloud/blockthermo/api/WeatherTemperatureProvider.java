package com.drownedcloud.blockthermo.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface WeatherTemperatureProvider {
    float getWeatherTemperature(Level world, BlockPos pos, boolean isRaining, boolean isThundering, boolean isOpenSky);
}
