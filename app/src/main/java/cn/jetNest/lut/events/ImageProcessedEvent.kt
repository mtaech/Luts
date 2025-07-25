package cn.jetNest.lut.events

/**
 * 图片处理完成事件
 */
data class ImageProcessedEvent(
    val originalPath: String,
    val processedPath: String,
    val lutFileName: String,
    val strength: Int,
    val quality: Int,
    val ditherType: String?
)