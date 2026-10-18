package mayorSystem.integration.text

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mayorSystem.messaging.DisplayTextParser
import org.bukkit.Server
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import java.net.URLClassLoader
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NexoTextIntegrationTest {
    @AfterTest
    fun clearProvider() {
        DisplayTextParser.useGlyphProvider(null)
    }

    @Test
    fun `missing optional plugin enables without loading any Nexo class`() {
        val pluginManager = mockk<PluginManager>(relaxed = true)
        val server = mockk<Server> { every { getPluginManager() } returns pluginManager }
        val plugin = mockk<JavaPlugin> { every { getServer() } returns server }
        every { pluginManager.getPlugin("Nexo") } returns null

        // Load the lifecycle adapter with a classloader that cannot see Nexo, even though tests can.
        val className = NexoTextIntegration::class.java.name
        val location = NexoTextIntegration::class.java.protectionDomain.codeSource.location
        URLClassLoader(arrayOf(location), javaClass.classLoader).use { parent ->
            val loader = object : URLClassLoader(arrayOf(location), parent) {
                override fun loadClass(name: String, resolve: Boolean): Class<*> {
                    if (name.startsWith("com.nexomc.")) throw ClassNotFoundException(name)
                    if (name.startsWith("mayorSystem.integration.text.Nexo")) {
                        return findLoadedClass(name) ?: findClass(name).also { if (resolve) resolveClass(it) }
                    }
                    return super.loadClass(name, resolve)
                }
            }
            loader.use {
                val type = it.loadClass(className)
                val integration = type.getConstructor(JavaPlugin::class.java).newInstance(plugin)
                type.getMethod("enable").invoke(integration)
                type.getMethod("disable").invoke(integration)
            }
        }

        verify(exactly = 1) { pluginManager.registerEvents(any(), plugin) }
        assertEquals("<glyph:town_badge>", DisplayTextParser.plain("<glyph:town_badge>"))
    }
}
