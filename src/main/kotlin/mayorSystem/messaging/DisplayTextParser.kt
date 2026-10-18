package mayorSystem.messaging

import mayorSystem.integration.text.GlyphTextProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale

/**
 * Pure display-text parser for trusted MayorSystem display config and
 * integration-provided legacy text. Optional glyph handling stays behind a provider.
 */
object DisplayTextParser {
    private const val MAX_CACHE_SIZE = 512

    @Volatile
    private var glyphProvider: GlyphTextProvider? = null
    // Bedrock conversion edits component children; serialized virtual tags would retain their original glyphs.
    private val mini = MiniMessage.builder().emitVirtuals(false).editTags { tags ->
        tags.resolver(object : TagResolver {
            override fun has(name: String): Boolean =
                glyphProvider?.takeIf { it.available }?.resolver?.has(name) == true

            override fun resolve(name: String, arguments: ArgumentQueue, context: Context): Tag? =
                glyphProvider?.takeIf { it.available }?.resolver?.resolve(name, arguments, context)
        })
    }.build()
    private val legacyAmpersand = LegacyComponentSerializer.builder()
        .character('&')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build()
    private val legacySection = LegacyComponentSerializer.builder()
        .character('\u00A7')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build()
    private val plainSerializer = PlainTextComponentSerializer.plainText()

    private val ampersandHexPattern = Regex("(?i)&\\#([0-9a-f]{6})")
    private val sectionHexPattern = Regex("(?i)\u00A7\\#([0-9a-f]{6})")
    private val ampersandLegacyPattern = Regex("(?i)&(?:#[0-9a-f]{6}|[0-9a-fk-orx])")
    private val sectionLegacyPattern = Regex("(?i)\u00A7(?:#[0-9a-f]{6}|[0-9a-fk-orx])")
    private val miniLikeTagPattern = Regex("</?[a-zA-Z0-9_:#-]+(?:[: ][^>]*)?>")
    private val componentCache: MutableMap<CacheKey, Component> = Collections.synchronizedMap(
        object : LinkedHashMap<CacheKey, Component>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, Component>?): Boolean =
                size > MAX_CACHE_SIZE
        }
    )

    fun component(raw: String, parseMiniMessage: Boolean = true): Component {
        val text = raw.trim()
        if (text.isBlank()) return Component.empty()
        val key = CacheKey(text, parseMiniMessage)
        // Nexo owns live glyph definitions. A glyph reload must affect the next render.
        val cacheable = glyphProvider?.available != true
        if (cacheable) componentCache[key]?.let { return it }

        val parsed = parseComponent(text, parseMiniMessage) ?: Component.text(text)
        if (cacheable) componentCache[key] = parsed
        return parsed
    }

    /** Shared trusted-template parser. Only Nexo's glyph and shift tags are added. */
    fun miniMessage(): MiniMessage = mini

    internal fun useGlyphProvider(provider: GlyphTextProvider?) {
        glyphProvider = provider
        componentCache.clear()
    }

    fun mini(raw: String, parseMiniMessage: Boolean = true): String =
        mini(component(raw, parseMiniMessage))

    fun mini(component: Component): String =
        mini.serialize(component)

    fun plain(raw: String, parseMiniMessage: Boolean = true): String =
        plain(component(raw, parseMiniMessage))

    fun plain(component: Component): String =
        plainSerializer.serialize(readable(component)).trim()

    /**
     * Adapts an already-rendered title to the current client without changing
     * normal menu text. Bedrock receives readable glyph IDs instead of Java
     * resource-pack glyph codepoints.
     */
    fun inventoryTitle(component: Component, bedrock: Boolean): Component {
        if (!bedrock) return component
        return readable(component)
    }

    private fun readable(component: Component): Component =
        glyphProvider?.takeIf { it.available }?.readable(component) ?: component

    fun legacyAmpersand(raw: String, parseMiniMessage: Boolean = true): String =
        legacyAmpersand.serialize(component(raw, parseMiniMessage))

    fun escapePlain(raw: String): String =
        raw

    private fun parseComponent(text: String, parseMiniMessage: Boolean): Component? {
        if (parseMiniMessage && miniLikeTagPattern.containsMatchIn(text)) {
            deserializeMiniSafely(text)?.let { return it }
        }

        return when {
            sectionLegacyPattern.containsMatchIn(text) -> {
                runCatching { legacySection.deserialize(normalizeSectionHex(text)) }.getOrNull()
            }
            ampersandLegacyPattern.containsMatchIn(text) -> {
                runCatching { legacyAmpersand.deserialize(normalizeAmpersandHex(text)) }.getOrNull()
            }
            parseMiniMessage -> {
                deserializeMiniSafely(text)
            }
            else -> null
        }
    }

    private fun deserializeMiniSafely(text: String): Component? {
        return runCatching { mini.deserialize(text) }.getOrNull()
    }

    private fun normalizeAmpersandHex(text: String): String =
        ampersandHexPattern.replace(text) { match ->
            val hex = match.groupValues[1].lowercase(Locale.ROOT)
            buildString(14) {
                append("&x")
                for (char in hex) {
                    append('&')
                    append(char)
                }
            }
        }

    private fun normalizeSectionHex(text: String): String =
        sectionHexPattern.replace(text) { match ->
            val hex = match.groupValues[1].lowercase(Locale.ROOT)
            buildString(14) {
                append('\u00A7')
                append('x')
                for (char in hex) {
                    append('\u00A7')
                    append(char)
                }
            }
        }

    private data class CacheKey(
        val text: String,
        val parseMiniMessage: Boolean
    )
}
