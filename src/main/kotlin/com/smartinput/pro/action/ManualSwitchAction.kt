package com.smartinput.pro.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.smartinput.pro.service.SmartInputConfigService
import com.smartinput.pro.service.InputMethodService
import com.smartinput.pro.model.InputMethodType

/**
 * Action to manually switch input method
 */
class ManualSwitchAction : AnAction(), DumbAware {
    
    companion object {
        private val LOG = Logger.getInstance(ManualSwitchAction::class.java)
    }

    private val configService = SmartInputConfigService.getInstance()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            val inputMethodService = InputMethodService.getInstance(project)
            val currentMethod = inputMethodService.getCurrentInputMethod()
            
            // Toggle between English and Chinese
            val targetMethod = when (currentMethod) {
                InputMethodType.ENGLISH -> InputMethodType.CHINESE
                InputMethodType.CHINESE -> InputMethodType.ENGLISH
                InputMethodType.UNKNOWN -> InputMethodType.ENGLISH
            }
            
            inputMethodService.switchToInputMethod(targetMethod, "manual")
            
            val message = "Switched to ${targetMethod.displayName} input method"
            LOG.info(message)
            
            // Show brief notification
            if (configService.isDebugMode()) {
                Messages.showInfoMessage(project, message, "Smart Input Pro")
            }
            
        } catch (ex: Exception) {
            LOG.error("Error manually switching input method", ex)
            Messages.showErrorDialog(
                e.project,
                "Failed to switch input method: ${ex.message}",
                "Smart Input Pro Error"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        
        // Only enable when we have a project and editor
        val isAvailable = project != null && editor != null && configService.isEnabled()
        
        presentation.isEnabled = isAvailable
        presentation.isVisible = true
        
        if (isAvailable) {
            try {
                val inputMethodService = InputMethodService.getInstance(project!!)
                val currentMethod = inputMethodService.getCurrentInputMethod()
                val targetMethod = when (currentMethod) {
                    InputMethodType.ENGLISH -> InputMethodType.CHINESE
                    InputMethodType.CHINESE -> InputMethodType.ENGLISH
                    InputMethodType.UNKNOWN -> InputMethodType.ENGLISH
                }
                
                presentation.text = "Switch to ${targetMethod.displayName}"
                presentation.description = "Manually switch to ${targetMethod.displayName} input method"
            } catch (ex: Exception) {
                presentation.text = "Switch Input Method"
                presentation.description = "Manually switch input method"
            }
        } else {
            presentation.text = "Switch Input Method"
            presentation.description = "Manually switch input method (requires Smart Input Pro to be enabled)"
        }
    }
}
