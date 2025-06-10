package com.enhancedzombies.mod;

import com.enhancedzombies.mod.config.EnhancedZombiesConfig;
import com.enhancedzombies.mod.entity.EnhancedZombie;
import com.enhancedzombies.mod.event.ModEventHandler;
import com.enhancedzombies.mod.legion.ZombieLegionManager;
import com.enhancedzombies.mod.network.NetworkHandler;
import com.enhancedzombies.mod.command.ZombieLegionCommand;
import com.enhancedzombies.mod.client.ClientSetup;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 增强僵尸模组主类
 * 功能：管理模组的初始化和核心系统
 * 职责：
 * 1. 注册实体类型和属性
 * 2. 初始化配置系统
 * 3. 设置网络通信
 * 4. 启动僵尸军团管理器
 */
@Mod(EnhancedZombiesMod.MODID)
public class EnhancedZombiesMod {
    public static final String MODID = "enhancedzombies";
    public static final Logger LOGGER = LogManager.getLogger();
    
    // 调试模式开关 - 生产环境应设为false
    public static final boolean DEBUG_MODE = false;
    
    // 实体注册器
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    
    // 增强僵尸实体类型注册
    public static final RegistryObject<EntityType<EnhancedZombie>> ENHANCED_ZOMBIE = 
        ENTITY_TYPES.register("enhanced_zombie", () -> EntityType.Builder.of(EnhancedZombie::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .build("enhanced_zombie"));
    
    // 僵尸军团管理器实例
    public static ZombieLegionManager 军团管理器;
    
    public EnhancedZombiesMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // 注册实体类型
        ENTITY_TYPES.register(modEventBus);
        
        // 注册模组事件监听器
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onEntityAttributeCreation);
        modEventBus.addListener(this::clientSetup);
        
        // 注册Forge事件总线
        MinecraftForge.EVENT_BUS.register(new ModEventHandler());
        MinecraftForge.EVENT_BUS.register(this);
        
        // 注册配置文件
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EnhancedZombiesConfig.SPEC);
        
        LOGGER.info("增强僵尸模组初始化完成");
    }
    
    /**
     * 通用设置阶段
     * 时间复杂度：O(1)
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 初始化网络处理器
            NetworkHandler.register();
            
            // 初始化僵尸军团管理器
            军团管理器 = ZombieLegionManager.getInstance();
            
            LOGGER.info("增强僵尸模组通用设置完成");
        });
    }
    
    /**
     * 实体属性创建事件处理
     * 为增强僵尸设置基础属性
     */
    @SubscribeEvent
    public void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ENHANCED_ZOMBIE.get(), EnhancedZombie.createAttributes().build());
        LOGGER.info("增强僵尸实体属性注册完成");
    }
    
    /**
     * 命令注册事件处理
     * 注册僵尸军团相关命令
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ZombieLegionCommand.register(event.getDispatcher());
        LOGGER.info("僵尸军团命令注册完成");
    }
    
    /**
     * 客户端设置阶段
     * 用于注册客户端特定内容
     * 时间复杂度：O(1)
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 客户端渲染器已在ClientSetup类中注册
            LOGGER.info("增强僵尸模组客户端设置完成");
        });
    }
}