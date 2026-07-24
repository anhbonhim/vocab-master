package com.nhimz.vocabmaster.domain.model

interface BackupRepository {
    suspend fun exportBackup(): String
    suspend fun importBackup(jsonString: String): Result<Boolean>
}
