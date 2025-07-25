package cn.jetNest.lut.ui.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.jetNest.lut.viewmodel.MainViewModel
import coil3.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GalleryImage(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val path: String
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ManualProcessScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // 从viewModel获取状态
    val selectedLutFile by viewModel.selectedLutFile.collectAsState()
    val lutFiles by viewModel.lutFiles.collectAsState()
    val strength by viewModel.strength.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val ditherType by viewModel.ditherType.collectAsState()
    val outputDirectory by viewModel.outputDirectory.collectAsState()
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var galleryImages by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var selectedImages by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var isLoadingImages by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isParametersExpanded by remember { mutableStateOf(false) }
    
    // 权限状态
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    val permissionState = rememberMultiplePermissionsState(permissions)
    
    // 加载图片
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            loadGalleryImages(context) { images ->
                galleryImages = images
            }
        }
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        item {
            Text(
                text = "手动处理图片",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // LUT文件选择
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LUT文件",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedLutFile ?: "请选择LUT文件",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (lutFiles.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("暂无LUT文件") },
                                    onClick = { }
                                )
                            } else {
                                lutFiles.forEach { filename ->
                                    DropdownMenuItem(
                                        text = { Text(filename) },
                                        onClick = {
                                            viewModel.selectLutFile(filename)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 参数设置
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 可点击的标题栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isParametersExpanded = !isParametersExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "处理参数",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = if (isParametersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isParametersExpanded) "收起参数" else "展开参数",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 参数内容区域（可折叠）
                    AnimatedVisibility(
                        visible = isParametersExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 强度设置
                            Column {
                                Text("强度: $strength%")
                                Slider(
                                    value = strength.toFloat(),
                                    onValueChange = { viewModel.setStrength(it.toInt()) },
                                    valueRange = 0f..100f,
                                    steps = 99
                                )
                            }
                            
                            // 质量设置
                            Column {
                                Text("质量: $quality%")
                                Slider(
                                    value = quality.toFloat(),
                                    onValueChange = { viewModel.setQuality(it.toInt()) },
                                    valueRange = 1f..100f,
                                    steps = 98
                                )
                            }
                            
                            // 抖动类型
                            Column {
                                Text("抖动类型")
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = ditherType == null,
                                        onClick = { viewModel.setDitherType(null) },
                                        label = { Text("无") }
                                    )
                                    FilterChip(
                                        selected = ditherType == "floyd",
                                        onClick = { viewModel.setDitherType("floyd") },
                                        label = { Text("Floyd-Steinberg") }
                                    )
                                    FilterChip(
                                        selected = ditherType == "random",
                                        onClick = { viewModel.setDitherType("random") },
                                        label = { Text("随机") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 开始处理按钮
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val canProcess = selectedLutFile != null && 
                            outputDirectory != null && 
                            selectedImages.isNotEmpty() && 
                            !isProcessing
                    
                    Button(
                        onClick = {
                            if (canProcess) {
                                val imagesToProcess = galleryImages.filter { it.id in selectedImages }
                                isProcessing = true
                                // 调用viewModel的处理方法
                                coroutineScope.launch {
                                    val success = viewModel.processSelectedImages(
                                        selectedImages = imagesToProcess.map { it.path },
                                        lutFileName = selectedLutFile!!,
                                        strength = strength,
                                        quality = quality,
                                        ditherType = ditherType
                                    )
                                    // 处理完成后重置选择
                                    selectedImages = emptySet()
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = canProcess,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("处理中...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("开始处理 (${selectedImages.size})")
                        }
                    }
                    
                    if (selectedImages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "已选择 ${selectedImages.size} 张图片",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                    
                    if (outputDirectory == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请先在设置中配置输出目录",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        // 权限请求
        if (!permissionState.allPermissionsGranted) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "需要存储权限来访问相册",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissionState.launchMultiplePermissionRequest() }
                        ) {
                            Text("授予权限")
                        }
                    }
                }
            }
        } else {
            // 图片网格标题
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
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
                                text = "相册图片 (${galleryImages.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Row {
                                if (selectedImages.isNotEmpty()) {
                                    TextButton(
                                        onClick = { selectedImages = emptySet() }
                                    ) {
                                        Text("清空选择")
                                    }
                                }
                                
                                TextButton(
                                    onClick = {
                                        selectedImages = if (selectedImages.size == galleryImages.size) {
                                            emptySet()
                                        } else {
                                            galleryImages.map { it.id }.toSet()
                                        }
                                    }
                                ) {
                                    Text(if (selectedImages.size == galleryImages.size) "取消全选" else "全选")
                                }
                            }
                        }
                    }
                }
            }
            
            // 图片网格
            if (galleryImages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "暂无图片",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(galleryImages.chunked(3)) { imageRow ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            imageRow.forEach { image ->
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    GalleryImageItem(
                                        image = image,
                                        isSelected = image.id in selectedImages,
                                        onSelectionChange = { isSelected ->
                                            selectedImages = if (isSelected) {
                                                selectedImages + image.id
                                            } else {
                                                selectedImages - image.id
                                            }
                                        }
                                    )
                                }
                            }
                            // 填充空白位置
                            repeat(3 - imageRow.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryImageItem(
    image: GalleryImage,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelectionChange(!isSelected) }
    ) {
        Image(
            painter = rememberAsyncImagePainter(image.uri),
            contentDescription = image.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 选择指示器
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.3f)
                    )
            )
            
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "已选择",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
            )
        }
    }
}

private suspend fun loadGalleryImages(
    context: Context,
    onImagesLoaded: (List<GalleryImage>) -> Unit
) = withContext(Dispatchers.IO) {
    val images = mutableListOf<GalleryImage>()
    
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATA
    )
    
    val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
    
    val cursor: Cursor? = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )
    
    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        
        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val name = it.getString(nameColumn)
            val path = it.getString(dataColumn)
            
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            
            images.add(
                GalleryImage(
                    id = id,
                    uri = contentUri,
                    displayName = name,
                    path = path
                )
            )
        }
    }
    
    withContext(Dispatchers.Main) {
        onImagesLoaded(images)
    }
}