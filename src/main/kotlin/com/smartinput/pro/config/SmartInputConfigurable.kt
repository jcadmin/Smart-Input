package com.smartinput.pro.config

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.*
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.smartinput.pro.service.SmartInputConfigService
import com.smartinput.pro.platform.PlatformDetector
import javax.swing.*

/**
 * Configuration panel for Smart Input Pro settings
 */
class SmartInputConfigurable : Configurable {
    
    private val configService = SmartInputConfigService.getInstance()
    private val platformDetector = PlatformDetector()
    
    // UI Components
    private lateinit var enabledCheckBox: JBCheckBox
    private lateinit var autoSwitchCheckBox: JBCheckBox
    private lateinit var showIndicatorCheckBox: JBCheckBox
    
    // Context Rules
    private lateinit var switchInCodeAreasCheckBox: JBCheckBox
    private lateinit var switchInCommentsCheckBox: JBCheckBox
    private lateinit var switchInStringsCheckBox: JBCheckBox
    private lateinit var switchInDocumentationCheckBox: JBCheckBox
    
    // Input Method Preferences
    private lateinit var codeAreaInputMethodCombo: ComboBox<String>
    private lateinit var commentInputMethodCombo: ComboBox<String>
    private lateinit var stringInputMethodCombo: ComboBox<String>
    private lateinit var documentationInputMethodCombo: ComboBox<String>
    
    // Appearance Settings
    private lateinit var indicatorPositionCombo: ComboBox<String>
    private lateinit var indicatorSizeSpinner: JSpinner
    private lateinit var indicatorOpacitySlider: JSlider
    private lateinit var indicatorTimeoutSpinner: JSpinner
    
    // Advanced Settings
    private lateinit var detectionDelaySpinner: JSpinner
    private lateinit var switchDelaySpinner: JSpinner
    private lateinit var debugModeCheckBox: JBCheckBox
    
    // Platform info
    private lateinit var platformInfoLabel: JBLabel

    override fun getDisplayName(): String = "Smart Input Pro"

    override fun createComponent(): JComponent {
        initializeComponents()
        return createMainPanel()
    }

    private fun initializeComponents() {
        // 基本设置
        enabledCheckBox = JBCheckBox("启用智能输入法专业版")
        autoSwitchCheckBox = JBCheckBox("自动切换模式")
        showIndicatorCheckBox = JBCheckBox("显示输入法指示器")

        // 上下文规则
        switchInCodeAreasCheckBox = JBCheckBox("在代码区域切换")
        switchInCommentsCheckBox = JBCheckBox("在注释中切换")
        switchInStringsCheckBox = JBCheckBox("在字符串字面量中切换")
        switchInDocumentationCheckBox = JBCheckBox("在文档中切换")

        // 输入法偏好设置
        val inputMethodOptions = arrayOf("english", "chinese", "auto")
        codeAreaInputMethodCombo = ComboBox(inputMethodOptions)
        commentInputMethodCombo = ComboBox(inputMethodOptions)
        stringInputMethodCombo = ComboBox(arrayOf("english", "chinese", "auto"))
        documentationInputMethodCombo = ComboBox(inputMethodOptions)

        // 外观设置
        val positionOptions = arrayOf("cursor", "top-right", "bottom-right")
        indicatorPositionCombo = ComboBox(positionOptions)
        indicatorSizeSpinner = JSpinner(SpinnerNumberModel(12, 8, 24, 1))
        indicatorOpacitySlider = JSlider(10, 100, 80)
        indicatorTimeoutSpinner = JSpinner(SpinnerNumberModel(2000, 500, 10000, 100))

        // 高级设置
        detectionDelaySpinner = JSpinner(SpinnerNumberModel(100, 0, 5000, 10))
        switchDelaySpinner = JSpinner(SpinnerNumberModel(50, 0, 1000, 10))
        debugModeCheckBox = JBCheckBox("调试模式")

        // 平台信息
        val platformInfo = platformDetector.getPlatformInfo()
        platformInfoLabel = JBLabel("<html><b>平台:</b> ${platformInfo.platform.displayName}<br>" +
                "<b>操作系统:</b> ${platformInfo.osName} ${platformInfo.osVersion}<br>" +
                "<b>支持:</b> ${if (platformInfo.isSupported) "是" else "否"}</html>")
    }

    private fun createMainPanel(): JPanel {
        return panel {
            group("基本设置") {
                row { cell(enabledCheckBox) }
                row { cell(autoSwitchCheckBox) }
                row { cell(showIndicatorCheckBox) }
            }

            group("上下文规则") {
                row("代码区域:") {
                    cell(switchInCodeAreasCheckBox)
                    cell(codeAreaInputMethodCombo)
                }
                row("注释:") {
                    cell(switchInCommentsCheckBox)
                    cell(commentInputMethodCombo)
                }
                row("字符串字面量:") {
                    cell(switchInStringsCheckBox)
                    cell(stringInputMethodCombo)
                }
                row("文档:") {
                    cell(switchInDocumentationCheckBox)
                    cell(documentationInputMethodCombo)
                }
            }

            group("外观") {
                row("指示器位置:") { cell(indicatorPositionCombo) }
                row("指示器大小:") { cell(indicatorSizeSpinner) }
                row("指示器透明度:") {
                    cell(indicatorOpacitySlider)
                    label("${indicatorOpacitySlider.value}%")
                }
                row("显示超时时间 (毫秒):") { cell(indicatorTimeoutSpinner) }
            }

            group("高级设置") {
                row("检测延迟 (毫秒):") { cell(detectionDelaySpinner) }
                row("切换延迟 (毫秒):") { cell(switchDelaySpinner) }
                row { cell(debugModeCheckBox) }
            }

            group("平台信息") {
                row { cell(platformInfoLabel) }
            }

            row {
                button("重置为默认值") {
                    resetToDefaults()
                }
                button("测试切换到英文") {
                    testSwitchToEnglish()
                }
                button("测试切换到中文") {
                    testSwitchToChinese()
                }
                button("检测当前输入法") {
                    detectCurrentInputMethod()
                }
            }
        }.apply {
            border = JBUI.Borders.empty(10)
        }
    }

    override fun isModified(): Boolean {
        return enabledCheckBox.isSelected != configService.isEnabled() ||
                autoSwitchCheckBox.isSelected != configService.isAutoSwitchMode() ||
                showIndicatorCheckBox.isSelected != configService.isShowIndicator() ||
                switchInCodeAreasCheckBox.isSelected != configService.shouldSwitchInCodeAreas() ||
                switchInCommentsCheckBox.isSelected != configService.shouldSwitchInComments() ||
                switchInStringsCheckBox.isSelected != configService.shouldSwitchInStrings() ||
                switchInDocumentationCheckBox.isSelected != configService.shouldSwitchInDocumentation() ||
                codeAreaInputMethodCombo.selectedItem != configService.getCodeAreaInputMethod() ||
                commentInputMethodCombo.selectedItem != configService.getCommentInputMethod() ||
                stringInputMethodCombo.selectedItem != configService.getStringInputMethod() ||
                documentationInputMethodCombo.selectedItem != configService.getDocumentationInputMethod() ||
                indicatorPositionCombo.selectedItem != configService.getIndicatorPosition() ||
                indicatorSizeSpinner.value != configService.getIndicatorSize() ||
                indicatorOpacitySlider.value != (configService.getIndicatorOpacity() * 100).toInt() ||
                indicatorTimeoutSpinner.value != configService.getIndicatorTimeout() ||
                detectionDelaySpinner.value != configService.getDetectionDelay() ||
                switchDelaySpinner.value != configService.getSwitchDelay() ||
                debugModeCheckBox.isSelected != configService.isDebugMode()
    }

    override fun apply() {
        configService.setEnabled(enabledCheckBox.isSelected)
        configService.setAutoSwitchMode(autoSwitchCheckBox.isSelected)
        configService.setShowIndicator(showIndicatorCheckBox.isSelected)
        
        // Update state directly since we don't have setters for all properties
        val state = configService.state
        state.switchInCodeAreas = switchInCodeAreasCheckBox.isSelected
        state.switchInComments = switchInCommentsCheckBox.isSelected
        state.switchInStrings = switchInStringsCheckBox.isSelected
        state.switchInDocumentation = switchInDocumentationCheckBox.isSelected
        
        state.codeAreaInputMethod = codeAreaInputMethodCombo.selectedItem as String
        state.commentInputMethod = commentInputMethodCombo.selectedItem as String
        state.stringInputMethod = stringInputMethodCombo.selectedItem as String
        state.documentationInputMethod = documentationInputMethodCombo.selectedItem as String
        
        state.indicatorPosition = indicatorPositionCombo.selectedItem as String
        state.indicatorSize = indicatorSizeSpinner.value as Int
        state.indicatorOpacity = indicatorOpacitySlider.value / 100.0f
        state.indicatorTimeout = indicatorTimeoutSpinner.value as Int
        
        state.detectionDelay = detectionDelaySpinner.value as Int
        state.switchDelay = switchDelaySpinner.value as Int
        state.debugMode = debugModeCheckBox.isSelected
        
        configService.validateAndFix()
    }

    override fun reset() {
        enabledCheckBox.isSelected = configService.isEnabled()
        autoSwitchCheckBox.isSelected = configService.isAutoSwitchMode()
        showIndicatorCheckBox.isSelected = configService.isShowIndicator()
        
        switchInCodeAreasCheckBox.isSelected = configService.shouldSwitchInCodeAreas()
        switchInCommentsCheckBox.isSelected = configService.shouldSwitchInComments()
        switchInStringsCheckBox.isSelected = configService.shouldSwitchInStrings()
        switchInDocumentationCheckBox.isSelected = configService.shouldSwitchInDocumentation()
        
        codeAreaInputMethodCombo.selectedItem = configService.getCodeAreaInputMethod()
        commentInputMethodCombo.selectedItem = configService.getCommentInputMethod()
        stringInputMethodCombo.selectedItem = configService.getStringInputMethod()
        documentationInputMethodCombo.selectedItem = configService.getDocumentationInputMethod()
        
        indicatorPositionCombo.selectedItem = configService.getIndicatorPosition()
        indicatorSizeSpinner.value = configService.getIndicatorSize()
        indicatorOpacitySlider.value = (configService.getIndicatorOpacity() * 100).toInt()
        indicatorTimeoutSpinner.value = configService.getIndicatorTimeout()
        
        detectionDelaySpinner.value = configService.getDetectionDelay()
        switchDelaySpinner.value = configService.getSwitchDelay()
        debugModeCheckBox.isSelected = configService.isDebugMode()
    }

    private fun resetToDefaults() {
        configService.resetToDefaults()
        reset()
    }

    private fun testSwitchToEnglish() {
        try {
            // 使用Java Robot类直接发送键盘事件
            val robot = java.awt.Robot()

            // 发送Ctrl+Space
            robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL)
            Thread.sleep(10)
            robot.keyPress(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(50)
            robot.keyRelease(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(10)
            robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL)

            JOptionPane.showMessageDialog(
                null,
                "Java Robot Test Completed!\n\nCtrl+Space has been sent using Java Robot.\nPlease test input in Notepad immediately!",
                "Test Switch to English - Java Robot",
                JOptionPane.INFORMATION_MESSAGE
            )

        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "Java Robot test failed: ${e.message}",
                "Test Switch to English - Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun testSwitchToChinese() {
        try {
            // 使用Java Robot类直接发送键盘事件
            val robot = java.awt.Robot()

            // 发送Ctrl+Space
            robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL)
            Thread.sleep(10)
            robot.keyPress(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(50)
            robot.keyRelease(java.awt.event.KeyEvent.VK_SPACE)
            Thread.sleep(10)
            robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL)

            JOptionPane.showMessageDialog(
                null,
                "Java Robot Test Completed!\n\nCtrl+Space has been sent using Java Robot.\nPlease test input in Notepad immediately!",
                "Test Switch to Chinese - Java Robot",
                JOptionPane.INFORMATION_MESSAGE
            )

        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "Java Robot test failed: ${e.message}",
                "Test Switch to Chinese - Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun detectCurrentInputMethod() {
        try {
            // 检测当前输入法状态
            val script = """
                Add-Type -TypeDefinition @'
                using System;
                using System.Runtime.InteropServices;
                public class InputMethod {
                    [DllImport("user32.dll")]
                    public static extern IntPtr GetKeyboardLayout(uint idThread);
                }
'@
                ${'$'}layout = [InputMethod]::GetKeyboardLayout(0).ToString('X8')
                Write-Output "当前键盘布局: ${'$'}layout"

                ${'$'}lang = Get-WinUserLanguageList | Select-Object -First 1
                Write-Output "当前语言: $(${'$'}lang.LanguageTag)"
                Write-Output "输入法: $(${'$'}lang.InputMethodTips)"
            """.trimIndent()

            val process = ProcessBuilder("powershell", "-Command", script).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            JOptionPane.showMessageDialog(
                null,
                "当前输入法状态：\n\n$output",
                "检测当前输入法",
                JOptionPane.INFORMATION_MESSAGE
            )
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "检测失败：${e.message}",
                "检测当前输入法",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
}
