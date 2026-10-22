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
    gradientTop = Color(0xFFB8EBD3),
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
