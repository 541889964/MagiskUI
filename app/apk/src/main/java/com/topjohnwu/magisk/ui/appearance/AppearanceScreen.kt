package com.topjohnwu.magisk.ui.appearance

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.topjohnwu.magisk.core.Config
import com.topjohnwu.magisk.ui.liquid.liquidGlassCard

@Composable
fun AppearanceScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current

    var bgType by remember { mutableIntStateOf(Config.appearanceBgType) }
    var bgPath by remember { mutableStateOf(Config.appearanceBgPath) }
    var bgBlur by remember { mutableIntStateOf(Config.appearanceBgBlur) }
    var bgDim by remember { mutableIntStateOf(Config.appearanceBgDim) }
    var glass by remember { mutableIntStateOf(Config.glassAlpha) }
    var trollOn by remember { mutableStateOf(Config.trollEnabled) }
    var trollSpeed by remember { mutableIntStateOf(Config.trollSpeed) }
    var trollCount by remember { mutableIntStateOf(Config.trollCount) }
    var trollSize by remember { mutableIntStateOf(Config.trollSize) }

    fun persist() {
        Config.appearanceBgType = bgType
        Config.appearanceBgPath = bgPath
        Config.appearanceBgBlur = bgBlur
        Config.appearanceBgDim = bgDim
        Config.glassAlpha = glass
        Config.trollEnabled = trollOn
        Config.trollSpeed = trollSpeed
        Config.trollCount = trollCount
        Config.trollSize = trollSize
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            bgPath = uri.toString()
            bgType = 1
            persist()
        }
    }

    val pickVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            bgPath = uri.toString()
            bgType = 2
            persist()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionCard(title = "背景壁纸") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { bgType = 0; bgPath = ""; persist() }) {
                    Text("无")
                }
                Button(onClick = { pickImage.launch(arrayOf("image/*")) }) {
                    Text("静态图")
                }
                Button(onClick = { pickVideo.launch(arrayOf("video/*", "image/*")) }) {
                    Text("动态壁纸")
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = when (bgType) {
                    0 -> "当前：无自定义背景"
                    1 -> "当前：静态壁纸"
                    else -> "当前：动态壁纸"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }

        SectionCard(title = "背景模糊 / 调暗") {
            SliderRow("模糊度 ${bgBlur}dp", bgBlur.toFloat(), 0f, 25f) {
                bgBlur = it.toInt(); persist()
            }
            SliderRow("调暗 ${bgDim}%", bgDim.toFloat(), 0f, 80f) {
                bgDim = it.toInt(); persist()
            }
        }

        SectionCard(title = "液态玻璃") {
            SliderRow("玻璃透明度 ${glass}%", glass.toFloat(), 5f, 60f) {
                glass = it.toInt(); persist()
            }
        }

        SectionCard(title = "巨魔飘落特效") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("启用巨魔飘落", modifier = Modifier.weight(1f))
                Switch(checked = trollOn, onCheckedChange = { trollOn = it; persist() })
            }
            SliderRow("飘落速度 ${trollSpeed}%", trollSpeed.toFloat(), 20f, 300f) {
                trollSpeed = it.toInt(); persist()
            }
            SliderRow("屏幕数量 ${trollCount}", trollCount.toFloat(), 1f, 30f) {
                trollCount = it.toInt(); persist()
            }
            SliderRow("图标尺寸 ${trollSize}%", trollSize.toFloat(), 40f, 300f) {
                trollSize = it.toInt(); persist()
            }
            Text(
                "开启后图标从屏幕顶部缓慢飘下，并随陀螺仪倾斜方向左右漂移。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().liquidGlassCard(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}