package cn.jetNest.lut.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 事件总线，用于应用内组件间通信
 * 使用SharedFlow替代LocalBroadcastManager
 */
object EventBus {
    
    private val _imageProcessedEvents = MutableSharedFlow<ImageProcessedEvent>(
        replay = 0, // 不重放历史事件
        extraBufferCapacity = 10 // 额外缓冲容量
    )
    
    private val _processingQueueUpdateEvents = MutableSharedFlow<ProcessingQueueUpdateEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )
    
    private val _fileAddedToQueueEvents = MutableSharedFlow<FileAddedToQueueEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )
    
    private val _processingEvents = MutableSharedFlow<ProcessingEvents>(
        replay = 0,
        extraBufferCapacity = 10
    )
    
    /**
     * 图片处理完成事件流
     */
    val imageProcessedEvents: SharedFlow<ImageProcessedEvent> = _imageProcessedEvents.asSharedFlow()
    
    /**
     * 处理队列更新事件流
     */
    val processingQueueUpdateEvents: SharedFlow<ProcessingQueueUpdateEvent> = _processingQueueUpdateEvents.asSharedFlow()
    
    /**
     * 文件添加到队列事件流
     */
    val fileAddedToQueueEvents: SharedFlow<FileAddedToQueueEvent> = _fileAddedToQueueEvents.asSharedFlow()
    
    /**
     * 处理事件流
     */
    val processingEvents: SharedFlow<ProcessingEvents> = _processingEvents.asSharedFlow()
    
    /**
     * 发送图片处理完成事件
     */
    suspend fun sendImageProcessedEvent(event: ImageProcessedEvent) {
        _imageProcessedEvents.emit(event)
    }
    
    /**
     * 发送处理队列更新事件
     */
    suspend fun sendProcessingQueueUpdateEvent(event: ProcessingQueueUpdateEvent) {
        _processingQueueUpdateEvents.emit(event)
    }
    
    /**
     * 发送文件添加到队列事件
     */
    suspend fun sendFileAddedToQueueEvent(event: FileAddedToQueueEvent) {
        _fileAddedToQueueEvents.emit(event)
    }
    
    /**
     * 发送处理事件
     */
    suspend fun sendEvent(event: ProcessingEvents) {
        _processingEvents.emit(event)
    }
}