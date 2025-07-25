package cn.jetNest.lut.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cn.jetNest.lut.MainActivity
import cn.jetNest.lut.R
import cn.jetNest.lut.processor.LutProcessor
import cn.jetNest.lut.events.EventBus
import cn.jetNest.lut.events.ImageProcessedEvent
import cn.jetNest.lut.data.ProcessingQueueItem
import cn.jetNest.lut.data.ProcessingStatus
import cn.jetNest.lut.events.FileAddedToQueueEvent
import cn.jetNest.lut.events.ProcessingQueueUpdateEvent
import cn.jetNest.lut.utils.FileUtils
import kotlinx.coroutines.*
import java.io.File

class FileMonitorService : Service() {
    
    private var fileObserver: FileObserver? = null
    private var lutProcessor: LutProcessor? = null
    private var outputDir: String? = null
    private var lutFileName: String? = null
    private var currentStrength: Int = 60
    private var currentQuality: Int = 90
    private var currentDitherType: String? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        const val EXTRA_WATCH_DIR = "watch_dir"
        const val EXTRA_OUTPUT_DIR = "output_dir"
        const val EXTRA_LUT_PATH = "lut_path"
        const val EXTRA_STRENGTH = "strength"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_DITHER = "dither"
        const val EXTRA_MANUAL_PROCESS = "manual_process"
        const val EXTRA_SELECTED_IMAGES = "selected_images"
        
        // 前台服务相关常量
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "file_monitor_channel"
        private const val CHANNEL_NAME = "文件监控服务"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        // 处理停止服务的动作
        if (intent.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }
        
        // 创建通知渠道（Android 8.0+）
        createNotificationChannel()
        
        // 启动前台服务
        val notification = createNotification("正在监控新增文件...")
        startForeground(NOTIFICATION_ID, notification)
        
        try {
            setupMonitoring(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }
        
        // 返回 START_REDELIVER_INTENT 以确保服务在被杀死后能重启并重新传递Intent
        return START_REDELIVER_INTENT
    }
    
    private fun setupMonitoring(intent: Intent) {
        // 检查Intent的extras是否为null
        val extras = intent.extras
        if (extras == null) {
            throw IllegalArgumentException("Intent extras cannot be null")
        }
        
        val isManualProcess = intent.getBooleanExtra(EXTRA_MANUAL_PROCESS, false)
        val outputDirPath = intent.getStringExtra(EXTRA_OUTPUT_DIR)
        val lutPath = intent.getStringExtra(EXTRA_LUT_PATH)
        
        if (outputDirPath.isNullOrEmpty()) {
            throw IllegalArgumentException("Output directory cannot be null or empty")
        }
        if (lutPath.isNullOrEmpty()) {
            throw IllegalArgumentException("LUT path cannot be null or empty")
        }
        
        outputDir = outputDirPath
        currentStrength = intent.getIntExtra(EXTRA_STRENGTH, 60)
        currentQuality = intent.getIntExtra(EXTRA_QUALITY, 90)
        currentDitherType = intent.getStringExtra(EXTRA_DITHER)
        
        // 从LUT路径提取文件名
        lutFileName = File(lutPath).name
        
        val ditherType = currentDitherType?.let {
            when (it) {
                "floyd" -> LutProcessor.DitherType.FLOYD_STEINBERG
                "random" -> LutProcessor.DitherType.RANDOM
                else -> null
            }
        }
        
        try {
            lutProcessor = LutProcessor(lutPath, currentStrength, currentQuality, ditherType)
            
            if (isManualProcess) {
                // 手动处理模式
                val selectedImages = intent.getStringArrayExtra(EXTRA_SELECTED_IMAGES)
                if (selectedImages != null) {
                    processSelectedImages(selectedImages.toList())
                }
            } else {
                // 自动监控模式
                val watchDir = intent.getStringExtra(EXTRA_WATCH_DIR)
                if (watchDir.isNullOrEmpty()) {
                    throw IllegalArgumentException("Watch directory cannot be null or empty")
                }
                startWatching(watchDir)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    
    private fun startWatching(watchDir: String) {
        stopWatching()
        
        fileObserver = object : FileObserver(watchDir, CREATE or MOVED_TO) {
            override fun onEvent(event: Int, path: String?) {
                path?.let { filename ->
                    if (FileUtils.isImageFile(filename)) {
                        val inputFile = File(watchDir, filename)
                        val outputFile = File(outputDir!!, filename)
                        
                        // 创建处理队列项目
                        val queueItem = ProcessingQueueItem(
                            id = "${System.currentTimeMillis()}_${filename}",
                            fileName = filename,
                            filePath = inputFile.absolutePath,
                            status = ProcessingStatus.WAITING,
                            timestamp = System.currentTimeMillis(),
                            progress = 0f,
                            errorMessage = null,
                            outputPath = outputFile.absolutePath,
                            addedTime = System.currentTimeMillis()
                        )
                        
                        // 发送文件添加到队列事件
                        serviceScope.launch {
                            EventBus.sendFileAddedToQueueEvent(FileAddedToQueueEvent(queueItem))
                        }
                        
                        // 等待文件完全写入并处理
                        serviceScope.launch {
                            delay(1000)
                            if (inputFile.exists()) {
                                // 更新状态为处理中
                                EventBus.sendProcessingQueueUpdateEvent(
                                    ProcessingQueueUpdateEvent(queueItem.id, ProcessingStatus.PROCESSING)
                                )
                                
                                val success = lutProcessor?.processImage(inputFile.absolutePath, outputFile.absolutePath) ?: false
                                if (success) {
                                    // 更新通知显示处理结果
                                    updateNotification("已处理: ${filename}")
                                    
                                    // 更新状态为完成
                                    EventBus.sendProcessingQueueUpdateEvent(
                                        ProcessingQueueUpdateEvent(queueItem.id, ProcessingStatus.COMPLETED)
                                    )
                                    
                                    sendImageProcessedEvent(inputFile.absolutePath, outputFile.absolutePath)
                                } else {
                                    // 更新状态为失败
                                    EventBus.sendProcessingQueueUpdateEvent(
                                        ProcessingQueueUpdateEvent(queueItem.id, ProcessingStatus.FAILED, errorMessage = "处理失败")
                                    )
                                }
                            } else {
                                // 文件不存在，标记为失败
                                EventBus.sendProcessingQueueUpdateEvent(
                                    ProcessingQueueUpdateEvent(queueItem.id, ProcessingStatus.FAILED, errorMessage = "文件不存在")
                                )
                            }
                        }
                    }
                }
            }
        }
        
        fileObserver?.startWatching()
    }
    
    private suspend fun sendImageProcessedEvent(originalPath: String, processedPath: String) {
        val event = ImageProcessedEvent(
            originalPath = originalPath,
            processedPath = processedPath,
            lutFileName = lutFileName ?: "unknown",
            strength = currentStrength,
            quality = currentQuality,
            ditherType = currentDitherType
        )
        EventBus.sendImageProcessedEvent(event)
    }
    
    /**
     * 处理选中的图片列表
     */
    private fun processSelectedImages(imagePaths: List<String>) {
        serviceScope.launch {
            updateNotification("正在处理 ${imagePaths.size} 张图片...")
            
            imagePaths.forEachIndexed { index, imagePath ->
                val inputFile = File(imagePath)
                val outputFileName = "${System.currentTimeMillis()}_${inputFile.name}"
                val outputFile = File(outputDir!!, outputFileName)
                
                val queueItemId = "${System.currentTimeMillis()}_${inputFile.name}"
                
                try {
                    // 更新状态为处理中
                    EventBus.sendProcessingQueueUpdateEvent(
                        ProcessingQueueUpdateEvent(queueItemId, ProcessingStatus.PROCESSING, (index.toFloat() / imagePaths.size))
                    )
                    
                    val success = lutProcessor?.processImage(inputFile.absolutePath, outputFile.absolutePath) ?: false
                    
                    if (success) {
                        // 更新状态为完成
                        EventBus.sendProcessingQueueUpdateEvent(
                            ProcessingQueueUpdateEvent(queueItemId, ProcessingStatus.COMPLETED, 1.0f)
                        )
                        
                        sendImageProcessedEvent(inputFile.absolutePath, outputFile.absolutePath)
                        updateNotification("已处理: ${inputFile.name} (${index + 1}/${imagePaths.size})")
                    } else {
                        // 更新状态为失败
                        EventBus.sendProcessingQueueUpdateEvent(
                            ProcessingQueueUpdateEvent(queueItemId, ProcessingStatus.FAILED, errorMessage = "处理失败")
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    EventBus.sendProcessingQueueUpdateEvent(
                        ProcessingQueueUpdateEvent(queueItemId, ProcessingStatus.FAILED, errorMessage = e.message ?: "未知错误")
                    )
                }
                
                // 添加延迟避免过快处理
                delay(100)
            }
            
            updateNotification("处理完成，共处理 ${imagePaths.size} 张图片")
            
            // 处理完成后停止服务
            delay(3000)
            stopSelf()
        }
    }
    
    private fun stopWatching() {
        fileObserver?.stopWatching()
        fileObserver = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopWatching()
        serviceScope.cancel()
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // 提高重要性以增加服务优先级
            ).apply {
                description = "LUT图像处理文件监控服务"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null) // 禁用声音
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 创建停止服务的PendingIntent
        val stopIntent = Intent(this, FileMonitorService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LUT图像处理")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification) // 需要添加通知图标
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 持续通知，不能被滑动删除
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 提高优先级
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(R.drawable.ic_notification, "停止监控", stopPendingIntent)
            .build()
    }
    
    /**
     * 更新通知内容
     */
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}