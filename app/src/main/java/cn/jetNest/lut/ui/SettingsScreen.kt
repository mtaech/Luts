package cn.jetNest.lut.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import cn.jetNest.lut.utils.FileUtils
import cn.jetNest.lut.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val watchDirectory by viewModel.watchDirectory.collectAsState()
    val outputDirectory by viewModel.outputDirectory.collectAsState()
    val isSelectingDirectory by viewModel.isSelectingDirectory.collectAsState()
    
    // 处理系统返回手势
    BackHandler {
        onNavigateBack()
    }
    
    // 监控文件夹选择器
    val watchDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val path = FileUtils.getDirectoryPathFromTreeUri(uri)
                path?.let { 
                    scope.launch {
                        viewModel.setWatchDirectory(it)
                    }
                }
            }
        }
    }
    
    // 输出文件夹选择器
    val outputDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val path = FileUtils.getDirectoryPathFromTreeUri(uri)
                path?.let { 
                    scope.launch {
                        viewModel.setOutputDirectory(it)
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 文件夹配置部分
            Text(
                text = "文件夹配置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            // 监控文件夹选择
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "监控文件夹",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "选择要监控新图片的文件夹",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = watchDirectory ?: "未选择",
                            modifier = Modifier.weight(1f),
                            color = if (watchDirectory == null) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        
                        FilledTonalButton(
                            onClick = {
                                if (!isSelectingDirectory) {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                    watchDirLauncher.launch(intent)
                                }
                            },
                            enabled = !isSelectingDirectory
                        ) {
                            if (isSelectingDirectory) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Folder, 
                                    contentDescription = "选择文件夹",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSelectingDirectory) "处理中..." else "选择")
                        }
                    }
                }
            }
            
            // 输出文件夹选择
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "输出文件夹",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "选择处理后图片的保存位置",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = outputDirectory ?: "未选择",
                            modifier = Modifier.weight(1f),
                            color = if (outputDirectory == null) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        
                        FilledTonalButton(
                            onClick = {
                                if (!isSelectingDirectory) {
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                    outputDirLauncher.launch(intent)
                                }
                            },
                            enabled = !isSelectingDirectory
                        ) {
                            if (isSelectingDirectory) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Folder, 
                                    contentDescription = "选择文件夹",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSelectingDirectory) "处理中..." else "选择")
                        }
                    }
                }
            }
            
            // 配置状态提示
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (watchDirectory != null && outputDirectory != null) {
                            "✓ 文件夹配置完成"
                        } else {
                            "⚠ 请完成文件夹配置"
                        },
                        color = if (watchDirectory != null && outputDirectory != null) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 应用信息部分
            Text(
                text = "应用信息",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            var showAboutDialog by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "关于",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("关于应用")
                    }
                }
            }
            
            if (showAboutDialog) {
                AboutDialog(
                    onDismiss = { showAboutDialog = false }
                )
            }
        }
    }
}