package com.enhancedzombies.mod.entity.ai;

import com.enhancedzombies.mod.config.EnhancedZombiesConfig;
import com.enhancedzombies.mod.entity.EnhancedZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 僵尸建造方块AI目标
 * 功能：让僵尸能够智能地建造简单结构
 * 职责：
 * 1. 识别适合建造的位置
 * 2. 选择合适的建造材料
 * 3. 执行建造动作和逻辑
 * 4. 创建防御性或攻击性结构
 */
public class ZombieBuildBlockGoal extends Goal {
    
    private final EnhancedZombie 僵尸;
    private final Level 世界;
    private BlockPos 建造位置;
    private Block 建造材料;
    private int 建造进度;
    private int 建造阶段; // 0: 寻找位置, 1: 准备材料, 2: 执行建造
    private static final int 建造时间 = 40; // 2秒
    
    // 可用的建造材料（按优先级排序）
    private static final Block[] 建造材料列表 = {
        Blocks.COBBLESTONE, Blocks.STONE, Blocks.DIRT, Blocks.OAK_PLANKS,
        Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.SAND, Blocks.GRAVEL
    };
    
    // 建造模式枚举
    private enum 建造模式 {
        防御墙,    // 在自己周围建造防御墙
        攻击平台,  // 建造攻击平台以获得高度优势
        陷阱,      // 建造简单陷阱
        桥梁       // 建造桥梁跨越障碍
    }
    
    private 建造模式 当前建造模式;
    
    public ZombieBuildBlockGoal(EnhancedZombie 僵尸) {
        this.僵尸 = 僵尸;
        this.世界 = 僵尸.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }
    
    /**
     * 检查是否可以开始建造
     * 时间复杂度：O(1)
     */
    @Override
    public boolean canUse() {
        // 检查配置和冷却
        if (!僵尸.可以建造() || Math.random() > EnhancedZombiesConfig.方块建造概率.get()) {
            return false;
        }
        
        // 智能等级至少为2才能建造
        if (僵尸.get智能等级() < 2) {
            return false;
        }
        
        // 只有在有明确目标且需要建造时才执行
        if (僵尸.getTarget() == null || !需要建造来攻击目标()) {
            return false;
        }
        
        // 检查是否有建造材料
        if (!检查建造材料()) {
            return false;
        }
        
        // 选择建造模式
        当前建造模式 = 选择建造模式();
        
        // 寻找合适的建造位置
        建造位置 = 寻找建造位置();
        return 建造位置 != null;
    }
    
    /**
     * 检查是否应该继续建造
     */
    @Override
    public boolean canContinueToUse() {
        if (建造位置 == null) {
            return false;
        }
        
        // 检查距离
        double 距离 = 僵尸.blockPosition().distSqr(建造位置);
        if (距离 > EnhancedZombiesConfig.最大建造距离.get() * EnhancedZombiesConfig.最大建造距离.get()) {
            return false;
        }
        
        // 检查位置是否仍然适合建造
        return 世界.getBlockState(建造位置).isAir();
    }
    
    /**
     * 开始建造行为
     */
    @Override
    public void start() {
        建造进度 = 0;
        建造阶段 = 0;
        建造材料 = 选择建造材料();
        
        // 面向建造位置
        Vec3 目标位置 = Vec3.atCenterOf(建造位置);
        僵尸.getLookControl().setLookAt(目标位置.x, 目标位置.y, 目标位置.z);
    }
    
    /**
     * 执行建造逻辑
     */
    @Override
    public void tick() {
        if (建造位置 == null) {
            return;
        }
        
        switch (建造阶段) {
            case 0: // 移动到建造位置
                移动到建造位置();
                break;
            case 1: // 准备建造
                准备建造();
                break;
            case 2: // 执行建造
                执行建造();
                break;
        }
    }
    
    /**
     * 停止建造行为
     */
    @Override
    public void stop() {
        建造位置 = null;
        建造材料 = null;
        建造进度 = 0;
        建造阶段 = 0;
        僵尸.set建造冷却(60); // 3秒冷却
    }
    
    /**
     * 移动到建造位置
     */
    private void 移动到建造位置() {
        double 距离 = 僵尸.blockPosition().distSqr(建造位置);
        
        if (距离 <= 9.0) { // 3格距离内
            建造阶段 = 1;
        } else {
            // 移动到建造位置附近
            僵尸.getNavigation().moveTo(建造位置.getX(), 建造位置.getY(), 建造位置.getZ(), 1.0);
        }
    }
    
    /**
     * 准备建造阶段
     */
    private void 准备建造() {
        // 面向建造位置
        Vec3 目标位置 = Vec3.atCenterOf(建造位置);
        僵尸.getLookControl().setLookAt(目标位置.x, 目标位置.y, 目标位置.z);
        
        // 播放准备音效
        if (建造进度 == 0) {
            世界.playSound(null, 僵尸.blockPosition(), SoundEvents.ZOMBIE_AMBIENT, 
                         SoundSource.HOSTILE, 0.5F, 1.2F);
        }
        
        建造进度++;
        
        if (建造进度 >= 20) { // 1秒准备时间
            建造阶段 = 2;
            建造进度 = 0;
        }
    }
    
    /**
     * 执行建造阶段
     */
    private void 执行建造() {
        建造进度++;
        
        // 播放建造音效
        if (建造进度 % 10 == 0) {
            世界.playSound(null, 建造位置, SoundEvents.STONE_PLACE, 
                         SoundSource.BLOCKS, 0.8F, 0.8F + 世界.random.nextFloat() * 0.4F);
        }
        
        // 完成建造
        if (建造进度 >= 建造时间) {
            完成建造();
        }
    }
    
    /**
     * 完成建造
     */
    private void 完成建造() {
        if (建造位置 == null || 建造材料 == null) {
            return;
        }
        
        // 检查位置是否仍然可以建造
        if (!世界.getBlockState(建造位置).isAir()) {
            return;
        }
        
        // 放置方块
        BlockState 新方块状态 = 建造材料.defaultBlockState();
        世界.setBlock(建造位置, 新方块状态, 3);
        
        // 播放完成音效
        世界.playSound(null, 建造位置, 新方块状态.getSoundType().getPlaceSound(), 
                     SoundSource.BLOCKS, 1.0F, 0.8F + 世界.random.nextFloat() * 0.4F);
        
        // 根据建造模式执行额外逻辑
        执行建造模式逻辑();
        
        // 增加僵尸经验
        if (Math.random() < 0.15) {
            int 当前等级 = 僵尸.get智能等级();
            if (当前等级 < 5) {
                僵尸.set智能等级(当前等级 + 1);
            }
        }
    }
    
    /**
     * 选择建造模式
     * 根据当前情况和僵尸智能等级选择最适合的建造模式
     */
    private 建造模式 选择建造模式() {
        int 智能等级 = 僵尸.get智能等级();
        
        // 如果有目标且距离较远，考虑建造攻击平台
        if (僵尸.getTarget() != null) {
            double 目标距离 = 僵尸.distanceTo(僵尸.getTarget());
            if (目标距离 > 8.0 && 智能等级 >= 3) {
                return 建造模式.攻击平台;
            }
        }
        
        // 如果生命值较低，优先建造防御墙
        if (僵尸.getHealth() < 僵尸.getMaxHealth() * 0.5 && 智能等级 >= 2) {
            return 建造模式.防御墙;
        }
        
        // 高智能等级可以建造陷阱
        if (智能等级 >= 4 && Math.random() < 0.3) {
            return 建造模式.陷阱;
        }
        
        // 默认建造防御墙
        return 建造模式.防御墙;
    }
    
    /**
     * 寻找合适的建造位置
     * 时间复杂度：O(n²) - n为搜索半径
     */
    private BlockPos 寻找建造位置() {
        BlockPos 僵尸位置 = 僵尸.blockPosition();
        int 搜索半径 = Math.min(EnhancedZombiesConfig.最大建造距离.get(), 5);
        
        switch (当前建造模式) {
            case 防御墙:
                return 寻找防御墙位置(僵尸位置, 搜索半径);
            case 攻击平台:
                return 寻找攻击平台位置(僵尸位置, 搜索半径);
            case 陷阱:
                return 寻找陷阱位置(僵尸位置, 搜索半径);
            case 桥梁:
                return 寻找桥梁位置(僵尸位置, 搜索半径);
            default:
                return 寻找防御墙位置(僵尸位置, 搜索半径);
        }
    }
    
    /**
     * 寻找防御墙建造位置
     */
    private BlockPos 寻找防御墙位置(BlockPos 中心位置, int 半径) {
        // 在僵尸周围寻找合适的位置建造防御墙
        for (int x = -半径; x <= 半径; x++) {
            for (int z = -半径; z <= 半径; z++) {
                if (Math.abs(x) + Math.abs(z) > 半径) continue;
                
                BlockPos 检查位置 = 中心位置.offset(x, 0, z);
                
                // 检查是否适合建造
                if (是否适合建造(检查位置) && 是否为防御位置(检查位置, 中心位置)) {
                    return 检查位置;
                }
            }
        }
        return null;
    }
    
    /**
     * 寻找攻击平台建造位置
     */
    private BlockPos 寻找攻击平台位置(BlockPos 中心位置, int 半径) {
        // 寻找较高的位置建造攻击平台
        for (int y = 1; y <= 3; y++) {
            for (int x = -半径; x <= 半径; x++) {
                for (int z = -半径; z <= 半径; z++) {
                    if (Math.abs(x) + Math.abs(z) > 半径) continue;
                    
                    BlockPos 检查位置 = 中心位置.offset(x, y, z);
                    
                    if (是否适合建造(检查位置) && 是否为攻击位置(检查位置)) {
                        return 检查位置;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 寻找陷阱建造位置
     */
    private BlockPos 寻找陷阱位置(BlockPos 中心位置, int 半径) {
        if (僵尸.getTarget() == null) return null;
        
        // 在目标可能经过的路径上建造陷阱
        Vec3 目标位置 = 僵尸.getTarget().position();
        Vec3 方向 = 目标位置.subtract(Vec3.atCenterOf(中心位置)).normalize();
        
        for (int i = 2; i <= 半径; i++) {
            Vec3 陷阱位置 = Vec3.atCenterOf(中心位置).add(方向.scale(i));
            BlockPos 检查位置 = new BlockPos((int)陷阱位置.x, (int)陷阱位置.y - 1, (int)陷阱位置.z);
            
            if (是否适合建造(检查位置)) {
                return 检查位置;
            }
        }
        return null;
    }
    
    /**
     * 寻找桥梁建造位置（优化：优先在脚下建造）
     */
    private BlockPos 寻找桥梁位置(BlockPos 中心位置, int 半径) {
        if (僵尸.getTarget() == null) return null;
        
        // 优先检查僵尸脚下是否需要搭路
        BlockPos 脚下位置 = 中心位置.below();
        
        // 如果脚下是空气、液体或不稳定方块，优先在脚下放置方块
        BlockState 脚下方块 = 世界.getBlockState(脚下位置);
        if (脚下方块.isAir() || 
            脚下方块.getFluidState().isSource() ||
            !脚下方块.isSolidRender(世界, 脚下位置)) {
            
            // 确保脚下位置适合建造
            if (是否适合建造(脚下位置)) {
                return 脚下位置;
            }
        }
        
        // 检查前进方向是否需要搭路
        Vec3 目标方向 = 僵尸.getTarget().position().subtract(僵尸.position()).normalize();
        
        // 检查前方1-3格的位置，优先检查距离近的
        for (int i = 1; i <= 3; i++) {
            BlockPos 前方位置 = 中心位置.offset(
                (int)Math.round(目标方向.x * i),
                0,
                (int)Math.round(目标方向.z * i)
            );
            
            BlockPos 前方脚下 = 前方位置.below();
            
            // 如果前方脚下是空气或液体，需要搭路
            BlockState 前方脚下方块 = 世界.getBlockState(前方脚下);
            if (前方脚下方块.isAir() || 
                前方脚下方块.getFluidState().isSource() ||
                !前方脚下方块.isSolidRender(世界, 前方脚下)) {
                
                // 确保前方位置本身是空气（可以通行）且适合建造
                if (世界.getBlockState(前方位置).isAir() && 是否适合建造(前方脚下)) {
                    return 前方脚下;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 检查位置是否适合建造
     */
    private boolean 是否适合建造(BlockPos 位置) {
        // 检查目标位置是否为空气
        if (!世界.getBlockState(位置).isAir()) {
            return false;
        }
        
        // 检查下方是否有支撑
        BlockPos 下方位置 = 位置.below();
        BlockState 下方方块 = 世界.getBlockState(下方位置);
        if (下方方块.isAir()) {
            return false;
        }
        
        // 检查是否在世界边界内
        return 世界.isInWorldBounds(位置);
    }
    
    /**
     * 检查是否为合适的防御位置
     */
    private boolean 是否为防御位置(BlockPos 位置, BlockPos 中心位置) {
        // 防御墙应该在僵尸和潜在威胁之间
        if (僵尸.getTarget() != null) {
            Vec3 目标方向 = 僵尸.getTarget().position().subtract(Vec3.atCenterOf(中心位置)).normalize();
            Vec3 建造方向 = Vec3.atCenterOf(位置).subtract(Vec3.atCenterOf(中心位置)).normalize();
            
            // 建造位置应该在目标方向上
            return 目标方向.dot(建造方向) > 0.5;
        }
        
        return true;
    }
    
    /**
     * 检查是否为合适的攻击位置
     */
    private boolean 是否为攻击位置(BlockPos 位置) {
        // 攻击平台应该有良好的视野
        return 位置.getY() > 僵尸.blockPosition().getY();
    }
    
    /**
     * 检查是否有建造材料
     */
    private boolean 检查建造材料() {
        // 简化版本：假设僵尸总是有基础建造材料
        // 在实际实现中，可以检查僵尸的物品栏或周围的掉落物
        return true;
    }
    
    /**
     * 选择建造材料
     */
    private Block 选择建造材料() {
        // 根据智能等级和可用性选择材料
        int 智能等级 = 僵尸.get智能等级();
        
        if (智能等级 >= 4) {
            return 建造材料列表[0]; // 鹅卵石
        } else if (智能等级 >= 3) {
            return 建造材料列表[2]; // 泥土
        } else {
            return 建造材料列表[2]; // 泥土
        }
    }
    
    /**
     * 执行建造模式特定逻辑
     */
    private void 执行建造模式逻辑() {
        switch (当前建造模式) {
            case 防御墙:
                // 可以考虑建造多个连续的防御方块
                break;
            case 攻击平台:
                // 建造完成后，僵尸可以尝试跳上平台
                break;
            case 陷阱:
                // 陷阱建造完成后，可以设置触发逻辑
                break;
            case 桥梁:
                // 桥梁建造逻辑
                break;
        }
    }
    
    /**
     * 检查是否需要建造来攻击目标
     * 根据目标高度差和距离来判断
     */
    private boolean 需要建造来攻击目标() {
        LivingEntity 目标 = 僵尸.getTarget();
        if (目标 == null) return false;
        
        Vec3 僵尸位置 = 僵尸.position();
        Vec3 目标位置 = 目标.position();
        double 水平距离 = Math.sqrt(Math.pow(目标位置.x - 僵尸位置.x, 2) + Math.pow(目标位置.z - 僵尸位置.z, 2));
        double 高度差 = 目标位置.y - 僵尸位置.y;
        
        // 如果目标在高处且距离适中，考虑建造攻击平台
        if (高度差 >= 2.0 && 水平距离 <= 8.0 && 水平距离 >= 3.0) {
            return true;
        }
        
        // 如果有深坑或水体阻挡，考虑建造桥梁
        if (检查是否需要桥梁(僵尸位置, 目标位置)) {
            return true;
        }
        
        // 如果僵尸生命值较低且有敌人，考虑建造防御工事
        if (僵尸.getHealth() < 僵尸.getMaxHealth() * 0.4 && 水平距离 <= 6.0) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否需要建造桥梁
     */
    private boolean 检查是否需要桥梁(Vec3 起点, Vec3 终点) {
        Vec3 方向 = 终点.subtract(起点).normalize();
        double 总距离 = 起点.distanceTo(终点);
        
        // 检查路径上是否有深坑或水体
        for (double d = 1.0; d < 总距离 && d < 8.0; d += 1.0) {
            Vec3 检查点 = 起点.add(方向.scale(d));
            BlockPos 脚下位置 = new BlockPos((int)检查点.x, (int)检查点.y - 1, (int)检查点.z);
            BlockState 脚下方块 = 世界.getBlockState(脚下位置);
            
            // 检查是否是水或空气（深坑）
            if (脚下方块.isAir() || 脚下方块.getFluidState().isSource()) {
                // 检查深度
                int 深度 = 0;
                for (int y = 脚下位置.getY(); y > 脚下位置.getY() - 5 && y > 世界.getMinBuildHeight(); y--) {
                    BlockPos 检查深度位置 = new BlockPos(脚下位置.getX(), y, 脚下位置.getZ());
                    if (世界.getBlockState(检查深度位置).isAir()) {
                        深度++;
                    } else {
                        break;
                    }
                }
                
                if (深度 >= 2) {
                    return true;
                }
            }
        }
        
        return false;
    }
}