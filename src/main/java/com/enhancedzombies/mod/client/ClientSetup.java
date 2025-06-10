package com.enhancedzombies.mod.client;

import com.enhancedzombies.mod.EnhancedZombiesMod;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端设置类
 * 功能：处理客户端特定的初始化和渲染器注册
 * 职责：
 * 1. 注册实体渲染器
 * 2. 注册模型
 * 3. 处理客户端特定事件
 */
@Mod.EventBusSubscriber(modid = EnhancedZombiesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    
    /**
     * 注册实体渲染器
     * 为增强僵尸设置原版僵尸的渲染器
     */
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EnhancedZombiesMod.ENHANCED_ZOMBIE.get(), ZombieRenderer::new);
        EnhancedZombiesMod.LOGGER.info("增强僵尸实体渲染器注册完成，使用原版僵尸渲染器");
    }
}