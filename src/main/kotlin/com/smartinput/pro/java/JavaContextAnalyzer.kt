package com.smartinput.pro.java

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import com.smartinput.pro.model.ContextInfo
import com.smartinput.pro.model.ContextType
import com.smartinput.pro.model.InputMethodType

/**
 * Java-specific context analyzer that provides enhanced analysis for Java code
 */
class JavaContextAnalyzer : DocumentationProvider {
    
    companion object {
        private val LOG = Logger.getInstance(JavaContextAnalyzer::class.java)
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        return null // We don't provide quick navigation info
    }

    override fun getUrlFor(element: PsiElement?, originalElement: PsiElement?): MutableList<String>? {
        return null // We don't provide URLs
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        return null // We don't generate documentation
    }

    override fun getDocumentationElementForLookupItem(psiManager: PsiManager?, `object`: Any?, element: PsiElement?): PsiElement? {
        return null // We don't provide documentation elements for lookup
    }

    override fun getDocumentationElementForLink(psiManager: PsiManager?, link: String?, context: PsiElement?): PsiElement? {
        return null // We don't handle documentation links
    }

    /**
     * Analyze Java-specific context at the given element
     */
    fun analyzeJavaContext(element: PsiElement, offset: Int): ContextInfo {
        try {
            return when {
                isInJavaDoc(element) -> analyzeJavaDocContext(element)
                isInJavaComment(element) -> analyzeJavaCommentContext(element)
                isInJavaString(element) -> analyzeJavaStringContext(element)
                isInJavaAnnotation(element) -> analyzeJavaAnnotationContext(element)
                isInJavaIdentifier(element) -> analyzeJavaIdentifierContext(element)
                else -> analyzeGeneralJavaContext(element)
            }
        } catch (e: Exception) {
            LOG.error("Error analyzing Java context", e)
            return ContextInfo(ContextType.UNKNOWN, 0.0f, "Analysis error: ${e.message}")
        }
    }

    /**
     * Check if element is in JavaDoc
     */
    private fun isInJavaDoc(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is PsiDocComment) return true
            current = current.parent
        }
        return false
    }

    /**
     * Check if element is in Java comment
     */
    private fun isInJavaComment(element: PsiElement): Boolean {
        return element is PsiComment || element.parent is PsiComment
    }

    /**
     * Check if element is in Java string literal
     */
    private fun isInJavaString(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is PsiLiteralExpression && current.type?.equalsToText("java.lang.String") == true) {
                return true
            }
            current = current.parent
        }
        return false
    }

    /**
     * Check if element is in Java annotation
     */
    private fun isInJavaAnnotation(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current is PsiAnnotation) return true
            current = current.parent
        }
        return false
    }

    /**
     * Check if element is a Java identifier
     */
    private fun isInJavaIdentifier(element: PsiElement): Boolean {
        return element is PsiIdentifier || 
               (element.parent is PsiIdentifier) ||
               (element.text.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*")))
    }

    /**
     * Analyze JavaDoc context
     */
    private fun analyzeJavaDocContext(element: PsiElement): ContextInfo {
        val docComment = findParentOfType<PsiDocComment>(element)
        val content = docComment?.text ?: ""
        
        return ContextInfo(
            type = ContextType.DOCUMENTATION,
            confidence = 1.0f,
            details = "JavaDoc comment: ${content.take(50)}...",
            suggestedInputMethod = InputMethodType.CHINESE
        )
    }

    /**
     * Analyze Java comment context
     */
    private fun analyzeJavaCommentContext(element: PsiElement): ContextInfo {
        val comment = element as? PsiComment ?: element.parent as? PsiComment
        val commentText = comment?.text ?: ""
        
        val commentType = when {
            commentText.startsWith("//") -> "line comment"
            commentText.startsWith("/*") -> "block comment"
            else -> "comment"
        }
        
        return ContextInfo(
            type = ContextType.COMMENT,
            confidence = 1.0f,
            details = "Java $commentType: ${commentText.take(50)}...",
            suggestedInputMethod = InputMethodType.CHINESE
        )
    }

    /**
     * Analyze Java string context
     */
    private fun analyzeJavaStringContext(element: PsiElement): ContextInfo {
        val stringLiteral = findParentOfType<PsiLiteralExpression>(element)
        val stringValue = stringLiteral?.value as? String ?: ""
        
        val suggestedMethod = when {
            containsChinese(stringValue) -> InputMethodType.CHINESE
            stringValue.isEmpty() -> InputMethodType.CHINESE
            containsOnlyEnglish(stringValue) -> InputMethodType.ENGLISH
            else -> InputMethodType.CHINESE
        }
        
        return ContextInfo(
            type = ContextType.STRING,
            confidence = 0.9f,
            details = "Java string literal: ${stringValue.take(30)}...",
            suggestedInputMethod = suggestedMethod
        )
    }

    /**
     * Analyze Java annotation context
     */
    private fun analyzeJavaAnnotationContext(element: PsiElement): ContextInfo {
        val annotation = findParentOfType<PsiAnnotation>(element)
        val annotationName = annotation?.qualifiedName ?: "unknown"
        
        return ContextInfo(
            type = ContextType.CODE,
            confidence = 0.9f,
            details = "Java annotation: @$annotationName",
            suggestedInputMethod = InputMethodType.ENGLISH
        )
    }

    /**
     * Analyze Java identifier context
     */
    private fun analyzeJavaIdentifierContext(element: PsiElement): ContextInfo {
        val parent = element.parent
        val contextType = when (parent) {
            is PsiMethod -> "method name"
            is PsiClass -> "class name"
            is PsiField -> "field name"
            is PsiVariable -> "variable name"
            is PsiParameter -> "parameter name"
            else -> "identifier"
        }
        
        return ContextInfo(
            type = ContextType.CODE,
            confidence = 0.95f,
            details = "Java $contextType: ${element.text}",
            suggestedInputMethod = InputMethodType.ENGLISH
        )
    }

    /**
     * Analyze general Java context
     */
    private fun analyzeGeneralJavaContext(element: PsiElement): ContextInfo {
        val elementType = element.javaClass.simpleName
        
        return ContextInfo(
            type = ContextType.CODE,
            confidence = 0.7f,
            details = "Java code element: $elementType",
            suggestedInputMethod = InputMethodType.ENGLISH
        )
    }

    /**
     * Find parent of specific type
     */
    private inline fun <reified T : PsiElement> findParentOfType(element: PsiElement): T? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is T) return current
            current = current.parent
        }
        return null
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
}
