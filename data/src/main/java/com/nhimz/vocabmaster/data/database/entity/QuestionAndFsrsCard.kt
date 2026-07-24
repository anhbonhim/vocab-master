package com.nhimz.vocabmaster.data.database.entity

/**
 * Plain data holder pairing a curriculum [QuestionEntity] with its optional per-user
 * [FsrsCardEntity].
 *
 * Previously this was a Room `@Embedded`/`@Relation` projection, but after the split-database
 * refactor the two sides live in separate Room databases ([CurriculumDatabase] and
 * [UserDataDatabase]). The repository assembles each pair in memory (see
 * `VocabularyRepositoryImpl.assembleQuestionAndCards`), so this is now a plain data class with no
 * Room annotations.
 */
data class QuestionAndFsrsCard(
    val question: QuestionEntity,
    val fsrsCard: FsrsCardEntity?
)
