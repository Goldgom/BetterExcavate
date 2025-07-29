package org.goldgomtech.betterexcavate.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.goldgomtech.betterexcavate.BetterExcavate;
import org.goldgomtech.betterexcavate.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = BetterExcavate.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlockBreakHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterExcavate");
    
    // 记录玩家对特定方块的挖掘开始时间
    private static final Map<String, Long> miningStartTimes = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastDamageTime = new ConcurrentHashMap<>();
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // 如果掉落物控制被禁用，直接返回
        if (!Config.enableDropControl) {
            return;
        }
        
        // 只处理玩家破坏的情况
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        
        // 如果是创造模式，不受限制
        if (player.isCreative()) {
            return;
        }
        
        // 获取方块状态和硬度
        BlockState state = event.getState();
        float blockHardness = state.getDestroySpeed(event.getLevel(), event.getPos());
        
        // 如果方块硬度为-1（如基岩），直接返回
        if (blockHardness < 0) {
            return;
        }
        
        // 获取玩家手持的工具
        ItemStack tool = player.getMainHandItem();
        
        double toolHardness;
        String toolName = "hand";
        
        // 获取工具硬度
        if (tool.isEmpty()) {
            toolHardness = Config.defaultHardness;
        } else {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(tool.getItem());
            if (itemId == null) {
                toolHardness = Config.defaultHardness;
            } else {
                toolName = itemId.toString();
                toolHardness = Config.toolHardnessMap.getOrDefault(toolName, Config.defaultHardness);
            }
        }
        
        // 检查是否可以获得掉落物
        double maxMineableHardness = toolHardness * Config.hardnessMultiplier;
        
        if (blockHardness > maxMineableHardness) {
            // 方块太硬，完全取消破坏事件
            String blockName = state.getBlock().getDescriptionId();
            LOGGER.info("[BetterExcavate] Block {} too hard for tool {} - cancelling break event! Block hardness: {}, Tool hardness: {}, Max mineable hardness: {}",
                    blockName, toolName, blockHardness, toolHardness, maxMineableHardness);
            
            // 取消整个破坏事件，这样既不会破坏方块也不会有掉落物
            event.setCanceled(true);
            return;
        }
        
        // 如果工具硬度足够，允许正常破坏和掉落
        String blockName = state.getBlock().getDescriptionId();
        LOGGER.info("[BetterExcavate] Block {} can be properly mined with {} - drops/exp enabled! Block hardness: {}, Tool hardness: {}",
                blockName, toolName, blockHardness, toolHardness);
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 如果工具损坏功能被禁用，直接返回
        if (!Config.enableToolDamageOnInvalidMining) {
            return;
        }
        
        // 只在服务端处理
        if (event.side.isClient()) {
            return;
        }
        
        // 只处理玩家tick的结束阶段
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        Player player = event.player;
        
        // 如果是创造模式，不处理
        if (player.isCreative()) {
            return;
        }
        
        // 检查玩家是否在尝试挖掘方块（攻击状态）
        if (!isPlayerMiningBlock(player)) {
            // 如果玩家停止挖掘，清理所有记录
            cleanupAllMiningRecords(player);
            return;
        }
        
        // 获取玩家瞄准的方块
        HitResult hitResult = player.pick(5.0D, 0.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }
        
        BlockPos pos = blockHitResult.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        
        // 获取方块硬度
        float blockHardness = state.getDestroySpeed(player.level(), pos);
        
        // 如果方块硬度为-1或0，不处理
        if (blockHardness <= 0) {
            return;
        }
        
        // 获取玩家手持的工具
        ItemStack tool = player.getMainHandItem();
        
        // 如果工具为空或不能损坏，不处理
        if (tool.isEmpty() || !tool.isDamageableItem()) {
            return;
        }
        
        double toolHardness;
        String toolName;
        
        // 获取工具硬度
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(tool.getItem());
        if (itemId == null) {
            toolHardness = Config.defaultHardness;
            toolName = "unknown";
        } else {
            toolName = itemId.toString();
            toolHardness = Config.toolHardnessMap.getOrDefault(toolName, Config.defaultHardness);
        }
        
        // 检查是否可以挖掘
        double maxMineableHardness = toolHardness * Config.hardnessMultiplier;
        
        if (blockHardness > maxMineableHardness) {
            // 方块太硬，每秒损坏工具耐久
            String playerKey = player.getUUID().toString() + "_" + pos.toString();
            long currentTime = System.currentTimeMillis();
            
            // 检查是否已经开始挖掘这个方块
            if (!miningStartTimes.containsKey(playerKey)) {
                miningStartTimes.put(playerKey, currentTime);
                lastDamageTime.put(playerKey, currentTime);
                return;
            }
            
            // 检查是否需要损坏工具（每1000毫秒 = 1秒）
            long lastDamage = lastDamageTime.getOrDefault(playerKey, currentTime);
            if (currentTime - lastDamage >= 1000) {
                // 损坏工具1点耐久
                tool.hurt(1, player.getRandom(), null);
                lastDamageTime.put(playerKey, currentTime);
                
                String blockName = state.getBlock().getDescriptionId();
                LOGGER.info("[BetterExcavate] Tool {} damaged while trying to mine {} - durability: {}/{}", 
                           toolName, blockName, tool.getMaxDamage() - tool.getDamageValue(), tool.getMaxDamage());
                
                // 如果工具完全损坏，清理记录
                if (tool.getDamageValue() >= tool.getMaxDamage()) {
                    miningStartTimes.remove(playerKey);
                    lastDamageTime.remove(playerKey);
                }
            }
        } else {
            // 如果可以挖掘，清理挖掘记录
            cleanupMiningRecords(player, pos);
        }
    }
    
    /**
     * 检查玩家是否在挖掘方块
     */
    private static boolean isPlayerMiningBlock(Player player) {
        // 检查玩家是否按住左键（攻击键）
        // 这需要检查玩家的攻击冷却时间或其他指标
        // 简化版本：检查玩家是否手持工具且面向方块
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) {
            return false;
        }
        
        // 检查玩家是否面向方块
        HitResult hitResult = player.pick(5.0D, 0.0F, false);
        return hitResult instanceof BlockHitResult;
    }
    
    /**
     * 清理指定位置的挖掘记录
     */
    private static void cleanupMiningRecords(Player player, BlockPos pos) {
        String playerKey = player.getUUID().toString() + "_" + pos.toString();
        miningStartTimes.remove(playerKey);
        lastDamageTime.remove(playerKey);
    }
    
    /**
     * 清理玩家的所有挖掘记录
     */
    private static void cleanupAllMiningRecords(Player player) {
        String playerUUID = player.getUUID().toString();
        miningStartTimes.entrySet().removeIf(entry -> entry.getKey().startsWith(playerUUID));
        lastDamageTime.entrySet().removeIf(entry -> entry.getKey().startsWith(playerUUID));
    }
}
