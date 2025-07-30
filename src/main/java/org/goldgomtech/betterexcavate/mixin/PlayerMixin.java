package org.goldgomtech.betterexcavate.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.goldgomtech.betterexcavate.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(Player.class)
public class PlayerMixin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterExcavate");
    
    /**
     * 这个Mixin拦截玩家的挖掘速度计算
     * 使用ln()曲线调整挖掘速度，当工具硬度不足时速度急剧下降
     */
    /**
     * 这个Mixin拦截玩家是否有正确工具的判断
     * 通过修改这个方法来实现挖掘速度控制
     */
    @Inject(method = "hasCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void onHasCorrectToolForDrops(BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        ItemStack heldItem = player.getMainHandItem();
        
        // 获取方块信息
        String blockName = blockState.getBlock().getDescriptionId();
        float blockHardness = blockState.getDestroySpeed(player.level(), null);
        
        // 如果方块硬度为-1（如基岩），则无法挖掘
        if (blockHardness < 0) {
            return; // 保持原版行为
        }
        
        double toolHardness;
        String toolName = "hand";
        
        // 如果没有手持物品，使用默认硬度
        if (heldItem.isEmpty()) {
            toolHardness = Config.defaultHardness;
        } else {
            // 获取工具的注册名
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
        
        // 计算工具硬度与方块硬度的比值
        double hardnessRatio = effectiveToolHardness / blockHardness;
        
        // 检查是否可以获得掉落物
        double maxMineableHardness = effectiveToolHardness * Config.hardnessMultiplier;
        
        if (blockHardness > maxMineableHardness) {
            // 方块太硬，无法用此工具正确挖掘
            LOGGER.info("[BetterExcavate] Block {} too hard for tool {} - no correct tool! Block hardness: {}, Original tool hardness: {}, Effective tool hardness: {}, Max mineable hardness: {}",
                    blockName, toolName, blockHardness, toolHardness, effectiveToolHardness, maxMineableHardness);
            cir.setReturnValue(false);
            return;
        }
        
        LOGGER.info("[BetterExcavate] Block {} can be properly mined with {} - has correct tool! Block hardness: {}, Original tool hardness: {}, Effective tool hardness: {}, Max mineable hardness: {}",
                blockName, toolName, blockHardness, toolHardness, effectiveToolHardness, maxMineableHardness);
        // 不设置返回值，保持原版逻辑
    }
    
    /**
     * 使用ln()曲线计算挖掘速度修正系数
     * @param hardnessRatio 工具硬度/方块硬度的比值
     * @param multiplier 硬度倍数配置
     * @return 速度修正系数 (0.0 到 1.0+)
     */
    private float calculateSpeedMultiplier(double hardnessRatio, double multiplier) {
        // 如果工具硬度足够（比值 >= 倍数），保持原速度或稍微提升
        if (hardnessRatio >= multiplier) {
            // 可以稍微提升速度，但不要过分
            return Math.min(1.0f + (float)(hardnessRatio - multiplier) * 0.1f, 2.0f);
        }
        
        // 如果工具硬度不足，使用ln()曲线急剧降低速度
        // 当hardnessRatio接近0时，速度接近0
        // 当hardnessRatio = multiplier时，速度 = 1
        
        // 使用修正的ln函数：ln(hardnessRatio / multiplier + e) / e
        // 这样当hardnessRatio = multiplier时，结果约为1
        // 当hardnessRatio趋近于0时，结果趋近于1/e ≈ 0.37，然后我们再进一步压缩
        
        double x = hardnessRatio / multiplier;
        
        if (x <= 0.001) {
            // 极小的比值，几乎无法挖掘
            return 0.001f;
        }
        
        // 使用 ln(x + 1) / ln(2) 的变形，使得x=1时结果为1，x=0时结果为0
        // 进一步调整为更陡峭的曲线
        double logValue = Math.log(x * Math.E + 1) / Math.E;
        
        // 再应用一个平方来让曲线更陡峭
        float result = (float)(logValue * logValue);
        
        // 确保结果在合理范围内
        return Math.max(0.001f, Math.min(result, 1.0f));
    }
}
