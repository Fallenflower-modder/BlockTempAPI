package com.drownedcloud.blockthermo.config;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class TemperatureRadiationConfig {
    @SerializedName("version")
    public String version;

    @SerializedName("sources")
    public Map<String, Float> sources;
}
