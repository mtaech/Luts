package cn.jetNest.lut.events

/**
 * 处理队列更新事件
 */
data class ProcessingQueueUpdateEvent(
    val itemId: String,
    val status: cn.jetNest.lut.data.ProcessingStatus,
    val progress: Float = 0f,
    val errorMessage: String? = null
)

/**
 * 新文件添加到队列事件
 */
data class FileAddedToQueueEvent(
    val queueItem: cn.jetNest.lut.data.ProcessingQueueItem
)

/**
 * 处理事件的密封类
 */
sealed class ProcessingEvents {
    data class FileAddedToQueue(val queueItem: cn.jetNest.lut.data.ProcessingQueueItem) : ProcessingEvents()
    data class QueueUpdate(
        val itemId: String,
        val status: cn.jetNest.lut.data.ProcessingStatus,
        val progress: Float = 0f,
        val errorMessage: String? = null
    ) : ProcessingEvents()
}