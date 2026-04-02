# Block Thermo

<div align="center">
  <img src="src/main/resources/META-INF/logo.png" alt="Block Thermo Logo" width="200" height="200" />
</div>

一个基于 Minecraft 1.21.1 NeoForge 的热力学系统模组，提供精确的方块坐标温度计算功能。

## 功能特性

- **精确温度计算**：基于生物群系、海拔高度、时间、天气和辐射源的多因素温度计算
- **配置系统**：支持自定义维度配置、生物群系温度和辐射源配置
- **API 接口**：提供完整的 API 供其他模组扩展
- **游戏命令**：内置温度查询命令
- **多语言支持**：支持英文和简体中文

## 构建要求

- Java 21
- Gradle 8.5+
- Minecraft 1.21.1
- NeoForge 21.1.186

## 快速开始

### 构建项目

```bash
./gradlew build
```

构建完成后， jar 文件将位于 `build/libs/` 目录下。

### 安装模组

1. 下载最新版本的模组 jar 文件
2. 将 jar 文件放入 Minecraft 的 `mods` 文件夹中
3. 启动游戏，配置文件会在首次加载时自动生成

## 配置说明

模组配置文件位于 Minecraft 目录下的 `config/block_thermo/` 文件夹中，包含以下文件：

### temperature_main.json

主配置文件，包含维度配置、每日温度范围、天气温度偏移和辐射设置：

```json
{
  "version": "1.0.0",
  "dimensions": {
    "minecraft:overworld": {
      "altitude_rate": -0.01,
      "sea_level": 63,
      "apply_daily_cycle": true,
      "apply_weather": true
    },
    "minecraft:nether": {
      "altitude_rate": 0.0,
      "sea_level": 31,
      "apply_daily_cycle": false,
      "apply_weather": false
    },
    "minecraft:end": {
      "altitude_rate": 0.0,
      "sea_level": 63,
      "apply_daily_cycle": false,
      "apply_weather": false
    }
  },
  "daily_temp_range": {
    "min_min": 5.0,
    "min_max": 10.0,
    "max_min": 25.0,
    "max_max": 35.0
  },
  "weather_temp_offset": {
    "clear": 0.0,
    "rain": -5.0,
    "thunder": -10.0
  },
  "radiation": {
    "max_distance": 5,
    "decay_type": "inverse"
  }
}
```

### temperature_biomes.json

生物群系配置文件，用于自定义特定生物群系的基础温度和海拔变化率：

```json
{
  "version": "1.0.0",
  "biomes": {
    "minecraft:desert": {
      "base_temp": 30.0,
      "altitude_rate": -0.008
    },
    "minecraft:snowy_tundra": {
      "base_temp": -10.0,
      "altitude_rate": -0.012
    }
  }
}
```

### temperature_radiation.json

辐射源配置文件，用于定义方块的辐射强度：

```json
{
  "sources": {
    "minecraft:fire": 50.0,
    "minecraft:lava": 100.0,
    "minecraft:torch": 10.0,
    "minecraft:campfire": 30.0,
    "minecraft:soul_fire": 30.0
  }
}
```

## 游戏命令

### 查询温度

```
/bt temp
```

显示当前所在位置的温度信息，包括：
- 基础温度
- 海拔修正
- 时间修正
- 天气修正
- 辐射修正
- 最终温度

### 查询指定位置温度

```
/bt temp <x> <y> <z>
```

显示指定坐标位置的温度信息。

### 重载配置

```
/bt reload
```

重新加载所有配置文件（需要管理员权限）。

## API 使用

### 获取温度

```java
import com.drownedcloud.blockthermo.temperature.TemperatureCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// 获取当前位置的温度
float temperature = TemperatureCalculator.getTemperature(level, blockPos);
```

### 扩展温度计算

模组提供了多个扩展点供其他模组自定义温度计算：

#### 生物群系温度提供器

```java
import com.drownedcloud.blockthermo.api.ExtensionManager;
import com.drownedcloud.blockthermo.api.BiomeTemperatureProvider;
import net.minecraft.world.level.biome.Biome;

ExtensionManager.setBiomeTemperatureProvider(new BiomeTemperatureProvider() {
    @Override
    public float getBaseTemperature(Biome biome) {
        // 自定义生物群系基础温度计算
        return biome.getBaseTemperature() * 30.0f;
    }
});
```

#### 海拔温度提供器

```java
import com.drownedcloud.blockthermo.api.ExtensionManager;
import com.drownedcloud.blockthermo.api.AltitudeTemperatureProvider;

ExtensionManager.setAltitudeTemperatureProvider(new AltitudeTemperatureProvider() {
    @Override
    public float getAltitudeModifier(float altitudeRate, int y, int seaLevel) {
        // 自定义海拔修正计算
        return altitudeRate * (y - seaLevel);
    }
});
```

#### 时间温度提供器

```java
import com.drownedcloud.blockthermo.api.ExtensionManager;
import com.drownedcloud.blockthermo.api.TimeTemperatureProvider;

ExtensionManager.setTimeTemperatureProvider(new TimeTemperatureProvider() {
    @Override
    public float getTimeModifier(Level level, float minTemp, float maxTemp) {
        // 自定义时间修正计算
        long dayTime = level.getDayTime() % 24000;
        return minTemp + (maxTemp - minTemp) * (1 - Math.abs(dayTime - 12000) / 12000.0f);
    }
});
```

#### 天气温度提供器

```java
import com.drownedcloud.blockthermo.api.ExtensionManager;
import com.drownedcloud.blockthermo.api.WeatherTemperatureProvider;

ExtensionManager.setWeatherTemperatureProvider(new WeatherTemperatureProvider() {
    @Override
    public float getWeatherModifier(Level level, float clear, float rain, float thunder) {
        // 自定义天气修正计算
        if (level.isThundering()) return thunder;
        if (level.isRaining()) return rain;
        return clear;
    }
});
```

#### 辐射温度提供器

```java
import com.drownedcloud.blockthermo.api.ExtensionManager;
import com.drownedcloud.blockthermo.api.RadiationTemperatureProvider;

ExtensionManager.setRadiationTemperatureProvider(new RadiationTemperatureProvider() {
    @Override
    public float getRadiationModifier(Level level, BlockPos pos, int maxDistance, String decayType, Map<Block, Float> sources) {
        // 自定义辐射修正计算
        return 0.0f;
    }
});
```

## 温度计算算法

模组使用以下公式计算最终温度：

```
最终温度 = 生物群系温度 + 海拔修正 + 时间修正 + 天气修正 + 辐射修正
```

### 生物群系温度

- 如果在 `temperature_biomes.json` 中配置了该生物群系，则使用配置的基础温度
- 否则使用生物群系的默认基础温度属性

### 海拔修正

- 根据配置的海拔变化率和当前高度计算
- `海拔修正 = 海拔变化率 × (当前高度 - 海平面高度)`

### 时间修正

- 基于每日温度范围和当前时间计算
- 正午时温度最高，午夜时温度最低

### 天气修正

- 根据当前天气状态应用温度偏移
- 晴天：0.0°C
- 下雨：-5.0°C
- 雷暴：-10.0°C

### 辐射修正

- 检测周围指定距离内的辐射源
- 根据距离衰减计算辐射影响
- 支持线性衰减和反比衰减两种类型

## 开发者指南

### 包结构

```
com.drownedcloud.blockthermo
├── api/                    # API 接口和扩展点
│   ├── extension/          # 扩展实现（可选）
│   ├── AltitudeTemperatureProvider.java
│   ├── BiomeTemperatureProvider.java
│   ├── ExtensionManager.java
│   ├── RadiationTemperatureProvider.java
│   ├── TimeTemperatureProvider.java
│   └── WeatherTemperatureProvider.java
├── command/                # 命令实现
│   └── TemperatureCommand.java
├── config/                 # 配置系统
│   ├── ConfigLoader.java
│   ├── TemperatureBiomesConfig.java
│   ├── TemperatureMainConfig.java
│   └── TemperatureRadiationConfig.java
├── temperature/            # 温度计算核心
│   └── TemperatureCalculator.java
└── BlockThermo.java        # 模组主类
```

### 事件监听

模组使用 NeoForge 的事件系统：

```java
@Mod(BlockThermo.MOD_ID)
public class BlockThermo {
    public static final String MOD_ID = "block_thermo";

    public BlockThermo() {
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TemperatureCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ConfigLoader.init();
    }
}
```

### 本地化

模组支持多语言，语言文件位于 `src/main/resources/assets/block_thermo/lang/`：

- `en_us.json` - 英文
- `zh_cn.json` - 简体中文

添加新语言只需创建对应的语言文件并按照现有格式添加翻译。

## 许可证

本模组仅供学习和研究使用。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

- 作者：DrownedCloud
- 项目地址：[GitHub Repository]

## 更新日志

### 0.1.1-build1
- 修复配置文件生成问题
- 优化温度计算算法
- 改进 API 接口设计
- 添加模组 Logo 支持

### 0.1.0-build1
- 初始版本发布
- 实现基础温度计算功能
- 添加配置系统
- 实现游戏命令
- 添加多语言支持
