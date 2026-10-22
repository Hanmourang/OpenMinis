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
