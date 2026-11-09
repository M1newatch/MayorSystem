package mayorSystem.perks

import mayorSystem.MayorPlugin
import mayorSystem.api.events.MayorPerksAppliedEvent
import mayorSystem.api.events.MayorPerksClearedEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.LootGenerateEvent
import java.util.Random
import kotlin.math.max

/**
 * Applies configured loot-curse perks (per-perk `drop_modifier` config key) to mob
 * loot. A `drop_modifier: 0.81` means: every generated loot entry has an 81% chance
 * to still drop (the "Mehrwertsteuer" curse seizes the remaining 19%).
 *
 * Semantics (deliberately simple and reversible):
 * - Only loot with a non-null, non-player entity context is taxed: mob kills,
 *   piglin barters, cat gifts, fishing-hook artifacts — basically "your hunting
 *   loot". Chests, crates and other null-entity loot tables are untouched.
 * - Player-linked loot contexts are never modified.
 * - Multiple active loot-curse perks multiply (0.8 * 0.5 = 0.4).
 * - The listener fires [MayorPerksAppliedEvent]/[MayorPerksClearedEvent]-driven
 *   state plus a boot-time refresh (mid-term restarts do not re-apply perks).
 */
class LootCurseListener(private val plugin: MayorPlugin) : Listener {

    @Volatile private var activeModifier: Double? = null

    /** Called from term bootstrap/rebuild so mid-term restarts keep the curse active. */
    fun refreshForTerm(term: Int) {
        if (!plugin.config.getBoolean("perks.enabled", true) || term < 0) {
            activeModifier = null
            return
        }
        val mayor = plugin.store.winner(term)
        val modifier = if (mayor == null) {
            null
        } else {
            modifierFor(plugin.store.chosenPerks(term, mayor))
        }
        activeModifier = modifier
        if (modifier != null) {
            plugin.logger.info("Loot curse active for term $term (drop_modifier=$modifier).")
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPerksApplied(event: MayorPerksAppliedEvent) {
        activeModifier = modifierFor(event.perkIds)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPerksCleared(event: MayorPerksClearedEvent) {
        // Cleared perks never retain a loot curse.
        activeModifier = null
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onLootGenerate(event: LootGenerateEvent) {
        val modifier = activeModifier ?: return
        if (modifier >= 0.999) return // no-op guard so a clean catalog never taxes
        val victim = event.entity ?: return // null entity = chest/crate loot -> untouched
        if (victim is Player) return
        val loot = event.loot
        if (loot.isEmpty()) return

        val iterator = loot.iterator()
        while (iterator.hasNext()) {
            val stack = iterator.next()
            if (stack.type.isAir) continue
            // Drop chance semantics: `modifier` is the probability an entry survives.
            if (random.nextDouble() >= modifier) iterator.remove()
        }
    }

    /** Product of all active loot-curse modifiers in the term selection, or null if none. */
    private fun modifierFor(perkIds: Collection<String>): Double? {
        val preset = plugin.perks.presetPerks()
        var product = 1.0
        var found = false
        for (perkId in perkIds) {
            val def = preset[perkId] ?: continue
            val modifier = def.dropModifier ?: continue
            product *= modifier
            found = true
        }
        return if (found) max(MIN_MODIFIER, product) else null
    }

    private companion object {
        private val random = Random()
        private const val MIN_MODIFIER = 0.01
    }
}
