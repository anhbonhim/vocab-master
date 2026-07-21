@file:Suppress("MagicNumber")
package com.nhimz.vocabmaster.domain.fsrs.v6

/**
 * FSRS-6 card state.
 *
 * Int values match the legacy VocabMaster DB encoding AND py-fsrs state values.
 * py-fsrs has only Learning(1)/Review(2)/Relearning(3); a pristine card is represented as
 * Learning with step=0 and null stability/difficulty. VocabMaster's v6 port keeps an
 * explicit [New] state (value 0) as an alias for that pristine-Learning card, so the
 * UI/DB can continue to distinguish never-seen cards. After any first review the card
 * becomes Learning or Review and never returns to New.
 */
enum class State(val value: Int) {
    New(0),
    Learning(1),
    Review(2),
    Relearning(3)
}

/**
 * FSRS-6 review rating.
 *
 * Int values match py-fsrs Rating (Again=1..Easy=4) and legacy DB encoding.
 */
enum class Rating(val value: Int) {
    Again(1),
    Hard(2),
    Good(3),
    Easy(4)
}
