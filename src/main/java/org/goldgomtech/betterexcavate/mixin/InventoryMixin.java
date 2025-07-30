package org.goldgomtech.betterexcavate.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.goldgomtech.betterexcavate.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(Inventory.class)
public class InventoryMixin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterExcavate");
    
    @Shadow
    public Player player;
    
    /**
     * 这个Mixin拦截背包的破坏速度计算
     * 使用ln()曲线调整挖掘速度，当工具硬度不足时速度急剧下降
     */
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void onGetDestroySpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
        ItemStack heldItem = player.getMainHandItem();
        
        // 获取原版计算的挖掘速度
        float originalSpeed = cir.getReturnValue();
        
        // 获取方块信息
        String blockName = blockState.getBlock().getDescriptionId();
        float blockHardness = blockState.getDestroySpeed(player.level(), null);
        
        // 如果方块硬度为-1（如基岩），则无法挖掘
        if (blockHardness < 0) {
            LOGGER.info("[BetterExcavate] Block {} is unbreakable (hardness: {})", blockName, blockHardness);
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
        
        // 应用耐久度惩罚到工具硬度
        double effectiveToolHardness = toolHardness;
        if (Config.enableDurabilityHardnessPenalty && !heldItem.isEmpty()) {
            double hardnessPenalty = Config.calculateDurabilityPenalty(heldItem, Config.maxDurabilityHardnessPenalty);
            effectiveToolHardness = toolHardness * hardnessPenalty;
        }
        
        // 计算工具硬度与方块硬度的比值
        double hardnessRatio = effectiveToolHardness / blockHardness;
        
        // 使用ln()曲线计算速度修正系数
        float speedMultiplier = calculateSpeedMultiplier(hardnessRatio, Config.hardnessMultiplier);
        
        // 如果启用了周围方块修正，计算周围方块影响
        float surroundingMultiplier = 1.0f;
        if (Config.enableSurroundingBlocksModifier) {
            BlockPos targetPos = getTargetBlockPos();
            if (targetPos != null) {
                int identicalBlocks = countIdenticalSurroundingBlocks(blockState, targetPos, player.level());
                surroundingMultiplier = calculateSurroundingBlocksMultiplier(identicalBlocks);
                LOGGER.info("[BetterExcavate] Found {} identical surrounding blocks for {}", identicalBlocks, blockName);
            }
        }
        
        // 应用耐久度速度惩罚
        float durabilitySpeedMultiplier = 1.0f;
        if (Config.enableDurabilitySpeedPenalty && !heldItem.isEmpty()) {
            durabilitySpeedMultiplier = (float) Config.calculateDurabilityPenalty(heldItem, Config.maxDurabilitySpeedPenalty);
        }
        
        // 应用速度修正（工具硬度修正 × 周围方块修正 × 耐久度修正）
        float newSpeed = originalSpeed * speedMultiplier * surroundingMultiplier * durabilitySpeedMultiplier;
        
        // 记录挖掘信息
        if (speedMultiplier < 0.1f) {
            LOGGER.info("[BetterExcavate] Mining {} with {} (severely limited): Block hardness: {}, Tool hardness: {}, Ratio: {}, Speed multiplier: {}, Surrounding multiplier: {}, Durability multiplier: {}, Original speed: {}, Final speed: {}",
                    blockName, toolName, 
                    String.format("%.2f", blockHardness), 
                    String.format("%.2f", toolHardness), 
                    String.format("%.2f", hardnessRatio), 
                    String.format("%.3f", speedMultiplier),
                    String.format("%.3f", surroundingMultiplier),
                    String.format("%.3f", durabilitySpeedMultiplier), 
                    String.format("%.3f", originalSpeed), 
                    String.format("%.3f", newSpeed));
        } else if (speedMultiplier < 1.0f || surroundingMultiplier < 1.0f || durabilitySpeedMultiplier < 1.0f) {
            LOGGER.info("[BetterExcavate] Mining {} with {} (reduced speed): Block hardness: {}, Tool hardness: {}, Ratio: {}, Speed multiplier: {}, Surrounding multiplier: {}, Durability multiplier: {}, Original speed: {}, Final speed: {}",
                    blockName, toolName, 
                    String.format("%.2f", blockHardness), 
                    String.format("%.2f", toolHardness), 
                    String.format("%.2f", hardnessRatio), 
                    String.format("%.3f", speedMultiplier),
                    String.format("%.3f", surroundingMultiplier),
                    String.format("%.3f", durabilitySpeedMultiplier), 
                    String.format("%.3f", originalSpeed), 
                    String.format("%.3f", newSpeed));
        } else {
            LOGGER.info("[BetterExcavate] Mining {} with {} (normal speed): Block hardness: {}, Tool hardness: {}, Ratio: {}, Speed multiplier: {}, Surrounding multiplier: {}, Durability multiplier: {}, Original speed: {}, Final speed: {}",
                    blockName, toolName, 
                    String.format("%.2f", blockHardness), 
                    String.format("%.2f", toolHardness), 
                    String.format("%.2f", hardnessRatio), 
                    String.format("%.3f", speedMultiplier),
                    String.format("%.3f", surroundingMultiplier),
                    String.format("%.3f", durabilitySpeedMultiplier), 
                    String.format("%.3f", originalSpeed), 
                    String.format("%.3f", newSpeed));
        }
        
        cir.setReturnValue(newSpeed);
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
    
    /**
     * 获取玩家瞄准的方块位置
     */
    private BlockPos getTargetBlockPos() {
        HitResult hitResult = player.pick(5.0D, 0.0F, false);
        if (hitResult instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getBlockPos();
        }
        return null;
    }
    
    /**
     * 统计目标方块周围6面的相同方块数量
     */
    private int countIdenticalSurroundingBlocks(BlockState targetState, BlockPos targetPos, Level level) {
        int count = 0;
        
        // 检查6个方向：上下东西南北
        BlockPos[] directions = {
            targetPos.above(),    // 上
            targetPos.below(),    // 下
            targetPos.east(),     // 东
            targetPos.west(),     // 西
            targetPos.north(),    // 北
            targetPos.south()     // 南
        };
        
        for (BlockPos pos : directions) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() == targetState.getBlock()) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 根据周围相同方块数量计算速度修正系数
     * @param identicalBlocks 周围相同方块数量 (0-6)
     * @return 速度修正系数
     */
    private float calculateSurroundingBlocksMultiplier(int identicalBlocks) {
        if (identicalBlocks == 0) {
            return (float) Config.maxSpeedMultiplier;
        }
        
        // 计算归一化的方块数量 (0到1之间)
        double normalizedCount = identicalBlocks / 6.0;
        
        double multiplier;
        if ("logarithmic".equals(Config.speedCurveType)) {
            // 对数曲线：开始下降很快，后面趋于平缓
            // 使用 1 - log(1 + x * 9) / log(10) 这样当x=0时结果为1，当x=1时结果约为0
            multiplier = 1.0 - Math.log(1 + normalizedCount * 9) / Math.log(10);
        } else {
            // 线性曲线：均匀下降
            multiplier = 1.0 - normalizedCount;
        }
        
        // 将结果映射到配置的范围内
        double range = Config.maxSpeedMultiplier - Config.minSpeedMultiplier;
        multiplier = Config.minSpeedMultiplier + multiplier * range;
        
        return (float) Math.max(Config.minSpeedMultiplier, Math.min(multiplier, Config.maxSpeedMultiplier));
    }
}
