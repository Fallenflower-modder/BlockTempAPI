package com.drownedcloud.blockthermo.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public interface BiomeTemperatureProvider {
    float getBiomeTemperature(Level world, BlockPos pos, Biome biome);
}
