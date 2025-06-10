package com.enhancedzombies.mod.entity;

import com.enhancedzombies.mod.config.EnhancedZombiesConfig;
import com.enhancedzombies.mod.entity.ai.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 增强僵尸实体类
 * 功能：实现智能僵尸行为和军团协作
 * 职责：
 * 1. 管理僵尸的智能AI行为
 * 2. 处理方块破坏和建造逻辑
 * 3. 实现军团协作机制
 * 4. 控制装备生成和属性加成
 */
public class EnhancedZombie extends Zombie implements RangedAttackMob {
    
    /**
     * 移除远程攻击功能 - 专注近战攻击
     * 原远程攻击方法已禁用，确保僵尸只进行近战
     */
    @Override
    public void performRangedAttack(LivingEntity 目标, float 攻击力度) {
        // 不执行任何远程攻击，强制僵尸进行近战
        return;
    }
    
    // 数据同步器
    private static final EntityDataAccessor<Integer> 军团ID = 
        SynchedEntityData.defineId(EnhancedZombie.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> 是否为军团长 = 
        SynchedEntityData.defineId(EnhancedZombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> 智能等级 = 
        SynchedEntityData.defineId(EnhancedZombie.class, EntityDataSerializers.INT);
    
    // 军团相关属性
    private List<UUID> 军团成员列表 = new ArrayList<>();
    private int 上次协作时间 = 0;
    private BlockPos 目标建造位置;
    private int 建造冷却时间 = 0;
    private int 破坏冷却时间 = 0;
    
    // AI更新相关
    private int AI更新计时器 = 0;
    private static final int AI更新间隔 = 200; // 进一步增加到200
    private int 军团协作计时器 = 0;
    private static final int 军团协作间隔 = 400; // 大幅增加军团协作间隔
    private int 装备检查计时器 = 0;
    private static final int 装备检查间隔 = 600; // 大幅增加装备检查间隔
    private int 实体查询计时器 = 0;
    private static final int 实体查询间隔 = 300; // 新增实体查询间隔控制
    
    public EnhancedZombie(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        this.setCanPickUpLoot(true);
        this.setPersistenceRequired();
    }
    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(军团ID, -1);
        this.entityData.define(是否为军团长, false);
        this.entityData.define(智能等级, 1);
    }
    
    /**
     * 创建增强僵尸的属性
     * 基于配置文件动态调整属性值
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
            .add(Attributes.MAX_HEALTH, 20.0 * EnhancedZombiesConfig.生命值倍数.get())
            .add(Attributes.ATTACK_DAMAGE, 3.0 * EnhancedZombiesConfig.攻击力倍数.get())
            .add(Attributes.MOVEMENT_SPEED, 0.23 * EnhancedZombiesConfig.移动速度倍数.get())
            .add(Attributes.FOLLOW_RANGE, EnhancedZombiesConfig.最大追击距离.get())
            .add(Attributes.ARMOR, 2.0)
            .add(Attributes.ARMOR_TOUGHNESS, 1.0);
    }
    
    @Override
    protected void registerGoals() {
        // 清除原有目标
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.5D, true)); // 提高攻击速度
        this.goalSelector.addGoal(3, new ZombieBreakBlockGoal(this));
        this.goalSelector.addGoal(4, new ZombieBuildBlockGoal(this));
        
        this.goalSelector.addGoal(7, new MoveThroughVillageGoal(this, 1.0D, true, 4, () -> false));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        
        // 目标选择AI - 优先攻击玩家
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true)); // 简化参数，确保持续攻击玩家
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("军团ID", this.get军团ID());
        compound.putBoolean("是否为军团长", this.是否为军团长());
        compound.putInt("智能等级", this.get智能等级());
        
        // 保存军团成员列表
        ListTag 成员列表 = new ListTag();
        for (UUID 成员ID : 军团成员列表) {
            CompoundTag 成员数据 = new CompoundTag();
            成员数据.putUUID("成员ID", 成员ID);
            成员列表.add(成员数据);
        }
        compound.put("军团成员列表", 成员列表);
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.set军团ID(compound.getInt("军团ID"));
        this.set是否为军团长(compound.getBoolean("是否为军团长"));
        this.set智能等级(compound.getInt("智能等级"));
        
        // 读取军团成员列表
        军团成员列表.clear();
        if (compound.contains("军团成员列表")) {
            ListTag 成员列表 = compound.getList("军团成员列表", 10);
            for (int i = 0; i < 成员列表.size(); i++) {
                CompoundTag 成员数据 = 成员列表.getCompound(i);
                军团成员列表.add(成员数据.getUUID("成员ID"));
            }
        }
    }
    
    public double getFollowRange() {
        // 增强僵尸的追踪距离，基于智能等级
        double 基础距离 = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        int 智能等级 = get智能等级();
        return 基础距离 + (智能等级 * 8.0); // 每级智能增加8格追踪距离
    }
    
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        
        // 当设置目标时，增强追踪能力
        if (target != null && !this.level().isClientSide) {
            // 提高移动速度以便追踪
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25 + (get智能等级() * 0.02));
            
            // 军团成员共享目标信息
            if (get军团ID() != -1) {
                共享目标给军团成员(target);
            }
        }
    }
    
    /**
     * 共享目标给附近的军团成员（优化版）
     */
    private void 共享目标给军团成员(LivingEntity 目标) {
        if (get军团ID() == -1) return;
        
        // 添加频率控制，避免每次setTarget都执行
        if (this.tickCount % 60 != 0) return; // 每3秒才执行一次
        
        List<EnhancedZombie> 附近军团成员 = this.level().getEntitiesOfClass(
            EnhancedZombie.class,
            this.getBoundingBox().inflate(12.0), // 减小范围从32.0到12.0
            zombie -> zombie.get军团ID() == this.get军团ID() && zombie != this
        );
        
        // 限制处理数量，最多处理2个成员
        int 处理数量 = Math.min(2, 附近军团成员.size());
        for (int i = 0; i < 处理数量; i++) {
            EnhancedZombie 成员 = 附近军团成员.get(i);
            if (成员.getTarget() == null || 成员.distanceTo(目标) > this.distanceTo(目标)) {
                成员.setTarget(目标);
                break; // 只设置一个就退出
            }
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide) {
            // 增强目标追踪逻辑（进一步降低频率）
            if (this.getTarget() != null && this.tickCount % 20 == 0) {
                增强目标追踪();
            }
            
            // 更新AI计时器（大幅降低频率）
            AI更新计时器++;
            if (AI更新计时器 >= AI更新间隔) {
                AI更新计时器 = 0;
                更新智能行为();
            }
            
            // 军团协作计时器（独立控制，大幅降低频率）
            军团协作计时器++;
            if (军团协作计时器 >= 军团协作间隔) {
                军团协作计时器 = 0;
                if (Math.random() < 0.1) { // 进一步降低执行概率到10%
                    执行军团协作();
                }
            }
            
            // 装备检查计时器（大幅降低频率）
            装备检查计时器++;
            if (装备检查计时器 >= 装备检查间隔) {
                装备检查计时器 = 0;
                if (Math.random() < 0.5) { // 添加概率控制
                    管理装备();
                }
            }
            
            // 更新冷却时间
            if (建造冷却时间 > 0) 建造冷却时间--;
            if (破坏冷却时间 > 0) 破坏冷却时间--;
        }
    }
    
    /**
     * 增强目标追踪逻辑
     * 防止僵尸在远距离时失去目标
     */
    private void 增强目标追踪() {
        LivingEntity 目标 = this.getTarget();
        if (目标 == null || !目标.isAlive()) {
            // 如果没有目标，重置追踪范围到默认值
            double 默认追踪距离 = EnhancedZombiesConfig.最大追击距离.get() + (get智能等级() * 8.0);
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(默认追踪距离);
            return;
        }
        
        double 距离 = this.distanceTo(目标);
        double 最大追踪距离 = getFollowRange();
        
        // 如果目标在追踪范围内但僵尸没有路径，尝试重新寻路
        if (距离 <= 最大追踪距离) {
            if (this.getNavigation().getPath() == null || this.getNavigation().isDone()) {
                // 尝试直接寻路到目标
                if (!this.getNavigation().moveTo(目标, 1.0 + (get智能等级() * 0.1))) {
                    // 如果直接寻路失败，尝试寻路到目标附近
                    BlockPos 目标位置 = 目标.blockPosition();
                    for (int 尝试次数 = 0; 尝试次数 < 5; 尝试次数++) {
                        int 偏移X = this.random.nextInt(6) - 3;
                        int 偏移Z = this.random.nextInt(6) - 3;
                        BlockPos 替代位置 = 目标位置.offset(偏移X, 0, 偏移Z);
                        
                        if (this.getNavigation().moveTo(替代位置.getX(), 替代位置.getY(), 替代位置.getZ(), 1.0)) {
                            break;
                        }
                    }
                }
            }
            // 确保追踪范围保持正常值
            double 默认追踪距离 = EnhancedZombiesConfig.最大追击距离.get() + (get智能等级() * 8.0);
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(默认追踪距离);
        }
        
        // 如果目标超出范围但在视线内，临时延长追踪时间
        else if (距离 > 最大追踪距离 && 距离 <= 最大追踪距离 * 1.5) {
            if (this.hasLineOfSight(目标)) {
                // 临时扩展追踪范围，但限制最大值
                double 扩展追踪距离 = Math.min(最大追踪距离 * 1.2, 最大追踪距离 + 16.0);
                this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(扩展追踪距离);
            } else {
                // 没有视线时重置到默认值
                double 默认追踪距离 = EnhancedZombiesConfig.最大追击距离.get() + (get智能等级() * 8.0);
                this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(默认追踪距离);
            }
        }
        // 目标太远时重置追踪范围
        else {
            double 默认追踪距离 = EnhancedZombiesConfig.最大追击距离.get() + (get智能等级() * 8.0);
            this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(默认追踪距离);
        }
    }
    
    /**
     * 更新智能行为逻辑
     * 时间复杂度：O(1) - 优化后减少复杂计算
     */
    private void 更新智能行为() {
        // 更新军团成员列表（降低频率）
        if (this.tickCount % 200 == 0) {
            更新军团成员();
        }
    }
    
    /**
     * 执行军团协作逻辑（优化版）
     * 包括信息共享、协同攻击、资源分配
     */
    private void 执行军团协作() {
        if (get军团ID() == -1) return;
        
        // 控制实体查询频率
        实体查询计时器++;
        if (实体查询计时器 < 实体查询间隔) return;
        实体查询计时器 = 0;
        
        // 寻找附近的军团成员（减小查询范围）
        List<EnhancedZombie> 附近军团成员 = this.level().getEntitiesOfClass(
            EnhancedZombie.class, 
            this.getBoundingBox().inflate(8.0), // 从16.0减少到8.0
            zombie -> zombie.get军团ID() == this.get军团ID() && zombie != this
        );
        
        // 限制处理的成员数量，避免过多循环
        if (附近军团成员.size() > 5) {
            附近军团成员 = 附近军团成员.subList(0, 5);
        }
        
        // 共享目标信息（仅在有目标且成员较少时）
        if (this.getTarget() != null && 附近军团成员.size() > 0 && 附近军团成员.size() <= 3) {
            for (EnhancedZombie 成员 : 附近军团成员) {
                if (成员.getTarget() == null) {
                    成员.setTarget(this.getTarget());
                    break; // 只设置一个成员，避免过多操作
                }
            }
        }
        
        // 协同建造（仅军团长且成员数量合理时）
        if (是否为军团长() && 目标建造位置 != null && 附近军团成员.size() <= 3) {
            协调军团建造(附近军团成员);
        }
    }
    
    /**
     * 协调军团建造行为
     * 算法：分配不同成员负责不同的建造任务
     */
    private void 协调军团建造(List<EnhancedZombie> 军团成员) {
        if (军团成员.size() < 2) return;
        
        // 为每个成员分配建造位置
        for (int i = 0; i < 军团成员.size() && i < 4; i++) {
            EnhancedZombie 成员 = 军团成员.get(i);
            BlockPos 分配位置 = 目标建造位置.offset(i % 2, 0, i / 2);
            成员.set目标建造位置(分配位置);
        }
    }
    
    /**
     * 管理僵尸装备
     * 根据配置概率生成和优化装备
     */
    private void 管理装备() {
        // 武器管理
        if (this.getMainHandItem().isEmpty() && Math.random() < EnhancedZombiesConfig.武器生成概率.get()) {
            生成武器();
        }
        
        // 护甲管理
        if (Math.random() < EnhancedZombiesConfig.护甲生成概率.get()) {
            生成护甲();
        }
    }
    
    /**
     * 生成武器装备
     * 根据智能等级和军团长身份生成不同品质的武器
     */
    private void 生成武器() {
        ItemStack 武器;
        
        if (是否为军团长()) {
            // 军团长专属：下界合金武器
            武器 = new ItemStack(Items.NETHERITE_SWORD);
            // 为军团长武器添加附魔
            try {
                武器.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 5);
                武器.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
                武器.enchant(net.minecraft.world.item.enchantment.Enchantments.MENDING, 1);
            } catch (Exception e) {
                // 附魔失败时忽略
            }
        } else {
            // 普通成员：多样化武器
            int 智能等级 = get智能等级();
            double 随机值 = Math.random();
            
            if (智能等级 >= 4 && 随机值 < 0.2) {
                // 高智能：钻石武器
                if (Math.random() < 0.6) {
                    武器 = new ItemStack(Items.DIAMOND_SWORD);
                } else {
                    武器 = new ItemStack(Items.DIAMOND_AXE);
                }
            } else if (智能等级 >= 3 && 随机值 < 0.4) {
                // 中等智能：铁质武器
                double 武器选择 = Math.random();
                if (武器选择 < 0.4) {
                    武器 = new ItemStack(Items.IRON_SWORD);
                } else if (武器选择 < 0.7) {
                    武器 = new ItemStack(Items.IRON_AXE);
                } else {
                    武器 = new ItemStack(Items.IRON_SWORD); // 移除弩，改为铁剑
                }
            } else if (智能等级 >= 2 && 随机值 < 0.6) {
                // 低等智能：石质武器
                if (Math.random() < 0.5) {
                    武器 = new ItemStack(Items.STONE_SWORD);
                } else {
                    武器 = new ItemStack(Items.STONE_AXE);
                }
            } else {
                // 最低智能：木质武器
                double 武器选择 = Math.random();
                if (武器选择 < 0.4) {
                    武器 = new ItemStack(Items.WOODEN_SWORD);
                } else if (武器选择 < 0.7) {
                    武器 = new ItemStack(Items.WOODEN_AXE);
                } else {
                    武器 = new ItemStack(Items.WOODEN_SWORD); // 移除弓，改为木剑
                }
            }
            
            // 普通成员附魔概率 - 只对近战武器附魔
            if (Math.random() < EnhancedZombiesConfig.附魔装备概率.get()) {
                try {
                    武器.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 1 + this.random.nextInt(3));
                } catch (Exception e) {
                    // 附魔失败时忽略
                }
            }
        }
        
        this.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, 武器);
    }
    
    /**
     * 生成护甲装备
     * 军团长获得全套下界合金护甲，普通成员获得多样化护甲
     */
    private void 生成护甲() {
        if (是否为军团长()) {
            // 军团长：全套下界合金护甲
            生成军团长护甲();
        } else {
            // 普通成员：多样化护甲
            生成普通护甲();
        }
    }
    
    /**
     * 为军团长生成全套下界合金护甲
     */
    private void 生成军团长护甲() {
        // 头盔
        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            ItemStack 头盔 = new ItemStack(Items.NETHERITE_HELMET);
            try {
                头盔.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 4);
                头盔.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
                头盔.enchant(net.minecraft.world.item.enchantment.Enchantments.MENDING, 1);
            } catch (Exception e) {}
            this.setItemSlot(EquipmentSlot.HEAD, 头盔);
        }
        
        // 胸甲
        if (this.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            ItemStack 胸甲 = new ItemStack(Items.NETHERITE_CHESTPLATE);
            try {
                胸甲.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 4);
                胸甲.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
                胸甲.enchant(net.minecraft.world.item.enchantment.Enchantments.MENDING, 1);
            } catch (Exception e) {}
            this.setItemSlot(EquipmentSlot.CHEST, 胸甲);
        }
        
        // 护腿
        if (this.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) {
            ItemStack 护腿 = new ItemStack(Items.NETHERITE_LEGGINGS);
            try {
                护腿.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 4);
                护腿.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
                护腿.enchant(net.minecraft.world.item.enchantment.Enchantments.MENDING, 1);
            } catch (Exception e) {}
            this.setItemSlot(EquipmentSlot.LEGS, 护腿);
        }
        
        // 靴子
        if (this.getItemBySlot(EquipmentSlot.FEET).isEmpty()) {
            ItemStack 靴子 = new ItemStack(Items.NETHERITE_BOOTS);
            try {
                靴子.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION, 4);
                靴子.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
                靴子.enchant(net.minecraft.world.item.enchantment.Enchantments.MENDING, 1);
                靴子.enchant(net.minecraft.world.item.enchantment.Enchantments.DEPTH_STRIDER, 3);
            } catch (Exception e) {}
            this.setItemSlot(EquipmentSlot.FEET, 靴子);
        }
    }
    
    /**
     * 为普通成员生成多样化护甲
     */
    private void 生成普通护甲() {
        int 智能等级 = get智能等级();
        
        // 头盔生成
        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && Math.random() < 0.8) {
            ItemStack 头盔;
            if (智能等级 >= 4 && Math.random() < 0.3) {
                头盔 = new ItemStack(Items.DIAMOND_HELMET);
            } else if (智能等级 >= 3 && Math.random() < 0.5) {
                头盔 = new ItemStack(Items.IRON_HELMET);
            } else if (智能等级 >= 2 && Math.random() < 0.6) {
                头盔 = new ItemStack(Items.CHAINMAIL_HELMET);
            } else {
                头盔 = new ItemStack(Items.LEATHER_HELMET);
            }
            this.setItemSlot(EquipmentSlot.HEAD, 头盔);
        }
        
        // 胸甲生成（概率较低）
        if (this.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && Math.random() < 0.4) {
            ItemStack 胸甲;
            if (智能等级 >= 4 && Math.random() < 0.2) {
                胸甲 = new ItemStack(Items.DIAMOND_CHESTPLATE);
            } else if (智能等级 >= 3 && Math.random() < 0.4) {
                胸甲 = new ItemStack(Items.IRON_CHESTPLATE);
            } else {
                胸甲 = new ItemStack(Items.LEATHER_CHESTPLATE);
            }
            this.setItemSlot(EquipmentSlot.CHEST, 胸甲);
        }
        
        // 靴子生成（概率中等）
        if (this.getItemBySlot(EquipmentSlot.FEET).isEmpty() && Math.random() < 0.6) {
            ItemStack 靴子;
            if (智能等级 >= 3 && Math.random() < 0.3) {
                靴子 = new ItemStack(Items.IRON_BOOTS);
            } else {
                靴子 = new ItemStack(Items.LEATHER_BOOTS);
            }
            this.setItemSlot(EquipmentSlot.FEET, 靴子);
        }
    }
    
    /**
     * 更新军团成员列表，移除已死亡或距离过远的成员（优化版）
     */
    private void 更新军团成员() {
        // 限制每次处理的成员数量，避免一次性处理过多
        if (军团成员列表.size() > 10) {
            // 如果成员过多，只保留前10个
            军团成员列表 = new ArrayList<>(军团成员列表.subList(0, 10));
        }
        
        // 分批处理，每次最多检查3个成员
        int 检查数量 = Math.min(3, 军团成员列表.size());
        List<UUID> 待移除列表 = new ArrayList<>();
        
        for (int i = 0; i < 检查数量; i++) {
            UUID uuid = 军团成员列表.get(i);
            Entity 实体 = ((net.minecraft.server.level.ServerLevel)this.level()).getEntity(uuid);
            if (实体 == null || 
                !(实体 instanceof EnhancedZombie) || 
                实体.distanceTo(this) > 24.0) { // 减小距离阈值
                待移除列表.add(uuid);
            }
        }
        
        // 批量移除
        军团成员列表.removeAll(待移除列表);
    }
    
    // Getter和Setter方法
    public int get军团ID() {
        return this.entityData.get(军团ID);
    }
    
    public void set军团ID(int id) {
        this.entityData.set(军团ID, id);
    }
    
    public boolean 是否为军团长() {
        return this.entityData.get(是否为军团长);
    }
    
    public void set军团长(boolean 是军团长) {
        this.entityData.set(是否为军团长, 是军团长);
    }
    
    public void set是否为军团长(boolean 是军团长) {
        this.entityData.set(是否为军团长, 是军团长);
    }
    
    public int get智能等级() {
        return this.entityData.get(智能等级);
    }
    
    public void set智能等级(int 等级) {
        this.entityData.set(智能等级, Math.max(1, Math.min(5, 等级)));
    }
    
    public BlockPos get目标建造位置() {
        return 目标建造位置;
    }
    
    public void set目标建造位置(BlockPos 位置) {
        this.目标建造位置 = 位置;
    }
    
    public boolean 可以建造() {
        return 建造冷却时间 <= 0;
    }
    
    public void set建造冷却(int 冷却时间) {
        this.建造冷却时间 = 冷却时间;
    }
    
    public boolean 可以破坏() {
        return 破坏冷却时间 <= 0;
    }
    
    public void set破坏冷却(int 冷却时间) {
        this.破坏冷却时间 = 冷却时间;
    }
    
    public List<UUID> get军团成员列表() {
        return 军团成员列表;
    }
    
    public void 添加军团成员(UUID 成员UUID) {
        if (!军团成员列表.contains(成员UUID)) {
            军团成员列表.add(成员UUID);
        }
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }
    
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ZOMBIE_HURT;
    }
    
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }
    
    /**
     * 重写近战攻击方法
     * 确保僵尸能够正确攻击目标实体
     * 时间复杂度：O(1)
     * 空间复杂度：O(1)
     */
    @Override
    public boolean doHurtTarget(Entity 目标实体) {
        // 调用父类的攻击方法
        boolean 攻击成功 = super.doHurtTarget(目标实体);
        
        if (攻击成功 && 目标实体 instanceof LivingEntity 生命实体) {
            // 根据智能等级调整攻击伤害
            float 智能加成 = 1.0F + (get智能等级() - 1) * 0.2F; // 每级增加20%伤害
            float 基础伤害 = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float 最终伤害 = 基础伤害 * 智能加成;
            
            // 应用额外伤害
            if (智能加成 > 1.0F) {
                生命实体.hurt(this.damageSources().mobAttack(this), 最终伤害 - 基础伤害);
            }
            
            // 军团长攻击时有概率造成额外效果
            if (是否为军团长() && this.random.nextFloat() < 0.3F) {
                // 30%概率造成缓慢效果
                生命实体.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            }
            
            // 记录攻击日志（仅在调试模式下）
            if (com.enhancedzombies.mod.EnhancedZombiesMod.DEBUG_MODE) {
                com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.debug(
                    "[增强僵尸] {} 攻击 {} 造成 {} 伤害 (智能等级: {})", 
                    this.getName().getString(), 
                    生命实体.getName().getString(), 
                    最终伤害, 
                    get智能等级());
            }
        }
        
        return 攻击成功;
    }
    
    /**
     * 重写canAttack方法，确保僵尸可以攻击所有敌对目标
     * 解决僵尸不攻击玩家的问题
     */
    @Override
    public boolean canAttack(LivingEntity 目标) {
        // 基础攻击判断
        if (!super.canAttack(目标)) {
            return false;
        }
        
        // 确保可以攻击玩家
        if (目标 instanceof net.minecraft.world.entity.player.Player) {
            return true;
        }
        
        // 可以攻击村民和铁傀儡
        if (目标 instanceof net.minecraft.world.entity.npc.AbstractVillager || 
            目标 instanceof net.minecraft.world.entity.animal.IronGolem) {
            return true;
        }
        
        // 记录攻击判断日志（仅在调试模式下）
        if (com.enhancedzombies.mod.EnhancedZombiesMod.DEBUG_MODE) {
            com.enhancedzombies.mod.EnhancedZombiesMod.LOGGER.debug(
                "[增强僵尸] {} 判断是否可攻击 {} : {}", 
                this.getName().getString(), 
                目标.getName().getString(), 
                true);
        }
        
        return true;
    }
    
    /**
     * 重写canAttackType方法，确保可以攻击玩家类型
     */
    @Override
    public boolean canAttackType(EntityType<?> 实体类型) {
        // 允许攻击玩家
        if (实体类型 == EntityType.PLAYER) {
            return true;
        }
        
        return super.canAttackType(实体类型);
    }
    
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, 
                                       MobSpawnType reason, @Nullable SpawnGroupData spawnData, 
                                       @Nullable CompoundTag dataTag) {
        SpawnGroupData 生成数据 = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        
        // 设置初始智能等级（普通成员）
        set智能等级(1 + this.random.nextInt(3));
        
        // 默认设为普通成员，军团长身份由ZombieLegionManager统一管理
        set是否为军团长(false);
        
        // 普通成员装备生成
        if (Math.random() < 0.8) {
            生成武器();
        }
        if (Math.random() < 0.6) {
            生成护甲();
        }
        
        return 生成数据;
    }
    
    /**
     * 设置为军团长（由ZombieLegionManager调用）
     * 包含军团长专属的属性和装备设置
     */
    public void 设置为军团长() {
        set是否为军团长(true);
        set智能等级(4 + this.random.nextInt(2)); // 军团长智能等级4-5
        
        // 清空现有装备，重新生成军团长专属装备
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        
        // 为军团长设置特殊名称
        设置军团长名称();
        
        // 军团长必定生成高级装备
        生成武器();
        生成护甲();
    }
    
    /**
     * 为军团长设置特殊名称
     */
    private void 设置军团长名称() {
        String[] 军团长称号 = {
            "§c§l僵尸军团长",
            "§4§l亡灵统帅", 
            "§6§l腐朽将军",
            "§5§l暗夜领主",
            "§8§l死亡骑士",
            "§9§l堕落指挥官",
            "§d§l血腥督军",
            "§2§l瘟疫先锋"
        };
        
        String 选中称号 = 军团长称号[this.random.nextInt(军团长称号.length)];
        this.setCustomName(net.minecraft.network.chat.Component.literal(选中称号));
        this.setCustomNameVisible(true);
    }
}