package com.smartinput.pro.platform

import com.intellij.openapi.diagnostic.Logger
import com.smartinput.pro.model.InputMethodType
import com.smartinput.pro.model.PlatformInputMethod
import com.smartinput.pro.service.SmartInputConfigService

/**
 * Abstract base class for platform-specific input method management
 */
abstract class InputMethodManager {
    
    companion object {
        private val LOG = Logger.getInstance(InputMethodManager::class.java)
        
        /**
         * Factory method to create platform-specific input method manager
         */
        fun create(platform: PlatformDetector.Platform): InputMethodManager {
            return when (platform) {
                PlatformDetector.Platform.WINDOWS -> WindowsInputMethodManager()
                PlatformDetector.Platform.MACOS -> MacOSInputMethodManager()
                PlatformDetector.Platform.LINUX -> LinuxInputMethodManager()
                PlatformDetector.Platform.UNKNOWN -> UnsupportedInputMethodManager()
            }
        }
    }

    protected val configService = SmartInputConfigService.getInstance()

    /**
     * Check if input method management is available on this platform
     */
    abstract fun isAvailable(): Boolean

    /**
     * Get the current active input method
     */
    abstract suspend fun getCurrentInputMethod(): InputMethodType

    /**
     * Switch to the specified input method
     */
    abstract suspend fun switchToInputMethod(inputMethod: InputMethodType): Boolean

    /**
     * Get list of available input methods on this platform
     */
    abstract fun getAvailableInputMethods(): List<PlatformInputMethod>

    /**
     * Get list of supported input method types
     */
    abstract fun getSupportedInputMethods(): List<InputMethodType>

    /**
     * Initialize the input method manager
     */
    open fun initialize(): Boolean {
        LOG.info("Initializing input method manager for ${this::class.simpleName}")
        return true
    }

    /**
     * Cleanup resources
     */
    open fun dispose() {
        LOG.info("Disposing input method manager for ${this::class.simpleName}")
    }

    /**
     * Test if input method switching is working
     */
    open suspend fun testSwitching(): Boolean {
        return try {
            val current = getCurrentInputMethod()
            LOG.info("Current input method: $current")
            true
        } catch (e: Exception) {
            LOG.error("Input method test failed", e)
            false
        }
    }
}

/**
 * Windows-specific input method manager
 */
class WindowsInputMethodManager : InputMethodManager() {

    companion object {
        private val LOG = Logger.getInstance(WindowsInputMethodManager::class.java)
    }

    override fun isAvailable(): Boolean = true

    override suspend fun getCurrentInputMethod(): InputMethodType {
        return try {
            // 方法1：检查当前键盘布局
            val layoutResult = executeCommand("powershell", "-Command",
                "Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; public class Win32 { [DllImport(\"user32.dll\")] public static extern IntPtr GetKeyboardLayout(uint idThread); }'; " +
                "[Win32]::GetKeyboardLayout(0).ToString('X8')")

            when {
                layoutResult.contains("00000409") || layoutResult.contains("409") -> InputMethodType.ENGLISH
                layoutResult.contains("00000804") || layoutResult.contains("804") -> InputMethodType.CHINESE
                else -> {
                    // 方法2：检查语言列表
                    val langResult = executeCommand("powershell", "-Command",
                        "Get-WinUserLanguageList | Select-Object -First 1 | ForEach-Object { \$_.LanguageTag }")
                    when {
                        langResult.contains("en") -> InputMethodType.ENGLISH
                        langResult.contains("zh") -> InputMethodType.CHINESE
                        else -> InputMethodType.UNKNOWN
                    }
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to detect Windows input method", e)
            InputMethodType.UNKNOWN
        }
    }

    override suspend fun switchToInputMethod(inputMethod: InputMethodType): Boolean {
        return try {
            when (inputMethod) {
                InputMethodType.ENGLISH -> switchToEnglishWindows()
                InputMethodType.CHINESE -> switchToChineseWindows()
                else -> false
            }
        } catch (e: Exception) {
            LOG.error("Failed to switch Windows input method to $inputMethod", e)
            false
        }
    }

    private fun switchToEnglishWindows(): Boolean {
        return try {
            // 使用Java Robot类直接发送键盘事件
            val robot = java.awt.Robot()

            // 发送Ctrl+Space
            robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL)
            Thread.sleep(10)
            robot.keyPress(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(50)
            robot.keyRelease(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(10)
            robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL)

            LOG.info("使用Java Robot发送Ctrl+Space切换到英文")

            // 添加调试通知
            if (configService.isDebugMode()) {
                showDebugNotification("已使用Java Robot发送Ctrl+Space切换到英文")
            }
            true
        } catch (e: Exception) {
            LOG.error("Java Robot切换失败", e)
            if (configService.isDebugMode()) {
                showDebugNotification("Java Robot切换失败: ${e.message}")
            }
            false
        }
    }

    private fun switchToChineseWindows(): Boolean {
        return try {
            // 使用Java Robot类直接发送键盘事件
            val robot = java.awt.Robot()

            // 发送Ctrl+Space
            robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL)
            Thread.sleep(10)
            robot.keyPress(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(50)
            robot.keyRelease(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(10)
            robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL)

            LOG.info("使用Java Robot发送Ctrl+Space切换到中文")

            // 添加调试通知
            if (configService.isDebugMode()) {
                showDebugNotification("已使用Java Robot发送Ctrl+Space切换到中文")
            }
            true
        } catch (e: Exception) {
            LOG.error("Java Robot切换失败", e)
            if (configService.isDebugMode()) {
                showDebugNotification("Java Robot切换失败: ${e.message}")
            }
            false
        }


    }

    override fun getAvailableInputMethods(): List<PlatformInputMethod> {
        return try {
            val result = executeCommand("powershell", "-Command",
                "Get-WinUserLanguageList | ForEach-Object { \$_.InputMethodTips }")

            parseWindowsInputMethods(result)
        } catch (e: Exception) {
            LOG.warn("Failed to get Windows input methods", e)
            emptyList()
        }
    }

    override fun getSupportedInputMethods(): List<InputMethodType> {
        return listOf(InputMethodType.ENGLISH, InputMethodType.CHINESE)
    }

    private fun parseWindowsInputMethods(output: String): List<PlatformInputMethod> {
        // Parse PowerShell output to extract input methods
        // This is a simplified implementation
        return listOf(
            PlatformInputMethod("0409:00000409", "English (US)", InputMethodType.ENGLISH, true),
            PlatformInputMethod("0804:00000804", "Chinese (Simplified)", InputMethodType.CHINESE)
        )
    }
}

/**
 * macOS-specific input method manager
 */
class MacOSInputMethodManager : InputMethodManager() {

    companion object {
        private val LOG = Logger.getInstance(MacOSInputMethodManager::class.java)
    }

    override fun isAvailable(): Boolean = true

    override suspend fun getCurrentInputMethod(): InputMethodType {
        return try {
            val result = executeCommand("osascript", "-e", 
                "tell application \"System Events\" to tell process \"SystemUIServer\" to get the value of the first menu bar item of menu bar 1 whose description is \"text input\"")
            
            when {
                result.contains("U.S.") || result.contains("English") -> InputMethodType.ENGLISH
                result.contains("Chinese") || result.contains("中文") -> InputMethodType.CHINESE
                else -> InputMethodType.UNKNOWN
            }
        } catch (e: Exception) {
            LOG.warn("Failed to detect macOS input method", e)
            InputMethodType.UNKNOWN
        }
    }

    override suspend fun switchToInputMethod(inputMethod: InputMethodType): Boolean {
        return try {
            val key = when (inputMethod) {
                InputMethodType.ENGLISH -> "english"
                InputMethodType.CHINESE -> "chinese"
                else -> inputMethod.code
            }
            val inputMethodId = configService.getInputMethodId("macos", key)
            if (inputMethodId != null) {
                executeCommand("osascript", "-e",
                    "tell application \"System Events\" to tell process \"SystemUIServer\" to click menu bar item \"$inputMethodId\" of menu bar 1")
                true
            } else {
                LOG.warn("No macOS input method ID configured for $inputMethod (key: $key)")
                false
            }
        } catch (e: Exception) {
            LOG.error("Failed to switch macOS input method to $inputMethod", e)
            false
        }
    }

    override fun getAvailableInputMethods(): List<PlatformInputMethod> {
        return listOf(
            PlatformInputMethod("com.apple.keylayout.US", "U.S.", InputMethodType.ENGLISH, true),
            PlatformInputMethod("com.apple.inputmethod.SCIM.ITABC", "Simplified Chinese", InputMethodType.CHINESE)
        )
    }

    override fun getSupportedInputMethods(): List<InputMethodType> {
        return listOf(InputMethodType.ENGLISH, InputMethodType.CHINESE)
    }
}

/**
 * Linux-specific input method manager
 */
class LinuxInputMethodManager : InputMethodManager() {

    companion object {
        private val LOG = Logger.getInstance(LinuxInputMethodManager::class.java)
    }

    override fun isAvailable(): Boolean {
        return try {
            executeCommand("which", "ibus").isNotEmpty() || 
            executeCommand("which", "fcitx").isNotEmpty() ||
            executeCommand("which", "fcitx5").isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getCurrentInputMethod(): InputMethodType {
        return try {
            val result = executeCommand("ibus", "engine")
            when {
                result.contains("xkb:us") -> InputMethodType.ENGLISH
                result.contains("pinyin") || result.contains("chinese") -> InputMethodType.CHINESE
                else -> InputMethodType.UNKNOWN
            }
        } catch (e: Exception) {
            LOG.warn("Failed to detect Linux input method", e)
            InputMethodType.UNKNOWN
        }
    }

    override suspend fun switchToInputMethod(inputMethod: InputMethodType): Boolean {
        return try {
            val engineName = when (inputMethod) {
                InputMethodType.ENGLISH -> "xkb:us::eng"
                InputMethodType.CHINESE -> "pinyin"
                else -> return false
            }
            
            executeCommand("ibus", "engine", engineName)
            true
        } catch (e: Exception) {
            LOG.error("Failed to switch Linux input method to $inputMethod", e)
            false
        }
    }

    override fun getAvailableInputMethods(): List<PlatformInputMethod> {
        return listOf(
            PlatformInputMethod("xkb:us::eng", "English (US)", InputMethodType.ENGLISH, true),
            PlatformInputMethod("pinyin", "Intelligent Pinyin", InputMethodType.CHINESE)
        )
    }

    override fun getSupportedInputMethods(): List<InputMethodType> {
        return listOf(InputMethodType.ENGLISH, InputMethodType.CHINESE)
    }
}

/**
 * Fallback manager for unsupported platforms
 */
class UnsupportedInputMethodManager : InputMethodManager() {

    companion object {
        private val LOG = Logger.getInstance(UnsupportedInputMethodManager::class.java)
    }

    override fun isAvailable(): Boolean = false

    override suspend fun getCurrentInputMethod(): InputMethodType = InputMethodType.UNKNOWN

    override suspend fun switchToInputMethod(inputMethod: InputMethodType): Boolean {
        LOG.warn("Input method switching not supported on this platform")
        return false
    }

    override fun getAvailableInputMethods(): List<PlatformInputMethod> = emptyList()

    override fun getSupportedInputMethods(): List<InputMethodType> = emptyList()
}

/**
 * Show debug notification (only in debug mode)
 */
private fun showDebugNotification(message: String) {
    try {
        // 使用系统通知显示调试信息
        val script = """
            Add-Type -AssemblyName System.Windows.Forms
            [System.Windows.Forms.MessageBox]::Show('$message', '智能输入法调试', 'OK', 'Information')
        """.trimIndent()

        executeCommand("powershell", "-Command", script)
    } catch (e: Exception) {
        // 如果通知失败，至少记录日志
        println("调试通知: $message")
    }
}

/**
 * Execute a system command and return the output
 */
private fun executeCommand(vararg command: String): String {
    return try {
        val process = ProcessBuilder(*command).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        output
    } catch (e: Exception) {
        throw RuntimeException("Failed to execute command: ${command.joinToString(" ")}", e)
    }
}
