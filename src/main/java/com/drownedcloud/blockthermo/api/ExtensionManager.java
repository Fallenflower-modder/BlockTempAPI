package com.drownedcloud.blockthermo.api;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class ExtensionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static BiomeTemperatureProvider biomeTemperatureProvider;
    private static DailyTemperatureProvider dailyTemperatureProvider;
    private static WeatherTemperatureProvider weatherTemperatureProvider;

    public static void registerBiomeTemperatureProvider(BiomeTemperatureProvider provider) {
        LOGGER.info("Registering BiomeTemperatureProvider");
        biomeTemperatureProvider = provider;
    }

    public static void registerDailyTemperatureProvider(DailyTemperatureProvider provider) {
        LOGGER.info("Registering DailyTemperatureProvider");
        dailyTemperatureProvider = provider;
    }

    public static void registerWeatherTemperatureProvider(WeatherTemperatureProvider provider) {
        LOGGER.info("Registering WeatherTemperatureProvider");
        weatherTemperatureProvider = provider;
    }

    public static boolean hasBiomeTemperatureProvider() {
        return biomeTemperatureProvider != null;
    }

    public static BiomeTemperatureProvider getBiomeTemperatureProvider() {
        return biomeTemperatureProvider;
    }

    public static boolean hasDailyTemperatureProvider() {
        return dailyTemperatureProvider != null;
    }

    public static DailyTemperatureProvider getDailyTemperatureProvider() {
        return dailyTemperatureProvider;
    }

    public static boolean hasWeatherTemperatureProvider() {
        return weatherTemperatureProvider != null;
    }

    public static WeatherTemperatureProvider getWeatherTemperatureProvider() {
        return weatherTemperatureProvider;
    }
}
