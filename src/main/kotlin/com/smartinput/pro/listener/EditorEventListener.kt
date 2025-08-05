package com.smartinput.pro.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiDocumentManager
import com.smartinput.pro.service.SmartInputConfigService
import com.smartinput.pro.service.InputMethodService
import com.smartinput.pro.analyzer.ContextAnalyzer
import com.smartinput.pro.model.ContextInfo
import com.smartinput.pro.indicator.InputMethodIndicator
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Listens to editor events and triggers input method switching based on cursor context
 */
class EditorEventListener {
    
    companion object {
        private val LOG = Logger.getInstance(EditorEventListener::class.java)
        private const val DEBOUNCE_DELAY = 150L // milliseconds
    }

    private val configService = SmartInputConfigService.getInstance()
    private val contextAnalyzer = ContextAnalyzer()
    private val indicators = ConcurrentHashMap<Editor, InputMethodIndicator>()
    
    // Coroutine scope for debounced operations
    private val listenerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val debounceJobs = ConcurrentHashMap<Editor, Job>()



    /**
     * Initialize the listener for a specific editor
     */
    fun initializeForEditor(editor: Editor) {
        if (!configService.isEnabled()) {
            return
        }

        LOG.debug("Initializing Smart Input Pro for editor: ${editor.document.text.take(50)}...")

        // Add caret listener
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                handleCaretPositionChanged(event)
            }
        })

        // Add document listener
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                handleDocumentChanged(event)
            }
        })

        // Add focus listener
        editor.contentComponent.addFocusListener(object : java.awt.event.FocusListener {
            override fun focusGained(e: java.awt.event.FocusEvent?) {
                handleEditorFocusGained(editor)
            }

            override fun focusLost(e: java.awt.event.FocusEvent?) {
                handleEditorFocusLost(editor)
            }
        })

        // Initialize indicator if enabled
        if (configService.isShowIndicator()) {
            val indicator = InputMethodIndicator(editor)
            indicators[editor] = indicator
        }

        LOG.debug("Smart Input Pro initialized for editor")
    }

    /**
     * Handle caret position changes
     */
    private fun handleCaretPositionChanged(event: CaretEvent) {
        if (!configService.isEnabled() || !configService.isAutoSwitchMode()) {
            return
        }

        val editor = event.editor
        val project = editor.project ?: return

        // Cancel previous debounce job for this editor
        debounceJobs[editor]?.cancel()

        // Create new debounced job
        debounceJobs[editor] = listenerScope.launch {
            delay(DEBOUNCE_DELAY)
            
            try {
                processCaretPositionChange(editor, project, event.newPosition)
            } catch (e: Exception) {
                LOG.error("Error processing caret position change", e)
            }
        }
    }

    /**
     * Process caret position change with context analysis
     */
    private suspend fun processCaretPositionChange(editor: Editor, project: Project, caretPosition: com.intellij.openapi.editor.LogicalPosition) {
        if (configService.isDebugMode()) {
            LOG.debug("Processing caret position change: line=${caretPosition.line}, column=${caretPosition.column}")
        }

        // 使用ReadAction确保线程安全访问PSI
        val contextInfo = ApplicationManager.getApplication().runReadAction<ContextInfo?> {
            try {
                // Get PSI file
                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return@runReadAction null

                // Analyze context at caret position
                val offset = editor.logicalPositionToOffset(caretPosition)
                contextAnalyzer.analyzeContext(psiFile, offset)
            } catch (e: Exception) {
                LOG.error("Error analyzing context in ReadAction", e)
                null
            }
        }

        if (contextInfo == null) return

        if (configService.isDebugMode()) {
            LOG.debug("Context analysis result: $contextInfo")
        }

        // Get input method service
        val inputMethodService = InputMethodService.getInstance(project)

        // Determine preferred input method for this context
        val preferredInputMethod = inputMethodService.getPreferredInputMethod(contextInfo.type.name.lowercase())

        // Switch input method if needed
        if (preferredInputMethod != inputMethodService.getCurrentInputMethod()) {
            inputMethodService.switchToInputMethod(preferredInputMethod, contextInfo.type.name.lowercase())
        }

        // Update indicator
        indicators[editor]?.updateInputMethod(preferredInputMethod, contextInfo)
    }

    /**
     * Handle document changes
     */
    private fun handleDocumentChanged(event: DocumentEvent) {
        if (!configService.isEnabled()) {
            return
        }

        // We might want to re-analyze context after significant document changes
        // For now, we'll just log debug information
        if (configService.isDebugMode()) {
            LOG.debug("Document changed: offset=${event.offset}, oldLength=${event.oldLength}, newLength=${event.newLength}")
        }
    }

    /**
     * Handle editor gaining focus
     */
    private fun handleEditorFocusGained(editor: Editor) {
        if (!configService.isEnabled()) {
            return
        }

        LOG.debug("Editor gained focus")
        
        // Show indicator if configured
        indicators[editor]?.show()
        
        // Trigger immediate context analysis
        val project = editor.project ?: return
        val caretPosition = editor.caretModel.logicalPosition
        
        listenerScope.launch {
            try {
                processCaretPositionChange(editor, project, caretPosition)
            } catch (e: Exception) {
                LOG.error("Error processing editor focus gain", e)
            }
        }
    }

    /**
     * Handle editor losing focus
     */
    private fun handleEditorFocusLost(editor: Editor) {
        if (!configService.isEnabled()) {
            return
        }

        LOG.debug("Editor lost focus")
        
        // Hide indicator
        indicators[editor]?.hide()
        
        // Cancel any pending debounce jobs for this editor
        debounceJobs[editor]?.cancel()
        debounceJobs.remove(editor)
    }

    /**
     * Cleanup resources for an editor
     */
    fun cleanupForEditor(editor: Editor) {
        // Cancel debounce job
        debounceJobs[editor]?.cancel()
        debounceJobs.remove(editor)
        
        // Dispose indicator
        indicators[editor]?.dispose()
        indicators.remove(editor)
        
        LOG.debug("Cleaned up Smart Input Pro for editor")
    }

    /**
     * Dispose all resources
     */
    fun dispose() {
        // Cancel all debounce jobs
        debounceJobs.values.forEach { it.cancel() }
        debounceJobs.clear()
        
        // Dispose all indicators
        indicators.values.forEach { it.dispose() }
        indicators.clear()
        
        // Cancel coroutine scope
        listenerScope.cancel()
        
        LOG.info("Smart Input Pro editor event listener disposed")
    }
}
