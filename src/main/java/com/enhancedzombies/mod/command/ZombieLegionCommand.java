package com.enhancedzombies.mod.command;

import com.enhancedzombies.mod.EnhancedZombiesMod;
import com.enhancedzombies.mod.legion.ZombieLegionManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 僵尸军团指令处理器
 * 功能：提供游戏内指令来生成和管理僵尸军团
 * 职责：
 * 1. 注册军团生成指令
 * 2. 处理指令参数验证
 * 3. 调用军团管理器执行生成
 * 4. 向玩家反馈执行结果
 */
public class ZombieLegionCommand {
    
    /**
     * 注册所有僵尸军团相关指令
     * 时间复杂度：O(1)
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("enhancedzombies")
                .requires(source -> source.hasPermission(2)) // 需要管理员权限
                .then(Commands.literal("spawn_legion")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("size", IntegerArgumentType.integer(1, 100))
                            .executes(ZombieLegionCommand::spawnLegionWithSize)
                        )
                        .executes(ZombieLegionCommand::spawnLegionDefault)
                    )
                    .executes(ZombieLegionCommand::spawnLegionSelf)
                )
                .then(Commands.literal("test_legion")
                    .executes(ZombieLegionCommand::spawnTestLegion)
                )
                .then(Commands.literal("clear_legions")
                    .executes(ZombieLegionCommand::clearAllLegions)
                )
        );
    }
    
    /**
     * 为指定玩家生成指定大小的军团
     * 指令格式：/enhancedzombies spawn_legion <玩家> <大小>
     */
    private static int spawnLegionWithSize(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer 目标玩家 = EntityArgument.getPlayer(context, "target");
        int 军团大小 = IntegerArgumentType.getInteger(context, "size");
        
        return 执行军团生成(context.getSource(), 目标玩家, 军团大小);
    }
    
    /**
     * 为指定玩家生成默认大小的军团
     * 指令格式：/enhancedzombies spawn_legion <玩家>
     */
    private static int spawnLegionDefault(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer 目标玩家 = EntityArgument.getPlayer(context, "target");
        
        return 执行军团生成(context.getSource(), 目标玩家, -1); // -1表示使用默认大小
    }
    
    /**
     * 为指令执行者生成军团
     * 指令格式：/enhancedzombies spawn_legion
     */
    private static int spawnLegionSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!(context.getSource().getEntity() instanceof ServerPlayer 执行者)) {
            context.getSource().sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }
        
        return 执行军团生成(context.getSource(), 执行者, -1);
    }
    
    /**
     * 生成50个僵尸的测试军团
     */
    private static int spawnTestLegion(CommandContext<CommandSourceStack> context) {
        CommandSourceStack 源 = context.getSource();
        
        try {
            // 获取命令执行者（必须是玩家）
            Player 玩家 = 源.getPlayerOrException();
            
            // 调用军团管理器生成50个僵尸的军团
            ZombieLegionManager 管理器 = com.enhancedzombies.mod.EnhancedZombiesMod.军团管理器;
            if (管理器 != null) {
                boolean 成功 = 管理器.强制生成指定大小军团(玩家, 50);
                
                if (成功) {
                    源.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                        "§a成功生成了一个包含50个僵尸的军团！"), true);
                    return 1;
                } else {
                    源.sendFailure(net.minecraft.network.chat.Component.literal(
                        "§c生成军团失败！可能是军团数量已达上限或位置不合适。"));
                    return 0;
                }
            } else {
                源.sendFailure(net.minecraft.network.chat.Component.literal(
                    "§c军团管理器未初始化！"));
                return 0;
            }
            
        } catch (Exception e) {
            源.sendFailure(net.minecraft.network.chat.Component.literal(
                "§c执行命令时出错: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 清除所有活跃军团
     * 指令格式：/enhancedzombies clear_legions
     */
    private static int clearAllLegions(CommandContext<CommandSourceStack> context) {
        CommandSourceStack 源 = context.getSource();
        
        try {
            // 获取服务器世界
            net.minecraft.server.level.ServerLevel 世界 = 源.getLevel();
            
            // 调用军团管理器清除所有军团
            ZombieLegionManager 管理器 = com.enhancedzombies.mod.EnhancedZombiesMod.军团管理器;
            if (管理器 != null) {
                int 清除数量 = 管理器.清除所有活跃军团(世界);
                
                源.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "§a成功清除了 " + 清除数量 + " 个僵尸和所有军团数据！"), true);
                return 1;
            } else {
                源.sendFailure(net.minecraft.network.chat.Component.literal(
                    "§c军团管理器未初始化！"));
                return 0;
            }
            
        } catch (Exception e) {
            源.sendFailure(net.minecraft.network.chat.Component.literal(
                "§c执行命令时出错: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 执行军团生成的核心逻辑
     * 算法：调用军团管理器生成军团并处理结果反馈
     * 时间复杂度：O(n) - n为军团大小
     */
    private static int 执行军团生成(CommandSourceStack 指令源, ServerPlayer 目标玩家, int 军团大小) {
        try {
            ZombieLegionManager 管理器 = ZombieLegionManager.getInstance();
            
            // 使用指定大小或默认大小
            boolean 生成成功;
            if (军团大小 > 0) {
                生成成功 = 管理器.强制生成指定大小军团(目标玩家, 军团大小);
            } else {
                生成成功 = 管理器.强制生成军团(目标玩家);
            }
            
            if (生成成功) {
                String 大小描述 = 军团大小 > 0 ? String.valueOf(军团大小) : "随机";
                
                指令源.sendSuccess(
                    () -> Component.literal(String.format(
                        "§6[增强僵尸] §a成功为玩家 %s 生成军团！\n" +
                        "§7军团大小: %s 个僵尸\n" +
                        "§7目标坐标: %d, %d, %d",
                        目标玩家.getName().getString(),
                        大小描述,
                        (int)目标玩家.getX(),
                        (int)目标玩家.getY(),
                        (int)目标玩家.getZ()
                    )), 
                    true
                );
                
                // 向目标玩家发送通知
                if (!指令源.getEntity().equals(目标玩家)) {
                    目标玩家.sendSystemMessage(Component.literal(
                        "§c§l[警告] §r§c管理员为您生成了僵尸军团！准备战斗！"
                    ));
                }
                
                EnhancedZombiesMod.LOGGER.info("管理员 {} 为玩家 {} 生成了军团，大小: {}", 
                    指令源.getTextName(), 目标玩家.getName().getString(), 大小描述);
                
                return 1;
            } else {
                指令源.sendFailure(Component.literal(
                    "§6[增强僵尸] §c军团生成失败！\n" +
                    "§7可能原因：找不到合适的生成位置或玩家不在有效世界中"
                ));
                
                return 0;
            }
            
        } catch (Exception e) {
            指令源.sendFailure(Component.literal(
                "§6[增强僵尸] §c生成军团时发生错误: " + e.getMessage()
            ));
            
            EnhancedZombiesMod.LOGGER.error("执行军团生成指令时发生错误: {}", e.getMessage(), e);
            return 0;
        }
    }
}