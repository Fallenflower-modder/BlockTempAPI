package com.drownedcloud.blockthermo.temperature;

import com.drownedcloud.blockthermo.api.ExtensionManager;
import com.drownedcloud.blockthermo.config.ConfigLoader;
import com.drownedcloud.blockthermo.config.TemperatureMainConfig;
import com.drownedcloud.blockthermo.config.TemperatureRadiationConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

        String dimensionId = world.dimension().location().toString();
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
        TemperatureMainConfig.DecayType decayType = mainConfig.radiation.decayType;

        if (decayType == null || decayType.select == null || decayType.formulas == null) {
            LOGGER.warn("Decay type config is null, skipping radiation calculation");
            return 0.0f;
        }

        String selectedFormula = decayType.select;
        String formula = decayType.formulas.get(selectedFormula);

        if (formula == null) {
            LOGGER.warn("No formula found for decay type: {}, skipping radiation calculation", selectedFormula);
            return 0.0f;
        }

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
                            float attenuation = calculateAttenuation(formula, distance, maxDistance);
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

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(
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

    private static float calculateAttenuation(String formula, double distance, int maxDistance) {
        try {
            FormulaParser parser = new FormulaParser(formula, distance, maxDistance);
            return parser.evaluate();
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate attenuation formula: {}", formula, e);
            LOGGER.warn("Using linear attenuation as fallback");
            return (float) Math.max(0, (maxDistance - distance) / maxDistance);
        }
    }

    private static class FormulaParser {
        private final String formula;
        private final double distance;
        private final int maxDistance;
        private int pos = 0;

        FormulaParser(String formula, double distance, int maxDistance) {
            this.formula = formula;
            this.distance = distance;
            this.maxDistance = maxDistance;
        }

        float evaluate() {
            double result = parseExpression();
            skipWhitespace();
            if (pos < formula.length()) {
                throw new IllegalArgumentException("Unexpected character at position " + pos);
            }
            return (float) result;
        }

        private double parseExpression() {
            double left = parseTerm();
            while (true) {
                skipWhitespace();
                if (pos < formula.length()) {
                    char c = formula.charAt(pos);
                    if (c == '+' || c == '-') {
                        pos++;
                        double right = parseTerm();
                        if (c == '+') {
                            left += right;
                        } else {
                            left -= right;
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            return left;
        }

        private double parseTerm() {
            double left = parseFactor();
            while (true) {
                skipWhitespace();
                if (pos < formula.length()) {
                    char c = formula.charAt(pos);
                    if (c == '*' || c == '/') {
                        pos++;
                        double right = parseFactor();
                        if (c == '*') {
                            left *= right;
                        } else {
                            if (right == 0) {
                                throw new ArithmeticException("Division by zero");
                            }
                            left /= right;
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            return left;
        }

        private double parseFactor() {
            skipWhitespace();
            if (pos >= formula.length()) {
                throw new IllegalArgumentException("Unexpected end of formula");
            }

            char c = formula.charAt(pos);
            if (c == '(') {
                pos++;
                double result = parseExpression();
                skipWhitespace();
                if (pos >= formula.length() || formula.charAt(pos) != ')') {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                pos++;
                return result;
            } else if (c == '-') {
                pos++;
                return -parseFactor();
            } else if (c == '+') {
                pos++;
                return parseFactor();
            } else if (Character.isDigit(c) || c == '.') {
                return parseNumber();
            } else if (Character.isLetter(c)) {
                return parseFunctionOrVariable();
            } else {
                throw new IllegalArgumentException("Unexpected character: " + c + " at position " + pos);
            }
        }

        private double parseNumber() {
            int start = pos;
            while (pos < formula.length() && (Character.isDigit(formula.charAt(pos)) || formula.charAt(pos) == '.')) {
                pos++;
            }
            String number = formula.substring(start, pos);
            try {
                return Double.parseDouble(number);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number: " + number);
            }
        }

        private double parseFunctionOrVariable() {
            int start = pos;
            while (pos < formula.length() && Character.isLetterOrDigit(formula.charAt(pos))) {
                pos++;
            }
            String name = formula.substring(start, pos);

            skipWhitespace();
            if (pos < formula.length() && formula.charAt(pos) == '(') {
                pos++;
                double argument = parseExpression();
                skipWhitespace();
                if (pos >= formula.length() || formula.charAt(pos) != ')') {
                    throw new IllegalArgumentException("Missing closing parenthesis for function: " + name);
                }
                pos++;
                return evaluateFunction(name, argument);
            } else {
                return evaluateVariable(name);
            }
        }

        private double evaluateFunction(String name, double argument) {
            switch (name.toLowerCase()) {
                case "exp":
                    return Math.exp(argument);
                case "ln":
                case "log":
                    if (argument <= 0) {
                        throw new IllegalArgumentException("Logarithm of non-positive number");
                    }
                    return Math.log(argument);
                case "log10":
                    if (argument <= 0) {
                        throw new IllegalArgumentException("Logarithm of non-positive number");
                    }
                    return Math.log10(argument);
                case "sqrt":
                    if (argument < 0) {
                        throw new IllegalArgumentException("Square root of negative number");
                    }
                    return Math.sqrt(argument);
                case "sin":
                    return Math.sin(argument);
                case "cos":
                    return Math.cos(argument);
                case "tan":
                    return Math.tan(argument);
                case "asin":
                    if (argument < -1 || argument > 1) {
                        throw new IllegalArgumentException("Asin argument out of range [-1, 1]");
                    }
                    return Math.asin(argument);
                case "acos":
                    if (argument < -1 || argument > 1) {
                        throw new IllegalArgumentException("Acos argument out of range [-1, 1]");
                    }
                    return Math.acos(argument);
                case "atan":
                    return Math.atan(argument);
                case "abs":
                    return Math.abs(argument);
                default:
                    throw new IllegalArgumentException("Unknown function: " + name);
            }
        }

        private double evaluateVariable(String name) {
            switch (name.toLowerCase()) {
                case "distance":
                case "d":
                    return distance;
                case "maxdistance":
                case "md":
                    return maxDistance;
                default:
                    throw new IllegalArgumentException("Unknown variable: " + name);
            }
        }

        private void skipWhitespace() {
            while (pos < formula.length() && Character.isWhitespace(formula.charAt(pos))) {
                pos++;
            }
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
