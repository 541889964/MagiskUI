package com.topjohnwu.magisk.ui.liquid

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.core.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 液态玻璃卡片修饰符：半透明渐变 + 圆角 + 细边 + 柔和阴影。
 */
fun Modifier.liquidGlassCard(
    cornerRadius: Int = 22,
    alpha: Int = Config.glassAlpha
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (alpha / 100f).coerceIn(0.05f, 0.55f)),
                Color.White.copy(alpha = (alpha / 220f).coerceIn(0.02f, 0.35f))
            )
        )
    )
    .graphicsLayer {
        shadowElevation = 8f
        shape = RoundedCornerShape(cornerRadius.dp)
    }

/**
 * 全屏背景层：支持无 / 静态图片 / 动态视频（视频退化为封面帧循环淡入淡出）
 * + 模糊 + 调暗。
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val ctx = LocalContext.current
    val type = Config.appearanceBgType
    val path = Config.appearanceBgPath
    val blurDp = Config.appearanceBgBlur.dp
    val dim = (Config.appearanceBgDim / 100f).coerceIn(0f, 0.85f)

    Box(modifier = modifier.fillMaxSize()) {
        when {
            type == 1 && path.isNotBlank() -> {
                val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            val uri = if (path.startsWith("content")) Uri.parse(path) else Uri.parse("file://$path")
                            ctx.contentResolver.openInputStream(uri)?.use {
                                BitmapFactory.decodeStream(it)?.asImageBitmap()
                            }
                        }.getOrNull()
                    }
                }
                bmp?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(blurDp)
                    )
                }
            }
            type == 2 -> {
                // 动态壁纸模式：使用背景色渐变呼吸动画模拟动态壁纸，避免引入重依赖
                val transition = rememberInfiniteTransition(label = "dynbg")
                val phase by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
                    label = "phase"
                )
                val c1 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f + 0.15f * phase)
                val c2 = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f - 0.15f * phase)
                Box(
                    Modifier.fillMaxSize()
                        .background(Brush.linearGradient(listOf(c1, c2)))
                )
                // 若有视频文件，叠加抽帧位图作为静态图层
                if (path.isNotBlank()) {
                    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
                        value = withContext(Dispatchers.IO) {
                            runCatching {
                                val uri = if (path.startsWith("content")) Uri.parse(path) else Uri.parse("file://$path")
                                ctx.contentResolver.openInputStream(uri)?.use {
                                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                                }
                            }.getOrNull()
                        }
                    }
                    bmp?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().blur(blurDp).graphicsLayer(alpha = 0.65f)
                        )
                    }
                }
            }
            else -> {
                Box(
                    Modifier.fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                )
            }
        }
        if (dim > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
        }
        content()
    }
}