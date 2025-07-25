package cn.jetNest.lut.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File

object FileUtils {
    
    /**
     * 从URI获取真实的文件路径
     */
    fun getRealPathFromURI(uri: Uri, context: Context): String? {
        return try {
            when {
                // DocumentProvider
                DocumentsContract.isDocumentUri(context, uri) -> {
                    when {
                        // ExternalStorageProvider
                        isExternalStorageDocument(uri) -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":")
                            val type = split[0]
                            
                            if ("primary".equals(type, ignoreCase = true)) {
                                Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                            } else {
                                "/storage/$type/${split[1]}"
                            }
                        }
                        // DownloadsProvider
                        isDownloadsDocument(uri) -> {
                            val id = DocumentsContract.getDocumentId(uri)
                            if (id.startsWith("raw:")) {
                                id.replaceFirst("raw:", "")
                            } else {
                                getDataColumn(context, uri, null, null)
                            }
                        }
                        // MediaProvider
                        isMediaDocument(uri) -> {
                            val docId = DocumentsContract.getDocumentId(uri)
                            val split = docId.split(":")
                            val type = split[0]
                            
                            val contentUri = when (type) {
                                "image" -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                "video" -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                "audio" -> android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                else -> null
                            }
                            
                            contentUri?.let {
                                val selection = "_id=?"
                                val selectionArgs = arrayOf(split[1])
                                getDataColumn(context, it, selection, selectionArgs)
                            }
                        }
                        else -> null
                    }
                }
                // MediaStore (and general)
                "content".equals(uri.scheme, ignoreCase = true) -> {
                    getDataColumn(context, uri, null, null)
                }
                // File
                "file".equals(uri.scheme, ignoreCase = true) -> {
                    uri.path
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从树形URI获取目录路径
     */
    fun getDirectoryPathFromTreeUri(uri: Uri): String? {
        return try {
            val path = uri.path
            if (path?.startsWith("/tree/") == true) {
                val docId = path.substringAfter("/tree/").substringBefore(":")
                val subPath = if (path.contains(":")) {
                    path.substringAfter(":")
                } else ""
                
                when (docId) {
                    "primary" -> {
                        val basePath = Environment.getExternalStorageDirectory().absolutePath
                        if (subPath.isNotEmpty()) "$basePath/$subPath" else basePath
                    }
                    else -> {
                        if (subPath.isNotEmpty()) "/storage/$docId/$subPath" else "/storage/$docId"
                    }
                }
            } else {
                path
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        return try {
            val column = "_data"
            val projection = arrayOf(column)
            
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    cursor.getString(columnIndex)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
    
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }
    
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
    
    /**
     * 检查文件是否为图片
     */
    fun isImageFile(filename: String): Boolean {
        // 过滤掉以 .pending- 开头的文件
        if (filename.startsWith(".pending-")) {
            return false
        }
        
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension in listOf("jpg", "jpeg", "png", "tiff", "bmp", "webp")
    }
    
    /**
     * 检查文件是否为LUT文件
     */
    fun isLutFile(filename: String): Boolean {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension in listOf("cube", "3dl", "lut")
    }
    
    /**
     * 从URI获取文件名
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                cursor.getString(nameIndex)
                            } else null
                        } else null
                    }
                }
                "file" -> {
                    uri.path?.let { path ->
                        File(path).name
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 确保目录存在
     */
    fun ensureDirectoryExists(path: String): Boolean {
        return try {
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}