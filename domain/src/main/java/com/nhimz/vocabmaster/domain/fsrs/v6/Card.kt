package com.nhimz.vocabmaster.domain.fsrs.v6

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * FSRS-6 card model.
 *
 * Deviation from py-fsrs:
 * - [due] and [lastReview] are stored as UTC epoch milliseconds ([Long]) instead of
 *   ISO-8601 strings. This avoids string parsing in the scheduler math and makes the
 *   domain module locale-safe.
 * - [cardId] is a [String] because VocabMaster identifies cards by questionId.
 * - [reps] and [lapses] are non-math metadata carried for the UI/Statistics layer;
 *   py-fsrs does not have these fields. They are included in serde under "reps"/"lapses".
 *
 * Serde uses py-fsrs JSON key names:
 * - `card_id`
 * - `state` (stored as int value)
 * - `step`
 * - `stability`
 * - `difficulty`
 * - `due` (epoch milliseconds)
 * - `last_review` (epoch milliseconds or null)
 * - `reps`
 * - `lapses`
 */
@Serializable
private data class CardJson(
    @SerialName("card_id") val cardId: String,
    @SerialName("state") val stateValue: Int,
    @SerialName("step") val step: Int? = null,
    @SerialName("stability") val stability: Double? = null,
    @SerialName("difficulty") val difficulty: Double? = null,
    @SerialName("due") val due: Long,
    @SerialName("last_review") val lastReview: Long? = null,
    @SerialName("reps") val reps: Int = 0,
    @SerialName("lapses") val lapses: Int = 0
)

/**
 * @param cardId Identifier of the card (VocabMaster uses questionId).
 * @param state Current FSRS state. [State.New] is the alias for a pristine py-fsrs card.
 * @param step Current learning/relearning step, or null when in Review state.
 * @param stability FSRS stability value, or null for a pristine card.
 * @param difficulty FSRS difficulty value, or null for a pristine card.
 * @param due UTC epoch milliseconds when the card is due next.
 * @param lastReview UTC epoch milliseconds of the last review, or null.
 * @param reps Non-math metadata: total review count.
 * @param lapses Non-math metadata: total lapse count.
 */
data class Card(
    val cardId: String,
    val state: State = State.New,
    val step: Int? = 0,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val due: Long = System.currentTimeMillis(),
    val lastReview: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0
) {
    /**
     * Returns a plain [Map] representation using py-fsrs key names.
     */
    fun toDict(): Map<String, Any?> = mapOf(
        "card_id" to cardId,
        "state" to state.value,
        "step" to step,
        "stability" to stability,
        "difficulty" to difficulty,
        "due" to due,
        "last_review" to lastReview,
        "reps" to reps,
        "lapses" to lapses
    )

    companion object {
        /**
         * Builds a [Card] from a py-fsrs-style [Map].
         */
        @Suppress("UNCHECKED_CAST")
        fun fromDict(dict: Map<String, Any?>): Card {
            return Card(
                cardId = dict["card_id"] as String,
                state = State.values().first { it.value == (dict["state"] as Number).toInt() },
                step = (dict["step"] as Number?)?.toInt(),
                stability = (dict["stability"] as Number?)?.toDouble(),
                difficulty = (dict["difficulty"] as Number?)?.toDouble(),
                due = (dict["due"] as Number).toLong(),
                lastReview = (dict["last_review"] as Number?)?.toLong(),
                reps = (dict["reps"] as Number?)?.toInt() ?: 0,
                lapses = (dict["lapses"] as Number?)?.toInt() ?: 0
            )
        }

        private val json = Json { encodeDefaults = true }

        fun toJson(card: Card): String = json.encodeToString(
            CardJson.serializer(),
            CardJson(
                cardId = card.cardId,
                stateValue = card.state.value,
                step = card.step,
                stability = card.stability,
                difficulty = card.difficulty,
                due = card.due,
                lastReview = card.lastReview,
                reps = card.reps,
                lapses = card.lapses
            )
        )

        fun fromJson(source: String): Card {
            val dto = json.decodeFromString(CardJson.serializer(), source)
            return Card(
                cardId = dto.cardId,
                state = State.values().first { it.value == dto.stateValue },
                step = dto.step,
                stability = dto.stability,
                difficulty = dto.difficulty,
                due = dto.due,
                lastReview = dto.lastReview,
                reps = dto.reps,
                lapses = dto.lapses
            )
        }
    }
}
