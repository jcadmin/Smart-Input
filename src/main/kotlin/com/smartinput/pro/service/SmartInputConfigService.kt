package com.smartinput.pro.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Service for managing Smart Input Pro configuration settings.
 * This service persists user preferences and provides access to configuration options.
 */
@Service(Service.Level.APP)
@State(
    name = "SmartInputProConfig",
    storages = [Storage("smartInputPro.xml")]
)
class SmartInputConfigService : PersistentStateComponent<SmartInputConfigService.State> {

    companion object {
        fun getInstance(): SmartInputConfigService {
            return ApplicationManager.getApplication().getService(SmartInputConfigService::class.java)
        }
    }

    /**
     * Configuration state class that holds all plugin settings
     */
    data class State(
        // General Settings
        var enabled: Boolean = true,
        var showIndicator: Boolean = true,
        var autoSwitchMode: Boolean = true,
        
        // Context Rules
        var switchInCodeAreas: Boolean = true,
        var switchInComments: Boolean = false,
        var switchInStrings: Boolean = false,
        var switchInDocumentation: Boolean = false,
        
        // Input Method Preferences
        var codeAreaInputMethod: String = "english",
        var commentInputMethod: String = "chinese",
        var stringInputMethod: String = "auto",
        var documentationInputMethod: String = "chinese",
        
        // Appearance Settings
        var indicatorPosition: String = "cursor", // cursor, top-right, bottom-right
        var indicatorSize: Int = 12,
        var indicatorOpacity: Float = 0.8f,
        var indicatorTimeout: Int = 2000,
        
        // Advanced Settings
        var detectionDelay: Int = 100,
        var switchDelay: Int = 50,
        var debugMode: Boolean = false,
        
        // Platform-specific settings
        var windowsInputMethods: MutableMap<String, String> = mutableMapOf(
            "english" to "0409:00000409", // US English
            "chinese" to "0804:00000804"  // Chinese Simplified
        ),
        var macosInputMethods: MutableMap<String, String> = mutableMapOf(
            "english" to "com.apple.keylayout.US",
            "chinese" to "com.apple.inputmethod.SCIM.ITABC"
        ),
        var linuxInputMethods: MutableMap<String, String> = mutableMapOf(
            "english" to "en",
            "chinese" to "zh"
        )
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    // Convenience methods for accessing configuration
    fun isEnabled(): Boolean = myState.enabled
    fun setEnabled(enabled: Boolean) {
        myState.enabled = enabled
    }

    fun isShowIndicator(): Boolean = myState.showIndicator
    fun setShowIndicator(show: Boolean) {
        myState.showIndicator = show
    }

    fun isAutoSwitchMode(): Boolean = myState.autoSwitchMode
    fun setAutoSwitchMode(auto: Boolean) {
        myState.autoSwitchMode = auto
    }

    fun shouldSwitchInCodeAreas(): Boolean = myState.switchInCodeAreas
    fun shouldSwitchInComments(): Boolean = myState.switchInComments
    fun shouldSwitchInStrings(): Boolean = myState.switchInStrings
    fun shouldSwitchInDocumentation(): Boolean = myState.switchInDocumentation

    fun getCodeAreaInputMethod(): String = myState.codeAreaInputMethod
    fun getCommentInputMethod(): String = myState.commentInputMethod
    fun getStringInputMethod(): String = myState.stringInputMethod
    fun getDocumentationInputMethod(): String = myState.documentationInputMethod

    fun getIndicatorPosition(): String = myState.indicatorPosition
    fun getIndicatorSize(): Int = myState.indicatorSize
    fun getIndicatorOpacity(): Float = myState.indicatorOpacity
    fun getIndicatorTimeout(): Int = myState.indicatorTimeout

    fun getDetectionDelay(): Int = myState.detectionDelay
    fun getSwitchDelay(): Int = myState.switchDelay
    fun isDebugMode(): Boolean = myState.debugMode

    fun getInputMethodId(platform: String, type: String): String? {
        return when (platform.lowercase()) {
            "windows" -> myState.windowsInputMethods[type]
            "macos", "mac" -> myState.macosInputMethods[type]
            "linux" -> myState.linuxInputMethods[type]
            else -> null
        }
    }

    /**
     * Reset all settings to default values
     */
    fun resetToDefaults() {
        myState = State()
    }

    /**
     * Validate configuration and fix any invalid values
     */
    fun validateAndFix() {
        // Ensure delays are within reasonable bounds
        if (myState.detectionDelay < 0) myState.detectionDelay = 0
        if (myState.detectionDelay > 5000) myState.detectionDelay = 5000
        
        if (myState.switchDelay < 0) myState.switchDelay = 0
        if (myState.switchDelay > 1000) myState.switchDelay = 1000
        
        // Ensure opacity is within valid range
        if (myState.indicatorOpacity < 0.1f) myState.indicatorOpacity = 0.1f
        if (myState.indicatorOpacity > 1.0f) myState.indicatorOpacity = 1.0f
        
        // Ensure timeout is reasonable
        if (myState.indicatorTimeout < 500) myState.indicatorTimeout = 500
        if (myState.indicatorTimeout > 10000) myState.indicatorTimeout = 10000
    }
}
