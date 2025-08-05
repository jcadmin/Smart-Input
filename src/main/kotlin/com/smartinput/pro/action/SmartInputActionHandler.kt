package com.smartinput.pro.action

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.smartinput.pro.service.SmartInputConfigService

/**
 * Custom action handler for Smart Input Pro
 * Handles special key combinations and editor actions
 */
class SmartInputActionHandler : EditorActionHandler() {
    
    companion object {
        private val LOG = Logger.getInstance(SmartInputActionHandler::class.java)
    }

    private val configService = SmartInputConfigService.getInstance()

    override fun doExecute(editor: Editor, caret: com.intellij.openapi.editor.Caret?, dataContext: DataContext?) {
        if (!configService.isEnabled()) {
            return
        }

        try {
            // Handle escape key - could be used to hide indicator or reset state
            handleEscapeAction(editor)
            
        } catch (e: Exception) {
            LOG.error("Error in Smart Input action handler", e)
        }
    }

    override fun isEnabledForCaret(editor: Editor, caret: com.intellij.openapi.editor.Caret, dataContext: DataContext?): Boolean {
        return configService.isEnabled()
    }

    /**
     * Handle escape key action
     */
    private fun handleEscapeAction(editor: Editor) {
        // This could be used to hide the input method indicator
        // or perform other Smart Input Pro specific actions
        
        if (configService.isDebugMode()) {
            LOG.debug("Escape action handled by Smart Input Pro")
        }
    }
}
