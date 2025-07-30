package org.goldgomtech.betterexcavate.mixin;

import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(Block.class)
public class BlockMixin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterExcavate");
    
    // BlockMixin现在主要由事件系统处理
    // 保留这个类是为了未来可能需要的其他Block相关功能
}

