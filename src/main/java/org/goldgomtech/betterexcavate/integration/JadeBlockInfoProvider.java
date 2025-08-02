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
     * 根据配置选择使用原版速度修正或自定义速度计算
     */
    private static float calculateMiningSpeed(Player player, BlockState blockState, ItemStack tool, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        // 获取方块硬度和工具硬度信息
        float blockHardness = blockState.getDestroySpeed(level, pos);
        
        if (blockHardness < 0) {
            return 0.0F; // 不可破坏的方块
        }
        
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
        
        // 应用错误工具类型的硬度惩罚
        boolean isWrongTool = false;
        if (Config.enableWrongToolPenalty && !tool.isEmpty()) {
            boolean isCorrectTool = Config.isCorrectToolType(tool, blockState);
            if (!isCorrectTool) {
                isWrongTool = true;
                // 错误工具类型时，工具硬度降低20%
                effectiveToolHardness = effectiveToolHardness * 0.8;
            }
        }
        
        // 检查挖掘模式
        int miningMode = Config.getMiningMode(blockHardness, effectiveToolHardness);
        
        if (miningMode == 0) {
            return 0.0F; // 无法挖掘
        }
        
        float finalSpeed;
        
        if (Config.useCustomSpeedCalculation) {
            // 使用自定义速度计算
            finalSpeed = Config.calculateCustomMiningSpeed(effectiveToolHardness, blockHardness);
        } else {
            // 使用原版速度修正方法
            float destroySpeed = tool.getDestroySpeed(blockState);
            
            // 绕过原版工具类型限制（如果启用）
            if (Config.bypassVanillaToolRestrictions && !tool.isEmpty()) {
                float toolSpeed = tool.getDestroySpeed(blockState);
                if (toolSpeed > destroySpeed) {
                    destroySpeed = toolSpeed;
                }
            }
            
            // 计算硬度比值和速度修正
            double hardnessRatio = effectiveToolHardness / blockHardness;
            float speedMultiplier = calculateSpeedMultiplier(hardnessRatio, Config.hardnessMultiplier);
            
            finalSpeed = destroySpeed * speedMultiplier;
        }
        
        // 应用其他修正因子
        
        // 缓慢挖掘模式的速度惩罚
        if (miningMode == 2) {
            finalSpeed *= (1.0f - (float)Config.slowMiningSpeedPenalty);
        }
        
        // 错误工具类型惩罚（速度惩罚）
        if (Config.enableWrongToolPenalty && isWrongTool) {
            finalSpeed *= (1.0f - (float)Config.wrongToolSpeedPenalty);
        }
        
        // 耐久度速度惩罚
        if (Config.enableDurabilitySpeedPenalty && !tool.isEmpty()) {
            double durabilitySpeedMultiplier = Config.calculateDurabilityPenalty(tool, Config.maxDurabilitySpeedPenalty);
            finalSpeed *= (float)durabilitySpeedMultiplier;
        }
        
        // 周围方块速度修正
        if (Config.enableSurroundingBlocksModifier) {
            float surroundingModifier = calculateSurroundingBlocksModifier(blockState, level, pos);
            finalSpeed *= surroundingModifier;
        }
        
        // 原版环境惩罚
        if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) && !net.minecraft.world.item.enchantment.EnchantmentHelper.hasAquaAffinity(player)) {
            finalSpeed /= 5.0F;
        }
        
        if (!player.onGround()) {
            finalSpeed /= 5.0F;
        }
        
        return finalSpeed;
    }
    
    /**
     * 使用ln()曲线计算挖掘速度修正系数（与InventoryMixin保持一致）
     */
    private static float calculateSpeedMultiplier(double hardnessRatio, double multiplier) {
        // 如果工具硬度足够（比值 >= 倍数），保持原速度或稍微提升
        if (hardnessRatio >= multiplier) {
            // 可以稍微提升速度，但不要过分
            return Math.min(1.0f + (float)(hardnessRatio - multiplier) * 0.1f, 2.0f);
        }
        
        // 如果工具硬度不足，使用ln()曲线急剧降低速度
        double x = hardnessRatio / multiplier;
        
        if (x <= 0.001) {
            // 极小的比值，几乎无法挖掘
            return 0.001f;
        }
        
        // 使用修正的ln函数
        double logValue = Math.log(x * Math.E + 1) / Math.E;
        
        // 再应用一个平方来让曲线更陡峭
        float result = (float)(logValue * logValue);
        
        // 确保结果在合理范围内
        return Math.max(0.001f, Math.min(result, 1.0f));
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
