package cn.jetNest.lut.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 设置管理器，负责应用设置的持久化存储
 */
class SettingsManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 监控目录
    private val _watchDirectory = MutableStateFlow(getWatchDirectory())
    val watchDirectory: StateFlow<String?> = _watchDirectory.asStateFlow()
    
    // 输出目录
    private val _outputDirectory = MutableStateFlow(getOutputDirectory())
    val outputDirectory: StateFlow<String?> = _outputDirectory.asStateFlow()
    
    // 选中的LUT文件
    private val _selectedLutFile = MutableStateFlow(getSelectedLutFile())
    val selectedLutFile: StateFlow<String?> = _selectedLutFile.asStateFlow()
    
    // 处理参数
    private val _strength = MutableStateFlow(getStrength())
    val strength: StateFlow<Int> = _strength.asStateFlow()
    
    private val _quality = MutableStateFlow(getQuality())
    val quality: StateFlow<Int> = _quality.asStateFlow()
    
    private val _ditherType = MutableStateFlow(getDitherType())
    val ditherType: StateFlow<String?> = _ditherType.asStateFlow()
    
    /**
     * 设置监控目录
     */
    fun setWatchDirectory(path: String?) {
        val normalizedPath = path?.takeIf { it.isNotBlank() }
        sharedPreferences.edit()
            .putString(KEY_WATCH_DIRECTORY, normalizedPath)
            .apply()
        _watchDirectory.value = normalizedPath
    }
    
    /**
     * 获取监控目录
     */
    private fun getWatchDirectory(): String? {
        return sharedPreferences.getString(KEY_WATCH_DIRECTORY, null)?.takeIf { it.isNotBlank() }
    }
    
    /**
     * 设置输出目录
     */
    fun setOutputDirectory(path: String?) {
        val normalizedPath = path?.takeIf { it.isNotBlank() }
        sharedPreferences.edit()
            .putString(KEY_OUTPUT_DIRECTORY, normalizedPath)
            .apply()
        _outputDirectory.value = normalizedPath
    }
    
    /**
     * 获取输出目录
     */
    private fun getOutputDirectory(): String? {
        return sharedPreferences.getString(KEY_OUTPUT_DIRECTORY, null)?.takeIf { it.isNotBlank() }
    }
    
    /**
     * 设置选中的LUT文件
     */
    fun setSelectedLutFile(filename: String?) {
        val normalizedFilename = filename?.takeIf { it.isNotBlank() }
        sharedPreferences.edit()
            .putString(KEY_SELECTED_LUT_FILE, normalizedFilename)
            .apply()
        _selectedLutFile.value = normalizedFilename
    }
    
    /**
     * 获取选中的LUT文件
     */
    private fun getSelectedLutFile(): String? {
        return sharedPreferences.getString(KEY_SELECTED_LUT_FILE, null)?.takeIf { it.isNotBlank() }
    }
    
    /**
     * 设置强度
     */
    fun setStrength(value: Int) {
        val clampedValue = value.coerceIn(0, 100)
        sharedPreferences.edit()
            .putInt(KEY_STRENGTH, clampedValue)
            .apply()
        _strength.value = clampedValue
    }
    
    /**
     * 获取强度
     */
    private fun getStrength(): Int {
        return sharedPreferences.getInt(KEY_STRENGTH, DEFAULT_STRENGTH)
    }
    
    /**
     * 设置质量
     */
    fun setQuality(value: Int) {
        val clampedValue = value.coerceIn(1, 100)
        sharedPreferences.edit()
            .putInt(KEY_QUALITY, clampedValue)
            .apply()
        _quality.value = clampedValue
    }
    
    /**
     * 获取质量
     */
    private fun getQuality(): Int {
        return sharedPreferences.getInt(KEY_QUALITY, DEFAULT_QUALITY)
    }
    
    /**
     * 设置抖动类型
     */
    fun setDitherType(type: String?) {
        val normalizedType = type?.takeIf { it.isNotBlank() }
        sharedPreferences.edit()
            .putString(KEY_DITHER_TYPE, normalizedType)
            .apply()
        _ditherType.value = normalizedType
    }
    
    /**
     * 获取抖动类型
     */
    private fun getDitherType(): String? {
        return sharedPreferences.getString(KEY_DITHER_TYPE, null)?.takeIf { it.isNotBlank() }
    }
    
    /**
     * 清除所有设置
     */
    fun clearAllSettings() {
        sharedPreferences.edit().clear().apply()
        _watchDirectory.value = null
        _outputDirectory.value = null
        _selectedLutFile.value = null
        _strength.value = DEFAULT_STRENGTH
        _quality.value = DEFAULT_QUALITY
        _ditherType.value = null
    }
    
    companion object {
        private const val PREFS_NAME = "lut_app_settings"
        
        // 设置键名
        private const val KEY_WATCH_DIRECTORY = "watch_directory"
        private const val KEY_OUTPUT_DIRECTORY = "output_directory"
        private const val KEY_SELECTED_LUT_FILE = "selected_lut_file"
        private const val KEY_STRENGTH = "strength"
        private const val KEY_QUALITY = "quality"
        private const val KEY_DITHER_TYPE = "dither_type"
        
        // 默认值
        private const val DEFAULT_STRENGTH = 60
        private const val DEFAULT_QUALITY = 90
    }
}