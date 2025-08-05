package com.smartinput.pro.platform

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo

/**
 * Detects the current operating system platform and provides platform-specific information
 */
class PlatformDetector {
    
    companion object {
        private val LOG = Logger.getInstance(PlatformDetector::class.java)
    }

    enum class Platform(val displayName: String) {
        WINDOWS("Windows"),
        MACOS("macOS"),
        LINUX("Linux"),
        UNKNOWN("Unknown")
    }

    /**
     * Get the current platform
     */
    fun getCurrentPlatform(): Platform {
        return when {
            SystemInfo.isWindows -> Platform.WINDOWS
            SystemInfo.isMac -> Platform.MACOS
            SystemInfo.isLinux -> Platform.LINUX
            else -> {
                LOG.warn("Unknown platform detected: ${SystemInfo.OS_NAME}")
                Platform.UNKNOWN
            }
        }
    }

    /**
     * Get platform-specific information
     */
    fun getPlatformInfo(): PlatformInfo {
        val platform = getCurrentPlatform()
        return PlatformInfo(
            platform = platform,
            osName = SystemInfo.OS_NAME,
            osVersion = SystemInfo.OS_VERSION,
            osArch = SystemInfo.OS_ARCH,
            javaVersion = SystemInfo.JAVA_VERSION,
            isSupported = isSupportedPlatform(platform)
        )
    }

    /**
     * Check if the current platform is supported for input method switching
     */
    fun isSupportedPlatform(platform: Platform = getCurrentPlatform()): Boolean {
        return when (platform) {
            Platform.WINDOWS -> true
            Platform.MACOS -> true
            Platform.LINUX -> true
            Platform.UNKNOWN -> false
        }
    }

    /**
     * Get the command line tool available for input method switching on current platform
     */
    fun getInputMethodTool(): String? {
        return when (getCurrentPlatform()) {
            Platform.WINDOWS -> "powershell" // Will use PowerShell scripts
            Platform.MACOS -> "osascript" // AppleScript
            Platform.LINUX -> detectLinuxInputMethodTool()
            Platform.UNKNOWN -> null
        }
    }

    /**
     * Detect available input method tools on Linux
     */
    private fun detectLinuxInputMethodTool(): String? {
        val tools = listOf("ibus", "fcitx", "fcitx5", "scim")
        
        for (tool in tools) {
            if (isCommandAvailable(tool)) {
                LOG.info("Detected Linux input method tool: $tool")
                return tool
            }
        }
        
        LOG.warn("No supported Linux input method tool found")
        return null
    }

    /**
     * Check if a command is available in the system PATH
     */
    private fun isCommandAvailable(command: String): Boolean {
        return try {
            val process = ProcessBuilder("which", command).start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get platform-specific configuration recommendations
     */
    fun getRecommendedConfig(): Map<String, Any> {
        return when (getCurrentPlatform()) {
            Platform.WINDOWS -> mapOf(
                "detectionDelay" to 150,
                "switchDelay" to 100,
                "useRegistry" to true,
                "fallbackMethod" to "sendkeys"
            )
            Platform.MACOS -> mapOf(
                "detectionDelay" to 100,
                "switchDelay" to 50,
                "useAppleScript" to true,
                "fallbackMethod" to "keyboard_shortcut"
            )
            Platform.LINUX -> mapOf(
                "detectionDelay" to 200,
                "switchDelay" to 150,
                "preferredTool" to getInputMethodTool(),
                "fallbackMethod" to "dbus"
            )
            Platform.UNKNOWN -> mapOf(
                "detectionDelay" to 500,
                "switchDelay" to 300,
                "enabled" to false
            )
        }
    }
}

/**
 * Data class containing platform information
 */
data class PlatformInfo(
    val platform: PlatformDetector.Platform,
    val osName: String,
    val osVersion: String,
    val osArch: String,
    val javaVersion: String,
    val isSupported: Boolean,
    val additionalInfo: Map<String, String> = emptyMap()
) {
    override fun toString(): String {
        return "Platform: ${platform.displayName}, OS: $osName $osVersion ($osArch), " +
                "Java: $javaVersion, Supported: $isSupported"
    }
}
