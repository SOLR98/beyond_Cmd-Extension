# Beyond:Cmd Extension

一个为 **Beyond Dimensions** 模组提供强大命令管理功能的 Minecraft Forge 模组。(完全由deepseek编写）

## ✨ 功能特性

### 🎯 核心功能
- **网络管理**: 查看、管理 Beyond Dimensions 网络
- **成员管理**: 添加/移除成员和管理员
- **资源操作**: 物品插入、资源生成
- **NBT分析**: OP手动触发的NBT大小计算

### 📊 网络信息命令
```bash
/bdtools network info <网络ID>        # 查看网络详细信息
/bdtools network info <网络ID> nbt   # 计算网络NBT大小（OP专用）
/bdtools network list                # 查看网络列表
/bdtools myNetworks                  # 查看我的网络
```

### 👥 成员管理命令
```bash
/bdtools member add <玩家> <网络ID>      # 添加成员
/bdtools member addManager <玩家> <网络ID> # 添加管理员
/bdtools member remove <玩家> <网络ID>   # 移除玩家
```

### 🛠️ 工具命令
```bash
/bdtools open <网络ID>                # 打开网络界面
/bdtools network giveTerminal         # 给予网络终端
/bdtools network giveEnchantedBooks   # 给予附魔书
```

## 🚀 快速开始

### 安装要求
1. **Minecraft**: 1.20.1
2. **Forge**: 47.3.33 或更高版本
3. **Beyond Dimensions**: 0.7.9 或更高版本

### 安装步骤
1. 下载最新版本的 `beyond_cmd_extension-0.2.0.jar`
2. 放入 Minecraft 的 `mods` 文件夹
3. 启动游戏

### 权限要求
所有命令需要 **OP权限2级**，但是部分功能可以只需要网络权限

## 🔧 NBT计算功能

### 功能说明
NBT计算功能允许服务器管理员分析网络中物品的NBT数据大小，帮助优化网络存储。

### 使用方式
```bash
# 计算指定网络的NBT大小
/bdtools network info 5 nbt

# 计算当前网络的NBT大小
/bdtools network info nbt
```

### 输出示例
```
=== NBT数据分析（手动触发） ===
    NBT物品种类: 10,257, 总大小: 11.9 MB
    警告: 检测到大量NBT数据（>1GB）！
    注意: NBT分析只包括带有NBT标签的物品。
```

### 技术特点
- **按种类统计**: 相同NBT标签只计算一次
- **完整遍历**: 处理网络中所有物品
- **准确估算**: 使用保守的NBT大小估算方法
- **多语言支持**: 完整的提示框文本翻译

## 🌐 多语言支持

### 支持语言
- **英语** (en_us) - 默认
- **中文** (zh_cn)

### 配置方法
编辑配置文件 `config/beyond_cmd_extension-server.toml`:
```toml
[language]
# 命令输出语言: en_us 或 zh_cn
command_language = "zh_cn"

[network_list]
# 每页显示的网络数量
max_networks_per_page = 10
```

## ⚙️ 配置选项

### 主配置文件
`config/beyond_cmd_extension-common.toml`

#### 附魔书分离设置
```toml
# 启用附魔书自动分离
enableEnchantmentSeparation = true

# 基础经验消耗
enchantmentSeparationBaseCost = 10

# 等级乘数
enchantmentSeparationLevelMultiplier = 5

# 高成本附魔列表
highCostEnchantments = [
    "minecraft:mending:3.0",      # 经验修补（3倍成本）
    "minecraft:silk_touch:1.5",   # 精准采集（1.5倍成本）
    "minecraft:sharpness:1.2",    # 锋利（1.2倍成本）
]
```

#### 网络传输设置
```toml
# 启用网络间物品传输（需要重启）
enableNetworkTransfer = false
```

## 📚 文档索引

### 🚀 快速开始
| 文档 | 描述 | 推荐 |
|------|------|------|
| **[命令速查表](COMMANDS_CHEATSHEET.md)** | 🚀 快速查阅常用命令 | 新用户首选 |
| **[命令简化指南](COMMANDS_SIMPLE.md)** | 📖 简明使用指南 | 日常参考 |

### 📖 详细参考
| 文档 | 描述 | 适合 |
|------|------|------|
| **[完整命令文档](COMMANDS.md)** | 📚 全面功能介绍 | 需要详细信息 |

### 🔧 技术参考
| 文档 | 描述 | 适合 |
|------|------|------|
| **[项目分析](PROJECT_ANALYSIS.md)** | 🔍 项目架构分析 | 开发者 |
| **[发布说明](RELEASE_NOTES.md)** | 📝 版本更新记录 | 所有用户 |

### 🔍 完整文档索引
**[查看所有文档](DOCUMENTATION_INDEX.md)** - 文档分类、用途和阅读建议

## 📖 命令参考

### 网络管理命令
| 命令 | 描述 | 权限 |
|------|------|------|
| `/bdtools network list` | 查看网络列表 | OP 2 |
| `/bdtools network info <ID>` | 查看网络信息 | 网络成员 |
| `/bdtools network info <ID> nbt` | 计算NBT大小 | OP 2 |
| `/bdtools network insert <ID>` | 插入物品到网络 | 网络成员 |
| `/bdtools network generateResources` | 生成测试资源 | OP 2 |

### 成员管理命令
| 命令 | 描述 | 权限 |
|------|------|------|
| `/bdtools member add <玩家> <ID>` | 添加成员 | 网络所有者/管理员 |
| `/bdtools member addManager <玩家> <ID>` | 添加管理员 | 网络所有者 |
| `/bdtools member remove <玩家> <ID>` | 移除玩家 | 网络所有者/管理员 |
| `/bdtools member removeManager <玩家> <ID>` | 移除管理员 | 网络所有者 |

### 个人命令
| 命令 | 描述 | 权限 |
|------|------|------|
| `/bdtools myNetworks` | 查看我的网络 | 玩家 |
| `/bdtools open <ID>` | 打开网络界面 | 网络成员 |

### 工具命令
| 命令 | 描述 | 权限 |
|------|------|------|
| `/bdtools network giveTerminal` | 给予网络终端 | OP 2 |
| `/bdtools network giveEnchantedBooks` | 给予附魔书 | OP 2 |
| `/bdtools network batchCreate` | 批量创建网络 | OP 2 |

## 🏗️ 项目结构

```
src/main/java/com/solr98/beyondcmdextension/
├── Beyond_cmd_extension.java          # 主模组类
├── Config.java                        # 配置文件
├── CommandConfig.java                 # 命令配置
├── command/                           # 命令系统
│   ├── BDNetworkCommands.java        # 命令注册
│   ├── CommandLang.java              # 多语言支持
│   ├── network/                      # 网络命令
│   ├── member/                       # 成员命令
│   └── util/                         # 工具类
├── handler/                          # 事件处理器
└── client/                           # 客户端代码 （未来会尝试提供客户端功能）
```

## 🔍 技术细节

### 权限系统
- **OP权限验证**: 检查玩家OP等级
- **网络访问权限**: 验证玩家网络成员身份
- **管理权限**: 区分所有者和管理员权限

### 输出格式化
- **SI单位制**: 使用 K, M, G, T, P, E 单位 （这玩意可不好决定所以就按照国际单位制（SI）的数值单位了）
- **悬停文本**: 丰富的鼠标悬停信息
- **分页系统**: 支持大量数据的分页显示

### 错误处理
- **输入验证**: 网络ID、玩家名格式验证
- **权限检查**: 操作前验证权限
- **友好提示**: 清晰的错误信息

## 🚨 注意事项

### 性能考虑
1. **NBT计算**: 处理大量物品时可能消耗较多内存
2. **网络列表**: 扫描所有网络（0-9999）需要时间
3. **建议**: 在服务器空闲时运行资源密集型命令

### 权限安全
1. **命令权限**: 所有命令需要OP权限2级
2. **网络操作**: 需要相应的网络权限
3. **管理操作**: 敏感操作需要所有者权限

### 兼容性
1. **必需**: Beyond Dimensions 0.7.9+
2. **推荐**: 单独使用，避免与其他命令模组冲突
3. **测试**: 已在测试环境中验证功能

## 📈 版本历史

### v0.2.0 (当前)
- ✅ 新增NBT计算功能
- ✅ 改进命令输出格式
- ✅ 添加完整的多语言支持
- ✅ 修复多个命令功能
- ✅ 优化用户体验

### v0.1.0
- ✅ 基础命令框架
- ✅ 网络管理功能
- ✅ 成员管理功能
- ✅ 基本配置系统

## 🤝 贡献指南

### 报告问题
1. 在GitHub Issues中创建新问题
2. 描述详细的重现步骤
3. 提供相关日志和版本信息

## 📄 许可证

本项目采用 **MIT许可证** - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 支持与联系

- **GitHub**: [SOL-R98/beyond_cmd_extension](https://github.com/SOL-R98/beyond_cmd_extension)
- **问题反馈**: [GitHub Issues](https://github.com/SOL-R98/beyond_cmd_extension/issues)
- **作者**: SOL_R98

---

**注意**: 本模组需要 Beyond Dimensions 模组才能正常工作。请确保已安装正确版本的依赖模组。

**Happy Mining!** ⛏️
