package cn.jetNest.lut.data

import java.io.File

/**
 * 处理历史记录数据类
 */
data class ProcessedImage(
    val originalPath: String,
    val processedPath: String,
    val timestamp: Long,
    val lutFileName: String,
    val strength: Int,
    val quality: Int,
    val ditherType: String?
) {
    val originalFile: File get() = File(originalPath)
    val processedFile: File get() = File(processedPath)
    val originalName: String get() = originalFile.name
    val processedName: String get() = processedFile.name
}