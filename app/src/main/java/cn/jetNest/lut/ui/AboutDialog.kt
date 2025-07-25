package cn.jetNest.lut.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Info, contentDescription = null)
        },
        title = {
            Text("关于 LUT 图片处理器")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "版本 1.0.0",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "一个强大的LUT图片处理应用，支持实时监控文件夹并自动应用LUT效果。"
                )
                Text(
                    text = "主要功能：",
                    fontWeight = FontWeight.Medium
                )
                Text("• 支持CUBE格式LUT文件")
                Text("• 实时文件夹监控")
                Text("• 三线性插值算法")
                Text("• Floyd-Steinberg和随机抖动")
                Text("• 可调节强度和质量")
                Text("• 保留EXIF数据")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}