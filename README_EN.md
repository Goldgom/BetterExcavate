# BetterExcavate

A comprehensive mining mechanics overhaul mod that introduces a realistic tool-based mining system for Minecraft 1.20.1.

## 🌟 Core Features

### 🔨 Tool Hardness System
- **Realistic Mining Mechanics**: Each tool has unique hardness values determining which blocks can be mined
- **Intelligent Tool Matching**: Automatically detects hardness attributes of vanilla and modded tools
- **Configurable Hardness Values**: Support for custom hardness parameters for each tool

### ⚖️ Three-Tier Mining Modes
1. **Normal Mining** (Green) - Tool hardness sufficient, normal item and experience drops
2. **Slow Mining** (Orange) - Tool hardness insufficient but within tolerance, slow mining with no drops
3. **Cannot Mine** (Red) - Tool hardness severely insufficient, completely unable to break blocks

### 🛠️ Tool Type Validation
- **Smart Tool Detection**: Automatically recognizes appropriate blocks for pickaxes, axes, shovels, hoes, and swords
- **Wrong Tool Penalty**: 50% mining speed reduction when using incorrect tool types
- **Tool Compatibility Hints**: Real-time display of tool suitability for current blocks

### 💎 Durability Impact System
- **Progressive Hardness Decay**: Tool hardness decreases as tools wear, using natural logarithmic curves
- **Intelligent Damage Mechanics**: Tools take continuous damage when mining overly hard blocks
- **Durability Visualization**: Intuitive display of remaining tool durability and wear percentage

### 🔍 Jade Integration
- **Real-time Information Display**: Hover to show block hardness, tool hardness, and mining speed
- **Mining Mode Indicators**: Clear indication of current mining status and possibilities
- **Tool Efficiency Analysis**: Display tool type correctness and speed penalty information
- **Durability Monitoring**: Real-time tool wear condition display

### 🎯 Advanced Features
- **Surrounding Block Effects**: Mining speed affected when identical blocks cluster together
- **Precise State Tracking**: Smart detection of player mining state, preventing state loss
- **Multi-dimensional Support**: Independent mining state management for each dimension
- **Performance Optimization**: Efficient event handling and state management

## ⚙️ Configuration Guide

Configuration file location: `.minecraft/config/betterexcavate-common.toml`

### 🔧 Tool Hardness Configuration
```toml
# Tool hardness configuration in format 'toolname:hardness'
toolHardness = [
    "minecraft:wooden_pickaxe:2.1",     # Wooden Pickaxe - Can mine dirt, sand, soft blocks
    "minecraft:stone_pickaxe:3.5",      # Stone Pickaxe - Can mine coal ore, iron ore
    "minecraft:iron_pickaxe:5.0",       # Iron Pickaxe - Can mine gold ore, lapis ore
    "minecraft:diamond_pickaxe:60.0",   # Diamond Pickaxe - Can mine diamond ore, obsidian
    "minecraft:netherite_pickaxe:110.0" # Netherite Pickaxe - Can mine all blocks
]

# Default hardness for tools not specified (Range: 0.1 ~ 100.0)
defaultHardness = 1.0                   # Recommended: 1.0 (hand mining hardness)

# Auto-detect tool hardness based on vanilla tiers
autoDetectToolHardness = true           # Recommended: true (auto-adapt modded tools)
```

### ⚖️ Basic Mining Mechanics
```toml
# Hardness multiplier determining maximum mineable hardness (Range: 0.1 ~ 10.0)
hardnessMultiplier = 1.0                # Tool hardness × this value = max mineable hardness
                                        # Recommended: 1.0 (balanced mining experience)

# Enable hardness-based drop control
enableDropControl = true                # Recommended: true (core functionality)
```

### 🛡️ Slow Mining System
```toml
# Enable slow mining mode without drops
enableSlowMiningWithoutDrops = true     # Recommended: true (provides buffer mechanism)

# Slow mining hardness multiplier (Range: 1.0 ~ 10.0)
slowMiningHardnessMultiplier = 2.0      # Tool hardness × this = max slow mineable hardness
                                        # Recommended: 2.0 (reasonable tolerance range)

# Slow mining speed penalty (Range: 0.0 ~ 1.0)
slowMiningSpeedPenalty = 0.8            # 0.8 = 80% speed reduction
                                        # Recommended: 0.8 (noticeable speed difference)
```

### 💎 Durability Impact System
```toml
# Enable durability speed penalty
enableDurabilitySpeedPenalty = true     # Recommended: true (realistic experience)

# Enable durability hardness penalty
enableDurabilityHardnessPenalty = true  # Recommended: true (core mechanism)

# Maximum speed penalty for fully worn tools (Range: 0.0 ~ 1.0)
maxDurabilitySpeedPenalty = 0.5         # 0.5 = up to 50% speed reduction
                                        # Recommended: 0.5 (moderate penalty)

# Maximum hardness penalty for fully worn tools (Range: 0.0 ~ 1.0)
maxDurabilityHardnessPenalty = 0.32     # 0.32 = up to 32% hardness reduction
                                        # Recommended: 0.32 (progressive decay)

# Durability penalty curve type
durabilityPenaltyCurve = "quadratic"    # Options: linear, quadratic, exponential
                                        # Recommended: quadratic (smooth progressive effect)
```

### 🛠️ Tool Type Validation System
```toml
# Enable wrong tool type penalty
enableWrongToolPenalty = true           # Recommended: true (encourage proper tool usage)

# Wrong tool type speed penalty (Range: 0.0 ~ 1.0)
wrongToolSpeedPenalty = 0.5             # 0.5 = 50% speed reduction
                                        # Recommended: 0.5 (significant but fair penalty)
```

### ⚙️ Tool Damage Mechanics
```toml
# Enable tool damage during invalid mining
enableToolDamageOnInvalidMining = true  # Recommended: true (prevent brute force mining)

# Tool damage hardness threshold (Range: 0.01 ~ 1.0)
toolDamageHardnessThreshold = 0.1       # Tools take damage when hardness ratio below this
                                        # Recommended: 0.1 (only severe mismatches cause damage)
```

### 🌍 Environmental Effects System
```toml
# Enable surrounding blocks modifier for mining speed
enableSurroundingBlocksModifier = true  # Recommended: true (adds strategic depth)

# Minimum speed multiplier when surrounded by identical blocks (Range: 0.001 ~ 1.0)
minSpeedMultiplier = 0.05               # 0.05 = up to 95% speed reduction
                                        # Recommended: 0.05 (significant clustering effect)

# Maximum speed multiplier when no identical blocks around (Range: 1.0 ~ 5.0)
maxSpeedMultiplier = 1.0                # 1.0 = no speed bonus
                                        # Recommended: 1.0 (maintain balance)

# Speed curve type
speedCurveType = "logarithmic"          # Options: linear, logarithmic
                                        # Recommended: logarithmic (more natural transition)
```

### 📋 Configuration Recommendations

#### 🎯 Beginner-Friendly Configuration
```toml
hardnessMultiplier = 1.2                # Slightly lenient mining restrictions
maxDurabilityHardnessPenalty = 0.2      # Lighter durability penalty
wrongToolSpeedPenalty = 0.3             # Lighter wrong tool penalty
enableToolDamageOnInvalidMining = false # Disable tool damage
```

#### ⚔️ Hard Mode Configuration
```toml
hardnessMultiplier = 0.8                # Stricter mining restrictions
maxDurabilityHardnessPenalty = 0.5      # Heavier durability penalty
wrongToolSpeedPenalty = 0.7             # Heavier wrong tool penalty
toolDamageHardnessThreshold = 0.2       # Easier to trigger tool damage
minSpeedMultiplier = 0.02               # Stronger clustering effect
```

#### 🔄 Display-Only Mode Configuration
```toml
enableDropControl = false               # Disable drop control
enableDurabilityHardnessPenalty = false # Disable durability effects
enableWrongToolPenalty = false          # Disable wrong tool penalty
enableToolDamageOnInvalidMining = false # Disable tool damage
# Keep information display but don't affect gameplay mechanics
```

### 🔍 Configuration Reload
- Game restart required after modifying configuration
- Recommended to test configuration parameters in a test world
- Use Jade display information to observe configuration effects in real-time

## 🎮 Gameplay Experience

### Strategic Mining
- Need to select appropriate tools for different materials
- Tool wear affects mining efficiency
- Plan tool durability management strategically

### Balanced Design
- Sword tools have 50% reduced mining hardness, better suited for combat
- Progressive penalty curves avoid sudden performance drops
- Three-tier mining system provides buffer mechanisms

### Information Transparency
- Real-time display of all relevant information through Jade mod
- Clear status indicators and color coding
- Detailed tool efficiency analysis

## 🔧 Technical Features

- **Minecraft Version**: 1.20.1
- **Forge Version**: 47.4.4+
- **Optional Dependencies**: Jade (for information display)
- **Mod Compatibility**: Automatically recognizes modded tools and blocks
- **Performance Optimization**: Efficient event handling and caching mechanisms

## 📦 Installation

1. Ensure Minecraft Forge 47.4.4 or higher is installed
2. Download the BetterExcavate mod file
3. Place the mod file in `.minecraft/mods` folder
4. (Recommended) Install Jade mod for complete information display functionality
5. Launch the game and enjoy the new mining experience!

## 🛠️ Development Information

- **Developer**: Goldgom
- **License**: LGPL-3.0
- **Version**: 1.0.0-beta
- **Repository**: BetterExcavate

## 🤝 Contributing & Feedback

Suggestions, bug reports, and code contributions are welcome! This mod aims to bring a more balanced and strategic mining experience to Minecraft.

---

*Make mining more strategic!*
