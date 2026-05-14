# 📱 Expense Tracker（记账应用）

一个基于 Android 平台的本地记账应用，帮助用户记录日常收支，支持分类管理、广播通知和服务后台运行。

## 📌 项目简介

Expense Tracker 是一款轻量级的本地记账应用，采用 Java 语言编写，适合 Android Studio 开发环境。  
项目以**教学实践**为目标，结合了 Android 四大组件中的 Activity、Service、BroadcastReceiver，并演示了 RecyclerView、本地广播通信、后台服务等核心技术的使用。

## ✨ 功能特性

- **📝 交易记录管理**
  - 添加、删除、查看交易记录
  - 支持收入和支出分类
  - 支持自定义备注和日期

- **🔔 广播通知**
  - 交易新增时自动发送广播
  - 数据更新时主动通知 UI 刷新
  - 支持自定义广播 Action

- **🔄 后台服务**
  - 应用启动时自动启动后台服务
  - 服务持续运行，支持定时任务

- **📊 数据统计**
  - 自动统计交易笔数、总收入、总支出
  - 实时显示净额（结余）

- **🎨 用户界面**
  - 基于 RecyclerView 的列表展示
  - 每个交易项左侧显示类别图标
  - 支持长按删除操作

## 🧰 技术栈

- **语言**：Java
- **开发工具**：Android Studio
- **最低 API 级别**：24（Android 7.0）
- **目标 API 级别**：34（Android 14）
- **核心技术**：
  - RecyclerView + Adapter
  - BroadcastReceiver（本地广播）
  - Service（后台服务）
  - SharedPreferences（数据持久化）
  - Vector Drawable（矢量图标）

## 🚀 快速开始

### 1️⃣ 克隆项目

```bash
git clone https://github.com/your-username/expense-tracker.git
```

### 2️⃣ 导入项目

- 打开 Android Studio
- 选择 **File → Open**
- 选择项目根目录 `expense-tracker/`
- 等待 Gradle 同步完成

### 3️⃣ 运行项目

- 连接 Android 设备或启动模拟器
- 点击 **Run** 按钮（或按 `Shift + F10`）

## 📁 项目结构

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/expensetracker/
│   │   │   ├── MainActivity.java          # 主界面
│   │   │   ├── TransactionAdapter.java    # 交易列表适配器
│   │   │   ├── Transaction.java           # 交易数据模型
│   │   │   ├── TransactionDataSource.java # 交易数据源
│   │   │   ├── ExpenseBroadcastReceiver.java # 广播接收器
│   │   │   ├── BroadcastConstants.java    # 广播常量定义
│   │   │   └── NewsBackgroundService.java # 后台服务
│   │   ├── res/
│   │   │   ├── drawable/                  # 图标资源
│   │   │   ├── layout/                    # 布局文件
│   │   │   └── values/                    # 颜色、字符串等
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

## 📖 使用说明

### 添加交易记录

- 在主界面点击“添加交易”按钮
- 填写金额、选择类别、输入备注
- 保存后自动刷新列表并发送广播

### 查看交易列表

- 所有交易以 RecyclerView 形式展示
- 每个交易项显示：
  - 类别图标（左侧）
  - 金额（右侧，颜色区分收支）
  - 日期和备注

### 删除交易记录

- 长按任意交易项，弹出删除确认
- 确认后删除并从列表中移除

### 查看统计信息

- 顶部状态栏显示：
  - 总交易笔数
  - 总支出
  - 总收入
  - 净额（结余）

## ⚙️ 自定义配置

### 修改类别图标

编辑 `TransactionAdapter.java` 中的 `setCategoryIcon()` 方法，可自定义类别与图标的映射关系：

```java
private void setCategoryIcon(ImageView imageView, String category) {
    switch (category) {
        case "餐饮":
            iconResId = R.drawable.ic_food;
            break;
        // 添加更多类别...
    }
}
```

### 修改广播行为

编辑 `ExpenseBroadcastReceiver.java` 中的 `onReceive()` 方法，可自定义广播处理逻辑。

## ❓ 常见问题

**Q：为什么我的广播没有收到？**  
A：请确保广播接收器已正确注册，并使用 `LocalBroadcastManager` 发送广播。

**Q：列表数据没有刷新？**  
A：检查 Adapter 是否调用了 `notifyDataSetChanged()` 方法。

**Q：应用退到后台后服务还在运行吗？**  
A：后台服务默认会在应用退到后台时继续运行，但系统可能会根据内存情况杀死服务。

## 📝 许可证

本项目仅供学习和教学使用，遵循 MIT 许可证。

## 🤝 贡献

欢迎提交 Issue 或 Pull Request 改进本项目。

---

> 如有任何问题，请联系项目维护者或提交 GitHub Issue。
