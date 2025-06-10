package com.enhancedzombies.mod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

/**
 * 增强僵尸模组配置类
 * 功能：管理模组的所有可配置参数
 * 职责：
 * 1. 定义僵尸军团大小限制
 * 2. 设置生成间隔时间范围
 * 3. 控制僵尸智能行为参数
 * 4. 管理装备生成概率
 */
@Mod.EventBusSubscriber
public class EnhancedZombiesConfig {
    
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // 军团配置
    public static final ForgeConfigSpec.IntValue 最大军团大小;
    public static final ForgeConfigSpec.IntValue 最小军团大小;
    public static final ForgeConfigSpec.IntValue 军团生成最小间隔秒数;
    public static final ForgeConfigSpec.IntValue 军团生成最大间隔秒数;
    public static final ForgeConfigSpec.DoubleValue 军团生成概率;
    
    // 僵尸智能配置
    public static final ForgeConfigSpec.DoubleValue 方块破坏概率;
    public static final ForgeConfigSpec.DoubleValue 方块建造概率;
    public static final ForgeConfigSpec.IntValue 最大破坏距离;
    public static final ForgeConfigSpec.IntValue 最大建造距离;
    public static final ForgeConfigSpec.DoubleValue 团队协作概率;
    
    // 装备配置
    public static final ForgeConfigSpec.DoubleValue 高级装备概率;
    public static final ForgeConfigSpec.DoubleValue 武器生成概率;
    public static final ForgeConfigSpec.DoubleValue 护甲生成概率;
    public static final ForgeConfigSpec.DoubleValue 附魔装备概率;
    
    // 战斗配置
    public static final ForgeConfigSpec.DoubleValue 攻击力倍数;
    public static final ForgeConfigSpec.DoubleValue 生命值倍数;
    public static final ForgeConfigSpec.DoubleValue 移动速度倍数;
    public static final ForgeConfigSpec.IntValue 最大追击距离;
    
    // 开发配置
    public static final ForgeConfigSpec.IntValue 开发模式;
    
    static {
        BUILDER.comment("增强僵尸模组配置文件")
               .comment("Enhanced Zombies Mod Configuration")
               .push("enhanced_zombies");
        
        // 军团配置部分
        BUILDER.comment("僵尸军团配置 / Zombie Legion Configuration")
               .push("legion");
        
        最大军团大小 = BUILDER
            .comment("单个军团的最大僵尸数量 (1-25) / Maximum zombies per legion (1-25)")
            .defineInRange("max_legion_size", 25, 1, 25);
        
        最小军团大小 = BUILDER
            .comment("单个军团的最小僵尸数量 (1-25) / Minimum zombies per legion (1-25)")
            .defineInRange("min_legion_size", 5, 1, 25);
        
        军团生成最小间隔秒数 = BUILDER
            .comment("军团生成最小间隔时间(秒) / Minimum legion spawn interval (seconds)")
            .defineInRange("min_spawn_interval", 30, 10, 3600);
        
        军团生成最大间隔秒数 = BUILDER
            .comment("军团生成最大间隔时间(秒) / Maximum legion spawn interval (seconds)")
            .defineInRange("max_spawn_interval", 120, 30, 7200);
        
        军团生成概率 = BUILDER
            .comment("每次检查时军团生成的概率 (0.0-1.0) / Legion spawn probability per check (0.0-1.0)")
            .defineInRange("legion_spawn_chance", 0.8, 0.0, 1.0);
        
        BUILDER.pop();
        
        // 智能行为配置
        BUILDER.comment("僵尸智能行为配置 / Zombie AI Configuration")
               .push("ai");
        
        方块破坏概率 = BUILDER
            .comment("僵尸破坏方块的概率 (0.0-1.0) / Block breaking probability (0.0-1.0)")
            .defineInRange("block_break_chance", 0.4, 0.0, 1.0);
        
        方块建造概率 = BUILDER
            .comment("僵尸建造方块的概率 (0.0-1.0) / Block building probability (0.0-1.0)")
            .defineInRange("block_build_chance", 0.2, 0.0, 1.0);
        
        最大破坏距离 = BUILDER
            .comment("僵尸破坏方块的最大距离 / Maximum block breaking distance")
            .defineInRange("max_break_distance", 8, 1, 16);
        
        最大建造距离 = BUILDER
            .comment("僵尸建造方块的最大距离 / Maximum block building distance")
            .defineInRange("max_build_distance", 5, 1, 10);
        
        团队协作概率 = BUILDER
            .comment("僵尸团队协作的概率 (0.0-1.0) / Team cooperation probability (0.0-1.0)")
            .defineInRange("team_cooperation_chance", 0.6, 0.0, 1.0);
        
        BUILDER.pop();
        
        // 装备配置
        BUILDER.comment("僵尸装备配置 / Zombie Equipment Configuration")
               .push("equipment");
        
        高级装备概率 = BUILDER
            .comment("生成高级装备的概率 (0.0-1.0) / Advanced equipment probability (0.0-1.0)")
            .defineInRange("advanced_equipment_chance", 0.3, 0.0, 1.0);
        
        武器生成概率 = BUILDER
            .comment("武器生成概率 (0.0-1.0) / Weapon spawn probability (0.0-1.0)")
            .defineInRange("weapon_spawn_chance", 0.8, 0.0, 1.0);
        
        护甲生成概率 = BUILDER
            .comment("护甲生成概率 (0.0-1.0) / Armor spawn probability (0.0-1.0)")
            .defineInRange("armor_spawn_chance", 0.7, 0.0, 1.0);
        
        附魔装备概率 = BUILDER
            .comment("附魔装备概率 (0.0-1.0) / Enchanted equipment probability (0.0-1.0)")
            .defineInRange("enchanted_equipment_chance", 0.4, 0.0, 1.0);
        
        BUILDER.pop();
        
        // 战斗配置
        BUILDER.comment("僵尸战斗配置 / Zombie Combat Configuration")
               .push("combat");
        
        攻击力倍数 = BUILDER
            .comment("僵尸攻击力倍数 (0.5-5.0) / Zombie attack damage multiplier (0.5-5.0)")
            .defineInRange("attack_damage_multiplier", 1.5, 0.5, 5.0);
        
        生命值倍数 = BUILDER
            .comment("僵尸生命值倍数 (0.5-5.0) / Zombie health multiplier (0.5-5.0)")
            .defineInRange("health_multiplier", 1.8, 0.5, 5.0);
        
        移动速度倍数 = BUILDER
            .comment("僵尸移动速度倍数 (0.5-3.0) / Zombie movement speed multiplier (0.5-3.0)")
            .defineInRange("movement_speed_multiplier", 1.2, 0.5, 3.0);
        
        最大追击距离 = BUILDER
            .comment("僵尸最大追击距离 / Maximum zombie chase distance")
            .defineInRange("max_chase_distance", 32, 8, 64);
        
        BUILDER.pop();
        
        // 开发配置
        BUILDER.comment("开发和调试配置 / Development and Debug Configuration")
               .push("development");
        
        开发模式 = BUILDER
            .comment("开发模式 (0=关闭, 1=开启) / Development mode (0=off, 1=on)")
            .comment("开启后允许白天生成军团并显示调试信息 / When enabled, allows daytime legion spawning and shows debug info")
            .defineInRange("development_mode", 0, 0, 1);
        
        BUILDER.pop();
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
    
    /**
     * 获取当前军团大小配置
     * 时间复杂度：O(1)
     */
    public static int get当前最大军团大小() {
        return 最大军团大小.get();
    }
    
    /**
     * 获取随机军团大小
     * 时间复杂度：O(1)
     */
    public static int get随机军团大小() {
        int 最小值 = 最小军团大小.get();
        int 最大值 = 最大军团大小.get();
        return 最小值 + (int)(Math.random() * (最大值 - 最小值 + 1));
    }
    
    /**
     * 获取随机生成间隔
     * 时间复杂度：O(1)
     */
    public static int get随机生成间隔() {
        int 最小间隔 = 军团生成最小间隔秒数.get();
        int 最大间隔 = 军团生成最大间隔秒数.get();
        return 最小间隔 + (int)(Math.random() * (最大间隔 - 最小间隔 + 1));
    }
    
    /**
     * 检查是否为开发模式
     * 时间复杂度：O(1)
     */
    public static boolean 是否为开发模式() {
        return 开发模式.get() == 1;
    }
}