package org.goldgomtech.betterexcavate.integration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.goldgomtech.betterexcavate.Config;

/**
 * Jade集成：在方块信息面板中显示挖掘硬度和工具信息
 * 注意：这个类需要Jade mod才能正常工作
 * 如果没有安装Jade，这个类不会被加载
 * 
 * 插件注册通过JadePlugin类的@WailaPlugin注解自动处理
 */
public class JadeBlockInfoProvider {
    
    /**
     * 获取方块和工具的显示信息
     * 这可以被其他信息显示系统使用
     */
    public static Component[] getBlockToolInfo(BlockState blockState, Player player, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        java.util.List<Component> info = new java.util.ArrayList<>();
        
        // 获取方块硬度
        float blockHardness = blockState.getDestroySpeed(level, pos);
        
        // 如果方块硬度为-1（如基岩），显示特殊信息
        if (blockHardness < 0) {
            info.add(Component.translatable("betterexcavate.jade.unbreakable"));
            return info.toArray(new Component[0]);
        }
        
        // 显示方块硬度
        info.add(Component.translatable("betterexcavate.jade.block_hardness", String.format("%.2f", blockHardness)));
        
        // 获取玩家手持工具信息
        ItemStack heldItem = player.getMainHandItem();
        
        double toolHardness;
        String toolName = "hand";
        
        if (heldItem.isEmpty()) {
            toolHardness = Config.defaultHardness;
        } else {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(heldItem.getItem());
            if (itemId == null) {
                toolHardness = Config.defaultHardness;
            } else {
                toolName = itemId.toString();
                toolHardness = Config.toolHardnessMap.getOrDefault(toolName, Config.defaultHardness);
            }
        }
        
        // 应用耐久度硬度惩罚
        double effectiveToolHardness = toolHardness;
        if (Config.enableDurabilityHardnessPenalty && !heldItem.isEmpty()) {
            double durabilityHardnessMultiplier = Config.calculateDurabilityPenalty(heldItem, Config.maxDurabilityHardnessPenalty);
            effectiveToolHardness = toolHardness * durabilityHardnessMultiplier;
        }
        
        // 显示工具硬度
        if (heldItem.isEmpty()) {
            info.add(Component.translatable("betterexcavate.jade.tool_hardness_hand", String.format("%.2f", effectiveToolHardness)));
        } else {
            info.add(Component.translatable("betterexcavate.jade.tool_hardness_tool", 
                heldItem.getDisplayName().getString(), String.format("%.2f", effectiveToolHardness)));
            
            // 检查工具类型是否正确
            if (Config.enableWrongToolPenalty) {
                boolean isCorrectTool = Config.isCorrectToolType(heldItem, blockState);
                if (isCorrectTool) {
                    info.add(Component.translatable("betterexcavate.jade.tool_correct"));
                } else {
                    info.add(Component.translatable("betterexcavate.jade.tool_wrong", 
                        String.format("%.0f", Config.wrongToolSpeedPenalty * 100)));
                }
            }
            
            // 如果有耐久度惩罚，显示原始硬度
            if (effectiveToolHardness != toolHardness) {
                info.add(Component.translatable("betterexcavate.jade.tool_hardness_original", String.format("%.2f", toolHardness)));
                
                // 显示耐久度信息
                if (heldItem.isDamageableItem()) {
                    int durability = heldItem.getMaxDamage() - heldItem.getDamageValue();
                    int maxDurability = heldItem.getMaxDamage();
                    double wearPercentage = (double) heldItem.getDamageValue() / heldItem.getMaxDamage() * 100;
                    
                    info.add(Component.translatable("betterexcavate.jade.tool_durability", 
                        durability, maxDurability, String.format("%.1f", wearPercentage)));
                }
            }
        }
        
        // 计算挖掘能力
        int miningMode = Config.getMiningMode(blockHardness, effectiveToolHardness);
        
        // 计算挖掘速度
        float miningSpeed = calculateMiningSpeed(player, blockState, heldItem, level, pos);
        
        // 显示挖掘速度
        if (miningSpeed > 0) {
            info.add(Component.translatable("betterexcavate.jade.mining_speed", String.format("%.2f", miningSpeed)));
        } else {
            info.add(Component.translatable("betterexcavate.jade.mining_speed_none"));
        }
        
        // 显示挖掘模式
        if (miningMode == 0) {
            // 无法挖掘
            info.add(Component.translatable("betterexcavate.jade.cannot_mine"));
        } else if (miningMode == 1) {
            // 正常挖掘，有掉落物
            double hardnessRatio = effectiveToolHardness / blockHardness;
            
            if (hardnessRatio >= Config.hardnessMultiplier) {
                info.add(Component.translatable("betterexcavate.jade.efficient_mining"));
            } else if (hardnessRatio >= Config.hardnessMultiplier * 0.5) {
                info.add(Component.translatable("betterexcavate.jade.normal_mining"));
            } else {
                info.add(Component.translatable("betterexcavate.jade.slow_mining"));
            }
        } else if (miningMode == 2) {
            // 缓慢挖掘，无掉落物
            info.add(Component.translatable("betterexcavate.jade.slow_mining_no_drops"));
        }
        
        // 显示周围方块影响（如果启用）
        if (Config.enableSurroundingBlocksModifier) {
            // 简化版本：只检查上下前后左右6个面
            int identicalBlocks = 0;
            
            // 检查6个面的相邻方块
            if (level.getBlockState(pos.above()).equals(blockState)) identicalBlocks++;
            if (level.getBlockState(pos.below()).equals(blockState)) identicalBlocks++;
            if (level.getBlockState(pos.north()).equals(blockState)) identicalBlocks++;
            if (level.getBlockState(pos.south()).equals(blockState)) identicalBlocks++;
            if (level.getBlockState(pos.east()).equals(blockState)) identicalBlocks++;
            if (level.getBlockState(pos.west()).equals(blockState)) identicalBlocks++;
            
            if (identicalBlocks > 0) {
                info.add(Component.translatable("betterexcavate.jade.surrounding_blocks", identicalBlocks));
            }
        }
        
        return info.toArray(new Component[0]);
    }
    
    /**
     * 计算玩家对指定方块的挖掘速度
     * 参考原版计算逻辑，包括工具效率、状态效果等
     */
    private static float calculateMiningSpeed(Player player, BlockState blockState, ItemStack tool, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        // 获取基础挖掘速度
        float destroySpeed = tool.getDestroySpeed(blockState);
        
        // 获取方块硬度和工具硬度信息
        float blockHardness = blockState.getDestroySpeed(level, pos);
        
        double toolHardness;
        if (tool.isEmpty()) {
            toolHardness = Config.defaultHardness;
        } else {
            net.minecraft.resources.ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tool.getItem());
            if (itemId == null) {
                toolHardness = Config.defaultHardness;
            } else {
                String toolName = itemId.toString();
                toolHardness = Config.toolHardnessMap.getOrDefault(toolName, Config.defaultHardness);
            }
        }
        
        // 应用耐久度硬度惩罚
        double effectiveToolHardness = toolHardness;
        if (Config.enableDurabilityHardnessPenalty && !tool.isEmpty()) {
            double durabilityHardnessMultiplier = Config.calculateDurabilityPenalty(tool, Config.maxDurabilityHardnessPenalty);
            effectiveToolHardness = toolHardness * durabilityHardnessMultiplier;
        }
        
        // 检查挖掘模式
        int miningMode = Config.getMiningMode(blockHardness, effectiveToolHardness);
        
        if (miningMode == 0) {
            return 0.0F; // 无法挖掘
        }
        
        if (destroySpeed > 1.0F) {
            // 有工具加速
            
            // 如果是缓慢挖掘模式，应用速度惩罚
            if (miningMode == 2) {
                destroySpeed *= (1.0f - (float)Config.slowMiningSpeedPenalty);
            }
            
            // 检查是否为正确工具类型，如果启用了错误工具惩罚
            if (Config.enableWrongToolPenalty) {
                boolean isCorrectTool = Config.isCorrectToolType(tool, blockState);
                if (!isCorrectTool) {
                    // 应用错误工具速度惩罚
                    destroySpeed *= (1.0f - (float)Config.wrongToolSpeedPenalty);
                }
            }
            
            // 应用耐久度速度惩罚
            if (Config.enableDurabilitySpeedPenalty && !tool.isEmpty()) {
                double durabilitySpeedMultiplier = Config.calculateDurabilityPenalty(tool, Config.maxDurabilitySpeedPenalty);
                destroySpeed *= (float)durabilitySpeedMultiplier;
            }
            
            // TODO: 这里可以添加状态效果的影响，如急迫、挖掘疲劳等
            // 但需要访问玩家的状态效果，在静态方法中比较复杂
            
            // 应用周围方块速度修正（如果启用）
            if (Config.enableSurroundingBlocksModifier) {
                float surroundingModifier = calculateSurroundingBlocksModifier(blockState, level, pos);
                destroySpeed *= surroundingModifier;
            }
        }
        
        // 应用在水中或不在地面上的惩罚
        if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) && !net.minecraft.world.item.enchantment.EnchantmentHelper.hasAquaAffinity(player)) {
            destroySpeed /= 5.0F;
        }
        
        if (!player.onGround()) {
            destroySpeed /= 5.0F;
        }
        
        return destroySpeed;
    }
    
    /**
     * 计算周围方块对挖掘速度的影响
     */
    private static float calculateSurroundingBlocksModifier(BlockState blockState, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        // 计算周围相同方块的数量
        int identicalBlocks = 0;
        int totalChecked = 0;
        
        // 检查3x3x3区域内的方块
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue; // 跳过中心方块
                    
                    net.minecraft.core.BlockPos checkPos = pos.offset(dx, dy, dz);
                    if (level.getBlockState(checkPos).equals(blockState)) {
                        identicalBlocks++;
                    }
                    totalChecked++;
                }
            }
        }
        
        // 计算相同方块的比例
        float identicalRatio = (float) identicalBlocks / totalChecked;
        
        // 根据配置的曲线类型计算速度修正
        if ("linear".equals(Config.speedCurveType)) {
            // 线性插值
            return (float) (Config.maxSpeedMultiplier - (Config.maxSpeedMultiplier - Config.minSpeedMultiplier) * identicalRatio);
        } else {
            // 对数曲线（默认）
            if (identicalRatio <= 0.001f) {
                return (float) Config.maxSpeedMultiplier;
            }
            
            // 使用对数函数，identicalRatio越大，速度越慢
            double logValue = Math.log(identicalRatio * Math.E + 1) / Math.E;
            double speedMultiplier = Config.maxSpeedMultiplier - (Config.maxSpeedMultiplier - Config.minSpeedMultiplier) * logValue;
            
            return (float) Math.max(Config.minSpeedMultiplier, Math.min(speedMultiplier, Config.maxSpeedMultiplier));
        }
    }
}
