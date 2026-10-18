package mayorSystem.messaging

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NexoGlyphTextTest {
    private val font = Key.key("guild:icons")
    private val otherFont = Key.key("other:icons")
    private val spacingFont = Key.key("guild:spacing")
    private val plain = PlainTextComponentSerializer.plainText()

    @Test
    fun `fallback uses inherited font and keeps the same codepoint in other fonts untouched`() {
        val title = Component.text("\uE123").font(font)
            .append(Component.text("\uE123").font(otherFont))
        val result = fallback(title, NexoGlyphText.Glyph("guild_badge", font, listOf("\uE123")))

        assertEquals("[guild badge]\uE123", plain.serialize(result))
    }

    @Test
    fun `supplementary glyphs and multiple bitmap cells use loaded definitions`() {
        val astral = String(Character.toChars(0xF1234))
        val title = Component.text(astral + "\uE124\uE125").font(font)
        val result = fallback(title, NexoGlyphText.Glyph("guild_banner", font, listOf(astral, "\uE124\uE125")))

        assertEquals("[guild banner][guild banner][guild banner]", plain.serialize(result))
    }

    @Test
    fun `literal markup stays literal and surrounding component styles survive`() {
        val title = Component.text("<red>\uE123 👑").color(NamedTextColor.GOLD).font(font)
        val result = fallback(title, NexoGlyphText.Glyph("guild_badge", font, listOf("\uE123")))

        assertEquals("<red>[guild badge] 👑", plain.serialize(result))
        assertEquals(NamedTextColor.GOLD, result.color())
        assertNull(result.clickEvent())
    }

    @Test
    fun `unregistered private use text survives without invented labels`() {
        val result = fallback(Component.text("\uE2B6 👑"))
        assertEquals("\uE2B6 👑", plain.serialize(result))
    }

    @Test
    fun `spacing font may also contain ordinary registered glyphs`() {
        val title = Component.text("\uE123").font(spacingFont)
        val result = fallback(title, NexoGlyphText.Glyph("guild_badge", spacingFont, listOf("\uE123")))
        assertEquals("[guild badge]", plain.serialize(result))
    }

    @Test
    fun `only registered spacing characters disappear even outside the private use range`() {
        val title = Component.text("X\uF801\uE999").font(spacingFont)
        val result = NexoGlyphText.readable(title, emptySequence(), spacingFont, setOf('X'.code, 0xF801))
        assertEquals("\uE999", plain.serialize(result))
    }

    private fun fallback(component: Component, vararg glyphs: NexoGlyphText.Glyph): Component =
        NexoGlyphText.readable(component, glyphs.asSequence(), spacingFont, setOf(0xF801))
}
