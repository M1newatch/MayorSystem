package mayorSystem.integration.text

import com.nexomc.nexo.NexoPlugin
import com.nexomc.nexo.glyphs.GlyphTag
import com.nexomc.nexo.glyphs.ShiftTag
import com.nexomc.nexo.glyphs.Shift
import mayorSystem.messaging.NexoGlyphText
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/** Loaded only when Nexo is enabled. Nexo remains the authority for every glyph. */
internal class NexoGlyphTextProvider(private val enabled: () -> Boolean) : GlyphTextProvider {
    override val available: Boolean get() = enabled()
    override val resolver: TagResolver = TagResolver.resolver(GlyphTag.RESOLVER, ShiftTag.RESOLVER)

    override fun readable(component: Component): Component = NexoGlyphText.readable(
        component,
        NexoPlugin.instance().fontManager().glyphs().asSequence().map { glyph ->
            NexoGlyphText.Glyph(glyph.id, glyph.font, glyph.unicodes)
        },
        ShiftTag.FONT,
        Shift.fontProvider.advances().keys.flatMap { it.codePoints().toArray().asIterable() }.toSet()
    )
}
