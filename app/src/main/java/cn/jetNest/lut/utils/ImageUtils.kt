package cn.jetNest.lut.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.File

object ImageUtils {
    
    /**
     * 从文件路径加载位图
     */
    fun loadBitmapFromFile(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(filePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 加载缩略图
     */
    fun loadThumbnail(filePath: String, maxSize: Int): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            
            // 首先获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            
            // 计算缩放比例
            val scale = calculateInSampleSize(options, maxSize, maxSize)
            
            // 加载缩放后的图片
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
                inJustDecodeBounds = false
            }
            
            val bitmap = BitmapFactory.decodeFile(filePath, loadOptions)
            
            // 如果还是太大，进一步缩放
            bitmap?.let { bmp ->
                if (bmp.width > maxSize || bmp.height > maxSize) {
                    val scaleFactor = minOf(
                        maxSize.toFloat() / bmp.width,
                        maxSize.toFloat() / bmp.height
                    )
                    
                    val matrix = Matrix().apply {
                        setScale(scaleFactor, scaleFactor)
                    }
                    
                    Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                } else {
                    bmp
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 计算合适的采样率
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && 
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * 获取图片文件大小（字节）
     */
    fun getImageFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 获取图片尺寸信息
     */
    fun getImageDimensions(filePath: String): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}