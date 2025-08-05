package com.smartinput.pro.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.smartinput.pro.platform.InputMethodManager
import com.smartinput.pro.platform.PlatformDetector
import com.smartinput.pro.model.InputMethodType
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 输入法管理服务
 * 负责处理输入法的实际切换和状态跟踪，
 * 在不同上下文中维护当前状态
 */
@Service(Service.Level.PROJECT)
class InputMethodService(private val project: Project) {
    
    companion object {
        private val LOG = Logger.getInstance(InputMethodService::class.java)
        
        fun getInstance(project: Project): InputMethodService {
            return project.getService(InputMethodService::class.java)
        }
    }

    private val configService = SmartInputConfigService.getInstance()
    private val platformDetector = PlatformDetector()
    private val inputMethodManager = InputMethodManager.create(platformDetector.getCurrentPlatform())
    
    // 异步操作的协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 状态跟踪
    private var currentInputMethod: InputMethodType = InputMethodType.UNKNOWN
    private var lastSwitchTime: Long = 0
    private val switchHistory = ConcurrentHashMap<String, InputMethodType>()

    // 输入法变化监听器
    private val listeners = mutableListOf<InputMethodChangeListener>()

    init {
        // 初始化当前输入法状态
        detectCurrentInputMethod()
    }

    /**
     * 输入法变化监听器接口
     */
    interface InputMethodChangeListener {
        fun onInputMethodChanged(oldMethod: InputMethodType, newMethod: InputMethodType)
        fun onSwitchFailed(targetMethod: InputMethodType, error: Throwable)
    }

    /**
     * 添加输入法变化监听器
     */
    fun addListener(listener: InputMethodChangeListener) {
        listeners.add(listener)
    }

    /**
     * 移除输入法变化监听器
     */
    fun removeListener(listener: InputMethodChangeListener) {
        listeners.remove(listener)
    }

    /**
     * Get the current input method type
     */
    fun getCurrentInputMethod(): InputMethodType = currentInputMethod

    /**
     * Switch to the specified input method
     */
    fun switchToInputMethod(targetMethod: InputMethodType, context: String = "unknown") {
        if (!configService.isEnabled()) {
            LOG.debug("Smart Input Pro is disabled, skipping switch")
            return
        }

        if (targetMethod == InputMethodType.UNKNOWN || targetMethod == currentInputMethod) {
            LOG.debug("No switch needed: target=$targetMethod, current=$currentInputMethod")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastSwitchTime < configService.getSwitchDelay()) {
            LOG.debug("Switch too frequent, skipping")
            return
        }

        serviceScope.launch {
            try {
                val success = performSwitch(targetMethod, context)
                if (success) {
                    val oldMethod = currentInputMethod
                    currentInputMethod = targetMethod
                    lastSwitchTime = now
                    switchHistory[context] = targetMethod
                    
                    LOG.info("Successfully switched from $oldMethod to $targetMethod in context: $context")
                    
                    // Notify listeners
                    listeners.forEach { listener ->
                        try {
                            listener.onInputMethodChanged(oldMethod, targetMethod)
                        } catch (e: Exception) {
                            LOG.warn("Error notifying listener", e)
                        }
                    }
                } else {
                    LOG.warn("Failed to switch to $targetMethod")
                }
            } catch (e: Exception) {
                LOG.error("Error switching input method", e)
                listeners.forEach { listener ->
                    try {
                        listener.onSwitchFailed(targetMethod, e)
                    } catch (ex: Exception) {
                        LOG.warn("Error notifying listener of failure", ex)
                    }
                }
            }
        }
    }

    /**
     * Switch to English input method
     */
    fun switchToEnglish(context: String = "code") {
        switchToInputMethod(InputMethodType.ENGLISH, context)
    }

    /**
     * Switch to Chinese input method
     */
    fun switchToChinese(context: String = "comment") {
        switchToInputMethod(InputMethodType.CHINESE, context)
    }

    /**
     * Get the preferred input method for a given context
     */
    fun getPreferredInputMethod(context: String): InputMethodType {
        return when (context.lowercase()) {
            "code", "variable", "method", "class" -> {
                if (configService.shouldSwitchInCodeAreas()) {
                    when (configService.getCodeAreaInputMethod()) {
                        "english" -> InputMethodType.ENGLISH
                        "chinese" -> InputMethodType.CHINESE
                        else -> InputMethodType.ENGLISH
                    }
                } else {
                    currentInputMethod
                }
            }
            "comment", "line_comment", "block_comment" -> {
                if (configService.shouldSwitchInComments()) {
                    when (configService.getCommentInputMethod()) {
                        "english" -> InputMethodType.ENGLISH
                        "chinese" -> InputMethodType.CHINESE
                        else -> InputMethodType.CHINESE
                    }
                } else {
                    currentInputMethod
                }
            }
            "string", "string_literal" -> {
                if (configService.shouldSwitchInStrings()) {
                    when (configService.getStringInputMethod()) {
                        "english" -> InputMethodType.ENGLISH
                        "chinese" -> InputMethodType.CHINESE
                        "auto" -> detectStringInputMethod()
                        else -> currentInputMethod
                    }
                } else {
                    currentInputMethod
                }
            }
            "documentation", "javadoc", "kdoc" -> {
                if (configService.shouldSwitchInDocumentation()) {
                    when (configService.getDocumentationInputMethod()) {
                        "english" -> InputMethodType.ENGLISH
                        "chinese" -> InputMethodType.CHINESE
                        else -> InputMethodType.CHINESE
                    }
                } else {
                    currentInputMethod
                }
            }
            else -> currentInputMethod
        }
    }

    /**
     * Detect current input method from system
     */
    private fun detectCurrentInputMethod() {
        serviceScope.launch {
            try {
                currentInputMethod = inputMethodManager.getCurrentInputMethod()
                LOG.debug("Detected current input method: $currentInputMethod")
            } catch (e: Exception) {
                LOG.warn("Failed to detect current input method", e)
                currentInputMethod = InputMethodType.UNKNOWN
            }
        }
    }

    /**
     * Perform the actual input method switch
     */
    private suspend fun performSwitch(targetMethod: InputMethodType, context: String = "unknown"): Boolean {
        return try {
            // 如果是Windows平台，传递上下文信息
            if (inputMethodManager is com.smartinput.pro.platform.WindowsInputMethodManager) {
                inputMethodManager.switchToInputMethod(targetMethod, context)
            } else {
                inputMethodManager.switchToInputMethod(targetMethod)
            }
        } catch (e: Exception) {
            LOG.error("Failed to perform input method switch", e)
            false
        }
    }

    /**
     * Auto-detect appropriate input method for string content
     */
    private fun detectStringInputMethod(): InputMethodType {
        // This could be enhanced with more sophisticated detection
        // For now, default to current method
        return currentInputMethod
    }

    /**
     * Get switch history for debugging
     */
    fun getSwitchHistory(): Map<String, InputMethodType> = switchHistory.toMap()

    /**
     * Clear switch history
     */
    fun clearHistory() {
        switchHistory.clear()
    }

    /**
     * Check if the service is available on current platform
     */
    fun isAvailable(): Boolean = inputMethodManager.isAvailable()

    /**
     * Get supported input methods on current platform
     */
    fun getSupportedInputMethods(): List<InputMethodType> = inputMethodManager.getSupportedInputMethods()

    /**
     * Cleanup resources when service is disposed
     */
    fun dispose() {
        serviceScope.cancel()
        listeners.clear()
        switchHistory.clear()
    }
}
