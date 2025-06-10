package com.enhancedzombies.mod.legion;

import com.enhancedzombies.mod.config.EnhancedZombiesConfig;
import com.enhancedzombies.mod.entity.EnhancedZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 僵尸军团管理器
 * 功能：管理所有僵尸军团的生成、协调和状态
 * 职责：
 * 1. 控制军团生成时机和位置
 * 2. 管理军团成员和军团长
 * 3. 协调多个军团之间的行为
 * 4. 处理军团的生命周期
 */
@Mod.EventBusSubscriber
public class ZombieLegionManager {
    
    private static ZombieLegionManager INSTANCE;
    
    // 军团数据存储
    private final Map<Integer, ZombieLegion> 活跃军团 = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> 僵尸军团映射 = new ConcurrentHashMap<>();
    private final Map<UUID, Long> 玩家上次被攻击时间 = new ConcurrentHashMap<>();
    
    // 生成控制
    private int 下次军团ID = 1;
    private long 上次生成时间 = 0;
    private final Random 随机数生成器 = new Random();
    
    // 预警系统
    private final Map<UUID, Long> 玩家预警时间 = new ConcurrentHashMap<>();
    private final Map<UUID, BlockPos> 玩家预警位置 = new ConcurrentHashMap<>();
    private static final long 预警时间间隔 = 30000; // 30秒预警
    
    // 性能优化
    private int tick计数器 = 0;
    private static final int 更新间隔 = 20; // 每秒更新一次
    
    private ZombieLegionManager() {}
    
    public static ZombieLegionManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ZombieLegionManager();
        }
        return INSTANCE;
    }
    
    /**
     * 主要更新逻辑
     * 时间复杂度：O(n) - n为活跃军团数量
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        ZombieLegionManager manager = getInstance();
        manager.tick计数器++;
        
        // 每秒更新一次以优化性能
        if (manager.tick计数器 % 更新间隔 == 0) {
            manager.更新军团状态();
            manager.检查预警系统();
            manager.检查生成新军团();
            manager.清理无效军团();
        }
    }
    
    /**
     * 更新所有军团状态
     */
    private void 更新军团状态() {
        for (ZombieLegion 军团 : 活跃军团.values()) {
            军团.更新状态();
        }
    }
    
    /**
     * 检查是否需要生成新军团
     * 算法：基于时间间隔、玩家状态、概率等多重条件判断
     * 时间复杂度：O(n) - n为在线玩家数量
     */
    private void 检查生成新军团() {
        long 当前时间 = System.currentTimeMillis();
        
        // 开局保护：服务器启动后5分钟内不生成军团
        MinecraftServer 服务器 = ServerLifecycleHooks.getCurrentServer();
        if (服务器 != null) {
            long 服务器运行时间 = 服务器.getTickCount() * 50; // tick转毫秒
            if (服务器运行时间 < 300000) { // 5分钟 = 300000毫秒
                if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                    com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 开局保护期内，跳过生成 (剩余: {}秒)", 
                        (300000 - 服务器运行时间) / 1000);
                }
                return;
            }
        }
        
        // 计算随机生成间隔
        int 最小间隔 = EnhancedZombiesConfig.军团生成最小间隔秒数.get() * 1000;
        int 最大间隔 = EnhancedZombiesConfig.军团生成最大间隔秒数.get() * 1000;
        int 生成间隔 = 最小间隔 + 随机数生成器.nextInt(最大间隔 - 最小间隔 + 1);
        
        // 开发模式下输出调试信息
        if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 检查生成条件 - 当前时间: {}, 上次生成: {}, 需要间隔: {}ms", 
                当前时间, 上次生成时间, 生成间隔);
        }
        
        // 检查时间间隔
        if (当前时间 - 上次生成时间 < 生成间隔) {
            if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 时间间隔不足，跳过生成");
            }
            return;
        }
        
        // 检查军团数量限制
        if (活跃军团.size() >= 10) { // 临时硬编码，后续可配置
            if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 活跃军团数量已达上限: {}/10", 活跃军团.size());
            }
            return;
        }
        
        // 寻找合适的玩家目标
        List<Player> 在线玩家 = 获取在线玩家();
        if (在线玩家.isEmpty()) {
            if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 没有在线玩家，跳过生成");
            }
            return;
        }
        
        // 随机选择一个玩家作为目标
        Player 目标玩家 = 在线玩家.get(随机数生成器.nextInt(在线玩家.size()));
        
        if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 选择目标玩家: {}", 目标玩家.getName().getString());
        }
        
        // 检查该玩家是否最近被攻击过
        Long 上次攻击时间 = 玩家上次被攻击时间.get(目标玩家.getUUID());
        if (上次攻击时间 != null && 当前时间 - 上次攻击时间 < 300000) { // 5分钟冷却
            if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 玩家 {} 最近被攻击过，跳过生成", 目标玩家.getName().getString());
            }
            return;
        }
        
        // 检查该玩家是否已有活跃军团正在攻击
        boolean 已有军团攻击 = false;
        for (ZombieLegion 军团 : 活跃军团.values()) {
            if (军团.get目标玩家UUID().equals(目标玩家.getUUID())) {
                // 检查军团是否还有活跃成员
                if (!军团.get成员列表().isEmpty()) {
                    已有军团攻击 = true;
                    break;
                }
            }
        }
        
        if (已有军团攻击) {
            if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 玩家 {} 已有活跃军团攻击，跳过生成", 目标玩家.getName().getString());
            }
            return;
        }
        
        // 检查生成概率
        double 随机值 = Math.random();
        double 生成概率 = EnhancedZombiesConfig.军团生成概率.get();
        if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 概率检查 - 随机值: {}, 需要概率: {}", 随机值, 生成概率);
        }
        if (随机值 > 生成概率) {
            if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 概率检查失败，跳过生成");
            }
            return;
        }
        
        // 检查是否已有预警
        Long 预警时间 = 玩家预警时间.get(目标玩家.getUUID());
        if (预警时间 != null) {
            if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 玩家 {} 已有预警，跳过生成", 目标玩家.getName().getString());
            }
            return;
        }
        
        // 启动预警系统
        if (目标玩家 instanceof ServerPlayer 服务器玩家) {
            启动军团预警(服务器玩家);
            上次生成时间 = 当前时间; // 更新生成时间以避免频繁预警
        } else {
            if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.warn("[军团系统] 目标玩家不是ServerPlayer类型，无法启动预警");
            }
        }
    }
    
    /**
     * 生成新的僵尸军团
     * 算法：在玩家周围寻找合适的生成位置，创建军团并生成僵尸
     */
    private boolean 生成军团(Player 目标玩家) {
        if (!(目标玩家.level() instanceof ServerLevel 服务器世界)) {
            return false;
        }
        
        // 检查时间条件（非开发模式下只在夜晚生成）
        if (!com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
            long 世界时间 = 服务器世界.getDayTime() % 24000;
            if (世界时间 >= 0 && 世界时间 < 13000) { // 白天时间 0-13000
                return false;
            }
        }
        
        // 寻找生成位置
        BlockPos 生成位置 = 寻找军团生成位置(目标玩家, 服务器世界);
        if (生成位置 == null) {
            return false;
        }
        
        // 创建新军团
        int 军团ID = 下次军团ID++;
        int 军团大小 = EnhancedZombiesConfig.get随机军团大小();
        
        ZombieLegion 新军团 = new ZombieLegion(军团ID, 目标玩家.getUUID(), 生成位置);
        活跃军团.put(军团ID, 新军团);
        
        // 生成军团成员
        return 生成军团成员(新军团, 服务器世界, 生成位置, 军团大小);
    }
    
    /**
     * 生成军团成员
     * 时间复杂度：O(n) - n为军团大小
     */
    private boolean 生成军团成员(ZombieLegion 军团, ServerLevel 世界, BlockPos 中心位置, int 数量) {
        // 向附近玩家发送军团来袭警告
        发送军团来袭警告(世界, 中心位置, 军团.get目标玩家UUID());
        
        int 成功生成数量 = 0;
        boolean 已生成军团长 = false;
        
        for (int i = 0; i < 数量; i++) {
            // 在中心位置周围随机选择生成点
            BlockPos 生成点 = 中心位置.offset(
                随机数生成器.nextInt(10) - 5,
                0,
                随机数生成器.nextInt(10) - 5
            );
            
            // 调整Y坐标到地面
            生成点 = 智能调整到地面高度(世界, 生成点);
            if (生成点 == null) continue;
            
            // 创建增强僵尸
            EnhancedZombie 僵尸 = new EnhancedZombie(com.enhancedzombies.mod.EnhancedZombiesMod.ENHANCED_ZOMBIE.get(), 世界);
            僵尸.moveTo(生成点.getX() + 0.5, 生成点.getY(), 生成点.getZ() + 0.5, 0, 0);
            
            // 初始化僵尸装备和属性
            僵尸.finalizeSpawn(世界, 世界.getCurrentDifficultyAt(生成点), MobSpawnType.NATURAL, null, null);
            
            // 设置军团信息
            僵尸.set军团ID(军团.get军团ID());
            
            // 第一个僵尸设为军团长
            if (!已生成军团长) {
                僵尸.设置为军团长(); // 使用专门的方法设置军团长
                军团.set军团长UUID(僵尸.getUUID());
                已生成军团长 = true;
            }
            
            // 设置目标玩家
            Player 目标玩家 = 世界.getPlayerByUUID(军团.get目标玩家UUID());
            if (目标玩家 != null) {
                僵尸.setTarget(目标玩家);
            }
            
            // 生成到世界
            if (世界.addFreshEntity(僵尸)) {
                军团.添加成员(僵尸.getUUID());
                僵尸军团映射.put(僵尸.getUUID(), 军团.get军团ID());
                成功生成数量++;
            }
        }
        
        // 如果生成的僵尸太少，删除军团
        if (成功生成数量 < 数量 / 2) {
            活跃军团.remove(军团.get军团ID());
            return false;
        }
        
        return true;
    }
    
    /**
     * 寻找合适的军团生成位置
     * 算法：在玩家周围寻找平坦且安全的区域，支持高度变化和地形适应
     * 时间复杂度：O(n*m) - n为搜索半径内的位置数量，m为高度搜索范围
     */
    private BlockPos 寻找军团生成位置(Player 玩家, ServerLevel 世界) {
        BlockPos 玩家位置 = 玩家.blockPosition();
        int 最小距离 = 60; // 确保最小40格距离，避免与玩家重叠
        int 最大距离 = 80; // 增加最大距离到80格
        int 尝试次数 = 80; // 增加尝试次数以提高成功率
        int 高度搜索范围 = 10; // 玩家高度±10格的搜索范围
        
        for (int i = 0; i < 尝试次数; i++) {
            // 在环形区域内随机选择位置，确保最小距离
            double 角度 = 随机数生成器.nextDouble() * 2 * Math.PI;
            double 距离 = 最小距离 + 随机数生成器.nextDouble() * (最大距离 - 最小距离);
            
            int x = (int) (玩家位置.getX() + Math.cos(角度) * 距离);
            int z = (int) (玩家位置.getZ() + Math.sin(角度) * 距离);
            
            // 在玩家高度±10格范围内随机选择初始高度
            int 随机高度偏移 = 随机数生成器.nextInt(高度搜索范围 * 2 + 1) - 高度搜索范围;
            int 初始高度 = Math.max(世界.getMinBuildHeight() + 5, 
                                Math.min(世界.getMaxBuildHeight() - 5, 
                                       玩家位置.getY() + 随机高度偏移));
            
            BlockPos 候选位置 = new BlockPos(x, 初始高度, z);
            
            // 检查位置是否合适（包含地形适应）
            BlockPos 最终位置 = 检查并调整生成位置(世界, 候选位置, 玩家位置);
            if (最终位置 != null) {
                // 额外检查：确保与玩家有足够的距离差异
                double 实际距离 = Math.sqrt(Math.pow(最终位置.getX() - 玩家位置.getX(), 2) + 
                                        Math.pow(最终位置.getZ() - 玩家位置.getZ(), 2));
                if (实际距离 >= 最小距离) {
                    return 最终位置;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查并调整生成位置，支持地形适应
     * 算法：先尝试调整到合适的地面高度，再检查周围区域的平坦度
     */
    private BlockPos 检查并调整生成位置(ServerLevel 世界, BlockPos 候选位置, BlockPos 玩家位置) {
        // 首先调整到合适的地面高度
        BlockPos 地面位置 = 智能调整到地面高度(世界, 候选位置);
        if (地面位置 == null) {
            return null;
        }
        
        // 检查高度差是否在合理范围内（相对于玩家）
        int 高度差 = Math.abs(地面位置.getY() - 玩家位置.getY());
        if (高度差 > 15) { // 允许更大的高度差，但不要太极端
            return null;
        }
        
        // 检查周围3x3区域的可用性
        int 可用位置数 = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos 检查位置 = 地面位置.offset(dx, 0, dz);
                
                // 检查该位置是否适合生成
                if (检查单个位置是否适合生成(世界, 检查位置, 地面位置)) {
                    可用位置数++;
                }
            }
        }
        
        // 至少需要5个可用位置（3x3区域的一半以上）
        return 可用位置数 >= 5 ? 地面位置 : null;
    }
    
    /**
     * 检查单个位置是否适合僵尸生成
     */
    private boolean 检查单个位置是否适合生成(ServerLevel 世界, BlockPos 检查位置, BlockPos 基准位置) {
        // 检查高度差（允许小幅度的地形起伏）
        int 高度差 = Math.abs(检查位置.getY() - 基准位置.getY());
        if (高度差 > 3) {
            return false;
        }
        
        // 检查是否有足够的垂直空间（2格高度）
        if (!世界.getBlockState(检查位置.above()).isAir() || 
            !世界.getBlockState(检查位置.above(2)).isAir()) {
            return false;
        }
        
        // 检查脚下是否有实体方块
        if (世界.getBlockState(检查位置).isAir()) {
            return false;
        }
        
        // 检查是否在危险方块上（岩浆、仙人掌等）
         net.minecraft.world.level.block.Block 方块类型 = 世界.getBlockState(检查位置).getBlock();
         if (方块类型 instanceof net.minecraft.world.level.block.CactusBlock ||
             方块类型 instanceof net.minecraft.world.level.block.MagmaBlock ||
             方块类型.toString().contains("lava")) {
             return false;
         }
        
        return true;
    }
    
    /**
     * 智能调整位置到合适的地面高度
     * 算法：优先向下搜索，然后向上搜索，支持多种地形类型
     */
    private BlockPos 智能调整到地面高度(ServerLevel 世界, BlockPos 位置) {
        int 搜索范围 = 20; // 增加搜索范围
        
        // 向下搜索地面（优先）
        for (int y = 位置.getY(); y > Math.max(世界.getMinBuildHeight(), 位置.getY() - 搜索范围); y--) {
            BlockPos 检查位置 = new BlockPos(位置.getX(), y, 位置.getZ());
            if (是否为合适的地面位置(世界, 检查位置)) {
                return 检查位置.above();
            }
        }
        
        // 向上搜索地面
        for (int y = 位置.getY() + 1; y < Math.min(世界.getMaxBuildHeight() - 2, 位置.getY() + 搜索范围); y++) {
            BlockPos 检查位置 = new BlockPos(位置.getX(), y, 位置.getZ());
            if (是否为合适的地面位置(世界, 检查位置)) {
                return 检查位置.above();
            }
        }
        
        return null;
    }
    
    /**
     * 判断是否为合适的地面位置
     */
    private boolean 是否为合适的地面位置(ServerLevel 世界, BlockPos 位置) {
        // 检查当前位置是否为实体方块
        if (世界.getBlockState(位置).isAir()) {
            return false;
        }
        
        // 检查上方是否有足够空间（2格高度）
        if (!世界.getBlockState(位置.above()).isAir() || 
            !世界.getBlockState(位置.above(2)).isAir()) {
            return false;
        }
        
        // 检查是否为危险方块
         net.minecraft.world.level.block.Block 危险方块 = 世界.getBlockState(位置).getBlock();
         if (危险方块 instanceof net.minecraft.world.level.block.CactusBlock ||
             危险方块 instanceof net.minecraft.world.level.block.MagmaBlock ||
             危险方块.toString().contains("lava")) {
             return false;
         }
        
        // 检查是否为可站立的方块（排除一些特殊方块）
        net.minecraft.world.level.block.Block 方块 = 世界.getBlockState(位置).getBlock();
        if (方块 instanceof net.minecraft.world.level.block.FenceBlock ||
            方块 instanceof net.minecraft.world.level.block.WallBlock ||
            方块 instanceof net.minecraft.world.level.block.FenceGateBlock) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理无效军团（增强版：包含距离检查）
     */
    private void 清理无效军团() {
        Iterator<Map.Entry<Integer, ZombieLegion>> 迭代器 = 活跃军团.entrySet().iterator();
        
        while (迭代器.hasNext()) {
            Map.Entry<Integer, ZombieLegion> 条目 = 迭代器.next();
            ZombieLegion 军团 = 条目.getValue();
            
            boolean 应该清理 = false;
            String 清理原因 = "";
            
            // 检查军团是否应该被清理
            if (军团.应该被清理()) {
                应该清理 = true;
                清理原因 = "军团生命周期结束";
            } else {
                // 检查玩家距离
                应该清理 = 检查军团距离并决定清理(军团);
                if (应该清理) {
                    清理原因 = "玩家距离过远";
                }
            }
            
            if (应该清理) {
                // 清理僵尸军团映射
                for (UUID 僵尸UUID : 军团.get成员列表()) {
                    僵尸军团映射.remove(僵尸UUID);
                }
                
                if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                    com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info(
                        "[军团系统] 清理军团 {} - 原因: {}, 成员数量: {}", 
                        军团.get军团ID(), 清理原因, 军团.get成员列表().size()
                    );
                }
                
                迭代器.remove();
            }
        }
    }
    
    /**
     * 检查军团距离并决定是否清理
     * @param 军团 要检查的军团
     * @return 是否应该清理
     */
    private boolean 检查军团距离并决定清理(ZombieLegion 军团) {
        try {
            // 获取目标玩家
            MinecraftServer 服务器 = ServerLifecycleHooks.getCurrentServer();
            if (服务器 == null) return false;
            
            Player 目标玩家 = null;
            for (ServerLevel 世界 : 服务器.getAllLevels()) {
                目标玩家 = 世界.getPlayerByUUID(军团.get目标玩家UUID());
                if (目标玩家 != null) break;
            }
            
            // 如果目标玩家不在线，清理军团
            if (目标玩家 == null) {
                return true;
            }
            
            // 检查距离（128格，即8个区块）
            double 最大距离 = 128.0;
            double 当前距离 = 目标玩家.blockPosition().distSqr(军团.get初始生成位置());
            
            if (当前距离 > 最大距离 * 最大距离) {
                // 距离过远，但给予一定的宽限时间（5分钟）
                long 当前时间 = System.currentTimeMillis();
                long 军团存在时间 = 当前时间 - 军团.get创建时间();
                
                // 如果军团存在超过5分钟且距离过远，则清理
                if (军团存在时间 > 300000) { // 5分钟 = 300000毫秒
                    return true;
                }
            }
            
        } catch (Exception e) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.error("检查军团距离时出错: {}", e.getMessage());
            return false;
        }
        
        return false;
    }
    
    /**
     * 获取在线玩家列表
     */
    private List<Player> 获取在线玩家() {
        List<Player> 玩家列表 = new ArrayList<>();
        
        // 获取服务器实例
        MinecraftServer 服务器 = ServerLifecycleHooks.getCurrentServer();
        if (服务器 != null) {
            // 获取所有维度的玩家
            for (ServerLevel 世界 : 服务器.getAllLevels()) {
                玩家列表.addAll(世界.players());
            }
        }
        
        return 玩家列表;
    }
    
    /**
     * 获取僵尸所属的军团ID
     */
    public int 获取僵尸军团ID(UUID 僵尸UUID) {
        return 僵尸军团映射.getOrDefault(僵尸UUID, -1);
    }
    
    /**
     * 获取军团信息
     */
    public ZombieLegion 获取军团(int 军团ID) {
        return 活跃军团.get(军团ID);
    }
    
    /**
     * 移除僵尸
     */
    public void 移除僵尸(UUID 僵尸UUID) {
        Integer 军团ID = 僵尸军团映射.remove(僵尸UUID);
        if (军团ID != null) {
            ZombieLegion 军团 = 活跃军团.get(军团ID);
            if (军团 != null) {
                军团.移除成员(僵尸UUID);
            }
        }
    }
    
    /**
     * 获取活跃军团数量
     */
    public int 获取活跃军团数量() {
        return 活跃军团.size();
    }
    
    /**
     * 获取总僵尸数量
     */
    public int 获取总僵尸数量() {
        return 僵尸军团映射.size();
    }
    
    /**
     * 强制生成军团（用于命令系统）
     */
    public boolean 强制生成军团(Player 目标玩家) {
        if (目标玩家 == null) {
            return false;
        }
        
        if (活跃军团.size() >= 10) { // 临时硬编码，后续可配置
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.warn("[命令生成] 活跃军团数量已达上限，无法生成新军团");
            return false;
        }
        
        // 强制生成时跳过冷却和概率检查
        try {
            ServerLevel 世界 = (ServerLevel) 目标玩家.level();
            
            // 寻找合适的生成位置
            BlockPos 生成位置 = 寻找军团生成位置(目标玩家, 世界);
            if (生成位置 == null) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.warn("[命令生成] 找不到合适的生成位置，玩家: {}", 目标玩家.getName().getString());
                return false;
            }
            
            // 创建新军团
            int 军团ID = 下次军团ID++;
            ZombieLegion 新军团 = new ZombieLegion(军团ID, 目标玩家.getUUID(), 生成位置);
            
            // 生成默认数量的僵尸（使用配置范围）
            int 军团大小 = com.enhancedzombies.mod.config.EnhancedZombiesConfig.get随机军团大小();
            boolean 生成成功 = 生成军团成员(新军团, 世界, 生成位置, 军团大小);
            
            if (生成成功) {
                活跃军团.put(军团ID, 新军团);
                
                // 开发模式下输出调试信息
                if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                    com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info(
                        "[命令生成] 为玩家 {} 生成了 {} 个僵尸的军团，位置: {}", 
                        目标玩家.getName().getString(), 新军团.get成员列表().size(), 生成位置
                    );
                }
                
                return true;
            } else {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.warn("[命令生成] 生成军团成员失败，玩家: {}", 目标玩家.getName().getString());
            }
            
        } catch (Exception e) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.error("强制生成军团时出错: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * 强制生成指定大小的军团（用于命令系统）
     */
    public boolean 强制生成指定大小军团(Player 目标玩家, int 军团大小) {
        if (目标玩家 == null || 军团大小 <= 0) {
            return false;
        }
        
        if (活跃军团.size() >= 10) { // 临时硬编码，后续可配置
            return false;
        }
        
        try {
            ServerLevel 世界 = (ServerLevel) 目标玩家.level();
            BlockPos 玩家位置 = 目标玩家.blockPosition();
            
            // 寻找合适的生成位置
            BlockPos 生成位置 = 寻找军团生成位置(目标玩家, 世界);
            if (生成位置 == null) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.warn("[命令生成] 找不到合适的生成位置，玩家: {}", 目标玩家.getName().getString());
                return false;
            }
            
            // 创建新军团
            int 军团ID = 下次军团ID++;
            ZombieLegion 新军团 = new ZombieLegion(军团ID, 目标玩家.getUUID(), 生成位置);
            
            // 修复：直接生成指定数量的僵尸，而不是循环调用生成军团成员
            boolean 生成成功 = 生成军团成员(新军团, 世界, 生成位置, 军团大小);
            
            if (生成成功) {
                活跃军团.put(军团ID, 新军团);
                
                // 开发模式下输出调试信息
                if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                    com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info(
                        "[命令生成] 为玩家 {} 生成了 {} 个僵尸的军团，位置: {}", 
                        目标玩家.getName().getString(), 新军团.get成员列表().size(), 生成位置
                    );
                }
                
                return true;
            } else {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.warn("[命令生成] 生成军团成员失败，玩家: {}", 目标玩家.getName().getString());
            }
            
        } catch (Exception e) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.error("强制生成指定大小军团时出错: {}", e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * 清除所有活跃军团（用于命令系统）
     */
    public int 清除所有活跃军团(ServerLevel 世界) {
        int 清除数量 = 0;
        
        try {
            // 移除所有军团中的僵尸实体
            for (ZombieLegion 军团 : 活跃军团.values()) {
                for (UUID 僵尸UUID : 军团.get成员列表()) {
                    Entity 实体 = 世界.getEntity(僵尸UUID);
                    if (实体 != null) {
                        实体.remove(Entity.RemovalReason.DISCARDED);
                        清除数量++;
                    }
                }
            }
            
            // 清理数据结构
            清理所有军团();
            
            // 开发模式下输出调试信息
            if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info(
                    "[命令清除] 清除了 {} 个僵尸实体和所有军团数据", 清除数量
                );
            }
            
        } catch (Exception e) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.error("清除所有活跃军团时出错: {}", e.getMessage());
        }
        
        return 清除数量;
    }
    
    /**
     * 清理所有军团（用于重置或调试）
     */
    public void 清理所有军团() {
        活跃军团.clear();
        僵尸军团映射.clear();
        玩家上次被攻击时间.clear();
        下次军团ID = 1;
    }
    
    /**
     * 获取军团统计信息
     */
    public String 获取统计信息() {
        StringBuilder 信息 = new StringBuilder();
        信息.append("活跃军团数量: ").append(活跃军团.size()).append("\n");
        信息.append("总僵尸数量: ").append(僵尸军团映射.size()).append("\n");
        
        for (ZombieLegion 军团 : 活跃军团.values()) {
            信息.append("军团 ").append(军团.get军团ID())
                .append(": ").append(军团.get成员数量()).append(" 成员\n");
        }
        
        return 信息.toString();
    }
    
    /**
     * 获取僵尸所属的军团信息
     * @param 僵尸实体 要查询的僵尸
     * @return 军团信息，如果不属于任何军团则返回null
     */
    public ZombieLegion 获取僵尸军团信息(EnhancedZombie 僵尸实体) {
        Integer 军团ID = 僵尸军团映射.get(僵尸实体.getUUID());
        if (军团ID != null) {
            return 活跃军团.get(军团ID);
        }
        return null;
    }
    
    /**
     * 启动军团预警系统
     * @param 目标玩家 即将被攻击的玩家
     */
    private void 启动军团预警(ServerPlayer 目标玩家) {
        long 当前时间 = System.currentTimeMillis();
        BlockPos 玩家位置 = 目标玩家.blockPosition();
        
        // 记录预警信息
        玩家预警时间.put(目标玩家.getUUID(), 当前时间);
        玩家预警位置.put(目标玩家.getUUID(), 玩家位置);
        
        // 发送预警消息
        发送军团预警消息(目标玩家, 玩家位置.getX(), 玩家位置.getZ());
        
        if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 已启动预警，目标玩家: {}，位置: ({}, {}, {})", 
                目标玩家.getName().getString(), 玩家位置.getX(), 玩家位置.getY(), 玩家位置.getZ());
        }
    }
    
    /**
     * 检查预警系统并处理到期的预警
     */
    private void 检查预警系统() {
        long 当前时间 = System.currentTimeMillis();
        
        // 检查所有预警
        玩家预警时间.entrySet().removeIf(entry -> {
            UUID 玩家UUID = entry.getKey();
            long 预警时间 = entry.getValue();
            
            // 检查预警是否到期（30秒）
            if (当前时间 - 预警时间 >= 预警时间间隔) {
                // 获取玩家
                MinecraftServer 服务器 = ServerLifecycleHooks.getCurrentServer();
                if (服务器 != null) {
                    ServerPlayer 玩家 = 服务器.getPlayerList().getPlayer(玩家UUID);
                    
                    if (玩家 != null && 玩家.isAlive()) {
                        // 获取预警位置
                        BlockPos 预警位置 = 玩家预警位置.get(玩家UUID);
                        if (预警位置 != null) {
                            // 生成军团
                            if (生成军团(玩家)) {
                                玩家上次被攻击时间.put(玩家UUID, 当前时间);
                                
                                if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                                    com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 预警到期，军团生成成功！目标玩家: {}，活跃军团数量: {}", 
                                        玩家.getName().getString(), 活跃军团.size());
                                }
                            } else {
                                if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                                    com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.info("[军团系统] 预警到期，但军团生成失败！目标玩家: {}", 
                                        玩家.getName().getString());
                                }
                            }
                        }
                    }
                }
                
                // 清理预警位置
                玩家预警位置.remove(玩家UUID);
                return true; // 移除此预警
            }
            
            return false; // 保留此预警
        });
    }
    
    /**
     * 发送军团预警消息
     * @param 目标玩家 目标玩家
     * @param x X坐标
     * @param z Z坐标
     */
    private void 发送军团预警消息(ServerPlayer 目标玩家, int x, int z) {
        try {
            // 发送预警消息
            目标玩家.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§e§l⚠ 僵尸军团预警！ ⚠"), 
                true // 显示在屏幕上方
            );
            
            目标玩家.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "§e[增强僵尸] §f检测到僵尸军团活动，30秒后可能遭遇攻击！当前位置: §c" + 
                    x + ", " + z
                )
            );
            
        } catch (Exception e) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.error("发送军团预警时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 发送军团来袭警告消息
     * 向目标玩家和附近玩家发送警告
     */
    private void 发送军团来袭警告(ServerLevel 世界, BlockPos 生成位置, UUID 目标玩家UUID) {
        try {
            // 获取目标玩家
            Player 目标玩家 = 世界.getPlayerByUUID(目标玩家UUID);
            if (目标玩家 != null) {
                // 发送标题消息
                目标玩家.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§c§l⚠ 僵尸军团来袭！ ⚠"), 
                    true // 显示在屏幕上方
                );
                
                // 发送聊天消息
                目标玩家.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§c[增强僵尸] §f一支僵尸军团正在向你靠近！坐标: §e" + 
                        生成位置.getX() + ", " + 生成位置.getY() + ", " + 生成位置.getZ()
                    )
                );
                
                // 开发模式下显示额外信息
                if (com.enhancedzombies.mod.config.EnhancedZombiesConfig.是否为开发模式()) {
                    目标玩家.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§7[开发模式] §f军团生成时间: " + 
                            (世界.getDayTime() % 24000 < 13000 ? "白天" : "夜晚")
                        )
                    );
                }
            }
            
            // 向附近其他玩家发送警告
            List<Player> 附近玩家 = 世界.getEntitiesOfClass(
                Player.class, 
                new net.minecraft.world.phys.AABB(生成位置).inflate(100.0)
            );
            
            for (Player 玩家 : 附近玩家) {
                if (!玩家.getUUID().equals(目标玩家UUID)) {
                    玩家.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                            "§6[增强僵尸] §f附近出现了僵尸军团！"
                        )
                    );
                }
            }
            
        } catch (Exception e) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.error("发送军团警告时出错: {}", e.getMessage());
        }
    }
}