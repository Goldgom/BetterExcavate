package org.goldgomtech.betterexcavate.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.goldgomtech.betterexcavate.Config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Mixin 辅助类，用于配置连锁挖掘行为
 */
public class MixinHelper {
    
    // 可连锁挖掘的方块集合
    private static final Set<Block> CHAINABLE_BLOCKS = new HashSet<>(Arrays.asList(
        // 石头类
        Blocks.STONE,
        Blocks.COBBLESTONE,
        Blocks.DEEPSLATE,
        Blocks.COBBLED_DEEPSLATE,
        
        // 原木类
        Blocks.OAK_LOG,
        Blocks.BIRCH_LOG,
        Blocks.SPRUCE_LOG,
        Blocks.JUNGLE_LOG,
        Blocks.ACACIA_LOG,
        Blocks.DARK_OAK_LOG,
        Blocks.MANGROVE_LOG,
        Blocks.CHERRY_LOG,
        
        // 矿石类
        Blocks.COAL_ORE,
        Blocks.IRON_ORE,
        Blocks.GOLD_ORE,
        Blocks.DIAMOND_ORE,
        Blocks.EMERALD_ORE,
        Blocks.LAPIS_ORE,
        Blocks.REDSTONE_ORE,
        Blocks.COPPER_ORE,
        
        // 深层矿石
        Blocks.DEEPSLATE_COAL_ORE,
        Blocks.DEEPSLATE_IRON_ORE,
        Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.DEEPSLATE_COPPER_ORE,
        
        // 其他常见方块
        Blocks.DIRT,
        Blocks.GRAVEL,
        Blocks.SAND,
        Blocks.NETHERRACK,
        Blocks.END_STONE
    ));
    
    /**
     * 检查方块是否可以连锁挖掘
     */
    public static boolean isChainableBlock(Block block) {
        return CHAINABLE_BLOCKS.contains(block);
    }
    
    /**
     * 获取最大连锁挖掘数量
     */
    public static int getMaxChainBlocks() {
        return Config.maxChainBlocks;
    }
    
    /**
     * 检查是否启用连锁挖掘
     */
    public static boolean isChainMiningEnabled() {
        return Config.enableChainMining;
    }
    
    /**
     * 检查是否需要正确的工具
     */
    public static boolean requireCorrectTool() {
        return Config.requireCorrectTool;
    }
    
    /**
     * 检查是否在连锁挖掘时损坏工具
     */
    public static boolean damageToolOnChain() {
        return Config.damageToolOnChain;
    }
    
    /**
     * 检查是否只对相同方块类型进行连锁挖掘
     */
    public static boolean isSameTypeOnly() {
        return true; // 只挖掘相同类型的方块
    }
}
