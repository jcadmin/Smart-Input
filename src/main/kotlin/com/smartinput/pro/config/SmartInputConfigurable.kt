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
            // Robot会自动将事件发送到当前有焦点的窗口
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
                "Java Robot Test Completed!\n\nCtrl+Space has been sent using Java Robot.\nPlease test input in the current window immediately!",
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
            // Robot会自动将事件发送到当前有焦点的窗口
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
                "Java Robot Test Completed!\n\nCtrl+Space has been sent using Java Robot.\nPlease test input in the current window immediately!",
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
            val results = mutableListOf<String>()
            results.add("=== 输入法状态检测调试 ===\n")

            // 方法1：QQ输入法窗口检测
            try {
                results.add("【方法1：QQ输入法窗口检测】")
                val windowResult = testQQInputMethodWindow()
                results.add("窗口检测结果:\n$windowResult")
                results.add("")
            } catch (e: Exception) {
                results.add("QQ窗口检测失败: ${e.message}")
                results.add("")
            }

            // 方法2：检测系统语言环境
            try {
                results.add("【方法2：系统语言环境】")
                val systemLocale = java.util.Locale.getDefault()
                results.add("系统默认语言: ${systemLocale.displayLanguage} (${systemLocale.language})")

                val inputContext = java.awt.im.InputContext.getInstance()
                val currentLocale = inputContext?.locale
                if (currentLocale != null) {
                    results.add("输入法上下文语言: ${currentLocale.displayLanguage} (${currentLocale.language})")
                } else {
                    results.add("输入法上下文语言: 未检测到")
                }
                results.add("")
            } catch (e: Exception) {
                results.add("系统语言环境检测失败: ${e.message}")
                results.add("")
            }

            // 方法3：检测输入法进程
            try {
                results.add("【方法3：输入法进程检测】")
                val processResult = testInputMethodProcess()
                results.add("进程检测结果: $processResult")
                results.add("")
            } catch (e: Exception) {
                results.add("进程检测失败: ${e.message}")
                results.add("")
            }

            // 方法4：使用插件的完整检测逻辑
            try {
                results.add("【方法4：插件完整检测】")
                // 注意：WindowsInputMethodManager构造函数不需要参数
                val inputMethodManager = com.smartinput.pro.platform.WindowsInputMethodManager()

                // 注意：这需要在协程中调用，这里我们模拟调用
                results.add("插件检测: 需要在实际环境中测试")
                results.add("当前使用: Windows平台 + Java Robot切换")
                results.add("")
            } catch (e: Exception) {
                results.add("插件检测失败: ${e.message}")
                results.add("")
            }

            results.add("=== 检测完成 ===")
            results.add("请根据上述结果判断哪种方法最准确")

            JOptionPane.showMessageDialog(
                null,
                results.joinToString("\n"),
                "输入法状态检测调试",
                JOptionPane.INFORMATION_MESSAGE
            )
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "检测失败：${e.message}",
                "输入法状态检测调试 - 错误",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    /**
     * 测试QQ输入法窗口检测
     */
    private fun testQQInputMethodWindow(): String {
        return try {
            val script = """
                Add-Type -TypeDefinition @'
                using System;
                using System.Runtime.InteropServices;
                using System.Text;
                public class WindowDetector {
                    [DllImport("user32.dll", SetLastError = true)]
                    public static extern IntPtr FindWindow(string lpClassName, string lpWindowName);

                    [DllImport("user32.dll", SetLastError = true)]
                    public static extern IntPtr FindWindowEx(IntPtr hwndParent, IntPtr hwndChildAfter, string lpszClass, string lpszWindow);

                    [DllImport("user32.dll", CharSet = CharSet.Auto)]
                    public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

                    [DllImport("user32.dll")]
                    public static extern bool IsWindowVisible(IntPtr hWnd);

                    [DllImport("user32.dll")]
                    public static extern bool EnumWindows(EnumWindowsProc enumProc, IntPtr lParam);

                    [DllImport("user32.dll")]
                    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

                    [DllImport("user32.dll", CharSet = CharSet.Auto)]
                    public static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

                    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
                }
'@ -ErrorAction SilentlyContinue

                # 查找QQ输入法相关窗口
                ${'$'}qqWindows = @()

                # 常见的QQ输入法窗口类名和标题
                ${'$'}qqClassNames = @(
                    "QQPinyinFloatWnd",
                    "QQPinyinMainWnd",
                    "QQInputMethodStatusWnd",
                    "QQPinyinCandWnd",
                    "QQInputStatusWnd",
                    "TXGuiFoundation",
                    "QQPinyin"
                )

                ${'$'}qqTitles = @(
                    "*QQ*",
                    "*拼音*",
                    "*输入法*"
                )

                # 搜索QQ输入法窗口
                foreach (${'$'}className in ${'$'}qqClassNames) {
                    try {
                        ${'$'}hwnd = [WindowDetector]::FindWindow(${'$'}className, ${'$'}null)
                        if (${'$'}hwnd -ne [IntPtr]::Zero) {
                            ${'$'}sb = New-Object System.Text.StringBuilder(256)
                            [WindowDetector]::GetWindowText(${'$'}hwnd, ${'$'}sb, 256)
                            ${'$'}title = ${'$'}sb.ToString()
                            ${'$'}visible = [WindowDetector]::IsWindowVisible(${'$'}hwnd)

                            Write-Output "找到QQ窗口: 类名=${'$'}className, 标题=${'$'}title, 可见=${'$'}visible"
                        }
                    } catch {
                        # 忽略错误继续搜索
                    }
                }

                # 枚举所有窗口寻找QQ输入法
                ${'$'}foundWindows = @()
                ${'$'}enumProc = {
                    param(${'$'}hwnd, ${'$'}lParam)

                    try {
                        ${'$'}sb = New-Object System.Text.StringBuilder(256)
                        [WindowDetector]::GetClassName(${'$'}hwnd, ${'$'}sb, 256)
                        ${'$'}className = ${'$'}sb.ToString()

                        ${'$'}sb2 = New-Object System.Text.StringBuilder(256)
                        [WindowDetector]::GetWindowText(${'$'}hwnd, ${'$'}sb2, 256)
                        ${'$'}title = ${'$'}sb2.ToString()

                        if (${'$'}className -like "*QQ*" -or ${'$'}className -like "*pinyin*" -or ${'$'}title -like "*QQ*" -or ${'$'}title -like "*拼音*") {
                            ${'$'}visible = [WindowDetector]::IsWindowVisible(${'$'}hwnd)
                            ${'$'}script:foundWindows += "类名: ${'$'}className, 标题: ${'$'}title, 可见: ${'$'}visible"
                        }
                    } catch {
                        # 忽略错误
                    }

                    return ${'$'}true
                }

                [WindowDetector]::EnumWindows(${'$'}enumProc, [IntPtr]::Zero)

                if (${'$'}foundWindows.Count -gt 0) {
                    Write-Output "枚举找到的QQ相关窗口:"
                    foreach (${'$'}window in ${'$'}foundWindows) {
                        Write-Output "  ${'$'}window"
                    }
                } else {
                    Write-Output "未找到QQ输入法相关窗口"
                }
            """.trimIndent()

            val process = ProcessBuilder("powershell", "-Command", script).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            output.trim()
        } catch (e: Exception) {
            "QQ窗口检测异常: ${e.message}"
        }
    }

    /**
     * 测试多种输入法检测方法
     */
    private fun testIMEConversionStatus(): String {
        val results = mutableListOf<String>()

        // 方法1：检测当前键盘布局
        try {
            results.add("=== 键盘布局检测 ===")
            val layoutScript = """
                Add-Type -TypeDefinition @'
                using System;
                using System.Runtime.InteropServices;
                using System.Text;
                public class KeyboardDetector {
                    [DllImport("user32.dll")]
                    public static extern IntPtr GetKeyboardLayout(uint idThread);

                    [DllImport("user32.dll")]
                    public static extern int GetKeyboardLayoutName(StringBuilder pwszKLID);

                    [DllImport("user32.dll")]
                    public static extern IntPtr GetForegroundWindow();

                    [DllImport("user32.dll")]
                    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);
                }
'@ -ErrorAction SilentlyContinue

                ${'$'}hwnd = [KeyboardDetector]::GetForegroundWindow()
                ${'$'}processId = 0
                ${'$'}threadId = [KeyboardDetector]::GetWindowThreadProcessId(${'$'}hwnd, [ref]${'$'}processId)

                ${'$'}layout = [KeyboardDetector]::GetKeyboardLayout(${'$'}threadId)
                Write-Output "当前线程键盘布局: $(${'$'}layout.ToInt64())"

                ${'$'}sb = New-Object System.Text.StringBuilder(9)
                ${'$'}result = [KeyboardDetector]::GetKeyboardLayoutName(${'$'}sb)
                if (${'$'}result -ne 0) {
                    Write-Output "键盘布局名称: $(${'$'}sb.ToString())"
                }
            """.trimIndent()

            val process1 = ProcessBuilder("powershell", "-Command", layoutScript).start()
            val output1 = process1.inputStream.bufferedReader().readText()
            process1.waitFor()
            results.add(output1.trim())
        } catch (e: Exception) {
            results.add("键盘布局检测失败: ${e.message}")
        }

        // 方法2：检测Text Services Framework
        try {
            results.add("\n=== TSF输入法检测 ===")
            val tsfScript = """
                try {
                    # 尝试获取当前输入法信息
                    ${'$'}inputLanguage = [System.Windows.Forms.InputLanguage]::CurrentInputLanguage
                    Write-Output "当前输入语言: $(${'$'}inputLanguage.Culture.Name)"
                    Write-Output "输入语言显示名: $(${'$'}inputLanguage.Culture.DisplayName)"
                    Write-Output "布局名称: $(${'$'}inputLanguage.LayoutName)"

                    # 获取所有已安装的输入语言
                    ${'$'}installedLanguages = [System.Windows.Forms.InputLanguage]::InstalledInputLanguages
                    Write-Output "已安装输入语言数量: $(${'$'}installedLanguages.Count)"

                } catch {
                    Write-Output "TSF检测失败: $(${'$'}_.Exception.Message)"
                }
            """.trimIndent()

            val process2 = ProcessBuilder("powershell", "-Command", "Add-Type -AssemblyName System.Windows.Forms; $tsfScript").start()
            val output2 = process2.inputStream.bufferedReader().readText()
            process2.waitFor()
            results.add(output2.trim())
        } catch (e: Exception) {
            results.add("TSF检测失败: ${e.message}")
        }

        // 方法3：检测注册表中的输入法信息
        try {
            results.add("\n=== 注册表输入法检测 ===")
            val regScript = """
                try {
                    # 检查当前用户的输入法设置
                    ${'$'}currentIM = Get-ItemProperty -Path "HKCU:\Control Panel\International" -ErrorAction SilentlyContinue
                    if (${'$'}currentIM) {
                        Write-Output "当前区域设置: $(${'$'}currentIM.Locale)"
                        Write-Output "默认输入法: $(${'$'}currentIM.sLanguage)"
                    }

                    # 检查输入法列表
                    ${'$'}imePath = "HKLM:\SYSTEM\CurrentControlSet\Control\Keyboard Layouts"
                    ${'$'}layouts = Get-ChildItem -Path ${'$'}imePath -ErrorAction SilentlyContinue
                    Write-Output "系统键盘布局数量: $(${'$'}layouts.Count)"

                } catch {
                    Write-Output "注册表检测失败: $(${'$'}_.Exception.Message)"
                }
            """.trimIndent()

            val process3 = ProcessBuilder("powershell", "-Command", regScript).start()
            val output3 = process3.inputStream.bufferedReader().readText()
            process3.waitFor()
            results.add(output3.trim())
        } catch (e: Exception) {
            results.add("注册表检测失败: ${e.message}")
        }

        return results.joinToString("\n")
    }

    /**
     * 测试输入法进程检测
     */
    private fun testInputMethodProcess(): String {
        return try {
            val script = """
                # 检查QQ输入法相关进程
                ${'$'}qqProcesses = Get-Process | Where-Object {
                    ${'$'}_.ProcessName -like "*QQ*" -or
                    ${'$'}_.ProcessName -like "*qq*" -or
                    ${'$'}_.ProcessName -like "*QQPinyin*" -or
                    ${'$'}_.ProcessName -like "*QQInput*"
                }

                if (${'$'}qqProcesses) {
                    Write-Output "找到QQ输入法相关进程:"
                    foreach (${'$'}proc in ${'$'}qqProcesses) {
                        Write-Output "  - $(${'$'}proc.ProcessName): $(${'$'}proc.MainWindowTitle)"
                    }
                } else {
                    Write-Output "未找到QQ输入法相关进程"
                }

                # 检查所有输入法相关进程
                ${'$'}imeProcesses = Get-Process | Where-Object {
                    ${'$'}_.ProcessName -like "*ime*" -or
                    ${'$'}_.ProcessName -like "*input*" -or
                    ${'$'}_.ProcessName -like "*pinyin*"
                }

                if (${'$'}imeProcesses) {
                    Write-Output "找到输入法相关进程:"
                    foreach (${'$'}proc in ${'$'}imeProcesses) {
                        Write-Output "  - $(${'$'}proc.ProcessName)"
                    }
                }
            """.trimIndent()

            val process = ProcessBuilder("powershell", "-Command", script).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            output.trim()
        } catch (e: Exception) {
            "进程检测异常: ${e.message}"
        }
    }


}
