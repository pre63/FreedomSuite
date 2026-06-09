package org.freedomsuite.inbox.spam

enum class SpamVerdict {
    HAM,
    SUSPECT,
    SPAM,
}

data class SpamRuleHit(
    val ruleId: String,
    val weight: Int,
    val detail: String,
)

data class SpamClassification(
    val score: Int,
    val verdict: SpamVerdict,
    val hits: List<SpamRuleHit>,
) {
    val reasons: String = hits.joinToString(";") { "${it.ruleId}:${it.weight}" }
}
