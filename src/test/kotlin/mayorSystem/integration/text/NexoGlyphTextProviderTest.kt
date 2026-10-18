package mayorSystem.integration.text

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NexoGlyphTextProviderTest {
    @Test
    fun `adapter exposes Nexo official glyph and spacing resolvers only`() {
        val provider = NexoGlyphTextProvider { true }

        assertTrue(provider.resolver.has("glyph"))
        assertTrue(provider.resolver.has("g"))
        assertTrue(provider.resolver.has("shift"))
        assertTrue(provider.resolver.has("s"))
        assertFalse(provider.resolver.has("papi"))
        assertFalse(provider.resolver.has("click"))
    }

    @Test
    fun `adapter observes Nexo availability instead of retaining enabled state`() {
        var enabled = true
        val provider = NexoGlyphTextProvider { enabled }
        assertTrue(provider.available)
        enabled = false
        assertFalse(provider.available)
    }
}
