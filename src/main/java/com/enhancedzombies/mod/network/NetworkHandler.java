package com.enhancedzombies.mod.network;

import com.enhancedzombies.mod.EnhancedZombiesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络处理器
 * 功能：管理客户端与服务端之间的数据包通信
 * 职责：
 * 1. 注册网络消息类型
 * 2. 处理僵尸军团状态同步
 * 3. 处理配置文件同步
 * 4. 处理客户端UI交互消息
 */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(EnhancedZombiesMod.MODID + ":main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    /**
     * 注册网络消息
     */
    public static void register() {
        // 目前暂时不需要网络消息，后续可以添加
        // 例如：军团状态同步、配置同步等
        EnhancedZombiesMod.LOGGER.info("网络处理器已初始化");
    }
    
    /**
     * 获取下一个数据包ID
     */
    private static int nextId() {
        return packetId++;
    }
}