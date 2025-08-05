package com.smartinput.pro.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.smartinput.pro.service.SmartInputConfigService

/**
 * 切换智能输入法专业版开关的动作
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

            val newState = if (configService.isEnabled()) "已启用" else "已禁用"
            val message = "智能输入法专业版$newState"

            LOG.info(message)

            // 向用户显示通知
            Messages.showInfoMessage(
                e.project,
                message,
                "智能输入法专业版"
            )

        } catch (ex: Exception) {
            LOG.error("切换智能输入法专业版时出错", ex)
            Messages.showErrorDialog(
                e.project,
                "切换智能输入法专业版失败：${ex.message}",
                "智能输入法专业版错误"
            )
        }
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation

        // 根据当前状态更新动作文本
        if (configService.isEnabled()) {
            presentation.text = "禁用智能输入法专业版"
            presentation.description = "禁用自动输入法切换"
        } else {
            presentation.text = "启用智能输入法专业版"
            presentation.description = "启用自动输入法切换"
        }

        // 动作始终可用
        presentation.isEnabled = true
        presentation.isVisible = true
    }
}
