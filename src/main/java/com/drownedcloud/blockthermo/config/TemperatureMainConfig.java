package com.drownedcloud.blockthermo.config;

import java.util.Map;

public class TemperatureMainConfig {
    public Map<String, DimensionConfig> dimensions;
    public DailyTempRange dailyTempRange;
    public WeatherTempOffset weatherTempOffset;
    public Radiation radiation;

    public static class DimensionConfig {
        public float altitudeRate;
        public int seaLevel;
        public boolean applyDailyCycle;
        public boolean applyWeather;
    }

    public static class DailyTempRange {
        public float minMin;
        public float minMax;
        public float maxMin;
        public float maxMax;
    }

    public static class WeatherTempOffset {
        public float clear;
        public float rain;
        public float thunder;
    }

    public static class Radiation {
        public int maxDistance;
        public String decayType;
    }
}
