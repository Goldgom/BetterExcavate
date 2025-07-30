package org.goldgomtech.betterexcavate.integration;

import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IWailaConfig;

/**
 * Jade插件实现类
 * 按照标准的Jade插件模式实现
 */
@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    
    public static final String ID = "betterexcavate";
    public static final ResourceLocation BLOCK_HARDNESS_CONFIG = ResourceLocation.fromNamespaceAndPath(ID, "block_hardness");
    
    @Override
    public void register(IWailaCommonRegistration registration) {
        // 注册通用数据提供者（服务器端）
        // 对于我们的硬度显示，不需要服务器端数据
    }
    
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        org.goldgomtech.betterexcavate.BetterExcavate.LOGGER.info("[BetterExcavate] Registering Jade client integration");
        
        // 注册到所有方块
        registration.registerBlockComponent(BlockHardnessProvider.INSTANCE, net.minecraft.world.level.block.Block.class);
        
        // 添加配置项（允许用户启用/禁用硬度显示）
        registration.addConfig(BLOCK_HARDNESS_CONFIG, true);
        
        // 添加配置监听器（可选）
        registration.addConfigListener(BLOCK_HARDNESS_CONFIG, id -> {
            boolean enabled = IWailaConfig.get().getPlugin().get(id);
            org.goldgomtech.betterexcavate.BetterExcavate.LOGGER.info("[BetterExcavate] Block hardness display changed to: " + enabled);
        });
        
        // 标记为客户端功能
        registration.markAsClientFeature(BLOCK_HARDNESS_CONFIG);
        
        org.goldgomtech.betterexcavate.BetterExcavate.LOGGER.info("[BetterExcavate] Jade integration registered successfully");
    }
}
