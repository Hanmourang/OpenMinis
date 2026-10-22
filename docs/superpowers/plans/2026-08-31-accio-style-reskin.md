# OpenMinis Android Accio 风格核心界面优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 spec `docs/superpowers/specs/2026-08-31-accio-style-reskin-design.md`，为 OpenMinis Android 核心界面层（会话列表主页 / 聊天页 / 输入栏 / 弹窗）落地 Accio 风格视觉，并新增环境/模型切换器 pill。

**Architecture:** 新增独立令牌层（AccioTokens + AccioGradient + AccioPill），通过 CompositionLocal 注入（挂在现有 MinisTheme 内，跟随应用内主题切换）；只改呈现层，不动业务逻辑。Git 提交需用户明确批准，本计划不含自动 commit 步骤。

**Tech Stack:** Kotlin + Jetpack Compose + Material3；构建 `cd src/android && ./gradlew :app:assembleDebug`；验证用 AVD `OpenMinis` + adb 截图。

**项目根：** `/Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis`
**源码根（下文相对路径基于此）：** `src/android/app/src/main/java/com/openminis/app`

---

### Task 1: AccioTokens 设计令牌

**Files:**
- Create: `ui/theme/AccioTokens.kt`

- [ ] **Step 1: 创建令牌文件**

```kotlin
package com.openminis.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Accio Work 风格设计令牌（参考其手机端截图）。
 * 与 ChatPalette 独立：ChatPalette 管聊天消息区语义色，本令牌层管
 * 品牌绿 / 渐变背景 / 卡片 / 胶囊组件。深浅色变体由 MinisTheme 注入，
 * 跟随应用内主题切换（与 LocalChatPalette 同一 darkTheme 源）。
 */
@Immutable
data class AccioColors(
    val isDark: Boolean,
    val brand: Color,
    val brandSoft: Color,
    val gradientTop: Color,
    val gradientEnd: Color,
    val card: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val statusOnline: Color,
    val statusOffline: Color,
)

val LightAccioColors = AccioColors(
    isDark = false,
    brand = Color(0xFF00C16E),
    brandSoft = Color(0xFFDFF7EC),
    gradientTop = Color(0xFFD9F5E8),
    gradientEnd = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0x14000000),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF6B6B6B),
    statusOnline = Color(0xFF00C16E),
    statusOffline = Color(0xFFFF3B30),
)

val DarkAccioColors = AccioColors(
    isDark = true,
    brand = Color(0xFF3DD68C),
    brandSoft = Color(0xFF123324),
    gradientTop = Color(0xFF0A1F17),
    gradientEnd = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    cardBorder = Color(0x33FFFFFF),
    textPrimary = Color(0xFFF2F2F2),
    textSecondary = Color(0xFF9E9E9E),
    statusOnline = Color(0xFF3DD68C),
    statusOffline = Color(0xFFFF453A),
)

val LocalAccioColors = staticCompositionLocalOf { LightAccioColors }

/** 短访问器：AccioTheme.brand */
val AccioTheme: AccioColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAccioColors.current
```

- [ ] **Step 2: 编译验证**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

---

### Task 2: AccioGradient 渐变背景容器

**Files:**
- Create: `ui/components/AccioGradient.kt`

- [ ] **Step 1: 创建渐变容器组件**

```kotlin
package com.openminis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.openminis.app.ui.theme.AccioTheme

/**
 * Accio 风格页面渐变背景：顶部薄荷绿 → 40% 高度处过渡到素色底。
 * 包裹在 Scaffold 外层，Scaffold 自身 containerColor 需设为透明。
 */
@Composable
fun AccioGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AccioTheme
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0.0f to colors.gradientTop,
                0.4f to colors.gradientEnd,
                1.0f to colors.gradientEnd,
            ),
        ),
        content = content,
    )
}
```

- [ ] **Step 2: 编译验证**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

---

### Task 3: AccioPill 胶囊组件

**Files:**
- Create: `ui/components/AccioPill.kt`

- [ ] **Step 1: 创建胶囊组件**

```kotlin
package com.openminis.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.ui.theme.AccioTheme

/**
 * Accio 风格胶囊（pill）：brandSoft 底 + brand 字，全圆角。
 * 用于头部模型胶囊、输入栏下方环境/模型切换器等。
 */
@Composable
fun AccioPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    showChevron: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(AccioTheme.brandSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = AccioTheme.brand,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = AccioTheme.brand,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showChevron) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AccioTheme.brand,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

---

### Task 4: 全局主色换薄荷绿 + MinisTheme 注入令牌

**Files:**
- Modify: `ui/theme/Theme.kt`

- [ ] **Step 1: M3 主色换绿**（保持 `Teal` 命名不动 90+ 调用点，值改为 Accio 绿）

`Theme.kt` 第 34 行：
```kotlin
private val TealPrimary = Color(0xFF528AD2)
```
改为：
```kotlin
// Accent: Accio mint green (#00C16E). Supersedes the desaturated iOS blue
// (see the historical note below); Teal prefix kept to avoid churning 90+
// call sites — the value is the contract, not the name.
private val TealPrimary = Color(0xFF00C16E)
```

第 36-37 行容器色同步为薄荷色系：
```kotlin
private val TealPrimaryContainer = Color(0xFFB2DFDB)
private val TealOnPrimaryContainer = Color(0xFF00332F)
```
改为：
```kotlin
private val TealPrimaryContainer = Color(0xFFDFF7EC)
private val TealOnPrimaryContainer = Color(0xFF063B25)
```

第 54 行深色主色：
```kotlin
private val TealDarkPrimary = Color(0xFF6A94CE)
```
改为：
```kotlin
private val TealDarkPrimary = Color(0xFF3DD68C)
```

第 56-57 行：
```kotlin
private val TealDarkPrimaryContainer = Color(0xFF1A6B6B)
private val TealDarkOnPrimaryContainer = Color(0xFFB2DFDB)
```
改为：
```kotlin
private val TealDarkPrimaryContainer = Color(0xFF123324)
private val TealDarkOnPrimaryContainer = Color(0xFF3DD68C)
```

- [ ] **Step 2: MinisTheme 注入 LocalAccioColors**

`Theme.kt` 中 `MinisTheme` 函数体（第 153-170 行）：
```kotlin
        CompositionLocalProvider(LocalChatPalette provides chatPalette, content = content)
```
改为：
```kotlin
        CompositionLocalProvider(
            LocalChatPalette provides chatPalette,
            LocalAccioColors provides if (darkTheme) DarkAccioColors else LightAccioColors,
            content = content,
        )
```

- [ ] **Step 3: 编译验证**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

---

### Task 5: ChatScreen 渐变背景接入

**Files:**
- Modify: `ui/chat/ChatScreen.kt`（Scaffold 在 ~2306 行，TopAppBar colors 在 ~2796 行）

- [ ] **Step 1: Scaffold 外层包渐变、容器透明**

找到（~2306 行）：
```kotlin
    Scaffold(
        containerColor = ChatColors.background,
        contentWindowInsets = WindowInsets(0),
```
改为：
```kotlin
    com.openminis.app.ui.components.AccioGradientBackground(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0),
```
并在该 Scaffold 对应的闭合大括号后补一个 `}`（关闭 AccioGradientBackground 的 lambda）。

- [ ] **Step 2: TopAppBar 容器透明**

找到 ~2796 行 `TopAppBarDefaults.topAppBarColors(`，将其 `containerColor` 参数值改为 `androidx.compose.ui.graphics.Color.Transparent`（保持其余参数不动），让渐变透到头部。

- [ ] **Step 3: 编译验证**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

---

### Task 6: ChatScreen 头部 Accio 对话页化

**Files:**
- Modify: `ui/chat/ChatScreen.kt`（TopAppBar ~2310 行起；navigationIcon ~2530 行；actions ~2536 行）

- [ ] **Step 1: 返回按钮包圆形浅底**

找到：
```kotlin
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
```
改为：
```kotlin
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .background(com.openminis.app.ui.theme.AccioTheme.brandSoft, CircleShape),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = com.openminis.app.ui.theme.AccioTheme.brand,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
```

- [ ] **Step 2: 副标题（组名 + provider·model 区块）胶囊化**

找到副标题外层 Column（~2387 行，`clickable { showModelPicker = true }` 那个）：
```kotlin
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showModelPicker = true }
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            ) {
```
改为（圆角加大 + brandSoft 底 + 品牌绿状态点）：
```kotlin
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(com.openminis.app.ui.theme.AccioTheme.brandSoft)
                                    .clickable { showModelPicker = true }
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            ) {
```
同区块内状态点颜色（~2403 行）`Color(0xFF34C759)` 改为 `com.openminis.app.ui.theme.AccioTheme.statusOnline`，离线色 `Color(0xFFFF9500)` 改为 `com.openminis.app.ui.theme.AccioTheme.statusOffline`；组名文字颜色 `ChatColors.secondaryText`（~2434 行）改为 `com.openminis.app.ui.theme.AccioTheme.brand`。

- [ ] **Step 3: 右侧 MoreVert 包圆形浅底**

找到：
```kotlin
                    Box {
                        IconButton(onClick = { showChatMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
```
改为：
```kotlin
                    Box {
                        IconButton(onClick = { showChatMenu = true }) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(com.openminis.app.ui.theme.AccioTheme.brandSoft, CircleShape),
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = com.openminis.app.ui.theme.AccioTheme.brand,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
```

- [ ] **Step 4: 提取共享的组名胶囊文案**

在 ~461 行 `val selectedGroupName by viewModel.selectedGroupName.collectAsState()` 附近追加（供本任务副标题、Task 7 空态胶囊、Task 8 切换器三处复用）：

```kotlin
    // Accio 胶囊共享文案：与 TopAppBar 副标题同一兜底链
    // （_selectedGroupName → 默认组名 → badge 字符串）。
    val groupNameForPill = selectedGroupName.ifEmpty {
        val defaultGroupId = providerRepository.defaultPrimaryGroupId
        availableGroups.firstOrNull { it.id == defaultGroupId }?.name
            ?: stringResource(R.string.model_picker_default_badge)
    }
```

（`availableGroups` 已在 ChatScreen 中收集，若名称不同以现场为准。）

- [ ] **Step 5: 编译验证**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

---

### Task 7: ChatScreen 空态居中 Soul 区

**Files:**
- Modify: `ui/chat/ChatScreen.kt`（消息列表容器在 ~2842 行 `Box(modifier = Modifier.weight(1f))`）

- [ ] **Step 1: 定位消息为空的分支点**

在 ~2842 行 `Box(modifier = Modifier.weight(1f)) {` 内部，消息 LazyColumn 之前插入空态分支。判断条件用会话消息为空（执行时用编辑器搜索 `flatItems` 的 LazyColumn 起点，确认以 `uiMessages`/flatItems 为空为准）。插入代码：

```kotlin
                val isConversationEmpty = viewModel.messages.value.isEmpty()
                if (isConversationEmpty) {
                    // Accio 风格空态：居中 Soul 头像 + 名称 + 副标题 + 模型切换胶囊
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val emptySoul by com.openminis.app.agent.SoulStore
                            .cachedMetadata.collectAsState()
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .background(com.openminis.app.ui.theme.AccioTheme.brandSoft, CircleShape),
                        ) {
                            // SoulMetadata 无头像字段，UI 身份统一为锁定 ✨
                            // （SoulStore.DISPLAY_EMOJI，与设置预览/聊天气泡一致）。
                            Text(text = emptySoul.displayEmoji, fontSize = 34.sp)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = emptySoul.name.ifBlank { stringResource(R.string.app_name) },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = com.openminis.app.ui.theme.AccioTheme.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // SoulMetadata 无描述/tagline 字段，副标题用固定兜底文案。
                        Text(
                            text = "你的本地 AI 助手",
                            fontSize = 13.sp,
                            color = com.openminis.app.ui.theme.AccioTheme.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (availableGroups.isNotEmpty()) {
                            com.openminis.app.ui.components.AccioPill(
                                text = groupNameForPill,
                                onClick = { showModelPicker = true },
                                leadingIcon = Icons.Default.SmartToy,
                            )
                        }
                    }
                }
```

注意：`groupNameForPill` 已在 Task 6 Step 4 定义；`availableGroups` 已在 ChatScreen 收集。若 LazyColumn 需要 else 分支包裹，执行时保持消息列表逻辑原样、仅加 if/else。

- [ ] **Step 2: 编译验证**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

---

### Task 8: 输入卡 24dp 圆角 + 环境/模型切换器

**Files:**
- Modify: `ui/chat/ChatScreen.kt`（输入卡 Column ~4714 行；切换器插在输入卡父容器闭合之后）

- [ ] **Step 1: 输入卡圆角 20→24**

找到（~4718 行）：
```kotlin
                            val radiusPx = 20.dp.toPx()
```
改为：
```kotlin
                            val radiusPx = 24.dp.toPx()
```

- [ ] **Step 2: 输入卡描边**

在输入卡 Column 的 modifier 链中（`.padding(top = ...)` 之前）追加描边：
```kotlin
                        .border(
                            1.dp,
                            com.openminis.app.ui.theme.AccioTheme.cardBorder,
                            RoundedCornerShape(24.dp),
                        )
```
（`androidx.compose.foundation.border` 若未导入则补导入。）

- [ ] **Step 3: 输入卡下方新增环境/模型切换器 pill**

找到 swipe 手势 Box（~4633 行 `Box(modifier = Modifier.fillMaxWidth().pointerInput(Unit) {`)的**闭合处**（输入卡整个结束之后、底部容器 Column 之内），在其后追加：

```kotlin
                // Accio 风格环境/模型切换器：输入卡正下方居中 pill，
                // 点击打开现有 ChatModelPickerSheet。无可用组时隐藏。
                if (availableGroups.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                    ) {
                        com.openminis.app.ui.components.AccioPill(
                            text = if (modelName.isNotEmpty()) {
                                "$groupNameForPill · $modelName"
                            } else {
                                groupNameForPill
                            },
                            onClick = { showModelPicker = true },
                            leadingIcon = Icons.Default.SmartToy,
                        )
                    }
                }
```

`groupNameForPill` 与 Task 7 共用同一兜底链（`selectedGroupName` → 默认组名 → badge 字符串），在 ChatScreen 顶部合适位置提取为局部 val 供三处复用。

- [ ] **Step 4: 编译验证**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

---

### Task 9: SessionListScreen 主页改造

**Files:**
- Modify: `ui/sessions/SessionListScreen.kt`（Scaffold ~610 行；SessionItemContent ~1646 行）

- [ ] **Step 1: 渐变背景接入**

Scaffold（~610 行）外层包 `AccioGradientBackground`，`Scaffold` 增加 `containerColor = androidx.compose.ui.graphics.Color.Transparent`；TopAppBar（~612 行）加 `colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)`。

- [ ] **Step 2: 头部图标按钮圆形浅底**

TopAppBar 的 navigationIcon 设置按钮与 actions 中的定时任务/终端 IconButton（~630-680 行），图标外层包：
```kotlin
Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
        .size(34.dp)
        .background(com.openminis.app.ui.theme.AccioTheme.brandSoft, CircleShape),
) { /* 原 Icon，tint 改为 AccioTheme.brand */ }
```

- [ ] **Step 3: 会话列表项卡片化**

找到 `SessionItemContent`（~1646 行）的最外层容器（Row/Surface），将其背景/形状改为：
```kotlin
Surface(
    shape = RoundedCornerShape(20.dp),
    color = com.openminis.app.ui.theme.AccioTheme.card,
    border = androidx.compose.foundation.BorderStroke(
        1.dp, com.openminis.app.ui.theme.AccioTheme.cardBorder,
    ),
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 4.dp),
) { /* 原内容 */ }
```
选中态（如有 selection 高亮）底色改 `AccioTheme.brandSoft`。分组标题文字统一 `fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccioTheme.textSecondary`。LazyColumn 背景保持透明让渐变透出（~1501 行注释处即为透明处理，确认不误改）。

- [ ] **Step 4: 编译验证**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

---

### Task 10: StandardChatSheet 弹窗统一

**Files:**
- Modify: `ui/chat/StandardChatSheet.kt`

- [ ] **Step 1: 容器色换 Accio 卡片色**

`ModalBottomSheet(` 的 `containerColor = ChatColors.background` 改为 `containerColor = com.openminis.app.ui.theme.AccioTheme.card`（M3 ModalBottomSheet 默认即为 28dp 顶部圆角，无需改形状；dragHandle 已是 CompactDragHandle）。

- [ ] **Step 2: 编译验证**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:compileDebugKotlin 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

---

### Task 11: 全量构建 + 模拟器截图验收

- [ ] **Step 1: 全量打包**

Run: `cd /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android && ./gradlew :app:assembleDebug 2>&1 | tail -3`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: 安装到模拟器并启动**

Run:
```sh
export PATH=$PATH:$HOME/Library/Android/sdk/platform-tools
adb install -r /Users/eleme/Documents/workspace/ai_workspace_online/OpenMinis/src/android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.openminis.app/.MainActivityIconAuto
sleep 8
adb exec-out screencap -p > /tmp/accio-home.png
```

- [ ] **Step 3: 逐场景截图验收**

依次截图并与 Accio 参考截图对比：
1. 会话列表主页（渐变 + 卡片列表）→ `/tmp/accio-list.png`
2. 聊天页空态（居中 Soul 区 + 切换胶囊）→ `/tmp/accio-empty.png`
3. 发送一条消息后的对话态 → `/tmp/accio-chat.png`
4. 模型选择弹窗（28dp 圆角 + 拖拽条）→ `/tmp/accio-sheet.png`
5. 设置里切深色模式后重截 1/2 两张 → `/tmp/accio-dark-*.png`

- [ ] **Step 4: 行为回归确认**

- 发消息、收流式回复正常
- 头部副标题/空态胶囊/输入栏下切换器三处点击均能打开模型选择弹窗
- 切换模型组后头部与切换器文案同步更新
- MoreVert 菜单、返回键、会话重命名点击行为不变
