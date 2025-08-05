package com.smartinput.pro.listener

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.smartinput.pro.service.SmartInputConfigService

/**
 * Listens to file editor events and initializes Smart Input Pro for new editors
 */
class EditorManagerListener : FileEditorManagerListener {
    
    companion object {
        private val LOG = Logger.getInstance(EditorManagerListener::class.java)
    }

    private val configService = SmartInputConfigService.getInstance()
    private val editorEventListener = EditorEventListener()

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (!configService.isEnabled()) {
            return
        }

        try {
            // Get the text editor for this file
            val fileEditor = source.getSelectedEditor(file)
            if (fileEditor is TextEditor) {
                val editor = fileEditor.editor
                
                LOG.debug("File opened: ${file.name}, initializing Smart Input Pro for editor")
                editorEventListener.initializeForEditor(editor)
            }
        } catch (e: Exception) {
            LOG.error("Error initializing Smart Input Pro for opened file: ${file.name}", e)
        }
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        try {
            // Find and cleanup the editor for this file
            val fileEditor = source.getSelectedEditor(file)
            if (fileEditor is TextEditor) {
                val editor = fileEditor.editor
                
                LOG.debug("File closed: ${file.name}, cleaning up Smart Input Pro for editor")
                editorEventListener.cleanupForEditor(editor)
            }
        } catch (e: Exception) {
            LOG.error("Error cleaning up Smart Input Pro for closed file: ${file.name}", e)
        }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        if (!configService.isEnabled()) {
            return
        }

        try {
            val newEditor = event.newEditor
            if (newEditor is TextEditor) {
                val editor = newEditor.editor

                LOG.debug("Editor selection changed, ensuring Smart Input Pro is initialized")
                editorEventListener.initializeForEditor(editor)
            }
        } catch (e: Exception) {
            LOG.error("Error handling editor selection change", e)
        }
    }
}
