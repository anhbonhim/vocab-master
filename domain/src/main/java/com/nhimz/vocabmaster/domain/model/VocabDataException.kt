package com.nhimz.vocabmaster.domain.model

/**
 * Typed exception for data-layer parse/decoding failures.
 * Carries a human-readable message and the original cause so callers can log or
 * surface diagnostics without silently swallowing corrupt JSON.
 */
class VocabDataException(message: String, cause: Throwable? = null) : Exception(message, cause)
