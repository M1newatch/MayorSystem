package mayorSystem.domain.perk

data class PerkCatalogEntry(
    val id: String,
    val sectionId: String,
    val enabled: Boolean = true
)

data class PerkSectionRule(
    val sectionId: String,
    /** Upper bound for picks in this section; null = unlimited (0 in config). */
    val pickLimit: Int?,
    /**
     * Lower bound for picks in this section; > 0 makes the section mandatory.
     * The sum of all minima must never exceed the term's allowed pick count or
     * no valid selection exists (guarded by HealthService).
     */
    val pickMin: Int = 0
)

data class PerkSelectionInput(
    val selectedPerkIds: Set<String>,
    val requiredTotal: Int,
    val catalog: Collection<PerkCatalogEntry>,
    val sectionRules: Collection<PerkSectionRule>
)

sealed class PerkSelectionResult {
    data object Allowed : PerkSelectionResult()
    data class WrongTotal(val required: Int, val selected: Int) : PerkSelectionResult()
    data class UnknownPerks(val ids: Set<String>) : PerkSelectionResult()
    data class DisabledPerks(val ids: Set<String>) : PerkSelectionResult()
    data class SectionLimitExceeded(val sectionId: String, val limit: Int, val selected: Int) : PerkSelectionResult()
    data class SectionMinimumNotMet(val sectionId: String, val min: Int, val selected: Int) : PerkSelectionResult()
}

class PerkSelectionPolicy {
    fun evaluate(input: PerkSelectionInput): PerkSelectionResult {
        if (input.selectedPerkIds.size != input.requiredTotal) {
            return PerkSelectionResult.WrongTotal(input.requiredTotal, input.selectedPerkIds.size)
        }

        val catalogById = input.catalog.associateBy { it.id }
        val unknown = input.selectedPerkIds.filterNot { it.startsWith("custom:", ignoreCase = true) || it in catalogById }.toSet()
        if (unknown.isNotEmpty()) return PerkSelectionResult.UnknownPerks(unknown)

        val disabled = input.selectedPerkIds
            .mapNotNull { catalogById[it] }
            .filterNot { it.enabled }
            .map { it.id }
            .toSet()
        if (disabled.isNotEmpty()) return PerkSelectionResult.DisabledPerks(disabled)

        val maxLimits = input.sectionRules
            .mapNotNull { rule -> rule.pickLimit?.takeIf { it >= 0 }?.let { rule.sectionId to it } }
            .toMap()

        val counts = linkedMapOf<String, Int>()
        for (perkId in input.selectedPerkIds) {
            val sectionId = if (perkId.startsWith("custom:", ignoreCase = true)) {
                "custom"
            } else {
                catalogById[perkId]?.sectionId ?: continue
            }
            counts[sectionId] = (counts[sectionId] ?: 0) + 1
        }

        // Max limits fail first: they tell the candidate which section to slim down.
        for ((sectionId, selected) in counts) {
            val limit = maxLimits[sectionId] ?: continue
            if (selected > limit) {
                return PerkSelectionResult.SectionLimitExceeded(sectionId, limit, selected)
            }
        }

        // Then minimums: which mandatory section is still missing.
        for (rule in input.sectionRules) {
            val min = rule.pickMin.takeIf { it > 0 } ?: continue
            val selected = counts[rule.sectionId] ?: 0
            if (selected < min) {
                return PerkSelectionResult.SectionMinimumNotMet(rule.sectionId, min, selected)
            }
        }

        return PerkSelectionResult.Allowed
    }
}