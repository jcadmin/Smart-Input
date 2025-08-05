package com.smartinput.pro.model

/**
 * Enumeration of supported input method types
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
 * Represents the context where input method switching occurs
 */
enum class ContextType(val displayName: String, val description: String) {
    CODE("Code", "Variable names, method names, class names"),
    COMMENT("Comment", "Line comments, block comments"),
    STRING("String", "String literals and content"),
    DOCUMENTATION("Documentation", "JavaDoc, KDoc, and other documentation"),
    UNKNOWN("Unknown", "Unrecognized context");

    companion object {
        fun fromString(str: String): ContextType {
            return values().find { it.name.equals(str, ignoreCase = true) } ?: UNKNOWN
        }
    }

    override fun toString(): String = displayName
}

/**
 * Represents a context detection result
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
