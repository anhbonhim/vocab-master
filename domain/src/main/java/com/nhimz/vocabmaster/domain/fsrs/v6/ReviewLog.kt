package com.nhimz.vocabmaster.domain.fsrs.v6

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * FSRS-6 review log entry.
 *
 * Serde uses py-fsrs JSON key names:
 * - `card_id`
 * - `rating` (stored as int value 1..4)
 * - `review_datetime` (UTC epoch milliseconds)
 * - `review_duration` (milliseconds, optional)
 */
@Serializable
private data class ReviewLogJson(
    @SerialName("card_id") val cardId: String,
    @SerialName("rating") val ratingValue: Int,
    @SerialName("review_datetime") val reviewDatetime: Long,
    @SerialName("review_duration") val reviewDuration: Long? = null
)

/**
 * @param cardId Identifier of the reviewed card.
 * @param rating The rating given during the review.
 * @param reviewDatetime UTC epoch milliseconds of the review.
 * @param reviewDuration Optional review duration in milliseconds.
 */
data class ReviewLog(
    val cardId: String,
    val rating: Rating,
    val reviewDatetime: Long,
    val reviewDuration: Long? = null
) {
    /**
     * Returns a plain [Map] representation using py-fsrs key names.
     */
    fun toDict(): Map<String, Any?> = mapOf(
        "card_id" to cardId,
        "rating" to rating.value,
        "review_datetime" to reviewDatetime,
        "review_duration" to reviewDuration
    )

    companion object {
        /**
         * Builds a [ReviewLog] from a py-fsrs-style [Map].
         */
        @Suppress("UNCHECKED_CAST")
        fun fromDict(dict: Map<String, Any?>): ReviewLog {
            return ReviewLog(
                cardId = dict["card_id"] as String,
                rating = Rating.values().first { it.value == (dict["rating"] as Number).toInt() },
                reviewDatetime = (dict["review_datetime"] as Number).toLong(),
                reviewDuration = (dict["review_duration"] as Number?)?.toLong()
            )
        }

        private val json = Json { encodeDefaults = true }

        fun toJson(reviewLog: ReviewLog): String = json.encodeToString(
            ReviewLogJson.serializer(),
            ReviewLogJson(
                cardId = reviewLog.cardId,
                ratingValue = reviewLog.rating.value,
                reviewDatetime = reviewLog.reviewDatetime,
                reviewDuration = reviewLog.reviewDuration
            )
        )

        fun fromJson(source: String): ReviewLog {
            val dto = json.decodeFromString(ReviewLogJson.serializer(), source)
            return ReviewLog(
                cardId = dto.cardId,
                rating = Rating.values().first { it.value == dto.ratingValue },
                reviewDatetime = dto.reviewDatetime,
                reviewDuration = dto.reviewDuration
            )
        }
    }
}
