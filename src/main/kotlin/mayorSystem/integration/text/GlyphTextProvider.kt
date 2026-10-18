package mayorSystem.integration.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

internal interface GlyphTextProvider {
    val available: Boolean
    val resolver: TagResolver

    fun readable(component: Component): Component
}
