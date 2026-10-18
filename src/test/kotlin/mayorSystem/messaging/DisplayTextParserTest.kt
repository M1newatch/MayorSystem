package mayorSystem.messaging

import kotlin.test.Test
import kotlin.test.AfterTest
import mayorSystem.integration.text.GlyphTextProvider
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisplayTextParserTest {
    private val font = Key.key("town:badges")
    private val spacingFont = Key.key("town:spacing")
    private var glyphCharacter = "\uE345"
    private var enabled = true
    private var glyphArguments: List<String> = emptyList()

    @AfterTest
    fun clearProvider() {
        DisplayTextParser.useGlyphProvider(null)
    }

    private fun installGlyphs() {
        DisplayTextParser.useGlyphProvider(object : GlyphTextProvider {
            override val available: Boolean get() = enabled
            override val resolver: TagResolver = TagResolver.resolver(
                TagResolver.resolver("glyph") { arguments, _ ->
                    val id = arguments.popOr("glyph ID").value()
                    glyphArguments = buildList { while (arguments.hasNext()) add(arguments.pop().value()) }
                    if (id != "town_badge") null else Tag.selfClosingInserting(
                        Component.text(glyphCharacter).font(font).color(NamedTextColor.WHITE)
                    )
                },
                TagResolver.resolver("shift") { _, _ ->
                    Tag.selfClosingInserting(Component.text("\uF801").font(spacingFont))
                }
            )

            override fun readable(component: Component): Component = NexoGlyphText.readable(
                component,
                sequenceOf(NexoGlyphText.Glyph("town_badge", font, listOf(glyphCharacter))),
                spacingFont,
                setOf(0xF801)
            )
        })
    }

    @Test
    fun `missing Nexo never invents a server specific rank glyph`() {
        assertEquals("<glyph:rank_owner> Alex", DisplayTextParser.plain("<glyph:rank_owner> Alex"))
    }

    @Test
    fun `a private use character is not assumed to be a StarForged rank`() {
        assertEquals("\uE2B6 Alex", DisplayTextParser.plain("<font:another:icons>\uE2B6</font> Alex"))
    }

    @Test
    fun `ampersand hex shorthand renders without leaking raw marker`() {
        val raw = "&#04b5ff[LEGEND]"

        assertEquals("[LEGEND]", DisplayTextParser.plain(raw))
        assertFalse(DisplayTextParser.mini(raw).contains("&#04b5ff", ignoreCase = true))
    }

    @Test
    fun `uppercase ampersand hex shorthand renders display text`() {
        val raw = "&#FF0000[PRESIDENT] offline"

        assertEquals("[PRESIDENT] offline", DisplayTextParser.plain(raw))
        assertFalse(DisplayTextParser.mini(raw).contains("&#FF0000", ignoreCase = true))
    }

    @Test
    fun `classic ampersand legacy color renders display text`() {
        assertEquals("[Mayor]", DisplayTextParser.plain("&6[Mayor]"))
    }

    @Test
    fun `section legacy color renders display text`() {
        assertEquals("[Mayor]", DisplayTextParser.plain("\u00A76[Mayor]"))
    }

    @Test
    fun `unusual repeated hex legacy color renders display text`() {
        assertEquals("[LEGEND]", DisplayTextParser.plain("&x&0&4&b&5&f&f[LEGEND]"))
    }

    @Test
    fun `minimessage color renders display text`() {
        assertEquals("[LEGEND]", DisplayTextParser.plain("<#04b5ff>[LEGEND]"))
    }

    @Test
    fun `minimessage takes priority over legacy markers inside minimessage text`() {
        val rendered = DisplayTextParser.mini("<#04b5ff>[LEGEND] &l")

        assertFalse(rendered.contains("&#04b5ff", ignoreCase = true))
        assertEquals("[LEGEND] &l", DisplayTextParser.plain("<#04b5ff>[LEGEND] &l"))
    }

    @Test
    fun `invalid minimessage falls back to preserved plain text`() {
        val plain = DisplayTextParser.plain("<not_a_real_tag>Admin</not_a_real_tag>")

        assertEquals("<not_a_real_tag>Admin</not_a_real_tag>", plain)
    }

    @Test
    fun `plain comparison text preserves angle brackets`() {
        assertEquals("3 < 5 > 1", DisplayTextParser.plain("3 < 5 > 1"))
    }

    @Test
    fun `arbitrary glyph uses the installed provider font and character`() {
        installGlyphs()
        val rendered = DisplayTextParser.mini("<glyph:town_badge> <#67E8F9>Alex")

        assertTrue(rendered.contains("town:badges"))
        assertTrue(rendered.contains(glyphCharacter))
        assertFalse(rendered.contains("<glyph:"))
        assertEquals("[town badge] Alex", DisplayTextParser.plain("<glyph:town_badge> <#67E8F9>Alex"))
    }

    @Test
    fun `bedrock inventory titles replace arbitrary glyphs and spacing with readable text`() {
        installGlyphs()
        val javaTitle = DisplayTextParser.component(
            "<gradient:#f7971e:#ffd200>👑 <shift:-8><glyph:town_badge> <#67E8F9>Alex</gradient>"
        )
        val bedrockTitle = DisplayTextParser.inventoryTitle(javaTitle, bedrock = true)
        val serialized = DisplayTextParser.mini(bedrockTitle)

        assertEquals("👑 [town badge] Alex", PlainTextComponentSerializer.plainText().serialize(bedrockTitle))
        assertFalse(serialized.contains(glyphCharacter), serialized)
        assertFalse(serialized.contains("\uF801"))
        assertFalse(serialized.contains("<glyph:"))
    }

    @Test
    fun `java inventory titles preserve the rendered nexo glyph`() {
        installGlyphs()
        val title = DisplayTextParser.component("<glyph:town_badge> Alex")
        val javaTitle = DisplayTextParser.inventoryTitle(title, bedrock = false)

        assertEquals(title, javaTitle)
        assertTrue(DisplayTextParser.mini(javaTitle).contains("town:badges"))
        assertTrue(DisplayTextParser.mini(javaTitle).contains(glyphCharacter))
    }

    @Test
    fun `glyph reload affects the very next render without restarting MayorSystem`() {
        installGlyphs()
        val raw = "<glyph:town_badge> Alex"
        assertTrue(DisplayTextParser.mini(raw).contains("\uE345"))

        glyphCharacter = "\uE678"

        assertTrue(DisplayTextParser.mini(raw).contains("\uE678"))
        assertFalse(DisplayTextParser.mini(raw).contains("\uE345"))
        assertEquals("[town badge] Alex", DisplayTextParser.plain(raw))
    }

    @Test
    fun `bitmap colorable and shadow arguments reach the glyph provider unchanged`() {
        installGlyphs()
        DisplayTextParser.component("<glyph:town_badge:2..3:colorable:shadow:#aaffffff>")
        assertEquals(listOf("2..3", "colorable", "shadow", "#aaffffff"), glyphArguments)
    }

    @Test
    fun `shared template parsers see optional provider changes after their construction`() {
        val parser = DisplayTextParser.miniMessage()
        val raw = "<glyph:town_badge> Alex"
        assertEquals(raw, PlainTextComponentSerializer.plainText().serialize(parser.deserialize(raw)))
        installGlyphs()
        assertTrue(DisplayTextParser.mini(parser.deserialize(raw)).contains(glyphCharacter))
        enabled = false
        assertEquals(raw, PlainTextComponentSerializer.plainText().serialize(parser.deserialize(raw)))
        DisplayTextParser.useGlyphProvider(null)
        assertEquals(raw, DisplayTextParser.plain(raw))
    }

    @Test
    fun `escaped tags and external placeholder values stay literal with Nexo enabled`() {
        installGlyphs()
        val untrusted = "<glyph:town_badge><click:run_command:'/example'>Hello</click>"
        val template = "<green><glyph:town_badge> " + MiniMessageSafety.escapeUntrustedMiniMessage(untrusted)
        val component = DisplayTextParser.component(template)
        val title = DisplayTextParser.inventoryTitle(component, bedrock = true)

        assertEquals("[town badge] $untrusted", DisplayTextParser.plain(title))
        fun assertNoAction(part: Component) {
            kotlin.test.assertNull(part.clickEvent())
            part.children().forEach(::assertNoAction)
        }
        assertNoAction(title)
    }

    @Test
    fun `unparsed glyph tags and unknown glyphs stay intact`() {
        installGlyphs()
        assertEquals("<glyph:town_badge>", DisplayTextParser.plain("<glyph:town_badge>", parseMiniMessage = false))
        assertEquals("<glyph:unknown>", DisplayTextParser.plain("<glyph:unknown>"))
    }

    @Test
    fun `broadcast templates use the shared glyph resolver`() {
        installGlyphs()
        assertEquals("[town badge] Hello", DisplayTextParser.plain(MayorBroadcasts.deserialize("<glyph:town_badge> Hello")))
    }
}
