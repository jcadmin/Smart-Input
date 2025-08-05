package com.smartinput.pro.analyzer

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.smartinput.pro.model.ContextInfo
import com.smartinput.pro.model.ContextType
import com.smartinput.pro.model.InputMethodType

/**
 * Analyzes the context at a given position in the code to determine
 * the appropriate input method for that context
 */
class ContextAnalyzer {
    
    companion object {
        private val LOG = Logger.getInstance(ContextAnalyzer::class.java)
    }

    /**
     * Analyze the context at the given offset in the PSI file
     */
    fun analyzeContext(psiFile: PsiFile, offset: Int): ContextInfo {
        try {
            val element = psiFile.findElementAt(offset)
            if (element == null) {
                LOG.debug("No PSI element found at offset $offset")
                return ContextInfo(ContextType.UNKNOWN, 0.0f, "No element found")
            }

            return analyzeElement(element, offset)
        } catch (e: Exception) {
            LOG.error("Error analyzing context at offset $offset", e)
            return ContextInfo(ContextType.UNKNOWN, 0.0f, "Analysis error: ${e.message}")
        }
    }

    /**
     * Analyze a specific PSI element to determine its context
     */
    private fun analyzeElement(element: PsiElement, offset: Int): ContextInfo {
        // Check for comments first (highest priority)
        val commentInfo = analyzeCommentContext(element)
        if (commentInfo.type != ContextType.UNKNOWN) {
            return commentInfo
        }

        // Check for string literals
        val stringInfo = analyzeStringContext(element)
        if (stringInfo.type != ContextType.UNKNOWN) {
            return stringInfo
        }

        // Check for documentation
        val docInfo = analyzeDocumentationContext(element)
        if (docInfo.type != ContextType.UNKNOWN) {
            return docInfo
        }

        // Default to code context
        return analyzeCodeContext(element)
    }

    /**
     * Analyze comment context
     */
    private fun analyzeCommentContext(element: PsiElement): ContextInfo {
        // Check if element is within a comment
        val commentElement = PsiTreeUtil.getParentOfType(element, PsiComment::class.java)
        if (commentElement != null) {
            val commentType = when {
                commentElement.text.startsWith("//") -> "line_comment"
                commentElement.text.startsWith("/*") -> "block_comment"
                else -> "comment"
            }
            
            return ContextInfo(
                type = ContextType.COMMENT,
                confidence = 1.0f,
                details = "In $commentType: ${commentElement.text.take(50)}...",
                suggestedInputMethod = InputMethodType.CHINESE
            )
        }

        // Check if element itself is a comment
        if (element is PsiComment) {
            return ContextInfo(
                type = ContextType.COMMENT,
                confidence = 1.0f,
                details = "Comment element: ${element.text.take(50)}...",
                suggestedInputMethod = InputMethodType.CHINESE
            )
        }

        return ContextInfo(ContextType.UNKNOWN)
    }

    /**
     * Analyze string literal context
     */
    private fun analyzeStringContext(element: PsiElement): ContextInfo {
        // Check if element is within a string literal
        val stringParent = findStringLiteralParent(element)
        if (stringParent != null) {
            val stringContent = extractStringContent(stringParent)
            val confidence = calculateStringConfidence(stringContent)
            val suggestedMethod = suggestInputMethodForString(stringContent)
            
            return ContextInfo(
                type = ContextType.STRING,
                confidence = confidence,
                details = "In string literal: ${stringContent.take(30)}...",
                suggestedInputMethod = suggestedMethod
            )
        }

        return ContextInfo(ContextType.UNKNOWN)
    }

    /**
     * Analyze documentation context (JavaDoc, KDoc, etc.)
     */
    private fun analyzeDocumentationContext(element: PsiElement): ContextInfo {
        // Check for documentation patterns in comments
        val parent = element.parent
        if (parent is PsiComment && isDocumentationComment(parent)) {
            return ContextInfo(
                type = ContextType.DOCUMENTATION,
                confidence = 0.9f,
                details = "In documentation comment",
                suggestedInputMethod = InputMethodType.CHINESE
            )
        }

        // Check if element itself is a documentation comment
        if (element is PsiComment && isDocumentationComment(element)) {
            return ContextInfo(
                type = ContextType.DOCUMENTATION,
                confidence = 1.0f,
                details = "Documentation comment",
                suggestedInputMethod = InputMethodType.CHINESE
            )
        }

        return ContextInfo(ContextType.UNKNOWN)
    }

    /**
     * Analyze code context (default case)
     */
    private fun analyzeCodeContext(element: PsiElement): ContextInfo {
        val contextDetails = determineCodeContextDetails(element)
        
        return ContextInfo(
            type = ContextType.CODE,
            confidence = 0.8f,
            details = contextDetails,
            suggestedInputMethod = InputMethodType.ENGLISH
        )
    }

    /**
     * Find string literal parent element
     */
    private fun findStringLiteralParent(element: PsiElement): PsiElement? {
        var current: PsiElement? = element
        while (current != null) {
            if (isStringLiteral(current)) {
                return current
            }
            current = current.parent
        }
        return null
    }

    /**
     * Check if element is a string literal
     */
    private fun isStringLiteral(element: PsiElement): Boolean {
        return when {
            element.text.startsWith("\"") && element.text.endsWith("\"") -> true
            element.text.startsWith("'") && element.text.endsWith("'") -> true
            element.text.startsWith("`") && element.text.endsWith("`") -> true // Template strings
            element.toString().contains("STRING_LITERAL") -> true
            else -> false
        }
    }

    /**
     * Extract content from string literal
     */
    private fun extractStringContent(stringElement: PsiElement): String {
        val text = stringElement.text
        return try {
            when {
                text.length >= 2 && text.startsWith("\"") && text.endsWith("\"") ->
                    text.substring(1, text.length - 1)
                text.length >= 2 && text.startsWith("'") && text.endsWith("'") ->
                    text.substring(1, text.length - 1)
                text.length >= 2 && text.startsWith("`") && text.endsWith("`") ->
                    text.substring(1, text.length - 1)
                else -> text
            }
        } catch (e: StringIndexOutOfBoundsException) {
            LOG.warn("Error extracting string content from: '$text'", e)
            text
        }
    }

    /**
     * Calculate confidence for string context based on content
     */
    private fun calculateStringConfidence(content: String): Float {
        return when {
            content.isEmpty() -> 0.5f
            content.length < 5 -> 0.6f
            containsChinese(content) -> 1.0f
            containsOnlyEnglish(content) -> 0.9f
            else -> 0.7f
        }
    }

    /**
     * Suggest input method for string content
     */
    private fun suggestInputMethodForString(content: String): InputMethodType {
        return when {
            containsChinese(content) -> InputMethodType.CHINESE
            containsOnlyEnglish(content) -> InputMethodType.ENGLISH
            else -> InputMethodType.CHINESE // Default to Chinese for mixed content
        }
    }

    /**
     * Check if text contains Chinese characters
     */
    private fun containsChinese(text: String): Boolean {
        return text.any { char ->
            char.code in 0x4E00..0x9FFF || // CJK Unified Ideographs
            char.code in 0x3400..0x4DBF || // CJK Extension A
            char.code in 0x20000..0x2A6DF   // CJK Extension B
        }
    }

    /**
     * Check if text contains only English characters and common symbols
     */
    private fun containsOnlyEnglish(text: String): Boolean {
        return text.all { char ->
            char.isLetterOrDigit() && char.code < 128 || // ASCII letters and digits
            char in " !@#$%^&*()_+-=[]{}|;':\",./<>?" // Common symbols
        }
    }

    /**
     * Check if comment is documentation
     */
    private fun isDocumentationComment(comment: PsiComment): Boolean {
        val text = comment.text
        return text.startsWith("/**") || 
               text.startsWith("/*!") ||
               text.contains("@param") ||
               text.contains("@return") ||
               text.contains("@author") ||
               text.contains("@since")
    }

    /**
     * Determine specific code context details
     */
    private fun determineCodeContextDetails(element: PsiElement): String {
        val parent = element.parent
        return when {
            parent?.toString()?.contains("IDENTIFIER") == true -> "Identifier"
            parent?.toString()?.contains("METHOD") == true -> "Method"
            parent?.toString()?.contains("CLASS") == true -> "Class"
            parent?.toString()?.contains("VARIABLE") == true -> "Variable"
            parent?.toString()?.contains("KEYWORD") == true -> "Keyword"
            else -> "Code element: ${element.javaClass.simpleName}"
        }
    }
}
