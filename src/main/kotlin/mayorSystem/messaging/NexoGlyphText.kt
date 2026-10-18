package mayorSystem.messaging

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent

/** Converts registered glyphs to literal labels without reparsing text as markup. */
internal object NexoGlyphText {
    private val defaultFont = Key.key("minecraft:default")

    data class Glyph(val id: String, val font: Key, val unicodes: List<String>)

    fun readable(component: Component, glyphs: Sequence<Glyph>, spacingFont: Key, spacingCharacters: Set<Int>): Component {
        val labels = mutableMapOf<Pair<Key, Int>, String>()
        val requested = mutableSetOf<Int>()
        fun collect(part: Component) {
            if (part is TextComponent) part.content().codePoints().forEach { requested.add(it) }
            part.children().forEach(::collect)
        }
        collect(component)
        if (requested.isEmpty()) return component
        for (glyph in glyphs) {
            for (unicode in glyph.unicodes) {
                unicode.codePoints().forEach { codepoint ->
                    if (codepoint in requested) labels[glyph.font to codepoint] = "[${glyph.id.replace('_', ' ')}]"
                }
            }
        }

        fun convert(part: Component, inheritedFont: Key): Component {
            val font = part.font() ?: inheritedFont
            val children = part.children().map { convert(it, font) }
            if (part !is TextComponent) return part.children(children)
            var output: Component = part.content("").children(emptyList())
            val literal = StringBuilder()
            fun flush() {
                if (literal.isNotEmpty()) {
                    output = output.append(Component.text(literal.toString()))
                    literal.setLength(0)
                }
            }
            part.content().codePoints().forEach { codepoint ->
                val label = labels[font to codepoint]
                when {
                    label != null -> {
                        flush()
                        output = output.append(Component.text(label).font(defaultFont))
                    }
                    font == spacingFont && codepoint in spacingCharacters -> Unit
                    else -> literal.appendCodePoint(codepoint)
                }
            }
            flush()
            return output.children(output.children() + children)
        }
        return convert(component, defaultFont)
    }
}
