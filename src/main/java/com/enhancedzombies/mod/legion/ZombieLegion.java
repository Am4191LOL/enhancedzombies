package com.enhancedzombies.mod.legion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 僵尸军团数据类
 * 功能：存储和管理单个军团的所有信息
 * 职责：
 * 1. 维护军团成员列表和军团长信息
 * 2. 跟踪军团状态和目标
 * 3. 管理军团的生命周期
 * 4. 提供军团协作所需的数据接口
 */
public class ZombieLegion {
    
    // 基本信息
    private final int 军团ID;
    private final UUID 目标玩家UUID;
    private final BlockPos 初始生成位置;
    private final long 创建时间;
    
    // 军团成员管理
    private final Set<UUID> 成员列表 = ConcurrentHashMap.newKeySet();
    private UUID 军团长UUID;
    private final Map<UUID, Long> 成员最后活跃时间 = new ConcurrentHashMap<>();
    
    // 军团状态
    private 军团状态 当前状态;
    private BlockPos 当前目标位置;
    private long 上次状态更新时间;
    private int 连续失败次数;
    
    // 军团统计
    private int 总击杀数;
    private int 总死亡数;
    private long 总存活时间;
    
    // 军团行为参数
    private double 军团凝聚度; // 0.0-1.0，表示军团成员的聚集程度
    private double 军团士气;   // 0.0-1.0，影响战斗效率
    private 战术模式 当前战术;
    
    /**
     * 军团状态枚举
     */
    public enum 军团状态 {
        初始化,     // 刚创建，成员还在生成
        集结中,     // 成员正在向目标集结
        追击中,     // 正在追击目标玩家
        战斗中,     // 与玩家或其他敌对实体战斗
        搜索中,     // 失去目标，正在搜索
        撤退中,     // 受到重大损失，正在撤退
        解散中,     // 军团即将解散
        已解散      // 军团已解散
    }
    
    /**
     * 战术模式枚举
     */
    public enum 战术模式 {
        直接攻击,   // 直接冲向目标
        包围战术,   // 从多个方向包围
        游击战术,   // 打了就跑的游击战
        防御战术,   // 建立防御阵地
        潜行战术,   // 尝试隐蔽接近
        分散战术    // 分散攻击多个目标
    }
    
    /**
     * 构造函数
     */
    public ZombieLegion(int 军团ID, UUID 目标玩家UUID, BlockPos 初始生成位置) {
        this.军团ID = 军团ID;
        this.目标玩家UUID = 目标玩家UUID;
        this.初始生成位置 = 初始生成位置;
        this.创建时间 = System.currentTimeMillis();
        this.当前状态 = 军团状态.初始化;
        this.上次状态更新时间 = 创建时间;
        this.军团凝聚度 = 1.0;
        this.军团士气 = 1.0;
        this.当前战术 = 战术模式.直接攻击;
        this.连续失败次数 = 0;
    }
    
    /**
     * 更新军团状态
     * 时间复杂度：O(n) - n为成员数量
     */
    public void 更新状态() {
        long 当前时间 = System.currentTimeMillis();
        
        // 清理无效成员
        清理无效成员();
        
        // 更新军团统计
        更新军团统计();
        
        // 根据当前情况更新状态
        军团状态 新状态 = 计算新状态();
        if (新状态 != 当前状态) {
            切换状态(新状态);
        }
        
        // 更新战术模式
        更新战术模式();
        
        // 更新军团属性
        更新军团属性();
        
        上次状态更新时间 = 当前时间;
    }
    
    /**
     * 计算新的军团状态
     */
    private 军团状态 计算新状态() {
        // 如果没有成员，应该解散
        if (成员列表.isEmpty()) {
            return 军团状态.已解散;
        }
        
        // 如果成员太少，考虑解散
        if (成员列表.size() < 3 && 当前状态 != 军团状态.初始化) {
            连续失败次数++;
            if (连续失败次数 > 5) {
                return 军团状态.解散中;
            }
        } else {
            连续失败次数 = 0;
        }
        
        // 根据当前状态和条件决定新状态
        switch (当前状态) {
            case 初始化:
                // 如果大部分成员已生成，进入集结状态
                if (成员列表.size() >= 5) {
                    return 军团状态.集结中;
                }
                break;
                
            case 集结中:
                // 检查是否已经集结完成
                if (检查军团是否集结()) {
                    return 军团状态.追击中;
                }
                break;
                
            case 追击中:
                // 检查是否接近目标或进入战斗
                if (检查是否进入战斗()) {
                    return 军团状态.战斗中;
                }
                // 检查是否失去目标
                if (检查是否失去目标()) {
                    return 军团状态.搜索中;
                }
                break;
                
            case 战斗中:
                // 检查战斗是否结束
                if (!检查是否进入战斗()) {
                    if (检查是否失去目标()) {
                        return 军团状态.搜索中;
                    } else {
                        return 军团状态.追击中;
                    }
                }
                // 检查是否需要撤退
                if (检查是否需要撤退()) {
                    return 军团状态.撤退中;
                }
                break;
                
            case 搜索中:
                // 检查是否重新发现目标
                if (!检查是否失去目标()) {
                    return 军团状态.追击中;
                }
                // 搜索超时，解散军团
                if (System.currentTimeMillis() - 上次状态更新时间 > 300000) { // 5分钟
                    return 军团状态.解散中;
                }
                break;
                
            case 撤退中:
                // 检查是否撤退完成
                if (检查撤退是否完成()) {
                    return 军团状态.搜索中;
                }
                break;
                
            case 解散中:
                // 解散过程完成
                return 军团状态.已解散;
        }
        
        return 当前状态;
    }
    
    /**
     * 切换军团状态
     */
    private void 切换状态(军团状态 新状态) {
        军团状态 旧状态 = 当前状态;
        当前状态 = 新状态;
        
        // 执行状态切换时的特殊逻辑
        switch (新状态) {
            case 集结中:
                // 重置战术为直接攻击
                当前战术 = 战术模式.直接攻击;
                break;
                
            case 追击中:
                // 根据军团大小选择战术
                if (成员列表.size() >= 8) {
                    当前战术 = 战术模式.包围战术;
                } else {
                    当前战术 = 战术模式.直接攻击;
                }
                break;
                
            case 战斗中:
                // 提高士气
                军团士气 = Math.min(1.0, 军团士气 + 0.2);
                break;
                
            case 撤退中:
                // 降低士气
                军团士气 = Math.max(0.1, 军团士气 - 0.3);
                当前战术 = 战术模式.分散战术;
                break;
                
            case 解散中:
                // 开始解散流程
                开始解散流程();
                break;
        }
    }
    
    /**
     * 更新战术模式
     */
    private void 更新战术模式() {
        // 根据军团状态、成员数量、士气等因素调整战术
        if (当前状态 == 军团状态.战斗中) {
            if (军团士气 < 0.3) {
                当前战术 = 战术模式.游击战术;
            } else if (成员列表.size() >= 10) {
                当前战术 = 战术模式.包围战术;
            } else if (军团凝聚度 < 0.5) {
                当前战术 = 战术模式.分散战术;
            }
        }
    }
    
    /**
     * 更新军团属性
     */
    private void 更新军团属性() {
        // 更新凝聚度（基于成员之间的距离）
        更新军团凝聚度();
        
        // 更新士气（基于战斗结果和损失）
        更新军团士气();
        
        // 更新存活时间
        总存活时间 = System.currentTimeMillis() - 创建时间;
    }
    
    /**
     * 更新军团凝聚度
     */
    private void 更新军团凝聚度() {
        // 简化版本：基于成员数量变化
        if (成员列表.size() >= 8) {
            军团凝聚度 = Math.min(1.0, 军团凝聚度 + 0.05);
        } else if (成员列表.size() <= 3) {
            军团凝聚度 = Math.max(0.1, 军团凝聚度 - 0.1);
        }
    }
    
    /**
     * 更新军团士气
     */
    private void 更新军团士气() {
        // 基于成员数量和时间的士气衰减
        double 衰减率 = 0.001;
        if (成员列表.size() < 5) {
            衰减率 *= 2;
        }
        
        军团士气 = Math.max(0.1, 军团士气 - 衰减率);
    }
    
    /**
     * 清理无效成员
     */
    private void 清理无效成员() {
        long 当前时间 = System.currentTimeMillis();
        Iterator<UUID> 迭代器 = 成员列表.iterator();
        
        while (迭代器.hasNext()) {
            UUID 成员UUID = 迭代器.next();
            Long 最后活跃时间 = 成员最后活跃时间.get(成员UUID);
            
            // 如果成员超过5分钟没有活跃，认为已死亡或离线
            if (最后活跃时间 == null || 当前时间 - 最后活跃时间 > 300000) {
                迭代器.remove();
                成员最后活跃时间.remove(成员UUID);
                
                // 如果移除的是军团长，选择新的军团长
                if (成员UUID.equals(军团长UUID)) {
                    选择新军团长();
                }
                
                总死亡数++;
            }
        }
    }
    
    /**
     * 选择新的军团长
     */
    private void 选择新军团长() {
        if (成员列表.isEmpty()) {
            军团长UUID = null;
            return;
        }
        
        // 选择最早加入的成员作为新军团长
        UUID 新军团长 = null;
        long 最早时间 = Long.MAX_VALUE;
        
        for (UUID 成员UUID : 成员列表) {
            Long 活跃时间 = 成员最后活跃时间.get(成员UUID);
            if (活跃时间 != null && 活跃时间 < 最早时间) {
                最早时间 = 活跃时间;
                新军团长 = 成员UUID;
            }
        }
        
        军团长UUID = 新军团长;
    }
    
    /**
     * 更新军团统计
     */
    private void 更新军团统计() {
        // 这里可以添加更复杂的统计逻辑
        // 例如：计算平均战斗时间、效率等
    }
    
    // 状态检查方法
    private boolean 检查军团是否集结() {
        // 简化版本：如果有足够的成员，认为已集结
        return 成员列表.size() >= 5;
    }
    
    private boolean 检查是否进入战斗() {
        // 简化版本：基于状态和时间判断
        return 当前状态 == 军团状态.战斗中;
    }
    
    private boolean 检查是否失去目标() {
        // 简化版本：基于时间判断
        return System.currentTimeMillis() - 上次状态更新时间 > 60000; // 1分钟
    }
    
    private boolean 检查是否需要撤退() {
        // 如果成员数量过少或士气过低
        return 成员列表.size() < 3 || 军团士气 < 0.2;
    }
    
    private boolean 检查撤退是否完成() {
        // 简化版本：撤退一定时间后认为完成
        return System.currentTimeMillis() - 上次状态更新时间 > 30000; // 30秒
    }
    
    /**
     * 开始解散流程
     */
    private void 开始解散流程() {
        // 清理所有数据
        成员列表.clear();
        成员最后活跃时间.clear();
        军团长UUID = null;
    }
    
    // 公共接口方法
    
    /**
     * 添加成员
     */
    public void 添加成员(UUID 成员UUID) {
        成员列表.add(成员UUID);
        成员最后活跃时间.put(成员UUID, System.currentTimeMillis());
    }
    
    /**
     * 移除成员
     */
    public void 移除成员(UUID 成员UUID) {
        成员列表.remove(成员UUID);
        成员最后活跃时间.remove(成员UUID);
        
        if (成员UUID.equals(军团长UUID)) {
            选择新军团长();
        }
        
        总死亡数++;
    }
    
    /**
     * 更新成员活跃时间
     */
    public void 更新成员活跃时间(UUID 成员UUID) {
        if (成员列表.contains(成员UUID)) {
            成员最后活跃时间.put(成员UUID, System.currentTimeMillis());
        }
    }
    
    /**
     * 检查军团是否应该被清理
     */
    public boolean 应该被清理() {
        // 基本清理条件
        if (当前状态 == 军团状态.已解散 || 
            (成员列表.isEmpty() && System.currentTimeMillis() - 创建时间 > 60000)) {
            return true;
        }
        
        // 距离检查：如果目标玩家距离军团过远，清理军团
        try {
            net.minecraft.server.MinecraftServer 服务器 = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (服务器 != null) {
                for (net.minecraft.server.level.ServerLevel 世界 : 服务器.getAllLevels()) {
                    net.minecraft.world.entity.player.Player 目标玩家 = 世界.getPlayerByUUID(目标玩家UUID);
                    if (目标玩家 != null) {
                        double 距离 = 目标玩家.blockPosition().distSqr(初始生成位置);
                        // 如果玩家距离军团超过128格（16384 = 128^2），清理军团
                        if (距离 > 16384) {
                            return true;
                        }
                        break; // 找到玩家就退出循环
                    }
                }
            }
        } catch (Exception e) {
            // 如果检查过程中出现异常，不清理军团
        }
        
        return false;
    }
    
    // Getter方法
    public int get军团ID() { return 军团ID; }
    public UUID get目标玩家UUID() { return 目标玩家UUID; }
    public BlockPos get初始生成位置() { return 初始生成位置; }
    public long get创建时间() { return 创建时间; }
    public Set<UUID> get成员列表() { return new HashSet<>(成员列表); }
    public UUID get军团长UUID() { return 军团长UUID; }
    public 军团状态 get当前状态() { return 当前状态; }
    public BlockPos get当前目标位置() { return 当前目标位置; }
    public int get成员数量() { return 成员列表.size(); }
    public double get军团凝聚度() { return 军团凝聚度; }
    public double get军团士气() { return 军团士气; }
    public 战术模式 get当前战术() { return 当前战术; }
    public int get总击杀数() { return 总击杀数; }
    public int get总死亡数() { return 总死亡数; }
    public long get总存活时间() { return 总存活时间; }
    
    // Setter方法
    public void set军团长UUID(UUID 军团长UUID) { this.军团长UUID = 军团长UUID; }
    public void set当前目标位置(BlockPos 位置) { this.当前目标位置 = 位置; }
    public void 增加击杀数() { this.总击杀数++; }
    
    /**
     * 获取军团详细信息
     */
    public String get详细信息() {
        return String.format(
            "军团ID: %d, 状态: %s, 成员: %d, 战术: %s, 士气: %.2f, 凝聚度: %.2f, 击杀: %d, 死亡: %d",
            军团ID, 当前状态, 成员列表.size(), 当前战术, 军团士气, 军团凝聚度, 总击杀数, 总死亡数
        );
    }
}