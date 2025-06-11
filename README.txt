
Enhanced Zombies Mod - Developer Documentation
===============================================

项目概述 | Project Overview
---------------------------
Enhanced Zombies 是一个基于 Minecraft Forge 47.4.1 的高级AI模组，
为 Minecraft 1.20.1 版本设计。本模组实现了智能僵尸系统，包括
方块破坏、建造能力和军团协调攻击机制。

Enhanced Zombies is an advanced AI mod based on Minecraft Forge 47.4.1,
designed for Minecraft 1.20.1. This mod implements intelligent zombie systems
including block breaking, building capabilities, and legion coordination attacks.

技术规格 | Technical Specifications
=====================================
- Minecraft Version: 1.20.1
- Forge Version: 47.4.1+
- Java Version: 17+
- Gradle Version: 8.x
- Mapping: Official Mojang Mappings
- License: BSD 2-Clause

开发环境设置 | Development Environment Setup
=============================================

前置要求 | Prerequisites
------------------------
1. Java Development Kit (JDK) 17 或更高版本
2. Git 版本控制系统
3. 至少 4GB 可用内存
4. 稳定的网络连接（用于下载依赖）

快速开始 | Quick Start
---------------------

1. 克隆项目 | Clone Project:
   git clone https://github.com/Am4191LOL/enhancedzombies.git
   cd enhancedzombies

2. 设置开发环境 | Setup Development Environment:
   
   对于 IntelliJ IDEA | For IntelliJ IDEA:
   - 打开 IDEA，选择 "Import Project"
   - 选择项目根目录下的 build.gradle 文件
   - 等待 Gradle 同步完成
   - 运行: ./gradlew genIntellijRuns
   - 刷新 Gradle 项目（如果需要）
   
   对于 Eclipse | For Eclipse:
   - 运行: ./gradlew genEclipseRuns
   - 打开 Eclipse
   - Import > Existing Gradle Project > 选择项目文件夹
   - 或运行: ./gradlew eclipse 生成项目文件

3. 构建项目 | Build Project:
   ./gradlew build

4. 运行开发环境 | Run Development Environment:
   ./gradlew runClient    # 客户端
   ./gradlew runServer    # 服务端
   ./gradlew runData      # 数据生成

常用 Gradle 任务 | Common Gradle Tasks
=====================================
./gradlew clean                    # 清理构建文件
./gradlew build                    # 构建模组
./gradlew runClient               # 启动客户端测试环境
./gradlew runServer               # 启动服务端测试环境
./gradlew runData                 # 运行数据生成器
./gradlew genEclipseRuns          # 生成 Eclipse 运行配置
./gradlew genIntellijRuns         # 生成 IntelliJ 运行配置
./gradlew --refresh-dependencies  # 刷新依赖缓存

项目结构 | Project Structure
============================
enhancedzombies/
├── src/main/java/                # Java 源代码
│   └── com/enhancedzombies/mod/  # 主包
├── src/main/resources/           # 资源文件
│   ├── META-INF/                 # 模组元数据
│   ├── assets/                   # 游戏资源
│   └── data/                     # 数据包
├── build.gradle                  # Gradle 构建脚本
├── gradle.properties             # Gradle 属性配置
└── README.md                     # 用户文档

核心系统架构 | Core System Architecture
=======================================

1. AI 系统 | AI System
   - 路径规划算法 (A* 寻路)
   - 方块破坏逻辑
   - 建造行为树
   - 目标优先级系统

2. 军团系统 | Legion System
   - 群体行为协调
   - 动态生成管理
   - 战术分工机制
   - 性能优化策略

3. 配置系统 | Configuration System
   - TOML 配置文件
   - 运行时配置重载
   - 客户端/服务端同步

代码规范 | Coding Standards
===========================

命名约定 | Naming Conventions
----------------------------
- 类名: PascalCase (例: ZombieAIController)
- 方法名: camelCase (例: calculatePathToTarget)
- 变量名: 中文命名 (例: 僵尸生成间隔, 军团大小限制)
- 常量: UPPER_SNAKE_CASE (例: MAX_LEGION_SIZE)
- 包名: 全小写 (例: com.enhancedzombies.mod.ai)

注释标准 | Comment Standards
---------------------------
/// <summary>
/// 类功能描述
/// 职责说明和主要功能
/// </summary>

/**
 * 方法功能描述
 * @param 参数名 参数说明
 * @return 返回值说明
 * @complexity 时间复杂度: O(n), 空间复杂度: O(1)
 */

性能优化指南 | Performance Optimization Guide
=============================================

1. 内存管理 | Memory Management
   - 使用对象池减少GC压力
   - 及时清理事件监听器
   - 避免在tick方法中创建大量临时对象

2. 计算优化 | Computation Optimization
   - 缓存昂贵的计算结果
   - 使用增量更新而非全量计算
   - 合理使用多线程（注意线程安全）

3. 网络优化 | Network Optimization
   - 减少不必要的数据包发送
   - 使用批量更新机制
   - 压缩大型数据结构

调试技巧 | Debugging Tips
=========================

1. 日志系统 | Logging System
   - 使用 LOGGER.debug() 进行详细调试
   - 关键路径添加性能监控日志
   - 错误处理使用 LOGGER.error() 并包含堆栈信息

2. 开发工具 | Development Tools
   - 使用 F3+H 显示详细信息
   - 利用 /forge generate 命令生成数据
   - 使用 JProfiler 进行性能分析

许可证信息 | License Information
=================================
本项目使用 BSD 2-Clause 许可证。Mojang 映射文件受到特定许可证约束，
详情请参考: https://github.com/MinecraftForge/MCPConfig/blob/master/Mojang.md

如果您不同意 Mojang 许可证条款，可以在 build.gradle 中更改为
社区众包的映射名称。

技术支持 | Technical Support
============================
- 官方文档: https://docs.minecraftforge.net/en/1.20.1/
- Forge 论坛: https://forums.minecraftforge.net/
- Forge Discord: https://discord.minecraftforge.net/
- 项目 Issues: https://github.com/Am4191LOL/enhancedzombies/issues

贡献指南 | Contributing Guidelines
==================================
1. Fork 项目到您的 GitHub 账户
2. 创建功能分支: git checkout -b feature/新功能名称
3. 提交更改: git commit -am '添加新功能'
4. 推送分支: git push origin feature/新功能名称
5. 创建 Pull Request

请确保您的代码符合项目的编码规范，并包含适当的测试。

联系信息 | Contact Information
==============================
- 开发者: Am4191LOL
- 团队: 木木工作室 | MuMu Studio
- 邮箱: [请通过 GitHub Issues 联系]
- GitHub: https://github.com/Am4191LOL/enhancedzombies

最后更新: 2025年1月
Last Updated: January 2025
