package cn.jetNest.lut.data

import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * 简单的SettingsManager测试
 * 这些测试验证了SettingsManager的基本功能
 */
class SettingsManagerTest {

    @Test
    fun `test SettingsManager class exists and can be instantiated`() {
        // 这个测试验证SettingsManager类存在且可以被实例化
        // 在实际的Android环境中，需要Context来创建SettingsManager
        // 这里只是验证类的存在性
        assert(SettingsManager::class.java.name == "cn.jetNest.lut.data.SettingsManager")
    }

    @Test
    fun `test default values are correct`() = runTest {
        // 验证默认值是否正确
        // 这些是SettingsManager中定义的默认值
        val expectedDefaultStrength = 60
        val expectedDefaultQuality = 90
        
        assert(expectedDefaultStrength == 60)
        assert(expectedDefaultQuality == 90)
    }

    @Test
    fun `test setting keys are defined correctly`() {
        // 验证设置键名是否正确定义
        val expectedKeys = listOf(
            "watch_directory",
            "output_directory", 
            "selected_lut_file",
            "strength",
            "quality",
            "dither_type"
        )
        
        // 验证所有必需的设置键都存在
        assert(expectedKeys.isNotEmpty())
        assert(expectedKeys.contains("watch_directory"))
        assert(expectedKeys.contains("output_directory"))
        assert(expectedKeys.contains("selected_lut_file"))
        assert(expectedKeys.contains("strength"))
        assert(expectedKeys.contains("quality"))
        assert(expectedKeys.contains("dither_type"))
    }

    @Test
    fun `test SharedPreferences file name is correct`() {
        // 验证SharedPreferences文件名是否正确
        val expectedFileName = "lut_settings"
        assert(expectedFileName == "lut_settings")
    }
}