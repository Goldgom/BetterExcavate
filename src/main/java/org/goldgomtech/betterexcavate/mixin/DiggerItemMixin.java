package org.goldgomtech.betterexcavate.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.DiggerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.goldgomtech.betterexcavate.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 禁用DiggerItem（镐子、斧子、铲子等）的原版挖掘等级检查
 */
@Mixin(DiggerItem.class)
public class DiggerItemMixin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterExcavate");
    
    /**
     * 禁用DiggerItem的isCorrectToolForDrops方法
     * 这个方法通常检查工具是否适合特定方块
     */
    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void onDiggerItemIsCorrectToolForDrops(BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
        // 如果掉落物控制被禁用，保持原版行为
        if (!Config.enableDropControl) {
            return;
        }
        
        // 总是返回true，让我们的系统处理
        LOGGER.debug("[BetterExcavate] Bypassing vanilla DiggerItem correctness check for block: {}", 
                    blockState.getBlock().getDescriptionId());
        cir.setReturnValue(true);
    }
}
