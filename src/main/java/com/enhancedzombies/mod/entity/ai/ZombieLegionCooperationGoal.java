package com.enhancedzombies.mod.entity.ai;

import com.enhancedzombies.mod.config.EnhancedZombiesConfig;
import com.enhancedzombies.mod.entity.EnhancedZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 僵尸军团协作AI目标
 * 功能：实现僵尸之间的智能协作行为
 * 职责：
 * 1. 协调军团成员的攻击策略
 * 2. 实现信息共享和通信
 * 3. 执行群体战术和阵型
 * 4. 管理军团资源分配
 */
public class ZombieLegionCooperationGoal extends Goal {
    
    private final EnhancedZombie 僵尸;
    private final Level 世界;
    private int 协作冷却时间;
    private int 上次协作时间;
    private LivingEntity 共享目标;
    private 协作模式 当前协作模式;
    
    // 协作行为模式
    private enum 协作模式 {
        包围攻击,    // 从多个方向包围目标
        集中火力,    // 集中攻击单一目标
        分散注意,    // 分散敌人注意力
        防御阵型,    // 形成防御阵型
        侦察支援,    // 侦察和支援
        资源共享     // 共享装备和资源
    }
    
    // 阵型位置偏移
    private static final Vec3[] 包围阵型偏移 = {
        new Vec3(2, 0, 0),   // 东
        new Vec3(-2, 0, 0),  // 西
        new Vec3(0, 0, 2),   // 南
        new Vec3(0, 0, -2),  // 北
        new Vec3(1.5, 0, 1.5),   // 东南
        new Vec3(-1.5, 0, 1.5),  // 西南
        new Vec3(1.5, 0, -1.5),  // 东北
        new Vec3(-1.5, 0, -1.5)  // 西北
    };
    
    public ZombieLegionCooperationGoal(EnhancedZombie 僵尸) {
        this.僵尸 = 僵尸;
        this.世界 = 僵尸.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET));
        this.协作冷却时间 = 0;
    }
    
    /**
     * 检查是否可以开始协作
     * 时间复杂度：O(1)
     */
    @Override
    public boolean canUse() {
        // 检查协作冷却
        if (协作冷却时间 > 0) {
            协作冷却时间--;
            return false;
        }
        
        // 检查配置概率 - 降低激活频率
        if (Math.random() > EnhancedZombiesConfig.团队协作概率.get() * 0.3) {
            return false;
        }
        
        // 必须属于某个军团
        if (僵尸.get军团ID() == -1) {
            return false;
        }
        
        // 只有在有明确目标时才启用协作
        if (僵尸.getTarget() == null) {
            return false;
        }
        
        // 寻找附近的军团成员
        List<EnhancedZombie> 附近军团成员 = 获取附近军团成员();
        if (附近军团成员.size() < 3) { // 至少需要3个成员才协作
            return false;
        }
        
        // 选择协作模式
        当前协作模式 = 选择协作模式(附近军团成员);
        
        return true;
    }
    
    /**
     * 检查是否应该继续协作
     */
    @Override
    public boolean canContinueToUse() {
        // 检查军团成员是否仍在附近
        List<EnhancedZombie> 附近军团成员 = 获取附近军团成员();
        if (附近军团成员.size() < 2) {
            return false;
        }
        
        // 检查共享目标是否仍然有效
        if (共享目标 != null && (!共享目标.isAlive() || 僵尸.distanceTo(共享目标) > 32.0)) {
            共享目标 = null;
        }
        
        return true;
    }
    
    /**
     * 开始协作行为
     */
    @Override
    public void start() {
        上次协作时间 = 僵尸.tickCount;
        
        // 如果是军团长，负责协调整个军团
        if (僵尸.是否为军团长()) {
            协调军团行动();
        }
    }
    
    /**
     * 执行协作逻辑
     */
    @Override
    public void tick() {
        switch (当前协作模式) {
            case 包围攻击:
                执行包围攻击();
                break;
            case 集中火力:
                执行集中火力();
                break;
            case 分散注意:
                执行分散注意();
                break;
            case 防御阵型:
                执行防御阵型();
                break;
            case 侦察支援:
                执行侦察支援();
                break;
            case 资源共享:
                执行资源共享();
                break;
        }
    }
    
    /**
     * 停止协作行为
     */
    @Override
    public void stop() {
        协作冷却时间 = 60; // 3秒冷却
        共享目标 = null;
        当前协作模式 = null;
    }
    
    /**
     * 获取附近的军团成员
     * 时间复杂度：O(n) - n为附近实体数量
     */
    private List<EnhancedZombie> 获取附近军团成员() {
        return 世界.getEntitiesOfClass(
            EnhancedZombie.class,
            僵尸.getBoundingBox().inflate(16.0),
            zombie -> zombie != 僵尸 && 
                     zombie.get军团ID() == 僵尸.get军团ID() && 
                     zombie.isAlive()
        );
    }
    
    /**
     * 选择协作模式
     * 根据当前情况和军团状态选择最适合的协作模式
     */
    private 协作模式 选择协作模式(List<EnhancedZombie> 军团成员) {
        // 检查是否有共同目标
        LivingEntity 目标 = 僵尸.getTarget();
        if (目标 != null) {
            // 计算军团成员中有多少也在攻击同一目标
            long 攻击同一目标的数量 = 军团成员.stream()
                .filter(zombie -> zombie.getTarget() == 目标)
                .count();
            
            if (攻击同一目标的数量 >= 3) {
                // 如果有足够多的成员攻击同一目标，使用包围战术
                return 协作模式.包围攻击;
            } else if (攻击同一目标的数量 >= 1) {
                // 集中火力攻击
                return 协作模式.集中火力;
            }
        }
        
        // 检查军团整体生命值
        double 平均生命值比例 = 计算军团平均生命值比例(军团成员);
        if (平均生命值比例 < 0.5) {
            return 协作模式.防御阵型;
        }
        
        // 检查是否需要侦察
        if (目标 == null && 僵尸.是否为军团长()) {
            return 协作模式.侦察支援;
        }
        
        // 检查是否需要资源共享
        if (需要资源共享(军团成员)) {
            return 协作模式.资源共享;
        }
        
        // 默认使用分散注意模式
        return 协作模式.分散注意;
    }
    
    /**
     * 协调军团行动（军团长专用）
     */
    private void 协调军团行动() {
        List<EnhancedZombie> 军团成员 = 获取附近军团成员();
        
        // 共享目标信息
        if (僵尸.getTarget() != null) {
            共享目标 = 僵尸.getTarget();
            for (EnhancedZombie 成员 : 军团成员) {
                if (成员.getTarget() == null || 成员.distanceTo(共享目标) < 成员.distanceTo(成员.getTarget())) {
                    成员.setTarget(共享目标);
                }
            }
        }
        
        // 分配阵型位置
        分配阵型位置(军团成员);
    }
    
    /**
     * 执行包围攻击
     * 算法：将军团成员分配到目标周围的不同位置
     */
    private void 执行包围攻击() {
        if (共享目标 == null) {
            共享目标 = 僵尸.getTarget();
        }
        
        if (共享目标 == null) return;
        
        List<EnhancedZombie> 军团成员 = 获取附近军团成员();
        
        // 为每个成员分配包围位置
        for (int i = 0; i < 军团成员.size() && i < 包围阵型偏移.length; i++) {
            EnhancedZombie 成员 = 军团成员.get(i);
            Vec3 目标位置 = 共享目标.position().add(包围阵型偏移[i]);
            
            // 移动到分配的位置
            成员.getNavigation().moveTo(目标位置.x, 目标位置.y, 目标位置.z, 1.2);
            
            // 只在成员没有目标时才设置共享目标
            if (成员.getTarget() == null) {
                成员.setTarget(共享目标);
            }
        }
    }
    
    /**
     * 执行集中火力
     */
    private void 执行集中火力() {
        if (共享目标 == null) {
            共享目标 = 僵尸.getTarget();
        }
        
        if (共享目标 == null) return;
        
        List<EnhancedZombie> 军团成员 = 获取附近军团成员();
        
        // 协调成员攻击，但不强制覆盖现有目标
        for (EnhancedZombie 成员 : 军团成员) {
            // 只在成员没有目标时才设置共享目标
            if (成员.getTarget() == null) {
                成员.setTarget(共享目标);
            }
            
            // 加快移动速度
            成员.getNavigation().setSpeedModifier(1.3);
        }
    }
    
    /**
     * 执行分散注意
     */
    private void 执行分散注意() {
        List<EnhancedZombie> 军团成员 = 获取附近军团成员();
        
        // 寻找附近的玩家
        List<Player> 附近玩家 = 世界.getEntitiesOfClass(
            Player.class,
            僵尸.getBoundingBox().inflate(24.0),
            player -> player.isAlive() && !player.isCreative() && !player.isSpectator()
        );
        
        if (附近玩家.isEmpty()) return;
        
        // 为每个军团成员分配不同的目标
        for (int i = 0; i < 军团成员.size() && i < 附近玩家.size(); i++) {
            EnhancedZombie 成员 = 军团成员.get(i);
            Player 目标玩家 = 附近玩家.get(i % 附近玩家.size());
            
            if (成员.getTarget() != 目标玩家) {
                成员.setTarget(目标玩家);
            }
        }
    }
    
    /**
     * 执行防御阵型
     */
    private void 执行防御阵型() {
        List<EnhancedZombie> 军团成员 = 获取附近军团成员();
        
        // 找到军团的中心位置
        Vec3 军团中心 = 计算军团中心位置(军团成员);
        
        // 让受伤的成员移动到中心，健康的成员在外围
        for (EnhancedZombie 成员 : 军团成员) {
            double 生命值比例 = 成员.getHealth() / 成员.getMaxHealth();
            
            if (生命值比例 < 0.3) {
                // 重伤成员移动到中心
                成员.getNavigation().moveTo(军团中心.x, 军团中心.y, 军团中心.z, 0.8);
            } else {
                // 健康成员在外围防御
                Vec3 防御位置 = 军团中心.add(
                    (成员.position().x - 军团中心.x) * 1.5,
                    0,
                    (成员.position().z - 军团中心.z) * 1.5
                );
                成员.getNavigation().moveTo(防御位置.x, 防御位置.y, 防御位置.z, 0.6);
            }
        }
    }
    
    /**
     * 执行侦察支援
     */
    private void 执行侦察支援() {
        if (!僵尸.是否为军团长()) return;
        
        List<EnhancedZombie> 军团成员 = 获取附近军团成员();
        
        // 派遣部分成员进行侦察
        int 侦察员数量 = Math.min(2, 军团成员.size() / 3);
        
        for (int i = 0; i < 侦察员数量; i++) {
            EnhancedZombie 侦察员 = 军团成员.get(i);
            
            // 向不同方向派遣侦察员
            double 角度 = (2 * Math.PI * i) / 侦察员数量;
            Vec3 侦察方向 = new Vec3(Math.cos(角度) * 16, 0, Math.sin(角度) * 16);
            Vec3 侦察位置 = 僵尸.position().add(侦察方向);
            
            侦察员.getNavigation().moveTo(侦察位置.x, 侦察位置.y, 侦察位置.z, 1.0);
        }
    }
    
    /**
     * 执行资源共享
     */
    private void 执行资源共享() {
        List<EnhancedZombie> 军团成员 = 获取附近军团成员();
        
        // 简化版本：共享目标信息和位置信息
        // 在完整实现中，可以共享装备、食物等资源
        
        for (EnhancedZombie 成员 : 军团成员) {
            // 共享目标信息
            if (僵尸.getTarget() != null && 成员.getTarget() == null) {
                成员.setTarget(僵尸.getTarget());
            }
            
            // 共享建造位置信息
            if (僵尸.get目标建造位置() != null && 成员.get目标建造位置() == null) {
                BlockPos 偏移位置 = 僵尸.get目标建造位置().offset(
                    世界.random.nextInt(3) - 1, 0, 世界.random.nextInt(3) - 1
                );
                成员.set目标建造位置(偏移位置);
            }
        }
    }
    
    /**
     * 分配阵型位置
     */
    private void 分配阵型位置(List<EnhancedZombie> 军团成员) {
        if (共享目标 == null) return;
        
        Vec3 目标位置 = 共享目标.position();
        
        for (int i = 0; i < 军团成员.size() && i < 包围阵型偏移.length; i++) {
            EnhancedZombie 成员 = 军团成员.get(i);
            Vec3 分配位置 = 目标位置.add(包围阵型偏移[i]);
            
            // 设置成员的目标位置（可以用于其他AI目标）
            // 这里可以扩展为更复杂的阵型管理
        }
    }
    
    /**
     * 计算军团平均生命值比例
     */
    private double 计算军团平均生命值比例(List<EnhancedZombie> 军团成员) {
        if (军团成员.isEmpty()) return 1.0;
        
        double 总生命值比例 = 0.0;
        for (EnhancedZombie 成员 : 军团成员) {
            总生命值比例 += 成员.getHealth() / 成员.getMaxHealth();
        }
        
        return 总生命值比例 / 军团成员.size();
    }
    
    /**
     * 计算军团中心位置
     */
    private Vec3 计算军团中心位置(List<EnhancedZombie> 军团成员) {
        if (军团成员.isEmpty()) return 僵尸.position();
        
        double 总X = 僵尸.getX();
        double 总Y = 僵尸.getY();
        double 总Z = 僵尸.getZ();
        int 总数量 = 1;
        
        for (EnhancedZombie 成员 : 军团成员) {
            总X += 成员.getX();
            总Y += 成员.getY();
            总Z += 成员.getZ();
            总数量++;
        }
        
        return new Vec3(总X / 总数量, 总Y / 总数量, 总Z / 总数量);
    }
    
    /**
     * 检查是否需要资源共享
     */
    private boolean 需要资源共享(List<EnhancedZombie> 军团成员) {
        // 检查是否有成员缺少装备或处于不利状态
        for (EnhancedZombie 成员 : 军团成员) {
            if (成员.getMainHandItem().isEmpty() || 成员.getHealth() < 成员.getMaxHealth() * 0.3) {
                return true;
            }
        }
        return false;
    }
}