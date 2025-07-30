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
 */
public class JadeBlockInfoProvider {

    /**
     * 注册Jade集成
     * 这个方法会在mod初始化时被调用
     */
    public static void registerJadeIntegration() {
        try {
            // 尝试加载Jade的核心类
            Class.forName("snownee.jade.api.IWailaPlugin");
            Class.forName("snownee.jade.api.IWailaClientRegistration");
            Class.forName("snownee.jade.api.IComponentProvider");
            
            // 如果成功加载，说明Jade已安装
            org.goldgomtech.betterexcavate.BetterExcavate.LOGGER.info("[BetterExcavate] Jade detected, enabling block hardness integration");
            
            // 注册我们的组件提供者
            // 注意：实际的注册逻辑需要在JadePlugin类中实现
            
        } catch (ClassNotFoundException e) {
            // Jade未安装，跳过集成
            org.goldgomtech.betterexcavate.BetterExcavate.LOGGER.info("[BetterExcavate] Jade not found, skipping integration");
        } catch (Exception e) {
            org.goldgomtech.betterexcavate.BetterExcavate.LOGGER.error("[BetterExcavate] Error initializing Jade integration: " + e.getMessage());
        }
    }
    
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
        double maxMineableHardness = effectiveToolHardness * Config.hardnessMultiplier;
        
        if (blockHardness > maxMineableHardness) {
            // 无法挖掘
            info.add(Component.translatable("betterexcavate.jade.cannot_mine"));
        } else {
            // 可以挖掘，显示效率
            double hardnessRatio = effectiveToolHardness / blockHardness;
            
            if (hardnessRatio >= Config.hardnessMultiplier) {
                info.add(Component.translatable("betterexcavate.jade.efficient_mining"));
            } else if (hardnessRatio >= Config.hardnessMultiplier * 0.5) {
                info.add(Component.translatable("betterexcavate.jade.normal_mining"));
            } else {
                info.add(Component.translatable("betterexcavate.jade.slow_mining"));
            }
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
}
