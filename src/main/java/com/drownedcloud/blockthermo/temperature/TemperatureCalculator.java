package com.drownedcloud.blockthermo.temperature;

import com.drownedcloud.blockthermo.api.ExtensionManager;
import com.drownedcloud.blockthermo.config.ConfigLoader;
import com.drownedcloud.blockthermo.config.TemperatureMainConfig;
import com.drownedcloud.blockthermo.config.TemperatureRadiationConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TemperatureCalculator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    private static final Map<String, DailyTemp> DAILY_TEMPS = new HashMap<>();

    public static float getTemperature(Level world, double x, double y, double z, long tick) {
        BlockPos pos = new BlockPos((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
        return getTemperature(world, pos, tick);
    }

    public static float getTemperature(Level world, BlockPos pos, long tick) {
        if (world == null || pos == null) {
            LOGGER.warn("World or position is null, returning default temperature");
            return 20.0f;
        }

        if (!world.hasChunkAt(pos)) {
            LOGGER.warn("Chunk not loaded at {}, returning default temperature", pos);
            return 20.0f;
        }

        String dimensionId = world.dimension().toString();
        if (dimensionId.startsWith("ResourceKey[minecraft:dimension / ")) {
            dimensionId = dimensionId.substring("ResourceKey[minecraft:dimension / ".length(), dimensionId.length() - 1);
        }
        TemperatureMainConfig.DimensionConfig dimConfig = getDimensionConfig(dimensionId);
        if (dimConfig == null) {
            LOGGER.warn("No dimension config for {}, using default", dimensionId);
            return 20.0f;
        }

        float biomeTemp = getBiomeTemperature(world, pos, dimensionId);
        float altitudeTemp = getAltitudeTemperature(dimConfig, pos.getY());
        float dailyTemp = getDailyTemperature(world, dimConfig, dimensionId, pos, tick);
        float weatherTemp = getWeatherTemperature(world, dimConfig, pos);
        float radiationTemp = getRadiationTemperature(world, pos, dimensionId);

        float totalTemp = biomeTemp + altitudeTemp + dailyTemp + weatherTemp + radiationTemp;
        
        LOGGER.debug("Temperature at {}: biome={}, altitude={}, daily={}, weather={}, radiation={}, total={}",
                pos, biomeTemp, altitudeTemp, dailyTemp, weatherTemp, radiationTemp, totalTemp);
        
        return totalTemp;
    }

    private static TemperatureMainConfig.DimensionConfig getDimensionConfig(String dimensionId) {
        TemperatureMainConfig mainConfig = ConfigLoader.getMainConfig();
        if (mainConfig == null || mainConfig.dimensions == null) {
            return null;
        }
        return mainConfig.dimensions.get(dimensionId);
    }

    private static float getBiomeTemperature(Level world, BlockPos pos, String dimensionId) {
        Biome biome = world.getBiome(pos).value();
        
        if (ExtensionManager.hasBiomeTemperatureProvider()) {
            return ExtensionManager.getBiomeTemperatureProvider().getBiomeTemperature(world, pos, biome);
        }
        
        float baseTemp = biome.getBaseTemperature();
        return baseTemp * 30.0f;
    }

    private static float getAltitudeTemperature(TemperatureMainConfig.DimensionConfig dimConfig, int y) {
        float altitudeRate = dimConfig.altitudeRate;
        int seaLevel = dimConfig.seaLevel;
        return (y - seaLevel) * altitudeRate;
    }

    private static float getDailyTemperature(Level world, TemperatureMainConfig.DimensionConfig dimConfig, 
                                             String dimensionId, BlockPos pos, long tick) {
        if (!dimConfig.applyDailyCycle) {
            return 0.0f;
        }

        if (ExtensionManager.hasDailyTemperatureProvider()) {
            return ExtensionManager.getDailyTemperatureProvider().getDailyTemperature(world, pos, tick);
        }

        TemperatureMainConfig mainConfig = ConfigLoader.getMainConfig();
        if (mainConfig == null || mainConfig.dailyTempRange == null) {
            LOGGER.warn("Daily temp range config is null, using default");
            return 0.0f;
        }

        long day = tick / 24000;
        String cacheKey = dimensionId + ":" + day;
        
        DailyTemp dailyTemp = DAILY_TEMPS.get(cacheKey);
        if (dailyTemp == null) {
            dailyTemp = generateDailyTemp(mainConfig.dailyTempRange);
            DAILY_TEMPS.put(cacheKey, dailyTemp);
        }

        long timeOfDay = tick % 24000;
        float phase = (float) (2 * Math.PI * (timeOfDay - 6000) / 24000);
        return (dailyTemp.max + dailyTemp.min) / 2 + (dailyTemp.max - dailyTemp.min) / 2 * (float) Math.sin(phase);
    }

    private static DailyTemp generateDailyTemp(TemperatureMainConfig.DailyTempRange range) {
        float min = range.minMin + RANDOM.nextFloat() * (range.minMax - range.minMin);
        float max = range.maxMin + RANDOM.nextFloat() * (range.maxMax - range.maxMin);
        return new DailyTemp(min, max);
    }

    private static float getWeatherTemperature(Level world, TemperatureMainConfig.DimensionConfig dimConfig, BlockPos pos) {
        if (!dimConfig.applyWeather) {
            return 0.0f;
        }

        boolean isOpenSky = isOpenSky(world, pos);
        
        if (ExtensionManager.hasWeatherTemperatureProvider()) {
            return ExtensionManager.getWeatherTemperatureProvider().getWeatherTemperature(
                world, pos, world.isRaining(), world.isThundering(), isOpenSky);
        }

        if (!isOpenSky) {
            return 0.0f;
        }

        TemperatureMainConfig mainConfig = ConfigLoader.getMainConfig();
        if (mainConfig == null || mainConfig.weatherTempOffset == null) {
            LOGGER.warn("Weather temp offset config is null, using default");
            return 0.0f;
        }

        if (world.isThundering()) {
            return mainConfig.weatherTempOffset.thunder;
        } else if (world.isRaining()) {
            return mainConfig.weatherTempOffset.rain;
        } else {
            return mainConfig.weatherTempOffset.clear;
        }
    }

    private static boolean isOpenSky(Level world, BlockPos pos) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        mutablePos.set(pos);
        
        for (int y = pos.getY() + 1; y <= world.getHeight(); y++) {
            mutablePos.setY(y);
            if (world.getBlockState(mutablePos).isSolid()) {
                return false;
            }
        }
        return true;
    }

    private static float getRadiationTemperature(Level world, BlockPos pos, String dimensionId) {
        TemperatureMainConfig mainConfig = ConfigLoader.getMainConfig();
        if (mainConfig == null || mainConfig.radiation == null) {
            LOGGER.warn("Radiation config is null, skipping radiation calculation");
            return 0.0f;
        }

        int maxDistance = mainConfig.radiation.maxDistance;
        String decayType = mainConfig.radiation.decayType;

        TemperatureRadiationConfig radiationConfig = ConfigLoader.getRadiationConfig();
        if (radiationConfig == null || radiationConfig.sources == null) {
            LOGGER.warn("Radiation sources config is null, skipping radiation calculation");
            return 0.0f;
        }

        float totalRadiation = 0.0f;

        for (int dx = -maxDistance; dx <= maxDistance; dx++) {
            for (int dy = -maxDistance; dy <= maxDistance; dy++) {
                for (int dz = -maxDistance; dz <= maxDistance; dz++) {
                    BlockPos sourcePos = pos.offset(dx, dy, dz);
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    
                    if (distance > maxDistance) {
                        continue;
                    }

                    float blockTemp = getBlockTemperature(world, sourcePos);
                    
                    if (blockTemp != 0) {
                        if (!isLineOfSightBlocked(world, pos, sourcePos)) {
                            float attenuation = calculateAttenuation(decayType, distance, maxDistance);
                            totalRadiation += blockTemp * attenuation;
                        }
                    }
                }
            }
        }

        return totalRadiation;
    }

    private static float getBlockTemperature(Level world, BlockPos pos) {
        TemperatureRadiationConfig radiationConfig = ConfigLoader.getRadiationConfig();
        if (radiationConfig == null || radiationConfig.sources == null) {
            return 0.0f;
        }

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(
            world.getBlockState(pos).getBlock());
        
        if (blockId == null) {
            return 0.0f;
        }

        Float strength = radiationConfig.sources.get(blockId.toString());
        if (strength == null) {
            return 0.0f;
        }

        return strength;
    }

    private static boolean isLineOfSightBlocked(Level world, BlockPos from, BlockPos to) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 0) {
            return false;
        }

        float stepX = (float) dx / steps;
        float stepY = (float) dy / steps;
        float stepZ = (float) dz / steps;

        for (int i = 1; i < steps; i++) {
            float x = from.getX() + 0.5f + stepX * i;
            float y = from.getY() + 0.5f + stepY * i;
            float z = from.getZ() + 0.5f + stepZ * i;
            
            mutablePos.set((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            
            if (world.getBlockState(mutablePos).isSolid()) {
                return true;
            }
        }

        return false;
    }

    private static float calculateAttenuation(String decayType, double distance, int maxDistance) {
        if ("linear".equals(decayType)) {
            return (float) Math.max(0, (maxDistance - distance) / maxDistance);
        } else if ("inverse".equals(decayType)) {
            return (float) (1 / (distance + 1));
        } else {
            LOGGER.warn("Unknown decay type: {}, using linear", decayType);
            return (float) Math.max(0, (maxDistance - distance) / maxDistance);
        }
    }

    private static class DailyTemp {
        final float min;
        final float max;

        DailyTemp(float min, float max) {
            this.min = min;
            this.max = max;
        }
    }
}
