package org.goldgomtech.betterexcavate.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.goldgomtech.betterexcavate.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 这个Mixin用于禁用原版的挖掘等级检查系统
 * 让我们的自定义硬度系统完全接管挖掘判定
 */
@Mixin(ItemStack.class)
public class MixinHelper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterExcavate");
    
    /**
     * 禁用原版的isCorrectToolForDrops方法
     * 这个方法决定工具是否正确，从而影响掉落物和挖掘速度
     */
    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void onIsCorrectToolForDrops(BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
        // 如果掉落物控制被禁用，保持原版行为
        if (!Config.enableDropControl) {
            return;
        }
        
        // 总是返回true，让我们的系统在BlockBreakHandler中处理掉落物控制
        // 这样原版的挖掘等级检查就被完全绕过了
        LOGGER.debug("[BetterExcavate] Bypassing vanilla tool correctness check for block: {}", 
                    blockState.getBlock().getDescriptionId());
        cir.setReturnValue(true);
    }
}
