package com.drownedcloud.blockthermo.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface DailyTemperatureProvider {
    float getDailyTemperature(Level world, BlockPos pos, long tick);
}
