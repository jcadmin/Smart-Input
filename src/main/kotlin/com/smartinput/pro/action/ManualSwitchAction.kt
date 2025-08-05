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
 * 手动切换输入法的动作
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

            // 在英文和中文之间切换
            val targetMethod = when (currentMethod) {
                InputMethodType.ENGLISH -> InputMethodType.CHINESE
                InputMethodType.CHINESE -> InputMethodType.ENGLISH
                InputMethodType.UNKNOWN -> InputMethodType.ENGLISH
            }

            inputMethodService.switchToInputMethod(targetMethod, "手动")

            val message = "已切换到${targetMethod.displayName}输入法"
            LOG.info(message)

            // 显示简短通知
            if (configService.isDebugMode()) {
                Messages.showInfoMessage(project, message, "智能输入法专业版")
            }

        } catch (ex: Exception) {
            LOG.error("手动切换输入法时出错", ex)
            Messages.showErrorDialog(
                e.project,
                "切换输入法失败：${ex.message}",
                "智能输入法专业版错误"
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
