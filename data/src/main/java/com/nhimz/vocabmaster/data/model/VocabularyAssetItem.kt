package com.nhimz.vocabmaster.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class VocabularyAssetItem(
    val word: String,
    val level: String,
    val type: String,
    val definition: String? = null,
    val translation: Translation,
    val phonetic: String? = null,
    val examples: Examples
)

@Serializable
internal data class Translation(
    val en: String? = null,
    val vi: String? = null,
    val es: String? = null,
    val fr: String? = null
)

@Serializable
internal data class Examples(
    val beginner: List<Example>? = null,
    val intermediate: List<Example>? = null,
    val advanced: List<Example>? = null
)

@Serializable
internal data class Example(
    val text: String,
    val translation: Translation
)
