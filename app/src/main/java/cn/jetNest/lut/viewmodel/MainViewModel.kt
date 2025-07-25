package cn.jetNest.lut.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import cn.jetNest.lut.data.ProcessedImage
import cn.jetNest.lut.data.ProcessingQueueItem
import cn.jetNest.lut.data.ProcessingStatus
import cn.jetNest.lut.data.SettingsManager
import cn.jetNest.lut.service.FileMonitorService
import cn.jetNest.lut.events.EventBus
import cn.jetNest.lut.events.ProcessingEvents
import cn.jetNest.lut.utils.FileUtils
import java.io.File
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context: Context = application.applicationContext
    
    // 设置管理器
    private val settingsManager = SettingsManager(context)
    
    // UI状态 - 从设置管理器获取
    val watchDirectory: StateFlow<String?> = settingsManager.watchDirectory
    val outputDirectory: StateFlow<String?> = settingsManager.outputDirectory
    val selectedLutFile: StateFlow<String?> = settingsManager.selectedLutFile
    val strength: StateFlow<Int> = settingsManager.strength
    val quality: StateFlow<Int> = settingsManager.quality
    val ditherType: StateFlow<String?> = settingsManager.ditherType
    
    private val _lutFiles = MutableStateFlow<List<String>>(emptyList())
    val lutFiles: StateFlow<List<String>> = _lutFiles.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    // 处理历史记录
    private val _processedImages = MutableStateFlow<List<ProcessedImage>>(emptyList())
    val processedImages: StateFlow<List<ProcessedImage>> = _processedImages.asStateFlow()
    
    // 加载状态管理
    private val _isImportingLut = MutableStateFlow(false)
    val isImportingLut: StateFlow<Boolean> = _isImportingLut.asStateFlow()
    
    private val _isRefreshingImages = MutableStateFlow(false)
    val isRefreshingImages: StateFlow<Boolean> = _isRefreshingImages.asStateFlow()
    
    private val _isSelectingDirectory = MutableStateFlow(false)
    val isSelectingDirectory: StateFlow<Boolean> = _isSelectingDirectory.asStateFlow()
    
    // 处理队列状态
    private val _processingQueue = MutableStateFlow<List<ProcessingQueueItem>>(emptyList())
    val processingQueue: StateFlow<List<ProcessingQueueItem>> = _processingQueue.asStateFlow()
    

    
    // LUT文件存储目录
    private val lutDirectory = File(context.filesDir, "luts")
    
    init {
        // 确保LUT目录存在
        if (!lutDirectory.exists()) {
            lutDirectory.mkdirs()
        }
        refreshLutFiles()
        
        // 监听图片处理完成事件
        viewModelScope.launch {
            EventBus.imageProcessedEvents.collect { event ->
                val processedImage = ProcessedImage(
                    originalPath = event.originalPath,
                    processedPath = event.processedPath,
                    timestamp = System.currentTimeMillis(),
                    lutFileName = event.lutFileName,
                    strength = event.strength,
                    quality = event.quality,
                    ditherType = event.ditherType
                )
                addProcessedImage(processedImage)
            }
        }
        
        // 监听文件添加到队列事件
        viewModelScope.launch {
            EventBus.fileAddedToQueueEvents.collect { event ->
                addToProcessingQueue(event.queueItem)
            }
        }
        
        // 监听处理队列更新事件
        viewModelScope.launch {
            EventBus.processingQueueUpdateEvents.collect { event ->
                updateProcessingQueueItem(event.itemId, event.status, event.progress, event.errorMessage)
            }
        }
        
        // 监听新的处理事件
        viewModelScope.launch {
            EventBus.processingEvents.collect { event ->
                when (event) {
                    is ProcessingEvents.FileAddedToQueue -> {
                        addToProcessingQueue(event.queueItem)
                    }
                    is ProcessingEvents.QueueUpdate -> {
                        updateProcessingQueueItem(event.itemId, event.status, event.progress, event.errorMessage)
                    }
                }
            }
        }
    }
    
    /**
     * 导入LUT文件
     */
    suspend fun importLutFile(uri: Uri, filename: String): Boolean = withContext(Dispatchers.IO) {
        _isImportingLut.value = true
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputFile = File(lutDirectory, filename)
            
            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            withContext(Dispatchers.Main) {
                refreshLutFiles()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            _isImportingLut.value = false
        }
    }
    
    /**
     * 刷新LUT文件列表
     */
    private fun refreshLutFiles() {
        val files = lutDirectory.listFiles()
            ?.filter { it.extension.lowercase() == "cube" }
            ?.map { it.name }
            ?: emptyList()
        _lutFiles.value = files
    }
    
    /**
     * 设置监控目录
     */
    suspend fun setWatchDirectory(path: String) = withContext(Dispatchers.IO) {
        _isSelectingDirectory.value = true
        try {
            // 模拟一些处理时间（验证目录等）
            kotlinx.coroutines.delay(500)
            settingsManager.setWatchDirectory(path)
        } finally {
            _isSelectingDirectory.value = false
        }
    }
    
    /**
     * 设置输出目录
     */
    suspend fun setOutputDirectory(path: String) = withContext(Dispatchers.IO) {
        _isSelectingDirectory.value = true
        try {
            // 模拟一些处理时间（验证目录等）
            kotlinx.coroutines.delay(500)
            settingsManager.setOutputDirectory(path)
        } finally {
            _isSelectingDirectory.value = false
        }
    }
    
    /**
     * 选择LUT文件
     */
    fun selectLutFile(filename: String) {
        settingsManager.setSelectedLutFile(filename)
    }
    
    /**
     * 设置强度
     */
    fun setStrength(value: Int) {
        settingsManager.setStrength(value.coerceIn(0, 100))
    }
    
    /**
     * 设置质量
     */
    fun setQuality(value: Int) {
        settingsManager.setQuality(value.coerceIn(1, 100))
    }
    
    /**
     * 设置抖动类型
     */
    fun setDitherType(type: String?) {
        settingsManager.setDitherType(type)
    }
    
    /**
     * 开始监控
     */
    fun startMonitoring(): Boolean {
        try {
            val watchDir = watchDirectory.value
            val outputDir = outputDirectory.value
            val lutFileName = selectedLutFile.value
            
            if (watchDir.isNullOrEmpty() || outputDir.isNullOrEmpty() || lutFileName.isNullOrEmpty()) {
                return false
            }
            
            // 验证目录是否存在
            val watchDirFile = File(watchDir)
            val outputDirFile = File(outputDir)
            if (!watchDirFile.exists() || !watchDirFile.isDirectory) {
                return false
            }
            if (!outputDirFile.exists() || !outputDirFile.isDirectory) {
                return false
            }
            
            val lutPath = File(lutDirectory, lutFileName).absolutePath
            val lutFile = File(lutPath)
            if (!lutFile.exists() || !lutFile.isFile) {
                return false
            }
            
            val intent = Intent(context, FileMonitorService::class.java).apply {
                putExtra(FileMonitorService.EXTRA_WATCH_DIR, watchDir)
                putExtra(FileMonitorService.EXTRA_OUTPUT_DIR, outputDir)
                putExtra(FileMonitorService.EXTRA_LUT_PATH, lutPath)
                putExtra(FileMonitorService.EXTRA_STRENGTH, strength.value)
                putExtra(FileMonitorService.EXTRA_QUALITY, quality.value)
                putExtra(FileMonitorService.EXTRA_DITHER, ditherType.value)
            }
            
            // 使用前台服务启动方式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _isMonitoring.value = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            _isMonitoring.value = false
            return false
        }
    }
    
    /**
     * 停止监控
     */
    fun stopMonitoring() {
        try {
            val intent = Intent(context, FileMonitorService::class.java)
            context.stopService(intent)
            _isMonitoring.value = false
        } catch (e: Exception) {
            e.printStackTrace()
            // 即使停止服务失败，也要更新状态
            _isMonitoring.value = false
        }
    }
    
    /**
     * 获取LUT文件的完整路径
     */
    fun getLutFilePath(filename: String): String {
        return File(lutDirectory, filename).absolutePath
    }
    
    /**
     * 添加处理历史记录
     */
    fun addProcessedImage(
        originalPath: String,
        processedPath: String,
        lutFileName: String,
        strength: Int,
        quality: Int,
        ditherType: String?
    ) {
        val processedImage = ProcessedImage(
            originalPath = originalPath,
            processedPath = processedPath,
            timestamp = System.currentTimeMillis(),
            lutFileName = lutFileName,
            strength = strength,
            quality = quality,
            ditherType = ditherType
        )
        
        addProcessedImage(processedImage)
    }
    
    /**
     * 添加处理历史记录（重载方法）
     */
    private fun addProcessedImage(processedImage: ProcessedImage) {
        val currentList = _processedImages.value.toMutableList()
        currentList.add(0, processedImage) // 添加到列表开头（最新的在前面）
        
        // 限制历史记录数量，保留最近的100条
        if (currentList.size > 100) {
            currentList.removeAt(currentList.size - 1)
        }
        
        _processedImages.value = currentList
    }
    
    /**
     * 清空处理历史记录
     */
    fun clearProcessedImages() {
        _processedImages.value = emptyList()
    }
    
    /**
     * 刷新处理历史记录（从输出目录扫描）
     */
    suspend fun refreshProcessedImages() = withContext(Dispatchers.IO) {
        _isRefreshingImages.value = true
        try {
            val outputDir = outputDirectory.value ?: return@withContext
            val outputDirectory = File(outputDir)
            
            if (!outputDirectory.exists() || !outputDirectory.isDirectory) {
                return@withContext
            }
            
            val processedFiles = outputDirectory.listFiles()
                ?.filter { it.isFile && FileUtils.isImageFile(it.name) }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            
            val processedImages = processedFiles.map { file ->
                ProcessedImage(
                    originalPath = "", // 原始路径未知
                    processedPath = file.absolutePath,
                    timestamp = file.lastModified(),
                    lutFileName = selectedLutFile.value ?: "unknown",
                    strength = strength.value,
                    quality = quality.value,
                    ditherType = ditherType.value
                )
            }
            
            withContext(Dispatchers.Main) {
                _processedImages.value = processedImages
            }
        } finally {
            _isRefreshingImages.value = false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // viewModelScope 会自动取消协程，无需手动处理
    }
    
    /**
     * 添加文件到处理队列
     */
    private fun addToProcessingQueue(item: ProcessingQueueItem) {
        val currentQueue = _processingQueue.value.toMutableList()
        currentQueue.add(item)
        _processingQueue.value = currentQueue
    }
    
    /**
     * 更新处理队列中的项目状态
     */
    private fun updateProcessingQueueItem(
        itemId: String,
        status: ProcessingStatus,
        progress: Float? = null,
        errorMessage: String? = null
    ) {
        val currentQueue = _processingQueue.value.toMutableList()
        val index = currentQueue.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val updatedItem = currentQueue[index].copy(
                status = status,
                progress = progress ?: currentQueue[index].progress,
                errorMessage = errorMessage
            )
            currentQueue[index] = updatedItem
            _processingQueue.value = currentQueue
            
            // 如果处理完成，从队列中移除（延迟移除，让用户看到完成状态）
            if (status == ProcessingStatus.COMPLETED) {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000) // 2秒后移除
                    removeFromProcessingQueue(itemId)
                }
            }
        }
    }
    
    /**
     * 从处理队列中移除项目
     */
    private fun removeFromProcessingQueue(itemId: String) {
        val currentQueue = _processingQueue.value.toMutableList()
        currentQueue.removeAll { it.id == itemId }
        _processingQueue.value = currentQueue
    }
    
    /**
     * 清空处理队列
     */
    fun clearProcessingQueue() {
        _processingQueue.value = emptyList()
    }
    
    /**
     * 手动处理选中的图片
     */
    suspend fun processSelectedImages(
        selectedImages: List<String>,
        lutFileName: String,
        strength: Int,
        quality: Int,
        ditherType: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val outputDir = outputDirectory.value
            if (outputDir.isNullOrEmpty()) {
                return@withContext false
            }
            
            val outputDirFile = File(outputDir)
            if (!outputDirFile.exists() || !outputDirFile.isDirectory) {
                return@withContext false
            }
            
            val lutPath = File(lutDirectory, lutFileName).absolutePath
            val lutFile = File(lutPath)
            if (!lutFile.exists() || !lutFile.isFile) {
                return@withContext false
            }
            
            // 为每个选中的图片创建处理队列项
            selectedImages.forEach { imagePath ->
                val imageFile = File(imagePath)
                val outputFileName = "${imageFile.nameWithoutExtension}_processed.${imageFile.extension}"
                val outputPath = File(outputDirFile, outputFileName).absolutePath
                
                val queueItem = ProcessingQueueItem(
                    id = UUID.randomUUID().toString(),
                    fileName = imageFile.name,
                    filePath = imagePath,
                    status = ProcessingStatus.WAITING,
                    timestamp = System.currentTimeMillis(),
                    progress = 0f,
                    errorMessage = null,
                    outputPath = outputPath,
                    addedTime = System.currentTimeMillis()
                )
                
                withContext(Dispatchers.Main) {
                    addToProcessingQueue(queueItem)
                }
                
                // 发送事件通知有新文件添加到队列
                EventBus.sendEvent(ProcessingEvents.FileAddedToQueue(queueItem))
            }
            
            // 启动处理服务
            val intent = Intent(context, FileMonitorService::class.java).apply {
                putExtra(FileMonitorService.EXTRA_MANUAL_PROCESS, true)
                putExtra(FileMonitorService.EXTRA_SELECTED_IMAGES, selectedImages.toTypedArray())
                putExtra(FileMonitorService.EXTRA_OUTPUT_DIR, outputDir)
                putExtra(FileMonitorService.EXTRA_LUT_PATH, lutPath)
                putExtra(FileMonitorService.EXTRA_STRENGTH, strength)
                putExtra(FileMonitorService.EXTRA_QUALITY, quality)
                putExtra(FileMonitorService.EXTRA_DITHER, ditherType)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}