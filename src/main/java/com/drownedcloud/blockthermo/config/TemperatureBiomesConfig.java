package com.drownedcloud.blockthermo.config;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class TemperatureBiomesConfig {
    @SerializedName("version")
    public String version;

    @SerializedName("biomes")
    public Map<String, BiomeConfig> biomes;

    public static class BiomeConfig {
        @SerializedName("base_temp")
        public float baseTemp;

        @SerializedName("altitude_rate")
        public float altitudeRate;
    }
}
