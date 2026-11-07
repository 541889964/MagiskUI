package com.topjohnwu.magisk.ui.liquid

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.core.Config
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class TrollParticle(
    var x: Float,
    var y: Float,
    var vy: Float,
    var rot: Float,
    var rotSpeed: Float,
    var scale: Float,
    var alpha: Float
)

/**
 * 巨魔图标从顶部缓慢飘落，随陀螺仪倾斜方向横向漂移。
 * 速度/数量/尺寸来自 Config（appearance 设置）。
 */
@Composable
fun TrollRain(modifier: Modifier = Modifier) {
    if (!Config.trollEnabled) return

    val ctx = LocalContext.current
    val density = LocalDensity.current
    val count = Config.trollCount.coerceIn(1, 40)
    val speedMult = (Config.trollSpeed / 100f).coerceIn(0.2f, 3f)
    val sizeMult = (Config.trollSize / 100f).coerceIn(0.4f, 3f)

    // 陀螺仪：tiltX 表示左右倾斜，tiltY 表示前后（用于速度增益）
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(ctx) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val gyro = sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        tiltX = tiltX * 0.9f + e.values[1] * 0.1f   // 绕 y 轴 -> 左右
                        tiltY = tiltY * 0.9f + e.values[0] * 0.1f
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        tiltX = tiltX * 0.9f - e.values[0] * 0.1f
                        tiltY = tiltY * 0.9f + e.values[1] * 0.1f
                    }
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        gyro?.let { sm?.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sm?.unregisterListener(listener) }
    }

    val particles = remember(count) {
        mutableStateListOf<TrollParticle>().apply {
            repeat(count) { i ->
                add(
                    TrollParticle(
                        x = Random.nextFloat(),
                        y = -Random.nextFloat() * 1.4f - i * 0.05f,
                        vy = 0.02f + Random.nextFloat() * 0.035f,
                        rot = Random.nextFloat() * 360f,
                        rotSpeed = (Random.nextFloat() - 0.5f) * 45f,
                        scale = 0.7f + Random.nextFloat() * 0.8f,
                        alpha = 0.55f + Random.nextFloat() * 0.45f
                    )
                )
            }
        }
    }

    val curTiltX by rememberUpdatedState(tiltX)
    val curTiltY by rememberUpdatedState(tiltY)

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = now
                val drift = curTiltX * 0.35f + curTiltY * 0.05f
                for (p in particles) {
                    p.y += p.vy * speedMult * (dt * 60f) * (1f + curTiltY * 0.4f)
                    p.x += drift * dt
                    p.rot += p.rotSpeed * dt
                    if (p.x < -0.12f) p.x = 1.12f
                    if (p.x > 1.12f) p.x = -0.12f
                    if (p.y > 1.15f) {
                        p.y = -0.15f
                        p.x = Random.nextFloat()
                        p.vy = 0.02f + Random.nextFloat() * 0.035f
                        p.scale = 0.7f + Random.nextFloat() * 0.8f
                        p.alpha = 0.55f + Random.nextFloat() * 0.45f
                    }
                }
            }
        }
    }

    val baseSizePx = with(density) { 34.dp.toPx() } * sizeMult
    val gold = Color(0xFFFFC83D)

    Canvas(modifier = modifier.fillMaxSize()) {
        for (p in particles) {
            val cx = p.x * size.x
            val cy = p.y * size.y
            val s = baseSizePx * p.scale
            rotate(p.rot, pivot = Offset(cx, cy)) {
                drawTroll(cx, cy, s, p.alpha, gold)
            }
        }
    }
}

/** 简易巨魔（丑萌小脸）矢量绘制，无需外部资源。 */
private fun DrawScope.drawTroll(cx: Float, cy: Float, s: Float, alpha: Float, color: Color) {
    val r = s / 2f
    // 头
    drawCircle(color.copy(alpha = alpha), radius = r, center = Offset(cx, cy))
    // 头发（顶部金色刺）
    val hair = Color(0xFFFFE066).copy(alpha = alpha)
    for (i in -1..1) {
        val ang = (-90f + i * 40f) * (Math.PI.toFloat() / 180f)
        val hx = cx + cos(ang) * r * 0.95f
        val hy = cy + sin(ang) * r * 0.95f
        drawCircle(hair, radius = r * 0.28f, center = Offset(hx, hy))
    }
    // 眼睛
    val eye = Color.White.copy(alpha = alpha)
    drawCircle(eye, radius = r * 0.22f, center = Offset(cx - r * 0.34f, cy - r * 0.05f))
    drawCircle(eye, radius = r * 0.22f, center = Offset(cx + r * 0.34f, cy - r * 0.05f))
    val pupil = Color(0xFF111111).copy(alpha = alpha)
    drawCircle(pupil, radius = r * 0.1f, center = Offset(cx - r * 0.34f, cy - r * 0.02f))
    drawCircle(pupil, radius = r * 0.1f, center = Offset(cx + r * 0.34f, cy - r * 0.02f))
    // 坏笑嘴
    drawArc(
        color = pupil,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.45f, cy + r * 0.15f),
        size = androidx.compose.ui.geometry.Size(r * 0.9f, r * 0.55f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.12f)
    )
}