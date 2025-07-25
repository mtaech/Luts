package cn.jetNest.lut.processor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.math.*
import androidx.core.graphics.createBitmap

class LutProcessor(
    private val lutPath: String,
    private val strength: Int = 60,
    private val quality: Int = 90,
    private val ditherType: DitherType? = null
) {
    
    enum class DitherType {
        FLOYD_STEINBERG,
        RANDOM
    }
    
    private var lut: Array<Array<Array<FloatArray>>>? = null
    private var lutSize: Int = 0
    
    init {
        loadCubeLut(lutPath)
    }
    
    /**
     * 加载CUBE格式的LUT文件
     */
    private fun loadCubeLut(filePath: String) {
        try {
            val lines = File(filePath).readLines()
                .filter { it.trim().isNotEmpty() }
            
            var size: Int? = null
            var dataStart = 0
            
            // 查找LUT_3D_SIZE
            for (i in lines.indices) {
                val line = lines[i].trim()
                if (line.startsWith("LUT_3D_SIZE")) {
                    size = line.split(" ")[1].toInt()
                    dataStart = i + 1
                    break
                }
            }
            
            if (size == null) {
                throw IllegalArgumentException("LUT_3D_SIZE未在CUBE文件中找到")
            }
            
            lutSize = size
            
            // 解析数据
            val data = mutableListOf<FloatArray>()
            for (i in dataStart until lines.size) {
                val line = lines[i].trim()
                if (line.startsWith("#")) continue
                
                val parts = line.split(" ")
                if (parts.size == 3) {
                    try {
                        val r = parts[0].toFloat()
                        val g = parts[1].toFloat()
                        val b = parts[2].toFloat()
                        data.add(floatArrayOf(r, g, b))
                    } catch (e: NumberFormatException) {
                        continue
                    }
                }
            }
            
            val expectedLength = size * size * size
            if (data.size != expectedLength) {
                throw IllegalArgumentException("需要${expectedLength}个数据点，实际找到${data.size}个")
            }
            
            // 创建3D LUT数组
            lut = Array(size) { Array(size) { Array(size) { FloatArray(3) } } }
            var index = 0
            for (b in 0 until size) {
                for (g in 0 until size) {
                    for (r in 0 until size) {
                        lut!![b][g][r] = data[index]
                        index++
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("加载LUT文件失败: ${e.message}", e)
        }
    }
    
    /**
     * 三线性插值应用LUT
     */
    private fun trilinearInterpolation(r: Float, g: Float, b: Float): FloatArray {
        val scale = (lutSize - 1).toFloat()
        val rIdx = r * scale
        val gIdx = g * scale
        val bIdx = b * scale
        
        // 计算整数和小数部分
        val r0 = floor(rIdx).toInt().coerceIn(0, lutSize - 1)
        val g0 = floor(gIdx).toInt().coerceIn(0, lutSize - 1)
        val b0 = floor(bIdx).toInt().coerceIn(0, lutSize - 1)
        
        val r1 = (r0 + 1).coerceIn(0, lutSize - 1)
        val g1 = (g0 + 1).coerceIn(0, lutSize - 1)
        val b1 = (b0 + 1).coerceIn(0, lutSize - 1)
        
        val rD = rIdx - r0
        val gD = gIdx - g0
        val bD = bIdx - b0
        
        // 获取8个角点的值
        val c000 = lut!![b0][g0][r0]
        val c001 = lut!![b0][g0][r1]
        val c010 = lut!![b0][g1][r0]
        val c011 = lut!![b0][g1][r1]
        val c100 = lut!![b1][g0][r0]
        val c101 = lut!![b1][g0][r1]
        val c110 = lut!![b1][g1][r0]
        val c111 = lut!![b1][g1][r1]
        
        // 在r方向插值
        val c00 = FloatArray(3) { i -> c000[i] * (1 - rD) + c001[i] * rD }
        val c01 = FloatArray(3) { i -> c010[i] * (1 - rD) + c011[i] * rD }
        val c10 = FloatArray(3) { i -> c100[i] * (1 - rD) + c101[i] * rD }
        val c11 = FloatArray(3) { i -> c110[i] * (1 - rD) + c111[i] * rD }
        
        // 在g方向插值
        val c0 = FloatArray(3) { i -> c00[i] * (1 - gD) + c01[i] * gD }
        val c1 = FloatArray(3) { i -> c10[i] * (1 - gD) + c11[i] * gD }
        
        // 在b方向插值
        return FloatArray(3) { i -> c0[i] * (1 - bD) + c1[i] * bD }
    }
    
    /**
     * Floyd-Steinberg抖动
     */
    private fun applyFloydSteinbergDithering(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val errorBuffer = Array(height) { Array(width) { FloatArray(3) } }
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                
                // 提取RGB值并添加误差
                val r = ((pixel shr 16) and 0xFF) / 255f + errorBuffer[y][x][0]
                val g = ((pixel shr 8) and 0xFF) / 255f + errorBuffer[y][x][1]
                val b = (pixel and 0xFF) / 255f + errorBuffer[y][x][2]
                
                // 量化
                val newR = round(r * 255f) / 255f
                val newG = round(g * 255f) / 255f
                val newB = round(b * 255f) / 255f
                
                // 计算误差
                val errorR = r - newR
                val errorG = g - newG
                val errorB = b - newB
                
                // 更新像素
                val newPixel = (0xFF shl 24) or
                        ((newR * 255).toInt().coerceIn(0, 255) shl 16) or
                        ((newG * 255).toInt().coerceIn(0, 255) shl 8) or
                        (newB * 255).toInt().coerceIn(0, 255)
                pixels[y * width + x] = newPixel
                
                // 传播误差
                if (x < width - 1) {
                    errorBuffer[y][x + 1][0] += errorR * 7f / 16f
                    errorBuffer[y][x + 1][1] += errorG * 7f / 16f
                    errorBuffer[y][x + 1][2] += errorB * 7f / 16f
                }
                if (y < height - 1) {
                    if (x > 0) {
                        errorBuffer[y + 1][x - 1][0] += errorR * 3f / 16f
                        errorBuffer[y + 1][x - 1][1] += errorG * 3f / 16f
                        errorBuffer[y + 1][x - 1][2] += errorB * 3f / 16f
                    }
                    errorBuffer[y + 1][x][0] += errorR * 5f / 16f
                    errorBuffer[y + 1][x][1] += errorG * 5f / 16f
                    errorBuffer[y + 1][x][2] += errorB * 5f / 16f
                    if (x < width - 1) {
                        errorBuffer[y + 1][x + 1][0] += errorR * 1f / 16f
                        errorBuffer[y + 1][x + 1][1] += errorG * 1f / 16f
                        errorBuffer[y + 1][x + 1][2] += errorB * 1f / 16f
                    }
                }
            }
        }
        
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * 随机抖动
     */
    private fun applyRandomDithering(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            
            val r = ((pixel shr 16) and 0xFF) + (Math.random() - 0.5f) * 1f
            val g = ((pixel shr 8) and 0xFF) + (Math.random() - 0.5f) * 1f
            val b = (pixel and 0xFF) + (Math.random() - 0.5f) * 1f
            
            val newPixel = (0xFF shl 24) or
                    (r.toInt().coerceIn(0, 255) shl 16) or
                    (g.toInt().coerceIn(0, 255) shl 8) or
                    b.toInt().coerceIn(0, 255)
            pixels[i] = newPixel
        }
        
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * 根据强度混合原始图像和LUT处理后的图像
     */
    private fun applyLutWithStrength(original: Bitmap, lutOutput: Bitmap): Bitmap {
        val strengthRatio = (strength / 100f).coerceIn(0f, 1f)
        val width = original.width
        val height = original.height
        
        val result = createBitmap(width, height)
        val canvas = Canvas(result)
        val paint = Paint()
        
        // 绘制原始图像
        paint.alpha = ((1 - strengthRatio) * 255).toInt()
        canvas.drawBitmap(original, 0f, 0f, paint)
        
        // 绘制LUT处理后的图像
        paint.alpha = (strengthRatio * 255).toInt()
        canvas.drawBitmap(lutOutput, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * 处理图像
     */
    suspend fun processImage(inputPath: String, outputPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 读取图像
            val originalBitmap = BitmapFactory.decodeFile(inputPath)
                ?: throw IllegalArgumentException("无法读取图像文件: $inputPath")
            
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            // 创建LUT处理后的图像
            val lutBitmap = createBitmap(width, height)
            val pixels = IntArray(width * height)
            originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // 应用LUT到每个像素
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f
                
                val lutResult = trilinearInterpolation(r, g, b)
                
                val newPixel = (0xFF shl 24) or
                        ((lutResult[0] * 255).toInt().coerceIn(0, 255) shl 16) or
                        ((lutResult[1] * 255).toInt().coerceIn(0, 255) shl 8) or
                        (lutResult[2] * 255).toInt().coerceIn(0, 255)
                pixels[i] = newPixel
            }
            
            lutBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            
            // 混合原始图像和LUT处理后的图像
            var finalBitmap = applyLutWithStrength(originalBitmap, lutBitmap)
            
            // 应用抖动
            ditherType?.let { dither ->
                finalBitmap = when (dither) {
                    DitherType.FLOYD_STEINBERG -> applyFloydSteinbergDithering(finalBitmap)
                    DitherType.RANDOM -> applyRandomDithering(finalBitmap)
                }
            }
            
            // 保存图像
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            
            // 复制EXIF数据
            try {
                val inputExif = ExifInterface(inputPath)
                val outputExif = ExifInterface(outputPath)
                
                val attributes = arrayOf(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE
                )
                
                for (attribute in attributes) {
                    val value = inputExif.getAttribute(attribute)
                    if (value != null) {
                        outputExif.setAttribute(attribute, value)
                    }
                }
                outputExif.saveAttributes()
            } catch (e: Exception) {
                // EXIF复制失败不影响主要功能
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}