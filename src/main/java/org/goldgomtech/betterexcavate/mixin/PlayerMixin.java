package org.goldgomtech.betterexcavate.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.goldgomtech.betterexcavate.BetterExcavate;

@Mixin(Player.class)
public class PlayerMixin {
    
    /**
     * 这个Mixin拦截玩家的挖掘速度计算
     * 你可以在这里添加逻辑来修改挖掘行为
     */
    @Inject(method = "getDigSpeed", at = @At("RETURN"), cancellable = true)
    private void onGetDigSpeed(BlockState blockState, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        // 获取原始挖掘速度
        float originalSpeed = cir.getReturnValue();
        
        // 这里可以添加你的Better Excavate逻辑
        // 例如：如果玩家手持特定工具，提高挖掘速度
        Player player = (Player) (Object) this;
        
        // 示例：在创造模式下，挖掘速度提高2倍
        if (player.isCreative()) {
            cir.setReturnValue(originalSpeed * 2.0f);
        }
        
        // 你可以在这里添加更多的Better Excavate功能
        // 比如连锁挖掘、区域挖掘等逻辑
    }
}
