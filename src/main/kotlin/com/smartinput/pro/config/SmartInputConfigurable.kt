package com.smartinput.pro.config

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.*
import com.intellij.ui.layout.panel
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
        // General Settings
        enabledCheckBox = JBCheckBox("Enable Smart Input Pro")
        autoSwitchCheckBox = JBCheckBox("Auto Switch Mode")
        showIndicatorCheckBox = JBCheckBox("Show Input Method Indicator")
        
        // Context Rules
        switchInCodeAreasCheckBox = JBCheckBox("Switch in Code Areas")
        switchInCommentsCheckBox = JBCheckBox("Switch in Comments")
        switchInStringsCheckBox = JBCheckBox("Switch in String Literals")
        switchInDocumentationCheckBox = JBCheckBox("Switch in Documentation")
        
        // Input Method Preferences
        val inputMethodOptions = arrayOf("english", "chinese", "auto")
        codeAreaInputMethodCombo = ComboBox(inputMethodOptions)
        commentInputMethodCombo = ComboBox(inputMethodOptions)
        stringInputMethodCombo = ComboBox(arrayOf("english", "chinese", "auto"))
        documentationInputMethodCombo = ComboBox(inputMethodOptions)
        
        // Appearance Settings
        val positionOptions = arrayOf("cursor", "top-right", "bottom-right")
        indicatorPositionCombo = ComboBox(positionOptions)
        indicatorSizeSpinner = JSpinner(SpinnerNumberModel(12, 8, 24, 1))
        indicatorOpacitySlider = JSlider(10, 100, 80)
        indicatorTimeoutSpinner = JSpinner(SpinnerNumberModel(2000, 500, 10000, 100))
        
        // Advanced Settings
        detectionDelaySpinner = JSpinner(SpinnerNumberModel(100, 0, 5000, 10))
        switchDelaySpinner = JSpinner(SpinnerNumberModel(50, 0, 1000, 10))
        debugModeCheckBox = JBCheckBox("Debug Mode")
        
        // Platform Info
        val platformInfo = platformDetector.getPlatformInfo()
        platformInfoLabel = JBLabel("<html><b>Platform:</b> ${platformInfo.platform.displayName}<br>" +
                "<b>OS:</b> ${platformInfo.osName} ${platformInfo.osVersion}<br>" +
                "<b>Supported:</b> ${if (platformInfo.isSupported) "Yes" else "No"}</html>")
    }

    private fun createMainPanel(): JPanel {
        return panel {
            titledRow("General Settings") {
                row { enabledCheckBox() }
                row { autoSwitchCheckBox() }
                row { showIndicatorCheckBox() }
            }
            
            titledRow("Context Rules") {
                row("Code Areas:") { 
                    switchInCodeAreasCheckBox()
                    codeAreaInputMethodCombo()
                }
                row("Comments:") { 
                    switchInCommentsCheckBox()
                    commentInputMethodCombo()
                }
                row("String Literals:") { 
                    switchInStringsCheckBox()
                    stringInputMethodCombo()
                }
                row("Documentation:") { 
                    switchInDocumentationCheckBox()
                    documentationInputMethodCombo()
                }
            }
            
            titledRow("Appearance") {
                row("Indicator Position:") { indicatorPositionCombo() }
                row("Indicator Size:") { indicatorSizeSpinner() }
                row("Indicator Opacity:") { 
                    indicatorOpacitySlider()
                    JBLabel("${indicatorOpacitySlider.value}%")
                }
                row("Display Timeout (ms):") { indicatorTimeoutSpinner() }
            }
            
            titledRow("Advanced Settings") {
                row("Detection Delay (ms):") { detectionDelaySpinner() }
                row("Switch Delay (ms):") { switchDelaySpinner() }
                row { debugModeCheckBox() }
            }
            
            titledRow("Platform Information") {
                row { platformInfoLabel() }
            }
            
            row {
                button("Reset to Defaults") {
                    resetToDefaults()
                }
                button("Test Input Method Switching") {
                    testInputMethodSwitching()
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

    private fun testInputMethodSwitching() {
        // This would trigger a test of the input method switching functionality
        JOptionPane.showMessageDialog(
            null,
            "Input method switching test would be performed here.\nCheck the IDE logs for results.",
            "Test Input Method Switching",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
}
