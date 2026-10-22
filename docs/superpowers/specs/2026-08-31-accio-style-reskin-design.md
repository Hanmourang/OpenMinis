# OpenMinis Android 端 Accio 风格界面优化设计

日期：2026-08-31
范围：核心界面层（聊天主页 / 输入栏 / 会话抽屉 / 弹窗组件）
参考：Accio Work 手机端截图（5 张：主界面、抽屉、对话页、两个底部弹窗）

## 目标与边界

- 参考 Accio Work 的视觉语言，优化 OpenMinis Android 端核心界面层的样式、布局与背景
- 唯一新增功能：**环境/模型切换器**（输入栏下方的模型组切换 pill）
- 明确不做：设置等深层页面重排、建议词条 chips、新对话黑色主按钮、AI 生成声明文案
- 行为逻辑零改动：44 个聊天相关文件的功能逻辑不动，只动呈现层

## 方案

方案 A：新增 Accio 设计令牌层（`ui/theme/AccioTokens.kt` + 一组组件），定向应用到核心界面层；全局 Material3 主色换为薄荷绿。深色模式提供整套暗色变体。

## 1. 设计令牌（AccioTokens.kt）

### 颜色

| 令牌 | 浅色 | 深色 |
|---|---|---|
| brand（主绿） | `#00C16E` | `#3DD68C` |
| brandSoft（浅绿底） | `#DFF7EC` | `#123324` |
| gradientTop（渐变起） | `#D9F5E8` | `#0A1F17` |
| gradientEnd（渐变止） | `#FFFFFF` | `#000000` |
| card（卡片） | `#FFFFFF` | `#1C1C1E` |
| cardBorder | `#14000000` | `#33FFFFFF` |
| textPrimary | `#1A1A1A` | `#F2F2F2` |
| textSecondary | `#6B6B6B` | `#9E9E9E` |
| statusOnline | `#00C16E` | `#3DD68C` |
| statusOffline | `#FF3B30` | `#FF453A` |

- 全局 M3 `primary` 同步改为 brand 绿（`Theme.kt` 的 `TealPrimary/TealDarkPrimary`，保持 Teal 命名不动 90+ 调用点）
- ChatPalette 不动（聊天消息区语义色保持现有体系），令牌层独立

### 形状与尺寸

- 卡片圆角 24dp（输入卡 24dp、信息卡 20dp）
- 弹窗顶部圆角 28dp + 拖拽条 32×4dp（`textSecondary` 30% 透明度）
- 胶囊组件（pill）全圆角，高度 32dp，水平内边距 12dp
- 状态点 8dp 圆形

### 渐变背景

- 聊天主页/会话抽屉根容器：垂直渐变 `gradientTop` → `gradientEnd`，过渡点在高度 40% 处
- 仅在浅色模式有明显观感；深色模式为暗绿到黑的低饱和渐变

## 2. 聊天主页（ChatScreen.kt）

### 头部（对齐 Accio 对话页范式，非主页范式）

ChatScreen 是压栈页（导航图 startDestination 为会话列表，ChatScreen 带返回键），因此头部对齐 Accio 对话页（截图 5）而非主页：
- 左上：返回按钮包一层圆形浅底（brandSoft 底），行为不变
- 居中：粗体标题（17sp SemiBold，现有 displayTitle 兜底链不变）+ 副标题行（现有组名+provider·model 两行结构不变，整体改为薄荷绿胶囊视觉：brandSoft 底 + brand 状态点），点击打开现有 ChatModelPickerSheet；fallback 红色脉冲动画保留
- 右侧：现有 MoreVert 菜单按钮包一层胶囊浅底，样式统一

### 空态（无消息时）

- 居中布局：Soul 头像（复用 SoulStore 元数据，无头像则用首字母圆形占位，brandSoft 底）→ Soul 名称（17sp SemiBold）→ 副标题（13sp textSecondary，取 Soul 描述，无则 "你的本地 AI 助手"）→ 模型切换胶囊（同头部右侧胶囊的复用组件）
- 有消息后空态消失，消息列表照常渲染在渐变背景上（消息气泡色不变）

### 背景

- Scaffold containerColor 从 `ChatColors.background` 改为透明，外层 Box 铺 Accio 渐变

## 3. 输入栏 + 环境/模型切换器（ChatScreen.kt 内联输入区）

### 输入卡片

- 白色圆角 24dp 卡片，浅色带 1dp cardBorder 描边 + 柔和阴影（深色无阴影）
- 内部结构不变：左侧 + 附件圆形按钮（`InputCircleButton` 样式微调为品牌绿 hover）、右侧发送按钮、BasicTextField 占位文案不变

### 环境/模型切换器（新增功能，输入卡片正下方）

- pill 组件：左图标（模型/云图标）+ 文本 `当前组名 · 当前模型`（超长省略）+ ▾
- 数据源：ChatViewModel `_availableGroups` + 现有选中态（与头部胶囊同源）
- 点击：打开现有 ChatModelPickerSheet
- 无可用组时隐藏（不显示空胶囊）

## 4. 会话列表主页（SessionListScreen.kt）

SessionListScreen 是导航起点（对应 Accio 主页范式），OpenMinis 为本地 BYOK 应用、无账号体系，不加头像/配额卡等不存在的功能，只做样式层改造：
- 头部：保留现有 actions（设置/定时任务/终端菜单），图标按钮套用令牌样式（圆形浅底）
- 列表项卡片化：白卡 20dp 圆角 + 1dp cardBorder 描边，卡片间距 8dp，选中态 brandSoft 底
- 分组标题 13sp SemiBold textSecondary
- 背景铺同一渐变（Scaffold 容器透明 + 外层渐变 Box）
- 不新增功能入口

## 5. 弹窗统一（StandardChatSheet.kt）

- 统一改为：28dp 顶部圆角、拖拽条、居中标题（17sp SemiBold）、右上角关闭 X
- 改此一处，所有基于 StandardChatSheet 的弹窗（模型选择、技能、MCP 等）自动受益

## 6. 深色模式

- 全部令牌提供暗色变体（见颜色表）
- 渐变暗绿低饱和；卡片用 `#1C1C1E`；黑胶囊反转为浅色胶囊
- 跟随现有主题切换机制（theme_mode pref / 系统）

## 7. 验证

1. `./gradlew :app:assembleDebug` 编译通过
2. 模拟器（AVD OpenMinis）安装启动，adb 截图：主界面空态、对话态、抽屉、模型选择弹窗
3. 与 Accio 截图逐项对比（头部布局、渐变、卡片、胶囊、弹窗圆角）
4. 深浅色模式各验证一轮；聊天发送/模型切换/fallback 脉冲等现有行为回归确认

## 涉及文件

- 新增：`ui/theme/AccioTokens.kt`、`ui/components/AccioPill.kt`（模型切换器等胶囊复用）、`ui/components/AccioGradient.kt`（渐变背景容器）
- 修改：`ui/theme/Theme.kt`（M3 primary 换绿）、`ui/chat/ChatScreen.kt`（头部/空态/输入区/背景）、`ui/sessions/SessionListScreen.kt`（令牌套用）、`ui/chat/StandardChatSheet.kt`（弹窗统一）
- 不动：ChatColors.kt、ChatViewModel 及全部业务逻辑
