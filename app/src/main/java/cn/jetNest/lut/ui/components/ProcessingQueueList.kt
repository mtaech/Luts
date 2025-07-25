package cn.jetNest.lut.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.jetNest.lut.data.ProcessingQueueItem
import cn.jetNest.lut.data.ProcessingStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProcessingQueueList(
    queueItems: List<ProcessingQueueItem>,
    modifier: Modifier = Modifier
) {
    if (queueItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无处理任务",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "开始监控后，新增的图片文件将在此显示",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(queueItems) { item ->
                ProcessingQueueItemCard(item = item)
            }
        }
    }
}

@Composable
fun ProcessingQueueItemCard(
    item: ProcessingQueueItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                ProcessingStatusIndicator(status = item.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.filePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            Text(
                text = "添加时间: ${timeFormat.format(Date(item.addedTime))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 显示进度条（如果有进度信息）
            if (item.status == ProcessingStatus.PROCESSING && item.progress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = item.progress,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${(item.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 显示错误信息（如果有）
            if (item.status == ProcessingStatus.FAILED && !item.errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "错误: ${item.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ProcessingStatusIndicator(
    status: ProcessingStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val (icon, color, text) = when (status) {
            ProcessingStatus.WAITING -> Triple(
                Icons.Default.HourglassEmpty,
                MaterialTheme.colorScheme.onSurfaceVariant,
                "等待中"
            )
            ProcessingStatus.PROCESSING -> Triple(
                Icons.Default.Refresh,
                MaterialTheme.colorScheme.primary,
                "处理中"
            )
            ProcessingStatus.COMPLETED -> Triple(
                Icons.Default.CheckCircle,
                Color(0xFF4CAF50),
                "已完成"
            )
            ProcessingStatus.FAILED -> Triple(
                Icons.Default.Error,
                MaterialTheme.colorScheme.error,
                "失败"
            )
        }
        
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}