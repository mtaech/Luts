package cn.jetNest.lut

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import cn.jetNest.lut.ui.components.ProcessingQueueItemCard
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import cn.jetNest.lut.ui.theme.LutTheme
import cn.jetNest.lut.ui.SettingsScreen
import cn.jetNest.lut.utils.FileUtils
import cn.jetNest.lut.viewmodel.MainViewModel
import cn.jetNest.lut.data.ProcessedImage
import cn.jetNest.lut.ui.components.ImageViewerDialog
import cn.jetNest.lut.ui.components.BottomNavigationBar
import cn.jetNest.lut.ui.components.BottomNavItem
import cn.jetNest.lut.ui.components.ProcessingQueueList
import cn.jetNest.lut.ui.components.ManualProcessScreen
import cn.jetNest.lut.ui.components.ProcessedImagesList
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边到边显示
        enableEdgeToEdge()
        
        // 配置系统栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            LutTheme {
                LutApp(viewModel)
            }
        }
    }
}

@Composable
fun ProcessedImagesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val processedImages by viewModel.processedImages.collectAsState()
    val isRefreshingImages by viewModel.isRefreshingImages.collectAsState()
    val outputDirectory by viewModel.outputDirectory.collectAsState()
    
    // 协程作用域
    val coroutineScope = rememberCoroutineScope()
    
    // 图片查看器状态
    var selectedImage by remember { mutableStateOf<ProcessedImage?>(null) }
    
    // 使用ProcessedImagesList组件，支持宫格和列表切换
    ProcessedImagesList(
        processedImages = processedImages,
        onImageClick = { selectedImage = it },
        onClearHistory = {
            viewModel.clearProcessedImages()
        },
        isRefreshing = isRefreshingImages,
        modifier = modifier.fillMaxSize()
    )
    
    // 图片查看器对话框
    selectedImage?.let { image ->
        ImageViewerDialog(
            processedImage = image,
            onDismiss = { selectedImage = null }
        )
    }
    
    // 当输出目录改变时刷新处理历史记录
    LaunchedEffect(outputDirectory) {
        if (outputDirectory != null) {
            viewModel.refreshProcessedImages()
        }
    }
}

@Composable
fun ProcessedImageCard(
    image: ProcessedImage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 文件名
            Text(
                text = image.processedPath.substringAfterLast("/"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 处理路径
            Text(
                text = "输出路径: ${image.processedPath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (image.originalPath.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "原始路径: ${image.originalPath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 处理参数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "LUT: ${image.lutFileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "强度: ${image.strength}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column {
                    Text(
                        text = "质量: ${image.quality}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "抖动: ${image.ditherType ?: "无"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 处理时间
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            Text(
                text = "处理时间: ${timeFormat.format(Date(image.timestamp))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LutApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf("main") }
    var currentBottomNavRoute by remember { mutableStateOf(BottomNavItem.HOME.route) }
    
    when (currentScreen) {
        "main" -> {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        currentRoute = currentBottomNavRoute,
                        onNavigate = { route -> currentBottomNavRoute = route }
                    )
                },
                contentWindowInsets = WindowInsets(0)
            ) { paddingValues ->
                when (currentBottomNavRoute) {
                    BottomNavItem.HOME.route -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { currentScreen = "settings" },
                        modifier = Modifier
                            .padding(paddingValues)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    )
                    BottomNavItem.MANUAL_PROCESS.route -> ManualProcessScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .padding(paddingValues)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    )
                    BottomNavItem.PROCESSED_IMAGES.route -> ProcessedImagesScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .padding(paddingValues)
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    )
                }
            }
        }
        "settings" -> SettingsScreen(
            viewModel = viewModel,
            onNavigateBack = { currentScreen = "main" }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 状态收集
    val watchDirectory by viewModel.watchDirectory.collectAsState()
    val outputDirectory by viewModel.outputDirectory.collectAsState()
    val selectedLutFile by viewModel.selectedLutFile.collectAsState()
    val lutFiles by viewModel.lutFiles.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val strength by viewModel.strength.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val ditherType by viewModel.ditherType.collectAsState()
    val processingQueue by viewModel.processingQueue.collectAsState()
    
    // 加载状态
    val isImportingLut by viewModel.isImportingLut.collectAsState()
    
    // 权限状态
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    val permissionState = rememberMultiplePermissionsState(permissions)
    
    // 文件选择器
    val lutFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // 获取原始文件名
            val originalFilename = FileUtils.getFileNameFromUri(context, it)
            val filename = if (originalFilename?.endsWith(".cube", ignoreCase = true) == true) {
                originalFilename
            } else {
                // 如果无法获取原始文件名或不是.cube文件，使用默认命名
                "imported_${System.currentTimeMillis()}.cube"
            }
            
            // 使用协程导入文件
            (context as ComponentActivity).lifecycleScope.launch {
                val success = viewModel.importLutFile(it, filename)
                if (success) {
                    viewModel.selectLutFile(filename)
                }
            }
        }
    }
    

    
    // 管理外部存储权限（Android 11+）
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                manageStorageLauncher.launch(intent)
            }
        } else {
            permissionState.launchMultiplePermissionRequest()
        }
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题栏
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LUT 图片处理器",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 导入LUT文件按钮
                    FilledTonalButton(
                        onClick = { 
                            if (!isImportingLut) {
                                lutFileLauncher.launch("*/*")
                            }
                        },
                        enabled = !isImportingLut
                    ) {
                        if (isImportingLut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导入中...")
                        } else {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "导入LUT文件",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("导入LUT")
                        }
                    }
                    
                    // 设置按钮
                    IconButton(
                        onClick = onNavigateToSettings
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            }
        }
        
        // 配置状态提示
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (watchDirectory != null && outputDirectory != null) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (watchDirectory != null && outputDirectory != null) {
                            "✓ 文件夹配置完成"
                        } else {
                            "⚠ 请在设置中配置文件夹"
                        },
                        color = if (watchDirectory != null && outputDirectory != null) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (watchDirectory == null || outputDirectory == null) {
                        TextButton(
                            onClick = onNavigateToSettings
                        ) {
                            Text("去设置")
                        }
                    }
                }
            }
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
        
        // 参数设置（可折叠）
        item {
            var parametersExpanded by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { parametersExpanded = !parametersExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "处理参数",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Icon(
                            imageVector = if (parametersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (parametersExpanded) "收起" else "展开"
                        )
                    }
                    
                    AnimatedVisibility(
                        visible = parametersExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
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
        
        // 控制按钮
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val canStart = watchDirectory != null && 
                            outputDirectory != null && 
                            selectedLutFile != null && 
                            !isMonitoring
                    

                    Button(
                        onClick = {
                            if (isMonitoring) {
                                viewModel.stopMonitoring()
                            } else {
                                viewModel.startMonitoring()
                            }
                        },
                        enabled = canStart || isMonitoring,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isMonitoring) "停止监控" else "开始监控")
                    }
                    
                    if (isMonitoring) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "正在监控文件夹...",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        // 文件处理队列
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
                            text = "文件处理队列",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (processingQueue.isNotEmpty()) {
                            Button(
                                onClick = { 
                                    viewModel.clearProcessingQueue()
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清空队列",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "清空",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (processingQueue.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "暂无处理任务",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // 显示最近的几个任务，避免嵌套滚动
                        val recentItems = processingQueue.take(5)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentItems.forEach { item ->
                                ProcessingQueueItemCard(item = item)
                            }
                            
                            if (processingQueue.size > 5) {
                                Text(
                                    text = "还有 ${processingQueue.size - 5} 个任务...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}