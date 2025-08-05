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
        // 由于QQ输入法状态检测极其困难，我们采用实用策略：
        // 总是返回UNKNOWN，让切换逻辑根据上下文执行
        // 但添加智能频率控制，避免过度切换
        return InputMethodType.UNKNOWN
    }

    /**
     * 检测当前输入法状态
     * 使用Windows IME API强制检测输入法的实际中英文状态
     */
    private fun detectCurrentInputMethodState(): InputMethodType {
        return try {
            // 方法1：使用IMM32 API检测输入法转换状态
            val imeResult = detectIMEConversionStatus()
            if (imeResult != InputMethodType.UNKNOWN) {
                LOG.debug("IME转换状态检测结果: $imeResult")
                return imeResult
            }

            // 方法2：检测输入法窗口状态
            val windowResult = detectInputMethodWindowStatus()
            if (windowResult != InputMethodType.UNKNOWN) {
                LOG.debug("窗口状态检测结果: $windowResult")
                return windowResult
            }

            // 方法3：检测当前活动的输入法
            val activeResult = detectActiveInputMethod()
            if (activeResult != InputMethodType.UNKNOWN) {
                LOG.debug("活动输入法检测结果: $activeResult")
                return activeResult
            }

            LOG.debug("所有检测方法都返回UNKNOWN")
            InputMethodType.UNKNOWN

        } catch (e: Exception) {
            LOG.warn("输入法状态检测异常", e)
            InputMethodType.UNKNOWN
        }
    }

    /**
     * 使用IMM32 API检测输入法转换状态
     */
    private fun detectIMEConversionStatus(): InputMethodType {
        return try {
            val script = """
                Add-Type -TypeDefinition @'
                using System;
                using System.Runtime.InteropServices;
                public class IMEDetector {
                    [DllImport("imm32.dll")]
                    public static extern IntPtr ImmGetDefaultIMEWnd(IntPtr hWnd);

                    [DllImport("imm32.dll")]
                    public static extern IntPtr ImmGetContext(IntPtr hWnd);

                    [DllImport("imm32.dll")]
                    public static extern bool ImmGetConversionStatus(IntPtr hIMC, out uint conversion, out uint sentence);

                    [DllImport("imm32.dll")]
                    public static extern bool ImmReleaseContext(IntPtr hWnd, IntPtr hIMC);

                    [DllImport("user32.dll")]
                    public static extern IntPtr GetForegroundWindow();

                    public const uint IME_CMODE_NATIVE = 0x0001;
                    public const uint IME_CMODE_FULLSHAPE = 0x0008;
                    public const uint IME_CMODE_NOCONVERSION = 0x0100;
                }
'@ -ErrorAction SilentlyContinue

                try {
                    ${'$'}hwnd = [IMEDetector]::GetForegroundWindow()
                    if (${'$'}hwnd -eq [IntPtr]::Zero) {
                        Write-Output "Status: NO_WINDOW"
                        return
                    }

                    ${'$'}imeWnd = [IMEDetector]::ImmGetDefaultIMEWnd(${'$'}hwnd)
                    if (${'$'}imeWnd -eq [IntPtr]::Zero) {
                        Write-Output "Status: NO_IME"
                        return
                    }

                    ${'$'}hIMC = [IMEDetector]::ImmGetContext(${'$'}hwnd)
                    if (${'$'}hIMC -eq [IntPtr]::Zero) {
                        Write-Output "Status: NO_CONTEXT"
                        return
                    }

                    ${'$'}conversion = 0
                    ${'$'}sentence = 0
                    ${'$'}result = [IMEDetector]::ImmGetConversionStatus(${'$'}hIMC, [ref]${'$'}conversion, [ref]${'$'}sentence)

                    if (${'$'}result) {
                        Write-Output "ConversionMode: ${'$'}conversion"
                        Write-Output "SentenceMode: ${'$'}sentence"

                        # 检查是否为中文模式 (NATIVE模式表示中文输入)
                        if ((${'$'}conversion -band [IMEDetector]::IME_CMODE_NATIVE) -ne 0) {
                            Write-Output "Status: CHINESE"
                        } else {
                            Write-Output "Status: ENGLISH"
                        }
                    } else {
                        Write-Output "Status: UNKNOWN"
                    }

                    [IMEDetector]::ImmReleaseContext(${'$'}hwnd, ${'$'}hIMC)

                } catch {
                    Write-Output "Status: ERROR"
                    Write-Output "Error: $(${'$'}_.Exception.Message)"
                }
            """.trimIndent()

            val result = executeCommand("powershell", "-Command", script)
            LOG.debug("IME转换状态检测结果: $result")

            when {
                result.contains("Status: CHINESE") -> InputMethodType.CHINESE
                result.contains("Status: ENGLISH") -> InputMethodType.ENGLISH
                else -> {
                    LOG.debug("IME状态未知: $result")
                    InputMethodType.UNKNOWN
                }
            }
        } catch (e: Exception) {
            LOG.debug("IME转换状态检测失败", e)
            InputMethodType.UNKNOWN
        }
    }

    /**
     * 检测输入法窗口状态
     */
    private fun detectInputMethodWindowStatus(): InputMethodType {
        return try {
            // 这个方法可以检测输入法相关窗口的状态
            // 但对于QQ输入法来说，窗口状态检测也比较困难
            InputMethodType.UNKNOWN
        } catch (e: Exception) {
            LOG.debug("输入法窗口状态检测失败", e)
            InputMethodType.UNKNOWN
        }
    }

    /**
     * 检测当前活动的输入法
     */
    private fun detectActiveInputMethod(): InputMethodType {
        return try {
            // 这个方法可以检测当前活动的输入法
            // 但对于第三方输入法的内部状态仍然难以准确检测
            InputMethodType.UNKNOWN
        } catch (e: Exception) {
            LOG.debug("活动输入法检测失败", e)
            InputMethodType.UNKNOWN
        }
    }

    /**
     * 从注册表检测输入法状态
     */
    private fun detectInputMethodFromRegistry(): InputMethodType {
        return try {
            val script = """
                ${'$'}currentIM = Get-ItemProperty -Path "HKCU:\Control Panel\International" -Name "Locale" -ErrorAction SilentlyContinue
                if (${'$'}currentIM) {
                    Write-Output ${'$'}currentIM.Locale
                } else {
                    Write-Output "unknown"
                }
            """.trimIndent()

            val result = executeCommand("powershell", "-Command", script)
            when {
                result.contains("0409") -> InputMethodType.ENGLISH
                result.contains("0804") -> InputMethodType.CHINESE
                else -> InputMethodType.UNKNOWN
            }
        } catch (e: Exception) {
            LOG.debug("注册表检测失败", e)
            InputMethodType.UNKNOWN
        }
    }

    override suspend fun switchToInputMethod(inputMethod: InputMethodType): Boolean {
        return switchToInputMethod(inputMethod, "unknown")
    }

    suspend fun switchToInputMethod(inputMethod: InputMethodType, context: String): Boolean {
        return try {
            when (inputMethod) {
                InputMethodType.ENGLISH -> switchToEnglishWindows(context)
                InputMethodType.CHINESE -> switchToChineseWindows(context)
                else -> false
            }
        } catch (e: Exception) {
            LOG.error("Failed to switch Windows input method to $inputMethod", e)
            false
        }
    }

    // 智能切换控制
    private var lastSwitchTime: Long = 0
    private var lastSwitchTarget: String = ""
    private var lastSwitchContext: String = ""

    private fun switchToEnglishWindows(): Boolean {
        return switchToEnglishWindows("unknown")
    }

    private fun switchToEnglishWindows(context: String): Boolean {
        return try {
            // 改进的频率控制：只有相同目标和相同上下文才跳过
            val now = System.currentTimeMillis()
            if (lastSwitchTarget == "ENGLISH" && lastSwitchContext == context && (now - lastSwitchTime) < 1000) {
                LOG.info("跳过频繁的英文切换请求 (上下文: $context)")
                return true
            }

            // 使用Java Robot类直接发送键盘事件
            val robot = java.awt.Robot()

            // 发送Ctrl+Space进行切换
            robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL)
            Thread.sleep(10)
            robot.keyPress(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(50)
            robot.keyRelease(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(10)
            robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL)

            // 记录切换
            lastSwitchTime = now
            lastSwitchTarget = "ENGLISH"
            lastSwitchContext = context

            LOG.info("执行输入法切换（目标：英文，上下文：$context）")

            // 添加调试通知
            if (configService.isDebugMode()) {
                showDebugNotification("已执行输入法切换（目标：英文）")
            }
            true
        } catch (e: Exception) {
            LOG.error("输入法切换失败", e)
            if (configService.isDebugMode()) {
                showDebugNotification("输入法切换失败: ${e.message}")
            }
            false
        }
    }

    private fun switchToChineseWindows(): Boolean {
        return switchToChineseWindows("unknown")
    }

    private fun switchToChineseWindows(context: String): Boolean {
        return try {
            // 改进的频率控制：只有相同目标和相同上下文才跳过
            val now = System.currentTimeMillis()
            if (lastSwitchTarget == "CHINESE" && lastSwitchContext == context && (now - lastSwitchTime) < 1000) {
                LOG.info("跳过频繁的中文切换请求 (上下文: $context)")
                return true
            }

            // 使用Java Robot类直接发送键盘事件
            val robot = java.awt.Robot()

            // 发送Ctrl+Space进行切换
            robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL)
            Thread.sleep(10)
            robot.keyPress(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(50)
            robot.keyRelease(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(10)
            robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL)

            // 记录切换
            lastSwitchTime = now
            lastSwitchTarget = "CHINESE"
            lastSwitchContext = context

            LOG.info("执行输入法切换（目标：中文，上下文：$context）")

            // 添加调试通知
            if (configService.isDebugMode()) {
                showDebugNotification("已执行输入法切换（目标：中文）")
            }
            true
        } catch (e: Exception) {
            LOG.error("输入法切换失败", e)
            if (configService.isDebugMode()) {
                showDebugNotification("输入法切换失败: ${e.message}")
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
