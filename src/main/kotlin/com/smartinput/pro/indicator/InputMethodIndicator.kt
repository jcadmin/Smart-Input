package com.smartinput.pro.indicator

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.*
import com.intellij.ui.JBColor
import com.smartinput.pro.model.ContextInfo
import com.smartinput.pro.model.InputMethodType
import com.smartinput.pro.service.SmartInputConfigService
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

/**
 * Visual indicator that shows the current input method status near the cursor
 */
class InputMethodIndicator(private val editor: Editor) {
    
    companion object {
        private val LOG = Logger.getInstance(InputMethodIndicator::class.java)
        private const val INDICATOR_WIDTH = 60
        private const val INDICATOR_HEIGHT = 20
    }

    private val configService = SmartInputConfigService.getInstance()
    private var indicatorPanel: JPanel? = null
    private var currentInputMethod: InputMethodType = InputMethodType.UNKNOWN
    private var isVisible = false
    
    // Coroutine scope for animations and auto-hide
    private val indicatorScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var hideJob: Job? = null

    init {
        createIndicatorPanel()
        setupEditorListeners()
    }

    /**
     * Create the visual indicator panel
     */
    private fun createIndicatorPanel() {
        indicatorPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                paintIndicator(g as Graphics2D)
            }
        }.apply {
            isOpaque = false
            preferredSize = Dimension(INDICATOR_WIDTH, INDICATOR_HEIGHT)
            size = preferredSize
        }
    }

    /**
     * Setup listeners for editor events
     */
    private fun setupEditorListeners() {
        // Listen for editor component resize/move events
        editor.component.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                updateIndicatorPosition()
            }

            override fun componentMoved(e: ComponentEvent?) {
                updateIndicatorPosition()
            }
        })
    }

    /**
     * Paint the indicator with current input method information
     */
    private fun paintIndicator(g: Graphics2D) {
        if (!isVisible || indicatorPanel == null) return

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val panel = indicatorPanel!!
        val width = panel.width
        val height = panel.height

        // Set opacity
        val alpha = (configService.getIndicatorOpacity() * 255).toInt()
        val composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, configService.getIndicatorOpacity())
        g.composite = composite

        // Draw background
        val backgroundColor = getBackgroundColor()
        g.color = Color(backgroundColor.red, backgroundColor.green, backgroundColor.blue, alpha)
        g.fillRoundRect(0, 0, width, height, 8, 8)

        // Draw border
        val borderColor = getBorderColor()
        g.color = Color(borderColor.red, borderColor.green, borderColor.blue, alpha)
        g.drawRoundRect(0, 0, width - 1, height - 1, 8, 8)

        // Draw text
        val text = getIndicatorText()
        val textColor = getTextColor()
        g.color = Color(textColor.red, textColor.green, textColor.blue, alpha)
        
        val font = Font(Font.SANS_SERIF, Font.BOLD, configService.getIndicatorSize())
        g.font = font
        
        val fontMetrics = g.fontMetrics
        val textWidth = fontMetrics.stringWidth(text)
        val textHeight = fontMetrics.height
        
        val x = (width - textWidth) / 2
        val y = (height - textHeight) / 2 + fontMetrics.ascent
        
        g.drawString(text, x, y)
    }

    /**
     * Get the text to display in the indicator
     */
    private fun getIndicatorText(): String {
        return when (currentInputMethod) {
            InputMethodType.ENGLISH -> "EN"
            InputMethodType.CHINESE -> "中"
            InputMethodType.UNKNOWN -> "?"
        }
    }

    /**
     * Get background color based on input method
     */
    private fun getBackgroundColor(): Color {
        return when (currentInputMethod) {
            InputMethodType.ENGLISH -> JBColor(Color(0, 120, 215), Color(0, 120, 215)) // Blue
            InputMethodType.CHINESE -> JBColor(Color(255, 140, 0), Color(255, 140, 0)) // Orange
            InputMethodType.UNKNOWN -> JBColor.GRAY
        }
    }

    /**
     * Get border color
     */
    private fun getBorderColor(): Color {
        val bg = getBackgroundColor()
        return Color(bg.red, bg.green, bg.blue, 180)
    }

    /**
     * Get text color
     */
    private fun getTextColor(): Color {
        return JBColor.WHITE
    }

    /**
     * Update the input method and refresh the indicator
     */
    fun updateInputMethod(inputMethod: InputMethodType, contextInfo: ContextInfo? = null) {
        if (currentInputMethod == inputMethod) return

        currentInputMethod = inputMethod
        
        if (configService.isDebugMode()) {
            LOG.debug("Updating indicator: $inputMethod, context: ${contextInfo?.type}")
        }

        // Show indicator
        show()

        // Schedule auto-hide
        scheduleAutoHide()
    }

    /**
     * Show the indicator
     */
    fun show() {
        if (!configService.isShowIndicator()) return

        isVisible = true
        
        indicatorScope.launch {
            try {
                addToEditor()
                updateIndicatorPosition()
                indicatorPanel?.repaint()
            } catch (e: Exception) {
                LOG.error("Error showing indicator", e)
            }
        }
    }

    /**
     * Hide the indicator
     */
    fun hide() {
        isVisible = false
        hideJob?.cancel()
        
        indicatorScope.launch {
            try {
                removeFromEditor()
            } catch (e: Exception) {
                LOG.error("Error hiding indicator", e)
            }
        }
    }

    /**
     * Schedule automatic hiding of the indicator
     */
    private fun scheduleAutoHide() {
        hideJob?.cancel()
        
        val timeout = configService.getIndicatorTimeout().toLong()
        if (timeout > 0) {
            hideJob = indicatorScope.launch {
                delay(timeout)
                hide()
            }
        }
    }

    /**
     * Add indicator to editor
     */
    private fun addToEditor() {
        val panel = indicatorPanel ?: return

        // Remove if already added
        removeFromEditor()

        try {
            // Add to editor's layered pane
            val layeredPane = editor.component.rootPane?.layeredPane
            if (layeredPane != null) {
                layeredPane.add(panel, JLayeredPane.POPUP_LAYER as Any)
                layeredPane.revalidate()
                layeredPane.repaint()
            }
        } catch (e: Exception) {
            LOG.error("Error adding indicator to editor", e)
        }
    }

    /**
     * Remove indicator from editor
     */
    private fun removeFromEditor() {
        val panel = indicatorPanel ?: return
        val parent = panel.parent
        
        if (parent != null) {
            parent.remove(panel)
            parent.revalidate()
            parent.repaint()
        }
    }

    /**
     * Update indicator position based on cursor location
     */
    private fun updateIndicatorPosition() {
        val panel = indicatorPanel ?: return
        if (!isVisible) return

        try {
            val caretModel = editor.caretModel
            val caretPosition = caretModel.visualPosition
            val point = editor.visualPositionToXY(caretPosition)
            
            // Calculate position based on configuration
            val position = calculateIndicatorPosition(point)
            
            panel.location = position
            panel.parent?.repaint()
            
        } catch (e: Exception) {
            LOG.error("Error updating indicator position", e)
        }
    }

    /**
     * Calculate indicator position based on cursor and configuration
     */
    private fun calculateIndicatorPosition(cursorPoint: Point): Point {
        val editorBounds = editor.component.bounds
        val indicatorSize = Dimension(INDICATOR_WIDTH, INDICATOR_HEIGHT)
        
        return when (configService.getIndicatorPosition()) {
            "cursor" -> {
                // Position near cursor with offset
                val maxX = maxOf(0, editorBounds.width - indicatorSize.width)
                Point(
                    (cursorPoint.x + 20).coerceIn(0, maxX),
                    (cursorPoint.y - 25).coerceAtLeast(0)
                )
            }
            "top-right" -> {
                val x = maxOf(0, editorBounds.width - indicatorSize.width - 10)
                Point(x, 10)
            }
            "bottom-right" -> {
                val x = maxOf(0, editorBounds.width - indicatorSize.width - 10)
                val y = maxOf(0, editorBounds.height - indicatorSize.height - 10)
                Point(x, y)
            }
            else -> {
                // Default to cursor position
                val maxX = maxOf(0, editorBounds.width - indicatorSize.width)
                Point(
                    (cursorPoint.x + 20).coerceIn(0, maxX),
                    (cursorPoint.y - 25).coerceAtLeast(0)
                )
            }
        }
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        hide()
        indicatorScope.cancel()
        indicatorPanel = null
        
        LOG.debug("Input method indicator disposed")
    }
}
