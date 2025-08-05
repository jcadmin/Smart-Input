package com.smartinput.pro

import com.smartinput.pro.model.InputMethodType
import com.smartinput.pro.model.ContextType
import com.smartinput.pro.platform.PlatformDetector
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Basic tests for Smart Input Pro functionality
 */
class SmartInputProTest {

    @Test
    fun testInputMethodTypeEnum() {
        assertEquals("English", InputMethodType.ENGLISH.displayName)
        assertEquals("Chinese", InputMethodType.CHINESE.displayName)
        assertEquals("en", InputMethodType.ENGLISH.code)
        assertEquals("zh", InputMethodType.CHINESE.code)
        
        assertEquals(InputMethodType.ENGLISH, InputMethodType.fromCode("en"))
        assertEquals(InputMethodType.CHINESE, InputMethodType.fromCode("zh"))
        assertEquals(InputMethodType.UNKNOWN, InputMethodType.fromCode("invalid"))
    }

    @Test
    fun testContextTypeEnum() {
        assertEquals("Code", ContextType.CODE.displayName)
        assertEquals("Comment", ContextType.COMMENT.displayName)
        assertEquals("String", ContextType.STRING.displayName)
        assertEquals("Documentation", ContextType.DOCUMENTATION.displayName)
        
        assertEquals(ContextType.CODE, ContextType.fromString("CODE"))
        assertEquals(ContextType.COMMENT, ContextType.fromString("comment"))
        assertEquals(ContextType.UNKNOWN, ContextType.fromString("invalid"))
    }

    @Test
    fun testPlatformDetector() {
        val detector = PlatformDetector()
        val platform = detector.getCurrentPlatform()
        val platformInfo = detector.getPlatformInfo()
        
        assertNotNull(platform)
        assertNotNull(platformInfo)
        assertNotNull(platformInfo.osName)
        assertNotNull(platformInfo.osVersion)
        
        // Platform should be one of the supported types
        assertTrue(platform in listOf(
            PlatformDetector.Platform.WINDOWS,
            PlatformDetector.Platform.MACOS,
            PlatformDetector.Platform.LINUX,
            PlatformDetector.Platform.UNKNOWN
        ))
    }

    @Test
    fun testInputMethodManagerCreation() {
        val detector = PlatformDetector()
        val platform = detector.getCurrentPlatform()
        
        // Should not throw exception
        val manager = com.smartinput.pro.platform.InputMethodManager.create(platform)
        assertNotNull(manager)
        
        // Should return supported input methods
        val supportedMethods = manager.getSupportedInputMethods()
        assertNotNull(supportedMethods)
    }

    @Test
    fun testChineseCharacterDetection() {
        assertTrue(containsChinese("你好"))
        assertTrue(containsChinese("Hello 世界"))
        assertTrue(!containsChinese("Hello World"))
        assertTrue(!containsChinese("123456"))
        assertTrue(!containsChinese(""))
    }

    @Test
    fun testEnglishCharacterDetection() {
        assertTrue(containsOnlyEnglish("Hello World"))
        assertTrue(containsOnlyEnglish("test123"))
        assertTrue(containsOnlyEnglish("Hello, World!"))
        assertTrue(!containsOnlyEnglish("Hello 世界"))
        assertTrue(!containsOnlyEnglish("你好"))
    }

    /**
     * Helper method to check if text contains Chinese characters
     */
    private fun containsChinese(text: String): Boolean {
        return text.any { char ->
            char.code in 0x4E00..0x9FFF || // CJK Unified Ideographs
            char.code in 0x3400..0x4DBF || // CJK Extension A
            char.code in 0x20000..0x2A6DF   // CJK Extension B
        }
    }

    /**
     * Helper method to check if text contains only English characters and common symbols
     */
    private fun containsOnlyEnglish(text: String): Boolean {
        return text.all { char ->
            char.isLetterOrDigit() && char.code < 128 || // ASCII letters and digits
            char in " !@#$%^&*()_+-=[]{}|;':\",./<>?" // Common symbols
        }
    }
}
