package org.goldgomtech.betterexcavate.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.goldgomtech.betterexcavate.BetterExcavate;
import org.goldgomtech.betterexcavate.Config;

/**
 * 客户端工具提示处理器
 * 在鼠标悬浮在工具上时显示挖掘硬度信息
 */
@Mod.EventBusSubscriber(modid = BetterExcavate.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ToolTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        
        // 检查物品是否为空
        if (itemStack.isEmpty()) {
            return;
        }
        
        // 获取物品的注册名
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem());
        if (itemId == null) {
            return;
        }
        
        String itemName = itemId.toString();
        
        // 检查是否为工具（包含pickaxe, axe, shovel, hoe, sword）
        boolean isTool = itemName.contains("pickaxe") || 
                        itemName.contains("axe") || 
                        itemName.contains("shovel") || 
                        itemName.contains("hoe") || 
                        itemName.contains("sword");
        
        if (!isTool) {
            return;
        }
        
        // 获取工具的硬度值
        double toolHardness = Config.toolHardnessMap.getOrDefault(itemName, Config.defaultHardness);
        
        // 计算有效硬度（考虑耐久度惩罚）
        double effectiveHardness = toolHardness;
        if (Config.enableDurabilityHardnessPenalty && itemStack.isDamageableItem()) {
            double durabilityMultiplier = Config.calculateDurabilityPenalty(itemStack, Config.maxDurabilityHardnessPenalty);
            effectiveHardness = toolHardness * durabilityMultiplier;
        }
        
        // 添加工具硬度信息到提示框
        event.getToolTip().add(Component.literal(""));  // 空行分隔
        
        // 显示原始硬度
        Component hardnessText = Component.translatable("betterexcavate.tooltip.tool_hardness", 
            String.format("%.2f", toolHardness))
            .withStyle(ChatFormatting.YELLOW);
        event.getToolTip().add(hardnessText);
        
        // 如果有耐久度惩罚，显示有效硬度
        if (effectiveHardness != toolHardness && itemStack.isDamageableItem()) {
            Component effectiveHardnessText = Component.translatable("betterexcavate.tooltip.effective_hardness", 
                String.format("%.2f", effectiveHardness))
                .withStyle(ChatFormatting.GOLD);
            event.getToolTip().add(effectiveHardnessText);
            
            // 显示硬度损失百分比
            double hardnessLoss = (1.0 - (effectiveHardness / toolHardness)) * 100;
            if (hardnessLoss > 0.01) {  // 只有当损失大于0.01%时才显示
                Component hardnessLossText = Component.translatable("betterexcavate.tooltip.hardness_loss", 
                    String.format("%.1f", hardnessLoss))
                    .withStyle(ChatFormatting.RED);
                event.getToolTip().add(hardnessLossText);
            }
        }
        
        // 显示可挖掘硬度范围
        double maxMineableHardness = effectiveHardness * Config.hardnessMultiplier;
        Component mineableText = Component.translatable("betterexcavate.tooltip.max_mineable_hardness", 
            String.format("%.2f", maxMineableHardness))
            .withStyle(ChatFormatting.GREEN);
        event.getToolTip().add(mineableText);
        
        // 如果启用了缓慢挖掘，显示缓慢挖掘范围
        if (Config.enableSlowMiningWithoutDrops) {
            double maxSlowMineableHardness = effectiveHardness * Config.slowMiningHardnessMultiplier;
            Component slowMineableText = Component.translatable("betterexcavate.tooltip.max_slow_mineable_hardness", 
                String.format("%.2f", maxSlowMineableHardness))
                .withStyle(ChatFormatting.GOLD);
            event.getToolTip().add(slowMineableText);
        }
        
        // 显示工具类型信息
        String toolTypeKey = getToolType(itemName);
        if (!toolTypeKey.isEmpty()) {
            Component toolTypeText = Component.translatable("betterexcavate.tooltip.tool_type", 
                Component.translatable(toolTypeKey).getString())
                .withStyle(ChatFormatting.AQUA);
            event.getToolTip().add(toolTypeText);
        }
        
        // 如果工具有耐久度，显示耐久度相关信息
        if (itemStack.isDamageableItem()) {
            int currentDurability = itemStack.getMaxDamage() - itemStack.getDamageValue();
            int maxDurability = itemStack.getMaxDamage();
            double wearPercentage = (double) itemStack.getDamageValue() / itemStack.getMaxDamage() * 100;
            
            Component durabilityText = Component.translatable("betterexcavate.tooltip.durability_info", 
                currentDurability, maxDurability, String.format("%.1f", wearPercentage))
                .withStyle(ChatFormatting.GRAY);
            event.getToolTip().add(durabilityText);
        }
    }
    
    /**
     * 根据物品名称获取工具类型的本地化字符串
     */
    private static String getToolType(String itemName) {
        if (itemName.contains("pickaxe")) {
            return "betterexcavate.tool_type.pickaxe";
        } else if (itemName.contains("axe") && !itemName.contains("pickaxe")) {
            return "betterexcavate.tool_type.axe";
        } else if (itemName.contains("shovel")) {
            return "betterexcavate.tool_type.shovel";
        } else if (itemName.contains("hoe")) {
            return "betterexcavate.tool_type.hoe";
        } else if (itemName.contains("sword")) {
            return "betterexcavate.tool_type.sword";
        }
        return "";
    }
}
