package org.goldgomtech.betterexcavate.integration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;

/**
 * 方块硬度组件提供者
 * 使用枚举单例模式实现Jade的IBlockComponentProvider接口
 */
public enum BlockHardnessProvider implements IBlockComponentProvider {
    INSTANCE;
    
    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        // 检查配置是否启用了硬度显示
        if (!IWailaConfig.get().getPlugin().get(JadePlugin.BLOCK_HARDNESS_CONFIG)) {
            return;
        }
        
        try {
            // 获取方块状态和玩家
            BlockState blockState = accessor.getBlockState();
            Player player = accessor.getPlayer();
            
            // 获取方块和工具信息
            Component[] info = JadeBlockInfoProvider.getBlockToolInfo(
                blockState, player, 
                accessor.getLevel(), 
                accessor.getPosition()
            );
            
            // 添加信息到tooltip
            for (Component component : info) {
                tooltip.add(component);
            }
            
        } catch (Exception e) {
            org.goldgomtech.betterexcavate.BetterExcavate.LOGGER.error(
                "[BetterExcavate] Error adding block hardness info to Jade tooltip: " + e.getMessage());
        }
    }
    
    @Override
    public ResourceLocation getUid() {
        return JadePlugin.BLOCK_HARDNESS_CONFIG;
    }
}
