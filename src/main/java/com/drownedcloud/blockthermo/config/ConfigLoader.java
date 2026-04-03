package com.drownedcloud.blockthermo.config;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static TemperatureMainConfig mainConfig;
    private static TemperatureRadiationConfig radiationConfig;

    public static void init() {
        LOGGER.info("Initializing ConfigLoader");
        loadMainConfig();
        loadRadiationConfig();
    }

    private static void loadMainConfig() {
        mainConfig = new TemperatureMainConfig();
        
        mainConfig.dimensions = new HashMap<>();
        
        TemperatureMainConfig.DimensionConfig overworld = new TemperatureMainConfig.DimensionConfig();
        overworld.altitudeRate = -0.01f;
        overworld.seaLevel = 63;
        overworld.applyDailyCycle = true;
        overworld.applyWeather = true;
        mainConfig.dimensions.put("minecraft:overworld", overworld);
        
        TemperatureMainConfig.DimensionConfig nether = new TemperatureMainConfig.DimensionConfig();
        nether.altitudeRate = 0.0f;
        nether.seaLevel = 31;
        nether.applyDailyCycle = false;
        nether.applyWeather = false;
        mainConfig.dimensions.put("minecraft:nether", nether);
        
        TemperatureMainConfig.DimensionConfig end = new TemperatureMainConfig.DimensionConfig();
        end.altitudeRate = 0.0f;
        end.seaLevel = 63;
        end.applyDailyCycle = false;
        end.applyWeather = false;
        mainConfig.dimensions.put("minecraft:end", end);
        
        mainConfig.dailyTempRange = new TemperatureMainConfig.DailyTempRange();
        mainConfig.dailyTempRange.minMin = 5.0f;
        mainConfig.dailyTempRange.minMax = 10.0f;
        mainConfig.dailyTempRange.maxMin = 25.0f;
        mainConfig.dailyTempRange.maxMax = 35.0f;
        
        mainConfig.weatherTempOffset = new TemperatureMainConfig.WeatherTempOffset();
        mainConfig.weatherTempOffset.clear = 0.0f;
        mainConfig.weatherTempOffset.rain = -5.0f;
        mainConfig.weatherTempOffset.thunder = -10.0f;
        
        mainConfig.radiation = new TemperatureMainConfig.Radiation();
        mainConfig.radiation.maxDistance = 5;
        mainConfig.radiation.decayType = "inverse";
        
        LOGGER.info("Main config loaded");
    }

    private static void loadRadiationConfig() {
        radiationConfig = new TemperatureRadiationConfig();
        radiationConfig.sources = new HashMap<>();
        
        radiationConfig.sources.put("minecraft:torch", 5.0f);
        radiationConfig.sources.put("minecraft:campfire", 10.0f);
        radiationConfig.sources.put("minecraft:lava", 15.0f);
        radiationConfig.sources.put("minecraft:fire", 12.0f);
        radiationConfig.sources.put("minecraft:glowstone", 8.0f);
        radiationConfig.sources.put("minecraft:jack_o_lantern", 6.0f);
        radiationConfig.sources.put("minecraft:lantern", 7.0f);
        
        LOGGER.info("Radiation config loaded");
    }

    public static TemperatureMainConfig getMainConfig() {
        return mainConfig;
    }

    public static TemperatureRadiationConfig getRadiationConfig() {
        return radiationConfig;
    }
}
