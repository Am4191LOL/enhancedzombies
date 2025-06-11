package com.enhancedzombies.mod.entity.ai;

import com.enhancedzombies.mod.config.EnhancedZombiesConfig;
import com.enhancedzombies.mod.entity.EnhancedZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 僵尸破坏方块AI目标
 * 功能：让僵尸能够智能地破坏阻挡路径的方块
 * 职责：
 * 1. 识别需要破坏的目标方块
 * 2. 执行破坏动作和动画
 * 3. 处理破坏后的掉落物
 * 4. 避免破坏重要结构方块
 */
public class ZombieBreakBlockGoal extends Goal {
    
    private final EnhancedZombie 僵尸;
    private final Level 世界;
    private BlockPos 目标方块位置;
    private int 破坏进度;
    private int 最大破坏时间;
    private static final int 基础破坏时间 = 60; // 3秒 (60 ticks)
    
    // 可破坏的方块类型
    private static final Block[] 可破坏方块 = {
        Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.JUNGLE_DOOR,
        Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.MANGROVE_DOOR, Blocks.CHERRY_DOOR,
        Blocks.BAMBOO_DOOR, Blocks.CRIMSON_DOOR, Blocks.WARPED_DOOR,
        Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE, Blocks.BIRCH_FENCE_GATE,
        Blocks.JUNGLE_FENCE_GATE, Blocks.ACACIA_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE,
        Blocks.MANGROVE_FENCE_GATE, Blocks.CHERRY_FENCE_GATE, Blocks.BAMBOO_FENCE_GATE,
        Blocks.CRIMSON_FENCE_GATE, Blocks.WARPED_FENCE_GATE,
        Blocks.GLASS, Blocks.GLASS_PANE, Blocks.WHITE_STAINED_GLASS,
        Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS,
        Blocks.COBBLESTONE, Blocks.STONE_BRICKS, Blocks.DIRT, Blocks.GRASS_BLOCK,
        Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES
    };
    
    // 不可破坏的重要方块
    private static final Block[] 禁止破坏方块 = {
        Blocks.BEDROCK, Blocks.BARRIER, Blocks.COMMAND_BLOCK,
        Blocks.STRUCTURE_BLOCK, Blocks.JIGSAW, Blocks.SPAWNER,
        Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR,
        Blocks.ANCIENT_DEBRIS, Blocks.NETHERITE_BLOCK
    };
    
    public ZombieBreakBlockGoal(EnhancedZombie 僵尸) {
        this.僵尸 = 僵尸;
        this.世界 = 僵尸.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }
    
    /**
     * 检查是否可以开始破坏方块
     * 时间复杂度：O(n) - n为搜索范围内的方块数量
     */
    @Override
    public boolean canUse() {
        // 检查配置和冷却
        if (!僵尸.可以破坏() || Math.random() > EnhancedZombiesConfig.方块破坏概率.get()) {
            return false;
        }
        
        // 只有在追击目标时才考虑破坏方块
        if (僵尸.getTarget() == null) {
            return false;
        }
        
        // 检查是否真的需要破坏方块来到达目标
        if (!需要破坏方块到达目标()) {
            return false;
        }
        
        // 寻找阻挡路径的方块
        目标方块位置 = 寻找阻挡方块();
        if (目标方块位置 == null) {
            return false;
        }
        
        // 检查方块是否可以破坏
        BlockState 方块状态 = 世界.getBlockState(目标方块位置);
        return 是否可破坏方块(方块状态.getBlock());
    }
    
    /**
     * 检查是否应该继续破坏
     */
    @Override
    public boolean canContinueToUse() {
        if (目标方块位置 == null) {
            return false;
        }
        
        // 检查方块是否还存在
        BlockState 方块状态 = 世界.getBlockState(目标方块位置);
        if (方块状态.isAir()) {
            return false;
        }
        
        // 检查距离
        double 距离 = 僵尸.blockPosition().distSqr(目标方块位置);
        return 距离 <= EnhancedZombiesConfig.最大破坏距离.get() * EnhancedZombiesConfig.最大破坏距离.get();
    }
    
    /**
     * 开始破坏行为
     */
    @Override
    public void start() {
        破坏进度 = 0;
        BlockState 方块状态 = 世界.getBlockState(目标方块位置);
        最大破坏时间 = 计算破坏时间(方块状态.getBlock());
        
        // 面向目标方块
        Vec3 目标位置 = Vec3.atCenterOf(目标方块位置);
        僵尸.getLookControl().setLookAt(目标位置.x, 目标位置.y, 目标位置.z);
        
        // 播放破坏开始音效
        世界.playSound(null, 目标方块位置, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 
                     SoundSource.HOSTILE, 0.5F, 0.8F + 世界.random.nextFloat() * 0.4F);
    }
    
    /**
     * 执行破坏逻辑
     */
    @Override
    public void tick() {
        if (目标方块位置 == null) {
            return;
        }
        
        // 移动到方块附近
        double 距离 = 僵尸.blockPosition().distSqr(目标方块位置);
        if (距离 > 4.0) { // 2格距离
            僵尸.getNavigation().moveTo(目标方块位置.getX(), 目标方块位置.getY(), 目标方块位置.getZ(), 1.0);
            return;
        }
        
        // 面向目标方块
        Vec3 目标位置 = Vec3.atCenterOf(目标方块位置);
        僵尸.getLookControl().setLookAt(目标位置.x, 目标位置.y, 目标位置.z);
        
        // 增加破坏进度
        破坏进度++;
        
        // 播放破坏音效和粒子效果
        if (破坏进度 % 10 == 0) {
            播放破坏效果();
        }
        
        // 检查是否完成破坏
        if (破坏进度 >= 最大破坏时间) {
            执行破坏();
        }
    }
    
    /**
     * 停止破坏行为
     */
    @Override
    public void stop() {
        目标方块位置 = null;
        破坏进度 = 0;
        僵尸.set破坏冷却(40); // 2秒冷却
    }
    
    /**
     * 寻找阻挡路径的方块
     * 算法：从僵尸位置向目标方向搜索阻挡方块
     * 时间复杂度：O(d) - d为最大破坏距离
     */
    private BlockPos 寻找阻挡方块() {
        if (僵尸.getTarget() == null) {
            return null;
        }
        
        Vec3 僵尸位置 = 僵尸.position();
        Vec3 目标位置 = 僵尸.getTarget().position();
        Vec3 方向 = 目标位置.subtract(僵尸位置).normalize();
        
        int 最大距离 = EnhancedZombiesConfig.最大破坏距离.get();
        
        // 沿着方向向量搜索阻挡方块
        for (int i = 1; i <= 最大距离; i++) {
            Vec3 检查位置 = 僵尸位置.add(方向.scale(i));
            BlockPos 方块位置 = new BlockPos((int)检查位置.x, (int)检查位置.y, (int)检查位置.z);
            
            // 检查僵尸眼部高度和脚部高度
            for (int y偏移 = 0; y偏移 <= 1; y偏移++) {
                BlockPos 检查方块 = 方块位置.offset(0, y偏移, 0);
                BlockState 方块状态 = 世界.getBlockState(检查方块);
                
                if (!方块状态.isAir() && 是否可破坏方块(方块状态.getBlock())) {
                    // 检查这个方块是否真的阻挡了路径
                    if (方块状态.isCollisionShapeFullBlock(世界, 检查方块)) {
                        return 检查方块;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查方块是否可以被破坏
     * 时间复杂度：O(n) - n为可破坏方块数组长度
     */
    private boolean 是否可破坏方块(Block 方块) {
        // 检查是否在禁止破坏列表中
        for (Block 禁止方块 : 禁止破坏方块) {
            if (方块 == 禁止方块) {
                return false;
            }
        }
        
        // 检查是否在可破坏列表中
        for (Block 可破坏 : 可破坏方块) {
            if (方块 == 可破坏) {
                return true;
            }
        }
        
        // 默认情况下，硬度较低的方块可以破坏
        float 硬度 = 方块.defaultBlockState().getDestroySpeed(世界, BlockPos.ZERO);
        return 硬度 >= 0 && 硬度 <= 3.0F;
    }
    
    /**
     * 计算破坏时间
     * 根据方块硬度和僵尸智能等级计算
     */
    private int 计算破坏时间(Block 方块) {
        float 硬度 = 方块.defaultBlockState().getDestroySpeed(世界, BlockPos.ZERO);
        int 智能等级 = 僵尸.get智能等级();
        
        // 基础时间根据硬度调整
        int 基础时间 = (int)(基础破坏时间 * (1.0F + 硬度 * 0.5F));
        
        // 智能等级越高，破坏越快
        int 调整时间 = 基础时间 - (智能等级 - 1) * 10;
        
        return Math.max(20, 调整时间); // 最少1秒
    }
    
    /**
     * 播放破坏效果
     */
    private void 播放破坏效果() {
        if (目标方块位置 == null) return;
        
        BlockState 方块状态 = 世界.getBlockState(目标方块位置);
        
        // 播放破坏音效
        世界.playSound(null, 目标方块位置, 方块状态.getSoundType().getHitSound(), 
                     SoundSource.BLOCKS, 0.3F, 0.8F + 世界.random.nextFloat() * 0.4F);
        
        // 在客户端显示破坏粒子效果
        if (世界.isClientSide) {
            // TODO: 添加粒子效果
        }
    }
    
    /**
     * 执行实际的方块破坏
     */
    private void 执行破坏() {
        if (目标方块位置 == null) return;
        
        BlockState 方块状态 = 世界.getBlockState(目标方块位置);
        
        // 播放破坏完成音效
        世界.playSound(null, 目标方块位置, 方块状态.getSoundType().getBreakSound(), 
                     SoundSource.BLOCKS, 1.0F, 0.8F + 世界.random.nextFloat() * 0.4F);
        
        // 生成掉落物（概率性）
        if (Math.random() < 0.3) {
            Block.dropResources(方块状态, 世界, 目标方块位置);
        }
        
        // 破坏方块
        世界.destroyBlock(目标方块位置, false);
        
        // 增加僵尸经验（智能等级）
        if (Math.random() < 0.1) {
            int 当前等级 = 僵尸.get智能等级();
            if (当前等级 < 5) {
                僵尸.set智能等级(当前等级 + 1);
            }
        }
    }
    
    /**
     * 检查是否需要破坏方块来到达目标
     * 算法：智能分析路径受阻情况和攻击需求
     * 优化：优先考虑路径规划失败和攻击视线阻挡
     */
    private boolean 需要破坏方块到达目标() {
        LivingEntity 目标 = 僵尸.getTarget();
        if (目标 == null) return false;
        
        Vec3 僵尸位置 = 僵尸.position();
        Vec3 目标位置 = 目标.position();
        double 距离 = 僵尸位置.distanceTo(目标位置);
        
        // 情况1：路径规划完全失败 - 优先级最高
        if (检查路径规划失败(目标位置)) {
            return true;
        }
        
        // 情况2：在攻击范围内但视线被阻挡 - 需要破坏阻挡方块进行攻击
        if (距离 <= 8.0 && 检查攻击视线阻挡(僵尸位置, 目标位置)) {
            return true;
        }
        
        // 情况3：路径存在但进展缓慢 - 可能有部分阻挡
        if (检查路径进展缓慢()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查路径规划是否完全失败
     * 算法：尝试寻路到目标，如果失败则需要破坏阻挡方块
     */
    private boolean 检查路径规划失败(Vec3 目标位置) {
        // 尝试计算到目标的路径
        boolean 路径成功 = 僵尸.getNavigation().moveTo(目标位置.x, 目标位置.y, 目标位置.z, 1.0);
        
        // 如果路径计算失败，说明被完全阻挡
        if (!路径成功) {
            return true;
        }
        
        // 检查当前路径是否为空或已完成但未到达目标
        var 当前路径 = 僵尸.getNavigation().getPath();
        if (当前路径 == null || 当前路径.isDone()) {
            double 到目标距离 = 僵尸.position().distanceTo(目标位置);
            // 如果路径完成但距离目标还很远，说明路径不完整
            return 到目标距离 > 3.0;
        }
        
        return false;
    }
    
    /**
     * 检查攻击视线是否被方块阻挡
     * 算法：检查僵尸与目标之间的直线路径上是否有可破坏的阻挡方块
     */
    private boolean 检查攻击视线阻挡(Vec3 起点, Vec3 终点) {
        Vec3 方向 = 终点.subtract(起点).normalize();
        double 总距离 = 起点.distanceTo(终点);
        
        // 沿着攻击视线检查阻挡方块
        for (double d = 1.0; d < 总距离 && d < 8.0; d += 0.5) {
            Vec3 检查点 = 起点.add(方向.scale(d));
            
            // 检查僵尸身体高度和眼部高度的方块
            for (int y偏移 = 0; y偏移 <= 1; y偏移++) {
                BlockPos 检查位置 = new BlockPos((int)检查点.x, (int)检查点.y + y偏移, (int)检查点.z);
                BlockState 方块状态 = 世界.getBlockState(检查位置);
                
                // 如果发现可破坏的阻挡方块，返回true
                if (!方块状态.isAir() && 
                    方块状态.isCollisionShapeFullBlock(世界, 检查位置) && 
                    是否可破坏方块(方块状态.getBlock())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查路径进展是否缓慢
     * 算法：监测僵尸在一定时间内的移动距离
     */
    private boolean 检查路径进展缓慢() {
        // 如果僵尸长时间停留在同一位置且有目标，可能需要破坏阻挡
        if (僵尸.getNavigation().isStuck()) {
            return true;
        }
        
        // 检查是否有路径但移动速度很慢
        var 当前路径 = 僵尸.getNavigation().getPath();
        if (当前路径 != null && !当前路径.isDone()) {
            // 如果路径存在但僵尸移动很慢，可能有部分阻挡
            double 移动速度 = 僵尸.getDeltaMovement().length();
            return 移动速度 < 0.1; // 移动速度过慢
        }
        
        return false;
    }
}