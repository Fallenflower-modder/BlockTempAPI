package com.drownedcloud.blockthermo.config;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIRECTORY = "block_thermo";

    private static TemperatureMainConfig mainConfig;
    private static TemperatureBiomesConfig biomesConfig;
    private static TemperatureRadiationConfig radiationConfig;
    private static Path configDirPath;

    public static void init() {
        configDirPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIRECTORY);

        try {
            if (!Files.exists(configDirPath)) {
                Files.createDirectories(configDirPath);
                LOGGER.info("Created config directory at {}", configDirPath);
            }

            loadAllConfigs();
            LOGGER.info("ConfigLoader initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize ConfigLoader", e);
            throw new RuntimeException("Failed to initialize ConfigLoader", e);
        }
    }

    private static void loadAllConfigs() {
        mainConfig = loadOrCreateConfig("temperature_main.json", TemperatureMainConfig.class);
        biomesConfig = loadOrCreateConfig("temperature_biomes.json", TemperatureBiomesConfig.class);
        radiationConfig = loadOrCreateConfig("temperature_radiation.json", TemperatureRadiationConfig.class);

        validateConfigs();
    }

    private static <T> T loadOrCreateConfig(String fileName, Class<T> clazz) {
        Path filePath = configDirPath.resolve(fileName);

        try {
            if (Files.exists(filePath)) {
                LOGGER.info("Loading config file: {}", filePath);
                String content = Files.readString(filePath, StandardCharsets.UTF_8);
                
                if (clazz == TemperatureMainConfig.class) {
                    content = migrateMainConfig(content, filePath);
                }
                
                T config = GSON.fromJson(content, clazz);
                LOGGER.info("Successfully loaded config: {}", fileName);
                return config;
            } else {
                LOGGER.warn("Config file not found, creating default: {}", filePath);
                T defaultConfig = createDefaultConfig(clazz);
                saveConfig(fileName, defaultConfig);
                return defaultConfig;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config: {}", fileName, e);
            throw new RuntimeException("Failed to load config: " + fileName, e);
        }
    }

    private static String migrateMainConfig(String content, Path filePath) {
        try {
            JsonElement root = JsonParser.parseString(content);
            
            if (!root.isJsonObject()) {
                return content;
            }
            
            JsonObject rootObj = root.getAsJsonObject();
            
            if (!rootObj.has("radiation") || !rootObj.get("radiation").isJsonObject()) {
                return content;
            }
            
            JsonObject radiation = rootObj.getAsJsonObject("radiation");
            
            if (radiation.has("decay_type") && radiation.get("decay_type").isJsonPrimitive()) {
                String oldDecayType = radiation.get("decay_type").getAsString();
                LOGGER.info("Detected old format decay_type: {}, migrating to new format", oldDecayType);
                
                radiation.remove("decay_type");
                
                JsonObject decayTypeObj = new JsonObject();
                decayTypeObj.addProperty("select", oldDecayType);
                
                JsonObject formulasObj = new JsonObject();
                formulasObj.addProperty("linear", "(maxDistance - distance) / maxDistance");
                formulasObj.addProperty("inverse", "1 / (distance + 1)");
                formulasObj.addProperty("exponential", "exp(-distance / maxDistance)");
                formulasObj.addProperty("quadratic", "1 - (distance / maxDistance)^2");
                
                decayTypeObj.add("formulas", formulasObj);
                radiation.add("decay_type", decayTypeObj);
                
                String migratedContent = GSON.toJson(root);
                Files.writeString(filePath, migratedContent, StandardCharsets.UTF_8);
                LOGGER.info("Successfully migrated config to new format");
                
                return migratedContent;
            }
            
            return content;
        } catch (Exception e) {
            LOGGER.error("Failed to migrate main config", e);
            return content;
        }
    }

    private static <T> T createDefaultConfig(Class<T> clazz) {
        try {
            if (clazz == TemperatureMainConfig.class) {
                TemperatureMainConfig config = new TemperatureMainConfig();
                config.version = "1.0.0";
                config.dimensions = new HashMap<>();
                
                TemperatureMainConfig.DimensionConfig overworld = new TemperatureMainConfig.DimensionConfig();
                overworld.altitudeRate = -0.01f;
                overworld.seaLevel = 63;
                overworld.applyDailyCycle = true;
                overworld.applyWeather = true;
                config.dimensions.put("minecraft:overworld", overworld);
                
                TemperatureMainConfig.DimensionConfig nether = new TemperatureMainConfig.DimensionConfig();
                nether.altitudeRate = 0.0f;
                nether.seaLevel = 31;
                nether.applyDailyCycle = false;
                nether.applyWeather = false;
                config.dimensions.put("minecraft:nether", nether);
                
                TemperatureMainConfig.DimensionConfig end = new TemperatureMainConfig.DimensionConfig();
                end.altitudeRate = 0.0f;
                end.seaLevel = 63;
                end.applyDailyCycle = false;
                end.applyWeather = false;
                config.dimensions.put("minecraft:end", end);
                
                config.dailyTempRange = new TemperatureMainConfig.DailyTempRange();
                config.dailyTempRange.minMin = 5.0f;
                config.dailyTempRange.minMax = 10.0f;
                config.dailyTempRange.maxMin = 25.0f;
                config.dailyTempRange.maxMax = 35.0f;
                
                config.weatherTempOffset = new TemperatureMainConfig.WeatherTempOffset();
                config.weatherTempOffset.clear = 0.0f;
                config.weatherTempOffset.rain = -5.0f;
                config.weatherTempOffset.thunder = -10.0f;
                
                config.radiation = new TemperatureMainConfig.Radiation();
                config.radiation.maxDistance = 5;
                config.radiation.decayType = new TemperatureMainConfig.DecayType();
                config.radiation.decayType.select = "linear";
                config.radiation.decayType.formulas = new HashMap<>();
                config.radiation.decayType.formulas.put("linear", "(maxDistance - distance) / maxDistance");
                config.radiation.decayType.formulas.put("inverse", "1 / (distance + 1)");
                config.radiation.decayType.formulas.put("exponential", "exp(-distance / maxDistance)");
                config.radiation.decayType.formulas.put("quadratic", "1 - (distance / maxDistance)^2");
                
                return clazz.cast(config);
            } else if (clazz == TemperatureBiomesConfig.class) {
                TemperatureBiomesConfig config = new TemperatureBiomesConfig();
                config.version = "1.0.0";
                config.biomes = new HashMap<>();
                return clazz.cast(config);
            } else if (clazz == TemperatureRadiationConfig.class) {
                TemperatureRadiationConfig config = new TemperatureRadiationConfig();
                config.sources = new HashMap<>();
                config.sources.put("minecraft:fire", 50.0f);
                config.sources.put("minecraft:lava", 100.0f);
                config.sources.put("minecraft:torch", 10.0f);
                config.sources.put("minecraft:campfire", 30.0f);
                config.sources.put("minecraft:soul_fire", 30.0f);
                return clazz.cast(config);
            } else {
                throw new IllegalArgumentException("Unknown config class: " + clazz);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create default config for: {}", clazz, e);
            throw new RuntimeException("Failed to create default config for: " + clazz, e);
        }
    }

    private static <T> void saveConfig(String fileName, T config) {
        Path filePath = configDirPath.resolve(fileName);

        try {
            String content = GSON.toJson(config);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            LOGGER.info("Saved config file: {}", filePath);
        } catch (Exception e) {
            LOGGER.error("Failed to save config: {}", fileName, e);
            throw new RuntimeException("Failed to save config: " + fileName, e);
        }
    }

    private static void validateConfigs() {
        if (mainConfig == null) {
            throw new IllegalStateException("Main config is null");
        }
        if (biomesConfig == null) {
            throw new IllegalStateException("Biomes config is null");
        }
        if (radiationConfig == null) {
            throw new IllegalStateException("Radiation config is null");
        }

        LOGGER.info("All configs validated successfully");
    }

    public static void reloadConfigs() {
        LOGGER.info("Reloading all configs");
        loadAllConfigs();
    }

    public static TemperatureMainConfig getMainConfig() {
        return mainConfig;
    }

    public static TemperatureBiomesConfig getBiomesConfig() {
        return biomesConfig;
    }

    public static TemperatureRadiationConfig getRadiationConfig() {
        return radiationConfig;
    }
}
