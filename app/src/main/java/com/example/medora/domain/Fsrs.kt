package com.example.medora.domain

import com.example.medora.data.Card
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToLong

// FSRS-4.5 scheduler, written from the published formulas with the default weights.
// Target retention 90%: at that retention the next interval equals the stability (days).
object Fsrs {
    enum class Rating(val value: Int) { Again(1), Hard(2), Good(3), Easy(4) }

    private val w = doubleArrayOf(
        0.4872, 1.4003, 3.7145, 13.8206, 5.1618, 1.2298, 0.8975, 0.031, 1.6474,
        0.1367, 1.0461, 2.1072, 0.0793, 0.3246, 1.587, 0.2272, 2.8755,
    )
    private const val DECAY = -0.5
    private const val FACTOR = 19.0 / 81.0
    private const val DAY = 86_400_000L
    private const val RELEARN_MS = 10 * 60_000L
    private const val MAX_STABILITY = 36_500.0

    private fun retrievability(elapsedDays: Double, stability: Double): Double =
        (1 + FACTOR * elapsedDays / stability).pow(DECAY)

    private fun initDifficulty(g: Int): Double = (w[4] - (g - 3) * w[5]).coerceIn(1.0, 10.0)

    private fun nextDifficulty(d: Double, g: Int): Double {
        val updated = d - w[6] * (g - 3)
        return (w[7] * initDifficulty(3) + (1 - w[7]) * updated).coerceIn(1.0, 10.0)
    }

    private fun recallStability(d: Double, s: Double, r: Double, g: Int): Double {
        val hard = if (g == 2) w[15] else 1.0
        val easy = if (g == 4) w[16] else 1.0
        return s * (exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp(w[10] * (1 - r)) - 1) * hard * easy + 1)
    }

    private fun forgetStability(d: Double, s: Double, r: Double): Double =
        w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp(w[14] * (1 - r))

    // Returns the card with its new schedule after one answer.
    fun schedule(card: Card, rating: Rating, now: Long = System.currentTimeMillis()): Card {
        val g = rating.value
        val isNew = card.reps == 0 || card.stability <= 0.0
        val s: Double
        val d: Double
        if (isNew) {
            s = w[g - 1]
            d = initDifficulty(g)
        } else {
            val elapsed = (now - card.lastReview).coerceAtLeast(0L) / DAY.toDouble()
            val r = retrievability(elapsed, card.stability)
            s = if (g == 1) {
                forgetStability(card.difficulty, card.stability, r)
            } else {
                recallStability(card.difficulty, card.stability, r, g)
            }
            d = nextDifficulty(card.difficulty, g)
        }
        val stability = s.coerceIn(0.1, MAX_STABILITY)
        val due = if (g == 1) {
            now + RELEARN_MS
        } else {
            now + stability.roundToLong().coerceAtLeast(1L) * DAY
        }
        return card.copy(
            stability = stability,
            difficulty = d,
            due = due,
            reps = card.reps + 1,
            lapses = card.lapses + if (g == 1 && !isNew) 1 else 0,
            lastReview = now,
        )
    }
}
