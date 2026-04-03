package com.drownedcloud.blockthermo.config;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class TemperatureMainConfig {
    @SerializedName("version")
    public String version;

    @SerializedName("dimensions")
    public Map<String, DimensionConfig> dimensions;

    @SerializedName("daily_temp_range")
    public DailyTempRange dailyTempRange;

    @SerializedName("weather_temp_offset")
    public WeatherTempOffset weatherTempOffset;

    @SerializedName("radiation")
    public Radiation radiation;

    public static class DimensionConfig {
        @SerializedName("apply_daily_cycle")
        public boolean applyDailyCycle;

        @SerializedName("apply_weather")
        public boolean applyWeather;

        @SerializedName("sea_level")
        public int seaLevel;

        @SerializedName("altitude_rate")
        public float altitudeRate;
    }

    public static class DailyTempRange {
        @SerializedName("min_min")
        public float minMin;

        @SerializedName("min_max")
        public float minMax;

        @SerializedName("max_min")
        public float maxMin;

        @SerializedName("max_max")
        public float maxMax;
    }

    public static class WeatherTempOffset {
        @SerializedName("clear")
        public float clear;

        @SerializedName("rain")
        public float rain;

        @SerializedName("thunder")
        public float thunder;
    }

    public static class Radiation {
        @SerializedName("max_distance")
        public int maxDistance;

        @SerializedName("decay_type")
        public DecayType decayType;
    }

    public static class DecayType {
        @SerializedName("select")
        public String select;

        @SerializedName("formulas")
        public Map<String, String> formulas;
    }
}
