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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
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
    
    // 记录玩家的挖掘状态
    private static final Map<String, BlockPos> playerMiningBlocks = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastMiningActivity = new ConcurrentHashMap<>();
    
    // 记录玩家是否正在持续挖掘（用于检测挖掘停止）
    private static final Map<String, Boolean> playerActivelyMining = new ConcurrentHashMap<>();
    
    // 记录上一tick的挖掘状态，用于检测状态是否被意外重置
    private static final Map<String, Boolean> previousMiningState = new ConcurrentHashMap<>();
    private static final Map<String, BlockPos> previousMiningPos = new ConcurrentHashMap<>();
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // 清理挖掘状态记录
        if (event.getPlayer() != null) {
            String playerUUID = event.getPlayer().getUUID().toString();
            playerMiningBlocks.remove(playerUUID);
            lastMiningActivity.remove(playerUUID);
            playerActivelyMining.remove(playerUUID);
        }
        
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
        
        // 应用耐久度硬度惩罚
        double effectiveToolHardness = toolHardness;
        if (Config.enableDurabilityHardnessPenalty && !tool.isEmpty()) {
            double durabilityHardnessMultiplier = Config.calculateDurabilityPenalty(tool, Config.maxDurabilityHardnessPenalty);
            effectiveToolHardness = toolHardness * durabilityHardnessMultiplier;
        }
        
        // 检查挖掘模式
        int miningMode = Config.getMiningMode(blockHardness, effectiveToolHardness);
        String blockName = state.getBlock().getDescriptionId();
        
        if (miningMode == 0) {
            // 无法挖掘，取消破坏事件
            LOGGER.info("[BetterExcavate] Block {} too hard for tool {} - cancelling break event! Block hardness: {}, Original tool hardness: {}, Effective tool hardness: {}",
                    blockName, toolName, blockHardness, toolHardness, effectiveToolHardness);
            
            event.setCanceled(true);
            return;
        } else if (miningMode == 1) {
            // 正常挖掘，有掉落物
            LOGGER.info("[BetterExcavate] Block {} can be properly mined with {} - drops/exp enabled! Block hardness: {}, Original tool hardness: {}, Effective tool hardness: {}",
                    blockName, toolName, blockHardness, toolHardness, effectiveToolHardness);
        } else if (miningMode == 2) {
            // 缓慢挖掘，无掉落物
            LOGGER.info("[BetterExcavate] Block {} can be slowly mined with {} - no drops/exp! Block hardness: {}, Original tool hardness: {}, Effective tool hardness: {}",
                    blockName, toolName, blockHardness, toolHardness, effectiveToolHardness);
            
            // 直接清除掉落物和经验值
            try {
                // 尝试使用反射来访问drops列表
                java.lang.reflect.Field dropsField = event.getClass().getDeclaredField("drops");
                dropsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<net.minecraft.world.item.ItemStack> drops = 
                    (java.util.List<net.minecraft.world.item.ItemStack>) dropsField.get(event);
                drops.clear();
                
                // 清除经验值
                java.lang.reflect.Field expField = event.getClass().getDeclaredField("expToDrop");
                expField.setAccessible(true);
                expField.setInt(event, 0);
                
                LOGGER.info("[BetterExcavate] Successfully cleared drops and experience for slow mining");
            } catch (Exception e) {
                LOGGER.warn("[BetterExcavate] Could not clear drops for slow mining: {}", e.getMessage());
            }
        }
    }
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // 跟踪玩家开始挖掘方块
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        
        if (player != null && pos != null) {
            String playerUUID = player.getUUID().toString();
            playerMiningBlocks.put(playerUUID, pos);
            lastMiningActivity.put(playerUUID, System.currentTimeMillis());
            playerActivelyMining.put(playerUUID, true);
            
            LOGGER.debug("[BetterExcavate] Player {} started mining block at {}", playerUUID, pos);
        }
    }
    
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        // 玩家左键点击空气，停止挖掘
        // 注意：这个事件在某些情况下可能会误触发，所以增加额外检查
        Player player = event.getEntity();
        if (player != null) {
            String playerUUID = player.getUUID().toString();
            
            // 检查玩家当前是否真的瞄准空气
            HitResult hitResult = player.pick(5.0D, 0.0F, false);
            if (hitResult.getType() == HitResult.Type.MISS) {
                // 确实瞄准空气，停止挖掘
                playerActivelyMining.put(playerUUID, false);
                LOGGER.debug("[BetterExcavate] Player {} stopped mining - clicked empty air", playerUUID);
            }
        }
    }
    
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        // 玩家攻击实体，停止挖掘方块
        Player player = event.getEntity();
        if (player != null) {
            String playerUUID = player.getUUID().toString();
            playerActivelyMining.put(playerUUID, false);
        }
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
        String playerUUID = player.getUUID().toString();
        
        // 如果是创造模式，不处理
        if (player.isCreative()) {
            return;
        }
        
        // 在开始处理前检查挖掘状态是否被意外重置
        checkAndRestoreMiningState(player);
        
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
        
        // 如果方块硬度为-1（如基岩），设置为一个很大的值用于计算
        // 这样基岩也会被纳入工具损坏的计算中
        float effectiveHardness = (blockHardness < 0) ? 1000.0f : blockHardness;
        
        // 如果原始硬度为0，不处理（空气等）
        if (blockHardness == 0) {
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
        
        // 应用耐久度硬度惩罚
        double effectiveToolHardness = toolHardness;
        if (Config.enableDurabilityHardnessPenalty && !tool.isEmpty()) {
            double durabilityHardnessMultiplier = Config.calculateDurabilityPenalty(tool, Config.maxDurabilityHardnessPenalty);
            effectiveToolHardness = toolHardness * durabilityHardnessMultiplier;
        }
        
        // 检查是否可以挖掘
        double maxMineableHardness = effectiveToolHardness * Config.hardnessMultiplier;
        
        // 计算工具硬度与方块硬度的比值，用于判断是否应该损坏工具
        // 使用有效硬度进行计算，这样基岩等方块也会被计算
        // 这里使用有效工具硬度来计算比值，考虑耐久度惩罚
        double hardnessRatio = effectiveToolHardness / effectiveHardness;
        
        // 当工具硬度严重不足时损坏工具
        // 这包括完全无法破坏的方块（如基岩）和极难挖掘的方块
        if (hardnessRatio < Config.toolDamageHardnessThreshold) {
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
                // 在损坏工具前保存挖掘状态
                String playerUUID2 = player.getUUID().toString();
                Boolean wasMining = playerActivelyMining.get(playerUUID2);
                BlockPos miningPos = playerMiningBlocks.get(playerUUID2);
                Long lastActivity = lastMiningActivity.get(playerUUID2);
                
                // 检查工具是否即将完全损坏
                boolean willBreak = tool.getDamageValue() + 1 >= tool.getMaxDamage();
                
                // 如果工具即将完全损坏，直接设置为完全损坏状态
                if (willBreak) {
                    // 设置耐久度为最大值（完全损坏）
                    tool.setDamageValue(tool.getMaxDamage());
                    // 清理所有相关记录
                    miningStartTimes.remove(playerKey);
                    lastDamageTime.remove(playerKey);
                    cleanupAllMiningRecords(player);
                    
                    String blockName = state.getBlock().getDescriptionId();
                    LOGGER.info("[BetterExcavate] Tool {} completely broken while mining {} - tool destroyed!", 
                               toolName, blockName);
                    return; // 工具已损坏，直接返回
                } else {
                    // 正常损坏工具1点耐久
                    tool.hurt(1, player.getRandom(), null);
                }
                
                lastDamageTime.put(playerKey, currentTime);
                
                // 恢复挖掘状态（防止工具损坏导致状态重置）
                if (wasMining != null && wasMining && miningPos != null) {
                    playerActivelyMining.put(playerUUID2, true);
                    playerMiningBlocks.put(playerUUID2, miningPos);
                    if (lastActivity != null) {
                        lastMiningActivity.put(playerUUID2, lastActivity);
                    }
                    LOGGER.debug("[BetterExcavate] Restored mining state after tool damage for player {}", playerUUID2);
                }
                
                String blockName = state.getBlock().getDescriptionId();
                if (blockHardness < 0 || blockHardness > maxMineableHardness) {
                    LOGGER.info("[BetterExcavate] Tool {} damaged while trying to mine unbreakable {} (hardness: {}, ratio: {}) - durability: {}/{}", 
                               toolName, blockName, blockHardness < 0 ? "indestructible" : String.format("%.2f", blockHardness), 
                               String.format("%.3f", hardnessRatio), tool.getMaxDamage() - tool.getDamageValue(), tool.getMaxDamage());
                } else {
                    LOGGER.info("[BetterExcavate] Tool {} damaged while slowly mining {} (hardness: {}, ratio: {}) - durability: {}/{}", 
                               toolName, blockName, String.format("%.2f", blockHardness), 
                               String.format("%.3f", hardnessRatio), tool.getMaxDamage() - tool.getDamageValue(), tool.getMaxDamage());
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
        String playerUUID = player.getUUID().toString();
        
        // 首先检查玩家是否被标记为正在挖掘
        Boolean activelyMining = playerActivelyMining.get(playerUUID);
        if (activelyMining == null || !activelyMining) {
            return false;
        }
        
        // 检查玩家是否有记录的挖掘活动
        Long lastActivity = lastMiningActivity.get(playerUUID);
        if (lastActivity == null) {
            return false;
        }
        
        // 如果最后的挖掘活动超过3秒前，认为已经停止挖掘
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActivity > 3000) {
            playerMiningBlocks.remove(playerUUID);
            lastMiningActivity.remove(playerUUID);
            playerActivelyMining.put(playerUUID, false);
            LOGGER.debug("[BetterExcavate] Player {} stopped mining - timeout (3s)", playerUUID);
            return false;
        }
        
        // 检查玩家当前瞄准的方块是否与记录的挖掘方块一致
        HitResult hitResult = player.pick(5.0D, 0.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            // 如果玩家没有瞄准方块，停止挖掘
            playerActivelyMining.put(playerUUID, false);
            LOGGER.debug("[BetterExcavate] Player {} stopped mining - not targeting block", playerUUID);
            return false;
        }
        
        BlockPos currentPos = blockHitResult.getBlockPos();
        BlockPos miningPos = playerMiningBlocks.get(playerUUID);
        
        // 只有当玩家仍在瞄准同一个方块时才认为在挖掘
        if (miningPos == null || !currentPos.equals(miningPos)) {
            // 如果玩家瞄准了不同的方块，停止当前挖掘
            playerActivelyMining.put(playerUUID, false);
            LOGGER.debug("[BetterExcavate] Player {} stopped mining - targeting different block", playerUUID);
            return false;
        }
        
        // 检查玩家的攻击冷却时间来判断是否真的在挖掘
        // 如果攻击冷却时间接近满值，说明玩家可能停止了挖掘
        // 但这个检测可能过于敏感，所以放宽条件
        float attackCooldown = player.getAttackStrengthScale(0.0F);
        if (attackCooldown > 0.99F) {
            // 攻击冷却时间几乎满了，说明玩家可能停止了挖掘
            // 给一个更长的宽限期，因为某些方块挖掘时攻击冷却可能会短暂恢复
            if (currentTime - lastActivity > 1000) { // 1秒宽限期
                playerActivelyMining.put(playerUUID, false);
                LOGGER.debug("[BetterExcavate] Player {} stopped mining - attack cooldown full for too long", playerUUID);
                return false;
            }
        } else {
            // 如果攻击冷却时间不满，说明玩家可能正在挖掘，更新活动时间
            lastMiningActivity.put(playerUUID, currentTime);
        }
        
        // 更新最后活动时间
        lastMiningActivity.put(playerUUID, currentTime);
        return true;
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
        playerMiningBlocks.remove(playerUUID);
        lastMiningActivity.remove(playerUUID);
        playerActivelyMining.remove(playerUUID);
        previousMiningState.remove(playerUUID);
        previousMiningPos.remove(playerUUID);
    }
    
    /**
     * 检查并恢复可能被意外重置的挖掘状态
     */
    private static void checkAndRestoreMiningState(Player player) {
        String playerUUID = player.getUUID().toString();
        
        // 获取当前状态
        Boolean currentMining = playerActivelyMining.get(playerUUID);
        BlockPos currentPos = playerMiningBlocks.get(playerUUID);
        
        // 获取上一tick的状态
        Boolean previousMining = previousMiningState.get(playerUUID);
        BlockPos previousPos = previousMiningPos.get(playerUUID);
        
        // 如果上一tick在挖掘，但现在状态丢失了，尝试恢复
        if (previousMining != null && previousMining && previousPos != null) {
            // 检查玩家是否仍在瞄准同一个方块
            HitResult hitResult = player.pick(5.0D, 0.0F, false);
            if (hitResult instanceof BlockHitResult blockHitResult) {
                BlockPos targetPos = blockHitResult.getBlockPos();
                
                // 如果玩家仍在瞄准同一个方块，但状态丢失了，恢复状态
                if (targetPos.equals(previousPos) && 
                    (currentMining == null || !currentMining || 
                     currentPos == null || !currentPos.equals(targetPos))) {
                    
                    // 检查攻击冷却来确认玩家可能仍在挖掘
                    float attackCooldown = player.getAttackStrengthScale(0.0F);
                    if (attackCooldown < 0.95F) { // 如果攻击冷却不满，说明可能在挖掘
                        // 恢复挖掘状态
                        playerActivelyMining.put(playerUUID, true);
                        playerMiningBlocks.put(playerUUID, targetPos);
                        lastMiningActivity.put(playerUUID, System.currentTimeMillis());
                        
                        LOGGER.debug("[BetterExcavate] Restored mining state for player {} - was mining {} but state was lost", 
                                   playerUUID, targetPos);
                    }
                }
            }
        }
        
        // 保存当前状态用于下一tick检查
        if (currentMining != null && currentMining && currentPos != null) {
            previousMiningState.put(playerUUID, true);
            previousMiningPos.put(playerUUID, currentPos);
        } else {
            previousMiningState.put(playerUUID, false);
            previousMiningPos.remove(playerUUID);
        }
    }
}
