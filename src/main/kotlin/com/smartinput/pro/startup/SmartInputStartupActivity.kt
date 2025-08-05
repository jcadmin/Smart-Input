package com.smartinput.pro.startup

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.smartinput.pro.service.SmartInputConfigService
import com.smartinput.pro.service.InputMethodService
import com.smartinput.pro.platform.PlatformDetector
import com.smartinput.pro.listener.EditorEventListener

/**
 * Startup activity that initializes Smart Input Pro when a project is opened
 */
class SmartInputStartupActivity : ProjectActivity {
    
    companion object {
        private val LOG = Logger.getInstance(SmartInputStartupActivity::class.java)
    }

    override suspend fun execute(project: Project) {
        LOG.info("Starting Smart Input Pro initialization for project: ${project.name}")
        
        try {
            // Initialize services
            initializeServices(project)
            
            // Check platform compatibility
            checkPlatformCompatibility()
            
            // Setup editor listeners
            setupEditorListeners(project)
            
            // Validate configuration
            validateConfiguration()
            
            LOG.info("Smart Input Pro successfully initialized for project: ${project.name}")
            
        } catch (e: Exception) {
            LOG.error("Failed to initialize Smart Input Pro for project: ${project.name}", e)
        }
    }

    /**
     * Initialize all required services
     */
    private fun initializeServices(project: Project) {
        LOG.debug("Initializing Smart Input Pro services...")
        
        // Initialize configuration service
        val configService = SmartInputConfigService.getInstance()
        configService.validateAndFix()
        
        // Initialize input method service
        val inputMethodService = InputMethodService.getInstance(project)
        
        LOG.debug("Services initialized successfully")
    }

    /**
     * Check if the current platform is supported
     */
    private fun checkPlatformCompatibility() {
        val platformDetector = PlatformDetector()
        val platformInfo = platformDetector.getPlatformInfo()
        
        LOG.info("Platform detected: $platformInfo")
        
        if (!platformInfo.isSupported) {
            LOG.warn("Current platform is not fully supported: ${platformInfo.platform}")
        }
        
        // Log recommended configuration for this platform
        val recommendedConfig = platformDetector.getRecommendedConfig()
        LOG.debug("Recommended configuration: $recommendedConfig")
    }

    /**
     * Setup editor event listeners
     */
    private fun setupEditorListeners(project: Project) {
        LOG.debug("Setting up editor event listeners...")
        
        // The actual listener registration is handled by the applicationListeners
        // section in plugin.xml, but we can perform additional setup here if needed
        
        LOG.debug("Editor event listeners setup completed")
    }

    /**
     * Validate and log configuration status
     */
    private fun validateConfiguration() {
        val configService = SmartInputConfigService.getInstance()
        
        LOG.info("Smart Input Pro configuration:")
        LOG.info("  Enabled: ${configService.isEnabled()}")
        LOG.info("  Auto Switch Mode: ${configService.isAutoSwitchMode()}")
        LOG.info("  Show Indicator: ${configService.isShowIndicator()}")
        LOG.info("  Switch in Code Areas: ${configService.shouldSwitchInCodeAreas()}")
        LOG.info("  Switch in Comments: ${configService.shouldSwitchInComments()}")
        LOG.info("  Switch in Strings: ${configService.shouldSwitchInStrings()}")
        LOG.info("  Switch in Documentation: ${configService.shouldSwitchInDocumentation()}")
        LOG.info("  Detection Delay: ${configService.getDetectionDelay()}ms")
        LOG.info("  Switch Delay: ${configService.getSwitchDelay()}ms")
        LOG.info("  Debug Mode: ${configService.isDebugMode()}")
        
        if (configService.isDebugMode()) {
            LOG.info("Debug mode is enabled - additional logging will be available")
        }
    }
}
