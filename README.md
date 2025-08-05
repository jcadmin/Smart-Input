# 智能输入法专业版 (Smart Input Pro)

一个为IntelliJ IDEA开发的智能输入法切换插件，能够根据代码上下文自动切换中英文输入法。

## ✨ 功能特性

- 🎯 **智能上下文检测**：自动识别代码、注释、字符串、文档等不同区域
- 🔄 **自动输入法切换**：根据光标位置智能切换中英文输入法
- 🎨 **可视化指示器**：实时显示当前输入法状态
- ⚙️ **丰富配置选项**：支持个性化配置和调试模式
- 🌍 **跨平台支持**：支持Windows、macOS、Linux系统

## 🚀 快速开始

### 安装插件
1. 构建插件：`./gradlew buildPlugin`
2. 安装插件包：`build/distributions/SmartInputPro-1.0.0.zip`
3. 重启IntelliJ IDEA

### 基本配置
1. 打开设置：`File` → `Settings` → `Tools` → `Smart Input Pro`
2. 启用插件：✅ **启用智能输入法专业版**
3. 配置上下文规则：
   - 代码区域 → 英文输入法
   - 注释区域 → 中文输入法

### 快捷键
- `Ctrl + Alt + I`：启用/禁用插件
- `Ctrl + Shift + I`：手动切换输入法

## 🛠️ 开发构建

### 环境要求
- IntelliJ IDEA 2024.1+
- JDK 17+
- Kotlin 1.9+

### 构建命令
```bash
# 构建插件
./gradlew buildPlugin

# 启动开发IDE
./gradlew runIde

# 运行测试
./gradlew test
```

## 📁 项目结构

```
SmartInputPro/
├── src/main/kotlin/com/smartinput/pro/
│   ├── action/           # 用户动作处理
│   ├── analyzer/         # 智能上下文分析
│   ├── config/          # 配置界面
│   ├── indicator/       # 可视化指示器
│   ├── listener/        # 事件监听器
│   ├── model/           # 数据模型
│   ├── platform/        # 跨平台支持
│   ├── service/         # 核心服务
│   └── startup/         # 启动活动
├── build.gradle.kts     # 构建配置
└── README.md           # 项目说明
```

## 🔧 故障排除

### 输入法不切换
1. 确保插件已启用
2. 检查输入法配置
3. 启用调试模式查看日志
4. 尝试以管理员权限运行IDE

### 查看调试日志
1. 启用调试模式：`Settings` → `Tools` → `Smart Input Pro` → ✅ `调试模式`
2. 查看日志：`Help` → `Show Log in Files`
3. 搜索"SmartInput"相关日志

## 📄 许可证

MIT License - 详见LICENSE文件

## 🤝 贡献

欢迎提交Issue和Pull Request！
