package mayorSystem.integration.text

import mayorSystem.messaging.DisplayTextParser
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

/** Owns optional Nexo linkage and releases the provider when either plugin stops. */
internal class NexoTextIntegration(private val plugin: JavaPlugin) : Listener {
    fun enable() {
        refresh()
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun disable() {
        DisplayTextParser.useGlyphProvider(null)
    }

    @EventHandler
    fun onPluginEnable(event: PluginEnableEvent) {
        if (event.plugin.name == "Nexo") refresh()
    }

    @EventHandler
    fun onPluginDisable(event: PluginDisableEvent) {
        if (event.plugin.name == "Nexo") disable()
    }

    private fun refresh() {
        val nexo = plugin.server.pluginManager.getPlugin("Nexo")
        if (nexo?.isEnabled != true) {
            disable()
            return
        }
        try {
            DisplayTextParser.useGlyphProvider(NexoGlyphTextProvider { nexo.isEnabled })
        } catch (error: LinkageError) {
            disable()
            plugin.logger.log(Level.WARNING, "Nexo glyph support could not load. Check the installed Nexo version.", error)
        }
    }
}
