package com.nhimz.vocabmaster.ui.screens.debug_components

import com.nhimz.vocabmaster.data.database.VocabDao
import com.nhimz.vocabmaster.domain.fsrs.v6.Scheduler
import com.nhimz.vocabmaster.domain.fsrs.v6.State
import com.nhimz.vocabmaster.domain.model.BackupRepository
import com.nhimz.vocabmaster.domain.model.SettingsRepository
import kotlinx.coroutines.flow.first

private const val DIFFICULTY_MIN = 1.0
private const val DIFFICULTY_MAX = 10.0

@Suppress("LongMethod", "CyclomaticComplexMethod")
suspend fun testDatabaseIntegrityCheck(vocabDao: VocabDao): TestResult {
    return runTest(
        name = "Database Integrity Check",
        description = "Scans all FSRS cards and review logs for stability, due-ordering, and logic anomalies."
    ) { log, assertions ->
        log.appendLine("Fetching all FSRS cards from database...")
        val cards = vocabDao.getAllCards()
        val totalCards = cards.size
        log.appendLine("Total FSRS cards: $totalCards")

        var stabilityAnomalies = 0
        var difficultyAnomalies = 0
        var dueOrderAnomalies = 0
        var repsWithoutReviewDate = 0

        cards.forEach { card ->
            val stability = card.stability
            if (stability != null && stability < Scheduler.STABILITY_MIN) {
                stabilityAnomalies++
                log.appendLine("Anomaly: Card ${card.questionId} has stability below floor: $stability")
            }
            val difficulty = card.difficulty
            val isNonNew = card.state != State.New.value
            val difficultyOutOfBounds = difficulty != null &&
                (difficulty < DIFFICULTY_MIN || difficulty > DIFFICULTY_MAX)
            if (isNonNew && difficultyOutOfBounds) {
                difficultyAnomalies++
                log.appendLine(
                    "Anomaly: Card ${card.questionId} has difficulty out of bounds: $difficulty"
                )
            }
            val lastReview = card.lastReview
            if (lastReview != null && card.due < lastReview) {
                dueOrderAnomalies++
                log.appendLine(
                    "Anomaly: Card ${card.questionId} due (${card.due}) before lastReview ($lastReview)"
                )
            }
            if (card.reps > 0 && lastReview == null) {
                repsWithoutReviewDate++
                log.appendLine("Anomaly: Card ${card.questionId} has reps > 0 but lastReview is null")
            }
        }

        assertions.add(
            AssertionResult(
                "Stability values are at or above floor",
                stabilityAnomalies == 0,
                "Found $stabilityAnomalies cards below stability floor"
            )
        )
        assertions.add(
            AssertionResult(
                "Difficulty values in [$DIFFICULTY_MIN, $DIFFICULTY_MAX] for non-New cards",
                difficultyAnomalies == 0,
                "Found $difficultyAnomalies difficulty anomalies"
            )
        )
        assertions.add(
            AssertionResult(
                "Due date is not before last review",
                dueOrderAnomalies == 0,
                "Found $dueOrderAnomalies due-ordering anomalies"
            )
        )
        assertions.add(
            AssertionResult(
                "Cards with reps > 0 have a lastReview timestamp",
                repsWithoutReviewDate == 0,
                "Found $repsWithoutReviewDate cards with reps > 0 but null lastReview"
            )
        )

        // Check for orphan review logs
        log.appendLine("Fetching all review logs from database...")
        val reviewLogs = vocabDao.getAllReviewLogsList()
        val cardIds = cards.map { it.questionId }.toSet()
        var orphanLogs = 0
        reviewLogs.forEach { logItem ->
            if (!cardIds.contains(logItem.cardId)) {
                orphanLogs++
                log.appendLine(
                    "Anomaly: ReviewLog ID ${logItem.id} is orphan (Card ID ${logItem.cardId} does not exist)"
                )
            }
        }
        assertions.add(
            AssertionResult(
                "No orphan review logs exist",
                orphanLogs == 0,
                "Found $orphanLogs orphan review logs"
            )
        )

        val isClean = stabilityAnomalies == 0 && difficultyAnomalies == 0 &&
            dueOrderAnomalies == 0 && repsWithoutReviewDate == 0 && orphanLogs == 0
        if (isClean) TestStatus.PASS else TestStatus.FAIL
    }
}

@Suppress("LongMethod")
suspend fun testBackupRestoreRoundtrip(
    backupRepository: BackupRepository,
    vocabDao: VocabDao,
    settingsRepository: SettingsRepository
): TestResult {
    return runTest(
        name = "Backup/Restore Roundtrip",
        description = "Export current progress, perform restore, and verify zero data regression."
    ) { log, assertions ->
        // 1. Snapshot current state
        log.appendLine("1. Recording pre-backup snapshot...")
        val preCardsCount = vocabDao.getCardCount()
        val preLogsCount = vocabDao.getAllReviewLogsList().size
        val preXp = settingsRepository.xpTotal.first()
        val preStreak = settingsRepository.currentStreak.first()
        log.appendLine(
            "Pre-backup state: cards=$preCardsCount, logs=$preLogsCount, XP=$preXp, streak=$preStreak"
        )

        // 2. Export backup
        log.appendLine("2. Exporting backup...")
        val json = backupRepository.exportBackup()
        val jsonLength = json.length
        assertions.add(AssertionResult("Exported JSON is non-empty", json.isNotBlank(), "Length: $jsonLength"))
        log.appendLine("Exported JSON successfully (size: $jsonLength chars). Preview:")
        log.appendLine(json.take(150) + "...")

        // 3. Restore backup
        log.appendLine("3. Importing backup...")
        val importSuccess = backupRepository.importBackup(json)
        assertions.add(AssertionResult("Import operation returned success", importSuccess))

        // 4. Verify post-backup state
        log.appendLine("4. Recording post-restore snapshot...")
        val postCardsCount = vocabDao.getCardCount()
        val postLogsCount = vocabDao.getAllReviewLogsList().size
        val postXp = settingsRepository.xpTotal.first()
        val postStreak = settingsRepository.currentStreak.first()
        log.appendLine(
            "Post-restore state: cards=$postCardsCount, logs=$postLogsCount, XP=$postXp, streak=$postStreak"
        )

        val cardsMatch = preCardsCount == postCardsCount
        val logsMatch = preLogsCount == postLogsCount
        val xpMatch = preXp == postXp
        val streakMatch = preStreak == postStreak

        assertions.add(
            AssertionResult(
                "Cards count matches pre-backup ($preCardsCount)",
                cardsMatch,
                "Post-restore got: $postCardsCount"
            )
        )
        assertions.add(
            AssertionResult(
                "Review logs count matches pre-backup ($preLogsCount)",
                logsMatch,
                "Post-restore got: $postLogsCount"
            )
        )
        assertions.add(
            AssertionResult(
                "XP matches pre-backup ($preXp)",
                xpMatch,
                "Post-restore got: $postXp"
            )
        )
        assertions.add(
            AssertionResult(
                "Streak matches pre-backup ($preStreak)",
                streakMatch,
                "Post-restore got: $postStreak"
            )
        )

        val passed = importSuccess && cardsMatch && logsMatch && xpMatch && streakMatch
        if (passed) TestStatus.PASS else TestStatus.FAIL
    }
}
