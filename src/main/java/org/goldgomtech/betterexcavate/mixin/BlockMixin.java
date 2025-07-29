package org.goldgomtech.betterexcavate.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

@Mixin(Block.class)
public class BlockMixin {
    
    private static final int MAX_CHAIN_BLOCKS = MixinHelper.getMaxChainBlocks(); // 最大连锁挖掘数量
    
    /**
     * 拦截方块破坏事件，实现连锁挖掘
     */
    @Inject(method = "playerDestroy", at = @At("HEAD"))
    private void onPlayerDestroy(Level level, Player player, BlockPos pos, BlockState state, 
                                net.minecraft.world.level.block.entity.BlockEntity blockEntity, 
                                ItemStack tool, CallbackInfo ci) {
        
        // 检查是否应该触发连锁挖掘
        if (shouldTriggerChainMining(player, tool, state)) {
            performChainMining(level, player, pos, state, tool);
        }
    }
    
    /**
     * 检查是否应该触发连锁挖掘
     */
    private boolean shouldTriggerChainMining(Player player, ItemStack tool, BlockState state) {
        // 检查是否启用连锁挖掘
        if (!MixinHelper.isChainMiningEnabled()) {
            return false;
        }
        
        // 只在生存模式下工作
        if (player.isCreative()) {
            return false;
        }
        
        // 检查玩家是否潜行（潜行时禁用连锁挖掘）
        if (player.isShiftKeyDown()) {
            return false;
        }
        
        // 检查工具是否有效且有耐久
        if (tool.isEmpty() || tool.getDamageValue() >= tool.getMaxDamage() - 1) {
            return false;
        }
        
        // 只对特定方块类型进行连锁挖掘
        return MixinHelper.isChainableBlock(state.getBlock());
    }
    
    /**
     * 检查方块是否可以连锁挖掘
     */
    private boolean isChainableBlock(BlockState state) {
        return MixinHelper.isChainableBlock(state.getBlock());
    }
    
    /**
     * 执行连锁挖掘
     */
    private void performChainMining(Level level, Player player, BlockPos startPos, 
                                   BlockState targetState, ItemStack tool) {
        
        Set<BlockPos> visited = new HashSet<>();
        Stack<BlockPos> toProcess = new Stack<>();
        int blocksDestroyed = 0;
        
        toProcess.push(startPos);
        visited.add(startPos);
        
        while (!toProcess.isEmpty() && blocksDestroyed < MAX_CHAIN_BLOCKS) {
            BlockPos currentPos = toProcess.pop();
            
            // 检查周围的方块
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        // 跳过中心方块
                        if (x == 0 && y == 0 && z == 0) continue;
                        
                        BlockPos neighborPos = currentPos.offset(x, y, z);
                        
                        // 如果已经访问过，跳过
                        if (visited.contains(neighborPos)) continue;
                        
                        visited.add(neighborPos);
                        BlockState neighborState = level.getBlockState(neighborPos);
                        
                        // 检查是否是相同类型的方块
                        if (neighborState.getBlock() == targetState.getBlock()) {
                            // 检查工具耐久
                            if (tool.getDamageValue() >= tool.getMaxDamage() - 1) {
                                return; // 工具快坏了，停止连锁
                            }
                            
                            // 破坏方块
                            level.destroyBlock(neighborPos, true, player);
                            
                            // 损坏工具（如果配置启用）
                            if (MixinHelper.damageToolOnChain()) {
                                tool.hurt(1, level.random, null);
                            }
                            
                            // 添加到处理队列
                            toProcess.push(neighborPos);
                            blocksDestroyed++;
                            
                            if (blocksDestroyed >= MAX_CHAIN_BLOCKS) {
                                return; // 达到最大连锁数量
                            }
                        }
                    }
                }
            }
        }
    }
}
