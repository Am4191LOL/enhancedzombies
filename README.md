# Enhanced Zombies | 增强僵尸模组

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net/)
[![Forge Version](https://img.shields.io/badge/Forge-47.4.1-orange.svg)](https://files.minecraftforge.net/)
[![License](https://img.shields.io/badge/License-BSD%202--Clause-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.0.0-red.svg)](https://github.com/Am4191LOL/enhancedzombies/releases)

## 📖 模组简介 | Description

**Enhanced Zombies** 是一个革命性的我的世界模组，它将普通的僵尸转变为具有智能战术的威胁。通过先进的AI系统，僵尸现在能够破坏和建造方块，形成军团进行协调攻击，为玩家带来前所未有的挑战体验。

**Enhanced Zombies** is a revolutionary Minecraft mod that transforms ordinary zombies into intelligent, strategic threats. Through an advanced AI system, zombies can now break and build blocks, form legions for coordinated attacks, providing players with unprecedented challenging experiences.

## ✨ 核心特性 | Key Features

### 🧠 智能AI系统 | Smart AI System
- **方块破坏能力** | Block Breaking: 僵尸可以智能地破坏阻挡路径的方块
- **建造能力** | Building Capability: 僵尸能够搭建简单结构来到达目标
- **路径规划** | Path Planning: 高级寻路算法，绕过复杂地形
- **目标优先级** | Target Prioritization: 智能选择攻击目标

### 👥 军团系统 | Legion System
- **协调攻击** | Coordinated Attacks: 多个僵尸协同作战
- **可配置军团大小** | Configurable Legion Size: 自定义军团规模（默认10-30只）
- **动态生成间隔** | Dynamic Spawn Intervals: 可调节的生成频率
- **战术分工** | Tactical Division: 不同僵尸承担不同角色

### ⚙️ 高度可配置 | Highly Configurable
- **难度调节** | Difficulty Scaling: 多级难度设置
- **性能优化** | Performance Optimization: 智能资源管理
- **兼容性保证** | Compatibility Assurance: 与其他模组良好兼容

## 🚀 安装指南 | Installation Guide

### 系统要求 | System Requirements
- **Minecraft**: 1.20.1
- **Minecraft Forge**: 47.4.1 或更高版本
- **Java**: 17 或更高版本
- **内存**: 建议至少 4GB RAM

### 安装步骤 | Installation Steps

1. **下载前置** | Download Prerequisites
   - 确保已安装 [Minecraft Forge 47.4.1+](https://files.minecraftforge.net/)
   - 确认 Minecraft 版本为 1.20.1

2. **下载模组** | Download Mod
   - 从 [Releases](https://github.com/Am4191LOL/enhancedzombies/releases) 页面下载最新版本
   - 或从 [CurseForge](https://www.curseforge.com/) 下载

3. **安装模组** | Install Mod
   ```
   将 .jar 文件放入以下目录：
   Windows: %appdata%\.minecraft\mods\
   macOS: ~/Library/Application Support/minecraft/mods/
   Linux: ~/.minecraft/mods/
   ```

4. **启动游戏** | Launch Game
   - 启动 Minecraft Launcher
   - 选择 Forge 配置文件
   - 开始游戏！

## 🎮 游戏玩法 | Gameplay

### 基础机制 | Basic Mechanics
- 僵尸会主动寻找并攻击玩家
- 能够破坏木制和石制方块（可配置）
- 在夜晚或黑暗环境中更加活跃
- 军团会协调进攻，包围玩家

### 生存策略 | Survival Strategies
- **建造高墙**: 使用更坚固的材料（如黑曜石）
- **照明防护**: 充足的光照可以减少僵尸生成
- **陷阱设计**: 利用僵尸的AI特性设计陷阱
- **快速移动**: 保持机动性，避免被包围

## ⚙️ 配置选项 | Configuration

模组提供丰富的配置选项，位于 `config/enhancedzombies-common.toml`：

```toml
[legion]
    # 军团最小大小 | Minimum legion size
    min_legion_size = 3
    # 军团最大大小 | Maximum legion size  
    max_legion_size = 8
    # 生成间隔（tick） | Spawn interval in ticks
    spawn_interval = 1200

[abilities]
    # 启用方块破坏 | Enable block breaking
    enable_block_breaking = true
    # 启用建造能力 | Enable building capability
    enable_building = true
    # 可破坏的方块类型 | Breakable block types
    breakable_blocks = ["minecraft:wood", "minecraft:stone"]
```

## 🔧 开发者信息 | Developer Information

### 构建项目 | Building the Project

```bash
# 克隆仓库 | Clone repository
git clone https://github.com/Am4191LOL/enhancedzombies.git
cd enhancedzombies

# 构建模组 | Build mod
./gradlew build

# 运行开发环境 | Run development environment
./gradlew runClient
```

### 技术栈 | Tech Stack
- **语言**: Java 17
- **框架**: Minecraft Forge 47.4.1
- **构建工具**: Gradle 8.x
- **映射**: Official Mojang Mappings

## 🤝 贡献指南 | Contributing

我们欢迎社区贡献！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详细信息。

We welcome community contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### 报告问题 | Reporting Issues
- 使用 [GitHub Issues](https://github.com/Am4191LOL/enhancedzombies/issues) 报告 Bug
- 提供详细的重现步骤和环境信息
- 包含相关的日志文件

## 📄 许可证 | License

本项目采用 BSD 2-Clause 许可证。详见 [LICENSE](LICENSE) 文件。

This project is licensed under the BSD 2-Clause License. See the [LICENSE](LICENSE) file for details.

## 🙏 致谢 | Acknowledgments

- **Minecraft Forge Team** - 提供优秀的模组开发框架
- **Mojang Studios** - 创造了精彩的 Minecraft 世界
- **模组社区** - 持续的支持和反馈

## 📞 联系我们 | Contact

- **作者**: Am4191LOL
- **团队**: 木木工作室 | MuMu Studio
- **GitHub**: [enhancedzombies](https://github.com/Am4191LOL/enhancedzombies)
- **问题反馈**: [Issues](https://github.com/Am4191LOL/enhancedzombies/issues)

---

**享受增强的僵尸挑战！| Enjoy the Enhanced Zombie Challenge!** 🧟‍♂️⚔️
