/// <summary>
/// 僵尸远程攻击AI目标
/// 实现僵尸使用弓箭或弩进行远程攻击的智能行为
/// </summary>
/// <author>Enhanced Zombies Mod</author>
/// <version>1.0</version>

package com.enhancedzombies.mod.entity.ai;

import com.enhancedzombies.mod.entity.EnhancedZombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import java.util.EnumSet;

/**
 * 僵尸远程攻击AI目标
 * 功能：管理僵尸使用弓箭或弩的远程攻击行为
 * 职责：
 * 1. 检测僵尸是否装备远程武器
 * 2. 计算攻击距离和瞄准角度
 * 3. 执行射击动作并播放音效
 * 4. 处理弹药消耗和冷却时间
 */
public class ZombieRangedAttackGoal extends Goal {
    private final EnhancedZombie 僵尸;
    private final double 移动速度;
    private int 攻击冷却时间;
    private final int 最大攻击冷却;
    private final float 攻击范围;
    private final float 攻击范围平方;
    private int 瞄准时间;
    private int 看向目标时间;
    private boolean 正在瞄准;
    
    /**
     * 构造函数
     * @param 僵尸 执行攻击的僵尸实体
     * @param 移动速度 攻击时的移动速度
     * @param 攻击间隔 攻击之间的间隔时间（tick）
     * @param 攻击范围 最大攻击距离
     */
    public ZombieRangedAttackGoal(EnhancedZombie 僵尸, double 移动速度, int 攻击间隔, float 攻击范围) {
        this.僵尸 = 僵尸;
        this.移动速度 = 移动速度;
        this.最大攻击冷却 = 攻击间隔;
        this.攻击范围 = 攻击范围;
        this.攻击范围平方 = 攻击范围 * 攻击范围;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }
    
    /**
     * 检查是否可以使用此AI目标
     * @return 如果僵尸装备了远程武器且有攻击目标则返回true
     */
    @Override
    public boolean canUse() {
        LivingEntity 目标 = this.僵尸.getTarget();
        if (目标 == null || !目标.isAlive()) {
            return false;
        }
        
        // 检查是否装备了远程武器
        ItemStack 主手物品 = this.僵尸.getMainHandItem();
        return 是否为远程武器(主手物品) && this.僵尸.distanceToSqr(目标) >= 4.0D; // 至少2格距离才使用远程攻击
    }
    
    /**
     * 检查是否应该继续使用此AI目标
     */
    @Override
    public boolean canContinueToUse() {
        return this.canUse() || !this.僵尸.getNavigation().isDone();
    }
    
    /**
     * 开始执行AI目标
     */
    @Override
    public void start() {
        super.start();
        this.僵尸.setAggressive(true);
        this.瞄准时间 = 0;
        this.看向目标时间 = 0;
        this.正在瞄准 = false;
    }
    
    /**
     * 停止执行AI目标
     */
    @Override
    public void stop() {
        super.stop();
        this.僵尸.setAggressive(false);
        this.瞄准时间 = 0;
        this.看向目标时间 = 0;
        this.正在瞄准 = false;
    }
    
    /**
     * 每tick执行的逻辑
     */
    @Override
    public void tick() {
        LivingEntity 目标 = this.僵尸.getTarget();
        if (目标 == null) return;
        
        double 距离平方 = this.僵尸.distanceToSqr(目标.getX(), 目标.getY(), 目标.getZ());
        boolean 能看到目标 = this.僵尸.getSensing().hasLineOfSight(目标);
        boolean 之前能看到 = this.看向目标时间 > 0;
        
        if (能看到目标 != 之前能看到) {
            this.看向目标时间 = 0;
        }
        
        if (能看到目标) {
            ++this.看向目标时间;
        } else {
            --this.看向目标时间;
        }
        
        // 移动逻辑
        if (距离平方 <= (double)this.攻击范围平方 && this.看向目标时间 >= 20) {
            this.僵尸.getNavigation().stop();
            ++this.瞄准时间;
        } else {
            this.僵尸.getNavigation().moveTo(目标, this.移动速度);
            this.瞄准时间 = -1;
        }
        
        if (this.瞄准时间 >= 20) {
            if (距离平方 <= (double)this.攻击范围平方 && 能看到目标) {
                this.正在瞄准 = true;
            } else {
                this.正在瞄准 = false;
            }
        }
        
        // 攻击逻辑
        if (this.攻击冷却时间 > 0) {
            --this.攻击冷却时间;
        }
        
        if (this.正在瞄准 && this.攻击冷却时间 <= 0) {
            this.执行远程攻击(目标, (float)Math.sqrt(距离平方));
            this.攻击冷却时间 = this.最大攻击冷却;
        }
        
        // 看向目标
        this.僵尸.getLookControl().setLookAt(目标, 30.0F, 30.0F);
    }
    
    /**
     * 执行远程攻击
     * @param 目标 攻击目标
     * @param 距离 到目标的距离
     */
    private void 执行远程攻击(LivingEntity 目标, float 距离) {
        ItemStack 武器 = this.僵尸.getMainHandItem();
        
        if (武器.getItem() instanceof BowItem) {
            this.射箭(目标, 距离);
        } else if (武器.getItem() instanceof CrossbowItem) {
            this.射弩箭(目标, 距离);
        }
    }
    
    /**
     * 射箭攻击
     * @param 目标 攻击目标
     * @param 距离 到目标的距离
     */
    private void 射箭(LivingEntity 目标, float 距离) {
        ItemStack 弓 = this.僵尸.getMainHandItem();
        ItemStack 箭矢 = this.僵尸.getProjectile(弓);
        
        if (箭矢.isEmpty()) {
            // 如果没有箭矢，创建一个基础箭矢
            箭矢 = new ItemStack(Items.ARROW);
        }
        
        AbstractArrow 箭实体 = ProjectileUtil.getMobArrow(this.僵尸, 箭矢, 1.6F);
        
        // 计算射击参数
        double dx = 目标.getX() - this.僵尸.getX();
        double dy = 目标.getY(0.3333333333333333D) - 箭实体.getY();
        double dz = 目标.getZ() - this.僵尸.getZ();
        double 水平距离 = Math.sqrt(dx * dx + dz * dz);
        
        // 设置箭矢速度和轨迹
        箭实体.shoot(dx, dy + 水平距离 * 0.20000000298023224D, dz, 1.6F, (float)(14 - this.僵尸.level().getDifficulty().getId() * 4));
        
        // 应用附魔效果
        int 力量等级 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, 弓);
        if (力量等级 > 0) {
            箭实体.setBaseDamage(箭实体.getBaseDamage() + (double)力量等级 * 0.5D + 0.5D);
        }
        
        int 冲击等级 = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, 弓);
        if (冲击等级 > 0) {
            箭实体.setKnockback(冲击等级);
        }
        
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, 弓) > 0) {
            箭实体.setSecondsOnFire(100);
        }
        
        // 播放射击音效
        this.僵尸.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.僵尸.getRandom().nextFloat() * 0.4F + 0.8F));
        
        // 发射箭矢
        this.僵尸.level().addFreshEntity(箭实体);
    }
    
    /**
     * 射弩箭攻击
     * @param 目标 攻击目标
     * @param 距离 到目标的距离
     */
    private void 射弩箭(LivingEntity 目标, float 距离) {
        ItemStack 弩 = this.僵尸.getMainHandItem();
        
        if (CrossbowItem.isCharged(弩)) {
            // 如果弩已装填，直接射击
            CrossbowItem.performShooting(this.僵尸.level(), this.僵尸, this.僵尸.getUsedItemHand(), 弩, 3.15F, (float)(14 - this.僵尸.level().getDifficulty().getId() * 4));
            
            // 播放射击音效
            this.僵尸.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (this.僵尸.getRandom().nextFloat() * 0.4F + 0.8F));
        } else {
            // 如果弩未装填，进行装填
            ItemStack 弹药 = this.僵尸.getProjectile(弩);
            if (弹药.isEmpty()) {
                弹药 = new ItemStack(Items.ARROW);
            }
            CrossbowItem.setCharged(弩, true);
        }
    }
    
    /**
     * 检查物品是否为远程武器
     * @param 物品 要检查的物品
     * @return 如果是弓或弩则返回true
     */
    private boolean 是否为远程武器(ItemStack 物品) {
        return 物品.getItem() instanceof BowItem || 物品.getItem() instanceof CrossbowItem;
    }
}