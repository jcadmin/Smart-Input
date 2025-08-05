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
    
    override fun isAvailable(): Boolean = true

    override suspend fun getCurrentInputMethod(): InputMethodType {
        return try {
            val result = executeCommand("powershell", "-Command", 
                "Get-WinUserLanguageList | Select-Object -First 1 | ForEach-Object { $_.InputMethodTips }")
            
            when {
                result.contains("0409") -> InputMethodType.ENGLISH
                result.contains("0804") -> InputMethodType.CHINESE
                else -> InputMethodType.UNKNOWN
            }
        } catch (e: Exception) {
            LOG.warn("Failed to detect Windows input method", e)
            InputMethodType.UNKNOWN
        }
    }

    override suspend fun switchToInputMethod(inputMethod: InputMethodType): Boolean {
        return try {
            val inputMethodId = configService.getInputMethodId("windows", inputMethod.code)
            if (inputMethodId != null) {
                executeCommand("powershell", "-Command", 
                    "Set-WinUserLanguageList -LanguageList $inputMethodId -Force")
                true
            } else {
                LOG.warn("No Windows input method ID configured for $inputMethod")
                false
            }
        } catch (e: Exception) {
            LOG.error("Failed to switch Windows input method to $inputMethod", e)
            false
        }
    }

    override fun getAvailableInputMethods(): List<PlatformInputMethod> {
        return try {
            val result = executeCommand("powershell", "-Command", 
                "Get-WinUserLanguageList | ForEach-Object { $_.InputMethodTips }")
            
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
            val inputMethodId = configService.getInputMethodId("macos", inputMethod.code)
            if (inputMethodId != null) {
                executeCommand("osascript", "-e", 
                    "tell application \"System Events\" to tell process \"SystemUIServer\" to click menu bar item \"$inputMethodId\" of menu bar 1")
                true
            } else {
                LOG.warn("No macOS input method ID configured for $inputMethod")
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
