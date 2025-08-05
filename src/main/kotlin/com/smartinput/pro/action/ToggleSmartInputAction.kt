package com.smartinput.pro.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.smartinput.pro.service.SmartInputConfigService

/**
 * Action to toggle Smart Input Pro on/off
 */
class ToggleSmartInputAction : AnAction(), DumbAware {
    
    companion object {
        private val LOG = Logger.getInstance(ToggleSmartInputAction::class.java)
    }

    private val configService = SmartInputConfigService.getInstance()

    override fun actionPerformed(e: AnActionEvent) {
        try {
            val wasEnabled = configService.isEnabled()
            configService.setEnabled(!wasEnabled)
            
            val newState = if (configService.isEnabled()) "enabled" else "disabled"
            val message = "Smart Input Pro has been $newState"
            
            LOG.info(message)
            
            // Show notification to user
            Messages.showInfoMessage(
                e.project,
                message,
                "Smart Input Pro"
            )
            
        } catch (ex: Exception) {
            LOG.error("Error toggling Smart Input Pro", ex)
            Messages.showErrorDialog(
                e.project,
                "Failed to toggle Smart Input Pro: ${ex.message}",
                "Smart Input Pro Error"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        
        // Update action text based on current state
        if (configService.isEnabled()) {
            presentation.text = "Disable Smart Input Pro"
            presentation.description = "Disable automatic input method switching"
        } else {
            presentation.text = "Enable Smart Input Pro"
            presentation.description = "Enable automatic input method switching"
        }
        
        // Action is always available
        presentation.isEnabled = true
        presentation.isVisible = true
    }
}
