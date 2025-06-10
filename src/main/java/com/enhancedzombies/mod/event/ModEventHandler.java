package com.enhancedzombies.mod.event;

import com.enhancedzombies.mod.EnhancedZombiesMod;
import com.enhancedzombies.mod.entity.EnhancedZombie;
import com.enhancedzombies.mod.legion.ZombieLegionManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 模组事件处理器
 * 功能：处理与增强僵尸相关的游戏事件
 * 职责：
 * 1. 监听实体生成和死亡事件
 * 2. 处理增强僵尸的特殊行为
 * 3. 管理军团状态更新
 * 4. 记录战斗统计数据
 */
@Mod.EventBusSubscriber(modid = EnhancedZombiesMod.MODID)
public class ModEventHandler {
    
    /**
     * 处理实体加入世界事件
     * 用于初始化增强僵尸的特殊属性
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof EnhancedZombie 增强僵尸) {
            // 确保增强僵尸正确初始化
            if (!event.getLevel().isClientSide) {
                // 服务器端初始化逻辑
                初始化增强僵尸(增强僵尸);
            }
        }
    }
    
    /**
     * 处理实体离开世界事件
     * 用于清理增强僵尸的相关数据
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof EnhancedZombie 增强僵尸) {
            if (!event.getLevel().isClientSide) {
                // 从军团管理器中移除
                ZombieLegionManager.getInstance().移除僵尸(增强僵尸.getUUID());
                
                EnhancedZombiesMod.LOGGER.debug("增强僵尸离开世界: {}", 增强僵尸.getUUID());
            }
        }
    }
    
    /**
     * 处理实体死亡事件
     * 用于处理增强僵尸死亡时的特殊逻辑
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity 死亡实体 = event.getEntity();
        
        // 处理增强僵尸死亡
        if (死亡实体 instanceof EnhancedZombie 死亡僵尸) {
            处理增强僵尸死亡(死亡僵尸, event);
        }
        
        // 处理玩家击杀增强僵尸
        if (event.getSource().getEntity() instanceof Player 玩家 && 
            死亡实体 instanceof EnhancedZombie 被击杀僵尸) {
            处理玩家击杀僵尸(玩家, 被击杀僵尸);
        }
        
        // 处理增强僵尸击杀其他实体
        if (event.getSource().getEntity() instanceof EnhancedZombie 攻击僵尸) {
            处理僵尸击杀实体(攻击僵尸, 死亡实体);
        }
    }
    
    /**
     * 处理实体受伤事件
     * 用于实现增强僵尸的特殊战斗机制
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity 受伤实体 = event.getEntity();
        
        // 处理增强僵尸受伤
        if (受伤实体 instanceof EnhancedZombie 受伤僵尸) {
            处理增强僵尸受伤(受伤僵尸, event);
        }
        
        // 处理增强僵尸攻击其他实体
        if (event.getSource().getEntity() instanceof EnhancedZombie 攻击僵尸) {
            处理增强僵尸攻击(攻击僵尸, 受伤实体, event);
        }
    }
    
    /**
     * 初始化增强僵尸
     */
    private static void 初始化增强僵尸(EnhancedZombie 僵尸) {
        try {
            // 确保僵尸有正确的AI目标
            if (僵尸.goalSelector.getAvailableGoals().isEmpty()) {
                EnhancedZombiesMod.LOGGER.warn("增强僵尸 {} 没有AI目标，重新初始化", 僵尸.getUUID());
                // 这里可以添加重新初始化AI的逻辑
            }
            
            // 更新军团管理器中的活跃时间
            ZombieLegionManager.getInstance().移除僵尸(僵尸.getUUID()); // 先移除旧记录
            
            EnhancedZombiesMod.LOGGER.debug("增强僵尸初始化完成: {}", 僵尸.getUUID());
            
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("初始化增强僵尸时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理增强僵尸死亡
     */
    private static void 处理增强僵尸死亡(EnhancedZombie 死亡僵尸, LivingDeathEvent event) {
        try {
            // 从军团管理器中移除
            ZombieLegionManager manager = ZombieLegionManager.getInstance();
            manager.移除僵尸(死亡僵尸.getUUID());
            
            // 记录死亡统计
            int 军团ID = 死亡僵尸.get军团ID();
            if (军团ID != -1) {
                var 军团 = manager.获取军团(军团ID);
                if (军团 != null) {
                    // 军团死亡统计已在移除成员时处理
                    EnhancedZombiesMod.LOGGER.debug("军团 {} 成员死亡，剩余成员: {}", 
                        军团ID, 军团.get成员数量());
                }
            }
            
            // 如果是军团长死亡，触发特殊效果
            if (死亡僵尸.是否为军团长()) {
                处理军团长死亡(死亡僵尸);
            }
            
            // 播放死亡音效（如果需要）
            播放僵尸死亡音效(死亡僵尸);
            
            EnhancedZombiesMod.LOGGER.debug("增强僵尸死亡处理完成: {}", 死亡僵尸.getUUID());
            
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("处理增强僵尸死亡时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理玩家击杀僵尸
     */
    private static void 处理玩家击杀僵尸(Player 玩家, EnhancedZombie 被击杀僵尸) {
        try {
            // 给予玩家额外经验
            int 额外经验 = 计算额外经验(被击杀僵尸);
            if (额外经验 > 0) {
                玩家.giveExperiencePoints(额外经验);
            }
            
            // 记录击杀统计
            记录玩家击杀统计(玩家, 被击杀僵尸);
            
            // 如果击杀的是军团长，给予特殊奖励
            if (被击杀僵尸.是否为军团长()) {
                给予军团长击杀奖励(玩家, 被击杀僵尸);
            }
            
            EnhancedZombiesMod.LOGGER.debug("玩家 {} 击杀增强僵尸 {}", 
                玩家.getName().getString(), 被击杀僵尸.getUUID());
            
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("处理玩家击杀僵尸时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理僵尸击杀实体
     */
    private static void 处理僵尸击杀实体(EnhancedZombie 攻击僵尸, LivingEntity 死亡实体) {
        try {
            // 更新军团击杀统计
            int 军团ID = 攻击僵尸.get军团ID();
            if (军团ID != -1) {
                var 军团 = ZombieLegionManager.getInstance().获取军团(军团ID);
                if (军团 != null) {
                    军团.增加击杀数();
                }
            }
            
            // 如果击杀的是玩家，触发特殊效果
            if (死亡实体 instanceof Player 死亡玩家) {
                处理僵尸击杀玩家(攻击僵尸, 死亡玩家);
            }
            
            // 提升僵尸的智能等级
            提升僵尸智能等级(攻击僵尸);
            
            EnhancedZombiesMod.LOGGER.debug("增强僵尸 {} 击杀实体 {}", 
                攻击僵尸.getUUID(), 死亡实体.getType().getDescriptionId());
            
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("处理僵尸击杀实体时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理增强僵尸受伤
     */
    private static void 处理增强僵尸受伤(EnhancedZombie 受伤僵尸, LivingHurtEvent event) {
        try {
            // 更新军团管理器中的活跃时间
            int 军团ID = 受伤僵尸.get军团ID();
            if (军团ID != -1) {
                var 军团 = ZombieLegionManager.getInstance().获取军团(军团ID);
                if (军团 != null) {
                    军团.更新成员活跃时间(受伤僵尸.getUUID());
                }
            }
            
            // 如果生命值过低，触发求援行为
            if (受伤僵尸.getHealth() - event.getAmount() < 受伤僵尸.getMaxHealth() * 0.3) {
                触发求援行为(受伤僵尸);
            }
            
            // 根据伤害来源调整行为
            if (event.getSource().getEntity() instanceof Player 攻击玩家) {
                处理玩家攻击僵尸(攻击玩家, 受伤僵尸, event);
            }
            
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("处理增强僵尸受伤时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理增强僵尸攻击
     */
    private static void 处理增强僵尸攻击(EnhancedZombie 攻击僵尸, LivingEntity 受伤实体, LivingHurtEvent event) {
        try {
            // 根据智能等级调整伤害
            float 智能加成 = 攻击僵尸.get智能等级() * 0.1f;
            event.setAmount(event.getAmount() * (1.0f + 智能加成));
            
            // 更新军团活跃时间
            int 军团ID = 攻击僵尸.get军团ID();
            if (军团ID != -1) {
                var 军团 = ZombieLegionManager.getInstance().获取军团(军团ID);
                if (军团 != null) {
                    军团.更新成员活跃时间(攻击僵尸.getUUID());
                }
            }
            
            // 如果攻击的是玩家，通知其他军团成员
            if (受伤实体 instanceof Player 受伤玩家) {
                通知军团成员攻击目标(攻击僵尸, 受伤玩家);
            }
            
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("处理增强僵尸攻击时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理军团长死亡
     * 功能：掉落所有装备作为战利品
     */
    private static void 处理军团长死亡(EnhancedZombie 军团长) {
        try {
            int 军团ID = 军团长.get军团ID();
            if (军团ID != -1) {
                // 掉落军团长的所有装备
                掉落军团长装备(军团长);
                
                var 军团 = ZombieLegionManager.getInstance().获取军团(军团ID);
                if (军团 != null) {
                    EnhancedZombiesMod.LOGGER.info("军团 {} 的军团长死亡，装备已掉落，军团将重新组织", 军团ID);
                    
                    // 军团长死亡的特殊效果可以在这里添加
                    // 例如：爆炸效果、召唤增援等
                }
            }
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("处理军团长死亡时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 掉落军团长装备
     * 将军团长身上的所有装备转换为掉落物
     */
    private static void 掉落军团长装备(EnhancedZombie 军团长) {
        try {
            net.minecraft.world.level.Level 世界 = 军团长.level();
            net.minecraft.core.BlockPos 位置 = 军团长.blockPosition();
            
            // 掉落主手武器
            net.minecraft.world.item.ItemStack 主手武器 = 军团长.getMainHandItem();
            if (!主手武器.isEmpty()) {
                掉落物品(世界, 位置, 主手武器.copy());
                军团长.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            }
            
            // 掉落副手物品
            net.minecraft.world.item.ItemStack 副手物品 = 军团长.getOffhandItem();
            if (!副手物品.isEmpty()) {
                掉落物品(世界, 位置, 副手物品.copy());
                军团长.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            }
            
            // 掉落护甲装备
            for (net.minecraft.world.entity.EquipmentSlot 装备槽 : net.minecraft.world.entity.EquipmentSlot.values()) {
                if (装备槽.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
                    net.minecraft.world.item.ItemStack 护甲 = 军团长.getItemBySlot(装备槽);
                    if (!护甲.isEmpty()) {
                        掉落物品(世界, 位置, 护甲.copy());
                        军团长.setItemSlot(装备槽, net.minecraft.world.item.ItemStack.EMPTY);
                    }
                }
            }
            
            EnhancedZombiesMod.LOGGER.debug("军团长装备已全部掉落在位置: {}", 位置);
            
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("掉落军团长装备时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 在指定位置掉落物品
     */
    private static void 掉落物品(net.minecraft.world.level.Level 世界, net.minecraft.core.BlockPos 位置, net.minecraft.world.item.ItemStack 物品) {
        if (!世界.isClientSide && !物品.isEmpty()) {
            net.minecraft.world.entity.item.ItemEntity 掉落物 = new net.minecraft.world.entity.item.ItemEntity(
                世界, 
                位置.getX() + 0.5, 
                位置.getY() + 0.5, 
                位置.getZ() + 0.5, 
                物品
            );
            
            // 添加随机速度，让物品散开
            double 随机X = (世界.random.nextDouble() - 0.5) * 0.5;
            double 随机Y = 0.2;
            double 随机Z = (世界.random.nextDouble() - 0.5) * 0.5;
            掉落物.setDeltaMovement(随机X, 随机Y, 随机Z);
            
            世界.addFreshEntity(掉落物);
        }
    }
    
    /**
     * 处理僵尸击杀玩家
     */
    private static void 处理僵尸击杀玩家(EnhancedZombie 攻击僵尸, Player 死亡玩家) {
        try {
            // 提升整个军团的士气
            int 军团ID = 攻击僵尸.get军团ID();
            if (军团ID != -1) {
                var 军团 = ZombieLegionManager.getInstance().获取军团(军团ID);
                if (军团 != null) {
                    // 军团士气提升逻辑可以在ZombieLegion类中实现
                    EnhancedZombiesMod.LOGGER.info("军团 {} 击杀玩家 {}，士气大振！", 
                        军团ID, 死亡玩家.getName().getString());
                }
            }
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("处理僵尸击杀玩家时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理玩家攻击僵尸
     */
    private static void 处理玩家攻击僵尸(Player 攻击玩家, EnhancedZombie 受伤僵尸, LivingHurtEvent event) {
        try {
            // 通知军团成员有玩家攻击
            通知军团成员攻击目标(受伤僵尸, 攻击玩家);
            
            // 根据僵尸的智能等级调整受到的伤害
            float 智能减伤 = 受伤僵尸.get智能等级() * 0.05f;
            event.setAmount(event.getAmount() * (1.0f - 智能减伤));
            
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("处理玩家攻击僵尸时发生错误: {}", e.getMessage(), e);
        }
    }
    
    // 辅助方法
    
    /**
     * 计算额外经验
     */
    private static int 计算额外经验(EnhancedZombie 僵尸) {
        int 基础经验 = 5;
        int 智能加成 = 僵尸.get智能等级() * 2;
        int 军团长加成 = 僵尸.是否为军团长() ? 10 : 0;
        
        return 基础经验 + 智能加成 + 军团长加成;
    }
    
    /**
     * 记录玩家击杀统计
     */
    private static void 记录玩家击杀统计(Player 玩家, EnhancedZombie 僵尸) {
        // 这里可以实现更复杂的统计系统
        // 例如：记录到数据库、发送到统计服务器等
    }
    
    /**
     * 给予军团长击杀奖励
     */
    private static void 给予军团长击杀奖励(Player 玩家, EnhancedZombie 军团长) {
        // 给予额外经验
        玩家.giveExperiencePoints(20);
        
        // 这里可以添加其他奖励
        // 例如：特殊物品、成就等
    }
    
    /**
     * 提升僵尸智能等级
     */
    private static void 提升僵尸智能等级(EnhancedZombie 僵尸) {
        int 当前等级 = 僵尸.get智能等级();
        if (当前等级 < 10 && Math.random() < 0.1) { // 10%概率提升
            僵尸.set智能等级(当前等级 + 1);
            EnhancedZombiesMod.LOGGER.debug("僵尸 {} 智能等级提升到 {}", 
                僵尸.getUUID(), 当前等级 + 1);
        }
    }
    
    /**
     * 触发求援行为
     */
    private static void 触发求援行为(EnhancedZombie 僵尸) {
        // 通知附近的军团成员前来支援
        通知军团成员支援(僵尸);
    }
    
    /**
     * 通知军团成员攻击目标
     */
    private static void 通知军团成员攻击目标(EnhancedZombie 发起者, Player 目标玩家) {
        try {
            int 军团ID = 发起者.get军团ID();
            if (军团ID == -1) return;
            
            // 寻找附近的军团成员
            var 附近僵尸 = 发起者.level().getEntitiesOfClass(
                EnhancedZombie.class,
                发起者.getBoundingBox().inflate(16.0),
                zombie -> zombie.get军团ID() == 军团ID && zombie != 发起者
            );
            
            // 设置目标
            for (EnhancedZombie 成员 : 附近僵尸) {
                if (成员.getTarget() == null || 成员.distanceTo(目标玩家) < 成员.distanceTo(成员.getTarget())) {
                    成员.setTarget(目标玩家);
                }
            }
            
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("通知军团成员攻击目标时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 通知军团成员支援
     */
    private static void 通知军团成员支援(EnhancedZombie 求援者) {
        try {
            int 军团ID = 求援者.get军团ID();
            if (军团ID == -1) return;
            
            // 寻找附近的军团成员
            var 附近僵尸 = 求援者.level().getEntitiesOfClass(
                EnhancedZombie.class,
                求援者.getBoundingBox().inflate(24.0),
                zombie -> zombie.get军团ID() == 军团ID && zombie != 求援者
            );
            
            // 让附近成员前来支援
            for (EnhancedZombie 成员 : 附近僵尸) {
                // 如果成员没有更重要的目标，让其前来支援
                if (成员.getTarget() == null) {
                    成员.getNavigation().moveTo(求援者.getX(), 求援者.getY(), 求援者.getZ(), 1.2);
                }
            }
            
        } catch (Exception e) {
            EnhancedZombiesMod.LOGGER.error("通知军团成员支援时发生错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 播放僵尸死亡音效
     */
    private static void 播放僵尸死亡音效(EnhancedZombie 僵尸) {
        // 这里可以播放自定义的死亡音效
        // 需要注册自定义音效资源
    }
}