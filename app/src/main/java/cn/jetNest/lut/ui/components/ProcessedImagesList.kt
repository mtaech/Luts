package cn.jetNest.lut.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.jetNest.lut.data.ProcessedImage
import cn.jetNest.lut.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * 处理历史列表组件
 */
@Composable
fun ProcessedImagesList(
    processedImages: List<ProcessedImage>,
    onImageClick: (ProcessedImage) -> Unit,
    onClearHistory: () -> Unit,
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isGridView by remember { mutableStateOf(true) }
    
    Column(modifier = modifier) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "处理历史 (${processedImages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // 刷新状态指示器
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Row {
                // 视图切换按钮
                IconButton(
                    onClick = { isGridView = !isGridView }
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = if (isGridView) "列表视图" else "网格视图"
                    )
                }
                
                // 清空历史按钮
                if (processedImages.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "清空历史",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        if (processedImages.isEmpty()) {
            // 空状态
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无处理历史",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 图片列表
            if (isGridView) {
                // 网格视图 - 使用普通的Column和Row布局
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 将图片分组，每行3个
                    val chunkedImages = processedImages.chunked(3)
                    chunkedImages.forEach { rowImages ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowImages.forEach { processedImage ->
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ProcessedImageGridItem(
                                        processedImage = processedImage,
                                        onClick = { onImageClick(processedImage) }
                                    )
                                }
                            }
                            // 如果这一行不满3个，用空白填充
                            repeat(3 - rowImages.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // 列表视图 - 使用普通的Column布局
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    processedImages.forEach { processedImage ->
                        ProcessedImageListItem(
                            processedImage = processedImage,
                            onClick = { onImageClick(processedImage) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessedImageGridItem(
    processedImage: ProcessedImage,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(processedImage.processedPath) {
        ImageUtils.loadThumbnail(processedImage.processedPath, 200)
    }
    
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = processedImage.processedName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "无法加载",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 文件名覆盖层
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                    .align(Alignment.BottomCenter)
                    .padding(4.dp)
            ) {
                Text(
                    text = processedImage.processedName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ProcessedImageListItem(
    processedImage: ProcessedImage,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(processedImage.processedPath) {
        ImageUtils.loadThumbnail(processedImage.processedPath, 100)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                bitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = processedImage.processedName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = processedImage.processedName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "LUT: ${processedImage.lutFileName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                        .format(Date(processedImage.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}