package com.smartinput.pro.model

/**
 * 支持的输入法类型枚举
 */
enum class InputMethodType(val displayName: String, val code: String) {
    ENGLISH("English", "en"),
    CHINESE("Chinese", "zh"),
    UNKNOWN("Unknown", "unknown");

    companion object {
        fun fromCode(code: String): InputMethodType {
            return values().find { it.code == code } ?: UNKNOWN
        }

        fun fromDisplayName(displayName: String): InputMethodType {
            return values().find { it.displayName.equals(displayName, ignoreCase = true) } ?: UNKNOWN
        }
    }

    override fun toString(): String = displayName
}

/**
 * 输入法切换发生的上下文类型
 */
enum class ContextType(val displayName: String, val description: String) {
    CODE("代码", "变量名、方法名、类名"),
    COMMENT("注释", "行注释、块注释"),
    STRING("字符串", "字符串字面量和内容"),
    DOCUMENTATION("文档", "JavaDoc、KDoc和其他文档"),
    UNKNOWN("未知", "无法识别的上下文");

    companion object {
        fun fromString(str: String): ContextType {
            return values().find { it.name.equals(str, ignoreCase = true) } ?: UNKNOWN
        }
    }

    override fun toString(): String = displayName
}

/**
 * 上下文检测结果
 */
data class ContextInfo(
    val type: ContextType,
    val confidence: Float = 1.0f,
    val details: String = "",
    val suggestedInputMethod: InputMethodType = InputMethodType.UNKNOWN
) {
    fun isHighConfidence(): Boolean = confidence >= 0.8f
    fun isMediumConfidence(): Boolean = confidence >= 0.5f && confidence < 0.8f
    fun isLowConfidence(): Boolean = confidence < 0.5f
}

/**
 * Platform-specific input method information
 */
data class PlatformInputMethod(
    val id: String,
    val name: String,
    val type: InputMethodType,
    val isDefault: Boolean = false,
    val isAvailable: Boolean = true
)

/**
 * Input method switch result
 */
sealed class SwitchResult {
    object Success : SwitchResult()
    data class Failed(val reason: String, val exception: Throwable? = null) : SwitchResult()
    object NotNeeded : SwitchResult()
    object Disabled : SwitchResult()
}

/**
 * Configuration for input method switching behavior
 */
data class SwitchConfig(
    val enabled: Boolean = true,
    val autoSwitch: Boolean = true,
    val showIndicator: Boolean = true,
    val contextRules: Map<ContextType, InputMethodType> = mapOf(
        ContextType.CODE to InputMethodType.ENGLISH,
        ContextType.COMMENT to InputMethodType.CHINESE,
        ContextType.STRING to InputMethodType.CHINESE,
        ContextType.DOCUMENTATION to InputMethodType.CHINESE
    ),
    val delayMs: Int = 100,
    val debugMode: Boolean = false
)
