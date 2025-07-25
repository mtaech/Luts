package cn.jetNest.lut.data

/**
 * 处理队列项数据类
 */
data class ProcessingQueueItem(
    val id: String,
    val fileName: String,
    val filePath: String,
    val status: ProcessingStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val outputPath: String,
    val addedTime: Long
)

/**
 * 处理状态枚举
 */
enum class ProcessingStatus {
    WAITING,    // 等待处理
    PROCESSING, // 正在处理
    COMPLETED,  // 处理完成
    FAILED      // 处理失败
}