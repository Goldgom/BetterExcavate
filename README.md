# BetterExcavate

一个全面的挖掘机制改革模组，为Minecraft 1.20.1引入基于工具硬度的现实化挖掘系统。

## 🌟 核心特性

### 🔨 工具硬度系统
- **现实化挖掘机制**：每个工具都有独特的硬度值，决定可挖掘方块的种类
- **智能工具匹配**：自动检测原版和模组工具的硬度属性
- **可配置硬度值**：支持自定义每个工具的硬度参数

### ⚖️ 三级挖掘模式
1. **正常挖掘** (绿色) - 工具硬度充足，正常掉落物品和经验
2. **缓慢挖掘** (橙色) - 工具硬度不足但在容忍范围内，挖掘缓慢且无掉落物
3. **无法挖掘** (红色) - 工具硬度严重不足，完全无法破坏方块

### 🛠️ 工具类型验证
- **智能工具检测**：自动识别镐子、斧头、铲子、锄头、剑的适用方块
- **错误工具惩罚**：使用错误工具类型时挖掘速度降低50%
- **工具适配性提示**：实时显示工具是否适合当前方块

### 💎 耐久度影响系统
- **渐进式硬度衰减**：工具磨损时硬度随之下降，采用自然对数曲线
- **智能损坏机制**：挖掘过硬方块时工具会持续损坏
- **耐久度可视化**：直观显示工具剩余耐久度和磨损百分比

### 🔍 Jade集成
- **实时信息显示**：鼠标悬停显示方块硬度、工具硬度、挖掘速度
- **挖掘模式指示**：清晰标示当前挖掘状态和可能性
- **工具效率分析**：显示工具类型正确性和速度惩罚信息
- **耐久度监控**：实时显示工具磨损情况

### 🎯 高级特性
- **周围方块影响**：相同方块聚集时挖掘速度会受到影响
- **精确状态追踪**：智能检测玩家挖掘状态，防止状态丢失
- **多维度支持**：每个维度的挖掘状态独立管理
- **性能优化**：高效的事件处理和状态管理

## ⚙️ 配置选项详解

配置文件位置：`.minecraft/config/betterexcavate-common.toml`

### 🔧 工具硬度配置
```toml
# 工具硬度配置，格式为 '工具名:硬度值'
toolHardness = [
    "minecraft:wooden_pickaxe:2.1",     # 木镐 - 可挖掘泥土、沙子等软质方块
    "minecraft:stone_pickaxe:3.5",      # 石镐 - 可挖掘煤矿、铁矿
    "minecraft:iron_pickaxe:5.0",       # 铁镐 - 可挖掘金矿、青金石矿
    "minecraft:diamond_pickaxe:60.0",   # 钻石镐 - 可挖掘钻石矿、黑曜石
    "minecraft:netherite_pickaxe:110.0" # 下界合金镐 - 可挖掘所有方块
]

# 未配置工具的默认硬度 (范围: 0.1 ~ 100.0)
defaultHardness = 1.0                   # 推荐值: 1.0 (空手挖掘硬度)

# 自动检测工具硬度 (基于原版等级)
autoDetectToolHardness = true           # 推荐值: true (自动适配模组工具)
```

### ⚖️ 基础挖掘机制
```toml
# 硬度乘数，决定最大可挖掘硬度 (范围: 0.1 ~ 10.0)
hardnessMultiplier = 1.0                # 工具硬度 × 此值 = 最大可挖掘硬度
                                        # 推荐值: 1.0 (平衡的挖掘体验)

# 启用基于硬度的掉落物控制
enableDropControl = true                # 推荐值: true (核心功能)
```

### 🛡️ 缓慢挖掘系统
```toml
# 启用无掉落物缓慢挖掘模式
enableSlowMiningWithoutDrops = true     # 推荐值: true (提供缓冲机制)

# 缓慢挖掘硬度乘数 (范围: 1.0 ~ 10.0)
slowMiningHardnessMultiplier = 2.0      # 工具硬度 × 此值 = 最大缓慢可挖掘硬度
                                        # 推荐值: 2.0 (合理的容错范围)

# 缓慢挖掘速度惩罚 (范围: 0.0 ~ 1.0)
slowMiningSpeedPenalty = 0.8            # 0.8 = 80%速度减少
                                        # 推荐值: 0.8 (明显的速度差异)
```

### 💎 耐久度影响系统
```toml
# 启用耐久度速度惩罚
enableDurabilitySpeedPenalty = true     # 推荐值: true (现实化体验)

# 启用耐久度硬度惩罚
enableDurabilityHardnessPenalty = true  # 推荐值: true (核心机制)

# 完全磨损工具的最大速度惩罚 (范围: 0.0 ~ 1.0)
maxDurabilitySpeedPenalty = 0.5         # 0.5 = 最多50%速度减少
                                        # 推荐值: 0.5 (适中的惩罚)

# 完全磨损工具的最大硬度惩罚 (范围: 0.0 ~ 1.0)
maxDurabilityHardnessPenalty = 0.32     # 0.32 = 最多32%硬度减少
                                        # 推荐值: 0.32 (渐进式衰减)

# 耐久度惩罚曲线类型
durabilityPenaltyCurve = "quadratic"    # 选项: linear(线性), quadratic(二次), exponential(指数)
                                        # 推荐值: quadratic (平滑的渐进效果)
```

### 🛠️ 工具类型验证系统
```toml
# 启用错误工具类型惩罚
enableWrongToolPenalty = true           # 推荐值: true (鼓励正确工具使用)

# 错误工具类型速度惩罚 (范围: 0.0 ~ 1.0)
wrongToolSpeedPenalty = 0.5             # 0.5 = 50%速度减少
                                        # 推荐值: 0.5 (显著但不过分的惩罚)
```

### ⚙️ 工具损坏机制
```toml
# 启用无效挖掘时的工具损坏
enableToolDamageOnInvalidMining = true  # 推荐值: true (防止暴力挖掘)

# 工具损坏硬度阈值 (范围: 0.01 ~ 1.0)
toolDamageHardnessThreshold = 0.1       # 当硬度比值低于此值时工具会损坏
                                        # 推荐值: 0.1 (仅在严重不匹配时损坏)
```

### 🌍 环境影响系统
```toml
# 启用周围方块对挖掘速度的影响
enableSurroundingBlocksModifier = true  # 推荐值: true (增加策略性)

# 被相同方块包围时的最小速度乘数 (范围: 0.001 ~ 1.0)
minSpeedMultiplier = 0.05               # 0.05 = 最多95%速度减少
                                        # 推荐值: 0.05 (显著的聚集效应)

# 周围无相同方块时的最大速度乘数 (范围: 1.0 ~ 5.0)
maxSpeedMultiplier = 1.0                # 1.0 = 无加速效果
                                        # 推荐值: 1.0 (保持平衡)

# 速度变化曲线类型
speedCurveType = "logarithmic"          # 选项: linear(线性), logarithmic(对数)
                                        # 推荐值: logarithmic (更自然的过渡)
```

### 📋 配置建议

#### 🎯 新手友好配置
```toml
hardnessMultiplier = 1.2                # 稍微宽松的挖掘限制
maxDurabilityHardnessPenalty = 0.2      # 较轻的耐久度惩罚
wrongToolSpeedPenalty = 0.3             # 较轻的错误工具惩罚
enableToolDamageOnInvalidMining = false # 关闭工具损坏
```

#### ⚔️ 困难模式配置
```toml
hardnessMultiplier = 0.8                # 更严格的挖掘限制
maxDurabilityHardnessPenalty = 0.5      # 更重的耐久度惩罚
wrongToolSpeedPenalty = 0.7             # 更重的错误工具惩罚
toolDamageHardnessThreshold = 0.2       # 更容易触发工具损坏
minSpeedMultiplier = 0.02               # 更强的聚集效应
```

#### 🔄 纯展示模式配置
```toml
enableDropControl = false               # 关闭掉落物控制
enableDurabilityHardnessPenalty = false # 关闭耐久度影响
enableWrongToolPenalty = false          # 关闭错误工具惩罚
enableToolDamageOnInvalidMining = false # 关闭工具损坏
# 保留信息显示功能，但不影响游戏机制
```

### 🔍 配置文件重载
- 修改配置后需要重启游戏才能生效
- 建议在测试世界中调试配置参数
- 可以通过Jade显示的信息实时观察配置效果

## 🎮 游戏体验

### 策略性挖掘
- 需要为不同材料选择合适的工具
- 工具磨损会影响挖掘效率
- 计划性地管理工具耐久度

### 平衡性设计
- 剑类工具挖掘硬度减少50%，更适合战斗
- 渐进式惩罚曲线，避免突然的性能断崖
- 三级挖掘系统提供缓冲机制

### 信息透明化
- 通过Jade mod实时显示所有相关信息
- 清晰的状态指示和颜色编码
- 详细的工具效率分析

## 🔧 技术特性

- **Minecraft版本**: 1.20.1
- **Forge版本**: 47.4.4+
- **可选依赖**: Jade (用于信息显示)
- **模组兼容性**: 自动识别模组工具和方块
- **性能优化**: 高效的事件处理和缓存机制

## 📦 安装方法

1. 确保已安装Minecraft Forge 47.4.4或更高版本
2. 下载BetterExcavate mod文件
3. 将mod文件放入`.minecraft/mods`文件夹
4. (推荐) 安装Jade mod以获得完整的信息显示功能
5. 启动游戏并享受全新的挖掘体验！

## 🛠️ 开发信息

- **开发者**: Goldgom
- **许可证**: LGPL-3.0
- **版本**: 1.0.0-beta
- **仓库**: BetterExcavate

## 🤝 贡献与反馈

欢迎提出建议、报告bug或贡献代码！这个mod旨在为Minecraft带来更加平衡和策略性的挖掘体验。

---

*让挖掘变得更有策略性！*
