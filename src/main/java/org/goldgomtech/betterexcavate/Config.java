package org.goldgomtech.betterexcavate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mod.EventBusSubscriber(modid = BetterExcavate.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Tool hardness configuration
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TOOL_HARDNESS_CONFIG = BUILDER
            .comment("Tool hardness configuration in format 'toolname:hardness'. Higher hardness = can mine harder blocks.")
            .defineListAllowEmpty("toolHardness", List.of(
                    "minecraft:wooden_pickaxe:2.1",
                    "minecraft:stone_pickaxe:3.5",
                    "minecraft:iron_pickaxe:5.0",
                    "minecraft:golden_pickaxe:2.1",
                    "minecraft:diamond_pickaxe:60.0",
                    "minecraft:netherite_pickaxe:110.0",
                    "minecraft:wooden_axe:2.1",
                    "minecraft:stone_axe:3.5",
                    "minecraft:iron_axe:5.0",
                    "minecraft:golden_axe:2.1",
                    "minecraft:diamond_axe:60.0",
                    "minecraft:netherite_axe:110.0",
                    "minecraft:wooden_shovel:2.1",
                    "minecraft:stone_shovel:3.5",
                    "minecraft:iron_shovel:5.0",
                    "minecraft:golden_shovel:2.1",
                    "minecraft:diamond_shovel:60.0",
                    "minecraft:netherite_shovel:110.0",
                    "minecraft:wooden_hoe:2.1",
                    "minecraft:stone_hoe:3.5",
                    "minecraft:iron_hoe:5.0",
                    "minecraft:golden_hoe:2.1",
                    "minecraft:diamond_hoe:60.0",
                    "minecraft:netherite_hoe:110.0",
                    "minecraft:wooden_sword:1.0",
                    "minecraft:stone_sword:1.7",
                    "minecraft:iron_sword:2.5",
                    "minecraft:golden_sword:1.0",
                    "minecraft:diamond_sword:30.0",
                    "minecraft:netherite_sword:55.0"
            ), Config::validateToolConfig);

    private static final ForgeConfigSpec.DoubleValue DEFAULT_HARDNESS = BUILDER
            .comment("Default hardness for tools not specified in the config")
            .defineInRange("defaultHardness", 1.0, 0.1, 100.0);

    private static final ForgeConfigSpec.DoubleValue HARDNESS_MULTIPLIER = BUILDER
            .comment("Multiplier for tool hardness to determine maximum block hardness that can be mined. " +
                    "If block hardness > tool hardness * this multiplier, the block cannot be mined.")
            .defineInRange("hardnessMultiplier", 1.0, 0.1, 10.0);

    private static final ForgeConfigSpec.BooleanValue ENABLE_DROP_CONTROL = BUILDER
            .comment("Enable drop control based on tool hardness. If disabled, blocks will always drop items regardless of tool hardness.")
            .define("enableDropControl", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_TOOL_DAMAGE_ON_INVALID_MINING = BUILDER
            .comment("Enable tool damage when trying to mine blocks that are too hard for the tool. Tool will lose 1 durability per second.")
            .define("enableToolDamageOnInvalidMining", true);

    private static final ForgeConfigSpec.DoubleValue TOOL_DAMAGE_HARDNESS_THRESHOLD = BUILDER
            .comment("Tool hardness ratio threshold below which tools will take damage. Lower values mean tools take damage only when severely inadequate.")
            .defineInRange("toolDamageHardnessThreshold", 0.1, 0.01, 1.0);

    // Surrounding blocks speed modifier configuration
    private static final ForgeConfigSpec.BooleanValue ENABLE_SURROUNDING_BLOCKS_MODIFIER = BUILDER
            .comment("Enable mining speed modification based on surrounding identical blocks.")
            .define("enableSurroundingBlocksModifier", true);

    private static final ForgeConfigSpec.DoubleValue MIN_SPEED_MULTIPLIER = BUILDER
            .comment("Minimum speed multiplier when surrounded by many identical blocks.")
            .defineInRange("minSpeedMultiplier", 0.05, 0.001, 1.0);

    private static final ForgeConfigSpec.DoubleValue MAX_SPEED_MULTIPLIER = BUILDER
            .comment("Maximum speed multiplier when no identical blocks are around.")
            .defineInRange("maxSpeedMultiplier", 1.0, 1.0, 5.0);

    private static final ForgeConfigSpec.ConfigValue<String> SPEED_CURVE_TYPE = BUILDER
            .comment("Speed curve type: 'linear' or 'logarithmic'")
            .define("speedCurveType", "logarithmic", Config::validateCurveType);

    private static final ForgeConfigSpec.BooleanValue AUTO_DETECT_TOOL_HARDNESS = BUILDER
            .comment("Automatically detect and assign hardness values to tools based on their vanilla tier levels.")
            .define("autoDetectToolHardness", true);

    // Tool durability wear penalties
    private static final ForgeConfigSpec.BooleanValue ENABLE_DURABILITY_SPEED_PENALTY = BUILDER
            .comment("Enable mining speed penalty based on tool durability wear. More damaged tools mine slower.")
            .define("enableDurabilitySpeedPenalty", true);

    private static final ForgeConfigSpec.BooleanValue ENABLE_DURABILITY_HARDNESS_PENALTY = BUILDER
            .comment("Enable tool hardness penalty based on tool durability wear. More damaged tools can't mine as hard blocks.")
            .define("enableDurabilityHardnessPenalty", true);

    private static final ForgeConfigSpec.DoubleValue MAX_DURABILITY_SPEED_PENALTY = BUILDER
            .comment("Maximum speed penalty for completely worn tools (0.0 = no penalty, 0.9 = 90% speed reduction)")
            .defineInRange("maxDurabilitySpeedPenalty", 0.5, 0.0, 1.0);

    private static final ForgeConfigSpec.DoubleValue MAX_DURABILITY_HARDNESS_PENALTY = BUILDER
            .comment("Maximum hardness penalty for completely worn tools (0.0 = no penalty, 0.9 = 90% hardness reduction)")
            .defineInRange("maxDurabilityHardnessPenalty", 0.32, 0.0, 1.0);

    private static final ForgeConfigSpec.ConfigValue<String> DURABILITY_PENALTY_CURVE = BUILDER
            .comment("Durability penalty curve type: 'linear', 'quadratic', or 'exponential'")
            .define("durabilityPenaltyCurve", "quadratic", Config::validatePenaltyCurve);

    // Tool type correctness penalty
    private static final ForgeConfigSpec.BooleanValue ENABLE_WRONG_TOOL_PENALTY = BUILDER
            .comment("Enable mining speed penalty when using wrong tool type for a block (e.g., using pickaxe on dirt)")
            .define("enableWrongToolPenalty", true);

    private static final ForgeConfigSpec.DoubleValue WRONG_TOOL_SPEED_PENALTY = BUILDER
            .comment("Speed penalty when using wrong tool type (0.0 = no penalty, 0.5 = 50% speed reduction)")
            .defineInRange("wrongToolSpeedPenalty", 0.5, 0.0, 1.0);

    // Slow mining without drops feature
    private static final ForgeConfigSpec.BooleanValue ENABLE_SLOW_MINING_WITHOUT_DROPS = BUILDER
            .comment("Enable slow mining without drops when tool hardness is insufficient but within tolerance range")
            .define("enableSlowMiningWithoutDrops", true);

    private static final ForgeConfigSpec.DoubleValue SLOW_MINING_HARDNESS_MULTIPLIER = BUILDER
            .comment("Hardness multiplier for slow mining without drops. Blocks with hardness between tool_hardness * hardnessMultiplier and tool_hardness * slowMiningHardnessMultiplier can be broken slowly without drops.")
            .defineInRange("slowMiningHardnessMultiplier", 2.0, 1.0, 10.0);

    private static final ForgeConfigSpec.DoubleValue SLOW_MINING_SPEED_PENALTY = BUILDER
            .comment("Speed penalty for slow mining without drops (0.0 = no penalty, 0.9 = 90% speed reduction)")
            .defineInRange("slowMiningSpeedPenalty", 0.8, 0.0, 1.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Tool hardness values
    public static java.util.Map<String, Double> toolHardnessMap;
    public static double defaultHardness;
    public static double hardnessMultiplier;
    public static boolean enableDropControl;
    public static boolean enableToolDamageOnInvalidMining;
    public static double toolDamageHardnessThreshold;

    // Surrounding blocks speed modifier
    public static boolean enableSurroundingBlocksModifier;
    public static double minSpeedMultiplier;
    public static double maxSpeedMultiplier;
    public static String speedCurveType;
    public static boolean autoDetectToolHardness;

    // Tool durability wear penalties
    public static boolean enableDurabilitySpeedPenalty;
    public static boolean enableDurabilityHardnessPenalty;
    public static double maxDurabilitySpeedPenalty;
    public static double maxDurabilityHardnessPenalty;
    public static String durabilityPenaltyCurve;

    // Tool type correctness penalty
    public static boolean enableWrongToolPenalty;
    public static double wrongToolSpeedPenalty;

    // Slow mining without drops feature
    public static boolean enableSlowMiningWithoutDrops;
    public static double slowMiningHardnessMultiplier;
    public static double slowMiningSpeedPenalty;

    private static boolean validateToolConfig(final Object obj)
    {
        if (!(obj instanceof String configLine)) {
            return false;
        }
        String[] parts = configLine.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            Double.parseDouble(parts[2]);
            return ForgeRegistries.ITEMS.containsKey(new ResourceLocation(parts[0] + ":" + parts[1]));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean validateCurveType(final Object obj)
    {
        if (!(obj instanceof String curveType)) {
            return false;
        }
        return "linear".equals(curveType) || "logarithmic".equals(curveType);
    }

    private static boolean validatePenaltyCurve(final Object obj)
    {
        if (!(obj instanceof String curveType)) {
            return false;
        }
        return "linear".equals(curveType) || "quadratic".equals(curveType) || "exponential".equals(curveType);
    }
    
    /**
     * 自动检测并更新工具硬度配置
     */
    private static void autoDetectAndUpdateToolHardness() {
        Logger logger = LoggerFactory.getLogger("BetterExcavate");
        logger.info("[BetterExcavate] Starting automatic tool hardness detection...");
        
        java.util.List<String> newToolConfigs = new java.util.ArrayList<>(TOOL_HARDNESS_CONFIG.get());
        int addedTools = 0;
        
        // 遍历所有注册的物品
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null) continue;
            
            String toolName = itemId.toString();
            
            // 如果工具已在配置中，跳过
            if (toolHardnessMap.containsKey(toolName)) continue;
            
            double hardness = -1;
            
            // 检查是否为分层工具（TieredItem）
            if (item instanceof TieredItem tieredItem) {
                boolean isSword = toolName.toLowerCase().contains("sword");
                hardness = getTierHardness(tieredItem.getTier(), isSword);
            } else {
                // 检查是否为其他类型的挖掘工具
                hardness = getToolHardnessByBehavior(item, toolName);
            }
            
            if (hardness > 0) {
                String configEntry = toolName + ":" + hardness;
                newToolConfigs.add(configEntry);
                toolHardnessMap.put(toolName, hardness);
                addedTools++;
                
                logger.info("[BetterExcavate] Added tool: {} with hardness: {}", toolName, hardness);
            }
        }
        
        if (addedTools > 0) {
            logger.info("[BetterExcavate] Auto-detected {} new tools and added to configuration", addedTools);
            logger.info("[BetterExcavate] ================== NEW TOOL CONFIGURATIONS ==================");
            logger.info("[BetterExcavate] Add the following lines to your config file under 'toolHardness':");
            
            // 只输出新添加的工具配置
            for (String config : newToolConfigs) {
                if (!TOOL_HARDNESS_CONFIG.get().contains(config)) {
                    logger.info("[BetterExcavate] \"{}\"", config);
                }
            }
            logger.info("[BetterExcavate] ============================================================");
        } else {
            logger.info("[BetterExcavate] No new tools detected, all known tools already configured");
        }
    }
    
    /**
     * 根据工具等级获取对应的硬度值
     */
    private static double getTierHardness(net.minecraft.world.item.Tier tier) {
        return getTierHardness(tier, false);
    }
    
    /**
     * 根据工具等级获取对应的硬度值
     * @param tier 工具等级
     * @param isSword 是否为剑类武器
     */
    private static double getTierHardness(net.minecraft.world.item.Tier tier, boolean isSword) {
        double baseHardness;
        
        // 根据原版工具等级分配基础硬度值
        if (tier == Tiers.WOOD) {
            baseHardness = 2.1;  // 木制工具
        } else if (tier == Tiers.STONE) {
            baseHardness = 3.5;  // 石制工具
        } else if (tier == Tiers.IRON) {
            baseHardness = 5.0;  // 铁制工具
        } else if (tier == Tiers.GOLD) {
            baseHardness = 2.1;  // 金工具（等级与木制相同）
        } else if (tier == Tiers.DIAMOND) {
            baseHardness = 60.0; // 钻石工具（可以挖掘黑曜石 硬度50）
        } else if (tier == Tiers.NETHERITE) {
            baseHardness = 110.0; // 下界合金工具
        } else {
            // 对于模组工具，根据其等级进行计算
            int level = tier.getLevel();
            if (level <= 0) {
                baseHardness = 2.1; // 木制等级
            } else if (level == 1) {
                baseHardness = 3.5; // 石制等级
            } else if (level == 2) {
                baseHardness = 5.0; // 铁制等级
            } else if (level == 3) {
                baseHardness = 60.0; // 钻石等级
            } else if (level == 4) {
                baseHardness = 110.0; // 下界合金等级
            } else {
                // 超过下界合金等级，每级增加100
                baseHardness = 110.0 + (level - 4) * 100.0;
            }
        }
        
        // 如果是剑类武器，硬度降低50%
        if (isSword) {
            baseHardness = baseHardness * 0.5;
        }
        
        return baseHardness;
    }

    /**
     * 根据工具行为和名称推测工具硬度
     */
    private static double getToolHardnessByBehavior(Item item, String toolName) {
        // 创建物品堆栈用于测试
        ItemStack stack = new ItemStack(item);
        
        // 测试对不同方块的挖掘速度来推测工具等级
        float stoneSpeed = stack.getDestroySpeed(Blocks.STONE.defaultBlockState());
        float ironSpeed = stack.getDestroySpeed(Blocks.IRON_ORE.defaultBlockState());
        float diamondSpeed = stack.getDestroySpeed(Blocks.DIAMOND_ORE.defaultBlockState());
        float obsidianSpeed = stack.getDestroySpeed(Blocks.OBSIDIAN.defaultBlockState());
        
        // 如果对石头有效果，认为是挖掘工具
        if (stoneSpeed > 1.0f) {
            // 根据对不同材料的挖掘效果推测等级
            if (obsidianSpeed > 1.0f) {
                return 60.0; // 钻石级别或更高（可以挖掘黑曜石）
            } else if (diamondSpeed > 1.0f) {
                return 5.0;  // 铁级别
            } else if (ironSpeed > 1.0f) {
                return 3.5;  // 石级别
            } else {
                return 2.1;  // 木级别
            }
        }
        
        // 根据工具名称推测（备用方案）
        String lowerName = toolName.toLowerCase();
        if (lowerName.contains("sword")) {
            // 剑类工具挖掘硬度降低50%
            if (lowerName.contains("wood")) return 1.0;
            if (lowerName.contains("stone")) return 1.7;
            if (lowerName.contains("iron")) return 2.5;
            if (lowerName.contains("gold")) return 1.0;
            if (lowerName.contains("diamond")) return 30.0;
            if (lowerName.contains("netherite")) return 55.0;
        } else {
            // 其他工具保持原有硬度
            if (lowerName.contains("wood")) return 2.1;
            if (lowerName.contains("stone")) return 3.5;
            if (lowerName.contains("iron")) return 5.0;
            if (lowerName.contains("gold")) return 2.1;
            if (lowerName.contains("diamond")) return 60.0;
            if (lowerName.contains("netherite")) return 110.0;
        }
        
        // 如果包含挖掘相关词汇，给予默认硬度
        if (lowerName.contains("pickaxe") || lowerName.contains("axe") || 
            lowerName.contains("shovel") || lowerName.contains("hoe") || 
            lowerName.contains("sword") || lowerName.contains("tool")) {
            return defaultHardness;
        }
        
        return -1; // 不是挖掘工具
    }

    /**
     * 计算工具耐久磨损惩罚系数
     * @param itemStack 工具物品堆栈
     * @param maxPenalty 最大惩罚值 (0.0 到 1.0)
     * @return 惩罚系数 (0.0 = 最大惩罚, 1.0 = 无惩罚)
     */
    public static double calculateDurabilityPenalty(ItemStack itemStack, double maxPenalty) {
        if (itemStack.isEmpty() || !itemStack.isDamageableItem()) {
            return 1.0; // 无惩罚
        }

        // 计算磨损百分比 (0.0 = 全新, 1.0 = 完全磨损)
        double wearPercentage = (double) itemStack.getDamageValue() / itemStack.getMaxDamage();
        
        // 根据配置的曲线类型计算惩罚
        double penaltyFactor;
        switch (durabilityPenaltyCurve) {
            case "linear":
                // 线性惩罚：磨损与惩罚成正比
                penaltyFactor = wearPercentage;
                break;
            case "quadratic":
                // 二次惩罚：早期惩罚较小，后期急剧增加
                penaltyFactor = wearPercentage * wearPercentage;
                break;
            case "exponential":
                // 指数惩罚：使用自然对数函数，前50%惩罚因子控制在0.2以内
                // 使用 f(x) = ln(1 + 0.65x) / ln(1.65) 的函数，确保50%磨损时惩罚因子为0.2
                if (wearPercentage == 0) {
                    penaltyFactor = 0;
                } else {
                    penaltyFactor = Math.log(1 + 0.65 * wearPercentage) / Math.log(1.65);
                }
                break;
            default:
                penaltyFactor = wearPercentage;
                break;
        }
        
        // 应用最大惩罚限制并返回乘数 (1.0 - 惩罚 = 剩余效果)
        double actualPenalty = penaltyFactor * maxPenalty;
        return 1.0 - actualPenalty;
    }

    /**
     * 检查工具类型是否适合挖掘指定方块
     * @param itemStack 工具物品堆栈
     * @param blockState 方块状态
     * @return true如果工具类型正确或方块没有特定工具要求，false如果使用了错误的工具类型
     */
    public static boolean isCorrectToolType(ItemStack itemStack, net.minecraft.world.level.block.state.BlockState blockState) {
        if (itemStack.isEmpty()) {
            return true; // 空手挖掘不算错误工具
        }

        // 获取方块和工具的信息
        net.minecraft.world.level.block.Block block = blockState.getBlock();
        Item tool = itemStack.getItem();
        String toolName = tool.toString().toLowerCase();

        // 首先检查方块是否有特定的工具要求
        boolean blockHasToolRequirement = hasSpecificToolRequirement(block);
        
        // 如果方块没有特定工具要求，任何工具都是正确的
        if (!blockHasToolRequirement) {
            return true;
        }

        // 检查是否为正确的工具类型
        // 镐子适合挖掘石头、矿物、金属类方块
        if (toolName.contains("pickaxe")) {
            return isPickaxeBlock(block);
        }
        // 斧头适合挖掘木质方块
        else if (toolName.contains("axe")) {
            return isAxeBlock(block);
        }
        // 铲子适合挖掘土、沙、雪等软质方块
        else if (toolName.contains("shovel")) {
            return isShovelBlock(block);
        }
        // 锄头适合挖掘农作物相关方块
        else if (toolName.contains("hoe")) {
            return isHoeBlock(block);
        }
        // 剑不是专门的挖掘工具，但可以快速破坏植物
        else if (toolName.contains("sword")) {
            return isSwordBlock(block);
        }
        
        // 对于其他工具或非工具物品，如果方块有特定要求但工具不匹配，则为错误
        return false;
    }

    /**
     * 检查方块是否有特定的工具挖掘要求
     * @param block 方块
     * @return true如果方块有特定工具要求，false如果任何工具都可以
     */
    private static boolean hasSpecificToolRequirement(net.minecraft.world.level.block.Block block) {
        // 检查是否为有特定工具要求的方块
        return isPickaxeBlock(block) || isAxeBlock(block) || isShovelBlock(block) || 
               isHoeBlock(block) || isSwordBlock(block);
    }

    /**
     * 检查方块是否适合用镐子挖掘
     */
    private static boolean isPickaxeBlock(net.minecraft.world.level.block.Block block) {
        // 石头、矿物、金属、混凝土等硬质方块
        return block == Blocks.STONE || block == Blocks.COBBLESTONE || block == Blocks.DEEPSLATE ||
               block == Blocks.COAL_ORE || block == Blocks.IRON_ORE || block == Blocks.GOLD_ORE ||
               block == Blocks.DIAMOND_ORE || block == Blocks.EMERALD_ORE || block == Blocks.REDSTONE_ORE ||
               block == Blocks.LAPIS_ORE || block == Blocks.COPPER_ORE || block == Blocks.NETHER_QUARTZ_ORE ||
               block == Blocks.NETHER_GOLD_ORE || block == Blocks.ANCIENT_DEBRIS ||
               block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN ||
               block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK || block == Blocks.DIAMOND_BLOCK ||
               block == Blocks.EMERALD_BLOCK || block == Blocks.NETHERITE_BLOCK ||
               block == Blocks.ANDESITE || block == Blocks.GRANITE || block == Blocks.DIORITE ||
               block == Blocks.BLACKSTONE || block == Blocks.BASALT || block == Blocks.NETHERRACK ||
               block == Blocks.END_STONE || block == Blocks.PURPUR_BLOCK ||
               block.toString().toLowerCase().contains("ore") ||
               block.toString().toLowerCase().contains("stone") ||
               block.toString().toLowerCase().contains("concrete");
    }

    /**
     * 检查方块是否适合用斧头挖掘
     */
    private static boolean isAxeBlock(net.minecraft.world.level.block.Block block) {
        // 木质方块
        return block.toString().toLowerCase().contains("log") ||
               block.toString().toLowerCase().contains("wood") ||
               block.toString().toLowerCase().contains("plank") ||
               block == Blocks.CHEST || block == Blocks.CRAFTING_TABLE ||
               block == Blocks.BOOKSHELF || block == Blocks.LADDER ||
               block.toString().toLowerCase().contains("fence") ||
               block.toString().toLowerCase().contains("door") ||
               block.toString().toLowerCase().contains("trapdoor");
    }

    /**
     * 检查方块是否适合用铲子挖掘
     */
    private static boolean isShovelBlock(net.minecraft.world.level.block.Block block) {
        // 土、沙、雪等软质方块
        return block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.COARSE_DIRT ||
               block == Blocks.SAND || block == Blocks.RED_SAND || block == Blocks.GRAVEL ||
               block == Blocks.CLAY || block == Blocks.SNOW || block == Blocks.SNOW_BLOCK ||
               block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL ||
               block.toString().toLowerCase().contains("dirt") ||
               block.toString().toLowerCase().contains("sand") ||
               block.toString().toLowerCase().contains("snow");
    }

    /**
     * 检查方块是否适合用锄头挖掘
     */
    private static boolean isHoeBlock(net.minecraft.world.level.block.Block block) {
        // 农作物和叶子
        return block.toString().toLowerCase().contains("leaves") ||
               block.toString().toLowerCase().contains("crop") ||
               block == Blocks.HAY_BLOCK || block == Blocks.DRIED_KELP_BLOCK ||
               block == Blocks.TARGET || block == Blocks.SPONGE || block == Blocks.WET_SPONGE;
    }

    /**
     * 检查方块是否适合用剑挖掘
     */
    private static boolean isSwordBlock(net.minecraft.world.level.block.Block block) {
        // 植物、网、竹子等
        return block.toString().toLowerCase().contains("leaves") ||
               block.toString().toLowerCase().contains("plant") ||
               block.toString().toLowerCase().contains("flower") ||
               block.toString().toLowerCase().contains("grass") ||
               block.toString().toLowerCase().contains("vine") ||
               block == Blocks.COBWEB || block == Blocks.BAMBOO ||
               block == Blocks.SUGAR_CANE || block == Blocks.CACTUS ||
               block == Blocks.MELON || block == Blocks.PUMPKIN;
    }

    /**
     * 检查挖掘模式
     * @param blockHardness 方块硬度
     * @param effectiveToolHardness 有效工具硬度
     * @return 挖掘模式：0=无法挖掘，1=正常挖掘有掉落，2=缓慢挖掘无掉落
     */
    public static int getMiningMode(float blockHardness, double effectiveToolHardness) {
        if (blockHardness < 0) {
            return 0; // 不可破坏的方块
        }
        
        double maxMineableHardness = effectiveToolHardness * hardnessMultiplier;
        
        if (blockHardness <= maxMineableHardness) {
            return 1; // 正常挖掘，有掉落物
        }
        
        if (enableSlowMiningWithoutDrops) {
            double maxSlowMineableHardness = effectiveToolHardness * slowMiningHardnessMultiplier;
            if (blockHardness <= maxSlowMineableHardness) {
                return 2; // 缓慢挖掘，无掉落物
            }
        }
        
        return 0; // 无法挖掘
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        // Load tool hardness configuration
        toolHardnessMap = new java.util.HashMap<>();
        for (String configLine : TOOL_HARDNESS_CONFIG.get()) {
            String[] parts = configLine.split(":");
            if (parts.length == 3) {
                try {
                    String toolName = parts[0] + ":" + parts[1];
                    double hardness = Double.parseDouble(parts[2]);
                    toolHardnessMap.put(toolName, hardness);
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
        defaultHardness = DEFAULT_HARDNESS.get();
        hardnessMultiplier = HARDNESS_MULTIPLIER.get();
        enableDropControl = ENABLE_DROP_CONTROL.get();
        enableToolDamageOnInvalidMining = ENABLE_TOOL_DAMAGE_ON_INVALID_MINING.get();
        toolDamageHardnessThreshold = TOOL_DAMAGE_HARDNESS_THRESHOLD.get();
        enableSurroundingBlocksModifier = ENABLE_SURROUNDING_BLOCKS_MODIFIER.get();
        minSpeedMultiplier = MIN_SPEED_MULTIPLIER.get();
        maxSpeedMultiplier = MAX_SPEED_MULTIPLIER.get();
        speedCurveType = SPEED_CURVE_TYPE.get();
        autoDetectToolHardness = AUTO_DETECT_TOOL_HARDNESS.get();
        enableDurabilitySpeedPenalty = ENABLE_DURABILITY_SPEED_PENALTY.get();
        enableDurabilityHardnessPenalty = ENABLE_DURABILITY_HARDNESS_PENALTY.get();
        maxDurabilitySpeedPenalty = MAX_DURABILITY_SPEED_PENALTY.get();
        maxDurabilityHardnessPenalty = MAX_DURABILITY_HARDNESS_PENALTY.get();
        durabilityPenaltyCurve = DURABILITY_PENALTY_CURVE.get();
        enableWrongToolPenalty = ENABLE_WRONG_TOOL_PENALTY.get();
        wrongToolSpeedPenalty = WRONG_TOOL_SPEED_PENALTY.get();
        enableSlowMiningWithoutDrops = ENABLE_SLOW_MINING_WITHOUT_DROPS.get();
        slowMiningHardnessMultiplier = SLOW_MINING_HARDNESS_MULTIPLIER.get();
        slowMiningSpeedPenalty = SLOW_MINING_SPEED_PENALTY.get();
        
        // 自动检测工具硬度
        if (autoDetectToolHardness) {
            autoDetectAndUpdateToolHardness();
        }
    }
}
